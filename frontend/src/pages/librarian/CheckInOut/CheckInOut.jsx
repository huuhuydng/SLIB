import React, { useState, useMemo, useEffect } from 'react';
import { Search } from 'lucide-react';
import "../../../styles/librarian/librarian-shared.css";
import "../../../styles/librarian/CheckInOut.css";
import librarianService from "../../../services/librarianService";
import StudentDetailModal from "../../../components/librarian/StudentDetailModal";
import websocketService from "../../../services/websocketService";


const CheckInOut = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [filterSort, setFilterSort] = useState('');

  // Date filter states - mặc định ngày hôm nay
  const todayStr = new Date().toISOString().split('T')[0];
  const [startDate, setStartDate] = useState(todayStr);
  const [endDate, setEndDate] = useState(todayStr);

  // Time filter states
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');

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
  const itemsPerPage = 10;

  // Convert YYYY-MM-DD to DD/MM/YYYY for display
  const formatDateDisplay = (isoDate) => {
    if (!isoDate) return '';
    const [year, month, day] = isoDate.split('-');
    return `${day}/${month}/${year}`;
  };

  // Fetch data on component mount and when date filter changes
  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 30000);
    return () => clearInterval(interval);
  }, [startDate, endDate]);

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
      let logsData;
      if (startDate || endDate) {
        logsData = await librarianService.getAccessLogsByDateRange(startDate, endDate);
      } else {
        logsData = await librarianService.getAllAccessLogs();
      }
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

  const displayedLogs = useMemo(() => {
    let data = accessLogs.filter(log =>
      log.userName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      log.userCode.toLowerCase().includes(searchTerm.toLowerCase())
    );
    if (filterSort === 'checkin') {
      data = data.filter(log => log.checkOutTime === null);
    } else if (filterSort === 'checkout') {
      data = data.filter(log => log.checkOutTime !== null);
    }

    // Lọc theo thời gian (giờ)
    if (startTime || endTime) {
      data = data.filter(log => {
        const logTime = log.action === 'CHECK_IN' ? log.checkInTime : log.checkOutTime;
        if (!logTime) return false;
        const date = new Date(logTime);
        const hours = date.getHours();
        const minutes = date.getMinutes();
        const logMinutes = hours * 60 + minutes;

        if (startTime) {
          const [sh, sm] = startTime.split(':').map(Number);
          if (logMinutes < sh * 60 + sm) return false;
        }
        if (endTime) {
          const [eh, em] = endTime.split(':').map(Number);
          if (logMinutes > eh * 60 + em) return false;
        }
        return true;
      });
    }

    return data;
  }, [searchTerm, filterSort, accessLogs, startTime, endTime]);

  const totalPages = Math.ceil(displayedLogs.length / itemsPerPage);
  const paginatedLogs = useMemo(() => {
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    return displayedLogs.slice(startIndex, endIndex);
  }, [displayedLogs, currentPage, itemsPerPage]);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, filterSort, startTime, endTime]);

  const handleFilterClick = (type) => {
    setFilterSort(prev => prev === type ? '' : type);
  };

  const clearDateFilter = () => {
    setStartDate('');
    setEndDate('');
    setStartTime('');
    setEndTime('');
  };

  // Hàm tính các trang cần hiện (rút gọn)
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
      let url = `${import.meta.env.VITE_API_URL || "http://localhost:8080"}/slib/hce/access-logs/export`;
      const params = new URLSearchParams();
      if (startDate) params.append('startDate', startDate);
      if (endDate) params.append('endDate', endDate);
      if (params.toString()) url += '?' + params.toString();

      const response = await fetch(url, { method: 'GET', headers: { 'Content-Type': 'application/json' } });
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
      alert('Không thể xuất báo cáo. Vui lòng thử lại.');
    }
  };

  const getMinDate = () => {
    const date = new Date();
    date.setMonth(date.getMonth() - 3);
    return date.toISOString().split('T')[0];
  };

  const getMaxDate = () => new Date().toISOString().split('T')[0];

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
      {/* Page Title + Inline Stats */}
      <div className="lib-page-title">
        <h1>Kiểm tra ra/vào</h1>
        <div className="lib-inline-stats">
          <span className="lib-inline-stat">
            <span className="dot blue"></span>
            Lượt vào <strong>{stats.totalCheckInsToday}</strong>
          </span>
          <span className="lib-inline-stat">
            <span className="dot red"></span>
            Lượt ra <strong>{stats.totalCheckOutsToday}</strong>
          </span>
          <span className="lib-inline-stat">
            <span className="dot green"></span>
            Đang trong TV <strong>{stats.currentlyInLibrary}</strong>
          </span>
        </div>
      </div>

      {/* Table Panel */}
      <div className="lib-panel">
        <div className="lib-panel-header cio-header-layout">
          {/* Hàng 1: Title + Bộ lọc ngày giờ */}
          <div className="cio-header-row">
            <h3 className="lib-panel-title">Danh sách sinh viên ra vào</h3>
            <div className="cio-date-filters">
              <div className="cio-date-field">
                <label>Từ ngày</label>
                <div className="cio-date-input-wrapper">
                  <input
                    type="text"
                    value={formatDateDisplay(startDate)}
                    readOnly
                    placeholder="dd/mm/yyyy"
                    onClick={() => document.getElementById('startDatePicker').showPicker()}
                    className="cio-date-display"
                  />
                  <input
                    id="startDatePicker"
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    min={getMinDate()}
                    max={getMaxDate()}
                    className="cio-date-hidden"
                  />
                </div>
              </div>
              <span className="cio-date-separator">-</span>
              <div className="cio-date-field">
                <label>Đến ngày</label>
                <div className="cio-date-input-wrapper">
                  <input
                    type="text"
                    value={formatDateDisplay(endDate)}
                    readOnly
                    placeholder="dd/mm/yyyy"
                    onClick={() => document.getElementById('endDatePicker').showPicker()}
                    className="cio-date-display"
                  />
                  <input
                    id="endDatePicker"
                    type="date"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    min={getMinDate()}
                    max={getMaxDate()}
                    className="cio-date-hidden"
                  />
                </div>
              </div>
              <span className="cio-date-separator-dot"></span>
              <div className="cio-date-field">
                <label>Từ giờ</label>
                <input
                  type="time"
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                  className="cio-time-input"
                />
              </div>
              <span className="cio-date-separator">-</span>
              <div className="cio-date-field">
                <label>Đến giờ</label>
                <input
                  type="time"
                  value={endTime}
                  onChange={(e) => setEndTime(e.target.value)}
                  className="cio-time-input"
                />
              </div>
              {(startDate || endDate || startTime || endTime) && (
                <button className="lib-btn ghost cio-clear-btn" onClick={clearDateFilter}>
                  Xóa lọc
                </button>
              )}
            </div>
          </div>

          {/* Hàng 2: Tìm kiếm + Filter Tabs */}
          <div className="cio-header-row">
            <div className="lib-search">
              <Search size={16} className="lib-search-icon" />
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Tìm tên hoặc mã SV..."
              />
            </div>
            <div className="lib-tabs" style={{ margin: 0 }}>
              <button
                className={`lib-tab ${filterSort === '' ? 'active' : ''}`}
                onClick={() => setFilterSort('')}
              >
                Tất cả
              </button>
              <button
                className={`lib-tab ${filterSort === 'checkin' ? 'active' : ''}`}
                onClick={() => handleFilterClick('checkin')}
              >
                Vào
              </button>
              <button
                className={`lib-tab ${filterSort === 'checkout' ? 'active' : ''}`}
                onClick={() => handleFilterClick('checkout')}
              >
                Ra
              </button>
            </div>
          </div>
        </div>

        {/* Table */}
        <div className="cio-table-wrapper">
          <table className="cio-table">
            <thead>
              <tr>
                <th>Tên sinh viên</th>
                <th>Mã số sinh viên</th>
                <th>Hành động</th>
                <th>Thời gian</th>
              </tr>
            </thead>
            <tbody>
              {paginatedLogs.length === 0 ? (
                <tr>
                  <td colSpan="4" className="cio-table-empty">
                    Không có dữ liệu
                  </td>
                </tr>
              ) : (
                paginatedLogs.map((log) => (
                  <tr
                    key={`${log.logId}-${log.action}`}
                    onClick={() => handleUserClick(log)}
                    className="cio-table-row"
                  >
                    <td className="cio-name-cell">{log.userName}</td>
                    <td className="cio-code-cell">{log.userCode}</td>
                    <td>
                      <span className={`cio-action-badge ${log.action === 'CHECK_IN' ? 'in' : 'out'}`}>
                        {log.action === 'CHECK_IN' ? 'Vào' : 'Ra'}
                      </span>
                    </td>
                    <td className="cio-time-cell">
                      {log.action === 'CHECK_IN'
                        ? formatDateTime(log.checkInTime)
                        : (log.checkOutTime ? formatDateTime(log.checkOutTime) : '-')
                      }
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="cio-pagination">
            <button
              onClick={() => setCurrentPage(prev => Math.max(1, prev - 1))}
              disabled={currentPage === 1}
              className="cio-page-btn"
            >
              Trước
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
              Sau
            </button>
            <div className="cio-page-info">
              Trang {currentPage} / {totalPages} ({displayedLogs.length} kết quả)
            </div>
          </div>
        )}

        {/* Export Button */}
        <div className="cio-export">
          <button className="lib-btn primary" onClick={handleExportToExcel}>
            In báo cáo Excel
          </button>
        </div>
      </div>
      {/* Student Detail Modal (chỉ đọc) */}
      <StudentDetailModal
        userId={selectedUserId}
        isOpen={showStudentModal}
        onClose={() => {
          setShowStudentModal(false);
          setSelectedUserId(null);
        }}
      />
    </div>
  );
};

export default CheckInOut;