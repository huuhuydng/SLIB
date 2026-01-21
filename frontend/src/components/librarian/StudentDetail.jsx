import React from 'react';
import { 
  ChevronLeft,
  CheckCircle2
} from 'lucide-react';
import Header from '../shared/Header';
import '../../styles/librarian/StudentDetail.css';

// --- MOCK DATA ---
const MOCK_STUDENTS = [
    { id: 1, studentId: 'DE170706', name: 'Nguyễn Hoàng Phúc', email: 'phucnhde170706@fpt.edu.vn', score: 90, seat: 'A1', avatar: 'https://i.pravatar.cc/150?u=1' },
    { id: 2, studentId: 'DE170707', name: 'Trần Văn An', email: 'antv@fpt.edu.vn', score: 69, seat: 'B1', avatar: 'https://i.pravatar.cc/150?u=2' },
    { id: 3, studentId: 'DE170708', name: 'Lê Thị Bình', email: 'binhlt@fpt.edu.vn', score: 59, seat: 'C1', avatar: 'https://i.pravatar.cc/150?u=3' },
    { id: 4, studentId: 'DE170709', name: 'Phạm Minh Cường', email: 'cuongpm@fpt.edu.vn', score: 95, seat: 'A2', avatar: 'https://i.pravatar.cc/150?u=4' },
    { id: 5, studentId: 'DE170710', name: 'Đỗ Hải Đăng', email: 'dangdh@fpt.edu.vn', score: 75, seat: 'B2', avatar: 'https://i.pravatar.cc/150?u=5' },
    { id: 6, studentId: 'DE170711', name: 'Hoàng Thùy Linh', email: 'linhht@fpt.edu.vn', score: 45, seat: 'C2', avatar: 'https://i.pravatar.cc/150?u=6' },
    { id: 7, studentId: 'DE170712', name: 'Ngô Kiến Huy', email: 'huynk@fpt.edu.vn', score: 82, seat: 'A3', avatar: 'https://i.pravatar.cc/150?u=7' },
    { id: 8, studentId: 'DE170713', name: 'Sơn Tùng MTP', email: 'tungmtp@fpt.edu.vn', score: 68, seat: 'B3', avatar: 'https://i.pravatar.cc/150?u=8' },
    { id: 9, studentId: 'DE170714', name: 'Đen Vâu', email: 'denvau@fpt.edu.vn', score: 60, seat: 'C3', avatar: 'https://i.pravatar.cc/150?u=9' },
    { id: 10, studentId: 'DE170715', name: 'Bích Phương', email: 'phuongb@fpt.edu.vn', score: 88, seat: 'A4', avatar: 'https://i.pravatar.cc/150?u=10' },
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

const StudentDetail = ({ student: studentProp, onBack }) => {
    // Use the student from props, or fallback to first mock student
    const student = studentProp || MOCK_STUDENTS[0];

    const getRankInfo = (score) => {
        if(score >= 80) return { label: 'Gương mẫu', color: '#2ecc71', icon: true, desc: 'Tuyệt vời! Bạn đang giữ kỷ luật check-in rất tốt.' };
        if(score >= 65) return { label: 'Khá', color: '#f1c40f', icon: false, desc: 'Tốt! Hãy cố gắng duy trì.' };
        return { label: 'Trung bình', color: '#e74c3c', icon: false, desc: 'Cần chú ý kỷ luật hơn.' };
    }

    const rankInfo = getRankInfo(student.score);

    return (
        <>
            <Header 
                searchPlaceholder="Search for anything..."
                showBackButton={true}
                onBackClick={onBack}
            />

                <div className="content-body" style={{
                    padding: '2rem',
                    maxWidth: '1400px',
                    margin: '0 auto',
                    backgroundColor: '#f9fafb',
                    minHeight: 'calc(100vh - 80px)'
                }}>
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
            </>
        );
    };

export default StudentDetail;