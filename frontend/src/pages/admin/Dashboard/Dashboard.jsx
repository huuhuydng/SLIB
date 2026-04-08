import React, { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import {
  Users,
  Armchair,
  AlertCircle,
  Sparkles,
  Clock,
  Bell,
  Calendar,
  ChevronRight,
  BookOpen,
  MapPin,
  Activity,
  Shield,
  Headphones,
  Star,
  TrendingUp,
  BarChart3,
  MessageSquare,
  Award,
  RefreshCw,
  Loader2,
  AlertTriangle,
  Server,
  Wifi,
  Monitor,
  ArrowUpRight,
  ArrowDownRight,
  ScanLine,
  Settings2,
  Siren,
  ClipboardList
} from "lucide-react";
import StatCard from "./StatCard";
import dashboardService from "../../../services/admin/dashboardService";
import { getRealtimeCapacity } from "../../../services/admin/ai/analyticsService";
import systemHealthService from "../../../services/admin/systemHealthService";
import hceStationService from "../../../services/admin/hceStationService";
import LoadErrorState from "../../../components/common/LoadErrorState";
import "../../../styles/Dashboard.css";

const Dashboard = () => {
  const PANEL_LIMITS = {
    priorityTasks: 6,
    attentionZones: 5,
    recentActivities: 8,
    recentBookings: 8,
    topStudents: 5,
    recentViolations: 5,
    recentSupportRequests: 5,
    recentComplaints: 5,
    recentFeedbacks: 5,
    news: 4,
    recentNewBooks: 4,
  };

  const navigate = useNavigate();
  const [stats, setStats] = useState(null);
  const [topStudents, setTopStudents] = useState([]);
  const [topStudentsRange, setTopStudentsRange] = useState("month");
  const [news, setNews] = useState([]);
  const [recentNewBooks, setRecentNewBooks] = useState([]);
  const [chartData, setChartData] = useState([]);
  const [chartRange, setChartRange] = useState("week");
  const [aiCapacity, setAiCapacity] = useState(null);
  const [systemInfo, setSystemInfo] = useState(null);
  const [stations, setStations] = useState([]);
  const [kioskSessions, setKioskSessions] = useState([]);
  const [sourceStatus, setSourceStatus] = useState({
    ai: "idle",
    system: "idle",
    stations: "idle",
    kiosk: "idle",
    topStudents: "idle",
  });
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const fetchOverview = useCallback(async (isRefresh = false) => {
    try {
      if (isRefresh) setRefreshing(true);
      else setLoading(true);
      setError(null);

      const [statsResult, newsResult, newBooksResult, aiResult, systemResult, stationResult, kioskResult] = await Promise.allSettled([
        dashboardService.getDashboardStats(),
        dashboardService.getRecentNews(),
        dashboardService.getRecentNewBooks(),
        getRealtimeCapacity(),
        systemHealthService.getSystemInfo(),
        hceStationService.getAllStations(),
        dashboardService.getKioskSessions(),
      ]);

      const statsData = statsResult.status === "fulfilled" ? statsResult.value : null;
      if (!statsData) {
        throw new Error("Không thể tải dữ liệu tổng quan");
      }

      setSourceStatus((prev) => ({
        ...prev,
        ai: aiResult.status === "fulfilled" ? "ready" : "error",
        system: systemResult.status === "fulfilled" ? "ready" : "error",
        stations: stationResult.status === "fulfilled" ? "ready" : "error",
        kiosk: kioskResult.status === "fulfilled" ? "ready" : "error",
      }));
      setStats(statsData);
      setNews(newsResult.status === "fulfilled" ? newsResult.value || [] : []);
      setRecentNewBooks(newBooksResult.status === "fulfilled" ? newBooksResult.value || [] : []);
      setAiCapacity(aiResult.status === "fulfilled" ? aiResult.value : null);
      setSystemInfo(systemResult.status === "fulfilled" ? systemResult.value : null);
      setStations(stationResult.status === "fulfilled" ? stationResult.value || [] : []);
      setKioskSessions(kioskResult.status === "fulfilled" ? kioskResult.value || [] : []);
    } catch (e) {
      console.error("Dashboard fetch error:", e);
      setError("Không thể tải dữ liệu tổng quan quản trị");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  const fetchChart = useCallback(async () => {
    try {
      const chart = await dashboardService.getChartStats(chartRange);
      setChartData(chart || []);
    } catch (e) {
      console.error("Chart fetch error:", e);
      setChartData([]);
    }
  }, [chartRange]);

  const fetchTopStudents = useCallback(async () => {
    try {
      const data = await dashboardService.getTopStudents(topStudentsRange);
      setTopStudents(data || []);
      setSourceStatus((prev) => ({ ...prev, topStudents: "ready" }));
    } catch (e) {
      console.error("Top students fetch error:", e);
      setTopStudents([]);
      setSourceStatus((prev) => ({ ...prev, topStudents: "error" }));
    }
  }, [topStudentsRange]);

  useEffect(() => {
    fetchOverview();
    const interval = setInterval(() => fetchOverview(true), 60000);
    return () => clearInterval(interval);
  }, [fetchOverview]);

  useEffect(() => {
    fetchChart();
  }, [fetchChart]);

  useEffect(() => {
    fetchTopStudents();
  }, [fetchTopStudents]);

  useEffect(() => {
    const interval = setInterval(() => fetchChart(), 60000);
    return () => clearInterval(interval);
  }, [fetchChart]);

  useEffect(() => {
    const interval = setInterval(() => fetchTopStudents(), 60000);
    return () => clearInterval(interval);
  }, [fetchTopStudents]);

  const getOccupancyColor = (pct) => {
    if (pct >= 90) return "#D32F2F";
    if (pct >= 60) return "#FF9800";
    return "#4CA75B";
  };

  const getBadgeClass = (status) => {
    const s = (status || "").toUpperCase();
    if (["CONFIRMED", "COMPLETED", "RESOLVED", "ACCEPTED"].includes(s)) return "badge--success";
    if (["BOOKED", "PENDING", "IN_PROGRESS", "VERIFIED"].includes(s)) return "badge--warning";
    if (["CANCELLED", "CANCEL", "REJECTED", "EXPIRED", "DENIED"].includes(s)) return "badge--error";
    return "badge--neutral";
  };

  const translateStatus = (status) => {
    const map = {
      PENDING: "Chờ xử lý",
      IN_PROGRESS: "Đang xử lý",
      RESOLVED: "Đã giải quyết",
      CONFIRMED: "Đã xác nhận",
      COMPLETED: "Hoàn thành",
      BOOKED: "Đã đặt",
      CANCEL: "Đã huỷ",
      CANCELLED: "Đã huỷ",
      REJECTED: "Từ chối",
      EXPIRED: "Hết hạn",
      ACCEPTED: "Chấp nhận",
      DENIED: "Từ chối",
      VERIFIED: "Đã xác minh",
      CHECKED_IN: "Đã check-in",
      CHECKED_OUT: "Đã check-out",
      NO_SHOW: "Vắng mặt",
    };
    return map[(status || "").toUpperCase()] || status;
  };

  const translateViolationType = (type) => {
    const map = {
      UNAUTHORIZED_USE: "Sử dụng ghế không đúng",
      LEFT_BELONGINGS: "Để đồ giữ chỗ",
      NOISE: "Gây ồn ào",
      FEET_ON_SEAT: "Gác chân lên ghế/bàn",
      FOOD_DRINK: "Ăn uống trong thư viện",
      SLEEPING: "Ngủ tại chỗ ngồi",
      OTHER: "Khác",
    };
    return map[(type || "").toUpperCase()] || type || "Không xác định";
  };

  const formatTime = (dt) => {
    if (!dt) return "";
    try {
      const d = new Date(dt);
      return d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
    } catch { return ""; }
  };

  const formatDate = (dt) => {
    if (!dt) return "";
    try {
      const d = new Date(dt);
      return d.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit" });
    } catch { return ""; }
  };

  const formatDateTime = (dt) => {
    if (!dt) return "";
    try {
      const d = new Date(dt);
      return d.toLocaleString("vi-VN", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" });
    } catch { return ""; }
  };

  const getInitials = (name) => {
    if (!name || name === "N/A") return "?";
    return name.split(" ").map(w => w[0]).join("").toUpperCase().substring(0, 2);
  };

  const formatRelativeTime = (dt) => {
    if (!dt) return "Vừa xong";
    try {
      const diffMs = Date.now() - new Date(dt).getTime();
      const diffMinutes = Math.max(0, Math.floor(diffMs / 60000));
      if (diffMinutes < 1) return "Vừa xong";
      if (diffMinutes < 60) return `${diffMinutes} phút trước`;
      const diffHours = Math.floor(diffMinutes / 60);
      if (diffHours < 24) return `${diffHours} giờ trước`;
      const diffDays = Math.floor(diffHours / 24);
      return `${diffDays} ngày trước`;
    } catch {
      return "";
    }
  };

  const getSystemHealthMeta = () => {
    if (!systemInfo) {
      return {
        value: "Chưa tải",
        trend: "neutral",
        trendValue: "Chưa lấy được trạng thái máy chủ",
      };
    }

    const cpu = Number(systemInfo.cpu || 0);
    const memory = Number(systemInfo.memory || 0);
    const disk = Number(systemInfo.disk || 0);
    const maxLoad = Math.max(cpu, memory, disk);

    if (maxLoad >= 90) {
      return {
        value: "Tải rất cao",
        trend: "down",
        trendValue: `CPU ${cpu}% · RAM ${memory}% · Đĩa ${disk}%`,
      };
    }

    if (maxLoad >= 70) {
      return {
        value: "Tải cao, cần theo dõi",
        trend: "neutral",
        trendValue: `CPU ${cpu}% · RAM ${memory}% · Đĩa ${disk}%`,
      };
    }

    return {
      value: "Vận hành bình thường",
      trend: "up",
      trendValue: `CPU ${cpu}% · RAM ${memory}% · Đĩa ${disk}%`,
    };
  };

  const hceOnlineCount = stations.filter((station) => station.online).length;
  const hceOfflineCount = stations.filter((station) => !station.online && station.status !== "MAINTENANCE").length;
  const hceMaintenanceCount = stations.filter((station) => station.status === "MAINTENANCE").length;
  const activeKioskCount = kioskSessions.filter((session) => session.isActive).length;
  const validKioskCount = kioskSessions.filter((session) => session.isActive && session.tokenValid).length;
  const expiredKioskCount = kioskSessions.filter((session) => session.hasDeviceToken && !session.tokenValid).length;
  const systemHealthMeta = getSystemHealthMeta();
  const pendingBacklogCount =
    (stats?.pendingSupportRequests || 0) +
    (stats?.inProgressSupportRequests || 0) +
    (stats?.pendingViolations || 0) +
    (stats?.pendingSeatStatusReports || 0) +
    (stats?.pendingComplaints || 0);
  const systemNeedsAttention = sourceStatus.system === "ready" && systemHealthMeta.trend !== "up" ? 1 : 0;
  const adminAttentionCount =
    pendingBacklogCount +
    (sourceStatus.stations === "ready" ? hceOfflineCount : 0) +
    (sourceStatus.kiosk === "ready" ? expiredKioskCount : 0) +
    systemNeedsAttention;

  const formatDelta = (todayValue = 0, yesterdayValue = 0) => {
    const delta = todayValue - yesterdayValue;
    if (delta === 0) {
      return { icon: <Activity size={16} />, badge: "Không đổi", tone: "neutral" };
    }

    return {
      icon: delta > 0 ? <ArrowUpRight size={14} /> : <ArrowDownRight size={14} />,
      badge: `${delta > 0 ? "Tăng" : "Giảm"} ${Math.abs(delta)}`,
      tone: delta > 0 ? "up" : "down",
    };
  };

  const getChartSummary = () => {
    if (chartRange === "month") {
      return {
        title: "Biến động hoạt động",
        subtitle: "Lượt vào thư viện và đặt chỗ theo từng tuần trong tháng này.",
      };
    }

    if (chartRange === "year") {
      return {
        title: "Biến động hoạt động",
        subtitle: "Lượt vào thư viện và đặt chỗ theo từng tháng trong năm nay.",
      };
    }

    return {
      title: "Biến động hoạt động",
      subtitle: "Lượt vào thư viện và đặt chỗ trong 7 ngày gần đây.",
    };
  };

  const chartSummary = getChartSummary();
  const priorityTasks = stats?.priorityTasks || [];
  const chatAttention = stats?.chatAttention || null;
  const attentionZones = stats?.attentionZones || [];
  const recentActivities = stats?.recentActivities || [];
  const displayedPriorityTasks = priorityTasks.slice(0, PANEL_LIMITS.priorityTasks);
  const displayedAttentionZones = attentionZones.slice(0, PANEL_LIMITS.attentionZones);
  const displayedRecentActivities = recentActivities.slice(0, PANEL_LIMITS.recentActivities);
  const displayedRecentBookings = (stats?.recentBookings || []).slice(0, PANEL_LIMITS.recentBookings);
  const displayedTopStudents = topStudents.slice(0, PANEL_LIMITS.topStudents);
  const displayedRecentViolations = (stats?.recentViolations || []).slice(0, PANEL_LIMITS.recentViolations);
  const displayedRecentSupportRequests = (stats?.recentSupportRequests || []).slice(0, PANEL_LIMITS.recentSupportRequests);
  const displayedRecentComplaints = (stats?.recentComplaints || []).slice(0, PANEL_LIMITS.recentComplaints);
  const displayedRecentFeedbacks = (stats?.recentFeedbacks || []).slice(0, PANEL_LIMITS.recentFeedbacks);
  const displayedNews = news.slice(0, PANEL_LIMITS.news);
  const displayedRecentNewBooks = recentNewBooks.slice(0, PANEL_LIMITS.recentNewBooks);
  const sourceErrorLabels = [
    sourceStatus.system === "error" ? "máy chủ thư viện" : null,
    sourceStatus.stations === "error" ? "trạm quét HCE" : null,
    sourceStatus.kiosk === "error" ? "kiosk thư viện" : null,
    sourceStatus.ai === "error" ? "dịch vụ AI" : null,
    sourceStatus.topStudents === "error" ? "thống kê sinh viên nổi bật" : null,
  ].filter(Boolean);
  const systemCard = sourceStatus.system === "error"
    ? {
        title: "Tình trạng máy chủ",
        value: "Không rõ",
        detail: "Không lấy được trạng thái máy chủ",
        tone: "down",
        icon: <Server size={18} />,
        warning: "Lỗi nguồn dữ liệu",
      }
    : {
        title: "Tình trạng máy chủ",
        value: systemHealthMeta.value,
        detail: systemHealthMeta.trendValue,
        tone: systemHealthMeta.trend,
        icon: <Server size={18} />,
      };
  const hceCard = sourceStatus.stations === "error"
    ? {
        title: "Trạm quét HCE",
        value: "Không rõ",
        detail: "Không lấy được trạng thái trạm",
        tone: "down",
        icon: <Wifi size={18} />,
        warning: "Lỗi nguồn dữ liệu",
      }
    : {
        title: "Trạm quét HCE",
        value: stations.length > 0 ? `${hceOnlineCount}/${stations.length}` : "Chưa có",
        detail: stations.length > 0
          ? `${hceMaintenanceCount} trạm bảo trì, ${hceOfflineCount} trạm ngoại tuyến`
          : "Chưa khai báo trạm quét",
        tone: stations.length > 0 && hceOfflineCount > 0 ? "down" : "neutral",
        icon: <Wifi size={18} />,
      };
  const kioskCard = sourceStatus.kiosk === "error"
    ? {
        title: "Kiosk thư viện",
        value: "Không rõ",
        detail: "Không lấy được trạng thái kiosk",
        tone: "down",
        icon: <Monitor size={18} />,
        warning: "Lỗi nguồn dữ liệu",
      }
    : {
        title: "Kiosk thư viện",
        value: activeKioskCount > 0 ? `${validKioskCount}/${activeKioskCount}` : "Chưa có",
        detail: activeKioskCount > 0
          ? `${expiredKioskCount} kiosk hết hiệu lực`
          : "Chưa có kiosk đang hoạt động",
        tone: expiredKioskCount > 0 ? "down" : "up",
        icon: <Monitor size={18} />,
      };
  const topStudentsRangeLabel = {
    week: "Tuần này",
    month: "Tháng này",
    year: "Năm nay",
  };
  const topStudentsSubtitle = {
    week: "Những sinh viên có thời lượng sử dụng chỗ ngồi thực tế cao trong 7 ngày gần đây.",
    month: "Những sinh viên có thời lượng sử dụng chỗ ngồi thực tế cao trong 30 ngày gần đây.",
    year: "Những sinh viên có thời lượng sử dụng chỗ ngồi thực tế cao trong 12 tháng gần đây.",
  };
  const adminQuickActions = [
    { title: "Người dùng", desc: "Quản lý tài khoản và phân quyền", icon: <Users size={18} />, path: "/admin/users", tone: "purple" },
    { title: "Trạm quét NFC", desc: "Theo dõi và cấu hình thiết bị cổng", icon: <ScanLine size={18} />, path: "/admin/devices", tone: "blue" },
    { title: "Kiosk thư viện", desc: "Kiểm tra kết nối và cấp mã kích hoạt", icon: <Monitor size={18} />, path: "/admin/kiosk", tone: "orange" },
    { title: "Bản đồ thư viện", desc: "Điều chỉnh khu vực, ghế và mặt bằng", icon: <MapPin size={18} />, path: "/admin/library-map", tone: "green" },
    { title: "Cấu hình thư viện", desc: "Lịch phục vụ và quy tắc vận hành", icon: <Settings2 size={18} />, path: "/admin/config", tone: "teal" },
    { title: "AI và kho tri thức", desc: "Nhà cung cấp AI, tài liệu và đồng bộ", icon: <Sparkles size={18} />, path: "/admin/ai-config", tone: "pink" },
    { title: "Sức khỏe hệ thống", desc: "Giám sát máy chủ, nhật ký và sao lưu", icon: <Server size={18} />, path: "/admin/health", tone: "slate" },
    { title: "NFC Bridge", desc: "Công cụ cục bộ cho máy quét NFC", icon: <Wifi size={18} />, path: "/admin/nfc-management", tone: "gold" },
  ];

  const serviceAlerts = [];
  if (sourceStatus.system === "error") {
    serviceAlerts.push({
      key: "system-fetch",
      severity: "critical",
      title: "Không lấy được trạng thái hạ tầng",
      detail: "Dashboard chưa đọc được tình trạng máy chủ và các dịch vụ lõi.",
      action: "/admin/health",
    });
  } else if (systemInfo?.services) {
    Object.values(systemInfo.services)
      .filter((service) => service?.status && service.status !== "UP")
      .forEach((service) => {
        serviceAlerts.push({
          key: `service-${service.label}`,
          severity: service.status === "DOWN" ? "critical" : "warning",
          title: `${service.label} đang ${service.status === "DOWN" ? "mất kết nối" : "phản hồi bất thường"}`,
          detail: service.detail,
          action: "/admin/health",
        });
      });
  }
  if (sourceStatus.stations === "ready" && hceOfflineCount > 0) {
    serviceAlerts.push({
      key: "hce-offline",
      severity: "warning",
      title: `${hceOfflineCount} trạm quét đang ngoại tuyến`,
      detail: `Có ${hceMaintenanceCount} trạm đang bảo trì và cần rà lại kết nối của các trạm còn lại.`,
      action: "/admin/devices",
    });
  }
  if (sourceStatus.kiosk === "ready" && expiredKioskCount > 0) {
    serviceAlerts.push({
      key: "kiosk-expired",
      severity: "warning",
      title: `${expiredKioskCount} kiosk cần cấp lại mã kích hoạt`,
      detail: "Một số kiosk đang còn cấu hình nhưng mã kích hoạt đã hết hiệu lực hoặc mất kết nối.",
      action: "/admin/kiosk",
    });
  }
  if (chatAttention?.waitingCount > 0) {
    serviceAlerts.push({
      key: "chat-waiting",
      severity: chatAttention.oldestWaitingMinutes >= 10 ? "critical" : "warning",
      title: `${chatAttention.waitingCount} phiên chat đang chờ thủ thư`,
      detail: chatAttention.oldestWaitingMinutes > 0
        ? `Phiên chờ lâu nhất đã đợi ${chatAttention.oldestWaitingMinutes} phút.`
        : "Có sinh viên đang yêu cầu hỗ trợ trực tiếp từ thủ thư.",
      action: null,
    });
  }
  if (priorityTasks.some((task) => task.key === "support-overdue" && task.count > 0)) {
    const overdueTask = priorityTasks.find((task) => task.key === "support-overdue");
    serviceAlerts.push({
      key: "support-overdue",
      severity: "critical",
      title: `${overdueTask.count} yêu cầu hỗ trợ quá ngưỡng theo dõi`,
      detail: "Cần phối hợp thủ thư để nhận xử lý sớm các yêu cầu này.",
      action: null,
    });
  }
  if (sourceStatus.ai === "ready" && aiCapacity?.occupancy_rate >= 85) {
    serviceAlerts.push({
      key: "ai-occupancy",
      severity: "warning",
      title: "AI dự báo thư viện đang rất đông",
      detail: `${aiCapacity.occupied_seats || 0}/${aiCapacity.total_seats || 0} ghế đang được sử dụng, dự báo tiếp tục tăng trong 1 giờ tới.`,
      action: "/admin/ai-config",
    });
  }
  const adminAlerts = serviceAlerts.slice(0, 5);

  // ===== LOADING STATE =====
  if (loading) {
    return (
      <div className="dashboard-page dashboard-loading">
        <div className="dashboard-page-header">
          <div><h1>Bảng điều hành thư viện</h1><p>Đang tải dữ liệu điều hành...</p></div>
        </div>
        <div className="statsRow">
          {[...Array(6)].map((_, i) => <div key={i} className="skeleton skeleton--stat" />)}
        </div>
        <div className="gridMid">
          <div className="skeleton skeleton--panel" />
          <div className="skeleton skeleton--panel" />
        </div>
        <div className="gridBottom">
          <div className="skeleton skeleton--panel" />
          <div className="skeleton skeleton--panel" />
        </div>
      </div>
    );
  }

  // ===== ERROR STATE =====
  if (error && !stats) {
    return (
      <div className="dashboard-page">
        <div className="dashboard-page-header">
          <div><h1>Bảng điều hành thư viện</h1></div>
        </div>
        <div className="panel">
          <LoadErrorState
            title="Không thể tải dữ liệu tổng quan quản trị"
            message={error}
            onRetry={() => fetchOverview()}
          />
        </div>
      </div>
    );
  }

  const maxChartVal = Math.max(...chartData.map(d => Math.max(d.checkInCount || 0, d.bookingCount || 0)), 1);

  return (
    <div className="dashboard-page">
      {/* Page Header */}
      <div className="dashboard-page-header">
        <div>
          <h1>Bảng điều hành thư viện</h1>
          <p>Theo dõi vận hành, tồn đọng xử lý và tình trạng hệ thống của thư viện tại thời điểm hiện tại.</p>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <button
            className="slib-btn slib-btn--ghost"
            onClick={() => fetchOverview(true)}
            disabled={refreshing}
            title="Làm mới"
          >
            {refreshing ? <Loader2 size={16} className="spin" /> : <RefreshCw size={16} />}
          </button>
          <div className="dashboard-server-time">
            <Activity size={16} color="var(--slib-primary)" />
            {stats?.serverTime
              ? new Date(stats.serverTime).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })
              : new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}
          </div>
        </div>
      </div>

      {sourceErrorLabels.length > 0 && (
        <div className="dashboard-source-alert">
          <div className="dashboard-source-alert__icon">
            <AlertTriangle size={18} />
          </div>
          <div className="dashboard-source-alert__body">
            <strong>Một số nguồn dữ liệu chưa phản hồi</strong>
            <span>
              Dashboard chưa lấy được dữ liệu từ {sourceErrorLabels.join(", ")}. Các ô liên quan có thể hiển thị
              `Không rõ` thay vì số thực tế.
            </span>
          </div>
        </div>
      )}

      <div className="dashboard-ops-grid">
        <div className="panel dashboard-alert-panel">
          <div className="panelHeader">
            <div className="panelHeader__left">
              <div className="panelHeader__icon" style={{ background: "#FFF1F2", color: "#D32F2F" }}>
                <Siren size={18} />
              </div>
              <div>
                <h3 className="panelTitle">Trung tâm cảnh báo quản trị</h3>
                <p className="panelSubtitle">Những tín hiệu cần quản trị viên kiểm tra trước trong ca vận hành hiện tại.</p>
              </div>
            </div>
            <span className="panelHeader__badge">{adminAlerts.length}</span>
          </div>
          {adminAlerts.length > 0 ? (
            <div className="dashboard-alert-list">
              {adminAlerts.map((alert) => (
                <button
                  type="button"
                  key={alert.key}
                  className={`dashboard-alert-item dashboard-alert-item--${alert.severity}`}
                  onClick={() => alert.action && navigate(alert.action)}
                  disabled={!alert.action}
                >
                  <div className="dashboard-alert-item__body">
                    <div className="dashboard-alert-item__header">
                      <strong>{alert.title}</strong>
                      <span className={`dashboard-alert-item__badge dashboard-alert-item__badge--${alert.severity}`}>
                        {alert.severity === "critical" ? "Ưu tiên cao" : "Cần theo dõi"}
                      </span>
                    </div>
                    <p>{alert.detail}</p>
                  </div>
                  {alert.action && <ChevronRight size={16} />}
                </button>
              ))}
            </div>
          ) : (
            <div className="empty-state dashboard-empty-lite">
              <Shield size={28} />
              <p>Hiện chưa có cảnh báo quản trị nghiêm trọng nào cần can thiệp.</p>
            </div>
          )}
        </div>

        <div className="panel dashboard-quick-action-panel">
          <div className="panelHeader">
            <div className="panelHeader__left">
              <div className="panelHeader__icon" style={{ background: "#EEF2FF", color: "#4F46E5" }}>
                <ClipboardList size={18} />
              </div>
              <div>
                <h3 className="panelTitle">Lối tắt quản trị</h3>
                <p className="panelSubtitle">Các màn admin cần dùng thường xuyên trong quá trình kiểm tra và cấu hình.</p>
              </div>
            </div>
          </div>
          <div className="dashboard-quick-action-grid">
            {adminQuickActions.map((action) => (
              <button
                key={action.path}
                type="button"
                className={`dashboard-quick-action dashboard-quick-action--${action.tone}`}
                onClick={() => navigate(action.path)}
              >
                <div className="dashboard-quick-action__icon">{action.icon}</div>
                <div className="dashboard-quick-action__body">
                  <strong>{action.title}</strong>
                  <span>{action.desc}</span>
                </div>
                <ChevronRight size={15} />
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* ===== PRIMARY ADMIN CARDS ===== */}
      <div className="statsRow statsRow--four">
        <StatCard
          icon={<Users size={22} />}
          value={stats?.currentlyInLibrary ?? 0}
          label="Sinh viên đang có mặt"
          bg="#F3E8FF" color="#7C3AED"
          trend={stats?.currentlyInLibrary > 0 ? "up" : "neutral"}
          trendValue={`${stats?.totalCheckInsToday ?? 0} lượt vào hôm nay`}
        />
        <StatCard
          icon={<Armchair size={22} />}
          value={`${stats?.occupancyRate ?? 0}%`}
          label="Ghế đang được sử dụng"
          bg="#E8F5E9" color="#388E3C"
          trend={stats?.occupancyRate >= 80 ? "up" : "neutral"}
          trendValue={`${stats?.occupiedSeats ?? 0} đang ngồi, ${stats?.reservedSeats ?? 0} đã giữ chỗ`}
        />
        <StatCard
          icon={<BarChart3 size={22} />}
          value={stats?.totalBookingsToday ?? 0}
          label="Đặt chỗ bắt đầu hôm nay"
          bg="#E3F2FD" color="#0054A6"
          trend="neutral"
          trendValue={`${stats?.activeBookings ?? 0} lượt đang hiệu lực`}
        />
        <StatCard
          icon={<AlertTriangle size={22} />}
          value={adminAttentionCount}
          label="Mục cần theo dõi"
          bg="#FFF1F2" color="#D32F2F"
          trend={adminAttentionCount > 0 ? "down" : "up"}
          trendValue={`${stats?.overdueSupportRequests ?? 0} hỗ trợ quá hạn`}
        />
      </div>

      <div className="dashboard-admin-strip">
        {[
          systemCard,
          hceCard,
          kioskCard,
          {
            icon: <Users size={18} />,
            title: "Tài khoản trong hệ thống",
            value: `${stats?.totalUsers ?? 0}`,
            detail: "Sinh viên, thủ thư và quản trị viên",
            tone: "neutral",
          },
        ].map((item) => (
          <div key={item.title} className={`dashboard-admin-mini dashboard-admin-mini--${item.tone}`}>
            <div className="dashboard-admin-mini__icon">{item.icon}</div>
            <div className="dashboard-admin-mini__body">
              <div className="dashboard-admin-mini__header">
                <span className="dashboard-admin-mini__title">{item.title}</span>
                {item.warning && <span className="dashboard-admin-mini__badge">{item.warning}</span>}
              </div>
              <strong className="dashboard-admin-mini__value">{item.value}</strong>
              <span className="dashboard-admin-mini__detail">{item.detail}</span>
            </div>
          </div>
        ))}
      </div>

      <div className="dashboard-insight-grid">
        <div className="panel dashboard-trend-panel">
          <div className="panelHeader">
            <div className="panelHeader__left">
              <div
                className="panelHeader__icon"
                style={{ background: "#FFF3E8", color: "#EA6A1B" }}
              >
                <TrendingUp size={18} />
              </div>
              <div>
                <h3 className="panelTitle">Xu hướng hôm nay so với hôm qua</h3>
                <p className="panelSubtitle">Biến động lượt vào thư viện, đặt chỗ, vi phạm và yêu cầu hỗ trợ mới.</p>
              </div>
            </div>
          </div>
          <div className="dashboard-trend-grid">
            {[
              {
                label: "Lượt vào",
                today: stats?.trendSummary?.checkInsToday ?? 0,
                yesterday: stats?.trendSummary?.checkInsYesterday ?? 0,
                icon: <Users size={20} />,
                accent: "checkin",
              },
              {
                label: "Đặt chỗ",
                today: stats?.trendSummary?.bookingsToday ?? 0,
                yesterday: stats?.trendSummary?.bookingsYesterday ?? 0,
                icon: <Calendar size={20} />,
                accent: "booking",
              },
              {
                label: "Vi phạm",
                today: stats?.trendSummary?.violationsToday ?? 0,
                yesterday: stats?.trendSummary?.violationsYesterday ?? 0,
                icon: <Shield size={20} />,
                accent: "violation",
              },
              {
                label: "Yêu cầu hỗ trợ",
                today: stats?.trendSummary?.supportToday ?? 0,
                yesterday: stats?.trendSummary?.supportYesterday ?? 0,
                icon: <Headphones size={20} />,
                accent: "support",
              },
            ].map((item) => {
              const delta = formatDelta(item.today, item.yesterday);
              return (
                <div key={item.label} className={`dashboard-trend-item dashboard-trend-item--${item.accent}`}>
                  <div className="dashboard-trend-item__top">
                    <div className={`dashboard-trend-item__icon dashboard-trend-item__icon--${item.accent}`}>
                      {item.icon}
                    </div>
                    <span className={`dashboard-trend-item__badge dashboard-trend-item__badge--${delta.tone}`}>
                      {delta.icon}
                      {delta.badge}
                    </span>
                  </div>
                  <strong className="dashboard-trend-item__value">{item.today}</strong>
                  <span className="dashboard-trend-item__label">{item.label}</span>
                  <span className="dashboard-trend-item__subtext">Hôm qua: {item.yesterday}</span>
                </div>
              );
            })}
          </div>
        </div>

        <div className="panel dashboard-workload-panel">
          <div className="panelHeader">
            <div className="panelHeader__left">
              <div>
                <h3 className="panelTitle">Công việc ưu tiên hôm nay</h3>
                <p className="panelSubtitle">Những đầu việc admin cần theo dõi sát để tránh lan thành sự cố vận hành.</p>
              </div>
            </div>
          </div>
          {displayedPriorityTasks.length > 0 ? (
            <div className="dashboard-priority-list">
              {displayedPriorityTasks.map((task) => (
                <div key={task.key} className={`dashboard-priority-item dashboard-priority-item--${task.severity}`}>
                  <div className="dashboard-priority-item__body">
                    <div className="dashboard-priority-item__header">
                      <strong>{task.title}</strong>
                      <span className={`dashboard-priority-item__count dashboard-priority-item__count--${task.severity}`}>
                        {task.count}
                      </span>
                    </div>
                    <p>{task.description}</p>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state dashboard-empty-lite">
              <Shield size={28} />
              <p>Hiện không có đầu việc ưu tiên nào vượt ngưỡng theo dõi.</p>
            </div>
          )}
        </div>
      </div>

      <div className="dashboard-focus-grid">
        <div className="panel dashboard-chat-panel">
          <div className="panelHeader">
            <div className="panelHeader__left">
              <div className="panelHeader__icon" style={{ background: "#EFF6FF", color: "#2563EB" }}>
                <MessageSquare size={18} />
              </div>
              <div>
                <h3 className="panelTitle">Chat cần phản hồi</h3>
                <p className="panelSubtitle">Theo dõi hàng chờ hỗ trợ giữa sinh viên và thủ thư để tránh bỏ sót.</p>
              </div>
            </div>
            <span className="panelHeader__badge">{(chatAttention?.waitingCount || 0) + (chatAttention?.activeCount || 0)}</span>
          </div>
          <div className="dashboard-chat-summary">
            <div className="dashboard-chat-summary__card">
              <span className="dashboard-chat-summary__label">Đang chờ tiếp nhận</span>
              <strong>{chatAttention?.waitingCount || 0}</strong>
              <span className="dashboard-chat-summary__subtext">
                {chatAttention?.oldestWaitingMinutes
                  ? `Phiên chờ lâu nhất ${chatAttention.oldestWaitingMinutes} phút`
                  : "Chưa có phiên nào vượt ngưỡng"}
              </span>
            </div>
            <div className="dashboard-chat-summary__card">
              <span className="dashboard-chat-summary__label">Đang được thủ thư hỗ trợ</span>
              <strong>{chatAttention?.activeCount || 0}</strong>
              <span className="dashboard-chat-summary__subtext">Các phiên đang trong vòng hỗ trợ trực tiếp</span>
            </div>
          </div>
          <div className="dashboard-chat-preview">
            <div className="dashboard-chat-preview__title">Cập nhật gần nhất</div>
            {chatAttention?.latestStudentName ? (
              <div className="dashboard-chat-preview__body">
                <div className="dashboard-chat-preview__avatar">{getInitials(chatAttention.latestStudentName)}</div>
                <div className="dashboard-chat-preview__content">
                  <strong>{chatAttention.latestStudentName}</strong>
                  <span>{chatAttention.latestStudentCode || "Không có mã số"}</span>
                  <p>{chatAttention.latestMessagePreview || "Có cập nhật mới trong cuộc hội thoại."}</p>
                </div>
                <span className="dashboard-chat-preview__time">{formatRelativeTime(chatAttention.latestMessageAt)}</span>
              </div>
            ) : (
              <div className="empty-state dashboard-empty-lite">
                <MessageSquare size={28} />
                <p>Hiện chưa có phiên chat thủ thư nào đang chờ phản hồi.</p>
              </div>
            )}
          </div>
        </div>

        <div className="panel dashboard-zone-panel">
          <div className="panelHeader">
            <div className="panelHeader__left">
              <div className="panelHeader__icon" style={{ background: "#ECFDF3", color: "#15803D" }}>
                <MapPin size={18} />
              </div>
              <div>
                <h3 className="panelTitle">Khu vực cần chú ý</h3>
                <p className="panelSubtitle">Khu vực đang đông bất thường hoặc phát sinh nhiều báo cáo cần xử lý.</p>
              </div>
            </div>
          </div>
          {displayedAttentionZones.length > 0 ? (
            <div className="dashboard-zone-list">
              {displayedAttentionZones.map((zone) => (
                <div key={`${zone.zoneId}-${zone.zoneName}`} className={`dashboard-zone-item dashboard-zone-item--${zone.severity}`}>
                  <div className="dashboard-zone-item__header">
                    <div>
                      <strong>{zone.zoneName}</strong>
                      <span>{zone.areaName || "Chưa gán khu vực"}</span>
                    </div>
                    <span className={`dashboard-zone-item__badge dashboard-zone-item__badge--${zone.severity}`}>
                      {Math.round(zone.occupancyPercentage)}%
                    </span>
                  </div>
                  <p>{zone.reason}</p>
                  <div className="dashboard-zone-item__meta">
                    <span>{zone.occupiedSeats}/{zone.totalSeats} ghế đang sử dụng</span>
                    <span>{zone.pendingSeatReports} báo cáo ghế</span>
                    <span>{zone.pendingViolations} vi phạm</span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state dashboard-empty-lite">
              <MapPin size={28} />
              <p>Chưa có khu vực nào vượt ngưỡng cảnh báo trong thời điểm hiện tại.</p>
            </div>
          )}
        </div>
      </div>

      <div className="panel dashboard-activity-panel">
        <div className="panelHeader">
          <div className="panelHeader__left">
            <div className="panelHeader__icon" style={{ background: "#FFF7ED", color: "#EA6A1B" }}>
              <Activity size={18} />
            </div>
            <div>
              <h3 className="panelTitle">Lịch hoạt động gần nhất</h3>
              <p className="panelSubtitle">Dòng thời gian các biến động mới nhất để admin nắm nhanh nhịp vận hành toàn thư viện.</p>
            </div>
          </div>
        </div>
        {displayedRecentActivities.length > 0 ? (
          <div className="dashboard-activity-list">
            {displayedRecentActivities.map((activity, index) => (
              <div key={`${activity.type}-${activity.createdAt}-${index}`} className="dashboard-activity-item">
                <div className={`dashboard-activity-item__dot dashboard-activity-item__dot--${activity.severity || "info"}`} />
                <div className="dashboard-activity-item__body">
                  <div className="dashboard-activity-item__header">
                    <strong>{activity.title}</strong>
                    <span>{formatRelativeTime(activity.createdAt)}</span>
                  </div>
                  <p>{activity.description}</p>
                  <div className="dashboard-activity-item__meta">
                    <span>{activity.actorName || "Hệ thống"}</span>
                    {activity.actorCode && <span>{activity.actorCode}</span>}
                    {activity.createdAt && <span>{formatDateTime(activity.createdAt)}</span>}
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-state dashboard-empty-lite">
            <Activity size={28} />
            <p>Chưa có biến động vận hành mới trong dòng thời gian gần nhất.</p>
          </div>
        )}
      </div>

      {/* ===== ROW 1: Recent Bookings + Weekly Chart ===== */}
      <div className="gridMid">
        {/* Recent Bookings Table */}
        <div className="panel" style={{ overflow: "hidden", padding: 0 }}>
          <div className="panelHeader" style={{ padding: "20px 24px" }}>
            <div className="panelHeader__left">
              <div>
                <h3 className="panelTitle">Lịch đặt chỗ trong ngày</h3>
                <p className="panelSubtitle">Các lượt đặt chỗ có giờ bắt đầu trong hôm nay.</p>
              </div>
            </div>
            {(stats?.recentBookings || []).length > 0 && (
              <span className="panelHeader__badge">{stats.recentBookings.length}</span>
            )}
          </div>
          <div style={{ overflowX: "auto" }}>
            <table className="table">
              <thead>
                <tr>
                  <th>Sinh viên</th>
                  <th>Mã số</th>
                  <th>Ghế</th>
                  <th style={{ textAlign: "center" }}>Trạng thái</th>
                  <th style={{ textAlign: "right" }}>Thời gian</th>
                </tr>
              </thead>
              <tbody>
                {displayedRecentBookings.length === 0 ? (
                  <tr><td colSpan={5}>
                    <div className="empty-state">
                      <BookOpen size={32} />
                      <p>Chưa có đặt chỗ nào</p>
                    </div>
                  </td></tr>
                ) : displayedRecentBookings.map((b, i) => (
                  <tr key={b.reservationId || i}>
                    <td>
                      <div className="user-cell">
                        <div className="user-avatar">{getInitials(b.userName)}</div>
                        <span className="user-name">{b.userName}</span>
                      </div>
                    </td>
                    <td><span className="code-cell">{b.userCode}</span></td>
                    <td>
                      <span style={{ fontWeight: 500 }}>{b.seatCode}</span>
                      {b.zoneName && b.zoneName !== "N/A" && (
                        <span style={{ fontSize: 11, color: "var(--slib-text-muted)", marginLeft: 4 }}>
                          ({b.zoneName})
                        </span>
                      )}
                    </td>
                    <td style={{ textAlign: "center" }}>
                      <span className={`badge ${getBadgeClass(b.status)}`}>
                        {translateStatus(b.status)}
                      </span>
                    </td>
                    <td style={{ textAlign: "right" }}>
                      <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end", gap: 2 }}>
                        <span style={{ fontSize: 13, fontWeight: 600, color: "var(--slib-text-primary)" }}>
                          {formatTime(b.startTime)} - {formatTime(b.endTime)}
                        </span>
                        <span style={{ fontSize: 11, color: "var(--slib-text-muted)" }}>
                          {formatDate(b.createdAt)}
                        </span>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Weekly Activity Chart */}
        <div className="panel" style={{ display: "flex", flexDirection: "column" }}>
          <div className="panelHeader">
            <div className="panelHeader__left">
              <div>
                <h3 className="panelTitle">{chartSummary.title}</h3>
                <p className="panelSubtitle">{chartSummary.subtitle}</p>
              </div>
            </div>
            <select
              className="slib-input"
              value={chartRange}
              onChange={(e) => setChartRange(e.target.value)}
              style={{ width: "auto", padding: "6px 12px", fontSize: 12 }}
            >
              <option value="week">Tuần</option>
              <option value="month">Tháng</option>
              <option value="year">Năm</option>
            </select>
          </div>

          {chartData.length > 0 ? (
            <>
              <div className="chart-container">
                {chartData.map((d, i) => (
                  <div key={i} className="chart-bar-group">
                    <div className="chart-bars">
                      <div
                        className="chart-bar chart-bar--checkin"
                        style={{ height: `${Math.max(4, (d.checkInCount / maxChartVal) * 100)}%` }}
                        title={`Lượt vào: ${d.checkInCount}`}
                      />
                      <div
                        className="chart-bar chart-bar--booking"
                        style={{ height: `${Math.max(4, (d.bookingCount / maxChartVal) * 100)}%` }}
                        title={`Đặt chỗ: ${d.bookingCount}`}
                      />
                    </div>
                    <span className="chart-label">{d.label}</span>
                  </div>
                ))}
              </div>
              <div className="chart-legend">
                  <div className="chart-legend__item">
                    <div className="chart-legend__dot" style={{ background: "var(--slib-primary)" }} />
                    Lượt vào
                  </div>
                <div className="chart-legend__item">
                  <div className="chart-legend__dot" style={{ background: "var(--slib-accent-blue)" }} />
                  Đặt chỗ
                </div>
              </div>
            </>
          ) : (
            <div className="empty-state">
              <BarChart3 size={32} />
              <p>Chưa có dữ liệu</p>
            </div>
          )}
        </div>
      </div>

      {/* ===== ROW 2: Area Occupancy + AI Insights ===== */}
      <div className="gridBottom">
        {/* Area Occupancy - uses AI service zone data or fallback to backend areas */}
        <div className="panel">
          <div className="panelHeader">
            <div className="panelHeader__left">
              <div>
                <h3 className="panelTitle">Khu vực đang được sử dụng</h3>
                <p className="panelSubtitle">Tỷ lệ ghế đang có sinh viên ngồi thực tế theo từng khu vực.</p>
              </div>
            </div>
          </div>
          {(() => {
            // Prefer AI service zones, fallback to backend areas
            const zones = aiCapacity?.zones || [];
            const areas = stats?.areaOccupancies || [];
            const dataSource = zones.length > 0 ? zones.map(z => ({
              id: z.zone_id,
              name: z.zone_name,
              occupied: z.occupied_seats,
              total: z.total_seats,
              pct: z.occupancy_rate || 0
            })) : areas.map(a => ({
              id: a.areaId,
              name: a.areaName,
              occupied: a.occupiedSeats,
              total: a.totalSeats,
              pct: a.occupancyPercentage || 0
            }));

            if (dataSource.length === 0) return (
              <div className="empty-state">
                <MapPin size={32} />
                <p>Chưa có dữ liệu khu vực</p>
              </div>
            );

            return dataSource.map((item, i) => {
              const color = getOccupancyColor(item.pct);
              return (
                <div key={item.id || i} className="areaRow">
                  <div className="areaTop">
                    <div className="areaTop__name">
                      <span>{item.name}</span>
                      <span className="areaTop__seats">
                        ({item.occupied}/{item.total} ghế đang sử dụng)
                      </span>
                    </div>
                    <span className="areaTop__pct" style={{ color }}>{item.pct}%</span>
                  </div>
                  <div className="bar">
                    <div className="fill" style={{
                      width: `${item.pct}%`,
                      background: `linear-gradient(90deg, ${color}, ${color}CC)`
                    }} />
                  </div>
                </div>
              );
            });
          })()}
        </div>

        {/* AI Insights - realtime capacity analysis */}
        <div className="panel">
          <div className="panelHeader">
            <div className="panelHeader__left">
              <div>
                <h3 className="panelTitle">Đánh giá nhanh tình trạng chỗ ngồi</h3>
                <p className="panelSubtitle">Tóm tắt mức sử dụng ghế và dự báo ngắn hạn từ dữ liệu hiện tại.</p>
              </div>
            </div>
          </div>
          {sourceStatus.ai === "error" ? (
            <div className="empty-state empty-state--warning">
              <AlertTriangle size={32} />
              <p>Không lấy được dữ liệu AI lúc này</p>
            </div>
          ) : aiCapacity ? (
            <>
              <div className="list-item" style={{
                background: aiCapacity.occupancy_rate >= 70 ? "var(--slib-status-warning-bg)" : "var(--slib-status-info-bg)",
                borderColor: aiCapacity.occupancy_rate >= 70 ? "#FFE0B2" : "#BBDEFB"
              }}>
                <div className="list-item__icon" style={{
                  background: aiCapacity.occupancy_rate >= 70 ? "#FF9800" : "#0054A6"
                }}>
                  {aiCapacity.occupancy_rate >= 70
                    ? <AlertCircle size={16} color="#fff" />
                    : <Activity size={16} color="#fff" />
                  }
                </div>
                <div className="list-item__body">
                  <p className="list-item__name" style={{ marginBottom: 4 }}>{aiCapacity.status}</p>
                  <p className="list-item__desc">{aiCapacity.message}</p>
                </div>
              </div>
              <div className="list-item" style={{
                background: "var(--slib-status-info-bg)",
                borderColor: "#BBDEFB"
              }}>
                <div className="list-item__icon" style={{ background: "#0054A6" }}>
                  <TrendingUp size={16} color="#fff" />
                </div>
                <div className="list-item__body">
                  <p className="list-item__name" style={{ marginBottom: 4 }}>Dự báo 1 giờ tới</p>
                  <p className="list-item__desc">
                    Có {aiCapacity.upcoming_1h || 0} lượt đặt chỗ sắp tới.
                    Hiện tại: {aiCapacity.occupied_seats || 0}/{aiCapacity.total_seats || 0} ghế đang được sử dụng ({aiCapacity.occupancy_rate || 0}%)
                  </p>
                </div>
              </div>
            </>
          ) : (
            <div className="empty-state">
              <Sparkles size={32} />
              <p>Đang tổng hợp dữ liệu hiện tại...</p>
            </div>
          )}
        </div>
      </div>

      {/* ===== ROW 3: Top Students + Violations + Support ===== */}
      <div className="gridTriple">
        {/* Top Students */}
        <div className="panel">
            <div className="panelHeader">
              <div className="panelHeader__left">
                <div>
                  <h3 className="panelTitle">Sinh viên sử dụng nổi bật</h3>
                  <p className="panelSubtitle">{topStudentsSubtitle[topStudentsRange]}</p>
                </div>
              </div>
              <select
                className="slib-input"
                value={topStudentsRange}
                onChange={(e) => setTopStudentsRange(e.target.value)}
                style={{ width: "auto", padding: "6px 12px", fontSize: 12 }}
              >
                {Object.entries(topStudentsRangeLabel).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
            </div>
          {sourceStatus.topStudents === "error" ? (
            <div className="empty-state empty-state--warning">
              <AlertTriangle size={32} />
              <p>Không lấy được dữ liệu sử dụng sinh viên</p>
            </div>
          ) : topStudents.length === 0 ? (
            <div className="empty-state">
              <Award size={32} />
              <p>Chưa có dữ liệu</p>
            </div>
          ) : displayedTopStudents.map((s, i) => {
            const rankClass = i < 3 ? `top-student__rank--${i + 1}` : "top-student__rank--other";
            const avatarClass = i === 0 ? "top-student__avatar--gold" : i === 1 ? "top-student__avatar--accent" : "top-student__avatar--neutral";
            return (
              <div key={s.userId || i} className="top-student">
                <div className={`top-student__rank ${rankClass}`}>{i + 1}</div>
                <div className={`top-student__avatar ${avatarClass}`}>
                  {s.avatarUrl ? (
                    <img
                      src={s.avatarUrl}
                      alt={s.fullName}
                      className="top-student__avatar-image"
                    />
                  ) : (
                    getInitials(s.fullName)
                  )}
                </div>
                <div className="top-student__info">
                  <div className="top-student__name">{s.fullName}</div>
                  <div className="top-student__code">{s.userCode}</div>
                </div>
                <div className="top-student__stats">
                  <div className="top-student__hours">
                    {s.totalMinutes >= 60
                      ? `${Math.floor(s.totalMinutes / 60)}h${s.totalMinutes % 60 > 0 ? s.totalMinutes % 60 + "p" : ""}`
                      : `${s.totalMinutes}p`
                    }
                  </div>
                  <div className="top-student__visits">{s.totalVisits} lần</div>
                </div>
                <ChevronRight size={18} className="top-student__chevron" />
              </div>
            );
          })}
        </div>

        {/* Recent Violations */}
        <div className="panel">
          <div className="panelHeader">
            <div className="panelHeader__left">
              <div>
                <h3 className="panelTitle">Vi phạm cần xử lý</h3>
                <p className="panelSubtitle">{stats?.pendingViolations || 0} cần xử lý</p>
              </div>
            </div>
          </div>
          {displayedRecentViolations.length === 0 ? (
            <div className="empty-state">
              <Shield size={32} />
              <p>Hiện không có vi phạm nào cần được xử lý</p>
            </div>
          ) : displayedRecentViolations.map((v, i) => (
            <div key={v.id || i} className="list-item">
              <div className="list-item__icon" style={{ background: "#FFEBEE", color: "#D32F2F" }}>
                <AlertTriangle size={16} />
              </div>
              <div className="list-item__body">
                <div className="list-item__header">
                  <span className="list-item__name">{v.violatorName}</span>
                  <span className={`badge ${getBadgeClass(v.status)}`}>{translateStatus(v.status)}</span>
                </div>
                <span className="list-item__code">{v.violatorCode}</span>
                <p className="list-item__desc" style={{ marginTop: 4 }}>{translateViolationType(v.violationType)}</p>
                <div className="list-item__time">
                  <Clock size={10} />
                  {formatDateTime(v.createdAt)}
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Recent Support Requests */}
        <div className="panel">
          <div className="panelHeader">
            <div className="panelHeader__left">
              <div>
                <h3 className="panelTitle">Yêu cầu hỗ trợ đang mở</h3>
                <p className="panelSubtitle">{stats?.pendingSupportRequests || 0} đang chờ, {stats?.overdueSupportRequests || 0} quá hạn</p>
              </div>
            </div>
          </div>
          {displayedRecentSupportRequests.length === 0 ? (
            <div className="empty-state">
              <Headphones size={32} />
              <p>Không có yêu cầu</p>
            </div>
          ) : displayedRecentSupportRequests.map((sr, i) => (
            <div key={sr.id || i} className="list-item">
              <div className="list-item__icon" style={{ background: "#E3F2FD", color: "#0054A6" }}>
                <MessageSquare size={16} />
              </div>
              <div className="list-item__body">
                <div className="list-item__header">
                  <span className="list-item__name">{sr.studentName}</span>
                  <span className={`badge ${getBadgeClass(sr.status)}`}>{translateStatus(sr.status)}</span>
                </div>
                <span className="list-item__code">{sr.studentCode}</span>
                <p className="list-item__desc" style={{ marginTop: 4 }}>{sr.description}</p>
                <div className="list-item__time">
                  <Clock size={10} />
                  {formatDateTime(sr.createdAt)}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* ===== ROW 4: Complaints + Feedbacks ===== */}
      <div className="gridBottom">
        {/* Recent Complaints */}
        <div className="panel">
          <div className="panelHeader">
            <div className="panelHeader__left">
              <div>
                <h3 className="panelTitle">Khiếu nại đang chờ</h3>
                <p className="panelSubtitle">{stats?.pendingComplaints || 0} khiếu nại đang chờ phản hồi</p>
              </div>
            </div>
          </div>
          {displayedRecentComplaints.length === 0 ? (
            <div className="empty-state">
              <AlertCircle size={32} />
              <p>Không có khiếu nại</p>
            </div>
          ) : displayedRecentComplaints.map((c, i) => (
            <div key={c.id || i} className="list-item">
              <div className="list-item__icon" style={{ background: "#FFF3E0", color: "#FF9800" }}>
                <AlertCircle size={16} />
              </div>
              <div className="list-item__body">
                <div className="list-item__header">
                  <span className="list-item__name">{c.userName}</span>
                  <span className={`badge ${getBadgeClass(c.status)}`}>{translateStatus(c.status)}</span>
                </div>
                <span className="list-item__code">{c.userCode}</span>
                <p className="list-item__desc" style={{ marginTop: 4 }}>{c.subject}</p>
                <div className="list-item__time">
                  <Clock size={10} />
                  {formatDateTime(c.createdAt)}
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Recent Feedbacks */}
        <div className="panel">
          <div className="panelHeader">
            <div className="panelHeader__left">
              <div>
                <h3 className="panelTitle">Đánh giá mới nhận</h3>
                <p className="panelSubtitle">Những phản hồi mới nhất từ người dùng thư viện.</p>
              </div>
            </div>
          </div>
          {displayedRecentFeedbacks.length === 0 ? (
            <div className="empty-state">
              <Star size={32} />
              <p>Chưa có đánh giá</p>
            </div>
          ) : displayedRecentFeedbacks.map((f, i) => (
            <div key={f.id || i} className="list-item">
              <div className="list-item__icon" style={{ background: "#E8F5E9", color: "#388E3C" }}>
                <Star size={16} />
              </div>
              <div className="list-item__body">
                <div className="list-item__header">
                  <span className="list-item__name">{f.userName}</span>
                  <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
                    {f.rating && [...Array(5)].map((_, s) => (
                      <Star key={s} size={12} fill={s < f.rating ? "#FDB913" : "none"}
                        color={s < f.rating ? "#FDB913" : "#E2E8F0"} />
                    ))}
                  </div>
                </div>
                <span className="list-item__code">{f.userCode}</span>
                <p className="list-item__desc" style={{ marginTop: 4 }}>{f.content}</p>
                <div className="list-item__time">
                  <Clock size={10} />
                  {formatDateTime(f.createdAt)}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* ===== NEWS / NEW BOOKS ===== */}
      {(displayedNews.length > 0 || displayedRecentNewBooks.length > 0) && (
        <>
      {displayedNews.length > 0 && (
        <div className="panel" style={{ marginBottom: 20 }}>
          <div className="panelHeader">
            <div className="panelHeader__left">
              <div>
                <h3 className="panelTitle">Tin đang hiển thị</h3>
                <p className="panelSubtitle">Các bản tin hiện đang phát trong hệ thống.</p>
              </div>
            </div>
            <span className="panelHeader__badge">{news.length} mới</span>
          </div>
          {displayedNews.map((n, i) => (
            <div key={n.id || i} className="list-item">
              <div className="list-item__icon list-item__icon--news">
                {n.imageUrl ? (
                  <img src={n.imageUrl} alt={n.title} className="list-item__cover" />
                ) : (
                  <BookOpen size={16} />
                )}
              </div>
              <div className="list-item__body">
                <p className="list-item__name">{n.title}</p>
                {n.summary && <p className="list-item__desc" style={{ marginTop: 4 }}>{n.summary}</p>}
                <div className="list-item__time">
                  <Calendar size={10} />
                  {formatDateTime(n.createdAt || n.publishDate)}
                </div>
              </div>
              <ChevronRight size={18} color="var(--slib-text-muted)" style={{ flexShrink: 0, alignSelf: "center" }} />
            </div>
          ))}
        </div>
      )}

      <div className="panel" style={{ marginBottom: 20 }}>
        <div className="panelHeader">
          <div className="panelHeader__left">
            <div>
              <h3 className="panelTitle">Sách đang hiển thị</h3>
              <p className="panelSubtitle">Các đầu sách mới đang được hiển thị trong hệ thống.</p>
            </div>
          </div>
          <span className="panelHeader__badge">{recentNewBooks.length}</span>
        </div>
        {displayedRecentNewBooks.length === 0 ? (
          <div className="empty-state">
            <BookOpen size={32} />
            <p>Chưa có sách nào đang hiển thị</p>
          </div>
        ) : displayedRecentNewBooks.map((book, i) => {
          const title = book.title || "Chưa có tiêu đề";
          const author = book.author || book.publisher || "Chưa có thông tin tác giả";
          const cover = book.coverUrl || null;
          return (
            <div key={book.id || i} className="list-item">
              <div className="list-item__icon list-item__icon--book">
                {cover ? (
                  <img src={cover} alt={title} className="list-item__cover" />
                ) : (
                  <BookOpen size={16} />
                )}
              </div>
              <div className="list-item__body">
                <p className="list-item__name">{title}</p>
                <p className="list-item__desc" style={{ marginTop: 4 }}>{author}</p>
                <div className="list-item__time">
                  <Calendar size={10} />
                  {formatDateTime(book.updatedAt || book.createdAt || book.arrivalDate)}
                </div>
              </div>
              <ChevronRight size={18} color="var(--slib-text-muted)" style={{ flexShrink: 0, alignSelf: "center" }} />
            </div>
          );
        })}
      </div>
        </>
      )}
    </div>
  );
};

export default Dashboard;
