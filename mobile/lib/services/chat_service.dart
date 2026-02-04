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
