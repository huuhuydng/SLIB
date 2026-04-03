// ============================================
// FILE: SeatManage.jsx (Frontend)
// Thay thế toàn bộ file bằng code dưới đây
// ============================================

import React, { useState, useMemo, useEffect } from "react";
import {
  Armchair,
  Sparkles,
  Check,
  Filter,
  CircleAlert,
  ChevronDown,
} from "lucide-react";
import Header from "../../../components/shared/Header";
import { seatService } from '../../../services/seatService';
import "../../../styles/librarian/SeatManage.css";
import { handleLogout } from "../../../utils/auth";

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

const SeatManage = () => {
  const [currentSlotIndex, setCurrentSlotIndex] = useState(0);
  // ✅ Map: seatCode -> { status, seatId } (seatId từ database để gọi API restrict)
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

    console.log(`📅 Parsed slot ${slotIndex} (${slot}):`, { startTime, endTime });

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
      // ✅ Clear old state before loading new data
      setSeatStatusMap({});

      let response;
      if (slotIndex === 0) {
        // ✅ "Hiện tại" - xác định khung giờ hiện tại theo realtime
        const currentSlot = getCurrentTimeSlot();
        const today = new Date();
        const year = today.getFullYear();
        const month = String(today.getMonth() + 1).padStart(2, '0');
        const day = String(today.getDate()).padStart(2, '0');

        const startTime = `${year}-${month}-${day}T${currentSlot.start}:00`;
        const endTime = `${year}-${month}-${day}T${currentSlot.end}:00`;

        console.log('🔴 Loading CURRENT TIME SLOT:', { currentSlot, startTime, endTime });
        response = await seatService.getAllSeats({
          startTime,
          endTime
        });
      } else {
        const timeRange = parseTimeSlot(slotIndex);
        console.log('🟢 Loading TIME-RANGE data:', timeRange);
        response = await seatService.getAllSeats({
          startTime: timeRange.startTime,
          endTime: timeRange.endTime
        });
      }

      console.log(`=== DATA FOR SLOT ${slotIndex} (${TIME_SLOTS[slotIndex]}) ===`, response);

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

      // ✅ TẠO MAP: seat_code -> { status, seatId }
      const statusMap = {};
      seatsData.forEach(seat => {
        const seatCode = seat.seatCode || seat.seat_code || seat.code;
        const status = seat.seatStatus || seat.status; // Backend trả về "seatStatus"
        const seatId = seat.seatId || seat.seat_id || seat.id; // Lấy seatId từ database
        if (seatCode && status) {
          statusMap[seatCode] = { status, seatId };
        }
      });

      console.log('📊 Status Map:', statusMap);
      setSeatStatusMap(statusMap);

      // ✅ Tính toán stats
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
      console.error(`=== ERROR LOADING SLOT ${slotIndex} ===`, error);
      showToast('Lỗi tải dữ liệu khung giờ');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSeatDataForTimeSlot(0);

    const interval = setInterval(() => {
      if (currentSlotIndex === 0) {
        loadSeatDataForTimeSlot(0);
      }
    }, 30000);

    return () => clearInterval(interval);
  }, [currentSlotIndex]);

  useEffect(() => {
    loadSeatDataForTimeSlot(currentSlotIndex);
  }, [currentSlotIndex]);

  const showToast = (msg) => {
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 3000);
  };

  const handleSeatClick = (seat) => {
    const seatData = seatStatusMap[seat.id];
    const status = seatData?.status || 'AVAILABLE';

    if (status === 'BOOKED') {
      showToast(`Ghế ${seat.id} đã có người đặt!`);
      return;
    }

    if (status === 'UNAVAILABLE') {
      showToast(`Ghế ${seat.id} đang bị hạn chế!`);
    }

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
      console.error('Error toggling restriction:', error);
      showToast('Lỗi cập nhật hạn chế. Vui lòng thử lại.');
    }

    setSelectedSeatId(null);
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
    // ✅ Đọc status từ map
    const seatData = seatStatusMap[seat.id];
    const status = seatData?.status || 'AVAILABLE';
    const isSelected = selectedSeatId === seat.id;

    let isDimmed = false;
    if (activeFilter !== "Tất cả khu vực" && seat.zone !== activeFilter) {
      isDimmed = true;
    }

    // ✅ Xác định CSS class dựa vào status
    let statusClass = "seatManage__seat--empty"; // AVAILABLE → xanh dương
    if (status === 'UNAVAILABLE') {
      statusClass = "seatManage__seat--restricted"; // UNAVAILABLE → xám
    } else if (status === 'BOOKED') {
      statusClass = "seatManage__seat--occupied"; // BOOKED → cam
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
      <main style={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        backgroundColor: '#f9fafb',
        minHeight: '100vh',
        justifyContent: 'center',
        alignItems: 'center'
      }}>
        <div>Đang tải dữ liệu...</div>
      </main>
    );
  }

  return (
    <>
      <Header
        searchValue={searchTerm}
        onSearchChange={handleSearch}
        searchPlaceholder="Search for anything..."
        onLogout={handleLogout}
      />

      <div style={{
        padding: '2rem',
        maxWidth: '1400px',
        margin: '0 auto',
        backgroundColor: '#f9fafb',
        minHeight: 'calc(100vh - 80px)'
      }}>
        <h2 className="seatManage__title" style={{
          fontSize: '2rem',
          fontWeight: '700',
          color: '#1a1a1a',
          marginBottom: '1.5rem',
        }}>Quản lý chỗ ngồi</h2>

        <section className="seatManage__topCards" style={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: '1.5rem',
          marginBottom: '1.5rem',
        }}>
          <div className="seatManage__leftStack">
            <div className="seatManage__statCard">
              <div className="seatManage__statIconCircle">
                <Armchair size={28} color="#8dc63f" fill="#8dc63f" />
              </div>
              <div className="seatManage__statInfo">
                <div className="seatManage__statNumber">
                  {stats.bookedSeats}/{stats.totalSeats}
                </div>
                <div className="seatManage__statLabel">
                  Chỗ ngồi đang được sử dụng
                </div>
              </div>
            </div>

            <div className="seatManage__restrictedCard">
              <div className="seatManage__restrictedHeader">
                <span className="seatManage__restrictedTitle">
                  Ghế bị hạn chế
                </span>
                <span className="seatManage__restrictedCount">
                  {stats.unavailableSeats} ghế
                </span>
              </div>
              <div className="seatManage__chipList">
                {(() => {
                  const restrictedSeats = Object.keys(seatStatusMap)
                    .filter(code => seatStatusMap[code]?.status === 'UNAVAILABLE');
                  console.log('🔴 Restricted seats:', restrictedSeats, 'Total:', restrictedSeats.length);
                  return restrictedSeats
                    .slice(0, 6)
                    .map((id) => (
                      <span key={id} className="seatManage__chip">
                        {id}
                      </span>
                    ));
                })()}
                {stats.unavailableSeats > 6 && (
                  <span className="seatManage__chipMore">
                    +{stats.unavailableSeats - 6}
                  </span>
                )}
              </div>
            </div>
          </div>

          <div className="seatManage__aiCard">
            <div className="seatManage__aiHeader">
              <Sparkles size={18} className="seatManage__sparkle" />
              <span>AI phân tích</span>
            </div>
            <div className="seatManage__aiContent">
              <div className="seatManage__aiAlertIcon">
                <CircleAlert size={24} color="#ff7b00" />
              </div>
              <div>
                <div className="seatManage__aiAlertTitle">Cảnh báo đông đúc</div>
                <div className="seatManage__aiAlertDesc">
                  Khu yên tĩnh đã được lấp 95% khu vực. <br />
                  Hãy điều hướng sinh viên sang khu thảo luận.
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="seatManage__controls" style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '1.5rem',
          gap: '1rem',
          flexWrap: 'wrap',
        }}>
          <div className="seatManage__slots">
            {TIME_SLOTS.map((slot, index) => (
              <button
                key={slot}
                className={`seatManage__slotBtn ${index === currentSlotIndex ? "seatManage__slotBtn--active" : ""
                  }`}
                onClick={() => setCurrentSlotIndex(index)}
              >
                {index === currentSlotIndex && <Check size={16} />}
                {slot}
              </button>
            ))}
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
        </section>

        <section className="seatManage__mapPanel" style={{
          backgroundColor: '#fff',
          borderRadius: '12px',
          padding: '2rem',
          boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
          position: 'relative',
        }}>
          <div className="seatManage__mapGrid">
            <div className="seatManage__staticElement seatManage__bookshelf">
              Kệ<br />sách
            </div>
            <div className="seatManage__staticElement seatManage__entrance">
              Cửa ra vào
            </div>
            <div className="seatManage__staticElement seatManage__hall">
              Sảnh chính
            </div>
            <div className="seatManage__staticElement seatManage__librarian">
              Thủ thư
            </div>
            <div className="seatManage__staticElement seatManage__pillar"></div>

            <div className="seatManage__zone seatManage__zoneB">
              {SEATS_B.map(renderSeat)}
            </div>

            <div className="seatManage__zone seatManage__zoneA">
              {SEATS_A.map(renderSeat)}
            </div>

            <div className="seatManage__divider-wall"></div>

            <div className="seatManage__zone seatManage__zoneC">
              {SEATS_C.map(renderSeat)}
            </div>
          </div>

          {selectedSeatId && seatStatusMap[selectedSeatId]?.status !== 'BOOKED' && (
            <div className="seatManage__seatActionCard">
              <div className="seatManage__actionHeader">
                <Armchair size={20} />
                <span>{selectedSeatId}</span>
                <span className="seatManage__actionZone">
                  - {ALL_SEATS.find(s => s.id === selectedSeatId)?.zone}
                </span>
              </div>
              <div className="seatManage__actionBody">
                <button
                  className={`seatManage__actionBtn ${seatStatusMap[selectedSeatId]?.status === 'UNAVAILABLE'
                    ? "seatManage__actionBtn--unrestrict"
                    : "seatManage__actionBtn--restrict"
                    }`}
                  onClick={() => toggleRestriction(selectedSeatId)}
                >
                  {seatStatusMap[selectedSeatId]?.status === 'UNAVAILABLE'
                    ? "Bỏ hạn chế"
                    : "Hạn chế"}
                </button>
              </div>
            </div>
          )}
        </section>
      </div>

      {toastMsg && <div className="seatManage__toast" style={{
        position: 'fixed',
        bottom: '2rem',
        right: '2rem',
        backgroundColor: '#333',
        color: '#fff',
        padding: '0.75rem 1.25rem',
        borderRadius: '8px',
        boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
        zIndex: 1000,
      }}>{toastMsg}</div>}
    </>
  );
};

export default SeatManage;
