import React, { useState, useEffect } from 'react';
import {
    X, User, Mail, Shield, Calendar, Clock, MapPin,
    BookOpen, LogIn, LogOut, AlertTriangle, Phone, Building,
    GraduationCap, Star, Activity, CheckCircle, Loader2
} from 'lucide-react';
import librarianService from '../../services/librarianService';

/**
 * Modal chi tiết sinh viên cho thủ thư (CHỈ ĐỌC - không có nút sửa/xóa/khóa)
 */
const StudentDetailModal = ({ userId, isOpen, onClose }) => {
    const [activeTab, setActiveTab] = useState('info');
    const [loading, setLoading] = useState(false);
    const [data, setData] = useState(null);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (isOpen && userId) {
            setLoading(true);
            setError(null);
            setActiveTab('info');
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
            case 'BOOKING_SUCCESS': return { icon: Calendar, color: '#FF751F', label: 'Đặt chỗ' };
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

    return (
        <div style={styles.overlay} onClick={onClose}>
            <div style={styles.modal} onClick={e => e.stopPropagation()}>
                {loading ? (
                    <div style={styles.loadingContainer}>
                        <Loader2 size={36} style={{ animation: 'spin 1s linear infinite' }} color="#FF751F" />
                        <p style={{ color: '#6B7280', marginTop: 12 }}>Đang tải thông tin...</p>
                    </div>
                ) : error ? (
                    <div style={styles.loadingContainer}>
                        <AlertTriangle size={36} color="#DC2626" />
                        <p style={{ color: '#DC2626', marginTop: 12 }}>{error}</p>
                        <button onClick={onClose} style={styles.closeTextBtn}>Đóng</button>
                    </div>
                ) : data ? (
                    <>
                        {/* Header */}
                        <div style={styles.header}>
                            <button style={styles.closeBtn} onClick={onClose}>
                                <X size={20} />
                            </button>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 20 }}>
                                <div style={{
                                    width: 72, height: 72, borderRadius: 18,
                                    background: data.avtUrl
                                        ? `url(${data.avtUrl}) center/cover no-repeat`
                                        : 'rgba(255,255,255,0.2)',
                                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                                    fontSize: 24, fontWeight: 700, color: '#fff',
                                    border: '3px solid rgba(255,255,255,0.3)',
                                    boxShadow: '0 8px 20px rgba(0,0,0,0.15)'
                                }}>
                                    {!data.avtUrl && initials}
                                </div>
                                <div style={{ flex: 1 }}>
                                    <h2 style={{ margin: '0 0 6px', fontSize: 22, fontWeight: 700, color: '#fff' }}>
                                        {data.fullName || 'Chưa có tên'}
                                    </h2>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
                                        <span style={styles.headerBadge}>
                                            <GraduationCap size={13} /> {getRoleLabel(data.role)}
                                        </span>
                                        <span style={{ fontSize: 13, opacity: 0.9, color: '#fff', display: 'flex', alignItems: 'center', gap: 4 }}>
                                            <Building size={13} /> {data.userCode || 'N/A'}
                                        </span>
                                    </div>
                                    <div style={{ marginTop: 6 }}>
                                        <span style={{
                                            ...styles.statusBadge,
                                            background: data.isActive !== false ? 'rgba(5,150,105,0.2)' : 'rgba(220,38,38,0.2)'
                                        }}>
                                            <CheckCircle size={11} />
                                            {data.isActive !== false ? 'Hoạt động' : 'Đã khóa'}
                                        </span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Tabs */}
                        <div style={styles.tabs}>
                            {[
                                { key: 'info', label: 'Thông tin' },
                                { key: 'stats', label: 'Thống kê' },
                                { key: 'history', label: 'Lịch sử hoạt động' }
                            ].map(tab => (
                                <button
                                    key={tab.key}
                                    style={styles.tab(activeTab === tab.key)}
                                    onClick={() => setActiveTab(tab.key)}
                                >
                                    {tab.label}
                                </button>
                            ))}
                        </div>

                        {/* Content */}
                        <div style={styles.content}>
                            {activeTab === 'info' && (
                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
                                    <div style={styles.card}>
                                        <h3 style={styles.cardTitle}>Thông tin cá nhân</h3>
                                        <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                                            <InfoRow icon={User} label="Họ tên" value={data.fullName || 'N/A'} />
                                            <InfoRow icon={Mail} label="Email" value={data.email || 'N/A'} />
                                            <InfoRow icon={Building} label="Mã sinh viên" value={data.userCode || 'N/A'} />
                                            <InfoRow icon={Phone} label="Số điện thoại" value={data.phone || 'Chưa cập nhật'} />
                                            <InfoRow icon={Calendar} label="Ngày sinh" value={data.dob ? formatDate(data.dob) : 'Chưa cập nhật'} />
                                        </div>
                                    </div>
                                    <div style={styles.card}>
                                        <h3 style={styles.cardTitle}>Thông tin tài khoản</h3>
                                        <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                                            <InfoRow icon={Shield} label="Vai trò" value={
                                                <span style={{
                                                    padding: '3px 10px', borderRadius: 12,
                                                    background: '#D1FAE5', color: '#059669',
                                                    fontSize: 12, fontWeight: 600
                                                }}>
                                                    {getRoleLabel(data.role)}
                                                </span>
                                            } />
                                            <InfoRow icon={Activity} label="Trạng thái" value={
                                                <span style={{
                                                    padding: '3px 10px', borderRadius: 12,
                                                    background: data.isActive !== false ? '#D1FAE5' : '#FEE2E2',
                                                    color: data.isActive !== false ? '#059669' : '#DC2626',
                                                    fontSize: 12, fontWeight: 600
                                                }}>
                                                    {data.isActive !== false ? 'Hoạt động' : 'Đã khóa'}
                                                </span>
                                            } />
                                            <InfoRow icon={Star} label="Điểm uy tín" value={
                                                <span style={{
                                                    padding: '3px 10px', borderRadius: 12,
                                                    background: data.reputationScore >= 80 ? '#D1FAE5' : data.reputationScore >= 50 ? '#FEF3C7' : '#FEE2E2',
                                                    color: data.reputationScore >= 80 ? '#059669' : data.reputationScore >= 50 ? '#B45309' : '#DC2626',
                                                    fontSize: 12, fontWeight: 600
                                                }}>
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
                                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 24 }}>
                                        <StatCard icon={LogIn} value={data.totalCheckIns} label="Lượt vào" color="#059669" bg="#D1FAE5" />
                                        <StatCard icon={Clock} value={formatStudyTime(data.totalStudyMinutes)} label="Thời gian học" color="#2563EB" bg="#DBEAFE" />
                                        <StatCard icon={Calendar} value={data.totalBookings} label="Lượt đặt chỗ" color="#FF751F" bg="#FFF7ED" />
                                        <StatCard icon={AlertTriangle} value={data.violationCount} label="Vi phạm" color="#DC2626" bg="#FEE2E2" />
                                    </div>
                                    <div style={styles.card}>
                                        <h3 style={{ ...styles.cardTitle, marginBottom: 12 }}>Tóm tắt</h3>
                                        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                                            <div style={styles.summaryRow}>
                                                <span>Tổng thời gian học tại thư viện</span>
                                                <strong>{formatStudyTime(data.totalStudyMinutes)}</strong>
                                            </div>
                                            <div style={styles.summaryRow}>
                                                <span>Trung bình mỗi lần</span>
                                                <strong>{data.totalCheckIns > 0 ? formatStudyTime(Math.round(data.totalStudyMinutes / data.totalCheckIns)) : 'N/A'}</strong>
                                            </div>
                                            <div style={styles.summaryRow}>
                                                <span>Điểm uy tín</span>
                                                <strong style={{ color: data.reputationScore >= 80 ? '#059669' : '#DC2626' }}>
                                                    {data.reputationScore}/100
                                                </strong>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            )}

                            {activeTab === 'history' && (
                                <div>
                                    <h3 style={{ ...styles.cardTitle, marginBottom: 16 }}>Hoạt động gần đây</h3>
                                    {data.recentActivities && data.recentActivities.length > 0 ? (
                                        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                                            {data.recentActivities.map((activity) => {
                                                const info = getActivityIcon(activity.activityType);
                                                const Icon = info.icon;
                                                return (
                                                    <div key={activity.id} style={styles.activityRow}>
                                                        <div style={{ ...styles.activityIcon, background: `${info.color}15` }}>
                                                            <Icon size={16} color={info.color} />
                                                        </div>
                                                        <div style={{ flex: 1 }}>
                                                            <div style={{ fontSize: 14, fontWeight: 600, color: '#1A1A1A', marginBottom: 3 }}>
                                                                {activity.title}
                                                            </div>
                                                            {activity.description && (
                                                                <div style={{ fontSize: 13, color: '#6B7280', marginBottom: 4 }}>
                                                                    {activity.description}
                                                                </div>
                                                            )}
                                                            <div style={{ fontSize: 12, color: '#A0AEC0' }}>
                                                                {formatRelativeTime(activity.createdAt)} · {formatDateTime(activity.createdAt)}
                                                            </div>
                                                        </div>
                                                    </div>
                                                );
                                            })}
                                        </div>
                                    ) : (
                                        <div style={{ textAlign: 'center', padding: 40, color: '#A0AEC0' }}>
                                            Chưa có hoạt động nào
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>

                        {/* Footer - chỉ có nút Đóng */}
                        <div style={styles.footer}>
                            <button onClick={onClose} style={styles.closeFooterBtn}>
                                Đóng
                            </button>
                        </div>
                    </>
                ) : null}
            </div>
        </div>
    );
};

// --- Sub-components ---
const InfoRow = ({ icon: Icon, label, value }) => (
    <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10 }}>
        <div style={{
            width: 30, height: 30, borderRadius: 8, background: '#E2E8F0',
            display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0
        }}>
            <Icon size={14} color="#6B7280" />
        </div>
        <div style={{ flex: 1 }}>
            <div style={{ fontSize: 11, color: '#A0AEC0', marginBottom: 1 }}>{label}</div>
            <div style={{ fontSize: 13, color: '#1A1A1A', fontWeight: 500 }}>
                {typeof value === 'string' ? value : value}
            </div>
        </div>
    </div>
);

