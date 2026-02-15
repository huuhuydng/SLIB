import React, { useState, useEffect } from 'react';
import {
    X,
    User,
    Mail,
    Shield,
    Calendar,
    Clock,
    MapPin,
    BookOpen,
    LogIn,
    AlertTriangle,
    Edit2,
    Lock,
    Unlock,
    Trash2,
    ExternalLink,
    Phone,
    Building,
    GraduationCap,
    Star,
    Activity,
    CheckCircle,
    XCircle,
    Loader2
} from 'lucide-react';

/**
 * Modal hiển thị chi tiết thông tin người dùng
 * Props:
 * - user: object user cần hiển thị
 * - isOpen: boolean
 * - onClose: function
 * - onEdit: function(user) - optional
 * - onLock: function(user) - optional
 * - onDelete: function(user) - optional
 */
const UserDetailsModal = ({ user, isOpen, onClose, onEdit, onLock, onDelete }) => {
    const [activeTab, setActiveTab] = useState('info');
    const [loading, setLoading] = useState(false);
    const [userDetails, setUserDetails] = useState(null);
    const [activityHistory, setActivityHistory] = useState([]);
    const [stats, setStats] = useState({
        totalBookings: 0,
        totalCheckIns: 0,
        violations: 0,
        points: 100
    });

    // Mock data for demo
    useEffect(() => {
        if (isOpen && user) {
            setLoading(true);
            // Simulate API call
            setTimeout(() => {
                setUserDetails(user);
                setStats({
                    totalBookings: Math.floor(Math.random() * 50) + 10,
                    totalCheckIns: Math.floor(Math.random() * 100) + 20,
                    violations: Math.floor(Math.random() * 5),
                    points: Math.floor(Math.random() * 50) + 50
                });
                setActivityHistory([
                    {
                        id: 1,
                        type: 'CHECK_IN',
                        title: 'Check-in tại thư viện',
                        description: 'Khu A - Bàn 12',
                        createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString()
                    },
                    {
                        id: 2,
                        type: 'BOOKING',
                        title: 'Đặt chỗ thành công',
                        description: 'Khu B - Bàn 25, 14:00 - 16:00',
                        createdAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString()
                    },
                    {
                        id: 3,
                        type: 'CHECK_OUT',
                        title: 'Check-out',
                        description: 'Thời gian học: 2 giờ 30 phút',
                        createdAt: new Date(Date.now() - 26 * 60 * 60 * 1000).toISOString()
                    },
                    {
                        id: 4,
                        type: 'VIOLATION',
                        title: 'Vi phạm: Không đến',
                        description: 'Đặt chỗ nhưng không check-in',
                        createdAt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString()
                    }
                ]);
                setLoading(false);
            }, 500);
        }
    }, [isOpen, user]);

    if (!isOpen || !user) return null;

    const getRoleInfo = (role) => {
        switch (role) {
            case 'ADMIN':
                return { label: 'Admin', color: '#DC2626', bg: '#FEE2E2', icon: Shield };
            case 'LIBRARIAN':
                return { label: 'Thủ thư', color: '#2563EB', bg: '#DBEAFE', icon: BookOpen };
            case 'STUDENT':
                return { label: 'Sinh viên', color: '#059669', bg: '#D1FAE5', icon: GraduationCap };
            default:
                return { label: role, color: '#6B7280', bg: '#F3F4F6', icon: User };
        }
    };

    const getActivityIcon = (type) => {
        switch (type) {
            case 'CHECK_IN': return { icon: LogIn, color: '#059669' };
            case 'CHECK_OUT': return { icon: LogIn, color: '#2563EB' };
            case 'BOOKING': return { icon: Calendar, color: '#FF751F' };
            case 'VIOLATION': return { icon: AlertTriangle, color: '#DC2626' };
            default: return { icon: Activity, color: '#6B7280' };
        }
    };

    const formatDate = (dateStr) => {
        if (!dateStr) return 'N/A';
        const date = new Date(dateStr);
        return date.toLocaleDateString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    const formatRelativeTime = (dateStr) => {
        if (!dateStr) return 'N/A';
        const date = new Date(dateStr);
        const now = new Date();
        const diff = Math.floor((now - date) / 1000 / 60);
        if (diff < 60) return `${diff} phút trước`;
        if (diff < 1440) return `${Math.floor(diff / 60)} giờ trước`;
        return `${Math.floor(diff / 1440)} ngày trước`;
    };

    const roleInfo = getRoleInfo(user.role);
    const RoleIcon = roleInfo.icon;
    const initials = (user.fullName || user.email || 'U')
        .split(' ')
        .map(n => n[0])
        .slice(0, 2)
        .join('')
        .toUpperCase();

    const modalStyles = {
        overlay: {
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: 'rgba(0, 0, 0, 0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '20px'
        },
        modal: {
            background: '#fff',
            borderRadius: '20px',
            width: '100%',
            maxWidth: '800px',
            maxHeight: '90vh',
            overflow: 'hidden',
            display: 'flex',
            flexDirection: 'column',
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)'
        },
        header: {
            background: 'linear-gradient(135deg, #FF751F 0%, #FF9B5A 100%)',
            padding: '24px',
            color: '#fff',
            position: 'relative'
        },
        closeBtn: {
            position: 'absolute',
            top: '16px',
            right: '16px',
            background: 'rgba(255,255,255,0.2)',
            border: 'none',
            borderRadius: '10px',
            padding: '8px',
            cursor: 'pointer',
            color: '#fff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            transition: 'background 0.2s'
        },
        tabs: {
            display: 'flex',
            borderBottom: '2px solid #E2E8F0',
            padding: '0 24px',
            background: '#F7FAFC'
        },
        tab: (isActive) => ({
            padding: '16px 24px',
            fontSize: '14px',
            fontWeight: '600',
            color: isActive ? '#FF751F' : '#6B7280',
            background: 'transparent',
            border: 'none',
            borderBottom: isActive ? '3px solid #FF751F' : '3px solid transparent',
            marginBottom: '-2px',
            cursor: 'pointer',
            transition: 'all 0.2s'
        }),
        content: {
            flex: 1,
            overflow: 'auto',
            padding: '24px'
        },
        footer: {
            padding: '16px 24px',
            borderTop: '1px solid #E2E8F0',
            display: 'flex',
            justifyContent: 'flex-end',
            gap: '12px',
            background: '#F7FAFC'
        }
    };

    return (
        <div style={modalStyles.overlay} onClick={onClose}>
            <div style={modalStyles.modal} onClick={e => e.stopPropagation()}>
                {/* Header with Profile */}
                <div style={modalStyles.header}>
                    <button
                        style={modalStyles.closeBtn}
                        onClick={onClose}
                        onMouseEnter={e => e.target.style.background = 'rgba(255,255,255,0.3)'}
                        onMouseLeave={e => e.target.style.background = 'rgba(255,255,255,0.2)'}
                    >
                        <X size={20} />
                    </button>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
                        {/* Avatar */}
                        <div style={{
                            width: '80px',
                            height: '80px',
                            borderRadius: '20px',
                            background: user.avtUrl
                                ? `url(${user.avtUrl}) center/cover no-repeat`
                                : 'rgba(255,255,255,0.2)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontSize: '28px',
                            fontWeight: '700',
                            border: '3px solid rgba(255,255,255,0.3)',
                            boxShadow: '0 8px 20px rgba(0,0,0,0.15)'
                        }}>
                            {!user.avtUrl && initials}
                        </div>

                        {/* Info */}
                        <div style={{ flex: 1 }}>
                            <h2 style={{
                                margin: '0 0 8px 0',
                                fontSize: '24px',
                                fontWeight: '700'
                            }}>
                                {user.fullName || 'Chưa có tên'}
                            </h2>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
                                <span style={{
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    gap: '6px',
                                    padding: '6px 12px',
                                    background: 'rgba(255,255,255,0.2)',
                                    borderRadius: '20px',
                                    fontSize: '13px',
                                    fontWeight: '600'
                                }}>
                                    <RoleIcon size={14} />
                                    {roleInfo.label}
                                </span>
                                <span style={{
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    gap: '6px',
                                    fontSize: '14px',
                                    opacity: 0.9
                                }}>
                                    <Mail size={14} />
                                    {user.email}
                                </span>
                            </div>
                            {/* Status */}
                            <div style={{ marginTop: '8px' }}>
                                {user.isActive === false ? (
                                    <span style={{
                                        display: 'inline-flex',
                                        alignItems: 'center',
                                        gap: '6px',
                                        padding: '4px 10px',
                                        background: 'rgba(220, 38, 38, 0.2)',
                                        borderRadius: '12px',
                                        fontSize: '12px',
                                        fontWeight: '600'
                                    }}>
                                        <Lock size={12} /> Đã khóa
                                    </span>
                                ) : (
                                    <span style={{
                                        display: 'inline-flex',
                                        alignItems: 'center',
                                        gap: '6px',
                                        padding: '4px 10px',
                                        background: 'rgba(5, 150, 105, 0.2)',
                                        borderRadius: '12px',
                                        fontSize: '12px',
                                        fontWeight: '600'
                                    }}>
                                        <CheckCircle size={12} /> Hoạt động
                                    </span>
                                )}
                            </div>
                        </div>
                    </div>
                </div>

                {/* Tabs */}
                <div style={modalStyles.tabs}>
                    <button
                        style={modalStyles.tab(activeTab === 'info')}
                        onClick={() => setActiveTab('info')}
                    >
                        Thông tin
                    </button>
                    <button
                        style={modalStyles.tab(activeTab === 'stats')}
                        onClick={() => setActiveTab('stats')}
                    >
                        Thống kê
                    </button>
                    <button
                        style={modalStyles.tab(activeTab === 'history')}
                        onClick={() => setActiveTab('history')}
                    >
                        Lịch sử hoạt động
                    </button>
                </div>

                {/* Content */}
                <div style={modalStyles.content}>
                    {loading ? (
                        <div style={{ textAlign: 'center', padding: '40px', color: '#6B7280' }}>
                            <Loader2 size={32} style={{ animation: 'spin 1s linear infinite' }} />
                            <p>Đang tải thông tin...</p>
                        </div>
                    ) : (
                        <>
                            {/* Info Tab */}
                            {activeTab === 'info' && (
                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
                                    {/* Left Column */}
                                    <div style={{
                                        background: '#F7FAFC',
                                        borderRadius: '16px',
                                        padding: '20px'
                                    }}>
                                        <h3 style={{
                                            fontSize: '14px',
                                            fontWeight: '600',
                                            color: '#6B7280',
                                            marginBottom: '16px',
                                            textTransform: 'uppercase',
                                            letterSpacing: '0.5px'
                                        }}>Thông tin cơ bản</h3>

                                        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                                            <InfoRow icon={User} label="Họ tên" value={user.fullName || 'N/A'} />
                                            <InfoRow icon={Mail} label="Email" value={user.email || 'N/A'} />
                                            <InfoRow icon={Building} label="Mã người dùng" value={user.userCode || 'N/A'} />
                                            <InfoRow icon={Phone} label="Số điện thoại" value={user.phone || 'Chưa cập nhật'} />
                                            <InfoRow icon={MapPin} label="Địa chỉ" value={user.address || 'Chưa cập nhật'} />
                                        </div>
                                    </div>

                                    {/* Right Column */}
                                    <div style={{
                                        background: '#F7FAFC',
                                        borderRadius: '16px',
                                        padding: '20px'
                                    }}>
                                        <h3 style={{
                                            fontSize: '14px',
                                            fontWeight: '600',
                                            color: '#6B7280',
                                            marginBottom: '16px',
                                            textTransform: 'uppercase',
                                            letterSpacing: '0.5px'
                                        }}>Thông tin tài khoản</h3>

                                        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                                            <InfoRow
                                                icon={Shield}
                                                label="Vai trò"
                                                value={
                                                    <span style={{
                                                        padding: '4px 10px',
                                                        borderRadius: '12px',
                                                        background: roleInfo.bg,
                                                        color: roleInfo.color,
                                                        fontSize: '12px',
                                                        fontWeight: '600'
                                                    }}>
                                                        {roleInfo.label}
                                                    </span>
                                                }
                                            />
                                            <InfoRow
                                                icon={Activity}
                                                label="Trạng thái"
                                                value={
                                                    <span style={{
                                                        padding: '4px 10px',
                                                        borderRadius: '12px',
                                                        background: user.isActive === false ? '#FEE2E2' : '#D1FAE5',
                                                        color: user.isActive === false ? '#DC2626' : '#059669',
                                                        fontSize: '12px',
                                                        fontWeight: '600'
                                                    }}>
                                                        {user.isActive === false ? 'Đã khóa' : 'Hoạt động'}
                                                    </span>
                                                }
                                            />
                                            <InfoRow icon={Calendar} label="Ngày tạo" value={formatDate(user.createdAt)} />
                                            <InfoRow icon={Clock} label="Lần hoạt động cuối" value={user.lastActive ? formatRelativeTime(user.lastActive) : 'Chưa hoạt động'} />
                                            {user.passwordChanged === false && (
                                                <div style={{
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    gap: '8px',
                                                    padding: '12px',
                                                    background: '#FEF3C7',
                                                    borderRadius: '10px',
                                                    color: '#B45309',
                                                    fontSize: '13px'
                                                }}>
                                                    <AlertTriangle size={16} />
                                                    Chưa đổi mật khẩu mặc định
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            )}

                            {/* Stats Tab */}
                            {activeTab === 'stats' && (
                                <div>
                                    <div style={{
                                        display: 'grid',
                                        gridTemplateColumns: 'repeat(4, 1fr)',
                                        gap: '16px',
                                        marginBottom: '24px'
                                    }}>
                                        <StatCard
                                            icon={Calendar}
                                            value={stats.totalBookings}
                                            label="Lượt đặt chỗ"
                                            color="#FF751F"
                                            bg="#FFF7ED"
                                        />
                                        <StatCard
                                            icon={LogIn}
                                            value={stats.totalCheckIns}
                                            label="Lượt check-in"
                                            color="#059669"
                                            bg="#D1FAE5"
                                        />
                                        <StatCard
                                            icon={AlertTriangle}
                                            value={stats.violations}
                                            label="Vi phạm"
                                            color="#DC2626"
                                            bg="#FEE2E2"
                                        />
                                        <StatCard
                                            icon={Star}
                                            value={stats.points}
                                            label="Điểm tích lũy"
                                            color="#7C3AED"
                                            bg="#EDE9FE"
                                        />
                                    </div>

                                    <div style={{
                                        background: '#F7FAFC',
                                        borderRadius: '16px',
                                        padding: '20px'
                                    }}>
                                        <h3 style={{
                                            fontSize: '14px',
                                            fontWeight: '600',
                                            color: '#6B7280',
                                            marginBottom: '16px'
                                        }}>Biểu đồ hoạt động</h3>
                                        <div style={{
                                            height: '200px',
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            color: '#A0AEC0',
                                            fontSize: '14px'
                                        }}>
                                            [Chart placeholder - Tích hợp biểu đồ ở đây]
                                        </div>
                                    </div>
                                </div>
                            )}

                            {/* History Tab */}
                            {activeTab === 'history' && (
                                <div>
                                    <div style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'space-between',
                                        marginBottom: '16px'
                                    }}>
                                        <h3 style={{
                                            fontSize: '14px',
                                            fontWeight: '600',
                                            color: '#6B7280',
                                            margin: 0
                                        }}>Hoạt động gần đây</h3>
                                        <button style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: '6px',
                                            padding: '8px 12px',
                                            background: '#FF751F',
                                            color: '#fff',
                                            border: 'none',
                                            borderRadius: '8px',
                                            fontSize: '13px',
                                            fontWeight: '600',
                                            cursor: 'pointer'
                                        }}>
                                            <ExternalLink size={14} />
                                            Xem tất cả
                                        </button>
                                    </div>

                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                                        {activityHistory.map((activity) => {
                                            const activityInfo = getActivityIcon(activity.type);
                                            const ActivityIcon = activityInfo.icon;
                                            return (
                                                <div
                                                    key={activity.id}
                                                    style={{
                                                        display: 'flex',
                                                        alignItems: 'flex-start',
                                                        gap: '16px',
                                                        padding: '16px',
                                                        background: '#F7FAFC',
                                                        borderRadius: '12px',
                                                        transition: 'background 0.2s'
                                                    }}
                                                >
                                                    <div style={{
                                                        width: '40px',
                                                        height: '40px',
                                                        borderRadius: '10px',
                                                        background: `${activityInfo.color}15`,
                                                        display: 'flex',
                                                        alignItems: 'center',
                                                        justifyContent: 'center'
                                                    }}>
                                                        <ActivityIcon size={18} color={activityInfo.color} />
                                                    </div>
                                                    <div style={{ flex: 1 }}>
                                                        <div style={{
                                                            fontSize: '14px',
                                                            fontWeight: '600',
                                                            color: '#1A1A1A',
                                                            marginBottom: '4px'
                                                        }}>
                                                            {activity.title}
                                                        </div>
                                                        <div style={{
                                                            fontSize: '13px',
                                                            color: '#6B7280',
                                                            marginBottom: '6px'
                                                        }}>
                                                            {activity.description}
                                                        </div>
                                                        <div style={{
                                                            fontSize: '12px',
                                                            color: '#A0AEC0'
                                                        }}>
                                                            {formatRelativeTime(activity.createdAt)}
                                                        </div>
                                                    </div>
                                                </div>
                                            );
                                        })}
                                    </div>
                                </div>
                            )}
                        </>
                    )}
                </div>

                {/* Footer Actions */}
                <div style={modalStyles.footer}>
                    <button
                        onClick={() => onDelete?.(user)}
                        style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '8px',
                            padding: '10px 20px',
                            background: '#fff',
                            color: '#DC2626',
                            border: '2px solid #FCA5A5',
                            borderRadius: '10px',
                            fontSize: '14px',
                            fontWeight: '600',
                            cursor: 'pointer'
                        }}
                    >
                        <Trash2 size={16} />
                        Xóa
                    </button>
                    {user.isActive === false ? (
                        <button
                            onClick={() => onLock?.(user)}
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: '8px',
                                padding: '10px 20px',
                                background: '#D1FAE5',
                                color: '#059669',
                                border: 'none',
                                borderRadius: '10px',
                                fontSize: '14px',
                                fontWeight: '600',
                                cursor: 'pointer'
                            }}
                        >
                            <Unlock size={16} />
                            Mở khóa
                        </button>
                    ) : (
                        <button
                            onClick={() => onLock?.(user)}
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: '8px',
                                padding: '10px 20px',
                                background: '#FEE2E2',
                                color: '#DC2626',
                                border: 'none',
                                borderRadius: '10px',
                                fontSize: '14px',
                                fontWeight: '600',
                                cursor: 'pointer'
                            }}
                        >
                            <Lock size={16} />
                            Khóa tài khoản
                        </button>
                    )}
                    <button
                        onClick={() => onEdit?.(user)}
                        style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '8px',
                            padding: '10px 20px',
                            background: '#FF751F',
                            color: '#fff',
                            border: 'none',
                            borderRadius: '10px',
                            fontSize: '14px',
                            fontWeight: '600',
                            cursor: 'pointer',
                            boxShadow: '0 4px 12px rgba(255, 117, 31, 0.25)'
                        }}
                    >
                        <Edit2 size={16} />
                        Chỉnh sửa
                    </button>
                </div>
            </div>
        </div>
    );
};

