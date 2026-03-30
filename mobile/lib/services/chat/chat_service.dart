import 'dart:convert';
import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';
import 'package:slib/core/constants/api_constants.dart';

/// Chat Service for AI Assistant integration
/// Handles communication with AI Service API and Backend chat API
class ChatService {
  static final String _aiChatUrl = ApiConstants.aiChatUrl;
  final _storage = const FlutterSecureStorage();

  String? _sessionId;

  Future<Map<String, String>> _authHeaders() async {
    final token = await _storage.read(key: 'jwt_token');
    return {
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
  }

  /// Send message to AI and get response
  /// Returns ChatResponse with reply, escalation status, etc.
  Future<ChatResponse> sendMessage(
    String message, {
    String? studentId,
    String? conversationId,
  }) async {
    try {
      final response = await http.post(
        Uri.parse(_aiChatUrl),
        headers: await _authHeaders(),
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
        debugPrint('[AI Chat] Error status=${response.statusCode}');
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
      debugPrint('ChatService Error: $e');
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
        Uri.parse(
          '${ApiConstants.domain}/slib/chat/conversations/$conversationId/messages',
        ),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $authToken',
        },
        body: jsonEncode({'content': content, 'senderType': senderType}),
      );
      return response.statusCode == 200;
    } catch (e) {
      debugPrint('Send Message Error: $e');
      return false;
    }
  }

  /// Gửi tin nhắn kèm ảnh đến backend
  /// Trả về payload tin nhắn nếu thành công, null nếu thất bại
  Future<Map<String, dynamic>?> sendMessageWithImage({
    required String conversationId,
    required File imageFile,
    String content = '',
    String senderType = 'STUDENT',
    required String authToken,
  }) async {
    try {
      final uri = Uri.parse(
        '${ApiConstants.domain}/slib/chat/conversations/$conversationId/messages/with-image',
      );
      final mimeType = _getImageMimeType(imageFile.path);
      final request = http.MultipartRequest('POST', uri)
        ..headers['Authorization'] = 'Bearer $authToken'
        ..fields['content'] = content
        ..fields['senderType'] = senderType
        ..files.add(
          await http.MultipartFile.fromPath(
            'file',
            imageFile.path,
            contentType: MediaType.parse(mimeType),
          ),
        );

      final response = await request.send();
      if (response.statusCode == 200) {
        final body = await response.stream.bytesToString();
        return jsonDecode(body) as Map<String, dynamic>;
      }
      await response.stream.bytesToString();
      debugPrint('Send Image Message Error: status=${response.statusCode}');
      return null;
    } catch (e) {
      debugPrint('Send Image Message Error: $e');
      return null;
    }
  }

  String _getImageMimeType(String path) {
    final ext = path.split('.').last.toLowerCase();
    switch (ext) {
      case 'png':
        return 'image/png';
      case 'gif':
        return 'image/gif';
      case 'webp':
        return 'image/webp';
      case 'heic':
        return 'image/heic';
      case 'heif':
        return 'image/heif';
      case 'jpg':
      case 'jpeg':
      default:
        return 'image/jpeg';
    }
  }

  /// Lấy tất cả messages của conversation
  Future<List<Map<String, dynamic>>> getMessages(
    String conversationId,
    String authToken,
  ) async {
    try {
      final response = await http.get(
        Uri.parse(
          '${ApiConstants.domain}/slib/chat/conversations/$conversationId/messages',
        ),
        headers: {'Authorization': 'Bearer $authToken'},
      );

      if (response.statusCode == 200) {
        final List<dynamic> data = jsonDecode(response.body);
        return data.cast<Map<String, dynamic>>();
      }
      return [];
    } catch (e) {
      debugPrint('Get Messages Error: $e');
      return [];
    }
  }

