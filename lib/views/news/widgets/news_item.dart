import 'package:flutter/material.dart';

// Đây là file Model dùng chung cho cả ứng dụng
class NewsItem {
  final String id;
  final String title;
  final String summary;
  final String date;
  final String imageUrl;
  final String category;
  final Color tagColor;

  NewsItem({
    required this.id,
    required this.title,
    required this.summary,
    required this.date,
    required this.imageUrl,
    required this.category,
    required this.tagColor,
  });
}