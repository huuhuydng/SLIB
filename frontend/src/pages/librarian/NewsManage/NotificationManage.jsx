import React, { useState, useEffect, useMemo, useRef } from 'react';
import { API_BASE_URL } from '../../../config/apiConfig';
import { useToast } from '../../../components/common/ToastProvider';
import { useConfirm } from '../../../components/common/ConfirmDialog';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Plus,
  Trash2,
  Pencil,
  Paperclip,
  ChevronDown,
  Pin,
  Search,
  ArrowUpDown,
  ArrowUp,
  ArrowDown,
  Filter,
  X,
  SlidersHorizontal,
  Loader2
} from 'lucide-react';
import '../../../styles/librarian/librarian-shared.css';
import '../../../styles/librarian/CheckInOut.css';
import '../../../styles/librarian/BookingManage.css';
import '../../../styles/librarian/NotificationManage.css';
import { getAllNewsForAdmin, deleteNews, getNewsDetailForAdmin, getNewsImage, batchDeleteNews } from '../../../services/librarian/newsService';
import axios from 'axios';

const STATUS_OPTIONS = [
  { value: '', label: 'Tất cả' },
  { value: 'published', label: 'Đã đăng' },
  { value: 'scheduled', label: 'Lên lịch' },
  { value: 'draft', label: 'Nháp' },
];

