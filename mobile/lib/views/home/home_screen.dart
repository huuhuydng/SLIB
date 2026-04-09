import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/models/new_book_model.dart';
import 'package:slib/models/user_profile.dart';
import 'package:slib/models/news_model.dart';
import 'package:slib/services/auth/auth_service.dart';
import 'package:slib/services/news/news_service.dart';
import 'package:slib/services/new_books/new_book_service.dart';
import 'package:slib/services/app/local_storage_service.dart';
import 'package:slib/views/home/widgets/home_appbar.dart';
import 'package:slib/views/home/widgets/live_status_dashboard.dart';
import 'package:slib/views/home/widgets/new_books_slider.dart';
import 'package:slib/views/home/widgets/upcoming_booking_card.dart';
import 'package:slib/views/home/widgets/quick_action_grid.dart';
import 'package:slib/views/home/widgets/ai_suggestion_card.dart';
import 'package:slib/views/home/widgets/news_slider.dart';
import 'package:slib/views/home/widgets/compact_header.dart';
import 'package:slib/views/new_books/new_books_screen.dart';
import 'package:slib/views/home/widgets/section_title.dart';
import 'package:slib/views/news/news_screen.dart';
import 'package:slib/views/widgets/error_display_widget.dart';
import 'package:slib/views/widgets/feedback_dialog.dart';

class HomeScreen extends StatefulWidget {
  final UserProfile? user;
  final bool isActive;

  const HomeScreen({super.key, this.user, this.isActive = true});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with WidgetsBindingObserver {
  final ScrollController _scrollController = ScrollController();

  // --- GLOBAL KEYS FOR REFRESH ---
  final GlobalKey<UpcomingBookingCardState> _bookingCardKey = GlobalKey();
  final GlobalKey<LiveStatusDashboardState> _liveStatusKey = GlobalKey();
  int _aiCardRefreshVersion = 0;

  // --- STATE QUẢN LÝ TIN TỨC ---
  List<News> _newsList = [];
  List<NewBook> _newBooksList = [];
  bool _isLoading = true;

  final NewsService _newsService = NewsService();
  final NewBookService _newBookService = NewBookService();
  final LocalStorageService _localService = LocalStorageService();

  double _headerOpacity = 1.0;
  double _headerOffset = 0.0;
  bool _showCompactHeader = false;
  bool _isCheckingFeedback = false;
  bool _isFeedbackDialogOpen = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _scrollController.addListener(_onScroll);
    _loadHomeContent();
    _schedulePendingFeedbackCheck(delay: const Duration(seconds: 2));
  }

