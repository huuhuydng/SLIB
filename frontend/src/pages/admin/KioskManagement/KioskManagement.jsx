import React, { useState, useEffect, useMemo, useCallback, useRef } from 'react';
import {
  Monitor,
  Power,
  ShieldOff,
  RefreshCw,
  Loader2,
  CheckCircle,
  AlertTriangle,
  MapPin,
  Info,
  X,
  Search,
  ArrowUpDown,
  ArrowUp,
  ArrowDown,
  Filter,
  SlidersHorizontal,
  LayoutGrid,
  LayoutList,
  Clock,
  Activity,
  Plus,
  Edit,
  Settings,
  Trash2,
  Copy,
  ExternalLink
} from 'lucide-react';
import '../../../styles/librarian/librarian-shared.css';
import '../../../styles/librarian/CheckInOut.css';
import '../../../styles/admin/HceStationManagement.css';
import '../UserManagement/UserManagement.css';

import { API_BASE_URL as API_BASE } from '../../../config/apiConfig';

const getAdminToken = () =>
  localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');

const RUNTIME_STATUS = {
  READY: { label: 'Sẵn sàng', css: 'online' },
  STALE: { label: 'Mất kết nối', css: 'offline' },
  PENDING: { label: 'Chưa kết nối', css: 'inactive' },
  TOKEN_EXPIRED: { label: 'Token hết hạn', css: 'offline' },
  INACTIVE: { label: 'Chưa kích hoạt', css: 'inactive' },
  DISABLED: { label: 'Đã vô hiệu hóa', css: 'inactive' },
};

const STATUS_OPTIONS = [
  { value: '', label: 'Tất cả' },
  { value: 'READY', label: 'Sẵn sàng' },
  { value: 'STALE', label: 'Mất kết nối' },
  { value: 'PENDING', label: 'Chưa kết nối' },
  { value: 'TOKEN_EXPIRED', label: 'Token hết hạn' },
  { value: 'INACTIVE', label: 'Chưa kích hoạt' },
  { value: 'DISABLED', label: 'Đã vô hiệu hóa' },
];

const KIOSK_TYPE_MAP = {
  INTERACTIVE: { label: 'Tương tác', color: '#2563EB', bg: '#DBEAFE' },
  MONITORING: { label: 'Giám sát', color: '#7C3AED', bg: '#F3E8FF' },
};

const TYPE_OPTIONS = [
  { value: '', label: 'Tất cả' },
  { value: 'INTERACTIVE', label: 'Tương tác' },
  { value: 'MONITORING', label: 'Giám sát' },
];

const authHeaders = () => {
  const token = getAdminToken();
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
};

