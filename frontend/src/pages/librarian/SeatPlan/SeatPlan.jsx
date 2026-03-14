// Librarian SeatPlan - Quản lý ghế thư viện
import React, { useEffect, useMemo, useState, useRef, useCallback } from "react";
import axios from "axios";

import { LayoutProvider, useLayout, ACTIONS } from "../../../context/admin/area_management/LayoutContext";
import { seatPlanService } from "../../../services/librarian/seatPlanService";
import { seatService } from "../../../services/librarian/seatService";
import websocketService from "../../../services/shared/websocketService";
import { handleLogout } from "../../../utils/auth";
import "../../../styles/librarian/SeatPlan.css";
import "../../../styles/admin/layout.css";
import "../../../styles/admin/canvas.css";
import { Armchair, AlertCircle, ShieldOff, ShieldCheck, Clock4, LayoutTemplate } from "lucide-react";
import LibrarianArea from "../../../components/librarian/LibrarianArea";

// Tìm slot hiện tại dựa trên danh sách time slots từ API
const findCurrentSlotValue = (timeSlots) => {
  const now = new Date();
  const currentHour = now.getHours();
  const currentMinute = now.getMinutes();
  const currentTotal = currentHour * 60 + currentMinute;

  // Lọc bỏ "now" option
  const realSlots = (timeSlots || []).filter(s => s.value !== "now");

  // Nếu có danh sách slots → tìm slot chứa giờ hiện tại
  if (realSlots.length > 0) {
    for (const slot of realSlots) {
      const [start, end] = slot.value.split("-");
      const [sh, sm] = start.split(":").map(Number);
      const [eh, em] = end.split(":").map(Number);
      const startTotal = sh * 60 + sm;
      const endTotal = eh * 60 + em;
      if (currentTotal >= startTotal && currentTotal < endTotal) {
        return slot.value;
      }
    }
    // Ngoài tất cả slot → trả về slot cuối cùng
    return realSlots[realSlots.length - 1].value;
  }

  // Fallback: tự tính slot 1 giờ dựa trên giờ hiện tại
  // VD: 19:21 → "19:00-20:00", 07:30 → "07:00-08:00"
  const startH = String(currentHour).padStart(2, "0");
  const endH = String(currentHour + 1).padStart(2, "0");
  return `${startH}:00-${endH}:00`;
};

const buildTimeParams = (slotValue, allTimeSlots = []) => {
  const today = new Date();
  const yyyy = today.getFullYear();
  const mm = String(today.getMonth() + 1).padStart(2, "0");
  const dd = String(today.getDate()).padStart(2, "0");
  const datePrefix = `${yyyy}-${mm}-${dd}`;

  if (slotValue === "now") {
    // Real-time: dùng thời gian thực tại, check reservation overlap tại đúng thời điểm này
    const hh = String(today.getHours()).padStart(2, "0");
    const mi = String(today.getMinutes()).padStart(2, "0");
    const ss = String(today.getSeconds()).padStart(2, "0");
    const nowStr = `${datePrefix}T${hh}:${mi}:${ss}`;
    // endTime = now + 1 phút để check overlap
    const endDate = new Date(today.getTime() + 60000);
    const ehh = String(endDate.getHours()).padStart(2, "0");
    const emi = String(endDate.getMinutes()).padStart(2, "0");
    const ess = String(endDate.getSeconds()).padStart(2, "0");
    const endStr = `${datePrefix}T${ehh}:${emi}:${ess}`;
    return { startTime: nowStr, endTime: endStr };
  }

  // Slot cố định: dùng khoảng thời gian slot đó
  const [start, end] = slotValue.split("-");
  return {
    startTime: `${datePrefix}T${start}:00`,
    endTime: `${datePrefix}T${end}:00`,
  };
};

const normalizeSeat = (seat) => ({
  seatId: seat.seatId ?? seat.seat_id,
  zoneId: seat.zoneId ?? seat.zone_id,
  seatCode: seat.seatCode ?? seat.seat_code ?? seat.code,
  seatStatus: (seat.seatStatus ?? seat.seat_status ?? seat.status ?? "AVAILABLE").toUpperCase(),
  rowNumber: seat.rowNumber ?? seat.row_number ?? 1,
  columnNumber: seat.columnNumber ?? seat.column_number ?? 1,
});

