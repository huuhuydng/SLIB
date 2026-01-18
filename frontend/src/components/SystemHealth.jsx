import React, { useState, useMemo } from 'react';
import { 
  Activity, 
  Database, 
  Server, 
  HardDrive,
  Cpu,
  MemoryStick,
  Wifi,
  Clock,
  AlertTriangle,
  CheckCircle,
  XCircle,
  RefreshCw,
  Download,
  Search,
  Filter,
  Calendar,
  ArrowUpRight,
  ArrowDownRight,
  Zap,
  Shield,
  FileText,
  AlertCircle,
  Info,
  Bug,
  CloudOff,
  Save,
  History,
  Play,
  Pause
} from 'lucide-react';
import Header from './Header';

// Mock Data
const SYSTEM_METRICS = {
  cpu: 45,
  memory: 62,
  disk: 38,
  network: 99.8,
  uptime: '15 ngày 7 giờ 23 phút',
  lastBackup: '2025-01-16T03:00:00'
};

const API_ENDPOINTS = [
  { name: '/api/auth', status: 'healthy', responseTime: 45, requests: 1250 },
  { name: '/api/bookings', status: 'healthy', responseTime: 78, requests: 3420 },
  { name: '/api/users', status: 'healthy', responseTime: 52, requests: 890 },
  { name: '/api/seats', status: 'warning', responseTime: 234, requests: 5680 },
  { name: '/api/devices', status: 'healthy', responseTime: 38, requests: 420 },
  { name: '/api/notifications', status: 'error', responseTime: 0, requests: 156 },
];

const SYSTEM_LOGS = [
  { id: 1, timestamp: '2025-01-16T17:30:15', level: 'error', service: 'NotificationService', message: 'Failed to send push notification: Connection timeout', user: null },
  { id: 2, timestamp: '2025-01-16T17:28:42', level: 'warning', service: 'SeatService', message: 'High response time detected (>200ms)', user: null },
  { id: 3, timestamp: '2025-01-16T17:25:10', level: 'info', service: 'AuthService', message: 'User logged in successfully', user: 'phucnh@fpt.edu.vn' },
  { id: 4, timestamp: '2025-01-16T17:22:33', level: 'info', service: 'BookingService', message: 'New booking created', user: 'antv@fpt.edu.vn' },
  { id: 5, timestamp: '2025-01-16T17:18:55', level: 'debug', service: 'NFCService', message: 'Device NFC-004 disconnected', user: null },
  { id: 6, timestamp: '2025-01-16T17:15:20', level: 'warning', service: 'DatabaseService', message: 'Slow query detected (>1000ms)', user: null },
  { id: 7, timestamp: '2025-01-16T17:10:45', level: 'info', service: 'BackupService', message: 'Scheduled backup completed successfully', user: null },
  { id: 8, timestamp: '2025-01-16T17:05:12', level: 'error', service: 'NotificationService', message: 'Firebase Cloud Messaging error', user: null },
];

const BACKUP_HISTORY = [
  { id: 1, date: '2025-01-16T03:00:00', size: '2.4 GB', status: 'success', duration: '4 phút 32 giây' },
  { id: 2, date: '2025-01-15T03:00:00', size: '2.3 GB', status: 'success', duration: '4 phút 18 giây' },
  { id: 3, date: '2025-01-14T03:00:00', size: '2.3 GB', status: 'success', duration: '4 phút 25 giây' },
  { id: 4, date: '2025-01-13T03:00:00', size: '2.2 GB', status: 'failed', duration: '-- phút' },
  { id: 5, date: '2025-01-12T03:00:00', size: '2.2 GB', status: 'success', duration: '4 phút 10 giây' },
];

