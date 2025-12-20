import React, { useState, useMemo, useEffect, useRef } from "react";
import {
  ChevronLeft,
  Search,
  Bell,
  ChevronDown,
  Armchair,
  Sparkles,
  Check,
  Filter,
  CircleAlert,
  LayoutDashboard,
  ScanLine,
  Map,
  Users,
  AlertTriangle,
  MessageCircle,
  Layers,
  HelpCircle,
} from "lucide-react";
import "../../styles/SeatManage.css";

// --- MOCK DATA GENERATORS ---
const generateSeats = (prefix, start, count, zoneName) => {
  return Array.from({ length: count }, (_, i) => ({
    id: `${prefix}${i + 1}`,
    zone: zoneName,
  }));
};

const SEATS_A = generateSeats("A", 1, 15, "Khu tự học"); // 3x5
const SEATS_B = generateSeats("B", 1, 35, "Khu yên tĩnh"); // 5x7
const SEATS_C = generateSeats("C", 1, 14, "Khu thảo luận"); // 2x7

const ALL_SEATS = [...SEATS_A, ...SEATS_B, ...SEATS_C];
const TOTAL_SEATS = 64;

const TIME_SLOTS = [
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
  // --- STATE ---
  const [currentSlotIndex, setCurrentSlotIndex] = useState(1); // Default 09:00 - 11:00
  
  // occupancyMap: { [slotIndex]: [seatId1, seatId2, ...] }
  const [occupancyMap, setOccupancyMap] = useState(() => {
    const map = {};
    TIME_SLOTS.forEach((_, index) => {
      const occupied = [];
      // Randomly occupy some seats initially
      ALL_SEATS.forEach((seat) => {
        // Just random noise for demo
        if (Math.random() > 0.7) occupied.push(seat.id);
      });
      map[index] = occupied;
    });
    return map;
  });

  const [restrictedSeats, setRestrictedSeats] = useState(["B1", "A10", "C12"]);
  const [selectedSeatId, setSelectedSeatId] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [activeFilter, setActiveFilter] = useState("Tất cả khu vực");
  const [toastMsg, setToastMsg] = useState(null);

  // --- DERIVED STATE ---
  const currentSlotOccupied = useMemo(
    () => occupancyMap[currentSlotIndex] || [],
    [occupancyMap, currentSlotIndex]
  );

  const occupiedCount = currentSlotOccupied.length;
  const restrictedCount = restrictedSeats.length;

  // --- HANDLERS ---
  const showToast = (msg) => {
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 3000);
  };

  const handleSeatClick = (seat) => {
    const isRestricted = restrictedSeats.includes(seat.id);
    const isOccupied = currentSlotOccupied.includes(seat.id);

    // 1. Highlight/Select the seat
    setSelectedSeatId(seat.id);

    // 2. Interaction Logic
    if (isRestricted) {
      showToast(`Ghế ${seat.id} đang bị hạn chế!`);
      return;
    }

    // Toggle occupancy for current slot
    setOccupancyMap((prev) => {
      const currentList = prev[currentSlotIndex] || [];
      let newList;
      if (isOccupied) {
        newList = currentList.filter((id) => id !== seat.id);
      } else {
        newList = [...currentList, seat.id];
      }
      return { ...prev, [currentSlotIndex]: newList };
    });
  };

  const toggleRestriction = (seatId) => {
    if (!seatId) return;
    
    setRestrictedSeats((prev) => {
      const isRestricted = prev.includes(seatId);
      if (isRestricted) {
        // Remove restriction
        return prev.filter((id) => id !== seatId);
      } else {
        // Add restriction
        // Rule: If just restricted and currently occupied => remove occupancy immediately
        setOccupancyMap((occPrev) => {
            const currentList = occPrev[currentSlotIndex] || [];
            if (currentList.includes(seatId)) {
                return {
                    ...occPrev,
                    [currentSlotIndex]: currentList.filter(id => id !== seatId)
                };
            }
            return occPrev;
        });
        
        return [...prev, seatId];
      }
    });
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
        // In a real app, we might scroll to the seat here
      }
    } else {
        setSelectedSeatId(null);
    }
  };

  const handleFilterChange = (e) => {
      setActiveFilter(e.target.value);
  };

  // --- RENDER HELPERS ---
  const renderSeat = (seat) => {
    const isOccupied = currentSlotOccupied.includes(seat.id);
    const isRestricted = restrictedSeats.includes(seat.id);
    const isSelected = selectedSeatId === seat.id;
    
    // Filter logic: Dim if not in active zone
    let isDimmed = false;
    if (activeFilter !== "Tất cả khu vực" && seat.zone !== activeFilter) {
      isDimmed = true;
    }

    let statusClass = "seatManage__seat--empty";
    if (isRestricted) statusClass = "seatManage__seat--restricted";
    else if (isOccupied) statusClass = "seatManage__seat--occupied";

    return (
      <div
        key={seat.id}
        className={`seatManage__seat ${statusClass} ${
          isSelected ? "seatManage__seat--selected" : ""
        } ${isDimmed ? "seatManage__seat--dimmed" : ""}`}
        onClick={() => handleSeatClick(seat)}
      >
        {seat.id}
      </div>
    );
  };

  return (
    <div className="seatManage">
      {/* 1. SIDEBAR */}
      <aside className="seatManage__sidebar">
        <div className="seatManage__brand">
          <h1>Slib</h1>
          <div className="seatManage__brandIcon">📚</div>
        </div>
        <nav className="seatManage__nav">
          <div className="seatManage__navItem">
            <LayoutDashboard size={20} /> <span>Tổng quan</span>
          </div>
          <div className="seatManage__navItem">
            <ScanLine size={20} /> <span>Kiểm tra ra/vào</span>
          </div>
          <div className="seatManage__navItem">
            <Map size={20} /> <span>Heatmap</span>
          </div>
          <div className="seatManage__navItem seatManage__navItem--active">
            <Armchair size={20} /> <span>Quản lý chỗ ngồi</span>
          </div>
          <div className="seatManage__navItem">
            <Users size={20} /> <span>Sinh viên</span>
          </div>
          <div className="seatManage__navItem">
            <AlertTriangle size={20} /> <span>Vi phạm</span>
          </div>
          <div className="seatManage__navItem">
            <MessageCircle size={20} /> <span>Trò chuyện</span>
          </div>
          <div className="seatManage__navItem">
            <Layers size={20} /> <span>Thống kê</span>
          </div>
          <div className="seatManage__navItem">
            <Bell size={20} /> <span>Thông báo</span>
          </div>
        </nav>
        <div className="seatManage__navBottom">
            <div className="seatManage__navItem">
                 <HelpCircle size={20} />
            </div>
        </div>
      </aside>

      {/* 2. MAIN CONTENT */}
      <main className="seatManage__main">
        {/* TOPBAR */}
        <header className="seatManage__topbar">
          <button className="seatManage__backBtn">
            <ChevronLeft size={24} color="#555" />
          </button>
          
          <div className="seatManage__search">
            <Search size={18} className="seatManage__searchIcon" />
            <input
              type="text"
              placeholder="Search seat (e.g., A1, B12)..."
              value={searchTerm}
              onChange={handleSearch}
            />
          </div>
          
          <div className="seatManage__topbarRight">
            <div className="seatManage__bellWrapper">
              <Bell size={22} color="#555" />
              <span className="seatManage__badge">{occupiedCount}</span>
            </div>
            
            <div className="seatManage__profile">
              <div className="seatManage__avatar">
                <img src="https://picsum.photos/40/40" alt="Avatar" />
              </div>
              <div className="seatManage__profileInfo">
                <span className="seatManage__name">PhucNH</span>
                <span className="seatManage__role">Librarian</span>
              </div>
              <ChevronDown size={16} color="#888" />
            </div>
          </div>
        </header>

        {/* TITLE */}
        <h2 className="seatManage__title">Quản lý chỗ ngồi</h2>

        {/* TOP CARDS SECTION */}
        <section className="seatManage__topCards">
          
          {/* LEFT STACK: Occupied + Restricted */}
          <div className="seatManage__leftStack">
            
            {/* 1. Occupied Count Card */}
            <div className="seatManage__statCard">
              <div className="seatManage__statIconCircle">
                <Armchair size={28} color="#8dc63f" fill="#8dc63f" />
              </div>
              <div className="seatManage__statInfo">
                <div className="seatManage__statNumber">
                  {occupiedCount}/{TOTAL_SEATS}
                </div>
                <div className="seatManage__statLabel">
                  Chỗ ngồi đang được sử dụng
                </div>
              </div>
            </div>

            {/* 2. Restricted Seats Card */}
            <div className="seatManage__restrictedCard">
              <div className="seatManage__restrictedHeader">
                <span className="seatManage__restrictedTitle">
                  Ghế bị hạn chế
                </span>
                <span className="seatManage__restrictedCount">
                  {restrictedCount} ghế
                </span>
              </div>
              <div className="seatManage__chipList">
                {restrictedSeats.slice(0, 6).map((id) => (
                  <span key={id} className="seatManage__chip">
                    {id}
                  </span>
                ))}
                {restrictedSeats.length > 6 && (
                  <span className="seatManage__chipMore">
                    +{restrictedSeats.length - 6}
                  </span>
                )}
              </div>
            </div>
          </div>

          {/* RIGHT COLUMN: AI Analysis */}
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

        {/* CONTROLS (Slots + Filter) */}
        <section className="seatManage__controls">
          <div className="seatManage__slots">
            {TIME_SLOTS.map((slot, index) => (
              <button
                key={slot}
                className={`seatManage__slotBtn ${
                  index === currentSlotIndex ? "seatManage__slotBtn--active" : ""
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
                <ChevronDown size={14} className="seatManage__dropdownArrow"/>
            </div>
          </div>
        </section>

        {/* MAP PANEL */}
        <section className="seatManage__mapPanel">
          <div className="seatManage__mapGrid">
            
            {/* STATIC ELEMENTS */}
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
            
            <div className="seatManage__staticElement seatManage__pillar">
              {/* Pillar shape */}
            </div>

            {/* SEAT ZONES */}
            <div className="seatManage__zone seatManage__zoneB">
                {/* Zone B: 5 rows x 7 cols = 35 seats */}
                {SEATS_B.map(renderSeat)}
            </div>

            <div className="seatManage__zone seatManage__zoneA">
                {/* Zone A: 3 rows x 5 cols = 15 seats */}
                {SEATS_A.map(renderSeat)}
            </div>

            <div className="seatManage__zone seatManage__zoneC">
                {/* Zone C: 2 rows x 7 cols = 14 seats */}
                {SEATS_C.map(renderSeat)}
            </div>
          </div>

          {/* FLOATING ACTION CARD FOR SELECTED SEAT */}
          {selectedSeatId && (
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
                  className={`seatManage__actionBtn ${
                    restrictedSeats.includes(selectedSeatId)
                      ? "seatManage__actionBtn--unrestrict"
                      : "seatManage__actionBtn--restrict"
                  }`}
                  onClick={() => toggleRestriction(selectedSeatId)}
                >
                  {restrictedSeats.includes(selectedSeatId)
                    ? "Bỏ hạn chế"
                    : "Hạn chế"}
                </button>
              </div>
            </div>
          )}
        </section>

        {/* TOAST NOTIFICATION */}
        {toastMsg && <div className="seatManage__toast">{toastMsg}</div>}
      </main>
    </div>
  );
};

export default SeatManage;


