import React, { useEffect, useMemo, useState, useRef } from "react";
import { Client } from '@stomp/stompjs';
import Header from "../../../components/shared/Header";
import { LayoutProvider, useLayout, ACTIONS } from "../../../context/admin/area_management/LayoutContext";
import { getAreas } from "../../../services/admin/area_management/api";
import { seatService } from "../../../services/seatService";
import { handleLogout } from "../../../utils/auth";
import { Armchair, AlertCircle, ShieldOff, ShieldCheck, Clock4, LayoutTemplate } from "lucide-react";
import LibrarianArea from "../../../components/librarian/LibrarianArea";
import "./LibrarianAreas.css";

// Build time params for API calls
const buildTimeParams = (slotValue, dateOverride, timeSlots) => {
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
  if (slotValue === "now" && timeSlots.length > 0) {
    const hour = today.getHours();
    const minute = today.getMinutes();
    const currentMinutes = hour * 60 + minute;

    // Find the slot that contains current time
    const currentSlot = timeSlots.find(slot => {
      const [startH, startM] = slot.startTime.split(':').map(Number);
      const [endH, endM] = slot.endTime.split(':').map(Number);
      const slotStart = startH * 60 + startM;
      const slotEnd = endH * 60 + endM;
      return currentMinutes >= slotStart && currentMinutes < slotEnd;
    });

    if (currentSlot) {
      start = currentSlot.startTime;
      end = currentSlot.endTime;
    } else if (currentMinutes < timeSlots[0].startTime.split(':').map(Number)[0] * 60) {
      // Before opening - show first slot
      start = timeSlots[0].startTime;
      end = timeSlots[0].endTime;
    } else {
      // After closing - show last slot
      const lastSlot = timeSlots[timeSlots.length - 1];
      start = lastSlot.startTime;
      end = lastSlot.endTime;
    }
  } else if (slotValue !== "now") {
    [start, end] = slotValue.split("-");
  } else {
    // Fallback if no slots loaded yet
    start = "07:00";
    end = "08:00";
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
  const [timeSlots, setTimeSlots] = useState([]);
  const canvasRef = React.useRef(null);
  const [isOverSeat, setIsOverSeat] = useState(false); // Track if mouse is over a seat

  // Fetch time slots from library settings
  useEffect(() => {
    (async () => {
      try {
        const response = await fetch('http://localhost:8080/slib/settings/time-slots');
        if (response.ok) {
          const slots = await response.json();
          setTimeSlots(slots);
          console.log('📅 Loaded time slots from settings:', slots.length, 'slots');
        }
      } catch (err) {
        console.error('Failed to load time slots:', err);
      }
    })();
  }, []);

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
      const timeParams = buildTimeParams(slot, dateOverride || selectedDate, timeSlots);
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
    if (timeSlots.length === 0) return; // Wait for time slots to load
    loadSeatsForTimeSlot(slotValue, selectedDate);
  }, [slotValue, selectedDate, timeSlots]);

  // Real-time auto-refresh when viewing current time slot
  useEffect(() => {
    if (slotValue !== "now") return;

    const intervalId = setInterval(() => {
      console.log('🔄 Auto-refreshing seats (every 30s)...');
      loadSeatsForTimeSlot(slotValue, selectedDate);
    }, 30000); // 30 seconds

    return () => clearInterval(intervalId);
  }, [slotValue, selectedDate]);

  // WebSocket real-time updates
  const stompClientRef = useRef(null);
  useEffect(() => {
    const wsUrl = `ws://${window.location.hostname}:8080/ws`;
    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('🔌 WebSocket connected for seat updates');
        client.subscribe('/topic/seats', (message) => {
          const data = JSON.parse(message.body);
          console.log('📡 Seat status update:', data);
          // Update seat in local state
          dispatch({
            type: ACTIONS.UPDATE_SEAT_STATUS,
            payload: { seatId: data.seatId, seatStatus: data.status }
          });
        });
      },
      onStompError: (frame) => {
        console.error('WebSocket error:', frame);
      },
    });
    client.activate();
    stompClientRef.current = client;

    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
      }
    };
  }, [dispatch]);

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
              {/* Option "Hiện tại" */}
              <option value="now">Hiện tại</option>
              {/* Dynamic time slots from API */}
              {timeSlots.map((slot) => {
                const slotValueStr = `${slot.startTime}-${slot.endTime}`;
                let disabled = false;
                const isToday = selectedDate === new Date().toISOString().slice(0, 10);
                if (isToday) {
                  const now = new Date();
                  const endHour = Number(slot.endTime.split(':')[0]);
                  if (!isNaN(endHour) && now.getHours() >= endHour) disabled = true;
                }
                return (
                  <option key={slotValueStr} value={slotValueStr} disabled={disabled}>{slot.label}</option>
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
              <div className="librarian-stat-number">
                {slotValue === 'now'
                  ? 'Hiện tại'
                  : timeSlots.find((t) => `${t.startTime}-${t.endTime}` === slotValue)?.label || slotValue}
              </div>
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
