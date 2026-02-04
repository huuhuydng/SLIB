// Đã xóa file này vì không còn sử dụng
import React, { useEffect, useMemo, useState } from "react";
import Header from "../../../components/shared/Header";
import { LayoutProvider, useLayout, ACTIONS } from "../../../context/admin/area_management/LayoutContext";
import { seatPlanService } from "../../../services/seatPlanService";
import { seatService } from "../../../services/seatService";
import { handleLogout } from "../../../utils/auth";
import "../../../styles/librarian/SeatPlan.css";
import "../../../styles/admin/layout.css";
import "../../../styles/admin/canvas.css";
import { Armchair, AlertCircle, ShieldOff, ShieldCheck, Clock4, LayoutTemplate } from "lucide-react";
import LibrarianArea from "../../../components/librarian/LibrarianArea";

const TIME_SLOTS = [
  { label: "Hiện tại", value: "now" },
  { label: "07:00 - 09:00", value: "07:00-09:00" },
  { label: "09:00 - 11:00", value: "09:00-11:00" },
  { label: "11:00 - 13:00", value: "11:00-13:00" },
  { label: "13:00 - 15:00", value: "13:00-15:00" },
  { label: "15:00 - 17:00", value: "15:00-17:00" },
];

const buildTimeParams = (slotValue) => {
  const today = new Date();
  const yyyy = today.getFullYear();
  const mm = String(today.getMonth() + 1).padStart(2, "0");
  const dd = String(today.getDate()).padStart(2, "0");
  const datePrefix = `${yyyy}-${mm}-${dd}`;
  
  let start, end;
  if (slotValue === "now") {
    const hour = today.getHours();
    if (hour >= 7 && hour < 9) { start = "07:00"; end = "09:00"; }
    else if (hour >= 9 && hour < 11) { start = "09:00"; end = "11:00"; }
    else if (hour >= 11 && hour < 13) { start = "11:00"; end = "13:00"; }
    else if (hour >= 13 && hour < 15) { start = "13:00"; end = "15:00"; }
    else if (hour >= 15 && hour < 17) { start = "15:00"; end = "17:00"; }
    else { start = "07:00"; end = "09:00"; }
  } else {
    [start, end] = slotValue.split("-");
  }
  
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

  // Load seats theo khung giờ
  const loadSeatsForTimeSlot = async (slot) => {
    if (!selectedAreaId) return;
    setLoading(true);
    try {
      const timeParams = buildTimeParams(slot);
      const seatRes = await seatPlanService.getSeats(timeParams);
      const normalizedSeats = (seatRes.data || []).map(normalizeSeat);
      
      dispatch({ type: ACTIONS.SET_SEATS, payload: normalizedSeats });
      setMessage(null);
    } catch (err) {
      console.error("Load seats error", err);
      setMessage("Không tải được danh sách ghế");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (selectedAreaId) {
      loadSeatsForTimeSlot(slotValue);
    }
  }, [selectedAreaId, slotValue]);

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
      await loadSeatsForTimeSlot(slotValue);
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
      <Header searchPlaceholder="Tìm ghế hoặc khu vực" onLogout={handleLogout} />
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
              {TIME_SLOTS.map((s) => (
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
              <div className="sp-card__number">{TIME_SLOTS.find((t) => t.value === slotValue)?.label}</div>
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
