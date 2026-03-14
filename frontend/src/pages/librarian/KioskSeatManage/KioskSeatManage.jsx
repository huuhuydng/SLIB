import React, { useState, useMemo, useEffect } from "react";
import {
  Armchair,
  Sparkles,
  Check,
  Filter,
  CircleAlert,
  ChevronDown,
  Search,
  User,
  Clock,
  Shield,
  ExternalLink,
  X,
  Users,
} from "lucide-react";
import { seatService } from '../../../services/librarian/seatService';
import "../../../styles/librarian/librarian-shared.css";
import "../../../styles/librarian/CheckInOut.css";
import "../../../styles/librarian/SeatManage.css";

// Tính occupancy cho mỗi zone từ seatStatusMap
const getZoneOccupancy = (zoneSeats, seatStatusMap) => {
  const total = zoneSeats.length;
  if (total === 0) return { percent: 0, booked: 0, total: 0 };
  const booked = zoneSeats.filter(s => seatStatusMap[s.id]?.status === 'BOOKED').length;
  return { percent: Math.round((booked / total) * 100), booked, total };
};

const getOccupancyLevel = (percent) => {
  if (percent >= 90) return { level: 'high', text: 'Hết chỗ', color: '#E74C3C' };
  if (percent >= 50) return { level: 'medium', text: 'Khá đông', color: '#F39C12' };
  return { level: 'low', text: 'Trống', color: '#27AE60' };
};

const generateSeats = (prefix, start, count, zoneName) => {
  return Array.from({ length: count }, (_, i) => ({
    id: `${prefix}${i + 1}`,
    zone: zoneName,
  }));
};

const SEATS_A = generateSeats("A", 1, 21, "Khu tự học");
const SEATS_B = generateSeats("B", 1, 28, "Khu yên tĩnh");
const SEATS_C = generateSeats("C", 1, 21, "Khu thảo luận");

const ALL_SEATS = [...SEATS_A, ...SEATS_B, ...SEATS_C];
const TOTAL_SEATS = SEATS_A.length + SEATS_B.length + SEATS_C.length;

const TIME_SLOTS = [
  "Hiện tại",
  "07:00 - 09:00",
  "09:00 - 11:00",
  "11:00 - 13:00",
  "13:00 - 15:00",
  "15:00 - 17:00",
];

const FILTER_OPTIONS = [
  "Tất cả khu vực",
  "Khu yên tĩnh",
  "Khu thảo luận",
  "Khu tự học",
];

