import React from 'react';
import {
    ChevronLeft,
    CheckCircle2
} from 'lucide-react';

import '../../styles/librarian/StudentDetail.css';

const StudentDetail = ({ student: studentProp, onBack }) => {
    const student = studentProp;
    const bookingHistory = Array.isArray(student?.history) ? student.history : [];

    if (!student) {
        return (
            <div className="content-body" style={{
                padding: '2rem',
                maxWidth: '1400px',
                margin: '0 auto',
                backgroundColor: '#f9fafb',
                minHeight: 'calc(100vh - 80px)'
            }}>
                <h2 className="page-title">Thông tin sinh viên</h2>
                <div className="table-card history-card">
                    <div style={{ padding: '2rem', textAlign: 'center', color: '#64748b' }}>
                        Không có dữ liệu sinh viên để hiển thị.
                    </div>
                </div>
            </div>
        );
    }

    const getRankInfo = (score) => {
        if (score >= 80) return { label: 'Gương mẫu', color: '#2ecc71', icon: true, desc: 'Tuyệt vời! Bạn đang giữ kỷ luật check-in rất tốt.' };
        if (score >= 65) return { label: 'Khá', color: '#f1c40f', icon: false, desc: 'Tốt! Hãy cố gắng duy trì.' };
        return { label: 'Trung bình', color: '#e74c3c', icon: false, desc: 'Cần chú ý kỷ luật hơn.' };
    }

    const rankInfo = getRankInfo(student.score);

    return (
        <>

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
                                    <span className="score-val" style={{ color: rankInfo.color }}>{student.score}</span>
                                    <span className="score-max">/100</span>
                                </div>
                            </div>
                        </div>
                        <div className="rank-details">
                            <h3 className="rank-title">
                                Hạng: {rankInfo.label}
                                {rankInfo.icon && <CheckCircle2 size={18} color={rankInfo.color} fill={rankInfo.color} className="icon-check-filled" />}
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
                                    {bookingHistory.length > 0 ? bookingHistory.map((item, idx) => (
                                        <tr key={idx} className="no-hover">
                                            <td className="font-medium">{item.date}</td>
                                            <td className="text-center">
                                                <span className="time-pill">{item.slot}</span>
                                            </td>
                                            <td className="text-center">
                                                <span className="seat-pill blue">{item.seat}</span>
                                            </td>
                                        </tr>
                                    )) : (
                                        <tr className="no-hover">
                                            <td colSpan="3" className="text-center" style={{ color: '#64748b', padding: '1.5rem' }}>
                                                Chưa có lịch sử đặt chỗ.
                                            </td>
                                        </tr>
                                    )}
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
