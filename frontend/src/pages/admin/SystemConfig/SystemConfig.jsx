import React, { useState, useEffect } from 'react';
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
  Loader2
} from 'lucide-react';
import Header from '../../../components/shared/Header';

const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/slib/settings`;

// Mock Data
const VIOLATION_RULES = [
  { id: 1, name: 'Gây ồn ào', points: -10, description: 'Vi phạm quy định về tiếng ồn trong thư viện' },
  { id: 2, name: 'No-show (không đến)', points: -15, description: 'Đặt chỗ nhưng không đến check-in' },
  { id: 3, name: 'Ngủ trong thư viện', points: -5, description: 'Ngủ tại bàn học quá 30 phút' },
  { id: 4, name: 'Ăn uống', points: -8, description: 'Mang đồ ăn/nước uống vào khu vực cấm' },
  { id: 5, name: 'Sử dụng điện thoại gây ồn', points: -5, description: 'Nghe gọi điện thoại trong khu yên tĩnh' },
  { id: 6, name: 'Hủy đặt chỗ muộn', points: -3, description: 'Hủy đặt chỗ trong vòng 30 phút trước giờ hẹn' },
];

const REWARD_RULES = [
  { id: 1, name: 'Check-in đúng giờ', points: 2, description: 'Check-in trong vòng 10 phút sau khi đặt' },
  { id: 2, name: 'Sử dụng đủ thời gian', points: 3, description: 'Sử dụng ít nhất 80% thời gian đã đặt' },
  { id: 3, name: 'Không vi phạm trong tuần', points: 10, description: 'Bonus cuối tuần nếu không vi phạm' },
];

const SystemConfig = () => {
  const [activeTab, setActiveTab] = useState('library');
  const [showRuleModal, setShowRuleModal] = useState(false);
  const [editingRule, setEditingRule] = useState(null);
  const [ruleType, setRuleType] = useState('violation');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  // Library Config State
  const [libraryConfig, setLibraryConfig] = useState({
    openTime: '07:00',
    closeTime: '22:00',
    slotDuration: 60,
    maxBookingsPerDay: 3,
    maxHoursPerDay: 4,
    maxBookingDays: 14,
    workingDays: '2,3,4,5,6',
  });

  // Load settings from API on mount
  useEffect(() => {
    const fetchSettings = async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/library`);
        if (response.ok) {
          const data = await response.json();
          setLibraryConfig({
            openTime: data.openTime || '07:00',
            closeTime: data.closeTime || '22:00',
            slotDuration: data.slotDuration || 60,
            maxBookingsPerDay: data.maxBookingsPerDay || 3,
            maxHoursPerDay: data.maxHoursPerDay || 4,
            maxBookingDays: data.maxBookingDays || 14,
            workingDays: data.workingDays || '2,3,4,5,6',
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
        alert('Lưu cài đặt thành công!');
      } else {
        alert('Lỗi khi lưu cài đặt');
      }
    } catch (error) {
      console.error('Error saving settings:', error);
      alert('Lỗi kết nối server');
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

  return (
    <>
      <Header
        searchPlaceholder="Tìm kiếm cài đặt..."
      />

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
            <h1 style={{ fontSize: '28px', fontWeight: '700', color: '#1A1A1A', margin: '0 0 4px 0' }}>
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
                background: saving ? '#ccc' : '#FF751F',
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
            borderRadius: '16px',
            padding: '16px',
            boxShadow: '0 4px 20px rgba(0,0,0,0.06)',
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
                  background: activeTab === tab.id ? '#FFF7F2' : 'transparent',
                  border: activeTab === tab.id ? '2px solid #FF751F' : '2px solid transparent',
                  borderRadius: '12px',
                  fontSize: '14px',
                  fontWeight: activeTab === tab.id ? '600' : '500',
                  color: activeTab === tab.id ? '#FF751F' : '#4A5568',
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
                borderRadius: '16px',
                boxShadow: '0 4px 20px rgba(0,0,0,0.06)',
                overflow: 'hidden'
              }}>
                <div style={{ padding: '24px', borderBottom: '1px solid #E2E8F0' }}>
                  <h2 style={{ fontSize: '18px', fontWeight: '700', color: '#1A1A1A', margin: '0 0 4px 0' }}>
                    Tham số thư viện
                  </h2>
                  <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>
                    Cấu hình giờ hoạt động và các quy định đặt chỗ
                  </p>
                </div>
                <div style={{ padding: '24px' }}>
                  {/* Operating Hours */}
                  <div style={{ marginBottom: '32px' }}>
                    <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#1A1A1A', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Clock size={18} color="#FF751F" />
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
                      <Calendar size={18} color="#FF751F" />
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

                  {/* Auto Checkout */}
                  <div>
                    <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#1A1A1A', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Timer size={18} color="#FF751F" />
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
                          background: libraryConfig.autoCheckoutEnabled ? '#FF751F' : '#E2E8F0',
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
                borderRadius: '16px',
                boxShadow: '0 4px 20px rgba(0,0,0,0.06)',
                overflow: 'hidden'
              }}>
                <div style={{ padding: '24px', borderBottom: '1px solid #E2E8F0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <h2 style={{ fontSize: '18px', fontWeight: '700', color: '#1A1A1A', margin: '0 0 4px 0' }}>
                      Quy tắc điểm uy tín
                    </h2>
                    <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>
                      Cấu hình điểm thưởng và điểm phạt cho sinh viên
                    </p>
                  </div>
                  <button
                    onClick={() => { setRuleType('violation'); setEditingRule(null); setShowRuleModal(true); }}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      padding: '10px 16px',
                      background: '#FF751F',
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
                  {/* Violation Rules */}
                  <div style={{ marginBottom: '32px' }}>
                    <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#DC2626', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <MinusCircle size={18} />
                      Quy tắc trừ điểm (Vi phạm)
                    </h3>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                      {VIOLATION_RULES.map((rule) => (
                        <div key={rule.id} style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '16px',
                          background: '#FEF2F2',
                          borderRadius: '12px',
                          border: '1px solid #FECACA'
                        }}>
                          <div style={{ flex: 1 }}>
                            <div style={{ fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '4px' }}>{rule.name}</div>
                            <div style={{ fontSize: '13px', color: '#A0AEC0' }}>{rule.description}</div>
                          </div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                            <span style={{
                              padding: '6px 14px',
                              background: '#DC2626',
                              borderRadius: '20px',
                              fontSize: '14px',
                              fontWeight: '700',
                              color: '#fff'
                            }}>{rule.points}</span>
                            <button style={{
                              padding: '8px',
                              background: '#fff',
                              border: '1px solid #E2E8F0',
                              borderRadius: '8px',
                              cursor: 'pointer'
                            }}>
                              <Edit size={16} color="#4A5568" />
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
                      {REWARD_RULES.map((rule) => (
                        <div key={rule.id} style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '16px',
                          background: '#ECFDF5',
                          borderRadius: '12px',
                          border: '1px solid #A7F3D0'
                        }}>
                          <div style={{ flex: 1 }}>
                            <div style={{ fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '4px' }}>{rule.name}</div>
                            <div style={{ fontSize: '13px', color: '#A0AEC0' }}>{rule.description}</div>
                          </div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                            <span style={{
                              padding: '6px 14px',
                              background: '#059669',
                              borderRadius: '20px',
                              fontSize: '14px',
                              fontWeight: '700',
                              color: '#fff'
                            }}>+{rule.points}</span>
                            <button style={{
                              padding: '8px',
                              background: '#fff',
                              border: '1px solid #E2E8F0',
                              borderRadius: '8px',
                              cursor: 'pointer'
                            }}>
                              <Edit size={16} color="#4A5568" />
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Notifications Tab */}
            {activeTab === 'notifications' && (
              <div style={{
                background: '#fff',
                borderRadius: '16px',
                boxShadow: '0 4px 20px rgba(0,0,0,0.06)',
                overflow: 'hidden'
              }}>
                <div style={{ padding: '24px', borderBottom: '1px solid #E2E8F0' }}>
                  <h2 style={{ fontSize: '18px', fontWeight: '700', color: '#1A1A1A', margin: '0 0 4px 0' }}>
                    Cấu hình thông báo
                  </h2>
                  <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>
                    Thiết lập các loại thông báo gửi đến người dùng
                  </p>
                </div>
                <div style={{ padding: '24px' }}>
                  {[
                    { label: 'Thông báo đặt chỗ thành công', description: 'Gửi khi sinh viên đặt chỗ thành công', enabled: true },
                    { label: 'Nhắc nhở check-in', description: 'Gửi trước 15 phút khi đến giờ đặt', enabled: true },
                    { label: 'Cảnh báo hết giờ', description: 'Gửi trước 10 phút khi hết thời gian đặt', enabled: true },
                    { label: 'Thông báo vi phạm', description: 'Gửi khi sinh viên bị ghi nhận vi phạm', enabled: true },
                    { label: 'Báo cáo tuần', description: 'Gửi email tổng kết cuối tuần cho admin', enabled: false },
                    { label: 'Cảnh báo thiết bị', description: 'Thông báo khi thiết bị NFC gặp sự cố', enabled: true },
                  ].map((item, idx) => (
                    <div key={idx} style={{
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
                        <input type="checkbox" defaultChecked={item.enabled} style={{ opacity: 0, width: 0, height: 0 }} />
                        <span style={{
                          position: 'absolute',
                          cursor: 'pointer',
                          top: 0,
                          left: 0,
                          right: 0,
                          bottom: 0,
                          background: item.enabled ? '#FF751F' : '#E2E8F0',
                          borderRadius: '14px',
                          transition: 'all 0.3s'
                        }}>
                          <span style={{
                            position: 'absolute',
                            height: '22px',
                            width: '22px',
                            left: item.enabled ? '25px' : '3px',
                            bottom: '3px',
                            background: '#fff',
                            borderRadius: '50%',
                            transition: 'all 0.3s',
                            boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
                          }} />
                        </span>
                      </label>
                    </div>
                  ))}
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
            borderRadius: '20px',
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
              <h2 style={{ fontSize: '20px', fontWeight: '700', color: '#1A1A1A', margin: 0 }}>
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
                    onClick={() => setRuleType('violation')}
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
                    onClick={() => setRuleType('reward')}
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
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '8px' }}>
                  Tên quy tắc
                </label>
                <input type="text" placeholder="Nhập tên quy tắc" style={{
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
                  Số điểm
                </label>
                <input type="number" placeholder="Nhập số điểm" style={{
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
                <textarea placeholder="Mô tả chi tiết quy tắc" style={{
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
                <button style={{
                  flex: 1,
                  padding: '14px',
                  background: '#FF751F',
                  border: 'none',
                  borderRadius: '12px',
                  fontSize: '14px',
                  fontWeight: '600',
                  color: '#fff',
                  cursor: 'pointer'
                }}>Lưu quy tắc</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default SystemConfig;