const NotificationManage = () => {
  const toast = useToast();
  const { confirm } = useConfirm();
  const navigate = useNavigate();
  const location = useLocation();
  const basePath = '/librarian/news';

  const [viewMode, setViewMode] = useState('list');
  const [selectedNotification, setSelectedNotification] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  // Sort & Filter & Pagination
  const [sortConfig, setSortConfig] = useState({ column: null, direction: null });
  const [columnFilters, setColumnFilters] = useState({ category: '', title: '', status: '', publishedAt: '' });
  const [activeFilterCol, setActiveFilterCol] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [visibleColumns, setVisibleColumns] = useState({
    category: true, title: true, status: true, publishedAt: true, actions: true,
  });
  const [showColumnMenu, setShowColumnMenu] = useState(false);
  const filterRef = useRef(null);

  // Selection for batch delete
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [deleting, setDeleting] = useState(false);

  const classifyStatus = (item, now = new Date()) => {
    const publishedFlag = item?.isPublished === true || item?.isPublished === 'true';
    const publishedAtDate = item?.publishedAt ? new Date(item.publishedAt) : null;
    const isScheduled = !!(publishedAtDate && publishedAtDate > now);
    const publishedBySchedule = !!(publishedAtDate && publishedAtDate <= now);
    const effectivePublished = (publishedFlag && !isScheduled) || publishedBySchedule;
    const isDraft = !effectivePublished && !isScheduled;
    return { publishedFlag, publishedBySchedule, effectivePublished, isScheduled, isDraft, publishedAtDate };
  };

  const getStatusLabel = (item) => {
    const s = classifyStatus(item);
    if (s.isDraft) return 'Nháp';
    if (s.isScheduled) return 'Lên lịch';
    return 'Đã đăng';
  };

  const getStatusKey = (item) => {
    const s = classifyStatus(item);
    if (s.isDraft) return 'draft';
    if (s.isScheduled) return 'scheduled';
    return 'published';
  };

  // Create Form State
  const [formData, setFormData] = useState({
    startTime: '', endTime: '', title: '', subject: 'Thông báo', description: ''
  });

  useEffect(() => { loadNotifications(); }, [location]);

  // Close filter dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (filterRef.current && !filterRef.current.contains(e.target)) {
        setActiveFilterCol(null);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const loadNotifications = async () => {
    try {
      setLoading(true);
      const data = await getAllNewsForAdmin();
      if (data && Array.isArray(data)) {
        const sortedData = [...data].sort((a, b) => {
          if (a.isPinned && !b.isPinned) return -1;
          if (!a.isPinned && b.isPinned) return 1;
          return new Date(b.publishedAt || b.createdAt) - new Date(a.publishedAt || a.createdAt);
        });
        setNotifications(sortedData);
      } else {
        setNotifications([]);
      }
    } catch (error) {
      console.error('Error loading notifications:', error);
      setNotifications([]);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id, event) => {
    event.stopPropagation();
    const ok = await confirm({
      title: 'Xoá tin tức',
      message: 'Bạn có chắc chắn muốn xoá tin tức này? Hành động này không thể hoàn tác.',
      variant: 'danger',
      confirmText: 'Xoá',
    });
    if (!ok) return;
    try {
      await deleteNews(id);
      loadNotifications();
    } catch (error) {
      console.error('Error deleting notification:', error);
      toast.error('Không thể xóa tin tức');
    }
  };

  const handleTogglePin = async (id, event) => {
    event.stopPropagation();
    try {
      await axios.patch(`${API_BASE_URL}/slib/news/admin/${id}/pin`);
      loadNotifications();
    } catch (error) {
      console.error('Error toggling pin:', error);
      toast.error('Không thể ghim/bỏ ghim tin tức');
    }
  };

  const handleViewDetail = (item) => {
    navigate(`${basePath}/view/${item.id}`);
  };

  const formatDateTime = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleString('vi-VN', {
      hour: '2-digit', minute: '2-digit',
      day: '2-digit', month: '2-digit', year: 'numeric'
    });
  };

  const getSubjectFromCategory = (item) => {
    const CATEGORY_MAP = { 1: 'Sự kiện', 2: 'Thông báo quan trọng', 3: 'Sách mới', 4: 'Ưu đãi' };
    if (!item) return 'Thông báo';
    const id = item.categoryId ?? item.category?.id ?? (typeof item.category === 'number' ? item.category : undefined);
    if (id && CATEGORY_MAP[id]) return CATEGORY_MAP[id];
    if (item.category?.name) return item.category.name;
    return 'Thông báo';
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  // Sort handler
  const handleSort = (column) => {
    setSortConfig(prev => {
      if (prev.column === column) {
        if (prev.direction === 'asc') return { column, direction: 'desc' };
        if (prev.direction === 'desc') return { column: null, direction: null };
      }
      return { column, direction: 'asc' };
    });
  };

  const handleFilterChange = (column, value) => {
    setColumnFilters(prev => ({ ...prev, [column]: value }));
    setCurrentPage(1);
  };

  const clearColumnFilter = (column) => {
    setColumnFilters(prev => ({ ...prev, [column]: '' }));
  };

  const getNewsValue = (item, column) => {
    switch (column) {
      case 'category': return getSubjectFromCategory(item);
      case 'title': return item.title || '';
      case 'status': return getStatusKey(item);
      case 'publishedAt': return item.publishedAt || item.createdAt || '';
      default: return '';
    }
  };

  // Filtered + Sorted data
  const filteredNews = useMemo(() => {
    let result = [...notifications];

    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      result = result.filter(item =>
        (item.title || '').toLowerCase().includes(q) ||
        getSubjectFromCategory(item).toLowerCase().includes(q)
      );
    }

    Object.entries(columnFilters).forEach(([col, val]) => {
      if (!val) return;
      if (col === 'status') {
        result = result.filter(item => getStatusKey(item) === val);
      } else {
        const v = val.toLowerCase();
        result = result.filter(item => getNewsValue(item, col).toLowerCase().includes(v));
      }
    });

    if (sortConfig.column) {
      result.sort((a, b) => {
        let aVal = getNewsValue(a, sortConfig.column);
        let bVal = getNewsValue(b, sortConfig.column);
        if (sortConfig.column === 'publishedAt') {
          aVal = new Date(aVal || 0).getTime();
          bVal = new Date(bVal || 0).getTime();
        } else {
          aVal = String(aVal).toLowerCase();
          bVal = String(bVal).toLowerCase();
        }
        if (aVal < bVal) return sortConfig.direction === 'asc' ? -1 : 1;
        if (aVal > bVal) return sortConfig.direction === 'asc' ? 1 : -1;
        return 0;
      });
    }

    return result;
  }, [notifications, searchQuery, columnFilters, sortConfig]);

  // Pagination
  const totalPages = Math.ceil(filteredNews.length / pageSize) || 1;
  const paginatedNews = filteredNews.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  const getPageNumbers = () => {
    const pages = [];
    const max = 5;
    let start = Math.max(1, currentPage - Math.floor(max / 2));
    let end = Math.min(totalPages, start + max - 1);
    if (end - start < max - 1) start = Math.max(1, end - max + 1);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  };

  // Render sort icon
  const renderSortIcon = (column) => {
    if (sortConfig.column === column) {
      if (sortConfig.direction === 'asc') return <ArrowUp size={13} />;
      if (sortConfig.direction === 'desc') return <ArrowDown size={13} />;
    }
    return <ArrowUpDown size={13} />;
  };

  // Column header with sort + filter (BookingManage pattern)
  const renderColumnHeader = (column, label, isCenter = false) => {
    const hasFilter = !!columnFilters[column];
    return (
      <th key={column} className={isCenter ? 'center' : ''}>
        <div className="cio-th-content" style={isCenter ? { justifyContent: 'center' } : {}}>
          <span className="cio-th-label">{label}</span>
          <div className="cio-th-actions">
            <button
              className={`cio-th-btn${sortConfig.column === column ? ' active' : ''}`}
              onClick={(e) => { e.stopPropagation(); handleSort(column); }}
              title="Sắp xếp"
            >
              {renderSortIcon(column)}
            </button>
            <button
              className={`cio-th-btn${hasFilter ? ' active' : ''}${activeFilterCol === column ? ' open' : ''}`}
              onClick={(e) => {
                e.stopPropagation();
                setActiveFilterCol(prev => prev === column ? null : column);
              }}
              title="Lọc"
            >
              <Filter size={13} className={hasFilter ? 'cio-filter-active' : ''} />
            </button>
          </div>
          {activeFilterCol === column && (
            <div className="cio-filter-dropdown" ref={filterRef} onClick={e => e.stopPropagation()}>
              {column === 'status' ? (
                <div className="cio-filter-options">
                  {STATUS_OPTIONS.map(opt => (
                    <label key={opt.value} className="cio-filter-option">
                      <input
                        type="radio"
                        name="status-filter"
                        checked={columnFilters.status === opt.value}
                        onChange={() => { handleFilterChange('status', opt.value); setActiveFilterCol(null); }}
                      />
                      {opt.label}
                    </label>
                  ))}
                </div>
              ) : (
                <>
                  <input
                    type="text"
                    className="cio-filter-input"
                    placeholder={`Lọc ${label.toLowerCase()}...`}
                    value={columnFilters[column]}
                    onChange={(e) => handleFilterChange(column, e.target.value)}
                    autoFocus
                  />
                  {hasFilter && (
                    <button className="cio-filter-clear" onClick={() => clearColumnFilter(column)}>
                      <X size={12} /> Xóa lọc
                    </button>
                  )}
                </>
              )}
            </div>
          )}
        </div>
      </th>
    );
  };

  const activeFilterCount = Object.values(columnFilters).filter(Boolean).length;
  const visibleColumnCount = Object.values(visibleColumns).filter(Boolean).length + 1;

  // Selection logic
  const toggleSelect = (id) => {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const toggleSelectAll = () => {
    if (selectedIds.size === paginatedNews.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(paginatedNews.map(n => n.id)));
    }
  };

  const isAllSelected = paginatedNews.length > 0 && selectedIds.size === paginatedNews.length;

  const handleDeleteBatch = async () => {
    if (selectedIds.size === 0) return;
    const ok = await confirm({
      title: 'Xoá tin tức',
      message: `Bạn có chắc muốn xoá ${selectedIds.size} tin tức đã chọn?`,
      variant: 'danger',
      confirmText: 'Xoá',
      cancelText: 'Huỷ',
    });
    if (!ok) return;
    setDeleting(true);
    try {
      await batchDeleteNews(Array.from(selectedIds));
      toast.success(`Đã xoá ${selectedIds.size} tin tức thành công.`);
      setSelectedIds(new Set());
      loadNotifications();
    } catch (err) {
      toast.error('Không thể xoá tin tức: ' + (err.message || 'Lỗi không xác định'));
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="lib-container">
      {/* ================= LIST VIEW ================= */}
      {viewMode === 'list' && (
        <div className="nt-fade-in">
          <div className="lib-page-title">
            <h1>QUẢN LÝ TIN TỨC</h1>
          </div>

          <div className="lib-panel">
            {/* Toolbar */}
            <div className="cio-toolbar">
              <div className="lib-search">
                <Search size={16} className="lib-search-icon" />
                <input
                  type="text"
                  placeholder="Tìm tiêu đề, chủ đề..."
                  value={searchQuery}
                  onChange={(e) => { setSearchQuery(e.target.value); setCurrentPage(1); }}
                />
              </div>
              <div style={{ position: 'relative' }}>
                <button
                  className="cio-column-toggle"
                  onClick={() => setShowColumnMenu(!showColumnMenu)}
                >
                  <SlidersHorizontal size={14} />
                  Hiển thị cột
                </button>
                {showColumnMenu && (
                  <div className="cio-column-menu">
                    {[
                      { key: 'category', label: 'Chủ đề' },
                      { key: 'title', label: 'Tiêu đề' },
                      { key: 'status', label: 'Trạng thái' },
                      { key: 'publishedAt', label: 'Thời gian đăng' },
                      { key: 'actions', label: 'Thao tác' },
                    ].map(col => (
                      <label key={col.key} className="cio-column-menu-item">
                        <input
                          type="checkbox"
                          checked={visibleColumns[col.key]}
                          onChange={() => setVisibleColumns(prev => ({ ...prev, [col.key]: !prev[col.key] }))}
                          style={{ accentColor: '#FF751F' }}
                        />
                        {col.label}
                      </label>
                    ))}
                  </div>
                )}
              </div>

              {/* Batch delete */}
              {selectedIds.size > 0 && (
                <button className="sr-delete-btn" onClick={handleDeleteBatch} disabled={deleting}>
                  <Trash2 size={14} />
                  {deleting ? "Đang xoá..." : `Xoá (${selectedIds.size})`}
                </button>
              )}

              <span className="cio-result-count">
                {activeFilterCount > 0 && (
                  <span className="cio-active-filters">
                    {activeFilterCount} bộ lọc |{' '}
                  </span>
                )}
                Tổng số <strong>{filteredNews.length}</strong> kết quả
              </span>
              <button
                className="lib-btn primary"
                style={{ marginLeft: 'auto' }}
                onClick={() => navigate(`${basePath}/create`)}
              >
                <Plus size={16} /> Tạo mới
              </button>
            </div>

            {/* Table */}
            {loading ? (
              <div className="sm-loading">
                <Loader2 size={28} className="sm-spinner" />
                <span>Đang tải...</span>
              </div>
            ) : (
              <div className="bm-table-wrapper">
                <table className="bm-table">
                  <thead>
                    <tr>
                      <th className="sr-checkbox-col">
                        <input type="checkbox" checked={isAllSelected} onChange={toggleSelectAll} style={{ accentColor: '#FF751F' }} />
                      </th>
                      {visibleColumns.category && renderColumnHeader('category', 'Chủ đề')}
                      {visibleColumns.title && renderColumnHeader('title', 'Tiêu đề')}
                      {visibleColumns.status && renderColumnHeader('status', 'Trạng thái', true)}
                      {visibleColumns.publishedAt && renderColumnHeader('publishedAt', 'Thời gian đăng', true)}
                      {visibleColumns.actions && <th className="center"><span className="cio-th-label">Thao tác</span></th>}
                    </tr>
                  </thead>
                  <tbody>
                    {paginatedNews.length === 0 ? (
                      <tr>
                        <td colSpan={visibleColumnCount} className="bm-table-empty-cell">
                          {searchQuery ? `Không tìm thấy kết quả cho "${searchQuery}"` : 'Chưa có tin tức nào.'}
                        </td>
                      </tr>
                    ) : (
                      paginatedNews.map((item) => {
                        const statusKey = getStatusKey(item);
                        return (
                          <tr
                            key={item.id}
                            className={`bm-table-row${selectedIds.has(item.id) ? ' selected' : ''}`}
                            onClick={() => handleViewDetail(item)}
                          >
                            <td className="sr-checkbox-col" onClick={(e) => e.stopPropagation()}>
                              <input type="checkbox" checked={selectedIds.has(item.id)} onChange={() => toggleSelect(item.id)} style={{ accentColor: '#FF751F' }} />
                            </td>
                            {visibleColumns.category && (
                              <td>
                                <span className="nt-category-tag">{getSubjectFromCategory(item)}</span>
                              </td>
                            )}
                            {visibleColumns.title && (
                              <td>
                                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                                  <span style={{ fontWeight: 500, color: '#1e293b' }}>{item.title}</span>
                                  {item.isPinned && <span className="nt-pin-badge">Ghim</span>}
                                </div>
                              </td>
                            )}
                            {visibleColumns.status && (
                              <td className="center">
                                <span className="sr-status-text">
                                  <span className="sr-status-dot" style={{ background: statusKey === 'published' ? '#22c55e' : statusKey === 'scheduled' ? '#3b82f6' : '#94a3b8' }} />
                                  {getStatusLabel(item)}
                                </span>
                              </td>
                            )}
                            {visibleColumns.publishedAt && (
                              <td className="center" style={{ color: '#64748b', fontSize: 13 }}>
                                {formatDateTime(item.publishedAt || item.createdAt)}
                              </td>
                            )}
                            {visibleColumns.actions && (
                              <td className="center">
                                <div style={{ display: 'flex', gap: 8, justifyContent: 'center' }}>
                                  <button
                                    className="cio-th-btn"
                                    style={{ width: 32, height: 32, color: item.isPinned ? '#92400e' : '#64748b' }}
                                    onClick={(e) => handleTogglePin(item.id, e)}
                                    title={item.isPinned ? 'Bỏ ghim' : 'Ghim'}
                                  >
                                    <Pin size={15} />
                                  </button>
                                  <button
                                    className="cio-th-btn"
                                    style={{ width: 32, height: 32, color: '#059669' }}
                                    onClick={(e) => { e.stopPropagation(); navigate(`${basePath}/edit/${item.id}`); }}
                                  >
                                    <Pencil size={15} />
                                  </button>
                                  <button
                                    className="cio-th-btn"
                                    style={{ width: 32, height: 32, color: '#dc2626' }}
                                    onClick={(e) => handleDelete(item.id, e)}
                                  >
                                    <Trash2 size={15} />
                                  </button>
                                </div>
                              </td>
                            )}
                          </tr>
                        );
                      })
                    )}
                  </tbody>
                </table>
              </div>
            )}

            {/* Pagination */}
            <div className="cio-pagination">
              <div className="cio-page-size">
                <span>Hiển thị</span>
                <select value={pageSize} onChange={e => { setPageSize(Number(e.target.value)); setCurrentPage(1); }}>
                  <option value={10}>10</option>
                  <option value={20}>20</option>
                  <option value={50}>50</option>
                </select>
                <span>dòng</span>
              </div>
              <div className="cio-pagination-right">
                <button className="cio-page-btn" disabled={currentPage <= 1} onClick={() => setCurrentPage(1)}>«</button>
                <button className="cio-page-btn" disabled={currentPage <= 1} onClick={() => setCurrentPage(p => p - 1)}>‹</button>
                <div className="cio-page-numbers">
                  {getPageNumbers().map(p => (
                    <button key={p} className={`cio-page-btn${p === currentPage ? ' active' : ''}`} onClick={() => setCurrentPage(p)}>{p}</button>
                  ))}
                </div>
                <button className="cio-page-btn" disabled={currentPage >= totalPages} onClick={() => setCurrentPage(p => p + 1)}>›</button>
                <button className="cio-page-btn" disabled={currentPage >= totalPages} onClick={() => setCurrentPage(totalPages)}>»</button>
              </div>
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
            <button className="nt-btn-draft">Lưu nháp</button>
            <button
              className="nt-btn-submit"
              disabled={!formData.title}
            >
              Đăng tin
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
                  <span style={{ color: '#10b981' }}>Đã đăng</span>
                ) : (
                  <span style={{ color: '#6b7280' }}>Nháp</span>
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
