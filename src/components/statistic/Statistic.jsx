import React, { useState } from 'react';
import {
  Search,
  ChevronDown,
  LayoutGrid,
  ArrowRightLeft,
  Thermometer,
  Armchair,
  Users,
  AlertTriangle,
  MessageCircle,
  BarChart3,
  Bell,
  HelpCircle,
  ChevronLeft,
  Sparkles,
  AlertCircle,
  ChevronRight,
  List,
  CalendarOff,
  Briefcase
} from 'lucide-react';
import '../../styles/Statistic.css';

// Mock Data
const CHART_DATA = [
  { day: 'Mon', value: 35 },
  { day: 'Tue', value: 28 },
  { day: 'Wed', value: 50 },
  { day: 'Thu', value: 35 },
  { day: 'Fri', value: 80 }, // Highest
  { day: 'Sat', value: 35 },
  { day: 'Sun', value: 50 },
];

const ZONE_USAGE = [
  { name: 'Khu yên tĩnh', percent: 95, color: '#EF4444' }, // Red
  { name: 'Khu thảo luận', percent: 45, color: '#10B981' }, // Green
  { name: 'Khu tự học', percent: 70, color: '#FACC15' },   // Yellow
];

const FEEDBACKS = [
  {
    id: 1,
    date: '10:35-15/12/2023',
    content: 'Technical English for Beginners',
    user: 'PhucNH',
    code: 'DE170706',
    avatar: 'https://picsum.photos/id/64/40/40'
  },
  {
    id: 2,
    date: '10:35-15/12/2023',
    content: 'Anh bàn bên đẹp trai quá',
    user: 'PhucNH',
    code: 'DE170706',
    avatar: 'https://picsum.photos/id/65/40/40'
  },
  {
    id: 3,
    date: '09:00-14/12/2023',
    content: 'Wifi hôm nay hơi lag ở khu B',
    user: 'Minh Anh',
    code: 'DE182201',
    avatar: 'https://picsum.photos/id/66/40/40'
  },
  {
    id: 4,
    date: '14:20-13/12/2023',
    content: 'Cần thêm ổ cắm điện',
    user: 'Hoang Long',
    code: 'DE170999',
    avatar: 'https://picsum.photos/id/67/40/40'
  }
];

