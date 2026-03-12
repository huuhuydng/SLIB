import React, { useState, useEffect, useMemo, useCallback } from 'react';
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
  Pause,
  ChevronLeft,
  ChevronRight
} from 'lucide-react';
import systemHealthService from '../../../services/admin/systemHealthService';


const SystemHealth = () => {
  const [activeTab, setActiveTab] = useState('overview');

  // === SYSTEM INFO STATE (FE-55) ===
  const [systemInfo, setSystemInfo] = useState(null);
  const [loadingInfo, setLoadingInfo] = useState(true);

  // === SYSTEM LOGS STATE (FE-56) ===
  const [logs, setLogs] = useState([]);
  const [logStats, setLogStats] = useState({});
  const [logFilter, setLogFilter] = useState('all');
  const [logCategory, setLogCategory] = useState('all');
  const [searchLog, setSearchLog] = useState('');
  const [logPage, setLogPage] = useState(0);
  const [logTotalPages, setLogTotalPages] = useState(0);
  const [loadingLogs, setLoadingLogs] = useState(false);

  // === BACKUP STATE (FE-57/58) ===
  const [isBackingUp, setIsBackingUp] = useState(false);
  const [backupHistory, setBackupHistory] = useState([]);
  const [backupSchedule, setBackupSchedule] = useState(null);
  const [scheduleTime, setScheduleTime] = useState('03:00');
  const [scheduleRetainDays, setScheduleRetainDays] = useState(30);
  const [scheduleActive, setScheduleActive] = useState(false);
  const [loadingBackup, setLoadingBackup] = useState(false);

  // =========================================
  // === DATA FETCHING ===
  // =========================================

  const fetchSystemInfo = useCallback(async () => {
    try {
      setLoadingInfo(true);
      const data = await systemHealthService.getSystemInfo();
      setSystemInfo(data);
    } catch (e) {
      console.error('Error fetching system info:', e);
    } finally {
      setLoadingInfo(false);
    }
  }, []);

  const fetchLogs = useCallback(async () => {
    try {
      setLoadingLogs(true);
      const params = { page: logPage, size: 20 };
      if (logFilter !== 'all') params.level = logFilter.toUpperCase();
      if (logCategory !== 'all') params.category = logCategory.toUpperCase();
      if (searchLog) params.search = searchLog;

      const data = await systemHealthService.getLogs(params);
      setLogs(data.content || []);
      setLogTotalPages(data.totalPages || 0);

      const stats = await systemHealthService.getLogStats();
      setLogStats(stats);
    } catch (e) {
      console.error('Error fetching logs:', e);
    } finally {
      setLoadingLogs(false);
    }
  }, [logFilter, logCategory, searchLog, logPage]);

  const fetchBackupData = useCallback(async () => {
    try {
      setLoadingBackup(true);
      const [history, schedule] = await Promise.all([
        systemHealthService.getBackupHistory(),
        systemHealthService.getBackupSchedule()
      ]);
      setBackupHistory(history || []);
      setBackupSchedule(schedule);
      if (schedule) {
        setScheduleTime(schedule.time || '03:00');
        setScheduleRetainDays(schedule.retainDays || 30);
        setScheduleActive(schedule.isActive || false);
      }
    } catch (e) {
      console.error('Error fetching backup data:', e);
    } finally {
      setLoadingBackup(false);
    }
  }, []);

  // Auto-fetch on tab change
  useEffect(() => {
    if (activeTab === 'overview') fetchSystemInfo();
    if (activeTab === 'logs') fetchLogs();
    if (activeTab === 'backup') fetchBackupData();
  }, [activeTab, fetchSystemInfo, fetchLogs, fetchBackupData]);

  // Auto-refresh system info every 30s
  useEffect(() => {
    if (activeTab !== 'overview') return;
    const interval = setInterval(fetchSystemInfo, 30000);
    return () => clearInterval(interval);
  }, [activeTab, fetchSystemInfo]);

  // =========================================
  // === HANDLERS ===
  // =========================================

  const handleManualBackup = async () => {
    setIsBackingUp(true);
    try {
      await systemHealthService.triggerBackup();
      await fetchBackupData();
    } catch (e) {
      console.error('Backup failed:', e);
      alert('Sao lưu thất bại: ' + (e.response?.data?.message || e.message));
    } finally {
      setIsBackingUp(false);
    }
  };

  const handleSaveSchedule = async () => {
    try {
      await systemHealthService.updateBackupSchedule({
        time: scheduleTime,
        retainDays: scheduleRetainDays,
        isActive: scheduleActive
      });
      await fetchBackupData();
    } catch (e) {
      console.error('Failed to save schedule:', e);
    }
  };

  const handleDownloadBackup = (backupId) => {
    systemHealthService.downloadBackup(backupId);
  };

  // =========================================
  // === HELPERS ===
  // =========================================

  const getLogLevelStyle = (level) => {
    switch (level) {
      case 'ERROR': return { bg: '#FEE2E2', color: '#DC2626', icon: XCircle };
      case 'WARN': return { bg: '#FEF3C7', color: '#F59E0B', icon: AlertTriangle };
      case 'INFO': return { bg: '#DBEAFE', color: '#2563EB', icon: Info };
      case 'DEBUG': return { bg: '#F3E8FF', color: '#7C3AED', icon: Bug };
      default: return { bg: '#F3F4F6', color: '#6B7280', icon: Info };
    }
  };

  const getCategoryLabel = (cat) => {
    switch (cat) {
      case 'SYSTEM_ERROR': return 'Lỗi hệ thống';
      case 'PERFORMANCE': return 'Hiệu năng';
      case 'BACKGROUND_JOB': return 'Tác vụ nền';
      case 'INTEGRATION': return 'Tích hợp';
      case 'AUDIT': return 'Quản trị';
      default: return cat;
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A';
    const date = new Date(dateStr);
    return date.toLocaleString('vi-VN');
  };

  const getMetricColor = (value, thresholds = { warn: 70, danger: 90 }) => {
    if (value >= thresholds.danger) return '#DC2626';
    if (value >= thresholds.warn) return '#F59E0B';
    return '#059669';
  };

  // =========================================
  // === RENDER ===
  // =========================================

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
              Giám sát hệ thống
            </h1>
            <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>
              Theo dõi sức khỏe và hiệu suất của SLIB
            </p>
          </div>
          <div style={{ display: 'flex', gap: '12px' }}>
            <button
              onClick={() => {
                if (activeTab === 'overview') fetchSystemInfo();
                if (activeTab === 'logs') fetchLogs();
                if (activeTab === 'backup') fetchBackupData();
              }}
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
              }}>
              <RefreshCw size={18} />
              Làm mới
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
                background: activeTab === tab.id ? '#e8600a' : 'transparent',
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

        {/* ========== OVERVIEW TAB (FE-55) ========== */}
        {activeTab === 'overview' && (
          <>
            {loadingInfo && !systemInfo ? (
              <div style={{ textAlign: 'center', padding: '60px', color: '#A0AEC0' }}>Đang tải...</div>
            ) : systemInfo ? (
              <>
                {/* System Metrics */}
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(4, 1fr)',
                  gap: '16px',
                  marginBottom: '24px'
                }}>
                  {[
                    { label: 'CPU Usage', value: systemInfo.cpu, unit: '%', icon: Cpu },
                    { label: 'Memory Usage', value: systemInfo.memory, unit: '%', icon: MemoryStick, sub: `${systemInfo.memoryUsedMB} / ${systemInfo.memoryMaxMB} MB` },
                    { label: 'Disk Usage', value: systemInfo.disk, unit: '%', icon: HardDrive, sub: `${systemInfo.diskUsedGB} / ${systemInfo.diskTotalGB} GB` },
                    { label: 'Processors', value: systemInfo.availableProcessors, unit: ' cores', icon: Zap },
                  ].map((metric, idx) => {
                    const color = getMetricColor(typeof metric.value === 'number' && metric.unit === '%' ? metric.value : 0);
                    return (
                      <div key={idx} style={{
                        background: '#fff',
                        borderRadius: '10px',
                        padding: '24px',
                        boxShadow: '0 1px 3px rgba(0,0,0,0.04)'
                      }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '16px' }}>
                          <div style={{
                            width: '48px', height: '48px', borderRadius: '12px',
                            background: color === '#059669' ? '#D1FAE5' : color === '#F59E0B' ? '#FEF3C7' : '#FEE2E2',
                            display: 'flex', alignItems: 'center', justifyContent: 'center'
                          }}>
                            <metric.icon size={24} color={color} />
                          </div>
                        </div>
                        <div style={{ fontSize: '20px', fontWeight: '600', color: '#1A1A1A', marginBottom: '4px' }}>
                          {typeof metric.value === 'number' ? metric.value.toFixed(1) : metric.value}{metric.unit}
                        </div>
                        <div style={{ fontSize: '13px', color: '#A0AEC0' }}>{metric.label}</div>
                        {metric.sub && <div style={{ fontSize: '12px', color: '#A0AEC0', marginTop: '4px' }}>{metric.sub}</div>}
                        {metric.unit === '%' && (
                          <div style={{ marginTop: '12px', height: '6px', background: '#E2E8F0', borderRadius: '100px', overflow: 'hidden' }}>
                            <div style={{
                              height: '100%', width: `${metric.value}%`,
                              background: color, borderRadius: '100px', transition: 'width 0.5s ease'
                            }} />
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>

                {/* System Info Panel */}
                <div style={{
                  background: '#fff', borderRadius: '10px',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.04)', overflow: 'hidden'
                }}>
                  <div style={{ padding: '20px 24px', borderBottom: '1px solid #E2E8F0' }}>
                    <h3 style={{ fontSize: '16px', fontWeight: '600', color: '#1A1A1A', margin: 0 }}>Thông tin hệ thống</h3>
                  </div>
                  <div style={{ padding: '24px', display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '20px' }}>
                    {[
                      { label: 'Thời gian hoạt động', value: systemInfo.uptime, icon: Clock },
                      { label: 'Hệ điều hành', value: `${systemInfo.osName} ${systemInfo.osVersion}`, icon: Server },
                      { label: 'Kiến trúc', value: systemInfo.osArch, icon: Cpu },
                      { label: 'Java Version', value: systemInfo.javaVersion, icon: Zap },
                      { label: 'Java Vendor', value: systemInfo.javaVendor, icon: Shield },
                      { label: 'Database', value: 'PostgreSQL', icon: Database },
                    ].map((item, idx) => (
                      <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '12px', background: '#F7FAFC', borderRadius: '10px' }}>
                        <div style={{
                          width: '36px', height: '36px', borderRadius: '10px', background: '#fff',
                          display: 'flex', alignItems: 'center', justifyContent: 'center'
                        }}>
                          <item.icon size={18} color="#4A5568" />
                        </div>
                        <div>
                          <div style={{ fontSize: '12px', color: '#A0AEC0', marginBottom: '2px' }}>{item.label}</div>
                          <div style={{ fontSize: '14px', fontWeight: '600', color: '#1A1A1A' }}>{item.value}</div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </>
            ) : (
              <div style={{ textAlign: 'center', padding: '60px', color: '#A0AEC0' }}>Không thể tải thông tin hệ thống</div>
            )}
          </>
        )}

        {/* ========== LOGS TAB (FE-56) ========== */}
        {activeTab === 'logs' && (
          <div style={{
            background: '#fff', borderRadius: '10px',
            boxShadow: '0 1px 3px rgba(0,0,0,0.04)', overflow: 'hidden'
          }}>
            {/* Filter Bar */}
            <div style={{
              padding: '20px 24px',
              borderBottom: '1px solid #E2E8F0',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              flexWrap: 'wrap',
              gap: '12px'
            }}>
              <div style={{ display: 'flex', gap: '12px', alignItems: 'center', flexWrap: 'wrap' }}>
                <div style={{ position: 'relative' }}>
                  <Search size={16} style={{
                    position: 'absolute', left: '12px', top: '50%',
                    transform: 'translateY(-50%)', color: '#A0AEC0'
                  }} />
                  <input
                    type="text"
                    placeholder="Tìm kiếm log..."
                    value={searchLog}
                    onChange={(e) => { setSearchLog(e.target.value); setLogPage(0); }}
                    style={{
                      padding: '10px 12px 10px 40px',
                      border: '2px solid #E2E8F0',
                      borderRadius: '10px',
                      fontSize: '14px',
                      width: '250px',
                      outline: 'none'
                    }}
                  />
                </div>
                <select
                  value={logFilter}
                  onChange={(e) => { setLogFilter(e.target.value); setLogPage(0); }}
                  style={{
                    padding: '10px 16px', border: '2px solid #E2E8F0', borderRadius: '10px',
                    fontSize: '14px', color: '#4A5568', background: '#fff', cursor: 'pointer'
                  }}
                >
                  <option value="all">Tất cả mức độ</option>
                  <option value="ERROR">Error</option>
                  <option value="WARN">Warning</option>
                  <option value="INFO">Info</option>
                  <option value="DEBUG">Debug</option>
                </select>
                <select
                  value={logCategory}
                  onChange={(e) => { setLogCategory(e.target.value); setLogPage(0); }}
                  style={{
                    padding: '10px 16px', border: '2px solid #E2E8F0', borderRadius: '10px',
                    fontSize: '14px', color: '#4A5568', background: '#fff', cursor: 'pointer'
                  }}
                >
                  <option value="all">Tất cả loại</option>
                  <option value="SYSTEM_ERROR">Lỗi hệ thống</option>
                  <option value="PERFORMANCE">Hiệu năng</option>
                  <option value="BACKGROUND_JOB">Tác vụ nền</option>
                  <option value="INTEGRATION">Tích hợp</option>
                  <option value="AUDIT">Quản trị</option>
                </select>
              </div>
              {/* Stats badges */}
              <div style={{ display: 'flex', gap: '8px' }}>
                {logStats.errorsLast24h != null && (
                  <span style={{ padding: '6px 12px', borderRadius: '8px', fontSize: '12px', fontWeight: '600', background: '#FEE2E2', color: '#DC2626' }}>
                    {logStats.errorsLast24h} lỗi (24h)
                  </span>
                )}
                {logStats.warningsLast24h != null && (
                  <span style={{ padding: '6px 12px', borderRadius: '8px', fontSize: '12px', fontWeight: '600', background: '#FEF3C7', color: '#F59E0B' }}>
                    {logStats.warningsLast24h} cảnh báo (24h)
                  </span>
                )}
              </div>
            </div>

            {/* Logs List */}
            <div style={{ padding: '16px 24px' }}>
              {loadingLogs ? (
                <div style={{ textAlign: 'center', padding: '40px', color: '#A0AEC0' }}>Đang tải...</div>
              ) : logs.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '40px', color: '#A0AEC0' }}>Chưa có nhật ký nào</div>
              ) : (
                <>
                  {logs.map((logItem, idx) => {
                    const levelStyle = getLogLevelStyle(logItem.level);
                    const LevelIcon = levelStyle.icon;
                    return (
                      <div key={logItem.id || idx} style={{
                        display: 'flex', alignItems: 'flex-start', gap: '16px',
                        padding: '16px', background: '#F7FAFC', borderRadius: '12px',
                        marginBottom: '12px', border: `1px solid ${levelStyle.bg}`
                      }}>
                        <div style={{
                          width: '32px', height: '32px', borderRadius: '8px', background: levelStyle.bg,
                          display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0
                        }}>
                          <LevelIcon size={16} color={levelStyle.color} />
                        </div>
                        <div style={{ flex: 1 }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px', flexWrap: 'wrap' }}>
                            <span style={{
                              padding: '2px 8px', borderRadius: '4px', fontSize: '11px', fontWeight: '600',
                              textTransform: 'uppercase', background: levelStyle.bg, color: levelStyle.color
                            }}>{logItem.level}</span>
                            {logItem.category && (
                              <span style={{
                                padding: '2px 8px', borderRadius: '4px', fontSize: '11px', fontWeight: '600',
                                background: '#E2E8F0', color: '#4A5568'
                              }}>{getCategoryLabel(logItem.category)}</span>
                            )}
                            {logItem.service && (
                              <span style={{ fontSize: '12px', fontWeight: '600', color: '#4A5568' }}>{logItem.service}</span>
                            )}
                            <span style={{ fontSize: '12px', color: '#A0AEC0' }}>{formatDate(logItem.createdAt)}</span>
                          </div>
                          <div style={{ fontSize: '14px', color: '#1A1A1A' }}>{logItem.message}</div>
                          {logItem.details?.info && (
                            <div style={{ fontSize: '12px', color: '#A0AEC0', marginTop: '4px', fontFamily: 'monospace' }}>
                              {logItem.details.info}
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })}

                  {/* Pagination */}
                  {logTotalPages > 1 && (
                    <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', marginTop: '16px' }}>
                      <button
                        disabled={logPage === 0}
                        onClick={() => setLogPage(p => Math.max(0, p - 1))}
                        style={{
                          padding: '8px 12px', border: '1px solid #E2E8F0', borderRadius: '8px',
                          background: '#fff', cursor: logPage === 0 ? 'not-allowed' : 'pointer',
                          opacity: logPage === 0 ? 0.5 : 1, display: 'flex', alignItems: 'center'
                        }}
                      >
                        <ChevronLeft size={16} />
                      </button>
                      <span style={{ padding: '8px 16px', fontSize: '14px', color: '#4A5568' }}>
                        Trang {logPage + 1} / {logTotalPages}
                      </span>
                      <button
                        disabled={logPage >= logTotalPages - 1}
                        onClick={() => setLogPage(p => Math.min(logTotalPages - 1, p + 1))}
                        style={{
                          padding: '8px 12px', border: '1px solid #E2E8F0', borderRadius: '8px',
                          background: '#fff', cursor: logPage >= logTotalPages - 1 ? 'not-allowed' : 'pointer',
                          opacity: logPage >= logTotalPages - 1 ? 0.5 : 1, display: 'flex', alignItems: 'center'
                        }}
                      >
                        <ChevronRight size={16} />
                      </button>
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
        )}

        {/* ========== BACKUP TAB (FE-57/58) ========== */}
        {activeTab === 'backup' && (
          <div style={{ display: 'grid', gridTemplateColumns: '400px 1fr', gap: '24px' }}>
            {/* Backup Actions */}
            <div style={{
              background: '#fff', borderRadius: '10px',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)', overflow: 'hidden', height: 'fit-content'
            }}>
              <div style={{ padding: '20px 24px', borderBottom: '1px solid #E2E8F0' }}>
                <h3 style={{ fontSize: '16px', fontWeight: '600', color: '#1A1A1A', margin: 0 }}>Sao lưu thủ công</h3>
              </div>
              <div style={{ padding: '24px' }}>
                <div style={{
                  padding: '24px', background: '#F7FAFC', borderRadius: '12px',
                  textAlign: 'center', marginBottom: '20px'
                }}>
                  <HardDrive size={48} color="#e8600a" style={{ marginBottom: '12px' }} />
                  <p style={{ fontSize: '14px', color: '#4A5568', margin: '0 0 16px' }}>
                    Tạo bản sao lưu database ngay lập tức
                  </p>
                  <button
                    onClick={handleManualBackup}
                    disabled={isBackingUp}
                    style={{
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      gap: '8px', width: '100%', padding: '14px',
                      background: isBackingUp ? '#A0AEC0' : '#e8600a',
                      border: 'none', borderRadius: '12px', fontSize: '14px', fontWeight: '600',
                      color: '#fff', cursor: isBackingUp ? 'not-allowed' : 'pointer'
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
                  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                  padding: '16px', borderRadius: '12px', marginBottom: '16px',
                  background: scheduleActive ? '#D1FAE5' : '#FEE2E2',
                  border: `1px solid ${scheduleActive ? '#A7F3D0' : '#FECACA'}`
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    {scheduleActive ? <CheckCircle size={18} color="#059669" /> : <XCircle size={18} color="#DC2626" />}
                    <span style={{ fontSize: '14px', fontWeight: '600', color: scheduleActive ? '#059669' : '#DC2626' }}>
                      {scheduleActive ? 'Đã kích hoạt' : 'Chưa kích hoạt'}
                    </span>
                  </div>
                  <button
                    onClick={() => setScheduleActive(!scheduleActive)}
                    style={{
                      padding: '4px 12px', borderRadius: '6px', border: '1px solid #E2E8F0',
                      background: '#fff', fontSize: '12px', fontWeight: '600', cursor: 'pointer',
                      color: '#4A5568'
                    }}
                  >
                    {scheduleActive ? 'Tắt' : 'Bật'}
                  </button>
                </div>

                <div style={{ marginBottom: '16px' }}>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                    Thời gian sao lưu
                  </label>
                  <input
                    type="time"
                    value={scheduleTime}
                    onChange={(e) => setScheduleTime(e.target.value)}
                    style={{
                      width: '100%', padding: '12px 16px', border: '2px solid #E2E8F0',
                      borderRadius: '12px', fontSize: '14px', outline: 'none'
                    }}
                  />
                </div>

                <div style={{ marginBottom: '16px' }}>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                    Giữ lại số bản sao lưu
                  </label>
                  <select
                    value={scheduleRetainDays}
                    onChange={(e) => setScheduleRetainDays(parseInt(e.target.value))}
                    style={{
                      width: '100%', padding: '12px 16px', border: '2px solid #E2E8F0',
                      borderRadius: '12px', fontSize: '14px', color: '#4A5568', background: '#fff', cursor: 'pointer'
                    }}
                  >
                    <option value="7">7 ngày gần nhất</option>
                    <option value="14">14 ngày gần nhất</option>
                    <option value="30">30 ngày gần nhất</option>
                    <option value="60">60 ngày gần nhất</option>
                  </select>
                </div>

                <button
                  onClick={handleSaveSchedule}
                  style={{
                    width: '100%', padding: '12px', background: '#e8600a', border: 'none',
                    borderRadius: '12px', fontSize: '14px', fontWeight: '600', color: '#fff',
                    cursor: 'pointer'
                  }}
                >
                  Lưu cấu hình
                </button>
              </div>
            </div>

            {/* Backup History */}
            <div style={{
              background: '#fff', borderRadius: '10px',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)', overflow: 'hidden'
            }}>
              <div style={{ padding: '20px 24px', borderBottom: '1px solid #E2E8F0' }}>
                <h3 style={{ fontSize: '16px', fontWeight: '600', color: '#1A1A1A', margin: 0 }}>Lịch sử sao lưu</h3>
              </div>
              <div style={{ padding: '16px 24px' }}>
                {loadingBackup ? (
                  <div style={{ textAlign: 'center', padding: '40px', color: '#A0AEC0' }}>Đang tải...</div>
                ) : backupHistory.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '40px', color: '#A0AEC0' }}>Chưa có lịch sử sao lưu</div>
                ) : (
                  <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                    <thead>
                      <tr>
                        {['Thời gian', 'Kích thước', 'Thời lượng', 'Trạng thái', 'Thao tác'].map((header, idx) => (
                          <th key={idx} style={{
                            textAlign: idx === 4 ? 'center' : 'left',
                            padding: '12px 16px', fontSize: '12px', fontWeight: '600', color: '#A0AEC0',
                            textTransform: 'uppercase', letterSpacing: '0.5px', borderBottom: '2px solid #E2E8F0'
                          }}>{header}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {backupHistory.map((backup) => (
                        <tr key={backup.id} style={{ borderBottom: '1px solid #E2E8F0' }}>
                          <td style={{ padding: '16px' }}>
                            <div style={{ fontSize: '14px', fontWeight: '500', color: '#1A1A1A' }}>{formatDate(backup.startedAt)}</div>
                          </td>
                          <td style={{ padding: '16px' }}>
                            <span style={{ fontSize: '14px', color: '#4A5568' }}>{backup.fileSizeFormatted || 'N/A'}</span>
                          </td>
                          <td style={{ padding: '16px' }}>
                            <span style={{ fontSize: '14px', color: '#4A5568' }}>{backup.duration || 'N/A'}</span>
                          </td>
                          <td style={{ padding: '16px' }}>
                            <span style={{
                              padding: '4px 12px', borderRadius: '10px', fontSize: '12px', fontWeight: '600',
                              background: backup.status === 'SUCCESS' ? '#D1FAE5' : '#FEE2E2',
                              color: backup.status === 'SUCCESS' ? '#059669' : '#DC2626'
                            }}>
                              {backup.status === 'SUCCESS' ? 'Thành công' : 'Thất bại'}
                            </span>
                          </td>
                          <td style={{ padding: '16px', textAlign: 'center' }}>
                            {backup.status === 'SUCCESS' && (
                              <button
                                onClick={() => handleDownloadBackup(backup.id)}
                                style={{
                                  padding: '8px 12px', background: '#F7FAFC', border: '1px solid #E2E8F0',
                                  borderRadius: '8px', fontSize: '12px', fontWeight: '600', color: '#4A5568',
                                  cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '6px'
                                }}>
                                <Download size={14} />
                                Tải xuống
                              </button>
                            )}
                            {backup.status === 'FAILED' && backup.errorMessage && (
                              <span style={{ fontSize: '12px', color: '#DC2626' }} title={backup.errorMessage}>Xem lỗi</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
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