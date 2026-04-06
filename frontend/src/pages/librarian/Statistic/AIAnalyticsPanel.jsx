import { useEffect, useState } from 'react';
import {
  Activity,
  AlertCircle,
  Armchair,
  Brain,
  CheckCircle,
  TrendingUp,
  UserX,
  Users,
  XCircle,
} from 'lucide-react';
import {
  getBehaviorSummary,
  getDensityPrediction,
  getRealtimeCapacity,
  getUsageStatistics,
} from '../../../services/admin/ai/analyticsService';

const AI_TABS = [
  { id: 'density', label: 'Dự đoán mật độ', icon: TrendingUp },
  { id: 'usage', label: 'Thống kê sử dụng', icon: Users },
  { id: 'behavior', label: 'Hành vi sinh viên', icon: UserX },
  { id: 'capacity', label: 'Công suất tức thời', icon: Armchair },
];

function AIAnalyticsPanel({ period = 'week' }) {
  const [activeTab, setActiveTab] = useState('density');
  const [densityData, setDensityData] = useState(null);
  const [usageData, setUsageData] = useState(null);
  const [behaviorData, setBehaviorData] = useState(null);
  const [capacityData, setCapacityData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchData();
  }, [period]);

  const fetchData = async () => {
    setLoading(true);
    setError('');

    const periodDays =
      period === 'day' ? 1 : period === 'week' ? 7 : period === 'month' ? 30 : 365;
    const results = await Promise.allSettled([
      getDensityPrediction(null, periodDays),
      getUsageStatistics(period),
      getBehaviorSummary(periodDays),
      getRealtimeCapacity(),
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
      setCapacityData(results[3].value);
    } else {
      setCapacityData(null);
    }

    if (results.every((item) => item.status === 'rejected')) {
      setError('Không thể tải dữ liệu phân tích hỗ trợ ở thời điểm hiện tại.');
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
              <h2 className="st-ai-title">Phân tích hỗ trợ bằng AI</h2>
              <p className="st-ai-subtitle">Đang tổng hợp dữ liệu sử dụng thư viện, mật độ và công suất hiện tại.</p>
            </div>
          </div>
        </div>
        <div className="st-ai-loading">
          <Brain size={20} className="st-ai-loading-icon" />
          <span>Đang tổng hợp dữ liệu...</span>
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
            <h2 className="st-ai-title">Phân tích hỗ trợ bằng AI</h2>
            <p className="st-ai-subtitle">Tổng hợp nhận định về mật độ sử dụng, hành vi người dùng và công suất thư viện.</p>
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
                    Khung giờ vắng
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
                <div className="st-callout-title">Gợi ý vận hành</div>
                <div className="st-callout-body">
                  {densityData.recommendation || 'Chưa có gợi ý vận hành ở thời điểm hiện tại.'}
                </div>
              </div>
            </>
          ) : (
            renderPanelEmpty('Chưa có dữ liệu dự báo mật độ trong giai đoạn này.')
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
                  <span className="st-ai-metric-label">Sinh viên sử dụng</span>
                </div>
                <div className="st-ai-metric-card">
                  <span className="st-ai-metric-value">{usageData.avg_duration_minutes || 0}</span>
                  <span className="st-ai-metric-label">Thời gian trung bình / sinh viên</span>
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
                <h3 className="st-ai-block-title">Nhận định từ AI</h3>
                <div className="st-ai-insight-list">
                  {usageData.insights?.length ? (
                    usageData.insights.map((insight, index) => (
                      <div key={index} className="st-ai-insight-item">{insight}</div>
                    ))
                  ) : (
                    renderPanelEmpty('Chưa có nhận định cho giai đoạn này')
                  )}
                </div>
              </div>
            </>
          ) : (
            renderPanelEmpty('Chưa có dữ liệu phân tích sử dụng cho giai đoạn này.')
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
                  <span className="st-ai-metric-label">Tổng lượt hành vi</span>
                </div>
                <div className="st-ai-metric-card danger">
                  <span className="st-ai-metric-value">{behaviorData.totalNoShows || 0}</span>
                  <span className="st-ai-metric-label">Lượt không đến</span>
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
                    Sinh viên không đến nhiều
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
                      renderPanelEmpty('Chưa ghi nhận sinh viên không đến')
                    )}
                  </div>
                </div>

                <div className="st-ai-block">
                  <h3 className="st-ai-block-title">
                    <Activity size={16} />
                    Sinh viên sử dụng nhiều
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
                      renderPanelEmpty('Chưa có dữ liệu nhóm sử dụng nhiều')
                    )}
                  </div>
                </div>
              </div>
            </>
          ) : (
            renderPanelEmpty('Chưa có dữ liệu hành vi sinh viên trong giai đoạn này.')
          )}
        </div>
      )}

      {activeTab === 'capacity' && (
        <div className="st-ai-section">
          {capacityData ? (
            <>
              <div className="st-ai-metric-grid">
                <div className="st-ai-metric-card">
                  <span className="st-ai-metric-value">{capacityData.total_seats || 0}</span>
                  <span className="st-ai-metric-label">Tổng ghế đang hoạt động</span>
                </div>
                <div className="st-ai-metric-card">
                  <span className="st-ai-metric-value">{capacityData.occupied_seats || 0}</span>
                  <span className="st-ai-metric-label">Ghế đang có người ngồi</span>
                </div>
                <div className="st-ai-metric-card">
                  <span className="st-ai-metric-value">{capacityData.occupancy_rate || 0}%</span>
                  <span className="st-ai-metric-label">Tỷ lệ sinh viên đang ngồi tại thời điểm hiện tại</span>
                </div>
                <div className="st-ai-metric-card">
                  <span className="st-ai-metric-value">{capacityData.upcoming_1h || 0}</span>
                  <span className="st-ai-metric-label">Lượt đặt chỗ bắt đầu trong 1 giờ tới</span>
                </div>
              </div>

              <div className={`st-callout ${capacityData.occupancy_rate >= 80 ? 'warning' : 'info'}`}>
                <div className="st-callout-title">{capacityData.status || 'Trạng thái thư viện'}</div>
                <div className="st-callout-body">
                  {capacityData.message || 'Chưa có nhận định công suất ở thời điểm hiện tại.'}
                </div>
              </div>

              <div className="st-ai-block">
                <h3 className="st-ai-block-title">
                  <Armchair size={16} />
                  Tỷ lệ sử dụng ghế theo khu vực
                </h3>
                {capacityData.zones?.length ? (
                  <div className="st-ai-seat-list">
                    {capacityData.zones.map((zone, index) => (
                      <div key={zone.zone_id || index} className={`st-ai-seat-card ${index === 0 ? 'highlight' : ''}`}>
                        <div className="st-ai-seat-rank">{index + 1}</div>
                        <div className="st-ai-seat-main">
                          <div className="st-ai-seat-title">{zone.zone_name}</div>
                          <div className="st-ai-seat-subtitle">
                            {zone.occupied_seats}/{zone.total_seats} ghế đang có người ngồi
                          </div>
                        </div>
                        <div className="st-ai-seat-side">
                          <span className={`st-ai-seat-badge ${zone.occupancy_rate >= 80 ? 'warn' : 'good'}`}>
                            {zone.occupancy_rate}% lấp đầy
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  renderPanelEmpty('Chưa có dữ liệu công suất theo khu vực')
                )}
              </div>
            </>
          ) : (
            renderPanelEmpty('Chưa có dữ liệu công suất hiện tại.')
          )}
        </div>
      )}
    </div>
  );
}

export default AIAnalyticsPanel;
