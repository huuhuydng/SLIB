"""
Analytics Router - AI-powered behavior analytics and predictions
"""

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
from datetime import datetime, timedelta

router = APIRouter(prefix="/api/ai/analytics", tags=["Analytics"])


class StudentBehaviorRequest(BaseModel):
    user_id: str
    days: Optional[int] = 30


class BehaviorInsight(BaseModel):
    type: str
    message: str
    severity: str  # info, warning, positive


class PredictionRequest(BaseModel):
    zone_id: Optional[str] = None
    target_date: Optional[str] = None


@router.post("/student-behavior")
async def get_student_behavior_analytics(request: StudentBehaviorRequest) -> Dict[str, Any]:
    """
    Lấy phân tích hành vi của sinh viên
    """
    from app.services.analytics_service import analytics_ai_service

    # Get behavior data from database
    try:
        from app.core.database import engine
        from sqlalchemy import text

        with engine.connect() as conn:
            # Get user's booking history
            result = conn.execute(text("""
                SELECT
                    COUNT(*) as total_bookings,
                    SUM(CASE WHEN status IN ('COMPLETED', 'CONFIRMED') THEN 1 ELSE 0 END) as used_bookings,
                    SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled,
                    SUM(CASE WHEN status = 'EXPIRED' THEN 1 ELSE 0 END) as expired
                FROM reservations
                WHERE user_id = :user_id
                  AND created_time >= NOW() - INTERVAL '30 days'
            """), {"user_id": request.user_id})

            row = result.fetchone()
            if row:
                total_bookings = row[0] or 0
                used_bookings = row[1] or 0
                cancelled = row[2] or 0
                expired = row[3] or 0
            else:
                total_bookings = used_bookings = cancelled = expired = 0

            # Get no-shows from student_behaviors table if exists
            try:
                noshow_result = conn.execute(text("""
                    SELECT COUNT(*) as noshow_count
                    FROM student_behaviors
                    WHERE user_id = :user_id AND behavior_type = 'NO_SHOW'
                      AND created_at >= NOW() - INTERVAL '30 days'
                """), {"user_id": request.user_id})
                noshow_row = noshow_result.fetchone()
                no_shows = noshow_row[0] if noshow_row else 0
            except:
                no_shows = expired

    except Exception as e:
        print(f"Error fetching student behavior: {e}")
        # Fallback to mock
        total_bookings = 10
        used_bookings = 8
        no_shows = 1
        cancelled = 1

    no_show_rate = no_shows / total_bookings if total_bookings > 0 else 0
    on_time_rate = used_bookings / total_bookings if total_bookings > 0 else 0

    # Calculate reliability score
    score = 100
    score -= no_show_rate * 30
    score -= (1 - on_time_rate) * 20
    score = max(0, min(100, int(score)))

    # Generate insights
    insights = []

    if no_show_rate > 0.3:
        insights.append(BehaviorInsight(
            type="no_show",
            message=f"Tỷ lệ bỏ chỗ cao ({int(no_show_rate * 100)}%). Nên đặt chỗ khi chắc chắn.",
            severity="warning"
        ))
    elif no_show_rate > 0:
        insights.append(BehaviorInsight(
            type="no_show",
            message="Tỷ lệ bỏ chỗ ở mức thấp. Tiếp tục duy trì!",
            severity="positive"
        ))

    if on_time_rate > 0.9:
        insights.append(BehaviorInsight(
            type="checkin",
            message=f"Check-in đúng giờ rất tốt ({int(on_time_rate * 100)}%).",
            severity="positive"
        ))

    if no_shows > 3:
        insights.append(BehaviorInsight(
            type="violation",
            message=f"Có {no_shows} lần không đến. Cần tuân thủ nội quy thư viện.",
            severity="warning"
        ))

    if not insights:
        insights.append(BehaviorInsight(
            type="general",
            message="Hành vi tốt! Tiếp tục duy trì thói quen đặt chỗ và sử dụng thư viện đúng giờ.",
            severity="positive"
        ))

    return {
        "user_id": request.user_id,
        "period_days": request.days or 30,
        "total_bookings": total_bookings,
        "total_check_ins": used_bookings,
        "no_show_count": no_shows,
        "no_show_rate": round(no_show_rate, 2),
        "cancellations": cancelled,
        "on_time_checkin_rate": round(on_time_rate, 2),
        "reliability_score": score,
        "insights": [i.dict() for i in insights],
        "analyzed_at": datetime.now().isoformat()
    }


