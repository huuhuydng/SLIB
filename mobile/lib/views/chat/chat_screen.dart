import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/services/chat_service.dart';

// --- CẤU HÌNH MÀU SẮC ---

class ChatScreen extends StatefulWidget {
  const ChatScreen({super.key});

  @override
  State<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  final TextEditingController _textController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final ChatService _chatService = ChatService();
  
  bool _isTyping = false; // Trạng thái Bot đang gõ
  bool _isEscalated = false; // Đã chuyển sang thủ thư

  // Dữ liệu tin nhắn
  final List<ChatMessage> _messages = [
    ChatMessage(
      text: "Chào bạn! Mình là trợ lý ảo SLIB. Mình có thể giúp gì cho việc học tập của bạn hôm nay? 📚",
      isUser: false,
      time: DateTime.now().subtract(const Duration(minutes: 1)),
    ),
  ];

  // Danh sách câu hỏi gợi ý (Suggestion Chips)
  final List<String> _suggestions = [
    "Thư viện mở đến mấy giờ?",
    "Làm sao để đặt phòng họp?",
    "Wifi thư viện mật khẩu là gì?",
    "Cho em gặp thủ thư",
  ];

  @override
  void dispose() {
    _textController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

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
            // Avatar Bot hoặc Librarian
            Container(
              padding: const EdgeInsets.all(2),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                  color: _isEscalated ? Colors.green : AppColors.brandColor, 
                  width: 2
                ),
              ),
              child: CircleAvatar(
                radius: 18,
                backgroundColor: _isEscalated ? Colors.green : AppColors.brandColor,
                child: Icon(
                  _isEscalated ? Icons.support_agent : Icons.auto_awesome, 
                  color: Colors.white, 
                  size: 20
                ),
              ),
            ),
            const SizedBox(width: 12),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  _isEscalated ? "Thủ thư SLIB" : "SLIB AI Assistant", 
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)
                ),
                Row(
                  children: [
                    Container(
                      width: 8, 
                      height: 8, 
                      decoration: BoxDecoration(
                        color: _isEscalated ? Colors.orange : Colors.green, 
                        shape: BoxShape.circle
                      )
                    ),
                    const SizedBox(width: 4),
                    Text(
                      _isEscalated ? "Đang chờ..." : "Luôn sẵn sàng", 
                      style: const TextStyle(color: Colors.grey, fontSize: 12)
                    ),
                  ],
                )
              ],
            )
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: "Bắt đầu cuộc trò chuyện mới",
            onPressed: _resetConversation,
          ),
        ],
      ),
      body: Column(
        children: [
          // Escalation Banner
          if (_isEscalated)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              color: Colors.orange[50],
              child: Row(
                children: [
                  Icon(Icons.info_outline, color: Colors.orange[700], size: 20),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      "Bạn đã được chuyển đến thủ thư. Vui lòng chờ...",
                      style: TextStyle(color: Colors.orange[800], fontSize: 13),
                    ),
                  ),
                ],
              ),
            ),
          
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
          if (_messages.length < 4 && !_isEscalated) // Chỉ hiện gợi ý khi hội thoại còn ngắn
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
                      label: Text(
                        _suggestions[index], 
                        style: const TextStyle(fontSize: 12, color: AppColors.textPrimary)
                      ),
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
                  IconButton(
                    icon: const Icon(Icons.add_circle_outline, color: Colors.grey), 
                    onPressed: () {}
                  ),
                  Expanded(
                    child: TextField(
                      controller: _textController,
                      decoration: InputDecoration(
                        hintText: _isEscalated ? "Nhắn tin cho thủ thư..." : "Nhập tin nhắn...",
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
                    decoration: BoxDecoration(
                      color: _isEscalated ? Colors.green : AppColors.brandColor, 
                      shape: BoxShape.circle
                    ),
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
          color: isUser ? AppColors.brandColor : (message.isEscalation ? Colors.orange[100] : Colors.grey[100]),
          borderRadius: BorderRadius.only(
            topLeft: const Radius.circular(16),
            topRight: const Radius.circular(16),
            bottomLeft: isUser ? const Radius.circular(16) : Radius.zero,
            bottomRight: isUser ? Radius.zero : const Radius.circular(16),
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (message.isEscalation)
              Padding(
                padding: const EdgeInsets.only(bottom: 4),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.support_agent, size: 14, color: Colors.orange[700]),
                    const SizedBox(width: 4),
                    Text("Chuyển tiếp", style: TextStyle(fontSize: 11, color: Colors.orange[700], fontWeight: FontWeight.w600)),
                  ],
                ),
              ),
            Text(
              message.text,
              style: TextStyle(
                color: isUser ? Colors.white : AppColors.textPrimary,
                fontSize: 15,
                height: 1.4,
              ),
            ),
          ],
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

  // Xử lý gửi tin nhắn & Gọi AI Service
  void _handleSubmitted(String text) async {
    if (text.trim().isEmpty) return;

    _textController.clear();
    setState(() {
      _messages.add(ChatMessage(text: text, isUser: true, time: DateTime.now()));
      _isTyping = true; // Bật trạng thái bot đang gõ
    });

    // Cuộn xuống cuối
    _scrollToBottom();

    try {
      // Gọi AI Service API
      final response = await _chatService.sendMessage(text);
      
      if (!mounted) return;
      
      setState(() {
        _isTyping = false;
        
        // Thêm tin nhắn phản hồi
        _messages.add(ChatMessage(
          text: response.reply,
          isUser: false,
          time: DateTime.now(),
          isEscalation: response.escalated,
        ));
        
        // Cập nhật trạng thái escalation
        if (response.escalated) {
          _isEscalated = true;
        }
      });
      
      _scrollToBottom();
      
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isTyping = false;
        _messages.add(ChatMessage(
          text: "Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.",
          isUser: false,
          time: DateTime.now(),
        ));
      });
      _scrollToBottom();
    }
  }

  void _scrollToBottom() {
    Future.delayed(const Duration(milliseconds: 100), () {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  void _resetConversation() {
    setState(() {
      _messages.clear();
      _messages.add(ChatMessage(
        text: "Chào bạn! Mình là trợ lý ảo SLIB. Mình có thể giúp gì cho việc học tập của bạn hôm nay? 📚",
        isUser: false,
        time: DateTime.now(),
      ));
      _isEscalated = false;
      _chatService.clearSession();
    });
  }
}

// Model tin nhắn
class ChatMessage {
  final String text;
  final bool isUser;
  final DateTime time;
  final bool isEscalation;

  ChatMessage({
    required this.text, 
    required this.isUser, 
    required this.time,
    this.isEscalation = false,
  });
}