// KioskSeatBooking - Hiển thị sơ đồ ghế theo cấu hình admin cho sinh viên booking
import React, { useEffect, useMemo, useState, useRef, useCallback } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import axios from "axios";
import websocketService from "../../services/shared/websocketService";
import { LayoutProvider, useLayout, ACTIONS } from "../../context/admin/area_management/LayoutContext";
import { seatPlanService } from "../../services/librarian/seatPlanService";
import LibrarianArea from "../../components/librarian/LibrarianArea";
import { ArrowLeft, Armchair, Clock, MapPin, CheckCircle, XCircle, Lock, AlertTriangle } from "lucide-react";
import "../../styles/librarian/SeatPlan.css";
import "../../styles/admin/layout.css";
import "../../styles/admin/canvas.css";
import "./KioskSeatBooking.css";
import { API_BASE_URL } from '../../config/apiConfig';

const getKioskHeaders = () => {
    const token = localStorage.getItem('kiosk_device_token');
    return token ? { Authorization: `Bearer ${token}` } : {};
};

// Parse slot label ("HH:mm - HH:mm") thành startTime/endTime dạng ISO cho ngày hôm nay
const parseTimeSlot = (slotLabel) => {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');

    const [startStr, endStr] = slotLabel.split(' - ');
    return {
        startTime: `${year}-${month}-${day}T${startStr}:00`,
        endTime: `${year}-${month}-${day}T${endStr}:00`,
    };
};

// Lọc bỏ các slot đã qua (endTime <= now) - giống mobile booking
const filterPastSlots = (slots) => {
    const now = new Date();
    const currentMinutes = now.getHours() * 60 + now.getMinutes();

    return slots.filter((label) => {
        const parts = label.split(' - ');
        if (parts.length !== 2) return false;
        const [eh, em] = parts[1].split(':').map(Number);
        // Slot còn valid nếu endTime > now (slot vẫn đang diễn ra hoặc chưa bắt đầu)
        return eh * 60 + em > currentMinutes;
    });
};

const normalizeSeat = (seat) => ({
    seatId: seat.seatId ?? seat.seat_id,
    zoneId: seat.zoneId ?? seat.zone_id,
    seatCode: seat.seatCode ?? seat.seat_code ?? seat.code,
    seatStatus: (seat.seatStatus ?? seat.seat_status ?? seat.status ?? "AVAILABLE").toUpperCase(),
    rowNumber: seat.rowNumber ?? seat.row_number ?? 1,
    columnNumber: seat.columnNumber ?? seat.column_number ?? 1,
});

