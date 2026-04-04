import React, { useMemo, useState, useEffect } from "react";
import { API_BASE_URL } from '../../../config/apiConfig';
import {
  Users, Armchair, AlertCircle, AlertTriangle, Sparkles, Clock,
  Bell, Calendar, ChevronRight, BookOpen,
  ArrowDownLeft, ArrowUpRight, CalendarCheck,
  MessageSquare, TrendingUp, RefreshCw,
  Star, ShieldAlert, LifeBuoy, Award, UserX,
  ThumbsUp, FileText, Eye, BarChart3,
  MapPin, Layers, X, ExternalLink, MonitorPlay,
  Wifi, WifiOff, ShieldCheck, Siren, Activity
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
  const [wsConnected, setWsConnected] = useState(false);
  const [pendingCounts, setPendingCounts] = useState({
    feedbackNew: 0, complaintPending: 0, supportPending: 0, supportInProgress: 0
  });
  const [realtimeCapacity, setRealtimeCapacity] = useState(null);
  const [peakHours, setPeakHours] = useState([]);
  const [quietHours, setQuietHours] = useState([]);
  const [densityHours, setDensityHours] = useState([]);
  const [behaviorIssues, setBehaviorIssues] = useState([]);
  const [erroredViolationAvatars, setErroredViolationAvatars] = useState(new Set());
  const [chatOverview, setChatOverview] = useState({
    pendingReplies: 0,
    latestPending: null,
    totalActive: 0,
  });
  const [serviceSignals, setServiceSignals] = useState({
    websocket: false,
    aiAnalytics: true,
    realtimeCapacity: true,
    accessFeedFresh: true,
  });

  const getAuthToken = () =>
    sessionStorage.getItem('librarian_token') || localStorage.getItem('librarian_token');

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

  const renderViolationAvatar = (avatarUrl, name) => {
    const initial = name?.charAt(0)?.toUpperCase() || '?';
    if (avatarUrl && !erroredViolationAvatars.has(avatarUrl)) {
      return (
        <img
          src={avatarUrl}
          alt={name || 'Sinh viên'}
          className="violation-avatar-image"
          onError={() => setErroredViolationAvatars((previous) => new Set(previous).add(avatarUrl))}
        />
      );
    }

    return (
      <div className="violation-avatar violation-avatar-fallback">
        {initial}
      </div>
    );
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

  const fetchChatOverview = async () => {
    try {
      const token = getAuthToken();
      if (!token) return;

      const response = await fetch(`${API_BASE_URL}/slib/chat/conversations/all`, {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error('Không thể tải tổng quan chat');
      }

      const data = await response.json();
      const sortedData = sortByNewest(data || []);
      const actionableConversations = sortedData.filter((conv) => {
        const status = conv?.status;
        const senderType = conv?.lastMessage?.senderType;
        const unreadCount = Number(conv?.unreadCount || 0);

        if (status === 'QUEUE_WAITING') return true;
        if (status === 'HUMAN_CHATTING' && unreadCount > 0) return true;
        return status === 'HUMAN_CHATTING' && senderType === 'STUDENT';
      });

      setChatOverview({
        pendingReplies: actionableConversations.length,
        latestPending: actionableConversations[0] || null,
        totalActive: sortedData.filter((conv) => conv?.status === 'HUMAN_CHATTING' || conv?.status === 'QUEUE_WAITING')
          .length,
      });
    } catch (error) {
      console.warn('Could not fetch chat overview:', error);
      setChatOverview({
        pendingReplies: 0,
        latestPending: null,
        totalActive: 0,
      });
    }
  };

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      let analyticsHealthy = true;
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
        setServiceSignals((prev) => ({ ...prev, realtimeCapacity: true }));
      } catch (e) {
        console.warn('Could not fetch realtime capacity:', e);
        setServiceSignals((prev) => ({ ...prev, realtimeCapacity: false }));
      }

      // Fetch AI Analytics data
      try {
        const peakRes = await getPeakHours();
        const peakData = peakRes?.data || {};
        setPeakHours(Array.isArray(peakData.peak_hours) ? peakData.peak_hours.slice(0, 3) : []);
        setQuietHours(Array.isArray(peakData.quiet_hours) ? peakData.quiet_hours : []);
      } catch (e) { analyticsHealthy = false; console.warn('Could not fetch peak hours:', e); }

      try {
        const densityData = await getDensityPrediction();
        setDensityHours(Array.isArray(densityData?.hourly_predictions) ? densityData.hourly_predictions : []);
      } catch (e) { analyticsHealthy = false; console.warn('Could not fetch density:', e); }

      try {
        const behaviorData = await getBehaviorSummary();
        setBehaviorIssues(Array.isArray(behaviorData?.students) ? behaviorData.students : []);
      } catch (e) { analyticsHealthy = false; console.warn('Could not fetch behavior issues:', e); }

      await fetchChatOverview();
      setServiceSignals((prev) => ({ ...prev, aiAnalytics: analyticsHealthy }));
    } catch (e) {
      console.error('Error fetching dashboard data:', e);
      setInsights([]);
      setServiceSignals((prev) => ({ ...prev, aiAnalytics: false }));
    } finally {
      setLoading(false);
    }
  };

  const fetchAccessLogs = async () => {
    try {
      const today = new Date().toISOString().split('T')[0];
      const logs = await librarianService.getAccessLogsByDateRange(today, today);
      setAccessLogs(logs.slice(0, 15));
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
        setWsConnected(true);
        setServiceSignals((prev) => ({ ...prev, websocket: true }));
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
        setWsConnected(false);
        setServiceSignals((prev) => ({ ...prev, websocket: false }));
        console.error('[Dashboard] WebSocket error:', error);
      }
    );
    return () => {
      setWsConnected(false);
      setServiceSignals((prev) => ({ ...prev, websocket: false }));
      unsubscribers.forEach(unsub => { if (unsub) unsub(); });
    };
  }, []);

  // Fallback polling 60s - đảm bảo dashboard cập nhật khi WebSocket message không đến
  useEffect(() => {
    const interval = setInterval(() => {
      refreshStatsOnly();
      fetchChatOverview();
      setServiceSignals((prev) => ({ ...prev, websocket: websocketService.isConnected() }));
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
    overdueSupportRequests: dashStats?.overdueSupportRequests || 0,
    pendingSeatStatusReports: dashStats?.pendingSeatStatusReports || 0,
    totalUsers: dashStats?.totalUsers || 0,
    recentBookings: dashStats?.recentBookings || [],
    areaOccupancies: dashStats?.areaOccupancies || [],
    weeklyStats: dashStats?.weeklyStats || [],
    recentViolations: dashStats?.recentViolations || [],
    topStudents: dashStats?.topStudents || [],
    recentSupportRequests: dashStats?.recentSupportRequests || [],
    recentComplaints: dashStats?.recentComplaints || [],
    recentFeedbacks: dashStats?.recentFeedbacks || [],
    recentSeatStatusReports: dashStats?.recentSeatStatusReports || [],
    zoneOccupancies: dashStats?.zoneOccupancies || [],
    trendSummary: dashStats?.trendSummary || {}
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
      iconClass: 'quick-action-icon--chat',
      note: `${pendingCounts.supportPending + pendingCounts.supportInProgress} yêu cầu đang hoạt động`,
    },
    {
      label: 'Check-in / Check-out',
      href: '/librarian/checkinout',
      icon: MonitorPlay,
      iconClass: 'quick-action-icon--checkin',
      note: `${stats.totalCheckInsToday} lượt vào hôm nay`,
    },
    {
      label: 'Xử lý hỗ trợ',
      href: '/librarian/support-requests',
      icon: LifeBuoy,
      iconClass: 'quick-action-icon--support',
      note: `${pendingCounts.supportPending} chờ nhận`,
    },
    {
      label: 'Kiểm tra ghế',
      href: '/librarian/seat-status-reports',
      icon: FileText,
      iconClass: 'quick-action-icon--seat',
      note: 'Rà soát báo cáo ghế',
    },
    {
      label: 'Quản lý đặt chỗ',
      href: '/librarian/bookings',
      icon: CalendarCheck,
      iconClass: 'quick-action-icon--booking',
      note: `${stats.activeBookings} lượt đang hiệu lực`,
    },
    {
      label: 'Đăng tin tức',
      href: '/librarian/news/create',
      icon: Bell,
      iconClass: 'quick-action-icon--news',
      note: 'Tạo thông báo mới',
    },
    {
      label: 'Slideshow / Kiosk',
      href: '/librarian/slideshow-management',
      icon: Layers,
      iconClass: 'quick-action-icon--slideshow',
      note: 'Cập nhật slide hiển thị',
    },
    {
      label: 'Cập nhật sách mới',
      href: '/librarian/new-books',
      icon: BookOpen,
      iconClass: 'quick-action-icon--book',
      note: `${recentNewBooks.length} mục gần nhất`,
    },
  ]), [
    pendingCounts.supportPending,
    pendingCounts.supportInProgress,
    stats.totalCheckInsToday,
    stats.activeBookings,
    recentNewBooks.length,
  ]);

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

  const todayPriorityItems = useMemo(() => {
    const firstSupport = stats.recentSupportRequests?.[0];
    const firstSeatStatus = stats.recentSeatStatusReports?.[0];
    const firstComplaint = stats.recentComplaints?.[0];
    const firstViolation = stats.recentViolations?.[0];

    const items = [
      {
        key: 'support-overdue',
        label: 'Hỗ trợ chờ quá 10 phút',
        count: stats.overdueSupportRequests,
        href: firstSupport
          ? `/librarian/support-requests?detail=${firstSupport.id}`
          : '/librarian/support-requests?status=PENDING',
        tone: 'orange',
        hint: firstSupport?.description || 'Ưu tiên nhận các yêu cầu mới bị tồn.',
      },
      {
        key: 'seat-status-pending',
        label: 'Ghế lỗi chưa xác minh',
        count: stats.pendingSeatStatusReports,
        href: firstSeatStatus
          ? `/librarian/seat-status-reports?status=PENDING&detail=${firstSeatStatus.id}`
          : '/librarian/seat-status-reports?status=PENDING',
        tone: 'amber',
        hint: firstSeatStatus
          ? `${firstSeatStatus.seatCode} - ${firstSeatStatus.issueType}`
          : 'Kiểm tra các báo cáo ghế mới nhất.',
      },
      {
        key: 'complaint-new',
        label: 'Khiếu nại mới',
        count: pendingCounts.complaintPending,
        href: firstComplaint
          ? `/librarian/complaints?detail=${firstComplaint.id}`
          : '/librarian/complaints?status=PENDING',
        tone: 'red',
        hint: firstComplaint?.subject || 'Có khiếu nại đang cần phản hồi.',
      },
      {
        key: 'violation-new',
        label: 'Vi phạm mới',
        count: stats.pendingViolations,
        href: firstViolation
          ? `/librarian/violation?detail=${firstViolation.id}`
          : '/librarian/violation',
        tone: 'blue',
        hint: firstViolation?.violatorName
          ? `${firstViolation.violatorName} vừa bị ghi nhận.`
          : 'Theo dõi các báo cáo vi phạm mới.',
      },
    ];

    return items.filter((item) => item.count > 0);
  }, [
    stats.overdueSupportRequests,
    stats.pendingSeatStatusReports,
    stats.pendingViolations,
    pendingCounts.complaintPending,
    stats.recentSupportRequests,
    stats.recentSeatStatusReports,
    stats.recentComplaints,
    stats.recentViolations,
  ]);

  const chatReplySummary = useMemo(() => {
    const latest = chatOverview.latestPending;
    return {
      count: chatOverview.pendingReplies,
      active: chatOverview.totalActive,
      href: latest?.id
        ? `/librarian/chat?conversationId=${latest.id}`
        : '/librarian/chat',
      studentName: latest?.studentName || 'Chưa có cuộc chat cần phản hồi',
      preview: latest?.lastMessage?.content || latest?.escalationReason || 'Theo dõi các cuộc trò chuyện đang chờ thư viện phản hồi.',
      time: latest?.lastMessage?.createdAt || latest?.updatedAt || latest?.createdAt || null,
      status: latest?.status || null,
    };
  }, [chatOverview]);

  const trendCards = useMemo(() => {
    const compareMetric = (todayValue, yesterdayValue, positiveWhenUp = true) => {
      const delta = Number(todayValue || 0) - Number(yesterdayValue || 0);
      const pct = yesterdayValue > 0
        ? Math.round((delta / yesterdayValue) * 100)
        : (todayValue > 0 ? 100 : 0);

      let direction = 'flat';
      if (delta > 0) direction = positiveWhenUp ? 'up' : 'down';
      if (delta < 0) direction = positiveWhenUp ? 'down' : 'up';

      return {
        todayValue: Number(todayValue || 0),
        yesterdayValue: Number(yesterdayValue || 0),
        delta,
        pct,
        direction,
      };
    };

    const trend = stats.trendSummary || {};

    return [
      {
        key: 'checkins',
        label: 'Check-in',
        href: '/librarian/checkinout',
        icon: Users,
        tone: 'blue',
        ...compareMetric(trend.checkInsToday, trend.checkInsYesterday, true),
      },
      {
        key: 'bookings',
        label: 'Đặt chỗ',
        href: '/librarian/bookings',
        icon: CalendarCheck,
        tone: 'orange',
        ...compareMetric(trend.bookingsToday, trend.bookingsYesterday, true),
      },
      {
        key: 'violations',
        label: 'Vi phạm',
        href: '/librarian/violation',
        icon: ShieldAlert,
        tone: 'red',
        ...compareMetric(trend.violationsToday, trend.violationsYesterday, false),
      },
      {
        key: 'feedback',
        label: 'Phản hồi',
        href: '/librarian/feedback',
        icon: ThumbsUp,
        tone: 'green',
        ...compareMetric(trend.feedbackToday, trend.feedbackYesterday, true),
      },
    ];
  }, [stats.trendSummary]);

  const areaAttentionItems = useMemo(() => {
    const reportCountByZone = (stats.recentSeatStatusReports || []).reduce((acc, item) => {
      const key = item.zoneName || item.areaName || 'Khác';
      acc[key] = (acc[key] || 0) + 1;
      return acc;
    }, {});

    return [...(stats.zoneOccupancies || [])]
      .map((zone) => {
        const issueCount = reportCountByZone[zone.zoneName] || 0;
        const occupancy = Number(zone.occupancyPercentage || 0);
        const score = occupancy + issueCount * 18;
        let tone = 'green';
        if (occupancy >= 85 || issueCount >= 2) tone = 'red';
        else if (occupancy >= 65 || issueCount >= 1) tone = 'amber';

        const hints = [];
        if (occupancy >= 60) hints.push(`công suất ${Math.round(occupancy)}%`);
        if (issueCount > 0) hints.push(`${issueCount} báo cáo ghế mới`);
        if (hints.length === 0) hints.push('chưa có dấu hiệu bất thường');

        return {
          key: `${zone.areaName}-${zone.zoneName}`,
          title: zone.zoneName,
          subtitle: zone.areaName,
          tone,
          score,
          href: issueCount > 0 ? '/librarian/seat-status-reports' : '/librarian/checkinout',
          hint: hints.join(' · '),
          occupancy,
          issueCount,
        };
      })
      .sort((a, b) => b.score - a.score)
      .slice(0, 4);
  }, [stats.zoneOccupancies, stats.recentSeatStatusReports]);

  const activityFeedItems = useMemo(() => {
    const feed = [];

    accessLogs.slice(0, 6).forEach((log) => {
      const time = log.action === 'CHECK_IN' ? log.checkInTime : log.checkOutTime;
      feed.push({
        key: `access-${log.logId}-${log.action}`,
        time,
        title: `${log.userName} ${log.action === 'CHECK_IN' ? 'vừa vào thư viện' : 'vừa rời thư viện'}`,
        description: log.userCode || 'Bản ghi ra vào thư viện',
        icon: log.action === 'CHECK_IN' ? ArrowUpRight : ArrowDownLeft,
        tone: log.action === 'CHECK_IN' ? 'green' : 'slate',
        href: '/librarian/checkinout',
      });
    });

    (stats.recentSupportRequests || []).slice(0, 3).forEach((item) => {
      feed.push({
        key: `support-${item.id}`,
        time: item.createdAt,
        title: `${item.studentName} gửi yêu cầu hỗ trợ`,
        description: item.description || 'Yêu cầu hỗ trợ mới',
        icon: LifeBuoy,
        tone: 'orange',
        href: '/librarian/support-requests',
      });
    });

    (stats.recentSeatStatusReports || []).slice(0, 3).forEach((item) => {
      feed.push({
        key: `seat-status-${item.id}`,
        time: item.createdAt,
        title: `${item.seatCode} có báo cáo ${item.issueType?.toLowerCase() || 'sự cố'}`,
        description: `${item.zoneName} · ${item.userName}`,
        icon: FileText,
        tone: 'amber',
        href: '/librarian/seat-status-reports',
      });
    });

    (stats.recentViolations || []).slice(0, 3).forEach((item) => {
      feed.push({
        key: `violation-${item.id}`,
        time: item.createdAt,
        title: `${item.violatorName} bị báo vi phạm`,
        description: getViolationLabel(item.violationType),
        icon: ShieldAlert,
        tone: 'red',
        href: '/librarian/violation',
      });
    });

    return feed
      .filter((item) => item.time)
      .sort((a, b) => new Date(b.time).getTime() - new Date(a.time).getTime())
      .slice(0, 8);
  }, [accessLogs, stats.recentSupportRequests, stats.recentSeatStatusReports, stats.recentViolations]);

  const systemAlerts = useMemo(() => {
    const alerts = [];

    if (!serviceSignals.websocket) {
      alerts.push({
        key: 'ws-offline',
        tone: 'red',
        title: 'Kênh realtime đang gián đoạn',
        description: 'Badge và cập nhật tức thời có thể đến chậm hơn bình thường.',
        icon: WifiOff,
      });
    }

    if (!serviceSignals.realtimeCapacity || !serviceSignals.aiAnalytics) {
      alerts.push({
        key: 'ai-analytics',
        tone: 'amber',
        title: 'Dữ liệu phân tích chưa phản hồi đầy đủ',
        description: 'Kiểm tra lại dữ liệu phân tích nếu thẻ công suất hoặc nhận định vận hành chưa hiển thị.',
        icon: Sparkles,
      });
    }

    if (stats.pendingSeatStatusReports >= 5) {
      alerts.push({
        key: 'seat-buildup',
        tone: 'orange',
        title: 'Báo cáo ghế đang dồn',
        description: `${stats.pendingSeatStatusReports} báo cáo ghế đang chờ xác minh hoặc xử lý.`,
        icon: Siren,
      });
    }

    if (alerts.length === 0) {
      alerts.push(
        {
          key: 'ws-ok',
          tone: 'green',
          title: 'Kết nối thời gian thực ổn định',
          description: 'WebSocket đang hoạt động, bảng điều hành đang nhận dữ liệu mới bình thường.',
          icon: Wifi,
        },
        {
          key: 'system-ok',
          tone: 'blue',
          title: 'Các dịch vụ hỗ trợ đang hoạt động bình thường',
          description: 'Dữ liệu công suất, phân tích hỗ trợ và bảng điều hành đều phản hồi tốt.',
          icon: ShieldCheck,
        }
      );
    }

    return alerts.slice(0, 3);
  }, [serviceSignals, stats.pendingSeatStatusReports]);

  return (
    <>
      {loading ? (
        <div className="dashboard-skeleton">
          <div className="skeleton-title-row" />
          <div className="skeleton-grid">
            <div className="skeleton-block skeleton-block--lg" />
            <div className="skeleton-block skeleton-block--lg" />
          </div>
          <div className="skeleton-grid" style={{ gridTemplateColumns: '1.05fr 0.95fr 0.9fr' }}>
            <div className="skeleton-block skeleton-block--md" />
            <div className="skeleton-block skeleton-block--md" />
            <div className="skeleton-block skeleton-block--md" />
          </div>
        </div>
      ) : (
      <div className="dashboard-container">
        {/* Title row */}
        <div className="dashboard-title-row">
          <div className="dashboard-title-copy">
            <h1 className="dashboard-title">{getGreeting()}</h1>
            <p className="dashboard-subtitle">Theo dõi tình hình vận hành thư viện trong ngày, các việc cần xử lý sớm và những khu vực cần chú ý.</p>
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
            <button className="refresh-btn" onClick={fetchDashboardData} title="Làm mới dữ liệu" aria-label="Làm mới dữ liệu">
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
                      <span className={`quick-action-icon ${action.iconClass || ''}`}>
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
                <h3 className="panel-title">Công việc ưu tiên hôm nay</h3>
              </div>
              {todayPriorityItems.length === 0 ? (
                <div className="empty-section">Hiện chưa có việc gấp cần xử lý</div>
              ) : (
                <div className="urgent-list">
                  {todayPriorityItems.map((item) => (
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

            <section className="dashboard-panel panel-elevated chat-response-panel">
              <div className="panel-header">
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <MessageSquare size={16} color="#2563eb" />
                  <h3 className="panel-title">Chat cần phản hồi</h3>
                </div>
                <a href={chatReplySummary.href} className="panel-link">
                  Vào chat <ChevronRight size={14} />
                </a>
              </div>

              <div className="chat-response-card">
                <div className="chat-response-top">
                  <div>
                    <div className="chat-response-count">{chatReplySummary.count}</div>
                    <div className="chat-response-label">cuộc chat đang chờ thư viện phản hồi</div>
                  </div>
                  <div className="chat-response-meta">
                    <span className="dashboard-inline-pill">{chatReplySummary.active} đang hoạt động</span>
                  </div>
                </div>

                <a href={chatReplySummary.href} className="chat-response-highlight">
                  <div className="chat-response-highlight-copy">
                    <span className="chat-response-student">{chatReplySummary.studentName}</span>
                    <span className="chat-response-preview">{chatReplySummary.preview}</span>
                  </div>
                  <div className="chat-response-highlight-side">
                    <span className="chat-response-time">{formatRelativeTime(chatReplySummary.time)}</span>
                    <ChevronRight size={15} />
                  </div>
                </a>
              </div>
            </section>

          </div>
        </div>

        <div className="dashboard-grid-two dashboard-grid-equal-height">
          <section className="dashboard-panel panel-elevated trend-panel">
            <div className="panel-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <TrendingUp size={16} color="#ea580c" />
                <h3 className="panel-title">Xu hướng hôm nay so với hôm qua</h3>
              </div>
            </div>

            <div className="trend-grid">
              {trendCards.map((item) => {
                const Icon = item.icon;
                const deltaPrefix = item.delta > 0 ? '+' : '';
                const deltaLabel = item.delta === 0 ? 'Không đổi' : `${deltaPrefix}${item.pct}%`;
                return (
                  <a key={item.key} href={item.href} className={`trend-card trend-card--${item.tone}`}>
                    <div className="trend-card-head">
                      <span className="trend-card-icon"><Icon size={16} /></span>
                      <span className={`trend-card-badge trend-card-badge--${item.direction}`}>
                        {item.direction === 'up' && <ArrowUpRight size={13} />}
                        {item.direction === 'down' && <ArrowDownLeft size={13} />}
                        {item.direction === 'flat' && <Activity size={13} />}
                        {deltaLabel}
                      </span>
                    </div>
                    <div className="trend-card-value">{item.todayValue}</div>
                    <div className="trend-card-label">{item.label}</div>
                    <div className="trend-card-foot">Hôm qua: {item.yesterdayValue}</div>
                  </a>
                );
              })}
            </div>
          </section>

          <section className="dashboard-panel panel-elevated zone-status-side">
              <div className="panel-header">
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <h3 className="panel-title">Toàn cảnh khu vực</h3>
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
                <div className="empty-section">Chưa có dữ liệu công suất theo khu vực</div>
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
                  <option value="year">Năm nay</option>
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
              <h3 className="panel-title">Nhận định hỗ trợ</h3>
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
              <div className="empty-section">Chưa đủ dữ liệu để hiển thị</div>
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
                  <span className="behavior-title">Sinh viên cần lưu ý</span>
                </div>
                {behaviorIssues.length > 0 && (
                  <span className="behavior-count">{behaviorIssues.length}</span>
                )}
              </div>

              {behaviorIssues.length === 0 ? (
                <div className="behavior-ok">
                  <span>✓ Chưa có sinh viên nào cần lưu ý</span>
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
        <div className="dashboard-grid-mid dashboard-grid-equal-height">
          <section className="dashboard-panel panel-elevated">
            <div className="panel-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <h3 className="panel-title" style={{ whiteSpace: 'nowrap' }}>Sinh viên ra vào hôm nay</h3>
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
                Hiện chưa có sinh viên nào ra vào thư viện
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
              <h3 className="panel-title" style={{ whiteSpace: 'nowrap' }}>Đặt chỗ hôm nay</h3>
              <a href="/librarian/bookings" className="view-all-link">Xem tất cả</a>
            </div>

            {stats.recentBookings.length === 0 ? (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 1, minHeight: '300px', color: 'var(--muted)', fontSize: '14px', fontStyle: 'italic' }}>
                Chưa có lượt đặt chỗ nào hôm nay
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
        <div className="dashboard-grid-two dashboard-grid-equal-height">
          {/* Top 5 students */}
          <section className="dashboard-panel panel-elevated">
            <div className="panel-header">
              <h3 className="panel-title" style={{ whiteSpace: 'nowrap' }}>Sinh viên sử dụng nổi bật</h3>
              <select
                className="chart-range-select"
                style={{ width: 'auto', flexShrink: 0 }}
                value={topStudentsRange}
                onChange={(e) => setTopStudentsRange(e.target.value)}
              >
                <option value="week">Tuần này</option>
                <option value="month">Tháng này</option>
                <option value="year">Năm nay</option>
              </select>
            </div>

            {topStudentsData.length === 0 ? (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 1, minHeight: '200px', color: 'var(--muted)', fontSize: '14px', fontStyle: 'italic' }}>
                Chưa có dữ liệu sinh viên nổi bật
              </div>
            ) : (
              <div className="top-students-list dashboard-entity-list">
                {topStudentsData.map((student, idx) => (
                  <div key={idx} className="top-student-item dashboard-entity-item">
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
                <h3 className="panel-title">Vi phạm chờ xử lý</h3>
              </div>
              <a href="/librarian/violation" className="panel-link">
                Xem tất cả <ChevronRight size={14} />
              </a>
            </div>

            {
              stats.recentViolations.length === 0 ? (
                <div className="empty-section">Chưa có vi phạm cần xử lý</div>
              ) : (
                <div className="violations-list dashboard-entity-list">
                  {stats.recentViolations.map((v, idx) => {
                    const statusCfg = getStatusConfig(v.status);
                    return (
                        <div key={idx} className="violation-item violation-item-clickable dashboard-entity-item" onClick={() => setDetailModal({ type: 'violation', data: v })}>
                        <div className="violation-item-left">
                          <div className="violation-avatar-shell">
                            {renderViolationAvatar(v.avatarUrl, v.violatorName)}
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
        <div className="dashboard-grid-two dashboard-grid-balance">
          <section className="dashboard-panel panel-elevated activity-panel">
            <div className="panel-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Clock size={16} color="#475569" />
                <h3 className="panel-title">Hoạt động gần đây</h3>
              </div>
            </div>
            {activityFeedItems.length === 0 ? (
              <div className="empty-section">Chưa có hoạt động mới</div>
            ) : (
              <div className="activity-feed">
                {activityFeedItems.map((item) => {
                  const Icon = item.icon;
                  return (
                    <a key={item.key} href={item.href} className={`activity-item activity-item--${item.tone}`}>
                      <span className="activity-icon"><Icon size={15} /></span>
                      <div className="activity-copy">
                        <span className="activity-title">{item.title}</span>
                        <span className="activity-description">{item.description}</span>
                      </div>
                      <span className="activity-time">{formatRelativeTime(item.time)}</span>
                    </a>
                  );
                })}
              </div>
            )}
          </section>

          <section className="dashboard-panel panel-elevated requests-panel">
            <div className="panel-header">
              <h3 className="panel-title">Yêu cầu chờ xử lý</h3>
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
                <div className="empty-section">Chưa có yêu cầu cần xử lý</div>
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
          </section>
        </div>

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
              <div className="dashboard-media-list">
                {recentNews.map((news, idx) => (
                  <div
                    key={idx}
                    className="dashboard-media-item dashboard-media-item--news news-card-clickable"
                    onClick={() => setDetailModal({ type: 'news', data: news })}
                  >
                    <div className="dashboard-media-thumb">
                      {news.imageUrl ? (
                        <img src={news.imageUrl} alt={news.title} />
                      ) : (
                        <div className="dashboard-media-placeholder">
                          <BookOpen size={24} />
                        </div>
                      )}
                      {news.categoryName && (
                        <span className="dashboard-media-badge">{news.categoryName}</span>
                      )}
                    </div>
                    <div className="dashboard-media-body">
                      <h4 className="dashboard-media-title">{news.title}</h4>
                      {news.summary && (
                        <p className="dashboard-media-summary">{news.summary}</p>
                      )}
                      <div className="dashboard-media-footer">
                        <div className="dashboard-media-meta">
                          <Calendar size={11} />
                          <span>{news.publishedAt ? formatDate(news.publishedAt) : (news.createdAt ? formatDate(news.createdAt) : '')}</span>
                        </div>
                        <div className="dashboard-media-meta dashboard-media-meta--right">
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
              <div className="dashboard-media-list">
                {recentNewBooks.map((book, idx) => {
                  const title = book.title || book.bookTitle || book.name || 'Chưa có tiêu đề';
                  const author = book.author || book.authorName || book.publisher || 'Chưa có thông tin';
                  const cover = book.coverImage || book.coverUrl || book.imageUrl || null;
                  return (
                    <a
                      key={book.id || `${title}-${idx}`}
                      href={`/librarian/new-books${book.id ? `/edit/${book.id}` : ''}`}
                      className="dashboard-media-item dashboard-media-item--book"
                    >
                      <div className="dashboard-media-thumb dashboard-media-thumb--book">
                        {cover ? <img src={cover} alt={title} /> : <BookOpen size={18} />}
                      </div>
                      <div className="dashboard-media-body">
                        <h4 className="dashboard-media-title">{title}</h4>
                        <p className="dashboard-media-summary">{author}</p>
                      </div>
                      <div className="dashboard-media-footer dashboard-media-footer--book">
                        <div className="dashboard-media-meta">
                          <Calendar size={11} />
                          <span>{formatDate(book.createdAt || book.updatedAt || book.publishedAt)}</span>
                        </div>
                        <div className="dashboard-media-meta dashboard-media-meta--right">
                          <BookOpen size={11} />
                          <span>Sách mới</span>
                        </div>
                      </div>
                    </a>
                  );
                })}
              </div>
            )}
          </section>
        </div>
      </div >
      )}

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