  @override
  void didUpdateWidget(covariant HomeScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!oldWidget.isActive && widget.isActive) {
      _schedulePendingFeedbackCheck();
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && widget.isActive) {
      _schedulePendingFeedbackCheck();
    }
  }

  // Chiến thuật: Cache trước -> API sau
  void _loadHomeContent() async {
    final cachedNews = await _localService.loadNewsList();
    final cachedNewBooks = await _localService.loadNewBooksList();

    if ((cachedNews.isNotEmpty || cachedNewBooks.isNotEmpty) && mounted) {
      setState(() {
        _newsList = cachedNews;
        _newBooksList = _sortNewBooks(cachedNewBooks);
        _isLoading = false;
      });
    }

    try {
      final results = await Future.wait([
        _newsService.fetchPublicNews(),
        _newBookService.fetchPublicNewBooks(),
      ]);

      final freshNews = results[0] as List<News>;
      final freshNewBooks = results[1] as List<NewBook>;

      if (mounted) {
        setState(() {
          _newsList = freshNews;
          _newBooksList = _sortNewBooks(freshNewBooks);
          _isLoading = false;
        });
      }

      await _localService.saveNewsList(freshNews);
      await _localService.saveNewBooksList(freshNewBooks);
    } catch (e) {
      debugPrint("Lỗi tải nội dung trang chủ: $e");
      if (mounted && _newsList.isEmpty && _newBooksList.isEmpty) {
        setState(() => _isLoading = false);
      }
    }
  }

  void _schedulePendingFeedbackCheck({Duration delay = Duration.zero}) {
    if (!widget.isActive || _isCheckingFeedback || _isFeedbackDialogOpen) {
      return;
    }

    Future<void>(() async {
      if (delay > Duration.zero) {
        await Future.delayed(delay);
      }

      if (!mounted ||
          !widget.isActive ||
          _isCheckingFeedback ||
          _isFeedbackDialogOpen) {
        return;
      }

      await _checkPendingFeedback();
    });
  }

  Future<void> _checkPendingFeedback() async {
    if (_isCheckingFeedback || _isFeedbackDialogOpen) return;
    _isCheckingFeedback = true;

    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final url = Uri.parse(
        '${ApiConstants.domain}/slib/feedbacks/check-pending',
      );
      final response = await authService.authenticatedRequest('GET', url);

      if (!mounted || response.statusCode != 200) return;

      final data = jsonDecode(response.body) as Map<String, dynamic>;
      if (data['hasPending'] != true) return;

      final reservationId = data['reservationId'] as String;
      final zoneName = data['zoneName'] as String? ?? '';
      final seatCode = data['seatCode'] as String? ?? '';

      if (!mounted) return;

      _isFeedbackDialogOpen = true;
      try {
        await showSeatFeedbackPopup(
          context,
          reservationId: reservationId,
          zoneName: zoneName,
          seatCode: seatCode,
        );
      } finally {
        _isFeedbackDialogOpen = false;
      }
    } catch (e) {
      debugPrint('[HOME] Error checking pending feedback: $e');
    } finally {
      _isCheckingFeedback = false;
    }
  }

  Future<void> _onRefresh() async {
    // Refresh tất cả widgets cùng lúc
    await Future.wait([
      _refreshBookingCard(),
      _refreshLiveStatus(),
      _refreshAICard(),
      _refreshNews(),
      _refreshNewBooks(),
    ]);
  }

  Future<void> _refreshBookingCard() async {
    await _bookingCardKey.currentState?.refresh();
  }

  Future<void> _refreshLiveStatus() async {
    await _liveStatusKey.currentState?.refresh();
  }

  Future<void> _refreshAICard() async {
    if (!mounted) return;
    setState(() {
      _aiCardRefreshVersion++;
    });
  }

  Future<void> _refreshNews() async {
    try {
      final freshNews = await _newsService.fetchPublicNews();
      if (mounted) setState(() => _newsList = freshNews);
      await _localService.saveNewsList(freshNews);
    } catch (e) {
      debugPrint("Refresh news error: $e");
    }
  }

  Future<void> _refreshNewBooks() async {
    try {
      final freshNewBooks = await _newBookService.fetchPublicNewBooks();
      if (mounted) {
        setState(() => _newBooksList = _sortNewBooks(freshNewBooks));
      }
      await _localService.saveNewBooksList(freshNewBooks);
    } catch (e) {
      debugPrint("Refresh new books error: $e");
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    const double maxScroll = 200.0;
    final double offset = _scrollController.offset;
    setState(() {
      _headerOpacity = (1.0 - (offset / maxScroll)).clamp(0.0, 1.0);
      _headerOffset = (offset / 2).clamp(0.0, 100.0);
      _showCompactHeader = offset > 150.0;
    });
  }

  List<NewBook> _sortNewBooks(List<NewBook> books) {
    final sorted = List<NewBook>.from(books);
    sorted.sort((a, b) => b.arrivalDate.compareTo(a.arrivalDate));
    return sorted;
  }

  @override
  Widget build(BuildContext context) {
    final topPadding = MediaQuery.of(context).padding.top;

    // --- LOGIC LỌC TIN GHIM (Xử lý ngay trong build) ---
    List<News> displayNews = [];
    if (_newsList.isNotEmpty) {
      // Ưu tiên tin ghim
      List<News> pinnedNews = _newsList.where((item) => item.isPinned).toList();

      // Nếu ít tin ghim quá thì lấy thêm tin mới nhất cho đủ 5
      if (pinnedNews.length < 5) {
        var remaining = _newsList.where((item) => !item.isPinned).toList();
        pinnedNews.addAll(remaining.take(5 - pinnedNews.length));
      }
      displayNews = pinnedNews;
    }
    // ---------------------------------------------------
    final displayNewBooks = _newBooksList.take(6).toList();

    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA),
      body: Stack(
        children: [
          RefreshIndicator(
            onRefresh: _onRefresh,
            color: AppColors.brandColor,
            child: SingleChildScrollView(
              controller: _scrollController,
              padding: EdgeInsets.zero,
              physics: const AlwaysScrollableScrollPhysics(),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // HEADER
                  AnimatedOpacity(
                    duration: const Duration(milliseconds: 300),
                    opacity: _headerOpacity,
                    curve: Curves.easeInOut,
                    child: Transform.translate(
                      offset: Offset(0, -_headerOffset),
                      child: Container(
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            begin: Alignment.topLeft,
                            end: Alignment.bottomRight,
                            colors: [
                              AppColors.brandColor,
                              const Color(0xFFFF9052),
                            ],
                          ),
                          borderRadius: const BorderRadius.only(
                            bottomLeft: Radius.circular(30),
                            bottomRight: Radius.circular(30),
                          ),
                        ),
                        padding: EdgeInsets.fromLTRB(
                          20,
                          topPadding + 10,
                          20,
                          20,
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            HomeAppBar(user: widget.user),
                            const SizedBox(height: 20),
                            LiveStatusDashboard(key: _liveStatusKey),
                          ],
                        ),
                      ),
                    ),
                  ),

                  // BODY
                  Padding(
                    padding: const EdgeInsets.fromLTRB(20, 25, 20, 80),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const SectionTitle("Lịch trình của bạn"),
                        const SizedBox(height: 12),
                        UpcomingBookingCard(key: _bookingCardKey),

                        const SizedBox(height: 25),
                        const SectionTitle("Tiện ích nhanh"),
                        const SizedBox(height: 12),
                        const QuickActionGrid(),

                        Consumer<AuthService>(
                          builder: (context, authService, _) {
                            final aiEnabled =
                                authService
                                    .currentSetting
                                    ?.isAiRecommendEnabled ??
                                true;
                            if (!aiEnabled) return const SizedBox.shrink();
                            return Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const SizedBox(height: 25),
                                const SectionTitle("Gợi ý từ AI"),
                                const SizedBox(height: 12),
                                AICard(
                                  key: ValueKey(
                                    'ai-card-$_aiCardRefreshVersion',
                                  ),
                                ),
                              ],
                            );
                          },
                        ),

                        const SizedBox(height: 25),

                        // --- NEWS SECTION ---
                        if (_isLoading && _newsList.isEmpty)
                          const Center(child: CircularProgressIndicator())
                        else if (displayNews.isNotEmpty)
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              SectionTitle(
                                "Tin tức thư viện",
                                actionLabel: "Xem tất cả",
                                onTap: () {
                                  Navigator.push(
                                    context,
                                    MaterialPageRoute(
                                      builder: (context) => const NewsScreen(),
                                    ),
                                  );
                                },
                              ),
                              // Slider
                              NewsSlider(newsList: displayNews),
                              const SizedBox(height: 25),
                            ],
                          )
                        else
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const SectionTitle("Tin tức thư viện"),
                              const SizedBox(height: 12),
                              ErrorDisplayWidget.empty(
                                message: 'Chưa có tin tức nào',
                              ),
                              const SizedBox(height: 25),
                            ],
                          ),

                        // --- NEW BOOKS SECTION ---
                        if (_isLoading && _newBooksList.isEmpty)
                          const Center(child: CircularProgressIndicator())
                        else if (displayNewBooks.isNotEmpty)
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              SectionTitle(
                                "Sách mới thư viện",
                                actionLabel: "Xem tất cả",
                                onTap: () {
                                  Navigator.push(
                                    context,
                                    MaterialPageRoute(
                                      builder: (context) =>
                                          const NewBooksScreen(),
                                    ),
                                  );
                                },
                              ),
                              const SizedBox(height: 12),
                              NewBooksSlider(books: displayNewBooks),
                            ],
                          )
                        else
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const SectionTitle("Sách mới thư viện"),
                              const SizedBox(height: 12),
                              ErrorDisplayWidget.empty(
                                message: 'Chưa có sách mới nào',
                              ),
                            ],
                          ),
                        // --------------------
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          CompactHeader(user: widget.user, show: _showCompactHeader),
        ],
      ),
    );
  }
}