const StatCard = ({ icon: Icon, value, label, color, bg }) => (
    <div style={{ background: bg, borderRadius: 16, padding: 18, textAlign: 'center' }}>
        <div style={{
            width: 44, height: 44, borderRadius: 12, background: '#fff',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            margin: '0 auto 10px', boxShadow: '0 2px 8px rgba(0,0,0,0.05)'
        }}>
            <Icon size={22} color={color} />
        </div>
        <div style={{ fontSize: 24, fontWeight: 700, color, marginBottom: 2 }}>
            {value}
        </div>
        <div style={{ fontSize: 12, color: '#6B7280', fontWeight: 500 }}>{label}</div>
    </div>
);

// --- Styles ---
const styles = {
    overlay: {
        position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
        background: 'rgba(0,0,0,0.5)', display: 'flex',
        alignItems: 'center', justifyContent: 'center',
        zIndex: 1000, padding: 20
    },
    modal: {
        background: '#fff', borderRadius: 20, width: '100%', maxWidth: 780,
        maxHeight: '90vh', overflow: 'hidden', display: 'flex', flexDirection: 'column',
        boxShadow: '0 25px 50px -12px rgba(0,0,0,0.25)'
    },
    header: {
        background: 'linear-gradient(135deg, #FF751F 0%, #FF9B5A 100%)',
        padding: '22px 24px', color: '#fff', position: 'relative'
    },
    closeBtn: {
        position: 'absolute', top: 14, right: 14,
        background: 'rgba(255,255,255,0.2)', border: 'none', borderRadius: 10,
        padding: 8, cursor: 'pointer', color: '#fff',
        display: 'flex', alignItems: 'center', justifyContent: 'center'
    },
    headerBadge: {
        display: 'inline-flex', alignItems: 'center', gap: 5,
        padding: '4px 10px', background: 'rgba(255,255,255,0.2)',
        borderRadius: 16, fontSize: 12, fontWeight: 600, color: '#fff'
    },
    statusBadge: {
        display: 'inline-flex', alignItems: 'center', gap: 4,
        padding: '3px 8px', borderRadius: 10, fontSize: 11, fontWeight: 600, color: '#fff'
    },
    tabs: {
        display: 'flex', borderBottom: '2px solid #E2E8F0',
        padding: '0 24px', background: '#F7FAFC'
    },
    tab: (isActive) => ({
        padding: '14px 20px', fontSize: 13, fontWeight: 600,
        color: isActive ? '#FF751F' : '#6B7280', background: 'transparent',
        border: 'none', borderBottom: isActive ? '3px solid #FF751F' : '3px solid transparent',
        marginBottom: -2, cursor: 'pointer', transition: 'all 0.2s'
    }),
    content: {
        flex: 1, overflow: 'auto', padding: 24
    },
    card: {
        background: '#F7FAFC', borderRadius: 16, padding: 20
    },
    cardTitle: {
        fontSize: 13, fontWeight: 600, color: '#6B7280',
        marginBottom: 14, textTransform: 'uppercase', letterSpacing: 0.5
    },
    summaryRow: {
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        padding: '10px 0', borderBottom: '1px solid #E2E8F0',
        fontSize: 14, color: '#374151'
    },
    activityRow: {
        display: 'flex', alignItems: 'flex-start', gap: 14,
        padding: 14, background: '#F7FAFC', borderRadius: 12
    },
    activityIcon: {
        width: 36, height: 36, borderRadius: 10,
        display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0
    },
    footer: {
        padding: '14px 24px', borderTop: '1px solid #E2E8F0',
        display: 'flex', justifyContent: 'flex-end', background: '#F7FAFC'
    },
    closeFooterBtn: {
        padding: '10px 24px', background: '#F1F5F9', color: '#374151',
        border: '1px solid #E2E8F0', borderRadius: 10,
        fontSize: 14, fontWeight: 600, cursor: 'pointer'
    },
    closeTextBtn: {
        padding: '8px 20px', background: '#F1F5F9', color: '#374151',
        border: '1px solid #E2E8F0', borderRadius: 8, marginTop: 16,
        fontSize: 13, fontWeight: 600, cursor: 'pointer'
    },
    loadingContainer: {
        display: 'flex', flexDirection: 'column', alignItems: 'center',
        justifyContent: 'center', padding: 60
    }
};

export default StudentDetailModal;
