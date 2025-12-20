import React, { useState, useMemo } from 'react';
import { 
  LayoutGrid, LogOut, Map, Armchair, User, AlertCircle, MessageCircle, Layers, Bell,
  ChevronLeft, Search, ChevronDown, Filter, Users, ThumbsUp, AlertTriangle, CheckCircle2
} from 'lucide-react';
import '../../styles/ViolationManage.css';

// --- MOCK DATA ---
const INITIAL_STUDENTS = [
  { id: 1, studentId: 'DE170706', name: 'Nguyễn Hoàng Phúc', email: 'phucnhde170706@fpt.edu.vn', score: 90, avatar: 'https://i.pravatar.cc/150?u=1' },
  { id: 2, studentId: 'DE170707', name: 'Trần Văn An', email: 'antv@fpt.edu.vn', score: 69, avatar: 'https://i.pravatar.cc/150?u=2' },
  { id: 3, studentId: 'DE170708', name: 'Lê Thị Bình', email: 'binhlt@fpt.edu.vn', score: 59, avatar: 'https://i.pravatar.cc/150?u=3' },
  { id: 4, studentId: 'DE170709', name: 'Phạm Minh Cường', email: 'cuongpm@fpt.edu.vn', score: 90, avatar: 'https://i.pravatar.cc/150?u=4' },
  { id: 5, studentId: 'DE170710', name: 'Đỗ Hải Đăng', email: 'dangdh@fpt.edu.vn', score: 69, avatar: 'https://i.pravatar.cc/150?u=5' },
  { id: 6, studentId: 'DE170711', name: 'Hoàng Thùy Linh', email: 'linhht@fpt.edu.vn', score: 59, avatar: 'https://i.pravatar.cc/150?u=6' },
  { id: 7, studentId: 'DE170712', name: 'Ngô Kiến Huy', email: 'huynk@fpt.edu.vn', score: 90, avatar: 'https://i.pravatar.cc/150?u=7' },
  { id: 8, studentId: 'DE170713', name: 'Sơn Tùng MTP', email: 'tungmtp@fpt.edu.vn', score: 69, avatar: 'https://i.pravatar.cc/150?u=8' },
  { id: 9, studentId: 'DE170714', name: 'Đen Vâu', email: 'denvau@fpt.edu.vn', score: 59, avatar: 'https://i.pravatar.cc/150?u=9' },
  { id: 10, studentId: 'DE170715', name: 'Bích Phương', email: 'phuongb@fpt.edu.vn', score: 90, avatar: 'https://i.pravatar.cc/150?u=10' },
  { id: 11, studentId: 'DE170716', name: 'Tóc Tiên', email: 'tien@fpt.edu.vn', score: 69, avatar: 'https://i.pravatar.cc/150?u=11' },
  { id: 12, studentId: 'DE170717', name: 'Mỹ Tâm', email: 'tam@fpt.edu.vn', score: 59, avatar: 'https://i.pravatar.cc/150?u=12' },
];

const MOCK_VIOLATIONS = [
  { id: 1, date: '15/12/2025', reason: 'Gây mất trật tự', minus: 90, level: 'bad' },
  { id: 2, date: '15/12/2025', reason: 'Sử dụng thức ăn trong thư viện', minus: 69, level: 'average' },
  { id: 3, date: '15/12/2025', reason: 'Gây mất trật tự', minus: 59, level: 'bad' },
  { id: 4, date: '15/12/2025', reason: 'Sử dụng thức ăn trong thư viện', minus: 90, level: 'good' },
  { id: 5, date: '15/12/2025', reason: 'Gây mất trật tự', minus: 69, level: 'average' },
  { id: 6, date: '15/12/2025', reason: 'Sử dụng thức ăn trong thư viện', minus: 59, level: 'bad' },
  { id: 7, date: '15/12/2025', reason: 'Gây mất trật tự', minus: 59, level: 'bad' },
  { id: 8, date: '15/12/2025', reason: 'Sử dụng thức ăn trong thư viện', minus: 69, level: 'average' },
  { id: 9, date: '15/12/2025', reason: 'Gây mất trật tự', minus: 69, level: 'average' },
  { id: 10, date: '15/12/2025', reason: 'Sử dụng thức ăn trong thư viện', minus: 59, level: 'bad' },
];

