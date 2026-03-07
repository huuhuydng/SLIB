import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:slib/core/constants/api_constants.dart';

/// Chat Service for AI Assistant integration
/// Handles communication with AI Service API and Backend chat API
class ChatService {
  static final String _aiChatUrl = ApiConstants.aiChatUrl;
  
  String? _sessionId;
  
  /// Send message to AI and get response
  /// Returns ChatResponse with reply, escalation status, etc.
  Future<ChatResponse> sendMessage(String message, {String? studentId, String? conversationId}) async {
    try {
      final response = await http.post(
        Uri.parse(_aiChatUrl),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'message': message,
          'session_id': _sessionId,
          'student_id': studentId,
          'conversation_id': conversationId,
        }),
      );

      if (response.statusCode == 200) {
        final decodedBody = utf8.decode(response.bodyBytes);
        final jsonMap = jsonDecode(decodedBody);
        
        // Store session ID for subsequent messages
        _sessionId = jsonMap['session_id'];
        
        return ChatResponse(
          success: jsonMap['success'] ?? false,
          reply: jsonMap['reply'] ?? 'Xin lỗi, tôi không thể trả lời lúc này.',
          sessionId: _sessionId ?? '',
          confidenceScore: (jsonMap['confidence_score'] ?? 0.0).toDouble(),
          needsReview: jsonMap['needs_review'] ?? false,
          escalated: jsonMap['escalated'] ?? false,
          escalationMessage: jsonMap['escalation_message'],
        );
      } else {
        print('[AI Chat] Error: status=${response.statusCode}, body=${response.body}');
        return ChatResponse(
          success: false,
          reply: 'Lỗi kết nối với AI. Vui lòng thử lại sau.',
          sessionId: _sessionId ?? '',
          confidenceScore: 0.0,
          needsReview: true,
          escalated: false,
        );
      }
    } catch (e) {
      print('ChatService Error: $e');
      return ChatResponse(
        success: false,
        reply: 'Không thể kết nối. Vui lòng kiểm tra mạng và thử lại.',
        sessionId: _sessionId ?? '',
        confidenceScore: 0.0,
        needsReview: true,
        escalated: false,
      );
    }
  }

  /// Clear session to start fresh conversation
  void clearSession() {
    _sessionId = null;
  }
  
  /// Get current session ID
  String? get sessionId => _sessionId;

  // ==========================================
  // MESSAGE PERSISTENCE API CALLS
  // ==========================================

  /// Gửi tin nhắn đến backend để lưu vào DB
  Future<bool> sendMessageToBackend({
    required String conversationId,
    required String content,
    required String senderType, // STUDENT, LIBRARIAN, AI
    required String authToken,
  }) async {
    try {
      final response = await http.post(
        Uri.parse('${ApiConstants.domain}/slib/chat/conversations/$conversationId/messages'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $authToken',
        },
        body: jsonEncode({
          'content': content,
          'senderType': senderType,
        }),
      );
      return response.statusCode == 200;
    } catch (e) {
      print('Send Message Error: $e');
      return false;
    }
  }

  /// Gửi tin nhắn kèm ảnh đến backend
  /// Trả về content (chứa [IMAGES] url) nếu thành công, null nếu thất bại
  Future<String?> sendMessageWithImage({
    required String conversationId,
    required File imageFile,
    String content = '',
    String senderType = 'STUDENT',
    required String authToken,
  }) async {
    try {
      final uri = Uri.parse('${ApiConstants.domain}/slib/chat/conversations/$conversationId/messages/with-image');
      final request = http.MultipartRequest('POST', uri)
        ..headers['Authorization'] = 'Bearer $authToken'
        ..fields['content'] = content
        ..fields['senderType'] = senderType
        ..files.add(await http.MultipartFile.fromPath('file', imageFile.path));

      final response = await request.send();
      if (response.statusCode == 200) {
        final body = await response.stream.bytesToString();
        final data = jsonDecode(body);
        return data['content'] as String?;
      }
      return null;
    } catch (e) {
      print('Send Image Message Error: $e');
      return null;
    }
  }

  /// Lấy tất cả messages của conversation
  Future<List<Map<String, dynamic>>> getMessages(String conversationId, String authToken) async {
    try {
      final response = await http.get(
        Uri.parse('${ApiConstants.domain}/slib/chat/conversations/$conversationId/messages'),
        headers: {
          'Authorization': 'Bearer $authToken',
        },
      );

      if (response.statusCode == 200) {
        final List<dynamic> data = jsonDecode(response.body);
        return data.cast<Map<String, dynamic>>();
      }
      return [];
    } catch (e) {
      print('Get Messages Error: $e');
      return [];
    }
  }

  /// Lấy status của conversation (để biết khi nào librarian tiếp nhận)
  Future<ConversationStatus> getConversationStatus(String conversationId, String authToken) async {
    try {
      final response = await http.get(
        Uri.parse('${ApiConstants.domain}/slib/chat/conversations/$conversationId/status'),
        headers: {
          'Authorization': 'Bearer $authToken',
        },
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        return ConversationStatus(
          status: data['status'] ?? 'AI_HANDLING',
          librarianName: data['librarianName'] ?? '',
          queuePosition: data['queuePosition'] ?? 0,
        );
      }
      return ConversationStatus(status: 'AI_HANDLING', librarianName: '');
    } catch (e) {
      print('Get Status Error: $e');
      // Return với hasError=true để polling biết là lỗi mạng, không phải thủ thư kết thúc
      return ConversationStatus(status: 'ERROR', librarianName: '', hasError: true);
    }
  }

  // ==========================================
  // ESCALATION API CALLS
  // ==========================================

  /// Request librarian - tạo conversation mới và escalate trong 1 bước
  /// aiSessionId: session ID của AI service (MongoDB) để backend đọc chat history
  Future<EscalationResult> requestLibrarian(
    String? reason, 
    String authToken, 
    {String? aiSessionId}
  ) async {
    try {
      final response = await http.post(
        Uri.parse('${ApiConstants.domain}/slib/chat/conversations/request-librarian'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $authToken',
        },
        body: jsonEncode({
          'reason': reason ?? 'User yêu cầu gặp thủ thư',
          'aiSessionId': aiSessionId,
        }),
      );

      if (response.statusCode == 200) {
        final jsonMap = jsonDecode(response.body);
        return EscalationResult(
          success: true,
          conversationId: jsonMap['conversationId'].toString(),
          queuePosition: jsonMap['queuePosition'] ?? 1,
        );
      }
      return EscalationResult(success: false, conversationId: '', queuePosition: 0);
    } catch (e) {
      print('Request Librarian Error: $e');
      return EscalationResult(success: false, conversationId: '', queuePosition: 0);
    }
  }

  /// Escalate conversation to human librarian
  /// Returns the conversation ID from backend
  Future<EscalationResult> escalateToHuman(String conversationId, String reason, String authToken) async {
    try {
      final response = await http.post(
        Uri.parse('${ApiConstants.domain}/slib/chat/conversations/$conversationId/escalate'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $authToken',
        },
        body: jsonEncode({'reason': reason}),
      );

      if (response.statusCode == 200) {
        final jsonMap = jsonDecode(response.body);
        return EscalationResult(
          success: true,
          conversationId: jsonMap['id'],
          queuePosition: 1, // Will be updated by getQueuePosition
        );
      }
      return EscalationResult(success: false, conversationId: conversationId, queuePosition: 0);
    } catch (e) {
      print('Escalation Error: $e');
      return EscalationResult(success: false, conversationId: conversationId, queuePosition: 0);
    }
  }

  /// Get current queue position for a conversation
  Future<QueueInfo> getQueuePosition(String conversationId, String authToken) async {
    try {
      final response = await http.get(
        Uri.parse('${ApiConstants.domain}/slib/chat/conversations/$conversationId/queue-position'),
        headers: {
          'Authorization': 'Bearer $authToken',
        },
      );

      if (response.statusCode == 200) {
        final jsonMap = jsonDecode(response.body);
        return QueueInfo(
          position: jsonMap['position'] ?? 0,
          totalWaiting: jsonMap['totalWaiting'] ?? 0,
        );
      }
      return QueueInfo(position: 0, totalWaiting: 0);
    } catch (e) {
      print('Get Queue Position Error: $e');
      return QueueInfo(position: 0, totalWaiting: 0);
    }
  }

  /// Check xem student có conversation đang active không (HUMAN_CHATTING / QUEUE_WAITING)
  Future<Map<String, dynamic>?> getMyActiveConversation(String authToken) async {
    try {
      final response = await http.get(
        Uri.parse('${ApiConstants.domain}/slib/chat/conversations/my-active'),
        headers: {
          'Authorization': 'Bearer $authToken',
        },
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['hasActive'] == true) {
          return data;
        }
      }
      return null;
    } catch (e) {
      print('Get My Active Conversation Error: $e');
      return null;
    }
  }

  /// Cancel escalation and return to AI handling
  Future<bool> cancelEscalation(String conversationId, String authToken) async {
    try {
      final response = await http.post(
        Uri.parse('${ApiConstants.domain}/slib/chat/conversations/$conversationId/cancel-escalation'),
        headers: {
          'Authorization': 'Bearer $authToken',
        },
      );

      return response.statusCode == 200;
    } catch (e) {
      print('Cancel Escalation Error: $e');
      return false;
    }
  }

  /// Cancel queue - student hủy chờ
  Future<bool> cancelQueue(String conversationId, String authToken) async {
    try {
      final response = await http.post(
        Uri.parse('${ApiConstants.domain}/slib/chat/conversations/$conversationId/cancel-queue'),
        headers: {
          'Authorization': 'Bearer $authToken',
          'Content-Type': 'application/json',
        },
      );
      return response.statusCode == 200;
    } catch (e) {
      print('Cancel Queue Error: $e');
      return false;
    }
  }

  /// Student kết thúc cuộc trò chuyện với thủ thư
  Future<bool> studentResolveConversation(String conversationId, String authToken) async {
    try {
      final response = await http.post(
        Uri.parse('${ApiConstants.domain}/slib/chat/conversations/$conversationId/student-resolve'),
        headers: {
          'Authorization': 'Bearer $authToken',
        },
      );
      return response.statusCode == 200;
    } catch (e) {
      print('Student Resolve Error: $e');
      return false;
    }
  }

  /// Đánh dấu đã đọc tin nhắn trong conversation
  Future<void> markConversationAsRead(String conversationId, String authToken) async {
    try {
      await http.post(
        Uri.parse('${ApiConstants.domain}/slib/chat/conversations/$conversationId/mark-read'),
        headers: {
          'Authorization': 'Bearer $authToken',
        },
      );
    } catch (e) {
      print('Mark Read Error: $e');
    }
  }
}

