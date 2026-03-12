import React, { useState, useEffect } from 'react';
import { useToast } from '../../../components/common/ToastProvider';
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


const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/slib/settings`;
const REPUTATION_API_URL = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/slib/admin/reputation-rules`;

const SystemConfig = () => {
  const toast = useToast();
  const [activeTab, setActiveTab] = useState('library');
  const [showRuleModal, setShowRuleModal] = useState(false);
  const [editingRule, setEditingRule] = useState(null);
  const [ruleType, setRuleType] = useState('violation');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [toggling, setToggling] = useState(false);

  // Reputation Rules State
  const [reputationRules, setReputationRules] = useState([]);
  const [rulesLoading, setRulesLoading] = useState(false);
  const [ruleForm, setRuleForm] = useState({ ruleCode: '', ruleName: '', description: '', points: '', ruleType: 'PENALTY', isActive: true });

  // Library Lock State
  const [libraryClosed, setLibraryClosed] = useState(false);
  const [closedReason, setClosedReason] = useState('');

  // Library Config State
  const [libraryConfig, setLibraryConfig] = useState({
    openTime: '07:00',
    closeTime: '22:00',
    slotDuration: 60,
    maxBookingsPerDay: 3,
    maxHoursPerDay: 4,
    maxBookingDays: 14,
    workingDays: '2,3,4,5,6',
    autoCancelMinutes: 15,
    autoCancelOnLeaveMinutes: 30,
    minReputation: 0,
  });

  // Load settings from API on mount
  useEffect(() => {
    const fetchSettings = async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/library`);
        if (response.ok) {
          const data = await response.json();
          setLibraryClosed(data.libraryClosed || false);
          setClosedReason(data.closedReason || '');
          setLibraryConfig({
            openTime: data.openTime || '07:00',
            closeTime: data.closeTime || '22:00',
            slotDuration: data.slotDuration || 60,
            maxBookingsPerDay: data.maxBookingsPerDay || 3,
            maxHoursPerDay: data.maxHoursPerDay || 4,
            maxBookingDays: data.maxBookingDays || 14,
            workingDays: data.workingDays || '2,3,4,5,6',
            autoCancelMinutes: data.autoCancelMinutes ?? 15,
            autoCancelOnLeaveMinutes: data.autoCancelOnLeaveMinutes ?? 30,
            minReputation: data.minReputation ?? 0,
            notifyBookingSuccess: data.notifyBookingSuccess ?? true,
            notifyCheckinReminder: data.notifyCheckinReminder ?? true,
            notifyTimeExpiry: data.notifyTimeExpiry ?? true,
            notifyViolation: data.notifyViolation ?? true,
            notifyWeeklyReport: data.notifyWeeklyReport ?? false,
            notifyDeviceAlert: data.notifyDeviceAlert ?? true,
          });
        }
      } catch (error) {
        console.error('Error fetching settings:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchSettings();
  }, []);

  // Fetch reputation rules from API
  const fetchReputationRules = async () => {
    setRulesLoading(true);
    try {
      const response = await fetch(REPUTATION_API_URL);
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
        headers: { 'Content-Type': 'application/json' },
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
      const response = await fetch(`${REPUTATION_API_URL}/${ruleId}/toggle`, { method: 'PATCH' });
      if (response.ok) fetchReputationRules();
    } catch (error) {
      console.error('Error toggling rule:', error);
    }
  };

  const handleDeleteRule = async (ruleId) => {
    if (!window.confirm('Bạn có chắc muốn xóa quy tắc này?')) return;
    try {
      const response = await fetch(`${REPUTATION_API_URL}/${ruleId}`, { method: 'DELETE' });
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
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(libraryConfig),
      });
      if (response.ok) {
        toast.success('Lưu cài đặt thành công!');
      } else {
        toast.error('Lỗi khi lưu cài đặt');
      }
    } catch (error) {
      console.error('Error saving settings:', error);
      toast.error('Lỗi kết nối server');
    } finally {
      setSaving(false);
    }
  };

  const tabs = [
    { id: 'library', label: 'Tham số thư viện', icon: Clock },
    { id: 'reputation', label: 'Quy tắc điểm uy tín', icon: Star },
    { id: 'notifications', label: 'Cấu hình thông báo', icon: Bell },
  ];

  const handleConfigChange = (key, value) => {
    setLibraryConfig(prev => ({ ...prev, [key]: value }));
  };

  // Toggle library lock
  const handleToggleLock = async () => {
    const newClosed = !libraryClosed;
    if (newClosed && !closedReason.trim()) {
      toast.warning('Vui lòng nhập lý do đóng thư viện');
      return;
    }
    setToggling(true);
    try {
      const response = await fetch(`${API_BASE_URL}/library/toggle-lock`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ closed: newClosed, reason: newClosed ? closedReason.trim() : null }),
      });
      if (response.ok) {
        const data = await response.json();
        setLibraryClosed(data.libraryClosed || false);
        setClosedReason(data.closedReason || '');
        toast.success(newClosed ? 'Đã khoá thư viện!' : 'Đã mở khoá thư viện!');
      } else {
        toast.error('Lỗi khi thay đổi trạng thái thư viện');
      }
    } catch (error) {
      console.error('Error toggling lock:', error);
      toast.error('Lỗi kết nối server');
    } finally {
      setToggling(false);
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
              cursor: 'pointer'
            }}>
              <RotateCcw size={18} />
              Khôi phục mặc định
            </button>
            <button
              onClick={handleSave}
              disabled={saving}
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
                cursor: saving ? 'not-allowed' : 'pointer',
                boxShadow: '0 4px 14px rgba(255, 117, 31, 0.25)'
              }}>
              {saving ? <Loader2 size={18} className="animate-spin" /> : <Save size={18} />}
              {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
            </button>
          </div>
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
            {/* Library Parameters Tab */}
            {activeTab === 'library' && (
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
                  {/* Library Lock Toggle */}
                  <div style={{
                    marginBottom: '32px',
                    padding: '20px',
                    background: libraryClosed ? 'linear-gradient(135deg, #FFF5F5, #FED7D7)' : 'linear-gradient(135deg, #F0FFF4, #C6F6D5)',
                    borderRadius: '16px',
                    border: libraryClosed ? '2px solid #FC8181' : '2px solid #68D391',
                    transition: 'all 0.3s ease'
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <div style={{
                          width: '44px',
                          height: '44px',
                          borderRadius: '12px',
                          background: libraryClosed ? '#FC8181' : '#68D391',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          transition: 'background 0.3s ease'
                        }}>
                          {libraryClosed ? <Lock size={22} color="#fff" /> : <Unlock size={22} color="#fff" />}
                        </div>
                        <div>
                          <h3 style={{ fontSize: '16px', fontWeight: '700', color: libraryClosed ? '#C53030' : '#276749', margin: '0 0 2px 0' }}>
                            {libraryClosed ? 'Thư viện đang tạm đóng' : 'Thư viện đang hoạt động'}
                          </h3>
                          <p style={{ fontSize: '13px', color: libraryClosed ? '#E53E3E' : '#38A169', margin: 0, fontWeight: '500' }}>
                            {libraryClosed ? 'Sinh viên không thể đặt chỗ' : 'Sinh viên có thể đặt chỗ bình thường'}
                          </p>
                        </div>
                      </div>
                      <button
                        onClick={handleToggleLock}
                        disabled={toggling}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: '8px',
                          padding: '10px 20px',
                          background: libraryClosed ? '#38A169' : '#E53E3E',
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
                        {libraryClosed ? 'Mở khoá thư viện' : 'Khoá thư viện'}
                      </button>
                    </div>
                    {/* Reason input - show when open and about to close */}
                    {!libraryClosed && (
                      <div style={{ marginTop: '12px' }}>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '6px' }}>
                          Lý do khoá (bắt buộc khi khoá)
                        </label>
                        <input
                          type="text"
                          value={closedReason}
                          onChange={(e) => setClosedReason(e.target.value)}
                          placeholder="VD: Sự kiện đặc biệt, Bảo trì hệ thống..."
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
                    )}
                    {/* Show reason when closed */}
                    {libraryClosed && closedReason && (
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
                          Thời lượng mỗi slot (phút)
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
                          Số lượt đặt tối đa/ngày/người
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
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Số giờ tối đa/ngày/người
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
                      </div>
                      <div style={{ gridColumn: 'span 2' }}>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Ngày làm việc (1=CN, 2=T2, ..., 7=T7)
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
                          Tự hủy sau (phút) nếu không check-in
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
                          Đặt chỗ sẽ bị hủy nếu không check-in sau số phút này
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

                  {/* Auto Checkout */}
                  <div>
                    <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#1A1A1A', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Timer size={18} color="#e8600a" />
                      Tự động check-out
                    </h3>
                    <div style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '16px',
                      background: '#F7FAFC',
                      borderRadius: '12px',
                      marginBottom: '16px'
                    }}>
                      <div>
                        <div style={{ fontSize: '14px', fontWeight: '600', color: '#1A1A1A' }}>Bật tự động check-out</div>
                        <div style={{ fontSize: '13px', color: '#A0AEC0' }}>Tự động check-out khi quá thời gian</div>
                      </div>
                      <label style={{ position: 'relative', display: 'inline-block', width: '50px', height: '28px' }}>
                        <input
                          type="checkbox"
                          checked={libraryConfig.autoCheckoutEnabled}
                          onChange={(e) => handleConfigChange('autoCheckoutEnabled', e.target.checked)}
                          style={{ opacity: 0, width: 0, height: 0 }}
                        />
                        <span style={{
                          position: 'absolute',
                          cursor: 'pointer',
                          top: 0,
                          left: 0,
                          right: 0,
                          bottom: 0,
                          background: libraryConfig.autoCheckoutEnabled ? '#e8600a' : '#E2E8F0',
                          borderRadius: '14px',
                          transition: 'all 0.3s'
                        }}>
                          <span style={{
                            position: 'absolute',
                            content: '',
                            height: '22px',
                            width: '22px',
                            left: libraryConfig.autoCheckoutEnabled ? '25px' : '3px',
                            bottom: '3px',
                            background: '#fff',
                            borderRadius: '50%',
                            transition: 'all 0.3s',
                            boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
                          }} />
                        </span>
                      </label>
                    </div>
                    {libraryConfig.autoCheckoutEnabled && (
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Tự động check-out sau (phút)
                        </label>
                        <input
                          type="number"
                          value={libraryConfig.autoCheckoutAfter}
                          onChange={(e) => handleConfigChange('autoCheckoutAfter', parseInt(e.target.value))}
                          style={{
                            width: '200px',
                            padding: '12px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            outline: 'none'
                          }}
                        />
                      </div>
                    )}
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
            {activeTab === 'notifications' && (
              <div style={{
                background: '#fff',
                borderRadius: '10px',
                boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
                overflow: 'hidden'
              }}>
                <div style={{ padding: '24px', borderBottom: '1px solid #E2E8F0' }}>
                  <h2 style={{ fontSize: '18px', fontWeight: '600', color: '#1A1A1A', margin: '0 0 4px 0' }}>
                    Cấu hình thông báo
                  </h2>
                  <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>
                    Thiết lập các loại thông báo gửi đến người dùng
                  </p>
                </div>
                <div style={{ padding: '24px' }}>
                  {[
                    { key: 'notifyBookingSuccess', label: 'Thông báo đặt chỗ thành công', description: 'Gửi khi sinh viên đặt chỗ thành công' },
                    { key: 'notifyCheckinReminder', label: 'Nhắc nhở check-in', description: 'Gửi trước 15 phút khi đến giờ đặt' },
                    { key: 'notifyTimeExpiry', label: 'Cảnh báo hết giờ', description: 'Gửi trước 10 phút khi hết thời gian đặt' },
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