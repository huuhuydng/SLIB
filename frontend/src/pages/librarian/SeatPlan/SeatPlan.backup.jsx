// Đã xóa file này vì không còn sử dụng
import React, { useEffect, useMemo, useState } from "react";

import { LayoutProvider, useLayout, ACTIONS } from "../../../context/admin/area_management/LayoutContext";
import CanvasBoard from "../../../components/admin/area_managements/CanvasBoard";
import { seatPlanService } from "../../../services/librarian/seatPlanService";
import { seatService } from "../../../services/librarian/seatService";
import { handleLogout } from "../../../utils/auth";
import "../../../styles/librarian/SeatPlan.css";
import "../../../styles/admin/layout.css";
import { Armchair, AlertCircle, ShieldOff, ShieldCheck, Clock4, LayoutTemplate } from "lucide-react";

const TIME_SLOTS = [
  { label: "Hiện tại", value: "now" },
  { label: "07:00 - 09:00", value: "07:00-09:00" },
  { label: "09:00 - 11:00", value: "09:00-11:00" },
  { label: "11:00 - 13:00", value: "11:00-13:00" },
  { label: "13:00 - 15:00", value: "13:00-15:00" },
  { label: "15:00 - 17:00", value: "15:00-17:00" },
];

const getTodayRange = (slotValue) => {
  const today = new Date();
  const yyyy = today.getFullYear();
  const mm = String(today.getMonth() + 1).padStart(2, "0");
  const dd = String(today.getDate()).padStart(2, "0");

  if (slotValue === "now") {
    const hour = today.getHours();
    if (hour >= 7 && hour < 9) return { start: "07:00", end: "09:00" };
    if (hour >= 9 && hour < 11) return { start: "09:00", end: "11:00" };
    if (hour >= 11 && hour < 13) return { start: "11:00", end: "13:00" };
    if (hour >= 13 && hour < 15) return { start: "13:00", end: "15:00" };
    if (hour >= 15 && hour < 17) return { start: "15:00", end: "17:00" };
    return { start: "07:00", end: "09:00" };
  }

  const [start, end] = slotValue.split("-");
  return { start, end };
};

const buildTimeParams = (slotValue) => {
  const range = getTodayRange(slotValue);
  const today = new Date();
  const yyyy = today.getFullYear();
  const mm = String(today.getMonth() + 1).padStart(2, "0");
  const dd = String(today.getDate()).padStart(2, "0");
  const datePrefix = `${yyyy}-${mm}-${dd}`;
  return {
    startTime: `${datePrefix}T${range.start}:00`,
    endTime: `${datePrefix}T${range.end}:00`,
  };
};

const normalizeArea = (area) => ({
  areaId: area.area_id ?? area.areaId,
  areaName: area.area_name ?? area.areaName,
  width: area.width ?? 1000,
  height: area.height ?? 730,
  positionX: area.position_x ?? area.positionX ?? 0,
  positionY: area.position_y ?? area.positionY ?? 0,
  locked: area.locked ?? area.is_locked ?? false,
});

const normalizeZone = (zone) => ({
  zoneId: zone.zone_id ?? zone.zoneId,
  zoneName: zone.zone_name ?? zone.zoneName,
  zoneDes: zone.zone_des ?? zone.zoneDes ?? "",
  areaId: zone.area_id ?? zone.areaId,
  positionX: zone.position_x ?? zone.positionX ?? 0,
  positionY: zone.position_y ?? zone.positionY ?? 0,
  width: zone.width ?? 450,
  height: zone.height ?? 400,
  color: zone.color ?? "#e7f1ff",
});

const normalizeSeat = (seat) => ({
  seatId: seat.seatId ?? seat.seat_id,
  zoneId: seat.zoneId ?? seat.zone_id,
  seatCode: seat.seatCode ?? seat.seat_code ?? seat.code,
  seatStatus: (seat.seatStatus ?? seat.seat_status ?? seat.status ?? "AVAILABLE").toUpperCase(),
  rowNumber: seat.rowNumber ?? seat.row_number ?? 1,
  columnNumber: seat.columnNumber ?? seat.column_number ?? 1,
});

