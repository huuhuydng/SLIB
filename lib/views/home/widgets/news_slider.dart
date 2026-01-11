import 'package:flutter/material.dart';
import 'package:slib/models/news_model.dart';
import 'package:slib/views/news/news_detail_screen.dart';

class NewsSlider extends StatelessWidget {
  final List<News> newsList;

  const NewsSlider({super.key, required this.newsList});

  @override
  Widget build(BuildContext context) {
    if (newsList.isEmpty) {
      return const SizedBox.shrink();
    }

    return SizedBox(
      height: 170, 
      child: ListView.separated(
        padding: const EdgeInsets.symmetric(horizontal: 0, vertical: 5), 
        scrollDirection: Axis.horizontal,
        clipBehavior: Clip.none, // Cho phép shadow tràn ra ngoài
        itemCount: newsList.length,
        separatorBuilder: (context, index) => const SizedBox(width: 15),
        itemBuilder: (context, index) {
          final newsItem = newsList[index];
          return _buildNewsItem(context, newsItem);
        },
      ),
    );
  }

  Widget _buildNewsItem(BuildContext context, News item) {
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
        width: 270,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          // Màu nền nhạt theo category
          color: item.getTagColor().withOpacity(0.15),
          borderRadius: BorderRadius.circular(20),
          
          // --- LOGIC VIỀN GHIM (PIN) ---
          // Nếu ghim: Viền màu đậm, dày 2px
          // Không ghim: Viền rất mờ hoặc không viền
          border: Border.all(
            color: item.isPinned 
                ? item.getTagColor() // Màu đậm nếu ghim
                : Colors.transparent, 
            width: item.isPinned ? 2 : 0,
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.spaceBetween, // Căn đều trên dưới
          children: [
            // 1. HEADER: Badge Category + Icon Ghim
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Badge Category
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(8),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.05),
                        blurRadius: 4,
                        offset: const Offset(0, 2),
                      )
                    ],
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.flash_on, size: 12, color: item.getTagColor()),
                      const SizedBox(width: 4),
                      Text(
                        item.categoryName,
                        style: TextStyle(
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                          color: item.getTagColor(),
                        ),
                      ),
                    ],
                  ),
                ),

                // --- ICON GHIM (Chỉ hiện nếu isPinned = true) ---
                if (item.isPinned)
                  Container(
                    padding: const EdgeInsets.all(5),
                    decoration: const BoxDecoration(
                      color: Colors.white,
                      shape: BoxShape.circle,
                    ),
                    child: Transform.rotate(
                      angle: 0.5, // Nghiêng cái ghim 1 chút cho nghệ
                      child: const Icon(Icons.push_pin, size: 14, color: Colors.red),
                    ),
                  )
              ],
            ),

            // 2. Tiêu đề bài viết
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8.0),
              child: Text(
                item.title,
                style: const TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 16,
                  height: 1.3,
                  color: Colors.black87,
                ),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ),

            // 3. Footer: "Xem chi tiết"
            Row(
              children: [
                Text(
                  "Xem chi tiết",
                  style: TextStyle(
                    fontSize: 12, 
                    color: item.getTagColor().withOpacity(0.8),
                    fontWeight: FontWeight.w600
                  ),
                ),
                const SizedBox(width: 4),
                Icon(
                  Icons.arrow_right_alt, 
                  size: 16, 
                  color: item.getTagColor().withOpacity(0.8)
                )
              ],
            ),
          ],
        ),
      ),
    );
  }
}