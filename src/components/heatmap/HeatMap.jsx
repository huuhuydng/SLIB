
import React from 'react';
import {
  LayoutGrid,
  LogOut,
  Thermometer,
  Armchair,
  User,
  AlertTriangle,
  MessageCircle,
  Layers,
  Bell,
  Search,
  ChevronDown,
  HelpCircle,
  ChevronLeft,
  Armchair as ChairIcon,
  MapPin
} from 'lucide-react';
import "../../styles/HeatMap.css";

// Mock Data
const MOCK_DATA = {
  occupancy: 69,
  zones: {
    quiet: 70,
    discuss: 30,
    self: 90
  }
};

const Heatmap = () => {
  return (
    <div className="slib-container">
      {/* --- SIDEBAR --- */}
      <aside className="sidebar">
        <div className="sidebar-header">
          <div className="logo">Slib<span className="logo-book-icon">📖</span></div>
        </div>

        <nav className="sidebar-nav">
          <NavItem icon={<LayoutGrid size={20} />} label="Tổng quan" />
          <NavItem icon={<LogOut size={20} />} label="Kiểm tra ra/vào" />
          <NavItem icon={<Thermometer size={20} />} label="Bản đồ nhiệt" active />
          <NavItem icon={<Armchair size={20} />} label="Quản lý chỗ ngồi" />
          <NavItem icon={<User size={20} />} label="Sinh viên" />
          <NavItem icon={<AlertTriangle size={20} />} label="Vi phạm" />
          <NavItem icon={<MessageCircle size={20} />} label="Trò chuyện" />
          <NavItem icon={<Layers size={20} />} label="Thống kê" />
          <NavItem icon={<Bell size={20} />} label="Thông báo" />
        </nav>

        <div className="sidebar-footer">
          <button className="help-btn">
            <HelpCircle size={24} color="white" />
          </button>
        </div>
      </aside>

      {/* --- MAIN CONTENT --- */}
      <main className="main-content">
        {/* Topbar */}
        <header className="topbar">
          <div className="back-btn">
            <ChevronLeft size={18} />
          </div>
          
          <div className="search-bar">
            <Search className="search-icon" size={18} color="#9ca3af" />
            <input 
              type="text" 
              placeholder="Search for anything..." 
            />
          </div>

          <div className="user-profile">
            <img src="https://i.pravatar.cc/150?u=a042581f4e29026024d" alt="Admin" className="avatar" />
            <div className="user-info">
              <span className="user-name">PhucNH</span>
              <span className="user-role">Librarian</span>
            </div>
            <ChevronDown size={16} className="user-dropdown-icon" color="#6b7280" />
          </div>
        </header>

        {/* Page Title */}
        <h1 className="page-title">Bản đồ nhiệt</h1>

        {/* Stats Row */}
        <section className="stats-row">
          {/* Card 1: Occupancy */}
          <div className="stat-card green">
            <div className="stat-icon">
              <ChairIcon size={24} />
            </div>
            <div className="stat-content">
              <h3>{MOCK_DATA.occupancy}%</h3>
              <p>Mức độ chiếm dụng hiện tại</p>
            </div>
          </div>

          {/* Card 2: Highest Usage */}
          <div className="stat-card red">
            <div className="stat-icon">
              <MapPin size={24} />
            </div>
            <div className="stat-content">
              <h4>Khu tự học</h4>
              <p>Đang được sử dụng nhiều</p>
            </div>
          </div>

          {/* Card 3: Lowest Usage */}
          <div className="stat-card mint">
            <div className="stat-icon">
              <MapPin size={24} />
            </div>
            <div className="stat-content">
              <h4>Khu thảo luận</h4>
              <p>Đang còn nhiều chỗ trống</p>
            </div>
          </div>
        </section>

        {/* Map Canvas */}
        <section className="map-panel">
          <div className="map-bg-grid"></div>
          
          <div className="map-layout">
            {/* Shelf (Left) */}
            <div className="map-block zone-shelf">
              <span>Kệ sách</span>
            </div>

            {/* Middle Section */}
            <div className="map-block zone-entrance">
              <span>Cửa ra vào</span>
            </div>
            <div className="map-block zone-hall">
              <span>Sảnh chính</span>
            </div>
            <div className="map-block zone-library">
              <span>Thủ thư</span>
            </div>

            {/* Right Section (Quiet) */}
            <div className="map-block zone-quiet">
              <span>Khu Yên Tĩnh</span>
              <div className="map-badge">{MOCK_DATA.zones.quiet}%</div>
            </div>

            {/* Bottom Sections */}
            <div className="map-block zone-discuss">
              <span>Khu Thảo Luận</span>
              <div className="map-badge">{MOCK_DATA.zones.discuss}%</div>
            </div>
            <div className="map-block zone-self">
              <span>Khu Tự Học</span>
              <div className="map-badge">{MOCK_DATA.zones.self}%</div>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
};

// --- Sub Components ---
const NavItem = ({ icon, label, active }) => (
  <div className={`nav-item ${active ? 'active' : ''}`}>
    <span className="nav-icon">{icon}</span>
    <span className="nav-label">{label}</span>
  </div>
);

export default Heatmap;
