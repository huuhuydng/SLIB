import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';
import 'news_detail_screen.dart'; // Đừng quên import màn hình chi tiết
import 'news_item.dart'; // Import model dữ liệu tin tức


class NewsScreen extends StatefulWidget {
  const NewsScreen({super.key});

  @override
  State<NewsScreen> createState() => _NewsScreenState();
}

class _NewsScreenState extends State<NewsScreen> {
  // Danh mục đang chọn (Mặc định là 'Tất cả')
  String _selectedCategory = "Tất cả";

  // Danh sách các bộ lọc
  final List<String> _categories = ["Tất cả", "Quan trọng", "Sự kiện", "Sách mới", "Ưu đãi"];

  // Dữ liệu giả lập (Mock Data)
  final List<NewsItem> _allNews = [
    NewsItem(
      id: "1",
      title: "FPT Techday 2025: Công nghệ kiến tạo tương lai",
      summary: "Tham gia ngay sự kiện công nghệ lớn nhất năm để trải nghiệm các giải pháp AI và nhận quà khủng.",
      date: "12/12/2025",
      imageUrl: "https://images.unsplash.com/photo-1540575467063-178a50c2df87?q=80&w=600&auto=format&fit=crop",
      category: "Sự kiện",
      tagColor: Colors.blue,
    ),
    NewsItem(
      id: "2",
      title: "Thông báo bảo trì hệ thống điện Tầng 3",
      summary: "Thư viện sẽ ngắt điện khu vực Tầng 3 từ 08:00 - 12:00 ngày 10/12 để bảo trì định kỳ.",
      date: "10/12/2025",
      imageUrl: "https://images.unsplash.com/photo-1621905251189-fc015e70482e?q=80&w=600&auto=format&fit=crop",
      category: "Quan trọng",
      tagColor: Colors.red,
    ),
    NewsItem(
      id: "3",
      title: "Top 100 đầu sách AI & Data Science mới về",
      summary: "Cập nhật ngay kho tri thức mới nhất về Trí tuệ nhân tạo phục vụ đồ án tốt nghiệp.",
      date: "08/12/2025",
      imageUrl: "https://images.unsplash.com/photo-1532012197267-da84d127e765?q=80&w=600&auto=format&fit=crop",
      category: "Sách mới",
      tagColor: Colors.green,
    ),
    NewsItem(
      id: "4",
      title: "Workshop: Kỹ năng tra cứu tài liệu hiệu quả",
      summary: "Hướng dẫn sinh viên cách sử dụng CSDL quốc tế IEEE, ProQuest để làm nghiên cứu.",
      date: "05/12/2025",
      imageUrl: "https://images.unsplash.com/photo-1524178232363-1fb2b075b655?q=80&w=600&auto=format&fit=crop",
      category: "Sự kiện",
      tagColor: Colors.blue,
    ),
    NewsItem(
      id: "5",
      title: "Tặng thêm 10 điểm uy tín cho Top Check-in tuần",
      summary: "Chương trình thi đua check-in thư viện nhận điểm thưởng đổi quà.",
      date: "01/12/2025",
      imageUrl: "https://images.unsplash.com/photo-1555421689-491a97ff2040?q=80&w=600&auto=format&fit=crop",
      category: "Ưu đãi",
      tagColor: Colors.orange,
    ),
  ];

  @override
  Widget build(BuildContext context) {
    // Logic lọc tin tức
    List<NewsItem> filteredNews = _selectedCategory == "Tất cả"
        ? _allNews
        : _allNews.where((item) => item.category == _selectedCategory).toList();

    return Scaffold(
      backgroundColor: AppColors.backgroundPrimary,
      appBar: AppBar(
        title: const Text("Tin tức & Sự kiện", style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        centerTitle: true,
        surfaceTintColor: Colors.transparent,
        actions: [
          IconButton(icon: const Icon(Icons.search), onPressed: () {}),
        ],
      ),
      body: Column(
        children: [
          // 1. Bộ lọc danh mục
          Container(
            color: Colors.white,
            padding: const EdgeInsets.symmetric(vertical: 12),
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Row(
                children: _categories.map((category) {
                  bool isSelected = _selectedCategory == category;
                  return Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: ChoiceChip(
                      label: Text(category),
                      selected: isSelected,
                      onSelected: (bool selected) {
                        setState(() {
                          _selectedCategory = category;
                        });
                      },
                      selectedColor: AppColors.brandColor,
                      labelStyle: TextStyle(
                        color: isSelected ? Colors.white : Colors.black87,
                        fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                      ),
                      backgroundColor: Colors.grey.shade100,
                      side: BorderSide.none,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                    ),
                  );
                }).toList(),
              ),
            ),
          ),

          // 2. Danh sách tin tức
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: filteredNews.length,
              itemBuilder: (context, index) {
                // Truyền context vào hàm build card
                return _buildBigNewsCard(context, filteredNews[index]);
              },
            ),
          ),
        ],
      ),
    );
  }

  // Widget: Card tin tức dạng lớn (Đã cập nhật Navigation & Hero)
  Widget _buildBigNewsCard(BuildContext context, NewsItem item) {
    return GestureDetector(
      onTap: () {
        // Điều hướng sang màn hình chi tiết
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => NewsDetailScreen(news: item),
          ),
        );
      },
      child: Container(
        margin: const EdgeInsets.only(bottom: 20),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.05),
              blurRadius: 10,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 1. Ảnh bìa (Cover Image) với Hero Animation
            Stack(
              children: [
                ClipRRect(
                  borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
                  child: Hero(
                    tag: item.id, // Tag này giúp Flutter biết ảnh nào bay đi đâu
                    child: Image.network(
                      item.imageUrl,
                      height: 180,
                      width: double.infinity,
                      fit: BoxFit.cover,
                      errorBuilder: (context, error, stackTrace) => Container(
                        height: 180,
                        color: Colors.grey[200],
                        child: const Icon(Icons.image, size: 50, color: Colors.grey),
                      ),
                    ),
                  ),
                ),
                // Badge Category
                Positioned(
                  top: 12,
                  left: 12,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                    decoration: BoxDecoration(
                      color: item.tagColor,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      item.category.toUpperCase(),
                      style: const TextStyle(
                        color: Colors.white, 
                        fontSize: 10, 
                        fontWeight: FontWeight.bold
                      ),
                    ),
                  ),
                )
              ],
            ),

            // 2. Nội dung Text
            Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Ngày đăng
                  Row(
                    children: [
                      const Icon(Icons.calendar_today, size: 14, color: AppColors.textGrey),
                      const SizedBox(width: 6),
                      Text(
                        item.date,
                        style: const TextStyle(fontSize: 12, color: AppColors.textGrey),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  // Tiêu đề
                  Text(
                    item.title,
                    style: const TextStyle(
                      fontSize: 18, 
                      fontWeight: FontWeight.bold,
                      height: 1.3
                    ),
                  ),
                  const SizedBox(height: 8),
                  // Tóm tắt ngắn
                  Text(
                    item.summary,
                    style: const TextStyle(fontSize: 14, color: AppColors.textGrey, height: 1.5),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 16),
                  // Nút "Xem chi tiết"
                  Row(
                    children: [
                      Text(
                        "Xem chi tiết",
                        style: TextStyle(
                          color: AppColors.brandColor,
                          fontWeight: FontWeight.bold,
                          fontSize: 14
                        ),
                      ),
                      const SizedBox(width: 4),
                      const Icon(Icons.arrow_forward_rounded, size: 16, color: AppColors.brandColor)
                    ],
                  )
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

