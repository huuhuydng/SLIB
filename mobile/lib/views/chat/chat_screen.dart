import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:image_picker/image_picker.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/services/auth/auth_service.dart';
import 'package:slib/services/chat/chat_service.dart';
import 'package:slib/services/chat/chat_websocket_service.dart';
import 'package:slib/views/support/support_request_screen.dart';
import 'package:slib/services/notification/notification_service.dart';
import 'package:slib/views/widgets/error_display_widget.dart';

// --- CẤU HÌNH MÀU SẮC ---

class ChatScreen extends StatefulWidget {
  const ChatScreen({super.key});

  @override
  State<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> with TickerProviderStateMixin {
  final TextEditingController _textController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final ChatService _chatService = ChatService();
  final ChatWebSocketService _chatWsService = ChatWebSocketService();
  final FlutterSecureStorage _chatStateStorage = const FlutterSecureStorage();
  AnimationController? _dotAnimController;

  bool _isTyping = false; // Trang thai Bot dang go
  bool _isEscalated = false; // Da chuyen sang thu thu
  bool _isWaitingInQueue = false; // Dang cho trong queue
  bool _isHandlingChatEnd =
      false; // Guard against concurrent _handleChatEnded calls
  int _queuePosition = 0; // Vi tri trong hang doi
  String? _conversationId; // ID conversation khi escalate
  String? _librarianName; // Ten thu thu khi tiep nhan
  final Set<String> _messageIds = {}; // Track message IDs từ backend

  // Dữ liệu tin nhắn
  final List<ChatMessage> _messages = [
    ChatMessage(
      text:
          "Chào bạn! Mình là trợ lý ảo SLIB. Mình có thể giúp gì cho việc học tập của bạn hôm nay?",
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
  static const String _keyMessages = 'chat_messages';
  static const String _keyUserId = 'chat_user_id';

  Future<String?> _readChatState(String key) =>
      _chatStateStorage.read(key: key);

  Future<void> _writeChatState(String key, String value) =>
      _chatStateStorage.write(key: key, value: value);

  Future<void> _writeChatStateBool(String key, bool value) =>
      _chatStateStorage.write(key: key, value: value.toString());

  Future<bool> _readChatStateBool(
    String key, {
    bool defaultValue = false,
  }) async {
    final value = await _chatStateStorage.read(key: key);
    if (value == null) return defaultValue;
    return value.toLowerCase() == 'true';
  }

  Future<void> _removeChatStateKeys(List<String> keys) async {
    for (final key in keys) {
      await _chatStateStorage.delete(key: key);
    }
  }

  bool _isLoadingState = true; // Loading indicator
  bool _userCancelledQueue = false; // Flag: user chủ động hủy queue

  // Feedback after chat ends
  String? _feedbackConversationId;
  int _chatFeedbackRating = 0;
  bool _chatFeedbackSubmitted = false;
  bool _showScrollToBottom = false; // Hiện nút scroll xuống cuối
  bool _isLoadingMore = false; // Đang load thêm tin nhắn cũ
  int _currentPage = 0; // Trang hiện tại (pagination)
  bool _hasMorePages = true; // Còn trang nào chưa load
  static const int _pageSize = 20; // Số tin nhắn mỗi trang
  double _reloadSwipeDistance = 0;
  double _reloadSwipeProgress = 0;
  double _reloadSwipeVisualOffset = 0;
  bool _showReloadSwipeHint = false;
  bool _isReloadingFromSwipe = false;
  bool _didReachReloadThreshold = false;
  int? _activeReloadPointerId;
  double? _reloadSwipeLastY;
  static const double _reloadSwipeThreshold = 110;
  static const double _reloadSwipeMaxVisualOffset = 96;

  // Polling guards - prevent overlapping loops
  bool _isAIPollingActive = false;
  bool _isStatusPollingActive = false;
  bool _isMessagePollingActive = false;

  @override
  void initState() {
    super.initState();
    _dotAnimController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    )..repeat();
    // Scroll listener: hiện nút scroll-to-bottom khi user cuộn lên xa
    _scrollController.addListener(_onScrollChanged);
    _scrollController.addListener(_onScrollUp);
    // Suppress push notification khi đang ở chat screen
    WidgetsBinding.instance.addPostFrameCallback((_) {
      Provider.of<NotificationService>(
        context,
        listen: false,
      ).isChatScreenActive = true;
    });
    _loadSavedState();
  }

  /// Load saved conversation state từ SharedPreferences
  Future<void> _loadSavedState() async {
    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      String? savedConversationId = await _readChatState(_keyConversationId);
      final savedLibrarianName = await _readChatState(_keyLibrarianName);
      final savedIsWaiting = await _readChatStateBool(_keyIsWaitingInQueue);

      debugPrint(
        '[PERSIST] Loading state: convId=$savedConversationId, waiting=$savedIsWaiting',
      );

      final token = await authService.getToken();

      if (token == null) {
        debugPrint('[PERSIST] No auth token, loading local messages only');
        await _loadLocalMessages();
        return;
      }

      // QUAN TRỌNG: LUÔN gọi backend để xác định active conversation
      // Backend filter theo JWT user → không bao giờ trả conversation của user khác
      String? activeConversationId;
      String? activeLibrarianName;
      bool isFromBackend =
          false; // true = conversation xác nhận bởi backend, false = chỉ là AI session local

      final activeConv = await _chatService.getMyActiveConversation(token);
      if (activeConv != null && activeConv['hasActive'] == true) {
        activeConversationId = activeConv['conversationId'];
        activeLibrarianName =
            activeConv['librarianName'] ?? savedLibrarianName ?? '';
        isFromBackend = true;
        debugPrint(
          '[PERSIST] Backend returned active conversation: $activeConversationId, status: ${activeConv['status']}',
        );
      } else if (savedConversationId != null) {
        // Backend nói KHÔNG có active conversation → kiểm tra saved AI session
        // Chỉ dùng nếu _keyUserId đã lưu (code mới) — tránh dùng data user khác
        final savedUserId = await _readChatState(_keyUserId);
        if (savedUserId != null) {
          activeConversationId = savedConversationId;
          isFromBackend = false;
          debugPrint(
            '[PERSIST] No active backend conversation, using saved AI session (userId=$savedUserId): $activeConversationId',
          );
        } else {
          // Không có savedUserId → data cũ, không tin tưởng → clear conversation state nhưng giữ messages
          debugPrint(
            '[PERSIST] No savedUserId found, clearing untrusted conversation state (keeping messages)',
          );
          await _removeChatStateKeys([
            _keyConversationId,
            _keyIsEscalated,
            _keyLibrarianName,
            _keyIsWaitingInQueue,
          ]);
          // KHÔNG xóa _keyMessages — giữ lại tin nhắn cũ
        }
      } else {
        debugPrint('[PERSIST] No active conversation found');
      }

      if (activeConversationId != null && isFromBackend) {
        // Conversation xác nhận bởi backend (thuộc về user hiện tại) → check status
        final status = await _chatService.getConversationStatus(
          activeConversationId,
          token,
        );
        debugPrint(
          '[PERSIST] Backend status: ${status.status}, isHumanChatting: ${status.isHumanChatting}',
        );

        if (status.isHumanChatting) {
          // Database = HUMAN_CHATTING -> user PHẢI chat với thủ thư, KHÔNG với AI
          setState(() {
            _conversationId = activeConversationId;
            _isEscalated = true;
            _isWaitingInQueue = false;
            _librarianName = activeLibrarianName ?? status.librarianName;
          });

          await _loadMessagesFromBackend(activeConversationId, token);
          await _saveState();

          // Subscribe WebSocket để nhận tin nhắn realtime + typing indicator
          _chatWsService.setOnMessageReceived((msgData) {
            if (!mounted) return;
            final type = msgData['type'] as String?;

            if (type == 'TYPING' || type == 'MESSAGES_READ') return;

            final msgId = msgData['id']?.toString();
            if (msgId != null && _messageIds.contains(msgId)) return;
            if (msgId != null) _messageIds.add(msgId);

            final senderType = msgData['senderType'] as String? ?? '';
            if (senderType == 'LIBRARIAN') {
              _dismissChatFeedbackCard();
              final msgContent = msgData['content'] ?? '';
              final parsedMessage = _parseMessageContent(
                msgContent,
                attachmentUrl: msgData['attachmentUrl']?.toString(),
              );
              setState(() {
                _messages.add(
                  ChatMessage(
                    text: parsedMessage.text,
                    isUser: false,
                    time: DateTime.now(),
                    isFromLibrarian: true,
                    imageUrls: parsedMessage.imageUrls.isNotEmpty
                        ? parsedMessage.imageUrls
                        : null,
                  ),
                );
              });
              _scrollToBottom();
              _saveMessages();
            }
          });
          _chatWsService.subscribeToConversation(activeConversationId);
          _startMessagePolling(token);
        } else if (status.isWaiting) {
          // Đang chờ trong hàng đợi
          setState(() {
            _conversationId = activeConversationId;
            _isEscalated = true;
            _isWaitingInQueue = true;
            _queuePosition = status.queuePosition > 0
                ? status.queuePosition
                : 1;
          });

          await _loadMessagesFromBackend(activeConversationId, token);

          // Re-add queue waiting indicator message (UI-only, not stored in backend)
          setState(() {
            // Remove any existing waiting messages first
            _messages.removeWhere((m) => m.type == ChatMessageType.waiting);
            _messages.add(
              ChatMessage(
                text: "",
                isUser: false,
                time: DateTime.now(),
                type: ChatMessageType.waiting,
                queuePosition: _queuePosition,
                actions: [
                  ChatAction(id: 'cancel_queue', label: 'Không chờ nữa'),
                ],
              ),
            );
          });
          _scrollToBottom();

          await _saveState();
          _connectWebSocketForQueue(token);
        } else {
          // AI_HANDLING hoặc RESOLVED -> user chat với bot bình thường
          // Load messages từ backend (có pagination)
          await _loadMessagesFromBackendPaginated(activeConversationId, token);
          if (mounted) {
            setState(() {
              _conversationId = activeConversationId;
              _isEscalated = false;
              _isWaitingInQueue = false;
              _librarianName = null;
            });
          }
          debugPrint(
            '[PERSIST] Session is in AI mode, keeping session ID: $activeConversationId',
          );
        }
      } else if (activeConversationId != null && !isFromBackend) {
        // Saved AI session (không có active conversation trên backend)
        // Thử load từ backend trước, fallback sang local
        try {
          await _loadMessagesFromBackendPaginated(activeConversationId, token);
        } catch (_) {
          await _loadLocalMessages();
        }
        if (mounted) {
          setState(() {
            _conversationId = activeConversationId;
            _isEscalated = false;
            _isWaitingInQueue = false;
            _librarianName = null;
          });
        }
        debugPrint('[PERSIST] Loaded local AI session: $activeConversationId');
      } else {
        // Không có conversation nào → load local messages nếu có
        debugPrint('[PERSIST] No active conversation found');
        // Tạo conversation mới cho AI chat
        try {
          final convId = await _chatService.getOrCreateConversation(token);
          if (convId != null && mounted) {
            setState(() => _conversationId = convId);
            // Save conversation ID cho lần sau
            await _writeChatState(_keyConversationId, convId);
            // Load messages (nếu có từ lần trước)
            await _loadMessagesFromBackendPaginated(convId, token);
          } else {
            await _loadLocalMessages();
          }
        } catch (e) {
          debugPrint('[PERSIST] Error creating conversation: $e');
          await _loadLocalMessages();
        }
      }

      // LUÔN subscribe student topic WebSocket để nhận LIBRARIAN_JOINED
      // (kể cả khi đang ở AI mode - thủ thư có thể chat từ yêu cầu hỗ trợ)
      if (!_isWaitingInQueue) {
        _connectStudentTopicAlways(token);
      }
    } catch (e) {
      debugPrint('[PERSIST] Error loading state: $e');
      // Fallback: luôn thử load local messages khi có lỗi
      await _loadLocalMessages();
    } finally {
      if (mounted) {
        setState(() {
          _isLoadingState = false;
        });
      }
    }
  }

  /// Load messages từ backend và thêm vào list
  /// [keepExisting] = true: giữ messages local, chỉ thêm messages mới từ backend
  /// [keepExisting] = false: xóa tất cả, load lại từ đầu
  Future<void> _loadMessagesFromBackend(
    String conversationId,
    String token, {
    bool keepExisting = false,
  }) async {
    try {
      final backendMessages = await _chatService.getMessages(
        conversationId,
        token,
      );
      debugPrint(
        '[PERSIST] Loaded ${backendMessages.length} messages from backend (keepExisting=$keepExisting)',
      );

      if (mounted && backendMessages.isNotEmpty) {
        setState(() {
          if (!keepExisting) {
            // Clear default message
            _messages.clear();
            _messageIds.clear();
          }

          for (final msg in backendMessages) {
            final msgId = msg['id']?.toString() ?? '';
            final senderType = msg['senderType'] as String? ?? 'STUDENT';
            final parsedMessage = _buildChatMessageFromBackend(msg);

            if (msgId.isNotEmpty &&
                !_messageIds.contains(msgId) &&
                parsedMessage != null) {
              _messageIds.add(msgId);
              // Khi keepExisting, skip student messages (đã có local)
              if (keepExisting && senderType == 'STUDENT') continue;
              // Khi keepExisting, chỉ skip AI messages trùng nội dung với local
              // SYSTEM support-request context cần hiện lại theo từng human session
              if (keepExisting && senderType == 'AI') {
                final isDuplicate = _messages.any(
                  (m) =>
                      !m.isUser &&
                      m.text == parsedMessage.text &&
                      m.type == parsedMessage.type,
                );
                if (isDuplicate) continue;
              }

              _messages.add(parsedMessage);
            }
          }
        });
        _scrollToBottom();

        // Mark messages as read (student đánh dấu tin librarian là đã đọc)
      }
    } catch (e) {
      debugPrint('[PERSIST] Error loading messages: $e');
    }
  }

  /// Load messages từ backend với phân trang (page 0 = mới nhất)
  Future<void> _loadMessagesFromBackendPaginated(
    String conversationId,
    String token,
  ) async {
    try {
      _currentPage = 0;
      _hasMorePages = true;

      final result = await _chatService.getMessagesPaginated(
        conversationId,
        token,
        page: 0,
        size: _pageSize,
      );

      final List<dynamic> content = result['content'] ?? [];
      final bool isLast = result['last'] == true;

      debugPrint(
        '[PERSIST] Loaded ${content.length} messages from backend (paginated, isLast=$isLast)',
      );

      if (mounted && content.isNotEmpty) {
        setState(() {
          _messages.clear();
          _messageIds.clear();
          _hasMorePages = !isLast;

          // Content trả về DESC (mới nhất trước), cần reverse lại để hiển thị ASC
          for (final msg in content.reversed) {
            final msgId = msg['id']?.toString() ?? '';
            final parsedMessage = _buildChatMessageFromBackend(msg);

            if (msgId.isNotEmpty) _messageIds.add(msgId);
            if (parsedMessage == null) continue;

            _messages.add(parsedMessage);
          }
        });
        _scrollToBottom();
        _saveMessages(); // Cache locally too
      } else if (mounted && content.isEmpty) {
        // No messages from backend - keep default greeting or load local
        await _loadLocalMessages();
      }
    } catch (e) {
      debugPrint('[PERSIST] Error loading paginated messages: $e');
      await _loadLocalMessages(); // Fallback
    }
  }

  /// Phát hiện khi thủ thư join chat từ yêu cầu hỗ trợ
  /// Polling đơn giản, đáng tin cậy qua bất kỳ network nào
  void _connectStudentTopicAlways(String unusedToken) {
    _startAIModeStatusPolling();
  }

  /// Polling check status mỗi 2 giây khi ở AI mode
  void _startAIModeStatusPolling() async {
    if (_isAIPollingActive) {
      debugPrint('[AI-POLL] Already active, skipping');
      return;
    }
    _isAIPollingActive = true;
    debugPrint(
      '[AI-POLL] === STARTED === _isEscalated=$_isEscalated, _isWaitingInQueue=$_isWaitingInQueue',
    );
    int pollCount = 0;

    while (mounted && !_isEscalated && !_isWaitingInQueue) {
      await Future.delayed(const Duration(seconds: 2));
      pollCount++;

      if (!mounted || _isEscalated || _isWaitingInQueue) {
        debugPrint(
          '[AI-POLL] Breaking - mounted=$mounted, escalated=$_isEscalated, waiting=$_isWaitingInQueue',
        );
        break;
      }

      try {
        // Lấy token mới mỗi lần để tránh stale token
        final authService = Provider.of<AuthService>(context, listen: false);
        final token = await authService.getToken();
        if (token == null) {
          debugPrint('[AI-POLL] #$pollCount: No token, skipping');
          continue;
        }

        final activeConv = await _chatService.getMyActiveConversation(token);
        final status = activeConv?['status'];

        debugPrint(
          '[AI-POLL] #$pollCount: hasActive=${activeConv != null}, status=$status',
        );

        if (activeConv != null && status == 'HUMAN_CHATTING') {
          final convId = activeConv['conversationId']?.toString();
          final libName = activeConv['librarianName'] as String? ?? '';
          debugPrint(
            '[AI-POLL] >>> DETECTED HUMAN_CHATTING! convId=$convId, libName=$libName',
          );

          if (mounted && !_isEscalated) {
            await _handleLibrarianJoinedFromSupportRequest(
              libName,
              convId,
              token,
            );
          }
          break;
        }
      } catch (e) {
        debugPrint('[AI-POLL] #$pollCount: Error: $e');
      }
    }
    _isAIPollingActive = false;
    debugPrint('[AI-POLL] === ENDED === pollCount=$pollCount');
  }

  /// Xử lý khi thủ thư bắt đầu chat từ yêu cầu hỗ trợ (không qua queue)
  /// Giữ nguyên messages local, load messages từ backend
  Future<void> _handleLibrarianJoinedFromSupportRequest(
    String librarianName,
    String? convId,
    String token,
  ) async {
    if (!mounted) return;
    _dismissChatFeedbackCard();

    // Cập nhật conversationId nếu có
    final targetConvId = convId ?? _conversationId;
    if (targetConvId == null) {
      debugPrint('[CHAT] No conversationId for librarian joined event');
      return;
    }

    // Reset feedback flag for new human session
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('chat_feedback_$targetConvId');
    _isHandlingChatEnd = false;

    // Clear stale message IDs từ session cũ
    _messageIds.clear();

    setState(() {
      _conversationId = targetConvId;
      _isEscalated = true;
      _isWaitingInQueue = false;
      _librarianName = librarianName;
      _chatFeedbackSubmitted = false;
      _chatFeedbackRating = 0;
    });

    // Thêm notification message
    setState(() {
      _messages.add(
        ChatMessage(
          text:
              "Thủ thư ${librarianName.isNotEmpty ? librarianName : ''} đang phản hồi yêu cầu hỗ trợ của bạn",
          isUser: false,
          time: DateTime.now(),
        ),
      );
    });

    // Load messages đã có từ backend (bao gồm messages librarian đã gửi trước)
    await _loadMessagesFromBackend(targetConvId, token, keepExisting: true);

    await _saveState();
    _scrollToBottom();

    // Start message polling for real-time updates
    _startMessagePolling(token);
  }

  /// Save conversation state vào SharedPreferences
  Future<void> _saveState() async {
    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      if (_conversationId != null) {
        await _writeChatState(_keyConversationId, _conversationId!);
        await _writeChatStateBool(_keyIsEscalated, _isEscalated);
        await _writeChatStateBool(_keyIsWaitingInQueue, _isWaitingInQueue);
        if (_librarianName != null) {
          await _writeChatState(_keyLibrarianName, _librarianName!);
        }
        // Lưu userId để validate khi load lại
        final currentUserId = authService.currentUser?.id;
        if (currentUserId != null) {
          await _writeChatState(_keyUserId, currentUserId);
        }
        debugPrint(
          '[PERSIST] State saved: convId=$_conversationId, escalated=$_isEscalated, userId=$currentUserId',
        );
      }
    } catch (e) {
      debugPrint('[PERSIST] Error saving state: $e');
    }
  }

