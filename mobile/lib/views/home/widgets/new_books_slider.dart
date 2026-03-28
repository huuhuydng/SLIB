import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/new_book_model.dart';
import 'package:slib/views/new_books/new_book_detail_screen.dart';

class NewBooksSlider extends StatelessWidget {
  final List<NewBook> books;

  const NewBooksSlider({super.key, required this.books});

  @override
  Widget build(BuildContext context) {
    if (books.isEmpty) return const SizedBox.shrink();

    return SizedBox(
      height: 250,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(vertical: 4),
        clipBehavior: Clip.none,
        itemCount: books.length,
        separatorBuilder: (context, index) => const SizedBox(width: 14),
        itemBuilder: (context, index) => _buildBookItem(context, books[index]),
      ),
    );
  }

  Widget _buildBookItem(BuildContext context, NewBook book) {
    final accentColor = book.getAccentColor();

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
        width: 190,
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(22),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.08),
              blurRadius: 14,
              offset: const Offset(0, 8),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: ClipRRect(
                borderRadius: const BorderRadius.vertical(
                  top: Radius.circular(22),
                ),
                child: Hero(
                  tag: 'new_book_cover_${book.id}',
                  child: book.coverUrl.isNotEmpty
                      ? CachedNetworkImage(
                          imageUrl: book.coverUrl,
                          width: double.infinity,
                          fit: BoxFit.cover,
                          placeholder: (context, url) =>
                              Container(color: Colors.grey[100]),
                          errorWidget: (context, url, error) => Container(
                            color: const Color(0xFFFFF1E8),
                            child: Icon(
                              Icons.menu_book_rounded,
                              color: accentColor,
                              size: 38,
                            ),
                          ),
                        )
                      : Container(
                          color: const Color(0xFFFFF1E8),
                          child: Icon(
                            Icons.menu_book_rounded,
                            color: accentColor,
                            size: 38,
                          ),
                        ),
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(14, 14, 14, 16),
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
                      style: TextStyle(
                        color: accentColor,
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  const SizedBox(height: 10),
                  Text(
                    book.title,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w800,
                      height: 1.3,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    book.author.isNotEmpty ? book.author : 'Chưa có tác giả',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppColors.textSecondary,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    _formatMeta(book),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 11,
                      color: AppColors.textThird,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _formatMeta(NewBook book) {
    final pieces = <String>[];
    if (book.publisher.isNotEmpty) {
      pieces.add(book.publisher);
    }
    if (book.publishYear != null) {
      pieces.add(book.publishYear.toString());
    }
    if (pieces.isEmpty && book.arrivalDate.isNotEmpty) {
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
