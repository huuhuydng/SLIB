import 'dart:async';
import 'dart:convert';
import 'package:stomp_dart_client/stomp_dart_client.dart';
import 'package:slib/core/constants/api_constants.dart';

/// WebSocket service for real-time chat messages
class ChatWebSocketService {
  StompClient? _stompClient;
  Function(Map<String, dynamic>)? _onMessageReceived;
  Function(Map<String, dynamic>)? _onStudentTopicMessage;
  Function()? _onConnected;
  Function(String)? _onError;
  
  String? _currentConversationId;
  String? _currentStudentId;
  String? _authToken;
  StompUnsubscribe? _conversationSubscription;
  StompUnsubscribe? _studentTopicSubscription;

  bool get isConnected => _stompClient?.connected ?? false;

  /// Get WebSocket URL for SockJS
  /// SockJS cần http/https URL, tự xử lý upgrade protocol
  String _getWebSocketUrl() {
    String domain = ApiConstants.domain;
    return '$domain/ws';
  }

  /// Connect to WebSocket server
  void connect({
    required String authToken,
    Function()? onConnected,
    Function(String)? onError,
  }) {
    _authToken = authToken;
    _onConnected = onConnected;
    _onError = onError;

    final wsUrl = _getWebSocketUrl();
    print('[WS] Connecting to: $wsUrl');

    _stompClient = StompClient(
      config: StompConfig.sockJS(
        url: wsUrl,
        stompConnectHeaders: {
          'Authorization': 'Bearer $authToken',
        },
        onConnect: _onStompConnected,
        onDisconnect: _onStompDisconnected,
        onWebSocketError: (error) {
          print('[WS] WebSocket error: $error');
          _onError?.call(error.toString());
        },
        onStompError: (frame) {
          print('[WS] STOMP error: ${frame.body}');
          _onError?.call(frame.body ?? 'STOMP error');
        },
        reconnectDelay: const Duration(seconds: 5),
      ),
    );

    _stompClient!.activate();
  }

  void _onStompConnected(StompFrame frame) {
    print('[WS] Connected successfully');
    _onConnected?.call();
    
    // Re-subscribe if there was a previous conversation
    if (_currentConversationId != null) {
      subscribeToConversation(_currentConversationId!);
    }
    // Re-subscribe to student topic
    if (_currentStudentId != null) {
      subscribeToStudentTopic(_currentStudentId!);
    }
  }

  void _onStompDisconnected(StompFrame frame) {
    print('[WS] Disconnected');
  }

  /// Subscribe to student topic for queue updates and status changes
  /// Receives: QUEUE_POSITION_UPDATE, LIBRARIAN_JOINED, etc.
  void subscribeToStudentTopic(String studentId) {
    if (_stompClient == null || !_stompClient!.connected) {
      print('[WS] Cannot subscribe to student topic - not connected');
      return;
    }

    // Unsubscribe from previous
    _studentTopicSubscription?.call();

    _currentStudentId = studentId;
    final topic = '/topic/chat/$studentId';
    print('[WS] Subscribing to student topic: $topic');

    _studentTopicSubscription = _stompClient!.subscribe(
      destination: topic,
      callback: (frame) {
        if (frame.body != null) {
          try {
            final data = jsonDecode(frame.body!) as Map<String, dynamic>;
            print('[WS] Student topic event: ${data['type']}');
            _onStudentTopicMessage?.call(data);
          } catch (e) {
            print('[WS] Error parsing student topic message: $e');
          }
        }
      },
    );
  }

  /// Subscribe to a conversation topic for real-time messages
  void subscribeToConversation(String conversationId) {
    if (_stompClient == null || !_stompClient!.connected) {
      print('[WS] Cannot subscribe - not connected');
      return;
    }

    _conversationSubscription?.call();

    _currentConversationId = conversationId;
    final topic = '/topic/conversation/$conversationId';
    print('[WS] Subscribing to: $topic');

    _conversationSubscription = _stompClient!.subscribe(
      destination: topic,
      callback: (frame) {
        if (frame.body != null) {
          try {
            final message = jsonDecode(frame.body!) as Map<String, dynamic>;
            print('[WS] Received message: ${message['content']}');
            _onMessageReceived?.call(message);
          } catch (e) {
            print('[WS] Error parsing message: $e');
          }
        }
      },
    );
  }

  /// Set callback for when messages are received
  void setOnMessageReceived(Function(Map<String, dynamic>) callback) {
    _onMessageReceived = callback;
  }

  /// Set callback for student topic events (queue updates, librarian joined, etc.)
  void setOnStudentTopicMessage(Function(Map<String, dynamic>) callback) {
    _onStudentTopicMessage = callback;
  }

  /// Unsubscribe from current conversation
  void unsubscribeFromConversation() {
    _conversationSubscription?.call();
    _conversationSubscription = null;
    _currentConversationId = null;
  }

  /// Unsubscribe from student topic
  void unsubscribeFromStudentTopic() {
    _studentTopicSubscription?.call();
    _studentTopicSubscription = null;
    _currentStudentId = null;
  }

  /// Disconnect from WebSocket server
  void disconnect() {
    unsubscribeFromConversation();
    unsubscribeFromStudentTopic();
    _stompClient?.deactivate();
    _stompClient = null;
    print('[WS] Disconnected and cleaned up');
  }
}
