import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:slib/models/news_model.dart';
import 'package:slib/views/news/news_detail_screen.dart';

class NewsSlider extends StatelessWidget {
  final List<News> newsList;

  const NewsSlider({super.key, required this.newsList});

  @override
  Widget build(BuildContext context) {
    if (newsList.isEmpty) return const SizedBox.shrink();

    return SizedBox(
      height: 180,
      child: ListView.separated(
        padding: const EdgeInsets.symmetric(vertical: 5),
        scrollDirection: Axis.horizontal,
        clipBehavior: Clip.none,
        itemCount: newsList.length,
        separatorBuilder: (context, index) => const SizedBox(width: 15),
        itemBuilder: (context, index) => _buildNewsItem(context, newsList[index]),
      ),
    );
  }

  Widget _buildNewsItem(BuildContext context, News item) {
    return GestureDetector(
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(builder: (context) => NewsDetailScreen(news: item)),
        );
      },
      child: Container(
        width: 280,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(20),
          boxShadow: [
            BoxShadow(color: Colors.black.withOpacity(0.1), blurRadius: 10, offset: const Offset(0, 5))
          ],
          border: Border.all(
            color: item.isPinned ? item.getTagColor() : Colors.transparent,
            width: item.isPinned ? 2 : 0,
          ),
        ),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(20),
          child: Stack(
            children: [
              // 1. Ảnh nền (Cached)
              Positioned.fill(
                child: CachedNetworkImage(
                  imageUrl: item.imageUrl,
                  fit: BoxFit.cover,
                  placeholder: (context, url) => Container(color: Colors.grey[200]),
                  errorWidget: (context, url, error) => Container(
                    color: item.getTagColor().withOpacity(0.15),
                    child: Icon(Icons.broken_image, color: item.getTagColor()),
                  ),
                ),
              ),
              // 2. Lớp phủ đen mờ (Gradient)
              Positioned.fill(
                child: Container(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topCenter,
                      end: Alignment.bottomCenter,
                      colors: [Colors.transparent, Colors.black.withOpacity(0.8)],
                    ),
                  ),
                ),
              ),
              // 3. Nội dung chữ
              Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            color: Colors.white.withOpacity(0.9),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Row(
                            children: [
                              Icon(Icons.flash_on, size: 12, color: item.getTagColor()),
                              const SizedBox(width: 4),
                              Text(item.categoryName, style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: item.getTagColor())),
                            ],
                          ),
                        ),
                        if (item.isPinned)
                          Container(
                            padding: const EdgeInsets.all(5),
                            decoration: const BoxDecoration(color: Colors.white, shape: BoxShape.circle),
                            child: const Icon(Icons.push_pin, size: 14, color: Colors.red),
                          )
                      ],
                    ),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          item.title,
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, height: 1.3, color: Colors.white),
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 8),
                        Row(
                          children: [
                            const Text("Xem chi tiết", style: TextStyle(fontSize: 12, color: Colors.white70)),
                            const SizedBox(width: 4),
                            Icon(Icons.arrow_right_alt, size: 16, color: item.getTagColor()),
                          ],
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}