const KioskSeatManage = () => {
  const [currentSlotIndex, setCurrentSlotIndex] = useState(0);
  const [seatStatusMap, setSeatStatusMap] = useState({});

  const [selectedSeatId, setSelectedSeatId] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [activeFilter, setActiveFilter] = useState("Tất cả khu vực");
  const [toastMsg, setToastMsg] = useState(null);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    totalSeats: TOTAL_SEATS,
    bookedSeats: 0,
    unavailableSeats: 0,
    availableSeats: TOTAL_SEATS
  });

  const parseTimeSlot = (slotIndex) => {
    if (slotIndex === 0) {
      return null;
    }

    const slot = TIME_SLOTS[slotIndex];
    const [startStr, endStr] = slot.split(' - ');

    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');

    const startTime = `${year}-${month}-${day}T${startStr}:00`;
    const endTime = `${year}-${month}-${day}T${endStr}:00`;

    return { startTime, endTime };
  };

  const getCurrentTimeSlot = () => {
    const now = new Date();
    const hours = now.getHours();

    // Xác định khung giờ hiện tại
    if (hours >= 7 && hours < 9) return { start: '07:00', end: '09:00' };
    if (hours >= 9 && hours < 11) return { start: '09:00', end: '11:00' };
    if (hours >= 11 && hours < 13) return { start: '11:00', end: '13:00' };
    if (hours >= 13 && hours < 15) return { start: '13:00', end: '15:00' };
    if (hours >= 15 && hours < 17) return { start: '15:00', end: '17:00' };

    // Mặc định: 07:00-09:00
    return { start: '07:00', end: '09:00' };
  };

  const loadSeatDataForTimeSlot = async (slotIndex) => {
    try {
      setLoading(true);
      setSeatStatusMap({});

      let response;
      if (slotIndex === 0) {
        // "Hiện tại" - xác định khung giờ hiện tại theo realtime
        const currentSlot = getCurrentTimeSlot();
        const today = new Date();
        const year = today.getFullYear();
        const month = String(today.getMonth() + 1).padStart(2, '0');
        const day = String(today.getDate()).padStart(2, '0');

        const startTime = `${year}-${month}-${day}T${currentSlot.start}:00`;
        const endTime = `${year}-${month}-${day}T${currentSlot.end}:00`;

        response = await seatService.getAllSeats({
          startTime,
          endTime
        });
      } else {
        const timeRange = parseTimeSlot(slotIndex);
        response = await seatService.getAllSeats({
          startTime: timeRange.startTime,
          endTime: timeRange.endTime
        });
      }

      let seatsData = [];

      if (response) {
        if (Array.isArray(response.seats)) {
          seatsData = response.seats;
        } else if (Array.isArray(response.seatResponses)) {
          seatsData = response.seatResponses;
        } else if (Array.isArray(response)) {
          seatsData = response;
        }
      }

      // Tạo map: seat_code -> { status, seatId, booker info }
      const statusMap = {};
      seatsData.forEach(seat => {
        const seatCode = seat.seatCode || seat.seat_code || seat.code;
        const status = seat.seatStatus || seat.status;
        const seatId = seat.seatId || seat.seat_id || seat.id;
        if (seatCode && status) {
          statusMap[seatCode] = {
            status,
            seatId,
            reservationId: seat.reservationId || null,
            bookedByUserName: seat.bookedByUserName || null,
            bookedByUserCode: seat.bookedByUserCode || null,
            bookedByAvatarUrl: seat.bookedByAvatarUrl || null,
            reservationStartTime: seat.reservationStartTime || null,
            reservationEndTime: seat.reservationEndTime || null,
          };
        }
      });

      setSeatStatusMap(statusMap);

      // Tính toán stats
      const bookedCount = Object.values(statusMap).filter(s => s.status === 'BOOKED').length;
      const unavailableCount = Object.values(statusMap).filter(s => s.status === 'UNAVAILABLE').length;
      const availableCount = TOTAL_SEATS - bookedCount - unavailableCount;

      setStats({
        totalSeats: TOTAL_SEATS,
        bookedSeats: bookedCount,
        unavailableSeats: unavailableCount,
        availableSeats: Math.max(0, availableCount)
      });

    } catch (error) {
      showToast('Lỗi tải dữ liệu khung giờ');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSeatDataForTimeSlot(currentSlotIndex);
  }, [currentSlotIndex]);

  const showToast = (msg) => {
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 3000);
  };

  const handleSeatClick = (seat) => {
    setSelectedSeatId(seat.id);
  };

  const toggleRestriction = async (seatCode) => {
    if (!seatCode) return;

    try {
      const seatData = seatStatusMap[seatCode];
      const status = seatData?.status || 'AVAILABLE';
      const dbSeatId = seatData?.seatId; // Lấy seatId từ database

      if (!dbSeatId) {
        showToast(`Không tìm thấy ghế ${seatCode} trong database`);
        return;
      }

      const isRestricted = status === 'UNAVAILABLE';

      if (isRestricted) {
        await seatService.removeRestriction(dbSeatId);
        showToast(`Đã bỏ hạn chế ghế ${seatCode}`);
      } else {
        await seatService.addRestriction(dbSeatId);
        showToast(`Đã hạn chế ghế ${seatCode}`);
      }

      await loadSeatDataForTimeSlot(currentSlotIndex);

    } catch (error) {
      showToast('Lỗi cập nhật hạn chế. Vui lòng thử lại.');
    }

    setSelectedSeatId(null);
  };

  const confirmReservation = async (seatCode) => {
    const seatData = seatStatusMap[seatCode];
    if (!seatData?.reservationId) {
      showToast('Không tìm thấy mã đặt chỗ');
      return;
    }
    try {
      const token = localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');
      const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
      const res = await fetch(
        `${API_BASE}/slib/bookings/updateStatusReserv/${seatData.reservationId}?status=CONFIRMED`,
        {
          method: 'PUT',
          headers: { Authorization: `Bearer ${token}` },
        }
      );
      if (!res.ok) throw new Error('Lỗi xác nhận');
      showToast(`Đã xác nhận chỗ ngồi ${seatCode}`);
      await loadSeatDataForTimeSlot(currentSlotIndex);
      setSelectedSeatId(null);
    } catch (error) {
      showToast('Lỗi xác nhận chỗ ngồi. Vui lòng thử lại.');
    }
  };

  const handleSearch = (e) => {
    const val = e.target.value;
    setSearchTerm(val);

    if (val.trim()) {
      const found = ALL_SEATS.find(
        (s) => s.id.toLowerCase() === val.toLowerCase()
      );
      if (found) {
        setSelectedSeatId(found.id);
      }
    } else {
      setSelectedSeatId(null);
    }
  };

  const handleFilterChange = (e) => {
    setActiveFilter(e.target.value);
  };

  const renderSeat = (seat) => {
    const seatData = seatStatusMap[seat.id];
    const status = seatData?.status || 'AVAILABLE';
    const isSelected = selectedSeatId === seat.id;

    let isDimmed = false;
    if (activeFilter !== "Tất cả khu vực" && seat.zone !== activeFilter) {
      isDimmed = true;
    }

    // Xác định CSS class dựa vào status
    let statusClass = "seatManage__seat--empty"; // AVAILABLE -> xanh dương
    if (status === 'UNAVAILABLE') {
      statusClass = "seatManage__seat--restricted"; // UNAVAILABLE -> xám
    } else if (status === 'BOOKED') {
      statusClass = "seatManage__seat--occupied"; // BOOKED -> cam
    }

    return (
      <div
        key={seat.id}
        className={`seatManage__seat ${statusClass} ${isSelected ? "seatManage__seat--selected" : ""
          } ${isDimmed ? "seatManage__seat--dimmed" : ""}`}
        onClick={() => handleSeatClick(seat)}
      >
        {seat.id}
      </div>
    );
  };

  if (loading) {
    return (
      <div className="lib-container">
        <div className="lib-loading">
          <div className="lib-spinner" />
        </div>
      </div>
    );
  }

  return (
    <div className="lib-container">
      {/* Page Title */}
      <div className="lib-page-title">
        <h1>SƠ ĐỒ CHỖ NGỒI</h1>
      </div>

      {/* Toolbar */}
      <div className="lib-panel">
        <div className="cio-toolbar">
          <div className="lib-search">
            <Search size={16} className="lib-search-icon" />
            <input
              type="text"
              value={searchTerm}
              onChange={handleSearch}
              placeholder="Tìm ghế (A1, B2...)"
            />
          </div>

          <div className="seatManage__filterWrapper">
            <div className="seatManage__dropdownTrigger">
              <Filter size={16} />
              <select
                value={activeFilter}
                onChange={handleFilterChange}
                className="seatManage__filterSelect"
              >
                {FILTER_OPTIONS.map(opt => (
                  <option key={opt} value={opt}>{opt}</option>
                ))}
              </select>
              <ChevronDown size={14} className="seatManage__dropdownArrow" />
            </div>
          </div>

          <div className="seatManage__filterWrapper">
            <div className="seatManage__dropdownTrigger">
              <Clock size={16} />
              <select
                value={currentSlotIndex}
                onChange={(e) => setCurrentSlotIndex(Number(e.target.value))}
                className="seatManage__filterSelect"
              >
                {TIME_SLOTS.map((slot, index) => (
                  <option key={slot} value={index}>{slot}</option>
                ))}
              </select>
              <ChevronDown size={14} className="seatManage__dropdownArrow" />
            </div>
          </div>

          <div style={{ display: 'flex', gap: 16, alignItems: 'center', marginLeft: 'auto' }}>
            <span className="lib-inline-stat">
              <span className="dot orange"></span>
              Đang dùng <strong>{stats.bookedSeats}/{stats.totalSeats}</strong>
            </span>
            <span className="lib-inline-stat">
              <span className="dot green"></span>
              Trống <strong>{stats.availableSeats}</strong>
            </span>
            <span className="lib-inline-stat">
              <span className="dot gray"></span>
              Hạn chế <strong>{stats.unavailableSeats}</strong>
            </span>
          </div>
        </div>
      </div>

      {/* Map Panel */}
      <div className="lib-panel seatManage__mapPanel">
        <div className="seatManage__mapGrid">
          <div className="seatManage__staticElement seatManage__bookshelf">Kệ<br />sách</div>
          <div className="seatManage__staticElement seatManage__entrance">Cửa ra vào</div>
          <div className="seatManage__staticElement seatManage__hall">Sảnh chính</div>
          <div className="seatManage__staticElement seatManage__librarian">Thủ thư</div>
          <div className="seatManage__staticElement seatManage__pillar"></div>

          {(() => {
            const occB = getZoneOccupancy(SEATS_B, seatStatusMap);
            const levelB = getOccupancyLevel(occB.percent);
            return (
              <div className={`seatManage__zone seatManage__zoneB seatManage__zone--${levelB.level}`}>
                <div className="seatManage__zoneHeader">
                  <span className="seatManage__zoneName">Khu yên tĩnh</span>
                  <span className="seatManage__zoneBadge" style={{ background: levelB.color }}>
                    {occB.percent}% - {levelB.text}
                  </span>
                </div>
                <div className="seatManage__zoneSeats">
                  {SEATS_B.map(renderSeat)}
                </div>
              </div>
            );
          })()}

          {(() => {
            const occA = getZoneOccupancy(SEATS_A, seatStatusMap);
            const levelA = getOccupancyLevel(occA.percent);
            return (
              <div className={`seatManage__zone seatManage__zoneA seatManage__zone--${levelA.level}`}>
                <div className="seatManage__zoneHeader">
                  <span className="seatManage__zoneName">Khu tự học</span>
                  <span className="seatManage__zoneBadge" style={{ background: levelA.color }}>
                    {occA.percent}% - {levelA.text}
                  </span>
                </div>
                <div className="seatManage__zoneSeats">
                  {SEATS_A.map(renderSeat)}
                </div>
              </div>
            );
          })()}

          <div className="seatManage__divider-wall"></div>

          {(() => {
            const occC = getZoneOccupancy(SEATS_C, seatStatusMap);
            const levelC = getOccupancyLevel(occC.percent);
            return (
              <div className={`seatManage__zone seatManage__zoneC seatManage__zone--${levelC.level}`}>
                <div className="seatManage__zoneHeader">
                  <span className="seatManage__zoneName">Khu thảo luận</span>
                  <span className="seatManage__zoneBadge" style={{ background: levelC.color }}>
                    {occC.percent}% - {levelC.text}
                  </span>
                </div>
                <div className="seatManage__zoneSeats">
                  {SEATS_C.map(renderSeat)}
                </div>
              </div>
            );
          })()}
        </div>

        {selectedSeatId && (() => {
          const seatData = seatStatusMap[selectedSeatId];
          const status = seatData?.status || 'AVAILABLE';
          const zone = ALL_SEATS.find(s => s.id === selectedSeatId)?.zone;

          if (status === 'BOOKED' && seatData?.bookedByUserName) {
            // Card thông tin sinh viên đang đặt
            const startTime = seatData.reservationStartTime
              ? new Date(seatData.reservationStartTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
              : '--:--';
            const endTime = seatData.reservationEndTime
              ? new Date(seatData.reservationEndTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
              : '--:--';

            return (
              <div className="seatManage__seatActionCard seatManage__bookerCard">
                <div className="seatManage__actionHeader">
                  <Armchair size={18} />
                  <span>{selectedSeatId}</span>
                  <span className="seatManage__actionZone">- {zone}</span>
                  <button className="seatManage__closeBtn" onClick={() => setSelectedSeatId(null)}>
                    <X size={16} />
                  </button>
                </div>

                <div className="seatManage__bookerInfo">
                  <div className="seatManage__bookerAvatar">
                    {seatData.bookedByAvatarUrl ? (
                      <img src={seatData.bookedByAvatarUrl} alt="avatar" />
                    ) : (
                      <div className="seatManage__avatarFallback">
                        <User size={22} />
                      </div>
                    )}
                  </div>
                  <div className="seatManage__bookerDetails">
                    <span className="seatManage__bookerName">{seatData.bookedByUserName}</span>
                    <span className="seatManage__bookerCode">{seatData.bookedByUserCode}</span>
                  </div>
                </div>

                <div className="seatManage__bookerTime">
                  <Clock size={14} />
                  <span>{startTime} - {endTime}</span>
                </div>

                <div className="seatManage__actionBody">
                  {status === 'BOOKED' && seatData?.reservationId && (
                    <button
                      className="lib-btn primary"
                      onClick={() => confirmReservation(selectedSeatId)}
                    >
                      <Check size={14} />
                      Xác nhận chỗ ngồi
                    </button>
                  )}
                  <button
                    className="lib-btn ghost danger"
                    onClick={() => toggleRestriction(selectedSeatId)}
                  >
                    <Shield size={14} />
                    Hạn chế ghế này
                  </button>
                </div>
              </div>
            );
          }

          // Card cho ghế AVAILABLE hoặc UNAVAILABLE
          return (
            <div className="seatManage__seatActionCard">
              <div className="seatManage__actionHeader">
                <Armchair size={18} />
                <span>{selectedSeatId}</span>
                <span className="seatManage__actionZone">- {zone}</span>
                <button className="seatManage__closeBtn" onClick={() => setSelectedSeatId(null)}>
                  <X size={16} />
                </button>
              </div>
              <div className="seatManage__actionBody">
                <button
                  className={`lib-btn ${status === 'UNAVAILABLE' ? "primary" : "ghost danger"}`}
                  onClick={() => toggleRestriction(selectedSeatId)}
                >
                  {status === 'UNAVAILABLE' ? "Bỏ hạn chế" : "Hạn chế"}
                </button>
              </div>
            </div>
          );
        })()}
      </div>

      {toastMsg && (
        <div className="seatManage__toast">{toastMsg}</div>
      )}
    </div>
  );
};

export default KioskSeatManage;
