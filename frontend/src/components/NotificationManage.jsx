import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
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
  ArrowLeft,
  ChevronDown
} from 'lucide-react';
import Header from './Header';
import '../styles/NotificationManage.css';
import { getAllNewsForAdmin, deleteNews } from '../services/newsService';


const NotificationManage = () => {
  const navigate = useNavigate();
  const [viewMode, setViewMode] = useState('list'); // list | create | detail
  const [selectedNotification, setSelectedNotification] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  // Create Form State
  const [formData, setFormData] = useState({
    startTime: '',
    endTime: '',
    title: '',
    subject: 'Thông báo',
    description: ''
  });

  // Fetch notifications from API
  useEffect(() => {
    fetchNotifications();
  }, []);

  const fetchNotifications = async () => {
    try {
      setLoading(true);
      const data = await getAllNewsForAdmin();
      console.log('📰 Fetched news:', data);
      setNotifications(data || []);
    } catch (error) {
      console.error('Error fetching notifications:', error);
      setNotifications([]);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('Bạn có chắc chắn muốn xóa thông báo này?')) return;
    
    try {
      await deleteNews(id);
      alert('Xóa thành báo thành công!');
      fetchNotifications(); // Refresh list
    } catch (error) {
      alert('Lỗi khi xóa thông báo!');
      console.error(error);
    }
  };

  const formatDateTime = (dateString) => {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    const time = date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    const dateStr = date.toLocaleDateString('vi-VN');
    return `${time} ${dateStr}`;
  };

  const getCategoryName = (categoryId) => {
    const categories = {
      1: 'Thông báo',
      2: 'Sự kiện',
      3: 'Tin tức'
    };
    return categories[categoryId] || 'Khác';
  };

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
                  <span className="nt-stat-num">{notifications.length}</span>
                  <span className="nt-stat-label">Thông báo</span>
                </div>
              </div>

              <div className="nt-stat-card">
                <div className="nt-icon-circle nt-bg-pink">
                  <Calendar size={24} color="white" />
                </div>
                <div className="nt-stat-info">
                  <span className="nt-stat-num">{notifications.filter(n => n.categoryId === 2).length}</span>
                  <span className="nt-stat-label">
                    Sự kiện
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
                    {loading ? (
                      <tr>
                        <td colSpan="4" style={{ textAlign: 'center', padding: '2rem' }}>
                          Đang tải...
                        </td>
                      </tr>
                    ) : notifications.length === 0 ? (
                      <tr>
                        <td colSpan="4" style={{ textAlign: 'center', padding: '2rem' }}>
                          Chưa có thông báo nào
                        </td>
                      </tr>
                    ) : (
                      notifications.map((item) => (
                        <tr
                          key={item.id}
                          onClick={() => {
                            navigate(`/notification/view/${item.id}`);
                          }}
                          style={{ cursor: 'pointer' }}
                        >
                          <td>{getCategoryName(item.categoryId)}</td>
                          <td className="nt-font-medium">
                            {item.title}
                          </td>
                          <td className="nt-text-gray">
                            {formatDateTime(item.createdAt)}
                          </td>
                          <td>
                            <div className="nt-actions">
                              <button
                                className="nt-action-btn nt-btn-edit"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setSelectedNotification(item);
                                  setFormData({
                                    startTime: item.startTime || '',
                                    endTime: item.endTime || '',
                                    title: item.title || '',
                                    subject: getCategoryName(item.categoryId),
                                    description: item.content || ''
                                  });
                                  setIsEditing(true);
                                  setViewMode('detail');
                                }}
                              >
                                <Pencil size={16} />
                              </button>
                              <button 
                                className="nt-action-btn nt-btn-delete"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleDelete(item.id);
                                }}
                              >
                                <Trash2 size={16} />
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))
                    )}
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
                        {formatDateTime(selectedNotification.createdAt)}
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
                        {getCategoryName(selectedNotification.categoryId)}
                      </div>
                    </div>
                  </div>

                  <div className="nt-form-group nt-flex-grow">
                    <label>Mô tả</label>
                    <div className="nt-view-text nt-description">
                      {selectedNotification.content || 'Chưa có mô tả chi tiết'}
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