const SystemHealth = () => {
  const [activeTab, setActiveTab] = useState('overview');
  const [logFilter, setLogFilter] = useState('all');
  const [searchLog, setSearchLog] = useState('');
  const [isBackingUp, setIsBackingUp] = useState(false);

  const filteredLogs = useMemo(() => {
    return SYSTEM_LOGS.filter(log => {
      const matchSearch = log.message.toLowerCase().includes(searchLog.toLowerCase()) ||
                         log.service.toLowerCase().includes(searchLog.toLowerCase());
      const matchFilter = logFilter === 'all' || log.level === logFilter;
      return matchSearch && matchFilter;
    });
  }, [searchLog, logFilter]);

  const getLogLevelStyle = (level) => {
    switch(level) {
      case 'error': return { bg: '#FEE2E2', color: '#DC2626', icon: XCircle };
      case 'warning': return { bg: '#FEF3C7', color: '#F59E0B', icon: AlertTriangle };
      case 'info': return { bg: '#DBEAFE', color: '#2563EB', icon: Info };
      case 'debug': return { bg: '#F3E8FF', color: '#7C3AED', icon: Bug };
      default: return { bg: '#F3F4F6', color: '#6B7280', icon: Info };
    }
  };

  const getStatusStyle = (status) => {
    switch(status) {
      case 'healthy': return { bg: '#D1FAE5', color: '#059669' };
      case 'warning': return { bg: '#FEF3C7', color: '#F59E0B' };
      case 'error': return { bg: '#FEE2E2', color: '#DC2626' };
      default: return { bg: '#F3F4F6', color: '#6B7280' };
    }
  };

  const formatDate = (dateStr) => {
    const date = new Date(dateStr);
    return date.toLocaleString('vi-VN');
  };

  const handleManualBackup = () => {
    setIsBackingUp(true);
    setTimeout(() => setIsBackingUp(false), 3000);
  };

  return (
    <>
      <Header searchPlaceholder="Tìm kiếm..." />

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
              Giám sát hệ thống
            </h1>
            <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>
              Theo dõi sức khỏe và hiệu suất của SLIB
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
              <RefreshCw size={18} />
              Làm mới
            </button>
            <button style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '12px 20px',
              background: '#FF751F',
              border: 'none',
              borderRadius: '12px',
              fontSize: '14px',
              fontWeight: '600',
              color: '#fff',
              cursor: 'pointer',
              boxShadow: '0 4px 14px rgba(255, 117, 31, 0.25)'
            }}>
              <Download size={18} />
              Xuất báo cáo
            </button>
          </div>
        </div>

        {/* Tabs */}
        <div style={{
          display: 'flex',
          gap: '8px',
          marginBottom: '24px',
          background: '#fff',
          padding: '8px',
          borderRadius: '14px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
          width: 'fit-content'
        }}>
          {[
            { id: 'overview', label: 'Tổng quan', icon: Activity },
            { id: 'logs', label: 'Nhật ký hệ thống', icon: FileText },
            { id: 'backup', label: 'Sao lưu dữ liệu', icon: HardDrive },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '12px 20px',
                background: activeTab === tab.id ? '#FF751F' : 'transparent',
                border: 'none',
                borderRadius: '10px',
                fontSize: '14px',
                fontWeight: '600',
                color: activeTab === tab.id ? '#fff' : '#4A5568',
                cursor: 'pointer',
                transition: 'all 0.2s'
              }}
            >
              <tab.icon size={18} />
              {tab.label}
            </button>
          ))}
        </div>

        {/* Overview Tab */}
        {activeTab === 'overview' && (
          <>
            {/* System Metrics */}
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(4, 1fr)',
              gap: '16px',
              marginBottom: '24px'
            }}>
              {[
                { label: 'CPU Usage', value: `${SYSTEM_METRICS.cpu}%`, icon: Cpu, color: SYSTEM_METRICS.cpu > 80 ? '#DC2626' : '#059669', trend: '+2%' },
                { label: 'Memory Usage', value: `${SYSTEM_METRICS.memory}%`, icon: MemoryStick, color: SYSTEM_METRICS.memory > 80 ? '#DC2626' : '#F59E0B', trend: '+5%' },
                { label: 'Disk Usage', value: `${SYSTEM_METRICS.disk}%`, icon: HardDrive, color: '#059669', trend: '+1%' },
                { label: 'Network Uptime', value: `${SYSTEM_METRICS.network}%`, icon: Wifi, color: '#059669', trend: '0%' },
              ].map((metric, idx) => (
                <div key={idx} style={{
                  background: '#fff',
                  borderRadius: '16px',
                  padding: '24px',
                  boxShadow: '0 4px 20px rgba(0,0,0,0.06)'
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '16px' }}>
                    <div style={{
                      width: '48px',
                      height: '48px',
                      borderRadius: '12px',
                      background: metric.color === '#059669' ? '#D1FAE5' : metric.color === '#F59E0B' ? '#FEF3C7' : '#FEE2E2',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}>
                      <metric.icon size={24} color={metric.color} />
                    </div>
                    <span style={{
                      fontSize: '12px',
                      fontWeight: '600',
                      color: metric.trend.startsWith('+') && metric.trend !== '+0%' ? '#DC2626' : '#059669',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '2px'
                    }}>
                      {metric.trend.startsWith('+') && metric.trend !== '+0%' ? <ArrowUpRight size={14} /> : null}
                      {metric.trend}
                    </span>
                  </div>
                  <div style={{ fontSize: '28px', fontWeight: '700', color: '#1A1A1A', marginBottom: '4px' }}>{metric.value}</div>
                  <div style={{ fontSize: '13px', color: '#A0AEC0' }}>{metric.label}</div>
                  {/* Progress bar */}
                  <div style={{ marginTop: '12px', height: '6px', background: '#E2E8F0', borderRadius: '100px', overflow: 'hidden' }}>
                    <div style={{
                      height: '100%',
                      width: metric.value,
                      background: metric.color,
                      borderRadius: '100px',
                      transition: 'width 0.5s ease'
                    }} />
                  </div>
                </div>
              ))}
            </div>

            {/* System Info & API Status */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '24px' }}>
              {/* System Info */}
              <div style={{
                background: '#fff',
                borderRadius: '16px',
                boxShadow: '0 4px 20px rgba(0,0,0,0.06)',
                overflow: 'hidden'
              }}>
                <div style={{ padding: '20px 24px', borderBottom: '1px solid #E2E8F0' }}>
                  <h3 style={{ fontSize: '16px', fontWeight: '700', color: '#1A1A1A', margin: 0 }}>Thông tin hệ thống</h3>
                </div>
                <div style={{ padding: '24px' }}>
                  {[
                    { label: 'Thời gian hoạt động', value: SYSTEM_METRICS.uptime, icon: Clock },
                    { label: 'Database', value: 'PostgreSQL 15.2', icon: Database },
                    { label: 'Server', value: 'Ubuntu 22.04 LTS', icon: Server },
                    { label: 'Java Runtime', value: 'OpenJDK 17.0.2', icon: Zap },
                    { label: 'Sao lưu gần nhất', value: formatDate(SYSTEM_METRICS.lastBackup), icon: Shield },
                  ].map((item, idx) => (
                    <div key={idx} style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '12px',
                      padding: '12px 0',
                      borderBottom: idx < 4 ? '1px solid #E2E8F0' : 'none'
                    }}>
                      <div style={{
                        width: '36px',
                        height: '36px',
                        borderRadius: '10px',
                        background: '#F7FAFC',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                      }}>
                        <item.icon size={18} color="#4A5568" />
                      </div>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontSize: '12px', color: '#A0AEC0', marginBottom: '2px' }}>{item.label}</div>
                        <div style={{ fontSize: '14px', fontWeight: '600', color: '#1A1A1A' }}>{item.value}</div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* API Status */}
              <div style={{
                background: '#fff',
                borderRadius: '16px',
                boxShadow: '0 4px 20px rgba(0,0,0,0.06)',
                overflow: 'hidden'
              }}>
                <div style={{ padding: '20px 24px', borderBottom: '1px solid #E2E8F0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <h3 style={{ fontSize: '16px', fontWeight: '700', color: '#1A1A1A', margin: 0 }}>Trạng thái API</h3>
                  <span style={{ fontSize: '12px', color: '#A0AEC0' }}>Cập nhật: Vừa xong</span>
                </div>
                <div style={{ padding: '16px 24px' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                    <thead>
                      <tr>
                        {['Endpoint', 'Trạng thái', 'Response Time', 'Requests/h'].map((header, idx) => (
                          <th key={idx} style={{
                            textAlign: idx === 0 ? 'left' : 'center',
                            padding: '12px 8px',
                            fontSize: '12px',
                            fontWeight: '600',
                            color: '#A0AEC0',
                            textTransform: 'uppercase',
                            letterSpacing: '0.5px'
                          }}>{header}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {API_ENDPOINTS.map((endpoint, idx) => {
                        const statusStyle = getStatusStyle(endpoint.status);
                        return (
                          <tr key={idx}>
                            <td style={{ padding: '12px 8px' }}>
                              <span style={{ fontSize: '14px', fontWeight: '500', color: '#1A1A1A', fontFamily: 'monospace' }}>{endpoint.name}</span>
                            </td>
                            <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                              <span style={{
                                padding: '4px 10px',
                                borderRadius: '20px',
                                fontSize: '12px',
                                fontWeight: '600',
                                background: statusStyle.bg,
                                color: statusStyle.color
                              }}>
                                {endpoint.status === 'healthy' ? 'Tốt' : endpoint.status === 'warning' ? 'Chậm' : 'Lỗi'}
                              </span>
                            </td>
                            <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                              <span style={{
                                fontSize: '14px',
                                fontWeight: '600',
                                color: endpoint.responseTime > 200 ? '#F59E0B' : endpoint.responseTime === 0 ? '#DC2626' : '#059669'
                              }}>
                                {endpoint.responseTime > 0 ? `${endpoint.responseTime}ms` : 'N/A'}
                              </span>
                            </td>
                            <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                              <span style={{ fontSize: '14px', color: '#4A5568' }}>{endpoint.requests.toLocaleString()}</span>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </>
        )}

        {/* Logs Tab */}
        {activeTab === 'logs' && (
          <div style={{
            background: '#fff',
            borderRadius: '16px',
            boxShadow: '0 4px 20px rgba(0,0,0,0.06)',
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
                    placeholder="Tìm kiếm log..."
                    value={searchLog}
                    onChange={(e) => setSearchLog(e.target.value)}
                    style={{
                      padding: '10px 12px 10px 40px',
                      border: '2px solid #E2E8F0',
                      borderRadius: '10px',
                      fontSize: '14px',
                      width: '300px',
                      outline: 'none'
                    }}
                  />
                </div>
                <select
                  value={logFilter}
                  onChange={(e) => setLogFilter(e.target.value)}
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
                  <option value="all">Tất cả mức độ</option>
                  <option value="error">Error</option>
                  <option value="warning">Warning</option>
                  <option value="info">Info</option>
                  <option value="debug">Debug</option>
                </select>
              </div>
              <button style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '10px 16px',
                background: '#F7FAFC',
                border: '2px solid #E2E8F0',
                borderRadius: '10px',
                fontSize: '13px',
                fontWeight: '600',
                color: '#4A5568',
                cursor: 'pointer'
              }}>
                <Download size={16} />
                Xuất logs
              </button>
            </div>

            {/* Logs List */}
            <div style={{ padding: '16px 24px' }}>
              {filteredLogs.map((log) => {
                const levelStyle = getLogLevelStyle(log.level);
                const LevelIcon = levelStyle.icon;
                return (
                  <div key={log.id} style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: '16px',
                    padding: '16px',
                    background: '#F7FAFC',
                    borderRadius: '12px',
                    marginBottom: '12px',
                    border: `1px solid ${levelStyle.bg}`
                  }}>
                    <div style={{
                      width: '32px',
                      height: '32px',
                      borderRadius: '8px',
                      background: levelStyle.bg,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0
                    }}>
                      <LevelIcon size={16} color={levelStyle.color} />
                    </div>
                    <div style={{ flex: 1 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '6px' }}>
                        <span style={{
                          padding: '2px 8px',
                          borderRadius: '4px',
                          fontSize: '11px',
                          fontWeight: '700',
                          textTransform: 'uppercase',
                          background: levelStyle.bg,
                          color: levelStyle.color
                        }}>{log.level}</span>
                        <span style={{ fontSize: '12px', fontWeight: '600', color: '#4A5568' }}>{log.service}</span>
                        <span style={{ fontSize: '12px', color: '#A0AEC0' }}>{formatDate(log.timestamp)}</span>
                      </div>
                      <div style={{ fontSize: '14px', color: '#1A1A1A' }}>{log.message}</div>
                      {log.user && (
                        <div style={{ fontSize: '12px', color: '#A0AEC0', marginTop: '6px' }}>User: {log.user}</div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Backup Tab */}
        {activeTab === 'backup' && (
          <div style={{ display: 'grid', gridTemplateColumns: '400px 1fr', gap: '24px' }}>
            {/* Backup Actions */}
            <div style={{
              background: '#fff',
              borderRadius: '16px',
              boxShadow: '0 4px 20px rgba(0,0,0,0.06)',
              overflow: 'hidden',
              height: 'fit-content'
            }}>
              <div style={{ padding: '20px 24px', borderBottom: '1px solid #E2E8F0' }}>
                <h3 style={{ fontSize: '16px', fontWeight: '700', color: '#1A1A1A', margin: 0 }}>Sao lưu thủ công</h3>
              </div>
              <div style={{ padding: '24px' }}>
                <div style={{
                  padding: '24px',
                  background: '#F7FAFC',
                  borderRadius: '12px',
                  textAlign: 'center',
                  marginBottom: '20px'
                }}>
                  <HardDrive size={48} color="#FF751F" style={{ marginBottom: '12px' }} />
                  <p style={{ fontSize: '14px', color: '#4A5568', margin: '0 0 16px' }}>
                    Tạo bản sao lưu database ngay lập tức
                  </p>
                  <button 
                    onClick={handleManualBackup}
                    disabled={isBackingUp}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '8px',
                      width: '100%',
                      padding: '14px',
                      background: isBackingUp ? '#A0AEC0' : '#FF751F',
                      border: 'none',
                      borderRadius: '12px',
                      fontSize: '14px',
                      fontWeight: '600',
                      color: '#fff',
                      cursor: isBackingUp ? 'not-allowed' : 'pointer'
                    }}
                  >
                    {isBackingUp ? (
                      <>
                        <RefreshCw size={18} className="spin" />
                        Đang sao lưu...
                      </>
                    ) : (
                      <>
                        <Save size={18} />
                        Sao lưu ngay
                      </>
                    )}
                  </button>
                </div>

                <h4 style={{ fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '12px' }}>Lịch sao lưu tự động</h4>
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '16px',
                  background: '#D1FAE5',
                  borderRadius: '12px',
                  border: '1px solid #A7F3D0',
                  marginBottom: '16px'
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <CheckCircle size={18} color="#059669" />
                    <span style={{ fontSize: '14px', fontWeight: '600', color: '#059669' }}>Đã kích hoạt</span>
                  </div>
                  <span style={{ fontSize: '13px', color: '#059669' }}>Hàng ngày lúc 03:00</span>
                </div>

                <div style={{ marginBottom: '16px' }}>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                    Thời gian sao lưu
                  </label>
                  <input type="time" defaultValue="03:00" style={{
                    width: '100%',
                    padding: '12px 16px',
                    border: '2px solid #E2E8F0',
                    borderRadius: '12px',
                    fontSize: '14px',
                    outline: 'none'
                  }} />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                    Giữ lại số bản sao lưu
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
                    <option value="7">7 ngày gần nhất</option>
                    <option value="14">14 ngày gần nhất</option>
                    <option value="30">30 ngày gần nhất</option>
                  </select>
                </div>
              </div>
            </div>

            {/* Backup History */}
            <div style={{
              background: '#fff',
              borderRadius: '16px',
              boxShadow: '0 4px 20px rgba(0,0,0,0.06)',
              overflow: 'hidden'
            }}>
              <div style={{ padding: '20px 24px', borderBottom: '1px solid #E2E8F0' }}>
                <h3 style={{ fontSize: '16px', fontWeight: '700', color: '#1A1A1A', margin: 0 }}>Lịch sử sao lưu</h3>
              </div>
              <div style={{ padding: '16px 24px' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                  <thead>
                    <tr>
                      {['Thời gian', 'Kích thước', 'Thời lượng', 'Trạng thái', 'Thao tác'].map((header, idx) => (
                        <th key={idx} style={{
                          textAlign: idx === 4 ? 'center' : 'left',
                          padding: '12px 16px',
                          fontSize: '12px',
                          fontWeight: '600',
                          color: '#A0AEC0',
                          textTransform: 'uppercase',
                          letterSpacing: '0.5px',
                          borderBottom: '2px solid #E2E8F0'
                        }}>{header}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {BACKUP_HISTORY.map((backup, idx) => (
                      <tr key={backup.id} style={{ borderBottom: '1px solid #E2E8F0' }}>
                        <td style={{ padding: '16px' }}>
                          <div style={{ fontSize: '14px', fontWeight: '500', color: '#1A1A1A' }}>{formatDate(backup.date)}</div>
                        </td>
                        <td style={{ padding: '16px' }}>
                          <span style={{ fontSize: '14px', color: '#4A5568' }}>{backup.size}</span>
                        </td>
                        <td style={{ padding: '16px' }}>
                          <span style={{ fontSize: '14px', color: '#4A5568' }}>{backup.duration}</span>
                        </td>
                        <td style={{ padding: '16px' }}>
                          <span style={{
                            padding: '4px 12px',
                            borderRadius: '20px',
                            fontSize: '12px',
                            fontWeight: '600',
                            background: backup.status === 'success' ? '#D1FAE5' : '#FEE2E2',
                            color: backup.status === 'success' ? '#059669' : '#DC2626'
                          }}>
                            {backup.status === 'success' ? 'Thành công' : 'Thất bại'}
                          </span>
                        </td>
                        <td style={{ padding: '16px', textAlign: 'center' }}>
                          <button style={{
                            padding: '8px 12px',
                            background: '#F7FAFC',
                            border: '1px solid #E2E8F0',
                            borderRadius: '8px',
                            fontSize: '12px',
                            fontWeight: '600',
                            color: '#4A5568',
                            cursor: 'pointer',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '6px'
                          }}>
                            <Download size={14} />
                            Tải xuống
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </div>

      <style>{`
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
        .spin {
          animation: spin 1s linear infinite;
        }
      `}</style>
    </>
  );
};

export default SystemHealth;
