import React, { useEffect, useMemo, useState } from "react";
import Header from "../../../components/shared/Header";
import { LayoutProvider, useLayout, ACTIONS } from "../../../context/admin/area_management/LayoutContext";
import { getAreas } from "../../../services/admin/area_management/api";
import { seatService } from "../../../services/seatService";
import { handleLogout } from "../../../utils/auth";
import { Armchair, AlertCircle, ShieldOff, ShieldCheck, Clock4, LayoutTemplate } from "lucide-react";
import LibrarianArea from "../../../components/librarian/LibrarianArea";
import "./LibrarianAreas.css";

const TIME_SLOTS = [
  { label: "Hiện tại", value: "now" },
  { label: "07:00 - 09:00", value: "07:00-09:00" },
  { label: "09:00 - 11:00", value: "09:00-11:00" },
  { label: "11:00 - 13:00", value: "11:00-13:00" },
  { label: "13:00 - 15:00", value: "13:00-15:00" },
  { label: "15:00 - 17:00", value: "15:00-17:00" },
];

const buildTimeParams = (slotValue, dateOverride) => {
  const today = new Date();
  let datePrefix;
  if (dateOverride) {
    datePrefix = dateOverride;
  } else {
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, "0");
    const dd = String(today.getDate()).padStart(2, "0");
    datePrefix = `${yyyy}-${mm}-${dd}`;
  }
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

