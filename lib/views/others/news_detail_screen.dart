import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';
import 'news_item.dart'; // <--- Thêm dòng này để dùng model chung



class NewsDetailScreen extends StatelessWidget {
  final NewsItem news;

  const NewsDetailScreen({super.key, required this.news});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: Column(
        children: [
          // 1. Nội dung cuộn (Scrollable Content)
          Expanded(
            child: CustomScrollView(
              slivers: [
                // A. Ảnh bìa Parallax
                SliverAppBar(
                  expandedHeight: 280,
                  pinned: true, // Giữ lại thanh AppBar khi cuộn
                  backgroundColor: Colors.white,
                  foregroundColor: Colors.black, // Màu nút Back
                  flexibleSpace: FlexibleSpaceBar(
                    background: Hero( // Hiệu ứng chuyển cảnh ảnh mượt mà
                      tag: news.id, // Tag phải trùng với màn hình danh sách
                      child: Image.network(
                        news.imageUrl,
                        fit: BoxFit.cover,
                        color: Colors.black12,
                        colorBlendMode: BlendMode.darken, // Làm tối ảnh 1 chút để chữ nổi (nếu có)
                      ),
                    ),
                  ),
                ),

                // B. Nội dung bài viết
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.all(20.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        // Tag Category
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                          decoration: BoxDecoration(
                            color: news.tagColor.withOpacity(0.1),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Text(
                            news.category.toUpperCase(),
                            style: TextStyle(
                              color: news.tagColor,
                              fontWeight: FontWeight.bold,
                              fontSize: 12,
                            ),
                          ),
                        ),
                        
                        const SizedBox(height: 16),

                        // Tiêu đề lớn
                        Text(
                          news.title,
                          style: const TextStyle(
                            fontSize: 24,
                            fontWeight: FontWeight.w800,
                            height: 1.3,
                            color: AppColors.textPrimary,
                          ),
                        ),

                        const SizedBox(height: 12),

                        // Ngày đăng & Tác giả
                        Row(
                          children: [
                            const CircleAvatar(
                              radius: 14,
                              backgroundImage: NetworkImage('https://i.pravatar.cc/150?u=library_admin'), // Avatar Admin giả
                            ),
                            const SizedBox(width: 8),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Text("Ban Quản Lý Thư Viện", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
                                Text(news.date, style: const TextStyle(color: Colors.grey, fontSize: 11)),
                              ],
                            )
                          ],
                        ),

                        const Padding(
                          padding: EdgeInsets.symmetric(vertical: 20),
                          child: Divider(height: 1),
                        ),

                        // Nội dung chi tiết (Giả lập Rich Text)
                        // Phần mở đầu đậm
                        Text(
                          news.summary,
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold, // In đậm phần tóm tắt
                            height: 1.6,
                            color: Color(0xFF2D3748),
                          ),
                        ),
                        
                        const SizedBox(height: 16),

                        // Phần thân bài (Giả lập văn bản dài)
                        Text(
                          _generateDummyContent(),
                          style: const TextStyle(
                            fontSize: 16,
                            height: 1.6, // Khoảng cách dòng dễ đọc
                            color: Color(0xFF4A5568),
                          ),
                        ),

                         const SizedBox(height: 20),
                         
                         // Ảnh minh họa trong bài (nếu có)
                         ClipRRect(
                           borderRadius: BorderRadius.circular(12),
                           child: Image.network(
                             "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?q=80&w=600&auto=format&fit=crop",
                             height: 200,
                             width: double.infinity,
                             fit: BoxFit.cover,
                           ),
                         ),
                         const Text(
                           "Không gian học tập hiện đại tại SLIB",
                           style: TextStyle(fontSize: 12, color: Colors.grey, fontStyle: FontStyle.italic),
                         ),
                         
                         const SizedBox(height: 20),
                         
                         Text(
                          "Sinh viên có thể đăng ký tham gia ngay trên ứng dụng hoặc liên hệ quầy thủ thư để biết thêm chi tiết. Trân trọng thông báo.",
                          style: const TextStyle(
                            fontSize: 16,
                            height: 1.6,
                            color: Color(0xFF4A5568),
                          ),
                        ),
                        
                        const SizedBox(height: 40), // Khoảng trống cuối cùng
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),

          // 2. Bottom Action Bar (Thanh hành động dưới cùng)
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.white,
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.05),
                  blurRadius: 10,
                  offset: const Offset(0, -5),
                )
              ],
            ),

          )
        ],
      ),
    );
  }

  // Hàm tạo nội dung giả (Lorem Ipsum tiếng Việt)
  String _generateDummyContent() {
    return """
Thư viện thông minh SLIB xin trân trọng thông báo đến toàn thể sinh viên và giảng viên về sự kiện đặc biệt này. Đây là cơ hội không thể bỏ qua để cập nhật những kiến thức công nghệ mới nhất.

Trong khuôn khổ chương trình, chúng ta sẽ được lắng nghe chia sẻ từ các chuyên gia hàng đầu của FPT Software và các đối tác công nghệ lớn. Các chủ đề sẽ xoay quanh:
- Ứng dụng AI trong học tập và nghiên cứu.
- Xu hướng Big Data năm 2025.
- Kỹ năng mềm cho kỹ sư công nghệ tương lai.

Ngoài ra, khi tham gia sự kiện, các bạn sinh viên còn có cơ hội nhận được điểm rèn luyện và nhiều phần quà hấp dẫn từ ban tổ chức.

Chúng tôi khuyến khích các bạn đến sớm để check-in và chọn chỗ ngồi tốt nhất. Hệ thống check-in bằng khuôn mặt và thẻ NFC sẽ được kích hoạt tại cửa ra vào hội trường.
    """;
  }
}