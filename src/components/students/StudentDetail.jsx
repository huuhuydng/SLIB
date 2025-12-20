import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  ChevronLeft, 
  ChevronDown, 
  LayoutGrid, LogOut, Map, Armchair, User, AlertCircle, MessageCircle, Layers, Bell,
  CheckCircle2
} from 'lucide-react';
import '../../styles/StudentDetail.css';

// --- MOCK DATA ---
const MOCK_STUDENTS = [
    { id: 1, studentId: 'DE170706', name: 'Nguyễn Hoàng Phúc', email: 'phucnhde170706@fpt.edu.vn', score: 90, seat: 'A1', avatar: 'https://i.pravatar.cc/150?u=phuc' },
    // ... bạn có thể import data này từ file constant chung
];

const MOCK_HISTORY = [
    { date: '15/12/2025', slot: '07:00 - 09:00', seat: 'A1' },
    { date: '15/12/2025', slot: '07:00 - 09:00', seat: 'B1' },
    { date: '15/12/2025', slot: '07:00 - 09:00', seat: 'C1' },
    { date: '15/12/2025', slot: '07:00 - 09:00', seat: 'A2' },
    { date: '15/12/2025', slot: '07:00 - 09:00', seat: 'B2' },
    { date: '15/12/2025', slot: '07:00 - 09:00', seat: 'C2' },
    { date: '15/12/2025', slot: '07:00 - 09:00', seat: 'A3' },
    { date: '15/12/2025', slot: '07:00 - 09:00', seat: 'B3' },
];

const StudentDetail = () => {
    const { studentId } = useParams();
    const navigate = useNavigate();

    // Logic tìm sinh viên (Mock)
    const student = MOCK_STUDENTS.find(s => s.studentId === studentId) || MOCK_STUDENTS[0];

    const getRankInfo = (score) => {
        if(score >= 80) return { label: 'Gương mẫu', color: '#2ecc71', icon: true, desc: 'Tuyệt vời! Bạn đang giữ kỷ luật check-in rất tốt.' };
        if(score >= 65) return { label: 'Khá', color: '#f1c40f', icon: false, desc: 'Tốt! Hãy cố gắng duy trì.' };
        return { label: 'Trung bình', color: '#e74c3c', icon: false, desc: 'Cần chú ý kỷ luật hơn.' };
    }

    const rankInfo = getRankInfo(student.score);

    return (
        <div className="slib-container">
             {/* --- SIDEBAR (Giữ nguyên như trang trước) --- */}
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
                    <button className="btn-back" onClick={() => navigate('/students')}>
                        <ChevronLeft size={24} />
                    </button>
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

                <div className="content-body">
                    <h2 className="page-title">Thông tin sinh viên</h2>

                    {/* Info & Rank Row */}
                    <div className="info-grid">
                        {/* Info Card */}
                        <div className="info-card">
                            <img src={student.avatar} alt={student.name} className="info-avatar" />
                            <div className="info-text">
                                <p className="info-row"><span className="label">Tên:</span> <span className="val">{student.name}</span></p>
                                <p className="info-row"><span className="label">Mã sinh viên:</span> <span className="val">{student.studentId}</span></p>
                                <p className="info-row"><span className="label">Email:</span> <span className="val highlight">{student.email}</span></p>
                            </div>
                        </div>

                        {/* Rank Card */}
                        <div className="rank-card">
                            <div className="donut-chart-wrapper">
                                <div 
                                    className="donut-chart" 
                                    style={{
                                        background: `conic-gradient(${rankInfo.color} ${student.score}%, #f0f0f0 0)`
                                    }}
                                >
                                    <div className="inner-circle">
                                        <span className="score-val" style={{color: rankInfo.color}}>{student.score}</span>
                                        <span className="score-max">/100</span>
                                    </div>
                                </div>
                            </div>
                            <div className="rank-details">
                                <h3 className="rank-title">
                                    Hạng: {rankInfo.label} 
                                    {rankInfo.icon && <CheckCircle2 size={18} color={rankInfo.color} fill={rankInfo.color} className="icon-check-filled"/>}
                                </h3>
                                <p className="rank-desc">{rankInfo.desc}</p>
                            </div>
                        </div>
                    </div>

                    {/* History Table */}
                    <div className="history-section">
                        <h3 className="section-title">Lịch sử đặt chỗ</h3>
                        <div className="table-card history-card">
                            <div className="table-responsive">
                                <table className="student-table">
                                    <thead>
                                        <tr>
                                            <th>Ngày</th>
                                            <th className="text-center">Thời gian</th>
                                            <th className="text-center">Vị trí</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {MOCK_HISTORY.map((item, idx) => (
                                            <tr key={idx} className="no-hover">
                                                <td className="font-medium">{item.date}</td>
                                                <td className="text-center">
                                                    <span className="time-pill">{item.slot}</span>
                                                </td>
                                                <td className="text-center">
                                                    <span className="seat-pill blue">{item.seat}</span>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </main>
        </div>
    );
};

export default StudentDetail;