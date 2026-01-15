import React, { useState } from 'react';
import {
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
  Calendar,
  Plus,
  Filter,
  Trash2,
  Pencil,
  Paperclip,
  ArrowLeft
} from 'lucide-react';
import Header from './Header';
import '../styles/NotificationManage.css';


// Mock Data
const MOCK_NOTIFICATIONS = Array.from({ length: 14 }, (_, i) => ({
  id: i + 1,
  subject: i % 2 === 0 ? 'Thông báo' : 'Sự kiện',
  title:
    i % 2 === 0
      ? 'Thông báo lịch trả sách tháng 12'
      : 'Triển khai dự án "nuôi anh" bằng sách',
  time: '12:21:10 15/12/2025'
}));


const NotificationManage = () => {
  const [viewMode, setViewMode] = useState('list'); // list | create | detail
  const [selectedNotification, setSelectedNotification] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
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
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  return (
    <div className="nt-layout">
      {/* Sidebar */}
      <aside className="nt-sidebar">
        <div className="nt-brand">
          <h1>
            Slib<span className="nt-brand-icon">📚</span>
          </h1>
        </div>
        <nav className="nt-nav">
          <a className="nt-nav-item">
            <LayoutGrid size={20} /> Tổng quan
          </a>
          <a className="nt-nav-item">
            <ArrowRightLeft size={20} /> Kiểm tra ra/vào
          </a>
          <a className="nt-nav-item">
            <Thermometer size={20} /> Bản đồ nhiệt
          </a>
          <a className="nt-nav-item">
            <Armchair size={20} /> Quản lý chỗ ngồi
          </a>
          <a className="nt-nav-item">
            <Users size={20} /> Sinh viên
          </a>
          <a className="nt-nav-item">
            <AlertTriangle size={20} /> Vi phạm
          </a>
          <a className="nt-nav-item">
            <MessageCircle size={20} /> Trò chuyện
          </a>
          <a className="nt-nav-item">
            <BarChart3 size={20} /> Thống kê
          </a>
          <a
            className="nt-nav-item active"
            onClick={() => setViewMode('list')}
          >
            <Bell size={20} /> Thông báo
          </a>
        </nav>
        <div className="nt-sidebar-footer">
          <div className="nt-help-icon">
            <HelpCircle size={24} />
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="nt-main-content">
        <Header searchPlaceholder="Search for anything..." />
        
        <div style={{
          padding: '2rem',
          maxWidth: '1400px',
          margin: '0 auto',
          backgroundColor: '#f9fafb',
          minHeight: 'calc(100vh - 80px)'
        }}>

        {/* ================= LIST VIEW ================= */}
        {viewMode === 'list' && (
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
                  <span className="nt-stat-label">
                    Sự kiện đang diễn ra
                  </span>
                </div>
              </div>

              <div
                className="nt-stat-card nt-card-action"
                onClick={() => setViewMode('create')}
              >
                <div className="nt-icon-circle nt-bg-action">
                  <Plus size={24} color="white" />
                </div>
                <div className="nt-stat-info">
                  <span className="nt-stat-label nt-text-lg">
                    Tạo mới
                  </span>
                </div>
              </div>
            </div>

            <div className="nt-panel">
              <div className="nt-panel-header">
                <h3>Danh sách thông báo</h3>
                <button className="nt-filter-btn">
                  <Filter size={18} />
                </button>
              </div>

              <div className="nt-table-container">
                <table className="nt-table">
                  <thead>
                    <tr>
                      <th width="15%">Chủ đề</th>
                      <th width="45%">
                        Tiêu đề thông báo / sự kiện
                      </th>
                      <th width="25%">Thời gian đăng tải</th>
                      <th width="15%"></th>
                    </tr>
                  </thead>
                  <tbody>
                    {MOCK_NOTIFICATIONS.map((item) => (
                      <tr
      key={item.id}
      onClick={() => {
        setSelectedNotification(item);
        setViewMode('detail');
      }}
    >
                        <td>{item.subject}</td>
                        <td className="nt-font-medium">
                          {item.title}
                        </td>
                        <td className="nt-text-gray">
                          {item.time}
                        </td>
                        <td>
                          <div className="nt-actions">
                            <button
                                className="nt-action-btn nt-btn-edit"
                                onClick={(e) => {
                                  e.stopPropagation(); // 🔑 CHẶN LAN EVENT

                                  setSelectedNotification(item);
                                  setFormData({
                                    startTime: item.startTime || '',
                                    endTime: item.endTime || '',
                                    title: item.title || '',
                                    subject: item.subject || 'Thông báo',
                                    description: item.description || ''
                                  });

                                  setIsEditing(true);
                                  setViewMode('detail');
                                }}
                              >
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
        )}

        {/* ================= CREATE VIEW ================= */}
        {viewMode === 'create' && (
          <div className="nt-fade-in">
            <h2 className="nt-page-title">
  {isEditing ? 'Chỉnh sửa thông báo / sự kiện' : 'Tạo mới'}
</h2>
            <div className="nt-create-panel">
              <div className="nt-form-layout">
                <div className="nt-form-left">
                  <div className="nt-row-2">
                    <div className="nt-form-group">
                      <label>Thời gian bắt đầu</label>
                      <input
                        type="text"
                        name="startTime"
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
                          <option value="Thông báo">
                            Thông báo
                          </option>
                          <option value="Sự kiện">Sự kiện</option>
                        </select>
                        <ChevronDown
                          size={16}
                          className="nt-select-arrow"
                        />
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
                    />
                  </div>
                </div>

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
              <button
                className="nt-btn-submit"
                disabled={!formData.title}
              >
                Submit
              </button>
            </div>
          </div>
        )}

        {/* ================= DETAIL VIEW ================= */}
        {viewMode === 'detail' && selectedNotification && (
          <div className="nt-fade-in">
            <h2 className="nt-page-title">
              Chi tiết thông báo / sự kiện
            </h2>

            <div className="nt-create-panel">
              <div className="nt-form-layout">
                <div className="nt-form-left">
                  <div className="nt-row-2">
                    <div className="nt-form-group">
                      <label>Thời gian</label>
                      <div className="nt-view-text">
                        {selectedNotification.time}
                      </div>
                    </div>
                  </div>

                  <div className="nt-row-2">
                    <div className="nt-form-group">
                      <label>Tiêu đề</label>
                      <div className="nt-view-text">
                        {selectedNotification.title}
                      </div>
                    </div>
                    <div className="nt-form-group">
                      <label>Chủ đề</label>
                      <div className="nt-view-text nt-tag">
                        {selectedNotification.subject}
                      </div>
                    </div>
                  </div>

                  <div className="nt-form-group nt-flex-grow">
                    <label>Mô tả</label>
                    <div className="nt-view-text nt-description">
                      Chưa có mô tả chi tiết
                    </div>
                  </div>
                </div>

                <div className="nt-form-right">
                  <label>File đính kèm</label>
                  <p className="nt-empty-text">
                    Không có file đính kèm
                  </p>
                </div>
              </div>
            </div>

            <div className="nt-footer-actions">
              <button
                className="nt-btn-draft"
                onClick={() => setViewMode('list')}
              >
                Quay lại
              </button>
              <button
                    className="nt-btn-primary-blue"
                    onClick={() => {
                      setFormData({
                        startTime: selectedNotification.startTime || '',
                        endTime: selectedNotification.endTime || '',
                        title: selectedNotification.title || '',
                        subject: selectedNotification.subject || 'Thông báo',
                        description: selectedNotification.description || ''
                      });

                      setIsEditing(true);
                      setViewMode('create');
                    }}
                  >
                    Chỉnh sửa
                  </button>
            </div>
          </div>
        )}
        </div>
      </main>
    </div>
  );
};

export default NotificationManage;