// Canvas hiển thị - hỗ trợ touch zoom/pan + mouse drag cho kiosk cảm ứng
function KioskCanvas({ onSeatClick }) {
    const { state, dispatch, actions } = useLayout();
    const { areas } = state;
    const containerRef = useRef(null);

    const [view, setView] = useState({ zoom: 1, panX: 0, panY: 0 });

    // Touch gesture refs
    const touchRef = useRef({
        lastDistance: 0,
        lastCenter: null,
        isPanning: false,
        lastSingleTouch: null,
    });

    // Mouse drag refs
    const mouseRef = useRef({
        isDragging: false,
        lastX: 0,
        lastY: 0,
    });

    const MIN_ZOOM = 0.3;
    const MAX_ZOOM = 3;

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

    // Auto-fit: sơ đồ vừa khít viewport với padding
    const fitToView = useCallback(() => {
        if (areas.length === 0 || !containerRef.current) return;

        let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
        areas.forEach((a) => {
            minX = Math.min(minX, a.positionX || 0);
            minY = Math.min(minY, a.positionY || 0);
            maxX = Math.max(maxX, (a.positionX || 0) + (a.width || 300));
            maxY = Math.max(maxY, (a.positionY || 0) + (a.height || 250));
        });

        const rect = containerRef.current.getBoundingClientRect();
        const pad = 32;
        const contentW = maxX - minX;
        const contentH = maxY - minY;
        if (contentW <= 0 || contentH <= 0) return;

        const viewW = rect.width - pad * 2;
        const viewH = rect.height - pad * 2;

        const scale = Math.min(viewW / contentW, viewH / contentH, MAX_ZOOM);
        const scaledW = contentW * scale;
        const scaledH = contentH * scale;

        setView({
            zoom: scale,
            panX: (rect.width - scaledW) / 2 - minX * scale,
            panY: (rect.height - scaledH) / 2 - minY * scale,
        });
    }, [areas]);

    useEffect(() => { fitToView(); }, [areas.length]);

    // Sync zoom/pan to LayoutContext
    useEffect(() => {
        dispatch({ type: actions.SET_ZOOM, payload: view.zoom });
        dispatch({ type: actions.SET_PAN, payload: { x: view.panX, y: view.panY } });
    }, [view]);

    // === MOUSE DRAG ===
    const handleMouseDown = useCallback((e) => {
        if (e.target.closest('.ksb__zoom-controls')) return;
        e.preventDefault();
        mouseRef.current.isDragging = true;
        mouseRef.current.lastX = e.clientX;
        mouseRef.current.lastY = e.clientY;
    }, []);

    const handleMouseMove = useCallback((e) => {
        if (!mouseRef.current.isDragging) return;
        e.preventDefault();
        const dx = e.clientX - mouseRef.current.lastX;
        const dy = e.clientY - mouseRef.current.lastY;
        setView(prev => ({ ...prev, panX: prev.panX + dx, panY: prev.panY + dy }));
        mouseRef.current.lastX = e.clientX;
        mouseRef.current.lastY = e.clientY;
    }, []);

    const handleMouseUp = useCallback(() => {
        mouseRef.current.isDragging = false;
    }, []);

    useEffect(() => {
        const up = () => { mouseRef.current.isDragging = false; };
        window.addEventListener('mouseup', up);
        return () => window.removeEventListener('mouseup', up);
    }, []);

    // === TOUCH ===
    const getDistance = (t1, t2) => Math.sqrt((t1.clientX - t2.clientX) ** 2 + (t1.clientY - t2.clientY) ** 2);
    const getCenter = (t1, t2) => ({ x: (t1.clientX + t2.clientX) / 2, y: (t1.clientY + t2.clientY) / 2 });

    const handleTouchStart = useCallback((e) => {
        if (e.touches.length === 2) {
            e.preventDefault();
            touchRef.current.lastDistance = getDistance(e.touches[0], e.touches[1]);
            touchRef.current.lastCenter = getCenter(e.touches[0], e.touches[1]);
            touchRef.current.isPanning = false;
        } else if (e.touches.length === 1) {
            touchRef.current.isPanning = true;
            touchRef.current.lastSingleTouch = { x: e.touches[0].clientX, y: e.touches[0].clientY };
        }
    }, []);

    const handleTouchMove = useCallback((e) => {
        if (e.touches.length === 2) {
            e.preventDefault();
            const newDist = getDistance(e.touches[0], e.touches[1]);
            const newCenter = getCenter(e.touches[0], e.touches[1]);
            if (touchRef.current.lastDistance > 0) {
                const scaleDelta = newDist / touchRef.current.lastDistance;
                const rect = containerRef.current.getBoundingClientRect();
                const cx = newCenter.x - rect.left, cy = newCenter.y - rect.top;
                setView(prev => {
                    const nz = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, prev.zoom * scaleDelta));
                    const ratio = nz / prev.zoom;
                    return {
                        zoom: nz,
                        panX: cx - (cx - prev.panX) * ratio,
                        panY: cy - (cy - prev.panY) * ratio,
                    };
                });
            }
            touchRef.current.lastDistance = newDist;
            touchRef.current.lastCenter = newCenter;
            touchRef.current.isPanning = false;
        } else if (e.touches.length === 1 && touchRef.current.isPanning) {
            const t = e.touches[0];
            const dx = t.clientX - touchRef.current.lastSingleTouch.x;
            const dy = t.clientY - touchRef.current.lastSingleTouch.y;
            setView(prev => ({ ...prev, panX: prev.panX + dx, panY: prev.panY + dy }));
            touchRef.current.lastSingleTouch = { x: t.clientX, y: t.clientY };
        }
    }, []);

    const handleTouchEnd = useCallback(() => {
        touchRef.current.lastDistance = 0;
        touchRef.current.lastCenter = null;
        touchRef.current.isPanning = false;
    }, []);

    // Mouse wheel zoom — zoom towards cursor position
    const handleWheel = useCallback((e) => {
        e.preventDefault();
        const delta = e.deltaY > 0 ? 0.9 : 1.1;
        const rect = containerRef.current.getBoundingClientRect();
        const cx = e.clientX - rect.left, cy = e.clientY - rect.top;
        setView(prev => {
            const nz = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, prev.zoom * delta));
            const ratio = nz / prev.zoom;
            return {
                zoom: nz,
                panX: cx - (cx - prev.panX) * ratio,
                panY: cy - (cy - prev.panY) * ratio,
            };
        });
    }, []);

    // Zoom buttons: zoom towards CENTER of viewport
    const handleZoomIn = useCallback(() => {
        if (!containerRef.current) return;
        const rect = containerRef.current.getBoundingClientRect();
        const cx = rect.width / 2, cy = rect.height / 2;
        setView(prev => {
            const nz = Math.min(MAX_ZOOM, prev.zoom * 1.3);
            const ratio = nz / prev.zoom;
            return {
                zoom: nz,
                panX: cx - (cx - prev.panX) * ratio,
                panY: cy - (cy - prev.panY) * ratio,
            };
        });
    }, []);

    const handleZoomOut = useCallback(() => {
        if (!containerRef.current) return;
        const rect = containerRef.current.getBoundingClientRect();
        const cx = rect.width / 2, cy = rect.height / 2;
        setView(prev => {
            const nz = Math.max(MIN_ZOOM, prev.zoom / 1.3);
            const ratio = nz / prev.zoom;
            return {
                zoom: nz,
                panX: cx - (cx - prev.panX) * ratio,
                panY: cy - (cy - prev.panY) * ratio,
            };
        });
    }, []);

    useEffect(() => {
        const el = containerRef.current;
        if (!el) return;
        el.addEventListener('wheel', handleWheel, { passive: false });
        return () => el.removeEventListener('wheel', handleWheel);
    }, [handleWheel]);

    return (
        <div
            ref={containerRef}
            className="ksb__canvas"
            onMouseDown={handleMouseDown}
            onMouseMove={handleMouseMove}
            onMouseUp={handleMouseUp}
            onTouchStart={handleTouchStart}
            onTouchMove={handleTouchMove}
            onTouchEnd={handleTouchEnd}
            style={{ touchAction: 'none' }}
        >
            <div
                className="canvas-board"
                style={{
                    transform: `translate(${view.panX}px, ${view.panY}px) scale(${view.zoom})`,
                    transformOrigin: "0 0",
                    position: "absolute",
                    inset: 0,
                    overflow: "visible",
                }}
            >
                {areas.map((area) => (
                    <div key={area.areaId} style={{ position: 'relative' }}>
                        <LibrarianArea
                            area={area}
                            onSeatClick={(seat) => {
                                if (area.locked || !area.isActive) return;
                                onSeatClick(seat);
                            }}
                        />
                        {/* Locked/Inactive overlay */}
                        {(area.locked || !area.isActive) && (
                            <div
                                style={{
                                    position: 'absolute',
                                    left: area.positionX || 0,
                                    top: area.positionY || 0,
                                    width: area.width || 300,
                                    height: area.height || 250,
                                    background: 'rgba(0,0,0,0.45)',
                                    borderRadius: '12px',
                                    display: 'flex',
                                    flexDirection: 'column',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    gap: '8px',
                                    zIndex: 10,
                                    pointerEvents: 'none',
                                }}
                            >
                                <Lock size={32} color="#fff" />
                                <span style={{
                                    color: '#fff',
                                    fontSize: '16px',
                                    fontWeight: '700',
                                    textShadow: '0 1px 4px rgba(0,0,0,0.5)',
                                }}>
                                    {area.locked ? 'Phòng đang bị khóa' : 'Phòng đã đóng cửa'}
                                </span>
                            </div>
                        )}
                    </div>
                ))}
            </div>

            {/* Zoom controls */}
            <div className="ksb__zoom-controls">
                <button className="ksb__zoom-btn" onClick={handleZoomIn} title="Phóng to">+</button>
                <span className="ksb__zoom-level">{Math.round(view.zoom * 100)}%</span>
                <button className="ksb__zoom-btn" onClick={handleZoomOut} title="Thu nhỏ">-</button>
                <button className="ksb__zoom-btn ksb__zoom-btn--reset" onClick={fitToView} title="Vừa khung hình">Vừa</button>
            </div>
        </div>
    );
}

