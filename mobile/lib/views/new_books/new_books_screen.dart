import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/new_book_model.dart';
import 'package:slib/services/app/local_storage_service.dart';
import 'package:slib/services/new_books/new_book_service.dart';
import 'package:slib/views/new_books/new_book_detail_screen.dart';
import 'package:slib/views/widgets/error_display_widget.dart';

class NewBooksScreen extends StatefulWidget {
  const NewBooksScreen({super.key});

  @override
  State<NewBooksScreen> createState() => _NewBooksScreenState();
}

class _NewBooksScreenState extends State<NewBooksScreen> {
  final NewBookService _newBookService = NewBookService();
  final LocalStorageService _localStorageService = LocalStorageService();

  List<NewBook> _allBooks = [];
  List<NewBook> _displayBooks = [];
  bool _isLoading = true;
  String _searchQuery = '';

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    final cachedBooks = await _localStorageService.loadNewBooksList();
    if (cachedBooks.isNotEmpty && mounted) {
      setState(() {
        _allBooks = _sortBooks(cachedBooks);
        _applyFilter();
        _isLoading = false;
      });
    }

    try {
      final freshBooks = await _newBookService.fetchPublicNewBooks();
      if (!mounted) return;
      setState(() {
        _allBooks = _sortBooks(freshBooks);
        _applyFilter();
        _isLoading = false;
      });
      await _localStorageService.saveNewBooksList(freshBooks);
    } catch (e) {
      if (mounted && _allBooks.isEmpty) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _onRefresh() async {
    try {
      final freshBooks = await _newBookService.fetchPublicNewBooks();
      if (!mounted) return;
      setState(() {
        _allBooks = _sortBooks(freshBooks);
        _applyFilter();
      });
      await _localStorageService.saveNewBooksList(freshBooks);
    } catch (_) {}
  }

  void _applyFilter() {
    final query = _searchQuery.trim().toLowerCase();
    if (query.isEmpty) {
      _displayBooks = List.from(_allBooks);
      return;
    }

    _displayBooks = _allBooks.where((book) {
      final searchableFields = [
        book.title,
        book.author,
        book.publisher,
        book.category,
      ];

      return searchableFields
          .where((value) => value.isNotEmpty)
          .any((value) => value.toLowerCase().contains(query));
    }).toList();
  }

  List<NewBook> _sortBooks(List<NewBook> books) {
    final sorted = List<NewBook>.from(books);
    sorted.sort((a, b) {
      if (a.isPinned != b.isPinned) {
        return a.isPinned ? -1 : 1;
      }
      return b.arrivalDate.compareTo(a.arrivalDate);
    });
    return sorted;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey[50],
      appBar: AppBar(
        title: const Text(
          'Sách mới thư viện',
          style: TextStyle(fontWeight: FontWeight.bold, color: Colors.black87),
        ),
        centerTitle: true,
        elevation: 0,
        backgroundColor: Colors.white,
        iconTheme: const IconThemeData(color: Colors.black87),
      ),
      body: Column(
        children: [
          Container(
            color: Colors.white,
            padding: const EdgeInsets.fromLTRB(16, 10, 16, 14),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 2),
              decoration: BoxDecoration(
                color: const Color(0xFFF4F6FA),
                borderRadius: BorderRadius.circular(16),
              ),
              child: TextField(
                onChanged: (value) {
                  setState(() {
                    _searchQuery = value;
                    _applyFilter();
                  });
                },
                decoration: const InputDecoration(
                  icon: Icon(Icons.search_rounded),
                  hintText: 'Tìm theo tên sách, tác giả, nhà xuất bản...',
                  border: InputBorder.none,
                ),
              ),
            ),
          ),
          Expanded(
            child: RefreshIndicator(
              onRefresh: _onRefresh,
              color: AppColors.brandColor,
              child: _isLoading && _allBooks.isEmpty
                  ? const Center(child: CircularProgressIndicator())
                  : _displayBooks.isEmpty
                  ? ErrorDisplayWidget.empty(message: 'Chưa có sách mới nào')
                  : ListView.builder(
                      padding: const EdgeInsets.all(16),
                      itemCount: _displayBooks.length,
                      itemBuilder: (context, index) {
                        return _buildBookCard(context, _displayBooks[index]);
                      },
                    ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBookCard(BuildContext context, NewBook book) {
    return GestureDetector(
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => NewBookDetailScreen(book: book),
          ),
        );
      },
      child: Container(
        margin: const EdgeInsets.only(bottom: 18),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(18),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.05),
              blurRadius: 10,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Row(
          children: [
            SizedBox(
              width: 120,
              height: 160,
              child: Stack(
                children: [
                  ClipRRect(
                    borderRadius: const BorderRadius.horizontal(
                      left: Radius.circular(18),
                    ),
                    child: SizedBox(
                      width: 120,
                      height: 160,
                      child: Hero(
                        tag: 'new_book_cover_${book.id}',
                        child: book.coverUrl.isNotEmpty
                            ? CachedNetworkImage(
                                imageUrl: book.coverUrl,
                                fit: BoxFit.cover,
                                placeholder: (context, url) =>
                                    Container(color: Colors.grey[100]),
                                errorWidget: (context, url, error) =>
                                    _buildFallbackCover(),
                              )
                            : _buildFallbackCover(),
                      ),
                    ),
                  ),
                  if (book.isPinned)
                    const Positioned(
                      top: 10,
                      right: 10,
                      child: _PinnedBadge(),
                    ),
                ],
              ),
            ),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 10,
                        vertical: 5,
                      ),
                      decoration: BoxDecoration(
                        color: const Color(0xFFFFF1E8),
                        borderRadius: BorderRadius.circular(999),
                      ),
                      child: Text(
                        book.categoryTags.isNotEmpty
                            ? book.categoryTags.first
                            : 'Sách mới',
                        style: const TextStyle(
                          color: AppColors.brandColor,
                          fontWeight: FontWeight.w700,
                          fontSize: 11,
                        ),
                      ),
                    ),
                    const SizedBox(height: 10),
                    Text(
                      book.title,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 17,
                        fontWeight: FontWeight.w800,
                        height: 1.35,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      book.author.isNotEmpty ? book.author : 'Chưa có tác giả',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        color: AppColors.textSecondary,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      _buildMeta(book),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        color: AppColors.textThird,
                        fontSize: 12,
                        height: 1.5,
                      ),
                    ),
                    const SizedBox(height: 12),
                    Text(
                      book.description.isNotEmpty
                          ? book.description
                          : 'Mở để xem chi tiết đầu sách mới này.',
                      maxLines: 3,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        color: AppColors.textSecondary,
                        height: 1.5,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFallbackCover() {
    return Container(
      color: const Color(0xFFFFF1E8),
      child: const Icon(
        Icons.menu_book_rounded,
        color: AppColors.brandColor,
        size: 40,
      ),
    );
  }

  String _buildMeta(NewBook book) {
    final pieces = <String>[];
    if (book.publisher.isNotEmpty) {
      pieces.add(book.publisher);
    }
    if (book.publishYear != null) {
      pieces.add(book.publishYear.toString());
    }
    if (pieces.isEmpty) {
      try {
        return DateFormat(
          'dd/MM/yyyy',
        ).format(DateTime.parse(book.arrivalDate));
      } catch (_) {
        return book.arrivalDate;
      }
    }
    return pieces.join(' • ');
  }
}

class _PinnedBadge extends StatelessWidget {
  const _PinnedBadge();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(6),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.96),
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
        size: 15,
        color: Colors.red,
      ),
    );
  }
}
