import React, { useState, useEffect } from 'react';
import { useToast } from '../../../components/common/ToastProvider';
import { useConfirm } from '../../../components/common/ConfirmDialog';
import {
  Settings,
  Clock,
  Star,
  AlertTriangle,
  Save,
  RotateCcw,
  Plus,
  Edit,
  Trash2,
  X,
  ChevronRight,
  Bell,
  Calendar,
  Timer,
  Award,
  MinusCircle,
  PlusCircle,
  Loader2,
  Lock,
  Unlock,
  Power
} from 'lucide-react';
import LoadErrorState from '../../../components/common/LoadErrorState';


import { API_BASE_URL as BASE } from '../../../config/apiConfig';

const API_BASE_URL = `${BASE}/slib/settings`;
const REPUTATION_API_URL = `${BASE}/slib/admin/reputation-rules`;
const DEFAULT_LIBRARY_CONFIG = {
  openTime: '07:00',
  closeTime: '21:00',
  slotDuration: 60,
  maxBookingsPerDay: 3,
  maxActiveBookings: 2,
  maxHoursPerDay: 4,
  maxBookingDays: 14,
  workingDays: '2,3,4,5,6',
  autoCancelMinutes: 15,
  autoCancelOnLeaveMinutes: 30,
  seatConfirmationLeadMinutes: 15,
  bookingReminderLeadMinutes: 15,
  expiryWarningLeadMinutes: 10,
  bookingCancelDeadlineHours: 12,
  minReputation: 0,
  notifyBookingSuccess: true,
  notifyCheckinReminder: true,
  notifyTimeExpiry: true,
  notifyViolation: true,
  notifyWeeklyReport: false,
  notifyDeviceAlert: true,
};

const getAuthHeaders = () => {
  const token = sessionStorage.getItem('librarian_token') || localStorage.getItem('librarian_token');
  return token ? { Authorization: `Bearer ${token}` } : {};
};

const parseApiError = async (response, fallbackMessage) => {
  try {
    const data = await response.json();
    if (data?.errors && typeof data.errors === 'object') {
      return Object.values(data.errors).join('\n');
    }
    if (data?.message) {
      return data.message;
    }
  } catch {
    // Ignore JSON parse failures and return fallback.
  }
  return fallbackMessage;
};

const getNowDateTimeLocalInput = () => {
  const now = new Date();
  now.setSeconds(0, 0);
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 16);
};

const toDateTimeLocalInput = (value) => {
  if (!value) return '';
  const normalized = String(value);
  return normalized.length >= 16 ? normalized.slice(0, 16) : '';
};

const formatDateTimeDisplay = (value) => {
  if (!value) return '';
  try {
    const normalized = String(value).length === 16 ? `${value}:00` : value;
    return new Date(normalized).toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return value;
  }
};

const addDaysToDateTimeInput = (startValue, days) => {
  if (!startValue) return '';
  const start = new Date(startValue);
  if (Number.isNaN(start.getTime())) return '';
  const next = new Date(start);
  next.setDate(next.getDate() + Math.max(1, Number(days) || 1));
  const local = new Date(next.getTime() - next.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 16);
};

const toLocalDateTimePayload = (value) => (value ? `${value}:00` : null);

const resolveClosureDurationDays = (startValue, endValue) => {
  if (!startValue || !endValue) return 1;
  const start = new Date(startValue);
  const end = new Date(endValue);
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return 1;
  const diffDays = Math.round((end.getTime() - start.getTime()) / (24 * 60 * 60 * 1000));
  return Math.max(1, diffDays || 1);
};

