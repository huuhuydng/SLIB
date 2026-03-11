import React, { useState, useEffect, useMemo, useCallback } from 'react';
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
  Clock,
  Zap,
  Info,
  Wrench
} from 'lucide-react';
import hceStationService from '../../../services/admin/hceStationService';
import '../../../styles/admin/HceStationManagement.css';

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

const DeviceManagement = () => {
  const [stations, setStations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchText, setSearchText] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedStation, setSelectedStation] = useState(null);
  const [toast, setToast] = useState(null);
  const [formData, setFormData] = useState({
    deviceId: '', deviceName: '', deviceType: '', location: '', status: 'ACTIVE', areaId: ''
  });
  const [formLoading, setFormLoading] = useState(false);

  // Toast helper
  const showToast = useCallback((message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  }, []);

  // Fetch stations
  const fetchStations = useCallback(async () => {
    setLoading(true);
    try {
      const data = await hceStationService.getAllStations(searchText, statusFilter, typeFilter);
      setStations(Array.isArray(data) ? data : []);
    } catch (err) {
      showToast(err.message || 'Lỗi tải danh sách trạm quét', 'error');
      setStations([]);
    } finally {
      setLoading(false);
    }
  }, [searchText, statusFilter, typeFilter, showToast]);

  useEffect(() => {
    fetchStations();
  }, [fetchStations]);

  // Stats
  const stats = useMemo(() => {
    const total = stations.length;
    const online = stations.filter(s => s.online).length;
    const offline = total - online;
    const maintenance = stations.filter(s => s.status === 'MAINTENANCE').length;
    return { total, online, offline, maintenance };
  }, [stations]);

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
      if (!payload.areaId) delete payload.areaId;
      else payload.areaId = Number(payload.areaId);
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
      if (!payload.areaId) delete payload.areaId;
      else payload.areaId = Number(payload.areaId);
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

      <div className="hce-station-page">
        {/* Header */}
        <div className="hce-station-page__header">
          <div>
            <h1 className="hce-station-page__title">Quản lý trạm quét HCE</h1>
            <p className="hce-station-page__subtitle">
              Quản lý các trạm quét HCE (Raspberry Pi + ACR122U) trong thư viện
            </p>
          </div>
          <div className="hce-station-page__actions">
            <button className="hce-btn hce-btn--secondary" onClick={fetchStations}>
              <RefreshCw size={18} />
              Làm mới
            </button>
            <button className="hce-btn hce-btn--primary" onClick={() => { resetForm(); setShowCreateModal(true); }}>
              <Plus size={18} />
              Thêm trạm quét
            </button>
          </div>
        </div>

        {/* Stats */}
        <div className="hce-station-stats">
          {[
            { label: 'Tổng trạm', value: stats.total, icon: Router, color: '#7C3AED', bg: '#F3E8FF' },
            { label: 'Trực tuyến', value: stats.online, icon: Wifi, color: '#059669', bg: '#D1FAE5' },
            { label: 'Ngoại tuyến', value: stats.offline, icon: WifiOff, color: '#DC2626', bg: '#FEE2E2' },
            { label: 'Bảo trì', value: stats.maintenance, icon: Wrench, color: '#D97706', bg: '#FEF3C7' },
          ].map((stat, idx) => (
            <div key={idx} className="hce-station-stat-card">
              <div className="hce-station-stat-card__icon" style={{ background: stat.bg }}>
                <stat.icon size={22} color={stat.color} />
              </div>
              <div>
                <div className="hce-station-stat-card__value">{stat.value}</div>
                <div className="hce-station-stat-card__label">{stat.label}</div>
              </div>
            </div>
          ))}
        </div>

        {/* Main container */}
        <div className="hce-station-container">
          {/* Filters */}
          <div className="hce-station-filters">
            <div className="hce-station-filters__left">
              <div className="hce-station-search">
                <Search size={16} className="hce-station-search__icon" />
                <input
                  type="text"
                  placeholder="Tìm theo mã, tên, vị trí..."
                  value={searchText}
                  onChange={e => setSearchText(e.target.value)}
                  className="hce-station-search__input"
                />
              </div>
              <select
                value={statusFilter}
                onChange={e => setStatusFilter(e.target.value)}
                className="hce-station-select"
              >
                <option value="">Tất cả trạng thái</option>
                <option value="ACTIVE">Hoạt động</option>
                <option value="INACTIVE">Vô hiệu hóa</option>
                <option value="MAINTENANCE">Bảo trì</option>
              </select>
              <select
                value={typeFilter}
                onChange={e => setTypeFilter(e.target.value)}
                className="hce-station-select"
              >
                <option value="">Tất cả loại</option>
                <option value="ENTRY_GATE">Cổng vào</option>
                <option value="EXIT_GATE">Cổng ra</option>
                <option value="SEAT_READER">Đầu đọc ghế</option>
              </select>
            </div>
          </div>

          {/* Loading */}
          {loading && (
            <div className="hce-station-loading">
              <div className="hce-station-loading__spinner" />
              <div>Đang tải danh sách trạm quét...</div>
            </div>
          )}

          {/* Empty state */}
          {!loading && stations.length === 0 && (
            <div className="hce-station-empty">
              <Router size={48} className="hce-station-empty__icon" />
              <div className="hce-station-empty__title">Chưa có trạm quét nào</div>
              <div className="hce-station-empty__text">
                Nhấn "Thêm trạm quét" để đăng ký một trạm quét HCE mới
              </div>
            </div>
          )}

          {/* Station grid */}
          {!loading && stations.length > 0 && (
            <div className="hce-station-grid">
              {stations.map(station => {
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
                    </div>
                  </div>
                );
              })}
            </div>
          )}
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
                    <Clock size={12} style={{ marginRight: 4, verticalAlign: 'middle' }} />
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
    </>
  );
};

export default DeviceManagement;