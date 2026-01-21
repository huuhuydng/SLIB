import 'dart:math';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/area.dart';
import 'package:slib/models/area_factory.dart';
import 'package:slib/models/zone_occupancy.dart';
import 'package:slib/services/booking_service.dart';
import 'package:slib/views/home/widgets/booking_zone.dart';

/// Màn hình sơ đồ thư viện - hiển thị zones và factories giống admin
class LibraryMapScreen extends StatefulWidget {
  const LibraryMapScreen({super.key});

  @override
  State<LibraryMapScreen> createState() => _LibraryMapScreenState();
}

class _LibraryMapScreenState extends State<LibraryMapScreen> {
  final BookingService _bookingService = BookingService();
  
  // Data
  List<Area> _areas = [];
  List<ZoneOccupancy> _zones = [];
  List<AreaFactory> _factories = [];
  
  // Selected area
  Area? _selectedArea;
  
  // Loading state
  bool _isLoading = true;
  String? _errorMessage;
  
  // Canvas size (calculated from zones and factories)
  double _canvasWidth = 800;
  double _canvasHeight = 600;
  
  // Transform controller for InteractiveViewer
  final TransformationController _transformController = TransformationController();

  @override
  void initState() {
    super.initState();
    _loadAreas();
  }

  @override
  void dispose() {
    _transformController.dispose();
    super.dispose();
  }

  Future<void> _loadAreas() async {
    try {
      setState(() {
        _isLoading = true;
        _errorMessage = null;
      });
      
      final areas = await _bookingService.getAllAreas();
      setState(() {
        _areas = areas.where((a) => a.isActive).toList();
        if (_areas.isNotEmpty) {
          _selectedArea = _areas.first;
        }
        _isLoading = false;
      });
      
      if (_selectedArea != null) {
        await _loadAreaContent();
      }
    } catch (e) {
      setState(() {
        _isLoading = false;
        _errorMessage = 'Không thể tải danh sách khu vực: $e';
      });
    }
  }

