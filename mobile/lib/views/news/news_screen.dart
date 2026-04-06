import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/news_model.dart';
import 'package:slib/services/news/news_service.dart';
import 'package:slib/services/app/local_storage_service.dart';
import 'package:slib/views/widgets/error_display_widget.dart';
import 'news_detail_screen.dart';

class NewsScreen extends StatefulWidget {
  const NewsScreen({super.key});

  @override
  State<NewsScreen> createState() => _NewsScreenState();
}

class _NewsScreenState extends State<NewsScreen> {
  List<News> _allNews = [];
  List<News> _displayNews = [];
  bool _isLoading = true;

  String _selectedCategory = "Tất cả";
  final List<String> _categories = [
    "Tất cả",
    "Quan trọng",
    "Sự kiện",
    "Sách mới",
    "Ưu đãi",
  ];

  final NewsService _newsService = NewsService();
  final LocalStorageService _localService = LocalStorageService();

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  void _loadData() async {
    // 1. Cache
    final cachedNews = await _localService.loadNewsList();
    if (cachedNews.isNotEmpty && mounted) {
      setState(() {
        _allNews = cachedNews;
        _applyFilter();
        _isLoading = false;
      });
    }

    // 2. API
    try {
      final freshNews = await _newsService.fetchPublicNews();
      if (mounted) {
        setState(() {
          _allNews = freshNews;
          _applyFilter();
          _isLoading = false;
        });
      }
      await _localService.saveNewsList(freshNews);
    } catch (e) {
      if (mounted && _allNews.isEmpty) setState(() => _isLoading = false);
    }
  }

  void _applyFilter() {
    if (_selectedCategory == "Tất cả") {
      _displayNews = List.from(_allNews);
    } else {
      _displayNews = _allNews
          .where((item) => item.categoryName == _selectedCategory)
          .toList();
    }
    // Sắp xếp mới nhất lên đầu
    _displayNews.sort((a, b) => b.publishedAt.compareTo(a.publishedAt));
  }

  Future<void> _onRefresh() async {
    try {
      final freshNews = await _newsService.fetchPublicNews();
      if (mounted) {
        setState(() {
          _allNews = freshNews;
          _applyFilter();
        });
      }
      await _localService.saveNewsList(freshNews);
    } catch (e) {
      debugPrint('$e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey[50],
      appBar: AppBar(
        title: const Text(
          "Tin tức & Sự kiện",
          style: TextStyle(fontWeight: FontWeight.bold, color: Colors.black87),
        ),
        backgroundColor: Colors.white,
        centerTitle: true,
        elevation: 0,
        iconTheme: const IconThemeData(color: Colors.black87),
      ),
      body: Column(
        children: [
          // Filter Chips
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
                        if (selected) {
                          setState(() {
                            _selectedCategory = category;
                            _applyFilter();
                          });
                        }
                      },
                      selectedColor: AppColors.brandColor,
                      labelStyle: TextStyle(
                        color: isSelected ? Colors.white : Colors.black87,
                        fontWeight: isSelected
                            ? FontWeight.bold
                            : FontWeight.normal,
                      ),
                      backgroundColor: Colors.grey.shade100,
                      side: BorderSide.none,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(20),
                      ),
                    ),
                  );
                }).toList(),
              ),
            ),
          ),

          // List
          Expanded(
            child: RefreshIndicator(
              onRefresh: _onRefresh,
              color: AppColors.brandColor,
              child: _isLoading && _allNews.isEmpty
                  ? const Center(child: CircularProgressIndicator())
                  : _displayNews.isEmpty
                  ? ErrorDisplayWidget.empty(message: 'Chưa có tin tức nào')
                  : ListView.builder(
                      padding: const EdgeInsets.all(16),
                      itemCount: _displayNews.length,
                      itemBuilder: (context, index) {
                        return _buildBigNewsCard(context, _displayNews[index]);
                      },
                    ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBigNewsCard(BuildContext context, News item) {
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
              color: Colors.black.withValues(alpha: 0.05),
              blurRadius: 10,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Stack(
              children: [
                ClipRRect(
                  borderRadius: const BorderRadius.vertical(
                    top: Radius.circular(16),
                  ),
                  child: Hero(
                    tag:
                        "news_list_${item.id}", // Tag khác với Slider để tránh lỗi
                    child: CachedNetworkImage(
                      imageUrl: item.imageUrl,
                      height: 180,
                      width: double.infinity,
                      fit: BoxFit.cover,
                      placeholder: (context, url) =>
                          Container(height: 180, color: Colors.grey[100]),
                      errorWidget: (context, url, error) => Container(
                        height: 180,
                        color: Colors.grey[200],
                        child: const Icon(Icons.broken_image),
                      ),
                    ),
                  ),
                ),
                Positioned(
                  top: 12,
                  left: 12,
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 10,
                      vertical: 5,
                    ),
                    decoration: BoxDecoration(
                      color: item.getTagColor(),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      item.categoryName.toUpperCase(),
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ),
              ],
            ),
            Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Icon(
                        Icons.calendar_today,
                        size: 14,
                        color: Colors.grey,
                      ),
                      const SizedBox(width: 6),
                      Text(
                        formattedDate,
                        style: const TextStyle(
                          fontSize: 12,
                          color: Colors.grey,
                        ),
                      ),
                      const Spacer(),
                      const Icon(
                        Icons.remove_red_eye,
                        size: 14,
                        color: Colors.grey,
                      ),
                      const SizedBox(width: 4),
                      Text(
                        "${item.viewCount}",
                        style: const TextStyle(
                          fontSize: 12,
                          color: Colors.grey,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    item.title,
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      height: 1.3,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    item.summary,
                    style: const TextStyle(fontSize: 14, color: Colors.grey),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
