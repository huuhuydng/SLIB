"""
Analytics Router - AI-powered behavior analytics and predictions
"""

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
from datetime import datetime, timedelta
import logging

from app.core.admin_auth import require_admin_access

router = APIRouter(
    prefix="/api/ai/analytics",
    tags=["Analytics"],
    dependencies=[Depends(require_admin_access)],
)
logger = logging.getLogger(__name__)


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
            days = request.days or 30
            result = conn.execute(text("""
                SELECT
                    COUNT(*) as total_bookings,
                    SUM(CASE WHEN status IN ('COMPLETED', 'CONFIRMED') THEN 1 ELSE 0 END) as used_bookings,
                    SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled,
                    SUM(CASE WHEN status = 'EXPIRED' THEN 1 ELSE 0 END) as expired
                FROM reservations
                WHERE user_id = :user_id
                  AND created_at >= NOW() - MAKE_INTERVAL(days => :days)
            """), {"user_id": request.user_id, "days": days})

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
                      AND created_at >= NOW() - MAKE_INTERVAL(days => :days)
                """), {"user_id": request.user_id, "days": days})
                noshow_row = noshow_result.fetchone()
                no_shows = noshow_row[0] if noshow_row else 0
            except:
                no_shows = expired

    except Exception as e:
        logger.error("Error fetching student behavior: %s", e)
        total_bookings = 0
        used_bookings = 0
        no_shows = 0
        cancelled = 0

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
                    WHERE r.created_at >= NOW() - INTERVAL '30 days'
                    GROUP BY r.user_id
                )
                SELECT
                    u.full_name,
                    u.user_code,
                    sp.reputation_score,
                    COALESCE(rs.total_reservations, 0) AS total_reservations,
                    COALESCE(rs.expired_count, 0) AS expired_count,
                    COALESCE(rs.cancelled_count, 0) AS cancelled_count,
                    COALESCE(rs.completed_count, 0) AS completed_count,
                    sp.violation_count
                FROM student_profiles sp
                JOIN users u ON u.id = sp.user_id
                LEFT JOIN reservation_stats rs ON rs.user_id = u.id
                WHERE sp.reputation_score <= 80
                   OR sp.violation_count > 0
                   OR (rs.total_reservations >= 3
                       AND (rs.expired_count::float / rs.total_reservations) > 0.3)
                ORDER BY sp.reputation_score ASC,
                         COALESCE(rs.expired_count, 0) DESC
                LIMIT 20
            """))

            students = []
            for row in result:
                full_name = row[0]
                user_code = row[1]
                reputation_score = row[2] if row[2] is not None else 100
                total = row[3] or 0
                expired = row[4] or 0
                cancelled = row[5] or 0
                violation_count = row[7] or 0

                no_show_rate = (expired / total) if total >= 3 else 0
                cancel_rate = (cancelled / total) if total >= 3 else 0

                # Xác định severity
                if reputation_score < 60 or (no_show_rate > 0.5 and total >= 3):
                    severity = "critical"
                elif reputation_score < 80 or (no_show_rate > 0.3 and total >= 3):
                    severity = "warning"
                else:
                    severity = "info"

                # Xác định vấn đề chính
                issues = []
                if no_show_rate > 0.3 and total >= 3:
                    issues.append(("Bỏ chỗ nhiều", f"Tỷ lệ bỏ chỗ {int(no_show_rate * 100)}% ({expired}/{total} lượt)"))
                if cancel_rate > 0.3 and total >= 3:
                    issues.append(("Huỷ chỗ thường xuyên", f"Tỷ lệ huỷ {int(cancel_rate * 100)}% ({cancelled}/{total} lượt)"))
                if reputation_score < 60:
                    issues.append(("Vi phạm nội quy", f"Điểm uy tín rất thấp: {reputation_score}/100"))
                elif reputation_score <= 80:
                    issues.append(("Điểm uy tín thấp", f"Điểm uy tín: {reputation_score}/100"))
                if violation_count > 0:
                    issues.append(("Có vi phạm", f"Đã vi phạm {violation_count} lần"))

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

            # Random 3 sinh viên để dashboard không quá nhiều
            import random
            if len(students) > 3:
                students = random.sample(students, 3)

            return {"students": students}

    except Exception as e:
        logger.error(f"Lỗi khi truy vấn behavior-issues: {e}")
        # Fallback: trả về danh sách rỗng thay vì lỗi 500
        return {"students": []}


