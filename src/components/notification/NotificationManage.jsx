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
  Calendar,
  Plus,
  Filter,
  Trash2,
  Pencil,
  Paperclip,
  ArrowLeft
} from 'lucide-react';
import '../../styles/NotificationManage.css';

// Mock Data
const MOCK_NOTIFICATIONS = Array.from({ length: 14 }, (_, i) => ({
  id: i + 1,
  subject: i % 2 === 0 ? 'Thông báo' : 'Sự kiện',
  title: i % 2 === 0 
    ? 'Thông báo lịch trả sách tháng 12' 
    : 'Triển khai dự án "nuôi anh" bằng sách',
  time: '12:21:10 15/12/2025'
}));

const NotificationManage = () => {
  const [viewMode, setViewMode] = useState('list'); // 'list' | 'create'
  
  // Create Form State
  const [formData, setFormData] = useState({
    startTime: '',
    endTime: '',
    title: '',
    subject: 'Thông báo',
    description: ''
  });

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  return (
    <div className="nt-layout">
      {/* Sidebar */}
      <aside className="nt-sidebar">
        <div className="nt-brand">
          <h1>Slib<span className="nt-brand-icon">📚</span></h1>
        </div>
        <nav className="nt-nav">
          <a href="#" className="nt-nav-item"><LayoutGrid size={20} /> Tổng quan</a>
          <a href="#" className="nt-nav-item"><ArrowRightLeft size={20} /> Kiểm tra ra/vào</a>
          <a href="#" className="nt-nav-item"><Thermometer size={20} /> Bản đồ nhiệt</a>
          <a href="#" className="nt-nav-item"><Armchair size={20} /> Quản lý chỗ ngồi</a>
          <a href="#" className="nt-nav-item"><Users size={20} /> Sinh viên</a>
          <a href="#" className="nt-nav-item"><AlertTriangle size={20} /> Vi phạm</a>
          <a href="#" className="nt-nav-item"><MessageCircle size={20} /> Trò chuyện</a>
          <a href="#" className="nt-nav-item"><BarChart3 size={20} /> Thống kê</a>
          <a href="#" className="nt-nav-item active" onClick={() => setViewMode('list')}><Bell size={20} /> Thông báo</a>
        </nav>
        <div className="nt-sidebar-footer">
          <div className="nt-help-icon"><HelpCircle size={24} /></div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="nt-main-content">
        {/* Topbar */}
        <header className="nt-topbar">
          <button className="nt-back-btn" onClick={() => setViewMode('list')}>
            {viewMode === 'list' ? <ChevronLeft size={20} /> : <ArrowLeft size={20} />}
          </button>
          
          <div className="nt-search-bar">
            <Search size={18} className="nt-search-icon" />
            <input type="text" placeholder="Search for anything..." />
          </div>

          <div className="nt-profile-pill">
            <img src="https://picsum.photos/id/64/40/40" alt="Admin" className="nt-avatar-sm" />
            <div className="nt-profile-info">
              <span className="nt-profile-name">PhucNH</span>
              <span className="nt-profile-role">Librarian</span>
            </div>
            <ChevronDown size={16} className="nt-chevron" />
          </div>
        </header>

        {viewMode === 'list' ? (
          /* --- LIST VIEW --- */
          <div className="nt-fade-in">
            <h2 className="nt-page-title">Quản lý thông báo</h2>
            
            <div className="nt-stats-row">
              <div className="nt-stat-card">
                <div className="nt-icon-circle nt-bg-purple">
                  <Bell size={24} color="white" />
                </div>
                <div className="nt-stat-info">
                  <span className="nt-stat-num">18</span>
                  <span className="nt-stat-label">Thông báo</span>
                </div>
              </div>

              <div className="nt-stat-card">
                <div className="nt-icon-circle nt-bg-pink">
                  <Calendar size={24} color="white" />
                </div>
                <div className="nt-stat-info">
                  <span className="nt-stat-num">3</span>
                  <span className="nt-stat-label">Sự kiện đang diễn ra</span>
                </div>
              </div>

              <div className="nt-stat-card nt-card-action" onClick={() => setViewMode('create')}>
                <div className="nt-icon-circle nt-bg-action">
                  <Plus size={24} color="white" />
                </div>
                <div className="nt-stat-info">
                  <span className="nt-stat-label nt-text-lg">Tạo mới</span>
                </div>
              </div>
            </div>

            <div className="nt-panel">
              <div className="nt-panel-header">
                <h3>Danh sách thông báo</h3>
                <button className="nt-filter-btn"><Filter size={18} /></button>
              </div>
              
              <div className="nt-table-container">
                <table className="nt-table">
                  <thead>
                    <tr>
                      <th width="15%">Chủ đề</th>
                      <th width="45%">Tiêu đề thông báo/ sự kiện</th>
                      <th width="25%">Thời gian đăng tải</th>
                      <th width="15%"></th>
                    </tr>
                  </thead>
                  <tbody>
                    {MOCK_NOTIFICATIONS.map((item) => (
                      <tr key={item.id}>
                        <td>{item.subject}</td>
                        <td className="nt-font-medium">{item.title}</td>
                        <td className="nt-text-gray">{item.time}</td>
                        <td>
                          <div className="nt-actions">
                            <button className="nt-action-btn nt-btn-edit">
                              <Pencil size={16} />
                            </button>
                            <button className="nt-action-btn nt-btn-delete">
                              <Trash2 size={16} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        ) : (
          /* --- CREATE VIEW --- */
          <div className="nt-fade-in">
            <h2 className="nt-page-title">Tạo mới</h2>
            
            <div className="nt-create-panel">
              <div className="nt-form-layout">
                {/* Left Column: Form Fields */}
                <div className="nt-form-left">
                  <div className="nt-row-2">
                    <div className="nt-form-group">
                      <label>Thời gian bắt đầu</label>
                      <input 
                        type="text" 
                        name="startTime"
                        placeholder="--:-- --/--/----"
                        className="nt-input"
                        value={formData.startTime}
                        onChange={handleInputChange}
                      />
                    </div>
                    <div className="nt-form-group">
                      <label>Thời gian kết thúc</label>
                      <input 
                        type="text" 
                        name="endTime" 
                        placeholder="--:-- --/--/----"
                        className="nt-input"
                        value={formData.endTime}
                        onChange={handleInputChange}
                      />
                    </div>
                  </div>

                  <div className="nt-row-2">
                    <div className="nt-form-group">
                      <label>Tiêu đề</label>
                      <input 
                        type="text" 
                        name="title"
                        className="nt-input"
                        value={formData.title}
                        onChange={handleInputChange}
                      />
                    </div>
                    <div className="nt-form-group">
                      <label>Chủ đề</label>
                      <div className="nt-select-wrapper">
                        <select 
                          name="subject"
                          className="nt-input nt-select"
                          value={formData.subject}
                          onChange={handleInputChange}
                        >
                          <option value="Thông báo">Thông báo</option>
                          <option value="Sự kiện">Sự kiện</option>
                        </select>
                        <ChevronDown size={16} className="nt-select-arrow"/>
                      </div>
                    </div>
                  </div>

                  <div className="nt-form-group nt-flex-grow">
                    <label>Mô tả</label>
                    <textarea 
                      name="description"
                      className="nt-textarea"
                      value={formData.description}
                      onChange={handleInputChange}
                    ></textarea>
                  </div>
                </div>

                {/* Right Column: File Upload */}
                <div className="nt-form-right">
                  <button className="nt-btn-primary-blue">
                    <Paperclip size={16} /> File đính kèm
                  </button>
                  <div className="nt-dropzone">
                    <p>Hoặc di chuyển tệp vào vùng này</p>
                  </div>
                </div>
              </div>
            </div>

            <div className="nt-footer-actions">
              <button className="nt-btn-draft">Save Draft</button>
              <button className="nt-btn-submit" disabled={!formData.title}>Submit</button>
            </div>
          </div>
        )}
      </main>
    </div>
  );
};

export default NotificationManage;