  /// Lấy status của conversation (để biết khi nào librarian tiếp nhận)
  Future<ConversationStatus> getConversationStatus(
    String conversationId,
    String authToken,
  ) async {
    try {
      final response = await http.get(
        Uri.parse(
          '${ApiConstants.domain}/slib/chat/conversations/$conversationId/status',
        ),
        headers: {'Authorization': 'Bearer $authToken'},
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
      debugPrint('Get Status Error: $e');
      // Return với hasError=true để polling biết là lỗi mạng, không phải thủ thư kết thúc
      return ConversationStatus(
        status: 'ERROR',
        librarianName: '',
        hasError: true,
      );
    }
  }

  // ==========================================
  // ESCALATION API CALLS
  // ==========================================

  /// Request librarian - tạo conversation mới và escalate trong 1 bước
  /// aiSessionId: session ID của AI service (MongoDB) để backend đọc chat history
  Future<EscalationResult> requestLibrarian(
    String? reason,
    String authToken, {
    String? aiSessionId,
  }) async {
    try {
      final response = await http.post(
        Uri.parse(
          '${ApiConstants.domain}/slib/chat/conversations/request-librarian',
        ),
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
      return EscalationResult(
        success: false,
        conversationId: '',
        queuePosition: 0,
      );
    } catch (e) {
      debugPrint('Request Librarian Error: $e');
      return EscalationResult(
        success: false,
        conversationId: '',
        queuePosition: 0,
      );
    }
  }

  /// Escalate conversation to human librarian
  /// Returns the conversation ID from backend
  Future<EscalationResult> escalateToHuman(
    String conversationId,
    String reason,
    String authToken,
  ) async {
    try {
      final response = await http.post(
        Uri.parse(
          '${ApiConstants.domain}/slib/chat/conversations/$conversationId/escalate',
        ),
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
      return EscalationResult(
        success: false,
        conversationId: conversationId,
        queuePosition: 0,
      );
    } catch (e) {
      debugPrint('Escalation Error: $e');
      return EscalationResult(
        success: false,
        conversationId: conversationId,
        queuePosition: 0,
      );
    }
  }

  /// Get current queue position for a conversation
  Future<QueueInfo> getQueuePosition(
    String conversationId,
    String authToken,
  ) async {
    try {
      final response = await http.get(
        Uri.parse(
          '${ApiConstants.domain}/slib/chat/conversations/$conversationId/queue-position',
        ),
        headers: {'Authorization': 'Bearer $authToken'},
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
      debugPrint('Get Queue Position Error: $e');
      return QueueInfo(position: 0, totalWaiting: 0);
    }
  }

  /// Check xem student có conversation đang active không (HUMAN_CHATTING / QUEUE_WAITING)
  Future<Map<String, dynamic>?> getMyActiveConversation(
    String authToken,
  ) async {
    try {
      final response = await http.get(
        Uri.parse('${ApiConstants.domain}/slib/chat/conversations/my-active'),
        headers: {'Authorization': 'Bearer $authToken'},
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['hasActive'] == true) {
          return data;
        }
      }
      return null;
    } catch (e) {
      debugPrint('Get My Active Conversation Error: $e');
      return null;
    }
  }

  /// Cancel escalation and return to AI handling
  Future<bool> cancelEscalation(String conversationId, String authToken) async {
    try {
      final response = await http.post(
        Uri.parse(
          '${ApiConstants.domain}/slib/chat/conversations/$conversationId/cancel-escalation',
        ),
        headers: {'Authorization': 'Bearer $authToken'},
      );

      return response.statusCode == 200;
    } catch (e) {
      debugPrint('Cancel Escalation Error: $e');
      return false;
    }
  }

  /// Cancel queue - student hủy chờ
  Future<bool> cancelQueue(String conversationId, String authToken) async {
    try {
      final response = await http.post(
        Uri.parse(
          '${ApiConstants.domain}/slib/chat/conversations/$conversationId/cancel-queue',
        ),
        headers: {
          'Authorization': 'Bearer $authToken',
          'Content-Type': 'application/json',
        },
      );
      return response.statusCode == 200;
    } catch (e) {
      debugPrint('Cancel Queue Error: $e');
      return false;
    }
  }

  /// Student kết thúc cuộc trò chuyện với thủ thư
  Future<bool> studentResolveConversation(
    String conversationId,
    String authToken,
  ) async {
    try {
      final response = await http.post(
        Uri.parse(
          '${ApiConstants.domain}/slib/chat/conversations/$conversationId/student-resolve',
        ),
        headers: {'Authorization': 'Bearer $authToken'},
      );
      return response.statusCode == 200;
    } catch (e) {
      debugPrint('Student Resolve Error: $e');
      return false;
    }
  }

  /// Student reset chat:
  /// - kết thúc human chat nếu đang active
  /// - hủy queue nếu đang chờ
  /// - ẩn lịch sử cũ khỏi phía student, nhưng vẫn giữ dữ liệu trong DB
  Future<bool> resetConversationForStudent(
    String conversationId,
    String authToken,
  ) async {
    try {
      final response = await http.post(
        Uri.parse(
          '${ApiConstants.domain}/slib/chat/conversations/$conversationId/student-reset',
        ),
        headers: {
          'Authorization': 'Bearer $authToken',
          'Content-Type': 'application/json',
        },
      );
      return response.statusCode == 200;
    } catch (e) {
      debugPrint('Reset Conversation Error: $e');
      return false;
    }
  }

  /// Gửi typing indicator cho đối phương (deprecated - kept for backward compat)
  Future<void> sendTypingIndicator(String conversationId, bool isTyping) async {
    try {
      await http.post(
        Uri.parse(
          '${ApiConstants.domain}/slib/chat/conversations/$conversationId/typing',
        ),
        headers: await _authHeaders(),
        body: jsonEncode({'isTyping': isTyping}),
      );
    } catch (e) {
      // Silent fail - typing indicator is best-effort
    }
  }

  /// Lấy messages với phân trang (page 0 = mới nhất, client reverse lại)
  Future<Map<String, dynamic>> getMessagesPaginated(
    String conversationId,
    String authToken, {
    int page = 0,
    int size = 20,
  }) async {
    try {
      final response = await http.get(
        Uri.parse(
          '${ApiConstants.domain}/slib/chat/conversations/$conversationId/messages?page=$page&size=$size',
        ),
        headers: {'Authorization': 'Bearer $authToken'},
      );

      if (response.statusCode == 200) {
        final decodedBody = utf8.decode(response.bodyBytes);
        return jsonDecode(decodedBody);
      }
      return {'content': [], 'totalPages': 0, 'last': true};
    } catch (e) {
      debugPrint('Get Messages Paginated Error: $e');
      return {'content': [], 'totalPages': 0, 'last': true};
    }
  }

  /// Lấy hoặc tạo conversation cho AI chat (để lưu messages vào DB)
  Future<String?> getOrCreateConversation(String authToken) async {
    try {
      final response = await http.post(
        Uri.parse(
          '${ApiConstants.domain}/slib/chat/conversations/get-or-create',
        ),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $authToken',
        },
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        return data['conversationId'] as String?;
      }
      return null;
    } catch (e) {
      debugPrint('Get Or Create Conversation Error: $e');
      return null;
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

  QueueInfo({required this.position, required this.totalWaiting});
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
