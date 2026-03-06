import { useState, useEffect } from 'react';
import { Brain, TrendingUp, Users, Clock, AlertCircle, CheckCircle, XCircle, UserX, Activity, Armchair } from 'lucide-react';
import {
  getDensityPrediction,
  getUsageStatistics,
  getBehaviorSummary,
  getSeatRecommendation,
} from '../../../services/admin/ai/analyticsService';

const RANGE_OPTIONS = [
  { value: 'day', label: 'Hôm nay' },
  { value: 'week', label: 'Tuần này' },
  { value: 'month', label: 'Tháng này' },
];

function AIAnalyticsPanel({ period: initialPeriod = 'week', onPeriodChange }) {
  const [activeTab, setActiveTab] = useState('density');
  const [densityData, setDensityData] = useState(null);
  const [usageData, setUsageData] = useState(null);
  const [behaviorData, setBehaviorData] = useState(null);
  const [seatRecommendation, setSeatRecommendation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [period, setPeriod] = useState(initialPeriod);

  useEffect(() => {
    fetchData();
  }, [period]);

  useEffect(() => {
    if (initialPeriod) {
      setPeriod(initialPeriod);
    }
  }, [initialPeriod]);

  const handlePeriodChange = (newPeriod) => {
    setPeriod(newPeriod);
    if (onPeriodChange) {
      onPeriodChange(newPeriod);
    }
  };

  const fetchData = async () => {
    setLoading(true);
    try {
      const periodDays = period === 'day' ? 1 : period === 'week' ? 7 : 30;
      const [density, usage, behavior, seatRec] = await Promise.all([
        getDensityPrediction(),
        getUsageStatistics(period),
        getBehaviorSummary(periodDays),
        getSeatRecommendation(),
      ]);
      setDensityData(density);
      setUsageData(usage);
      setBehaviorData(behavior);
      setSeatRecommendation(seatRec);
    } catch (error) {
      console.error('Error fetching AI analytics:', error);
    } finally {
      setLoading(false);
    }
  };

  const tabs = [
    { id: 'density', label: 'Dự đoán mật độ', icon: TrendingUp },
    { id: 'usage', label: 'Thống kê sử dụng', icon: Users },
    { id: 'behavior', label: 'Hành vi sinh viên', icon: UserX },
    { id: 'seats', label: 'Gợi ý chỗ ngồi', icon: Armchair },
  ];

  if (loading) {
    return (
      <div style={{ padding: '20px', textAlign: 'center', color: '#666' }}>
        <Brain className="animate-spin" size={24} style={{ animation: 'spin 1s linear infinite' }} />
        <p>Đang phân tích dữ liệu...</p>
        <style>{`
          @keyframes spin {
            from { transform: rotate(0deg); }
            to { transform: rotate(360deg); }
          }
        `}</style>
      </div>
    );
  }

  return (
    <div style={{ padding: '20px' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px', marginBottom: '20px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <Brain size={24} color="#e8600a" />
          <h2 style={{ margin: 0, fontSize: '18px', fontWeight: '600' }}>AI Analytics</h2>
        </div>
        <div style={{ display: 'flex', gap: '8px' }}>
          {RANGE_OPTIONS.map(opt => (
            <button
              key={opt.value}
              onClick={() => handlePeriodChange(opt.value)}
              style={{
                padding: '8px 16px',
                background: period === opt.value ? '#e8600a' : '#f1f5f9',
                color: period === opt.value ? '#fff' : '#475569',
                border: 'none',
                borderRadius: '6px',
                cursor: 'pointer',
                fontWeight: '500',
                fontSize: '13px',
              }}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: '8px', marginBottom: '20px' }}>
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '10px 16px',
              background: activeTab === tab.id ? '#e8600a' : '#f1f5f9',
              color: activeTab === tab.id ? '#fff' : '#475569',
              border: 'none',
              borderRadius: '8px',
              cursor: 'pointer',
              fontWeight: '500',
            }}
          >
            <tab.icon size={16} />
            {tab.label}
          </button>
        ))}
      </div>

      {/* Density Prediction Tab */}
      {activeTab === 'density' && densityData && (
        <div>
          {/* Peak Hours */}
          <div style={{ marginBottom: '24px' }}>
            <h3 style={{ fontSize: '15px', fontWeight: '600', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <AlertCircle size={18} color="#dc2626" />
              Giờ cao điểm
            </h3>
            <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
              {densityData.peak_hours?.map((hour, idx) => (
                <div
                  key={idx}
                  style={{
                    padding: '12px 16px',
                    background: '#fef2f2',
                    borderRadius: '8px',
                    border: '1px solid #fecaca',
                    textAlign: 'center',
                  }}
                >
                  <div style={{ fontSize: '16px', fontWeight: '600', color: '#dc2626' }}>{hour.label}</div>
                  <div style={{ fontSize: '12px', color: '#666' }}>{Math.round(hour.occupancy * 100)}%</div>
                </div>
              ))}
            </div>
          </div>

          {/* Quiet Hours */}
          <div style={{ marginBottom: '24px' }}>
            <h3 style={{ fontSize: '15px', fontWeight: '600', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <CheckCircle size={18} color="#16a34a" />
              Giờ thưa người
            </h3>
            <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
              {densityData.quiet_hours?.map((hour, idx) => (
                <div
                  key={idx}
                  style={{
                    padding: '12px 16px',
                    background: '#f0fdf4',
                    borderRadius: '8px',
                    border: '1px solid #bbf7d0',
                    textAlign: 'center',
                  }}
                >
                  <div style={{ fontSize: '16px', fontWeight: '600', color: '#16a34a' }}>{hour.label}</div>
                  <div style={{ fontSize: '12px', color: '#666' }}>{Math.round(hour.occupancy * 100)}%</div>
                </div>
              ))}
            </div>
          </div>

          {/* Recommendation */}
          <div style={{
            padding: '16px',
            background: '#fff7ed',
            borderRadius: '12px',
            border: '1px solid #fed7aa',
          }}>
            <h4 style={{ margin: '0 0 8px 0', fontSize: '14px', fontWeight: '600', color: '#c2410c' }}>
              💡 Khuyến nghị từ AI
            </h4>
            <p style={{ margin: 0, fontSize: '14px', color: '#7c2d12' }}>
              {densityData.recommendation}
            </p>
          </div>
        </div>
      )}

      {/* Usage Statistics Tab */}
      {activeTab === 'usage' && usageData && (
        <div>
          {/* Stats Cards */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: '16px', marginBottom: '24px' }}>
            <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '12px', textAlign: 'center' }}>
              <div style={{ fontSize: '24px', fontWeight: '700', color: '#0f172a' }}>{usageData.total_visits?.toLocaleString()}</div>
              <div style={{ fontSize: '12px', color: '#64748b' }}>Tổng lượt ra vào</div>
            </div>
            <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '12px', textAlign: 'center' }}>
              <div style={{ fontSize: '24px', fontWeight: '700', color: '#0f172a' }}>{usageData.unique_users?.toLocaleString()}</div>
              <div style={{ fontSize: '12px', color: '#64748b' }}>Số sinh viên</div>
            </div>
            <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '12px', textAlign: 'center' }}>
              <div style={{ fontSize: '24px', fontWeight: '700', color: '#0f172a' }}>{usageData.avg_duration_minutes}</div>
              <div style={{ fontSize: '12px', color: '#64748b' }}>Phút/người</div>
            </div>
            <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '12px', textAlign: 'center' }}>
              <div style={{ fontSize: '24px', fontWeight: '700', color: '#0f172a' }}>{Math.round(usageData.peak_occupancy_rate * 100)}%</div>
              <div style={{ fontSize: '12px', color: '#64748b' }}>Cao điểm</div>
            </div>
          </div>

          {/* Most Popular Zones */}
          <div style={{ marginBottom: '24px' }}>
            <h3 style={{ fontSize: '15px', fontWeight: '600', marginBottom: '12px' }}>Khu vực được sử dụng nhiều nhất</h3>
            {usageData.most_popular_zones?.map((zone, idx) => (
              <div
                key={idx}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '12px 16px',
                  background: '#f8fafc',
                  borderRadius: '8px',
                  marginBottom: '8px',
                }}
              >
                <span style={{ fontWeight: '500' }}>{zone.zone}</span>
                <span style={{ fontWeight: '600', color: '#e8600a' }}>{zone.visits} lượt</span>
              </div>
            ))}
          </div>

          {/* Insights */}
          <div>
            <h3 style={{ fontSize: '15px', fontWeight: '600', marginBottom: '12px' }}>Insights từ AI</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {usageData.insights?.map((insight, idx) => (
                <div
                  key={idx}
                  style={{
                    padding: '12px 16px',
                    background: '#eff6ff',
                    borderRadius: '8px',
                    fontSize: '14px',
                    color: '#1e40af',
                  }}
                >
                  {insight}
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Behavior Analytics Tab */}
      {activeTab === 'behavior' && behaviorData && (
        <div>
          {/* Overview Stats */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '16px', marginBottom: '24px' }}>
            <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '12px', textAlign: 'center' }}>
              <div style={{ fontSize: '24px', fontWeight: '700', color: '#0f172a' }}>{behaviorData.totalStudents}</div>
              <div style={{ fontSize: '12px', color: '#64748b' }}>Tổng sinh viên</div>
            </div>
            <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '12px', textAlign: 'center' }}>
              <div style={{ fontSize: '24px', fontWeight: '700', color: '#0f172a' }}>{behaviorData.totalBehaviors}</div>
              <div style={{ fontSize: '12px', color: '#64748b' }}>Tổng hành vi</div>
            </div>
            <div style={{ padding: '16px', background: '#fef2f2', borderRadius: '12px', textAlign: 'center' }}>
              <div style={{ fontSize: '24px', fontWeight: '700', color: '#dc2626' }}>{behaviorData.totalNoShows}</div>
              <div style={{ fontSize: '12px', color: '#64748b' }}>Tổng bỏ chỗ</div>
            </div>
            <div style={{ padding: '16px', background: '#fefce8', borderRadius: '12px', textAlign: 'center' }}>
              <div style={{ fontSize: '24px', fontWeight: '700', color: '#ca8a04' }}>{behaviorData.totalCancellations}</div>
              <div style={{ fontSize: '12px', color: '#64748b' }}>Tổng hủy</div>
            </div>
          </div>

          {/* Average Rates */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '16px', marginBottom: '24px' }}>
            <div style={{ padding: '16px', background: '#f0f9ff', borderRadius: '12px', border: '1px solid #bae6fd' }}>
              <div style={{ fontSize: '12px', color: '#0369a1', marginBottom: '4px' }}>Tỷ lệ bỏ chỗ trung bình</div>
              <div style={{ fontSize: '28px', fontWeight: '700', color: '#0c4a6e' }}>{Math.round((behaviorData.avgNoShowRate || 0) * 100)}%</div>
            </div>
            <div style={{ padding: '16px', background: '#fefce8', borderRadius: '12px', border: '1px solid #fde047' }}>
              <div style={{ fontSize: '12px', color: '#a16207', marginBottom: '4px' }}>Tỷ lệ hủy trung bình</div>
              <div style={{ fontSize: '28px', fontWeight: '700', color: '#713f12' }}>{Math.round((behaviorData.avgCancellationRate || 0) * 100)}%</div>
            </div>
          </div>

          {/* Top No-Show Students */}
          <div style={{ marginBottom: '24px' }}>
            <h3 style={{ fontSize: '15px', fontWeight: '600', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <XCircle size={18} color="#dc2626" />
              Sinh viên hay bỏ chỗ
            </h3>
            {behaviorData.topNoShowStudents?.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {behaviorData.topNoShowStudents.slice(0, 5).map((student, idx) => (
                  <div
                    key={idx}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '12px 16px',
                      background: '#fef2f2',
                      borderRadius: '8px',
                      border: '1px solid #fecaca',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <span style={{ fontWeight: '600', color: '#dc2626' }}>#{idx + 1}</span>
                      <span style={{ fontFamily: 'monospace', fontSize: '13px' }}>
                        {student.userId?.substring(0, 8)}...
                      </span>
                    </div>
                    <span style={{ fontWeight: '600', color: '#dc2626' }}>{student.noShowCount} lần bỏ</span>
                  </div>
                ))}
              </div>
            ) : (
              <div style={{ padding: '20px', textAlign: 'center', color: '#16a34a', background: '#f0fdf4', borderRadius: '8px' }}>
                <CheckCircle size={20} style={{ marginRight: '8px' }} />
                Chưa có sinh viên bỏ chỗ
              </div>
            )}
          </div>

          {/* Top Active Students */}
          <div>
            <h3 style={{ fontSize: '15px', fontWeight: '600', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Activity size={18} color="#16a34a" />
              Sinh viên tích cực
            </h3>
            {behaviorData.topActiveStudents?.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {behaviorData.topActiveStudents.slice(0, 5).map((student, idx) => (
                  <div
                    key={idx}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '12px 16px',
                      background: '#f0fdf4',
                      borderRadius: '8px',
                      border: '1px solid #bbf7d0',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <span style={{ fontWeight: '600', color: '#16a34a' }}>#{idx + 1}</span>
                      <span style={{ fontFamily: 'monospace', fontSize: '13px' }}>
                        {student.userId?.substring(0, 8)}...
                      </span>
                    </div>
                    <span style={{ fontWeight: '600', color: '#16a34a' }}>{student.behaviorCount} hành vi</span>
                  </div>
                ))}
              </div>
            ) : (
              <div style={{ padding: '20px', textAlign: 'center', color: '#64748b', background: '#f8fafc', borderRadius: '8px' }}>
                Chưa có dữ liệu
              </div>
            )}
          </div>
        </div>
      )}

      {/* Seat Recommendation Tab */}
      {activeTab === 'seats' && seatRecommendation && (
        <div>
          {/* User Preferences */}
          {seatRecommendation.preferences?.user_favorite_zone && (
            <div style={{ marginBottom: '24px', padding: '16px', background: '#f0f9ff', borderRadius: '12px', border: '1px solid #bae6fd' }}>
              <h4 style={{ margin: '0 0 12px 0', fontSize: '14px', fontWeight: '600', color: '#0369a1' }}>
                Thói quen của sinh viên
              </h4>
              <div style={{ display: 'flex', gap: '24px', flexWrap: 'wrap' }}>
                <div>
                  <span style={{ fontSize: '12px', color: '#64748b' }}>Khu vực yêu thích:</span>
                  <div style={{ fontWeight: '600', color: '#0c4a6e' }}>{seatRecommendation.preferences.user_favorite_zone}</div>
                </div>
                <div>
                  <span style={{ fontSize: '12px', color: '#64748b' }}>Giờ hay đến:</span>
                  <div style={{ fontWeight: '600', color: '#0c4a6e' }}>{seatRecommendation.preferences.user_favorite_time ? `${seatRecommendation.preferences.user_favorite_time}:00` : '-'}</div>
                </div>
                <div>
                  <span style={{ fontSize: '12px', color: '#64748b' }}>Tổng đặt chỗ (30 ngày):</span>
                  <div style={{ fontWeight: '600', color: '#0c4a6e' }}>{seatRecommendation.preferences.total_bookings_30days}</div>
                </div>
              </div>
            </div>
          )}

          {/* Recommendations */}
          <div>
            <h3 style={{ fontSize: '15px', fontWeight: '600', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Armchair size={18} color="#e8600a" />
              Gợi ý chỗ ngồi tốt nhất
            </h3>
            {seatRecommendation.recommendations?.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {seatRecommendation.recommendations.map((seat, idx) => (
                  <div
                    key={idx}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '16px 20px',
                      background: idx === 0 ? '#fff7ed' : '#f8fafc',
                      borderRadius: '12px',
                      border: idx === 0 ? '2px solid #fdba74' : '1px solid #e2e8f0',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                      <div style={{
                        width: '36px',
                        height: '36px',
                        borderRadius: '50%',
                        background: idx === 0 ? '#f97316' : '#e2e8f0',
                        color: idx === 0 ? '#fff' : '#64748b',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontWeight: '700',
                        fontSize: '14px'
                      }}>
                        {idx + 1}
                      </div>
                      <div>
                        <div style={{ fontWeight: '600', fontSize: '16px', color: '#0f172a' }}>
                          Ghế {seat.seat_code}
                        </div>
                        <div style={{ fontSize: '13px', color: '#64748b' }}>
                          {seat.zone}
                        </div>
                      </div>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <div style={{
                        display: 'inline-block',
                        padding: '4px 12px',
                        background: seat.availability >= 0.8 ? '#dcfce7' : '#fef9c3',
                        color: seat.availability >= 0.8 ? '#166534' : '#854d0e',
                        borderRadius: '20px',
                        fontSize: '12px',
                        fontWeight: '600',
                        marginBottom: '4px'
                      }}>
                        {Math.round(seat.availability * 100)}% trống
                      </div>
                      <div style={{ fontSize: '12px', color: '#64748b', maxWidth: '200px' }}>
                        {seat.reason}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div style={{ padding: '40px', textAlign: 'center', color: '#64748b', background: '#f8fafc', borderRadius: '12px' }}>
                Không có gợi ý nào
              </div>
            )}
          </div>

          {/* Based On */}
          <div style={{ marginTop: '24px', padding: '16px', background: '#f8fafc', borderRadius: '12px', fontSize: '13px', color: '#64748b' }}>
            <strong>Dựa trên:</strong> {seatRecommendation.based_on}
          </div>
        </div>
      )}
    </div>
  );
}

export default AIAnalyticsPanel;
