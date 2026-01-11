import 'package:flutter/material.dart';
import 'package:intl/intl.dart'; // Import để format ngày
import 'package:slib/assets/colors.dart';
import 'package:slib/models/news_model.dart'; // Import model mới
import 'package:slib/services/news_service.dart'; // Import service
import 'news_detail_screen.dart';

class NewsScreen extends StatefulWidget {
  const NewsScreen({super.key});

  @override
  State<NewsScreen> createState() => _NewsScreenState();
}

class _NewsScreenState extends State<NewsScreen> {
  late Future<List<News>> _newsFuture;
  String _selectedCategory = "Tất cả";
  final List<String> _categories = ["Tất cả", "Quan trọng", "Sự kiện", "Sách mới"];

  @override
  void initState() {
    super.initState();
    _newsFuture = NewsService().fetchPublicNews(); // Gọi API khi màn hình khởi tạo
  }

  // Hàm refresh khi kéo xuống
  Future<void> _refreshNews() async {
    setState(() {
      _newsFuture = NewsService().fetchPublicNews();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey[50], // AppColors.backgroundPrimary
      appBar: AppBar(
        title: const Text("Tin tức & Sự kiện", style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        centerTitle: true,
        elevation: 0,
      ),
      body: Column(
        children: [
          // 1. Bộ lọc (Giữ nguyên UI của bạn)
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
                          // Nếu muốn lọc Server-side thì gọi API khác ở đây
                        });
                      },
                      selectedColor: Colors.orange, // AppColors.brandColor
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

          // 2. Danh sách tin tức (Dùng FutureBuilder)
          Expanded(
            child: RefreshIndicator(
              onRefresh: _refreshNews,
              child: FutureBuilder<List<News>>(
                future: _newsFuture,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator());
                  } else if (snapshot.hasError) {
                    return Center(child: Text("Lỗi: ${snapshot.error}"));
                  } else if (!snapshot.hasData || snapshot.data!.isEmpty) {
                    return const Center(child: Text("Chưa có tin tức nào"));
                  }

                  // Lọc Client-side (nếu cần)
                  List<News> newsList = snapshot.data!;
                  if (_selectedCategory != "Tất cả") {
                    newsList = newsList.where((item) => item.categoryName == _selectedCategory).toList();
                  }

                  return ListView.builder(
                    padding: const EdgeInsets.all(16),
                    itemCount: newsList.length,
                    itemBuilder: (context, index) {
                      return _buildBigNewsCard(context, newsList[index]);
                    },
                  );
                },
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBigNewsCard(BuildContext context, News item) {
    // Format ngày: 2025-12-12T10:00:00 -> 12/12/2025
    String formattedDate = "";
    try {
      DateTime dt = DateTime.parse(item.publishedAt);
      formattedDate = DateFormat('dd/MM/yyyy').format(dt);
    } catch (e) {
      formattedDate = item.publishedAt;
    }

    return GestureDetector(
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(builder: (context) => NewsDetailScreen(news: item)),
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
                offset: const Offset(0, 4)),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Ảnh bìa
            Stack(
              children: [
                ClipRRect(
                  borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
                  child: Hero(
                    tag: "news_${item.id}", // Tag unique
                    child: Image.network(
                      item.imageUrl,
                      height: 180,
                      width: double.infinity,
                      fit: BoxFit.cover,
                      errorBuilder: (context, error, stackTrace) => Container(
                        height: 180, color: Colors.grey[200], child: const Icon(Icons.broken_image),
                      ),
                    ),
                  ),
                ),
                Positioned(
                  top: 12, left: 12,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                    decoration: BoxDecoration(color: item.getTagColor(), borderRadius: BorderRadius.circular(8)),
                    child: Text(item.categoryName.toUpperCase(), style: const TextStyle(color: Colors.white, fontSize: 10, fontWeight: FontWeight.bold)),
                  ),
                )
              ],
            ),
            // Nội dung
            Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(children: [
                    const Icon(Icons.calendar_today, size: 14, color: Colors.grey),
                    const SizedBox(width: 6),
                    Text(formattedDate, style: const TextStyle(fontSize: 12, color: Colors.grey)),
                    const Spacer(),
                    const Icon(Icons.remove_red_eye, size: 14, color: Colors.grey),
                    const SizedBox(width: 4),
                    Text("${item.viewCount}", style: const TextStyle(fontSize: 12, color: Colors.grey)),
                  ]),
                  const SizedBox(height: 8),
                  Text(item.title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, height: 1.3)),
                  const SizedBox(height: 8),
                  Text(item.summary, style: const TextStyle(fontSize: 14, color: Colors.grey, height: 1.5), maxLines: 2, overflow: TextOverflow.ellipsis),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}