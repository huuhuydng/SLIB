import React, { useState, useEffect, useMemo, useCallback, useRef } from 'react';
import {
  Router,
  Search,
  Plus,
  Wifi,
  WifiOff,
  MapPin,
  Settings,
  Edit,
  X,
  CheckCircle,
  AlertTriangle,
  RefreshCw,
  Activity,
  Zap,
  Info,
  Wrench,
  Loader2,
  ArrowUpDown,
  ArrowUp,
  ArrowDown,
  Filter,
  SlidersHorizontal,
  LayoutGrid,
  LayoutList,
  Trash2
} from 'lucide-react';
import hceStationService from '../../../services/admin/hceStationService';
import { getAreas } from '../../../services/admin/area_management/api';
import '../../../styles/librarian/librarian-shared.css';
import '../../../styles/librarian/CheckInOut.css';
import '../../../styles/admin/HceStationManagement.css';
import '../UserManagement/UserManagement.css';

const DEVICE_TYPE_MAP = {
  ENTRY_GATE: { label: 'Cổng vào', color: '#2563EB', bg: '#DBEAFE' },
  EXIT_GATE: { label: 'Cổng ra', color: '#7C3AED', bg: '#F3E8FF' },
  SEAT_READER: { label: 'Đầu đọc ghế', color: '#059669', bg: '#D1FAE5' }
};

const STATUS_MAP = {
  ACTIVE: { label: 'Hoạt động', css: 'active' },
  INACTIVE: { label: 'Vô hiệu hóa', css: 'inactive' },
  MAINTENANCE: { label: 'Bảo trì', css: 'maintenance' }
};

const STATUS_OPTIONS = [
  { value: '', label: 'Tất cả' },
  { value: 'ACTIVE', label: 'Hoạt động' },
  { value: 'INACTIVE', label: 'Vô hiệu hóa' },
  { value: 'MAINTENANCE', label: 'Bảo trì' },
];

const TYPE_OPTIONS = [
  { value: '', label: 'Tất cả' },
  { value: 'ENTRY_GATE', label: 'Cổng vào' },
  { value: 'EXIT_GATE', label: 'Cổng ra' },
  { value: 'SEAT_READER', label: 'Đầu đọc ghế' },
];