  Future<void> _loadAreaContent() async {
    if (_selectedArea == null) return;
    
    try {
      setState(() => _isLoading = true);
      
      // Load zones with occupancy and factories in parallel
      final results = await Future.wait([
        _bookingService.getZoneOccupancy(_selectedArea!.areaId),
        _bookingService.getFactoriesByArea(_selectedArea!.areaId),
      ]);
      
      final zones = results[0] as List<ZoneOccupancy>;
      final factories = results[1] as List<AreaFactory>;
      
      // Calculate canvas size based on content
      _calculateCanvasSize(zones, factories);
      
      setState(() {
        _zones = zones;
        _factories = factories;
        _isLoading = false;
      });
      
      // Fit to screen after loading
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _fitToScreen();
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
        _errorMessage = 'Không thể tải dữ liệu: $e';
      });
    }
  }

  void _calculateCanvasSize(List<ZoneOccupancy> zones, List<AreaFactory> factories) {
    double maxX = 800;
    double maxY = 600;
    
    for (final zone in zones) {
      final right = zone.positionX + zone.width;
      final bottom = zone.positionY + zone.height;
      if (right > maxX) maxX = right.toDouble();
      if (bottom > maxY) maxY = bottom.toDouble();
    }
    
    for (final factory in factories) {
      final right = factory.positionX + factory.width;
      final bottom = factory.positionY + factory.height;
      if (right > maxX) maxX = right.toDouble();
      if (bottom > maxY) maxY = bottom.toDouble();
    }
    
    _canvasWidth = maxX + 50;  // Add padding
    _canvasHeight = maxY + 50;
  }

  void _fitToScreen() {
    final screenSize = MediaQuery.of(context).size;
    final appBarHeight = AppBar().preferredSize.height;
    final availableWidth = screenSize.width - 32;  // Padding
    final availableHeight = screenSize.height - appBarHeight - 200;  // Header, tabs, legend
    
    final scaleX = availableWidth / _canvasWidth;
    final scaleY = availableHeight / _canvasHeight;
    final scale = min(scaleX, scaleY).clamp(0.3, 1.0);
    
    _transformController.value = Matrix4.identity()..scale(scale);
  }

  Color _getZoneColor(int densityLevel) {
    switch (densityLevel) {
      case 2: return Colors.red;      // Đông (>80%)
      case 1: return Colors.orange;   // Vừa (50-80%)
      default: return Colors.green;   // Vắng (<50%)
    }
  }

  void _onZoneTap(ZoneOccupancy zone) {
    // Navigate to seat selection screen
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => SeatSelectionScreen(
          zoneName: zone.zoneName,
          zoneId: zone.zoneId,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Sơ đồ thư viện'),
        backgroundColor: AppColors.brandColor,
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadAreaContent,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _errorMessage != null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(_errorMessage!, style: const TextStyle(color: Colors.red)),
                      const SizedBox(height: 16),
                      ElevatedButton(
                        onPressed: _loadAreas,
                        child: const Text('Thử lại'),
                      ),
                    ],
                  ),
                )
              : Column(
                  children: [
                    // Area tabs
                    _buildAreaTabs(),
                    // Legend
                    _buildLegend(),
                    // Map
                    Expanded(child: _buildMap()),
                    // Instructions
                    _buildInstructions(),
                  ],
                ),
    );
  }

  Widget _buildAreaTabs() {
    if (_areas.length <= 1) return const SizedBox.shrink();
    
    return Container(
      height: 50,
      color: Colors.grey[200],
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 8),
        itemCount: _areas.length,
        itemBuilder: (context, index) {
          final area = _areas[index];
          final isSelected = _selectedArea?.areaId == area.areaId;
          
          return GestureDetector(
            onTap: () {
              setState(() => _selectedArea = area);
              _loadAreaContent();
            },
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
              margin: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
              decoration: BoxDecoration(
                color: isSelected ? AppColors.brandColor : Colors.white,
                borderRadius: BorderRadius.circular(20),
                boxShadow: isSelected ? [
                  BoxShadow(
                    color: AppColors.brandColor.withOpacity(0.3),
                    blurRadius: 4,
                    offset: const Offset(0, 2),
                  ),
                ] : null,
              ),
              child: Text(
                area.areaName,
                style: TextStyle(
                  color: isSelected ? Colors.white : Colors.black87,
                  fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildLegend() {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
      color: Colors.grey[100],
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          _buildLegendItem(Colors.green, 'Vắng'),
          const SizedBox(width: 24),
          _buildLegendItem(Colors.orange, 'Vừa'),
          const SizedBox(width: 24),
          _buildLegendItem(Colors.red, 'Đông'),
          const SizedBox(width: 24),
          _buildLegendItem(Colors.grey, 'Vật cản'),
        ],
      ),
    );
  }

  Widget _buildLegendItem(Color color, String label) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 16,
          height: 16,
          decoration: BoxDecoration(
            color: color.withOpacity(0.6),
            borderRadius: BorderRadius.circular(4),
            border: Border.all(color: color, width: 1.5),
          ),
        ),
        const SizedBox(width: 6),
        Text(label, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w500)),
      ],
    );
  }

  Widget _buildMap() {
    if (_zones.isEmpty && _factories.isEmpty) {
      return const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.map_outlined, size: 64, color: Colors.grey),
            SizedBox(height: 16),
            Text('Chưa có sơ đồ cho khu vực này', style: TextStyle(color: Colors.grey)),
          ],
        ),
      );
    }

    return InteractiveViewer(
      transformationController: _transformController,
      constrained: false,
      boundaryMargin: const EdgeInsets.all(100),
      minScale: 0.2,
      maxScale: 3.0,
      child: Container(
        width: _canvasWidth,
        height: _canvasHeight,
        decoration: BoxDecoration(
          color: Colors.grey[50],
          border: Border.all(color: Colors.grey[300]!),
        ),
        child: Stack(
          children: [
            // Grid background
            ..._buildGridLines(),
            // Factories (obstacles) - render first, below zones
            ..._factories.map((factory) => _buildFactoryWidget(factory)),
            // Zones with occupancy coloring
            ..._zones.map((zone) => _buildZoneWidget(zone)),
          ],
        ),
      ),
    );
  }

  List<Widget> _buildGridLines() {
    final List<Widget> lines = [];
    const gridSize = 50.0;
    
    // Vertical lines
    for (double x = 0; x <= _canvasWidth; x += gridSize) {
      lines.add(Positioned(
        left: x,
        top: 0,
        child: Container(
          width: 1,
          height: _canvasHeight,
          color: Colors.grey[200],
        ),
      ));
    }
    
    // Horizontal lines
    for (double y = 0; y <= _canvasHeight; y += gridSize) {
      lines.add(Positioned(
        left: 0,
        top: y,
        child: Container(
          width: _canvasWidth,
          height: 1,
          color: Colors.grey[200],
        ),
      ));
    }
    
    return lines;
  }

  Widget _buildFactoryWidget(AreaFactory factory) {
    return Positioned(
      left: factory.positionX.toDouble(),
      top: factory.positionY.toDouble(),
      child: Container(
        width: factory.width.toDouble(),
        height: factory.height.toDouble(),
        decoration: BoxDecoration(
          color: Colors.grey[400],
          borderRadius: BorderRadius.circular(6),
          border: Border.all(color: Colors.grey[600]!, width: 1.5),
        ),
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(4),
            child: Text(
              factory.factoryName,
              style: const TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.w600,
                color: Colors.white,
              ),
              textAlign: TextAlign.center,
              overflow: TextOverflow.ellipsis,
              maxLines: 2,
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildZoneWidget(ZoneOccupancy zone) {
    final color = _getZoneColor(zone.densityLevel);
    
    return Positioned(
      left: zone.positionX.toDouble(),
      top: zone.positionY.toDouble(),
      child: GestureDetector(
        onTap: () => _onZoneTap(zone),
        child: Container(
          width: zone.width.toDouble(),
          height: zone.height.toDouble(),
          decoration: BoxDecoration(
            color: color.withOpacity(0.3),
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: color, width: 2),
            boxShadow: [
              BoxShadow(
                color: color.withOpacity(0.2),
                blurRadius: 4,
                offset: const Offset(0, 2),
              ),
            ],
          ),
          child: Stack(
            children: [
              // Zone name
              Positioned(
                left: 8,
                top: 8,
                right: 8,
                child: Text(
                  zone.zoneName,
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.bold,
                    color: color.withOpacity(0.9),
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              // Seat count
              Positioned(
                left: 8,
                bottom: 8,
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.9),
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: Text(
                    '${zone.availableSeats}/${zone.totalSeats} trống',
                    style: TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.w600,
                      color: color,
                    ),
                  ),
                ),
              ),
              // Tap indicator
              Center(
                child: Icon(
                  Icons.touch_app,
                  size: 28,
                  color: color.withOpacity(0.4),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildInstructions() {
    return Container(
      padding: const EdgeInsets.all(12),
      color: Colors.blue[50],
      child: Row(
        children: [
          Icon(Icons.info_outline, color: Colors.blue[700], size: 20),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              'Chạm vào khu vực để chọn ghế • Zoom: pinch • Di chuyển: kéo',
              style: TextStyle(color: Colors.blue[700], fontSize: 12),
            ),
          ),
        ],
      ),
    );
  }
}