// LibrarianCanvas - đơn giản hóa từ CanvasBoard của admin, chỉ hiển thị không chỉnh sửa
function LibrarianCanvas({ onSeatClick }) {
  const { state, dispatch, actions } = useLayout();
  const { areas, canvas } = state;

  // Load areas from backend
  useEffect(() => {
    (async () => {
      try {
        const res = await seatPlanService.getAreas();
        const areasNormalized = (res.data || []).map((a) => ({
          areaId: a.area_id ?? a.areaId,
          areaName: a.area_name ?? a.areaName,
          positionX: a.position_x ?? a.positionX ?? 0,
          positionY: a.position_y ?? a.positionY ?? 0,
          width: a.width ?? 1000,
          height: a.height ?? 730,
          locked: a.locked ?? a.is_locked ?? false,
          isActive: a.is_active ?? a.isActive ?? true,
        }));

        if (areasNormalized.length > 0) {
          dispatch({ type: actions.SET_AREAS, payload: areasNormalized });
          dispatch({ type: actions.SELECT_AREA, payload: areasNormalized[0].areaId });
        }
      } catch (err) {
        console.error("Load areas failed", err);
      }
    })();
  }, []);

  // Tự động fit view khi areas load xong
  useEffect(() => {
    if (areas.length > 0) {
      // Zoom and pan to fit all areas
      let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
      areas.forEach((a) => {
        minX = Math.min(minX, a.positionX || 0);
        minY = Math.min(minY, a.positionY || 0);
        maxX = Math.max(maxX, (a.positionX || 0) + (a.width || 300));
        maxY = Math.max(maxY, (a.positionY || 0) + (a.height || 250));
      });

      const contentW = maxX - minX + 200;
      const contentH = maxY - minY + 200;
      const viewW = window.innerWidth - 400; // Trừ sidebar
      const viewH = window.innerHeight - 300; // Trừ header và stats

      const scale = Math.min(viewW / contentW, viewH / contentH, 1);
      const centerX = (minX + maxX) / 2;
      const centerY = (minY + maxY) / 2;

      dispatch({ type: actions.SET_ZOOM, payload: scale });
      dispatch({
        type: actions.SET_PAN,
        payload: {
          x: viewW / 2 - centerX * scale,
          y: viewH / 2 - centerY * scale,
        },
      });
    }
  }, [areas.length]);

  return (
    <div className="canvas-container">
      <div
        className="canvas-board"
        style={{
          transform: `translate(${canvas.panX}px, ${canvas.panY}px) scale(${canvas.zoom})`,
          transformOrigin: "0 0",
          position: "relative",
          width: "100%",
          height: "100%",
        }}
      >
        {areas.map((area) => (
          <LibrarianArea
            key={area.areaId}
            area={area}
            onSeatClick={onSeatClick}
          />
        ))}
      </div>
    </div>
  );
}

