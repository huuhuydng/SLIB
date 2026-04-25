import 'package:flutter/material.dart';

class NewBook {
  final int id;
  final String title;
  final String author;
  final String coverUrl;
  final String description;
  final String category;
  final int? publishYear;
  final String arrivalDate;
  final bool isActive;
  final bool isPinned;
  final String sourceUrl;
  final String publisher;

  const NewBook({
    required this.id,
    required this.title,
    required this.author,
    required this.coverUrl,
    required this.description,
    required this.category,
    required this.publishYear,
    required this.arrivalDate,
    required this.isActive,
    required this.isPinned,
    required this.sourceUrl,
    required this.publisher,
  });

  factory NewBook.fromJson(Map<String, dynamic> json) {
    return NewBook(
      id: json['id'] ?? 0,
      title: json['title'] ?? '',
      author: json['author'] ?? '',
      coverUrl: json['coverUrl'] ?? '',
      description: json['description'] ?? '',
      category: json['category'] ?? 'Sách mới',
      publishYear: json['publishYear'],
      arrivalDate: json['arrivalDate'] ?? DateTime.now().toIso8601String(),
      isActive: json['isActive'] ?? true,
      isPinned: json['isPinned'] ?? false,
      sourceUrl: json['sourceUrl'] ?? '',
      publisher: json['publisher'] ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'author': author,
      'coverUrl': coverUrl,
      'description': description,
      'category': category,
      'publishYear': publishYear,
      'arrivalDate': arrivalDate,
      'isActive': isActive,
      'isPinned': isPinned,
      'sourceUrl': sourceUrl,
      'publisher': publisher,
    };
  }

  List<String> get categoryTags {
    return category
        .split(',')
        .map((tag) => tag.trim())
        .where((tag) => tag.isNotEmpty)
        .toList();
  }

  Color getAccentColor() {
    return const Color(0xFFFF751F);
  }
}
