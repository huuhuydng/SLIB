
import React, { useState, useMemo } from 'react';
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
  Filter,
  LogIn,
  Users,
  HelpCircle,
  ChevronLeft,
  Check
} from 'lucide-react';
import "../../styles/CheckInOut.css";

// --- MOCK DATA ---
const MOCK_LOGS = [
  { id: 1, name: 'Nguyễn Hoàng Phúc', code: 'DE170706', action: 'Check in', zone: 'Khu yên tĩnh', time: '12:21:10 15/12/2025' },
  { id: 2, name: 'Trần Thị Mai', code: 'DE170112', action: 'Check out', zone: 'Khu thảo luận', time: '12:20:05 15/12/2025' },
  { id: 3, name: 'Lê Văn Nam', code: 'SE160554', action: 'Check out', zone: 'Khu tự học', time: '12:18:30 15/12/2025' },
  { id: 4, name: 'Phạm Minh Khoa', code: 'DE180001', action: 'Check out', zone: 'Khu thảo luận', time: '12:15:12 15/12/2025' },
  { id: 5, name: 'Đỗ Thảo Vy', code: 'GD150223', action: 'Check out', zone: 'Khu tự học', time: '12:10:45 15/12/2025' },
  { id: 6, name: 'Nguyễn Hoàng Phúc', code: 'DE170706', action: 'Check in', zone: 'Khu yên tĩnh', time: '11:55:10 15/12/2025' },
  { id: 7, name: 'Vũ Thanh Tùng', code: 'SE171111', action: 'Check out', zone: 'Khu thảo luận', time: '11:50:22 15/12/2025' },
  { id: 8, name: 'Hoàng Yến Nhi', code: 'MC170333', action: 'Check out', zone: 'Khu tự học', time: '11:45:15 15/12/2025' },
  { id: 9, name: 'Ngô Kiến Huy', code: 'DE170888', action: 'Check out', zone: 'Khu thảo luận', time: '11:40:00 15/12/2025' },
  { id: 10, name: 'Bùi Anh Tuấn', code: 'SE160999', action: 'Check out', zone: 'Khu tự học', time: '11:35:55 15/12/2025' },
  { id: 11, name: 'Lâm Vỹ Dạ', code: 'DE170706', action: 'Check out', zone: 'Khu thảo luận', time: '11:30:10 15/12/2025' },
  { id: 12, name: 'Trấn Thành', code: 'MC170123', action: 'Check out', zone: 'Khu tự học', time: '11:25:40 15/12/2025' },
  { id: 13, name: 'Trường Giang', code: 'SE150000', action: 'Check in', zone: 'Khu yên tĩnh', time: '11:20:10 15/12/2025' },
  { id: 14, name: 'Ninh Dương L.N', code: 'DE170456', action: 'Check out', zone: 'Khu thảo luận', time: '11:15:12 15/12/2025' },
  { id: 15, name: 'Thúy Ngân', code: 'GD170789', action: 'Check out', zone: 'Khu tự học', time: '11:10:05 15/12/2025' },
];

