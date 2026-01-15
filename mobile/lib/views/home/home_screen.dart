import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/user_profile.dart';
import 'package:slib/models/news_model.dart';
import 'package:slib/services/news_service.dart';
import 'package:slib/services/app/local_storage_service.dart';
import 'package:slib/views/home/widgets/home_appbar.dart';
import 'package:slib/views/home/widgets/live_status_dashboard.dart';
import 'package:slib/views/home/widgets/upcoming_booking_card.dart';
import 'package:slib/views/home/widgets/quick_action_grid.dart';
import 'package:slib/views/home/widgets/ai_suggestion_card.dart';
import 'package:slib/views/home/widgets/news_slider.dart';
import 'package:slib/views/home/widgets/compact_header.dart';
import 'package:slib/views/home/widgets/section_title.dart';
import 'package:slib/views/news/news_screen.dart';

class HomeScreen extends StatefulWidget {
  final UserProfile? user;

  const HomeScreen({super.key, this.user});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final ScrollController _scrollController = ScrollController();

  // --- STATE QUẢN LÝ TIN TỨC ---
  List<News> _newsList = [];
  bool _isLoading = true;

  final NewsService _newsService = NewsService();
  final LocalStorageService _localService = LocalStorageService();

  double _headerOpacity = 1.0;
  double _headerOffset = 0.0;
  bool _showCompactHeader = false;

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    _loadNewsData(); // Gọi hàm load 2 bước
  }

  // Chiến thuật: Cache trước -> API sau
  void _loadNewsData() async {
    // 1. Load Cache
    final cachedNews = await _localService.loadNewsList();
    if (cachedNews.isNotEmpty && mounted) {
      setState(() {
        _newsList = cachedNews;
        _isLoading = false;
      });
    }

    // 2. Load API
    try {
      final freshNews = await _newsService.fetchPublicNews();
      if (mounted) {
        setState(() {
          _newsList = freshNews;
          _isLoading = false;
        });
      }
      await _localService.saveNewsList(freshNews);
    } catch (e) {
      print("Lỗi tải tin mới: $e");
      if (mounted && _newsList.isEmpty) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _onRefresh() async {
    await Future.delayed(const Duration(milliseconds: 500));
    try {
      final freshNews = await _newsService.fetchPublicNews();
      if (mounted) setState(() => _newsList = freshNews);
      await _localService.saveNewsList(freshNews);
    } catch (e) {
      print("Refresh error: $e");
    }
  }

  @override
  void dispose() {
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
                            colors: [AppColors.brandColor, const Color(0xFFFF9052)],
                          ),
                          borderRadius: const BorderRadius.only(
                            bottomLeft: Radius.circular(30),
                            bottomRight: Radius.circular(30),
                          ),
                        ),
                        padding: EdgeInsets.fromLTRB(20, topPadding + 10, 20, 20),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            HomeAppBar(user: widget.user),
                            const SizedBox(height: 20),
                            LiveStatusDashboard(user: widget.user),
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
                        const UpcomingBookingCard(),

                        const SizedBox(height: 25),
                        const SectionTitle("Tiện ích nhanh"),
                        const SizedBox(height: 12),
                        const QuickActionGrid(),

                        const SizedBox(height: 25),
                        const SectionTitle("Gợi ý từ AI"),
                        const SizedBox(height: 12),
                        const AICard(),

                        const SizedBox(height: 25),

                        // --- NEWS SECTION ---
                        if (_isLoading && _newsList.isEmpty)
                           const Center(child: CircularProgressIndicator())
                        else if (displayNews.isNotEmpty)
                          Column(
                            children: [
                              Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: [
                                  const SectionTitle("Tin tức thư viện"),
                                  TextButton(
                                    onPressed: () {
                                      Navigator.push(
                                        context,
                                        MaterialPageRoute(
                                          builder: (context) => const NewsScreen(),
                                        ),
                                      );
                                    },
                                    child: const Text(
                                      "Xem tất cả",
                                      style: TextStyle(color: Colors.orange),
                                    ),
                                  ),
                                ],
                              ),
                              // Slider
                              NewsSlider(newsList: displayNews),
                            ],
                          )
                        else
                          const SizedBox.shrink(),
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