import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/user_profile.dart';
import 'package:slib/models/news_model.dart'; // Import Model
import 'package:slib/services/news_service.dart'; // Import Service
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

  // Tạo biến Future để chứa dữ liệu tin tức
  late Future<List<News>> _newsFuture;

  double _headerOpacity = 1.0;
  double _headerOffset = 0.0;
  bool _showCompactHeader = false;

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    // Gọi API lấy tin tức ngay khi vào màn hình
    _newsFuture = NewsService().fetchPublicNews();
  }

  // Hàm làm mới dữ liệu khi kéo xuống (Pull to Refresh)
  Future<void> _onRefresh() async {
    setState(() {
      _newsFuture = NewsService().fetchPublicNews();
      // Ở đây bạn có thể gọi thêm hàm refresh Dashboard nếu cần
    });
    // Giả lập delay 1 xíu cho mượt
    await Future.delayed(const Duration(milliseconds: 500));
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

    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA),
      body: Stack(
        children: [
          // 2. NỘI DUNG CHÍNH (Bọc trong RefreshIndicator để kéo làm mới)
          RefreshIndicator(
            onRefresh: _onRefresh,
            color: AppColors.brandColor,
            child: SingleChildScrollView(
              controller: _scrollController,
              padding: EdgeInsets.zero,
              physics:
                  const AlwaysScrollableScrollPhysics(), // Để kéo được ngay cả khi nội dung ngắn
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // PHẦN HEADER CAM
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
                            LiveStatusDashboard(user: widget.user),
                          ],
                        ),
                      ),
                    ),
                  ),

                  // NỘI DUNG PHÍA DƯỚI
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
                        // --- BẮT ĐẦU ĐOẠN SỬA ---
                        FutureBuilder<List<News>>(
                          future: _newsFuture,
                          builder: (context, snapshot) {
                            // Nếu chưa có dữ liệu hoặc lỗi, ẩn đi cho gọn
                            if (!snapshot.hasData || snapshot.data!.isEmpty) {
                              return const SizedBox.shrink();
                            }

                            final allNews = snapshot.data!;

                            // --- LOGIC GHIM (PIN) ---
                            // 1. Lọc ra danh sách các bài được ghim
                            List<News> pinnedNews = allNews
                                .where((item) => item.isPinned)
                                .toList();

                            // 2. Nếu KHÔNG có bài nào được ghim -> Lấy 5 bài mới nhất
                            if (pinnedNews.isEmpty) {
                              pinnedNews = allNews.take(5).toList();
                            }
                            // 3. Nếu ít bài ghim quá, bù thêm bài mới nhất cho đủ 5
                            else if (pinnedNews.length < 5) {
                              var remaining = allNews
                                  .where((item) => !item.isPinned)
                                  .toList();
                              pinnedNews.addAll(
                                remaining.take(5 - pinnedNews.length),
                              );
                            }

                            return Column(
                              children: [
                                // PHẦN BỊ THIẾU ĐÃ ĐƯỢC THÊM LẠI Ở ĐÂY 👇
                                Row(
                                  mainAxisAlignment:
                                      MainAxisAlignment.spaceBetween,
                                  children: [
                                    const SectionTitle("Tin tức thư viện"),
                                    TextButton(
                                      onPressed: () {
                                        Navigator.push(
                                          context,
                                          MaterialPageRoute(
                                            builder: (context) =>
                                                const NewsScreen(),
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
                                // ---------------------------------------

                                // Truyền danh sách ĐÃ LỌC vào Slider
                                NewsSlider(newsList: pinnedNews),
                              ],
                            );
                          },
                        ),
                        // --- KẾT THÚC ĐOẠN SỬA ---
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
