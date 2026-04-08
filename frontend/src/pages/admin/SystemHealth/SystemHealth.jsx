import React, { useState, useEffect, useCallback } from 'react';
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
import LoadErrorState from '../../../components/common/LoadErrorState';
import useAppDialog from '../../../hooks/useAppDialog';
import './SystemHealth.css';


const SystemHealth = () => {
  const { confirm, alert } = useAppDialog();
  const [activeTab, setActiveTab] = useState('overview');

  // === SYSTEM INFO STATE (FE-55) ===
  const [systemInfo, setSystemInfo] = useState(null);
  const [loadingInfo, setLoadingInfo] = useState(true);
  const [infoError, setInfoError] = useState('');

  // === SYSTEM LOGS STATE (FE-56) ===
  const [logs, setLogs] = useState([]);
  const [logStats, setLogStats] = useState({});
  const [logFilter, setLogFilter] = useState('all');
  const [logCategory, setLogCategory] = useState('all');
  const [searchLog, setSearchLog] = useState('');
  const [debouncedSearchLog, setDebouncedSearchLog] = useState('');
  const [logPage, setLogPage] = useState(0);
  const [logTotalPages, setLogTotalPages] = useState(0);
  const [loadingLogs, setLoadingLogs] = useState(false);
  const [logsError, setLogsError] = useState('');
  const [exportingLogs, setExportingLogs] = useState(false);
  const [cleaningLogs, setCleaningLogs] = useState(false);
  const [showCleanupPanel, setShowCleanupPanel] = useState(false);
  const [cleanupBeforeDate, setCleanupBeforeDate] = useState('');

  // === BACKUP STATE (FE-57/58) ===
  const [isBackingUp, setIsBackingUp] = useState(false);
  const [backupHistory, setBackupHistory] = useState([]);
  const [backupSchedule, setBackupSchedule] = useState(null);
  const [scheduleTime, setScheduleTime] = useState('03:00');
  const [scheduleRetainDays, setScheduleRetainDays] = useState(30);
  const [scheduleActive, setScheduleActive] = useState(false);
  const [loadingBackup, setLoadingBackup] = useState(false);
  const [savingSchedule, setSavingSchedule] = useState(false);
  const [restoringBackupId, setRestoringBackupId] = useState(null);
  const [backupError, setBackupError] = useState('');

  useEffect(() => {
    const timeout = setTimeout(() => {
      setDebouncedSearchLog(searchLog.trim());
    }, 350);
    return () => clearTimeout(timeout);
  }, [searchLog]);

  // =========================================
  // === DATA FETCHING ===
  // =========================================

  const fetchSystemInfo = useCallback(async () => {
    try {
      setLoadingInfo(true);
      setInfoError('');
      const data = await systemHealthService.getSystemInfo();
      setSystemInfo(data);
    } catch (e) {
      console.error('Error fetching system info:', e);
      setInfoError(parseApiError(e, 'Không thể tải tình trạng hệ thống'));
    } finally {
      setLoadingInfo(false);
    }
  }, []);

  const fetchLogs = useCallback(async () => {
    try {
      setLoadingLogs(true);
      setLogsError('');
      const params = { page: logPage, size: 20 };
      if (logFilter !== 'all') params.level = logFilter.toUpperCase();
      if (logCategory !== 'all') params.category = logCategory.toUpperCase();
      if (debouncedSearchLog) params.search = debouncedSearchLog;

      const [data, stats] = await Promise.all([
        systemHealthService.getLogs(params),
        systemHealthService.getLogStats()
      ]);
      setLogs(data.content || []);
      setLogTotalPages(data.totalPages || 0);
      setLogStats(stats);
    } catch (e) {
      console.error('Error fetching logs:', e);
      setLogsError(parseApiError(e, 'Không thể tải nhật ký hệ thống'));
    } finally {
      setLoadingLogs(false);
    }
  }, [logFilter, logCategory, debouncedSearchLog, logPage]);

  const fetchBackupData = useCallback(async () => {
    try {
      setLoadingBackup(true);
      setBackupError('');
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
      setBackupError(parseApiError(e, 'Không thể tải dữ liệu sao lưu'));
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
      setBackupError('');
      await systemHealthService.triggerBackup();
      await fetchBackupData();
    } catch (e) {
      console.error('Backup failed:', e);
      setBackupError(parseApiError(e, 'Sao lưu dữ liệu thất bại'));
    } finally {
      setIsBackingUp(false);
    }
  };

  const handleSaveSchedule = async () => {
    try {
      setSavingSchedule(true);
      setBackupError('');
      await systemHealthService.updateBackupSchedule({
        time: scheduleTime,
        retainDays: scheduleRetainDays,
        isActive: scheduleActive
      });
      await fetchBackupData();
    } catch (e) {
      console.error('Failed to save schedule:', e);
      setBackupError(parseApiError(e, 'Không thể lưu lịch sao lưu'));
    } finally {
      setSavingSchedule(false);
    }
  };

  const handleDownloadBackup = async (backupId, fileName) => {
    try {
      await systemHealthService.downloadBackup(backupId, fileName);
    } catch (e) {
      console.error('Failed to download backup:', e);
      setBackupError(parseApiError(e, 'Không thể tải file sao lưu'));
    }
  };

  const handleRestoreBackup = async (backupId, fileName) => {
    const confirmed = await confirm({
      title: 'Khôi phục dữ liệu PostgreSQL',
      message: `Bạn có chắc muốn khôi phục dữ liệu PostgreSQL từ file ${fileName || 'đã chọn'}? Thao tác này có thể ghi đè dữ liệu hiện tại và nên thực hiện ngoài giờ vận hành.`,
      confirmText: 'Khôi phục',
      cancelText: 'Huỷ',
      variant: 'danger',
    });
    if (!confirmed) return;

    try {
      setRestoringBackupId(backupId);
      setBackupError('');
      const result = await systemHealthService.restoreBackup(backupId);
      await fetchBackupData();
      await alert({
        title: 'Khôi phục thành công',
        message: result?.message || 'Đã khôi phục dữ liệu PostgreSQL từ bản sao lưu đã chọn.',
        icon: 'success',
      });
    } catch (e) {
      console.error('Failed to restore backup:', e);
      setBackupError(parseApiError(e, 'Không thể khôi phục dữ liệu từ file sao lưu'));
    } finally {
      setRestoringBackupId(null);
    }
  };

  const buildLogQueryParams = () => {
    const params = {};
    if (logFilter !== 'all') params.level = logFilter.toUpperCase();
    if (logCategory !== 'all') params.category = logCategory.toUpperCase();
    if (debouncedSearchLog) params.search = debouncedSearchLog;
    return params;
  };

  const handleExportLogs = async () => {
    try {
      setExportingLogs(true);
      setLogsError('');
      await systemHealthService.exportLogs(buildLogQueryParams());
    } catch (e) {
      console.error('Failed to export logs:', e);
      setLogsError(parseApiError(e, 'Không thể xuất file nhật ký'));
    } finally {
      setExportingLogs(false);
    }
  };

  const handleCleanupLogs = async () => {
    if (!cleanupBeforeDate) {
      setLogsError('Vui lòng chọn ngày mốc để dọn nhật ký');
      return;
    }

    const confirmed = await confirm({
      title: 'Dọn nhật ký hệ thống',
      message: `Bạn có chắc muốn dọn toàn bộ nhật ký trước ngày ${cleanupBeforeDate}?`,
      confirmText: 'Dọn nhật ký',
      cancelText: 'Huỷ',
      variant: 'warning',
    });
    if (!confirmed) {
      return;
    }

    try {
      setCleaningLogs(true);
      setLogsError('');
      const result = await systemHealthService.cleanupLogs(cleanupBeforeDate);
      await fetchLogs();
      setShowCleanupPanel(false);
      setCleanupBeforeDate('');
      await alert({
        title: 'Dọn nhật ký thành công',
        message: result?.message || 'Đã dọn nhật ký cũ thành công',
        icon: 'success',
      });
    } catch (e) {
      console.error('Failed to cleanup logs:', e);
      setLogsError(parseApiError(e, 'Không thể dọn nhật ký cũ'));
    } finally {
      setCleaningLogs(false);
    }
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

  const parseApiError = (error, fallback) => {
    return error?.response?.data?.message
      || error?.response?.data?.error
      || error?.message
      || fallback;
  };

  const getMetricColor = (value, thresholds = { warn: 70, danger: 90 }) => {
    if (value >= thresholds.danger) return '#DC2626';
    if (value >= thresholds.warn) return '#F59E0B';
    return '#059669';
  };

  const getServiceStatusMeta = (status) => {
    switch (status) {
      case 'UP':
        return { bg: '#D1FAE5', color: '#059669', label: 'Ổn định', icon: CheckCircle };
      case 'DEGRADED':
        return { bg: '#FEF3C7', color: '#D97706', label: 'Cần theo dõi', icon: AlertTriangle };
      case 'DOWN':
        return { bg: '#FEE2E2', color: '#DC2626', label: 'Mất kết nối', icon: XCircle };
      case 'CRITICAL':
        return { bg: '#FEE2E2', color: '#DC2626', label: 'Sự cố nghiêm trọng', icon: AlertCircle };
      default:
        return { bg: '#E2E8F0', color: '#64748B', label: 'Chưa xác định', icon: Info };
    }
  };

  const tabDefinitions = [
    { id: 'overview', label: 'Giám sát', icon: Activity },
    { id: 'logs', label: 'Nhật ký', icon: FileText },
    { id: 'backup', label: 'Sao lưu', icon: HardDrive },
  ];

  const activeTabMeta = {
    overview: {
      title: 'Giám sát hệ thống',
      description: 'Theo dõi trạng thái backend, cơ sở dữ liệu, dịch vụ AI và tài nguyên máy chủ để phát hiện sớm các dấu hiệu bất thường.',
      chips: [
        systemInfo?.overallStatus ? `Tổng thể: ${getServiceStatusMeta(systemInfo.overallStatus).label}` : 'Đang kiểm tra trạng thái',
        'Làm mới tự động mỗi 30 giây',
        'Số liệu ưu tiên cho vận hành thư viện',
      ],
      note: 'Tab này phản ánh tình trạng vận hành hiện tại của API, cơ sở dữ liệu, AI Service và tài nguyên máy chủ. Các chỉ số CPU/RAM/đĩa được dùng để phát hiện tải cao hoặc nguy cơ đầy bộ nhớ, đầy dung lượng.'
    },
    logs: {
      title: 'Nhật ký vận hành',
      description: 'Tra cứu lỗi, cảnh báo, tác vụ nền và hoạt động quản trị để xác định nguyên nhân sự cố và kiểm tra lịch sử thao tác.',
      chips: [
        `${logStats.errorsLast24h ?? 0} lỗi trong 24 giờ`,
        `${logStats.warningsLast24h ?? 0} cảnh báo trong 24 giờ`,
        'Tìm kiếm theo nội dung, mức độ và loại nhật ký',
      ],
      note: 'Nhật ký được lưu theo nhóm lỗi hệ thống, hiệu năng, tích hợp, tác vụ nền và thao tác quản trị. Dùng tab này để truy vết nguyên nhân khi có sự cố vận hành.'
    },
    backup: {
      title: 'Sao lưu và khôi phục PostgreSQL',
      description: 'Quản lý sao lưu thủ công, lịch sao lưu tự động và khôi phục dữ liệu PostgreSQL khi cần phục hồi dữ liệu thư viện.',
      chips: [
        scheduleActive ? 'Lịch sao lưu đang bật' : 'Lịch sao lưu đang tắt',
        `Giữ tối đa ${scheduleRetainDays} ngày`,
        'Chỉ áp dụng cho dữ liệu PostgreSQL (.dump)',
      ],
      note: 'Tab này hiện chỉ sao lưu và khôi phục dữ liệu PostgreSQL của hệ thống SLIB. Chưa bao gồm MongoDB chat history, Qdrant vector DB, Redis hay các file upload ngoài cơ sở dữ liệu.'
    }
  }[activeTab];

  // =========================================
  // === RENDER ===
  // =========================================

  return (
    <>
      <div className="sh-page">
        {/* Page Header */}
        <div className="sh-hero">
          <div>
            <h1 className="sh-hero__title">{activeTabMeta.title}</h1>
            <p className="sh-hero__desc">{activeTabMeta.description}</p>
            <div className="sh-hero__chips">
              {activeTabMeta.chips.map((chip) => (
                <span key={chip} className="sh-chip">{chip}</span>
              ))}
            </div>
          </div>
          <div className="sh-hero__actions">
            <button
              onClick={() => {
                if (activeTab === 'overview') fetchSystemInfo();
                if (activeTab === 'logs') fetchLogs();
                if (activeTab === 'backup') fetchBackupData();
              }}
              className="sh-refresh">
              <RefreshCw size={18} />
              Làm mới
            </button>
          </div>
        </div>

        {/* Tabs */}
        <div className="sh-tabbar">
          {tabDefinitions.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`sh-tab${activeTab === tab.id ? ' sh-tab--active' : ''}`}
            >
              <tab.icon size={18} />
              {tab.label}
            </button>
          ))}
        </div>

        <div className="sh-section-note">
          <strong>{activeTabMeta.title}:</strong> {activeTabMeta.note}
        </div>

        {/* ========== OVERVIEW TAB (FE-55) ========== */}
        {activeTab === 'overview' && (
          <>
            {loadingInfo && !systemInfo ? (
              <div style={{ textAlign: 'center', padding: '60px', color: '#A0AEC0' }}>Đang tải...</div>
            ) : infoError ? (
              <div style={{
                background: '#fff',
                borderRadius: '12px',
                boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
              }}>
                <LoadErrorState
                  title="Không thể tải tình trạng hệ thống"
                  message={infoError}
                  onRetry={fetchSystemInfo}
                  compact
                />
              </div>
            ) : systemInfo ? (
              <>
                {(() => {
                  const serviceMeta = getServiceStatusMeta(systemInfo.overallStatus);
                  const ServiceIcon = serviceMeta.icon;
                  const services = Object.values(systemInfo.services || {});
                  const metrics = [
                    { label: 'CPU máy chủ', value: systemInfo.cpu, unit: '%', icon: Cpu, sub: `${systemInfo.availableProcessors} lõi xử lý` },
                    {
                      label: 'RAM máy chủ',
                      value: systemInfo.memory,
                      unit: '%',
                      icon: MemoryStick,
                      sub: systemInfo.systemMemoryTotalMB > 0
                        ? `${systemInfo.systemMemoryUsedMB} / ${systemInfo.systemMemoryTotalMB} MB`
                        : 'Không lấy được RAM vật lý'
                    },
                    {
                      label: 'Bộ nhớ JVM',
                      value: systemInfo.jvmMemory,
                      unit: '%',
                      icon: Server,
                      sub: `${systemInfo.memoryUsedMB} / ${systemInfo.memoryMaxMB} MB`
                    },
                    { label: 'Dung lượng đĩa', value: systemInfo.disk, unit: '%', icon: HardDrive, sub: `${systemInfo.diskUsedGB} / ${systemInfo.diskTotalGB} GB` }
                  ];

                  return (
                    <>
                      <div style={{
                        background: '#fff',
                        borderRadius: '14px',
                        padding: '20px 24px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        marginBottom: '24px',
                        border: `1px solid ${serviceMeta.bg}`,
                        boxShadow: '0 1px 3px rgba(0,0,0,0.04)'
                      }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                          <div style={{
                            width: '44px',
                            height: '44px',
                            borderRadius: '12px',
                            background: serviceMeta.bg,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center'
                          }}>
                            <ServiceIcon size={22} color={serviceMeta.color} />
                          </div>
                          <div>
                            <div style={{ fontSize: '16px', fontWeight: '700', color: '#1A1A1A' }}>
                              Tình trạng tổng thể: <span style={{ color: serviceMeta.color }}>{serviceMeta.label}</span>
                            </div>
                            <div style={{ fontSize: '13px', color: '#64748B', marginTop: '4px' }}>
                              Cập nhật lúc {formatDate(systemInfo.checkedAt)}
                            </div>
                          </div>
                        </div>
                        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                          {services.map((service, idx) => {
                            const meta = getServiceStatusMeta(service.status);
                            return (
                              <span key={idx} style={{
                                padding: '8px 12px',
                                borderRadius: '999px',
                                background: meta.bg,
                                color: meta.color,
                                fontSize: '12px',
                                fontWeight: '700'
                              }}>
                                {service.label}: {meta.label}
                              </span>
                            );
                          })}
                        </div>
                      </div>

                      {/* System Metrics */}
                      <div style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(4, 1fr)',
                        gap: '16px',
                        marginBottom: '24px'
                      }}>
                        {metrics.map((metric, idx) => {
                          const color = getMetricColor(typeof metric.value === 'number' && metric.value >= 0 ? metric.value : 0);
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
                                {typeof metric.value === 'number' && metric.value >= 0 ? metric.value.toFixed(1) : 'N/A'}{metric.unit}
                              </div>
                              <div style={{ fontSize: '13px', color: '#64748B', fontWeight: '600' }}>{metric.label}</div>
                              {metric.sub && <div style={{ fontSize: '12px', color: '#A0AEC0', marginTop: '4px' }}>{metric.sub}</div>}
                              {typeof metric.value === 'number' && metric.value >= 0 && (
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
                          <h3 style={{ fontSize: '16px', fontWeight: '600', color: '#1A1A1A', margin: 0 }}>Dịch vụ và môi trường hệ thống</h3>
                        </div>
                        <div style={{ padding: '24px', display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '20px' }}>
                          {[
                            { label: 'Thời gian hoạt động', value: systemInfo.uptime, icon: Clock },
                            { label: 'Hệ điều hành', value: `${systemInfo.osName} ${systemInfo.osVersion}`, icon: Server },
                            { label: 'Kiến trúc', value: systemInfo.osArch, icon: Cpu },
                            { label: 'Phiên bản Java', value: systemInfo.javaVersion, icon: Zap },
                            { label: 'Nhà cung cấp Java', value: systemInfo.javaVendor, icon: Shield },
                            {
                              label: 'CPU tiến trình backend',
                              value: typeof systemInfo.cpuProcess === 'number' && systemInfo.cpuProcess >= 0
                                ? `${systemInfo.cpuProcess.toFixed(1)}%`
                                : 'Không khả dụng',
                              icon: Activity
                            },
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
                  );
                })()}
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
                    placeholder="Tìm kiếm nhật ký..."
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
                  <option value="ERROR">Lỗi</option>
                  <option value="WARN">Cảnh báo</option>
                  <option value="INFO">Thông tin</option>
                  <option value="DEBUG">Gỡ lỗi</option>
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
                <button
                  onClick={handleExportLogs}
                  disabled={exportingLogs}
                  className="sh-action-btn"
                >
                  <Download size={16} />
                  {exportingLogs ? 'Đang xuất...' : 'Xuất Excel'}
                </button>
                <button
                  onClick={() => setShowCleanupPanel((prev) => !prev)}
                  className="sh-action-btn sh-action-btn--danger"
                >
                  <History size={16} />
                  Dọn log cũ
                </button>
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

            {showCleanupPanel && (
              <div className="sh-cleanup-panel">
                <div className="sh-cleanup-panel__content">
                  <div>
                    <div className="sh-cleanup-panel__title">Dọn nhật ký cũ</div>
                    <div className="sh-cleanup-panel__desc">
                      Hệ thống sẽ xóa toàn bộ nhật ký được tạo trước ngày anh chọn. Nên xuất CSV trước khi dọn nếu cần lưu trữ đối soát.
                    </div>
                  </div>
                  <div className="sh-cleanup-panel__controls">
                    <input
                      type="date"
                      value={cleanupBeforeDate}
                      onChange={(e) => setCleanupBeforeDate(e.target.value)}
                      className="sh-cleanup-panel__date"
                    />
                    <button
                      onClick={handleCleanupLogs}
                      disabled={cleaningLogs}
                      className="sh-action-btn sh-action-btn--danger"
                    >
                      {cleaningLogs ? 'Đang dọn...' : 'Xác nhận dọn'}
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* Logs List */}
            <div style={{ padding: '16px 24px' }}>
              {logsError ? (
                <LoadErrorState
                  title="Không thể tải nhật ký hệ thống"
                  message={logsError}
                  onRetry={fetchLogs}
                  compact
                />
              ) : loadingLogs ? (
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
                    Tạo bản sao lưu dữ liệu PostgreSQL ngay lập tức
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

                <div style={{
                  marginBottom: '20px',
                  padding: '14px 16px',
                  borderRadius: '12px',
                  background: '#FFFBEB',
                  border: '1px solid #FDE68A'
                }}>
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px' }}>
                    <AlertTriangle size={18} color="#D97706" style={{ marginTop: '2px', flexShrink: 0 }} />
                    <div>
                      <div style={{ fontSize: '13px', fontWeight: '700', color: '#92400E', marginBottom: '6px' }}>
                        Phạm vi sao lưu hiện tại
                      </div>
                      <div style={{ fontSize: '13px', lineHeight: '1.6', color: '#92400E' }}>
                        Chỉ bao gồm dữ liệu PostgreSQL của SLIB. Chưa bao gồm MongoDB, Qdrant, Redis và các file upload ngoài cơ sở dữ liệu.
                      </div>
                    </div>
                  </div>
                </div>

                {backupError && (
                  <div style={{
                    marginBottom: '16px',
                    padding: '12px 14px',
                    borderRadius: '10px',
                    background: '#FEF2F2',
                    border: '1px solid #FECACA',
                    color: '#B91C1C',
                    fontSize: '13px'
                  }}>
                    {backupError}
                  </div>
                )}

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
                    Số ngày lưu trữ bản sao lưu
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
                  disabled={savingSchedule}
                  style={{
                    width: '100%', padding: '12px', background: savingSchedule ? '#CBD5E1' : '#e8600a', border: 'none',
                    borderRadius: '12px', fontSize: '14px', fontWeight: '600', color: '#fff',
                    cursor: savingSchedule ? 'not-allowed' : 'pointer'
                  }}
                >
                  {savingSchedule ? 'Đang lưu...' : 'Lưu cấu hình'}
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
                <div style={{
                  marginBottom: '16px',
                  padding: '14px 16px',
                  borderRadius: '12px',
                  background: '#F8FAFC',
                  border: '1px solid #E2E8F0',
                  color: '#475569',
                  fontSize: '13px',
                  lineHeight: '1.6'
                }}>
                  Bạn có thể tải file <strong>.dump</strong> về để lưu trữ, hoặc dùng nút <strong>Khôi phục</strong> để phục hồi lại dữ liệu PostgreSQL từ một bản sao lưu thành công ngay trên hệ thống. Khuyến nghị chỉ khôi phục ngoài giờ vận hành.
                </div>

                {backupError && !loadingBackup && backupHistory.length === 0 ? (
                  <LoadErrorState
                    title="Không thể tải dữ liệu sao lưu"
                    message={backupError}
                    onRetry={fetchBackupData}
                    compact
                  />
                ) : loadingBackup ? (
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
                              <div style={{ display: 'inline-flex', gap: '8px', flexWrap: 'wrap', justifyContent: 'center' }}>
                                <button
                                  onClick={() => handleDownloadBackup(backup.id, backup.fileName)}
                                  style={{
                                    padding: '8px 12px', background: '#F7FAFC', border: '1px solid #E2E8F0',
                                    borderRadius: '8px', fontSize: '12px', fontWeight: '600', color: '#4A5568',
                                    cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '6px'
                                  }}>
                                  <Download size={14} />
                                  Tải xuống
                                </button>
                                <button
                                  onClick={() => handleRestoreBackup(backup.id, backup.fileName)}
                                  disabled={restoringBackupId === backup.id}
                                  style={{
                                    padding: '8px 12px',
                                    background: restoringBackupId === backup.id ? '#CBD5E1' : '#FFF7ED',
                                    border: '1px solid #FDBA74',
                                    borderRadius: '8px',
                                    fontSize: '12px',
                                    fontWeight: '600',
                                    color: '#C2410C',
                                    cursor: restoringBackupId === backup.id ? 'not-allowed' : 'pointer',
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    gap: '6px'
                                  }}>
                                  <RotateCcw size={14} />
                                  {restoringBackupId === backup.id ? 'Đang khôi phục...' : 'Khôi phục'}
                                </button>
                              </div>
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
