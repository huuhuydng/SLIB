import React, { useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle,
  ArrowDownRight,
  ArrowUpRight,
  Brain,
  CalendarCheck,
  Clock,
  Download,
  Info,
  MapPin,
  MessageSquare,
  RefreshCw,
  Sparkles,
  Star,
  TrendingUp,
  TriangleAlert,
  Users,
} from 'lucide-react';
import statisticService from '../../../services/librarian/statisticService';
import dashboardService from '../../../services/librarian/dashboardService';
import AIAnalyticsPanel from './AIAnalyticsPanel';
import '../../../styles/librarian/librarian-shared.css';
import '../../../styles/librarian/Statistic.css';

const RANGE_OPTIONS = [
  { value: 'day', label: 'Hôm nay' },
  { value: 'week', label: 'Tuần này' },
  { value: 'month', label: 'Tháng này' },
  { value: 'year', label: 'Năm nay' },
];

const FEEDBACKS_PER_PAGE = 4;

const EMPTY_COMPARISON = {
  currentValue: 0,
  previousValue: 0,
  changeValue: 0,
  changePercent: 0,
};

const Statistic = () => {
  const [range, setRange] = useState('week');
  const [data, setData] = useState(null);
  const [chartData, setChartData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [hoveredBar, setHoveredBar] = useState(null);
  const [feedbackPage, setFeedbackPage] = useState(0);
  const [exporting, setExporting] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    setError('');

    const [statsResult, chartResult] = await Promise.allSettled([
      statisticService.getStatistics(range),
      dashboardService.getChartStats(range),
    ]);

    if (statsResult.status === 'fulfilled') {
      setData(statsResult.value);
    } else {
      console.error('Error fetching statistic data:', statsResult.reason);
      setError('Không thể tải dữ liệu thống kê. Vui lòng thử lại.');
      setData(null);
    }

    if (chartResult.status === 'fulfilled') {
      setChartData(chartResult.value || []);
    } else {
      console.error('Error fetching chart data:', chartResult.reason);
      setChartData([]);
    }

    setLoading(false);
  };

  const handleExport = async () => {
    try {
      setExporting(true);
      await statisticService.exportStatistics(range);
    } catch (e) {
      console.error('Error exporting statistics:', e);
      setError('Không thể xuất báo cáo thống kê ở thời điểm hiện tại.');
    } finally {
      setExporting(false);
    }
  };

  useEffect(() => {
    fetchData();
    setFeedbackPage(0);
  }, [range]);

  const chartMax = useMemo(() => {
    if (!chartData.length) return 10;
    const maxVal = Math.max(
      ...chartData.map((item) => Math.max(item.checkInCount || 0, item.bookingCount || 0)),
      1
    );
    return Math.ceil(maxVal * 1.1);
  }, [chartData]);

  const peakMax = useMemo(() => {
    if (!data?.peakHours?.length) return 1;
    return Math.max(...data.peakHours.map((item) => item.count), 1);
  }, [data?.peakHours]);

  const violationMax = useMemo(() => {
    if (!data?.violationsByType?.length) return 1;
    return Math.max(...data.violationsByType.map((item) => item.count), 1);
  }, [data?.violationsByType]);

  const overview = data?.overview || {};
  const comparison = data?.comparison || {};
  const booking = data?.bookingAnalysis || {};
  const feedback = data?.feedbackSummary || {};
  const zones = data?.zoneUsage || [];
  const violations = data?.violationsByType || [];
  const peakHours = data?.peakHours || [];
  const insights = data?.insights || [];
  const recentFeedbacks = feedback.recentFeedbacks || [];
  const ratingDist = feedback.ratingDistribution || [];

  const visibleFeedbacks = recentFeedbacks.slice(
    feedbackPage * FEEDBACKS_PER_PAGE,
    (feedbackPage + 1) * FEEDBACKS_PER_PAGE
  );
  const totalFeedbackPages = Math.ceil(recentFeedbacks.length / FEEDBACKS_PER_PAGE);

  const periodLabel = useMemo(() => {
    switch (range) {
      case 'day':
        return 'hôm qua';
      case 'month':
        return '30 ngày trước';
      case 'year':
        return 'năm trước';
      default:
        return 'tuần trước';
    }
  }, [range]);

  const peakInsight = useMemo(
    () => peakHours.reduce((best, item) => (item.count > best.count ? item : best), { hour: 0, count: 0 }),
    [peakHours]
  );

  const topViolation = useMemo(
    () => violations.reduce((best, item) => (item.count > best.count ? item : best), { label: '', count: 0 }),
    [violations]
  );

  const topZone = useMemo(
    () =>
      zones.reduce(
        (best, item) => (item.usagePercent > best.usagePercent ? item : best),
        { zoneName: '', usagePercent: 0, totalBookings: 0 }
      ),
    [zones]
  );

  const overviewCards = [
    {
      key: 'checkIns',
      label: 'Lượt check-in',
      value: overview.totalCheckIns || 0,
      icon: Users,
      tone: 'orange',
      comparison: comparison.checkIns || EMPTY_COMPARISON,
      positiveIsGood: true,
    },
    {
      key: 'bookings',
      label: 'Lượt đặt chỗ',
      value: overview.totalBookings || 0,
      icon: CalendarCheck,
      tone: 'blue',
      comparison: comparison.bookings || EMPTY_COMPARISON,
      positiveIsGood: true,
    },
    {
      key: 'violations',
      label: 'Vi phạm',
      value: overview.totalViolations || 0,
      icon: AlertTriangle,
      tone: 'red',
      comparison: comparison.violations || EMPTY_COMPARISON,
      positiveIsGood: false,
    },
    {
      key: 'feedbacks',
      label: 'Phản hồi',
      value: overview.totalFeedbacks || 0,
      icon: MessageSquare,
      tone: 'green',
      comparison: comparison.feedbacks || EMPTY_COMPARISON,
      positiveIsGood: true,
    },
  ];

  const formatDateTime = (value) => {
    if (!value) return '';
    const date = new Date(value);
    return date.toLocaleString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  };

  const renderStars = (rating) =>
    Array.from({ length: 5 }, (_, index) => (
      <span key={index} className={`st-star ${index < rating ? 'filled' : ''}`}>
        &#9733;
      </span>
    ));

  const renderDelta = (metric, positiveIsGood) => {
    const previousValue = metric?.previousValue ?? 0;
    const changeValue = metric?.changeValue ?? 0;
    const changePercent = metric?.changePercent ?? 0;

    if (previousValue === 0 && (metric?.currentValue ?? 0) > 0) {
      return (
        <span className="st-delta-chip neutral">
          <Sparkles size={13} />
          Mới
        </span>
      );
    }

    if (changeValue === 0) {
      return <span className="st-delta-chip neutral">Không đổi</span>;
    }

    const isUp = changeValue > 0;
    const tone = isUp === positiveIsGood ? 'positive' : 'negative';
    const Icon = isUp ? ArrowUpRight : ArrowDownRight;

    return (
      <span className={`st-delta-chip ${tone}`}>
        <Icon size={13} />
        {Math.abs(changePercent)}%
      </span>
    );
  };

  const renderPanelEmpty = (message) => (
    <div className="st-panel-empty">
      <Info size={16} />
      <span>{message}</span>
    </div>
  );

  if (loading && !data) {
    return (
      <div className="lib-container">
        <div className="st-skeleton-grid">
          <div className="st-skeleton-card"></div>
          <div className="st-skeleton-card"></div>
          <div className="st-skeleton-card"></div>
          <div className="st-skeleton-card"></div>
        </div>
        <div className="st-skeleton-row">
          <div className="st-skeleton-panel"></div>
          <div className="st-skeleton-panel"></div>
        </div>
      </div>
    );
  }

  if (!loading && error && !data) {
    return (
      <div className="lib-container">
        <div className="lib-panel st-page-state">
          <TriangleAlert size={22} />
          <div>
            <h3>Không thể tải dữ liệu thống kê</h3>
            <p>{error}</p>
          </div>
          <button className="st-inline-action" onClick={fetchData}>
            <RefreshCw size={14} />
            Thử lại
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="lib-container">
      <div className="st-header">
        <div className="st-header-left">
          <h1 className="st-title">THỐNG KÊ</h1>
          <p className="st-subtitle">Theo dõi tình hình sử dụng thư viện, đặt chỗ, vi phạm và phản hồi theo từng giai đoạn.</p>
        </div>
        <div className="st-header-right">
          <div className="st-range-selector">
            {RANGE_OPTIONS.map((option) => (
              <button
                key={option.value}
                className={`st-range-btn ${range === option.value ? 'active' : ''}`}
                onClick={() => setRange(option.value)}
              >
                {option.label}
              </button>
            ))}
          </div>
          <button
            className="st-export-btn"
            onClick={handleExport}
            disabled={exporting}
            title="Xuất báo cáo thống kê"
          >
            <Download size={15} className={exporting ? 'spinning' : ''} />
            <span>{exporting ? 'Đang xuất...' : 'Xuất báo cáo'}</span>
          </button>
          <button className="st-refresh-btn" onClick={fetchData} title="Làm mới dữ liệu">
            <RefreshCw size={15} className={loading ? 'spinning' : ''} />
          </button>
        </div>
      </div>

      {!!error && !!data && (
        <div className="st-inline-warning">
          <TriangleAlert size={16} />
          <span>Một số dữ liệu chưa tải được. Một vài biểu đồ có thể chưa hiển thị đầy đủ.</span>
        </div>
      )}

      {/* ── TỔNG QUAN ── */}
      <div className="st-group-card">
        <div className="st-overview-grid">
          {overviewCards.map((card) => {
            const Icon = card.icon;
            return (
              <div key={card.key} className="st-overview-card">
                <div className={`st-ov-icon st-ov-${card.tone}`}>
                  <Icon size={20} />
                </div>
                <div className="st-ov-info">
                  <div className="st-ov-topline">
                    <div className="st-ov-value">{card.value}</div>
                    {renderDelta(card.comparison, card.positiveIsGood)}
                  </div>
                  <div className="st-ov-label">{card.label}</div>
                  <div className="st-ov-meta">So với {periodLabel}: {card.comparison?.previousValue ?? 0}</div>
                </div>
              </div>
            );
          })}
        </div>

        <div className="st-insight-strip">
          {(insights.length ? insights : [
            {
              type: 'fallback',
              title: 'Chưa đủ dữ liệu để rút ra điểm đáng chú ý',
              description: 'Các nhận định tổng quan sẽ xuất hiện khi hệ thống ghi nhận thêm dữ liệu trong giai đoạn này.',
              tone: 'neutral',
            },
          ]).map((insight, index) => (
            <div key={`${insight.type}-${index}`} className={`st-insight-card ${insight.tone || 'neutral'}`}>
              <div className="st-insight-icon">
                <Sparkles size={16} />
              </div>
              <div className="st-insight-content">
                <div className="st-insight-title">{insight.title}</div>
                <div className="st-insight-description">{insight.description}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* ── SỬ DỤNG & ĐẶT CHỖ ── */}
      <div className="st-group-card">
        <div className="st-group-header">
          <span>Sử dụng & đặt chỗ</span>
        </div>
        <div className="st-row-2col st-booking-row">
        <div className="lib-panel st-chart-panel">
          <div className="st-panel-header">
            <div>
              <h3 className="lib-panel-title">Lượt check-in và đặt chỗ</h3>
              <p className="st-panel-caption">So sánh số lượt vào thư viện và đặt chỗ theo từng mốc trong giai đoạn đã chọn.</p>
            </div>
            <div className="st-chart-legend">
              <span className="st-legend-item">
                <i className="st-legend-dot" style={{ background: '#FF751F' }}></i>
                Check-in
              </span>
              <span className="st-legend-item">
                <i className="st-legend-dot" style={{ background: '#fbbf24' }}></i>
                Đặt chỗ
              </span>
            </div>
          </div>
          <div className="st-chart-container">
            {chartData.length === 0 ? (
              renderPanelEmpty('Chưa có dữ liệu cho biểu đồ này trong giai đoạn đã chọn')
            ) : (
              <div className="st-bar-chart">
                <div className="st-chart-y">
                  {[...Array(5)].map((_, index) => (
                    <span key={index}>{Math.round(chartMax * (1 - index / 4))}</span>
                  ))}
                </div>
                <div className="st-chart-body">
                  <div className="st-chart-grid-lines">
                    {[...Array(4)].map((_, index) => (
                      <div
                        key={index}
                        className="st-grid-line"
                        style={{ bottom: `${((index + 1) / 4) * 100}%` }}
                      />
                    ))}
                  </div>
                  {chartData.map((item, index) => (
                    <div key={`${item.label || item.dayOfWeek}-${index}`} className="st-bar-group">
                      <div className="st-bar-pair">
                        <div
                          className={`st-bar st-bar-ci ${hoveredBar === `${index}-ci` ? 'hovered' : ''}`}
                          style={{ height: `${chartMax > 0 ? ((item.checkInCount || 0) / chartMax) * 100 : 0}%` }}
                          onMouseEnter={() => setHoveredBar(`${index}-ci`)}
                          onMouseLeave={() => setHoveredBar(null)}
                        >
                          {hoveredBar === `${index}-ci` && (
                            <div className="st-bar-tooltip">{item.checkInCount || 0}</div>
                          )}
                        </div>
                        <div
                          className={`st-bar st-bar-bk ${hoveredBar === `${index}-bk` ? 'hovered' : ''}`}
                          style={{ height: `${chartMax > 0 ? ((item.bookingCount || 0) / chartMax) * 100 : 0}%` }}
                          onMouseEnter={() => setHoveredBar(`${index}-bk`)}
                          onMouseLeave={() => setHoveredBar(null)}
                        >
                          {hoveredBar === `${index}-bk` && (
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

        <div className="lib-panel st-zone-panel">
          <div className="st-panel-header">
            <div>
              <h3 className="lib-panel-title">Phân tích đặt chỗ</h3>
              <p className="st-panel-caption">Theo dõi số lượt đã sử dụng, đã hủy và không đến để điều chỉnh vận hành.</p>
            </div>
          </div>

          <div className="st-booking-overview">
            <div className="st-booking-big">{booking.totalBookings || 0}</div>
            <div className="st-booking-big-label">Tổng lượt đặt chỗ</div>
          </div>

          <div className="st-booking-bar">
            {(booking.usedPercent || booking.cancelledPercent || booking.expiredPercent) ? (
              <>
                <div className="st-booking-seg st-seg-green" style={{ width: `${booking.usedPercent || 0}%` }}></div>
                <div className="st-booking-seg st-seg-yellow" style={{ width: `${booking.cancelledPercent || 0}%` }}></div>
                <div className="st-booking-seg st-seg-gray" style={{ width: `${booking.expiredPercent || 0}%` }}></div>
              </>
            ) : (
              <div className="st-booking-seg st-seg-empty" style={{ width: '100%' }}></div>
            )}
          </div>

          <div className="st-booking-metrics">
            <div className="st-booking-metric">
              <div className="st-metric-indicator st-ind-green"></div>
              <div className="st-metric-detail">
                <span className="st-metric-val">{booking.usedBookings || 0}</span>
                <span className="st-metric-desc">{booking.usedPercent || 0}% đã sử dụng</span>
              </div>
            </div>
            <div className="st-booking-metric">
              <div className="st-metric-indicator st-ind-yellow"></div>
              <div className="st-metric-detail">
                <span className="st-metric-val">{booking.cancelledBookings || 0}</span>
                <span className="st-metric-desc">{booking.cancelledPercent || 0}% đã hủy</span>
              </div>
            </div>
            <div className="st-booking-metric">
              <div className="st-metric-indicator st-ind-gray"></div>
              <div className="st-metric-detail">
                <span className="st-metric-val">{booking.expiredNoShow || 0}</span>
                <span className="st-metric-desc">{booking.expiredPercent || 0}% không đến</span>
              </div>
            </div>
          </div>

          <div className={`st-callout ${booking.expiredPercent >= 20 ? 'warning' : 'info'}`}>
            <div className="st-callout-title">
              {booking.expiredPercent >= 20 ? 'Cần lưu ý tỷ lệ không đến' : 'Tỷ lệ không đến đang ở mức thấp'}
            </div>
            <div className="st-callout-body">
              {booking.expiredNoShow > 0
                ? `Có ${booking.expiredNoShow} lượt không đến trong giai đoạn này. Nên rà soát lại khung giờ và nhắc nhở sinh viên sớm hơn.`
                : 'Chưa ghi nhận lượt đặt chỗ không đến trong giai đoạn đang xem.'}
            </div>
          </div>
        </div>
        </div>
      </div>
      {/* ── PHÂN TÍCH CHI TIẾT ── */}
      <div className="st-group-card">
        <div className="st-group-header">
          <span>Vi phạm & phản hồi</span>
        </div>
        <div className="st-row-2col st-equal-height-row">
        <div className="lib-panel">
          <div className="st-panel-header">
            <div>
              <h3 className="lib-panel-title">Khung giờ đông nhất</h3>
              <p className="st-panel-caption">Xác định khung giờ đông nhất để chủ động bố trí nhân sự và hỗ trợ.</p>
            </div>
            <Clock size={16} color="var(--lib-muted)" />
          </div>
          <div className="st-peak-chart">
            {peakHours.length === 0
              ? renderPanelEmpty('Chưa có dữ liệu lượt vào thư viện trong giai đoạn này')
              : peakHours.map((item, index) => (
                  <div key={`${item.hour}-${index}`} className="st-peak-row">
                    <span className="st-peak-label">{item.hour}:00</span>
                    <div className="st-peak-bar-bg">
                      <div
                        className={`st-peak-bar-fill ${item.count === peakMax ? 'peak' : ''}`}
                        style={{ width: `${(item.count / peakMax) * 100}%` }}
                      ></div>
                    </div>
                    <span className="st-peak-count">{item.count}</span>
                  </div>
                ))}
          </div>
          <div className="st-inline-summary">
            {peakInsight.count > 0
              ? `Khung ${peakInsight.hour}:00 có nhiều lượt check-in nhất với ${peakInsight.count} lượt.`
              : 'Chưa đủ dữ liệu để xác định khung giờ cao điểm.'}
          </div>
        </div>

        <div className="lib-panel">
          <div className="st-panel-header">
            <div>
              <h3 className="lib-panel-title">Vi phạm theo loại</h3>
              <p className="st-panel-caption">Theo dõi nhóm vi phạm nổi bật để ưu tiên nhắc nhở và xử lý.</p>
            </div>
            <AlertTriangle size={16} color="var(--lib-red)" />
          </div>
          {violations.length === 0 ? (
            renderPanelEmpty('Chưa ghi nhận vi phạm trong giai đoạn này')
          ) : (
            <div className="st-violation-list">
              {violations.map((item, index) => (
                <div key={`${item.violationType}-${index}`} className="st-violation-row">
                  <div className="st-violation-info">
                    <span className="st-violation-label">{item.label}</span>
                    <span className="st-violation-count">{item.count}</span>
                  </div>
                  <div className="st-violation-bar-bg">
                    <div
                      className="st-violation-bar-fill"
                      style={{ width: `${(item.count / violationMax) * 100}%` }}
                    ></div>
                  </div>
                </div>
              ))}
            </div>
          )}
          <div className="st-inline-summary">
            {topViolation.count > 0
              ? `${topViolation.label} đang là loại vi phạm xuất hiện nhiều nhất với ${topViolation.count} trường hợp.`
              : 'Chưa ghi nhận loại vi phạm nổi bật trong giai đoạn này.'}
          </div>
        </div>
        </div>

        <div className="st-group-divider"></div>

        <div className="st-row-2col st-equal-height-row">
        <div className="lib-panel st-feedback-panel">
          <div className="st-panel-header">
            <div>
              <h3 className="lib-panel-title">Phản hồi sinh viên</h3>
              <p className="st-panel-caption">Tổng hợp điểm đánh giá và các phản hồi mới nhất từ sinh viên.</p>
            </div>
            <Star size={16} color="#fbbf24" />
          </div>

          <div className="st-feedback-top">
            <div className="st-rating-overview">
              <div className="st-rating-big">{feedback.averageRating || 0}</div>
              <div className="st-rating-stars">{renderStars(Math.round(feedback.averageRating || 0))}</div>
              <div className="st-rating-total">{feedback.totalCount || 0} đánh giá</div>
            </div>
            <div className="st-rating-dist">
              {[...ratingDist].reverse().map((item) => (
                <div key={item.rating} className="st-dist-row">
                  <span className="st-dist-label">{item.rating}</span>
                  <span className="st-dist-star">&#9733;</span>
                  <div className="st-dist-bar-bg">
                    <div className="st-dist-bar-fill" style={{ width: `${item.percent}%` }}></div>
                  </div>
                  <span className="st-dist-count">{item.count}</span>
                </div>
              ))}
            </div>
          </div>

          {recentFeedbacks.length > 0 ? (
            <>
              <div className="st-feedback-divider"></div>
              <div className="st-feedback-recent-header">
                <span>Phản hồi gần đây</span>
                {totalFeedbackPages > 1 && (
                  <div className="st-fb-nav">
                    <button
                      disabled={feedbackPage === 0}
                      onClick={() => setFeedbackPage((current) => current - 1)}
                      className="st-nav-btn"
                    >
                      &lt;
                    </button>
                    <span className="st-fb-page">{feedbackPage + 1}/{totalFeedbackPages}</span>
                    <button
                      disabled={feedbackPage >= totalFeedbackPages - 1}
                      onClick={() => setFeedbackPage((current) => current + 1)}
                      className="st-nav-btn"
                    >
                      &gt;
                    </button>
                  </div>
                )}
              </div>
              <div className="st-feedback-list-new">
                {visibleFeedbacks.map((item) => (
                  <div key={item.id} className="st-fb-card">
                    <div className="st-fb-card-top">
                      <div className="st-fb-user">
                        {item.avatarUrl ? (
                          <img src={item.avatarUrl} alt={item.userName} className="st-fb-avatar" />
                        ) : (
                          <div className="lib-avatar-placeholder" style={{ width: 32, height: 32, fontSize: 12 }}>
                            {item.userName?.charAt(0) || '?'}
                          </div>
                        )}
                        <div>
                          <div className="st-fb-name">{item.userName}</div>
                          <div className="st-fb-code">{item.userCode}</div>
                        </div>
                      </div>
                      <div className="st-fb-stars">{renderStars(item.rating || 0)}</div>
                    </div>
                    <div className="st-fb-content">{item.content || 'Không có nội dung chi tiết'}</div>
                    <div className="st-fb-time">{formatDateTime(item.createdAt)}</div>
                  </div>
                ))}
              </div>
            </>
          ) : (
            <div className="st-feedback-empty">{renderPanelEmpty('Chưa có phản hồi nào trong giai đoạn này')}</div>
          )}
        </div>

        <div className="lib-panel">
          <div className="st-panel-header">
            <div>
              <h3 className="lib-panel-title">Khu vực có nhiều lượt đặt chỗ nhất</h3>
              <p className="st-panel-caption">Theo dõi tỷ trọng đặt chỗ theo khu vực để nhận diện nơi đang có nhu cầu cao nhất.</p>
            </div>
            <MapPin size={16} color="var(--lib-muted)" />
          </div>
          {zones.length === 0 ? (
            renderPanelEmpty('Chưa có dữ liệu đặt chỗ theo khu vực trong giai đoạn này')
          ) : (
            <div className="st-zone-list-new">
              {zones.map((zone, index) => {
                const usagePercent = zone.usagePercent;
                const color = usagePercent >= 80 ? '#ef4444' : usagePercent >= 50 ? '#fbbf24' : '#22c55e';
                return (
                  <div key={`${zone.zoneId}-${index}`} className="st-zone-row">
                    <div className="st-zone-top">
                      <div className="st-zone-info">
                        <span className="st-zone-rank">#{index + 1}</span>
                        <div>
                          <span className="st-zone-name-new">{zone.zoneName}</span>
                          <span className="st-zone-area">{zone.areaName}</span>
                        </div>
                      </div>
                      <div className="st-zone-stats">
                        <span className="st-zone-bookings">{zone.totalBookings} đặt chỗ</span>
                        <span className="st-zone-seats">{zone.totalSeats} ghế</span>
                      </div>
                    </div>
                    <div className="st-zone-progress">
                      <div className="st-zone-bar-bg-new">
                        <div
                          className="st-zone-bar-fill-new"
                          style={{ width: `${Math.min(usagePercent, 100)}%`, background: color }}
                        ></div>
                      </div>
                      <span className="st-zone-percent">{Math.round(usagePercent)}%</span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
          <div className="st-inline-summary">
            {topZone.usagePercent > 0
              ? `${topZone.zoneName} đang chiếm khoảng ${Math.round(topZone.usagePercent)}% tổng lượt đặt chỗ trong giai đoạn này.`
              : 'Chưa ghi nhận khu vực nổi bật trong giai đoạn này.'}
          </div>
        </div>
      </div>
      </div>

      {/* ── AI PHÂN TÍCH ── */}
      <div className="st-group-card st-group-ai">
        <div className="st-group-header">
          <span>AI phân tích</span>
        </div>
        <div className="st-ai-panel-wrapper">
          <AIAnalyticsPanel period={range} />
        </div>
      </div>
    </div>
  );
};

export default Statistic;