// Component chính - sử dụng layout system của admin cho librarian
function SeatPlanContent() {
  const { state, dispatch } = useLayout();
  const { selectedAreaId, seats } = state;

  const [selectedSeat, setSelectedSeat] = useState(null);
  const [slotValue, setSlotValue] = useState("now");
  const [message, setMessage] = useState(null);
  const [loading, setLoading] = useState(false);
  const [timeSlots, setTimeSlots] = useState([{ label: "Hiện tại", value: "now" }]);

  // Fetch time slots từ backend
  useEffect(() => {
    const fetchTimeSlots = async () => {
      try {
        const res = await axios.get(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/slib/settings/time-slots`);
        const apiSlots = (res.data || []).map((s) => ({
          label: s.label || `${s.startTime} - ${s.endTime}`,
          value: `${s.startTime}-${s.endTime}`,
        }));
        setTimeSlots([{ label: "Hiện tại", value: "now" }, ...apiSlots]);
      } catch (err) {
        console.error("Không tải được khung giờ:", err);
        // Fallback nếu API fail
        setTimeSlots([
          { label: "Hiện tại", value: "now" },
          { label: "07:00 - 08:00", value: "07:00-08:00" },
          { label: "08:00 - 09:00", value: "08:00-09:00" },
        ]);
      }
    };
    fetchTimeSlots();
  }, []);

  // Handler cho khi click vào seat
  const handleSeatClick = (seat) => {
    setSelectedSeat(seat);
  };

  const stats = useMemo(() => {
    const total = seats.length;
    let booked = 0;
    let restricted = 0;
    seats.forEach((s) => {
      if (s.seatStatus === "BOOKED") booked += 1;
      if (s.seatStatus === "UNAVAILABLE") restricted += 1;
    });
    const available = Math.max(0, total - booked - restricted);
    const occupancy = total ? Math.round((booked / total) * 100) : 0;
    return { total, booked, restricted, available, occupancy };
  }, [seats]);

  // Refs để tránh stale closure trong WebSocket callback
  const slotValueRef = useRef(slotValue);
  const timeSlotsRef = useRef(timeSlots);
  const selectedAreaIdRef = useRef(selectedAreaId);
  const requestIdRef = useRef(0); // Request counter để tránh race condition

  useEffect(() => { slotValueRef.current = slotValue; }, [slotValue]);
  useEffect(() => { timeSlotsRef.current = timeSlots; }, [timeSlots]);
  useEffect(() => { selectedAreaIdRef.current = selectedAreaId; }, [selectedAreaId]);

  // Load seats theo khung giờ — với request counter chống race condition
  const loadSeatsForTimeSlot = useCallback(async (slot, slots) => {
    const areaId = selectedAreaIdRef.current;
    if (!areaId) return;

    // Tạo request ID mới — chỉ apply response nếu đây là request mới nhất
    const myRequestId = ++requestIdRef.current;

    try {
      const timeParams = buildTimeParams(slot, slots || timeSlotsRef.current);
      console.log(`[DEBUG] loadSeats #${myRequestId}: slot="${slot}" → API params:`, timeParams);
      const seatRes = await seatPlanService.getSeats(timeParams);

      // Nếu đã có request mới hơn → bỏ qua response cũ
      if (myRequestId !== requestIdRef.current) {
        console.log(`[DEBUG] Skipping stale response #${myRequestId} (current: ${requestIdRef.current})`);
        return;
      }

      const normalizedSeats = (seatRes.data || []).map(normalizeSeat);
      const bookedCount = normalizedSeats.filter(s => s.seatStatus === "BOOKED").length;
      console.log(`[DEBUG] Applied response #${myRequestId}: ${bookedCount} booked seats`);
      dispatch({ type: ACTIONS.SET_SEATS, payload: normalizedSeats });
      setMessage(null);
    } catch (err) {
      if (myRequestId !== requestIdRef.current) return;
      console.error("Load seats error", err);
      setMessage("Không tải được danh sách ghế");
    } finally {
      if (myRequestId === requestIdRef.current) {
        setLoading(false);
      }
    }
  }, [dispatch]);

  // Load seats khi area hoặc slot thay đổi (KHÔNG depend vào timeSlots để tránh ghost request)
  useEffect(() => {
    if (selectedAreaId && slotValue) {
      setLoading(true);
      loadSeatsForTimeSlot(slotValue, timeSlotsRef.current);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedAreaId, slotValue]);

  // Auto-refresh mỗi 1s khi ở mode "Hiện tại" → real-time chính xác
  useEffect(() => {
    if (slotValue !== "now") return;

    const interval = setInterval(() => {
      loadSeatsForTimeSlot("now", timeSlotsRef.current);
    }, 1000);

    return () => clearInterval(interval);
  }, [slotValue, loadSeatsForTimeSlot]);

  // WebSocket real-time seat updates (booking mới, cancel, etc.)
  useEffect(() => {
    let unsubscribe = null;

    websocketService.connect(
      () => {
        console.log("[SeatPlan] WebSocket connected");
        unsubscribe = websocketService.subscribe("/topic/seats", (msg) => {
          // Chỉ re-fetch nếu KHÔNG ở mode "now" (mode now đã có polling 1s)
          if (slotValueRef.current !== "now") {
            loadSeatsForTimeSlot(slotValueRef.current, timeSlotsRef.current);
          }
        });
      },
      (error) => {
        console.error("[SeatPlan] WebSocket error:", error);
      }
    );

    return () => {
      if (unsubscribe) {
        websocketService.unsubscribe("/topic/seats", unsubscribe);
      }
    };
  }, [loadSeatsForTimeSlot]);

  const toggleRestriction = async (seat) => {
    if (!seat) return;
    try {
      const isRestricted = seat.seatStatus === "UNAVAILABLE";
      if (isRestricted) {
        await seatService.removeRestriction(seat.seatId);
        setMessage(`Đã bỏ hạn chế ghế ${seat.seatCode}`);
      } else {
        await seatService.addRestriction(seat.seatId);
        setMessage(`Đã hạn chế ghế ${seat.seatCode}`);
      }
      await loadSeatsForTimeSlot(slotValue, timeSlots);
      setSelectedSeat((prev) =>
        prev && prev.seatId === seat.seatId
          ? { ...prev, seatStatus: isRestricted ? "AVAILABLE" : "UNAVAILABLE" }
          : prev
      );
    } catch (err) {
      setMessage("Cập nhật hạn chế thất bại");
    }
  };

  return (
    <>

      <main className="sp-page">
        <div className="sp-topbar">
          <div className="sp-topbar__group">
            <LayoutTemplate size={18} />
            <span>Sơ đồ thư viện & Quản lý ghế</span>
          </div>
          <div className="sp-actions">
            <select
              className="sp-select"
              value={slotValue}
              onChange={(e) => setSlotValue(e.target.value)}
            >
              {timeSlots.map((s) => (
                <option key={s.value} value={s.value}>
                  {s.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        <section className="sp-stats">
          <div className="sp-card">
            <div className="sp-card__icon sp-card__icon--green">
              <Armchair size={18} />
            </div>
            <div>
              <div className="sp-card__title">Tỷ lệ lấp đầy</div>
              <div className="sp-card__number">{stats.occupancy}%</div>
            </div>
          </div>
          <div className="sp-card">
            <div className="sp-card__icon sp-card__icon--blue">
              <Clock4 size={18} />
            </div>
            <div>
              <div className="sp-card__title">Khung giờ</div>
              <div className="sp-card__number">{timeSlots.find((t) => t.value === slotValue)?.label}</div>
            </div>
          </div>
          <div className="sp-card">
            <div className="sp-card__icon sp-card__icon--amber">
              <ShieldOff size={18} />
            </div>
            <div>
              <div className="sp-card__title">Ghế hạn chế</div>
              <div className="sp-card__number">{stats.restricted}</div>
            </div>
          </div>
          <div className="sp-card">
            <div className="sp-card__icon sp-card__icon--mint">
              <ShieldCheck size={18} />
            </div>
            <div>
              <div className="sp-card__title">Ghế trống</div>
              <div className="sp-card__number">{stats.available}</div>
            </div>
          </div>
        </section>

        {message && (
          <div className="sp-banner">
            <AlertCircle size={16} />
            <span>{message}</span>
          </div>
        )}

        {loading && <div className="sp-loading">Đang tải dữ liệu...</div>}

        {/* Sử dụng canvas giống admin để hiển thị sơ đồ 100% giống nhau */}
        <div style={{ display: 'flex', gap: '20px', padding: '20px' }}>
          <div style={{ flex: 1 }}>
            <LibrarianCanvas onSeatClick={handleSeatClick} />
          </div>

          <aside className="sp-sidebar" style={{
            width: '300px',
            backgroundColor: '#fff',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
            borderRadius: '8px',
            padding: '16px'
          }}>
            <div className="sp-sidebar__section">
              <div className="sp-sidebar__title">
                <Armchair size={16} /> Chi tiết ghế
              </div>
              {selectedSeat ? (
                <div className="sp-seat-detail">
                  <div className="sp-seat-detail__code">{selectedSeat.seatCode}</div>
                  <div className="sp-seat-detail__row">Mã hàng: {selectedSeat.rowNumber}</div>
                  <div className="sp-seat-detail__row">Cột: {selectedSeat.columnNumber}</div>
                  <div className="sp-seat-detail__row">Trạng thái: {selectedSeat.seatStatus}</div>
                  <button
                    className="sp-btn"
                    onClick={() => toggleRestriction(selectedSeat)}
                  >
                    {selectedSeat.seatStatus === "UNAVAILABLE" ? "Bỏ hạn chế" : "Hạn chế ghế"}
                  </button>
                </div>
              ) : (
                <div className="sp-empty">Chọn một ghế để xem chi tiết</div>
              )}
            </div>
            <div className="sp-sidebar__legend">
              <div className="sp-legend-item">
                <span className="sp-dot sp-dot--green" />
                <span>Trống (Available)</span>
              </div>
              <div className="sp-legend-item">
                <span className="sp-dot sp-dot--amber" />
                <span>Đã đặt (Booked)</span>
              </div>
              <div className="sp-legend-item">
                <span className="sp-dot sp-dot--red" />
                <span>Bị hạn chế (Unavailable)</span>
              </div>
            </div>
          </aside>
        </div>
      </main>
    </>
  );
}

const SeatPlan = () => {
  return (
    <LayoutProvider>
      <SeatPlanContent />
    </LayoutProvider>
  );
};

export default SeatPlan;