/// Escalation result model
class EscalationResult {
  final bool success;
  final String conversationId;
  final int queuePosition;

  EscalationResult({
    required this.success,
    required this.conversationId,
    required this.queuePosition,
  });
}

/// Queue info model
class QueueInfo {
  final int position;
  final int totalWaiting;

  QueueInfo({
    required this.position,
    required this.totalWaiting,
  });
}

/// Response model from AI Chat API
class ChatResponse {
  final bool success;
  final String reply;
  final String sessionId;
  final double confidenceScore;
  final bool needsReview;
  final bool escalated;
  final String? escalationMessage;

  ChatResponse({
    required this.success,
    required this.reply,
    required this.sessionId,
    required this.confidenceScore,
    required this.needsReview,
    required this.escalated,
    this.escalationMessage,
  });
}

/// Conversation status from backend
class ConversationStatus {
  final String status; // AI_HANDLING, QUEUE_WAITING, HUMAN_CHATTING, RESOLVED
  final String librarianName;
  final bool hasError;
  final int queuePosition;

  ConversationStatus({
    required this.status,
    required this.librarianName,
    this.hasError = false,
    this.queuePosition = 0,
  });

  bool get isHumanChatting => status == 'HUMAN_CHATTING';
  bool get isResolved => status == 'RESOLVED' || status == 'AI_HANDLING';
  bool get isWaiting => status == 'QUEUE_WAITING';
  bool get isAIHandling => !hasError && status == 'AI_HANDLING';
}
