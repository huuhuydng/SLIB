import React, { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Users, 
  AlertTriangle, 
  Search, 
  Filter, 
  ChevronLeft, 
  ChevronDown,
  LayoutGrid,
  LogOut,
  Map,
  Armchair,
  User,
  AlertCircle,
  MessageCircle,
  Layers,
  Bell,
  Check
} from 'lucide-react';
import '../../styles/StudentsManage.css';

// --- MOCK DATA ---
const MOCK_STUDENTS = [
  { id: 1, studentId: 'DE170706', name: 'Nguyễn Hoàng Phúc', email: 'phucnhde170706@fpt.edu.vn', score: 90, seat: 'A1' },
  { id: 2, studentId: 'DE170707', name: 'Trần Văn An', email: 'antv@fpt.edu.vn', score: 69, seat: 'B1' },
  { id: 3, studentId: 'DE170708', name: 'Lê Thị Bình', email: 'binhlt@fpt.edu.vn', score: 59, seat: 'C1' },
  { id: 4, studentId: 'DE170709', name: 'Phạm Minh Cường', email: 'cuongpm@fpt.edu.vn', score: 95, seat: 'A2' },
  { id: 5, studentId: 'DE170710', name: 'Đỗ Hải Đăng', email: 'dangdh@fpt.edu.vn', score: 75, seat: 'B2' },
  { id: 6, studentId: 'DE170711', name: 'Hoàng Thùy Linh', email: 'linhht@fpt.edu.vn', score: 45, seat: 'C2' },
  { id: 7, studentId: 'DE170712', name: 'Ngô Kiến Huy', email: 'huynk@fpt.edu.vn', score: 82, seat: 'A3' },
  { id: 8, studentId: 'DE170713', name: 'Sơn Tùng MTP', email: 'tungmtp@fpt.edu.vn', score: 68, seat: 'B3' },
  { id: 9, studentId: 'DE170714', name: 'Đen Vâu', email: 'denvau@fpt.edu.vn', score: 60, seat: 'C3' },
  { id: 10, studentId: 'DE170715', name: 'Bích Phương', email: 'phuongb@fpt.edu.vn', score: 88, seat: 'A4' },
];

const TIME_SLOTS = [
  "07:00 - 09:00",
  "09:00 - 11:00",
  "11:00 - 13:00",
  "13:00 - 15:00",
  "15:00 - 17:00"
];

