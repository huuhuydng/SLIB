import 'package:cached_network_image/cached_network_image.dart'; // Import Cache Ảnh
import 'package:flutter/material.dart';
import 'package:flutter_widget_from_html/flutter_widget_from_html.dart';
import 'package:intl/intl.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/models/news_model.dart';
import 'package:slib/services/news/news_service.dart';
import 'package:url_launcher/url_launcher.dart';

class NewsDetailScreen extends StatefulWidget {
  final News news;

  const NewsDetailScreen({super.key, required this.news});

  @override
  State<NewsDetailScreen> createState() => _NewsDetailScreenState();
}

class _NewsDetailScreenState extends State<NewsDetailScreen> {
  late News _news;

  @override
  void initState() {
    super.initState();
    _news = widget.news;
    // Vẫn gọi API để tính View Count phía Server (Fire & Forget)
    // Nhưng không cần chờ (await) nó, để UI hiện ngay lập tức
    _updateViewCount();
  }

  Future<void> _updateViewCount() async {
    try {
      final detail = await NewsService().fetchNewsDetail(widget.news.id);
      if (mounted) {
        setState(() => _news = detail);
      }
    } catch (e) {
      // Mất mạng thì thôi, không tăng view, không báo lỗi làm phiền user
      debugPrint("Offline: Không thể update view count");
    }
  }

  String _formatFileSize(int? bytes) {
    if (bytes == null || bytes <= 0) return 'PDF';
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) {
      return '${(bytes / 1024).toStringAsFixed(1)} KB';
    }
    return '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB';
  }

  Future<void> _openPdf() async {
    final uri = _buildPdfViewerUri();
    if (uri == null) return;

    final opened = await launchUrl(uri, mode: LaunchMode.externalApplication);
    if (!opened && mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Không thể mở file PDF')));
    }
  }

  Uri? _buildPdfViewerUri() {
    final pdfUrl = _news.pdfUrl;
    if (pdfUrl == null || pdfUrl.isEmpty) return null;

    final fileName = (_news.pdfFileName?.trim().isNotEmpty ?? false)
        ? _news.pdfFileName!.trim()
        : 'slib-news.pdf';

    return Uri.parse(
      '${ApiConstants.domain}/slib/files/proxy-pdf',
    ).replace(queryParameters: {'url': pdfUrl, 'fileName': fileName});
  }

  @override
  Widget build(BuildContext context) {
    // Format ngày
    String formattedDate = _news.publishedAt;
    try {
      formattedDate = DateFormat(
        'dd/MM/yyyy HH:mm',
      ).format(DateTime.parse(_news.publishedAt));
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
                tag: "news_list_${_news.id}",
                child: CachedNetworkImage(
                  imageUrl: _news.imageUrl,
                  fit: BoxFit.cover,
                  color: Colors.black26,
                  colorBlendMode: BlendMode.darken,
                  placeholder: (context, url) =>
                      Container(color: Colors.grey[200]),
                  errorWidget: (context, url, error) => Container(
                    color: Colors.grey[200],
                    child: const Icon(
                      Icons.broken_image,
                      size: 50,
                      color: Colors.grey,
                    ),
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
                    padding: const EdgeInsets.symmetric(
                      horizontal: 10,
                      vertical: 6,
                    ),
                    decoration: BoxDecoration(
                      color: _news.getTagColor().withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      _news.categoryName.toUpperCase(),
                      style: TextStyle(
                        color: _news.getTagColor(),
                        fontWeight: FontWeight.bold,
                        fontSize: 12,
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Tiêu đề
                  Text(
                    _news.title,
                    style: const TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.w800,
                      height: 1.3,
                      color: Colors.black87,
                    ),
                  ),
                  const SizedBox(height: 12),

                  // Metadata (Ngày + Tác giả)
                  Row(
                    children: [
                      const CircleAvatar(
                        radius: 14,
                        backgroundColor: Colors.orange,
                        child: Icon(
                          Icons.person,
                          size: 16,
                          color: Colors.white,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            "SLIB Admin",
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 12,
                            ),
                          ),
                          Text(
                            formattedDate,
                            style: const TextStyle(
                              color: Colors.grey,
                              fontSize: 11,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),

                  const Padding(
                    padding: EdgeInsets.symmetric(vertical: 20),
                    child: Divider(height: 1),
                  ),

                  // Tóm tắt (In đậm)
                  Text(
                    _news.summary,
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                      height: 1.6,
                      color: Colors.black87,
                    ),
                  ),
                  const SizedBox(height: 16),

                  // --- NỘI DUNG HTML ---
                  // Widget này tự động render HTML thành giao diện Flutter
                  HtmlWidget(
                    _news.content,
                    textStyle: const TextStyle(
                      fontSize: 16,
                      height: 1.6,
                      color: Color(0xFF4A5568),
                    ),

                    // Tùy chỉnh ảnh trong bài viết
                    // Nếu muốn ảnh trong bài viết cũng Cache được thì cần cấu hình thêm
                    // Nhưng mặc định HtmlWidget đã xử lý khá tốt rồi.
                    onTapImage: (ImageMetadata meta) {
                      // Có thể mở ảnh to xem chi tiết
                      debugPrint("Xem ảnh: ${meta.sources.first.url}");
                    },
                  ),

                  if (_news.pdfUrl != null && _news.pdfUrl!.isNotEmpty) ...[
                    const SizedBox(height: 24),
                    Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: const Color(0xFFFFF7ED),
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: const Color(0xFFFED7AA)),
                      ),
                      child: Column(
                        children: [
                          Row(
                            children: [
                              Container(
                                width: 48,
                                height: 48,
                                decoration: BoxDecoration(
                                  color: const Color(0xFFFFEDD5),
                                  borderRadius: BorderRadius.circular(14),
                                ),
                                child: const Icon(
                                  Icons.picture_as_pdf,
                                  color: Color(0xFFC2410C),
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    const Text(
                                      'File PDF đính kèm',
                                      style: TextStyle(
                                        fontSize: 12,
                                        color: Color(0xFF64748B),
                                        fontWeight: FontWeight.w700,
                                      ),
                                    ),
                                    const SizedBox(height: 4),
                                    Text(
                                      _news.pdfFileName ??
                                          'Tài liệu đính kèm.pdf',
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                      style: const TextStyle(
                                        color: Color(0xFF9A3412),
                                        fontWeight: FontWeight.w800,
                                      ),
                                    ),
                                    const SizedBox(height: 2),
                                    Text(
                                      _formatFileSize(_news.pdfFileSize),
                                      style: const TextStyle(
                                        fontSize: 12,
                                        color: Color(0xFF64748B),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 14),
                          SizedBox(
                            width: double.infinity,
                            child: ElevatedButton.icon(
                              onPressed: _openPdf,
                              icon: const Icon(Icons.open_in_new, size: 18),
                              label: const Text('Xem PDF'),
                              style: ElevatedButton.styleFrom(
                                backgroundColor: const Color(0xFFC2410C),
                                foregroundColor: Colors.white,
                                elevation: 0,
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(12),
                                ),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],

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
