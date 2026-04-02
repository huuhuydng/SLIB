import React, { useMemo, useState, useEffect } from "react";
import { API_BASE_URL } from '../../../config/apiConfig';
import {
  Users, Armchair, AlertCircle, AlertTriangle, Sparkles, Clock,
  Bell, Calendar, ChevronRight, BookOpen,
  ArrowDownLeft, ArrowUpRight, CalendarCheck,
  MessageSquare, TrendingUp, RefreshCw,
  Star, ShieldAlert, LifeBuoy, Award, UserX,
  ThumbsUp, FileText, Eye, BarChart3,
  MapPin, Layers, X, ExternalLink
} from "lucide-react";
import { getLibraryInsights } from "../../../services/ai/geminiService.jsx";
import librarianService from "../../../services/librarian/librarianService";
import dashboardService from "../../../services/librarian/dashboardService";
import { getAllNewBooksForAdmin } from "../../../services/librarian/newBookService";
import { getBehaviorSummary, getDensityPrediction, getRealtimeCapacity } from "../../../services/admin/ai/analyticsService";
import { getPeakHours } from "../../../services/admin/ai/pythonAiApi";
import websocketService from "../../../services/shared/websocketService";

import "../../../styles/librarian/Dashboard.css";

const Dashboard = () => {
  const [searchText, setSearchText] = useState("");
  const [insights, setInsights] = useState([]);
  const [accessLogs, setAccessLogs] = useState([]);
  const [dashStats, setDashStats] = useState(null);
  const [recentNews, setRecentNews] = useState([]);
  const [recentNewBooks, setRecentNewBooks] = useState([]);
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [detailModal, setDetailModal] = useState(null);
  const [loading, setLoading] = useState(true);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [activeRequestTab, setActiveRequestTab] = useState("support");
  const [hoveredBar, setHoveredBar] = useState(null);
  const [chartRange, setChartRange] = useState('week');
  const [chartData, setChartData] = useState([]);
  const [accessFilter, setAccessFilter] = useState('all');
  const [topStudentsRange, setTopStudentsRange] = useState('month');
  const [topStudentsData, setTopStudentsData] = useState([]);
  const [pendingCounts, setPendingCounts] = useState({
    feedbackNew: 0, complaintPending: 0, supportPending: 0, supportInProgress: 0
  });
  const [realtimeCapacity, setRealtimeCapacity] = useState(null);
  const [peakHours, setPeakHours] = useState([]);
  const [quietHours, setQuietHours] = useState([]);
  const [densityHours, setDensityHours] = useState([]);
  const [behaviorIssues, setBehaviorIssues] = useState([]);

  const formatDateTime = (dateTimeString) => {
    if (!dateTimeString) return '';
    const date = new Date(dateTimeString);
    const time = date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    const dateStr = date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
    return `${time} ${dateStr}`;
  };

  const formatDate = (dateTimeString) => {
    if (!dateTimeString) return '';
    const date = new Date(dateTimeString);
    return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
  };

  const formatTime = (dateTimeString) => {
    if (!dateTimeString) return '';
    const date = new Date(dateTimeString);
    return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  };

  const formatRelativeTime = (dateTimeString) => {
    if (!dateTimeString) return '';
    const date = new Date(dateTimeString);
    const now = new Date();
    const diffMs = now - date;
    const diffMin = Math.floor(diffMs / 60000);
    const diffHour = Math.floor(diffMs / 3600000);
    const diffDay = Math.floor(diffMs / 86400000);
    if (diffMin < 1) return 'Vừa xong';
    if (diffMin < 60) return `${diffMin} phút trước`;
    if (diffHour < 24) return `${diffHour} giờ trước`;
    if (diffDay < 7) return `${diffDay} ngày trước`;
    return formatDate(dateTimeString);
  };

  const sortByNewest = (items = []) => {
    return [...items].sort((a, b) => {
      const aTime = new Date(a?.publishedAt || a?.createdAt || a?.updatedAt || 0).getTime();
      const bTime = new Date(b?.publishedAt || b?.createdAt || b?.updatedAt || 0).getTime();
      return bTime - aTime;
    });
  };

  const getStatusConfig = (status) => {
    switch (status) {
      case 'CONFIRMED': return { label: 'Đã xác nhận', bg: '#d1fae5', color: '#065f46' };
      case 'BOOKED': return { label: 'Đã đặt', bg: '#fef3c7', color: '#92400e' };
      case 'PROCESSING': return { label: 'Đang xử lý', bg: '#dbeafe', color: '#1e40af' };
      case 'CANCELLED': return { label: 'Đã huỷ', bg: '#fee2e2', color: '#991b1b' };
      case 'CANCEL': return { label: 'Đã huỷ', bg: '#fee2e2', color: '#991b1b' };
      case 'COMPLETED': return { label: 'Hoàn thành', bg: '#d1fae5', color: '#065f46' };
      case 'EXPIRED': return { label: 'Không đến', bg: '#fef3c7', color: '#92400e' };
      case 'PENDING': return { label: 'Chờ xử lý', bg: '#fef3c7', color: '#92400e' };
      case 'VERIFIED': return { label: 'Đã xác minh', bg: '#dbeafe', color: '#1e40af' };
      case 'RESOLVED': return { label: 'Đã giải quyết', bg: '#d1fae5', color: '#065f46' };
      case 'REJECTED': return { label: 'Đã từ chối', bg: '#fee2e2', color: '#991b1b' };
      case 'IN_PROGRESS': return { label: 'Đang xử lý', bg: '#e0e7ff', color: '#3730a3' };
      case 'ACCEPTED': return { label: 'Chấp nhận', bg: '#d1fae5', color: '#065f46' };
      case 'DENIED': return { label: 'Từ chối', bg: '#fee2e2', color: '#991b1b' };
      case 'NEW': return { label: 'Mới', bg: '#dbeafe', color: '#1e40af' };
      case 'REVIEWED': return { label: 'Đã xem', bg: '#f3f4f6', color: '#6b7280' };
      case 'ACTED': return { label: 'Đã xử lý', bg: '#d1fae5', color: '#065f46' };
      default: return { label: status, bg: '#f3f4f6', color: '#6b7280' };
    }
  };

  const getViolationLabel = (type) => {
    switch (type) {
      case 'UNAUTHORIZED_USE': return 'Sử dụng trái phép';
      case 'LEFT_BELONGINGS': return 'Để đồ đạc';
      case 'NOISE': return 'Gây ồn ào';
      case 'FEET_ON_SEAT': return 'Gác chân lên ghế';
      case 'FOOD_DRINK': return 'Ăn uống';
      case 'SLEEPING': return 'Ngủ';
      case 'OTHER': return 'Khác';
      default: return type;
    }
  };

  // Hàm nhẹ chỉ refresh stats (không gọi AI insights hay news) - dùng cho real-time updates
  const refreshStatsOnly = async () => {
    try {
      const stats = await dashboardService.getDashboardStats();
      if (stats) {
        setDashStats(stats);
        // Dùng server time thay vì client time
        setLastUpdated(stats.serverTime ? new Date(stats.serverTime) : new Date());
      }
    } catch (e) {
      console.warn('Error refreshing stats:', e);
    }
  };

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      const [stats, news, newBooks] = await Promise.all([
        dashboardService.getDashboardStats(),
        dashboardService.getRecentNews(),
        getAllNewBooksForAdmin().catch(() => [])
      ]);

      if (stats) {
        setDashStats(stats);
        const data = await getLibraryInsights(stats);
        setInsights(Array.isArray(data) ? data : []);
        // Dùng server time thay vì client time
        setLastUpdated(stats.serverTime ? new Date(stats.serverTime) : new Date());
      }
      setRecentNews(news || []);
      setRecentNewBooks(sortByNewest(newBooks || []).slice(0, 5));

      // Fetch pending counts from new APIs
      const token = localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');
      const headers = token ? { Authorization: `Bearer ${token}` } : {};
      try {
        const [fbRes, cmpRes, supRes] = await Promise.all([
          fetch(`${API_BASE_URL}/slib/feedbacks/count`, { headers }).then(r => r.ok ? r.json() : null).catch(() => null),
          fetch(`${API_BASE_URL}/slib/complaints/count`, { headers }).then(r => r.ok ? r.json() : null).catch(() => null),
          fetch(`${API_BASE_URL}/slib/support-requests/count`, { headers }).then(r => r.ok ? r.json() : null).catch(() => null),
        ]);
        setPendingCounts({
          feedbackNew: fbRes?.new || 0,
          complaintPending: cmpRes?.pending || 0,
          supportPending: supRes?.pending || 0,
          supportInProgress: supRes?.inProgress || 0,
        });
      } catch (e) {
        console.warn('Could not fetch pending counts:', e);
      }

      // Fetch realtime capacity
      try {
        const capacity = await getRealtimeCapacity();
        setRealtimeCapacity(capacity);
      } catch (e) {
        console.warn('Could not fetch realtime capacity:', e);
      }

      // Fetch AI Analytics data
      try {
        const peakRes = await getPeakHours();
        const peakData = peakRes?.data || {};
        setPeakHours(Array.isArray(peakData.peak_hours) ? peakData.peak_hours.slice(0, 3) : []);
        setQuietHours(Array.isArray(peakData.quiet_hours) ? peakData.quiet_hours : []);
      } catch (e) { console.warn('Could not fetch peak hours:', e); }

      try {
        const densityData = await getDensityPrediction();
        setDensityHours(Array.isArray(densityData?.hourly_predictions) ? densityData.hourly_predictions : []);
      } catch (e) { console.warn('Could not fetch density:', e); }

      try {
        const behaviorData = await getBehaviorSummary();
        setBehaviorIssues(Array.isArray(behaviorData?.students) ? behaviorData.students : []);
      } catch (e) { console.warn('Could not fetch behavior issues:', e); }
    } catch (e) {
      console.error('Error fetching dashboard data:', e);
      setInsights([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchAccessLogs = async () => {
    try {
      const logs = await librarianService.getAllAccessLogs();
      setAccessLogs(logs.slice(0, 10));
    } catch (error) {
      console.error('Failed to fetch access logs:', error);
      setAccessLogs([]);
    }
  };

  // Load ban đầu
  useEffect(() => {
    fetchDashboardData();
    fetchAccessLogs();
  }, []);

  // WebSocket real-time updates cho toàn bộ dashboard
  useEffect(() => {
    const unsubscribers = [];
    websocketService.connect(
      () => {
        // Subscribe access-logs (check-in/out real-time)
        unsubscribers.push(websocketService.subscribe('/topic/access-logs', (message) => {
          if (message.type === 'CHECK_IN' || message.type === 'CHECK_OUT') {
            const newLog = {
              logId: `${message.userId}-${message.type}-${Date.now()}`,
              userId: message.userId,
              userName: message.fullName || message.userName,
              userCode: message.userCode,
              action: message.type,
              checkInTime: message.checkInTime || message.time || message.timestamp,
              checkOutTime: message.checkOutTime || (message.type === 'CHECK_OUT' ? (message.time || message.timestamp) : null)
            };
            setAccessLogs(prevLogs => {
              const isDuplicate = prevLogs.some(log =>
                log.userId === newLog.userId &&
                log.action === newLog.action &&
                Math.abs(new Date(log.checkInTime || log.checkOutTime) - new Date(newLog.checkInTime || newLog.checkOutTime)) < 2000
              );
              if (isDuplicate) return prevLogs;
              return [newLog, ...prevLogs].slice(0, 10);
            });
            // Refresh stats nhanh khi check-in/out (delay để đợi DB commit)
            setTimeout(() => refreshStatsOnly(), 800);
          }
        }));

        // Subscribe dashboard updates (bookings, violations, complaints, feedbacks, support)
        unsubscribers.push(websocketService.subscribe('/topic/dashboard', (message) => {
          // Dùng refreshStatsOnly cho mọi event - nhanh, không loading flash
          setTimeout(() => refreshStatsOnly(), 500);
        }));

        // Subscribe news updates
        unsubscribers.push(websocketService.subscribe('/topic/news', () => {
          // Refresh stats + fetch news mới
          setTimeout(async () => {
            refreshStatsOnly();
            try {
              const news = await dashboardService.getRecentNews();
              setRecentNews(news || []);
            } catch (e) { /* ignore */ }
          }, 500);
        }));
      },
      (error) => {
        console.error('[Dashboard] WebSocket error:', error);
      }
    );
    return () => { unsubscribers.forEach(unsub => { if (unsub) unsub(); }); };
  }, []);

  // Fallback polling 60s - đảm bảo dashboard cập nhật khi WebSocket message không đến
  useEffect(() => {
    const interval = setInterval(() => {
      refreshStatsOnly();
    }, 60000);
    return () => clearInterval(interval);
  }, []);

  // Fetch top students when range changes
  useEffect(() => {
    const fetchTopStudents = async () => {
      const data = await dashboardService.getTopStudents(topStudentsRange);
      setTopStudentsData(data);
    };
    fetchTopStudents();
  }, [topStudentsRange]);

  const filteredStudents = useMemo(() => {
    let logs = accessLogs;
    if (accessFilter === 'in') logs = logs.filter(l => l.action === 'CHECK_IN');
    else if (accessFilter === 'out') logs = logs.filter(l => l.action === 'CHECK_OUT');
    const q = searchText.trim().toLowerCase();
    if (!q) return logs;
    return logs.filter((log) =>
      (log.userName && log.userName.toLowerCase().includes(q)) ||
      (log.userCode && log.userCode.toLowerCase().includes(q)) ||
      (log.action && log.action.toLowerCase().includes(q))
    );
  }, [searchText, accessLogs, accessFilter]);

  const stats = {
    currentlyInLibrary: dashStats?.currentlyInLibrary || 0,
    totalCheckInsToday: dashStats?.totalCheckInsToday || 0,
    totalCheckOutsToday: dashStats?.totalCheckOutsToday || 0,
    occupancyRate: dashStats?.occupancyRate || 0,
    totalSeats: dashStats?.totalSeats || 0,
    totalBookingsToday: dashStats?.totalBookingsToday || 0,
    activeBookings: dashStats?.activeBookings || 0,
    pendingBookings: dashStats?.pendingBookings || 0,
    violationsToday: dashStats?.violationsToday || 0,
    pendingViolations: dashStats?.pendingViolations || 0,
    pendingSupportRequests: dashStats?.pendingSupportRequests || 0,
    inProgressSupportRequests: dashStats?.inProgressSupportRequests || 0,
    totalUsers: dashStats?.totalUsers || 0,
    recentBookings: dashStats?.recentBookings || [],
    areaOccupancies: dashStats?.areaOccupancies || [],
    weeklyStats: dashStats?.weeklyStats || [],
    recentViolations: dashStats?.recentViolations || [],
    topStudents: dashStats?.topStudents || [],
    recentSupportRequests: dashStats?.recentSupportRequests || [],
    recentComplaints: dashStats?.recentComplaints || [],
    recentFeedbacks: dashStats?.recentFeedbacks || [],
    zoneOccupancies: dashStats?.zoneOccupancies || []
  };

  // Greeting based on time of day
  const getGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Chào buổi sáng';
    if (hour < 18) return 'Chào buổi chiều';
    return 'Chào buổi tối';
  };

  // Fetch chart data when range changes
  useEffect(() => {
    const fetchChart = async () => {
      const data = await dashboardService.getChartStats(chartRange);
      setChartData(data);
    };
    fetchChart();
  }, [chartRange]);

  // Chart max value for scaling
  const chartMax = useMemo(() => {
    if (!chartData.length) return 10;
    const maxVal = Math.max(
      ...chartData.map(d => Math.max(d.checkInCount || 0, d.bookingCount || 0)),
      1
    );
    return Math.ceil(maxVal * 1.05);
  }, [chartData]);

  // Group zones by area for zone occupancy
  const zonesByArea = useMemo(() => {
    const grouped = {};
    (stats.zoneOccupancies || []).forEach(zone => {
      const areaName = zone.areaName || 'Khac';
      if (!grouped[areaName]) grouped[areaName] = [];
      grouped[areaName].push(zone);
    });
    return grouped;
  }, [stats.zoneOccupancies]);

  // Get active request tab data
  const getActiveRequestData = () => {
    switch (activeRequestTab) {
      case 'support': return stats.recentSupportRequests;
      case 'violation': return stats.recentViolations;
      case 'complaint': return stats.recentComplaints;
      case 'feedback': return stats.recentFeedbacks;
      default: return [];
    }
  };

  const quickActions = useMemo(() => ([
    {
      label: 'Chat sinh viên',
      href: '/librarian/chat',
      icon: MessageSquare,
      note: `${pendingCounts.supportPending + pendingCounts.supportInProgress} yêu cầu đang hoạt động`,
    },
    {
      label: 'Xử lý hỗ trợ',
      href: '/librarian/support-requests',
      icon: LifeBuoy,
      note: `${pendingCounts.supportPending} chờ nhận`,
    },
    {
      label: 'Kiểm tra ghế',
      href: '/librarian/seat-status-reports',
      icon: FileText,
      note: 'Rà soát báo cáo ghế',
    },
    {
      label: 'Quản lý đặt chỗ',
      href: '/librarian/bookings',
      icon: CalendarCheck,
      note: `${stats.activeBookings} lượt đang hiệu lực`,
    },
    {
      label: 'Đăng tin tức',
      href: '/librarian/news/create',
      icon: Bell,
      note: 'Tạo thông báo mới',
    },
    {
      label: 'Cập nhật sách mới',
      href: '/librarian/new-books',
      icon: BookOpen,
      note: `${recentNewBooks.length} mục gần nhất`,
    },
  ]), [
    pendingCounts.supportPending,
    pendingCounts.supportInProgress,
    stats.activeBookings,
    recentNewBooks.length,
  ]);

  const urgentItems = useMemo(() => {
    const items = [
      {
        key: 'support-pending',
        label: 'Yêu cầu hỗ trợ chờ nhận',
        count: pendingCounts.supportPending,
        href: '/librarian/support-requests?status=PENDING',
        tone: 'orange',
        hint: stats.recentSupportRequests?.[0]?.description || 'Ưu tiên nhận ca hỗ trợ mới',
      },
      {
        key: 'support-processing',
        label: 'Yêu cầu đang xử lý',
        count: pendingCounts.supportInProgress,
        href: '/librarian/support-requests?status=IN_PROGRESS',
        tone: 'blue',
        hint: 'Theo dõi các cuộc hỗ trợ chưa đóng',
      },
      {
        key: 'violations',
        label: 'Vi phạm chưa xử lý',
        count: stats.pendingViolations,
        href: '/librarian/violation',
        tone: 'red',
        hint: stats.recentViolations?.[0]?.violatorName
          ? `${stats.recentViolations[0].violatorName} vừa có báo cáo mới`
          : 'Kiểm tra các báo cáo vi phạm mới',
      },
      {
        key: 'complaints',
        label: 'Khiếu nại chờ xử lý',
        count: pendingCounts.complaintPending,
        href: '/librarian/complaints?status=PENDING',
        tone: 'amber',
        hint: stats.recentComplaints?.[0]?.subject || 'Phản hồi khiếu nại đang chờ',
      },
      {
        key: 'feedback',
        label: 'Phản hồi chưa xem',
        count: pendingCounts.feedbackNew,
        href: '/librarian/feedback?status=NEW',
        tone: 'green',
        hint: 'Nắm bắt góp ý mới từ sinh viên',
      },
    ];

    return items.filter((item) => item.count > 0);
  }, [pendingCounts, stats.pendingViolations, stats.recentSupportRequests, stats.recentViolations, stats.recentComplaints]);

  const compactCards = [
    {
      key: 'in-library',
      value: stats.currentlyInLibrary,
      label: 'Đang trong thư viện',
      iconClass: 'compact-icon--orange',
      icon: Users,
      href: '/librarian/checkinout',
    },
    {
      key: 'bookings',
      value: stats.totalBookingsToday,
      label: 'Đặt chỗ hôm nay',
      iconClass: 'compact-icon--blue',
      icon: CalendarCheck,
      href: '/librarian/bookings',
    },
    {
      key: 'pending',
      value:
        (pendingCounts.complaintPending || 0) +
        (pendingCounts.supportPending || 0) +
        (pendingCounts.supportInProgress || 0) +
        (pendingCounts.feedbackNew || 0) +
        (stats.pendingViolations || 0),
      label: 'Cần xử lý',
      iconClass: 'compact-icon--red',
      icon: AlertCircle,
      href: '/librarian/support-requests',
    },
    {
      key: 'feedback',
      value: pendingCounts.feedbackNew,
      label: 'Phản hồi mới',
      iconClass: 'compact-icon--green',
      icon: ThumbsUp,
      href: '/librarian/feedback',
    },
  ];

  const displayInsights = useMemo(() => insights.slice(0, 3), [insights]);

  return (
    <>
      <div className="dashboard-container">
        {/* Title row */}
        <div className="dashboard-title-row">
          <div className="dashboard-title-copy">
            <h1 className="dashboard-title">{getGreeting()}</h1>
            <p className="dashboard-subtitle">Tổng quan vận hành thư viện trong ngày, tập trung vào các việc cần phản hồi nhanh và khu vực cần theo dõi sát.</p>
            <div className="dashboard-inline-metrics">
              <span className="dashboard-inline-pill">
                Công suất hiện tại {Math.round(Number(stats.occupancyRate || 0))}%
              </span>
              <span className="dashboard-inline-pill">
                {compactCards[2].value} mục cần xử lý
              </span>
              <span className="dashboard-inline-pill">
                {stats.activeBookings} lượt đặt đang hiệu lực
              </span>
            </div>
          </div>
          <div className="dashboard-meta">
            {lastUpdated && (
              <span className="last-updated">
                <Clock size={13} />
                Cập nhật: {lastUpdated.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
              </span>
            )}
            <button className="refresh-btn" onClick={fetchDashboardData} title="Làm mới dữ liệu">
              <RefreshCw size={14} />
            </button>
          </div>
        </div>

        <div className="dashboard-overview-grid">
          <div className="dashboard-overview-col">
            <section className="dashboard-panel panel-elevated quick-actions-panel">
              <div className="panel-header">
                <h3 className="panel-title">Thao tác nhanh</h3>
              </div>
              <div className="quick-actions-grid">
                {quickActions.map((action) => {
                  const Icon = action.icon;
                  return (
                    <a key={action.label} href={action.href} className="quick-action-card">
                      <span className="quick-action-icon">
                        <Icon size={18} />
                      </span>
                      <span className="quick-action-body">
                        <span className="quick-action-label">{action.label}</span>
                        <span className="quick-action-note">{action.note}</span>
                      </span>
                      <ChevronRight size={16} className="quick-action-arrow" />
                    </a>
                  );
                })}
              </div>
            </section>

            <section className="dashboard-panel panel-elevated stats-panel">
              <div className="panel-header">
                <h3 className="panel-title">Chỉ số nhanh</h3>
              </div>
              <div className="stats-grid-compact">
                {compactCards.map((card) => {
                  const Icon = card.icon;
                  return (
                    <a key={card.key} href={card.href} className="compact-card compact-card--link">
                      <div className={`compact-card-icon ${card.iconClass}`}><Icon size={20} /></div>
                      <div className="compact-card-value">{card.value}</div>
                      <div className="compact-card-label">{card.label}</div>
                    </a>
                  );
                })}
              </div>
            </section>
          </div>

          <div className="dashboard-overview-col">
            <section className="dashboard-panel panel-elevated urgent-panel">
              <div className="panel-header">
                <h3 className="panel-title">Cần xử lý ngay</h3>
              </div>
              {urgentItems.length === 0 ? (
                <div className="empty-section">Hiện chưa có việc gấp cần xử lý</div>
              ) : (
                <div className="urgent-list">
                  {urgentItems.map((item) => (
                    <a key={item.key} href={item.href} className={`urgent-item urgent-item--${item.tone}`}>
                      <div className="urgent-item-main">
                        <span className="urgent-item-title">{item.label}</span>
                        <span className="urgent-item-hint">{item.hint}</span>
                      </div>
                      <div className="urgent-item-side">
                        <span className="urgent-item-count">{item.count}</span>
                        <ChevronRight size={15} />
                      </div>
                    </a>
                  ))}
                </div>
              )}
            </section>

            <section className="dashboard-panel panel-elevated zone-status-side">
              <div className="panel-header">
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <h3 className="panel-title">Trạng thái khu vực</h3>
                </div>
                {realtimeCapacity && (
                  <span className="dashboard-live-pill">
                    {realtimeCapacity.currently_occupied ?? stats.currentlyInLibrary}/{realtimeCapacity.total_capacity ?? stats.totalSeats} đang dùng
                  </span>
                )}
              </div>

              <div className="legend">
                <span className="legend-item">
                  <i className="dot dotGreen" />
                  Trống
                </span>
                <span className="legend-item">
                  <i className="dot dotYellow" />
                  Khá đông
                </span>
                <span className="legend-item">
                  <i className="dot dotRed" />
                  Đầy
                </span>
              </div>

              {Object.keys(zonesByArea).length === 0 ? (
                <div className="empty-section">Chưa có dữ liệu khu vực</div>
              ) : (
                Object.entries(zonesByArea).map(([areaName, zones]) => (
                  <div key={areaName} className="zone-area-group">
                    <div className="zone-area-header">
                      <MapPin size={13} />
                      <span>{areaName}</span>
                    </div>
                    {zones.map((zone, idx) => (
                      <div key={idx} className="area-row zone-row">
                        <div className="area-top">
                          <span className="area-name">{zone.zoneName}</span>
                          <span className="area-stats">
                            {zone.occupiedSeats}/{zone.totalSeats} ({zone.occupancyPercentage}%)
                          </span>
                        </div>
                        <div className="area-bar">
                          <div
                            className="area-bar-fill"
                            style={{
                              width: `${zone.occupancyPercentage}%`,
                              background: zone.occupancyPercentage >= 90 ? '#ef4444'
                                : zone.occupancyPercentage >= 60 ? '#fbbf24' : '#22c55e'
                            }}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                ))
              )}
            </section>
          </div>
        </div>

        {/* Analytics Chart + AI Panel */}
        <div className="dashboard-grid-chart">
          {/* Bar Chart - Weekly Analytics */}
          <section className="dashboard-panel panel-elevated chart-panel">
            <div className="panel-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexShrink: 0 }}>
                <h3 className="panel-title" style={{ whiteSpace: 'nowrap' }}>Thống kê</h3>
                <select
                  className="chart-range-select"
                  value={chartRange}
                  onChange={(e) => setChartRange(e.target.value)}
                >
                  <option value="week">Tuần này</option>
                  <option value="month">Tháng này</option>
                  <option value="year">Năm này</option>
                </select>
              </div>
              <div className="chart-legend">
                <span className="chart-legend-item">
                  <i className="chart-legend-dot" style={{ background: '#FF751F' }}></i>
                  Vào thư viện
                </span>
                <span className="chart-legend-item">
                  <i className="chart-legend-dot" style={{ background: '#fbbf24' }}></i>
                  Đặt chỗ
                </span>
              </div>
            </div>

            <div className="bar-chart-container">
              {chartData.length === 0 ? (
                <div className="empty-section">Chưa có dữ liệu thống kê</div>
              ) : (
                <div className="bar-chart">
                  <div className="bar-chart-y-axis">
                    {[...Array(5)].map((_, i) => (
                      <span key={i} className="y-axis-label">
                        {Math.round(chartMax * (1 - i / 4))}
                      </span>
                    ))}
                  </div>
                  <div className="bar-chart-bars">
                    <div className="bar-chart-grid">
                      {[...Array(4)].map((_, i) => (
                        <div key={i} className="grid-line" style={{ bottom: `${((i + 1) / 4) * 100}%` }} />
                      ))}
                    </div>
                    {chartData.map((day, idx) => (
                      <div key={idx} className="bar-group">
                        <div className="bar-pair">
                          <div
                            className={`bar bar-checkin ${hoveredBar === `${idx}-ci` ? 'bar-hovered' : ''}`}
                            style={{ height: `${chartMax > 0 ? ((day.checkInCount || 0) / chartMax) * 100 : 0}%` }}
                            onMouseEnter={() => setHoveredBar(`${idx}-ci`)}
                            onMouseLeave={() => setHoveredBar(null)}
                          >
                            {hoveredBar === `${idx}-ci` && (
                              <div className="bar-single-tooltip">{day.checkInCount || 0}</div>
                            )}
                          </div>
                          <div
                            className={`bar bar-booking ${hoveredBar === `${idx}-bk` ? 'bar-hovered' : ''}`}
                            style={{ height: `${chartMax > 0 ? ((day.bookingCount || 0) / chartMax) * 100 : 0}%` }}
                            onMouseEnter={() => setHoveredBar(`${idx}-bk`)}
                            onMouseLeave={() => setHoveredBar(null)}
                          >
                            {hoveredBar === `${idx}-bk` && (
                              <div className="bar-single-tooltip">{day.bookingCount || 0}</div>
                            )}
                          </div>
                        </div>
                        <span className="bar-label">{day.label || day.dayOfWeek}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </section>

          {/* AI Analytics */}
          <section className="dashboard-panel panel-elevated ai-panel">
            <div className="panel-header">
              <h3 className="panel-title">Thống kê bằng AI</h3>
            </div>

            {/* Peak hours horizontal bars */}
            {peakHours.length > 0 ? (
              <div className="peak-hours-list">
                {peakHours.map((h, i) => (
                  <div key={i} className="peak-hour-row">
                    <span className="peak-hour-time">{h.label}</span>
                    <div className="area-bar" style={{ flex: 1 }}>
                      <div
                        className="area-bar-fill"
                        style={{
                          width: `${Math.round(h.avg_occupancy * 100)}%`,
                          background: h.avg_occupancy >= 0.8 ? '#ef4444' : h.avg_occupancy >= 0.5 ? '#f59e0b' : '#22c55e'
                        }}
                      />
                    </div>
                    <span className="peak-hour-pct">{Math.round(h.avg_occupancy * 100)}%</span>
                  </div>
                ))}
              </div>
            ) : (
              <div className="empty-section">Chưa đủ dữ liệu</div>
            )}

            {/* Quiet hours note */}
            {quietHours.length > 0 && (
              <div className="peak-quiet-note">
                Giờ vắng: {quietHours.map(h => h.label).join(', ')}
              </div>
            )}

            {displayInsights.length > 0 && (
              <div className="insights-section">
                <div className="insights-header">
                  <Sparkles size={14} color="#f59e0b" />
                  <span>Gợi ý vận hành</span>
                </div>
                <div className="insights-list">
                  {displayInsights.map((insight, idx) => (
                    <div key={`${insight.title}-${idx}`} className={`insight-item insight-item--${insight.type || 'info'}`}>
                      <div className="insight-title">{insight.title}</div>
                      <div className="insight-message">{insight.message}</div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Density mini bars */}
            {densityHours.length > 0 && (
              <div className="density-mini-section">
                <div className="density-mini-header">
                  <span>Phân bố theo giờ</span>
                  <span className="density-mini-sub">7 ngày</span>
                </div>
                <div className="density-mini-bars">
                  {(() => {
                    const maxOcc = Math.max(...densityHours.map(h => h.predicted_occupancy || 0), 0.01);
                    return densityHours.map((h, i) => {
                      const occ = h.predicted_occupancy || 0;
                      const relativeHeight = (occ / maxOcc) * 100;
                      const barColor = occ >= 0.8 ? '#ef4444' : occ >= 0.5 ? '#f59e0b' : '#22c55e';
                      return (
                        <div key={i} className="density-mini-bar-col" title={`${h.hour}:00 — ${Math.round(occ * 100)}%`}>
                          <div className="density-mini-bar-bg">
                            <div className="density-mini-bar-fill" style={{ height: `${Math.max(relativeHeight, 8)}%`, background: barColor }} />
                          </div>
                          <span className="density-mini-label">{h.hour}</span>
                        </div>
                      );
                    });
                  })()}
                </div>
              </div>
            )}

            {/* Behavior issues */}
            <div className="behavior-section">
              <div className="behavior-header">
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <AlertTriangle size={14} color="#f59e0b" />
                  <span className="behavior-title">SV cần lưu ý</span>
                </div>
                {behaviorIssues.length > 0 && (
                  <span className="behavior-count">{behaviorIssues.length}</span>
                )}
              </div>

              {behaviorIssues.length === 0 ? (
                <div className="behavior-ok">
                  <span>✓ Không có sinh viên nào cần lưu ý</span>
                </div>
              ) : (
                <div className="behavior-list">
                  {behaviorIssues.map((s, idx) => (
                    <div key={idx} className={`behavior-item ${s.severity === 'critical' ? 'behavior-item--critical' : s.severity === 'warning' ? 'behavior-item--warning' : ''}`}>
                      <div className="behavior-item-top">
                        <span className="behavior-item-name">{s.full_name}</span>
                        <span className="behavior-item-code">{s.user_code}</span>
                      </div>
                      <div className="behavior-item-stats">
                        <span className={`behavior-score ${s.reputation_score < 70 ? 'score--low' : s.reputation_score < 90 ? 'score--mid' : ''}`}>
                          {s.reputation_score} điểm
                        </span>
                        <span className={`behavior-tag behavior-tag--${s.severity === 'critical' ? 'red' : s.severity === 'warning' ? 'amber' : 'gray'}`}>
                          {s.primary_issue}
                        </span>
                      </div>
                      {s.detail && <div className="behavior-detail">{s.detail}</div>}
                      <div className="behavior-suggestion">{s.suggestion}</div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </section>
        </div>

        {/* Middle section: Access logs table */}
        <div className="dashboard-grid-mid">
          <section className="dashboard-panel panel-elevated">
            <div className="panel-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <h3 className="panel-title" style={{ whiteSpace: 'nowrap' }}>Sinh viên ra vào gần đây</h3>
                <select
                  className="chart-range-select"
                  value={accessFilter}
                  onChange={(e) => setAccessFilter(e.target.value)}
                >
                  <option value="all">Tất cả</option>
                  <option value="in">Vào</option>
                  <option value="out">Ra</option>
                </select>
              </div>
              <a href="/librarian/students" className="view-all-link">Xem tất cả</a>
            </div>
            {filteredStudents.length === 0 ? (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 1, minHeight: '300px', color: 'var(--muted)', fontSize: '14px', fontStyle: 'italic' }}>
                Không có dữ liệu
              </div>
            ) : (
              <div className="table-wrapper">
                <table className="dashboard-table">
                  <thead>
                    <tr>
                      <th style={{ width: '22%' }}>Tên sinh viên</th>
                      <th style={{ width: '15%' }}>Mã số</th>
                      <th style={{ width: '13%' }}>Hành động</th>
                      <th style={{ width: '12%' }}>Cổng</th>
                      <th style={{ width: '15%' }}>Thời gian</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredStudents.map((log) => (
                      <tr key={`${log.logId}-${log.action}`}>
                        <td className="cell-name">{log.userName}</td>
                        <td className="cell-code">{log.userCode}</td>
                        <td>
                          <span className={`badge ${log.action === "CHECK_IN" ? "badgeIn" : "badgeOut"}`}>
                            {log.action === 'CHECK_IN' ? 'Vào' : 'Ra'}
                          </span>
                        </td>
                        <td>Cổng A</td>
                        <td className="cell-time">
                          {(() => {
                            const timeStr = log.action === 'CHECK_IN' ? log.checkInTime : log.checkOutTime;
                            if (!timeStr) return '-';
                            const d = new Date(timeStr);
                            return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
                          })()}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          {/* Recent bookings */}
          <section className="dashboard-panel panel-elevated">
            <div className="panel-header">
              <h3 className="panel-title" style={{ whiteSpace: 'nowrap' }}>Đặt chỗ gần đây</h3>
              <a href="/librarian/bookings" className="view-all-link">Xem tất cả</a>
            </div>

            {stats.recentBookings.length === 0 ? (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 1, minHeight: '300px', color: 'var(--muted)', fontSize: '14px', fontStyle: 'italic' }}>
                Chưa có đặt chỗ nào
              </div>
            ) : (
              <div className="booking-list">
                {stats.recentBookings.map((booking, idx) => {
                  const statusCfg = getStatusConfig(booking.status);
                  return (
                    <div key={idx} className="booking-row" onClick={() => setDetailModal({ type: 'booking', data: booking })}>
                      <div className="booking-row-left">
                        <span className="booking-row-name">{booking.userName}</span>
                        <span className="booking-row-meta">{booking.userCode}</span>
                      </div>
                      <div className="booking-row-center">
                        <span className="booking-row-seat">{booking.zoneName} - {booking.seatCode}</span>
                        <span className="booking-row-time">{formatTime(booking.startTime)} - {formatTime(booking.endTime)}</span>
                      </div>
                      <span className="status-badge" style={{ background: statusCfg.bg, color: statusCfg.color, flexShrink: 0 }}>
                        {statusCfg.label}
                      </span>
                    </div>
                  );
                })}
              </div>
            )}
          </section>
        </div>

        {/* Top students + Recent violations */}
        <div className="dashboard-grid-two">
          {/* Top 5 students */}
          <section className="dashboard-panel panel-elevated">
            <div className="panel-header">
              <h3 className="panel-title" style={{ whiteSpace: 'nowrap' }}>Top 5 sinh viên xuất sắc</h3>
              <select
                className="chart-range-select"
                style={{ width: 'auto', flexShrink: 0 }}
                value={topStudentsRange}
                onChange={(e) => setTopStudentsRange(e.target.value)}
              >
                <option value="week">Tuần này</option>
                <option value="month">Tháng này</option>
                <option value="year">Năm này</option>
              </select>
            </div>

            {topStudentsData.length === 0 ? (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 1, minHeight: '200px', color: 'var(--muted)', fontSize: '14px', fontStyle: 'italic' }}>
                Chưa có dữ liệu
              </div>
            ) : (
              <div className="top-students-list">
                {topStudentsData.map((student, idx) => (
                  <div key={idx} className="top-student-item">
                    <div className={`top-student-rank rank-${idx + 1}`}>
                      {idx + 1}
                    </div>
                    <div className="top-student-avatar-wrapper" onClick={() => setSelectedStudent(student)}>
                      {student.avatarUrl ? (
                        <img src={student.avatarUrl} alt={student.fullName} className="top-student-avatar" />
                      ) : (
                        <div className="top-student-avatar-fallback">
                          {student.fullName?.charAt(0)?.toUpperCase() || '?'}
                        </div>
                      )}
                    </div>
                    <div className="top-student-info" onClick={() => setSelectedStudent(student)}>
                      <span className="top-student-name">{student.fullName}</span>
                      <span className="top-student-code">{student.userCode}</span>
                    </div>
                    <div className="top-student-stats">
                      <span className="top-student-visits">
                        {student.totalMinutes > 60
                          ? `${Math.floor(student.totalMinutes / 60)}h${student.totalMinutes % 60}p`
                          : `${student.totalMinutes}p`
                        }
                      </span>
                      <span className="top-student-minutes">{student.totalVisits} lần</span>
                    </div>
                    <a
                      href={`/librarian/students?userId=${student.userId}`}
                      className="top-student-detail-link"
                      title="Xem chi tiết"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <ChevronRight size={16} />
                    </a>
                  </div>
                ))}
              </div>
            )}
          </section>

          {/* Recent violations */}
          <section className="dashboard-panel panel-elevated">
            <div className="panel-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <UserX size={16} color="#ef4444" />
                <h3 className="panel-title">Vi phạm gần đây</h3>
              </div>
              <a href="/librarian/violation" className="panel-link">
                Xem tất cả <ChevronRight size={14} />
              </a>
            </div>

            {
              stats.recentViolations.length === 0 ? (
                <div className="empty-section">Không có vi phạm nào</div>
              ) : (
                <div className="violations-list">
                  {stats.recentViolations.map((v, idx) => {
                    const statusCfg = getStatusConfig(v.status);
                    return (
                      <div key={idx} className="violation-item violation-item-clickable" onClick={() => setDetailModal({ type: 'violation', data: v })}>
                        <div className="violation-item-left">
                          <div className="violation-avatar">
                            <UserX size={14} />
                          </div>
                          <div className="violation-info">
                            <span className="violation-name">{v.violatorName}</span>
                            <span className="violation-type">{getViolationLabel(v.violationType)}</span>
                          </div>
                        </div>
                        <div className="violation-item-right">
                          <span className="status-badge" style={{ background: statusCfg.bg, color: statusCfg.color }}>
                            {statusCfg.label}
                          </span>
                          <span className="violation-time">{formatRelativeTime(v.createdAt)}</span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )
            }
          </section >
        </div >

        {/* Requests Tabs Section */}
        < section className="dashboard-panel panel-elevated requests-panel" >
          <div className="panel-header">
            <h3 className="panel-title">Yêu cầu và phản hồi</h3>
          </div>
          <div className="request-tabs">
            <button
              className={`request-tab ${activeRequestTab === 'support' ? 'active' : ''}`}
              onClick={() => setActiveRequestTab('support')}
            >
              <LifeBuoy size={14} />
              Hỗ trợ ({stats.recentSupportRequests.length})
            </button>
            <button
              className={`request-tab ${activeRequestTab === 'violation' ? 'active' : ''}`}
              onClick={() => setActiveRequestTab('violation')}
            >
              <ShieldAlert size={14} />
              Vi phạm ({stats.recentViolations.length})
            </button>
            <button
              className={`request-tab ${activeRequestTab === 'complaint' ? 'active' : ''}`}
              onClick={() => setActiveRequestTab('complaint')}
            >
              <FileText size={14} />
              Khiếu nại ({stats.recentComplaints.length})
            </button>
            <button
              className={`request-tab ${activeRequestTab === 'feedback' ? 'active' : ''}`}
              onClick={() => setActiveRequestTab('feedback')}
            >
              <ThumbsUp size={14} />
              Phản hồi ({stats.recentFeedbacks.length})
            </button>
          </div>

          <div className="request-list">
            {getActiveRequestData().length === 0 ? (
              <div className="empty-section">Không có dữ liệu</div>
            ) : (
              getActiveRequestData().map((item, idx) => {
                const statusCfg = getStatusConfig(item.status);
                return (
                  <div key={idx} className="request-item request-item-clickable" onClick={() => setDetailModal({ type: activeRequestTab, data: item })}>
                    <div className="request-item-main">
                      <div className="request-item-user">
                        <span className="request-item-name">
                          {item.studentName || item.violatorName || item.userName || 'N/A'}
                        </span>
                        <span className="request-item-code">
                          {item.studentCode || item.violatorCode || item.userCode || ''}
                        </span>
                      </div>
                      <p className="request-item-desc">
                        {activeRequestTab === 'violation'
                          ? getViolationLabel(item.violationType)
                          : activeRequestTab === 'complaint'
                            ? item.subject
                            : activeRequestTab === 'feedback'
                              ? (
                                <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                                  {item.rating && [...Array(5)].map((_, i) => (
                                    <Star key={i} size={12} fill={i < item.rating ? '#f59e0b' : 'none'} color={i < item.rating ? '#f59e0b' : '#d1d5db'} />
                                  ))}
                                  <span style={{ marginLeft: '6px' }}>{item.content}</span>
                                </span>
                              )
                              : item.description
                        }
                      </p>
                    </div>
                    <div className="request-item-meta">
                      <span className="status-badge" style={{ background: statusCfg.bg, color: statusCfg.color }}>
                        {statusCfg.label}
                      </span>
                      <span className="request-item-time">{formatRelativeTime(item.createdAt)}</span>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </section >

        <div className="dashboard-grid-two dashboard-grid-bottom">
          <section className="dashboard-panel panel-elevated news-panel">
            <div className="panel-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Bell size={16} color="#6b7280" />
                <h3 className="panel-title">Tin tức gần đây</h3>
              </div>
              <a href="/librarian/news" className="panel-link">
                Xem tất cả <ChevronRight size={14} />
              </a>
            </div>

            {recentNews.length === 0 ? (
              <div className="empty-section">Chưa có tin tức</div>
            ) : (
              <div className="news-cards-grid">
                {recentNews.map((news, idx) => (
                  <div key={idx} className="news-card news-card-clickable" onClick={() => setDetailModal({ type: 'news', data: news })}>
                    <div className="news-card-image">
                      {news.imageUrl ? (
                        <img src={news.imageUrl} alt={news.title} />
                      ) : (
                        <div className="news-card-placeholder">
                          <BookOpen size={24} />
                        </div>
                      )}
                      {news.categoryName && (
                        <span className="news-card-category">{news.categoryName}</span>
                      )}
                    </div>
                    <div className="news-card-body">
                      <h4 className="news-card-title">{news.title}</h4>
                      {news.summary && (
                        <p className="news-card-summary">{news.summary}</p>
                      )}
                      <div className="news-card-footer">
                        <div className="news-card-meta">
                          <Calendar size={11} />
                          <span>{news.publishedAt ? formatDate(news.publishedAt) : (news.createdAt ? formatDate(news.createdAt) : '')}</span>
                        </div>
                        <div className="news-card-views">
                          <Eye size={11} />
                          <span>{news.viewCount || 0}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>

          <section className="dashboard-panel panel-elevated books-panel">
            <div className="panel-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <BookOpen size={16} color="#6b7280" />
                <h3 className="panel-title">Sách mới gần đây</h3>
              </div>
              <a href="/librarian/new-books" className="panel-link">
                Xem tất cả <ChevronRight size={14} />
              </a>
            </div>

            {recentNewBooks.length === 0 ? (
              <div className="empty-section">Chưa có sách mới</div>
            ) : (
              <div className="book-list">
                {recentNewBooks.map((book, idx) => {
                  const title = book.title || book.bookTitle || book.name || 'Chưa có tiêu đề';
                  const author = book.author || book.authorName || book.publisher || 'Chưa có thông tin';
                  const cover = book.coverImage || book.coverUrl || book.imageUrl || null;
                  return (
                    <a
                      key={book.id || `${title}-${idx}`}
                      href={`/librarian/new-books${book.id ? `/edit/${book.id}` : ''}`}
                      className="book-row"
                    >
                      <div className="book-row-cover">
                        {cover ? <img src={cover} alt={title} /> : <BookOpen size={18} />}
                      </div>
                      <div className="book-row-main">
                        <span className="book-row-title">{title}</span>
                        <span className="book-row-meta">{author}</span>
                      </div>
                      <div className="book-row-side">
                        <span className="book-row-date">{formatDate(book.createdAt || book.updatedAt || book.publishedAt)}</span>
                        <ChevronRight size={14} />
                      </div>
                    </a>
                  );
                })}
              </div>
            )}
          </section>
        </div>
      </div >

      {/* Student Detail Modal */}
      {
        selectedStudent && (
          <div className="student-modal-overlay" onClick={() => setSelectedStudent(null)}>
            <div className="student-modal" onClick={(e) => e.stopPropagation()}>
              <div className="student-modal-header">
                <h3>Thông tin sinh viên</h3>
                <button className="student-modal-close" onClick={() => setSelectedStudent(null)}>
                  <X size={16} />
                </button>
              </div>
              <div className="student-modal-avatar">
                {selectedStudent.fullName?.charAt(0) || '?'}
              </div>
              <p className="student-modal-name">{selectedStudent.fullName}</p>
              <p className="student-modal-code">{selectedStudent.userCode}</p>
              <div className="student-modal-stats">
                <div className="student-modal-stat">
                  <span className="student-modal-stat-value">
                    {selectedStudent.totalMinutes > 60
                      ? `${Math.floor(selectedStudent.totalMinutes / 60)}h${selectedStudent.totalMinutes % 60}p`
                      : `${selectedStudent.totalMinutes}p`
                    }
                  </span>
                  <span className="student-modal-stat-label">Tổng thời gian học</span>
                </div>
                <div className="student-modal-stat">
                  <span className="student-modal-stat-value">{selectedStudent.totalVisits}</span>
                  <span className="student-modal-stat-label">Số lần ghé thăm</span>
                </div>
                <div className="student-modal-stat">
                  <span className="student-modal-stat-value">
                    {selectedStudent.totalVisits > 0
                      ? `${Math.round(selectedStudent.totalMinutes / selectedStudent.totalVisits)}p`
                      : '0p'
                    }
                  </span>
                  <span className="student-modal-stat-label">Trung bình mỗi lần</span>
                </div>
                <div className="student-modal-stat">
                  <span className="student-modal-stat-value" style={{ color: '#10b981' }}>
                    {selectedStudent.totalMinutes >= 600 ? 'Xuất sắc' : selectedStudent.totalMinutes >= 300 ? 'Tốt' : 'Khá'}
                  </span>
                  <span className="student-modal-stat-label">Đánh giá</span>
                </div>
              </div>
            </div>
          </div>
        )
      }

      {/* Detail Modal */}
      {
        detailModal && (
          <div className="detail-modal-overlay" onClick={() => setDetailModal(null)}>
            <div className="detail-modal" onClick={(e) => e.stopPropagation()}>
              <div className="detail-modal-header">
                <h3>
                  {detailModal.type === 'booking' && 'Chi tiết đặt chỗ'}
                  {detailModal.type === 'violation' && 'Chi tiết vi phạm'}
                  {detailModal.type === 'support' && 'Chi tiết yêu cầu hỗ trợ'}
                  {detailModal.type === 'complaint' && 'Chi tiết khiếu nại'}
                  {detailModal.type === 'feedback' && 'Chi tiết phản hồi'}
                  {detailModal.type === 'news' && 'Chi tiết tin tức'}
                </h3>
                <button className="detail-modal-close" onClick={() => setDetailModal(null)}>
                  <X size={16} />
                </button>
              </div>

              <div className="detail-modal-body">
                {/* BOOKING */}
                {detailModal.type === 'booking' && (() => {
                  const d = detailModal.data;
                  const sc = getStatusConfig(d.status);
                  return (
                    <>
                      <div className="detail-info-grid">
                        <div className="detail-info-item">
                          <span className="detail-label">Sinh viên</span>
                          <span className="detail-value">{d.userName}</span>
                        </div>
                        <div className="detail-info-item">
                          <span className="detail-label">Mã số</span>
                          <span className="detail-value">{d.userCode}</span>
                        </div>
                        <div className="detail-info-item">
                          <span className="detail-label">Khu vực</span>
                          <span className="detail-value">{d.zoneName}</span>
                        </div>
                        <div className="detail-info-item">
                          <span className="detail-label">Ghế</span>
                          <span className="detail-value">{d.seatCode}</span>
                        </div>
                        <div className="detail-info-item">
                          <span className="detail-label">Bắt đầu</span>
                          <span className="detail-value">{formatDateTime(d.startTime)}</span>
                        </div>
                        <div className="detail-info-item">
                          <span className="detail-label">Kết thúc</span>
                          <span className="detail-value">{formatDateTime(d.endTime)}</span>
                        </div>
                        <div className="detail-info-item">
                          <span className="detail-label">Trạng thái</span>
                          <span className="status-badge" style={{ background: sc.bg, color: sc.color }}>{sc.label}</span>
                        </div>
                        {d.createdAt && (
                          <div className="detail-info-item">
                            <span className="detail-label">Ngày tạo</span>
                            <span className="detail-value">{formatDateTime(d.createdAt)}</span>
                          </div>
                        )}
                      </div>
                      <a
                        href="/librarian/bookings"
                        className="detail-view-btn"
                        onClick={() => setDetailModal(null)}
                      >
                        <ExternalLink size={15} />
                        Đi tới trang quản lý đặt chỗ
                      </a>
                    </>
                  );
                })()}

                {/* VIOLATION */}
                {detailModal.type === 'violation' && (() => {
                  const d = detailModal.data;
                  const sc = getStatusConfig(d.status);
                  return (
                    <>
                      <div className="detail-info-grid">
                        <div className="detail-info-item">
                          <span className="detail-label">Người vi phạm</span>
                          <span className="detail-value">{d.violatorName || 'Không xác định'}</span>
                        </div>
                        <div className="detail-info-item">
                          <span className="detail-label">Loại vi phạm</span>
                          <span className="detail-value">{getViolationLabel(d.violationType)}</span>
                        </div>
                        {d.seatCode && (
                          <div className="detail-info-item">
                            <span className="detail-label">Ghế</span>
                            <span className="detail-value">{d.seatCode}</span>
                          </div>
                        )}
                        <div className="detail-info-item">
                          <span className="detail-label">Trạng thái</span>
                          <span className="status-badge" style={{ background: sc.bg, color: sc.color }}>{sc.label}</span>
                        </div>
                        {d.pointDeducted != null && (
                          <div className="detail-info-item">
                            <span className="detail-label">Điểm trừ</span>
                            <span className="detail-value" style={{ color: '#ef4444', fontWeight: 600 }}>-{d.pointDeducted}</span>
                          </div>
                        )}
                        <div className="detail-info-item">
                          <span className="detail-label">Thời gian</span>
                          <span className="detail-value">{formatDateTime(d.createdAt)}</span>
                        </div>
                      </div>
                      {d.description && (
                        <div className="detail-description">
                          <span className="detail-label">Mô tả</span>
                          <p>{d.description}</p>
                        </div>
                      )}
                      {d.evidenceUrl && (
                        <div className="detail-evidence">
                          <span className="detail-label">Bằng chứng</span>
                          <img src={d.evidenceUrl} alt="Bằng chứng" className="detail-evidence-img" />
                        </div>
                      )}
                      <a
                        href="/librarian/violation"
                        className="detail-view-btn"
                        onClick={() => setDetailModal(null)}
                      >
                        <ExternalLink size={15} />
                        Đi tới trang xử lý vi phạm
                      </a>
                    </>
                  );
                })()}

                {/* SUPPORT REQUEST */}
                {detailModal.type === 'support' && (() => {
                  const d = detailModal.data;
                  const sc = getStatusConfig(d.status);
                  return (
                    <>
                      <div className="detail-info-grid">
                        <div className="detail-info-item">
                          <span className="detail-label">Sinh viên</span>
                          <span className="detail-value">{d.studentName || 'N/A'}</span>
                        </div>
                        <div className="detail-info-item">
                          <span className="detail-label">Mã số</span>
                          <span className="detail-value">{d.studentCode || ''}</span>
                        </div>
                        <div className="detail-info-item">
                          <span className="detail-label">Trạng thái</span>
                          <span className="status-badge" style={{ background: sc.bg, color: sc.color }}>{sc.label}</span>
                        </div>
                        <div className="detail-info-item">
                          <span className="detail-label">Thời gian</span>
                          <span className="detail-value">{formatDateTime(d.createdAt)}</span>
                        </div>
                      </div>
                      {d.description && (
                        <div className="detail-description">
                          <span className="detail-label">Nội dung yêu cầu</span>
                          <p>{d.description}</p>
                        </div>
                      )}
                      {d.adminResponse && (
                        <div className="detail-description detail-response">
                          <span className="detail-label">Phản hồi từ thủ thư</span>
                          <p>{d.adminResponse}</p>
                        </div>
                      )}
                      {d.imageUrls && d.imageUrls.length > 0 && (
                        <div className="detail-evidence">
                          <span className="detail-label">Hình ảnh đính kèm</span>
                          <div className="detail-images-grid">
                            {d.imageUrls.map((url, i) => (
                              <img key={i} src={url} alt={`Ảnh ${i + 1}`} className="detail-evidence-img" />
                            ))}
                          </div>
                        </div>
                      )}
                      <a
                        href="/librarian/support-requests"
                        className="detail-view-btn"
                        onClick={() => setDetailModal(null)}
                      >
                        <ExternalLink size={15} />
                        Đi tới trang xử lý yêu cầu hỗ trợ
                      </a>
                    </>
                  );
                })()}

                {/* COMPLAINT */}
                {detailModal.type === 'complaint' && (() => {
                  const d = detailModal.data;
                  const sc = getStatusConfig(d.status);
                  return (
                    <>
                      <div className="detail-info-grid">
                        <div className="detail-info-item">
                          <span className="detail-label">Sinh viên</span>
                          <span className="detail-value">{d.studentName || d.userName || 'N/A'}</span>
                        </div>
                        <div className="detail-info-item">
                          <span className="detail-label">Mã số</span>
                          <span className="detail-value">{d.studentCode || d.userCode || ''}</span>
                        </div>
                        <div className="detail-info-item">
                          <span className="detail-label">Trạng thái</span>
                          <span className="status-badge" style={{ background: sc.bg, color: sc.color }}>{sc.label}</span>
                        </div>
                        <div className="detail-info-item">
                          <span className="detail-label">Thời gian</span>
                          <span className="detail-value">{formatDateTime(d.createdAt)}</span>
                        </div>
                      </div>
                      {d.subject && (
                        <div className="detail-description">
                          <span className="detail-label">Tiêu đề</span>
                          <p style={{ fontWeight: 600 }}>{d.subject}</p>
                        </div>
                      )}
                      {d.content && (
                        <div className="detail-description">
                          <span className="detail-label">Nội dung</span>
                          <p>{d.content}</p>
                        </div>
                      )}
                      {d.response && (
                        <div className="detail-description detail-response">
                          <span className="detail-label">Phản hồi từ thủ thư</span>
                          <p>{d.response}</p>
                        </div>
                      )}
                      <a
                        href="/librarian/complaints"
                        className="detail-view-btn"
                        onClick={() => setDetailModal(null)}
                      >
                        <ExternalLink size={15} />
                        Đi tới trang xử lý khiếu nại
                      </a>
                    </>
                  );
                })()}

                {/* FEEDBACK */}
                {detailModal.type === 'feedback' && (() => {
                  const d = detailModal.data;
                  const sc = getStatusConfig(d.status);
                  return (
                    <>
                      <div className="detail-info-grid">
                        <div className="detail-info-item">
                          <span className="detail-label">Sinh viên</span>
                          <span className="detail-value">{d.studentName || d.userName || 'N/A'}</span>
                        </div>
                        <div className="detail-info-item">
                          <span className="detail-label">Mã số</span>
                          <span className="detail-value">{d.studentCode || d.userCode || ''}</span>
                        </div>
                        <div className="detail-info-item">
                          <span className="detail-label">Trạng thái</span>
                          <span className="status-badge" style={{ background: sc.bg, color: sc.color }}>{sc.label}</span>
                        </div>
                        <div className="detail-info-item">
                          <span className="detail-label">Thời gian</span>
                          <span className="detail-value">{formatDateTime(d.createdAt)}</span>
                        </div>
                      </div>
                      {d.rating && (
                        <div className="detail-rating">
                          <span className="detail-label">Đánh giá</span>
                          <div className="detail-stars">
                            {[...Array(5)].map((_, i) => (
                              <Star key={i} size={20} fill={i < d.rating ? '#f59e0b' : 'none'} color={i < d.rating ? '#f59e0b' : '#d1d5db'} />
                            ))}
                            <span className="detail-rating-text">{d.rating}/5</span>
                          </div>
                        </div>
                      )}
                      {d.content && (
                        <div className="detail-description">
                          <span className="detail-label">Nội dung phản hồi</span>
                          <p>{d.content}</p>
                        </div>
                      )}
                      <a
                        href="/librarian/feedback"
                        className="detail-view-btn"
                        onClick={() => setDetailModal(null)}
                      >
                        <ExternalLink size={15} />
                        Đi tới trang quản lý phản hồi
                      </a>
                    </>
                  );
                })()}

                {/* NEWS */}
                {detailModal.type === 'news' && (() => {
                  const d = detailModal.data;
                  return (
                    <>
                      {d.imageUrl && (
                        <div className="detail-news-image">
                          <img src={d.imageUrl} alt={d.title} />
                        </div>
                      )}
                      <div className="detail-info-grid">
                        {d.categoryName && (
                          <div className="detail-info-item">
                            <span className="detail-label">Danh mục</span>
                            <span className="detail-value">{d.categoryName}</span>
                          </div>
                        )}
                        <div className="detail-info-item">
                          <span className="detail-label">Ngày đăng</span>
                          <span className="detail-value">{formatDateTime(d.publishedAt || d.createdAt)}</span>
                        </div>
                        <div className="detail-info-item">
                          <span className="detail-label">Lượt xem</span>
                          <span className="detail-value">{d.viewCount || 0}</span>
                        </div>
                      </div>
                      <div className="detail-description">
                        <h4 style={{ margin: '0 0 8px', fontSize: '15px' }}>{d.title}</h4>
                        {d.summary && <p>{d.summary}</p>}
                        {d.content && <p style={{ marginTop: '8px' }}>{d.content}</p>}
                      </div>
                      <a
                        href={`/librarian/news/view/${d.id}`}
                        className="detail-view-btn"
                        onClick={() => setDetailModal(null)}
                      >
                        <Eye size={15} />
                        Xem chi tiết bài viết
                      </a>
                    </>
                  );
                })()}
              </div >
            </div>
          </div>
        )
      }
    </>
  );
};

export default Dashboard;
