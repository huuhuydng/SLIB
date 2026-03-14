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
  AlertTriangle
} from "lucide-react";
import StatCard from "./StatCard";
import dashboardService from "../../../services/librarian/dashboardService";
import { getRealtimeCapacity } from "../../../services/admin/ai/analyticsService";
import "../../../styles/Dashboard.css";

const Dashboard = () => {
  const [stats, setStats] = useState(null);
  const [news, setNews] = useState([]);
  const [chartData, setChartData] = useState([]);
  const [chartRange, setChartRange] = useState("week");
  const [aiCapacity, setAiCapacity] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const fetchAll = useCallback(async (isRefresh = false) => {
    try {
      if (isRefresh) setRefreshing(true);
      else setLoading(true);
      setError(null);

      const [statsData, newsData, chart] = await Promise.all([
        dashboardService.getDashboardStats(),
        dashboardService.getRecentNews(),
        dashboardService.getChartStats(chartRange),
      ]);

      if (statsData) setStats(statsData);
      setNews(newsData || []);
      setChartData(chart || []);

      // AI realtime capacity (includes zone data + AI analysis)
      try {
        const capacityData = await getRealtimeCapacity();
        setAiCapacity(capacityData);
      } catch { setAiCapacity(null); }
    } catch (e) {
      console.error("Dashboard fetch error:", e);
      setError("Không thể tải dữ liệu dashboard");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [chartRange]);

  useEffect(() => {
    fetchAll();
    const interval = setInterval(() => fetchAll(true), 60000);
    return () => clearInterval(interval);
  }, [fetchAll]);

  // Reload chart when range changes
  useEffect(() => {
    dashboardService.getChartStats(chartRange).then(d => setChartData(d || []));
  }, [chartRange]);

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

  // ===== LOADING STATE =====
  if (loading) {
    return (
      <div className="dashboard-page dashboard-loading">
        <div className="dashboard-page-header">
          <div><h1>Tổng quan</h1><p>Đang tải dữ liệu...</p></div>
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
          <div><h1>Tổng quan</h1></div>
        </div>
        <div className="panel" style={{ textAlign: "center", padding: "60px 24px" }}>
          <AlertTriangle size={48} color="#D32F2F" style={{ marginBottom: 16, opacity: 0.6 }} />
          <p style={{ fontSize: 16, color: "#4A5568", marginBottom: 16 }}>{error}</p>
          <button className="slib-btn slib-btn--primary" onClick={() => fetchAll()}>
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
          <h1>Tổng quan</h1>
          <p>Xin chào! Đây là tổng quan hoạt động thư viện hôm nay.</p>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <button
            className="slib-btn slib-btn--ghost"
            onClick={() => fetchAll(true)}
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

      {/* ===== STAT CARDS (6 cards) ===== */}
      <div className="statsRow">
        <StatCard
          icon={<Users size={22} />}
          value={stats?.currentlyInLibrary ?? 0}
          label="Sinh viên trong thư viện"
          bg="#F3E8FF" color="#7C3AED"
          trend={stats?.currentlyInLibrary > 0 ? "up" : "neutral"}
          trendValue={`${stats?.totalCheckInsToday ?? 0} check-in hôm nay`}
        />
        <StatCard
          icon={<Armchair size={22} />}
          value={`${stats?.occupancyRate ?? 0}%`}
          label="Tỷ lệ lấp đầy chỗ ngồi"
          bg="#E8F5E9" color="#388E3C"
          trend={stats?.occupancyRate >= 80 ? "up" : "neutral"}
          trendValue={`${stats?.occupiedSeats ?? 0} / ${stats?.totalSeats ?? 0} ghế`}
        />
        <StatCard
          icon={<BarChart3 size={22} />}
          value={stats?.totalBookingsToday ?? 0}
          label="Đặt chỗ hôm nay"
          bg="#E3F2FD" color="#0054A6"
          trend="neutral"
          trendValue={`${stats?.activeBookings ?? 0} đang hoạt động`}
        />
        <StatCard
          icon={<AlertCircle size={22} />}
          value={stats?.violationsToday ?? 0}
          label="Vi phạm hôm nay"
          bg="#FFEBEE" color="#D32F2F"
          trend={stats?.violationsToday > 0 ? "down" : "neutral"}
          trendValue={`${stats?.pendingViolations ?? 0} chờ xử lý`}
        />
        <StatCard
          icon={<Headphones size={22} />}
          value={stats?.pendingSupportRequests ?? 0}
          label="Yêu cầu hỗ trợ chờ"
          bg="#FFF3E0" color="#E65100"
          trend={stats?.pendingSupportRequests > 0 ? "up" : "neutral"}
          trendValue={`${stats?.inProgressSupportRequests ?? 0} đang xử lý`}
        />
        <StatCard
          icon={<Users size={22} />}
          value={stats?.totalUsers ?? 0}
          label="Tổng người dùng"
          bg="#EDE7F6" color="#5E35B1"
          trend="neutral"
          trendValue="Tất cả hệ thống"
        />
      </div>

      {/* ===== ROW 1: Recent Bookings + Weekly Chart ===== */}
      <div className="gridMid">
        {/* Recent Bookings Table */}
        <div className="panel" style={{ overflow: "hidden", padding: 0 }}>
          <div className="panelHeader" style={{ padding: "20px 24px" }}>
            <div className="panelHeader__left">
              <div>
                <h3 className="panelTitle">Đặt chỗ gần đây</h3>
                <p className="panelSubtitle">Cập nhật theo thời gian thực</p>
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
                <h3 className="panelTitle">Hoạt động</h3>
                <p className="panelSubtitle">Check-in & Đặt chỗ</p>
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
                        title={`Check-in: ${d.checkInCount}`}
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
                  Check-in
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
                <h3 className="panelTitle">Trạng thái khu vực</h3>
                <p className="panelSubtitle">Mức độ lấp đầy theo thời gian thực</p>
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
                <h3 className="panelTitle">AI Phân tích</h3>
                <p className="panelSubtitle">Phân tích công suất thời gian thực</p>
              </div>
            </div>
          </div>
          {aiCapacity ? (
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
                    Hiện tại: {aiCapacity.occupied_seats || 0}/{aiCapacity.total_seats || 0} ghế ({aiCapacity.occupancy_rate || 0}%)
                  </p>
                </div>
              </div>
            </>
          ) : (
            <div className="empty-state">
              <Sparkles size={32} />
              <p>Đang phân tích dữ liệu...</p>
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
                <h3 className="panelTitle">Sinh viên tích cực</h3>
                <p className="panelSubtitle">Top đặt chỗ trong tháng</p>
              </div>
            </div>
          </div>
          {(stats?.topStudents || []).length === 0 ? (
            <div className="empty-state">
              <Award size={32} />
              <p>Chưa có dữ liệu</p>
            </div>
          ) : stats.topStudents.map((s, i) => {
            const rankClass = i < 3 ? `top-student__rank--${i + 1}` : "top-student__rank--other";
            return (
              <div key={s.userId || i} className="top-student">
                <div className={`top-student__rank ${rankClass}`}>{i + 1}</div>
                <div className="top-student__info">
                  <div className="top-student__name">{s.fullName}</div>
                  <div className="top-student__code">{s.userCode}</div>
                </div>
                <div className="top-student__stats">
                  <div className="top-student__visits">{s.totalVisits} lần</div>
                  <div className="top-student__hours">
                    {s.totalMinutes >= 60
                      ? `${Math.floor(s.totalMinutes / 60)}h${s.totalMinutes % 60 > 0 ? s.totalMinutes % 60 + "p" : ""}`
                      : `${s.totalMinutes}p`
                    }
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Recent Violations */}
        <div className="panel">
          <div className="panelHeader">
            <div className="panelHeader__left">
              <div>
                <h3 className="panelTitle">Vi phạm gần đây</h3>
                <p className="panelSubtitle">{stats?.violationsToday || 0} hôm nay</p>
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
                <p className="list-item__desc" style={{ marginTop: 4 }}>{v.violationType}</p>
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
                <h3 className="panelTitle">Yêu cầu hỗ trợ</h3>
                <p className="panelSubtitle">{stats?.pendingSupportRequests || 0} đang chờ</p>
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
                <h3 className="panelTitle">Khiếu nại gần đây</h3>
                <p className="panelSubtitle">Phản hồi từ người dùng</p>
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
                <h3 className="panelTitle">Đánh giá gần đây</h3>
                <p className="panelSubtitle">Phản hồi chất lượng</p>
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

      {/* ===== NEWS (if any) ===== */}
      {news.length > 0 && (
        <div className="panel" style={{ marginBottom: 20 }}>
          <div className="panelHeader">
            <div className="panelHeader__left">
              <div>
                <h3 className="panelTitle">Tin tức mới nhất</h3>
                <p className="panelSubtitle">Cập nhật từ hệ thống</p>
              </div>
            </div>
            <span className="panelHeader__badge">{news.length} mới</span>
          </div>
          {news.map((n, i) => (
            <div key={n.id || i} className="list-item">
              <div className="list-item__icon" style={{ background: "#E3F2FD", color: "#0054A6" }}>
                <BookOpen size={16} />
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
    </div>
  );
};

export default Dashboard;
