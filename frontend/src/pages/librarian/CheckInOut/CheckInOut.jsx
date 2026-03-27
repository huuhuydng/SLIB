import React, { useState, useMemo, useEffect, useRef } from 'react';
import { API_BASE_URL } from '../../../config/apiConfig';
import { useToast } from '../../../components/common/ToastProvider';
import { useConfirm } from '../../../components/common/ConfirmDialog';
import { Search, ArrowUpDown, ArrowUp, ArrowDown, Filter, X, SlidersHorizontal, Trash2 } from 'lucide-react';
import "../../../styles/librarian/librarian-shared.css";
import "../../../styles/librarian/CheckInOut.css";
import librarianService from "../../../services/librarian/librarianService";
import StudentDetailModal from "../../../components/librarian/StudentDetailModal";
import websocketService from "../../../services/shared/websocketService";


const CheckInOut = () => {
  const toast = useToast();
  const { confirm } = useConfirm();
  const [searchTerm, setSearchTerm] = useState('');

  // State for real data
  const [accessLogs, setAccessLogs] = useState([]);
  const [stats, setStats] = useState({
    totalCheckInsToday: 0,
    totalCheckOutsToday: 0,
    currentlyInLibrary: 0
  });
  const [loading, setLoading] = useState(true);

  // Modal state
  const [showStudentModal, setShowStudentModal] = useState(false);
  const [selectedUserId, setSelectedUserId] = useState(null);

  // Pagination state
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);

  // Sort state: { column: string, direction: 'asc' | 'desc' | null }
  const [sortConfig, setSortConfig] = useState({ column: null, direction: null });

  // Date filter state for export
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  // Column filter state: { columnKey: filterValue }
  const [columnFilters, setColumnFilters] = useState({
    userCode: '',
    userName: '',
    action: '',
    time: '',
  });
  const [activeFilterCol, setActiveFilterCol] = useState(null);
  const filterRef = useRef(null);

  // Column visibility state
  const [visibleColumns, setVisibleColumns] = useState({
    userName: true,
    userCode: true,
    action: true,
    time: true,
  });
  const [showColumnMenu, setShowColumnMenu] = useState(false);

  // Selection for batch delete
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [deleting, setDeleting] = useState(false);

  // Fetch data on mount
  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 30000);
    return () => clearInterval(interval);
  }, []);

  // Close filter dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (filterRef.current && !filterRef.current.contains(e.target)) {
        setActiveFilterCol(null);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // WebSocket real-time updates
  useEffect(() => {
    let unsubscribe = null;
    websocketService.connect(
      () => {
        unsubscribe = websocketService.subscribe('/topic/access-logs', (message) => {
          if (message.type === 'CHECK_IN' || message.type === 'CHECK_OUT') {
            const newLog = {
              logId: `${message.userId}-${message.type}-${Date.now()}`,
              userId: message.userId,
              userName: message.fullName || message.userName,
              userCode: message.userCode,
              action: message.type,
              checkInTime: message.checkInTime || message.time || message.timestamp,
              checkOutTime: message.checkOutTime || (message.type === 'CHECK_OUT' ? (message.time || message.timestamp) : null),
              deviceId: message.deviceId || null,
              avatarUrl: null
            };
            setAccessLogs(prevLogs => {
              const isDuplicate = prevLogs.some(log =>
                log.userId === newLog.userId &&
                log.action === newLog.action &&
                Math.abs(new Date(log.checkInTime || log.checkOutTime) - new Date(newLog.checkInTime || newLog.checkOutTime)) < 2000
              );
              if (isDuplicate) return prevLogs;
              return [newLog, ...prevLogs];
            });
            setStats(prevStats => ({
              ...prevStats,
              totalCheckInsToday: message.type === 'CHECK_IN' ? prevStats.totalCheckInsToday + 1 : prevStats.totalCheckInsToday,
              totalCheckOutsToday: message.type === 'CHECK_OUT' ? prevStats.totalCheckOutsToday + 1 : prevStats.totalCheckOutsToday,
              currentlyInLibrary: message.type === 'CHECK_IN'
                ? prevStats.currentlyInLibrary + 1
                : prevStats.currentlyInLibrary - 1
            }));
          }
        });
      },
      (error) => {
        console.error('WebSocket connection error:', error);
      }
    );
    return () => {
      if (unsubscribe) unsubscribe();
    };
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const logsData = await librarianService.getAllAccessLogs();
      const statsData = await librarianService.getAccessLogStats();
      setAccessLogs(logsData);
      setStats(statsData);
    } catch (error) {
      console.error('Failed to fetch access logs:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatDateTime = (dateTimeString) => {
    if (!dateTimeString) return '';
    const date = new Date(dateTimeString);
    const time = date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    const dateStr = date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
    return `${time} ${dateStr}`;
  };

  // Get value for sorting/filtering
  const getLogValue = (log, column) => {
    switch (column) {
      case 'userCode': return log.userCode || '';
      case 'userName': return log.userName || '';
      case 'action': return log.action === 'CHECK_IN' ? 'Vào' : 'Ra';
      case 'time':
        return log.action === 'CHECK_IN' ? log.checkInTime : (log.checkOutTime || '');
      default: return '';
    }
  };

  const displayedLogs = useMemo(() => {
    let data = [...accessLogs];

    // Global search
    if (searchTerm) {
      const q = searchTerm.toLowerCase();
      data = data.filter(log =>
        log.userName.toLowerCase().includes(q) ||
        log.userCode.toLowerCase().includes(q)
      );
    }

    // Column filters
    Object.entries(columnFilters).forEach(([col, filterVal]) => {
      if (!filterVal) return;
      const q = filterVal.toLowerCase();
      if (col === 'action') {
        // Special: filter by action type
        if (q === 'vào' || q === 'vao' || q === 'check_in' || q === 'in') {
          data = data.filter(log => log.action === 'CHECK_IN');
        } else if (q === 'ra' || q === 'check_out' || q === 'out') {
          data = data.filter(log => log.action === 'CHECK_OUT');
        }
      } else if (col === 'time') {
        data = data.filter(log => {
          const timeStr = getLogValue(log, 'time');
          return timeStr && formatDateTime(timeStr).toLowerCase().includes(q);
        });
      } else {
        data = data.filter(log => getLogValue(log, col).toLowerCase().includes(q));
      }
    });

    // Date range filter
    if (startDate || endDate) {
      data = data.filter(log => {
        const timeStr = log.action === 'CHECK_IN' ? log.checkInTime : (log.checkOutTime || log.checkInTime);
        if (!timeStr) return false;
        const logDate = new Date(timeStr);
        const logDateStr = logDate.getFullYear() + '-' +
          String(logDate.getMonth() + 1).padStart(2, '0') + '-' +
          String(logDate.getDate()).padStart(2, '0');
        if (startDate && logDateStr < startDate) return false;
        if (endDate && logDateStr > endDate) return false;
        return true;
      });
    }

    // Sort
    if (sortConfig.column && sortConfig.direction) {
      data.sort((a, b) => {
        let valA = getLogValue(a, sortConfig.column);
        let valB = getLogValue(b, sortConfig.column);

        if (sortConfig.column === 'time') {
          valA = valA ? new Date(valA).getTime() : 0;
          valB = valB ? new Date(valB).getTime() : 0;
        } else {
          valA = valA.toLowerCase();
          valB = valB.toLowerCase();
        }

        if (valA < valB) return sortConfig.direction === 'asc' ? -1 : 1;
        if (valA > valB) return sortConfig.direction === 'asc' ? 1 : -1;
        return 0;
      });
    }

    return data;
  }, [searchTerm, accessLogs, columnFilters, sortConfig, startDate, endDate]);

  const totalPages = Math.ceil(displayedLogs.length / itemsPerPage);
  const paginatedLogs = useMemo(() => {
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    return displayedLogs.slice(startIndex, endIndex);
  }, [displayedLogs, currentPage, itemsPerPage]);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, columnFilters, sortConfig, itemsPerPage]);

  // Sort handler: cycle null -> asc -> desc -> null
  const handleSort = (column) => {
    setSortConfig(prev => {
      if (prev.column !== column) return { column, direction: 'asc' };
      if (prev.direction === 'asc') return { column, direction: 'desc' };
      return { column: null, direction: null };
    });
  };

  // Filter handler
  const handleFilterChange = (column, value) => {
    setColumnFilters(prev => ({ ...prev, [column]: value }));
  };

  const clearColumnFilter = (column) => {
    setColumnFilters(prev => ({ ...prev, [column]: '' }));
    setActiveFilterCol(null);
  };

  // Pagination helpers
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

  const handleUserClick = (log) => {
    if (log.userId) {
      setSelectedUserId(log.userId);
      setShowStudentModal(true);
    }
  };

  const handleExportToExcel = async () => {
    try {
      const token = localStorage.getItem('librarian_token');
      let url = `${API_BASE_URL}/slib/hce/access-logs/export`;
      const params = new URLSearchParams();
      if (startDate) params.append('startDate', startDate);
      if (endDate) params.append('endDate', endDate);
      if (params.toString()) url += '?' + params.toString();

      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        }
      });
      if (!response.ok) throw new Error('Không thể xuất báo cáo');

      const blob = await response.blob();
      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;

      const contentDisposition = response.headers.get('Content-Disposition');
      let filename = 'BaoCao_CheckIn_CheckOut.xlsx';
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="?(.+)"?/i);
        if (filenameMatch) filename = filenameMatch[1];
      }
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(downloadUrl);
    } catch (error) {
      console.error('Lỗi khi xuất báo cáo:', error);
      toast.error('Không thể xuất báo cáo. Vui lòng thử lại.');
    }
  };

  // Render sort icon
  const renderSortIcon = (column) => {
    if (sortConfig.column === column) {
      if (sortConfig.direction === 'asc') return <ArrowUp size={13} />;
      if (sortConfig.direction === 'desc') return <ArrowDown size={13} />;
    }
    return <ArrowUpDown size={13} />;
  };

  // Render filter icon with active state
  const renderFilterIcon = (column) => {
    const isActive = !!columnFilters[column];
    return (
      <Filter
        size={13}
        className={isActive ? 'cio-filter-active' : ''}
      />
    );
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
              {renderFilterIcon(column)}
            </button>
          </div>
          {activeFilterCol === column && (
            <div className="cio-filter-dropdown" ref={filterRef} onClick={e => e.stopPropagation()}>
              {column === 'action' ? (
                <div className="cio-filter-options">
                  <label className="cio-filter-option">
                    <input
                      type="radio"
                      name="action-filter"
                      checked={columnFilters.action === ''}
                      onChange={() => { handleFilterChange('action', ''); setActiveFilterCol(null); }}
                    />
                    Tất cả
                  </label>
                  <label className="cio-filter-option">
                    <input
                      type="radio"
                      name="action-filter"
                      checked={columnFilters.action === 'vào'}
                      onChange={() => { handleFilterChange('action', 'vào'); setActiveFilterCol(null); }}
                    />
                    Vào
                  </label>
                  <label className="cio-filter-option">
                    <input
                      type="radio"
                      name="action-filter"
                      checked={columnFilters.action === 'ra'}
                      onChange={() => { handleFilterChange('action', 'ra'); setActiveFilterCol(null); }}
                    />
                    Ra
                  </label>
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

  // Count active filters
  const activeFilterCount = Object.values(columnFilters).filter(Boolean).length;

  // Count visible columns
  const visibleColumnCount = Object.values(visibleColumns).filter(Boolean).length + 1;

  // Selection logic
  const toggleSelect = (logId) => {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(logId)) next.delete(logId); else next.add(logId);
      return next;
    });
  };

  const toggleSelectAll = () => {
    if (selectedIds.size === paginatedLogs.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(paginatedLogs.map(log => log.logId)));
    }
  };

  const isAllSelected = paginatedLogs.length > 0 && selectedIds.size === paginatedLogs.length;

  const handleDeleteBatch = async () => {
    if (selectedIds.size === 0) return;
    const ok = await confirm({
      title: 'Xoá lịch sử',
      message: `Bạn có chắc muốn xoá ${selectedIds.size} bản ghi đã chọn?`,
      variant: 'danger',
      confirmText: 'Xoá',
      cancelText: 'Huỷ',
    });
    if (!ok) return;
    setDeleting(true);
    try {
      const token = sessionStorage.getItem('librarian_token') || localStorage.getItem('librarian_token');
      // Deduplicate logIds (CHECK_IN + CHECK_OUT share the same logId)
      const uniqueIds = [...new Set(Array.from(selectedIds))];
      const res = await fetch(`${API_BASE_URL}/slib/hce/access-logs/batch`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ ids: uniqueIds }),
      });
      if (res.ok) {
        toast.success(`Đã xoá ${selectedIds.size} bản ghi thành công.`);
        setSelectedIds(new Set());
        fetchData();
      } else {
        toast.error('Không thể xoá bản ghi.');
      }
    } catch (err) {
      toast.error('Lỗi: ' + err.message);
    } finally {
      setDeleting(false);
    }
  };

  if (loading) {
    return (
      <div className="lib-container">
        <div className="lib-loading">
          <div className="lib-spinner" />
        </div>
      </div>
    );
  }

  return (
    <div className="lib-container">
      {/* Page Title */}
      <div className="lib-page-title">
        <h1>DANH SÁCH SINH VIÊN</h1>
      </div>

      {/* Table Panel */}
      <div className="lib-panel">
        {/* Toolbar: Search + Column Toggle + Count + Export */}
        <div className="cio-toolbar">
          <div className="lib-search">
            <Search size={16} className="lib-search-icon" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Tìm kiếm"
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
                  { key: 'userCode', label: 'Mã sinh viên' },
                  { key: 'userName', label: 'Tên sinh viên' },
                  { key: 'action', label: 'Hành động' },
                  { key: 'time', label: 'Thời gian' },
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
          <span className="cio-result-count">
            {activeFilterCount > 0 && (
              <span className="cio-active-filters">
                {activeFilterCount} bộ lọc |{' '}
              </span>
            )}
            Tổng số <strong>{displayedLogs.length}</strong> kết quả
          </span>

          {/* Batch delete */}
          {selectedIds.size > 0 && (
            <button className="sr-delete-btn" onClick={handleDeleteBatch} disabled={deleting}>
              <Trash2 size={14} />
              {deleting ? "Đang xoá..." : `Xoá (${selectedIds.size})`}
            </button>
          )}

          <div className="cio-export-controls">
            <input
              type="date"
              className="cio-date-input"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              placeholder="Từ ngày"
            />
            <span style={{ color: '#666' }}>-</span>
            <input
              type="date"
              className="cio-date-input"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              placeholder="Đến ngày"
            />
            <button className="lib-btn primary" onClick={handleExportToExcel}>
              In báo cáo Excel
            </button>
          </div>
        </div>

        {/* Table */}
        <div className="cio-table-wrapper">
          <table className="cio-table">
            <thead>
              <tr>
                <th className="sr-checkbox-col">
                  <input type="checkbox" checked={isAllSelected} onChange={toggleSelectAll} style={{ accentColor: '#FF751F' }} />
                </th>
                {visibleColumns.userCode && renderColumnHeader('userCode', 'Mã sinh viên')}
                {visibleColumns.userName && renderColumnHeader('userName', 'Tên sinh viên')}
                {visibleColumns.action && renderColumnHeader('action', 'Hành động')}
                {visibleColumns.time && renderColumnHeader('time', 'Thời gian')}
                <th style={{ width: 80, textAlign: 'center' }}>Tùy chọn</th>
              </tr>
            </thead>
            <tbody>
              {paginatedLogs.length === 0 ? (
                <tr>
                  <td colSpan={visibleColumnCount + 1} className="cio-table-empty">
                    Không có dữ liệu
                  </td>
                </tr>
              ) : (
                paginatedLogs.map((log) => (
                  <tr
                    key={`${log.logId}-${log.action}`}
                    className={`cio-table-row${selectedIds.has(log.logId) ? ' selected' : ''}`}
                  >
                    <td className="sr-checkbox-col" onClick={(e) => e.stopPropagation()}>
                      <input type="checkbox" checked={selectedIds.has(log.logId)} onChange={() => toggleSelect(log.logId)} style={{ accentColor: '#FF751F' }} />
                    </td>
                    {visibleColumns.userCode && <td className="cio-code-cell">{log.userCode}</td>}
                    {visibleColumns.userName && <td className="cio-name-cell">{log.userName}</td>}
                    {visibleColumns.action && (
                      <td>
                        <span className={`cio-action-badge ${log.action === 'CHECK_IN' ? 'in' : 'out'}`}>
                          {log.action === 'CHECK_IN' ? 'Vào' : 'Ra'}
                        </span>
                      </td>
                    )}
                    {visibleColumns.time && (
                      <td className="cio-time-cell">
                        {log.action === 'CHECK_IN'
                          ? formatDateTime(log.checkInTime)
                          : (log.checkOutTime ? formatDateTime(log.checkOutTime) : '-')
                        }
                      </td>
                    )}
                    <td style={{ textAlign: 'center' }}>
                      <button
                        className="lib-btn secondary"
                        style={{ padding: '4px 8px', fontSize: 12 }}
                        onClick={() => handleUserClick(log)}
                        title="Xem chi tiết"
                      >
                        Chi tiết
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="cio-pagination">
          <div className="cio-page-size">
            <span>Số hàng mỗi trang:</span>
            <select
              value={itemsPerPage}
              onChange={(e) => setItemsPerPage(Number(e.target.value))}
            >
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
            </select>
          </div>
          {totalPages > 1 && (
            <div className="cio-pagination-right">
              <button
                onClick={() => setCurrentPage(prev => Math.max(1, prev - 1))}
                disabled={currentPage === 1}
                className="cio-page-btn"
              >
                &lt;
              </button>
              <div className="cio-page-numbers">
                {getPageNumbers().map((page, idx) => (
                  page === '...' ? (
                    <span key={`ellipsis-${idx}`} className="cio-page-ellipsis">...</span>
                  ) : (
                    <button
                      key={page}
                      onClick={() => setCurrentPage(page)}
                      className={`cio-page-btn ${currentPage === page ? 'active' : ''}`}
                    >
                      {page}
                    </button>
                  )
                ))}
              </div>
              <button
                onClick={() => setCurrentPage(prev => Math.min(totalPages, prev + 1))}
                disabled={currentPage === totalPages}
                className="cio-page-btn"
              >
                &gt;
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Student Detail Modal */}
      <StudentDetailModal
        userId={selectedUserId}
        isOpen={showStudentModal}
        onClose={() => {
          setShowStudentModal(false);
          setSelectedUserId(null);
        }}
      />

      {/* Close menus when clicking outside */}
      {showColumnMenu && (
        <div
          style={{ position: 'fixed', inset: 0, zIndex: 50 }}
          onClick={() => setShowColumnMenu(false)}
        />
      )}
    </div>
  );
};

export default CheckInOut;