const CheckInOut = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  // filterSort: '' | 'zone' | 'action'
  const [filterSort, setFilterSort] = useState(''); 

  const displayedLogs = useMemo(() => {
    let data = MOCK_LOGS.filter(log => 
      log.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
      log.code.toLowerCase().includes(searchTerm.toLowerCase())
    );

    // Simple sorting logic based on the filter selection
    if (filterSort === 'zone') {
      data = [...data].sort((a, b) => a.zone.localeCompare(b.zone));
    } else if (filterSort === 'action') {
      data = [...data].sort((a, b) => a.action.localeCompare(b.action));
    }
    
    return data;
  }, [searchTerm, filterSort]);

  const handleFilterClick = (type) => {
    // Toggle if clicking the same one, otherwise set new
    setFilterSort(prev => prev === type ? '' : type);
    setIsFilterOpen(false);
  };

  const getZoneClass = (zoneName) => {
    const lower = zoneName.toLowerCase();
    if (lower.includes('yên tĩnh')) return 'quiet';
    if (lower.includes('thảo luận')) return 'discuss';
    if (lower.includes('tự học')) return 'self-study';
    return '';
  };

  return (
    <div className="slib-container">
      {/* --- SIDEBAR --- */}
      <aside className="sidebar">
        <div className="sidebar-header">
          <div className="logo">Slib<span className="logo-book-icon">📖</span></div>
        </div>

        <nav className="sidebar-nav">
          <NavItem icon={<LayoutGrid size={20} />} label="Tổng quan" />
          <NavItem icon={<LogOut size={20} />} label="Kiểm tra ra/vào" active />
          <NavItem icon={<Thermometer size={20} />} label="Bản đồ nhiệt" />
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
          <div className="toggle-sidebar-btn">
            <div className="circle-btn"><ChevronLeft size={18} /></div>
          </div>
          
          <div className="search-bar">
            <Search className="search-icon" size={18} />
            <input 
              type="text" 
              placeholder="Search for anything..." 
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          <div className="user-profile">
            <img src="https://i.pravatar.cc/150?u=a042581f4e29026024d" alt="Admin" className="avatar" />
            <div className="user-info">
              <span className="user-name">PhucNH</span>
              <span className="user-role">Librarian</span>
            </div>
            <ChevronDown size={16} className="user-dropdown-icon" />
          </div>
        </header>

        {/* Page Title */}
        <h1 className="page-title">Kiểm tra ra/vào</h1>

        {/* Stats Cards */}
        <section className="stats-row">
          <StatCard 
            number="78" 
            label="Đã check in hôm nay" 
            icon={<LogIn size={28} color="#6366f1" />} 
            iconBg="#e0e7ff" 
          />
          <StatCard 
            number="60" 
            label="Đã check out hôm nay" 
            icon={<LogOut size={28} color="#ec4899" />} 
            iconBg="#fce7f3" 
          />
          <StatCard 
            number="18" 
            label="Đang trong thư viện" 
            icon={<Users size={28} color="#8b5cf6" />} 
            iconBg="#ede9fe" 
          />
        </section>

        {/* Table Panel */}
        <section className="table-panel">
          <div className="panel-header">
            <h2>Danh sách sinh viên ra vào</h2>
            <div className="filter-container">
              <button className="filter-btn" onClick={() => setIsFilterOpen(!isFilterOpen)}>
                <Filter size={18} />
              </button>
              {isFilterOpen && (
                <div className="filter-dropdown">
                  <div 
                    className={`filter-item ${filterSort === 'zone' ? 'active' : ''}`} 
                    onClick={() => handleFilterClick('zone')}
                  >
                    Khu vực
                    {filterSort === 'zone' && <Check size={14} />}
                  </div>
                  <div 
                    className={`filter-item ${filterSort === 'action' ? 'active' : ''}`} 
                    onClick={() => handleFilterClick('action')}
                  >
                    Hành động
                    {filterSort === 'action' && <Check size={14} />}
                  </div>
                </div>
              )}
            </div>
          </div>

          <div className="table-wrapper">
            <table className="log-table">
              <thead>
                <tr>
                  <th>Tên sinh viên</th>
                  <th>Mã số sinh viên</th>
                  <th>Hành động</th>
                  <th>Khu vực</th>
                  <th>Thời gian</th>
                </tr>
              </thead>
              <tbody>
                {displayedLogs.map((log) => (
                  <tr key={log.id}>
                    <td className="fw-500">{log.name}</td>
                    <td className="code-cell">{log.code}</td>
                    <td>
                      <span className={`badge action ${log.action === 'Check in' ? 'in' : 'out'}`}>
                        {log.action}
                      </span>
                    </td>
                    <td>
                      <span className={`badge zone ${getZoneClass(log.zone)}`}>
                        {log.zone}
                      </span>
                    </td>
                    <td className="time-cell">{log.time}</td>
                  </tr>
                ))}
              </tbody>
            </table>
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

const StatCard = ({ number, label, icon, iconBg }) => (
  <div className="stat-card">
    <div className="stat-icon-wrapper" style={{ backgroundColor: iconBg }}>
      {icon}
    </div>
    <div className="stat-info">
      <div className="stat-number">{number}</div>
      <div className="stat-label">{label}</div>
    </div>
  </div>
);

export default CheckInOut;