const SystemConfig = () => {
  const toast = useToast();
  const { confirm } = useConfirm();
  const [activeTab, setActiveTab] = useState('library');
  const [showRuleModal, setShowRuleModal] = useState(false);
  const [editingRule, setEditingRule] = useState(null);
  const [ruleType, setRuleType] = useState('violation');
  const [loading, setLoading] = useState(true);
  const [settingsError, setSettingsError] = useState('');
  const [saving, setSaving] = useState(false);
  const [toggling, setToggling] = useState(false);
  const [resetting, setResetting] = useState(false);

  // Reputation Rules State
  const [reputationRules, setReputationRules] = useState([]);
  const [rulesLoading, setRulesLoading] = useState(false);
  const [ruleForm, setRuleForm] = useState({ ruleCode: '', ruleName: '', description: '', points: '', ruleType: 'PENALTY', isActive: true });

  // Library Lock State
  const [libraryClosed, setLibraryClosed] = useState(false);
  const [closedReason, setClosedReason] = useState('');
  const [scheduledClosedFromAt, setScheduledClosedFromAt] = useState('');
  const [scheduledClosedUntilAt, setScheduledClosedUntilAt] = useState('');
  const [closureFormStartAt, setClosureFormStartAt] = useState(getNowDateTimeLocalInput());
  const [closureDurationDays, setClosureDurationDays] = useState(1);

  // Library Config State
  const [libraryConfig, setLibraryConfig] = useState(DEFAULT_LIBRARY_CONFIG);

  const applyLibrarySettings = (data) => {
    setLibraryClosed(data.libraryClosed || false);
    setClosedReason(data.closedReason || '');
    setScheduledClosedFromAt(data.closedFromAt || '');
    setScheduledClosedUntilAt(data.closedUntilAt || '');
    setClosureFormStartAt(toDateTimeLocalInput(data.closedFromAt) || getNowDateTimeLocalInput());
    setClosureDurationDays(resolveClosureDurationDays(data.closedFromAt, data.closedUntilAt));
    setLibraryConfig({
      openTime: data.openTime || DEFAULT_LIBRARY_CONFIG.openTime,
      closeTime: data.closeTime || DEFAULT_LIBRARY_CONFIG.closeTime,
      slotDuration: data.slotDuration ?? DEFAULT_LIBRARY_CONFIG.slotDuration,
      maxBookingsPerDay: data.maxBookingsPerDay ?? DEFAULT_LIBRARY_CONFIG.maxBookingsPerDay,
      maxActiveBookings: data.maxActiveBookings ?? DEFAULT_LIBRARY_CONFIG.maxActiveBookings,
      maxHoursPerDay: data.maxHoursPerDay ?? DEFAULT_LIBRARY_CONFIG.maxHoursPerDay,
      maxBookingDays: data.maxBookingDays ?? DEFAULT_LIBRARY_CONFIG.maxBookingDays,
      workingDays: data.workingDays || DEFAULT_LIBRARY_CONFIG.workingDays,
      autoCancelMinutes: data.autoCancelMinutes ?? DEFAULT_LIBRARY_CONFIG.autoCancelMinutes,
      autoCancelOnLeaveMinutes: data.autoCancelOnLeaveMinutes ?? DEFAULT_LIBRARY_CONFIG.autoCancelOnLeaveMinutes,
      seatConfirmationLeadMinutes: data.seatConfirmationLeadMinutes ?? DEFAULT_LIBRARY_CONFIG.seatConfirmationLeadMinutes,
      bookingReminderLeadMinutes: data.bookingReminderLeadMinutes ?? DEFAULT_LIBRARY_CONFIG.bookingReminderLeadMinutes,
      expiryWarningLeadMinutes: data.expiryWarningLeadMinutes ?? DEFAULT_LIBRARY_CONFIG.expiryWarningLeadMinutes,
      bookingCancelDeadlineHours: data.bookingCancelDeadlineHours ?? DEFAULT_LIBRARY_CONFIG.bookingCancelDeadlineHours,
      minReputation: data.minReputation ?? DEFAULT_LIBRARY_CONFIG.minReputation,
      notifyBookingSuccess: data.notifyBookingSuccess ?? DEFAULT_LIBRARY_CONFIG.notifyBookingSuccess,
      notifyCheckinReminder: data.notifyCheckinReminder ?? DEFAULT_LIBRARY_CONFIG.notifyCheckinReminder,
      notifyTimeExpiry: data.notifyTimeExpiry ?? DEFAULT_LIBRARY_CONFIG.notifyTimeExpiry,
      notifyViolation: data.notifyViolation ?? DEFAULT_LIBRARY_CONFIG.notifyViolation,
      notifyWeeklyReport: data.notifyWeeklyReport ?? DEFAULT_LIBRARY_CONFIG.notifyWeeklyReport,
      notifyDeviceAlert: data.notifyDeviceAlert ?? DEFAULT_LIBRARY_CONFIG.notifyDeviceAlert,
    });
  };

  const fetchSettings = async ({ showErrorToast = false } = {}) => {
    setLoading(true);
    setSettingsError('');
    try {
      const response = await fetch(`${API_BASE_URL}/library`, { headers: getAuthHeaders() });
      if (!response.ok) {
        const message = await parseApiError(response, 'Không thể tải cấu hình thư viện');
        throw new Error(message);
      }

      const data = await response.json();
      applyLibrarySettings(data);
    } catch (error) {
      console.error('Error fetching settings:', error);
      const message = error?.message || 'Không thể tải cấu hình thư viện';
      setSettingsError(message);
      if (showErrorToast) {
        toast.error(message);
      }
    } finally {
      setLoading(false);
    }
  };

  // Load settings from API on mount
  useEffect(() => {
    fetchSettings();
  }, []);

  // Fetch reputation rules from API
  const fetchReputationRules = async () => {
    setRulesLoading(true);
    try {
      const response = await fetch(REPUTATION_API_URL, { headers: getAuthHeaders() });
      if (response.ok) {
        const data = await response.json();
        setReputationRules(data);
      }
    } catch (error) {
      console.error('Error fetching reputation rules:', error);
    } finally {
      setRulesLoading(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'reputation') fetchReputationRules();
  }, [activeTab]);

  const handleOpenRuleModal = (type, rule = null) => {
    setRuleType(type);
    if (rule) {
      setEditingRule(rule);
      setRuleForm({
        ruleCode: rule.ruleCode || '',
        ruleName: rule.ruleName || '',
        description: rule.description || '',
        points: Math.abs(rule.points),
        ruleType: rule.ruleType || (type === 'violation' ? 'PENALTY' : 'REWARD'),
        isActive: rule.isActive !== false,
      });
    } else {
      setEditingRule(null);
      setRuleForm({ ruleCode: '', ruleName: '', description: '', points: '', ruleType: type === 'violation' ? 'PENALTY' : 'REWARD', isActive: true });
    }
    setShowRuleModal(true);
  };

  const handleSaveRule = async () => {
    const payload = {
      ruleCode: ruleForm.ruleCode,
      ruleName: ruleForm.ruleName,
      description: ruleForm.description,
      points: ruleForm.ruleType === 'PENALTY' ? -Math.abs(parseInt(ruleForm.points)) : Math.abs(parseInt(ruleForm.points)),
      ruleType: ruleForm.ruleType,
      isActive: ruleForm.isActive,
    };
    try {
      const url = editingRule ? `${REPUTATION_API_URL}/${editingRule.id}` : REPUTATION_API_URL;
      const method = editingRule ? 'PUT' : 'POST';
      const response = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(payload),
      });
      if (response.ok) {
        setShowRuleModal(false);
        fetchReputationRules();
      } else {
        toast.error(editingRule ? 'Lỗi khi cập nhật quy tắc' : 'Lỗi khi tạo quy tắc (mã đã tồn tại?)');
      }
    } catch (error) {
      console.error('Error saving rule:', error);
      toast.error('Lỗi kết nối server');
    }
  };

  const handleToggleRule = async (ruleId) => {
    try {
      const response = await fetch(`${REPUTATION_API_URL}/${ruleId}/toggle`, { method: 'PATCH', headers: getAuthHeaders() });
      if (response.ok) fetchReputationRules();
    } catch (error) {
      console.error('Error toggling rule:', error);
    }
  };

  const handleDeleteRule = async (ruleId) => {
    const confirmed = await confirm({
      title: 'Xóa quy tắc',
      message: 'Bạn có chắc muốn xóa quy tắc này?',
      variant: 'danger',
      confirmText: 'Xoá',
    });
    if (!confirmed) return;
    try {
      const response = await fetch(`${REPUTATION_API_URL}/${ruleId}`, { method: 'DELETE', headers: getAuthHeaders() });
      if (response.ok) fetchReputationRules();
    } catch (error) {
      console.error('Error deleting rule:', error);
    }
  };

  // Save settings to API
  const handleSave = async () => {
    setSaving(true);
    try {
      const response = await fetch(`${API_BASE_URL}/library`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(libraryConfig),
      });
      if (response.ok) {
        const data = await response.json();
        applyLibrarySettings(data);
        toast.success('Lưu cài đặt thành công!');
      } else {
        toast.error(await parseApiError(response, 'Lỗi khi lưu cài đặt'));
      }
    } catch (error) {
      console.error('Error saving settings:', error);
      toast.error(error?.message || 'Lỗi kết nối server');
    } finally {
      setSaving(false);
    }
  };

  const tabs = [
    { id: 'library', label: 'Tham số thư viện', icon: Clock },
    { id: 'reputation', label: 'Quy tắc điểm uy tín', icon: Star },
    { id: 'notifications', label: 'Thông báo', icon: Bell },
  ];

  const handleConfigChange = (key, value) => {
    setLibraryConfig(prev => ({ ...prev, [key]: value }));
  };

  const previewClosedUntilAt = addDaysToDateTimeInput(closureFormStartAt, closureDurationDays);
  const hasClosureSchedule = Boolean(scheduledClosedFromAt || scheduledClosedUntilAt);
  const isFutureClosureScheduled = hasClosureSchedule && !libraryClosed;

  const handleScheduleLibraryClosure = async () => {
    if (!closedReason.trim()) {
      toast.warning('Vui lòng nhập lý do tạm đóng thư viện');
      return;
    }
    if (!closureFormStartAt) {
      toast.warning('Vui lòng chọn thời điểm bắt đầu tạm đóng');
      return;
    }
    if (!previewClosedUntilAt) {
      toast.warning('Không thể tính được thời điểm tự mở lại');
      return;
    }

    setToggling(true);
    try {
      const response = await fetch(`${API_BASE_URL}/library/toggle-lock`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify({
          closed: true,
          reason: closedReason.trim(),
          closedFrom: toLocalDateTimePayload(closureFormStartAt),
          closedUntil: toLocalDateTimePayload(previewClosedUntilAt),
        }),
      });
      if (response.ok) {
        const data = await response.json();
        applyLibrarySettings(data);
        toast.success(isFutureClosureScheduled ? 'Đã cập nhật lịch tạm đóng thư viện' : 'Đã lên lịch tạm đóng thư viện');
      } else {
        toast.error(await parseApiError(response, 'Lỗi khi lên lịch tạm đóng thư viện'));
      }
    } catch (error) {
      console.error('Error scheduling library closure:', error);
      toast.error(error?.message || 'Lỗi kết nối server');
    } finally {
      setToggling(false);
    }
  };

  const handleClearLibraryClosure = async () => {
    const confirmed = await confirm({
      title: libraryClosed ? 'Mở lại thư viện' : 'Hủy lịch tạm đóng',
      message: libraryClosed
        ? 'Thư viện sẽ được mở lại ngay lập tức. Bạn có muốn tiếp tục không?'
        : 'Lịch tạm đóng thư viện đang chờ sẽ bị hủy. Bạn có muốn tiếp tục không?',
      confirmText: libraryClosed ? 'Mở lại ngay' : 'Hủy lịch',
      variant: libraryClosed ? 'warning' : 'danger',
    });

    if (!confirmed) return;

    setToggling(true);
    try {
      const response = await fetch(`${API_BASE_URL}/library/toggle-lock`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify({ closed: false, reason: null, closedFrom: null, closedUntil: null }),
      });
      if (response.ok) {
        const data = await response.json();
        applyLibrarySettings(data);
        toast.success(libraryClosed ? 'Đã mở lại thư viện' : 'Đã hủy lịch tạm đóng thư viện');
      } else {
        toast.error(await parseApiError(response, 'Lỗi khi cập nhật trạng thái thư viện'));
      }
    } catch (error) {
      console.error('Error clearing library closure:', error);
      toast.error(error?.message || 'Lỗi kết nối server');
    } finally {
      setToggling(false);
    }
  };

  const handleResetDefaults = async () => {
    const confirmed = await confirm({
      title: 'Khôi phục mặc định',
      message: 'Thao tác này sẽ đưa toàn bộ cấu hình thư viện và thông báo về mặc định. Bạn có muốn tiếp tục không?',
      confirmText: 'Khôi phục',
      variant: 'warning',
    });

    if (!confirmed) return;

    setResetting(true);
    try {
      const response = await fetch(`${API_BASE_URL}/library/reset`, {
        method: 'POST',
        headers: getAuthHeaders(),
      });

      if (!response.ok) {
        toast.error(await parseApiError(response, 'Không thể khôi phục cấu hình mặc định'));
        return;
      }

      const data = await response.json();
      applyLibrarySettings(data);
      toast.success('Đã khôi phục cấu hình mặc định');
    } catch (error) {
      console.error('Error resetting settings:', error);
      toast.error(error?.message || 'Lỗi kết nối server');
    } finally {
      setResetting(false);
    }
  };

  return (
    <>

      <div style={{
        padding: '0 24px 32px',
        maxWidth: '1440px',
        margin: '0 auto',
        minHeight: 'calc(100vh - 120px)'
      }}>
        {/* Page Header */}
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '24px'
        }}>
          <div>
            <h1 style={{ fontSize: '20px', fontWeight: '600', color: '#1A1A1A', margin: '0 0 4px 0' }}>
              Cấu hình hệ thống
            </h1>
            <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>
              Thiết lập các tham số vận hành của SLIB
            </p>
          </div>
          {activeTab !== 'reputation' && (
            <div style={{ display: 'flex', gap: '12px' }}>
              <button style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '12px 20px',
              background: '#F7FAFC',
              border: '2px solid #E2E8F0',
              borderRadius: '12px',
              fontSize: '14px',
              fontWeight: '600',
              color: '#4A5568',
              cursor: resetting ? 'not-allowed' : 'pointer',
              opacity: resetting ? 0.7 : 1
            }}
              onClick={handleResetDefaults}
              disabled={resetting || loading}
              >
                {resetting ? <Loader2 size={18} className="animate-spin" /> : <RotateCcw size={18} />}
                {resetting ? 'Đang khôi phục...' : 'Khôi phục mặc định'}
              </button>
              <button
                onClick={handleSave}
                disabled={saving || loading}
                style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '12px 20px',
                background: saving ? '#ccc' : '#e8600a',
                border: 'none',
                borderRadius: '12px',
                fontSize: '14px',
                fontWeight: '600',
                color: '#fff',
                cursor: saving || loading ? 'not-allowed' : 'pointer',
                boxShadow: '0 4px 14px rgba(255, 117, 31, 0.25)'
                }}>
                {saving ? <Loader2 size={18} className="animate-spin" /> : <Save size={18} />}
                {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
              </button>
            </div>
          )}
        </div>

        {/* Main Content */}
        <div style={{ display: 'flex', gap: '24px' }}>
          {/* Sidebar Tabs */}
          <div style={{
            width: '280px',
            flexShrink: 0,
            background: '#fff',
            borderRadius: '10px',
            padding: '16px',
            boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            height: 'fit-content'
          }}>
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                style={{
                  width: '100%',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  padding: '14px 16px',
                  background: activeTab === tab.id ? '#fef6f0' : 'transparent',
                  border: activeTab === tab.id ? '2px solid #e8600a' : '2px solid transparent',
                  borderRadius: '12px',
                  fontSize: '14px',
                  fontWeight: activeTab === tab.id ? '600' : '500',
                  color: activeTab === tab.id ? '#e8600a' : '#4A5568',
                  cursor: 'pointer',
                  marginBottom: '8px',
                  textAlign: 'left',
                  transition: 'all 0.2s ease'
                }}
              >
                <tab.icon size={20} />
                {tab.label}
                {activeTab === tab.id && <ChevronRight size={16} style={{ marginLeft: 'auto' }} />}
              </button>
            ))}
          </div>

          {/* Content Area */}
          <div style={{ flex: 1 }}>
            {loading && (
              <div style={{
                background: '#fff',
                borderRadius: '10px',
                boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
                padding: '48px 24px',
                textAlign: 'center',
                color: '#718096'
              }}>
                <Loader2 size={28} className="animate-spin" style={{ margin: '0 auto 12px' }} />
                Đang tải cấu hình thư viện...
              </div>
            )}

            {!loading && settingsError && activeTab !== 'reputation' && (
              <div style={{
                background: '#fff',
                borderRadius: '10px',
                boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
              }}>
                <LoadErrorState
                  title="Không thể tải cấu hình hệ thống"
                  message={settingsError}
                  onRetry={() => fetchSettings({ showErrorToast: true })}
                  retryLabel="Tải lại"
                  compact
                />
              </div>
            )}

            {/* Library Parameters Tab */}
            {!loading && !settingsError && activeTab === 'library' && (
              <div style={{
                background: '#fff',
                borderRadius: '10px',
                boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
                overflow: 'hidden'
              }}>
                <div style={{ padding: '24px', borderBottom: '1px solid #E2E8F0' }}>
                  <h2 style={{ fontSize: '18px', fontWeight: '600', color: '#1A1A1A', margin: '0 0 4px 0' }}>
                    Tham số thư viện
                  </h2>
                  <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>
                    Cấu hình giờ hoạt động và các quy định đặt chỗ
                  </p>
                </div>
                <div style={{ padding: '24px' }}>
                    {/* Library Lock Schedule */}
                  <div style={{
                    marginBottom: '32px',
                    padding: '20px',
                    background: libraryClosed
                      ? 'linear-gradient(135deg, #FFF5F5, #FED7D7)'
                      : hasClosureSchedule
                        ? 'linear-gradient(135deg, #FFFAF0, #FEEBC8)'
                        : 'linear-gradient(135deg, #F0FFF4, #C6F6D5)',
                    borderRadius: '16px',
                    border: libraryClosed
                      ? '2px solid #FC8181'
                      : hasClosureSchedule
                        ? '2px solid #F6AD55'
                        : '2px solid #68D391',
                    transition: 'all 0.3s ease'
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <div style={{
                          width: '44px',
                          height: '44px',
                          borderRadius: '12px',
                          background: libraryClosed ? '#FC8181' : hasClosureSchedule ? '#F6AD55' : '#68D391',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          transition: 'background 0.3s ease'
                        }}>
                          {libraryClosed ? <Lock size={22} color="#fff" /> : hasClosureSchedule ? <Calendar size={22} color="#fff" /> : <Unlock size={22} color="#fff" />}
                        </div>
                        <div>
                          <h3 style={{
                            fontSize: '16px',
                            fontWeight: '700',
                            color: libraryClosed ? '#C53030' : hasClosureSchedule ? '#C05621' : '#276749',
                            margin: '0 0 2px 0'
                          }}>
                            {libraryClosed
                              ? 'Thư viện đang tạm đóng'
                              : hasClosureSchedule
                                ? 'Đã lên lịch tạm đóng thư viện'
                                : 'Thư viện đang hoạt động'}
                          </h3>
                          <p style={{
                            fontSize: '13px',
                            color: libraryClosed ? '#E53E3E' : hasClosureSchedule ? '#DD6B20' : '#38A169',
                            margin: 0,
                            fontWeight: '500'
                          }}>
                            {libraryClosed
                              ? (scheduledClosedUntilAt
                                  ? `Tự mở lại vào ${formatDateTimeDisplay(scheduledClosedUntilAt)}`
                                  : 'Sinh viên không thể đặt chỗ cho đến khi admin mở lại')
                              : hasClosureSchedule
                                ? `Dự kiến đóng từ ${formatDateTimeDisplay(scheduledClosedFromAt)}`
                                : 'Sinh viên có thể đặt chỗ bình thường'}
                          </p>
                        </div>
                      </div>
                    </div>
                    {closedReason && (
                      <div style={{
                        marginTop: '8px',
                        padding: '10px 14px',
                        background: 'rgba(255,255,255,0.7)',
                        borderRadius: '10px',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px'
                      }}>
                        <AlertTriangle size={16} color="#E53E3E" />
                        <span style={{ fontSize: '13px', color: '#742A2A', fontWeight: '500' }}>
                          Lý do: {closedReason}
                        </span>
                      </div>
                    )}

                    {!libraryClosed && (
                      <div style={{ marginTop: '16px', display: 'grid', gridTemplateColumns: '1.5fr 1fr', gap: '16px' }}>
                        <div style={{ gridColumn: 'span 2' }}>
                          <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '6px' }}>
                            Lý do tạm đóng
                          </label>
                          <input
                            type="text"
                            value={closedReason}
                            onChange={(e) => setClosedReason(e.target.value)}
                            placeholder="VD: Bảo trì hệ thống điện, Sự kiện đặc biệt..."
                            style={{
                              width: '100%',
                              padding: '10px 14px',
                              border: '2px solid #E2E8F0',
                              borderRadius: '10px',
                              fontSize: '14px',
                              outline: 'none',
                              background: '#fff',
                              boxSizing: 'border-box'
                            }}
                          />
                        </div>

                        <div>
                          <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '6px' }}>
                            Bắt đầu tạm đóng
                          </label>
                          <input
                            type="datetime-local"
                            value={closureFormStartAt}
                            min={getNowDateTimeLocalInput()}
                            onChange={(e) => setClosureFormStartAt(e.target.value)}
                            style={{
                              width: '100%',
                              padding: '10px 14px',
                              border: '2px solid #E2E8F0',
                              borderRadius: '10px',
                              fontSize: '14px',
                              outline: 'none',
                              background: '#fff',
                              boxSizing: 'border-box'
                            }}
                          />
                        </div>

                        <div>
                          <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '6px' }}>
                            Tạm đóng trong bao nhiêu ngày
                          </label>
                          <input
                            type="number"
                            min="1"
                            max="30"
                            value={closureDurationDays}
                            onChange={(e) => setClosureDurationDays(Math.max(1, parseInt(e.target.value || '1', 10)))}
                            style={{
                              width: '100%',
                              padding: '10px 14px',
                              border: '2px solid #E2E8F0',
                              borderRadius: '10px',
                              fontSize: '14px',
                              outline: 'none',
                              background: '#fff',
                              boxSizing: 'border-box'
                            }}
                          />
                        </div>

                        <div style={{
                          gridColumn: 'span 2',
                          padding: '10px 14px',
                          background: '#fff',
                          borderRadius: '10px',
                          border: '1px dashed #F6AD55',
                          fontSize: '13px',
                          color: '#7B341E',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '8px'
                        }}>
                          <Timer size={16} color="#DD6B20" />
                          {previewClosedUntilAt
                            ? `Hệ thống sẽ tự mở lại vào ${formatDateTimeDisplay(previewClosedUntilAt)}.`
                            : 'Vui lòng chọn thời điểm bắt đầu hợp lệ để hệ thống tính thời điểm tự mở lại.'}
                        </div>

                        <div style={{ gridColumn: 'span 2', display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                          <button
                            onClick={handleScheduleLibraryClosure}
                            disabled={toggling}
                            style={{
                              display: 'flex',
                              alignItems: 'center',
                              gap: '8px',
                              padding: '10px 20px',
                              background: hasClosureSchedule ? '#DD6B20' : '#E53E3E',
                              color: '#fff',
                              border: 'none',
                              borderRadius: '10px',
                              fontSize: '14px',
                              fontWeight: '600',
                              cursor: toggling ? 'not-allowed' : 'pointer',
                              opacity: toggling ? 0.7 : 1,
                              transition: 'all 0.2s ease'
                            }}
                          >
                            {toggling ? <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} /> : <Power size={16} />}
                            {hasClosureSchedule ? 'Cập nhật lịch tạm đóng' : 'Lên lịch tạm đóng'}
                          </button>

                          {hasClosureSchedule && (
                            <button
                              onClick={handleClearLibraryClosure}
                              disabled={toggling}
                              style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: '8px',
                                padding: '10px 20px',
                                background: '#fff',
                                color: '#C53030',
                                border: '2px solid #FC8181',
                                borderRadius: '10px',
                                fontSize: '14px',
                                fontWeight: '600',
                                cursor: toggling ? 'not-allowed' : 'pointer',
                                opacity: toggling ? 0.7 : 1
                              }}
                            >
                              <X size={16} />
                              Hủy lịch tạm đóng
                            </button>
                          )}
                        </div>
                      </div>
                    )}

                    {libraryClosed && (
                      <div style={{ marginTop: '16px', display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                        {scheduledClosedFromAt && (
                          <div style={{
                            padding: '10px 14px',
                            background: 'rgba(255,255,255,0.72)',
                            borderRadius: '10px',
                            fontSize: '13px',
                            color: '#742A2A',
                            fontWeight: '500'
                          }}>
                            Bắt đầu: {formatDateTimeDisplay(scheduledClosedFromAt)}
                          </div>
                        )}
                        {scheduledClosedUntilAt && (
                          <div style={{
                            padding: '10px 14px',
                            background: 'rgba(255,255,255,0.72)',
                            borderRadius: '10px',
                            fontSize: '13px',
                            color: '#742A2A',
                            fontWeight: '500'
                          }}>
                            Tự mở lại: {formatDateTimeDisplay(scheduledClosedUntilAt)}
                          </div>
                        )}
                        <button
                          onClick={handleClearLibraryClosure}
                          disabled={toggling}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '8px',
                            padding: '10px 20px',
                            background: '#38A169',
                            color: '#fff',
                            border: 'none',
                            borderRadius: '10px',
                            fontSize: '14px',
                            fontWeight: '600',
                            cursor: toggling ? 'not-allowed' : 'pointer',
                            opacity: toggling ? 0.7 : 1,
                            transition: 'all 0.2s ease'
                          }}
                        >
                          {toggling ? <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} /> : <Unlock size={16} />}
                          Mở lại thư viện ngay
                        </button>
                      </div>
                    )}
                  </div>

                  {/* Operating Hours */}
                  <div style={{ marginBottom: '32px' }}>
                    <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#1A1A1A', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Clock size={18} color="#e8600a" />
                      Giờ hoạt động
                    </h3>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Giờ mở cửa
                        </label>
                        <input
                          type="time"
                          value={libraryConfig.openTime}
                          onChange={(e) => handleConfigChange('openTime', e.target.value)}
                          style={{
                            width: '100%',
                            padding: '12px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            outline: 'none'
                          }}
                        />
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Giờ đóng cửa
                        </label>
                        <input
                          type="time"
                          value={libraryConfig.closeTime}
                          onChange={(e) => handleConfigChange('closeTime', e.target.value)}
                          style={{
                            width: '100%',
                            padding: '12px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            outline: 'none'
                          }}
                        />
                      </div>
                    </div>
                  </div>

                  {/* Booking Rules */}
                  <div style={{ marginBottom: '32px' }}>
                    <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#1A1A1A', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Calendar size={18} color="#e8600a" />
                      Quy định đặt chỗ
                    </h3>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Thời lượng mỗi ca đặt chỗ (phút)
                        </label>
                        <input
                          type="number"
                          value={libraryConfig.slotDuration}
                          onChange={(e) => handleConfigChange('slotDuration', parseInt(e.target.value))}
                          style={{
                            width: '100%',
                            padding: '12px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            outline: 'none'
                          }}
                        />
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Đặt trước tối đa (ngày)
                        </label>
                        <input
                          type="number"
                          value={libraryConfig.maxBookingDays}
                          onChange={(e) => handleConfigChange('maxBookingDays', parseInt(e.target.value))}
                          style={{
                            width: '100%',
                            padding: '12px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            outline: 'none'
                          }}
                        />
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Số lượt tối đa trong cùng một ngày sử dụng
                        </label>
                        <input
                          type="number"
                          value={libraryConfig.maxBookingsPerDay}
                          onChange={(e) => handleConfigChange('maxBookingsPerDay', parseInt(e.target.value))}
                          style={{
                            width: '100%',
                            padding: '12px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            outline: 'none'
                          }}
                        />
                        <div style={{ fontSize: '12px', color: '#A0AEC0', marginTop: '4px' }}>
                          Chỉ tính các booking có cùng ngày sử dụng, không phải tổng số booking sắp tới
                        </div>
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Số booking sắp tới tối đa/người
                        </label>
                        <input
                          type="number"
                          value={libraryConfig.maxActiveBookings}
                          onChange={(e) => handleConfigChange('maxActiveBookings', parseInt(e.target.value))}
                          style={{
                            width: '100%',
                            padding: '12px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            outline: 'none'
                          }}
                        />
                        <div style={{ fontSize: '12px', color: '#A0AEC0', marginTop: '4px' }}>
                          Giới hạn tổng số booking chưa diễn ra hoặc chưa hoàn tất để tránh giữ chỗ liên tiếp trên nhiều ngày
                        </div>
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Tổng số giờ tối đa trong cùng một ngày sử dụng
                        </label>
                        <input
                          type="number"
                          value={libraryConfig.maxHoursPerDay}
                          onChange={(e) => handleConfigChange('maxHoursPerDay', parseInt(e.target.value))}
                          style={{
                            width: '100%',
                            padding: '12px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            outline: 'none'
                          }}
                        />
                        <div style={{ fontSize: '12px', color: '#A0AEC0', marginTop: '4px' }}>
                          Cộng tổng thời lượng các booking trong cùng ngày sử dụng
                        </div>
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Xác nhận ghế trước giờ bắt đầu (phút)
                        </label>
                        <input
                          type="number"
                          min="0"
                          value={libraryConfig.seatConfirmationLeadMinutes}
                          onChange={(e) => handleConfigChange('seatConfirmationLeadMinutes', parseInt(e.target.value))}
                          style={{
                            width: '100%',
                            padding: '12px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            outline: 'none'
                          }}
                        />
                        <div style={{ fontSize: '12px', color: '#A0AEC0', marginTop: '4px' }}>
                          Sinh viên được quét NFC để xác nhận ghế sớm hơn số phút này
                        </div>
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Hủy chỗ trước giờ bắt đầu (giờ)
                        </label>
                        <input
                          type="number"
                          min="1"
                          value={libraryConfig.bookingCancelDeadlineHours}
                          onChange={(e) => handleConfigChange('bookingCancelDeadlineHours', parseInt(e.target.value))}
                          style={{
                            width: '100%',
                            padding: '12px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            outline: 'none'
                          }}
                        />
                        <div style={{ fontSize: '12px', color: '#A0AEC0', marginTop: '4px' }}>
                          Sinh viên phải hủy trước số giờ này, trừ các trường hợp được hệ thống cho phép ngoại lệ
                        </div>
                      </div>
                      <div style={{ gridColumn: 'span 2' }}>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Các ngày phục vụ (1=CN, 2=T2, ..., 7=T7)
                        </label>
                        <input
                          type="text"
                          value={libraryConfig.workingDays}
                          onChange={(e) => handleConfigChange('workingDays', e.target.value)}
                          placeholder="VD: 2,3,4,5,6"
                          style={{
                            width: '100%',
                            padding: '12px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            outline: 'none'
                          }}
                        />
                      </div>
                    </div>
                  </div>

                  {/* Tự động hủy & Uy tín */}
                  <div style={{ marginBottom: '32px' }}>
                    <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#1A1A1A', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Timer size={18} color="#e8600a" />
                      Tự động hủy & Uy tín
                    </h3>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Tự hủy sau (phút) nếu không xác nhận ghế
                        </label>
                        <input
                          type="number"
                          value={libraryConfig.autoCancelMinutes}
                          onChange={(e) => handleConfigChange('autoCancelMinutes', parseInt(e.target.value))}
                          style={{
                            width: '100%',
                            padding: '12px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            outline: 'none'
                          }}
                        />
                        <div style={{ fontSize: '12px', color: '#A0AEC0', marginTop: '4px' }}>
                          Đặt chỗ sẽ bị hủy nếu không xác nhận ghế trong thời gian này
                        </div>
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Tự hủy sau (phút) khi rời chỗ
                        </label>
                        <input
                          type="number"
                          value={libraryConfig.autoCancelOnLeaveMinutes}
                          onChange={(e) => handleConfigChange('autoCancelOnLeaveMinutes', parseInt(e.target.value))}
                          style={{
                            width: '100%',
                            padding: '12px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            outline: 'none'
                          }}
                        />
                        <div style={{ fontSize: '12px', color: '#A0AEC0', marginTop: '4px' }}>
                          Đặt chỗ sẽ bị hủy nếu rời chỗ quá lâu
                        </div>
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Điểm uy tín tối thiểu để đặt chỗ
                        </label>
                        <input
                          type="number"
                          value={libraryConfig.minReputation}
                          onChange={(e) => handleConfigChange('minReputation', parseInt(e.target.value))}
                          style={{
                            width: '100%',
                            padding: '12px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            outline: 'none'
                          }}
                        />
                        <div style={{ fontSize: '12px', color: '#A0AEC0', marginTop: '4px' }}>
                          0 = không giới hạn. Sinh viên cần đạt mức tối thiểu để đặt chỗ
                        </div>
                      </div>
                    </div>
                  </div>

                </div>
              </div>
            )}

            {/* Reputation Rules Tab */}
            {activeTab === 'reputation' && (
              <div style={{
                background: '#fff',
                borderRadius: '10px',
                boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
                overflow: 'hidden'
              }}>
                <div style={{ padding: '24px', borderBottom: '1px solid #E2E8F0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <h2 style={{ fontSize: '18px', fontWeight: '600', color: '#1A1A1A', margin: '0 0 4px 0' }}>
                      Quy tắc điểm uy tín
                    </h2>
                    <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>
                      Cấu hình điểm thưởng và điểm phạt cho sinh viên
                    </p>
                  </div>
                  <button
                    onClick={() => handleOpenRuleModal('violation')}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      padding: '10px 16px',
                      background: '#e8600a',
                      border: 'none',
                      borderRadius: '10px',
                      fontSize: '13px',
                      fontWeight: '600',
                      color: '#fff',
                      cursor: 'pointer'
                    }}
                  >
                    <Plus size={16} />
                    Thêm quy tắc
                  </button>
                </div>

                <div style={{ padding: '24px' }}>
                  {rulesLoading ? (
                    <div style={{ textAlign: 'center', padding: '40px', color: '#A0AEC0' }}>
                      <Loader2 size={24} className="animate-spin" style={{ margin: '0 auto 12px' }} />
                      Đang tải...
                    </div>
                  ) : (
                    <>
                      {/* Violation Rules */}
                      <div style={{ marginBottom: '32px' }}>
                        <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#DC2626', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <MinusCircle size={18} />
                          Quy tắc trừ điểm (Vi phạm)
                        </h3>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                          {reputationRules.filter(r => r.ruleType === 'PENALTY').length === 0 && (
                            <div style={{ padding: '20px', textAlign: 'center', color: '#A0AEC0', fontSize: '14px' }}>Chưa có quy tắc trừ điểm</div>
                          )}
                          {reputationRules.filter(r => r.ruleType === 'PENALTY').map((rule) => (
                            <div key={rule.id} style={{
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'space-between',
                              padding: '16px',
                              background: rule.isActive ? '#FEF2F2' : '#F7FAFC',
                              borderRadius: '12px',
                              border: `1px solid ${rule.isActive ? '#FECACA' : '#E2E8F0'}`,
                              opacity: rule.isActive ? 1 : 0.6
                            }}>
                              <div style={{ flex: 1 }}>
                                <div style={{ fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                                  {rule.ruleName}
                                  {!rule.isActive && <span style={{ fontSize: '11px', padding: '2px 8px', background: '#E2E8F0', borderRadius: '6px', color: '#4A5568' }}>Tắt</span>}
                                </div>
                                <div style={{ fontSize: '13px', color: '#A0AEC0' }}>{rule.description}</div>
                                <div style={{ fontSize: '11px', color: '#CBD5E0', marginTop: '4px' }}>Mã: {rule.ruleCode}</div>
                              </div>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                <span style={{
                                  padding: '6px 14px',
                                  background: '#DC2626',
                                  borderRadius: '10px',
                                  fontSize: '14px',
                                  fontWeight: '600',
                                  color: '#fff'
                                }}>{rule.points}</span>
                                <button onClick={() => handleToggleRule(rule.id)} title={rule.isActive ? 'Tắt quy tắc' : 'Bật quy tắc'} style={{
                                  padding: '8px',
                                  background: rule.isActive ? '#ECFDF5' : '#FEF2F2',
                                  border: '1px solid #E2E8F0',
                                  borderRadius: '8px',
                                  cursor: 'pointer'
                                }}>
                                  {rule.isActive ? <AlertTriangle size={16} color="#059669" /> : <AlertTriangle size={16} color="#DC2626" />}
                                </button>
                                <button onClick={() => handleOpenRuleModal('violation', rule)} style={{
                                  padding: '8px',
                                  background: '#fff',
                                  border: '1px solid #E2E8F0',
                                  borderRadius: '8px',
                                  cursor: 'pointer'
                                }}>
                                  <Edit size={16} color="#4A5568" />
                                </button>
                                <button onClick={() => handleDeleteRule(rule.id)} style={{
                                  padding: '8px',
                                  background: '#FEF2F2',
                                  border: '1px solid #FECACA',
                                  borderRadius: '8px',
                                  cursor: 'pointer'
                                }}>
                                  <Trash2 size={16} color="#DC2626" />
                                </button>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>

                      {/* Reward Rules */}
                      <div>
                        <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#059669', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <PlusCircle size={18} />
                          Quy tắc cộng điểm (Thưởng)
                        </h3>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                          {reputationRules.filter(r => r.ruleType === 'REWARD').length === 0 && (
                            <div style={{ padding: '20px', textAlign: 'center', color: '#A0AEC0', fontSize: '14px' }}>Chưa có quy tắc thưởng điểm</div>
                          )}
                          {reputationRules.filter(r => r.ruleType === 'REWARD').map((rule) => (
                            <div key={rule.id} style={{
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'space-between',
                              padding: '16px',
                              background: rule.isActive ? '#ECFDF5' : '#F7FAFC',
                              borderRadius: '12px',
                              border: `1px solid ${rule.isActive ? '#A7F3D0' : '#E2E8F0'}`,
                              opacity: rule.isActive ? 1 : 0.6
                            }}>
                              <div style={{ flex: 1 }}>
                                <div style={{ fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                                  {rule.ruleName}
                                  {!rule.isActive && <span style={{ fontSize: '11px', padding: '2px 8px', background: '#E2E8F0', borderRadius: '6px', color: '#4A5568' }}>Tắt</span>}
                                </div>
                                <div style={{ fontSize: '13px', color: '#A0AEC0' }}>{rule.description}</div>
                                <div style={{ fontSize: '11px', color: '#CBD5E0', marginTop: '4px' }}>Mã: {rule.ruleCode}</div>
                              </div>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                <span style={{
                                  padding: '6px 14px',
                                  background: '#059669',
                                  borderRadius: '10px',
                                  fontSize: '14px',
                                  fontWeight: '600',
                                  color: '#fff'
                                }}>+{rule.points}</span>
                                <button onClick={() => handleToggleRule(rule.id)} title={rule.isActive ? 'Tắt quy tắc' : 'Bật quy tắc'} style={{
                                  padding: '8px',
                                  background: rule.isActive ? '#ECFDF5' : '#FEF2F2',
                                  border: '1px solid #E2E8F0',
                                  borderRadius: '8px',
                                  cursor: 'pointer'
                                }}>
                                  {rule.isActive ? <AlertTriangle size={16} color="#059669" /> : <AlertTriangle size={16} color="#DC2626" />}
                                </button>
                                <button onClick={() => handleOpenRuleModal('reward', rule)} style={{
                                  padding: '8px',
                                  background: '#fff',
                                  border: '1px solid #E2E8F0',
                                  borderRadius: '8px',
                                  cursor: 'pointer'
                                }}>
                                  <Edit size={16} color="#4A5568" />
                                </button>
                                <button onClick={() => handleDeleteRule(rule.id)} style={{
                                  padding: '8px',
                                  background: '#FEF2F2',
                                  border: '1px solid #FECACA',
                                  borderRadius: '8px',
                                  cursor: 'pointer'
                                }}>
                                  <Trash2 size={16} color="#DC2626" />
                                </button>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    </>
                  )}
                </div>
              </div>
            )}

            {/* Notifications Tab */}
            {!loading && !settingsError && activeTab === 'notifications' && (
              <div style={{
                background: '#fff',
                borderRadius: '10px',
                boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
                overflow: 'hidden'
              }}>
                <div style={{ padding: '24px', borderBottom: '1px solid #E2E8F0' }}>
                  <h2 style={{ fontSize: '18px', fontWeight: '600', color: '#1A1A1A', margin: '0 0 4px 0' }}>
                    Thông báo
                  </h2>
                  <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>
                    Thiết lập thời gian và loại thông báo gửi đến người dùng
                  </p>
                </div>
                <div style={{ padding: '24px' }}>
                  <div style={{ marginBottom: '24px' }}>
                    <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#1A1A1A', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Timer size={18} color="#e8600a" />
                      Thời gian thông báo
                    </h3>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Nhắc lịch trước giờ bắt đầu (phút)
                        </label>
                        <input
                          type="number"
                          min="1"
                          value={libraryConfig.bookingReminderLeadMinutes}
                          onChange={(e) => handleConfigChange('bookingReminderLeadMinutes', parseInt(e.target.value))}
                          style={{
                            width: '100%',
                            padding: '12px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            outline: 'none'
                          }}
                        />
                        <div style={{ fontSize: '12px', color: '#A0AEC0', marginTop: '4px' }}>
                          Hệ thống gửi thông báo nhắc lịch trước số phút này
                        </div>
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Cảnh báo sắp hết giờ (phút)
                        </label>
                        <input
                          type="number"
                          min="1"
                          value={libraryConfig.expiryWarningLeadMinutes}
                          onChange={(e) => handleConfigChange('expiryWarningLeadMinutes', parseInt(e.target.value))}
                          style={{
                            width: '100%',
                            padding: '12px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            outline: 'none'
                          }}
                        />
                        <div style={{ fontSize: '12px', color: '#A0AEC0', marginTop: '4px' }}>
                          Hệ thống gửi cảnh báo trước khi phiên sử dụng sắp kết thúc
                        </div>
                      </div>
                    </div>
                  </div>

                  <div style={{ marginBottom: '16px' }}>
                    <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#1A1A1A', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Bell size={18} color="#e8600a" />
                      Loại thông báo
                    </h3>
                  </div>
                  {[
                    { key: 'notifyBookingSuccess', label: 'Thông báo đặt chỗ thành công', description: 'Gửi khi sinh viên đặt chỗ thành công' },
                    { key: 'notifyCheckinReminder', label: 'Nhắc nhở xác nhận ghế', description: `Gửi trước ${libraryConfig.bookingReminderLeadMinutes} phút khi đến giờ đặt` },
                    { key: 'notifyTimeExpiry', label: 'Cảnh báo hết giờ', description: `Gửi trước ${libraryConfig.expiryWarningLeadMinutes} phút khi hết thời gian đặt` },
                    { key: 'notifyViolation', label: 'Thông báo vi phạm', description: 'Gửi khi sinh viên bị ghi nhận vi phạm' },
                    { key: 'notifyWeeklyReport', label: 'Báo cáo tuần', description: 'Gửi email tổng kết cuối tuần cho admin' },
                    { key: 'notifyDeviceAlert', label: 'Cảnh báo thiết bị', description: 'Thông báo khi thiết bị NFC gặp sự cố' },
                  ].map((item) => {
                    const isEnabled = libraryConfig[item.key] !== false;
                    return (
                      <div key={item.key} style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '16px',
                        background: '#F7FAFC',
                        borderRadius: '12px',
                        marginBottom: '12px'
                      }}>
                        <div>
                          <div style={{ fontSize: '14px', fontWeight: '600', color: '#1A1A1A' }}>{item.label}</div>
                          <div style={{ fontSize: '13px', color: '#A0AEC0' }}>{item.description}</div>
                        </div>
                        <label style={{ position: 'relative', display: 'inline-block', width: '50px', height: '28px' }}>
                          <input
                            type="checkbox"
                            checked={isEnabled}
                            onChange={(e) => handleConfigChange(item.key, e.target.checked)}
                            style={{ opacity: 0, width: 0, height: 0 }}
                          />
                          <span style={{
                            position: 'absolute',
                            cursor: 'pointer',
                            top: 0,
                            left: 0,
                            right: 0,
                            bottom: 0,
                            background: isEnabled ? '#e8600a' : '#E2E8F0',
                            borderRadius: '14px',
                            transition: 'all 0.3s'
                          }}>
                            <span style={{
                              position: 'absolute',
                              height: '22px',
                              width: '22px',
                              left: isEnabled ? '25px' : '3px',
                              bottom: '3px',
                              background: '#fff',
                              borderRadius: '50%',
                              transition: 'all 0.3s',
                              boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
                            }} />
                          </span>
                        </label>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Add/Edit Rule Modal */}
      {showRuleModal && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 2000
        }}>
          <div style={{
            background: '#fff',
            borderRadius: '10px',
            width: '500px',
            boxShadow: '0 20px 60px rgba(0,0,0,0.2)'
          }}>
            <div style={{
              padding: '24px',
              borderBottom: '1px solid #E2E8F0',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center'
            }}>
              <h2 style={{ fontSize: '20px', fontWeight: '600', color: '#1A1A1A', margin: 0 }}>
                {editingRule ? 'Sửa quy tắc' : 'Thêm quy tắc mới'}
              </h2>
              <button onClick={() => setShowRuleModal(false)} style={{
                padding: '8px',
                background: '#F7FAFC',
                border: 'none',
                borderRadius: '8px',
                cursor: 'pointer'
              }}>
                <X size={20} color="#4A5568" />
              </button>
            </div>
            <div style={{ padding: '24px' }}>
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '8px' }}>
                  Loại quy tắc
                </label>
                <div style={{ display: 'flex', gap: '12px' }}>
                  <button
                    onClick={() => { setRuleType('violation'); setRuleForm(f => ({ ...f, ruleType: 'PENALTY' })); }}
                    style={{
                      flex: 1,
                      padding: '12px',
                      background: ruleType === 'violation' ? '#FEE2E2' : '#F7FAFC',
                      border: `2px solid ${ruleType === 'violation' ? '#DC2626' : '#E2E8F0'}`,
                      borderRadius: '10px',
                      fontSize: '14px',
                      fontWeight: '600',
                      color: ruleType === 'violation' ? '#DC2626' : '#4A5568',
                      cursor: 'pointer'
                    }}
                  >
                    <MinusCircle size={18} style={{ marginRight: '8px' }} />
                    Trừ điểm
                  </button>
                  <button
                    onClick={() => { setRuleType('reward'); setRuleForm(f => ({ ...f, ruleType: 'REWARD' })); }}
                    style={{
                      flex: 1,
                      padding: '12px',
                      background: ruleType === 'reward' ? '#D1FAE5' : '#F7FAFC',
                      border: `2px solid ${ruleType === 'reward' ? '#059669' : '#E2E8F0'}`,
                      borderRadius: '10px',
                      fontSize: '14px',
                      fontWeight: '600',
                      color: ruleType === 'reward' ? '#059669' : '#4A5568',
                      cursor: 'pointer'
                    }}
                  >
                    <PlusCircle size={18} style={{ marginRight: '8px' }} />
                    Cộng điểm
                  </button>
                </div>
              </div>
              {!editingRule && (
                <div style={{ marginBottom: '20px' }}>
                  <label style={{ display: 'block', fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '8px' }}>
                    Mã quy tắc (UPPER_SNAKE_CASE)
                  </label>
                  <input type="text" placeholder="VD: NO_SHOW, NOISE_VIOLATION" value={ruleForm.ruleCode}
                    onChange={(e) => setRuleForm(f => ({ ...f, ruleCode: e.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, '') }))}
                    style={{
                      width: '100%',
                      padding: '12px 16px',
                      border: '2px solid #E2E8F0',
                      borderRadius: '12px',
                      fontSize: '14px',
                      outline: 'none'
                    }} />
                </div>
              )}
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '8px' }}>
                  Tên quy tắc
                </label>
                <input type="text" placeholder="Nhập tên quy tắc" value={ruleForm.ruleName}
                  onChange={(e) => setRuleForm(f => ({ ...f, ruleName: e.target.value }))}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    border: '2px solid #E2E8F0',
                    borderRadius: '12px',
                    fontSize: '14px',
                    outline: 'none'
                  }} />
              </div>
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '8px' }}>
                  Số điểm (số dương, hệ thống tự thêm dấu âm cho phạt)
                </label>
                <input type="number" placeholder="VD: 10" value={ruleForm.points} min="1"
                  onChange={(e) => setRuleForm(f => ({ ...f, points: e.target.value }))}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    border: '2px solid #E2E8F0',
                    borderRadius: '12px',
                    fontSize: '14px',
                    outline: 'none'
                  }} />
              </div>
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '8px' }}>
                  Mô tả
                </label>
                <textarea placeholder="Mô tả chi tiết quy tắc" value={ruleForm.description}
                  onChange={(e) => setRuleForm(f => ({ ...f, description: e.target.value }))}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    border: '2px solid #E2E8F0',
                    borderRadius: '12px',
                    fontSize: '14px',
                    outline: 'none',
                    resize: 'none',
                    height: '80px'
                  }} />
              </div>
              <div style={{ display: 'flex', gap: '12px' }}>
                <button onClick={() => setShowRuleModal(false)} style={{
                  flex: 1,
                  padding: '14px',
                  background: '#F7FAFC',
                  border: '2px solid #E2E8F0',
                  borderRadius: '12px',
                  fontSize: '14px',
                  fontWeight: '600',
                  color: '#4A5568',
                  cursor: 'pointer'
                }}>Hủy</button>
                <button onClick={handleSaveRule} style={{
                  flex: 1,
                  padding: '14px',
                  background: '#e8600a',
                  border: 'none',
                  borderRadius: '12px',
                  fontSize: '14px',
                  fontWeight: '600',
                  color: '#fff',
                  cursor: 'pointer'
                }}>{editingRule ? 'Cập nhật' : 'Lưu quy tắc'}</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default SystemConfig;
