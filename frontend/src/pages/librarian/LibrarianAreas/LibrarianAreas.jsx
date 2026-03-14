import React, { useEffect, useMemo, useState, useRef } from "react";
import { Client } from '@stomp/stompjs';

import { LayoutProvider, useLayout, ACTIONS } from "../../../context/admin/area_management/LayoutContext";
import { useToast } from '../../../components/common/ToastProvider';
import { useConfirm } from '../../../components/common/ConfirmDialog';
import { getAreas } from "../../../services/admin/area_management/api";
import { seatService } from "../../../services/seatService";
import { handleLogout } from "../../../utils/auth";
import { Armchair, AlertCircle, ShieldOff, ShieldCheck, Clock4, LayoutTemplate, User, Clock, X, Check } from "lucide-react";
import LibrarianArea from "../../../components/librarian/LibrarianArea";
import "../../../styles/librarian/librarian-shared.css";
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

  // Mode "Hiện tại" → dùng DateTime.now() chính xác, KHÔNG resolve slot
  if (slotValue === "now") {
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

  // Slot cố định → dùng khoảng thời gian slot đó
  const [start, end] = slotValue.split("-");
  return {
    startTime: `${datePrefix}T${start}:00`,
    endTime: `${datePrefix}T${end}:00`,
  };
};

