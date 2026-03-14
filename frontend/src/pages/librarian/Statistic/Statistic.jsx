import React, { useState, useEffect, useMemo } from 'react';
import {
  BarChart3, TrendingUp, Users, AlertTriangle,
  MessageSquare, Star, Clock, RefreshCw,
  ArrowUpRight, ArrowDownRight, FileWarning,
  MapPin, CalendarCheck
} from 'lucide-react';
import statisticService from '../../../services/statisticService';
import dashboardService from '../../../services/dashboardService';
import AIAnalyticsPanel from './AIAnalyticsPanel';
import '../../../styles/librarian/librarian-shared.css';
import '../../../styles/librarian/Statistic.css';

const RANGE_OPTIONS = [
  { value: 'day', label: 'Hôm nay' },
  { value: 'week', label: 'Tuần này' },
  { value: 'month', label: 'Tháng này' },
  { value: 'year', label: 'Năm nay' },
];

const Statistic = () => {
  const [range, setRange] = useState('week');
  const [data, setData] = useState(null);
  const [chartData, setChartData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [hoveredBar, setHoveredBar] = useState(null);
  const [feedbackPage, setFeedbackPage] = useState(0);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [stats, chart] = await Promise.all([
        statisticService.getStatistics(range),
        dashboardService.getChartStats(range),
      ]);
      setData(stats);
      setChartData(chart || []);
    } catch (e) {
      console.error('Error fetching statistic data:', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    setFeedbackPage(0);
  }, [range]);

  const chartMax = useMemo(() => {
    if (!chartData.length) return 10;
    const maxVal = Math.max(
      ...chartData.map(d => Math.max(d.checkInCount || 0, d.bookingCount || 0)),
      1
    );
    return Math.ceil(maxVal * 1.1);
  }, [chartData]);

  const peakMax = useMemo(() => {
    if (!data?.peakHours?.length) return 1;
    return Math.max(...data.peakHours.map(h => h.count), 1);
  }, [data?.peakHours]);

  const violationMax = useMemo(() => {
    if (!data?.violationsByType?.length) return 1;
    return Math.max(...data.violationsByType.map(v => v.count), 1);
  }, [data?.violationsByType]);

  const overview = data?.overview || {};
  const booking = data?.bookingAnalysis || {};
  const feedback = data?.feedbackSummary || {};
  const zones = data?.zoneUsage || [];
  const violations = data?.violationsByType || [];
  const peakHours = data?.peakHours || [];
  const recentFeedbacks = feedback.recentFeedbacks || [];
  const ratingDist = feedback.ratingDistribution || [];

  const FEEDBACKS_PER_PAGE = 4;
  const visibleFeedbacks = recentFeedbacks.slice(
    feedbackPage * FEEDBACKS_PER_PAGE,
    (feedbackPage + 1) * FEEDBACKS_PER_PAGE
  );
  const totalFeedbackPages = Math.ceil(recentFeedbacks.length / FEEDBACKS_PER_PAGE);

  const formatDateTime = (dt) => {
    if (!dt) return '';
    const d = new Date(dt);
    return d.toLocaleString('vi-VN', {
      hour: '2-digit', minute: '2-digit',
      day: '2-digit', month: '2-digit', year: 'numeric'
    });
  };

  const renderStars = (rating) => {
    return Array.from({ length: 5 }, (_, i) => (
      <span key={i} className={`st-star ${i < rating ? 'filled' : ''}`}>&#9733;</span>
    ));
  };

  if (loading && !data) {
    return (
      <div className="lib-container">
        <div className="lib-loading"><div className="lib-spinner"></div></div>
      </div>
    );
  }

  return (
    <div className="lib-container">
      {/* Header */}
      <div className="st-header">
        <div className="st-header-left">
          <h1 className="st-title">THỐNG KÊ</h1>
          <p className="st-subtitle">Phân tích xu hướng hoạt động thư viện</p>
        </div>
        <div className="st-header-right">
          <div className="st-range-selector">
            {RANGE_OPTIONS.map(opt => (
              <button
                key={opt.value}
                className={`st-range-btn ${range === opt.value ? 'active' : ''}`}
                onClick={() => setRange(opt.value)}
              >
                {opt.label}
              </button>
            ))}
          </div>
          <button className="st-refresh-btn" onClick={fetchData} title="Làm mới dữ liệu">
            <RefreshCw size={15} className={loading ? 'spinning' : ''} />
          </button>
        </div>
      </div>

      {/* Overview Cards */}
      <div className="st-overview-grid">
        <div className="st-overview-card">
          <div className="st-ov-icon st-ov-orange"><Users size={20} /></div>
          <div className="st-ov-info">
            <div className="st-ov-value">{overview.totalCheckIns || 0}</div>
            <div className="st-ov-label">Lượt check-in</div>
          </div>
        </div>
        <div className="st-overview-card">
          <div className="st-ov-icon st-ov-blue"><CalendarCheck size={20} /></div>
          <div className="st-ov-info">
            <div className="st-ov-value">{overview.totalBookings || 0}</div>
            <div className="st-ov-label">Lượt đặt chỗ</div>
          </div>
        </div>
        <div className="st-overview-card">
          <div className="st-ov-icon st-ov-red"><AlertTriangle size={20} /></div>
          <div className="st-ov-info">
            <div className="st-ov-value">{overview.totalViolations || 0}</div>
            <div className="st-ov-label">Vi phạm</div>
          </div>
        </div>
        <div className="st-overview-card">
          <div className="st-ov-icon st-ov-green"><MessageSquare size={20} /></div>
          <div className="st-ov-info">
            <div className="st-ov-value">{overview.totalFeedbacks || 0}</div>
            <div className="st-ov-label">Phản hồi</div>
          </div>
        </div>
      </div>

      {/* Row 1: Chart + Booking Analysis */}
      <div className="st-row-2col">
        {/* Bar Chart */}
        <div className="lib-panel st-chart-panel">
          <div className="st-panel-header">
            <h3 className="lib-panel-title">Lượt vào & Đặt chỗ</h3>
            <div className="st-chart-legend">
              <span className="st-legend-item">
                <i className="st-legend-dot" style={{ background: '#FF751F' }}></i>Check-in
              </span>
              <span className="st-legend-item">
                <i className="st-legend-dot" style={{ background: '#fbbf24' }}></i>Đặt chỗ
              </span>
            </div>
          </div>
          <div className="st-chart-container">
            {chartData.length === 0 ? (
              <div className="st-empty">Chưa có dữ liệu</div>
            ) : (
              <div className="st-bar-chart">
                <div className="st-chart-y">
                  {[...Array(5)].map((_, i) => (
                    <span key={i}>{Math.round(chartMax * (1 - i / 4))}</span>
                  ))}
                </div>
                <div className="st-chart-body">
                  <div className="st-chart-grid-lines">
                    {[...Array(4)].map((_, i) => (
                      <div key={i} className="st-grid-line" style={{ bottom: `${((i + 1) / 4) * 100}%` }} />
                    ))}
                  </div>
                  {chartData.map((item, idx) => (
                    <div key={idx} className="st-bar-group">
                      <div className="st-bar-pair">
                        <div
                          className={`st-bar st-bar-ci ${hoveredBar === `${idx}-ci` ? 'hovered' : ''}`}
                          style={{ height: `${chartMax > 0 ? ((item.checkInCount || 0) / chartMax) * 100 : 0}%` }}
                          onMouseEnter={() => setHoveredBar(`${idx}-ci`)}
                          onMouseLeave={() => setHoveredBar(null)}
                        >
                          {hoveredBar === `${idx}-ci` && (
                            <div className="st-bar-tooltip">{item.checkInCount || 0}</div>
                          )}
                        </div>
                        <div
                          className={`st-bar st-bar-bk ${hoveredBar === `${idx}-bk` ? 'hovered' : ''}`}
                          style={{ height: `${chartMax > 0 ? ((item.bookingCount || 0) / chartMax) * 100 : 0}%` }}
                          onMouseEnter={() => setHoveredBar(`${idx}-bk`)}
                          onMouseLeave={() => setHoveredBar(null)}
                        >
                          {hoveredBar === `${idx}-bk` && (
                            <div className="st-bar-tooltip">{item.bookingCount || 0}</div>
                          )}
                        </div>
                      </div>
                      <span className="st-bar-x-label">{item.label || item.dayOfWeek}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Booking Analysis */}
        <div className="lib-panel">
          <div className="st-panel-header">
            <h3 className="lib-panel-title">Phân tích đặt chỗ</h3>
          </div>
          <div className="st-booking-overview">
            <div className="st-booking-big">{booking.totalBookings || 0}</div>
            <div className="st-booking-big-label">Tổng đặt chỗ</div>
          </div>
          <div className="st-booking-bar">
            <div className="st-booking-seg st-seg-green" style={{ width: `${booking.usedPercent || 0}%` }}></div>
            <div className="st-booking-seg st-seg-yellow" style={{ width: `${booking.cancelledPercent || 0}%` }}></div>
            <div className="st-booking-seg st-seg-gray" style={{ width: `${booking.expiredPercent || 0}%` }}></div>
          </div>
          <div className="st-booking-metrics">
            <div className="st-booking-metric">
              <div className="st-metric-indicator st-ind-green"></div>
              <div className="st-metric-detail">
                <span className="st-metric-val">{booking.usedBookings || 0}</span>
                <span className="st-metric-desc">{booking.usedPercent || 0}% Đã sử dụng</span>
              </div>
            </div>
            <div className="st-booking-metric">
              <div className="st-metric-indicator st-ind-yellow"></div>
              <div className="st-metric-detail">
                <span className="st-metric-val">{booking.cancelledBookings || 0}</span>
                <span className="st-metric-desc">{booking.cancelledPercent || 0}% Đã hủy</span>
              </div>
            </div>
            <div className="st-booking-metric">
              <div className="st-metric-indicator st-ind-gray"></div>
              <div className="st-metric-detail">
                <span className="st-metric-val">{booking.expiredNoShow || 0}</span>
                <span className="st-metric-desc">{booking.expiredPercent || 0}% Không đến</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Row 2: Peak Hours + Violations */}
      <div className="st-row-2col">
        {/* Peak Hours */}
        <div className="lib-panel">
          <div className="st-panel-header">
            <h3 className="lib-panel-title">Giờ cao điểm</h3>
            <Clock size={16} color="var(--lib-muted)" />
          </div>
          <div className="st-peak-chart">
            {peakHours.length === 0 ? (
              <div className="st-empty">Chưa có dữ liệu</div>
            ) : (
              peakHours.map((h, idx) => (
                <div key={idx} className="st-peak-row">
                  <span className="st-peak-label">{h.hour}:00</span>
                  <div className="st-peak-bar-bg">
                    <div
                      className={`st-peak-bar-fill ${h.count === peakMax ? 'peak' : ''}`}
                      style={{ width: `${(h.count / peakMax) * 100}%` }}
                    ></div>
                  </div>
                  <span className="st-peak-count">{h.count}</span>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Violations by Type */}
        <div className="lib-panel">
          <div className="st-panel-header">
            <h3 className="lib-panel-title">Vi phạm theo loại</h3>
            <AlertTriangle size={16} color="var(--lib-red)" />
          </div>
          {violations.length === 0 ? (
            <div className="st-empty">Không có vi phạm trong giai đoạn này</div>
          ) : (
            <div className="st-violation-list">
              {violations.map((v, idx) => (
                <div key={idx} className="st-violation-row">
                  <div className="st-violation-info">
                    <span className="st-violation-label">{v.label}</span>
                    <span className="st-violation-count">{v.count}</span>
                  </div>
                  <div className="st-violation-bar-bg">
                    <div
                      className="st-violation-bar-fill"
                      style={{ width: `${(v.count / violationMax) * 100}%` }}
                    ></div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Row 3: Feedback + Zone Usage */}
      <div className="st-row-2col">
        {/* Feedback Summary */}
        <div className="lib-panel st-feedback-panel">
          <div className="st-panel-header">
            <h3 className="lib-panel-title">Phản hồi sinh viên</h3>
            <Star size={16} color="#fbbf24" />
          </div>

          <div className="st-feedback-top">
            <div className="st-rating-overview">
              <div className="st-rating-big">{feedback.averageRating || 0}</div>
              <div className="st-rating-stars">{renderStars(Math.round(feedback.averageRating || 0))}</div>
              <div className="st-rating-total">{feedback.totalCount || 0} đánh giá</div>
            </div>
            <div className="st-rating-dist">
              {[...ratingDist].reverse().map((r) => (
                <div key={r.rating} className="st-dist-row">
                  <span className="st-dist-label">{r.rating}</span>
                  <span className="st-dist-star">&#9733;</span>
                  <div className="st-dist-bar-bg">
                    <div
                      className="st-dist-bar-fill"
                      style={{ width: `${r.percent}%` }}
                    ></div>
                  </div>
                  <span className="st-dist-count">{r.count}</span>
                </div>
              ))}
            </div>
          </div>

          {recentFeedbacks.length > 0 && (
            <>
              <div className="st-feedback-divider"></div>
              <div className="st-feedback-recent-header">
                <span>Phản hồi gần đây</span>
                {totalFeedbackPages > 1 && (
                  <div className="st-fb-nav">
                    <button
                      disabled={feedbackPage === 0}
                      onClick={() => setFeedbackPage(p => p - 1)}
                      className="st-nav-btn"
                    >&lt;</button>
                    <span className="st-fb-page">{feedbackPage + 1}/{totalFeedbackPages}</span>
                    <button
                      disabled={feedbackPage >= totalFeedbackPages - 1}
                      onClick={() => setFeedbackPage(p => p + 1)}
                      className="st-nav-btn"
                    >&gt;</button>
                  </div>
                )}
              </div>
              <div className="st-feedback-list-new">
                {visibleFeedbacks.map((fb) => (
                  <div key={fb.id} className="st-fb-card">
                    <div className="st-fb-card-top">
                      <div className="st-fb-user">
                        {fb.avatarUrl ? (
                          <img src={fb.avatarUrl} alt={fb.userName} className="st-fb-avatar" />
                        ) : (
                          <div className="lib-avatar-placeholder" style={{ width: 32, height: 32, fontSize: 12 }}>
                            {fb.userName?.charAt(0) || '?'}
                          </div>
                        )}
                        <div>
                          <div className="st-fb-name">{fb.userName}</div>
                          <div className="st-fb-code">{fb.userCode}</div>
                        </div>
                      </div>
                      <div className="st-fb-stars">{renderStars(fb.rating || 0)}</div>
                    </div>
                    <div className="st-fb-content">{fb.content}</div>
                    <div className="st-fb-time">{formatDateTime(fb.createdAt)}</div>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>

        {/* Zone Usage */}
        <div className="lib-panel">
          <div className="st-panel-header">
            <h3 className="lib-panel-title">Khu vực sử dụng nhiều nhất</h3>
            <MapPin size={16} color="var(--lib-muted)" />
          </div>
          {zones.length === 0 ? (
            <div className="st-empty">Chưa có dữ liệu khu vực</div>
          ) : (
            <div className="st-zone-list-new">
              {zones.map((zone, idx) => {
                const pct = zone.usagePercent;
                const color = pct >= 80 ? '#ef4444' : pct >= 50 ? '#fbbf24' : '#22c55e';
                return (
                  <div key={idx} className="st-zone-row">
                    <div className="st-zone-top">
                      <div className="st-zone-info">
                        <span className="st-zone-name-new">{zone.zoneName}</span>
                        <span className="st-zone-area">{zone.areaName}</span>
                      </div>
                      <div className="st-zone-stats">
                        <span className="st-zone-bookings">{zone.totalBookings} đặt chỗ</span>
                        <span className="st-zone-seats">{zone.totalSeats} ghế</span>
                      </div>
                    </div>
                    <div className="st-zone-bar-bg-new">
                      <div
                        className="st-zone-bar-fill-new"
                        style={{ width: `${Math.min(pct, 100)}%`, background: color }}
                      ></div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Row 4: AI Analytics */}
      <div className="lib-panel" style={{ marginBottom: '14px' }}>
        <AIAnalyticsPanel period={range === 'year' ? 'month' : range} />
      </div>
    </div>
  );
};

export default Statistic;