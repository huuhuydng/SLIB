import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';

// --- CẤU HÌNH MÀU SẮC ---

class ChatScreen extends StatefulWidget {
  const ChatScreen({super.key});

  @override
  State<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  final TextEditingController _textController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  bool _isTyping = false; // Trạng thái Bot đang gõ

  // Dữ liệu tin nhắn mẫu
  final List<ChatMessage> _messages = [
    ChatMessage(
      text: "Chào Huy! Mình là trợ lý ảo SLIB. Mình có thể giúp gì cho việc học tập của bạn hôm nay?",
      isUser: false,
      time: DateTime.now().subtract(const Duration(minutes: 1)),
    ),
  ];

  // Danh sách câu hỏi gợi ý (Suggestion Chips)
  final List<String> _suggestions = [
    "Thư viện mở đến mấy giờ?",
    "Làm sao để đặt phòng họp?",
    "Wifi thư viện mật khẩu là gì?",
    "Tôi bị trừ điểm uy tín oan!"
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
        elevation: 1,
        shadowColor: Colors.black.withOpacity(0.1),
        title: Row(
          children: [
            // Avatar Bot
            Container(
              padding: const EdgeInsets.all(2),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(color: AppColors.brandColor, width: 2),
              ),
              child: const CircleAvatar(
                radius: 18,
                backgroundColor: AppColors.brandColor,
                child: Icon(Icons.auto_awesome, color: Colors.white, size: 20),
              ),
            ),
            const SizedBox(width: 12),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text("SLIB AI Assistant", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                Row(
                  children: [
                    Container(width: 8, height: 8, decoration: const BoxDecoration(color: Colors.green, shape: BoxShape.circle)),
                    const SizedBox(width: 4),
                    const Text("Luôn sẵn sàng", style: TextStyle(color: Colors.grey, fontSize: 12)),
                  ],
                )
              ],
            )
          ],
        ),
        actions: [
          IconButton(icon: const Icon(Icons.more_vert), onPressed: () {}),
        ],
      ),
      body: Column(
        children: [
          // 1. Danh sách tin nhắn
          Expanded(
            child: ListView.builder(
              controller: _scrollController,
              padding: const EdgeInsets.all(16),
              itemCount: _messages.length + (_isTyping ? 1 : 0), // +1 nếu đang typing
              itemBuilder: (context, index) {
                // Hiển thị Typing Indicator ở cuối cùng
                if (index == _messages.length && _isTyping) {
                  return _buildTypingIndicator();
                }
                return _buildMessageBubble(_messages[index]);
              },
            ),
          ),

          // 2. Khu vực Gợi ý (Suggestions)
          if (_messages.length < 3) // Chỉ hiện gợi ý khi hội thoại còn ngắn
            SizedBox(
              height: 50,
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 16),
                itemCount: _suggestions.length,
                itemBuilder: (context, index) {
                  return Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: ActionChip(
                      label: Text(_suggestions[index], style: const TextStyle(fontSize: 12, color: AppColors.textPrimary)),
                      backgroundColor: Colors.grey[100],
                      side: BorderSide.none,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                      onPressed: () => _handleSubmitted(_suggestions[index]),
                    ),
                  );
                },
              ),
            ),

          // 3. Thanh nhập liệu (Input Bar)
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: Colors.white,
              boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.05), offset: const Offset(0, -2), blurRadius: 10)],
            ),
            child: SafeArea(
              child: Row(
                children: [
                  IconButton(icon: const Icon(Icons.add_circle_outline, color: Colors.grey), onPressed: () {}),
                  Expanded(
                    child: TextField(
                      controller: _textController,
                      decoration: InputDecoration(
                        hintText: "Nhập tin nhắn...",
                        hintStyle: TextStyle(color: Colors.grey[400]),
                        filled: true,
                        fillColor: Colors.grey[100],
                        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(24), borderSide: BorderSide.none),
                      ),
                      onSubmitted: _handleSubmitted,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Container(
                    decoration: const BoxDecoration(color: AppColors.brandColor, shape: BoxShape.circle),
                    child: IconButton(
                      icon: const Icon(Icons.send_rounded, color: Colors.white, size: 20),
                      onPressed: () => _handleSubmitted(_textController.text),
                    ),
                  )
                ],
              ),
            ),
          )
        ],
      ),
    );
  }

  // Widget: Bong bóng tin nhắn
  Widget _buildMessageBubble(ChatMessage message) {
    bool isUser = message.isUser;
    return Align(
      alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 4),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.75),
        decoration: BoxDecoration(
          color: isUser ? AppColors.brandColor : Colors.grey[100],
          borderRadius: BorderRadius.only(
            topLeft: const Radius.circular(16),
            topRight: const Radius.circular(16),
            bottomLeft: isUser ? const Radius.circular(16) : Radius.zero,
            bottomRight: isUser ? Radius.zero : const Radius.circular(16),
          ),
        ),
        child: Text(
          message.text,
          style: TextStyle(
            color: isUser ? Colors.white : AppColors.textPrimary,
            fontSize: 15,
            height: 1.4,
          ),
        ),
      ),
    );
  }

  // Widget: Hiệu ứng "Đang gõ..."
  Widget _buildTypingIndicator() {
    return Align(
      alignment: Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 4),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: Colors.grey[100],
          borderRadius: const BorderRadius.only(
            topLeft: Radius.circular(16),
            topRight: Radius.circular(16),
            bottomRight: Radius.circular(16),
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            _dot(0), const SizedBox(width: 4),
            _dot(1), const SizedBox(width: 4),
            _dot(2),
          ],
        ),
      ),
    );
  }

  Widget _dot(int index) {
    return Container(
      width: 6, height: 6,
      decoration: BoxDecoration(color: Colors.grey[400], shape: BoxShape.circle),
    );
  }

  // Xử lý gửi tin nhắn & Giả lập AI trả lời
  void _handleSubmitted(String text) {
    if (text.trim().isEmpty) return;

    _textController.clear();
    setState(() {
      _messages.add(ChatMessage(text: text, isUser: true, time: DateTime.now()));
      _isTyping = true; // Bật trạng thái bot đang gõ
    });

    // Cuộn xuống cuối
    Future.delayed(const Duration(milliseconds: 100), () {
      _scrollController.animateTo(_scrollController.position.maxScrollExtent, duration: const Duration(milliseconds: 300), curve: Curves.easeOut);
    });

    // Giả lập độ trễ của AI (1.5 giây)
    Future.delayed(const Duration(milliseconds: 1500), () {
      if (!mounted) return;
      setState(() {
        _isTyping = false;
        // Logic trả lời đơn giản (Mock AI)
        String response = "Xin lỗi, mình chưa hiểu ý bạn.";
        if (text.toLowerCase().contains("wifi")) {
          response = "Mật khẩu Wifi thư viện là: FPTU_Library_2025. Bạn cần đăng nhập bằng tài khoản sinh viên nhé!";
        } else if (text.toLowerCase().contains("mở") || text.toLowerCase().contains("giờ")) {
          response = "Thư viện mở cửa từ 07:30 đến 21:00 (Thứ 2 - Thứ 7). Chủ nhật nghỉ ạ.";
        } else if (text.toLowerCase().contains("đặt") || text.toLowerCase().contains("chỗ")) {
          response = "Để đặt chỗ, bạn quay lại màn hình chính và chọn mục 'Đặt chỗ' hoặc biểu tượng cái ghế nhé!";
        }

        _messages.add(ChatMessage(text: response, isUser: false, time: DateTime.now()));
      });
      
      // Cuộn xuống lần nữa khi bot trả lời xong
      Future.delayed(const Duration(milliseconds: 100), () {
        _scrollController.animateTo(_scrollController.position.maxScrollExtent, duration: const Duration(milliseconds: 300), curve: Curves.easeOut);
      });
    });
  }
}

// Model tin nhắn
class ChatMessage {
  final String text;
  final bool isUser;
  final DateTime time;

  ChatMessage({required this.text, required this.isUser, required this.time});
}