const Statistic = () => {
  const [feedbackIndex, setFeedbackIndex] = useState(0);

  const handlePrevFeedback = () => {
    setFeedbackIndex((prev) => Math.max(0, prev - 1));
  };

  const handleNextFeedback = () => {
    setFeedbackIndex((prev) => Math.min(FEEDBACKS.length - 2, prev + 1));
  };

  const visibleFeedbacks = FEEDBACKS.slice(feedbackIndex, feedbackIndex + 2);

  return (
    <div className="st-layout">
      {/* Sidebar */}
      <aside className="st-sidebar">
        <div className="st-brand">
          <h1>Slib<span className="st-brand-icon">📚</span></h1>
        </div>
        <nav className="st-nav">
          <a href="#" className="st-nav-item"><LayoutGrid size={20} /> Tổng quan</a>
          <a href="#" className="st-nav-item"><ArrowRightLeft size={20} /> Kiểm tra ra/vào</a>
          <a href="#" className="st-nav-item"><Thermometer size={20} /> Bản đồ nhiệt</a>
          <a href="#" className="st-nav-item"><Armchair size={20} /> Quản lý chỗ ngồi</a>
          <a href="#" className="st-nav-item"><Users size={20} /> Sinh viên</a>
          <a href="#" className="st-nav-item"><AlertTriangle size={20} /> Vi phạm</a>
          <a href="#" className="st-nav-item"><MessageCircle size={20} /> Trò chuyện</a>
          <a href="#" className="st-nav-item active"><BarChart3 size={20} /> Thống kê</a>
          <a href="#" className="st-nav-item"><Bell size={20} /> Thông báo</a>
        </nav>
        <div className="st-sidebar-footer">
          <div className="st-help-icon"><HelpCircle size={24} /></div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="st-main-content">
        {/* Topbar */}
        <header className="st-topbar">
          <button className="st-back-btn">
            <ChevronLeft size={20} />
          </button>
          
          <div className="st-search-bar">
            <Search size={18} className="st-search-icon" />
            <input type="text" placeholder="Search for anything..." />
          </div>

          <div className="st-profile-pill">
            <img src="https://picsum.photos/id/64/40/40" alt="Admin" className="st-avatar-sm" />
            <div className="st-profile-info">
              <span className="st-profile-name">PhucNH</span>
              <span className="st-profile-role">Librarian</span>
            </div>
            <ChevronDown size={16} className="st-chevron" />
          </div>
        </header>

        <div className="st-grid">
          
          {/* Card 1: Đặt chỗ & Bar Chart */}
          <div className="st-card st-card-booking">
            <div className="st-card-header">
              <h3>Đặt chỗ</h3>
              <span className="st-chip-date"><CalendarOff size={12} style={{marginRight: 4}}/> last 7 days</span>
            </div>
            
            <div className="st-ai-box">
              <div className="st-ai-title">
                <Sparkles size={16} color="#C026D3" />
                <span>AI phân tích</span>
              </div>
              <div className="st-warning-box">
                <div className="st-warning-icon-wrapper">
                  <AlertCircle size={20} color="#EA580C" />
                </div>
                <div className="st-warning-content">
                  <strong>Cảnh báo đông đúc</strong>
                  <p>Khu yên tĩnh là khu vực thường xuyên được sử dụng. Hãy điều hướng sinh viên sang các khu vực khác để tận dụng không gian</p>
                </div>
              </div>
            </div>

            <div className="st-chart-container">
              <div className="st-y-axis">
                <span>100</span>
                <span>75</span>
                <span>50</span>
                <span>25</span>
                <span>0</span>
              </div>
              <div className="st-bars-wrapper">
                {CHART_DATA.map((item, idx) => (
                  <div key={idx} className="st-bar-group">
                    <div 
                      className={`st-bar ${item.value === 80 ? 'active' : ''}`} 
                      style={{ height: `${item.value}%` }}
                    ></div>
                    <span className="st-bar-label">{item.day}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Card 2: Tỉ lệ vi phạm */}
          <div className="st-card st-card-violation">
             <div className="st-card-header">
              <h3>Tỉ lệ vi phạm đặt chỗ</h3>
              <span className="st-chip-date"><CalendarOff size={12} style={{marginRight: 4}}/> last 7 days</span>
            </div>
            
            <div className="st-violation-main">
              <div className="st-big-percent">75%</div>
              <div className="st-subtitle">Sinh viên đã tuân thủ</div>
              
              <div className="st-progress-multi">
                <div className="st-seg st-seg-green" style={{width: '75%'}}></div>
                <div className="st-seg st-seg-yellow" style={{width: '20%'}}></div>
                <div className="st-seg st-seg-red" style={{width: '5%'}}></div>
              </div>
              <div className="st-ticks">
                <span style={{left: '75%'}}>75%</span>
                <span style={{left: '95%'}}>25%</span>
                <span style={{left: '100%'}}>5%</span>
              </div>
            </div>

            <div className="st-mini-stats">
              <div className="st-stat-item">
                <div className="st-icon-circle st-bg-green"><List size={20} color="white" /></div>
                <div className="st-stat-text">
                  <strong>75%</strong>
                  <span>Chỗ đặt được sử dụng</span>
                </div>
              </div>
              <div className="st-stat-item">
                <div className="st-icon-circle st-bg-yellow"><Briefcase size={20} color="white" /></div>
                <div className="st-stat-text">
                  <strong>16</strong>
                  <span>25% Hủy sau khi đặt chỗ</span>
                </div>
              </div>
              <div className="st-stat-item">
                <div className="st-icon-circle st-bg-red"><Briefcase size={20} color="white" /></div>
                <div className="st-stat-text">
                  <strong>16</strong>
                  <span>5% Vi phạm</span>
                </div>
              </div>
            </div>
          </div>

          {/* Card 3: Khu vực sử dụng nhiều nhất */}
          <div className="st-card st-card-zones">
            <div className="st-card-header">
              <h3>Khu vực được sử dụng nhiều nhất</h3>
            </div>
            <div className="st-zones-list">
              {ZONE_USAGE.map((zone, idx) => (
                <div key={idx} className="st-zone-item">
                  <div className="st-zone-header">
                    <span>{zone.name}</span>
                    <span>{zone.percent}%</span>
                  </div>
                  <div className="st-progress-bar-bg">
                    <div 
                      className="st-progress-fill" 
                      style={{ width: `${zone.percent}%`, backgroundColor: zone.color }}
                    ></div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Card 4: Phản hồi */}
          <div className="st-card st-card-feedback">
            <div className="st-card-header">
              <h3>Phản hồi của sinh viên</h3>
              <div className="st-nav-controls">
                <button onClick={handlePrevFeedback} disabled={feedbackIndex === 0}><ChevronLeft size={16} /></button>
                <button onClick={handleNextFeedback} disabled={feedbackIndex >= FEEDBACKS.length - 2}><ChevronRight size={16} /></button>
              </div>
            </div>
            
            <div className="st-feedback-list">
              {visibleFeedbacks.map((fb) => (
                <div key={fb.id} className="st-feedback-item">
                  <div className="st-fb-date">{fb.date}</div>
                  <div className="st-fb-content">{fb.content}</div>
                  <div className="st-fb-user">
                    <img src={fb.avatar} alt={fb.user} />
                    <div>
                      <div className="st-fb-name">{fb.user}</div>
                      <div className="st-fb-code">{fb.code}</div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

        </div>
      </main>
    </div>
  );
};

export default Statistic;
