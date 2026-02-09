import 'dart:async';
import 'dart:convert';
import 'package:stomp_dart_client/stomp_dart_client.dart';
import 'package:slib/core/constants/api_constants.dart';

/// WebSocket service for real-time chat messages
class ChatWebSocketService {
  StompClient? _stompClient;
  Function(Map<String, dynamic>)? _onMessageReceived;
  Function()? _onConnected;
  Function(String)? _onError;
  
  String? _currentConversationId;
  String? _authToken;
  StompUnsubscribe? _conversationSubscription;

  bool get isConnected => _stompClient?.connected ?? false;

  /// Convert HTTP URL to WebSocket URL
  String _getWebSocketUrl() {
    String domain = ApiConstants.domain;
    // Convert https:// to wss:// or http:// to ws://
    if (domain.startsWith('https://')) {
      return domain.replaceFirst('https://', 'wss://') + '/ws';
    } else if (domain.startsWith('http://')) {
      return domain.replaceFirst('http://', 'ws://') + '/ws';
    }
    return domain + '/ws';
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
  }

  void _onStompDisconnected(StompFrame frame) {
    print('[WS] Disconnected');
  }

  /// Subscribe to a conversation topic for real-time messages
  void subscribeToConversation(String conversationId) {
    if (_stompClient == null || !_stompClient!.connected) {
      print('[WS] Cannot subscribe - not connected');
      return;
    }

    // Unsubscribe from previous conversation
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

  /// Unsubscribe from current conversation
  void unsubscribeFromConversation() {
    _conversationSubscription?.call();
    _conversationSubscription = null;
    _currentConversationId = null;
  }

  /// Disconnect from WebSocket server
  void disconnect() {
    unsubscribeFromConversation();
    _stompClient?.deactivate();
    _stompClient = null;
    print('[WS] Disconnected and cleaned up');
  }
}
