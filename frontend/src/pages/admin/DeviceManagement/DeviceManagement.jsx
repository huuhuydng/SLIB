import React, { useState, useMemo } from 'react';
import {
  Cpu,
  Search,
  Plus,
  MoreVertical,
  Wifi,
  WifiOff,
  MapPin,
  Settings,
  Trash2,
  Edit,
  X,
  CheckCircle,
  XCircle,
  AlertTriangle,
  RefreshCw,
  Smartphone,
  Router,
  Link2,
  Unlink
} from 'lucide-react';


// Mock Data
const MOCK_DEVICES = [
  { id: 1, deviceId: 'NFC-001', name: 'Đầu đọc cửa chính', type: 'Door Reader', location: 'Cửa ra vào chính', status: 'online', lastPing: '2025-01-16T17:30:00', battery: null },
  { id: 2, deviceId: 'NFC-002', name: 'Đầu đọc khu A', type: 'Seat Reader', location: 'Ghế A1-A10', status: 'online', lastPing: '2025-01-16T17:29:00', battery: 85 },
  { id: 3, deviceId: 'NFC-003', name: 'Đầu đọc khu A', type: 'Seat Reader', location: 'Ghế A11-A21', status: 'online', lastPing: '2025-01-16T17:28:00', battery: 72 },
  { id: 4, deviceId: 'NFC-004', name: 'Đầu đọc khu B', type: 'Seat Reader', location: 'Ghế B1-B14', status: 'offline', lastPing: '2025-01-16T14:20:00', battery: 15 },
  { id: 5, deviceId: 'NFC-005', name: 'Đầu đọc khu B', type: 'Seat Reader', location: 'Ghế B15-B28', status: 'online', lastPing: '2025-01-16T17:25:00', battery: 90 },
  { id: 6, deviceId: 'NFC-006', name: 'Đầu đọc khu C', type: 'Seat Reader', location: 'Ghế C1-C10', status: 'warning', lastPing: '2025-01-16T17:00:00', battery: 25 },
  { id: 7, deviceId: 'NFC-007', name: 'Đầu đọc khu C', type: 'Seat Reader', location: 'Ghế C11-C21', status: 'online', lastPing: '2025-01-16T17:30:00', battery: 68 },
  { id: 8, deviceId: 'NFC-008', name: 'Đầu đọc cửa phụ', type: 'Door Reader', location: 'Cửa thoát hiểm', status: 'online', lastPing: '2025-01-16T17:29:00', battery: null },
];