// Main component - Librarian Areas page
function LibrarianAreasContent() {
  const { state, dispatch } = useLayout();
  const { areas, seats, canvas } = state;
  
  const [selectedSeat, setSelectedSeat] = useState(null);
  const [slotValue, setSlotValue] = useState("now");
  const [selectedDate, setSelectedDate] = useState(() => {
    const today = new Date();
    return today.toISOString().slice(0, 10);
  });
  const [message, setMessage] = useState(null);
  const [loading, setLoading] = useState(false);
  const canvasRef = React.useRef(null);
  const [isOverSeat, setIsOverSeat] = useState(false); // Track if mouse is over a seat

  // Load areas from admin design
  useEffect(() => {
    (async () => {
      try {
        const res = await getAreas();
        const areasNormalized = (res.data || []).map((a) => ({
          areaId: a.area_id ?? a.areaId,
          areaName: a.area_name ?? a.areaName,
          positionX: a.position_x ?? a.positionX ?? 0,
          positionY: a.position_y ?? a.positionY ?? 0,
          width: a.width ?? 300,
          height: a.height ?? 250,
          locked: a.locked ?? a.is_locked ?? false,
          isActive: a.is_active ?? a.isActive ?? true,
        }));

        if (areasNormalized.length > 0) {
          dispatch({ type: ACTIONS.SET_AREAS, payload: areasNormalized });
          dispatch({ type: ACTIONS.SELECT_AREA, payload: areasNormalized[0].areaId });
        }
      } catch (err) {
        console.error("Failed to load areas", err);
      }
    })();
  }, []);

  // Handler for seat click
  const handleSeatClick = (seat) => {
    setSelectedSeat(seat);
  };

  // Calculate statistics
  const stats = useMemo(() => {
    const total = seats.length;
    let booked = 0;
    let restricted = 0;
    seats.forEach((s) => {
      const status = (s.seatStatus || '').toUpperCase();
      if (status === "BOOKED") booked += 1;
      if (status === "UNAVAILABLE") restricted += 1;
    });
    const available = Math.max(0, total - booked - restricted);
    const occupancy = total ? Math.round((booked / total) * 100) : 0;
    return { total, booked, restricted, available, occupancy };
  }, [seats]);

  // Load seats for time slot
  const loadSeatsForTimeSlot = async (slot, dateOverride) => {
    setLoading(true);
    try {
      const timeParams = buildTimeParams(slot, dateOverride || selectedDate);
      console.log('🕐 Loading seats for time slot:', slot, timeParams);
      const seatRes = await seatService.getAllSeats(timeParams);
      console.log('📦 Raw API response:', seatRes);
      
      const normalizedSeats = (seatRes || []).map(s => ({
        seatId: s.seatId ?? s.seat_id,
        zoneId: s.zoneId ?? s.zone_id,
        seatCode: s.seatCode ?? s.seat_code ?? s.code,
        seatStatus: (s.seatStatus ?? s.seat_status ?? s.status ?? "AVAILABLE").toUpperCase(),
        rowNumber: s.rowNumber ?? s.row_number ?? 1,
        columnNumber: s.columnNumber ?? s.column_number ?? 1,
      }));
      
      console.log('✅ Normalized seats:', normalizedSeats);
      const a7Seat = normalizedSeats.find(s => s.seatCode === 'A7');
      console.log('🔍 A7 seat status:', a7Seat);
      if (a7Seat) {
        console.log('  - seatCode:', a7Seat.seatCode);
        console.log('  - seatStatus:', a7Seat.seatStatus);
        console.log('  - seatId:', a7Seat.seatId);
        console.log('  - zoneId:', a7Seat.zoneId);
      } else {
        console.log('❌ A7 seat NOT FOUND in normalized seats!');
      }
      
      // Check raw response too
      const a7Raw = (seatRes || []).find(s => 
        (s.seatCode === 'A7' || s.seat_code === 'A7' || s.code === 'A7')
      );
      console.log('📦 A7 in raw response:', a7Raw);
      
      dispatch({ type: ACTIONS.SET_SEATS, payload: normalizedSeats });
      setMessage(null);
    } catch (err) {
      console.error("Failed to load seats", err);
      setMessage("Không tải được danh sách ghế");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSeatsForTimeSlot(slotValue, selectedDate);
  }, [slotValue, selectedDate]);

  // Zoom controls
  const handleZoomIn = () => {
    dispatch({ type: ACTIONS.SET_ZOOM, payload: Math.min(canvas.zoom + 0.1, 3) });
  };

  const handleZoomOut = () => {
    dispatch({ type: ACTIONS.SET_ZOOM, payload: Math.max(canvas.zoom - 0.1, 0.1) });
  };

  const handleFitToView = () => {
    if (!areas.length || !canvasRef.current) return;

    const canvasRect = canvasRef.current.getBoundingClientRect();
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;

    areas.forEach((a) => {
      minX = Math.min(minX, a.positionX || 0);
      minY = Math.min(minY, a.positionY || 0);
      maxX = Math.max(maxX, (a.positionX || 0) + (a.width || 300));
      maxY = Math.max(maxY, (a.positionY || 0) + (a.height || 250));
    });

    const contentW = maxX - minX;
    const contentH = maxY - minY;

    // Scale to fit (contain) both width and height, never crop content
    let scale = 1;
    if (contentW > 0 && contentH > 0) {
      scale = Math.min(canvasRect.width / contentW, canvasRect.height / contentH);
      if (scale > 1) scale = 1;
    }

    // Center content in canvas
    const offsetX = ((canvasRect.width - contentW * scale) / 2) - minX * scale;
    const offsetY = ((canvasRect.height - contentH * scale) / 2) - minY * scale;

    dispatch({ type: ACTIONS.SET_ZOOM, payload: scale });
    dispatch({
      type: ACTIONS.SET_PAN,
      payload: { x: offsetX, y: offsetY },
    });
  };


  // Pan controls removed: canvas is zoom-only
  // handleMouseDown, handleMouseMove, handleMouseUp, isPanning, setIsPanning, startPan removed

  const handleWheel = React.useCallback((e) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? -0.05 : 0.05;
    const newZoom = Math.min(Math.max(canvas.zoom + delta, 0.1), 3);
    dispatch({ type: ACTIONS.SET_ZOOM, payload: newZoom });
  }, [canvas.zoom, dispatch]);

  // Pan listeners removed

  // Auto fit on load
  useEffect(() => {
    if (areas.length > 0 && canvasRef.current) {
      setTimeout(handleFitToView, 100);
    }
  }, [areas.length]);

  // Re-fit on window resize
  useEffect(() => {
    const handleResize = () => {
      if (areas.length > 0 && canvasRef.current) {
        handleFitToView();
      }
    };
    
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [areas.length]);

  // Toggle seat restriction
  const toggleRestriction = async (seat) => {
    if (!seat) return;
    try {
      const isRestricted = seat.seatStatus === "UNAVAILABLE";
      if (isRestricted) {
        await seatService.removeRestriction(seat.seatId);
        setMessage(`✓ Đã bỏ hạn chế ghế ${seat.seatCode}`);
      } else {
        await seatService.addRestriction(seat.seatId);
        setMessage(`✓ Đã hạn chế ghế ${seat.seatCode}`);
      }
      await loadSeatsForTimeSlot(slotValue);
      setSelectedSeat((prev) =>
        prev && prev.seatId === seat.seatId
          ? { ...prev, seatStatus: isRestricted ? "AVAILABLE" : "UNAVAILABLE" }
          : prev
      );
    } catch (err) {
      console.error("Failed to update seat restriction", err);
      setMessage("❌ Cập nhật hạn chế thất bại");
    }
  };

  return (
    <div className="librarian-areas-page">
      <Header searchPlaceholder="Tìm ghế hoặc khu vực" onLogout={handleLogout} />
      
      <main className="librarian-areas-main">
        {/* Top bar */}
        <div className="librarian-topbar">
          <div className="librarian-topbar-title">
            <LayoutTemplate size={18} />
            <span>Sơ đồ thư viện & Quản lý ghế</span>
          </div>
          <div className="librarian-topbar-actions" style={{ display: 'flex', gap: 8 }}>
            <input
              type="date"
              className="librarian-time-select"
              value={selectedDate}
              min={new Date().toISOString().slice(0, 10)}
              onChange={e => setSelectedDate(e.target.value)}
              style={{ width: 140 }}
            />
            <select
              className="librarian-time-select"
              value={slotValue}
              onChange={(e) => setSlotValue(e.target.value)}
            >
              {TIME_SLOTS.map((s) => {
                let disabled = false;
                const isToday = selectedDate === new Date().toISOString().slice(0, 10);
                if (!isToday && s.value === 'now') {
                  disabled = true;
                } else if (isToday && s.value !== 'now') {
                  const now = new Date();
                  // Parse end hour from slot (e.g. '13:00-15:00' => 15)
                  const endHour = Number(s.value.split('-')[1]?.split(':')[0]);
                  if (!isNaN(endHour) && now.getHours() >= endHour) disabled = true;
                }
                return (
                  <option key={s.value} value={s.value} disabled={disabled}>{s.label}</option>
                );
              })}
            </select>
          </div>
        </div>

        {/* Statistics */}
        <section className="librarian-stats">
          <div className="librarian-stat-card">
            <div className="librarian-stat-icon librarian-stat-icon--orange">
              <Armchair size={18} />
            </div>
            <div>
              <div className="librarian-stat-title">Đang sử dụng</div>
              <div className="librarian-stat-number">{stats.booked} / {stats.total}</div>
            </div>
          </div>
          <div className="librarian-stat-card">
            <div className="librarian-stat-icon librarian-stat-icon--green">
              <Armchair size={18} />
            </div>
            <div>
              <div className="librarian-stat-title">Tỷ lệ lấp đầy</div>
              <div className="librarian-stat-number">{stats.occupancy}%</div>
            </div>
          </div>
          <div className="librarian-stat-card">
            <div className="librarian-stat-icon librarian-stat-icon--blue">
              <Clock4 size={18} />
            </div>
            <div>
              <div className="librarian-stat-title">Khung giờ</div>
              <div className="librarian-stat-number">{TIME_SLOTS.find((t) => t.value === slotValue)?.label}</div>
            </div>
          </div>
          <div className="librarian-stat-card">
            <div className="librarian-stat-icon librarian-stat-icon--amber">
              <ShieldOff size={18} />
            </div>
            <div>
              <div className="librarian-stat-title">Ghế hạn chế</div>
              <div className="librarian-stat-number">{stats.restricted}</div>
            </div>
          </div>
          <div className="librarian-stat-card">
            <div className="librarian-stat-icon librarian-stat-icon--mint">
              <ShieldCheck size={18} />
            </div>
            <div>
              <div className="librarian-stat-title">Ghế trống</div>
              <div className="librarian-stat-number">{stats.available}</div>
            </div>
          </div>
        </section>

        {/* Message banner */}
        {message && (
          <div className={`librarian-message ${message.includes('❌') ? 'librarian-message--error' : ''}`}>
            <AlertCircle size={16} />
            <span>{message}</span>
          </div>
        )}

        {/* Loading indicator */}
        {loading && <div className="librarian-loading">Đang tải dữ liệu...</div>}
        
        {/* Floor plan display */}
        <div className="librarian-content">
          {/* Canvas - static view */}
          <div 
            ref={canvasRef}
            className="librarian-canvas"
            style={{ cursor: 'default', position: 'relative', width: '100%', height: '100%', overflowX: 'hidden', overflowY: 'auto' }}
          >
            <div 
              className="librarian-canvas-board"
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                width: '100%',
                height: '100%',
                transform: `translate(${canvas.panX}px, ${canvas.panY}px) scale(${canvas.zoom})`,
                transformOrigin: 'top left',
              }}
            >
              {areas.map((area) => (
                <LibrarianArea 
                  key={area.areaId} 
                  area={area}
                  onSeatClick={handleSeatClick}
                />
              ))}
            </div>
          </div>
          
          {/* Sidebar */}
          <aside className="librarian-sidebar">
            <div className="librarian-sidebar-section">
              <div className="librarian-sidebar-title">
                <Armchair size={16} /> Chi tiết ghế
              </div>
              {selectedSeat ? (
                <div className="librarian-seat-detail">
                  <div className="librarian-seat-code">{selectedSeat.seatCode}</div>
                  <div className="librarian-seat-info">Khu vực: {(() => {
                    // Tìm khu vực chứa ghế này
                    if (!state.zones || !areas) return 'Không xác định';
                    const seatZone = state.zones.find(zone => String(zone.zoneId) === String(selectedSeat.zoneId));
                    if (!seatZone) return 'Không xác định';
                    const area = areas.find(a => String(a.areaId) === String(seatZone.areaId));
                    return area?.areaName || 'Không xác định';
                  })()}</div>
                  <div className="librarian-seat-info">
                    Trạng thái: <strong>{
                      selectedSeat.seatStatus === 'AVAILABLE' ? 'Trống' :
                      selectedSeat.seatStatus === 'BOOKED' ? 'Đã đặt' :
                      selectedSeat.seatStatus === 'UNAVAILABLE' ? 'Bị hạn chế' : 
                      selectedSeat.seatStatus
                    }</strong>
                  </div>
                  <button
                    className="librarian-btn"
                    onClick={() => toggleRestriction(selectedSeat)}
                    disabled={selectedSeat.seatStatus === 'BOOKED'}
                    style={{
                      backgroundColor: selectedSeat.seatStatus === 'BOOKED' ? '#ccc' : 
                                       selectedSeat.seatStatus === 'UNAVAILABLE' ? '#10b981' : '#ef4444',
                      cursor: selectedSeat.seatStatus === 'BOOKED' ? 'not-allowed' : 'pointer',
                    }}
                  >
                    {selectedSeat.seatStatus === 'BOOKED' ? 'Đang được đặt' :
                     selectedSeat.seatStatus === 'UNAVAILABLE' ? 'Bỏ hạn chế' : 'Hạn chế ghế'}
                  </button>
                </div>
              ) : (
                <div className="librarian-empty">Chọn một ghế để xem chi tiết</div>
              )}
            </div>
            
            {/* Legend */}
            <div className="librarian-legend">
              <div className="librarian-legend-title">Chú thích</div>
              <div className="librarian-legend-item">
                <span className="librarian-legend-dot librarian-legend-dot--green" />
                <span>Trống (Available)</span>
              </div>
              <div className="librarian-legend-item">
                <span className="librarian-legend-dot librarian-legend-dot--orange" />
                <span>Đã đặt (Booked)</span>
              </div>
              <div className="librarian-legend-item">
                <span className="librarian-legend-dot librarian-legend-dot--gray" />
                <span>Bị hạn chế (Unavailable)</span>
              </div>
            </div>
          </aside>
        </div>
      </main>
    </div>
  );
}

const LibrarianAreas = () => {
  return (
    <LayoutProvider>
      <LibrarianAreasContent />
    </LayoutProvider>
  );
};

export default LibrarianAreas;