@router.get("/behavior-summary")
async def get_behavior_summary(days: int = 7) -> Dict[str, Any]:
    """
    Tổng hợp hành vi sinh viên trong khoảng thời gian (thay thế endpoint Java backend).
    Trả về thống kê no-show, cancellation, top students.
    """
    import logging
    logger = logging.getLogger(__name__)

    try:
        from app.core.database import engine
        from sqlalchemy import text

        with engine.connect() as conn:
            # --- Aggregate reservation stats ---
            agg_result = conn.execute(text("""
                SELECT
                    COUNT(DISTINCT user_id) AS total_students,
                    COUNT(*) AS total_behaviors,
                    SUM(CASE WHEN status = 'EXPIRED' THEN 1 ELSE 0 END) AS total_no_shows,
                    SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) AS total_cancellations
                FROM reservations
                WHERE created_at >= NOW() - MAKE_INTERVAL(days => :days)
            """), {"days": days})
            agg_row = agg_result.fetchone()

            total_students = agg_row[0] or 0
            total_behaviors = agg_row[1] or 0
            total_no_shows_reservations = agg_row[2] or 0
            total_cancellations = agg_row[3] or 0

            # --- Check student_behaviors table for NO_SHOW records ---
            try:
                sb_result = conn.execute(text("""
                    SELECT COUNT(*) FROM student_behaviors
                    WHERE behavior_type = 'NO_SHOW'
                      AND created_at >= NOW() - MAKE_INTERVAL(days => :days)
                """), {"days": days})
                sb_no_shows = sb_result.fetchone()[0] or 0
            except Exception:
                sb_no_shows = 0

            total_no_shows = max(total_no_shows_reservations, sb_no_shows)

            avg_no_show_rate = round(total_no_shows / max(total_behaviors, 1), 4)
            avg_cancellation_rate = round(total_cancellations / max(total_behaviors, 1), 4)

            # --- Top no-show students ---
            top_noshow_result = conn.execute(text("""
                SELECT
                    r.user_id,
                    u.full_name,
                    u.user_code,
                    COUNT(*) AS no_show_count
                FROM reservations r
                JOIN users u ON u.id = r.user_id
                WHERE r.status = 'EXPIRED'
                  AND r.created_at >= NOW() - MAKE_INTERVAL(days => :days)
                GROUP BY r.user_id, u.full_name, u.user_code
                ORDER BY no_show_count DESC
                LIMIT 10
            """), {"days": days})

            top_no_show_students = []
            for row in top_noshow_result:
                top_no_show_students.append({
                    "userId": str(row[0]),
                    "fullName": row[1],
                    "userCode": row[2],
                    "noShowCount": row[3],
                })

            # --- Top active students (chỉ đếm lần sử dụng thực sự: COMPLETED, CONFIRMED) ---
            top_active_result = conn.execute(text("""
                SELECT
                    r.user_id,
                    u.full_name,
                    u.user_code,
                    COUNT(*) AS usage_count
                FROM reservations r
                JOIN users u ON u.id = r.user_id
                WHERE r.status IN ('COMPLETED', 'CONFIRMED')
                  AND r.created_at >= NOW() - MAKE_INTERVAL(days => :days)
                GROUP BY r.user_id, u.full_name, u.user_code
                ORDER BY usage_count DESC
                LIMIT 10
            """), {"days": days})

            top_active_students = []
            for row in top_active_result:
                top_active_students.append({
                    "userId": str(row[0]),
                    "fullName": row[1],
                    "userCode": row[2],
                    "behaviorCount": row[3],
                })

        return {
            "totalStudents": total_students,
            "totalBehaviors": total_behaviors,
            "totalNoShows": total_no_shows,
            "totalCancellations": total_cancellations,
            "avgNoShowRate": avg_no_show_rate,
            "avgCancellationRate": avg_cancellation_rate,
            "topNoShowStudents": top_no_show_students,
            "topActiveStudents": top_active_students,
            "analyzedPeriod": f"{days} ngày gần nhất",
            "analyzedAt": datetime.now().isoformat(),
        }

    except Exception as e:
        logger.error(f"Lỗi khi truy vấn behavior-summary: {e}")
        raise HTTPException(status_code=500, detail=f"Lỗi khi phân tích hành vi: {str(e)}")