const DeviceManagement = () => {
  const [searchText, setSearchText] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [typeFilter, setTypeFilter] = useState('all');
  const [showAddModal, setShowAddModal] = useState(false);
  const [showMappingModal, setShowMappingModal] = useState(false);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [showActionMenu, setShowActionMenu] = useState(null);

  const filteredDevices = useMemo(() => {
    return MOCK_DEVICES.filter(device => {
      const matchSearch = device.name.toLowerCase().includes(searchText.toLowerCase()) ||
        device.deviceId.toLowerCase().includes(searchText.toLowerCase()) ||
        device.location.toLowerCase().includes(searchText.toLowerCase());
      const matchStatus = statusFilter === 'all' || device.status === statusFilter;
      const matchType = typeFilter === 'all' || device.type === typeFilter;
      return matchSearch && matchStatus && matchType;
    });
  }, [searchText, statusFilter, typeFilter]);

  const stats = useMemo(() => ({
    total: MOCK_DEVICES.length,
    online: MOCK_DEVICES.filter(d => d.status === 'online').length,
    offline: MOCK_DEVICES.filter(d => d.status === 'offline').length,
    warning: MOCK_DEVICES.filter(d => d.status === 'warning').length,
  }), []);

  const getStatusStyle = (status) => {
    switch (status) {
      case 'online': return { bg: '#D1FAE5', color: '#059669', icon: Wifi, label: 'Hoạt động' };
      case 'offline': return { bg: '#FEE2E2', color: '#DC2626', icon: WifiOff, label: 'Mất kết nối' };
      case 'warning': return { bg: '#FEF3C7', color: '#F59E0B', icon: AlertTriangle, label: 'Cảnh báo' };
      default: return { bg: '#F3F4F6', color: '#6B7280', icon: Cpu, label: 'Không xác định' };
    }
  };

  const formatLastPing = (dateStr) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diff = Math.floor((now - date) / 1000 / 60);
    if (diff < 5) return 'Vừa xong';
    if (diff < 60) return `${diff} phút trước`;
    if (diff < 1440) return `${Math.floor(diff / 60)} giờ trước`;
    return `${Math.floor(diff / 1440)} ngày trước`;
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
              Quản lý thiết bị
            </h1>
            <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>
              Quản lý các đầu đọc NFC và thiết bị IoT trong thư viện
            </p>
          </div>
          <div style={{ display: 'flex', gap: '12px' }}>
            <button
              onClick={() => { }}
              style={{
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
              }}
            >
              <RefreshCw size={18} />
              Làm mới
            </button>
            <button
              onClick={() => setShowAddModal(true)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '12px 20px',
                background: '#e8600a',
                border: 'none',
                borderRadius: '12px',
                fontSize: '14px',
                fontWeight: '600',
                color: '#fff',
                cursor: 'pointer',
                boxShadow: '0 4px 14px rgba(255, 117, 31, 0.25)'
              }}
            >
              <Plus size={18} />
              Thêm thiết bị
            </button>
          </div>
        </div>

        {/* Stats Cards */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(4, 1fr)',
          gap: '16px',
          marginBottom: '24px'
        }}>
          {[
            { label: 'Tổng thiết bị', value: stats.total, icon: Cpu, color: '#7C3AED', bg: '#F3E8FF' },
            { label: 'Đang hoạt động', value: stats.online, icon: Wifi, color: '#059669', bg: '#D1FAE5' },
            { label: 'Mất kết nối', value: stats.offline, icon: WifiOff, color: '#DC2626', bg: '#FEE2E2' },
            { label: 'Cảnh báo', value: stats.warning, icon: AlertTriangle, color: '#F59E0B', bg: '#FEF3C7' },
          ].map((stat, idx) => (
            <div key={idx} style={{
              background: '#fff',
              borderRadius: '12px',
              padding: '20px',
              boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
              display: 'flex',
              alignItems: 'center',
              gap: '16px'
            }}>
              <div style={{
                width: '48px',
                height: '48px',
                borderRadius: '12px',
                background: stat.bg,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <stat.icon size={22} color={stat.color} />
              </div>
              <div>
                <div style={{ fontSize: '18px', fontWeight: '600', color: '#1A1A1A' }}>{stat.value}</div>
                <div style={{ fontSize: '13px', color: '#A0AEC0', fontWeight: '500' }}>{stat.label}</div>
              </div>
            </div>
          ))}
        </div>

        {/* Device Grid */}
        <div style={{
          background: '#fff',
          borderRadius: '10px',
          boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
          overflow: 'hidden'
        }}>
          {/* Filter Bar */}
          <div style={{
            padding: '20px 24px',
            borderBottom: '1px solid #E2E8F0',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center'
          }}>
            <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
              <div style={{ position: 'relative' }}>
                <Search size={16} style={{
                  position: 'absolute',
                  left: '12px',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  color: '#A0AEC0'
                }} />
                <input
                  type="text"
                  placeholder="Tìm theo ID, tên, vị trí..."
                  value={searchText}
                  onChange={(e) => setSearchText(e.target.value)}
                  style={{
                    padding: '10px 12px 10px 40px',
                    border: '2px solid #E2E8F0',
                    borderRadius: '10px',
                    fontSize: '14px',
                    width: '280px',
                    outline: 'none'
                  }}
                />
              </div>

              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                style={{
                  padding: '10px 16px',
                  border: '2px solid #E2E8F0',
                  borderRadius: '10px',
                  fontSize: '14px',
                  color: '#4A5568',
                  background: '#fff',
                  cursor: 'pointer'
                }}
              >
                <option value="all">Tất cả trạng thái</option>
                <option value="online">Hoạt động</option>
                <option value="offline">Mất kết nối</option>
                <option value="warning">Cảnh báo</option>
              </select>

              <select
                value={typeFilter}
                onChange={(e) => setTypeFilter(e.target.value)}
                style={{
                  padding: '10px 16px',
                  border: '2px solid #E2E8F0',
                  borderRadius: '10px',
                  fontSize: '14px',
                  color: '#4A5568',
                  background: '#fff',
                  cursor: 'pointer'
                }}
              >
                <option value="all">Tất cả loại</option>
                <option value="Door Reader">Đầu đọc cửa</option>
                <option value="Seat Reader">Đầu đọc ghế</option>
              </select>
            </div>
          </div>

          {/* Device Cards Grid */}
          <div style={{
            padding: '24px',
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
            gap: '16px'
          }}>
            {filteredDevices.map((device) => {
              const statusStyle = getStatusStyle(device.status);
              const StatusIcon = statusStyle.icon;
              return (
                <div key={device.id} style={{
                  background: '#F7FAFC',
                  borderRadius: '14px',
                  padding: '20px',
                  border: '2px solid #E2E8F0',
                  transition: 'all 0.2s ease'
                }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.borderColor = '#e8600a';
                    e.currentTarget.style.boxShadow = '0 4px 12px rgba(255, 117, 31, 0.15)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.borderColor = '#E2E8F0';
                    e.currentTarget.style.boxShadow = 'none';
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '16px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <div style={{
                        width: '44px',
                        height: '44px',
                        borderRadius: '12px',
                        background: device.type === 'Door Reader' ? '#DBEAFE' : '#F3E8FF',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                      }}>
                        {device.type === 'Door Reader' ? (
                          <Router size={22} color="#2563EB" />
                        ) : (
                          <Smartphone size={22} color="#7C3AED" />
                        )}
                      </div>
                      <div>
                        <div style={{ fontSize: '15px', fontWeight: '600', color: '#1A1A1A' }}>{device.name}</div>
                        <div style={{ fontSize: '12px', color: '#A0AEC0', fontFamily: 'monospace' }}>{device.deviceId}</div>
                      </div>
                    </div>
                    <div style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '6px',
                      padding: '4px 10px',
                      borderRadius: '10px',
                      background: statusStyle.bg
                    }}>
                      <StatusIcon size={14} color={statusStyle.color} />
                      <span style={{ fontSize: '12px', fontWeight: '600', color: statusStyle.color }}>{statusStyle.label}</span>
                    </div>
                  </div>

                  <div style={{ marginBottom: '16px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
                      <MapPin size={14} color="#A0AEC0" />
                      <span style={{ fontSize: '13px', color: '#4A5568' }}>{device.location}</span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <RefreshCw size={14} color="#A0AEC0" />
                      <span style={{ fontSize: '13px', color: '#A0AEC0' }}>Ping: {formatLastPing(device.lastPing)}</span>
                    </div>
                  </div>

                  {device.battery !== null && (
                    <div style={{ marginBottom: '16px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                        <span style={{ fontSize: '12px', color: '#A0AEC0' }}>Pin</span>
                        <span style={{ fontSize: '12px', fontWeight: '600', color: device.battery < 30 ? '#DC2626' : '#059669' }}>
                          {device.battery}%
                        </span>
                      </div>
                      <div style={{ height: '6px', background: '#E2E8F0', borderRadius: '100px', overflow: 'hidden' }}>
                        <div style={{
                          height: '100%',
                          width: `${device.battery}%`,
                          background: device.battery < 30 ? '#DC2626' : device.battery < 50 ? '#F59E0B' : '#059669',
                          borderRadius: '100px'
                        }} />
                      </div>
                    </div>
                  )}

                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button
                      onClick={() => { setSelectedDevice(device); setShowMappingModal(true); }}
                      style={{
                        flex: 1,
                        padding: '10px',
                        background: '#fff',
                        border: '2px solid #E2E8F0',
                        borderRadius: '10px',
                        fontSize: '13px',
                        fontWeight: '600',
                        color: '#4A5568',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '6px'
                      }}
                    >
                      <Link2 size={14} />
                      Ánh xạ
                    </button>
                    <button style={{
                      flex: 1,
                      padding: '10px',
                      background: '#fff',
                      border: '2px solid #E2E8F0',
                      borderRadius: '10px',
                      fontSize: '13px',
                      fontWeight: '600',
                      color: '#4A5568',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '6px'
                    }}>
                      <Settings size={14} />
                      Cấu hình
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Add Device Modal */}
      {showAddModal && (
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
              <h2 style={{ fontSize: '20px', fontWeight: '600', color: '#1A1A1A', margin: 0 }}>Thêm thiết bị mới</h2>
              <button onClick={() => setShowAddModal(false)} style={{
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
                  Device ID
                </label>
                <input type="text" placeholder="NFC-XXX" style={{
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
                  Tên thiết bị
                </label>
                <input type="text" placeholder="Nhập tên thiết bị" style={{
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
                  Loại thiết bị
                </label>
                <select style={{
                  width: '100%',
                  padding: '12px 16px',
                  border: '2px solid #E2E8F0',
                  borderRadius: '12px',
                  fontSize: '14px',
                  color: '#4A5568',
                  background: '#fff',
                  cursor: 'pointer'
                }}>
                  <option value="">Chọn loại thiết bị</option>
                  <option value="Door Reader">Đầu đọc cửa</option>
                  <option value="Seat Reader">Đầu đọc ghế</option>
                </select>
              </div>
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '8px' }}>
                  Vị trí
                </label>
                <input type="text" placeholder="Mô tả vị trí thiết bị" style={{
                  width: '100%',
                  padding: '12px 16px',
                  border: '2px solid #E2E8F0',
                  borderRadius: '12px',
                  fontSize: '14px',
                  outline: 'none'
                }} />
              </div>
              <div style={{ display: 'flex', gap: '12px', marginTop: '24px' }}>
                <button onClick={() => setShowAddModal(false)} style={{
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
                  background: '#e8600a',
                  border: 'none',
                  borderRadius: '12px',
                  fontSize: '14px',
                  fontWeight: '600',
                  color: '#fff',
                  cursor: 'pointer'
                }}>Thêm thiết bị</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Mapping Modal */}
      {showMappingModal && selectedDevice && (
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
            width: '550px',
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
                Ánh xạ vị trí - {selectedDevice.deviceId}
              </h2>
              <button onClick={() => { setShowMappingModal(false); setSelectedDevice(null); }} style={{
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
              <div style={{
                background: '#F7FAFC',
                borderRadius: '12px',
                padding: '16px',
                marginBottom: '20px'
              }}>
                <p style={{ fontSize: '14px', color: '#4A5568', margin: 0 }}>
                  Liên kết thiết bị <strong>{selectedDevice.name}</strong> với một vị trí cụ thể trên bản đồ thư viện.
                </p>
              </div>

              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '8px' }}>
                  Loại vị trí
                </label>
                <select style={{
                  width: '100%',
                  padding: '12px 16px',
                  border: '2px solid #E2E8F0',
                  borderRadius: '12px',
                  fontSize: '14px',
                  color: '#4A5568',
                  background: '#fff',
                  cursor: 'pointer'
                }}>
                  <option value="">Chọn loại vị trí</option>
                  <option value="door">Cửa ra vào</option>
                  <option value="seat">Ghế / Chỗ ngồi</option>
                  <option value="zone">Khu vực</option>
                </select>
              </div>

              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '8px' }}>
                  Chọn vị trí cụ thể
                </label>
                <select style={{
                  width: '100%',
                  padding: '12px 16px',
                  border: '2px solid #E2E8F0',
                  borderRadius: '12px',
                  fontSize: '14px',
                  color: '#4A5568',
                  background: '#fff',
                  cursor: 'pointer'
                }}>
                  <option value="">Chọn vị trí</option>
                  <option value="A1">Ghế A1</option>
                  <option value="A2">Ghế A2</option>
                  <option value="A3">Ghế A3</option>
                </select>
              </div>

              <div style={{ display: 'flex', gap: '12px', marginTop: '24px' }}>
                <button onClick={() => { setShowMappingModal(false); setSelectedDevice(null); }} style={{
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
                  background: '#e8600a',
                  border: 'none',
                  borderRadius: '12px',
                  fontSize: '14px',
                  fontWeight: '600',
                  color: '#fff',
                  cursor: 'pointer'
                }}>Lưu ánh xạ</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default DeviceManagement;