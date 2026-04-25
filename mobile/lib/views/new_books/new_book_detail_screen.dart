import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/new_book_model.dart';
import 'package:slib/services/new_books/new_book_service.dart';
import 'package:url_launcher/url_launcher.dart';

class NewBookDetailScreen extends StatefulWidget {
  final NewBook book;

  const NewBookDetailScreen({super.key, required this.book});

  @override
  State<NewBookDetailScreen> createState() => _NewBookDetailScreenState();
}

class _NewBookDetailScreenState extends State<NewBookDetailScreen> {
  late NewBook _book;
  bool _isRefreshing = false;

  @override
  void initState() {
    super.initState();
    _book = widget.book;
    _refreshDetail();
  }

  Future<void> _refreshDetail() async {
    try {
      setState(() => _isRefreshing = true);
      final detail = await NewBookService().fetchNewBookDetail(widget.book.id);
      if (!mounted) return;
      setState(() => _book = detail);
    } catch (_) {
    } finally {
      if (mounted) {
        setState(() => _isRefreshing = false);
      }
    }
  }

  Future<void> _openSourceUrl() async {
    if (_book.sourceUrl.isEmpty) return;
    final uri = Uri.tryParse(_book.sourceUrl);
    if (uri == null) return;

    if (!await launchUrl(uri, mode: LaunchMode.externalApplication)) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Không thể mở link nguồn lúc này')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final accentColor = _book.getAccentColor();

    return Scaffold(
      backgroundColor: const Color(0xFFF7F8FC),
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            expandedHeight: 340,
            pinned: true,
            backgroundColor: Colors.white,
            surfaceTintColor: Colors.white,
            flexibleSpace: FlexibleSpaceBar(
              background: Stack(
                fit: StackFit.expand,
                children: [
                  Container(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                        colors: [
                          accentColor.withValues(alpha: 0.18),
                          Colors.white,
                        ],
                      ),
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.fromLTRB(28, 100, 28, 30),
                    child: Hero(
                      tag: 'new_book_cover_${_book.id}',
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(28),
                        child: _book.coverUrl.isNotEmpty
                            ? CachedNetworkImage(
                                imageUrl: _book.coverUrl,
                                fit: BoxFit.cover,
                                placeholder: (context, url) =>
                                    Container(color: Colors.grey[100]),
                                errorWidget: (context, url, error) =>
                                    _buildFallbackCover(accentColor),
                              )
                            : _buildFallbackCover(accentColor),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 20, 20, 40),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: Wrap(
                          spacing: 8,
                          runSpacing: 8,
                          children:
                              (_book.categoryTags.isNotEmpty
                                      ? _book.categoryTags
                                      : ['Sách mới'])
                                  .map(
                                    (tag) => Container(
                                      padding: const EdgeInsets.symmetric(
                                        horizontal: 12,
                                        vertical: 7,
                                      ),
                                      decoration: BoxDecoration(
                                        color: accentColor.withValues(
                                          alpha: 0.12,
                                        ),
                                        borderRadius: BorderRadius.circular(
                                          999,
                                        ),
                                      ),
                                      child: Text(
                                        tag,
                                        style: TextStyle(
                                          color: accentColor,
                                          fontWeight: FontWeight.w700,
                                        ),
                                      ),
                                    ),
                                  )
                                  .toList(),
                        ),
                      ),
                      const SizedBox(width: 12),
                      if (_book.isPinned)
                        Container(
                          margin: const EdgeInsets.only(top: 2),
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            shape: BoxShape.circle,
                            boxShadow: [
                              BoxShadow(
                                color: Colors.black.withValues(alpha: 0.08),
                                blurRadius: 10,
                                offset: const Offset(0, 4),
                              ),
                            ],
                          ),
                          child: const Icon(
                            Icons.push_pin,
                            size: 18,
                            color: Colors.red,
                          ),
                        ),
                      if (_book.isPinned && _isRefreshing) const SizedBox(width: 12),
                      if (_isRefreshing)
                        SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(
                            strokeWidth: 2.2,
                            color: accentColor,
                          ),
                        ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Text(
                    _book.title,
                    style: const TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.w800,
                      height: 1.35,
                      color: AppColors.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 10),
                  Text(
                    _book.author.isNotEmpty
                        ? _book.author
                        : 'Chưa có thông tin tác giả',
                    style: const TextStyle(
                      fontSize: 16,
                      color: AppColors.textSecondary,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const SizedBox(height: 20),
                  Wrap(
                    spacing: 10,
                    runSpacing: 10,
                    children: [
                      _buildInfoChip(
                        Icons.apartment_rounded,
                        _book.publisher,
                        'Chưa có nhà xuất bản',
                      ),
                      _buildInfoChip(
                        Icons.calendar_today_rounded,
                        _formatYearOrArrival(),
                        'Chưa có năm XB',
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),
                  _buildSectionCard(
                    title: 'Giới thiệu nhanh',
                    child: Text(
                      _book.description.isNotEmpty
                          ? _book.description
                          : 'Hiện chưa có phần giới thiệu cho đầu sách này.',
                      style: const TextStyle(
                        fontSize: 15,
                        height: 1.7,
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  if (_book.sourceUrl.isNotEmpty)
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton.icon(
                        onPressed: _openSourceUrl,
                        style: FilledButton.styleFrom(
                          backgroundColor: accentColor,
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(vertical: 16),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(18),
                          ),
                        ),
                        icon: const Icon(Icons.open_in_new_rounded),
                        label: const Text('Xem chi tiết và mượn sách'),
                      ),
                    ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFallbackCover(Color accentColor) {
    return Container(
      color: const Color(0xFFFFF1E8),
      child: Center(
        child: Icon(Icons.menu_book_rounded, size: 64, color: accentColor),
      ),
    );
  }

  Widget _buildInfoChip(IconData icon, String value, String fallback) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFE8ECF3)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 16, color: AppColors.brandColor),
          const SizedBox(width: 8),
          Text(
            value.isNotEmpty ? value : fallback,
            style: const TextStyle(
              color: AppColors.textSecondary,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSectionCard({required String title, required Widget child}) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(22),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.03),
            blurRadius: 12,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w800,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 12),
          child,
        ],
      ),
    );
  }

  String _formatYearOrArrival() {
    if (_book.publishYear != null) {
      return _book.publishYear.toString();
    }

    if (_book.arrivalDate.isEmpty) {
      return '';
    }

    try {
      return DateFormat('dd/MM/yyyy').format(DateTime.parse(_book.arrivalDate));
    } catch (_) {
      return _book.arrivalDate;
    }
  }
}