  /// Save messages vào SharedPreferences (cho AI chat)
  Future<void> _saveMessages() async {
    try {
      final messagesJson = _messages.map((m) => m.toJson()).toList();
      await _writeChatState(_keyMessages, jsonEncode(messagesJson));
    } catch (e) {
      debugPrint('[PERSIST] Error saving messages: $e');
    }
  }

  /// Load messages từ SharedPreferences (cho AI chat)
  Future<void> _loadLocalMessages() async {
    try {
      final savedMessages = await _readChatState(_keyMessages);
      if (savedMessages != null && savedMessages.isNotEmpty) {
        final List<dynamic> decoded = jsonDecode(savedMessages);
        if (mounted && decoded.isNotEmpty) {
          setState(() {
            _messages.clear();
            _messages.addAll(
              decoded.map((m) => ChatMessage.fromJson(m)).toList(),
            );
          });
          _scrollToBottom();
          debugPrint('[PERSIST] Loaded ${_messages.length} local messages');
        }
      }
    } catch (e) {
      debugPrint('[PERSIST] Error loading local messages: $e');
    }
  }

  /// Clear saved state (khi conversation kết thúc hoặc reset)
  Future<void> _clearSavedState() async {
    try {
      await _removeChatStateKeys([
        _keyConversationId,
        _keyIsEscalated,
        _keyLibrarianName,
        _keyIsWaitingInQueue,
        _keyMessages,
        _keyUserId,
      ]);
      debugPrint('[PERSIST] State cleared');
    } catch (e) {
      debugPrint('[PERSIST] Error clearing state: $e');
    }
  }