const DeviceManagement = () => {
  const [stations, setStations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [areas, setAreas] = useState([]);
  const [lastUpdatedAt, setLastUpdatedAt] = useState(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedStation, setSelectedStation] = useState(null);
  const [toast, setToast] = useState(null);
  const [formData, setFormData] = useState({
    deviceId: '', deviceName: '', deviceType: '', location: '', status: 'ACTIVE', areaId: ''
  });
  const [formLoading, setFormLoading] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
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
    deviceName: '',
    deviceId: '',
    deviceType: '',
    location: '',
    online: '',
    status: '',
  });
  const [activeFilterCol, setActiveFilterCol] = useState(null);
  const filterRef = useRef(null);

  // Column visibility
  const [visibleColumns, setVisibleColumns] = useState({
    deviceName: true,
    deviceId: true,
    deviceType: true,
    location: true,
    online: true,
    status: true,
  });
  const [showColumnMenu, setShowColumnMenu] = useState(false);

  // Toast helper
  const showToast = useCallback((message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  }, []);

  // Fetch stations
  const fetchStations = useCallback(async ({ silent = false } = {}) => {
    if (silent) {
      setIsRefreshing(true);
    } else {
      setLoading(true);
    }
    try {
      const [stationData, areasRes] = await Promise.all([
        hceStationService.getAllStations(),
        getAreas()
      ]);
      setStations(Array.isArray(stationData) ? stationData : []);
      setAreas(Array.isArray(areasRes?.data) ? areasRes.data : []);
      setLastUpdatedAt(new Date());
    } catch (err) {
      if (!silent) {
        showToast(err.message || 'Lỗi tải danh sách trạm quét', 'error');
        setStations([]);
      }
    } finally {
      if (silent) {
        setIsRefreshing(false);
      } else {
        setLoading(false);
      }
    }
  }, [showToast]);

  useEffect(() => {
    fetchStations();
  }, [fetchStations]);

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      fetchStations({ silent: true });
    }, 30000);
    return () => window.clearInterval(intervalId);
  }, [fetchStations]);

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

  // Format time
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

  // Get value for a column (for sort/filter)
  const getStationValue = useCallback((station, column) => {
    switch (column) {
      case 'deviceName': return station.deviceName || '';
      case 'deviceId': return station.deviceId || '';
      case 'deviceType': return DEVICE_TYPE_MAP[station.deviceType]?.label || station.deviceType || '';
      case 'location': return station.location || '';
      case 'online': return station.online ? 'Trực tuyến' : 'Ngoại tuyến';
      case 'status': return STATUS_MAP[station.status]?.label || station.status || '';
      default: return '';
    }
  }, []);

  // Filtered + sorted stations
  const filteredStations = useMemo(() => {
    let list = [...stations];

    // Global search
    const q = searchText.trim().toLowerCase();
    if (q) {
      list = list.filter(s =>
        (s.deviceName || '').toLowerCase().includes(q) ||
        (s.deviceId || '').toLowerCase().includes(q) ||
        (s.location || '').toLowerCase().includes(q) ||
        (DEVICE_TYPE_MAP[s.deviceType]?.label || '').toLowerCase().includes(q)
      );
    }

    // Column filters
    Object.entries(columnFilters).forEach(([col, filterVal]) => {
      if (!filterVal) return;
      const fq = filterVal.toLowerCase();

      if (col === 'deviceType') {
        list = list.filter(s => s.deviceType === filterVal);
      } else if (col === 'status') {
        list = list.filter(s => s.status === filterVal);
      } else if (col === 'online') {
        if (filterVal === 'online') list = list.filter(s => s.online);
        else if (filterVal === 'offline') list = list.filter(s => !s.online);
      } else if (col === 'deviceName') {
        list = list.filter(s => (s.deviceName || '').toLowerCase().includes(fq));
      } else if (col === 'deviceId') {
        list = list.filter(s => (s.deviceId || '').toLowerCase().includes(fq));
      } else if (col === 'location') {
        list = list.filter(s => (s.location || '').toLowerCase().includes(fq));
      }
    });

    // Sort
    if (sortConfig.column && sortConfig.direction) {
      list.sort((a, b) => {
        let valA = getStationValue(a, sortConfig.column).toLowerCase();
        let valB = getStationValue(b, sortConfig.column).toLowerCase();
        if (valA < valB) return sortConfig.direction === 'asc' ? -1 : 1;
        if (valA > valB) return sortConfig.direction === 'asc' ? 1 : -1;
        return 0;
      });
    }

    return list;
  }, [stations, searchText, columnFilters, sortConfig, getStationValue]);

  // Pagination
  const totalPages = Math.ceil(filteredStations.length / itemsPerPage);
  const paginatedStations = useMemo(() => {
    const start = (currentPage - 1) * itemsPerPage;
    return filteredStations.slice(start, start + itemsPerPage);
  }, [filteredStations, currentPage, itemsPerPage]);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchText, columnFilters, sortConfig, itemsPerPage]);

  // Sort handler
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

  // Sort icon
  const renderSortIcon = (column) => {
    if (sortConfig.column === column) {
      if (sortConfig.direction === 'asc') return <ArrowUp size={13} />;
      if (sortConfig.direction === 'desc') return <ArrowDown size={13} />;
    }
    return <ArrowUpDown size={13} />;
  };

  // Column header with sort + filter
  const renderColumnHeader = (column, label) => {
    const hasFilter = !!columnFilters[column];
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
              {column === 'deviceType' ? (
                <select
                  value={columnFilters.deviceType}
                  onChange={(e) => { handleFilterChange('deviceType', e.target.value); setActiveFilterCol(null); }}
                  autoFocus
                  className="cio-filter-input"
                >
                  {TYPE_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                </select>
              ) : column === 'status' ? (
                <select
                  value={columnFilters.status}
                  onChange={(e) => { handleFilterChange('status', e.target.value); setActiveFilterCol(null); }}
                  autoFocus
                  className="cio-filter-input"
                >
                  {STATUS_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                </select>
              ) : column === 'online' ? (
                <select
                  value={columnFilters.online}
                  onChange={(e) => { handleFilterChange('online', e.target.value); setActiveFilterCol(null); }}
                  autoFocus
                  className="cio-filter-input"
                >
                  <option value="">Tất cả</option>
                  <option value="online">Trực tuyến</option>
                  <option value="offline">Ngoại tuyến</option>
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
  const visibleColumnCount = Object.values(visibleColumns).filter(Boolean).length + 1; // +1 for actions

  // Reset form
  const resetForm = () => {
    setFormData({ deviceId: '', deviceName: '', deviceType: '', location: '', status: 'ACTIVE', areaId: '' });
  };

  // Create station
  const handleCreate = async () => {
    if (!formData.deviceId.trim() || !formData.deviceName.trim() || !formData.deviceType) {
      showToast('Vui lòng điền đầy đủ thông tin bắt buộc', 'error');
      return;
    }
    setFormLoading(true);
    try {
      const payload = { ...formData };
      payload.areaId = payload.areaId ? Number(payload.areaId) : 0;
      await hceStationService.createStation(payload);
      showToast('Tạo trạm quét thành công!');
      setShowCreateModal(false);
      resetForm();
      fetchStations();
    } catch (err) {
      showToast(err.message || 'Lỗi tạo trạm quét', 'error');
    } finally {
      setFormLoading(false);
    }
  };

  // Update station
  const handleUpdate = async () => {
    if (!selectedStation) return;
    setFormLoading(true);
    try {
      const payload = { ...formData };
      payload.areaId = payload.areaId ? Number(payload.areaId) : 0;
      await hceStationService.updateStation(selectedStation.id, payload);
      showToast('Cập nhật trạm quét thành công!');
      setShowEditModal(false);
      resetForm();
      fetchStations();
    } catch (err) {
      showToast(err.message || 'Lỗi cập nhật trạm quét', 'error');
    } finally {
      setFormLoading(false);
    }
  };

  // Change status
  const handleStatusChange = async (stationId, newStatus) => {
    try {
      await hceStationService.updateStationStatus(stationId, newStatus);
      showToast(`Đã chuyển trạng thái sang ${STATUS_MAP[newStatus]?.label || newStatus}`);
      setShowDetailModal(false);
      fetchStations();
    } catch (err) {
      showToast(err.message || 'Lỗi cập nhật trạng thái', 'error');
    }
  };

  // Delete station
  const handleDelete = async () => {
    if (!selectedStation) return;
    setDeleteLoading(true);
    try {
      await hceStationService.deleteStation(selectedStation.id);
      showToast('Đã xóa trạm quét thành công!');
      setShowDeleteModal(false);
      setShowDetailModal(false);
      setSelectedStation(null);
      fetchStations();
    } catch (err) {
      showToast(err.message || 'Lỗi xóa trạm quét', 'error');
    } finally {
      setDeleteLoading(false);
    }
  };

  // Open delete confirmation
  const openDeleteConfirm = (station) => {
    setSelectedStation(station);
    setShowDeleteModal(true);
  };

  // Open detail
  const openDetail = async (station) => {
    try {
      const detail = await hceStationService.getStationById(station.id);
      setSelectedStation(detail);
      setShowDetailModal(true);
    } catch (err) {
      showToast(err.message || 'Lỗi tải chi tiết trạm', 'error');
    }
  };

  // Open edit
  const openEdit = (station) => {
    setSelectedStation(station);
    setFormData({
      deviceId: station.deviceId || '',
      deviceName: station.deviceName || '',
      deviceType: station.deviceType || '',
      location: station.location || '',
      status: station.status || 'ACTIVE',
      areaId: station.areaId || ''
    });
    setShowEditModal(true);
  };

  // Form input handler
  const handleFormChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  // Render online badge
  const renderOnlineBadge = (online) => (
    <span className={`hce-badge hce-badge--${online ? 'online' : 'offline'}`}>
      {online ? <Wifi size={12} /> : <WifiOff size={12} />}
      {online ? 'Trực tuyến' : 'Ngoại tuyến'}
    </span>
  );

  // Render status badge
  const renderStatusBadge = (status) => {
    const info = STATUS_MAP[status] || { label: status, css: 'inactive' };
    return <span className={`hce-badge hce-badge--${info.css}`}>{info.label}</span>;
  };

  // Render type badge
  const renderTypeBadge = (deviceType) => {
    const info = DEVICE_TYPE_MAP[deviceType] || { label: deviceType, color: '#6B7280', bg: '#F3F4F6' };
    return (
      <span className="hce-badge" style={{ background: info.bg, color: info.color }}>
        {info.label}
      </span>
    );
  };

  // Render form fields
  const renderForm = (isEdit = false) => (
    <>
      <div className="hce-form-group">
        <label className="hce-form-label">Mã trạm (Device ID) *</label>
        <input
          type="text"
          className="hce-form-input"
          placeholder="VD: GATE_01"
          value={formData.deviceId}
          onChange={e => handleFormChange('deviceId', e.target.value)}
          disabled={isEdit}
        />
        <div className="hce-form-helper hce-form-helper--info">
          <Info size={14} style={{ flexShrink: 0, marginTop: 1 }} />
          <span>Mã trạm phải trùng với <code>SLIB_GATE_ID</code> cấu hình trên Raspberry Pi</span>
        </div>
      </div>
      <div className="hce-form-group">
        <label className="hce-form-label">Tên trạm *</label>
        <input
          type="text"
          className="hce-form-input"
          placeholder="VD: Cổng vào chính - Tầng 1"
          value={formData.deviceName}
          onChange={e => handleFormChange('deviceName', e.target.value)}
        />
      </div>
      <div className="hce-form-group">
        <label className="hce-form-label">Loại trạm *</label>
        <select
          className="hce-form-input"
          value={formData.deviceType}
          onChange={e => handleFormChange('deviceType', e.target.value)}
        >
          <option value="">Chọn loại trạm</option>
          <option value="ENTRY_GATE">Cổng vào</option>
          <option value="EXIT_GATE">Cổng ra</option>
          <option value="SEAT_READER">Đầu đọc ghế</option>
        </select>
      </div>
      <div className="hce-form-group">
        <label className="hce-form-label">Vị trí</label>
        <input
          type="text"
          className="hce-form-input"
          placeholder="VD: Tầng 1 - Cửa chính"
          value={formData.location}
          onChange={e => handleFormChange('location', e.target.value)}
        />
      </div>
      <div className="hce-form-group">
        <label className="hce-form-label">Khu vực</label>
        <select
          className="hce-form-input"
          value={formData.areaId}
          onChange={e => handleFormChange('areaId', e.target.value)}
        >
          <option value="">Chưa gán khu vực</option>
          {areas.map(area => (
            <option key={area.areaId} value={area.areaId}>
              {area.areaName}
            </option>
          ))}
        </select>
      </div>
      <div className="hce-form-group">
        <label className="hce-form-label">Trạng thái</label>
        <select
          className="hce-form-input"
          value={formData.status}
          onChange={e => handleFormChange('status', e.target.value)}
        >
          <option value="ACTIVE">Hoạt động</option>
          <option value="INACTIVE">Vô hiệu hóa</option>
          <option value="MAINTENANCE">Bảo trì</option>
        </select>
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
          <h1>QUẢN LÝ TRẠM QUÉT HCE</h1>
        </div>

        <div className="lib-panel">
          {/* Toolbar */}
          <div className="cio-toolbar">
            <div className="lib-search">
              <Search size={16} className="lib-search-icon" />
              <input
                type="text"
                placeholder="Tìm mã, tên, vị trí..."
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
                      { key: 'deviceName', label: 'Tên trạm' },
                      { key: 'deviceId', label: 'Mã trạm' },
                      { key: 'deviceType', label: 'Loại' },
                      { key: 'location', label: 'Vị trí' },
                      { key: 'online', label: 'Kết nối' },
                      { key: 'status', label: 'Trạng thái' },
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
              Tổng số <strong>{filteredStations.length}</strong> kết quả
            </span>

            <div className="cio-toolbar-right">
              <span className="cio-result-count" style={{ marginRight: 4, color: '#64748b' }}>
                {lastUpdatedAt ? `Cập nhật ${lastUpdatedAt.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}` : 'Chưa cập nhật'}
              </span>
              <button className="um-toolbar-btn" onClick={() => fetchStations()} disabled={loading}>
                <RefreshCw size={14} className={loading || isRefreshing ? 'sm-spinner' : ''} />
                Làm mới
              </button>
              <button className="um-toolbar-btn primary" onClick={() => { resetForm(); setShowCreateModal(true); }}>
                <Plus size={14} />
                Thêm trạm quét
              </button>
            </div>
          </div>

          {/* Content */}
          {loading ? (
            <div className="sm-loading" style={{ padding: '60px', textAlign: 'center' }}>
              <Loader2 size={28} className="sm-spinner" />
              <span style={{ display: 'block', marginTop: '12px', color: '#64748b' }}>Đang tải danh sách trạm quét...</span>
            </div>
          ) : viewMode === 'table' ? (
            /* ========== TABLE VIEW ========== */
            <div className="sr-table-wrapper">
              <table className="sr-table">
                <thead>
                  <tr>
                    {visibleColumns.deviceName && renderColumnHeader('deviceName', 'Tên trạm')}
                    {visibleColumns.deviceId && renderColumnHeader('deviceId', 'Mã trạm')}
                    {visibleColumns.deviceType && renderColumnHeader('deviceType', 'Loại')}
                    {visibleColumns.location && renderColumnHeader('location', 'Vị trí')}
                    {visibleColumns.online && renderColumnHeader('online', 'Kết nối')}
                    {visibleColumns.status && renderColumnHeader('status', 'Trạng thái')}
                    <th style={{ textAlign: 'center' }}>
                      <div className="cio-th-content" style={{ justifyContent: 'center' }}>
                        <span className="cio-th-label">Thao tác</span>
                      </div>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {paginatedStations.length === 0 ? (
                    <tr>
                      <td colSpan={visibleColumnCount} className="sr-table-empty-cell">
                        {searchText ? 'Không tìm thấy trạm quét phù hợp.' : 'Chưa có trạm quét nào.'}
                      </td>
                    </tr>
                  ) : (
                    paginatedStations.map(station => {
                      const typeInfo = DEVICE_TYPE_MAP[station.deviceType] || { label: station.deviceType, color: '#6B7280', bg: '#F3F4F6' };

                      return (
                        <tr
                          key={station.id}
                          className="sr-table-row"
                          onClick={() => openDetail(station)}
                        >
                          {visibleColumns.deviceName && (
                            <td>
                              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                                <div style={{
                                  width: 32, height: 32, borderRadius: 8,
                                  background: typeInfo.bg,
                                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                                  flexShrink: 0
                                }}>
                                  <Router size={16} color={typeInfo.color} />
                                </div>
                                <div>
                                  <div style={{ fontWeight: 600, fontSize: 13, color: '#0f172a' }}>{station.deviceName}</div>
                                  <div style={{ fontSize: 11, color: '#94a3b8' }}>
                                    Quét hôm nay: {station.todayScanCount || 0}
                                  </div>
                                </div>
                              </div>
                            </td>
                          )}
                          {visibleColumns.deviceId && (
                            <td>
                              <code style={{
                                fontSize: 12, color: '#64748b',
                                fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
                                background: '#f3f4f6', padding: '3px 8px', borderRadius: 6
                              }}>
                                {station.deviceId}
                              </code>
                            </td>
                          )}
                          {visibleColumns.deviceType && (
                            <td>{renderTypeBadge(station.deviceType)}</td>
                          )}
                          {visibleColumns.location && (
                            <td style={{ fontSize: 13, color: '#475569' }}>
                              {station.location || '—'}
                            </td>
                          )}
                          {visibleColumns.online && (
                            <td>{renderOnlineBadge(station.online)}</td>
                          )}
                          {visibleColumns.status && (
                            <td>{renderStatusBadge(station.status)}</td>
                          )}
                          <td style={{ textAlign: 'center' }} onClick={e => e.stopPropagation()}>
                            <div style={{ display: 'flex', gap: 6, justifyContent: 'center' }}>
                              <button
                                className="um-action-btn"
                                title="Chỉnh sửa"
                                onClick={() => openEdit(station)}
                              >
                                <Edit size={14} color="#64748b" />
                              </button>
                              <button
                                className="um-action-btn"
                                title="Chi tiết"
                                onClick={() => openDetail(station)}
                              >
                                <Settings size={14} color="#64748b" />
                              </button>
                              <button
                                className="um-action-btn"
                                title="Xóa"
                                onClick={() => openDeleteConfirm(station)}
                              >
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
              {paginatedStations.length === 0 ? (
                <div className="hce-station-empty">
                  <Router size={48} className="hce-station-empty__icon" />
                  <div className="hce-station-empty__title">Chưa có trạm quét nào</div>
                  <div className="hce-station-empty__text">
                    Nhấn "Thêm trạm quét" để đăng ký một trạm quét HCE mới
                  </div>
                </div>
              ) : (
                paginatedStations.map(station => {
                  const typeInfo = DEVICE_TYPE_MAP[station.deviceType] || { label: station.deviceType, color: '#6B7280', bg: '#F3F4F6' };
                  const statusInfo = STATUS_MAP[station.status] || { label: station.status, css: 'inactive' };

                  return (
                    <div key={station.id} className="hce-station-card" onClick={() => openDetail(station)}>
                      <div className="hce-station-card__header">
                        <div className="hce-station-card__info">
                          <div className="hce-station-card__icon-wrap" style={{ background: typeInfo.bg }}>
                            <Router size={22} color={typeInfo.color} />
                          </div>
                          <div>
                            <div className="hce-station-card__name">{station.deviceName}</div>
                            <div className="hce-station-card__device-id">{station.deviceId}</div>
                          </div>
                        </div>
                        <div className="hce-station-card__badges">
                          <span className={`hce-badge hce-badge--${station.online ? 'online' : 'offline'}`}>
                            {station.online ? <Wifi size={12} /> : <WifiOff size={12} />}
                            {station.online ? 'Trực tuyến' : 'Ngoại tuyến'}
                          </span>
                          <span className={`hce-badge hce-badge--${statusInfo.css}`}>
                            {statusInfo.label}
                          </span>
                        </div>
                      </div>

                      <div className="hce-station-card__meta">
                        {station.location && (
                          <div className="hce-station-card__meta-row">
                            <MapPin size={14} className="hce-station-card__meta-icon" />
                            <span className="hce-station-card__meta-text">{station.location}</span>
                          </div>
                        )}
                        <div className="hce-station-card__meta-row">
                          <Activity size={14} className="hce-station-card__meta-icon" />
                          <span className="hce-station-card__meta-text hce-station-card__meta-text--muted">
                            Heartbeat: {formatTime(station.lastHeartbeat)}
                          </span>
                        </div>
                        <div className="hce-station-card__meta-row">
                          <Zap size={14} className="hce-station-card__meta-icon" />
                          <span className="hce-station-card__meta-text">
                            Lượt quét hôm nay: <strong>{station.todayScanCount || 0}</strong>
                          </span>
                        </div>
                        {station.areaName && (
                          <div className="hce-station-card__meta-row">
                            <MapPin size={14} className="hce-station-card__meta-icon" />
                            <span className="hce-station-card__meta-text hce-station-card__meta-text--muted">
                              Khu vực: {station.areaName}
                            </span>
                          </div>
                        )}
                      </div>

                      <div className="hce-station-card__actions">
                        <button
                          className="hce-station-card__action-btn"
                          onClick={e => { e.stopPropagation(); openEdit(station); }}
                        >
                          <Edit size={14} />
                          Chỉnh sửa
                        </button>
                        <button
                          className="hce-station-card__action-btn"
                          onClick={e => { e.stopPropagation(); openDetail(station); }}
                        >
                          <Settings size={14} />
                          Chi tiết
                        </button>
                        <button
                          className="hce-station-card__action-btn"
                          onClick={e => { e.stopPropagation(); openDeleteConfirm(station); }}
                          style={{ color: '#ef4444', borderColor: '#fecaca' }}
                        >
                          <Trash2 size={14} />
                          Xóa
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

      {/* CREATE MODAL */}
      {showCreateModal && (
        <div className="hce-modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="hce-modal" onClick={e => e.stopPropagation()}>
            <div className="hce-modal__header">
              <h2 className="hce-modal__title">Thêm trạm quét mới</h2>
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
                {formLoading ? 'Đang tạo...' : 'Thêm trạm quét'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* EDIT MODAL */}
      {showEditModal && selectedStation && (
        <div className="hce-modal-overlay" onClick={() => setShowEditModal(false)}>
          <div className="hce-modal" onClick={e => e.stopPropagation()}>
            <div className="hce-modal__header">
              <h2 className="hce-modal__title">Chỉnh sửa trạm quét</h2>
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

      {/* DETAIL MODAL */}
      {showDetailModal && selectedStation && (
        <div className="hce-modal-overlay" onClick={() => setShowDetailModal(false)}>
          <div className="hce-modal" onClick={e => e.stopPropagation()}>
            <div className="hce-modal__header">
              <h2 className="hce-modal__title">Chi tiết trạm quét</h2>
              <button className="hce-modal__close" onClick={() => setShowDetailModal(false)}>
                <X size={20} color="#4A5568" />
              </button>
            </div>
            <div className="hce-modal__body">
              {/* Status badges */}
              <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
                <span className={`hce-badge hce-badge--${selectedStation.online ? 'online' : 'offline'}`}>
                  {selectedStation.online ? <Wifi size={14} /> : <WifiOff size={14} />}
                  {selectedStation.online ? 'Trực tuyến' : 'Ngoại tuyến'}
                </span>
                <span className={`hce-badge hce-badge--${STATUS_MAP[selectedStation.status]?.css || 'inactive'}`}>
                  {STATUS_MAP[selectedStation.status]?.label || selectedStation.status}
                </span>
              </div>

              {/* Detail info */}
              <div className="hce-detail-panel">
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Mã trạm</span>
                  <span className="hce-detail-row__value" style={{ fontFamily: 'monospace' }}>
                    {selectedStation.deviceId}
                  </span>
                </div>
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Tên trạm</span>
                  <span className="hce-detail-row__value">{selectedStation.deviceName}</span>
                </div>
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Loại</span>
                  <span className="hce-detail-row__value">
                    {DEVICE_TYPE_MAP[selectedStation.deviceType]?.label || selectedStation.deviceType}
                  </span>
                </div>
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Vị trí</span>
                  <span className="hce-detail-row__value">{selectedStation.location || '—'}</span>
                </div>
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Khu vực</span>
                  <span className="hce-detail-row__value">{selectedStation.areaName || '—'}</span>
                </div>
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Heartbeat cuối</span>
                  <span className="hce-detail-row__value">
                    {formatTime(selectedStation.lastHeartbeat)}
                  </span>
                </div>
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Lượt quét hôm nay</span>
                  <span className="hce-detail-row__value" style={{ color: '#e8600a', fontWeight: 700 }}>
                    {selectedStation.todayScanCount || 0}
                  </span>
                </div>
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Truy cập cuối</span>
                  <span className="hce-detail-row__value">{formatTime(selectedStation.lastAccessTime)}</span>
                </div>
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Ngày tạo</span>
                  <span className="hce-detail-row__value">
                    {selectedStation.createdAt ? new Date(selectedStation.createdAt).toLocaleDateString('vi-VN') : '—'}
                  </span>
                </div>
              </div>

              {/* Status change actions */}
              <div style={{ marginBottom: 16 }}>
                <label className="hce-form-label" style={{ marginBottom: 12 }}>Thay đổi trạng thái</label>
                <div className="hce-status-actions">
                  {Object.entries(STATUS_MAP).map(([key, val]) => (
                    <button
                      key={key}
                      className={`hce-status-btn hce-status-btn--${val.css}`}
                      onClick={() => handleStatusChange(selectedStation.id, key)}
                      disabled={selectedStation.status === key}
                      style={{ opacity: selectedStation.status === key ? 0.5 : 1 }}
                    >
                      {val.label}
                    </button>
                  ))}
                </div>
              </div>
            </div>
            <div className="hce-modal__footer">
              <button
                className="hce-btn hce-btn--secondary"
                style={{ flex: 1 }}
                onClick={() => {
                  setShowDetailModal(false);
                  openEdit(selectedStation);
                }}
              >
                <Edit size={16} />
                Chỉnh sửa
              </button>
              <button
                className="hce-btn"
                style={{ flex: 1, background: '#fef2f2', color: '#dc2626', border: '2px solid #fecaca' }}
                onClick={() => {
                  setShowDetailModal(false);
                  openDeleteConfirm(selectedStation);
                }}
              >
                <Trash2 size={16} />
                Xóa
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

      {/* DELETE CONFIRMATION MODAL */}
      {showDeleteModal && selectedStation && (
        <div className="hce-modal-overlay" onClick={() => !deleteLoading && setShowDeleteModal(false)}>
          <div className="hce-modal" onClick={e => e.stopPropagation()} style={{ width: 440 }}>
            <div className="hce-modal__header">
              <h2 className="hce-modal__title" style={{ color: '#dc2626' }}>Xác nhận xóa trạm quét</h2>
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
                    Tất cả dữ liệu liên quan đến trạm quét này sẽ bị xóa vĩnh viễn.
                  </div>
                </div>
              </div>
              <div className="hce-detail-panel">
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Tên trạm</span>
                  <span className="hce-detail-row__value">{selectedStation.deviceName}</span>
                </div>
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Mã trạm</span>
                  <span className="hce-detail-row__value" style={{ fontFamily: 'monospace' }}>{selectedStation.deviceId}</span>
                </div>
                <div className="hce-detail-row">
                  <span className="hce-detail-row__label">Loại</span>
                  <span className="hce-detail-row__value">{DEVICE_TYPE_MAP[selectedStation.deviceType]?.label || selectedStation.deviceType}</span>
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
                {deleteLoading ? 'Đang xóa...' : 'Xóa trạm quét'}
              </button>
            </div>
          </div>
        </div>
      )}

    </>
  );
};

export default DeviceManagement;
