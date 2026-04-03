import { useEffect, useState } from 'react';
import {
  Activity,
  AlertCircle,
  Armchair,
  Brain,
  CheckCircle,
  Clock,
  TrendingUp,
  UserX,
  Users,
  XCircle,
} from 'lucide-react';
import {
  getBehaviorSummary,
  getDensityPrediction,
  getSeatRecommendation,
  getUsageStatistics,
} from '../../../services/admin/ai/analyticsService';

const AI_TABS = [
  { id: 'density', label: 'Dự đoán mật độ', icon: TrendingUp },
  { id: 'usage', label: 'Thống kê sử dụng', icon: Users },
  { id: 'behavior', label: 'Hành vi sinh viên', icon: UserX },
  { id: 'seats', label: 'Gợi ý chỗ ngồi', icon: Armchair },
];

function AIAnalyticsPanel({ period = 'week' }) {
  const [activeTab, setActiveTab] = useState('density');
  const [densityData, setDensityData] = useState(null);
  const [usageData, setUsageData] = useState(null);
  const [behaviorData, setBehaviorData] = useState(null);
  const [seatRecommendation, setSeatRecommendation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchData();
  }, [period]);

  const fetchData = async () => {
    setLoading(true);
    setError('');

    const periodDays = period === 'day' ? 1 : period === 'week' ? 7 : 30;
    const results = await Promise.allSettled([
      getDensityPrediction(null, periodDays),
      getUsageStatistics(period),
      getBehaviorSummary(periodDays),
      getSeatRecommendation('summary'),
    ]);

    if (results[0].status === 'fulfilled') {
      setDensityData(results[0].value);
    } else {
      setDensityData(null);
    }

    if (results[1].status === 'fulfilled') {
      setUsageData(results[1].value);
    } else {
      setUsageData(null);
    }

    if (results[2].status === 'fulfilled') {
      setBehaviorData(results[2].value);
    } else {
      setBehaviorData(null);
    }

    if (results[3].status === 'fulfilled') {
      setSeatRecommendation(results[3].value);
    } else {
      setSeatRecommendation(null);
    }

    if (results.every((item) => item.status === 'rejected')) {
      setError('Không thể tải dữ liệu AI ở thời điểm hiện tại.');
    }

    setLoading(false);
  };

  const renderPanelEmpty = (message) => (
    <div className="st-panel-empty st-ai-empty">
      <AlertCircle size={16} />
      <span>{message}</span>
    </div>
  );

  if (loading) {
    return (
      <div className="st-ai-shell">
        <div className="st-ai-header">
          <div className="st-ai-title-wrap">
            <Brain size={20} color="#e8600a" />
            <div>
              <h2 className="st-ai-title">Phân tích bằng AI</h2>
              <p className="st-ai-subtitle">Đang tổng hợp dữ liệu hành vi, mật độ và xu hướng sử dụng thư viện.</p>
            </div>
          </div>
        </div>
        <div className="st-ai-loading">
          <Brain size={20} className="st-ai-loading-icon" />
          <span>Đang phân tích dữ liệu...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="st-ai-shell">
      <div className="st-ai-header">
        <div className="st-ai-title-wrap">
          <Brain size={20} color="#e8600a" />
          <div>
            <h2 className="st-ai-title">Phân tích bằng AI</h2>
            <p className="st-ai-subtitle">Tập trung vào khu vực cao điểm, hành vi sinh viên và gợi ý tối ưu vận hành.</p>
          </div>
        </div>
      </div>

      <div className="st-ai-tabs">
        {AI_TABS.map((tab) => {
          const Icon = tab.icon;
          return (
            <button
              key={tab.id}
              className={`st-ai-tab ${activeTab === tab.id ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              <Icon size={16} />
              {tab.label}
            </button>
          );
        })}
      </div>

      {error && (
        <div className="st-inline-warning">
          <AlertCircle size={16} />
          <span>{error}</span>
        </div>
      )}

      {activeTab === 'density' && (
        <div className="st-ai-section">
          {densityData ? (
            <>
              <div className="st-ai-chip-group">
                <div className="st-ai-chip-card st-ai-chip-card--danger">
                  <div className="st-ai-chip-title">
                    <AlertCircle size={16} />
                    Giờ cao điểm
                  </div>
                  <div className="st-ai-chip-list">
                    {densityData.peak_hours?.length
                      ? densityData.peak_hours.map((hour, index) => (
                          <div key={index} className="st-ai-chip-item">
                            <strong>{hour.label}</strong>
                            <span>{Math.round(hour.avg_occupancy * 100)}%</span>
                          </div>
                        ))
                      : 'Chưa đủ dữ liệu'}
                  </div>
                </div>

                <div className="st-ai-chip-card st-ai-chip-card--success">
                  <div className="st-ai-chip-title">
                    <CheckCircle size={16} />
                    Giờ thưa người
                  </div>
                  <div className="st-ai-chip-list">
                    {densityData.quiet_hours?.length
                      ? densityData.quiet_hours.map((hour, index) => (
                          <div key={index} className="st-ai-chip-item">
                            <strong>{hour.label}</strong>
                            <span>{Math.round(hour.avg_occupancy * 100)}%</span>
                          </div>
                        ))
                      : 'Chưa đủ dữ liệu'}
                  </div>
                </div>
              </div>

              <div className="st-callout info">
                <div className="st-callout-title">Khuyến nghị từ AI</div>
                <div className="st-callout-body">
                  {densityData.recommendation || 'Chưa có khuyến nghị ở thời điểm hiện tại.'}
                </div>
              </div>
            </>
          ) : (
            renderPanelEmpty('Không có dữ liệu dự đoán mật độ.')
          )}
        </div>
      )}

      {activeTab === 'usage' && (
        <div className="st-ai-section">
          {usageData ? (
            <>
              <div className="st-ai-metric-grid">
                <div className="st-ai-metric-card">
                  <span className="st-ai-metric-value">{usageData.total_visits?.toLocaleString() || 0}</span>
                  <span className="st-ai-metric-label">Tổng lượt ra vào</span>
                </div>
                <div className="st-ai-metric-card">
                  <span className="st-ai-metric-value">{usageData.unique_users?.toLocaleString() || 0}</span>
                  <span className="st-ai-metric-label">Số sinh viên</span>
                </div>
                <div className="st-ai-metric-card">
                  <span className="st-ai-metric-value">{usageData.avg_duration_minutes || 0}</span>
                  <span className="st-ai-metric-label">Phút mỗi sinh viên</span>
                </div>
                <div className="st-ai-metric-card">
                  <span className="st-ai-metric-value">{Math.round((usageData.peak_occupancy_rate || 0) * 100)}%</span>
                  <span className="st-ai-metric-label">Mật độ cao điểm</span>
                </div>
              </div>

              <div className="st-ai-block">
                <h3 className="st-ai-block-title">Khu vực được sử dụng nhiều nhất</h3>
                <div className="st-ai-list">
                  {usageData.most_popular_zones?.length ? (
                    usageData.most_popular_zones.map((zone, index) => (
                      <div key={index} className="st-ai-list-row">
                        <span>{zone.zone}</span>
                        <strong>{zone.visits} lượt</strong>
                      </div>
                    ))
                  ) : (
                    renderPanelEmpty('Chưa có dữ liệu khu vực nổi bật')
                  )}
                </div>
              </div>

              <div className="st-ai-block">
                <h3 className="st-ai-block-title">Insight từ AI</h3>
                <div className="st-ai-insight-list">
                  {usageData.insights?.length ? (
                    usageData.insights.map((insight, index) => (
                      <div key={index} className="st-ai-insight-item">{insight}</div>
                    ))
                  ) : (
                    renderPanelEmpty('Chưa có insight cho giai đoạn này')
                  )}
                </div>
              </div>
            </>
          ) : (
            renderPanelEmpty('Không có dữ liệu thống kê sử dụng từ AI.')
          )}
        </div>
      )}

      {activeTab === 'behavior' && (
        <div className="st-ai-section">
          {behaviorData ? (
            <>
              <div className="st-ai-metric-grid">
                <div className="st-ai-metric-card">
                  <span className="st-ai-metric-value">{behaviorData.totalStudents || 0}</span>
                  <span className="st-ai-metric-label">Tổng sinh viên</span>
                </div>
                <div className="st-ai-metric-card">
                  <span className="st-ai-metric-value">{behaviorData.totalBehaviors || 0}</span>
                  <span className="st-ai-metric-label">Tổng hành vi</span>
                </div>
                <div className="st-ai-metric-card danger">
                  <span className="st-ai-metric-value">{behaviorData.totalNoShows || 0}</span>
                  <span className="st-ai-metric-label">Tổng bỏ chỗ</span>
                </div>
                <div className="st-ai-metric-card warning">
                  <span className="st-ai-metric-value">{behaviorData.totalCancellations || 0}</span>
                  <span className="st-ai-metric-label">Tổng hủy</span>
                </div>
              </div>

              <div className="st-ai-rate-grid">
                <div className="st-ai-rate-card">
                  <span className="st-ai-rate-label">Tỷ lệ bỏ chỗ trung bình</span>
                  <strong>{Math.round((behaviorData.avgNoShowRate || 0) * 100)}%</strong>
                </div>
                <div className="st-ai-rate-card warning">
                  <span className="st-ai-rate-label">Tỷ lệ hủy trung bình</span>
                  <strong>{Math.round((behaviorData.avgCancellationRate || 0) * 100)}%</strong>
                </div>
              </div>

              <div className="st-ai-grid-2">
                <div className="st-ai-block">
                  <h3 className="st-ai-block-title">
                    <XCircle size={16} />
                    Sinh viên hay bỏ chỗ
                  </h3>
                  <div className="st-ai-list">
                    {behaviorData.topNoShowStudents?.length ? (
                      behaviorData.topNoShowStudents.slice(0, 5).map((student, index) => (
                        <div key={index} className="st-ai-list-row danger">
                          <span>#{index + 1} {student.fullName || student.userCode || student.userId?.substring(0, 8)}</span>
                          <strong>{student.noShowCount} lần</strong>
                        </div>
                      ))
                    ) : (
                      renderPanelEmpty('Chưa có sinh viên bỏ chỗ')
                    )}
                  </div>
                </div>

                <div className="st-ai-block">
                  <h3 className="st-ai-block-title">
                    <Activity size={16} />
                    Sinh viên tích cực
                  </h3>
                  <div className="st-ai-list">
                    {behaviorData.topActiveStudents?.length ? (
                      behaviorData.topActiveStudents.slice(0, 5).map((student, index) => (
                        <div key={index} className="st-ai-list-row success">
                          <span>#{index + 1} {student.fullName || student.userCode || student.userId?.substring(0, 8)}</span>
                          <strong>{student.behaviorCount} lần</strong>
                        </div>
                      ))
                    ) : (
                      renderPanelEmpty('Chưa có dữ liệu hành vi tích cực')
                    )}
                  </div>
                </div>
              </div>
            </>
          ) : (
            renderPanelEmpty('Không có dữ liệu hành vi sinh viên.')
          )}
        </div>
      )}

      {activeTab === 'seats' && (
        <div className="st-ai-section">
          {seatRecommendation ? (
            <>
              {seatRecommendation.preferences?.user_favorite_zone && (
                <div className="st-ai-preference-card">
                  <div className="st-ai-preference-title">
                    <Clock size={16} />
                    Thói quen đặt chỗ gần đây
                  </div>
                  <div className="st-ai-preference-grid">
                    <div>
                      <span>Khu vực yêu thích</span>
                      <strong>{seatRecommendation.preferences.user_favorite_zone}</strong>
                    </div>
                    <div>
                      <span>Giờ hay đến</span>
                      <strong>
                        {seatRecommendation.preferences.user_favorite_time
                          ? `${seatRecommendation.preferences.user_favorite_time}:00`
                          : '-'}
                      </strong>
                    </div>
                    <div>
                      <span>Tổng đặt chỗ 30 ngày</span>
                      <strong>{seatRecommendation.preferences.total_bookings_30days || 0}</strong>
                    </div>
                  </div>
                </div>
              )}

              <div className="st-ai-block">
                <h3 className="st-ai-block-title">
                  <Armchair size={16} />
                  Gợi ý chỗ ngồi tốt nhất
                </h3>
                {seatRecommendation.recommendations?.length ? (
                  <div className="st-ai-seat-list">
                    {seatRecommendation.recommendations.map((seat, index) => (
                      <div key={index} className={`st-ai-seat-card ${index === 0 ? 'highlight' : ''}`}>
                        <div className="st-ai-seat-rank">{index + 1}</div>
                        <div className="st-ai-seat-main">
                          <div className="st-ai-seat-title">Ghế {seat.seat_code}</div>
                          <div className="st-ai-seat-subtitle">{seat.zone}</div>
                        </div>
                        <div className="st-ai-seat-side">
                          <span className={`st-ai-seat-badge ${seat.availability >= 0.8 ? 'good' : 'warn'}`}>
                            {Math.round(seat.availability * 100)}% trống
                          </span>
                          <span className="st-ai-seat-reason">{seat.reason}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  renderPanelEmpty('Không có gợi ý chỗ ngồi phù hợp')
                )}
              </div>

              <div className="st-inline-summary">
                <strong>Dựa trên:</strong> {seatRecommendation.based_on || 'Lịch sử đặt chỗ và mức độ trống của khu vực'}
              </div>
            </>
          ) : (
            renderPanelEmpty('Không có dữ liệu gợi ý chỗ ngồi.')
          )}
        </div>
      )}
    </div>
  );
}

export default AIAnalyticsPanel;
