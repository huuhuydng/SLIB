import 'dart:async';
import 'dart:convert';
import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/services/auth/auth_service.dart';

/// Service quản lý trạng thái thư viện real-time
class LibraryStatusService extends ChangeNotifier {
  static const Duration _fallbackPollingInterval = Duration(seconds: 30);

  final AuthService _authService;

  StompClient? _stompClient;
  Timer? _fallbackTimer;
  bool _wsConnected = false;
  bool _isWsConnecting = false;
  bool _isInitialized = false;
  bool _isInitializing = false;

  // Data
  int _totalSeats = 0;
  int _occupiedSeats = 0;
  double _occupancyRate = 0.0;
  int _currentlyInLibrary = 0;
  bool _isLoading = true;
  bool _hasLoadedData = false;
  String? _lastError;

  // Getters
  int get totalSeats => _totalSeats;
  int get occupiedSeats => _occupiedSeats;
  double get occupancyRate => _occupancyRate;
  int get currentlyInLibrary => _currentlyInLibrary;
  bool get isLoading => _isLoading;
  bool get isWsConnected => _wsConnected;
  bool get hasLoadedData => _hasLoadedData;
  bool get hasSyncError => !_isLoading && !_hasLoadedData;
  String? get lastError => _lastError;

  /// Trạng thái text dựa trên occupancyRate
  String get statusText {
    if (!_hasLoadedData) return 'Đang cập nhật';
    if (_occupancyRate <= 30) return 'Vắng';
    if (_occupancyRate <= 60) return 'Bình thường';
    if (_occupancyRate <= 80) return 'Khá đông đúc';
    return 'Rất đông';
  }

  /// Color tương ứng
  Color get statusColor {
    if (!_hasLoadedData) return Colors.grey;
    if (_occupancyRate <= 30) return Colors.green;
    if (_occupancyRate <= 60) return Colors.blue;
    if (_occupancyRate <= 80) return Colors.orange;
    return Colors.red;
  }

  /// Badge color (cho container % Full)
  Color get badgeColor {
    if (!_hasLoadedData) return Colors.grey;
    if (_occupancyRate <= 30) return Colors.green;
    if (_occupancyRate <= 60) return Colors.blue;
    if (_occupancyRate <= 80) return Colors.orange;
    return Colors.red;
  }

  LibraryStatusService(this._authService);

  String get _baseUrl => '${ApiConstants.domain}/slib';

  Future<String?> get _token async => await _authService.getToken();

  String _normalizeErrorMessage(Object? error) {
    final raw = (error?.toString() ?? '').trim();
    if (raw.isEmpty) {
      return 'Không rõ nguyên nhân đồng bộ thất bại.';
    }

    if (raw.contains('SocketException') ||
        raw.contains('Failed host lookup') ||
        raw.contains('Connection refused')) {
      return 'Không kết nối được máy chủ.';
    }

    if (raw.contains('HandshakeException') || raw.contains('CERTIFICATE')) {
      return 'Kết nối bảo mật tới máy chủ thất bại.';
    }

    if (raw.contains('401') || raw.contains('403')) {
      return 'Phiên đăng nhập không còn hợp lệ.';
    }

    return raw;
  }

  /// Initialize: load data + connect WebSocket
  Future<void> initialize() async {
    if (_isInitializing) return;

    _isInitializing = true;
    try {
      final token = await _token;
      if (token == null || token.isEmpty) {
        debugPrint('[LibraryStatus] Chưa thể initialize vì chưa có token');
        _lastError = 'Chưa có token đăng nhập.';
        _isLoading = false;
        notifyListeners();
        return;
      }

      if (_isInitialized) {
        await fetchLibraryStatus();
        await _connectWebSocket();
        _ensureFallbackPolling();
        return;
      }

      await fetchLibraryStatus();
      await _connectWebSocket();
      _ensureFallbackPolling();
      _isInitialized = true;
    } finally {
      _isInitializing = false;
    }
  }

