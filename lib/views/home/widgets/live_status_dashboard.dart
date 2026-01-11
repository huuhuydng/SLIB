import 'package:flutter/material.dart';
import 'package:slib/models/user_profile.dart';

class LiveStatusDashboard extends StatelessWidget {
  final UserProfile? user;
  
  const LiveStatusDashboard({super.key, this.user});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Column(
        children: [
          // Dòng 1: Trạng thái thư viện (Live Occupancy)
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: Colors.orange.withOpacity(0.1),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.people_alt_rounded, color: Colors.orange),
              ),
              const SizedBox(width: 15),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      "Trạng thái thư viện",
                      style: TextStyle(color: Colors.grey, fontSize: 13),
                    ),
                    const SizedBox(height: 4),
                    Row(
                      children: const [
                        Text(
                          "Khá đông đúc",
                          style: TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 16,
                              color: Colors.black87),
                        ),
                        SizedBox(width: 8),
                        Icon(Icons.circle, size: 8, color: Colors.orange),
                      ],
                    ),
                  ],
                ),
              ),
              // % Đông đúc
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: Colors.orange,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: const Text(
                  "78% Full",
                  style: TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                      fontSize: 12),
                ),
              )
            ],
          ),
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 15),
            child: Divider(height: 1),
          ),
          // Dòng 2: Điểm uy tín & Vi phạm (Personal Stats)
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              _buildStatItem(
                label: "Điểm uy tín",
                value: "${user?.reputationScore ?? 100}",
                valueColor: Colors.green,
                icon: Icons.shield_outlined,
              ),
              Container(width: 1, height: 40, color: Colors.grey[200]),
              _buildStatItem(
                label: "Giờ đã học",
                value: "42.5h",
                valueColor: Colors.blue,
                icon: Icons.timer_outlined,
              ),
              Container(width: 1, height: 40, color: Colors.grey[200]),
              _buildStatItem(
                label: "Vi phạm",
                value: "0",
                valueColor: Colors.black87,
                icon: Icons.warning_amber_rounded,
              ),
            ],
          )
        ],
      ),
    );
  }

  Widget _buildStatItem({
    required String label,
    required String value,
    required Color valueColor,
    required IconData icon,
  }) {
    return Column(
      children: [
        Row(
          children: [
            Icon(icon, size: 14, color: Colors.grey),
            const SizedBox(width: 4),
            Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
          ],
        ),
        const SizedBox(height: 4),
        Text(
          value,
          style: TextStyle(
              fontWeight: FontWeight.bold, fontSize: 18, color: valueColor),
        ),
      ],
    );
  }
}