// Component chính
function KioskSeatBookingContent() {
    const { state, dispatch } = useLayout();
    const { selectedAreaId, seats, zones, areas } = state;
    const navigate = useNavigate();

    const [selectedSeat, setSelectedSeat] = useState(null);
    const [selectedSlot, setSelectedSlot] = useState(null); // label string: "07:00 - 08:00"
    const [loading, setLoading] = useState(false);
    const [timeSlots, setTimeSlots] = useState([]); // filtered list of labels
    const [bookingLoading, setBookingLoading] = useState(false);
    const [toast, setToast] = useState(null);

    // Library closed state (from library_settings)
    const [libraryClosed, setLibraryClosed] = useState(false);
    const [closedReason, setClosedReason] = useState('');

    const selectedAreaIdRef = useRef(selectedAreaId);
    const selectedSlotRef = useRef(selectedSlot);
    const requestIdRef = useRef(0);

    // Đọc session sinh viên: URL param > Router state > sessionStorage
    const location = useLocation();
    const sessionData = useMemo(() => {
        // 1. Từ URL query param (Dashboard truyền qua URL)
        const params = new URLSearchParams(location.search);
        const studentId = params.get('studentId');
        if (studentId) {
            return { studentId, studentName: params.get('name') || 'Sinh viên' };
        }

        // 2. Từ Router state
        const fromState = location.state?.session;
        if (fromState?.studentId) {
            return fromState;
        }

        // 3. Từ sessionStorage
        try {
            const raw = sessionStorage.getItem('kiosk_session');
            if (raw) {
                const parsed = JSON.parse(raw);
                if (parsed?.studentId) {
                    return parsed;
                }
            }
        } catch (e) { /* ignore */ }

        return null;
    }, [location]);

    const backTarget = sessionData?.studentId ? '/kiosk/student-mode' : '/kiosk';

    useEffect(() => { selectedAreaIdRef.current = selectedAreaId; }, [selectedAreaId]);
    useEffect(() => { selectedSlotRef.current = selectedSlot; }, [selectedSlot]);

    // Fetch library settings to check if library is closed
    useEffect(() => {
        const checkLibraryStatus = async () => {
            try {
                const res = await fetch(`${API_BASE_URL}/slib/settings/library`, { headers: getKioskHeaders() });
                const data = await res.json();
                setLibraryClosed(data.libraryClosed || false);
                setClosedReason(data.closedReason || '');
            } catch (err) {
                console.error('Failed to check library status:', err);
            }
        };
        checkLibraryStatus();
        // Re-check every 30s
        const interval = setInterval(checkLibraryStatus, 30000);
        return () => clearInterval(interval);
    }, []);

    // Fetch time slots từ library_settings và filter bỏ slots đã qua
    useEffect(() => {
        const fetchTimeSlots = async () => {
            try {
                const res = await fetch(`${API_BASE_URL}/slib/settings/time-slots`, { headers: getKioskHeaders() });
                const data = await res.json();
                const allSlots = (data || []).map((s) => s.label || `${s.startTime} - ${s.endTime}`);
                const validSlots = filterPastSlots(allSlots);
                setTimeSlots(validSlots);

                // Auto-chọn slot hiện tại (slot đầu tiên còn valid)
                if (validSlots.length > 0 && !selectedSlot) {
                    setSelectedSlot(validSlots[0]);
                }
            } catch (err) {
                console.error("Không tải được khung giờ từ library_settings:", err);
            }
        };
        fetchTimeSlots();
    }, []);

    const handleSeatClick = (seat) => {
        // Tìm zone và area, kiểm tra locked
        const zone = zones.find(z => z.zoneId === seat.zoneId);
        const area = areas.find(a => {
            const areaZones = zones.filter(z => z.areaId === a.areaId);
            return areaZones.some(z => z.zoneId === seat.zoneId);
        });
        if (area?.locked || area?.isActive === false) return;
        setSelectedSeat({ ...seat, zoneName: zone?.zoneName || '' });
    };

    const stats = useMemo(() => {
        const total = seats.length;
        let booked = 0, restricted = 0;
        seats.forEach((s) => {
            if (s.seatStatus === "BOOKED") booked += 1;
            if (s.seatStatus === "UNAVAILABLE") restricted += 1;
        });
        const available = Math.max(0, total - booked - restricted);
        const occupancy = total ? Math.round((booked / total) * 100) : 0;
        return { total, booked, restricted, available, occupancy };
    }, [seats]);

    // Load seats theo slot label
    const loadSeatsForSlot = useCallback(async (slotLabel) => {
        const areaId = selectedAreaIdRef.current;
        if (!areaId || !slotLabel) return;

        const myRequestId = ++requestIdRef.current;

        try {
            const timeParams = parseTimeSlot(slotLabel);
            const seatRes = await seatPlanService.getSeats(timeParams);

            if (myRequestId !== requestIdRef.current) return;

            const normalizedSeats = (seatRes.data || []).map(normalizeSeat);
            dispatch({ type: ACTIONS.SET_SEATS, payload: normalizedSeats });
        } catch (err) {
            if (myRequestId !== requestIdRef.current) return;
            console.error("Load seats error", err);
        } finally {
            if (myRequestId === requestIdRef.current) setLoading(false);
        }
    }, [dispatch]);

    // === BOOKING ===
    const handleBookSeat = useCallback(async () => {
        if (!sessionData?.studentId || !selectedSeat) return;
        if (selectedSeat.seatStatus !== "AVAILABLE") return;
        if (!selectedSlot) return;

        setBookingLoading(true);
        try {
            const timeParams = parseTimeSlot(selectedSlot);

            const createRes = await axios.post(`${API_BASE_URL}/slib/bookings/create`, {
                user_id: sessionData.studentId,
                seat_id: String(selectedSeat.seatId),
                start_time: timeParams.startTime,
                end_time: timeParams.endTime,
            }, { headers: getKioskHeaders() });

            const reservationId = createRes?.data?.reservationId;
            if (!reservationId) {
                throw new Error('Không nhận được mã đặt chỗ');
            }

            await axios.put(
                `${API_BASE_URL}/slib/bookings/updateStatusReserv/${reservationId}?status=BOOKED`,
                null,
                { headers: getKioskHeaders() }
            );

            setToast({ type: 'success', message: `Đặt chỗ ${selectedSeat.seatCode} thành công!` });
            setSelectedSeat(null);

            // Reload seats
            setLoading(true);
            loadSeatsForSlot(selectedSlot);
        } catch (err) {
            let msg = err.response?.data || err.message || 'Lỗi không xác định';
            if (typeof msg === 'string') {
                msg = msg.replace(/^Error:\s*/i, '');
            }
            setToast({ type: 'error', message: msg });
            setSelectedSeat(null);
            loadSeatsForSlot(selectedSlot);
        } finally {
            setBookingLoading(false);
        }
    }, [sessionData, selectedSeat, selectedSlot, loadSeatsForSlot]);

    // Auto-hide toast
    useEffect(() => {
        if (!toast) return;
        const timer = setTimeout(() => setToast(null), 4000);
        return () => clearTimeout(timer);
    }, [toast]);

    useEffect(() => {
        if (selectedAreaId && selectedSlot) {
            setLoading(true);
            loadSeatsForSlot(selectedSlot);
        }
    }, [selectedAreaId, selectedSlot]);

    // Auto-refresh mỗi 10s (fallback khi WebSocket mất kết nối)
    useEffect(() => {
        if (!selectedSlot) return;
        const interval = setInterval(() => {
            loadSeatsForSlot(selectedSlot);
        }, 10000);
        return () => clearInterval(interval);
    }, [selectedSlot, loadSeatsForSlot]);

    // WebSocket real-time seat updates (booking mới, cancel, hold, etc.)
    useEffect(() => {
        let unsubscribe = null;

        websocketService.connect(
            () => {
                unsubscribe = websocketService.subscribe("/topic/seats", (msg) => {
                    if (selectedSlotRef.current) {
                        loadSeatsForSlot(selectedSlotRef.current);
                    }
                });
            },
            (error) => {
                console.error("[KioskSeat] WebSocket error:", error);
            }
        );

        return () => {
            if (unsubscribe) unsubscribe();
        };
    }, [loadSeatsForSlot]);

    return (
        <div className="ksb">
            {/* Header */}
            <div className="ksb__header">
                <button className="ksb__back" onClick={() => navigate(backTarget)}>
                    <ArrowLeft size={20} />
                </button>
                <h1 className="ksb__title">Sơ đồ chỗ ngồi</h1>
                <div className="ksb__slot-select">
                    <Clock size={16} />
                    {timeSlots.length > 0 ? (
                        <select
                            value={selectedSlot || ''}
                            onChange={(e) => setSelectedSlot(e.target.value)}
                        >
                            {timeSlots.map((slot) => (
                                <option key={slot} value={slot}>{slot}</option>
                            ))}
                        </select>
                    ) : (
                        <span style={{ fontSize: 14, color: '#999' }}>Hết giờ hoạt động</span>
                    )}
                </div>
            </div>

            {/* Stats bar */}
            <div className="ksb__stats">
                <div className="ksb__stat">
                    <span className="ksb__stat-dot ksb__stat-dot--available"></span>
                    <span>Trống: <strong>{stats.available}</strong></span>
                </div>
                <div className="ksb__stat">
                    <span className="ksb__stat-dot ksb__stat-dot--booked"></span>
                    <span>Đã đặt: <strong>{stats.booked}</strong></span>
                </div>
                <div className="ksb__stat">
                    <span className="ksb__stat-dot ksb__stat-dot--unavailable"></span>
                    <span>Hạn chế: <strong>{stats.restricted}</strong></span>
                </div>
            </div>

            {/* Full-page locked message when library is closed (from settings) */}
            {libraryClosed && (
                <div className="ksb__locked-overlay">
                    <div className="ksb__locked-card">
                        <Lock size={48} color="#DC2626" />
                        <h2>Thư viện hiện đang tạm đóng</h2>
                        <p>{closedReason || 'Thư viện đang tạm ngưng hoạt động. Vui lòng quay lại sau.'}</p>
                        <button onClick={() => navigate(backTarget)} className="ksb__locked-back">
                            <ArrowLeft size={18} /> Quay lại
                        </button>
                    </div>
                </div>
            )}

            {/* Full-page locked message when ALL areas are locked/inactive */}
            {!libraryClosed && areas.length > 0 && areas.every(a => a.locked || !a.isActive) && (
                <div className="ksb__locked-overlay">
                    <div className="ksb__locked-card">
                        <Lock size={48} color="#DC2626" />
                        <h2>Thư viện hiện đang đóng cửa</h2>
                        <p>Tất cả phòng đọc đang bị khóa hoặc tạm ngưng hoạt động. Vui lòng quay lại sau.</p>
                        <button onClick={() => navigate(backTarget)} className="ksb__locked-back">
                            <ArrowLeft size={18} /> Quay lại
                        </button>
                    </div>
                </div>
            )}

            {/* Canvas */}
            <div className="ksb__map">
                {loading && <div className="ksb__loading">Đang tải sơ đồ...</div>}
                <KioskCanvas onSeatClick={handleSeatClick} />
            </div>

            {/* Toast thông báo */}
            {toast && (
                <div className={`ksb__toast ksb__toast--${toast.type}`}>
                    {toast.type === 'success' ? <CheckCircle size={20} /> : <XCircle size={20} />}
                    <span>{toast.message}</span>
                </div>
            )}

            {/* Seat detail popup */}
            {selectedSeat && (
                <div className="ksb__detail-overlay" onClick={() => setSelectedSeat(null)}>
                    <div className="ksb__detail-card" onClick={(e) => e.stopPropagation()}>
                        <div className="ksb__detail-header">
                            <Armchair size={20} />
                            <span className="ksb__detail-code">{selectedSeat.seatCode}</span>
                            <span className={`ksb__detail-status ksb__detail-status--${selectedSeat.seatStatus?.toLowerCase()}`}>
                                {selectedSeat.seatStatus === "AVAILABLE" ? "Trống" :
                                    selectedSeat.seatStatus === "BOOKED" ? "Đã đặt" : "Hạn chế"}
                            </span>
                        </div>
                        <div className="ksb__detail-info">
                            <div className="ksb__detail-info-row">
                                <span className="ksb__detail-info-label">Khu vực</span>
                                <span className="ksb__detail-info-value">{selectedSeat.zoneName || 'Chưa xác định'}</span>
                            </div>
                            <div className="ksb__detail-info-row">
                                <span className="ksb__detail-info-label">Ngày</span>
                                <span className="ksb__detail-info-value">{new Date().toLocaleDateString('vi-VN')}</span>
                            </div>
                            <div className="ksb__detail-info-row">
                                <span className="ksb__detail-info-label">Khung giờ</span>
                                <span className="ksb__detail-info-value">{selectedSlot || 'Chưa chọn'}</span>
                            </div>
                        </div>

                        {/* Nút đặt chỗ - chỉ hiện khi ghế trống VÀ có session sinh viên */}
                        {selectedSeat.seatStatus === "AVAILABLE" && sessionData?.studentId && (
                            <button
                                className="ksb__detail-book"
                                onClick={handleBookSeat}
                                disabled={bookingLoading}
                            >
                                {bookingLoading ? "Đang đặt..." : "Đặt chỗ này"}
                            </button>
                        )}

                        {/* Nếu ghế trống nhưng chưa đăng nhập */}
                        {selectedSeat.seatStatus === "AVAILABLE" && !sessionData?.studentId && (
                            <div className="ksb__detail-note">
                                Vui lòng quét QR đăng nhập trước khi đặt chỗ
                            </div>
                        )}

                        {selectedSeat.seatStatus === "BOOKED" && (
                            <div className="ksb__detail-note ksb__detail-note--warn">
                                Ghế này đã được đặt trong khung giờ hiện tại
                            </div>
                        )}

                        <button className="ksb__detail-close" onClick={() => setSelectedSeat(null)}>
                            Đóng
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}

const KioskSeatBooking = () => {
    return (
        <LayoutProvider>
            <KioskSeatBookingContent />
        </LayoutProvider>
    );
};

export default KioskSeatBooking;