// Helper Components
const InfoRow = ({ icon: Icon, label, value }) => (
    <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
        <div style={{
            width: '32px',
            height: '32px',
            borderRadius: '8px',
            background: '#E2E8F0',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0
        }}>
            <Icon size={16} color="#6B7280" />
        </div>
        <div style={{ flex: 1 }}>
            <div style={{ fontSize: '12px', color: '#A0AEC0', marginBottom: '2px' }}>{label}</div>
            <div style={{ fontSize: '14px', color: '#1A1A1A', fontWeight: '500' }}>
                {typeof value === 'string' ? value : value}
            </div>
        </div>
    </div>
);

const StatCard = ({ icon: Icon, value, label, color, bg }) => (
    <div style={{
        background: bg,
        borderRadius: '16px',
        padding: '20px',
        textAlign: 'center'
    }}>
        <div style={{
            width: '48px',
            height: '48px',
            borderRadius: '12px',
            background: '#fff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 12px',
            boxShadow: '0 2px 8px rgba(0,0,0,0.05)'
        }}>
            <Icon size={24} color={color} />
        </div>
        <div style={{
            fontSize: '28px',
            fontWeight: '700',
            color: color,
            marginBottom: '4px'
        }}>
            {value}
        </div>
        <div style={{
            fontSize: '13px',
            color: '#6B7280',
            fontWeight: '500'
        }}>
            {label}
        </div>
    </div>
);

export default UserDetailsModal;