function KioskManagement() {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [lastUpdatedAt, setLastUpdatedAt] = useState(null);
  const [searchText, setSearchText] = useState('');
  const [toast, setToast] = useState(null);
  const [activatingId, setActivatingId] = useState(null);
  const [activationResult, setActivationResult] = useState(null); // { activationCode, activationUrl, kioskCode }
  const [revokeTarget, setRevokeTarget] = useState(null);
  const [revoking, setRevoking] = useState(false);
  const [copied, setCopied] = useState(null); // 'code' | 'url'

  // CRUD modals
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [selectedKiosk, setSelectedKiosk] = useState(null);
  const [formData, setFormData] = useState({
    kioskCode: '', kioskName: '', kioskType: '', location: ''
  });
  const [formLoading, setFormLoading] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);

  // View mode
  const [viewMode, setViewMode] = useState('table');

  // Pagination
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);

  // Sort
  const [sortConfig, setSortConfig] = useState({ column: null, direction: null });

  // Column filters
  const [columnFilters, setColumnFilters] = useState({
    kioskCode: '',
    kioskType: '',
    location: '',
    status: '',
  });
  const [activeFilterCol, setActiveFilterCol] = useState(null);
  const filterRef = useRef(null);

  // Column visibility
  const [visibleColumns, setVisibleColumns] = useState({
    kioskCode: true,
    kioskType: true,
    location: true,
    status: true,
    expiresAt: true,
    lastActive: true,
  });
  const [showColumnMenu, setShowColumnMenu] = useState(false);

  const showToast = useCallback((message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  }, []);

  const fetchSessions = useCallback(async ({ silent = false } = {}) => {
    if (!silent) {
      setLoading(true);
    }
    setError(null);
    try {
      const res = await fetch(`${API_BASE}/slib/kiosk/admin/sessions`, {
        headers: authHeaders(),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message || 'Không thể tải danh sách kiosk');
      }
      const data = await res.json();
      setSessions(Array.isArray(data) ? data : (data.content || data.data || []));
      setLastUpdatedAt(new Date());
    } catch (err) {
      setError(err.message || 'Lỗi kết nối đến máy chủ');
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    fetchSessions();
  }, [fetchSessions]);

  useEffect(() => {
    const timer = setInterval(() => {
      fetchSessions({ silent: true });
    }, 30000);

    return () => clearInterval(timer);
  }, [fetchSessions]);

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

  // ========== CRUD HANDLERS ==========

  const resetForm = () => {
    setFormData({ kioskCode: '', kioskName: '', kioskType: '', location: '' });
  };

  const handleCreate = async () => {
    if (!formData.kioskCode.trim() || !formData.kioskName.trim() || !formData.kioskType) {
      showToast('Vui lòng điền đầy đủ thông tin bắt buộc', 'error');
      return;
    }
    setFormLoading(true);
    try {
      const res = await fetch(`${API_BASE}/slib/kiosk/admin/kiosks`, {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify(formData),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message || 'Lỗi tạo kiosk');
      }
      showToast('Tạo kiosk thành công!');
      setShowCreateModal(false);
      resetForm();
      fetchSessions();
    } catch (err) {
      showToast(err.message || 'Lỗi tạo kiosk', 'error');
    } finally {
      setFormLoading(false);
    }
  };

  const handleUpdate = async () => {
    if (!selectedKiosk) return;
    setFormLoading(true);
    try {
      const res = await fetch(`${API_BASE}/slib/kiosk/admin/kiosks/${selectedKiosk.id}`, {
        method: 'PUT',
        headers: authHeaders(),
        body: JSON.stringify({
          kioskName: formData.kioskName,
          kioskType: formData.kioskType,
          location: formData.location,
        }),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message || 'Lỗi cập nhật kiosk');
      }
      showToast('Cập nhật kiosk thành công!');
      setShowEditModal(false);
      resetForm();
      fetchSessions();
    } catch (err) {
      showToast(err.message || 'Lỗi cập nhật kiosk', 'error');
    } finally {
      setFormLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!selectedKiosk) return;
    setDeleteLoading(true);
    try {
      const res = await fetch(`${API_BASE}/slib/kiosk/admin/kiosks/${selectedKiosk.id}`, {
        method: 'DELETE',
        headers: authHeaders(),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message || 'Lỗi xóa kiosk');
      }
      showToast('Đã xóa kiosk thành công!');
      setShowDeleteModal(false);
      setShowDetailModal(false);
      setSelectedKiosk(null);
      fetchSessions();
    } catch (err) {
      showToast(err.message || 'Lỗi xóa kiosk', 'error');
    } finally {
      setDeleteLoading(false);
    }
  };

  const openDetail = (session) => {
    setSelectedKiosk(session);
    setShowDetailModal(true);
  };

  const openEdit = (session) => {
    setSelectedKiosk(session);
    setFormData({
      kioskCode: session.kioskCode || '',
      kioskName: session.kioskName || '',
      kioskType: session.kioskType || '',
      location: session.location || '',
    });
    setShowEditModal(true);
  };

  const openDeleteConfirm = (session) => {
    setSelectedKiosk(session);
    setShowDeleteModal(true);
  };

  // ========== TOKEN HANDLERS ==========

  const handleActivate = async (session) => {
    const kioskId = session.id;
    const forceReissue = session?.tokenValid && window.confirm(
      `Kiosk ${session.kioskCode} đang có token còn hiệu lực. Cấp lại token sẽ làm token cũ mất hiệu lực.\n\nBạn có chắc muốn tiếp tục không?`
    );

    if (session?.tokenValid && !forceReissue) {
      return;
    }

    setActivatingId(kioskId);
    try {
      const res = await fetch(`${API_BASE}/slib/kiosk/admin/token/${encodeURIComponent(kioskId)}?force=${forceReissue ? 'true' : 'false'}`, {
        method: 'POST',
        headers: authHeaders(),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message || 'Không thể tạo token kiosk');
      }
      const data = await res.json();
      setActivationResult({
        activationCode: data.activationCode,
        activationUrl: data.activationUrl,
        kioskCode: data.kioskCode,
      });
      setCopied(null);
      await fetchSessions();
    } catch (err) {
      showToast(err.message || 'Lỗi kích hoạt kiosk', 'error');
    } finally {
      setActivatingId(null);
    }
  };

  const handleCopy = async (text, type) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(type);
      setTimeout(() => setCopied(null), 2000);
    } catch {
      showToast('Không thể copy', 'error');
    }
  };

  const handleRevokeConfirm = async () => {
    if (!revokeTarget) return;
    setRevoking(true);
    try {
      const res = await fetch(`${API_BASE}/slib/kiosk/admin/token/${encodeURIComponent(revokeTarget.id)}`, {
        method: 'DELETE',
        headers: authHeaders(),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message || 'Không thể thu hồi token kiosk');
      }
      showToast(`Đã thu hồi token kiosk ${revokeTarget.kioskCode || revokeTarget.id}`);
      setRevokeTarget(null);
      await fetchSessions();
    } catch (err) {
      showToast(err.message || 'Lỗi thu hồi token', 'error');
    } finally {
      setRevoking(false);
    }
  };

  // ========== HELPERS ==========

  const getRuntimeStatus = (session) => {
    return session.runtimeStatus || 'INACTIVE';
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '—';
    try {
      return new Date(dateStr).toLocaleString('vi-VN');
    } catch {
      return '—';
    }
  };

  const formatTime = (dateStr) => {
    if (!dateStr) return 'Chưa có';
    const date = new Date(dateStr);
    const now = new Date();
    const diffSec = Math.floor((now - date) / 1000);
    if (diffSec < 60) return 'Vừa xong';
    if (diffSec < 3600) return `${Math.floor(diffSec / 60)} phút trước`;
    if (diffSec < 86400) return `${Math.floor(diffSec / 3600)} giờ trước`;
    return `${Math.floor(diffSec / 86400)} ngày trước`;
  };

  const statusConfig = (session) => {
    const key = getRuntimeStatus(session);
    return RUNTIME_STATUS[key] || RUNTIME_STATUS.INACTIVE;
  };

  // ========== SORT / FILTER / PAGINATION ==========

  const getSessionValue = useCallback((session, column) => {
    switch (column) {
      case 'kioskCode': return session.kioskCode || session.id || '';
      case 'kioskType': return KIOSK_TYPE_MAP[session.kioskType]?.label || session.kioskType || '';
      case 'location': return session.location || '';
      case 'status': return getRuntimeStatus(session);
      case 'expiresAt': return session.deviceTokenExpiresAt || '';
      case 'lastActive': return session.lastActiveAt || '';
      default: return '';
    }
  }, []);

  const filteredSessions = useMemo(() => {
    let list = [...sessions];

    const q = searchText.trim().toLowerCase();
    if (q) {
      list = list.filter(s =>
        (s.kioskCode || '').toLowerCase().includes(q) ||
        (s.kioskName || '').toLowerCase().includes(q) ||
        (s.location || '').toLowerCase().includes(q) ||
        (s.id || '').toString().toLowerCase().includes(q)
      );
    }

    Object.entries(columnFilters).forEach(([col, filterVal]) => {
      if (!filterVal) return;
      const fq = filterVal.toLowerCase();

      if (col === 'status') {
        list = list.filter(s => getRuntimeStatus(s) === filterVal);
      } else if (col === 'kioskType') {
        list = list.filter(s => s.kioskType === filterVal);
      } else if (col === 'kioskCode') {
        list = list.filter(s =>
          (s.kioskCode || '').toLowerCase().includes(fq) ||
          (s.kioskName || '').toLowerCase().includes(fq)
        );
      } else if (col === 'location') {
        list = list.filter(s => (s.location || '').toLowerCase().includes(fq));
      }
    });

    if (sortConfig.column && sortConfig.direction) {
      list.sort((a, b) => {
        let valA = getSessionValue(a, sortConfig.column).toString().toLowerCase();
        let valB = getSessionValue(b, sortConfig.column).toString().toLowerCase();
        if (valA < valB) return sortConfig.direction === 'asc' ? -1 : 1;
        if (valA > valB) return sortConfig.direction === 'asc' ? 1 : -1;
        return 0;
      });
    }

    return list;
  }, [sessions, searchText, columnFilters, sortConfig, getSessionValue]);

  const totalPages = Math.ceil(filteredSessions.length / itemsPerPage);
  const paginatedSessions = useMemo(() => {
    const start = (currentPage - 1) * itemsPerPage;
    return filteredSessions.slice(start, start + itemsPerPage);
  }, [filteredSessions, currentPage, itemsPerPage]);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchText, columnFilters, sortConfig, itemsPerPage]);

  const handleSort = (column) => {
    setSortConfig(prev => {
      if (prev.column !== column) return { column, direction: 'asc' };
      if (prev.direction === 'asc') return { column, direction: 'desc' };
      return { column: null, direction: null };
    });
  };

  const handleFilterChange = (column, value) => {
    setColumnFilters(prev => ({ ...prev, [column]: value }));
  };

  const clearColumnFilter = (column) => {
    setColumnFilters(prev => ({ ...prev, [column]: '' }));
    setActiveFilterCol(null);
  };

  const getPageNumbers = () => {
    if (totalPages <= 7) return Array.from({ length: totalPages }, (_, i) => i + 1);
    const pages = [];
    if (currentPage <= 4) {
      for (let i = 1; i <= 5; i++) pages.push(i);
      pages.push('...');
      pages.push(totalPages);
    } else if (currentPage >= totalPages - 3) {
      pages.push(1);
      pages.push('...');
      for (let i = totalPages - 4; i <= totalPages; i++) pages.push(i);
    } else {
      pages.push(1);
      pages.push('...');
      for (let i = currentPage - 1; i <= currentPage + 1; i++) pages.push(i);
      pages.push('...');
      pages.push(totalPages);
    }
    return pages;
  };

  const renderSortIcon = (column) => {
    if (sortConfig.column === column) {
      if (sortConfig.direction === 'asc') return <ArrowUp size={13} />;
      if (sortConfig.direction === 'desc') return <ArrowDown size={13} />;
    }
    return <ArrowUpDown size={13} />;
  };

  const renderColumnHeader = (column, label) => {
    const hasFilter = !!columnFilters[column];
    const filterable = ['kioskCode', 'kioskType', 'location', 'status'].includes(column);
    return (
      <th key={column}>
        <div className="cio-th-content">
          <span className="cio-th-label">{label}</span>
          <div className="cio-th-actions">
            <button
              className={`cio-th-btn${sortConfig.column === column ? ' active' : ''}`}
              onClick={(e) => { e.stopPropagation(); handleSort(column); }}
              title="Sắp xếp"
            >
              {renderSortIcon(column)}
            </button>
            {filterable && (
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
            )}
          </div>
          {activeFilterCol === column && (
            <div className="cio-filter-dropdown" ref={filterRef} onClick={e => e.stopPropagation()}>
              {column === 'status' ? (
                <select
                  value={columnFilters.status}
                  onChange={(e) => { handleFilterChange('status', e.target.value); setActiveFilterCol(null); }}
                  autoFocus
                  className="cio-filter-input"
                >
                  {STATUS_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                </select>
              ) : column === 'kioskType' ? (
                <select
                  value={columnFilters.kioskType}
                  onChange={(e) => { handleFilterChange('kioskType', e.target.value); setActiveFilterCol(null); }}
                  autoFocus
                  className="cio-filter-input"
                >
                  {TYPE_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                </select>
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

  // ========== RENDER HELPERS ==========

  const renderTypeBadge = (kioskType) => {
    const info = KIOSK_TYPE_MAP[kioskType] || { label: kioskType, color: '#6B7280', bg: '#F3F4F6' };
    return (
      <span className="hce-badge" style={{ background: info.bg, color: info.color }}>
        {info.label}
      </span>
    );
  };

  const handleFormChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const renderForm = (isEdit = false) => (
    <>
      <div className="hce-form-group">
        <label className="hce-form-label">Mã kiosk *</label>
        <input
          type="text"
          className="hce-form-input"
          placeholder="VD: KIOSK_001"
          value={formData.kioskCode}
          onChange={e => handleFormChange('kioskCode', e.target.value)}
          disabled={isEdit}
        />
        {!isEdit && (
          <div className="hce-form-helper hce-form-helper--info">
            <Info size={14} style={{ flexShrink: 0, marginTop: 1 }} />
            <span>Mã kiosk là duy nhất, không thể thay đổi sau khi tạo</span>
          </div>
        )}
      </div>
      <div className="hce-form-group">
        <label className="hce-form-label">Tên kiosk *</label>
        <input
          type="text"
          className="hce-form-input"
          placeholder="VD: Kiosk Sảnh A - Tầng 1"
          value={formData.kioskName}
          onChange={e => handleFormChange('kioskName', e.target.value)}
        />
      </div>
      <div className="hce-form-group">
        <label className="hce-form-label">Loại kiosk *</label>
        <select
          className="hce-form-input"
          value={formData.kioskType}
          onChange={e => handleFormChange('kioskType', e.target.value)}
        >
          <option value="">Chọn loại kiosk</option>
          <option value="INTERACTIVE">Tương tác</option>
          <option value="MONITORING">Giám sát</option>
        </select>
      </div>
      <div className="hce-form-group">
        <label className="hce-form-label">Vị trí</label>
        <input
          type="text"
          className="hce-form-input"
          placeholder="VD: Sảnh tầng 1"
          value={formData.location}
          onChange={e => handleFormChange('location', e.target.value)}
        />
      </div>
    </>
  );

  return (
    <>
      {/* Toast */}
      {toast && (
        <div className={`hce-toast hce-toast--${toast.type}`}>
          {toast.type === 'success' ? <CheckCircle size={18} /> : <AlertTriangle size={18} />}
          {toast.message}
        </div>
      )}

      <div className="lib-container">
        <div className="lib-page-title" style={{ marginBottom: '20px' }}>
          <h1>QUẢN LÝ KIOSK</h1>
        </div>

        <div className="lib-panel">
          {/* Toolbar */}
          <div className="cio-toolbar">
            <div className="lib-search">
              <Search size={16} className="lib-search-icon" />
              <input
                type="text"
                placeholder="Tìm mã kiosk, tên, vị trí..."
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
              />
            </div>

            {/* View toggle */}
            <div className="sr-view-toggle">
              <button
                className={`sr-view-btn${viewMode === 'table' ? ' active' : ''}`}
                onClick={() => setViewMode('table')}
                title="Dạng bảng"
              >
                <LayoutList size={16} />
              </button>
              <button
                className={`sr-view-btn${viewMode === 'card' ? ' active' : ''}`}
                onClick={() => setViewMode('card')}
                title="Dạng thẻ"
              >
                <LayoutGrid size={16} />
              </button>
            </div>

            {viewMode === 'table' && (
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
                      { key: 'kioskCode', label: 'Thiết bị' },
                      { key: 'kioskType', label: 'Loại' },
                      { key: 'location', label: 'Vị trí' },
                      { key: 'status', label: 'Trạng thái vận hành' },
                      { key: 'expiresAt', label: 'Hết hạn' },
                      { key: 'lastActive', label: 'Hoạt động cuối' },
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
            )}

            <span className="cio-result-count">
              {activeFilterCount > 0 && (
                <span className="cio-active-filters">{activeFilterCount} bộ lọc |{' '}</span>
              )}
              Tổng số <strong>{filteredSessions.length}</strong> kết quả
            </span>

            <div className="cio-toolbar-right">
              {lastUpdatedAt && (
                <span className="cio-result-count" style={{ marginRight: 4 }}>
                  Cập nhật: <strong>{lastUpdatedAt.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}</strong>
                </span>
              )}
              <button className="um-toolbar-btn" onClick={fetchSessions} disabled={loading}>
                <RefreshCw size={14} className={loading ? 'sm-spinner' : ''} />
                Làm mới
              </button>
              <button className="um-toolbar-btn primary" onClick={() => { resetForm(); setShowCreateModal(true); }}>
                <Plus size={14} />
                Thêm kiosk
              </button>
            </div>
          </div>

          {/* Content */}
          {loading ? (
            <div className="sm-loading" style={{ padding: '60px', textAlign: 'center' }}>
              <Loader2 size={28} className="sm-spinner" />
              <span style={{ display: 'block', marginTop: '12px', color: '#64748b' }}>
                Đang tải danh sách kiosk...
              </span>
            </div>
          ) : error ? (
            <div className="hce-station-empty">
              <AlertTriangle size={48} color="#DC2626" className="hce-station-empty__icon" />
              <div className="hce-station-empty__title">{error}</div>
              <button className="um-toolbar-btn" onClick={fetchSessions}>
                <RefreshCw size={14} />
                Thử lại
              </button>
            </div>
          ) : viewMode === 'table' ? (
            /* ========== TABLE VIEW ========== */
            <div className="sr-table-wrapper">
              <table className="sr-table">
                <thead>
                  <tr>
                    {visibleColumns.kioskCode && renderColumnHeader('kioskCode', 'Thiết bị')}
                    {visibleColumns.kioskType && renderColumnHeader('kioskType', 'Loại')}
                    {visibleColumns.location && renderColumnHeader('location', 'Vị trí')}
                    {visibleColumns.status && renderColumnHeader('status', 'Trạng thái vận hành')}
                    {visibleColumns.expiresAt && renderColumnHeader('expiresAt', 'Hết hạn lúc')}
                    {visibleColumns.lastActive && renderColumnHeader('lastActive', 'Hoạt động cuối')}
                    <th style={{ textAlign: 'center' }}>
                      <div className="cio-th-content" style={{ justifyContent: 'center' }}>
                        <span className="cio-th-label">Thao tác</span>
                      </div>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {paginatedSessions.length === 0 ? (
                    <tr>
                      <td colSpan={visibleColumnCount} className="sr-table-empty-cell">
                        {searchText || activeFilterCount > 0
                          ? 'Không tìm thấy kiosk phù hợp.'
                          : 'Chưa có kiosk nào. Nhấn "Thêm kiosk" để tạo mới.'}
                      </td>
                    </tr>
                  ) : (
                    paginatedSessions.map((session) => {
                      const status = statusConfig(session);
                      const kioskId = session.id;
                      const isActivating = activatingId === kioskId;
                      const runtimeStatusKey = getRuntimeStatus(session);
                      const typeInfo = KIOSK_TYPE_MAP[session.kioskType] || { label: session.kioskType, color: '#6B7280', bg: '#F3F4F6' };

                      return (
                        <tr key={kioskId} className="sr-table-row" onClick={() => openDetail(session)}>
                          {visibleColumns.kioskCode && (
                            <td>
                              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                                <div style={{
                                  width: 32, height: 32, borderRadius: 8,
                                  background: typeInfo.bg,
                                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                                  flexShrink: 0
                                }}>
                                  <Monitor size={16} color={typeInfo.color} />
                                </div>
                                <div>
                                  <div style={{ fontWeight: 600, fontSize: 13, color: '#0f172a' }}>
                                    {session.kioskCode}
                                  </div>
                                  {session.kioskName && (
                                    <div style={{ fontSize: 11, color: '#94a3b8' }}>
                                      {session.kioskName}
                                    </div>
                                  )}
                                </div>
                              </div>
                            </td>
                          )}
                          {visibleColumns.kioskType && (
                            <td>{renderTypeBadge(session.kioskType)}</td>
                          )}
                          {visibleColumns.location && (
                            <td style={{ fontSize: 13, color: '#475569' }}>
                              {session.location || '—'}
                            </td>
                          )}
                          {visibleColumns.status && (
                            <td>
                              <span className={`hce-badge hce-badge--${status.css}`}>
                                {status.label}
                              </span>
                            </td>
                          )}
                          {visibleColumns.expiresAt && (
                            <td style={{ fontSize: 13, color: '#475569' }}>
                              {formatDate(session.deviceTokenExpiresAt)}
                            </td>
                          )}
                          {visibleColumns.lastActive && (
                            <td style={{ fontSize: 13, color: '#475569' }}>
                              {formatDate(session.lastActiveAt)}
                            </td>
                          )}
                          <td style={{ textAlign: 'center' }} onClick={e => e.stopPropagation()}>
                            <div style={{ display: 'flex', gap: 6, justifyContent: 'center' }}>
                              <button
                                className="um-toolbar-btn primary"
                                style={{ fontSize: 12, padding: '6px 12px' }}
                                onClick={() => handleActivate(session)}
                                disabled={isActivating}
                                title="Kích hoạt token"
                              >
                                {isActivating
                                  ? <><Loader2 size={13} className="sm-spinner" /> Đang xử lý...</>
                                  : <><Power size={13} /> Kích hoạt</>
                                }
                              </button>
                              {session.hasDeviceToken && (
                                <button
                                  className="um-toolbar-btn"
                                  style={{ fontSize: 12, padding: '6px 12px', color: '#DC2626', borderColor: '#FCA5A5' }}
                                  onClick={() => setRevokeTarget(session)}
                                  title="Thu hồi token"
                                >
                                  <ShieldOff size={13} />
                                  Thu hồi
                                </button>
                              )}
                              <button className="um-action-btn" title="Chỉnh sửa" onClick={() => openEdit(session)}>
                                <Edit size={14} color="#64748b" />
                              </button>
                              <button className="um-action-btn" title="Xóa" onClick={() => openDeleteConfirm(session)}>
                                <Trash2 size={14} color="#ef4444" />
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          ) : (
            /* ========== CARD VIEW ========== */
            <div className="hce-station-grid">
              {paginatedSessions.length === 0 ? (
                <div className="hce-station-empty">
                  <Monitor size={48} className="hce-station-empty__icon" />
                  <div className="hce-station-empty__title">
                    {searchText || activeFilterCount > 0
                      ? 'Không tìm thấy kiosk phù hợp'
                      : 'Chưa có kiosk nào'}
                  </div>
                  <div className="hce-station-empty__text">
                    Nhấn "Thêm kiosk" để đăng ký một kiosk mới
                  </div>
                </div>
              ) : (
                paginatedSessions.map((session) => {
                  const status = statusConfig(session);
                  const kioskId = session.id;
                  const isActivating = activatingId === kioskId;
                  const typeInfo = KIOSK_TYPE_MAP[session.kioskType] || { label: session.kioskType, color: '#6B7280', bg: '#F3F4F6' };

                  return (
                    <div key={kioskId} className="hce-station-card" onClick={() => openDetail(session)}>
                      <div className="hce-station-card__header">
                        <div className="hce-station-card__info">
                          <div className="hce-station-card__icon-wrap" style={{ background: typeInfo.bg }}>
                            <Monitor size={22} color={typeInfo.color} />
                          </div>
                          <div>
                            <div className="hce-station-card__name">{session.kioskCode}</div>
                            {session.kioskName && (
                              <div className="hce-station-card__device-id">{session.kioskName}</div>
                            )}
                          </div>
                        </div>
                        <div className="hce-station-card__badges">
                          <span className="hce-badge" style={{ background: typeInfo.bg, color: typeInfo.color }}>
                            {typeInfo.label}
                          </span>
                          <span className={`hce-badge hce-badge--${status.css}`}>
                            {status.label}
                          </span>
                        </div>
                      </div>

                      <div className="hce-station-card__meta">
                        {session.location && (
                          <div className="hce-station-card__meta-row">
                            <MapPin size={14} className="hce-station-card__meta-icon" />
                            <span className="hce-station-card__meta-text">{session.location}</span>
                          </div>
                        )}
                        <div className="hce-station-card__meta-row">
                          <Clock size={14} className="hce-station-card__meta-icon" />
                          <span className="hce-station-card__meta-text hce-station-card__meta-text--muted">
                            Hết hạn token: {formatDate(session.deviceTokenExpiresAt)}
                          </span>
                        </div>
                        <div className="hce-station-card__meta-row">
                          <Activity size={14} className="hce-station-card__meta-icon" />
                          <span className="hce-station-card__meta-text hce-station-card__meta-text--muted">
                            Hoạt động cuối: {formatTime(session.lastActiveAt)}
                          </span>
                        </div>
                      </div>

                      <div className="hce-station-card__actions">
                        <button
                          className="hce-station-card__action-btn"
                          onClick={e => { e.stopPropagation(); handleActivate(session); }}
                          disabled={isActivating}
                          style={isActivating ? { opacity: 0.6 } : { color: '#e8600a', borderColor: '#fed7aa' }}
                        >
                          {isActivating
                            ? <><Loader2 size={14} className="sm-spinner" /> Đang xử lý...</>
                            : <><Power size={14} /> Kích hoạt</>
                          }
                        </button>
                        <button
                          className="hce-station-card__action-btn"
                          onClick={e => { e.stopPropagation(); openEdit(session); }}
                        >
                          <Edit size={14} /> Chỉnh sửa
                        </button>
                        <button
                          className="hce-station-card__action-btn"
                          onClick={e => { e.stopPropagation(); openDeleteConfirm(session); }}
                          style={{ color: '#ef4444', borderColor: '#fecaca' }}
                        >
                          <Trash2 size={14} /> Xóa
                        </button>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          )}

          {/* Pagination */}
          <div className="cio-pagination">
            <div className="cio-page-size">
              <span>Số hàng mỗi trang:</span>
              <select value={itemsPerPage} onChange={(e) => setItemsPerPage(Number(e.target.value))}>
                <option value={10}>10</option>
                <option value={20}>20</option>
                <option value={50}>50</option>
              </select>
            </div>
            {totalPages > 1 && (
              <div className="cio-pagination-right">
                <button onClick={() => setCurrentPage(prev => Math.max(1, prev - 1))} disabled={currentPage === 1} className="cio-page-btn">&lt;</button>
                <div className="cio-page-numbers">
                  {getPageNumbers().map((page, idx) => (
                    page === '...' ? (
                      <span key={`ellipsis-${idx}`} className="cio-page-ellipsis">...</span>
                    ) : (
                      <button key={page} onClick={() => setCurrentPage(page)} className={`cio-page-btn ${currentPage === page ? 'active' : ''}`}>{page}</button>
                    )
                  ))}
                </div>
                <button onClick={() => setCurrentPage(prev => Math.min(totalPages, prev + 1))} disabled={currentPage === totalPages} className="cio-page-btn">&gt;</button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ========== CREATE MODAL ========== */}
      {showCreateModal && (
        <div className="hce-modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="hce-modal" onClick={e => e.stopPropagation()}>
            <div className="hce-modal__header">
              <h2 className="hce-modal__title">Thêm kiosk mới</h2>
              <button className="hce-modal__close" onClick={() => setShowCreateModal(false)}>
                <X size={20} color="#4A5568" />
              </button>
            </div>
            <div className="hce-modal__body">
              {renderForm(false)}
            </div>
            <div className="hce-modal__footer">
              <button className="hce-btn hce-btn--secondary" style={{ flex: 1 }} onClick={() => setShowCreateModal(false)}>
                Hủy
              </button>
              <button className="hce-btn hce-btn--primary" style={{ flex: 1 }} onClick={handleCreate} disabled={formLoading}>
                {formLoading ? 'Đang tạo...' : 'Thêm kiosk'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========== EDIT MODAL ========== */}
      {showEditModal && selectedKiosk && (
        <div className="hce-modal-overlay" onClick={() => setShowEditModal(false)}>
          <div className="hce-modal" onClick={e => e.stopPropagation()}>
            <div className="hce-modal__header">
              <h2 className="hce-modal__title">Chỉnh sửa kiosk</h2>
              <button className="hce-modal__close" onClick={() => setShowEditModal(false)}>
                <X size={20} color="#4A5568" />
              </button>
            </div>
            <div className="hce-modal__body">
              {renderForm(true)}
            </div>
            <div className="hce-modal__footer">
              <button className="hce-btn hce-btn--secondary" style={{ flex: 1 }} onClick={() => setShowEditModal(false)}>
                Hủy
              </button>
              <button className="hce-btn hce-btn--primary" style={{ flex: 1 }} onClick={handleUpdate} disabled={formLoading}>
                {formLoading ? 'Đang cập nhật...' : 'Lưu thay đổi'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========== DETAIL MODAL ========== */}
      {showDetailModal && selectedKiosk && (
        <div className="hce-modal-overlay" onClick={() => setShowDetailModal(false)}>
          <div className="hce-modal" onClick={e => e.stopPropagation()}>
            <div className="hce-modal__header">
              <h2 className="hce-modal__title">Chi tiết kiosk</h2>
              <button className="hce-modal__close" onClick={() => setShowDetailModal(false)}>
                <X size={20} color="#4A5568" />
              </button>
            </div>
            <div className="hce-modal__body">
              {/* Status badges */}
              <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
                {renderTypeBadge(selectedKiosk.kioskType)}
                <span className={`hce-badge hce-badge--${statusConfig(selectedKiosk).css}`}>
                  {statusConfig(selectedKiosk).label}
                </span>
                <span className={`hce-badge hce-badge--${selectedKiosk.isActive ? 'active' : 'inactive'}`}>
                  {selectedKiosk.isActive ? 'Đang bật' : 'Đã tắt'}
                </span>
              </div>

              <div className="hce-detail-panel">
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Mã kiosk</span>
                  <span className="hce-detail-row__value" style={{ fontFamily: 'monospace' }}>
                    {selectedKiosk.kioskCode}
                  </span>
                </div>
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Tên kiosk</span>
                  <span className="hce-detail-row__value">{selectedKiosk.kioskName || '—'}</span>
                </div>
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Loại</span>
                  <span className="hce-detail-row__value">
                    {KIOSK_TYPE_MAP[selectedKiosk.kioskType]?.label || selectedKiosk.kioskType}
                  </span>
                </div>
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Vị trí</span>
                  <span className="hce-detail-row__value">{selectedKiosk.location || '—'}</span>
                </div>
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Token hết hạn</span>
                  <span className="hce-detail-row__value">
                    {formatDate(selectedKiosk.deviceTokenExpiresAt)}
                  </span>
                </div>
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Token cấp lúc</span>
                  <span className="hce-detail-row__value">
                    {formatDate(selectedKiosk.deviceTokenIssuedAt)}
                  </span>
                </div>
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Hoạt động cuối</span>
                  <span className="hce-detail-row__value">
                    {formatTime(selectedKiosk.lastActiveAt)}
                  </span>
                </div>
              </div>
            </div>
            <div className="hce-modal__footer">
              <button
                className="hce-btn hce-btn--secondary"
                style={{ flex: 1 }}
                onClick={() => { setShowDetailModal(false); openEdit(selectedKiosk); }}
              >
                <Edit size={16} /> Chỉnh sửa
              </button>
              <button
                className="hce-btn"
                style={{ flex: 1, background: '#fef2f2', color: '#dc2626', border: '2px solid #fecaca' }}
                onClick={() => { setShowDetailModal(false); openDeleteConfirm(selectedKiosk); }}
              >
                <Trash2 size={16} /> Xóa
              </button>
              <button
                className="hce-btn hce-btn--primary"
                style={{ flex: 1 }}
                onClick={() => setShowDetailModal(false)}
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========== DELETE MODAL ========== */}
      {showDeleteModal && selectedKiosk && (
        <div className="hce-modal-overlay" onClick={() => !deleteLoading && setShowDeleteModal(false)}>
          <div className="hce-modal" onClick={e => e.stopPropagation()} style={{ width: 440 }}>
            <div className="hce-modal__header">
              <h2 className="hce-modal__title" style={{ color: '#dc2626' }}>Xác nhận xóa kiosk</h2>
              <button className="hce-modal__close" onClick={() => !deleteLoading && setShowDeleteModal(false)}>
                <X size={20} color="#4A5568" />
              </button>
            </div>
            <div className="hce-modal__body">
              <div style={{
                background: '#fef2f2', border: '1px solid #fecaca', borderRadius: 12,
                padding: 16, marginBottom: 20, display: 'flex', gap: 12, alignItems: 'flex-start'
              }}>
                <AlertTriangle size={20} color="#dc2626" style={{ flexShrink: 0, marginTop: 2 }} />
                <div>
                  <div style={{ fontWeight: 600, color: '#991b1b', marginBottom: 4 }}>
                    Hành động này không thể hoàn tác
                  </div>
                  <div style={{ fontSize: 13, color: '#b91c1c' }}>
                    Tất cả dữ liệu liên quan đến kiosk này sẽ bị xóa vĩnh viễn.
                  </div>
                </div>
              </div>
              <div className="hce-detail-panel">
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Mã kiosk</span>
                  <span className="hce-detail-row__value" style={{ fontFamily: 'monospace' }}>
                    {selectedKiosk.kioskCode}
                  </span>
                </div>
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Tên kiosk</span>
                  <span className="hce-detail-row__value">{selectedKiosk.kioskName || '—'}</span>
                </div>
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Loại</span>
                  <span className="hce-detail-row__value">
                    {KIOSK_TYPE_MAP[selectedKiosk.kioskType]?.label || selectedKiosk.kioskType}
                  </span>
                </div>
              </div>
            </div>
            <div className="hce-modal__footer">
              <button
                className="hce-btn hce-btn--secondary"
                style={{ flex: 1 }}
                onClick={() => setShowDeleteModal(false)}
                disabled={deleteLoading}
              >
                Hủy
              </button>
              <button
                className="hce-btn"
                style={{ flex: 1, background: '#dc2626', color: '#fff', border: 'none' }}
                onClick={handleDelete}
                disabled={deleteLoading}
              >
                <Trash2 size={16} />
                {deleteLoading ? 'Đang xóa...' : 'Xóa kiosk'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========== ACTIVATION RESULT MODAL ========== */}
      {activationResult && (
        <div className="hce-modal-overlay" onClick={() => setActivationResult(null)}>
          <div className="hce-modal" onClick={e => e.stopPropagation()} style={{ width: 500 }}>
            <div className="hce-modal__header">
              <h2 className="hce-modal__title">
                <CheckCircle size={20} color="#059669" />
                Kích hoạt kiosk thành công
              </h2>
              <button className="hce-modal__close" onClick={() => setActivationResult(null)}>
                <X size={20} color="#4A5568" />
              </button>
            </div>
            <div className="hce-modal__body">
              <div style={{
                background: '#F0FDF4', border: '1px solid #BBF7D0', borderRadius: 12,
                padding: 16, marginBottom: 20, fontSize: 13, color: '#166534', lineHeight: 1.55
              }}>
                Đã tạo token cho kiosk <strong>{activationResult.kioskCode}</strong>.
                Sử dụng một trong hai cách bên dưới để kích hoạt thiết bị kiosk.
              </div>

              {/* Activation Code */}
              {activationResult.activationCode && (
                <div style={{ marginBottom: 20 }}>
                  <label className="hce-form-label" style={{ marginBottom: 8 }}>
                    Mã kích hoạt (có hiệu lực 15 phút)
                  </label>
                  <div style={{
                    display: 'flex', alignItems: 'center', gap: 10,
                    background: '#F8FAFC', border: '2px solid #E2E8F0', borderRadius: 12, padding: '14px 16px'
                  }}>
                    <span style={{
                      flex: 1, fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
                      fontSize: 28, fontWeight: 700, letterSpacing: 6, color: '#0f172a', textAlign: 'center'
                    }}>
                      {activationResult.activationCode}
                    </span>
                    <button
                      className="hce-btn hce-btn--secondary"
                      style={{ padding: '8px 14px', flexShrink: 0 }}
                      onClick={() => handleCopy(activationResult.activationCode, 'code')}
                    >
                      {copied === 'code' ? <><CheckCircle size={14} color="#059669" /> Đã copy</> : <><Copy size={14} /> Copy</>}
                    </button>
                  </div>
                  <div className="hce-form-helper" style={{ marginTop: 8 }}>
                    <Info size={14} style={{ flexShrink: 0, marginTop: 1 }} />
                    <span>Nhập mã này trên màn hình khóa của kiosk để kích hoạt</span>
                  </div>
                </div>
              )}

              {/* Activation URL */}
              <div>
                <label className="hce-form-label" style={{ marginBottom: 8 }}>
                  URL kích hoạt trực tiếp
                </label>
                <div style={{
                  display: 'flex', alignItems: 'center', gap: 8,
                  background: '#F8FAFC', border: '2px solid #E2E8F0', borderRadius: 12, padding: '10px 12px'
                }}>
                  <span style={{
                    flex: 1, fontSize: 12, color: '#64748b', wordBreak: 'break-all',
                    fontFamily: "'JetBrains Mono', 'Fira Code', monospace"
                  }}>
                    {activationResult.activationUrl}
                  </span>
                  <button
                    className="hce-btn hce-btn--secondary"
                    style={{ padding: '6px 12px', flexShrink: 0 }}
                    onClick={() => handleCopy(activationResult.activationUrl, 'url')}
                  >
                    {copied === 'url' ? <><CheckCircle size={14} color="#059669" /> Đã copy</> : <><Copy size={14} /> Copy</>}
                  </button>
                </div>
                <div className="hce-form-helper" style={{ marginTop: 8 }}>
                  <Info size={14} style={{ flexShrink: 0, marginTop: 1 }} />
                  <span>Mở URL này trên trình duyệt của thiết bị kiosk</span>
                </div>
              </div>
            </div>
            <div className="hce-modal__footer">
              <button
                className="hce-btn hce-btn--secondary"
                style={{ flex: 1 }}
                onClick={() => window.open(activationResult.activationUrl, '_blank')}
              >
                <ExternalLink size={16} />
                Mở trên tab mới
              </button>
              <button
                className="hce-btn hce-btn--primary"
                style={{ flex: 1 }}
                onClick={() => setActivationResult(null)}
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========== REVOKE TOKEN MODAL ========== */}
      {revokeTarget && (
        <div className="hce-modal-overlay" onClick={() => !revoking && setRevokeTarget(null)}>
          <div className="hce-modal" onClick={e => e.stopPropagation()} style={{ width: 440 }}>
            <div className="hce-modal__header">
              <h2 className="hce-modal__title" style={{ color: '#dc2626' }}>
                Xác nhận thu hồi token
              </h2>
              <button className="hce-modal__close" onClick={() => !revoking && setRevokeTarget(null)}>
                <X size={20} color="#4A5568" />
              </button>
            </div>
            <div className="hce-modal__body">
              <div style={{
                background: '#fef2f2', border: '1px solid #fecaca', borderRadius: 12,
                padding: 16, marginBottom: 20, display: 'flex', gap: 12, alignItems: 'flex-start'
              }}>
                <AlertTriangle size={20} color="#dc2626" style={{ flexShrink: 0, marginTop: 2 }} />
                <div>
                  <div style={{ fontWeight: 600, color: '#991b1b', marginBottom: 4 }}>
                    Thiết bị sẽ bị ngắt kết nối ngay lập tức
                  </div>
                  <div style={{ fontSize: 13, color: '#b91c1c' }}>
                    Bạn có chắc chắn muốn thu hồi token của kiosk{' '}
                    <strong>{revokeTarget.kioskCode || revokeTarget.id}</strong>?
                  </div>
                </div>
              </div>
            </div>
            <div className="hce-modal__footer">
              <button
                className="hce-btn hce-btn--secondary"
                style={{ flex: 1 }}
                onClick={() => setRevokeTarget(null)}
                disabled={revoking}
              >
                Huỷ
              </button>
              <button
                className="hce-btn"
                style={{ flex: 1, background: '#dc2626', color: '#fff', border: 'none' }}
                onClick={handleRevokeConfirm}
                disabled={revoking}
              >
                {revoking ? (
                  <><Loader2 size={16} className="sm-spinner" /> Đang thu hồi...</>
                ) : (
                  <><ShieldOff size={16} /> Thu hồi token</>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

export default KioskManagement;
