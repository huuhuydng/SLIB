import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/views/others/news_screen.dart';


class NewsList extends StatelessWidget {
  const NewsList({super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // 1. Header của mục (Tiêu đề + Nút Xem tất cả)
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              "Tin tức & Sự kiện",
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
            ),
            TextButton(
              onPressed: () {
                // Chuyển sang màn hình Tin tức đầy đủ
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const NewsScreen()),
                );
              },
              child: const Text(
                "Xem tất cả",
                style: TextStyle(color: AppColors.brandColor),
              ),
            )
          ],
        ),
        
        const SizedBox(height: 8),

        // 2. Danh sách các tin tức rút gọn (Preview)
        _buildNewsItem(
          title: "FPT Techday 2025: Công nghệ tương lai",
          date: "12/12/2025",
          tag: "Sự kiện",
          tagColor: Colors.blue,
          icon: Icons.event_note,
          bgColor: Colors.blue.shade50,
        ),
        _buildNewsItem(
          title: "Thông báo bảo trì khu vực Tầng 3",
          date: "10/12/2025",
          tag: "Quan trọng",
          tagColor: AppColors.error,
          icon: Icons.build_circle_outlined,
          bgColor: Colors.red.shade50,
        ),
        _buildNewsItem(
          title: "Top 100 đầu sách AI mới về thư viện",
          date: "08/12/2025",
          tag: "Sách mới",
          tagColor: AppColors.success,
          icon: Icons.menu_book_rounded,
          bgColor: Colors.green.shade50,
        ),
      ],
    );
  }

  // Widget con để vẽ từng thẻ tin tức nhỏ
  Widget _buildNewsItem({
    required String title,
    required String date,
    required String tag,
    required Color tagColor,
    required IconData icon,
    required Color bgColor,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.grey.withOpacity(0.1),
            spreadRadius: 1,
            blurRadius: 6,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Row(
          children: [
            // Icon ảnh đại diện (Placeholder)
            Container(
              width: 80,
              height: 80,
              decoration: BoxDecoration(
                color: bgColor,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(icon, color: tagColor.withOpacity(0.8), size: 32),
            ),
            const SizedBox(width: 16),
            
            // Nội dung chữ
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Tag loại tin
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: tagColor.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      tag.toUpperCase(),
                      style: TextStyle(
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                        color: tagColor,
                      ),
                    ),
                  ),
                  const SizedBox(height: 8),
                  
                  // Tiêu đề
                  Text(
                    title,
                    style: const TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 15,
                      height: 1.3,
                      color: AppColors.textPrimary,
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 8),
                  
                  // Ngày tháng
                  Row(
                    children: [
                      const Icon(Icons.access_time, size: 14, color: Colors.grey),
                      const SizedBox(width: 4),
                      Text(
                        date,
                        style: const TextStyle(fontSize: 12, color: Colors.grey),
                      ),
                    ],
                  ),
                ],
              ),
            )
          ],
        ),
      ),
    );
  }
}