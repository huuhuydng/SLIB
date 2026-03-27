import 'package:cached_network_image/cached_network_image.dart'; // Import Cache Ảnh
import 'package:flutter/material.dart';
import 'package:flutter_widget_from_html/flutter_widget_from_html.dart';
import 'package:intl/intl.dart';
import 'package:slib/models/news_model.dart';
import 'package:slib/services/news/news_service.dart';

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
    // Vẫn gọi API để tính View Count phía Server (Fire & Forget)
    // Nhưng không cần chờ (await) nó, để UI hiện ngay lập tức
    _updateViewCount();
  }

  Future<void> _updateViewCount() async {
    try {
      await NewsService().fetchNewsDetail(widget.news.id);
    } catch (e) {
      // Mất mạng thì thôi, không tăng view, không báo lỗi làm phiền user
      print("Offline: Không thể update view count");
    }
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
          // 1. APPBAR VỚI ẢNH CACHE
          SliverAppBar(
            expandedHeight: 280,
            pinned: true,
            flexibleSpace: FlexibleSpaceBar(
              background: Hero(
                // Lưu ý: Tag này phải trùng với Tag ở màn hình trước (NewsScreen hoặc HomeScreen)
                // Ở NewsScreen mình đặt là "news_list_${id}", bạn nên check lại cho khớp
                tag: "news_list_${widget.news.id}", 
                child: CachedNetworkImage(
                  imageUrl: widget.news.imageUrl,
                  fit: BoxFit.cover,
                  color: Colors.black26,
                  colorBlendMode: BlendMode.darken,
                  placeholder: (context, url) => Container(color: Colors.grey[200]),
                  errorWidget: (context, url, error) => Container(
                    color: Colors.grey[200],
                    child: const Icon(Icons.broken_image, size: 50, color: Colors.grey),
                  ),
                ),
              ),
            ),
            // Nút back tròn trắng cho dễ nhìn trên nền ảnh
            leading: IconButton(
              icon: const CircleAvatar(
                backgroundColor: Colors.white,
                child: Icon(Icons.arrow_back, color: Colors.black),
              ),
              onPressed: () => Navigator.pop(context),
            ),
          ),

          // 2. NỘI DUNG BÀI VIẾT
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

                  // Metadata (Ngày + Tác giả)
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

                  // --- NỘI DUNG HTML ---
                  // Widget này tự động render HTML thành giao diện Flutter
                  HtmlWidget(
                    widget.news.content,
                    textStyle: const TextStyle(fontSize: 16, height: 1.6, color: Color(0xFF4A5568)),
                    
                    // Tùy chỉnh ảnh trong bài viết
                    // Nếu muốn ảnh trong bài viết cũng Cache được thì cần cấu hình thêm
                    // Nhưng mặc định HtmlWidget đã xử lý khá tốt rồi.
                    onTapImage: (ImageMetadata meta) {
                       // Có thể mở ảnh to xem chi tiết
                       print("Xem ảnh: ${meta.sources.first.url}");
                    },
                  ),
                  // ---------------------
                  
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