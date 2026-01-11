import 'package:flutter/material.dart';
import 'package:flutter_widget_from_html/flutter_widget_from_html.dart'; // THƯ VIỆN QUAN TRỌNG
import 'package:intl/intl.dart';
import 'package:slib/models/news_model.dart';
import 'package:slib/services/news_service.dart';

class NewsDetailScreen extends StatefulWidget {
  final News news;

  const NewsDetailScreen({super.key, required this.news});

  @override
  State<NewsDetailScreen> createState() => _NewsDetailScreenState();
}

class _NewsDetailScreenState extends State<NewsDetailScreen> {
  
  @override
  void initState() {
    super.initState();
    // Gọi API để tăng View (Gọi ngầm không cần chờ kết quả)
    NewsService().fetchNewsDetail(widget.news.id);
  }

  @override
  Widget build(BuildContext context) {
    // Format ngày
    String formattedDate = widget.news.publishedAt;
    try {
      formattedDate = DateFormat('dd/MM/yyyy HH:mm').format(DateTime.parse(widget.news.publishedAt));
    } catch (_) {}

    return Scaffold(
      backgroundColor: Colors.white,
      body: CustomScrollView(
        slivers: [
          // AppBar với Ảnh bìa
          SliverAppBar(
            expandedHeight: 280,
            pinned: true,
            flexibleSpace: FlexibleSpaceBar(
              background: Hero(
                tag: "news_${widget.news.id}",
                child: Image.network(
                  widget.news.imageUrl,
                  fit: BoxFit.cover,
                  color: Colors.black26,
                  colorBlendMode: BlendMode.darken,
                ),
              ),
            ),
          ),

          // Nội dung bài viết
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Category Badge
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                    decoration: BoxDecoration(
                      color: widget.news.getTagColor().withOpacity(0.1),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      widget.news.categoryName.toUpperCase(),
                      style: TextStyle(color: widget.news.getTagColor(), fontWeight: FontWeight.bold, fontSize: 12),
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Tiêu đề
                  Text(
                    widget.news.title,
                    style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w800, height: 1.3, color: Colors.black87),
                  ),
                  const SizedBox(height: 12),

                  // Metadata (Ngày + View)
                  Row(
                    children: [
                      const CircleAvatar(radius: 14, backgroundColor: Colors.orange, child: Icon(Icons.person, size: 16, color: Colors.white)),
                      const SizedBox(width: 8),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text("SLIB Admin", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
                          Text(formattedDate, style: const TextStyle(color: Colors.grey, fontSize: 11)),
                        ],
                      )
                    ],
                  ),

                  const Padding(padding: EdgeInsets.symmetric(vertical: 20), child: Divider(height: 1)),

                  // Tóm tắt (In đậm)
                  Text(
                    widget.news.summary,
                    style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, height: 1.6, color: Colors.black87),
                  ),
                  const SizedBox(height: 16),

                  // --- WIDGET HIỂN THỊ HTML & ẢNH TỪ BACKEND ---
                  HtmlWidget(
                    widget.news.content,
                    textStyle: const TextStyle(fontSize: 16, height: 1.6, color: Color(0xFF4A5568)),
                    // Tùy chỉnh loading ảnh
                    onLoadingBuilder: (context, element, loadingProgress) => const Center(child: CircularProgressIndicator()),
                    // Khi user bấm vào ảnh trong bài viết
                    onTapImage: (ImageMetadata meta) {
                       print("User tapped image: ${meta.sources.first.url}");
                    },
                  ),
                  // ---------------------------------------------
                  
                  const SizedBox(height: 40),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}