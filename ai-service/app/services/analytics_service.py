"""
Analytics AI Service
AI-powered analytics for library usage patterns
"""

from typing import List, Dict, Any, Optional
from datetime import datetime, timedelta
import random


class AnalyticsAIService:
    """
    AI Service for library analytics and insights
    
    Future features:
    - Peak hours prediction
    - Optimal time slot recommendations
    - Usage pattern analysis
    - Capacity forecasting
    """
    
    def __init__(self):
        # Simulated historical data (in production, fetch from database)
        self._mock_usage_data = self._generate_mock_data()
    
    def analyze_peak_hours(self, area_id: Optional[str] = None) -> Dict[str, Any]:
        """
        Analyze peak hours for library or specific area
        
        Returns:
            {
                "peak_hours": [{"hour": 14, "avg_occupancy": 0.85}, ...],
                "quiet_hours": [{"hour": 8, "avg_occupancy": 0.2}, ...],
                "recommendation": "Giờ cao điểm là 14h-16h. Nên đến trước 10h để có chỗ tốt."
            }
        """
        # Simulated analysis
        peak_hours = [
            {"hour": 14, "avg_occupancy": 0.85, "label": "14:00"},
            {"hour": 15, "avg_occupancy": 0.90, "label": "15:00"},
            {"hour": 16, "avg_occupancy": 0.82, "label": "16:00"},
        ]
        
        quiet_hours = [
            {"hour": 8, "avg_occupancy": 0.15, "label": "08:00"},
            {"hour": 9, "avg_occupancy": 0.25, "label": "09:00"},
            {"hour": 20, "avg_occupancy": 0.30, "label": "20:00"},
        ]
        
        return {
            "peak_hours": peak_hours,
            "quiet_hours": quiet_hours,
            "busiest_day": "Thứ 4",
            "quietest_day": "Chủ nhật",
            "recommendation": "Giờ cao điểm là 14h-16h. Để có chỗ ngồi tốt, nên đến trước 10h sáng hoặc sau 18h.",
            "analyzed_at": datetime.now().isoformat()
        }
    
    def recommend_time_slots(
        self, 
        user_preferences: Optional[Dict] = None,
        duration_hours: int = 2
    ) -> Dict[str, Any]:
        """
        AI-powered time slot recommendations for students
        
        Args:
            user_preferences: {"preferred_zone": "A1", "quiet": True}
            duration_hours: Desired study duration
            
        Returns:
            Recommended time slots with availability prediction
        """
        today = datetime.now().date()
        
        recommendations = [
            {
                "date": str(today),
                "start_time": "08:00",
                "end_time": "10:00",
                "predicted_availability": 0.85,
                "zone_suggestion": "Khu yên tĩnh A",
                "reason": "Buổi sáng ít người, phù hợp để tập trung"
            },
            {
                "date": str(today),
                "start_time": "18:00",
                "end_time": "20:00",
                "predicted_availability": 0.70,
                "zone_suggestion": "Khu học nhóm B",
                "reason": "Giờ chiều muộn, đa số sinh viên đã về"
            },
            {
                "date": str(today + timedelta(days=1)),
                "start_time": "09:00",
                "end_time": "11:00",
                "predicted_availability": 0.90,
                "zone_suggestion": "Khu máy tính C",
                "reason": "Ngày mai buổi sáng dự kiến vắng"
            }
        ]
        
        return {
            "recommendations": recommendations,
            "based_on": "Dữ liệu sử dụng 30 ngày gần nhất",
            "model_confidence": 0.82,
            "generated_at": datetime.now().isoformat()
        }
    
    def get_usage_statistics(
        self,
        period: str = "week",
        area_id: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Get usage statistics for librarian dashboard
        
        Args:
            period: "day", "week", "month"
            area_id: Optional specific area
        """
        return {
            "period": period,
            "total_visits": 1250,
            "unique_users": 485,
            "avg_duration_minutes": 127,
            "peak_occupancy_rate": 0.92,
            "most_popular_zones": [
                {"zone": "Khu yên tĩnh A", "visits": 450},
                {"zone": "Khu học nhóm B", "visits": 380},
                {"zone": "Khu máy tính C", "visits": 420}
            ],
            "hourly_distribution": [
                {"hour": h, "visits": random.randint(20, 100)} 
                for h in range(8, 22)
            ],
            "insights": [
                "📈 Lượng truy cập tăng 15% so với tuần trước",
                "⏰ Giờ cao điểm dịch chuyển từ 15h sang 14h",
                "👥 Sinh viên năm 3 chiếm 40% lượng sử dụng"
            ],
            "generated_at": datetime.now().isoformat()
        }
    
    def predict_capacity(
        self,
        target_datetime: datetime,
        zone_id: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Predict library/zone capacity at a specific time
        """
        # Simulated prediction
        hour = target_datetime.hour
        day_of_week = target_datetime.weekday()
        
        # Simple heuristic (replace with ML model in production)
        base_occupancy = 0.4
        if 13 <= hour <= 17:
            base_occupancy = 0.8
        elif 8 <= hour <= 10:
            base_occupancy = 0.3
        
        if day_of_week in [5, 6]:  # Weekend
            base_occupancy *= 0.5
        
        return {
            "target_datetime": target_datetime.isoformat(),
            "predicted_occupancy": round(base_occupancy + random.uniform(-0.1, 0.1), 2),
            "confidence": 0.78,
            "factors": [
                "Ngày trong tuần",
                "Khung giờ",
                "Mùa thi (nếu có)"
            ]
        }
    
    def _generate_mock_data(self) -> List[Dict]:
        """Generate mock historical usage data"""
        data = []
        for day in range(30):
            for hour in range(8, 22):
                data.append({
                    "date": (datetime.now() - timedelta(days=day)).date().isoformat(),
                    "hour": hour,
                    "occupancy": random.uniform(0.2, 0.9),
                    "zone": random.choice(["A", "B", "C"])
                })
        return data


# Singleton instance
analytics_ai_service = AnalyticsAIService()
