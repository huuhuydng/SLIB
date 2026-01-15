import 'package:flutter/material.dart';

class News {
  final int id;
  final String title;
  final String summary;
  final String content; 
  final String imageUrl;
  final String categoryName;
  final String publishedAt;
  final int viewCount;
  final bool isPinned;

  News({
    required this.id,
    required this.title,
    required this.summary,
    required this.content,
    required this.imageUrl,
    required this.categoryName,
    required this.publishedAt,
    required this.viewCount,
    required this.isPinned,
  });

  factory News.fromJson(Map<String, dynamic> json) {
    return News(
      id: json['id'],
      title: json['title'],
      summary: json['summary'] ?? "",
      content: json['content'] ?? "",
      imageUrl: json['imageUrl'] ?? "https://via.placeholder.com/400x200",
      categoryName: json['category'] != null 
          ? json['category']['name'] 
          : (json['categoryName'] ?? "Tin tức"), 
      publishedAt: json['publishedAt'] ?? DateTime.now().toString(),
      viewCount: json['viewCount'] ?? 0,
      isPinned: json['isPinned'] ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'summary': summary,
      'content': content,
      'imageUrl': imageUrl,
      'categoryName': categoryName, 
      'publishedAt': publishedAt,
      'viewCount': viewCount,
      'isPinned': isPinned,
    };
  }

  Color getTagColor() {
    switch (categoryName) {
      case "Sự kiện": return Colors.blue;
      case "Quan trọng": return Colors.red;
      case "Sách mới": return Colors.green;
      case "Ưu đãi": return Colors.orange;
      default: return Colors.purple;
    }
  }
}