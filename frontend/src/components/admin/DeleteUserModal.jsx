import React, { useState, useEffect } from 'react';
import {
    X,
    AlertTriangle,
    Trash2,
    Loader2,
    User,
    Calendar,
    CheckCircle2
} from 'lucide-react';
import userService from '../../services/auth/userService';

/**
 * Modal xác nhận xóa người dùng với type-to-confirm
 * Props:
 * - user: object user cần xóa
 * - isOpen: boolean
 * - onClose: function
 * - onDeleted: function() - callback khi xóa thành công
 * - currentUserId: string - ID của user đang đăng nhập (để kiểm tra self-delete)
 */
const DeleteUserModal = ({ user, isOpen, onClose, onDeleted, currentUserId }) => {
    const [confirmText, setConfirmText] = useState('');
    const [loading, setLoading] = useState(false);
    const [checking, setChecking] = useState(true);
    const [activeBookings, setActiveBookings] = useState(0);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState(false);

    // Check active bookings when modal opens
    useEffect(() => {
        if (isOpen && user) {
            setConfirmText('');
            setError('');
            setSuccess(false);
            setChecking(true);

            // Check for active bookings
            userService.checkUserActiveBookings(user.id)
                .then(data => {
                    setActiveBookings(data.count || 0);
                })
                .catch(() => {
                    setActiveBookings(0);
                })
                .finally(() => {
                    setChecking(false);
                });
        }
    }, [isOpen, user]);

    if (!isOpen || !user) return null;

    // Validation checks
    const isSelf = user.id === currentUserId;
    const isLastAdmin = user.role === 'ADMIN'; // TODO: Check if last admin from API
    const confirmRequired = user.fullName || user.email || '';
    const isConfirmed = confirmText.trim().toLowerCase() === confirmRequired.trim().toLowerCase();

    const handleDelete = async () => {
        if (!isConfirmed || isSelf) return;

        try {
            setLoading(true);
            setError('');

            await userService.deleteUser(user.id, false); // soft delete

            setSuccess(true);
            setTimeout(() => {
                setSuccess(false);
                onDeleted?.();
                onClose();
            }, 1800);
        } catch (err) {
            console.error('Delete user error:', err);
            setError(err.response?.data?.message || err.message || 'Không thể xóa tài khoản');
        } finally {
            setLoading(false);
        }
    };

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
            maxWidth: '480px',
            overflow: 'hidden',
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
            position: 'relative'
        },
        header: {
            background: 'linear-gradient(135deg, #DC2626 0%, #EF4444 100%)',
            padding: '24px',
            color: '#fff',
            display: 'flex',
            alignItems: 'center',
            gap: '16px'
        },
        content: {
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
        <div style={modalStyles.overlay} onClick={success ? undefined : onClose}>
            <div style={modalStyles.modal} onClick={e => e.stopPropagation()}>
                {/* Success Overlay */}
                {success && (
                    <div style={{
                        position: 'absolute',
                        top: 0, left: 0, right: 0, bottom: 0,
                        background: 'rgba(255,255,255,0.96)',
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        zIndex: 10,
                        borderRadius: '20px',
                        animation: 'fadeIn 0.3s ease'
                    }}>
                        <div style={{
                            width: '72px', height: '72px',
                            borderRadius: '50%',
                            background: 'linear-gradient(135deg, #10B981, #059669)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            marginBottom: '16px',
                            animation: 'scaleIn 0.4s ease'
                        }}>
                            <CheckCircle2 size={36} color="#fff" />
                        </div>
                        <h3 style={{ margin: 0, fontSize: '20px', fontWeight: '700', color: '#1A1A1A' }}>
                            Xóa thành công!
                        </h3>
                        <p style={{ margin: '8px 0 0', fontSize: '14px', color: '#6B7280' }}>
                            Tài khoản <strong>{user.fullName}</strong> đã được xóa
                        </p>
                        <style>{`
                            @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
                            @keyframes scaleIn { from { transform: scale(0.5); opacity: 0; } to { transform: scale(1); opacity: 1; } }
                        `}</style>
                    </div>
                )}
                {/* Header */}
                <div style={modalStyles.header}>
                    <div style={{
                        width: '48px',
                        height: '48px',
                        borderRadius: '12px',
                        background: 'rgba(255,255,255,0.2)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                    }}>
                        <Trash2 size={24} />
                    </div>
                    <div style={{ flex: 1 }}>
                        <h2 style={{ margin: 0, fontSize: '20px', fontWeight: '700' }}>
                            Xóa tài khoản
                        </h2>
                        <p style={{ margin: '4px 0 0', fontSize: '14px', opacity: 0.9 }}>
                            Hành động này không thể hoàn tác
                        </p>
                    </div>
                    <button
                        onClick={onClose}
                        style={{
                            background: 'rgba(255,255,255,0.2)',
                            border: 'none',
                            borderRadius: '10px',
                            padding: '8px',
                            cursor: 'pointer',
                            color: '#fff',
                            display: 'flex'
                        }}
                    >
                        <X size={20} />
                    </button>
                </div>

                {/* Content */}
                <div style={modalStyles.content}>
                    {/* User Info Card */}
                    <div style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '16px',
                        padding: '16px',
                        background: '#FEF2F2',
                        borderRadius: '12px',
                        marginBottom: '20px'
                    }}>
                        <div style={{
                            width: '50px',
                            height: '50px',
                            borderRadius: '12px',
                            background: user.avtUrl
                                ? `url(${user.avtUrl}) center/cover no-repeat`
                                : 'linear-gradient(135deg, #DC2626, #EF4444)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: '#fff',
                            fontSize: '18px',
                            fontWeight: '600'
                        }}>
                            {!user.avtUrl && (user.fullName?.[0] || 'U')}
                        </div>
                        <div style={{ flex: 1 }}>
                            <div style={{ fontSize: '16px', fontWeight: '600', color: '#1A1A1A' }}>
                                {user.fullName || 'Chưa có tên'}
                            </div>
                            <div style={{ fontSize: '14px', color: '#6B7280' }}>
                                {user.email}
                            </div>
                            <div style={{ fontSize: '12px', color: '#9CA3AF', marginTop: '4px' }}>
                                {user.userCode} - {user.role}
                            </div>
                        </div>
                    </div>

                    {/* Self-delete warning */}
                    {isSelf && (
                        <div style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '12px',
                            padding: '16px',
                            background: '#FEF3C7',
                            borderRadius: '12px',
                            marginBottom: '16px',
                            color: '#B45309'
                        }}>
                            <AlertTriangle size={20} />
                            <span style={{ fontSize: '14px', fontWeight: '500' }}>
                                Không thể xóa tài khoản của chính mình
                            </span>
                        </div>
                    )}

                    {/* Active bookings warning */}
                    {!isSelf && checking && (
                        <div style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '12px',
                            padding: '16px',
                            background: '#F3F4F6',
                            borderRadius: '12px',
                            marginBottom: '16px',
                            color: '#6B7280'
                        }}>
                            <Loader2 size={18} style={{ animation: 'spin 1s linear infinite' }} />
                            <span style={{ fontSize: '14px' }}>Đang kiểm tra...</span>
                        </div>
                    )}

                    {!isSelf && !checking && activeBookings > 0 && (
                        <div style={{
                            display: 'flex',
                            alignItems: 'flex-start',
                            gap: '12px',
                            padding: '16px',
                            background: '#FFF7ED',
                            borderRadius: '12px',
                            marginBottom: '16px',
                            border: '1px solid #FDBA74'
                        }}>
                            <Calendar size={20} color="#EA580C" style={{ marginTop: '2px' }} />
                            <div>
                                <div style={{ fontSize: '14px', fontWeight: '600', color: '#EA580C' }}>
                                    Người dùng có {activeBookings} lượt đặt chỗ đang hoạt động
                                </div>
                                <div style={{ fontSize: '13px', color: '#9A3412', marginTop: '4px' }}>
                                    Các lượt đặt chỗ sẽ bị hủy khi xóa tài khoản
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Confirmation text input */}
                    {!isSelf && (
                        <div>
                            <label style={{
                                display: 'block',
                                fontSize: '14px',
                                fontWeight: '600',
                                color: '#374151',
                                marginBottom: '8px'
                            }}>
                                Để xác nhận, hãy nhập <strong style={{ color: '#DC2626' }}>"{confirmRequired}"</strong>
                            </label>
                            <input
                                type="text"
                                value={confirmText}
                                onChange={(e) => setConfirmText(e.target.value)}
                                placeholder="Nhập tên người dùng để xác nhận..."
                                style={{
                                    width: '100%',
                                    padding: '14px 16px',
                                    border: `2px solid ${isConfirmed ? '#10B981' : '#E5E7EB'}`,
                                    borderRadius: '12px',
                                    fontSize: '14px',
                                    outline: 'none',
                                    transition: 'border-color 0.2s',
                                    boxSizing: 'border-box'
                                }}
                            />
                            {confirmText && !isConfirmed && (
                                <p style={{
                                    fontSize: '12px',
                                    color: '#EF4444',
                                    margin: '8px 0 0',
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '6px'
                                }}>
                                    <AlertTriangle size={12} />
                                    Tên nhập vào không khớp
                                </p>
                            )}
                            {isConfirmed && (
                                <p style={{
                                    fontSize: '12px',
                                    color: '#10B981',
                                    margin: '8px 0 0'
                                }}>
                                    Đã xác nhận. Bạn có thể xóa tài khoản.
                                </p>
                            )}
                        </div>
                    )}

                    {/* Error message */}
                    {error && (
                        <div style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '8px',
                            padding: '12px',
                            background: '#FEE2E2',
                            borderRadius: '8px',
                            marginTop: '16px',
                            color: '#DC2626',
                            fontSize: '14px'
                        }}>
                            <AlertTriangle size={16} />
                            {error}
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div style={modalStyles.footer}>
                    <button
                        onClick={onClose}
                        disabled={loading}
                        style={{
                            padding: '12px 24px',
                            background: '#fff',
                            border: '2px solid #E5E7EB',
                            borderRadius: '12px',
                            fontSize: '14px',
                            fontWeight: '600',
                            color: '#6B7280',
                            cursor: loading ? 'not-allowed' : 'pointer',
                            opacity: loading ? 0.5 : 1
                        }}
                    >
                        Hủy
                    </button>
                    <button
                        onClick={handleDelete}
                        disabled={!isConfirmed || isSelf || loading}
                        style={{
                            padding: '12px 24px',
                            background: (!isConfirmed || isSelf) ? '#FDA4AF' : '#DC2626',
                            border: 'none',
                            borderRadius: '12px',
                            fontSize: '14px',
                            fontWeight: '600',
                            color: '#fff',
                            cursor: (!isConfirmed || isSelf || loading) ? 'not-allowed' : 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '8px',
                            opacity: loading ? 0.7 : 1,
                            transition: 'background 0.2s'
                        }}
                    >
                        {loading ? (
                            <>
                                <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} />
                                Đang xóa...
                            </>
                        ) : (
                            <>
                                <Trash2 size={16} />
                                Xóa tài khoản
                            </>
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default DeleteUserModal;
