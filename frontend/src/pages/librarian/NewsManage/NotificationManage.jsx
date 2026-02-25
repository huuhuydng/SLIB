import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Plus,
  Trash2,
  Pencil,
  Paperclip,
  ChevronDown,
  Pin,
  Search
} from 'lucide-react';
import '../../../styles/librarian/librarian-shared.css';
import '../../../styles/librarian/NotificationManage.css';
import { getAllNewsForAdmin, deleteNews, getNewsDetailForAdmin, getNewsImage } from '../../../services/newsService';
import axios from 'axios';


const NotificationManage = () => {
  const navigate = useNavigate();
  const location = useLocation();

  // Always use /librarian/news as base path
  const basePath = '/librarian/news';

  const [viewMode, setViewMode] = useState('list'); // list | create | detail
  const [selectedNotification, setSelectedNotification] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [viewFilter, setViewFilter] = useState('all'); // all | scheduled | draft
  const [searchQuery, setSearchQuery] = useState(''); // Search query

  const classifyStatus = (item, now = new Date()) => {
    const publishedFlag = item?.isPublished === true || item?.isPublished === 'true';
    const publishedAtDate = item?.publishedAt ? new Date(item.publishedAt) : null;
    const isScheduled = !!(publishedAtDate && publishedAtDate > now);
    const publishedBySchedule = !!(publishedAtDate && publishedAtDate <= now);
    // Only treat as published when either explicit publish flag is true AND schedule time has arrived (or no schedule),
    // or when the scheduled time is already due even if the flag is still false.
    const effectivePublished = (publishedFlag && !isScheduled) || publishedBySchedule;
    const isDraft = !effectivePublished && !isScheduled;
    return { publishedFlag, publishedBySchedule, effectivePublished, isScheduled, isDraft, publishedAtDate };
  };

  // Create Form State
  const [formData, setFormData] = useState({
    startTime: '',
    endTime: '',
    title: '',
    subject: 'Thông báo',
    description: ''
  });

  // Load notifications when component mounts
  useEffect(() => {
    console.log('🔄 NotificationManage component mounted, loading notifications...');
    loadNotifications();
  }, [location]);

  const loadNotifications = async () => {
    try {
      console.log('📡 Calling getAllNewsForAdmin API...');
      setLoading(true);
      const data = await getAllNewsForAdmin();
      console.log('📰 API Response:', data);
      console.log('📊 Response type:', typeof data);
      console.log('📊 Is Array:', Array.isArray(data));
      console.log('📊 Data length:', data?.length);

      // API trả về trực tiếp là array
      if (data && Array.isArray(data)) {
        // Sắp xếp: tin tức được ghim lên đầu, sau đó sắp xếp theo thời gian mới nhất
        const sortedData = [...data].sort((a, b) => {
          // Tin được ghim lên trước
          if (a.isPinned && !b.isPinned) return -1;
          if (!a.isPinned && b.isPinned) return 1;
          // Cùng trạng thái ghim, sắp xếp theo thời gian (mới nhất trước)
          const dateA = new Date(a.publishedAt || a.createdAt);
          const dateB = new Date(b.publishedAt || b.createdAt);
          return dateB - dateA;
        });

        console.log('✅ Setting notifications array, length:', sortedData.length);
        setNotifications(sortedData);
      } else {
        console.log('⚠️ Data is not array, setting empty array');
        setNotifications([]);
      }
    } catch (error) {
      console.error('❌ Error loading notifications:', error);
      console.error('Error details:', error.response?.data);
      setNotifications([]);
    } finally {
      setLoading(false);
      console.log('🏁 Loading complete, current notifications:', notifications.length);
    }
  };

  const handleDelete = async (id, event) => {
    event.stopPropagation();

    if (!window.confirm('Bạn có chắc chắn muốn xóa tin tức này?')) {
      return;
    }

    try {
      await deleteNews(id);
      // Reload notifications after delete
      loadNotifications();
    } catch (error) {
      console.error('Error deleting notification:', error);
      alert('Không thể xóa tin tức');
    }
  };

  const handleTogglePin = async (id, event) => {
    event.stopPropagation();
    try {
      await axios.patch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/slib/news/admin/${id}/pin`);
      loadNotifications();
    } catch (error) {
      console.error('Error toggling pin:', error);
      alert('Không thể ghim/bỏ ghim tin tức');
    }
  };

  const handleViewDetail = async (item) => {
    // Navigate to detail view page
    navigate(`${basePath}/view/${item.id}`);
  };

  const formatDateTime = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  };

  const getSubjectFromCategory = (item) => {
    const CATEGORY_MAP = {
      1: 'Sự kiện',
      2: 'Thông báo quan trọng',
      3: 'Sách mới',
      4: 'Ưu đãi'
    };

    if (!item) return 'Thông báo';

    // Support various API shapes: category object, id field, or raw id
    const id = item.categoryId
      ?? item.category?.id
      ?? (typeof item.category === 'number' ? item.category : undefined);

    if (id && CATEGORY_MAP[id]) return CATEGORY_MAP[id];
    if (item.category?.name) return item.category.name;
    return 'Thông báo';
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  return (
    <div className="lib-container">
      {/* ================= LIST VIEW ================= */}
      {viewMode === 'list' && (
        <div className="nt-fade-in">
          <div className="lib-page-title">
            <h1>Quản lý tin tức</h1>
            <div className="lib-inline-stats">
              <span className="lib-inline-stat">
                <span className="dot blue"></span>
                Tổng <strong>{notifications?.length || 0}</strong>
              </span>
              <span className="lib-inline-stat">
                <span className="dot green"></span>
                Đã đăng <strong>{notifications?.filter(n => classifyStatus(n).effectivePublished)?.length || 0}</strong>
              </span>
              <button
                className="lib-btn primary"
                style={{ marginLeft: 'auto' }}
                onClick={() => navigate(`${basePath}/create`)}
              >
                <Plus size={16} /> Tạo mới
              </button>
            </div>
          </div>

          <div className="lib-controls">
            <div className="lib-search">
              <Search size={16} className="lib-search-icon" />
              <input
                type="text"
                placeholder="Tìm kiếm tin tức..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
          </div>

          <div className="lib-tabs">
            <button
              className={`lib-tab ${viewFilter === 'all' ? 'active' : ''}`}
              onClick={() => setViewFilter('all')}
            >
              Tất cả ({notifications?.length || 0})
            </button>
            <button
              className={`lib-tab ${viewFilter === 'scheduled' ? 'active' : ''}`}
              onClick={() => setViewFilter('scheduled')}
            >
              Chờ lịch ({notifications?.filter(n => classifyStatus(n).isScheduled).length || 0})
            </button>
            <button
              className={`lib-tab ${viewFilter === 'draft' ? 'active' : ''}`}
              onClick={() => setViewFilter('draft')}
            >
              Nháp ({notifications?.filter(n => classifyStatus(n).isDraft).length || 0})
            </button>
          </div>

          <div className="lib-panel">
            <div className="nt-table-container">
              {(() => {
                if (loading) {
                  return (
                    <div style={{ textAlign: 'center', padding: '2rem' }}>
                      Đang tải...
                    </div>
                  );
                }

                const now = new Date();

                // Search filter function - search in title only
                const matchesSearch = (item) => {
                  if (!searchQuery.trim()) return true;
                  const query = searchQuery.toLowerCase().trim();
                  const title = (item.title || '').toLowerCase();
                  return title.includes(query);
                };

                // Apply search filter first, then status filter
                const searchFiltered = (notifications || []).filter(matchesSearch);
                const scheduled = searchFiltered.filter(n => classifyStatus(n, now).isScheduled);
                const drafts = searchFiltered.filter(n => classifyStatus(n, now).isDraft);
                const published = searchFiltered.filter(n => classifyStatus(n, now).effectivePublished);

                if ((notifications?.length || 0) === 0) {
                  return (
                    <div style={{ textAlign: 'center', padding: '2rem', color: '#6b7280' }}>
                      Chưa có thông báo nào
                    </div>
                  );
                }

                // Check if search has no results
                if (searchQuery.trim() && searchFiltered.length === 0) {
                  return (
                    <div style={{ textAlign: 'center', padding: '2rem', color: '#6b7280' }}>
                      <div style={{ fontSize: '48px', marginBottom: '12px' }}>🔍</div>
                      <div style={{ fontWeight: 500, marginBottom: '4px' }}>Không tìm thấy kết quả</div>
                      <div style={{ fontSize: '14px' }}>Thử tìm kiếm với từ khóa khác</div>
                    </div>
                  );
                }

                if (viewFilter === 'scheduled') {
                  if (scheduled.length === 0) {
                    return (
                      <div style={{ textAlign: 'center', padding: '2rem', color: '#6b7280' }}>
                        Chưa có tin nào chờ lịch đăng
                      </div>
                    );
                  }
                  return (
                    <table className="nt-table">
                      <thead>
                        <tr>
                          <th width="15%">Chủ đề</th>
                          <th width="35%">Tiêu đề thông báo / sự kiện</th>
                          <th width="20%">Lịch đăng</th>
                          <th width="15%">Thời gian tạo</th>
                          <th width="15%"></th>
                        </tr>
                      </thead>
                      <tbody>
                        {scheduled.map((item) => (
                          <tr
                            key={item.id}
                            onClick={() => handleViewDetail(item)}
                            style={{ cursor: 'pointer', background: '#e3f2fd' }}
                          >
                            <td>{getSubjectFromCategory(item)}</td>
                            <td className="nt-font-medium">
                              {item.title}
                              {item.isPinned && (
                                <span style={{
                                  marginLeft: '0.5rem',
                                  fontSize: '0.8rem',
                                  padding: '0.2rem 0.5rem',
                                  backgroundColor: '#fef3c7',
                                  color: '#92400e',
                                  borderRadius: '4px'
                                }}>
                                  📌 Ghim
                                </span>
                              )}
                            </td>
                            <td className="nt-text-gray">
                              <strong style={{ color: '#2196F3' }}>
                                {formatDateTime(item.publishedAt)}
                              </strong>
                            </td>
                            <td className="nt-text-gray">
                              {formatDateTime(item.createdAt)}
                            </td>
                            <td>
                              <div className="nt-actions">
                                <button
                                  className={`nt-action-btn ${item.isPinned ? 'nt-btn-pin-active' : 'nt-btn-pin'}`}
                                  onClick={(e) => handleTogglePin(item.id, e)}
                                  title={item.isPinned ? 'Bỏ ghim' : 'Ghim'}
                                  style={{
                                    background: item.isPinned ? 'linear-gradient(135deg, #fef3c7 0%, #fde68a 100%)' : 'linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%)',
                                    color: item.isPinned ? '#92400e' : '#64748b'
                                  }}
                                >
                                  <Pin size={16} />
                                </button>
                                <button
                                  className="nt-action-btn nt-btn-edit"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    navigate(`${basePath}/edit/${item.id}`);
                                  }}
                                >
                                  <Pencil size={16} />
                                </button>
                                <button
                                  className="nt-action-btn nt-btn-delete"
                                  onClick={(e) => handleDelete(item.id, e)}
                                >
                                  <Trash2 size={16} />
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  );
                }

                if (viewFilter === 'draft') {
                  if (drafts.length === 0) {
                    return (
                      <div style={{ textAlign: 'center', padding: '2rem', color: '#6b7280' }}>
                        Chưa có bản nháp nào
                      </div>
                    );
                  }
                  return (
                    <table className="nt-table">
                      <thead>
                        <tr>
                          <th width="15%">Chủ đề</th>
                          <th width="45%">Tiêu đề thông báo / sự kiện</th>
                          <th width="25%">Thời gian tạo</th>
                          <th width="15%"></th>
                        </tr>
                      </thead>
                      <tbody>
                        {drafts.map((item) => (
                          <tr
                            key={item.id}
                            onClick={() => handleViewDetail(item)}
                            style={{ cursor: 'pointer', background: '#f8fafc' }}
                          >
                            <td>{getSubjectFromCategory(item)}</td>
                            <td className="nt-font-medium">
                              {item.title}
                              {item.isPinned && (
                                <span style={{
                                  marginLeft: '0.5rem',
                                  fontSize: '0.8rem',
                                  padding: '0.2rem 0.5rem',
                                  backgroundColor: '#fef3c7',
                                  color: '#92400e',
                                  borderRadius: '4px'
                                }}>
                                  📌 Ghim
                                </span>
                              )}
                              <span style={{
                                marginLeft: '0.5rem',
                                fontSize: '0.8rem',
                                padding: '0.2rem 0.5rem',
                                backgroundColor: '#e5e7eb',
                                color: '#4b5563',
                                borderRadius: '4px'
                              }}>
                                Nháp
                              </span>
                            </td>
                            <td className="nt-text-gray">
                              {formatDateTime(item.createdAt)}
                            </td>
                            <td>
                              <div className="nt-actions">
                                <button
                                  className={`nt-action-btn ${item.isPinned ? 'nt-btn-pin-active' : 'nt-btn-pin'}`}
                                  onClick={(e) => handleTogglePin(item.id, e)}
                                  title={item.isPinned ? 'Bỏ ghim' : 'Ghim'}
                                  style={{
                                    background: item.isPinned ? 'linear-gradient(135deg, #fef3c7 0%, #fde68a 100%)' : 'linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%)',
                                    color: item.isPinned ? '#92400e' : '#64748b'
                                  }}
                                >
                                  <Pin size={16} />
                                </button>
                                <button
                                  className="nt-action-btn nt-btn-edit"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    navigate(`${basePath}/edit/${item.id}`);
                                  }}
                                >
                                  <Pencil size={16} />
                                </button>
                                <button
                                  className="nt-action-btn nt-btn-delete"
                                  onClick={(e) => handleDelete(item.id, e)}
                                >
                                  <Trash2 size={16} />
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  );
                }

                // Default 'all' tab: hiển thị TẤT CẢ items (kể cả draft, scheduled) - đã filter theo search
                const listToShow = searchFiltered;

                if (listToShow.length === 0) {
                  return (
                    <div style={{ textAlign: 'center', padding: '2rem', color: '#6b7280' }}>
                      Chưa có thông báo nào
                    </div>
                  );
                }

                return (
                  <table className="nt-table">
                    <thead>
                      <tr>
                        <th width="12%">Chủ đề</th>
                        <th width="35%">
                          Tiêu đề thông báo / sự kiện
                        </th>
                        <th width="12%">Trạng thái</th>
                        <th width="20%">Thời gian đăng tải</th>
                        <th width="12%"></th>
                      </tr>
                    </thead>
                    <tbody>
                      {listToShow.map((item) => (
                        <tr
                          key={item.id}
                          onClick={() => handleViewDetail(item)}
                          style={{ cursor: 'pointer' }}
                        >
                          <td>{getSubjectFromCategory(item)}</td>
                          <td className="nt-font-medium">
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                              <span style={{ maxWidth: '350px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'inline-block' }}>
                                {item.title}
                              </span>
                              {item.isPinned && (
                                <span style={{
                                  fontSize: '0.75rem',
                                  padding: '0.15rem 0.4rem',
                                  backgroundColor: '#fef3c7',
                                  color: '#92400e',
                                  borderRadius: '4px',
                                  whiteSpace: 'nowrap',
                                  flexShrink: 0
                                }}>
                                  📌 Ghim
                                </span>
                              )}
                            </div>
                          </td>
                          <td>
                            {(() => {
                              const status = classifyStatus(item, new Date());
                              if (status.isDraft) {
                                return (
                                  <span style={{
                                    fontSize: '0.8rem',
                                    padding: '0.25rem 0.6rem',
                                    backgroundColor: '#e5e7eb',
                                    color: '#4b5563',
                                    borderRadius: '6px',
                                    fontWeight: 500
                                  }}>
                                    Nháp
                                  </span>
                                );
                              } else if (status.isScheduled) {
                                return (
                                  <span style={{
                                    fontSize: '0.8rem',
                                    padding: '0.25rem 0.6rem',
                                    backgroundColor: '#dbeafe',
                                    color: '#1e40af',
                                    borderRadius: '6px',
                                    fontWeight: 500
                                  }}>
                                    ⏰ Lên lịch
                                  </span>
                                );
                              } else {
                                return (
                                  <span style={{
                                    fontSize: '0.8rem',
                                    padding: '0.25rem 0.6rem',
                                    backgroundColor: '#dcfce7',
                                    color: '#166534',
                                    borderRadius: '6px',
                                    fontWeight: 500
                                  }}>
                                    ✓ Đã đăng
                                  </span>
                                );
                              }
                            })()}
                          </td>
                          <td className="nt-text-gray">
                            {formatDateTime(item.publishedAt || item.createdAt)}
                          </td>
                          <td>
                            <div className="nt-actions">
                              <button
                                className={`nt-action-btn ${item.isPinned ? 'nt-btn-pin-active' : 'nt-btn-pin'}`}
                                onClick={(e) => handleTogglePin(item.id, e)}
                                title={item.isPinned ? 'Bỏ ghim' : 'Ghim'}
                                style={{
                                  background: item.isPinned ? 'linear-gradient(135deg, #fef3c7 0%, #fde68a 100%)' : 'linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%)',
                                  color: item.isPinned ? '#92400e' : '#64748b'
                                }}
                              >
                                <Pin size={16} />
                              </button>
                              <button
                                className="nt-action-btn nt-btn-edit"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  navigate(`${basePath}/edit/${item.id}`);
                                }}
                              >
                                <Pencil size={16} />
                              </button>
                              <button
                                className="nt-action-btn nt-btn-delete"
                                onClick={(e) => handleDelete(item.id, e)}
                              >
                                <Trash2 size={16} />
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                );
              })()}
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
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '1.5rem'
          }}>
            <h2 className="nt-page-title" style={{ margin: 0 }}>
              Xem trước bài viết
            </h2>
            <div style={{ display: 'flex', gap: '0.75rem' }}>
              <button
                className="nt-btn-draft"
                onClick={() => setViewMode('list')}
              >
                Quay lại
              </button>
              <button
                className="nt-btn-primary-blue"
                onClick={() => navigate(`${basePath}/edit/${selectedNotification.id}`)}
              >
                Chỉnh sửa
              </button>
            </div>
          </div>

          {/* HTML Content Preview */}
          <div style={{
            backgroundColor: 'white',
            borderRadius: '12px',
            padding: '2rem',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
            marginBottom: '1.5rem'
          }}>
            <div
              dangerouslySetInnerHTML={{ __html: selectedNotification.content }}
              style={{
                maxWidth: '100%',
                overflow: 'hidden'
              }}
            />
          </div>

          {/* Metadata Panel */}
          <div style={{
            backgroundColor: 'white',
            borderRadius: '12px',
            padding: '1.5rem',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
          }}>
            <h3 style={{
              margin: '0 0 1rem 0',
              fontSize: '1.1rem',
              fontWeight: '600',
              color: '#1f2937'
            }}>
              Thông tin bài viết
            </h3>
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '1rem',
              fontSize: '0.9rem',
              color: '#6b7280'
            }}>
              <div>
                <strong style={{ color: '#374151' }}>ID:</strong> {selectedNotification.id}
              </div>
              <div>
                <strong style={{ color: '#374151' }}>Chủ đề:</strong>{' '}
                <span className="nt-tag">
                  {selectedNotification.categoryName || getSubjectFromCategory(selectedNotification.category)}
                </span>
              </div>
              <div>
                <strong style={{ color: '#374151' }}>Trạng thái:</strong>{' '}
                {selectedNotification.isPublished ? (
                  <span style={{ color: '#10b981' }}>✓ Đã đăng</span>
                ) : (
                  <span style={{ color: '#6b7280' }}>📝 Nháp</span>
                )}
              </div>
              <div>
                <strong style={{ color: '#374151' }}>Ghim:</strong>{' '}
                {selectedNotification.isPinned ? 'Có' : 'Không'}
              </div>
              <div>
                <strong style={{ color: '#374151' }}>Lượt xem:</strong>{' '}
                {selectedNotification.viewCount || 0}
              </div>
              <div>
                <strong style={{ color: '#374151' }}>Đăng tải:</strong>{' '}
                {formatDateTime(selectedNotification.publishedAt || selectedNotification.createdAt)}
              </div>
              <div>
                <strong style={{ color: '#374151' }}>Tạo lúc:</strong>{' '}
                {formatDateTime(selectedNotification.createdAt)}
              </div>
              {selectedNotification.updatedAt && (
                <div>
                  <strong style={{ color: '#374151' }}>Cập nhật:</strong>{' '}
                  {formatDateTime(selectedNotification.updatedAt)}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default NotificationManage;