// Component that uses admin's layout system for librarian view
function SeatPlanContent() {
  const { state, dispatch } = useLayout();
  const { selectedAreaId, seats, zones } = state;

  const [selectedSeat, setSelectedSeat] = useState(null);
  const [slotValue, setSlotValue] = useState("now");
  const [message, setMessage] = useState(null);
  const [loading, setLoading] = useState(false);

  const zonesOfArea = useMemo(() => zones.filter((z) => z.areaId === selectedAreaId), [zones, selectedAreaId]);

  const seatsByZone = useMemo(() => {
    const map = new Map();
    zonesOfArea.forEach((z) => map.set(z.zoneId, []));
    seats.forEach((seat) => {
      if (map.has(seat.zoneId)) {
        map.get(seat.zoneId).push(seat);
      }
    });
    return map;
  }, [seats, zonesOfArea]);

  const stats = useMemo(() => {
    const total = seatsByZone.size
      ? Array.from(seatsByZone.values()).reduce((acc, list) => acc + list.length, 0)
      : 0;
    let booked = 0;
    let restricted = 0;
    seats.forEach((s) => {
      if (s.seatStatus === "BOOKED") booked += 1;
      if (s.seatStatus === "UNAVAILABLE") restricted += 1;
    });
    const available = Math.max(0, total - booked - restricted);
    const occupancy = total ? Math.round((booked / total) * 100) : 0;
    return { total, booked, restricted, available, occupancy };
  }, [seats, seatsByZone]);

  const loadAreas = async () => {
    setLoading(true);
    try {
      const res = await seatPlanService.getAreas();
      const normalized = (res.data || []).map(normalizeArea);

      console.log('📍 All areas:', normalized);

      setAreas(normalized);
      if (normalized[0]) {
        setSelectedAreaId(normalized[0].areaId);
      }
    } catch (err) {
      setMessage("Không tải được danh sách khu vực");
    } finally {
      setLoading(false);
    }
  };

  const loadZonesAndSeats = async (areaId, slot) => {
    if (!areaId) return;
    setLoading(true);
    try {
      const timeParams = buildTimeParams(slot);
      const [zoneRes, seatRes] = await Promise.all([
        seatPlanService.getZonesByArea(areaId),
        seatPlanService.getSeats(timeParams),
      ]);

      const normalizedSeats = (seatRes.data || []).map(normalizeSeat);

      // DEBUG: Log raw API response with details
      console.log('🔍 RAW API zones response:', zoneRes.data);

      let normalizedZones = (zoneRes.data || []).map(zone => {
        const normalized = normalizeZone(zone);
        // Ensure areaId is set correctly
        if (!normalized.areaId) {
          normalized.areaId = areaId;
        }
        return normalized;
      });

      // Auto-fit zone height based on seat count
      normalizedZones = normalizedZones.map(zone => {
        const zoneSeats = normalizedSeats.filter(s => s.zoneId === zone.zoneId);
        if (zoneSeats.length > 0) {
          const maxRow = Math.max(...zoneSeats.map(s => s.rowNumber || 1));
          const maxCol = Math.max(...zoneSeats.map(s => s.columnNumber || 1));

          // Calculate required dimensions (60px seats + 8px gap + 12px zone padding + header)
          const requiredWidth = maxCol * 60 + (maxCol - 1) * 8 + 24; // 12px padding * 2
          const requiredHeight = 40 + 10 + maxRow * 60 + (maxRow - 1) * 8 + 24; // header + gap + content + padding

          // Only update if calculated size is larger than default
          if (requiredWidth > zone.width) zone.width = requiredWidth;
          if (requiredHeight > zone.height) zone.height = requiredHeight;
        }
        return zone;
      });

      console.log('📍 Area ID:', areaId);
      console.log('📍 Loaded zones (normalized):', normalizedZones);
      console.log('📍 Zone positions:', normalizedZones.map(z => ({ name: z.zoneName, x: z.positionX, y: z.positionY, w: z.width, h: z.height })));
      console.log('📍 Loaded seats:', normalizedSeats.length, 'seats');

      setZones((prev) => {
        const other = prev.filter((z) => z.areaId !== areaId);
        return [...other, ...normalizedZones];
      });
      setSeats(normalizedSeats);
      setMessage(null);
    } catch (err) {
      console.error("Load zones/seats error", err);
      setMessage("Không tải được dữ liệu sơ đồ hoặc danh sách ghế");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAreas();
  }, []);

  useEffect(() => {
    if (selectedAreaId) {
      loadZonesAndSeats(selectedAreaId, slotValue);
    }
  }, [selectedAreaId, slotValue]);

  const handleSeatClick = (seat) => {
    setSelectedSeat(seat);
  };

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
      await loadZonesAndSeats(selectedAreaId, slotValue);
      setSelectedSeat((prev) =>
        prev && prev.seatId === seat.seatId
          ? { ...prev, seatStatus: isRestricted ? "AVAILABLE" : "UNAVAILABLE" }
          : prev
      );
    } catch (err) {
      setMessage("Cập nhật hạn chế thất bại");
    }
  };

  const renderSeatGrid = (zoneId) => {
    const zoneSeats = seatsByZone.get(zoneId) || [];

    if (zoneSeats.length === 0) {
      return <div className="sp-empty">Không có ghế</div>;
    }

    // Tìm số hàng và cột max để tạo layout đẹp
    const maxRow = zoneSeats.reduce((max, s) => Math.max(max, s.rowNumber || 1), 1);
    const maxCol = zoneSeats.reduce((max, s) => Math.max(max, s.columnNumber || 1), 1);

    // Tạo ma trận ghế theo vị trí row/column
    const seatMatrix = Array.from({ length: maxRow }, () => Array(maxCol).fill(null));

    zoneSeats.forEach((seat) => {
      const row = (seat.rowNumber || 1) - 1;
      const col = (seat.columnNumber || 1) - 1;
      if (row >= 0 && row < maxRow && col >= 0 && col < maxCol) {
        seatMatrix[row][col] = seat;
      }
    });

    return (
      <div
        className="sp-seat-grid"
        style={{
          gridTemplateColumns: `repeat(${maxCol}, 60px)`,
          gridTemplateRows: `repeat(${maxRow}, 60px)`
        }}
      >
        {seatMatrix.flat().map((seat, idx) => {
          if (!seat) {
            return <div key={`empty-${idx}`} className="sp-seat-empty"></div>;
          }

          const status = seat.seatStatus;
          const statusClass =
            status === "BOOKED"
              ? "sp-seat--booked"
              : status === "UNAVAILABLE"
                ? "sp-seat--blocked"
                : "sp-seat--free";

          return (
            <button
              key={seat.seatId}
              className={`sp-seat ${statusClass} ${selectedSeat?.seatId === seat.seatId ? "sp-seat--selected" : ""}`}
              onClick={() => handleSeatClick(seat)}
            >
              {seat.seatCode}
            </button>
          );
        })}
      </div>
    );
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
              value={selectedAreaId || ""}
              onChange={(e) => setSelectedAreaId(Number(e.target.value))}
            >
              {areas.map((a) => (
                <option key={a.areaId} value={a.areaId}>
                  {a.areaName}
                </option>
              ))}
            </select>
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
              <div className="sp-card__title">Khung gidd</div>
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

        <div className="sp-layout">
          <div className="sp-canvas-wrapper">
            {loading && <div className="sp-loading">Đang tải dữ liệu...</div>}
            {!loading && selectedArea && (
              <div
                className="sp-canvas"
                style={{
                  position: 'relative',
                  width: `${selectedArea.width}px`,
                  height: `${selectedArea.height}px`,
                  border: '1px solid #ddd',
                  borderRadius: '4px',
                  margin: '0 auto'
                }}
              >
                {zonesOfArea.map((zone) => (
                  <div
                    key={zone.zoneId}
                    className="sp-zone"
                    style={{
                      left: `${zone.positionX}px`,
                      top: `${zone.positionY}px`,
                      width: `${zone.width}px`,
                      height: `${zone.height}px`,
                    }}
                  >
                    <div className="sp-zone__header">
                      <span>{zone.zoneName}</span>
                      <span className="sp-zone__count">
                        {(seatsByZone.get(zone.zoneId) || []).length} ghế
                      </span>
                    </div>
                    {renderSeatGrid(zone.zoneId)}
                  </div>
                ))}
                {zonesOfArea.length === 0 && (
                  <div className="sp-empty">Chưa có khu vực nào trong khu này</div>
                )}
              </div>
            )}
          </div>

          <aside className="sp-sidebar">
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
};

export default SeatPlan;
