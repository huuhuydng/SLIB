import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import {
    ArrowLeft,
    Camera,
    User,
    Phone,
    Calendar,
    Lock,
    Mail,
    Shield,
    Edit3,
    X,
    Check,
    Eye,
    EyeOff,
    Hash,
    Bell,
    LogOut,
} from 'lucide-react';
import '../../styles/AccountSettings.css';
import { API_BASE_URL as BASE } from '../../config/apiConfig';
import { handleLogout as performLogout } from '../../utils/auth';
import {
    getFirstValidationMessage,
    normalizeFullName,
    normalizePhone,
    validateDob,
    validateFullName,
    validatePhone,
} from '../../utils/userValidation';

const API_BASE_URL = `${BASE}/slib`;
const getStoredToken = () => localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');
const getStoredUser = () => localStorage.getItem('librarian_user') || sessionStorage.getItem('librarian_user');

const AccountSettings = () => {
    const navigate = useNavigate();
    const fileInputRef = useRef(null);

    const [userData, setUserData] = useState({
        name: '',
        email: '',
        role: '',
        phone: '',
        dob: '',
        avatar: null,
        studentId: ''
    });

    const [isEditing, setIsEditing] = useState(false);
    const [loading, setSaving] = useState(false);
    const [success, setSuccess] = useState(null);
    const [error, setError] = useState(null);
    const [previewAvatar, setPreviewAvatar] = useState(null);

    // Password change state
    const [showPasswordModal, setShowPasswordModal] = useState(false);
    const [passwordData, setPasswordData] = useState({
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
    });
    const [showPasswords, setShowPasswords] = useState({
        current: false,
        new: false,
        confirm: false
    });
    const [passwordLoading, setPasswordLoading] = useState(false);
    const [passwordError, setPasswordError] = useState(null);

    const [isLoading, setIsLoading] = useState(true);
    const [notificationsEnabled, setNotificationsEnabled] = useState(() => {
        const saved = localStorage.getItem('slib_notifications_enabled');
        return saved !== 'false'; // Default: bật
    });

    // Load user data from API and save to session
    useEffect(() => {
        const fetchProfileData = async () => {
            try {
                const token = getStoredToken();

                // First load from localStorage for quick display
                const userStr = getStoredUser();
                if (userStr) {
                    const user = JSON.parse(userStr);
                    setUserData({
                        name: user.fullName || user.user_metadata?.full_name || user.email?.split('@')[0] || '',
                        email: user.email || '',
                        role: user.role || 'LIBRARIAN',
                        phone: user.phone || '',
                        dob: user.dob || '',
                        avatar: user.avtUrl || user.avatar || null,
                        studentId: user.studentCode || user.userCode || user.user_code || ''
                    });
                }

                // Then fetch fresh data from API
                if (token) {
                    const response = await axios.get(`${API_BASE_URL}/student-profile/me`, {
                        headers: {
                            'Authorization': `Bearer ${token}`
                        }
                    });

                    const profileData = response.data;

                    // Update state with API data
                    setUserData(prev => ({
                        ...prev,
                        name: profileData.fullName || prev.name,
                        email: profileData.email || prev.email,
                        role: profileData.role || prev.role,
                        phone: profileData.phone || prev.phone,
                        dob: profileData.dob || prev.dob,
                        avatar: profileData.avtUrl || profileData.avatarUrl || prev.avatar,
                        studentId: profileData.userCode || profileData.studentCode || prev.studentId
                    }));

                    // Save full profile to localStorage for session persistence
                    const currentUser = JSON.parse(getStoredUser() || '{}');
                    const updatedUser = {
                        ...currentUser,
                        fullName: profileData.fullName || currentUser.fullName,
                        email: profileData.email || currentUser.email,
                        role: profileData.role || currentUser.role,
                        phone: profileData.phone || currentUser.phone,
                        dob: profileData.dob || currentUser.dob,
                        avtUrl: profileData.avtUrl || profileData.avatarUrl || currentUser.avtUrl,
                        studentCode: profileData.userCode || profileData.studentCode || currentUser.studentCode,
                        userCode: profileData.userCode || profileData.studentCode || currentUser.userCode
                    };
                    localStorage.setItem('librarian_user', JSON.stringify(updatedUser));
                }
            } catch (err) {
                console.error('Error fetching profile:', err);
                // Continue with localStorage data if API fails
            } finally {
                setIsLoading(false);
            }
        };

        fetchProfileData();
    }, []);

    // Auto hide messages
    useEffect(() => {
        if (success || error) {
            const timer = setTimeout(() => {
                setSuccess(null);
                setError(null);
            }, 5000);
            return () => clearTimeout(timer);
        }
    }, [success, error]);

    const handleAvatarClick = () => {
        if (isEditing && fileInputRef.current) {
            fileInputRef.current.click();
        }
    };

    const handleAvatarChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            if (file.size > 5 * 1024 * 1024) {
                setError('Ảnh không được vượt quá 5MB');
                return;
            }
            const reader = new FileReader();
            reader.onloadend = () => {
                setPreviewAvatar(reader.result);
            };
            reader.readAsDataURL(file);
        }
    };

    const handleInputChange = (field, value) => {
        setUserData(prev => ({
            ...prev,
            [field]: value
        }));
    };

    const handleSave = async () => {
        setSaving(true);
        setError(null);

        const validationErrors = {
            fullName: validateFullName(userData.name),
            phone: validatePhone(userData.phone),
            dob: validateDob(userData.dob),
        };
        const firstError = getFirstValidationMessage(validationErrors);
        if (firstError) {
            setSaving(false);
            setError(firstError);
            return;
        }

        const normalizedFullName = normalizeFullName(userData.name);
        const normalizedPhone = normalizePhone(userData.phone);

        try {
            const token = getStoredToken();

            // Call API to update profile
            const response = await axios.put(
                `${API_BASE_URL}/student-profile/me`,
                {
                    fullName: normalizedFullName,
                    phone: normalizedPhone || null,
                    dob: userData.dob
                },
                {
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'Content-Type': 'application/json'
                    }
                }
            );

            // Update localStorage with new data
            const currentUser = JSON.parse(getStoredUser() || '{}');
            const updatedUser = {
                ...currentUser,
                fullName: normalizedFullName,
                phone: normalizedPhone || null,
                dob: userData.dob,
                avtUrl: previewAvatar || userData.avatar
            };
            localStorage.setItem('librarian_user', JSON.stringify(updatedUser));

            setUserData(prev => ({
                ...prev,
                name: normalizedFullName,
                phone: normalizedPhone || '',
            }));
            setSuccess('Cập nhật thông tin thành công!');
            setIsEditing(false);

            if (previewAvatar) {
                setUserData(prev => ({ ...prev, avatar: previewAvatar }));
                setPreviewAvatar(null);
            }
        } catch (err) {
            console.error('Lỗi lưu thông tin:', err);
            if (err.response?.status === 401 || err.response?.status === 403) {
                setError('Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.');
            } else {
                setError(err.response?.data?.message || 'Có lỗi xảy ra khi lưu thông tin. Vui lòng thử lại.');
            }
        } finally {
            setSaving(false);
        }
    };

    const handleCancel = () => {
        setIsEditing(false);
        setPreviewAvatar(null);
        const userStr = getStoredUser();
        if (userStr) {
            const user = JSON.parse(userStr);
            setUserData({
                name: user.fullName || user.user_metadata?.full_name || '',
                email: user.email || '',
                role: user.role || 'LIBRARIAN',
                phone: user.phone || '',
                dob: user.dob || '',
                avatar: user.avtUrl || user.avatar || null,
                studentId: user.studentCode || user.userCode || user.user_code || ''
            });
        }
    };

    const handlePasswordChange = async () => {
        setPasswordError(null);

        if (!passwordData.currentPassword) {
            setPasswordError('Vui lòng nhập mật khẩu hiện tại');
            return;
        }
        if (!passwordData.newPassword) {
            setPasswordError('Vui lòng nhập mật khẩu mới');
            return;
        }
        if (passwordData.newPassword.length < 6) {
            setPasswordError('Mật khẩu mới phải có ít nhất 6 ký tự');
            return;
        }
        if (passwordData.newPassword !== passwordData.confirmPassword) {
            setPasswordError('Mật khẩu xác nhận không khớp');
            return;
        }

        setPasswordLoading(true);

        try {
            const token = getStoredToken();

            // Call API to change password
            await axios.post(
                `${API_BASE_URL}/auth/change-password`,
                {
                    currentPassword: passwordData.currentPassword,
                    newPassword: passwordData.newPassword
                },
                {
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'Content-Type': 'application/json'
                    }
                }
            );

            setSuccess('Đổi mật khẩu thành công!');
            setShowPasswordModal(false);
            setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
        } catch (err) {
            console.error('Lỗi đổi mật khẩu:', err);
            if (err.response?.status === 401 || err.response?.status === 403) {
                setPasswordError('Mật khẩu hiện tại không đúng.');
            } else {
                setPasswordError(err.response?.data?.message || 'Có lỗi xảy ra. Vui lòng kiểm tra mật khẩu hiện tại.');
            }
        } finally {
            setPasswordLoading(false);
        }
    };

    const getRoleName = (role) => {
        switch (role?.toUpperCase()) {
            case 'ADMIN': return 'Quản trị viên';
            case 'LIBRARIAN': return 'Thủ thư';
            case 'TEACHER': return 'Giáo viên';
            case 'STUDENT': return 'Sinh viên';
            default: return role;
        }
    };

    const getInitials = (name) => {
        if (!name) return 'U';
        return name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2);
    };

    const handleBack = () => {
        const basePath = userData.role?.toUpperCase() === 'ADMIN' ? '/admin' : '/librarian';
        navigate(`${basePath}/dashboard`);
    };

    const handleToggleNotifications = () => {
        const newVal = !notificationsEnabled;
        setNotificationsEnabled(newVal);
        localStorage.setItem('slib_notifications_enabled', String(newVal));
    };

    const handleLogout = () => {
        performLogout();
    };

    const isAdmin = userData.role?.toUpperCase() === 'ADMIN';

    return (
        <div className="account-settings-container">
            {/* Page Header */}
            <div className="settings-page-header">
                <div className="settings-header-left">
                    <button className="settings-back-btn" onClick={handleBack}>
                        <ArrowLeft size={20} />
                    </button>
                    <div>
                        <h1>Cài đặt tài khoản</h1>
                        <p>Quản lý thông tin cá nhân và bảo mật tài khoản của bạn</p>
                    </div>
                </div>
                <div className="settings-header-actions">
                    {!isEditing ? (
                        <button className="settings-edit-btn" onClick={() => setIsEditing(true)}>
                            <Edit3 size={18} />
                            Chỉnh sửa
                        </button>
                    ) : (
                        <div className="settings-action-group">
                            <button className="settings-cancel-btn" onClick={handleCancel}>
                                <X size={18} />
                                Hủy
                            </button>
                            <button
                                className="settings-save-btn"
                                onClick={handleSave}
                                disabled={loading}
                            >
                                {loading ? (
                                    <>
                                        <span className="settings-spinner"></span>
                                        Đang lưu...
                                    </>
                                ) : (
                                    <>
                                        <Check size={18} />
                                        Lưu thay đổi
                                    </>
                                )}
                            </button>
                        </div>
                    )}
                </div>
            </div>

            {/* Alerts */}
            {success && (
                <div className="settings-toast success">
                    <Check size={18} />
                    {success}
                </div>
            )}
            {error && (
                <div className="settings-toast error">
                    <X size={18} />
                    {error}
                </div>
            )}

            {/* Main Content */}
            <div className="settings-main-content">
                {/* Left Column - Profile Card */}
                <div className="settings-profile-card">
                    <div className="profile-card-header">
                        <div className="profile-icon-container">
                            <User size={20} />
                        </div>
                        <h3>Hồ sơ</h3>
                    </div>

                    <div className="profile-avatar-section">
                        <div
                            className={`profile-avatar ${isEditing ? 'editable' : ''}`}
                            onClick={handleAvatarClick}
                        >
                            {(previewAvatar || userData.avatar) ? (
                                <img
                                    src={previewAvatar || userData.avatar}
                                    alt="Avatar"
                                />
                            ) : (
                                <span className="avatar-initials">
                                    {getInitials(userData.name)}
                                </span>
                            )}
                            {isEditing && (
                                <div className="avatar-overlay">
                                    <Camera size={20} />
                                </div>
                            )}
                        </div>
                        <input
                            ref={fileInputRef}
                            type="file"
                            accept="image/*"
                            onChange={handleAvatarChange}
                            style={{ display: 'none' }}
                        />
                        <div className="profile-info">
                            <h2>{userData.name || 'Chưa cập nhật'}</h2>
                            <span className="profile-role-badge">
                                <Shield size={14} />
                                <span>{getRoleName(userData.role)}</span>
                            </span>
                        </div>
                    </div>

                    <div className="profile-stats">
                        <div className="stat-item">
                            <Mail size={18} />
                            <span>{userData.email || 'Chưa cập nhật'}</span>
                        </div>
                        <div className="stat-item">
                            <Hash size={18} />
                            <span>{userData.studentId || 'Chưa có mã số'}</span>
                        </div>
                    </div>
                </div>

                {/* Right Column - Info Cards */}
                <div className="settings-info-column">
                    {/* Personal Info Card */}
                    <div className="settings-card">
                        <div className="card-header">
                            <div className="card-icon personal">
                                <User size={20} />
                            </div>
                            <div>
                                <h3>Thông tin cá nhân</h3>
                                <p>Thông tin cơ bản của bạn</p>
                            </div>
                        </div>

                        <div className="card-content">
                            <div className="info-item">
                                <div className="info-label">
                                    <User size={16} />
                                    <span>Họ tên</span>
                                </div>
                                <div className="info-value">{userData.name || 'Chưa cập nhật'}</div>
                            </div>

                            <div className="info-item">
                                <div className="info-label">
                                    <Phone size={16} />
                                    <span>Số điện thoại</span>
                                </div>
                                <div className="info-input-wrapper">
                                    {isEditing ? (
                                        <input
                                            type="tel"
                                            value={userData.phone}
                                            onChange={(e) => handleInputChange('phone', e.target.value)}
                                            placeholder="Nhập số điện thoại"
                                            className="info-input"
                                        />
                                    ) : (
                                        <div className="info-value">
                                            {userData.phone || 'Chưa cập nhật'}
                                        </div>
                                    )}
                                </div>
                            </div>

                            <div className="info-item">
                                <div className="info-label">
                                    <Calendar size={16} />
                                    <span>Ngày sinh</span>
                                </div>
                                <div className="info-input-wrapper">
                                    {isEditing ? (
                                        <input
                                            type="date"
                                            value={userData.dob}
                                            onChange={(e) => handleInputChange('dob', e.target.value)}
                                            className="info-input"
                                        />
                                    ) : (
                                        <div className="info-value">
                                            {userData.dob
                                                ? new Date(userData.dob).toLocaleDateString('vi-VN')
                                                : 'Chưa cập nhật'}
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Security Card */}
                    <div className="settings-card">
                        <div className="card-header">
                            <div className="card-icon security">
                                <Lock size={20} />
                            </div>
                            <div>
                                <h3>Bảo mật</h3>
                                <p>Quản lý mật khẩu và bảo mật tài khoản</p>
                            </div>
                        </div>

                        <div className="card-content">
                            <button
                                className="security-button"
                                onClick={() => setShowPasswordModal(true)}
                            >
                                <div className="security-button-content">
                                    <Lock size={18} />
                                    <div>
                                        <span className="security-title">Đổi mật khẩu</span>
                                        <span className="security-desc">Đổi mật khẩu đăng nhập của bạn</span>
                                    </div>
                                </div>
                                <ArrowLeft size={18} style={{ transform: 'rotate(180deg)' }} />
                            </button>
                        </div>
                    </div>

                    {!isAdmin && (
                        <div className="settings-card">
                            <div className="card-header">
                                <div className="card-icon" style={{ background: 'rgba(59, 130, 246, 0.1)', color: '#3b82f6' }}>
                                    <Bell size={20} />
                                </div>
                                <div>
                                    <h3>Thông báo</h3>
                                    <p>Quản lý cài đặt thông báo</p>
                                </div>
                            </div>

                            <div className="card-content">
                                <div className="info-item" style={{ cursor: 'pointer' }} onClick={handleToggleNotifications}>
                                    <div className="info-label">
                                        <Bell size={16} />
                                        <span>Hiển thị thông báo popup</span>
                                    </div>
                                    <div style={{ display: 'flex', alignItems: 'center' }}>
                                        <button
                                            className={`notif-toggle ${notificationsEnabled ? 'notif-toggle--on' : ''}`}
                                            onClick={(e) => { e.stopPropagation(); handleToggleNotifications(); }}
                                            aria-label="Toggle notifications"
                                        >
                                            <span className="notif-toggle__thumb" />
                                        </button>
                                    </div>
                                </div>
                                <p style={{ fontSize: 12, color: '#94a3b8', margin: '8px 0 0 36px' }}>
                                    Khi bật, thông báo sẽ hiện ở góc phải trên màn hình trong 5 giây mỗi khi có hoạt động mới.
                                </p>
                            </div>
                        </div>
                    )}

                    {/* Logout Card */}
                    <div className="settings-card">
                        <div className="card-header">
                            <div className="card-icon" style={{ background: 'rgba(239, 68, 68, 0.1)', color: '#ef4444' }}>
                                <LogOut size={20} />
                            </div>
                            <div>
                                <h3>Phiên đăng nhập</h3>
                                <p>Đăng xuất khỏi tài khoản</p>
                            </div>
                        </div>

                        <div className="card-content">
                            <button
                                className="security-button"
                                onClick={handleLogout}
                                style={{ borderColor: '#fecaca' }}
                            >
                                <div className="security-button-content">
                                    <LogOut size={18} style={{ color: '#ef4444' }} />
                                    <div>
                                        <span className="security-title" style={{ color: '#ef4444' }}>Đăng xuất</span>
                                        <span className="security-desc">Kết thúc phiên làm việc và quay về trang đăng nhập</span>
                                    </div>
                                </div>
                                <ArrowLeft size={18} style={{ transform: 'rotate(180deg)', color: '#ef4444' }} />
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            {/* Password Change Modal */}
            {showPasswordModal && (
                <div className="modal-overlay" onClick={() => setShowPasswordModal(false)}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h2>Đổi mật khẩu</h2>
                            <button
                                className="modal-close-btn"
                                onClick={() => setShowPasswordModal(false)}
                            >
                                <X size={20} />
                            </button>
                        </div>

                        <div className="modal-body">
                            {passwordError && (
                                <div className="password-error">
                                    <X size={16} />
                                    {passwordError}
                                </div>
                            )}

                            <div className="password-field">
                                <label>Mật khẩu hiện tại</label>
                                <div className="password-input-wrapper">
                                    <input
                                        type={showPasswords.current ? 'text' : 'password'}
                                        value={passwordData.currentPassword}
                                        onChange={(e) => setPasswordData(prev => ({
                                            ...prev,
                                            currentPassword: e.target.value
                                        }))}
                                        placeholder="Nhập mật khẩu hiện tại"
                                    />
                                    <button
                                        type="button"
                                        className="password-toggle"
                                        onClick={() => setShowPasswords(prev => ({
                                            ...prev,
                                            current: !prev.current
                                        }))}
                                    >
                                        {showPasswords.current ? <EyeOff size={18} /> : <Eye size={18} />}
                                    </button>
                                </div>
                            </div>

                            <div className="password-field">
                                <label>Mật khẩu mới</label>
                                <div className="password-input-wrapper">
                                    <input
                                        type={showPasswords.new ? 'text' : 'password'}
                                        value={passwordData.newPassword}
                                        onChange={(e) => setPasswordData(prev => ({
                                            ...prev,
                                            newPassword: e.target.value
                                        }))}
                                        placeholder="Nhập mật khẩu mới (ít nhất 6 ký tự)"
                                    />
                                    <button
                                        type="button"
                                        className="password-toggle"
                                        onClick={() => setShowPasswords(prev => ({
                                            ...prev,
                                            new: !prev.new
                                        }))}
                                    >
                                        {showPasswords.new ? <EyeOff size={18} /> : <Eye size={18} />}
                                    </button>
                                </div>
                            </div>

                            <div className="password-field">
                                <label>Xác nhận mật khẩu mới</label>
                                <div className="password-input-wrapper">
                                    <input
                                        type={showPasswords.confirm ? 'text' : 'password'}
                                        value={passwordData.confirmPassword}
                                        onChange={(e) => setPasswordData(prev => ({
                                            ...prev,
                                            confirmPassword: e.target.value
                                        }))}
                                        placeholder="Nhập lại mật khẩu mới"
                                    />
                                    <button
                                        type="button"
                                        className="password-toggle"
                                        onClick={() => setShowPasswords(prev => ({
                                            ...prev,
                                            confirm: !prev.confirm
                                        }))}
                                    >
                                        {showPasswords.confirm ? <EyeOff size={18} /> : <Eye size={18} />}
                                    </button>
                                </div>
                            </div>
                        </div>

                        <div className="modal-footer">
                            <button
                                className="modal-cancel-btn"
                                onClick={() => setShowPasswordModal(false)}
                            >
                                Hủy
                            </button>
                            <button
                                className="modal-submit-btn"
                                onClick={handlePasswordChange}
                                disabled={passwordLoading}
                            >
                                {passwordLoading ? (
                                    <>
                                        <span className="settings-spinner"></span>
                                        Đang xử lý...
                                    </>
                                ) : (
                                    'Đổi mật khẩu'
                                )}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AccountSettings;
