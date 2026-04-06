import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Service xử lý WebSocket real-time cho seat updates
/// Sử dụng STOMP protocol để tương thích với Spring Backend
class SeatWebSocketService {
  StompClient? _stompClient;
  final List<Function(Map<String, dynamic>)> _listeners = [];
  bool _isConnected = false;
  bool _isConnecting = false;
  bool _isReconnectScheduled = false;
  bool _shouldReconnect = true;
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
    if (_isConnected || _isConnecting) return;
    if (_reconnectAttempts >= _maxReconnectAttempts) {
      debugPrint(
        'STOMP: Max reconnect attempts reached, falling back to polling',
      );
      return;
    }

    try {
      _shouldReconnect = true;
      _isConnecting = true;
      final token = await const FlutterSecureStorage().read(key: 'jwt_token');
      if (token == null) {
        debugPrint('STOMP: Missing auth token, skip WebSocket connection');
        _isConnecting = false;
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
          stompConnectHeaders: {'Authorization': 'Bearer $token'},
          onConnect: _onConnect,
          onDisconnect: _onDisconnect,
          onStompError: (frame) {
            debugPrint('STOMP error: ${frame.body}');
            _isConnecting = false;
            _reconnectAttempts++;
            if (_shouldReconnect &&
                _reconnectAttempts < _maxReconnectAttempts) {
              _scheduleReconnect();
            }
          },
          onWebSocketError: (error) {
            debugPrint('WebSocket error: $error');
            _isConnecting = false;
            _reconnectAttempts++;
            if (_reconnectAttempts < _maxReconnectAttempts) {
              _scheduleReconnect();
            }
          },
          // Headers cho ngrok
          webSocketConnectHeaders: {'ngrok-skip-browser-warning': 'true'},
        ),
      );

      _stompClient!.activate();
    } catch (e) {
      debugPrint('STOMP connection failed: $e');
      _isConnecting = false;
      _reconnectAttempts++;
      if (_reconnectAttempts < _maxReconnectAttempts) {
        _scheduleReconnect();
      }
    }
  }

  void _onConnect(StompFrame frame) {
    _isConnected = true;
    _isConnecting = false;
    _isReconnectScheduled = false;
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
    _isConnecting = false;
    debugPrint('STOMP: Disconnected');
    if (_shouldReconnect && _reconnectAttempts < _maxReconnectAttempts) {
      _reconnectAttempts++;
      _scheduleReconnect();
    }
  }

  void _scheduleReconnect() {
    if (_isReconnectScheduled) {
      return;
    }

    _isReconnectScheduled = true;
    // Exponential backoff: 2s, 4s, 8s, 16s, 32s
    final delay = Duration(seconds: 2 * (1 << _reconnectAttempts));
    debugPrint(
      'STOMP: Scheduling reconnect in ${delay.inSeconds}s (attempt ${_reconnectAttempts + 1}/$_maxReconnectAttempts)',
    );

    Future.delayed(delay, () {
      _isReconnectScheduled = false;
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
    _shouldReconnect = false;
    _stompClient?.deactivate();
    _isConnected = false;
    _isConnecting = false;
    _isReconnectScheduled = false;
    debugPrint('STOMP: Manually disconnected');
  }

  bool get isConnected => _isConnected;

  // ============ API Methods ============

  /// Deprecated: seat holding now goes through reservation PROCESSING flow.
  Future<Map<String, dynamic>> holdSeat(int seatId, String userId) async {
    throw UnsupportedError(
      'Giữ ghế trực tiếp không còn được hỗ trợ. Vui lòng dùng luồng tạo booking.',
    );
  }

  /// Deprecated: seat release now follows reservation cancellation/expiry flow.
  Future<Map<String, dynamic>> releaseSeat(int seatId, String userId) async {
    throw UnsupportedError(
      'Nhả ghế trực tiếp không còn được hỗ trợ. Vui lòng dùng luồng booking hiện tại.',
    );
  }
}

/// Singleton instance
final seatWebSocketService = SeatWebSocketService();