const StudentsManage = () => {
  const navigate = useNavigate();
  const [activeSlot, setActiveSlot] = useState(1); // Index of "09:00 - 11:00"
  const [globalSearch, setGlobalSearch] = useState('');
  const [tableSearch, setTableSearch] = useState('');
  const [filterRank, setFilterRank] = useState('all'); // all, good, average, bad
  const [isFilterOpen, setIsFilterOpen] = useState(false);

  // --- LOGIC ---
  const getRank = (score) => {
    if (score >= 80) return 'good'; // Gương mẫu
    if (score >= 65) return 'average'; // Khá
    return 'bad'; // Trung bình (kém)
  };

  const filteredStudents = useMemo(() => {
    return MOCK_STUDENTS.filter(s => {
      const matchGlobal = s.name.toLowerCase().includes(globalSearch.toLowerCase()) || 
                          s.studentId.toLowerCase().includes(globalSearch.toLowerCase());
      const matchTable = s.studentId.toLowerCase().includes(tableSearch.toLowerCase());
      
      let matchFilter = true;
      const rank = getRank(s.score);
      if (filterRank !== 'all') {
        matchFilter = rank === filterRank;
      }

      return matchGlobal && matchTable && matchFilter;
    });
  }, [globalSearch, tableSearch, filterRank]);

  const handleRowClick = (studentId) => {
    navigate(`/students/${studentId}`);
  };

  return (
    <div className="slib-container">
      {/* --- SIDEBAR --- */}
      <aside className="slib-sidebar">
        <div className="sidebar-logo">
          <h1>Slib<span className="logo-dot">.</span></h1>
        </div>
        <nav className="sidebar-menu">
          <div className="menu-item"><LayoutGrid size={20} /> Tổng quan</div>
          <div className="menu-item"><LogOut size={20} /> Kiểm tra ra/vào</div>
          <div className="menu-item"><Map size={20} /> Bản đồ nhiệt</div>
          <div className="menu-item"><Armchair size={20} /> Quản lý chỗ ngồi</div>
          <div className="menu-item active"><User size={20} /> Sinh viên</div>
          <div className="menu-item"><AlertCircle size={20} /> Vi phạm</div>
          <div className="menu-item"><MessageCircle size={20} /> Trò chuyện</div>
          <div className="menu-item"><Layers size={20} /> Thống kê</div>
          <div className="menu-item"><Bell size={20} /> Thông báo</div>
        </nav>
        <div className="sidebar-help">
          <div className="help-icon">?</div>
        </div>
      </aside>

      {/* --- MAIN CONTENT --- */}
      <main className="slib-main">
        {/* Topbar */}
        <header className="slib-topbar">
          <button className="btn-back"><ChevronLeft size={24} /></button>
          <div className="search-bar">
            <input 
              type="text" 
              placeholder="Search for anything..." 
              value={globalSearch}
              onChange={(e) => setGlobalSearch(e.target.value)}
            />
          </div>
          <div className="profile-pill">
            <img src="https://i.pravatar.cc/150?u=phuc" alt="Avatar" className="avatar" />
            <div className="profile-info">
              <span className="name">PhucNH</span>
              <span className="role">Librarian</span>
            </div>
            <ChevronDown size={16} />
          </div>
        </header>

        {/* Content Body */}
        <div className="content-body">
          <h2 className="page-title">Quản lý sinh viên</h2>

          {/* Stats Cards */}
          <div className="stats-container">
            <div className="stat-card">
              <div className="stat-icon purple">
                <Users size={24} />
              </div>
              <div className="stat-info">
                <h3>69</h3>
                <p>Đang trong thư viện</p>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon red">
                <AlertTriangle size={24} />
              </div>
              <div className="stat-info">
                <h3>18</h3>
                <p>Điểm đánh giá dưới 65%</p>
              </div>
            </div>
          </div>

          {/* Time Slots */}
          <div className="time-slots">
            {TIME_SLOTS.map((slot, index) => (
              <button 
                key={index} 
                className={`slot-pill ${activeSlot === index ? 'active' : ''}`}
                onClick={() => setActiveSlot(index)}
              >
                {activeSlot === index && <Check size={16} className="slot-check" />}
                {slot}
              </button>
            ))}
          </div>

          {/* Table Section */}
          <div className="table-card">
            <div className="table-header">
              <h3>Danh sách sinh viên đang có mặt tại thư viện</h3>
              <div className="table-actions">
                <div className="mini-search">
                  <input 
                    type="text" 
                    placeholder="Tìm kiếm mã số sinh viên" 
                    value={tableSearch}
                    onChange={(e) => setTableSearch(e.target.value)}
                  />
                </div>
                
                <div className="filter-dropdown-wrapper">
                  <button className="btn-icon" onClick={() => setIsFilterOpen(!isFilterOpen)}>
                    <Filter size={20} />
                  </button>
                  {isFilterOpen && (
                    <div className="dropdown-menu">
                      <div className="dropdown-header">Điểm đánh giá</div>
                      <div className="dropdown-item" onClick={() => { setFilterRank('all'); setIsFilterOpen(false); }}>Tất cả</div>
                      <div className="dropdown-item" onClick={() => { setFilterRank('good'); setIsFilterOpen(false); }}>
                        <span className="dot green"></span> Gương mẫu
                      </div>
                      <div className="dropdown-item" onClick={() => { setFilterRank('average'); setIsFilterOpen(false); }}>
                        <span className="dot yellow"></span> Khá
                      </div>
                      <div className="dropdown-item" onClick={() => { setFilterRank('bad'); setIsFilterOpen(false); }}>
                        <span className="dot red"></span> Trung bình
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="table-responsive">
              <table className="student-table">
                <thead>
                  <tr>
                    <th>Tên sinh viên</th>
                    <th>Mã số sinh viên</th>
                    <th className="text-center">Điểm đánh giá</th>
                    <th className="text-center">Vị trí</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredStudents.map((student) => {
                    const rank = getRank(student.score);
                    return (
                      <tr key={student.id} onClick={() => handleRowClick(student.studentId)}>
                        <td className="font-medium">{student.name}</td>
                        <td>{student.studentId}</td>
                        <td className="text-center">
                          <span className={`score-badge ${rank}`}>
                            {student.score}
                          </span>
                        </td>
                        <td className="text-center">
                          <span className="seat-pill">{student.seat}</span>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
              {filteredStudents.length === 0 && (
                <div className="empty-state">Không tìm thấy sinh viên nào.</div>
              )}
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default StudentsManage;