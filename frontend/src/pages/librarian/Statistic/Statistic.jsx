import React, { useState } from 'react';
import '../../../styles/librarian/librarian-shared.css';
import '../../../styles/librarian/Statistic.css';

// Mock Data
const CHART_DATA = [
  { day: 'T2', value: 35 },
  { day: 'T3', value: 28 },
  { day: 'T4', value: 50 },
  { day: 'T5', value: 35 },
  { day: 'T6', value: 80 },
  { day: 'T7', value: 35 },
  { day: 'CN', value: 50 },
];

const ZONE_USAGE = [
  { name: 'Khu yên tĩnh', percent: 95, color: '#EF4444' },
  { name: 'Khu thảo luận', percent: 45, color: '#10B981' },
  { name: 'Khu tự học', percent: 70, color: '#FACC15' },
];

const FEEDBACKS = [
  { id: 1, date: '10:35 - 15/12/2023', content: 'Thư viện rất yên tĩnh, phù hợp để học bài', user: 'PhucNH', code: 'DE170706' },
  { id: 2, date: '10:35 - 15/12/2023', content: 'Wifi hơi chậm ở tầng 2, cần cải thiện', user: 'PhucNH', code: 'DE170706' },
  { id: 3, date: '09:00 - 14/12/2023', content: 'Wifi hôm nay hơi lag ở khu B', user: 'Minh Anh', code: 'DE182201' },
  { id: 4, date: '14:20 - 13/12/2023', content: 'Cần thêm ổ cắm điện', user: 'Hoàng Long', code: 'DE170999' }
];

const Statistic = () => {
  const [feedbackIndex, setFeedbackIndex] = useState(0);

  const handlePrevFeedback = () => setFeedbackIndex((prev) => Math.max(0, prev - 1));
  const handleNextFeedback = () => setFeedbackIndex((prev) => Math.min(FEEDBACKS.length - 2, prev + 1));

  const visibleFeedbacks = FEEDBACKS.slice(feedbackIndex, feedbackIndex + 2);
  const maxChartValue = Math.max(...CHART_DATA.map(d => d.value));

  return (
    <div className="lib-container">
      <div className="lib-page-title">
        <h1>Thống kê</h1>
      </div>

      <div className="st-grid">
        {/* Card 1: Đặt chỗ & Bar Chart */}
        <div className="lib-panel">
          <div className="st-card-header">
            <h3 className="lib-panel-title">Đặt chỗ</h3>
            <span className="st-time-badge">7 ngày qua</span>
          </div>

          <div className="st-ai-section">
            <div className="st-ai-header">AI phân tích</div>
            <div className="st-ai-alert">
              <div className="st-ai-alert-icon">!</div>
              <div>
                <strong className="st-ai-title">Cảnh báo đông đúc</strong>
                <p className="st-ai-text">Khu yên tĩnh là khu vực thường xuyên được sử dụng. Hãy điều hướng sinh viên sang các khu vực khác để tận dụng không gian</p>
              </div>
            </div>
          </div>

          <div className="st-chart">
            <div className="st-chart-y-axis">
              <span>100</span><span>75</span><span>50</span><span>25</span><span>0</span>
            </div>
            <div className="st-chart-bars">
              {CHART_DATA.map((item, idx) => (
                <div key={idx} className="st-bar-col">
                  <div
                    className={`st-bar ${item.value === maxChartValue ? 'highlight' : ''}`}
                    style={{ height: `${(item.value / 100) * 180}px` }}
                  ></div>
                  <span className="st-bar-label">{item.day}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Card 2: Tỉ lệ vi phạm */}
        <div className="lib-panel">
          <div className="st-card-header">
            <h3 className="lib-panel-title">Tỉ lệ vi phạm đặt chỗ</h3>
            <span className="st-time-badge">7 ngày qua</span>
          </div>

          <div className="st-violation-overview">
            <div className="st-big-number">75%</div>
            <div className="st-big-label">Sinh viên đã tuân thủ</div>

            <div className="st-progress-bar">
              <div className="st-progress-segment green" style={{ width: '75%' }}></div>
              <div className="st-progress-segment yellow" style={{ width: '20%' }}></div>
              <div className="st-progress-segment red" style={{ width: '5%' }}></div>
            </div>
          </div>

          <div className="st-metric-list">
            <div className="st-metric-item">
              <div className="st-metric-dot green"></div>
              <div>
                <strong>75%</strong>
                <span>Chỗ đặt được sử dụng</span>
              </div>
            </div>
            <div className="st-metric-item">
              <div className="st-metric-dot yellow"></div>
              <div>
                <strong>16</strong>
                <span>25% Hủy sau khi đặt chỗ</span>
              </div>
            </div>
            <div className="st-metric-item">
              <div className="st-metric-dot red"></div>
              <div>
                <strong>16</strong>
                <span>5% Vi phạm</span>
              </div>
            </div>
          </div>
        </div>

        {/* Card 3: Khu vực sử dụng nhiều nhất */}
        <div className="lib-panel">
          <h3 className="lib-panel-title" style={{ marginBottom: '20px' }}>Khu vực được sử dụng nhiều nhất</h3>
          <div className="st-zone-list">
            {ZONE_USAGE.map((zone, idx) => (
              <div key={idx} className="st-zone-item">
                <div className="st-zone-header">
                  <span className="st-zone-name">{zone.name}</span>
                  <span className="st-zone-percent">{zone.percent}%</span>
                </div>
                <div className="st-zone-bar-bg">
                  <div
                    className="st-zone-bar-fill"
                    style={{ width: `${zone.percent}%`, backgroundColor: zone.color }}
                  ></div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Card 4: Phản hồi */}
        <div className="lib-panel">
          <div className="st-card-header">
            <h3 className="lib-panel-title">Phản hồi của sinh viên</h3>
            <div className="st-nav-buttons">
              <button
                onClick={handlePrevFeedback}
                disabled={feedbackIndex === 0}
                className="st-nav-btn"
              >
                &lt;
              </button>
              <button
                onClick={handleNextFeedback}
                disabled={feedbackIndex >= FEEDBACKS.length - 2}
                className="st-nav-btn"
              >
                &gt;
              </button>
            </div>
          </div>

          <div className="st-feedback-list">
            {visibleFeedbacks.map((fb) => (
              <div key={fb.id} className="st-feedback-card">
                <div className="st-feedback-date">{fb.date}</div>
                <div className="st-feedback-content">{fb.content}</div>
                <div className="st-feedback-user">
                  <div className="lib-avatar-placeholder" style={{ width: 32, height: 32, fontSize: 12 }}>
                    {fb.user.charAt(0)}
                  </div>
                  <div>
                    <div className="st-feedback-name">{fb.user}</div>
                    <div className="st-feedback-code">{fb.code}</div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Statistic;