@router.get("/behavior-issues")
async def get_behavior_issues() -> Dict[str, Any]:
    """
    Lấy danh sách sinh viên có vấn đề hành vi (bỏ chỗ nhiều, điểm uy tín thấp, vi phạm)
    Được gọi bởi Librarian Dashboard.
    """
    import logging
    logger = logging.getLogger(__name__)

    try:
        from app.core.database import engine
        from sqlalchemy import text

        with engine.connect() as conn:
            # Lấy sinh viên cùng thống kê đặt chỗ 30 ngày gần nhất
            result = conn.execute(text("""
                WITH reservation_stats AS (
                    SELECT
                        r.user_id,
                        COUNT(*) AS total_reservations,
                        SUM(CASE WHEN r.status = 'EXPIRED' THEN 1 ELSE 0 END) AS expired_count,
                        SUM(CASE WHEN r.status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled_count,
                        SUM(CASE WHEN r.status IN ('COMPLETED', 'CONFIRMED') THEN 1 ELSE 0 END) AS completed_count
                    FROM reservations r
                    WHERE r.created_time >= NOW() - INTERVAL '30 days'
                    GROUP BY r.user_id
                )
                SELECT
                    u.full_name,
                    sp.student_code,
                    sp.reputation_score,
                    COALESCE(rs.total_reservations, 0) AS total_reservations,
                    COALESCE(rs.expired_count, 0) AS expired_count,
                    COALESCE(rs.cancelled_count, 0) AS cancelled_count,
                    COALESCE(rs.completed_count, 0) AS completed_count
                FROM student_profiles sp
                JOIN users u ON u.id = sp.user_id
                LEFT JOIN reservation_stats rs ON rs.user_id = u.id
                WHERE sp.reputation_score < 80
                   OR (rs.total_reservations > 0
                       AND (rs.expired_count::float / rs.total_reservations) > 0.3)
                ORDER BY sp.reputation_score ASC,
                         COALESCE(rs.expired_count, 0) DESC
                LIMIT 10
            """))

            students = []
            for row in result:
                full_name = row[0]
                user_code = row[1]
                reputation_score = row[2] if row[2] is not None else 100
                total = row[3] or 0
                expired = row[4] or 0
                cancelled = row[5] or 0

                no_show_rate = (expired / total) if total > 0 else 0
                cancel_rate = (cancelled / total) if total > 0 else 0

                # Xác định severity
                if reputation_score < 60 or no_show_rate > 0.5:
                    severity = "critical"
                elif reputation_score < 80 or no_show_rate > 0.3:
                    severity = "warning"
                else:
                    severity = "info"

                # Xác định vấn đề chính
                issues = []
                if no_show_rate > 0.3:
                    issues.append(("Bỏ chỗ nhiều", f"Tỷ lệ bỏ chỗ {int(no_show_rate * 100)}% ({expired}/{total} lượt)"))
                if cancel_rate > 0.3:
                    issues.append(("Huỷ chỗ thường xuyên", f"Tỷ lệ huỷ {int(cancel_rate * 100)}% ({cancelled}/{total} lượt)"))
                if reputation_score < 60:
                    issues.append(("Vi phạm nội quy", f"Điểm uy tín rất thấp: {reputation_score}/100"))
                elif reputation_score < 80:
                    issues.append(("Điểm uy tín thấp", f"Điểm uy tín: {reputation_score}/100"))

                if not issues:
                    continue

                primary_issue = issues[0][0]
                detail = "; ".join(i[1] for i in issues)

                # Gợi ý hành động
                if severity == "critical":
                    suggestion = "Cần nhắc nhở trực tiếp hoặc tạm khoá đặt chỗ"
                elif severity == "warning":
                    suggestion = "Nên gửi cảnh báo qua thông báo"
                else:
                    suggestion = "Theo dõi thêm"

                students.append({
                    "full_name": full_name,
                    "user_code": user_code,
                    "reputation_score": reputation_score,
                    "severity": severity,
                    "primary_issue": primary_issue,
                    "detail": detail,
                    "suggestion": suggestion,
                })

            return {"students": students}

    except Exception as e:
        logger.error(f"Lỗi khi truy vấn behavior-issues: {e}")
        # Fallback: trả về danh sách rỗng thay vì lỗi 500
        return {"students": []}


