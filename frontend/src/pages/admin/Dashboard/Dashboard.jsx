import React, { useEffect, useState, useCallback } from "react";
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
  ArrowDownRight
} from "lucide-react";
import StatCard from "./StatCard";
import dashboardService from "../../../services/admin/dashboardService";
import { getRealtimeCapacity } from "../../../services/admin/ai/analyticsService";
import systemHealthService from "../../../services/admin/systemHealthService";
import hceStationService from "../../../services/admin/hceStationService";
import "../../../styles/Dashboard.css";

const Dashboard = () => {
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
    week: "Những sinh viên có thời lượng sử dụng cao trong 7 ngày gần đây.",
    month: "Những sinh viên có thời lượng sử dụng cao trong 30 ngày gần đây.",
    year: "Những sinh viên có thời lượng sử dụng cao trong 12 tháng gần đây.",
  };

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
        <div className="panel" style={{ textAlign: "center", padding: "60px 24px" }}>
          <AlertTriangle size={48} color="#D32F2F" style={{ marginBottom: 16, opacity: 0.6 }} />
          <p style={{ fontSize: 16, color: "#4A5568", marginBottom: 16 }}>{error}</p>
          <button className="slib-btn slib-btn--primary" onClick={() => fetchOverview()}>
            <RefreshCw size={16} /> Thử lại
          </button>
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
          label="Ghế đang có người sử dụng"
          bg="#E8F5E9" color="#388E3C"
          trend={stats?.occupancyRate >= 80 ? "up" : "neutral"}
          trendValue={`${stats?.occupiedSeats ?? 0} / ${stats?.totalSeats ?? 0} ghế đang có người`}
        />
        <StatCard
          icon={<BarChart3 size={22} />}
          value={stats?.totalBookingsToday ?? 0}
          label="Đặt chỗ bắt đầu hôm nay"
          bg="#E3F2FD" color="#0054A6"
          trend="neutral"
          trendValue={`${stats?.activeBookings ?? 0} lượt đang diễn ra`}
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
                <h3 className="panelTitle">Việc đang chờ xử lý</h3>
                <p className="panelSubtitle">Các vi phạm, hỗ trợ, khiếu nại và báo cáo ghế đang chờ phản hồi hoặc xử lý.</p>
              </div>
            </div>
          </div>
          <div className="dashboard-workload-list">
            {[
              { label: "Vi phạm cần xử lý", value: stats?.pendingViolations ?? 0, tone: "danger" },
              { label: "Yêu cầu hỗ trợ đang chờ", value: stats?.pendingSupportRequests ?? 0, tone: "warning" },
              { label: "Yêu cầu hỗ trợ đang xử lý", value: stats?.inProgressSupportRequests ?? 0, tone: "warning" },
              { label: "Khiếu nại đang chờ", value: stats?.pendingComplaints ?? 0, tone: "warning" },
              { label: "Báo cáo ghế đang chờ", value: stats?.pendingSeatStatusReports ?? 0, tone: "warning" },
              { label: "Hỗ trợ quá hạn", value: stats?.overdueSupportRequests ?? 0, tone: "danger" },
            ].map((item) => (
              <div key={item.label} className="dashboard-workload-item">
                <span className="dashboard-workload-item__label">{item.label}</span>
                <span className={`dashboard-workload-item__value dashboard-workload-item__value--${item.tone}`}>
                  {item.value}
                </span>
              </div>
            ))}
          </div>
        </div>
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
            {stats?.recentBookings?.length > 0 && (
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
                {(stats?.recentBookings || []).length === 0 ? (
                  <tr><td colSpan={5}>
                    <div className="empty-state">
                      <BookOpen size={32} />
                      <p>Chưa có đặt chỗ nào</p>
                    </div>
                  </td></tr>
                ) : stats.recentBookings.map((b, i) => (
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
                <h3 className="panelTitle">Khu vực đang có người sử dụng</h3>
                <p className="panelSubtitle">Tỷ lệ ghế có đặt chỗ còn hiệu lực theo từng khu vực.</p>
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
                        ({item.occupied}/{item.total} ghế)
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
                    Hiện tại: {aiCapacity.occupied_seats || 0}/{aiCapacity.total_seats || 0} ghế đang có người sử dụng ({aiCapacity.occupancy_rate || 0}%)
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
          ) : topStudents.map((s, i) => {
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
          {(stats?.recentViolations || []).length === 0 ? (
            <div className="empty-state">
              <Shield size={32} />
              <p>Không có vi phạm</p>
            </div>
          ) : stats.recentViolations.map((v, i) => (
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
          {(stats?.recentSupportRequests || []).length === 0 ? (
            <div className="empty-state">
              <Headphones size={32} />
              <p>Không có yêu cầu</p>
            </div>
          ) : stats.recentSupportRequests.map((sr, i) => (
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
          {(stats?.recentComplaints || []).length === 0 ? (
            <div className="empty-state">
              <AlertCircle size={32} />
              <p>Không có khiếu nại</p>
            </div>
          ) : stats.recentComplaints.map((c, i) => (
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
          {(stats?.recentFeedbacks || []).length === 0 ? (
            <div className="empty-state">
              <Star size={32} />
              <p>Chưa có đánh giá</p>
            </div>
          ) : stats.recentFeedbacks.map((f, i) => (
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
      {(news.length > 0 || recentNewBooks.length > 0) && (
        <>
      {news.length > 0 && (
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
          {news.map((n, i) => (
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
        {recentNewBooks.length === 0 ? (
          <div className="empty-state">
            <BookOpen size={32} />
            <p>Chưa có sách nào đang hiển thị</p>
          </div>
        ) : recentNewBooks.map((book, i) => {
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