const ViolationManage = () => {
  // State
  const [currentView, setCurrentView] = useState('list'); // 'list' | 'detail'
  const [students, setStudents] = useState(INITIAL_STUDENTS);
  const [selectedStudentId, setSelectedStudentId] = useState(null);
  
  // Search & Filter State (List View)
  const [searchQuery, setSearchQuery] = useState('');
  const [filterRank, setFilterRank] = useState('all'); // 'all', 'good', 'average', 'bad'
  const [isFilterOpen, setIsFilterOpen] = useState(false);

  // Edit State (Detail View)
  const [newScore, setNewScore] = useState('');
  const [editReason, setEditReason] = useState('');

  // --- LOGIC HELPER ---
  const getRankInfo = (score) => {
    if (score >= 85) return { key: 'good', label: 'Gương mẫu', color: 'var(--green)', class: 'good' };
    if (score >= 65) return { key: 'average', label: 'Khá', color: 'var(--yellow)', class: 'average' };
    return { key: 'bad', label: 'Trung bình', color: 'var(--red)', class: 'bad' };
  };

  const selectedStudent = useMemo(() => {
    return students.find(s => s.studentId === selectedStudentId);
  }, [students, selectedStudentId]);

  const filteredStudents = useMemo(() => {
    return students.filter(s => {
      const matchSearch = s.studentId.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          s.name.toLowerCase().includes(searchQuery.toLowerCase());
      
      let matchFilter = true;
      const rank = getRankInfo(s.score);
      if (filterRank !== 'all') {
        matchFilter = rank.key === filterRank;
      }
      return matchSearch && matchFilter;
    });
  }, [students, searchQuery, filterRank]);

  // --- HANDLERS ---
  const handleRowClick = (studentId) => {
    setSelectedStudentId(studentId);
    // Reset form
    const st = students.find(s => s.studentId === studentId);
    setNewScore(st ? st.score : '');
    setEditReason('');
    setCurrentView('detail');
  };

  const handleBack = () => {
    setCurrentView('list');
    setSelectedStudentId(null);
  };

  const handleUpdateScore = () => {
    if (!selectedStudentId) return;
    const scoreNum = parseInt(newScore, 10);
    if (isNaN(scoreNum) || scoreNum < 0 || scoreNum > 100) {
      alert("Vui lòng nhập điểm hợp lệ (0-100)");
      return;
    }

    setStudents(prev => prev.map(s => {
      if (s.studentId === selectedStudentId) {
        return { ...s, score: scoreNum };
      }
      return s;
    }));

    alert("Đã cập nhật điểm thành công!");
    // Keep user on detail view to see change
  };

  // --- RENDER COMPONENTS ---

  const renderSidebar = () => (
    <aside className="slib-sidebar">
      <div className="sidebar-logo">
        <h1>Slib<span className="logo-dot">.</span></h1>
      </div>
      <nav className="sidebar-menu">
        <div className="menu-item"><LayoutGrid size={20} /> Tổng quan</div>
        <div className="menu-item"><LogOut size={20} /> Kiểm tra ra/vào</div>
        <div className="menu-item"><Map size={20} /> Bản đồ nhiệt</div>
        <div className="menu-item"><Armchair size={20} /> Quản lý chỗ ngồi</div>
        <div className="menu-item"><User size={20} /> Sinh viên</div>
        <div className="menu-item active"><AlertCircle size={20} /> Vi phạm</div>
        <div className="menu-item"><MessageCircle size={20} /> Trò chuyện</div>
        <div className="menu-item"><Layers size={20} /> Thống kê</div>
        <div className="menu-item"><Bell size={20} /> Thông báo</div>
      </nav>
      <div className="sidebar-help">
        <div className="help-icon">?</div>
      </div>
    </aside>
  );

  const renderTopbar = () => (
    <header className="slib-topbar">
      {currentView === 'detail' ? (
        <button className="btn-back" onClick={handleBack}>
          <ChevronLeft size={24} />
        </button>
      ) : (
        <div className="btn-back-placeholder">
           <button className="btn-back"><ChevronLeft size={24} /></button>
        </div>
      )}
      <div className="search-bar">
        <input type="text" placeholder="Search for anything..." />
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
  );

  const renderListView = () => (
    <>
      <div className="stats-row">
        <div className="stat-card">
          <div className="stat-icon green"><CheckCircle2 size={24} /></div>
          <div className="stat-info">
            <h3>69</h3>
            <p>sinh viên</p>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon yellow"><ThumbsUp size={24} /></div>
          <div className="stat-info">
            <h3>18</h3>
            <p>Điểm đánh giá dưới 65%</p>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon red"><AlertTriangle size={24} /></div>
          <div className="stat-info">
            <h3>18</h3>
            <p>Điểm đánh giá dưới 65%</p>
          </div>
        </div>
      </div>

      <div className="list-panel">
        <div className="panel-header">
          <h3>Danh sách điểm đánh giá sinh viên</h3>
          <div className="panel-actions">
            <div className="pill-search">
              <input 
                type="text" 
                placeholder="Tìm kiếm mã số sinh viên"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
            <div className="filter-wrapper">
              <button className="btn-filter" onClick={() => setIsFilterOpen(!isFilterOpen)}>
                <Filter size={20} />
              </button>
              {isFilterOpen && (
                <div className="filter-dropdown">
                  <div className="filter-title">Điểm đánh giá</div>
                  <div className="filter-option" onClick={() => { setFilterRank('all'); setIsFilterOpen(false); }}>
                     Tất cả
                  </div>
                  <div className="filter-option" onClick={() => { setFilterRank('good'); setIsFilterOpen(false); }}>
                    <span className="dot green"></span> Gương mẫu
                  </div>
                  <div className="filter-option" onClick={() => { setFilterRank('average'); setIsFilterOpen(false); }}>
                    <span className="dot yellow"></span> Khá
                  </div>
                  <div className="filter-option" onClick={() => { setFilterRank('bad'); setIsFilterOpen(false); }}>
                    <span className="dot red"></span> Trung bình
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="table-wrapper">
          <table className="violation-table">
            <thead>
              <tr>
                <th>Tên sinh viên</th>
                <th>Mã số sinh viên</th>
                <th className="text-center">Điểm đánh giá</th>
              </tr>
            </thead>
            <tbody>
              {filteredStudents.map(student => {
                const rank = getRankInfo(student.score);
                return (
                  <tr key={student.id} onClick={() => handleRowClick(student.studentId)}>
                    <td className="font-medium">{student.name}</td>
                    <td>{student.studentId}</td>
                    <td className="text-center">
                      <span className={`score-badge ${rank.class}`}>{student.score}</span>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );

  const renderDetailView = () => {
    if (!selectedStudent) return null;
    const rank = getRankInfo(selectedStudent.score);

    return (
      <>
        {/* Info & Rank Row */}
        <div className="detail-top-grid">
          <div className="info-card">
            <img src={selectedStudent.avatar} alt="Avatar" className="info-avatar" />
            <div className="info-content">
              <p className="info-title">Tên: <span className="highlight">{selectedStudent.name}</span></p>
              <p>Mã sinh viên: {selectedStudent.studentId}</p>
              <p>Email: <span className="email-text">{selectedStudent.email}</span></p>
            </div>
          </div>

          <div className="rank-card">
            <div className="donut-wrapper">
              <div 
                className="donut-chart"
                style={{
                  background: `conic-gradient(${rank.color} ${selectedStudent.score}%, #eee 0)`
                }}
              >
                <div className="donut-inner">
                  <span className="donut-score" style={{ color: rank.color }}>{selectedStudent.score}</span>
                  <span className="donut-total">/100</span>
                </div>
              </div>
            </div>
            <div className="rank-text">
              <h4>Hạng: {rank.label} {rank.key === 'good' && <CheckCircle2 size={18} className="icon-check" />}</h4>
            </div>
          </div>
        </div>

        {/* Bottom Split */}
        <div className="detail-bottom-grid">
          {/* Left: Violation List */}
          <div className="history-panel">
            <div className="panel-header-simple">
              <h3>Danh sách vi phạm của sinh viên</h3>
              <Filter size={20} color="#888" />
            </div>
            <div className="table-wrapper">
               <table className="violation-table">
                 <thead>
                   <tr>
                     <th>Thời gian</th>
                     <th>Lỗi vi phạm</th>
                     <th className="text-center">Số điểm trừ</th>
                   </tr>
                 </thead>
                 <tbody>
                    {MOCK_VIOLATIONS.map((v, i) => {
                       const badgeClass = v.minus >= 85 ? 'good' : (v.minus >= 65 ? 'average' : 'bad');
                       return (
                         <tr key={i} className="no-cursor">
                           <td>{v.date}</td>
                           <td>{v.reason}</td>
                           <td className="text-center">
                             <span className={`score-badge ${badgeClass}`}>{v.minus}</span>
                           </td>
                         </tr>
                       )
                    })}
                 </tbody>
               </table>
            </div>
          </div>

          {/* Right: Edit Form */}
          <div className="edit-panel">
            <h3>Chỉnh sửa điểm đánh giá sinh viên</h3>
            
            <div className="form-group">
              <label>Số điểm muốn thay đổi:</label>
              <div className="input-with-btn">
                <input 
                  type="number" 
                  value={newScore}
                  onChange={(e) => setNewScore(e.target.value)}
                  className="score-input"
                />
                <button className="btn-confirm" onClick={handleUpdateScore}>Xác nhận</button>
              </div>
            </div>

            <div className="form-group">
              <label>Lí do thay đổi: Sinh viên đã chấp hành tốt nội quy của thư viện, trên hết còn đẹp trai.</label>
              <textarea 
                rows="10" 
                className="reason-textarea"
                value={editReason}
                onChange={(e) => setEditReason(e.target.value)}
              ></textarea>
            </div>
          </div>
        </div>
      </>
    );
  };

  return (
    <div className="slib-container">
      {renderSidebar()}
      
      <main className="slib-main">
        {renderTopbar()}
        
        <div className="content-body">
          <h2 className="page-title">Quản lý vi phạm</h2>
          {currentView === 'list' ? renderListView() : renderDetailView()}
        </div>
      </main>
    </div>
  );
};

export default ViolationManage;