  Future<void> _reloadChatState() async {
    if (_isLoadingState) return;

    setState(() {
      _isLoadingState = true;
    });

    _isAIPollingActive = false;
    _isStatusPollingActive = false;
    _isMessagePollingActive = false;

    _chatWsService.disconnect();
    _messageIds.clear();

    try {
      await _loadSavedState();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đã làm mới trạng thái trò chuyện')),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Không thể làm mới hội thoại: $e')),
      );
      setState(() {
        _isLoadingState = false;
      });
    }
  }

  bool _canStartReloadSwipe() {
    if (_isLoadingState || _isReloadingFromSwipe) return false;
    if (!_scrollController.hasClients) return true;
    return _scrollController.offset <= 8;
  }

  void _handleReloadPointerDown(PointerDownEvent event) {
    if (!_canStartReloadSwipe() || _activeReloadPointerId != null) return;

    _activeReloadPointerId = event.pointer;
    _reloadSwipeLastY = event.position.dy;
    _reloadSwipeDistance = 0;
    _reloadSwipeProgress = 0;
    _reloadSwipeVisualOffset = 0;
    _showReloadSwipeHint = false;
    _didReachReloadThreshold = false;
  }

  void _handleReloadPointerMove(PointerMoveEvent event) {
    if (event.pointer != _activeReloadPointerId || _isReloadingFromSwipe) {
      return;
    }

    final lastY = _reloadSwipeLastY;
    if (lastY == null) return;

    _reloadSwipeLastY = event.position.dy;

    final delta = lastY - event.position.dy;
    final nextDistance = (_reloadSwipeDistance + delta).clamp(
      0.0,
      _reloadSwipeThreshold,
    );
    final nextProgress = (nextDistance / _reloadSwipeThreshold).clamp(0.0, 1.0);

    final crossedThreshold = nextProgress >= 1 && !_didReachReloadThreshold;
    setState(() {
      _reloadSwipeDistance = nextDistance;
      _reloadSwipeProgress = nextProgress;
      _reloadSwipeVisualOffset = nextDistance.clamp(
        0.0,
        _reloadSwipeMaxVisualOffset,
      );
      _showReloadSwipeHint = nextDistance > 8;
      if (crossedThreshold) {
        _didReachReloadThreshold = true;
      }
    });

    if (crossedThreshold) {
      HapticFeedback.mediumImpact();
    }
  }

  Future<void> _endReloadSwipe() async {
    if (_isReloadingFromSwipe) return;

    final shouldReload = _showReloadSwipeHint && _reloadSwipeProgress >= 1;

    _activeReloadPointerId = null;
    _reloadSwipeLastY = null;

    if (!shouldReload) {
      if (!_showReloadSwipeHint && _reloadSwipeProgress == 0) return;
      setState(() {
        _reloadSwipeDistance = 0;
        _reloadSwipeProgress = 0;
        _reloadSwipeVisualOffset = 0;
        _showReloadSwipeHint = false;
        _didReachReloadThreshold = false;
      });
      return;
    }

    setState(() {
      _isReloadingFromSwipe = true;
      _reloadSwipeDistance = _reloadSwipeThreshold;
      _reloadSwipeProgress = 1;
      _reloadSwipeVisualOffset = _reloadSwipeMaxVisualOffset * 0.9;
    });

    try {
      await _reloadChatState();
    } finally {
      if (mounted) {
        setState(() {
          _reloadSwipeDistance = 0;
          _reloadSwipeProgress = 0;
          _reloadSwipeVisualOffset = 0;
          _showReloadSwipeHint = false;
          _isReloadingFromSwipe = false;
          _didReachReloadThreshold = false;
        });
      }
    }
  }

  Widget _buildReloadSwipeIndicator() {
    final progress = _isReloadingFromSwipe ? null : _reloadSwipeProgress;
    final statusText = _isReloadingFromSwipe
        ? 'Đang làm mới cuộc trò chuyện...'
        : _reloadSwipeProgress >= 1
        ? 'Thả tay để làm mới cuộc trò chuyện'
        : 'Vuốt lên để làm mới cuộc trò chuyện';

    return IgnorePointer(
      child: AnimatedSlide(
        duration: const Duration(milliseconds: 180),
        curve: Curves.easeOut,
        offset: Offset(
          0,
          _isReloadingFromSwipe ? 0 : 0.08 * (1 - _reloadSwipeProgress),
        ),
        child: AnimatedOpacity(
          duration: const Duration(milliseconds: 140),
          opacity: 1,
          child: Padding(
            padding: const EdgeInsets.only(top: 12, bottom: 4),
            child: Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  SizedBox(
                    width: 48,
                    height: 48,
                    child: Stack(
                      alignment: Alignment.center,
                      children: [
                        SizedBox(
                          width: 48,
                          height: 48,
                          child: CircularProgressIndicator(
                            value: progress,
                            strokeWidth: 4,
                            backgroundColor: Colors.grey[300],
                            valueColor: const AlwaysStoppedAnimation<Color>(
                              AppColors.brandColor,
                            ),
                          ),
                        ),
                        Icon(
                          _isReloadingFromSwipe
                              ? Icons.refresh_rounded
                              : _reloadSwipeProgress >= 1
                              ? Icons.lock_open_rounded
                              : Icons.arrow_upward_rounded,
                          color: AppColors.brandColor,
                          size: 22,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 10),
                  Text(
                    statusText,
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: Colors.grey[700],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  @override
  void dispose() {
    // Bật lại push notification khi rời chat screen
    try {
      Provider.of<NotificationService>(
        context,
        listen: false,
      ).isChatScreenActive = false;
    } catch (_) {}
    // Stop all polling loops by resetting guards
    _isAIPollingActive = false;
    _isStatusPollingActive = false;
    _isMessagePollingActive = false;
    _dotAnimController?.dispose();
    _textController.dispose();
    _scrollController.dispose();
    _chatWsService.disconnect();
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
        shadowColor: Colors.black.withValues(alpha: 0.1),
        title: Row(
          children: [
            // Avatar Bot hoặc Librarian
            Container(
              padding: const EdgeInsets.all(2),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(color: AppColors.brandColor, width: 2),
              ),
              child: _isEscalated
                  ? const CircleAvatar(
                      radius: 18,
                      backgroundColor: AppColors.brandColor,
                      child: Icon(
                        Icons.support_agent,
                        color: Colors.white,
                        size: 20,
                      ),
                    )
                  : const CircleAvatar(
                      radius: 18,
                      backgroundImage: AssetImage('assets/images/ai_ava.png'),
                    ),
            ),
            const SizedBox(width: 12),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  _isEscalated ? "Thủ thư SLIB" : "Trợ thủ AI - SLIB",
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 16,
                  ),
                ),
                Text(
                  _isEscalated
                      ? (_isWaitingInQueue
                            ? "Đang chờ..."
                            : (_librarianName != null &&
                                      _librarianName!.isNotEmpty
                                  ? _librarianName!
                                  : "Đang trực tuyến"))
                      : "Trung tâm hỗ trợ",
                  style: const TextStyle(color: Colors.grey, fontSize: 12),
                ),
              ],
            ),
          ],
        ),
        actions: [
          // Nút kết thúc trò chuyện khi đang chat với thủ thư
          if (_isEscalated && !_isWaitingInQueue && _conversationId != null)
            IconButton(
              icon: const Icon(Icons.call_end_rounded, color: Colors.red),
              tooltip: "Kết thúc trò chuyện",
              onPressed: () {
                showDialog(
                  context: context,
                  builder: (ctx) => AlertDialog(
                    title: const Text('Kết thúc trò chuyện'),
                    content: const Text(
                      'Bạn có muốn kết thúc cuộc trò chuyện với thủ thư không?',
                    ),
                    actions: [
                      TextButton(
                        onPressed: () => Navigator.of(ctx).pop(),
                        child: const Text('Hủy'),
                      ),
                      TextButton(
                        onPressed: () {
                          Navigator.of(ctx).pop();
                          _handleStudentResolve();
                        },
                        child: const Text(
                          'Kết thúc',
                          style: TextStyle(color: Colors.red),
                        ),
                      ),
                    ],
                  ),
                );
              },
            ),
          IconButton(
            icon: const Icon(Icons.restart_alt),
            tooltip: "Bắt đầu cuộc trò chuyện mới",
            onPressed: () {
              showDialog(
                context: context,
                builder: (ctx) => AlertDialog(
                  title: const Text('Xác nhận'),
                  content: const Text(
                    'Bạn có muốn bắt đầu cuộc trò chuyện mới không? Toàn bộ tin nhắn hiện tại sẽ bị xóa.',
                  ),
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.of(ctx).pop(),
                      child: const Text('Hủy'),
                    ),
                    TextButton(
                      onPressed: () {
                        Navigator.of(ctx).pop();
                        _resetConversation();
                      },
                      child: const Text(
                        'Đồng ý',
                        style: TextStyle(color: Colors.red),
                      ),
                    ),
                  ],
                ),
              );
            },
          ),
        ],
      ),
      body: Listener(
        behavior: HitTestBehavior.translucent,
        onPointerDown: _handleReloadPointerDown,
        onPointerMove: _handleReloadPointerMove,
        onPointerUp: (_) => unawaited(_endReloadSwipe()),
        onPointerCancel: (_) => unawaited(_endReloadSwipe()),
        child: Stack(
          children: [
            Column(
              children: [
                // Escalation Banner - chỉ hiển thị khi đang chờ trong queue
                if (_isEscalated && _isWaitingInQueue)
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 10,
                    ),
                    color: Colors.orange[50],
                    child: Row(
                      children: [
                        Icon(
                          Icons.info_outline,
                          color: Colors.orange[700],
                          size: 20,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            "Bạn đã được chuyển đến thủ thư. Vui lòng chờ...",
                            style: TextStyle(
                              color: Colors.orange[800],
                              fontSize: 13,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),

                // 1. Danh sách tin nhắn + scroll-to-bottom button
                Expanded(
                  child: Stack(
                    children: [
                      Transform.translate(
                        offset: Offset(0, -_reloadSwipeVisualOffset),
                        child: Stack(
                          children: [
                            Builder(
                              builder: (context) {
                                final showReloadIndicator =
                                    _showReloadSwipeHint ||
                                    _isReloadingFromSwipe;
                                final reloadItemCount = showReloadIndicator
                                    ? 1
                                    : 0;
                                final typingItemCount = _isTyping ? 1 : 0;
                                final totalMessages = _messages.length;

                                return ListView.builder(
                                  controller: _scrollController,
                                  reverse: true,
                                  physics:
                                      const AlwaysScrollableScrollPhysics(),
                                  padding: const EdgeInsets.all(16),
                                  itemCount:
                                      totalMessages +
                                      typingItemCount +
                                      reloadItemCount +
                                      (_isLoadingMore ? 1 : 0),
                                  itemBuilder: (context, index) {
                                    if (showReloadIndicator && index == 0) {
                                      return _buildReloadSwipeIndicator();
                                    }

                                    final adjustedForReload =
                                        index - reloadItemCount;

                                    if (_isTyping && adjustedForReload == 0) {
                                      return _buildTypingIndicator();
                                    }

                                    final adjustedIndex =
                                        adjustedForReload - typingItemCount;

                                    if (_isLoadingMore &&
                                        adjustedIndex == totalMessages) {
                                      return const Padding(
                                        padding: EdgeInsets.all(16.0),
                                        child: Center(
                                          child: CircularProgressIndicator(
                                            strokeWidth: 2,
                                          ),
                                        ),
                                      );
                                    }

                                    final msgIndex =
                                        totalMessages - 1 - adjustedIndex;
                                    if (msgIndex < 0 ||
                                        msgIndex >= totalMessages) {
                                      return const SizedBox.shrink();
                                    }

                                    final message = _messages[msgIndex];
                                    final supportContext =
                                        message.isFromLibrarian &&
                                            msgIndex > 0 &&
                                            _messages[msgIndex - 1].type ==
                                                ChatMessageType
                                                    .supportRequestContext
                                        ? _messages[msgIndex - 1]
                                        : null;
                                    final shouldHideStandaloneContext =
                                        message.type ==
                                            ChatMessageType
                                                .supportRequestContext &&
                                        msgIndex + 1 < totalMessages &&
                                        _messages[msgIndex + 1].isFromLibrarian;
                                    if (shouldHideStandaloneContext) {
                                      return const SizedBox.shrink();
                                    }

                                    Widget? timeSeparator;
                                    if (msgIndex == 0) {
                                      timeSeparator = _buildTimeSeparator(
                                        message.time,
                                      );
                                    } else {
                                      final prevMessage =
                                          _messages[msgIndex - 1];
                                      final diff = message.time.difference(
                                        prevMessage.time,
                                      );
                                      if (diff.inMinutes.abs() >= 60) {
                                        timeSeparator = _buildTimeSeparator(
                                          message.time,
                                        );
                                      }
                                    }

                                    return Column(
                                      children: [
                                        if (timeSeparator != null)
                                          timeSeparator,
                                        _buildMessageBubble(
                                          message,
                                          supportContext: supportContext,
                                        ),
                                      ],
                                    );
                                  },
                                );
                              },
                            ),
                            // Nút scroll xuống cuối
                            if (_showScrollToBottom)
                              Positioned(
                                right: 16,
                                bottom: 8,
                                child: FloatingActionButton.small(
                                  heroTag: 'scroll_to_bottom',
                                  onPressed: _scrollToBottom,
                                  backgroundColor: AppColors.brandColor,
                                  child: const Icon(
                                    Icons.keyboard_arrow_down,
                                    color: Colors.white,
                                  ),
                                ),
                              ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),

                // 2. Khu vực Gợi ý (Suggestions)
                if (_messages.length < 4 &&
                    !_isEscalated) // Chỉ hiện gợi ý khi hội thoại còn ngắn
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
                              style: const TextStyle(
                                fontSize: 12,
                                color: AppColors.textPrimary,
                              ),
                            ),
                            backgroundColor: Colors.grey[100],
                            side: BorderSide.none,
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(20),
                            ),
                            onPressed: () =>
                                _handleSubmitted(_suggestions[index]),
                          ),
                        );
                      },
                    ),
                  ),

                // 3. Thanh nhập liệu (Input Bar)
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 12,
                  ),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withValues(alpha: 0.05),
                        offset: const Offset(0, -2),
                        blurRadius: 10,
                      ),
                    ],
                  ),
                  child: SafeArea(
                    child: Row(
                      children: [
                        IconButton(
                          icon: const Icon(
                            Icons.add_circle_outline,
                            color: Colors.grey,
                          ),
                          onPressed:
                              _isEscalated &&
                                  _conversationId != null &&
                                  !_isWaitingInQueue
                              ? () => _handlePickImage()
                              : null,
                        ),
                        Expanded(
                          child: TextField(
                            controller: _textController,
                            decoration: InputDecoration(
                              hintText: _isEscalated
                                  ? "Nhắn tin cho thủ thư..."
                                  : "Nhập tin nhắn...",
                              hintStyle: TextStyle(color: Colors.grey[400]),
                              filled: true,
                              fillColor: Colors.grey[100],
                              contentPadding: const EdgeInsets.symmetric(
                                horizontal: 16,
                                vertical: 10,
                              ),
                              border: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(24),
                                borderSide: BorderSide.none,
                              ),
                            ),
                            onChanged: (text) {},
                            onSubmitted: _handleSubmitted,
                          ),
                        ),
                        const SizedBox(width: 8),
                        Container(
                          decoration: BoxDecoration(
                            color: AppColors.brandColor,
                            shape: BoxShape.circle,
                          ),
                          child: IconButton(
                            icon: const Icon(
                              Icons.send_rounded,
                              color: Colors.white,
                              size: 20,
                            ),
                            onPressed: () =>
                                _handleSubmitted(_textController.text),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  // Widget: Time separator giữa các tin nhắn cách nhau > 1 giờ
  Widget _buildTimeSeparator(DateTime time) {
    final formatted = DateFormat('dd/MM/yyyy, HH:mm').format(time);
    return Center(
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 12),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
        decoration: BoxDecoration(
          color: Colors.grey[200],
          borderRadius: BorderRadius.circular(12),
        ),
        child: Text(
          formatted,
          style: TextStyle(fontSize: 11, color: Colors.grey[600]),
        ),
      ),
    );
  }

  ChatMessage? _buildChatMessageFromBackend(Map<String, dynamic> msg) {
    final content = msg['content'] as String? ?? '';
    final attachmentUrl = msg['attachmentUrl'] as String?;
    if (content.isEmpty && (attachmentUrl == null || attachmentUrl.isEmpty)) {
      return null;
    }

    final senderType = msg['senderType'] as String? ?? 'STUDENT';
    final msgTime = _parseBackendMessageTime(msg['createdAt']?.toString());

    if (_isSupportRequestContextMessage(content)) {
      final parsed = _parseSupportRequestContext(content);
      return ChatMessage(
        text: parsed.description,
        isUser: false,
        isFromLibrarian: true,
        time: msgTime,
        type: ChatMessageType.supportRequestContext,
        imageUrls: parsed.imageUrls.isNotEmpty ? parsed.imageUrls : null,
      );
    }

    final parsedContent = _parseMessageContent(
      content,
      attachmentUrl: attachmentUrl,
    );
    return ChatMessage(
      text: parsedContent.text,
      isUser: senderType == 'STUDENT',
      isFromLibrarian: senderType == 'LIBRARIAN',
      time: msgTime,
      imageUrls: parsedContent.imageUrls.isNotEmpty
          ? parsedContent.imageUrls
          : null,
    );
  }

  DateTime _parseBackendMessageTime(String? createdAtStr) {
    if (createdAtStr != null) {
      try {
        return DateTime.parse(createdAtStr);
      } catch (_) {}
    }
    return DateTime.now();
  }

  bool _isSupportRequestContextMessage(String content) {
    return content.trimLeft().startsWith('[YÊU CẦU HỖ TRỢ]');
  }

  ({String text, List<String> imageUrls}) _parseMessageContent(
    String content, {
    String? attachmentUrl,
  }) {
    if (!content.contains('[IMAGES]')) {
      return (
        text: content.trim(),
        imageUrls: _extractImageUrls(attachmentUrl: attachmentUrl),
      );
    }

    final parts = content.split('[IMAGES]');
    final text = parts.first.trim();
    final imageUrls = _extractImageUrls(
      contentBlock: parts.length > 1 ? parts[1] : null,
      attachmentUrl: attachmentUrl,
    );
    return (text: text, imageUrls: imageUrls);
  }

  List<String> _extractImageUrls({
    String? contentBlock,
    String? attachmentUrl,
  }) {
    final urls = <String>[];

    if (contentBlock != null && contentBlock.isNotEmpty) {
      urls.addAll(
        contentBlock
            .trim()
            .split('\n')
            .where((url) => url.trim().startsWith('http'))
            .map((url) => url.trim()),
      );
    }

    final normalizedAttachment = attachmentUrl?.trim();
    if (normalizedAttachment != null &&
        normalizedAttachment.startsWith('http') &&
        !urls.contains(normalizedAttachment)) {
      urls.add(normalizedAttachment);
    }

    return urls.map(_normalizeImageUrlForMobile).toList();
  }

  String _normalizeImageUrlForMobile(String url) {
    final trimmedUrl = url.trim();
    if (!trimmedUrl.startsWith('http')) {
      return trimmedUrl;
    }

    String transformedUrl = trimmedUrl;
    if (trimmedUrl.contains('res.cloudinary.com') &&
        trimmedUrl.contains('/upload/') &&
        !trimmedUrl.contains('/upload/f_auto,q_auto/')) {
      transformedUrl = trimmedUrl.replaceFirst(
        '/upload/',
        '/upload/f_auto,q_auto/',
      );
    }

    final encodedUrl = Uri.encodeComponent(transformedUrl);
    return '${ApiConstants.domain}/slib/files/proxy-image?url=$encodedUrl';
  }

  ({String description, List<String> imageUrls}) _parseSupportRequestContext(
    String content,
  ) {
    final parsedContent = _parseMessageContent(content);
    String description = parsedContent.text
        .replaceFirst('[YÊU CẦU HỖ TRỢ]', '')
        .trim();

    if (description.startsWith('Nội dung:')) {
      description = description.substring('Nội dung:'.length).trim();
    }

    return (description: description, imageUrls: parsedContent.imageUrls);
  }

  // Widget: Bong bóng tin nhắn
  Widget _buildMessageBubble(
    ChatMessage message, {
    ChatMessage? supportContext,
  }) {
    if (message.type == ChatMessageType.feedbackPrompt) {
      return _buildChatFeedbackCard();
    }

    if (message.type == ChatMessageType.supportRequestContext) {
      return _buildSupportRequestContextBubble(message);
    }

    bool isUser = message.isUser;
    bool isWaitingType = message.type == ChatMessageType.waiting;
    bool hasText = message.text.isNotEmpty;
    bool hasImages = message.imageUrls != null && message.imageUrls!.isNotEmpty;

    // Nội dung bubble + extra items
    Widget bubbleContent = IntrinsicWidth(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (supportContext != null)
            Padding(
              padding: const EdgeInsets.only(top: 4, bottom: 6),
              child: _buildSupportRequestCard(supportContext),
            ),
          if (hasText || hasImages)
            Container(
              margin: const EdgeInsets.symmetric(vertical: 4),
              padding: EdgeInsets.fromLTRB(
                16,
                hasText ? 12 : 8,
                16,
                hasText ? 12 : 8,
              ),
              constraints: BoxConstraints(
                maxWidth: MediaQuery.of(context).size.width * 0.7,
              ),
              decoration: BoxDecoration(
                color: isUser
                    ? AppColors.brandColor
                    : (message.isEscalation
                          ? Colors.orange[100]
                          : Colors.grey[100]),
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
                          Icon(
                            Icons.support_agent,
                            size: 14,
                            color: Colors.orange[700],
                          ),
                          const SizedBox(width: 4),
                          Text(
                            "Chuyển tiếp",
                            style: TextStyle(
                              fontSize: 11,
                              color: Colors.orange[700],
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ],
                      ),
                    ),
                  if (hasText)
                    Text(
                      message.text,
                      style: TextStyle(
                        color: isUser ? Colors.white : AppColors.textPrimary,
                        fontSize: 15,
                        height: 1.4,
                      ),
                    ),
                  // Hiện ảnh nếu có
                  if (hasImages)
                    Padding(
                      padding: EdgeInsets.only(top: hasText ? 8 : 0),
                      child: Wrap(
                        spacing: 6,
                        runSpacing: 6,
                        children: message.imageUrls!.asMap().entries.map((
                          entry,
                        ) {
                          final index = entry.key;
                          final url = entry.value;
                          final localImagePath = index == 0
                              ? message.localImagePath
                              : null;
                          return GestureDetector(
                            onTap: () => _showFullImage(
                              url,
                              localImagePath: localImagePath,
                            ),
                            child: ClipRRect(
                              borderRadius: BorderRadius.circular(8),
                              child: _buildChatImageWidget(
                                url: url,
                                localImagePath: localImagePath,
                                width: 180,
                                height: 220,
                                fit: BoxFit.contain,
                              ),
                            ),
                          );
                        }).toList(),
                      ),
                    ),
                ],
              ),
            ),
          if (isWaitingType && message.queuePosition != null)
            _buildQueueWaitingIndicator(message),
          if (message.type == ChatMessageType.withActions &&
              message.actions != null)
            ...message.actions!.map(
              (action) => Padding(
                padding: const EdgeInsets.only(top: 8),
                child: _buildActionButton(action, message),
              ),
            ),
        ],
      ),
    );

    // Tin nhắn user: không có avatar
    if (isUser) {
      return Align(alignment: Alignment.centerRight, child: bubbleContent);
    }

    // Tin nhắn bot/thủ thư: có avatar tròn bên trái
    // Waiting type (queue indicator) -> centered, không có avatar
    if (message.type == ChatMessageType.waiting) {
      return bubbleContent;
    }

    return Padding(
      padding: const EdgeInsets.only(bottom: 2),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          // Avatar tròn nhỏ
          message.isFromLibrarian
              ? const CircleAvatar(
                  radius: 14,
                  backgroundColor: AppColors.brandColor,
                  child: Icon(
                    Icons.support_agent,
                    color: Colors.white,
                    size: 14,
                  ),
                )
              : const CircleAvatar(
                  radius: 14,
                  backgroundImage: AssetImage('assets/images/ai_ava.png'),
                ),
          const SizedBox(width: 8),
          Flexible(child: bubbleContent),
        ],
      ),
    );
  }

  Widget _buildSupportRequestContextBubble(ChatMessage message) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 2),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const CircleAvatar(
            radius: 14,
            backgroundColor: AppColors.brandColor,
            child: Icon(Icons.support_agent, color: Colors.white, size: 14),
          ),
          const SizedBox(width: 8),
          Flexible(
            child: Padding(
              padding: const EdgeInsets.only(top: 4),
              child: _buildSupportRequestCard(message),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSupportRequestCard(ChatMessage message) {
    return Container(
      padding: const EdgeInsets.all(14),
      constraints: BoxConstraints(
        maxWidth: MediaQuery.of(context).size.width * 0.74,
      ),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF8F3),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFFFB88A), width: 1),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 26,
                height: 26,
                decoration: BoxDecoration(
                  color: const Color(0xFFFFE2CC),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Icon(
                  Icons.warning_amber_rounded,
                  color: AppColors.brandColor,
                  size: 18,
                ),
              ),
              const SizedBox(width: 8),
              const Expanded(
                child: Text(
                  'Yêu cầu hỗ trợ từ sinh viên',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w700,
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            'Vấn đề cần hỗ trợ',
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w600,
              color: Colors.grey[700],
            ),
          ),
          const SizedBox(height: 4),
          Text(
            message.text,
            style: const TextStyle(
              fontSize: 14,
              height: 1.45,
              color: AppColors.textPrimary,
            ),
          ),
          if (message.imageUrls != null && message.imageUrls!.isNotEmpty) ...[
            const SizedBox(height: 10),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: message.imageUrls!.asMap().entries.map((entry) {
                final index = entry.key;
                final url = entry.value;
                final localImagePath = index == 0
                    ? message.localImagePath
                    : null;
                return GestureDetector(
                  onTap: () =>
                      _showFullImage(url, localImagePath: localImagePath),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(10),
                    child: _buildChatImageWidget(
                      url: url,
                      localImagePath: localImagePath,
                      width: 110,
                      height: 140,
                      fit: BoxFit.contain,
                    ),
                  ),
                );
              }).toList(),
            ),
          ],
        ],
      ),
    );
  }

  // Widget: Queue waiting indicator - nền trắng, centered giữa màn hình
  Widget _buildQueueWaitingIndicator(ChatMessage message) {
    return Center(
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
        padding: const EdgeInsets.symmetric(vertical: 16),
        child: Column(
          children: [
            _buildWaveDots(),
            const SizedBox(height: 12),
            Text(
              "Bạn đang ở vị trí số #$_queuePosition trong hàng chờ",
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
              style: TextStyle(fontSize: 13, color: Colors.grey[600]),
            ),
            if (message.actions != null)
              ...message.actions!.map(
                (action) => Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: _buildActionButton(action, message),
                ),
              ),
          ],
        ),
      ),
    );
  }

  /// Hiện ảnh full-size trong dialog
  Widget _buildChatImageWidget({
    required String url,
    String? localImagePath,
    required double width,
    required double height,
    BoxFit fit = BoxFit.contain,
  }) {
    final hasLocalFile =
        localImagePath != null && File(localImagePath).existsSync();

    Widget imageWidget;
    if (hasLocalFile) {
      imageWidget = Image.file(
        File(localImagePath),
        width: width,
        height: height,
        fit: fit,
      );
    } else {
      imageWidget = Image.network(
        url,
        width: width,
        height: height,
        fit: fit,
        errorBuilder: (ctx, err, stack) => Container(
          width: width,
          height: height,
          color: Colors.grey[300],
          child: const Icon(Icons.broken_image, color: Colors.grey),
        ),
      );
    }

    return Container(
      width: width,
      height: height,
      color: Colors.white.withValues(alpha: 0.08),
      child: Center(child: imageWidget),
    );
  }

  void _showFullImage(String url, {String? localImagePath}) {
    showDialog(
      context: context,
      builder: (ctx) => Dialog(
        backgroundColor: Colors.transparent,
        child: GestureDetector(
          onTap: () => Navigator.of(ctx).pop(),
          child: InteractiveViewer(
            child: _buildChatImageWidget(
              url: url,
              localImagePath: localImagePath,
              width: double.infinity,
              height: double.infinity,
              fit: BoxFit.contain,
            ),
          ),
        ),
      ),
    );
  }

  // Widget: Action Button - FPT Orange Style
  Widget _buildActionButton(ChatAction action, ChatMessage message) {
    const Color fptOrange = AppColors.brandColor; // FPT Orange

    bool isPrimary = action.isPrimary;
    bool isCancel = action.id == 'cancel_queue' || action.id == 'contact_later';

    if (isCancel) {
      // Cancel: chỉ text màu cam
      return TextButton(
        onPressed: () => _handleActionTap(action.id),
        child: Text(
          action.label,
          style: const TextStyle(
            color: fptOrange,
            fontWeight: FontWeight.w600,
            fontSize: 14,
          ),
        ),
      );
    }

    // Primary: filled orange, rounded rect
    return Material(
      color: isPrimary ? fptOrange : Colors.white,
      borderRadius: BorderRadius.circular(8),
      child: InkWell(
        onTap: () => _handleActionTap(action.id),
        borderRadius: BorderRadius.circular(8),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: fptOrange, width: 1.5),
          ),
          child: Text(
            action.label,
            textAlign: TextAlign.center,
            style: TextStyle(
              color: isPrimary ? Colors.white : fptOrange,
              fontWeight: FontWeight.w600,
              fontSize: 15,
            ),
          ),
        ),
      ),
    );
  }

  // Widget: 3 chấm wave animation - FPT Orange, nhạt dần
  Widget _buildWaveDots() {
    const Color fptOrange = AppColors.brandColor;
    final controller = _dotAnimController;
    if (controller == null) {
      // Fallback: static dots khi chưa init
      return Row(
        mainAxisAlignment: MainAxisAlignment.center,
        mainAxisSize: MainAxisSize.min,
        children: List.generate(
          3,
          (index) => Container(
            margin: const EdgeInsets.symmetric(horizontal: 3),
            width: 8,
            height: 8,
            decoration: BoxDecoration(
              color: fptOrange.withValues(alpha: 1.0 - index * 0.25),
              shape: BoxShape.circle,
            ),
          ),
        ),
      );
    }
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      mainAxisSize: MainAxisSize.min,
      children: List.generate(3, (index) {
        final double opacity = 1.0 - (index * 0.25);
        return AnimatedBuilder(
          animation: controller,
          builder: (context, child) {
            final double delay = index * 0.2;
            final double value = ((controller.value + delay) % 1.0);
            final double bounce = (value < 0.5)
                ? -6.0 * (1.0 - (value * 2.0 - 1.0).abs())
                : 0.0;
            return Container(
              margin: const EdgeInsets.symmetric(horizontal: 3),
              child: Transform.translate(
                offset: Offset(0, bounce),
                child: Container(
                  width: 8,
                  height: 8,
                  decoration: BoxDecoration(
                    color: fptOrange.withValues(alpha: opacity),
                    shape: BoxShape.circle,
                  ),
                ),
              ),
            );
          },
        );
      }),
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
        child: _buildWaveDots(),
      ),
    );
  }

  // Chọn và gửi ảnh
  void _handlePickImage() async {
    showModalBottomSheet(
      context: context,
      builder: (ctx) => SafeArea(
        child: Wrap(
          children: [
            ListTile(
              leading: const Icon(Icons.photo_library),
              title: const Text('Chọn từ thư viện'),
              onTap: () {
                Navigator.pop(ctx);
                _pickAndSendImage(ImageSource.gallery);
              },
            ),
            ListTile(
              leading: const Icon(Icons.camera_alt),
              title: const Text('Chụp ảnh'),
              onTap: () {
                Navigator.pop(ctx);
                _pickAndSendImage(ImageSource.camera);
              },
            ),
          ],
        ),
      ),
    );
  }

  void _pickAndSendImage(ImageSource source) async {
    try {
      final picker = ImagePicker();
      final pickedFile = await picker.pickImage(
        source: source,
        maxWidth: 1200,
        imageQuality: 80,
      );
      if (pickedFile == null) return;

      final file = File(pickedFile.path);
      _dismissChatFeedbackCard();

      // Thêm optimistic message
      setState(() {
        _messages.add(
          ChatMessage(
            text: 'Đang gửi ảnh...',
            isUser: true,
            time: DateTime.now(),
          ),
        );
      });
      _scrollToBottom();

      if (!mounted) return;
      final authService = Provider.of<AuthService>(context, listen: false);
      final token = await authService.getToken();
      if (token != null && _conversationId != null) {
        final responseData = await _chatService.sendMessageWithImage(
          conversationId: _conversationId!,
          imageFile: file,
          senderType: 'STUDENT',
          authToken: token,
        );

        // Xóa optimistic message
        setState(() {
          _messages.removeWhere((m) => m.text == 'Đang gửi ảnh...' && m.isUser);
        });

        if (responseData != null) {
          final parsedMessage = _parseMessageContent(
            responseData['content']?.toString() ?? '',
            attachmentUrl: responseData['attachmentUrl']?.toString(),
          );
          setState(() {
            _messages.add(
              ChatMessage(
                text: parsedMessage.text,
                isUser: true,
                time: DateTime.now(),
                imageUrls: parsedMessage.imageUrls.isNotEmpty
                    ? parsedMessage.imageUrls
                    : null,
                localImagePath: file.path,
              ),
            );
          });
          _scrollToBottom();
          _saveMessages();
          debugPrint(
            '[CHAT] Image sent successfully, urls: ${parsedMessage.imageUrls}',
          );
        } else {
          setState(() {
            _messages.add(
              ChatMessage(
                text: 'Gửi ảnh thất bại. Vui lòng thử lại.',
                isUser: false,
                time: DateTime.now(),
              ),
            );
          });
        }
      }
    } catch (e) {
      debugPrint('[CHAT] Error picking/sending image: $e');
      setState(() {
        _messages.removeWhere((m) => m.text == 'Đang gửi ảnh...' && m.isUser);
      });
    }
  }

  // Xử lý gửi tin nhắn & Gọi AI Service
  void _handleSubmitted(String text) async {
    if (text.trim().isEmpty) return;

    _textController.clear();
    _dismissChatFeedbackCard();

    // Hiển thị tin nhắn user LÊN UI NGAY LẬP TỨC (optimistic)
    setState(() {
      _messages.add(
        ChatMessage(text: text, isUser: true, time: DateTime.now()),
      );
      _isTyping = !_isEscalated;
    });
    _saveMessages();
    _scrollToBottom();

    // Check backend status nếu chưa escalated (non-blocking cho UI)
    // Phòng trường hợp thủ thư đã join từ support request nhưng polling/WS chưa kịp phát hiện
    if (!_isEscalated && !_isWaitingInQueue) {
      try {
        final authService = Provider.of<AuthService>(context, listen: false);
        final token = await authService.getToken();
        if (token != null) {
          final activeConv = await _chatService.getMyActiveConversation(token);
          if (activeConv != null && activeConv['status'] == 'HUMAN_CHATTING') {
            final convId = activeConv['conversationId']?.toString();
            final libName = activeConv['librarianName'] as String? ?? '';
            debugPrint(
              '[CHAT] Detected HUMAN_CHATTING before sending to AI! Switching mode...',
            );
            setState(() {
              _isTyping = false;
            });

            await _handleLibrarianJoinedFromSupportRequest(
              libName,
              convId,
              token,
            );

            // Gửi tin nhắn user đến backend (đã switch sang human mode)
            await _chatService.sendMessageToBackend(
              conversationId: convId!,
              content: text,
              senderType: 'STUDENT',
              authToken: token,
            );
            _saveMessages();
            return;
          }
        }
      } catch (e) {
        debugPrint('[CHAT] Error checking status before submit: $e');
      }
    }

    // Nếu đang chat với librarian - gửi tin nhắn đến backend
    if (_isEscalated && _conversationId != null && !_isWaitingInQueue) {
      debugPrint(
        '[CHAT] Sending escalated message: convId=$_conversationId, text=$text',
      );
      try {
        if (!mounted) return;
        final authService = Provider.of<AuthService>(context, listen: false);
        final token = await authService.getToken();
        if (token != null) {
          final success = await _chatService.sendMessageToBackend(
            conversationId: _conversationId!,
            content: text,
            senderType: 'STUDENT',
            authToken: token,
          );
          debugPrint('[CHAT] Send result: $success');
        } else {
          debugPrint('[CHAT] No auth token!');
        }
      } catch (e) {
        debugPrint('[CHAT] Error sending message to backend: $e');
      }
      _saveMessages();
      return;
    }

    // Local detection cho intent "gap thu thu"
    final lowerText = text.toLowerCase();
    final wantsLibrarian =
        lowerText.contains('gap thu thu') ||
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
        _messages.add(
          ChatMessage(
            text:
                "Dạ, để được gặp thủ thư, bạn vui lòng bấm vào nút bên dưới ạ!",
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
          ),
        );
      });
      _scrollToBottom();
      _saveMessages();
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
          _messages.add(
            ChatMessage(
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
            ),
          );
        } else {
          // Tin nhan thong thuong
          _messages.add(
            ChatMessage(
              text: response.reply,
              isUser: false,
              time: DateTime.now(),
              isEscalation: response.escalated,
            ),
          );
        }

        // Cap nhat trang thai escalation
        if (response.escalated) {
          _isEscalated = true;
        }
      });

      _scrollToBottom();
      _saveMessages();

      // Lưu messages vào backend database
      if (_conversationId != null) {
        try {
          final authService = Provider.of<AuthService>(context, listen: false);
          final token = await authService.getToken();
          if (token != null) {
            // Lưu user message
            await _chatService.sendMessageToBackend(
              conversationId: _conversationId!,
              content: text,
              senderType: 'STUDENT',
              authToken: token,
            );
            // Lưu AI response
            await _chatService.sendMessageToBackend(
              conversationId: _conversationId!,
              content: response.reply,
              senderType: 'AI',
              authToken: token,
            );
          }
        } catch (e) {
          debugPrint('[CHAT] Error saving AI messages to backend: $e');
        }
      }
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isTyping = false;
        _messages.add(
          ChatMessage(
            text: "Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.",
            isUser: false,
            time: DateTime.now(),
          ),
        );
      });
      _scrollToBottom();
      _saveMessages();
    }
  }

  // Scroll listener: hiện/ẩn nút scroll-to-bottom
  void _onScrollChanged() {
    if (!_scrollController.hasClients) return;
    // reverse: true → offset 0 = bottom, offset > 0 = scrolled up
    final currentScroll = _scrollController.offset;
    final shouldShow = currentScroll > 300;
    if (shouldShow != _showScrollToBottom) {
      setState(() {
        _showScrollToBottom = shouldShow;
      });
    }
  }

  /// Khi scroll lên đầu (top = maxScrollExtent with reverse) → load thêm messages cũ hơn
  void _onScrollUp() {
    if (!_scrollController.hasClients) return;
    final maxScroll = _scrollController.position.maxScrollExtent;
    if (_scrollController.position.pixels >= maxScroll - 50 &&
        !_isLoadingMore &&
        _hasMorePages &&
        _conversationId != null) {
      _loadMoreMessages();
    }
  }

  /// Load thêm messages cũ hơn (pagination)
  Future<void> _loadMoreMessages() async {
    if (_isLoadingMore || !_hasMorePages) return;

    setState(() => _isLoadingMore = true);

    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final token = await authService.getToken();
      if (token == null || _conversationId == null) {
        setState(() => _isLoadingMore = false);
        return;
      }

      final nextPage = _currentPage + 1;
      final result = await _chatService.getMessagesPaginated(
        _conversationId!,
        token,
        page: nextPage,
        size: _pageSize,
      );

      final List<dynamic> content = result['content'] ?? [];
      final bool isLast = result['last'] == true;

      if (content.isEmpty) {
        setState(() {
          _hasMorePages = false;
          _isLoadingMore = false;
        });
        return;
      }

      // Lưu vị trí scroll hiện tại để giữ nguyên sau khi thêm messages
      final scrollBefore = _scrollController.position.maxScrollExtent;

      setState(() {
        _currentPage = nextPage;
        _hasMorePages = !isLast;

        // Content trả về DESC (mới nhất trước), cần reverse lại
        final olderMessages = content.reversed
            .map(
              (msg) =>
                  _buildChatMessageFromBackend(msg as Map<String, dynamic>),
            )
            .whereType<ChatMessage>()
            .toList();

        // Prepend older messages ở đầu list
        _messages.insertAll(0, olderMessages);
      });

      // Giữ vị trí scroll sau khi thêm messages ở đầu
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (_scrollController.hasClients) {
          final scrollAfter = _scrollController.position.maxScrollExtent;
          final diff = scrollAfter - scrollBefore;
          _scrollController.jumpTo(_scrollController.offset + diff);
        }
      });

      setState(() => _isLoadingMore = false);
    } catch (e) {
      debugPrint('[PAGINATION] Error loading more: $e');
      setState(() => _isLoadingMore = false);
    }
  }

  void _scrollToBottom() {
    Future.delayed(const Duration(milliseconds: 100), () {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          0.0, // reverse: true → 0 is bottom
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  // Xu ly khi user tap vao action button
  void _handleActionTap(String actionId) {
    _dismissChatFeedbackCard();

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

  void _dismissChatFeedbackCard() {
    final hasFeedbackCard = _messages.any(
      (m) => m.type == ChatMessageType.feedbackPrompt,
    );
    if (!hasFeedbackCard) return;

    setState(() {
      _messages.removeWhere((m) => m.type == ChatMessageType.feedbackPrompt);
      _chatFeedbackRating = 0;
      _chatFeedbackSubmitted = false;
    });
    _saveMessages();
  }

  // Yeu cau gap thu thu - gọi API thật
  Future<void> _handleRequestLibrarian() async {
    // Reset cancel guard cho lần escalate mới
    _userCancelledQueue = false;

    // Hiện UI waiting ngay lập tức
    setState(() {
      _isWaitingInQueue = true;
      _isEscalated = true;
      _queuePosition = 1;

      // Tin nhắn user gởi
      _messages.add(
        ChatMessage(
          text: "Chat với Thủ thư SLIB",
          isUser: true,
          time: DateTime.now(),
        ),
      );
      // Bot trả lời
      _messages.add(
        ChatMessage(
          text:
              "Dạ mình đang điều hướng bạn tới bộ phận thủ thư của thư viện, bạn vui lòng đợi chút nhé",
          isUser: false,
          time: DateTime.now(),
        ),
      );
      // Queue waiting indicator
      _messages.add(
        ChatMessage(
          text: "",
          isUser: false,
          time: DateTime.now(),
          type: ChatMessageType.waiting,
          queuePosition: _queuePosition,
          actions: [ChatAction(id: 'cancel_queue', label: 'Không chờ nữa')],
        ),
      );
    });
    _scrollToBottom();

    // Gọi API để tạo conversation và escalate
    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final token = await authService.getToken();

      if (token != null) {
        // Gửi AI session ID để backend đọc chat history từ MongoDB
        final result = await _chatService.requestLibrarian(
          'User yêu cầu gặp thủ thư',
          token,
          aiSessionId: _chatService.sessionId,
        );

        if (result.success && mounted) {
          setState(() {
            _conversationId = result.conversationId;
            _queuePosition = result.queuePosition;
          });

          // Save state để có thể restore khi mở lại app
          await _saveState();

          // Kết nối WebSocket để nhận queue updates real-time
          _connectWebSocketForQueue(token);
        }
      } else {
        debugPrint('User chưa đăng nhập, không thể tạo conversation');
      }
    } catch (e) {
      debugPrint('Error calling requestLibrarian: $e');
    }
  }

  // WebSocket connection for queue status & message updates (replaces polling)
  void _connectWebSocketForQueue(String token) async {
    final authService = Provider.of<AuthService>(context, listen: false);
    final userId = authService.currentUser?.id;
    if (userId == null) {
      debugPrint('[WS] No userId, falling back to polling');
      _startStatusPolling(token);
      return;
    }

    // Setup handler for student topic events
    _chatWsService.setOnStudentTopicMessage((data) {
      final type = data['type'] as String?;
      debugPrint('[WS] Received event: $type');

      if (type == 'QUEUE_POSITION_UPDATE' && mounted) {
        final newPosition = data['queuePosition'] as int? ?? 0;
        if (newPosition != _queuePosition) {
          setState(() {
            _queuePosition = newPosition;
          });
        }
      } else if (type == 'LIBRARIAN_JOINED' && mounted) {
        final libName = data['librarianName'] as String? ?? '';
        _handleLibrarianJoined(libName, token);
      }
    });

    // Connect and subscribe
    _chatWsService.connect(
      authToken: token,
      onConnected: () {
        debugPrint('[WS] Connected, subscribing to student topic: $userId');
        _chatWsService.subscribeToStudentTopic(userId);
      },
      onError: (error) {
        debugPrint('[WS] Connection error: $error, falling back to polling');
        _startStatusPolling(token);
      },
    );
  }

  // Handle librarian joined event from WebSocket
  Future<void> _handleLibrarianJoined(
    String librarianName,
    String token,
  ) async {
    if (!mounted || _conversationId == null) return;
    _dismissChatFeedbackCard();

    // Reset feedback flag for this conversation so a new session can prompt again
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('chat_feedback_$_conversationId');
    _isHandlingChatEnd = false;

    setState(() {
      _isWaitingInQueue = false;
      _librarianName = librarianName;
      _queuePosition = 0;
      _chatFeedbackSubmitted = false;
      _chatFeedbackRating = 0;
      // Xóa waiting message (queue indicator) khi thủ thư đã chấp nhận
      _messages.removeWhere((m) => m.type == ChatMessageType.waiting);
    });

    // Load messages from backend - GIỮ messages local (AI chat context)
    await _loadMessagesFromBackend(_conversationId!, token, keepExisting: true);

    // Show notification message
    setState(() {
      _messages.add(
        ChatMessage(
          text:
              "Thủ thư ${librarianName.isNotEmpty ? librarianName : ''} đã tiếp nhận yêu cầu của bạn. Bạn có thể chat trực tiếp ngay bây giờ!",
          isUser: false,
          time: DateTime.now(),
        ),
      );
    });

    await _saveState();
    _scrollToBottom();

    // Subscribe to conversation topic for real-time messages
    if (_conversationId != null) {
      _chatWsService.setOnMessageReceived((msgData) {
        if (!mounted) return;
        final type = msgData['type'] as String?;

        if (type == 'TYPING' || type == 'MESSAGES_READ') return;

        final msgId = msgData['id']?.toString();
        if (msgId != null && _messageIds.contains(msgId)) return; // Duplicate
        if (msgId != null) _messageIds.add(msgId);

        final senderType = msgData['senderType'] as String? ?? '';
        if (senderType == 'LIBRARIAN') {
          _dismissChatFeedbackCard();
          final msgContent = msgData['content'] ?? '';
          final parsedMessage = _parseMessageContent(
            msgContent,
            attachmentUrl: msgData['attachmentUrl']?.toString(),
          );
          setState(() {
            _messages.add(
              ChatMessage(
                text: parsedMessage.text,
                isUser: false,
                time: DateTime.now(),
                isFromLibrarian: true,
                imageUrls: parsedMessage.imageUrls.isNotEmpty
                    ? parsedMessage.imageUrls
                    : null,
              ),
            );
          });
          _scrollToBottom();
          _saveMessages();
        }
      });
      _chatWsService.subscribeToConversation(_conversationId!);
    }

    // Keep message polling as fallback (slower rate since WS is primary)
    _startMessagePolling(token);
  }

  // Fallback: Polling de check conversation status (khi WebSocket khong ket noi duoc)
  void _startStatusPolling(String token) async {
    if (_isStatusPollingActive) {
      debugPrint('[STATUS-POLL] Already active, skipping');
      return;
    }
    _isStatusPollingActive = true;
    while (_isWaitingInQueue && mounted && _conversationId != null) {
      await Future.delayed(const Duration(seconds: 2));

      if (!mounted || _conversationId == null) break;

      final status = await _chatService.getConversationStatus(
        _conversationId!,
        token,
      );

      // Cap nhat queue position
      if (status.isWaiting &&
          mounted &&
          status.queuePosition != _queuePosition) {
        setState(() {
          _queuePosition = status.queuePosition;
        });
      }

      if (status.isHumanChatting && mounted) {
        await _handleLibrarianJoined(status.librarianName, token);
        break;
      } else if (status.isResolved && mounted) {
        _handleChatEnded();
        break;
      }
    }
    _isStatusPollingActive = false;
  }

  // HTTP Polling để nhận tin nhắn mới từ librarian (WebSocket bị lỗi SSL với ngrok)
  void _startMessagePolling(String token) async {
    if (_isMessagePollingActive) {
      debugPrint('[POLLING] Already active, skipping');
      return;
    }
    _isMessagePollingActive = true;
    debugPrint('[POLLING] Started for: $_conversationId');

    if (_conversationId == null) {
      debugPrint('[POLLING] No conversationId, skipping');
      return;
    }

    int consecutiveErrors = 0;
    const int maxRetries = 60; // 60 lần x 500ms = 30 giây chờ server khôi phục

    while (_isEscalated &&
        mounted &&
        _conversationId != null &&
        !_isWaitingInQueue) {
      await Future.delayed(
        const Duration(milliseconds: 500),
      ); // 500ms cho gan real-time

      if (!mounted || _conversationId == null || !_isEscalated) {
        debugPrint('[POLLING] Stopped');
        break;
      }

      try {
        // Check nếu conversation đã resolved hoặc quay về AI mode
        final status = await _chatService.getConversationStatus(
          _conversationId!,
          token,
        );

        // API thành công → reset đếm lỗi
        consecutiveErrors = 0;

        if (status.isResolved) {
          debugPrint('[POLLING] Conversation resolved');
          _handleChatEnded();
          break;
        }
        // Check nếu thủ thư đã kết thúc (status quay về AI_HANDLING)
        if (status.isAIHandling && _isEscalated) {
          debugPrint('[POLLING] Librarian ended, back to AI mode');
          _handleChatEnded();
          break;
        }

        // Lấy messages mới từ backend
        final backendMessages = await _chatService.getMessages(
          _conversationId!,
          token,
        );
        // Verbose log removed

        if (mounted && backendMessages.isNotEmpty) {
          bool hasNewMessages = false;

          for (final msg in backendMessages) {
            final msgId = msg['id']?.toString() ?? '';
            final content = msg['content'] as String? ?? '';
            final senderType = msg['senderType'] as String? ?? 'STUDENT';

            // Check SYSTEM message (thủ thư kết thúc)
            if (senderType == 'SYSTEM' && content.contains('kết thúc')) {
              debugPrint('[POLLING] SYSTEM: librarian ended chat');
              _handleChatEnded();
              return; // Exit polling
            }

            // Only add if not already tracked
            if (msgId.isNotEmpty && !_messageIds.contains(msgId)) {
              _messageIds.add(msgId);

              // Only add LIBRARIAN messages to UI (STUDENT messages are already local)
              final attachmentUrl = msg['attachmentUrl']?.toString();
              if (senderType == 'LIBRARIAN' &&
                  (content.isNotEmpty ||
                      (attachmentUrl != null && attachmentUrl.isNotEmpty))) {
                debugPrint('[POLLING] New librarian message received');
                hasNewMessages = true;
                _dismissChatFeedbackCard();
                final parsedMessage = _parseMessageContent(
                  content,
                  attachmentUrl: attachmentUrl,
                );
                setState(() {
                  _messages.add(
                    ChatMessage(
                      text: parsedMessage.text,
                      isUser: false,
                      isFromLibrarian: true,
                      time: DateTime.now(),
                      imageUrls: parsedMessage.imageUrls.isNotEmpty
                          ? parsedMessage.imageUrls
                          : null,
                    ),
                  );
                });
              }
            }
          }

          if (hasNewMessages) {
            _scrollToBottom();
            _saveMessages(); // Lưu local khi có tin mới
          }
        }
      } catch (e) {
        consecutiveErrors++;
        debugPrint('[POLLING] Error #$consecutiveErrors: $e');

        // Server đang restart → chờ lâu hơn, KHÔNG kết thúc chat
        if (consecutiveErrors <= maxRetries) {
          // Chờ lâu hơn khi có lỗi liên tiếp (1-3 giây)
          await Future.delayed(
            Duration(milliseconds: consecutiveErrors > 10 ? 3000 : 1000),
          );
        } else {
          // Quá 30 giây lỗi liên tiếp → thông báo lỗi nhưng vẫn KHÔNG kết thúc
          debugPrint('[POLLING] Too many errors, stopping');
          break;
        }
      }
    }
    _isMessagePollingActive = false;
    debugPrint('[POLLING] Ended');
  }

  // Huy cho trong queue
  void _handleCancelQueue() async {
    // QUAN TRỌNG: Set guard TRƯỚC, stop polling VÀ WebSocket
    _userCancelledQueue = true;
    _isAIPollingActive = false;
    _isStatusPollingActive = false;
    _isMessagePollingActive = false;

    setState(() {
      _isWaitingInQueue = false;
      _isEscalated = false;
    });

    // Disconnect WebSocket để không nhận events nữa
    _chatWsService.disconnect();

    // Gọi API hủy queue ở backend
    if (_conversationId != null) {
      try {
        final authService = Provider.of<AuthService>(context, listen: false);
        final token = await authService.getToken();
        if (token != null) {
          await _chatService.cancelQueue(_conversationId!, token);
        }
      } catch (e) {
        debugPrint('[CHAT] Error cancelling queue: $e');
      }
    }

    setState(() {
      // Xóa waiting message (queue indicator)
      _messages.removeWhere((m) => m.type == ChatMessageType.waiting);

      // Thêm tin nhắn user
      _messages.add(
        ChatMessage(text: "Không chờ nữa", isUser: true, time: DateTime.now()),
      );

      _messages.add(
        ChatMessage(
          text:
              "Dạ, mong bạn thông cảm vì SLIB vẫn chưa hỗ trợ được bạn trực tiếp. Bạn có thể:",
          isUser: false,
          time: DateTime.now(),
          type: ChatMessageType.withActions,
          actions: [
            ChatAction(
              id: 'submit_support_request',
              label: 'Gửi yêu cầu hỗ trợ',
              icon: '',
              isPrimary: true,
            ),
            ChatAction(
              id: 'contact_later',
              label: 'Tôi sẽ liên hệ sau',
              icon: '',
            ),
          ],
        ),
      );
    });
    _scrollToBottom();
  }

  // Gửi yêu cầu hỗ trợ - chuyển đến trang Q&A
  void _handleSubmitSupportRequest() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const SupportRequestScreen()),
    );
  }

  // Lien he sau
  void _handleContactLater() {
    setState(() {
      // Tin nhắn user
      _messages.add(
        ChatMessage(
          text: "Tiếp tục chat với bot",
          isUser: true,
          time: DateTime.now(),
        ),
      );

      // Reset escalation
      _isEscalated = false;

      _messages.add(
        ChatMessage(
          text:
              "Dạ vâng ạ, SLIB luôn sẵn sàng giúp đỡ bạn. Bạn còn cần hỗ trợ gì không ạ?",
          isUser: false,
          time: DateTime.now(),
        ),
      );
    });
    _scrollToBottom();
  }

  // Xử lý khi sinh viên kết thúc cuộc chat với thủ thư
  void _handleStudentResolve() async {
    if (_conversationId == null) return;

    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final token = await authService.getToken();
      if (token == null) return;

      final success = await _chatService.studentResolveConversation(
        _conversationId!,
        token,
      );
      if (success) {
        debugPrint('[CHAT] Student resolved conversation: $_conversationId');
        // Reuse _handleChatEnded logic
        _handleChatEnded();
      } else {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text(
                'Không thể kết thúc cuộc trò chuyện. Vui lòng thử lại.',
              ),
            ),
          );
        }
      }
    } catch (e) {
      debugPrint('[CHAT] Error resolving conversation: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(ErrorDisplayWidget.toVietnamese(e))),
        );
      }
    }
  }

  // Xử lý khi librarian kết thúc cuộc chat
  // QUAN TRỌNG: Giữ conversationId (single session), chỉ reset escalation state
  Widget _buildChatFeedbackCard() {
    if (_chatFeedbackSubmitted) {
      return Container(
        margin: const EdgeInsets.symmetric(vertical: 8, horizontal: 16),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: const Color(0xFFF0FFF4),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: const Color(0xFF4CAF50).withValues(alpha: 0.3),
          ),
        ),
        child: const Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.check_circle_rounded,
              color: Color(0xFF4CAF50),
              size: 20,
            ),
            SizedBox(width: 8),
            Text(
              'Cảm ơn bạn đã đánh giá!',
              style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
            ),
          ],
        ),
      );
    }

    return Container(
      margin: const EdgeInsets.symmetric(vertical: 8, horizontal: 16),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: const Color(0xFFFF751F).withValues(alpha: 0.2),
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.04),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.center,
        mainAxisSize: MainAxisSize.min,
        children: [
          const Text(
            'Đánh giá cuộc trò chuyện',
            style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
          ),
          const SizedBox(height: 4),
          Text(
            'Ý kiến của bạn giúp cải thiện dịch vụ hỗ trợ',
            style: TextStyle(fontSize: 12, color: Colors.grey[500]),
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: List.generate(5, (i) {
              final star = i + 1;
              return GestureDetector(
                onTap: () => setState(() => _chatFeedbackRating = star),
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 6),
                  child: Icon(
                    star <= _chatFeedbackRating
                        ? Icons.star_rounded
                        : Icons.star_outline_rounded,
                    size: 36,
                    color: star <= _chatFeedbackRating
                        ? const Color(0xFFFF751F)
                        : Colors.grey[300],
                  ),
                ),
              );
            }),
          ),
          if (_chatFeedbackRating > 0) ...[
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: () => _submitChatFeedback(),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFFF751F),
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 10),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(10),
                  ),
                  elevation: 0,
                ),
                child: const Text(
                  'Gửi đánh giá',
                  style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Future<void> _submitChatFeedback() async {
    if (_chatFeedbackRating == 0 || _feedbackConversationId == null) return;

    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final url = Uri.parse('${ApiConstants.domain}/slib/feedbacks');

      final response = await authService.authenticatedRequest(
        'POST',
        url,
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'rating': _chatFeedbackRating,
          'content': '',
          'category': 'MESSAGE',
          'conversationId': _feedbackConversationId,
        }),
      );

      if (response.statusCode == 201 || response.statusCode == 200) {
        final prefs = await SharedPreferences.getInstance();
        await prefs.setBool('chat_feedback_$_feedbackConversationId', true);
        setState(() => _chatFeedbackSubmitted = true);
      }
    } catch (e) {
      debugPrint('[FEEDBACK] Error submitting chat feedback: $e');
    }
  }

  void _handleChatEnded() async {
    // Skip nếu user chủ động cancel queue (guard KHÔNG reset để block tất cả calls)
    if (_userCancelledQueue) {
      debugPrint(
        '[CHAT] Skipping _handleChatEnded because user cancelled queue',
      );
      return;
    }

    // Prevent concurrent calls
    if (_isHandlingChatEnd) {
      debugPrint('[CHAT] Skipping duplicate _handleChatEnded call');
      return;
    }
    _isHandlingChatEnd = true;

    // Stop all polling loops
    _isAIPollingActive = false;
    _isStatusPollingActive = false;
    _isMessagePollingActive = false;

    // Disconnect WebSocket
    _chatWsService.disconnect();

    // Clear message tracking IDs để session mới không bị block
    _messageIds.clear();

    // Chỉ clear escalation state, KHÔNG clear conversationId
    await _writeChatStateBool(_keyIsEscalated, false);
    await _writeChatStateBool(_keyIsWaitingInQueue, false);
    await _chatStateStorage.delete(key: _keyLibrarianName);
    // KHÔNG remove _keyConversationId - giữ session cho lần escalate tiếp theo

    _feedbackConversationId = _conversationId;
    _chatFeedbackRating = 0;
    _chatFeedbackSubmitted = false;

    final prefs = await SharedPreferences.getInstance();
    final alreadyRated =
        prefs.getBool('chat_feedback_$_feedbackConversationId') ?? false;

    setState(() {
      _isEscalated = false;
      _isWaitingInQueue = false;
      _librarianName = null;

      _messages.add(
        ChatMessage(
          text: "Cuộc trò chuyện với thủ thư đã kết thúc. Bạn có thể:",
          isUser: false,
          time: DateTime.now(),
          type: ChatMessageType.withActions,
          actions: [
            ChatAction(
              id: 'submit_support_request',
              label: 'Gửi yêu cầu hỗ trợ',
              icon: '',
              isPrimary: true,
            ),
            ChatAction(
              id: 'contact_later',
              label: 'Tiếp tục chat với bot',
              icon: '',
            ),
          ],
        ),
      );

      if (!alreadyRated && _feedbackConversationId != null) {
        _messages.add(
          ChatMessage(
            text: '',
            isUser: false,
            time: DateTime.now(),
            type: ChatMessageType.feedbackPrompt,
          ),
        );
      }
    });
    debugPrint(
      '[CHAT] Librarian ended conversation (human session ended), back to AI mode. Session ID kept: $_conversationId',
    );
    _scrollToBottom();

    _isHandlingChatEnd = false;

    // Restart polling để phát hiện nếu thủ thư chat lại từ support request khác
    if (!mounted) return;
    final authService = Provider.of<AuthService>(context, listen: false);
    final token = await authService.getToken();
    if (token != null) {
      _startAIModeStatusPolling();
    }
  }

  void _resetConversation() async {
    // Stop all polling loops trước
    _isAIPollingActive = false;
    _isStatusPollingActive = false;
    _isMessagePollingActive = false;

    // Unsubscribe WebSocket topics
    _chatWsService.unsubscribeFromConversation();
    _chatWsService.unsubscribeFromStudentTopic();

    // Nếu đang chờ queue, hủy trước
    if (_isWaitingInQueue && _conversationId != null) {
      try {
        final authService = Provider.of<AuthService>(context, listen: false);
        final token = await authService.getToken();
        if (token != null) {
          await _chatService.cancelQueue(_conversationId!, token);
        }
      } catch (e) {
        debugPrint('[CHAT] Error cancelling queue on reset: $e');
      }
    }

    // Clear saved state
    await _clearSavedState();

    setState(() {
      _messages.clear();
      _messageIds.clear();
      _messages.add(
        ChatMessage(
          text:
              "Dạ, hi bạn! Rất vui được gặp bạn. Cần mình giúp gì cứ nói nha!",
          isUser: false,
          time: DateTime.now(),
        ),
      );
      _isEscalated = false;
      _isWaitingInQueue = false;
      _conversationId = null;
      _librarianName = null;
      _queuePosition = 0;
      _chatService.clearSession(); // Clear AI session -> tạo session mới
    });
  }
}

