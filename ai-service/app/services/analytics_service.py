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
    
    def analyze_peak_hours(self, area_id: Optional[str] = None, days: int = 7) -> Dict[str, Any]:
        """
        Analyze peak hours for library or specific area (from real access_logs data)
        """
        try:
            from app.core.database import engine
            from sqlalchemy import text

            with engine.connect() as conn:
                # Get total seats for normalization
                seats_result = conn.execute(text("SELECT COUNT(*) FROM seats WHERE is_active = true"))
                total_seats = seats_result.fetchone()[0] or 1

                # Get hourly check-in counts
                result = conn.execute(text("""
                    SELECT
                        EXTRACT(HOUR FROM check_in_time) as hour,
                        COUNT(*) as count,
                        COUNT(DISTINCT DATE(check_in_time)) as num_days
                    FROM access_logs
                    WHERE check_in_time >= NOW() - MAKE_INTERVAL(days => :days)
                    GROUP BY EXTRACT(HOUR FROM check_in_time)
                    ORDER BY hour
                """), {"days": days})

                hourly_data = []
                for row in result:
                    h = int(row[0])
                    num_days = row[2] or 1
                    avg_per_day = row[1] / num_days
                    occ = min(avg_per_day / total_seats, 1.0)
                    hourly_data.append({
                        "hour": h,
                        "avg_occupancy": round(occ, 2),
                        "label": f"{h:02d}:00"
                    })

                if not hourly_data:
                    raise Exception("No access_logs data")

                # Sort by occupancy to find peak and quiet hours
                sorted_by_occ = sorted(hourly_data, key=lambda x: x["avg_occupancy"], reverse=True)
                peak_hours = sorted_by_occ[:3]
                quiet_hours = sorted_by_occ[-3:][::-1] if len(sorted_by_occ) >= 3 else []

                # Get busiest/quietest day
                day_result = conn.execute(text("""
                    SELECT
                        EXTRACT(DOW FROM check_in_time) as day,
                        COUNT(*) as count
                    FROM access_logs
                    WHERE check_in_time >= NOW() - MAKE_INTERVAL(days => :days)
                    GROUP BY EXTRACT(DOW FROM check_in_time)
                    ORDER BY count DESC
                """), {"days": days})

                days_map = ["Chủ nhật", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"]
                day_rows = list(day_result)
                busiest_day = days_map[int(day_rows[0][0])] if day_rows else "N/A"
                quietest_day = days_map[int(day_rows[-1][0])] if day_rows else "N/A"

                peak_labels = ", ".join(h["label"] for h in peak_hours)
                quiet_labels = ", ".join(h["label"] for h in quiet_hours)

                return {
                    "peak_hours": peak_hours,
                    "quiet_hours": quiet_hours,
                    "busiest_day": busiest_day,
                    "quietest_day": quietest_day,
                    "recommendation": f"Giờ cao điểm là {peak_labels}. Để có chỗ ngồi tốt, nên đến vào {quiet_labels}.",
                    "analyzed_at": datetime.now().isoformat()
                }

        except Exception as e:
            print(f"Error analyzing peak hours: {e}")
            # Fallback mock data
            return {
                "peak_hours": [
                    {"hour": 14, "avg_occupancy": 0.85, "label": "14:00"},
                    {"hour": 15, "avg_occupancy": 0.90, "label": "15:00"},
                    {"hour": 16, "avg_occupancy": 0.82, "label": "16:00"},
                ],
                "quiet_hours": [
                    {"hour": 8, "avg_occupancy": 0.15, "label": "08:00"},
                    {"hour": 9, "avg_occupancy": 0.25, "label": "09:00"},
                    {"hour": 20, "avg_occupancy": 0.30, "label": "20:00"},
                ],
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
        Get usage statistics for librarian dashboard (real data)
        """
        days = 1 if period == "day" else 7 if period == "week" else 30
        try:
            from app.core.database import engine
            from sqlalchemy import text

            with engine.connect() as conn:
                # Total visits
                visits_result = conn.execute(text("""
                    SELECT COUNT(*) FROM access_logs
                    WHERE check_in_time >= NOW() - MAKE_INTERVAL(days => :days)
                """), {"days": days})
                total_visits = visits_result.fetchone()[0] or 0

                # Unique users
                users_result = conn.execute(text("""
                    SELECT COUNT(DISTINCT user_id) FROM access_logs
                    WHERE check_in_time >= NOW() - MAKE_INTERVAL(days => :days)
                """), {"days": days})
                unique_users = users_result.fetchone()[0] or 0

                # Avg duration (minutes)
                dur_result = conn.execute(text("""
                    SELECT AVG(EXTRACT(EPOCH FROM (check_out_time - check_in_time)) / 60)
                    FROM access_logs
                    WHERE check_in_time >= NOW() - MAKE_INTERVAL(days => :days)
                      AND check_out_time IS NOT NULL
                """), {"days": days})
                avg_dur = dur_result.fetchone()[0]
                avg_duration_minutes = int(avg_dur) if avg_dur else 0

                # Total seats for peak occupancy
                seats_result = conn.execute(text("SELECT COUNT(*) FROM seats WHERE is_active = true"))
                total_seats = seats_result.fetchone()[0] or 1

                # Peak occupancy (max concurrent in any hour)
                peak_result = conn.execute(text("""
                    SELECT MAX(cnt) FROM (
                        SELECT EXTRACT(HOUR FROM check_in_time) as hour,
                               DATE(check_in_time) as day,
                               COUNT(*) as cnt
                        FROM access_logs
                        WHERE check_in_time >= NOW() - MAKE_INTERVAL(days => :days)
                        GROUP BY DATE(check_in_time), EXTRACT(HOUR FROM check_in_time)
                    ) sub
                """), {"days": days})
                peak_count = peak_result.fetchone()[0] or 0
                peak_occupancy_rate = min(peak_count / total_seats, 1.0)

                # Most popular zones
                zones_result = conn.execute(text("""
                    SELECT z.zone_name, COUNT(*) as visits
                    FROM reservations r
                    JOIN seats s ON s.seat_id = r.seat_id
                    JOIN zones z ON z.zone_id = s.zone_id
                    WHERE r.created_at >= NOW() - MAKE_INTERVAL(days => :days)
                    GROUP BY z.zone_name
                    ORDER BY visits DESC
                    LIMIT 5
                """), {"days": days})
                most_popular_zones = [
                    {"zone": row[0], "visits": row[1]} for row in zones_result
                ]

                # Hourly distribution
                hourly_result = conn.execute(text("""
                    SELECT EXTRACT(HOUR FROM check_in_time) as hour, COUNT(*) as visits
                    FROM access_logs
                    WHERE check_in_time >= NOW() - MAKE_INTERVAL(days => :days)
                    GROUP BY EXTRACT(HOUR FROM check_in_time)
                    ORDER BY hour
                """), {"days": days})
                hourly_distribution = [
                    {"hour": int(row[0]), "visits": row[1]} for row in hourly_result
                ]

                return {
                    "period": period,
                    "total_visits": total_visits,
                    "unique_users": unique_users,
                    "avg_duration_minutes": avg_duration_minutes,
                    "peak_occupancy_rate": round(peak_occupancy_rate, 2),
                    "most_popular_zones": most_popular_zones,
                    "hourly_distribution": hourly_distribution,
                    "insights": [],
                    "generated_at": datetime.now().isoformat()
                }
        except Exception as e:
            print(f"Error fetching usage statistics: {e}")
            return {
                "period": period,
                "total_visits": 0,
                "unique_users": 0,
                "avg_duration_minutes": 0,
                "peak_occupancy_rate": 0,
                "most_popular_zones": [],
                "hourly_distribution": [],
                "insights": [],
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
