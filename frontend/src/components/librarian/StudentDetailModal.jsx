import React, { useState, useEffect } from 'react';
import {
    X, User, Mail, Shield, Calendar, Clock, MapPin,
    BookOpen, LogIn, LogOut, AlertTriangle, Phone, Building,
    GraduationCap, Star, Activity, CheckCircle, Loader2
} from 'lucide-react';
import librarianService from '../../services/librarianService';
import '../../styles/librarian/librarian-shared.css';

/**
 * Modal chi tiết sinh viên cho thủ thư (CHỈ ĐỌC - không có nút sửa/xóa/khóa)
 */
const StudentDetailModal = ({ userId, isOpen, onClose }) => {
    const [activeTab, setActiveTab] = useState('info');
    const [loading, setLoading] = useState(false);
    const [data, setData] = useState(null);
    const [error, setError] = useState(null);
    const [erroredAvatar, setErroredAvatar] = useState(false);

    useEffect(() => {
        if (isOpen && userId) {
            setLoading(true);
            setError(null);
            setActiveTab('info');
            setErroredAvatar(false);
            librarianService.getStudentDetail(userId)
                .then(result => {
                    if (result) {
                        setData(result);
                    } else {
                        setError('Không thể tải thông tin sinh viên');
                    }
                })
                .catch(() => setError('Lỗi kết nối server'))
                .finally(() => setLoading(false));
        }
    }, [isOpen, userId]);

    if (!isOpen) return null;

    const getRoleLabel = (role) => {
        switch (role) {
            case 'STUDENT': return 'Sinh viên';
            case 'LIBRARIAN': return 'Thủ thư';
            case 'ADMIN': return 'Admin';
            default: return role || 'N/A';
        }
    };

    const getActivityIcon = (type) => {
        switch (type) {
            case 'CHECK_IN': return { icon: LogIn, color: '#059669', label: 'Vào thư viện' };
            case 'CHECK_OUT': return { icon: LogOut, color: '#2563EB', label: 'Rời thư viện' };
            case 'BOOKING_SUCCESS': return { icon: Calendar, color: '#e8600a', label: 'Đặt chỗ' };
            case 'BOOKING_CANCEL': return { icon: Calendar, color: '#DC2626', label: 'Hủy đặt' };
            case 'NFC_CONFIRM': return { icon: CheckCircle, color: '#059669', label: 'Xác nhận NFC' };
            case 'GATE_ENTRY': return { icon: LogIn, color: '#7C3AED', label: 'Quẹt cổng' };
            case 'NO_SHOW': return { icon: AlertTriangle, color: '#DC2626', label: 'Không đến' };
            case 'VIOLATION': return { icon: AlertTriangle, color: '#DC2626', label: 'Vi phạm' };
            default: return { icon: Activity, color: '#6B7280', label: type };
        }
    };

    const formatDate = (dateStr) => {
        if (!dateStr) return 'N/A';
        const d = new Date(dateStr);
        return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
    };

    const formatDateTime = (dateStr) => {
        if (!dateStr) return 'N/A';
        const d = new Date(dateStr);
        return d.toLocaleDateString('vi-VN', {
            day: '2-digit', month: '2-digit', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    };

    const formatRelativeTime = (dateStr) => {
        if (!dateStr) return '';
        const d = new Date(dateStr);
        const now = new Date();
        const diff = Math.floor((now - d) / 1000 / 60);
        if (diff < 1) return 'Vừa xong';
        if (diff < 60) return `${diff} phút trước`;
        if (diff < 1440) return `${Math.floor(diff / 60)} giờ trước`;
        return `${Math.floor(diff / 1440)} ngày trước`;
    };

    const formatStudyTime = (minutes) => {
        if (!minutes || minutes === 0) return '0 phút';
        const h = Math.floor(minutes / 60);
        const m = minutes % 60;
        if (h > 0) return `${h} giờ ${m} phút`;
        return `${m} phút`;
    };

    const initials = data
        ? (data.fullName || 'U').split(' ').map(n => n[0]).slice(0, 2).join('').toUpperCase()
        : '';

    const getScoreColor = (score) => {
        if (score >= 80) return 'good';
        if (score >= 50) return 'warn';
        return 'bad';
    };

    const tabs = [
        { key: 'info', label: 'Thông tin' },
        { key: 'stats', label: 'Thống kê' },
        { key: 'history', label: 'Lịch sử hoạt động' }
    ];

    return (
        <div className="sr-modal-overlay" onClick={onClose}>
            <div className="sd-modal" onClick={e => e.stopPropagation()}>
                {loading ? (
                    <div className="sd-loading">
                        <Loader2 size={36} className="sd-spinner" color="#e8600a" />
                        <p>Đang tải thông tin...</p>
                    </div>
                ) : error ? (
                    <div className="sd-loading">
                        <AlertTriangle size={36} color="#DC2626" />
                        <p className="sd-error-text">{error}</p>
                        <button className="sr-modal-btn ghost" onClick={onClose}>Đóng</button>
                    </div>
                ) : data ? (
                    <>
                        {/* Header */}
                        <div className="sd-header">
                            <button className="sr-modal-close" onClick={onClose}>&times;</button>
                            <div className="sd-header-content">
                                <div className="sd-avatar-wrapper">
                                    {data.avtUrl && !erroredAvatar ? (
                                        <img
                                            src={data.avtUrl}
                                            alt=""
                                            className="sd-avatar-img"
                                            onError={() => setErroredAvatar(true)}
                                        />
                                    ) : (
                                        <div className="sd-avatar-fallback">{initials}</div>
                                    )}
                                    <span className={`sd-status-indicator ${data.isActive !== false ? 'active' : 'locked'}`} />
                                </div>
                                <div className="sd-header-info">
                                    <h2 className="sd-name">{data.fullName || 'Chưa có tên'}</h2>
                                    <div className="sd-meta">
                                        <span className="sd-code">{data.userCode || 'N/A'}</span>
                                        <span className={`sd-badge ${data.isActive !== false ? 'active' : 'locked'}`}>
                                            {data.isActive !== false ? 'Hoạt động' : 'Đã khóa'}
                                        </span>
                                    </div>
                                </div>
                            </div>

                        </div>

                        {/* Tabs */}
                        <div className="sd-tabs">
                            {tabs.map(tab => (
                                <button
                                    key={tab.key}
                                    className={`sd-tab ${activeTab === tab.key ? 'active' : ''}`}
                                    onClick={() => setActiveTab(tab.key)}
                                >
                                    {tab.label}
                                </button>
                            ))}
                        </div>

                        {/* Content */}
                        <div className="sd-content">
                            {activeTab === 'info' && (
                                <div className="sd-info-grid">
                                    <div className="sd-info-card">
                                        <h3 className="sr-modal-label">Thông tin cá nhân</h3>
                                        <div className="sd-info-list">
                                            <InfoRow icon={User} label="Họ tên" value={data.fullName || 'N/A'} />
                                            <InfoRow icon={Mail} label="Email" value={data.email || 'N/A'} />
                                            <InfoRow icon={Building} label="Mã sinh viên" value={data.userCode || 'N/A'} />
                                            <InfoRow icon={Phone} label="Số điện thoại" value={data.phone || 'Chưa cập nhật'} />
                                            <InfoRow icon={Calendar} label="Ngày sinh" value={data.dob ? formatDate(data.dob) : 'Chưa cập nhật'} />
                                        </div>
                                    </div>
                                    <div className="sd-info-card">
                                        <h3 className="sr-modal-label">Thông tin tài khoản</h3>
                                        <div className="sd-info-list">
                                            <InfoRow icon={Shield} label="Vai trò" value={
                                                <span className="sd-tag green">{getRoleLabel(data.role)}</span>
                                            } />
                                            <InfoRow icon={Activity} label="Trạng thái" value={
                                                <span className={`sd-tag ${data.isActive !== false ? 'green' : 'red'}`}>
                                                    {data.isActive !== false ? 'Hoạt động' : 'Đã khóa'}
                                                </span>
                                            } />
                                            <InfoRow icon={Star} label="Điểm uy tín" value={
                                                <span className={`sd-tag ${getScoreColor(data.reputationScore)}`}>
                                                    {data.reputationScore}/100
                                                </span>
                                            } />
                                            <InfoRow icon={Calendar} label="Ngày tạo tài khoản" value={formatDate(data.createdAt)} />
                                        </div>
                                    </div>
                                </div>
                            )}

                            {activeTab === 'stats' && (
                                <div>
                                    <div className="sd-stat-grid">
                                        <StatCard icon={LogIn} value={data.totalCheckIns} label="Lượt vào" color="green" />
                                        <StatCard icon={Clock} value={formatStudyTime(data.totalStudyMinutes)} label="Thời gian học" color="blue" />
                                        <StatCard icon={Calendar} value={data.totalBookings} label="Lượt đặt chỗ" color="orange" />
                                        <StatCard icon={AlertTriangle} value={data.violationCount} label="Vi phạm" color="red" />
                                    </div>
                                    <div className="sd-summary-card">
                                        <h3 className="sr-modal-label">Tóm tắt</h3>
                                        <div className="sd-summary-list">
                                            <div className="sd-summary-row">
                                                <span>Tổng thời gian học tại thư viện</span>
                                                <strong>{formatStudyTime(data.totalStudyMinutes)}</strong>
                                            </div>
                                            <div className="sd-summary-row">
                                                <span>Trung bình mỗi lần</span>
                                                <strong>{data.totalCheckIns > 0 ? formatStudyTime(Math.round(data.totalStudyMinutes / data.totalCheckIns)) : 'N/A'}</strong>
                                            </div>
                                            <div className="sd-summary-row">
                                                <span>Điểm uy tín</span>
                                                <strong className={`sd-score-text ${getScoreColor(data.reputationScore)}`}>
                                                    {data.reputationScore}/100
                                                </strong>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            )}

                            {activeTab === 'history' && (
                                <div>
                                    <h3 className="sr-modal-label" style={{ marginBottom: 16 }}>Hoạt động gần đây</h3>
                                    {data.recentActivities && data.recentActivities.length > 0 ? (
                                        <div className="sd-activity-list">
                                            {data.recentActivities.map((activity) => {
                                                const info = getActivityIcon(activity.activityType);
                                                const Icon = info.icon;
                                                return (
                                                    <div key={activity.id} className="sd-activity-row">
                                                        <div className="sd-activity-icon" style={{ background: `${info.color}12` }}>
                                                            <Icon size={16} color={info.color} />
                                                        </div>
                                                        <div className="sd-activity-content">
                                                            <div className="sd-activity-title">{activity.title}</div>
                                                            {activity.description && (
                                                                <div className="sd-activity-desc">{activity.description}</div>
                                                            )}
                                                            <div className="sd-activity-time">
                                                                {formatRelativeTime(activity.createdAt)} · {formatDateTime(activity.createdAt)}
                                                            </div>
                                                        </div>
                                                    </div>
                                                );
                                            })}
                                        </div>
                                    ) : (
                                        <div className="sd-empty">Chưa có hoạt động nào</div>
                                    )}
                                </div>
                            )}
                        </div>

                        {/* Footer */}
                        <div className="sr-modal-footer">
                            <button className="sr-modal-btn ghost" onClick={onClose}>Đóng</button>
                        </div>
                    </>
                ) : null}
            </div>
        </div>
    );
};

// --- Sub-components ---
const InfoRow = ({ icon: Icon, label, value }) => (
    <div className="sd-info-row">
        <div className="sd-info-icon">
            <Icon size={14} />
        </div>
        <div className="sd-info-text">
            <div className="sd-info-label">{label}</div>
            <div className="sd-info-value">
                {typeof value === 'string' ? value : value}
            </div>
        </div>
    </div>
);

const StatCard = ({ icon: Icon, value, label, color }) => (
    <div className={`sd-stat-card ${color}`}>
        <div className="sd-stat-icon">
            <Icon size={20} />
        </div>
        <div className="sd-stat-value">{value}</div>
        <div className="sd-stat-label">{label}</div>
    </div>
);

export default StudentDetailModal;