  /// Fetch library status từ REST API
  Future<void> fetchLibraryStatus() async {
    try {
      _isLoading = true;
      notifyListeners();

      final token = await _token;
      if (token == null || token.isEmpty) {
        debugPrint(
          '[LibraryStatus] Không tìm thấy token để tải trạng thái thư viện',
        );
        _lastError = 'Không tìm thấy token đăng nhập.';
        _isLoading = false;
        notifyListeners();
        return;
      }

      final response = await _authService.authenticatedRequest(
        'GET',
        Uri.parse('$_baseUrl/dashboard/library-status'),
        headers: {'Content-Type': 'application/json'},
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(utf8.decode(response.bodyBytes));
        _totalSeats = (data['totalSeats'] as num?)?.toInt() ?? 0;
        _occupiedSeats = (data['occupiedSeats'] as num?)?.toInt() ?? 0;
        final rawOccupancyRate =
            (data['occupancyRate'] as num?)?.toDouble() ?? 0.0;
        _occupancyRate = math.max(0, math.min(rawOccupancyRate, 100));
        _currentlyInLibrary =
            (data['currentlyInLibrary'] as num?)?.toInt() ?? 0;
        _hasLoadedData = true;
        _lastError = null;
        _isLoading = false;
        notifyListeners();
        return;
      }

      debugPrint(
        '[LibraryStatus] API lỗi ${response.statusCode}: ${utf8.decode(response.bodyBytes)}',
      );
      _lastError = 'API lỗi ${response.statusCode} khi tải trạng thái thư viện.';
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      debugPrint('[LibraryStatus] Error fetching: $e');
      _lastError = _normalizeErrorMessage(e);
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> retrySync() async {
    await fetchLibraryStatus();
    await _connectWebSocket();
    _ensureFallbackPolling();
  }

  /// Connect WebSocket STOMP → subscribe /topic/dashboard
  Future<void> _connectWebSocket() async {
    if (_wsConnected || _isWsConnecting) return;

    try {
      _isWsConnecting = true;
      final token = await _token;
      if (token == null || token.isEmpty) {
        debugPrint('[LibraryStatus] Bỏ qua kết nối WebSocket vì chưa có token');
        _isWsConnecting = false;
        _ensureFallbackPolling();
        return;
      }

      String wsUrl = ApiConstants.domain;
      if (wsUrl.startsWith('https://')) {
        wsUrl = wsUrl.replaceFirst('https://', 'wss://');
      } else if (wsUrl.startsWith('http://')) {
        wsUrl = wsUrl.replaceFirst('http://', 'ws://');
      }
      final stompUrl = '$wsUrl/ws/websocket';

      debugPrint('[LibraryStatus] Connecting WebSocket to $stompUrl');

      _stompClient = StompClient(
        config: StompConfig(
          url: stompUrl,
          stompConnectHeaders: {'Authorization': 'Bearer $token'},
          webSocketConnectHeaders: {'ngrok-skip-browser-warning': 'true'},
          onConnect: _onStompConnected,
          onWebSocketError: (error) {
            debugPrint('[LibraryStatus] WebSocket error: $error');
            _wsConnected = false;
            _isWsConnecting = false;
            _ensureFallbackPolling();
          },
          onDisconnect: (_) {
            debugPrint('[LibraryStatus] Disconnected');
            _wsConnected = false;
            _isWsConnecting = false;
            _ensureFallbackPolling();
          },
          reconnectDelay: const Duration(seconds: 5),
        ),
      );
      _stompClient!.activate();
    } catch (e) {
      debugPrint('[LibraryStatus] Connection error: $e');
      _isWsConnecting = false;
      _ensureFallbackPolling();
    }
  }

  void _onStompConnected(StompFrame frame) {
    debugPrint('[LibraryStatus] Connected, subscribing to /topic/dashboard');
    _wsConnected = true;
    _isWsConnecting = false;
    _stopFallbackPolling();
    fetchLibraryStatus();

    _stompClient?.subscribe(
      destination: '/topic/dashboard',
      callback: (StompFrame frame) {
        if (frame.body != null) {
          try {
            final data = jsonDecode(frame.body!);
            debugPrint(
              '[LibraryStatus] Dashboard update: ${data['type']} - ${data['action']}',
            );
            // Khi nhận event dashboard update → refresh data từ API
            fetchLibraryStatus();
          } catch (e) {
            debugPrint('[LibraryStatus] Parse error: $e');
          }
        }
      },
    );
  }

  void _ensureFallbackPolling() {
    if (_fallbackTimer != null) return;

    _fallbackTimer = Timer.periodic(_fallbackPollingInterval, (_) async {
      if (_wsConnected || _isWsConnecting) return;
      await fetchLibraryStatus();
      await _connectWebSocket();
    });
  }

  void _stopFallbackPolling() {
    _fallbackTimer?.cancel();
    _fallbackTimer = null;
  }

  /// Disconnect và cleanup
  void clearData() {
    _stompClient?.deactivate();
    _stompClient = null;
    _stopFallbackPolling();
    _wsConnected = false;
    _isWsConnecting = false;
    _isInitialized = false;
    _totalSeats = 0;
    _occupiedSeats = 0;
    _occupancyRate = 0.0;
    _currentlyInLibrary = 0;
    _isLoading = true;
    _hasLoadedData = false;
    _lastError = null;
    notifyListeners();
  }

  @override
  void dispose() {
    _stompClient?.deactivate();
    _stompClient = null;
    _stopFallbackPolling();
    _isWsConnecting = false;
    super.dispose();
  }
}