// Main component - Librarian Areas page
function LibrarianAreasContent() {
  const { state, dispatch } = useLayout();
  const { areas, seats, canvas } = state;
  const toast = useToast();
  const { confirm } = useConfirm();

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
  const [isOverSeat, setIsOverSeat] = useState(false);

  // Refs
  const slotValueRef = useRef(slotValue);
  const selectedDateRef = useRef(selectedDate);
  const timeSlotsRef = useRef(timeSlots);
  const requestIdRef = useRef(0);

  useEffect(() => { slotValueRef.current = slotValue; }, [slotValue]);
  useEffect(() => { selectedDateRef.current = selectedDate; }, [selectedDate]);
  useEffect(() => { timeSlotsRef.current = timeSlots; }, [timeSlots]);

  // Fetch time slots from library settings
  useEffect(() => {
    (async () => {
      try {
        const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/slib/settings/time-slots`);
        if (response.ok) {
          const slots = await response.json();
          setTimeSlots(slots);
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
    let confirmed = 0;
    let restricted = 0;
    seats.forEach((s) => {
      const status = (s.seatStatus || '').toUpperCase();
      if (status === "BOOKED") booked += 1;
      else if (status === "CONFIRMED") confirmed += 1;
      else if (status === "UNAVAILABLE") restricted += 1;
    });
    const used = booked + confirmed;
    const available = Math.max(0, total - used - restricted);
    const occupancy = total ? Math.round((used / total) * 100) : 0;
    return { total, booked, confirmed, used, restricted, available, occupancy };
  }, [seats]);

  // Load seats — với request counter chống race condition
  const loadSeatsForTimeSlot = async (slot, dateOverride) => {
    const myRequestId = ++requestIdRef.current;

    try {
      const timeParams = buildTimeParams(slot, dateOverride || selectedDateRef.current, timeSlotsRef.current);
      const seatRes = await seatService.getAllSeats(timeParams);

      // Nếu đã có request mới hơn → bỏ qua response cũ
      if (myRequestId !== requestIdRef.current) return;

      const normalizedSeats = (seatRes || []).map(s => ({
        seatId: s.seatId ?? s.seat_id,
        zoneId: s.zoneId ?? s.zone_id,
        seatCode: s.seatCode ?? s.seat_code ?? s.code,
        seatStatus: (s.seatStatus ?? s.seat_status ?? s.status ?? "AVAILABLE").toUpperCase(),
        rowNumber: s.rowNumber ?? s.row_number ?? 1,
        columnNumber: s.columnNumber ?? s.column_number ?? 1,
        reservationEndTime: s.reservationEndTime ?? null,
        reservationStartTime: s.reservationStartTime ?? null,
        bookedByUserName: s.bookedByUserName ?? null,
        bookedByUserCode: s.bookedByUserCode ?? null,
        bookedByAvatarUrl: s.bookedByAvatarUrl ?? null,
        reservationId: s.reservationId ?? null,
      }));

      dispatch({ type: ACTIONS.SET_SEATS, payload: normalizedSeats });
      setMessage(null);
    } catch (err) {
      if (myRequestId !== requestIdRef.current) return;
      console.error("Failed to load seats", err);
      setMessage("Không tải được danh sách ghế");
    } finally {
      if (myRequestId === requestIdRef.current) {
        setLoading(false);
      }
    }
  };

  // Load seats khi slot hoặc date thay đổi
  useEffect(() => {
    if (!slotValue) return;
    setLoading(true);
    loadSeatsForTimeSlot(slotValue, selectedDate);
  }, [slotValue, selectedDate]);

  // Smart expiration timer — tìm endTime gần nhất → re-fetch đúng lúc hết hạn
  const expirationTimerRef = useRef(null);
  useEffect(() => {
    // Clear timer cũ
    if (expirationTimerRef.current) {
      clearTimeout(expirationTimerRef.current);
      expirationTimerRef.current = null;
    }

    if (slotValue !== "now" || seats.length === 0) return;

    // Tìm reservationEndTime gần nhất trong tương lai
    const now = Date.now();
    let nearestMs = Infinity;

    seats.forEach(s => {
      if (s.reservationEndTime && (s.seatStatus === "BOOKED" || s.seatStatus === "CONFIRMED" || s.seatStatus === "HOLDING")) {
        const endMs = new Date(s.reservationEndTime).getTime();
        const diff = endMs - now;
        if (diff > 0 && diff < nearestMs) {
          nearestMs = diff;
        }
      }
    });

    if (nearestMs < Infinity) {
      // Set timer re-fetch ngay khi ghế hết hạn (+ 100ms buffer)
      expirationTimerRef.current = setTimeout(() => {
        loadSeatsForTimeSlot("now", selectedDateRef.current);
      }, nearestMs + 100);
    }

    return () => {
      if (expirationTimerRef.current) {
        clearTimeout(expirationTimerRef.current);
      }
    };
  }, [seats, slotValue]);

  // Fallback polling 10s — safety net khi WebSocket mất kết nối hoặc DB thay đổi trực tiếp
  useEffect(() => {
    if (slotValue !== "now") return;
    const fallbackId = setInterval(() => {
      loadSeatsForTimeSlot("now", selectedDateRef.current);
    }, 10000);
    return () => clearInterval(fallbackId);
  }, [slotValue]);

  // WebSocket real-time updates
  const stompClientRef = useRef(null);
  useEffect(() => {
    const wsUrl = `ws://${window.location.hostname}:8080/ws`;
    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe('/topic/seats', (message) => {
          // Re-fetch seats ngay khi nhận WebSocket event (booking/cancel/expire)
          loadSeatsForTimeSlot(slotValueRef.current, selectedDateRef.current);
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

  // Confirm reservation (librarian manual confirm)
  const confirmReservation = async (seat) => {
    if (!seat?.reservationId) return;
    try {
      const token = localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');
      const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
      const res = await fetch(
        `${API_BASE}/slib/bookings/updateStatusReserv/${seat.reservationId}?status=CONFIRMED`,
        { method: 'PUT', headers: { Authorization: `Bearer ${token}` } }
      );
      if (!res.ok) throw new Error('Lỗi xác nhận');
      toast.success('Đã xác nhận chỗ ngồi thành công');
      setSelectedSeat(null);
      await loadSeatsForTimeSlot(slotValue);
    } catch (error) {
      console.error('Error confirming reservation:', error);
      toast.error('Lỗi xác nhận chỗ ngồi: ' + error.message);
    }
  };

  // Toggle seat restriction
  const toggleRestriction = async (seat) => {
    if (!seat) return;
    try {
      const isRestricted = seat.seatStatus === "UNAVAILABLE";
      if (!isRestricted) {
        // Hiển thị xác nhận nếu ghế đang được đặt hoặc đang có người ngồi
        if (seat.seatStatus === "BOOKED") {
          const confirmed = await confirm({
            title: 'Xác nhận hạn chế ghế',
            message: `Ghế ${seat.seatCode} đang có người đặt (${seat.bookedByUserName}). Nếu hạn chế, đặt chỗ sẽ bị huỷ tự động và sinh viên sẽ được thông báo. Bạn có chắc chắn?`,
            variant: 'danger',
            confirmText: 'Hạn chế',
            cancelText: 'Huỷ',
          });
          if (!confirmed) return;
        } else if (seat.seatStatus === "CONFIRMED") {
          const confirmed = await confirm({
            title: 'Xác nhận hạn chế ghế',
            message: `Ghế ${seat.seatCode} đang có người ngồi (${seat.bookedByUserName}). Nếu hạn chế, đặt chỗ sẽ bị huỷ tự động và sinh viên sẽ được thông báo. Bạn có chắc chắn?`,
            variant: 'danger',
            confirmText: 'Hạn chế',
            cancelText: 'Huỷ',
          });
          if (!confirmed) return;
        }
        await seatService.addRestriction(seat.seatId);
        toast.success('Đã hạn chế ghế thành công');
      } else {
        await seatService.removeRestriction(seat.seatId);
        toast.success('Đã bỏ hạn chế ghế thành công');
      }
      await loadSeatsForTimeSlot(slotValue);
      setSelectedSeat((prev) =>
        prev && prev.seatId === seat.seatId
          ? { ...prev, seatStatus: isRestricted ? "AVAILABLE" : "UNAVAILABLE" }
          : prev
      );
    } catch (err) {
      console.error("Failed to update seat restriction", err);
      toast.error('Lỗi cập nhật hạn chế: ' + err.message);
    }
  };

  return (
    <div className="lib-container">
      {/* Page Title */}
      <div className="lib-page-title">
        <h1>QUẢN LÝ CHỖ NGỒI</h1>
      </div>

      {/* Toolbar */}
      <div className="lib-panel" style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
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
              <option value="now">Hiện tại</option>
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

          <div style={{ display: 'flex', gap: 16, alignItems: 'center', marginLeft: 'auto', flexWrap: 'wrap' }}>
            <span className="lib-inline-stat">
              <span className="dot" style={{ background: '#ff9b4a' }}></span>
              Đã đặt <strong>{stats.booked}</strong>
            </span>
            <span className="lib-inline-stat">
              <span className="dot" style={{ background: '#22c55e' }}></span>
              Đang ngồi <strong>{stats.confirmed}</strong>
            </span>
            <span className="lib-inline-stat">
              <span className="dot" style={{ background: '#93c5fd' }}></span>
              Trống <strong>{stats.available}</strong>
            </span>
            <span className="lib-inline-stat">
              <span className="dot gray"></span>
              Hạn chế <strong>{stats.restricted}</strong>
            </span>
            <span className="lib-inline-stat">
              <span className="dot blue"></span>
              Lấp đầy <strong>{stats.occupancy}%</strong>
            </span>
          </div>
        </div>
      </div>

      {/* Message banner */}
      {message && (
        <div className={`librarian-message ${message.includes('❌') ? 'librarian-message--error' : ''}`}>
          <AlertCircle size={16} />
          <span>{message}</span>
        </div>
      )}

      {/* Loading indicator */}
      {loading && <div className="librarian-loading">Đang tải dữ liệu...</div>}

      {/* Floor plan + Sidebar */}
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
                  const seatZone = state.zones.find(zone => String(zone.zoneId) === String(selectedSeat.zoneId));
                  if (!seatZone) return 'Không xác định';
                  const area = areas.find(a => String(a.areaId) === String(seatZone.areaId));
                  return area?.areaName || 'Không xác định';
                })()}</div>
                <div className="librarian-seat-info">
                  Trạng thái: <strong style={{
                    color: selectedSeat.seatStatus === 'CONFIRMED' ? '#22c55e' :
                      selectedSeat.seatStatus === 'BOOKED' ? '#ff9b4a' :
                        selectedSeat.seatStatus === 'UNAVAILABLE' ? '#9ca3af' : '#93c5fd'
                  }}>{
                    selectedSeat.seatStatus === 'AVAILABLE' ? 'Trống' :
                      selectedSeat.seatStatus === 'BOOKED' ? 'Đã đặt - Chưa xác nhận' :
                        selectedSeat.seatStatus === 'CONFIRMED' ? 'Đang ngồi' :
                          selectedSeat.seatStatus === 'UNAVAILABLE' ? 'Bị hạn chế' :
                            selectedSeat.seatStatus
                  }</strong>
                </div>

                {/* Thông tin sinh viên khi ghế BOOKED hoặc CONFIRMED */}
                {(selectedSeat.seatStatus === 'BOOKED' || selectedSeat.seatStatus === 'CONFIRMED') && selectedSeat.bookedByUserName && (
                  <div className="librarian-booker-section">
                    <div className="librarian-booker-info">
                      <div className="librarian-booker-avatar">
                        {selectedSeat.bookedByAvatarUrl ? (
                          <img src={selectedSeat.bookedByAvatarUrl} alt="avatar" />
                        ) : (
                          <div className="librarian-avatar-fallback">
                            <User size={20} />
                          </div>
                        )}
                      </div>
                      <div className="librarian-booker-details">
                        <span className="librarian-booker-name">{selectedSeat.bookedByUserName}</span>
                        <span className="librarian-booker-code">{selectedSeat.bookedByUserCode}</span>
                      </div>
                    </div>
                    {selectedSeat.reservationStartTime && (
                      <div className="librarian-booker-time">
                        <Clock size={14} />
                        <span>
                          {new Date(selectedSeat.reservationStartTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                          {' - '}
                          {selectedSeat.reservationEndTime
                            ? new Date(selectedSeat.reservationEndTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
                            : '--:--'}
                        </span>
                      </div>
                    )}
                  </div>
                )}

                {selectedSeat.seatStatus === 'BOOKED' && selectedSeat.reservationId && (
                  <button
                    className="librarian-btn"
                    onClick={() => confirmReservation(selectedSeat)}
                    style={{ backgroundColor: '#22c55e', cursor: 'pointer', marginBottom: 8 }}
                  >
                    Xác nhận chỗ ngồi
                  </button>
                )}
                <button
                  className="librarian-btn"
                  onClick={() => toggleRestriction(selectedSeat)}
                  style={{
                    backgroundColor: selectedSeat.seatStatus === 'UNAVAILABLE' ? '#10b981' : '#ef4444',
                    cursor: 'pointer',
                  }}
                >
                  {selectedSeat.seatStatus === 'UNAVAILABLE' ? 'Bỏ hạn chế' : 'Hạn chế ghế'}
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
              <span className="librarian-legend-dot" style={{ backgroundColor: '#93c5fd' }} />
              <span>Trống (Available)</span>
            </div>
            <div className="librarian-legend-item">
              <span className="librarian-legend-dot" style={{ backgroundColor: '#ff9b4a' }} />
              <span>Đã đặt (Booked)</span>
            </div>
            <div className="librarian-legend-item">
              <span className="librarian-legend-dot" style={{ backgroundColor: '#22c55e' }} />
              <span>Đang ngồi (Confirmed)</span>
            </div>
            <div className="librarian-legend-item">
              <span className="librarian-legend-dot" style={{ backgroundColor: '#9ca3af' }} />
              <span>Bị hạn chế (Unavailable)</span>
            </div>

            <div className="librarian-legend-title" style={{ marginTop: 12 }}>Mật độ khu vực</div>
            <div className="librarian-legend-item">
              <span style={{ width: 12, height: 12, borderRadius: 3, backgroundColor: 'rgba(39, 174, 96, 0.3)', border: '1px solid #27AE60', display: 'inline-block', flexShrink: 0 }} />
              <span>Vắng ({'<'} 50%)</span>
            </div>
            <div className="librarian-legend-item">
              <span style={{ width: 12, height: 12, borderRadius: 3, backgroundColor: 'rgba(243, 156, 18, 0.3)', border: '1px solid #F39C12', display: 'inline-block', flexShrink: 0 }} />
              <span>Khá đông (50 - 90%)</span>
            </div>
            <div className="librarian-legend-item">
              <span style={{ width: 12, height: 12, borderRadius: 3, backgroundColor: 'rgba(231, 76, 60, 0.3)', border: '1px solid #E74C3C', display: 'inline-block', flexShrink: 0 }} />
              <span>Đông {'≥'} 90%)</span>
            </div>
          </div>
        </aside>
      </div>
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
