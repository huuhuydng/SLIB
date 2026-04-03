# Mobile Development Reference

Chi tiết về phát triển mobile Flutter cho SLIB.

## Tech Stack

- Flutter 3.x + Dart 3.9
- Provider (State Management)
- http, flutter_secure_storage
- Firebase Messaging

## Screen Pattern

```dart
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

class ResourceListScreen extends StatefulWidget {
  const ResourceListScreen({super.key});

  @override
  State<ResourceListScreen> createState() => _ResourceListScreenState();
}

class _ResourceListScreenState extends State<ResourceListScreen> {
  List<Resource> _resources = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadResources();
  }

  Future<void> _loadResources() async {
    try {
      setState(() => _isLoading = true);
      final resources = await ResourceService().getAll();
      setState(() {
        _resources = resources;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }
    
    if (_error != null) {
      return Center(child: Text('Loi: $_error'));
    }
    
    return ListView.builder(
      itemCount: _resources.length,
      itemBuilder: (context, index) => ResourceCard(
        resource: _resources[index],
      ),
    );
  }
}
```

## Service Pattern

```dart
// services/resource_service.dart
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/resource.dart';
import 'api_service.dart';

class ResourceService {
  final ApiService _api = ApiService();

  Future<List<Resource>> getAll() async {
    final response = await _api.get('/slib/resources');
    final List<dynamic> data = json.decode(response.body);
    return data.map((json) => Resource.fromJson(json)).toList();
  }

  Future<Resource> getById(int id) async {
    final response = await _api.get('/slib/resources/$id');
    return Resource.fromJson(json.decode(response.body));
  }

  Future<Resource> create(ResourceRequest request) async {
    final response = await _api.post(
      '/slib/resources',
      body: request.toJson(),
    );
    return Resource.fromJson(json.decode(response.body));
  }
}
```

## Model Pattern

```dart
// models/resource.dart
class Resource {
  final int id;
  final String name;
  final ResourceStatus status;
  final DateTime createdAt;

  Resource({
    required this.id,
    required this.name,
    required this.status,
    required this.createdAt,
  });

  factory Resource.fromJson(Map<String, dynamic> json) {
    return Resource(
      id: json['id'],
      name: json['name'],
      status: ResourceStatus.values.firstWhere(
        (e) => e.name == json['status'],
      ),
      createdAt: DateTime.parse(json['createdAt']),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'status': status.name,
    };
  }
}

enum ResourceStatus { AVAILABLE, BOOKED, MAINTENANCE }
```

## API Service

```dart
// services/api_service.dart
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class ApiService {
  static const String baseUrl = 'http://localhost:8080';
  final _storage = const FlutterSecureStorage();

  Future<Map<String, String>> _getHeaders() async {
    final token = await _storage.read(key: 'token');
    return {
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
  }

  Future<http.Response> get(String path) async {
    final headers = await _getHeaders();
    return http.get(Uri.parse('$baseUrl$path'), headers: headers);
  }

  Future<http.Response> post(String path, {Object? body}) async {
    final headers = await _getHeaders();
    return http.post(
      Uri.parse('$baseUrl$path'),
      headers: headers,
      body: body != null ? jsonEncode(body) : null,
    );
  }
}
```

## Widget Pattern

```dart
// widgets/resource_card.dart
class ResourceCard extends StatelessWidget {
  final Resource resource;
  final VoidCallback? onTap;

  const ResourceCard({
    super.key,
    required this.resource,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                resource.name,
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 8),
              _StatusBadge(status: resource.status),
            ],
          ),
        ),
      ),
    );
  }
}
```

## Navigation

```dart
// main.dart routes
MaterialApp(
  routes: {
    '/': (context) => const MainScreen(),
    '/login': (context) => const LoginScreen(),
    '/booking': (context) => const BookingScreen(),
    '/chat': (context) => const ChatScreen(),
    '/profile': (context) => const ProfileScreen(),
  },
);

// Navigate
Navigator.pushNamed(context, '/booking');
Navigator.pushReplacementNamed(context, '/login');
```
