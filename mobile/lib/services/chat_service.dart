import 'dart:convert';
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

  /// Lấy tất cả messages của conversation
  Future<List<Map<String, dynamic>>> getMessages(String conversationId, String authToken) async {
    try {
      final response = await http.get(
        Uri.parse('${ApiConstants.domain}/slib/chat/conversations/$conversationId/messages'),
        headers: {
          'Authorization': 'Bearer $authToken',
        },
      );

      print('[ChatService] getMessages response: ${response.statusCode}');
      if (response.statusCode == 200) {
        final List<dynamic> data = jsonDecode(response.body);
        print('[ChatService] Total messages: ${data.length}');
        // Debug: in ra 3 message cuối để xem senderType
        if (data.length > 0) {
          final last3 = data.length > 3 ? data.sublist(data.length - 3) : data;
          for (var m in last3) {
            print('[ChatService] Last msg: senderType=${m['senderType']}, content=${(m['content'] as String? ?? '').substring(0, (m['content'] as String? ?? '').length > 20 ? 20 : (m['content'] as String? ?? '').length)}...');
          }
        }
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
        );
      }
      return ConversationStatus(status: 'AI_HANDLING', librarianName: '');
    } catch (e) {
      print('Get Status Error: $e');
      return ConversationStatus(status: 'AI_HANDLING', librarianName: '');
    }
  }

  // ==========================================
  // ESCALATION API CALLS
  // ==========================================

  /// Request librarian - tạo conversation mới và escalate trong 1 bước
  /// messageHistory: list của {content, isUser, senderType}
  Future<EscalationResult> requestLibrarian(
    String? reason, 
    String authToken, 
    {List<Map<String, dynamic>>? messageHistory}
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
          'messageHistory': messageHistory ?? [],
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

  ConversationStatus({
    required this.status,
    required this.librarianName,
  });

  bool get isHumanChatting => status == 'HUMAN_CHATTING';
  bool get isResolved => status == 'RESOLVED';
  bool get isWaiting => status == 'QUEUE_WAITING';
}
