import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/services/auth_service.dart';
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
  
  bool _isTyping = false; // Trang thai Bot dang go
  bool _isEscalated = false; // Da chuyen sang thu thu
  bool _isWaitingInQueue = false; // Dang cho trong queue
  int _queuePosition = 0; // Vi tri trong hang doi
  String? _conversationId; // ID conversation khi escalate
  String? _librarianName; // Ten thu thu khi tiep nhan
  final Set<String> _messageIds = {}; // Track message IDs từ backend

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

  // SharedPreferences keys
  static const String _keyConversationId = 'chat_conversation_id';
  static const String _keyIsEscalated = 'chat_is_escalated';
  static const String _keyLibrarianName = 'chat_librarian_name';
  static const String _keyIsWaitingInQueue = 'chat_is_waiting';

  bool _isLoadingState = true; // Loading indicator

  @override
  void initState() {
    super.initState();
    _loadSavedState();
  }

  /// Load saved conversation state từ SharedPreferences
  Future<void> _loadSavedState() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final savedConversationId = prefs.getString(_keyConversationId);
      final savedIsEscalated = prefs.getBool(_keyIsEscalated) ?? false;
      final savedLibrarianName = prefs.getString(_keyLibrarianName);
      final savedIsWaiting = prefs.getBool(_keyIsWaitingInQueue) ?? false;

      print('[PERSIST] Loading state: convId=$savedConversationId, escalated=$savedIsEscalated, waiting=$savedIsWaiting');

      if (savedConversationId != null && savedIsEscalated) {
        // Có conversation đã lưu, load messages từ backend
        final authService = Provider.of<AuthService>(context, listen: false);
        final token = await authService.getToken();

        if (token != null) {
          // Check conversation status
          final status = await _chatService.getConversationStatus(savedConversationId, token);
          
          if (!status.isResolved) {
            // Conversation còn active, restore state
            setState(() {
              _conversationId = savedConversationId;
              _isEscalated = true;
              _librarianName = savedLibrarianName;
              _isWaitingInQueue = savedIsWaiting && !status.isHumanChatting;
            });

            // Load messages từ backend
            await _loadMessagesFromBackend(savedConversationId, token);

            // Start polling if librarian đã tiếp nhận
            if (status.isHumanChatting && !savedIsWaiting) {
              _startMessagePolling(token);
            } else if (savedIsWaiting) {
              // Tiếp tục polling status
              _startStatusPolling(token);
            }
          } else {
            // Conversation đã kết thúc, clear state
            await _clearSavedState();
          }
        }
      }
    } catch (e) {
      print('[PERSIST] Error loading state: $e');
    } finally {
      if (mounted) {
        setState(() {
          _isLoadingState = false;
        });
      }
    }
  }

  /// Load messages từ backend và thêm vào list
  Future<void> _loadMessagesFromBackend(String conversationId, String token) async {
    try {
      final backendMessages = await _chatService.getMessages(conversationId, token);
      print('[PERSIST] Loaded ${backendMessages.length} messages from backend');

      if (mounted && backendMessages.isNotEmpty) {
        setState(() {
          // Clear default message
          _messages.clear();
          _messageIds.clear();

          for (final msg in backendMessages) {
            final msgId = msg['id']?.toString() ?? '';
            final content = msg['content'] as String? ?? '';
            final senderType = msg['senderType'] as String? ?? 'STUDENT';

            if (msgId.isNotEmpty && !_messageIds.contains(msgId) && content.isNotEmpty) {
              _messageIds.add(msgId);
              _messages.add(ChatMessage(
                text: content,
                isUser: senderType == 'STUDENT',
                time: DateTime.now(), // TODO: parse actual time from backend
              ));
            }
          }
        });
        _scrollToBottom();
      }
    } catch (e) {
      print('[PERSIST] Error loading messages: $e');
    }
  }

  /// Save conversation state vào SharedPreferences
  Future<void> _saveState() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      if (_conversationId != null) {
        await prefs.setString(_keyConversationId, _conversationId!);
        await prefs.setBool(_keyIsEscalated, _isEscalated);
        await prefs.setBool(_keyIsWaitingInQueue, _isWaitingInQueue);
        if (_librarianName != null) {
          await prefs.setString(_keyLibrarianName, _librarianName!);
        }
        print('[PERSIST] State saved: convId=$_conversationId, escalated=$_isEscalated');
      }
    } catch (e) {
      print('[PERSIST] Error saving state: $e');
    }
  }

  /// Clear saved state (khi conversation kết thúc hoặc reset)
  Future<void> _clearSavedState() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove(_keyConversationId);
      await prefs.remove(_keyIsEscalated);
      await prefs.remove(_keyLibrarianName);
      await prefs.remove(_keyIsWaitingInQueue);
      print('[PERSIST] State cleared');
    } catch (e) {
      print('[PERSIST] Error clearing state: $e');
    }
  }

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
          // Escalation Banner - chỉ hiển thị khi đang chờ trong queue
          if (_isEscalated && _isWaitingInQueue)
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
        constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.85),
        decoration: BoxDecoration(
          color: isUser 
              ? AppColors.brandColor 
              : (message.isEscalation 
                  ? Colors.orange[100] 
                  : (message.type == ChatMessageType.waiting 
                      ? Colors.blue[50] 
                      : Colors.grey[100])),
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
            // Header cho tin nhắn chuyển tiếp
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
            
            // Nội dung tin nhắn
            Text(
              message.text,
              style: TextStyle(
                color: isUser ? Colors.white : AppColors.textPrimary,
                fontSize: 15,
                height: 1.4,
              ),
            ),
            
            // Waiting indicator với queue position - MoMo style
            if (message.type == ChatMessageType.waiting && message.queuePosition != null)
              Padding(
                padding: const EdgeInsets.only(top: 16),
                child: Column(
                  children: [
                    // Loading dots
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: List.generate(3, (index) => Container(
                        margin: const EdgeInsets.symmetric(horizontal: 3),
                        width: 8,
                        height: 8,
                        decoration: BoxDecoration(
                          color: Colors.grey[400],
                          shape: BoxShape.circle,
                        ),
                      )),
                    ),
                    const SizedBox(height: 16),
                    // Queue position text - centered
                    Text(
                      "Bạn đang ở vị trí số #${message.queuePosition} trong hàng chờ",
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 14,
                        color: Colors.grey[800],
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      "Vui lòng đợi trong giây lát để SLIB hỗ trợ bạn nhé",
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 13,
                        color: Colors.grey[600],
                      ),
                    ),
                  ],
                ),
              ),
            
            // Action buttons - MoMo style
            if (message.actions != null && message.actions!.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: message.actions!.map((action) => Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: _buildActionButton(action, message),
                  )).toList(),
                ),
              ),
          ],
        ),
      ),
    );
  }

  // Widget: Action Button - MoMo Style
  Widget _buildActionButton(ChatAction action, ChatMessage message) {
    // MoMo pink color
    const Color momoColor = Color(0xFFE91E63); // Pink/Magenta
    
    bool isPrimary = action.isPrimary;
    bool isCancel = action.id == 'cancel_queue' || action.id == 'contact_later';
    
    // Primary: filled pink, Secondary: outlined pink, Cancel: text only
    Color bgColor;
    Color textColor;
    Color? borderColor;
    
    if (isCancel) {
      bgColor = Colors.transparent;
      textColor = momoColor;
      borderColor = null;
    } else if (isPrimary) {
      bgColor = momoColor;
      textColor = Colors.white;
      borderColor = momoColor;
    } else {
      bgColor = Colors.white;
      textColor = momoColor;
      borderColor = momoColor;
    }
    
    return Material(
      color: bgColor,
      borderRadius: BorderRadius.circular(25),
      child: InkWell(
        onTap: () => _handleActionTap(action.id),
        borderRadius: BorderRadius.circular(25),
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(25),
            border: borderColor != null ? Border.all(color: borderColor, width: 1.5) : null,
          ),
          child: Text(
            action.label,
            textAlign: TextAlign.center,
            style: TextStyle(
              color: textColor,
              fontWeight: FontWeight.w600,
              fontSize: 15,
            ),
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

  // Xử lý gửi tin nhắn & Gọi AI Service
  void _handleSubmitted(String text) async {
    if (text.trim().isEmpty) return;

    _textController.clear();
    setState(() {
      _messages.add(ChatMessage(text: text, isUser: true, time: DateTime.now()));
      _isTyping = !_isEscalated; // Chỉ hiện typing khi chat với AI
    });

    // Cuon xuong cuoi
    _scrollToBottom();

    // Nếu đang chat với librarian - gửi tin nhắn đến backend
    if (_isEscalated && _conversationId != null && !_isWaitingInQueue) {
      try {
        final authService = Provider.of<AuthService>(context, listen: false);
        final token = await authService.getToken();
        if (token != null) {
          await _chatService.sendMessageToBackend(
            conversationId: _conversationId!,
            content: text,
            senderType: 'STUDENT',
            authToken: token,
          );
        }
      } catch (e) {
        print('Error sending message to backend: $e');
      }
      return;
    }

    // Local detection cho intent "gap thu thu"
    final lowerText = text.toLowerCase();
    final wantsLibrarian = lowerText.contains('gap thu thu') || 
                           lowerText.contains('gặp thủ thư') ||
                           lowerText.contains('noi chuyen voi thu thu') ||
                           lowerText.contains('nói chuyện với thủ thư') ||
                           lowerText.contains('lien he thu thu') ||
                           lowerText.contains('liên hệ thủ thư') ||
                           lowerText.contains('muon gap nguoi') ||
                           lowerText.contains('muốn gặp người');

    if (wantsLibrarian) {
      // Hien thi message voi action button
      await Future.delayed(const Duration(milliseconds: 800));
      if (!mounted) return;
      
      setState(() {
        _isTyping = false;
        _messages.add(ChatMessage(
          text: "Dạ, để được gặp thủ thư, bạn vui lòng bấm vào nút bên dưới ạ!",
          isUser: false,
          time: DateTime.now(),
          type: ChatMessageType.withActions,
          actions: [
            ChatAction(
              id: 'request_librarian', 
              label: 'Chat với Thủ thư SLIB', 
              icon: '📞',
              isPrimary: true,
            ),
          ],
        ));
      });
      _scrollToBottom();
      return;
    }

    try {
      // Goi AI Service API
      final response = await _chatService.sendMessage(text);
      
      if (!mounted) return;
      
      setState(() {
        _isTyping = false;
        
        // Check neu AI tra ve escalation
        if (response.needsReview || response.escalated) {
          // Hien thi message voi action button
          _messages.add(ChatMessage(
            text: response.reply,
            isUser: false,
            time: DateTime.now(),
            type: ChatMessageType.withActions,
            actions: [
              ChatAction(
                id: 'request_librarian', 
                label: 'Chat với Thủ thư SLIB', 
                icon: '📞',
                isPrimary: true,
              ),
            ],
          ));
        } else {
          // Tin nhan thong thuong
          _messages.add(ChatMessage(
            text: response.reply,
            isUser: false,
            time: DateTime.now(),
            isEscalation: response.escalated,
          ));
        }
        
        // Cap nhat trang thai escalation
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

  // Xu ly khi user tap vao action button
  void _handleActionTap(String actionId) {
    switch (actionId) {
      case 'request_librarian':
        _handleRequestLibrarian();
        break;
      case 'cancel_queue':
        _handleCancelQueue();
        break;
      case 'submit_support_request':
        _handleSubmitSupportRequest();
        break;
      case 'contact_later':
        _handleContactLater();
        break;
    }
  }

  // Yeu cau gap thu thu - gọi API thật
  Future<void> _handleRequestLibrarian() async {
    // Hiển thị UI waiting ngay lập tức
    setState(() {
      _isWaitingInQueue = true;
      _isEscalated = true;
      _queuePosition = 1;
      
      _messages.add(ChatMessage(
        text: "Dạ, SLIB đang kết nối bạn với thủ thư...",
        isUser: false,
        time: DateTime.now(),
        type: ChatMessageType.waiting,
        queuePosition: _queuePosition,
        actions: [
          ChatAction(id: 'cancel_queue', label: 'Không chờ nữa'),
        ],
      ));
    });
    _scrollToBottom();
    
    // Gọi API để tạo conversation và escalate
    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final token = await authService.getToken();
      
      if (token != null) {
        // Build message history để gửi kèm
        final messageHistory = _messages
          .where((m) => m.type == ChatMessageType.text || m.type == ChatMessageType.withActions)
          .map((m) => {
            'content': m.text,
            'isUser': m.isUser,
            'senderType': m.isUser ? 'STUDENT' : 'AI',
          })
          .toList();
        
        final result = await _chatService.requestLibrarian(
          'User yêu cầu gặp thủ thư',
          token,
          messageHistory: messageHistory,
        );
        
        if (result.success && mounted) {
          setState(() {
            _conversationId = result.conversationId;
            _queuePosition = result.queuePosition;
          });
          
          // Save state để có thể restore khi mở lại app
          await _saveState();
          
          // Bắt đầu polling để check khi librarian tiếp nhận
          _startStatusPolling(token);
        }
      } else {
        print('User chưa đăng nhập, không thể tạo conversation');
      }
    } catch (e) {
      print('Error calling requestLibrarian: $e');
    }
  }

  // Polling để check conversation status
  void _startStatusPolling(String token) async {
    while (_isWaitingInQueue && mounted && _conversationId != null) {
      await Future.delayed(const Duration(seconds: 3));
      
      if (!mounted || _conversationId == null) break;
      
      final status = await _chatService.getConversationStatus(_conversationId!, token);
      
      if (status.isHumanChatting && mounted) {
        setState(() {
          _isWaitingInQueue = false;
          _librarianName = status.librarianName;
          
          // Cập nhật message thể hiện đã có người tiếp nhận
          _messages.add(ChatMessage(
            text: "Thủ thư ${status.librarianName.isNotEmpty ? status.librarianName : ''} đã tiếp nhận yêu cầu của bạn. Bạn có thể chat trực tiếp ngay bây giờ!",
            isUser: false,
            time: DateTime.now(),
          ));
        });
        
        // Save updated state (isWaitingInQueue = false)
        await _saveState();
        
        _scrollToBottom();
        // Bắt đầu polling để nhận tin nhắn mới từ librarian
        _startMessagePolling(token);
        break;
      } else if (status.isResolved && mounted) {
        // Conversation đã kết thúc
        _handleChatEnded();
        break;
      }
    }
  }

  // HTTP Polling để nhận tin nhắn mới từ librarian (WebSocket bị lỗi SSL với ngrok)
  void _startMessagePolling(String token) async {
    print('[POLLING] Starting HTTP polling for conversation: $_conversationId');
    
    if (_conversationId == null) {
      print('[POLLING] No conversationId, cannot poll');
      return;
    }

    while (_isEscalated && mounted && _conversationId != null && !_isWaitingInQueue) {
      await Future.delayed(const Duration(seconds: 2));
      
      if (!mounted || _conversationId == null || !_isEscalated) {
        print('[POLLING] Stopping - conditions not met');
        break;
      }
      
      try {
        // Check nếu conversation đã resolved
        final status = await _chatService.getConversationStatus(_conversationId!, token);
        if (status.isResolved) {
          print('[POLLING] Conversation resolved, ending chat');
          _handleChatEnded();
          break;
        }
        
        // Lấy messages mới từ backend
        final backendMessages = await _chatService.getMessages(_conversationId!, token);
        print('[POLLING] Fetched ${backendMessages.length} messages, tracked IDs: ${_messageIds.length}');
        
        if (mounted && backendMessages.isNotEmpty) {
          bool hasNewMessages = false;
          
          for (final msg in backendMessages) {
            final msgId = msg['id']?.toString() ?? '';
            final content = msg['content'] as String? ?? '';
            final senderType = msg['senderType'] as String? ?? 'STUDENT';
            
            // DEBUG: In ra tất cả messages để kiểm tra
            print('[POLLING] MSG: id=$msgId, senderType=$senderType, content=${content.length > 30 ? content.substring(0, 30) : content}...');
            
            // Only add if not already tracked
            if (msgId.isNotEmpty && !_messageIds.contains(msgId)) {
              _messageIds.add(msgId);
              
              // Only add LIBRARIAN messages to UI (STUDENT messages are already local)
              if (senderType == 'LIBRARIAN' && content.isNotEmpty) {
                print('[POLLING] >>> NEW librarian message: $content');
                hasNewMessages = true;
                setState(() {
                  _messages.add(ChatMessage(
                    text: content,
                    isUser: false,
                    time: DateTime.now(),
                  ));
                });
              }
            }
          }
          
          if (hasNewMessages) {
            _scrollToBottom();
          }
        }
      } catch (e) {
        print('[POLLING] Error: $e');
      }
    }
    print('[POLLING] Polling ended');
  }

  // Huy cho trong queue
  void _handleCancelQueue() {
    setState(() {
      _isWaitingInQueue = false;
      
      _messages.add(ChatMessage(
        text: "Dạ, mong bạn thông cảm vì SLIB vẫn chưa hỗ trợ được bạn trực tiếp. Bạn có thể:",
        isUser: false,
        time: DateTime.now(),
        type: ChatMessageType.withActions,
        actions: [
          ChatAction(id: 'submit_support_request', label: 'Gửi yêu cầu hỗ trợ', icon: '📝', isPrimary: true),
          ChatAction(id: 'contact_later', label: 'Tôi sẽ liên hệ sau', icon: '🔙'),
        ],
      ));
    });
    _scrollToBottom();
  }

  // Gui yeu cau ho tro - chuyen den trang Q&A
  void _handleSubmitSupportRequest() {
    // TODO: Navigate to Support Request page
    setState(() {
      _messages.add(ChatMessage(
        text: "Dạ, SLIB sẽ chuyển bạn đến trang gửi yêu cầu hỗ trợ. Thủ thư sẽ phản hồi sớm nhất có thể!",
        isUser: false,
        time: DateTime.now(),
      ));
    });
    _scrollToBottom();
    
    // TODO: Navigator.push to Support Request Screen
  }

  // Lien he sau
  void _handleContactLater() {
    setState(() {
      _messages.add(ChatMessage(
        text: "Dạ vâng ạ, khi nào bạn cần hỗ trợ thì SLIB luôn sẵn sàng giúp đỡ bạn nhé! Bạn còn cần SLIB hỗ trợ gì thêm không ạ?",
        isUser: false,
        time: DateTime.now(),
      ));
    });
    _scrollToBottom();
  }

  // Xử lý khi librarian kết thúc cuộc chat
  void _handleChatEnded() async {
    // Clear saved state
    await _clearSavedState();
    
    setState(() {
      _isEscalated = false;
      _isWaitingInQueue = false;
      _conversationId = null;
      _librarianName = null;
      _messageIds.clear();
      
      _messages.add(ChatMessage(
        text: "Thủ thư đã kết thúc cuộc trò chuyện. Cảm ơn bạn đã liên hệ!\n\nMình là trợ lý ảo SLIB, sẵn sàng tiếp tục hỗ trợ bạn. Bạn cần mình giúp gì thêm không ạ? 📚",
        isUser: false,
        time: DateTime.now(),
      ));
    });
    print('[CHAT] Librarian ended conversation, reset to AI mode');
    _scrollToBottom();
  }

  void _resetConversation() async {
    // Clear saved state trước
    await _clearSavedState();
    
    setState(() {
      _messages.clear();
      _messageIds.clear();
      _messages.add(ChatMessage(
        text: "Chào bạn! Mình là trợ lý ảo SLIB. Mình có thể giúp gì cho việc học tập của bạn hôm nay? 📚",
        isUser: false,
        time: DateTime.now(),
      ));
      _isEscalated = false;
      _isWaitingInQueue = false;
      _conversationId = null;
      _librarianName = null;
      _queuePosition = 0;
      _chatService.clearSession();
    });
  }
}

// Model tin nhắn
enum ChatMessageType {
  text,           // Tin nhắn thông thường
  withActions,    // Tin nhắn có action buttons
  waiting,        // Đang chờ trong queue
}

class ChatAction {
  final String id;
  final String label;
  final String? icon;
  final bool isPrimary;

  ChatAction({
    required this.id,
    required this.label,
    this.icon,
    this.isPrimary = false,
  });
}

class ChatMessage {
  final String text;
  final bool isUser;
  final DateTime time;
  final bool isEscalation;
  final ChatMessageType type;
  final List<ChatAction>? actions;
  final int? queuePosition;

  ChatMessage({
    required this.text, 
    required this.isUser, 
    required this.time,
    this.isEscalation = false,
    this.type = ChatMessageType.text,
    this.actions,
    this.queuePosition,
  });
}