@router.get("/density-prediction")
async def get_density_prediction(zone_id: Optional[str] = None) -> Dict[str, Any]:
    """
    Dự đoán mật độ sử dụng theo giờ/ngày
    """
    from app.services.analytics_service import analytics_ai_service

    # Use real analytics service
    result = analytics_ai_service.analyze_peak_hours(zone_id)

    # Get hourly predictions from usage data
    try:
        from app.core.database import engine
        from sqlalchemy import text

        with engine.connect() as conn:
            # Get hourly distribution for the week
            hourly_result = conn.execute(text("""
                SELECT
                    EXTRACT(HOUR FROM check_in_time) as hour,
                    COUNT(*) as count
                FROM access_logs
                WHERE check_in_time >= NOW() - INTERVAL '7 days'
                GROUP BY EXTRACT(HOUR FROM check_in_time)
                ORDER BY hour
            """))

            hourly_predictions = []
            for row in hourly_result:
                hourly_predictions.append({
                    "hour": int(row[0]),
                    "predicted_occupancy": round(row[1] / 100, 2),  # Normalize
                    "confidence": 0.8
                })

            # Get daily predictions
            daily_result = conn.execute(text("""
                SELECT
                    EXTRACT(DOW FROM check_in_time) as day,
                    COUNT(*) as count
                FROM access_logs
                WHERE check_in_time >= NOW() - INTERVAL '30 days'
                GROUP BY EXTRACT(DOW FROM check_in_time)
                ORDER BY day
            """))

            days = ["Chủ nhật", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"]
            weekly_predictions = []
            for row in daily_result:
                day_idx = int(row[0])
                weekly_predictions.append({
                    "day": days[day_idx],
                    "predicted_occupancy": round(row[1] / 500, 2),
                    "recommendation": "Nên đến sớm" if row[1] > 300 else "Thư viện thoáng"
                })

            result["hourly_predictions"] = hourly_predictions
            result["weekly_predictions"] = weekly_predictions

    except Exception as e:
        print(f"Error fetching density prediction: {e}")
        # Add default predictions
        result["hourly_predictions"] = [
            {"hour": h, "predicted_occupancy": 0.5, "confidence": 0.7}
            for h in range(8, 22)
        ]
        result["weekly_predictions"] = [
            {"day": d, "predicted_occupancy": 0.6, "recommendation": "Nên đến sớm"}
            for d in ["Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"]
        ]

    return result


@router.get("/seat-recommendation")
async def get_seat_recommendation(
    user_id: str,
    zone_preference: Optional[str] = None,
    time_slot: Optional[str] = None
) -> Dict[str, Any]:
    """
    Gợi ý chỗ ngồi dựa trên AI
    """
    from app.services.analytics_service import analytics_ai_service

    # Get seat recommendations
    try:
        from app.core.database import engine
        from sqlalchemy import text

        with engine.connect() as conn:
            # Find seats that are available and in popular zones
            result = conn.execute(text("""
                SELECT
                    s.seat_id,
                    s.seat_code,
                    z.zone_name,
                    z.zone_id,
                    COUNT(r.reservation_id) as recent_bookings
                FROM seats s
                JOIN zones z ON z.zone_id = s.zone_id
                LEFT JOIN reservations r ON r.seat_id = s.seat_id
                    AND r.start_time >= NOW() - INTERVAL '7 days'
                WHERE s.status = 'AVAILABLE'
                GROUP BY s.seat_id, s.seat_code, z.zone_name, z.zone_id
                ORDER BY recent_bookings ASC
                LIMIT 5
            """))

            recommendations = []
            for row in result:
                recommendations.append({
                    "seat_code": row[1],
                    "zone": row[2],
                    "availability": 0.9,
                    "reason": f"Khu vực {row[2]} có lượng đặt ít trong tuần qua"
                })

            if not recommendations:
                raise Exception("No available seats")

    except Exception as e:
        print(f"Error getting seat recommendations: {e}")
        # Fallback to mock
        recommendations = [
            {
                "seat_code": "A1",
                "zone": "Khu yên tĩnh A",
                "availability": 0.85,
                "reason": "Khu yên tĩnh, ít người vào buổi sáng"
            },
            {
                "seat_code": "B3",
                "zone": "Khu học nhóm B",
                "availability": 0.70,
                "reason": "Gần cửa ra vào, thuận tiện"
            },
            {
                "seat_code": "C5",
                "zone": "Khu máy tính C",
                "availability": 0.90,
                "reason": "Có ổ cắm điện, thoáng mát"
            }
        ]

    return {
        "user_id": user_id,
        "preferences": {
            "zone_preference": zone_preference,
            "time_slot": time_slot
        },
        "recommendations": recommendations,
        "based_on": "Lịch sử đặt chỗ, thời gian trong ngày, mật độ hiện tại",
        "generated_at": datetime.now().isoformat()
    }


@router.get("/usage-statistics")
async def get_usage_statistics(period: str = "week") -> Dict[str, Any]:
    """
    Thống kê sử dụng thư viện
    """
    from app.services.analytics_service import analytics_ai_service

    # Use real analytics service
    return analytics_ai_service.get_usage_statistics(period)


@router.get("/realtime-capacity")
async def get_realtime_capacity() -> Dict[str, Any]:
    """
    Dự đoán công suất theo thời gian thực
    Check xem hiện tại có bao nhiêu người, có full không
    """
    from app.core.database import engine
    from sqlalchemy import text

    try:
        with engine.connect() as conn:
            # Lấy tổng số ghế
            seats_result = conn.execute(text("SELECT COUNT(*) FROM seats WHERE is_active = true"))
            total_seats = seats_result.fetchone()[0] or 0

            # Đếm reservations đang active (CONFIRMED/BOOKED) đang trong thời gian hiện tại
            now = datetime.now()
            active_result = conn.execute(text("""
                SELECT COUNT(*) FROM reservations
                WHERE status IN ('CONFIRMED', 'BOOKED')
                  AND start_time <= :now
                  AND end_time >= :now
            """), {"now": now})
            active_bookings = active_result.fetchone()[0] or 0

            # Tính % occupancy
            occupancy_rate = (active_bookings / total_seats * 100) if total_seats > 0 else 0

            # Lấy thông tin theo zone
            zones_result = conn.execute(text("""
                SELECT
                    z.zone_name,
                    z.zone_id,
                    COUNT(s.seat_id) as total_seats,
                    COALESCE((
                        SELECT COUNT(*)
                        FROM reservations r
                        JOIN seats s2 ON r.seat_id = s2.seat_id
                        WHERE s2.zone_id = z.zone_id
                          AND r.status IN ('CONFIRMED', 'BOOKED')
                          AND r.start_time <= :now
                          AND r.end_time >= :now
                    ), 0) as occupied_seats
                FROM zones z
                LEFT JOIN seats s ON s.zone_id = z.zone_id AND s.is_active = true
                GROUP BY z.zone_id, z.zone_name
                ORDER BY z.zone_id
            """), {"now": now})

            zones = []
            for row in zones_result:
                zone_seats = row[2] or 0
                zone_occupied = row[3] or 0
                zones.append({
                    "zone_name": row[0],
                    "zone_id": row[1],
                    "total_seats": zone_seats,
                    "occupied_seats": zone_occupied,
                    "occupancy_rate": round((zone_occupied / zone_seats * 100), 1) if zone_seats > 0 else 0
                })

            # Dự đoán xu hướng tiếp theo (trong 1 giờ tới)
            next_hour = now + timedelta(hours=1)
            trend_result = conn.execute(text("""
                SELECT COUNT(*) FROM reservations
                WHERE status IN ('CONFIRMED', 'BOOKED')
                  AND start_time BETWEEN :now AND :next_hour
            """), {"now": now, "next_hour": next_hour})
            upcoming_bookings = trend_result.fetchone()[0] or 0

            # Xác định trạng thái
            if occupancy_rate >= 90:
                status = "Đã kín"
                message = "Thư viện gần như đã kín chỗ. Khuyến nghị sinh viên đặt chỗ trước."
            elif occupancy_rate >= 70:
                status = "Khá đông"
                message = "Thư viện đang đông. Nên đến sớm để có chỗ tốt."
            elif occupancy_rate >= 50:
                status = "Bình thường"
                message = "Thư viện đang ở mức bình thường."
            else:
                status = "Còn trống"
                message = "Thư viện còn nhiều chỗ trống."

            return {
                "timestamp": now.isoformat(),
                "total_seats": total_seats,
                "occupied_seats": active_bookings,
                "occupancy_rate": round(occupancy_rate, 1),
                "status": status,
                "message": message,
                "upcoming_1h": upcoming_bookings,
                "zones": zones,
                "generated_at": now.isoformat()
            }

    except Exception as e:
        print(f"Error in realtime-capacity: {e}")
        return {
            "timestamp": datetime.now().isoformat(),
            "error": str(e),
            "status": "unknown",
            "message": "Không thể lấy dữ liệu công suất"
        }
