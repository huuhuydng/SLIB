import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;

/// Service xử lý WebSocket real-time cho seat updates
/// Sử dụng STOMP protocol để tương thích với Spring Backend
class SeatWebSocketService {
  StompClient? _stompClient;
  final List<Function(Map<String, dynamic>)> _listeners = [];
  bool _isConnected = false;
  int _reconnectAttempts = 0;
  static const int _maxReconnectAttempts = 5;

  /// Subscribe to seat updates
  void addListener(Function(Map<String, dynamic>) listener) {
    _listeners.add(listener);
  }

  void removeListener(Function(Map<String, dynamic>) listener) {
    _listeners.remove(listener);
  }

  /// Connect to WebSocket using STOMP protocol
  Future<void> connect() async {
    if (_isConnected) return;
    if (_reconnectAttempts >= _maxReconnectAttempts) {
      debugPrint('STOMP: Max reconnect attempts reached, falling back to polling');
      return;
    }

    try {
      final token = await const FlutterSecureStorage().read(key: 'jwt_token');
      if (token == null) {
        debugPrint('STOMP: Missing auth token, skip WebSocket connection');
        return;
      }

      // Build WebSocket URL từ API domain
      String wsUrl = ApiConstants.domain;
      
      // Convert http/https to ws/wss
      if (wsUrl.startsWith('https://')) {
        wsUrl = wsUrl.replaceFirst('https://', 'wss://');
      } else if (wsUrl.startsWith('http://')) {
        wsUrl = wsUrl.replaceFirst('http://', 'ws://');
      }
      
      // STOMP endpoint - thêm /websocket cho SockJS fallback
      final stompUrl = '$wsUrl/ws/websocket';
      
      debugPrint('STOMP: Connecting to $stompUrl');
      
      _stompClient = StompClient(
        config: StompConfig(
          url: stompUrl,
          stompConnectHeaders: {
            'Authorization': 'Bearer $token',
          },
          onConnect: _onConnect,
          onDisconnect: _onDisconnect,
          onStompError: (frame) {
            debugPrint('STOMP error: ${frame.body}');
            _scheduleReconnect();
          },
          onWebSocketError: (error) {
            debugPrint('WebSocket error: $error');
            _reconnectAttempts++;
            if (_reconnectAttempts < _maxReconnectAttempts) {
              _scheduleReconnect();
            }
          },
          // Headers cho ngrok
          webSocketConnectHeaders: {
            'ngrok-skip-browser-warning': 'true',
          },
          reconnectDelay: const Duration(seconds: 5),
        ),
      );
      
      _stompClient!.activate();
    } catch (e) {
      debugPrint('STOMP connection failed: $e');
      _reconnectAttempts++;
      if (_reconnectAttempts < _maxReconnectAttempts) {
        _scheduleReconnect();
      }
    }
  }

  void _onConnect(StompFrame frame) {
    _isConnected = true;
    _reconnectAttempts = 0;
    debugPrint('STOMP: Connected successfully');
    
    // Subscribe to seat updates topic
    _stompClient!.subscribe(
      destination: '/topic/seats',
      callback: (frame) {
        if (frame.body != null) {
          try {
            final data = jsonDecode(frame.body!) as Map<String, dynamic>;
            debugPrint('STOMP: Received seat update: $data');
            _notifyListeners(data);
          } catch (e) {
            debugPrint('STOMP: Parse error: $e');
          }
        }
      },
    );
  }

  void _onDisconnect(StompFrame frame) {
    _isConnected = false;
    debugPrint('STOMP: Disconnected');
  }

  void _scheduleReconnect() {
    // Exponential backoff: 2s, 4s, 8s, 16s, 32s
    final delay = Duration(seconds: 2 * (1 << _reconnectAttempts));
    debugPrint('STOMP: Scheduling reconnect in ${delay.inSeconds}s (attempt ${_reconnectAttempts + 1}/$_maxReconnectAttempts)');
    
    Future.delayed(delay, () {
      if (!_isConnected && _reconnectAttempts < _maxReconnectAttempts) {
        connect();
      }
    });
  }

  void _notifyListeners(Map<String, dynamic> data) {
    for (final listener in List.from(_listeners)) {
      try {
        listener(data);
      } catch (e) {
        debugPrint('STOMP: Listener error: $e');
      }
    }
  }

  /// Disconnect WebSocket
  void disconnect() {
    _stompClient?.deactivate();
    _isConnected = false;
    debugPrint('STOMP: Manually disconnected');
  }

  bool get isConnected => _isConnected;

  // ============ API Methods ============

  /// Hold a seat temporarily (5 minutes)
  Future<Map<String, dynamic>> holdSeat(int seatId, String userId) async {
    final url = Uri.parse('${ApiConstants.seatUrl}/$seatId/hold');
    final token = await const FlutterSecureStorage().read(key: 'jwt_token');
    final response = await http.post(
      url,
      headers: {
        'Content-Type': 'application/json',
        if (token != null) 'Authorization': 'Bearer $token',
      },
      body: jsonEncode({'userId': userId}),
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      final error = jsonDecode(response.body);
      throw Exception(error['error'] ?? 'Failed to hold seat');
    }
  }

  /// Release a held seat
  Future<Map<String, dynamic>> releaseSeat(int seatId, String userId) async {
    final url = Uri.parse('${ApiConstants.seatUrl}/$seatId/hold');
    final token = await const FlutterSecureStorage().read(key: 'jwt_token');
    final request = http.Request('DELETE', url);
    request.headers['Content-Type'] = 'application/json';
    if (token != null) request.headers['Authorization'] = 'Bearer $token';
    request.body = jsonEncode({'userId': userId});
    
    final streamedResponse = await request.send();
    final response = await http.Response.fromStream(streamedResponse);

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      final error = jsonDecode(response.body);
      throw Exception(error['error'] ?? 'Failed to release seat');
    }
  }
}

/// Singleton instance
final seatWebSocketService = SeatWebSocketService();
