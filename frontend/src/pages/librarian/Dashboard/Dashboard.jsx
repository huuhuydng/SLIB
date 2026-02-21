import React, { useMemo, useState, useEffect } from "react";
import {
  Users, Armchair, AlertCircle, Sparkles, Clock,
  Bell, Calendar, ChevronRight, BookOpen,
  ArrowDownLeft, ArrowUpRight, CalendarCheck,
  MessageSquare, TrendingUp, RefreshCw,
  Star, ShieldAlert, LifeBuoy, Award, UserX,
  ThumbsUp, FileText, Eye, BarChart3,
  MapPin, Layers, X
} from "lucide-react";
import Header from "../../../components/shared/Header";
import { getLibraryInsights } from "../../../services/geminiService.jsx";
import { handleLogout } from "../../../utils/auth";
import librarianService from "../../../services/librarianService";
import dashboardService from "../../../services/dashboardService";
import websocketService from "../../../services/websocketService";

import "../../../styles/librarian/Dashboard.css";

const Dashboard = () => {
  const [searchText, setSearchText] = useState("");
  const [insights, setInsights] = useState([]);
  const [accessLogs, setAccessLogs] = useState([]);
  const [dashStats, setDashStats] = useState(null);
  const [recentNews, setRecentNews] = useState([]);
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [detailModal, setDetailModal] = useState(null);
  const [loading, setLoading] = useState(true);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [activeRequestTab, setActiveRequestTab] = useState("support");
  const [hoveredBar, setHoveredBar] = useState(null);
  const [pendingCounts, setPendingCounts] = useState({
    feedbackNew: 0, complaintPending: 0, supportPending: 0, supportInProgress: 0
  });

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

  const getStatusConfig = (status) => {
    switch (status) {
      case 'CONFIRMED': return { label: 'Đã xác nhận', bg: '#d1fae5', color: '#065f46' };
      case 'BOOKED': return { label: 'Đã đặt', bg: '#fef3c7', color: '#92400e' };
      case 'PROCESSING': return { label: 'Đang xử lý', bg: '#dbeafe', color: '#1e40af' };
      case 'CANCELLED': return { label: 'Đã huỷ', bg: '#fee2e2', color: '#991b1b' };
      case 'CANCEL': return { label: 'Đã huỷ', bg: '#fee2e2', color: '#991b1b' };
      case 'COMPLETED': return { label: 'Hoàn thành', bg: '#d1fae5', color: '#065f46' };
      case 'EXPIRED': return { label: 'Hết hạn', bg: '#f3f4f6', color: '#6b7280' };
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

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      const [stats, news] = await Promise.all([
        dashboardService.getDashboardStats(),
        dashboardService.getRecentNews()
      ]);

      if (stats) {
        setDashStats(stats);
        const data = await getLibraryInsights(stats);
        setInsights(Array.isArray(data) ? data : []);
      }
      setRecentNews(news || []);
      setLastUpdated(new Date());

      // Fetch pending counts from new APIs
      const token = localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');
      const headers = token ? { Authorization: `Bearer ${token}` } : {};
      try {
        const [fbRes, cmpRes, supRes] = await Promise.all([
          fetch(`http://localhost:8080/slib/feedbacks/count`, { headers }).then(r => r.ok ? r.json() : null).catch(() => null),
          fetch(`http://localhost:8080/slib/complaints/count`, { headers }).then(r => r.ok ? r.json() : null).catch(() => null),
          fetch(`http://localhost:8080/slib/support-requests/count`, { headers }).then(r => r.ok ? r.json() : null).catch(() => null),
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
          }
        }));

        // Subscribe dashboard updates (bookings, violations, complaints, feedbacks, support)
        unsubscribers.push(websocketService.subscribe('/topic/dashboard', (message) => {
          console.log('[Dashboard] WebSocket dashboard update:', message.type, message.action);
          fetchDashboardData();
        }));

        // Subscribe news updates
        unsubscribers.push(websocketService.subscribe('/topic/news', () => {
          console.log('[Dashboard] WebSocket news update');
          fetchDashboardData();
        }));
      },
      (error) => {
        console.error('[Dashboard] WebSocket error:', error);
      }
    );
    return () => { unsubscribers.forEach(unsub => { if (unsub) unsub(); }); };
  }, []);

  const filteredStudents = useMemo(() => {
    const q = searchText.trim().toLowerCase();
    if (!q) return accessLogs;
    return accessLogs.filter((log) =>
      (log.userName && log.userName.toLowerCase().includes(q)) ||
      (log.userCode && log.userCode.toLowerCase().includes(q)) ||
      (log.action && log.action.toLowerCase().includes(q))
    );
  }, [searchText, accessLogs]);

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

  // Chart max value for scaling
  const chartMax = useMemo(() => {
    if (!stats.weeklyStats.length) return 10;
    const maxVal = Math.max(
      ...stats.weeklyStats.map(d => Math.max(d.checkInCount || 0, d.bookingCount || 0)),
      1
    );
    return Math.ceil(maxVal * 1.05);
  }, [stats.weeklyStats]);

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

  return (
    <>
      <Header
        searchValue={searchText}
        onSearchChange={(e) => setSearchText(e.target.value)}
        searchPlaceholder="Tìm kiếm sinh viên, mã số..."
        onLogout={handleLogout}
      />

      <div className="dashboard-container">
        {/* Title row */}
        <div className="dashboard-title-row">
          <div>
            <h1 className="dashboard-title">{getGreeting()}</h1>
            <p className="dashboard-subtitle">Tổng quan hoạt động thư viện hôm nay</p>
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

        {/* Stats Cards */}
        <div className="stats-grid">
          <div className="stat-card-wrapper stat-card--purple">
            <div className="stat-card-icon"><Users size={22} /></div>
            <div className="stat-card-body">
              <span className="stat-card-value">{stats.currentlyInLibrary}</span>
              <span className="stat-card-label">Đang trong thư viện</span>
            </div>
            <div className="stat-card-accent"></div>
          </div>

          <div className="stat-card-wrapper stat-card--green">
            <div className="stat-card-icon"><ArrowDownLeft size={22} /></div>
            <div className="stat-card-body">
              <span className="stat-card-value">{stats.totalCheckInsToday}</span>
              <span className="stat-card-label">Check-in hôm nay</span>
            </div>
            <div className="stat-card-accent"></div>
          </div>

          <div className="stat-card-wrapper stat-card--amber">
            <div className="stat-card-icon"><ArrowUpRight size={22} /></div>
            <div className="stat-card-body">
              <span className="stat-card-value">{stats.totalCheckOutsToday}</span>
              <span className="stat-card-label">Check-out hôm nay</span>
            </div>
            <div className="stat-card-accent"></div>
          </div>

          <div className="stat-card-wrapper stat-card--blue">
            <div className="stat-card-icon"><CalendarCheck size={22} /></div>
            <div className="stat-card-body">
              <span className="stat-card-value">{stats.totalBookingsToday}</span>
              <span className="stat-card-label">Đặt chỗ hôm nay</span>
            </div>
            <div className="stat-card-accent"></div>
          </div>

          <div className="stat-card-wrapper stat-card--red">
            <div className="stat-card-icon"><AlertCircle size={22} /></div>
            <div className="stat-card-body">
              <span className="stat-card-value">{stats.violationsToday}</span>
              <span className="stat-card-label">Vi phạm hôm nay</span>
            </div>
            <div className="stat-card-accent"></div>
          </div>

          <div className="stat-card-wrapper stat-card--orange">
            <div className="stat-card-icon"><MessageSquare size={22} /></div>
            <div className="stat-card-body">
              <span className="stat-card-value">{stats.pendingSupportRequests}</span>
              <span className="stat-card-label">Yêu cầu hỗ trợ chờ</span>
            </div>
            <div className="stat-card-accent"></div>
          </div>
        </div>

        {/* Analytics Chart + AI Panel */}
        <div className="dashboard-grid-chart">
          {/* Bar Chart - Weekly Analytics */}
          <section className="dashboard-panel panel-elevated chart-panel">
            <div className="panel-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <BarChart3 size={16} color="#7c3aed" />
                <h3 className="panel-title">Thống kê 7 ngày gần nhất</h3>
              </div>
              <div className="chart-legend">
                <span className="chart-legend-item">
                  <i className="chart-legend-dot" style={{ background: 'linear-gradient(135deg, #7c3aed, #a78bfa)' }}></i>
                  Check-in
                </span>
                <span className="chart-legend-item">
                  <i className="chart-legend-dot" style={{ background: 'linear-gradient(135deg, #f59e0b, #fbbf24)' }}></i>
                  Đặt chỗ
                </span>
              </div>
            </div>

            <div className="bar-chart-container">
              {stats.weeklyStats.length === 0 ? (
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
                    {stats.weeklyStats.map((day, idx) => (
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
                        <span className="bar-label">{day.dayOfWeek}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </section>

          {/* AI Panel */}
          <section className="dashboard-panel ai-panel panel-elevated">
            <div className="panel-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Sparkles size={16} color="#f59e0b" />
                <h3 className="panel-title">AI phân tích</h3>
              </div>
            </div>

            {(insights || []).map((it, idx) => (
              <div key={idx} className={`ai-card ${it.type === "warning" ? "ai-card--warn" : "ai-card--info"}`}>
                <div className="ai-card-icon">
                  {it.type === "warning" ?
                    <AlertCircle size={16} color="#f59e0b" /> :
                    <TrendingUp size={16} color="#3b82f6" />
                  }
                </div>
                <div>
                  <p className="ai-card-title">{it.title}</p>
                  <p className="ai-card-msg">{it.message}</p>
                </div>
              </div>
            ))}

            {/* Quick overview */}
            <div className="quick-overview">
              <h4 className="quick-overview-title">Tổng quan nhanh</h4>
              <div className="quick-stat-row">
                <span className="quick-stat-label">Tổng sinh viên</span>
                <span className="quick-stat-value">{stats.totalUsers}</span>
              </div>
              <div className="quick-stat-row">
                <span className="quick-stat-label">Tổng ghế ngồi</span>
                <span className="quick-stat-value">{stats.totalSeats}</span>
              </div>
              <div className="quick-stat-row">
                <span className="quick-stat-label">Tỷ lệ sử dụng</span>
                <span className="quick-stat-value highlight-value">{stats.occupancyRate}%</span>
              </div>
              <div className="quick-stat-row">
                <span className="quick-stat-label">Đặt chỗ đang hoạt động</span>
                <span className="quick-stat-value">{stats.activeBookings}</span>
              </div>
            </div>
          </section>
        </div>

        {/* Middle section: Access logs table */}
        <div className="dashboard-grid-mid">
          <section className="dashboard-panel panel-elevated">
            <div className="panel-header">
              <h3 className="panel-title">Sinh viên ra vào gần đây</h3>
              <span className="panel-badge">{filteredStudents.length} bản ghi</span>
            </div>
            <div className="table-wrapper">
              <table className="dashboard-table">
                <thead>
                  <tr>
                    <th style={{ width: '25%' }}>Tên sinh viên</th>
                    <th style={{ width: '18%' }}>Mã số</th>
                    <th style={{ width: '17%' }}>Hành động</th>
                    <th style={{ width: '40%' }}>Thời gian</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredStudents.length === 0 ? (
                    <tr>
                      <td colSpan="4" className="empty-state">Không có dữ liệu</td>
                    </tr>
                  ) : (
                    filteredStudents.map((log) => (
                      <tr key={`${log.logId}-${log.action}`}>
                        <td className="cell-name">{log.userName}</td>
                        <td className="cell-code">{log.userCode}</td>
                        <td>
                          <span className={`badge ${log.action === "CHECK_IN" ? "badgeIn" : "badgeOut"}`}>
                            {log.action === 'CHECK_IN' ? 'Vào' : 'Ra'}
                          </span>
                        </td>
                        <td className="cell-time">
                          {log.action === 'CHECK_IN'
                            ? formatDateTime(log.checkInTime)
                            : (log.checkOutTime ? formatDateTime(log.checkOutTime) : '-')
                          }
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </section>

          {/* Recent bookings */}
          <section className="dashboard-panel panel-elevated">
            <div className="panel-header">
              <h3 className="panel-title">Đặt chỗ gần đây</h3>
              <CalendarCheck size={16} color="#6b7280" />
            </div>

            {stats.recentBookings.length === 0 ? (
              <div className="empty-section">Chưa có đặt chỗ nào</div>
            ) : (
              stats.recentBookings.map((booking, idx) => {
                const statusCfg = getStatusConfig(booking.status);
                return (
                  <div key={idx} className="booking-item booking-item-clickable" onClick={() => setDetailModal({ type: 'booking', data: booking })}>
                    <div className="booking-item-top">
                      <div className="booking-user">
                        <span className="booking-name">{booking.userName}</span>
                        <span className="booking-code">{booking.userCode}</span>
                      </div>
                      <span className="status-badge" style={{ background: statusCfg.bg, color: statusCfg.color }}>
                        {statusCfg.label}
                      </span>
                    </div>
                    <div className="booking-detail">
                      <span className="booking-seat">{booking.zoneName} - {booking.seatCode}</span>
                      <span className="booking-time">
                        {formatTime(booking.startTime)} - {formatTime(booking.endTime)}
                      </span>
                    </div>
                  </div>
                );
              })
            )}
          </section>
        </div>

        {/* Top students + Recent violations */}
        <div className="dashboard-grid-two">
          {/* Top 5 students */}
          <section className="dashboard-panel panel-elevated">
            <div className="panel-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Award size={16} color="#f59e0b" />
                <h3 className="panel-title">Top 5 sinh viên xuất sắc</h3>
              </div>
              <span className="panel-badge">30 ngày</span>
            </div>

            {stats.topStudents.length === 0 ? (
              <div className="empty-section">Chưa có dữ liệu</div>
            ) : (
              <div className="top-students-list">
                {stats.topStudents.map((student, idx) => (
                  <div key={idx} className="top-student-item" onClick={() => setSelectedStudent(student)}>
                    <div className={`top-student-rank rank-${idx + 1}`}>
                      {idx + 1}
                    </div>
                    <div className="top-student-info">
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

            {stats.recentViolations.length === 0 ? (
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
            )}
          </section>
        </div>

        {/* Requests Tabs Section */}
        <section className="dashboard-panel panel-elevated requests-panel">
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
        </section>

        {/* Bottom section: News + Zone occupancy */}
        <div className="dashboard-grid-bottom">
          {/* News redesigned */}
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

          {/* Zone occupancy */}
          <section className="dashboard-panel panel-elevated">
            <div className="panel-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Layers size={16} color="#6b7280" />
                <h3 className="panel-title">Trạng thái khu vực</h3>
              </div>
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

      {/* Student Detail Modal */}
      {selectedStudent && (
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
      )}

      {/* Detail Modal */}
      {detailModal && (
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
                  </>
                );
              })()}
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default Dashboard;