// Model tin nhắn
enum ChatMessageType {
  text, // Tin nhắn thông thường
  withActions, // Tin nhắn có action buttons
  waiting, // Đang chờ trong queue
  feedbackPrompt, // Inline feedback card after chat ends
  supportRequestContext, // Card ngữ cảnh từ yêu cầu hỗ trợ
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
  final bool isFromLibrarian;
  final ChatMessageType type;
  final List<ChatAction>? actions;
  final int? queuePosition;
  final List<String>? imageUrls;
  final String? localImagePath;

  ChatMessage({
    required this.text,
    required this.isUser,
    required this.time,
    this.isEscalation = false,
    this.isFromLibrarian = false,
    this.type = ChatMessageType.text,
    this.actions,
    this.queuePosition,
    this.imageUrls,
    this.localImagePath,
  });

  Map<String, dynamic> toJson() => {
    'text': text,
    'isUser': isUser,
    'time': time.toIso8601String(),
    'isEscalation': isEscalation,
    'isFromLibrarian': isFromLibrarian,
    'type': type.index,
    'queuePosition': queuePosition,
    'imageUrls': imageUrls,
    'localImagePath': localImagePath,
  };

  factory ChatMessage.fromJson(Map<String, dynamic> json) => ChatMessage(
    text: json['text'] ?? '',
    isUser: json['isUser'] ?? false,
    time: DateTime.tryParse(json['time'] ?? '') ?? DateTime.now(),
    isEscalation: json['isEscalation'] ?? false,
    isFromLibrarian: json['isFromLibrarian'] ?? false,
    type: ChatMessageType.values[json['type'] ?? 0],
    queuePosition: json['queuePosition'],
    imageUrls: json['imageUrls'] != null
        ? List<String>.from(json['imageUrls'])
        : null,
    localImagePath: json['localImagePath'],
  );
}