@router.get("/density-prediction")
async def get_density_prediction(zone_id: Optional[str] = None, days: int = 7) -> Dict[str, Any]:
    """
    Dự đoán mật độ sử dụng theo giờ/ngày
    """
    from app.services.analytics_service import analytics_ai_service

    # Use real analytics service
    result = analytics_ai_service.analyze_peak_hours(zone_id, days)

    # Get hourly predictions from usage data
    try:
        from app.core.database import engine
        from sqlalchemy import text

        with engine.connect() as conn:
            # Get total seats for normalization
            seats_result = conn.execute(text("SELECT COUNT(*) FROM seats WHERE is_active = true"))
            total_seats = seats_result.fetchone()[0] or 1

            # Get hourly distribution
            hourly_result = conn.execute(text("""
                SELECT
                    EXTRACT(HOUR FROM check_in_time) as hour,
                    COUNT(*) as count,
                    COUNT(DISTINCT DATE(check_in_time)) as num_days
                FROM access_logs
                WHERE check_in_time >= NOW() - MAKE_INTERVAL(days => :days)
                GROUP BY EXTRACT(HOUR FROM check_in_time)
                ORDER BY hour
            """), {"days": days})

            hourly_predictions = []
            for row in hourly_result:
                num_days = row[2] or 1
                avg_per_day = row[1] / num_days
                occ = min(avg_per_day / total_seats, 1.0)
                hourly_predictions.append({
                    "hour": int(row[0]),
                    "predicted_occupancy": round(occ, 2),
                    "confidence": 0.8
                })

            # Get daily predictions
            daily_result = conn.execute(text("""
                SELECT
                    EXTRACT(DOW FROM check_in_time) as day,
                    COUNT(*) as count,
                    COUNT(DISTINCT DATE(check_in_time)) as num_days
                FROM access_logs
                WHERE check_in_time >= NOW() - MAKE_INTERVAL(days => :days)
                GROUP BY EXTRACT(DOW FROM check_in_time)
                ORDER BY day
            """), {"days": days})

            day_names = ["Chủ nhật", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"]
            weekly_predictions = []
            for row in daily_result:
                day_idx = int(row[0])
                num_days = row[2] or 1
                avg_per_day = row[1] / num_days
                occ = min(avg_per_day / total_seats, 1.0)
                weekly_predictions.append({
                    "day": day_names[day_idx],
                    "predicted_occupancy": round(occ, 2),
                    "recommendation": "Nên đến sớm" if occ >= 0.7 else "Thư viện thoáng"
                })

            result["hourly_predictions"] = hourly_predictions
            result["weekly_predictions"] = weekly_predictions

    except Exception as e:
        logger.error("Error fetching density prediction: %s", e)
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
    Gợi ý chỗ ngồi thông minh — scoring system kết hợp nhiều yếu tố:

    Mỗi ghế trống được chấm điểm dựa trên:
      - Quen thuộc (40%): user đã ngồi ghế/khu vực này bao nhiêu lần
      - Yên tĩnh  (30%): khu vực ít người đặt trong tuần qua
      - Trống     (20%): không bị đặt trong 2h tới
      - Thời gian (10%): khớp với giờ user hay đến

    Trả về top 5 ghế điểm cao nhất + lý do tổng hợp.
    """
    from app.core.database import engine
    from sqlalchemy import text

    user_favorite_zone = None
    user_favorite_seat = None
    user_favorite_time = None
    user_booking_count = 0

    try:
        with engine.connect() as conn:
            # === DATA 1: Lịch sử user (30 ngày) ===
            user_seat_history = {}   # seat_code -> times_booked
            user_zone_history = {}   # zone_name -> times_booked

            user_rows = conn.execute(text("""
                SELECT
                    s.seat_code,
                    z.zone_name,
                    COUNT(*) as times_booked,
                    EXTRACT(HOUR FROM MODE() WITHIN GROUP (ORDER BY r.start_time)) as fav_hour
                FROM reservations r
                JOIN seats s ON s.seat_id = r.seat_id
                JOIN zones z ON z.zone_id = s.zone_id
                WHERE r.user_id = CAST(:user_id AS UUID)
                  AND r.status IN ('COMPLETED', 'CONFIRMED', 'BOOKED')
                  AND r.start_time >= NOW() - INTERVAL '30 days'
                GROUP BY s.seat_code, z.zone_name
                ORDER BY times_booked DESC
            """), {"user_id": user_id}).fetchall()

            for row in user_rows:
                seat_code, zone_name, times, fav_hour = row
                user_seat_history[seat_code] = times
                user_zone_history[zone_name] = user_zone_history.get(zone_name, 0) + times
                if not user_favorite_seat:
                    user_favorite_seat = seat_code
                    user_favorite_zone = zone_name
                    user_favorite_time = int(fav_hour) if fav_hour else None

            user_booking_count = sum(user_seat_history.values())
            max_seat_visits = max(user_seat_history.values()) if user_seat_history else 1
            max_zone_visits = max(user_zone_history.values()) if user_zone_history else 1

            # === DATA 2: Ghế trống + mật độ khu vực (tuần qua) ===
            seats_data = conn.execute(text("""
                SELECT
                    s.seat_id,
                    s.seat_code,
                    z.zone_name,
                    z.zone_id,
                    COUNT(r.reservation_id) as zone_bookings_7d
                FROM seats s
                JOIN zones z ON z.zone_id = s.zone_id
                LEFT JOIN reservations r ON r.seat_id = s.seat_id
                    AND r.start_time >= NOW() - INTERVAL '7 days'
                WHERE s.is_active = true
                  AND NOT EXISTS (
                    SELECT 1 FROM reservations r2
                    WHERE r2.seat_id = s.seat_id
                      AND r2.status IN ('BOOKED', 'CONFIRMED', 'PROCESSING')
                      AND r2.start_time <= NOW() + INTERVAL '2 hours'
                      AND r2.end_time >= NOW()
                  )
                GROUP BY s.seat_id, s.seat_code, z.zone_name, z.zone_id
            """)).fetchall()

            if not seats_data:
                raise Exception("No available seats")

            max_zone_bookings = max(row[4] for row in seats_data) or 1
            current_hour = datetime.now().hour

            # === SCORING: Tính điểm cho mỗi ghế ===
            scored_seats = []
            for row in seats_data:
                seat_id, seat_code, zone_name, zone_id, zone_bookings = row

                # Điểm quen thuộc (0-1): ghế user hay ngồi
                seat_familiarity = user_seat_history.get(seat_code, 0) / max_seat_visits
                zone_familiarity = user_zone_history.get(zone_name, 0) / max_zone_visits
                familiarity_score = seat_familiarity * 0.6 + zone_familiarity * 0.4

                # Điểm yên tĩnh (0-1): khu vực ít booking = yên tĩnh hơn
                quietness_score = 1.0 - (zone_bookings / max_zone_bookings) if max_zone_bookings > 0 else 1.0

                # Điểm trống (luôn = 1 vì đã filter ở SQL)
                availability_score = 1.0

                # Điểm thời gian (0-1): khớp giờ hay đến
                time_score = 0.5  # mặc định
                if user_favorite_time is not None:
                    hour_diff = abs(current_hour - user_favorite_time)
                    time_score = max(0, 1.0 - hour_diff / 12.0)

                # Tổng điểm = weighted sum
                total_score = (
                    familiarity_score * 0.40 +
                    quietness_score * 0.30 +
                    availability_score * 0.20 +
                    time_score * 0.10
                )

                # Tạo reason tổng hợp
                reasons = []
                if user_seat_history.get(seat_code, 0) > 0:
                    reasons.append(f"bạn đã ngồi {user_seat_history[seat_code]} lần")
                if user_zone_history.get(zone_name, 0) > 0 and not reasons:
                    reasons.append(f"khu vực bạn hay ngồi")
                if zone_bookings == 0:
                    reasons.append("rất vắng tuần qua")
                elif zone_bookings <= 3:
                    reasons.append("khá yên tĩnh")
                if user_favorite_time is not None and abs(current_hour - user_favorite_time) <= 2:
                    reasons.append("đúng giờ bạn hay học")

                if not reasons:
                    reasons.append("đang trống, phù hợp lúc này")

                scored_seats.append({
                    "seat_code": seat_code,
                    "zone": zone_name,
                    "zone_id": zone_id,
                    "seat_id": seat_id,
                    "score": round(total_score, 3),
                    "availability": round(total_score, 2),
                    "reason": reason_to_text(seat_code, zone_name, reasons)
                })

            # Sắp xếp theo điểm giảm dần, lấy top 5
            scored_seats.sort(key=lambda x: x["score"], reverse=True)
            recommendations = scored_seats[:5]

    except Exception as e:
        logger.error("Error getting seat recommendations: %s", e)
        recommendations = [
            {"seat_code": "N/A", "zone": "N/A", "availability": 0,
             "reason": "Không thể tải gợi ý lúc này"}
        ]

    return {
        "user_id": user_id,
        "preferences": {
            "zone_preference": zone_preference or user_favorite_zone,
            "time_slot": time_slot,
            "user_favorite_seat": user_favorite_seat,
            "user_favorite_zone": user_favorite_zone,
            "user_favorite_time": user_favorite_time,
            "user_booking_count": user_booking_count
        },
        "recommendations": recommendations,
        "based_on": "Lịch sử cá nhân (40%), mật độ khu vực (30%), trạng thái trống (20%), thời gian (10%)",
        "generated_at": datetime.now().isoformat()
    }


def reason_to_text(seat_code: str, zone_name: str, reasons: list) -> str:
    """Ghép các reason thành câu tự nhiên"""
    text = f"{seat_code} tại {zone_name}"
    if reasons:
        text += " — " + ", ".join(reasons)
    return text


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
        logger.error("Error in realtime-capacity: %s", e)
        return {
            "timestamp": datetime.now().isoformat(),
            "error": str(e),
            "status": "unknown",
            "message": "Không thể lấy dữ liệu công suất"
        }
