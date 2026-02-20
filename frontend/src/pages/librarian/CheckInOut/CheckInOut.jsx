
import React, { useState, useMemo, useEffect } from 'react';
import {
  LogOut,
  Filter,
  LogIn,
  Users,
  Check,
  ChevronDown,
  Search,
  FileDown
} from 'lucide-react';
import Header from "../../../components/shared/Header";
import "../../../styles/librarian/CheckInOut.css";
import { handleLogout } from "../../../utils/auth";
import librarianService from "../../../services/librarianService";
import userService from "../../../services/userService";
import UserDetailsModal from "../../../components/admin/UserDetailsModal";
import websocketService from "../../../services/websocketService";


const CheckInOut = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [filterSort, setFilterSort] = useState('');
  const [selectedZone, setSelectedZone] = useState('');

  // Date filter states
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  // State for real data
  const [accessLogs, setAccessLogs] = useState([]);
  const [stats, setStats] = useState({
    totalCheckInsToday: 0,
    totalCheckOutsToday: 0,
    currentlyInLibrary: 0
  });
  const [loading, setLoading] = useState(true);

  // Modal state
  const [showUserDetailsModal, setShowUserDetailsModal] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [allUsers, setAllUsers] = useState([]);

  // Pagination state
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  // Convert YYYY-MM-DD to DD/MM/YYYY for display
  const formatDateDisplay = (isoDate) => {
    if (!isoDate) return '';
    const [year, month, day] = isoDate.split('-');
    return `${day}/${month}/${year}`;
  };

  // Convert DD/MM/YYYY to YYYY-MM-DD for API
  const formatDateISO = (displayDate) => {
    if (!displayDate || displayDate.length !== 10) return '';
    const [day, month, year] = displayDate.split('/');
    return `${year}-${month}-${day}`;
  };

  // Fetch users on component mount
  useEffect(() => {
    fetchAllUsers();
  }, []);

  // Fetch data on component mount and when date filter changes
  useEffect(() => {
    fetchData();
    // Auto refresh every 30 seconds (fallback)
    const interval = setInterval(fetchData, 30000);
    return () => clearInterval(interval);
  }, [startDate, endDate]);

  // WebSocket real-time updates
  useEffect(() => {
    console.log('🔌 Connecting to WebSocket...');
    let unsubscribe = null;

    // Connect to WebSocket
    websocketService.connect(
      () => {
        console.log('✅ WebSocket connected successfully');

        // Subscribe to access-logs topic
        unsubscribe = websocketService.subscribe('/topic/access-logs', (message) => {
          console.log('📨 Received real-time update:', message);

          // Add new record to the top of the list without full page refresh
          if (message.type === 'CHECK_IN' || message.type === 'CHECK_OUT') {
            console.log('➕ Adding new record to list...');

            // Create new log entry from WebSocket message
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

            // Check for duplicates before adding (prevent double data)
            setAccessLogs(prevLogs => {
              // Check if this exact record already exists (same user, action, and recent time)
              const isDuplicate = prevLogs.some(log =>
                log.userId === newLog.userId &&
                log.action === newLog.action &&
                Math.abs(new Date(log.checkInTime || log.checkOutTime) - new Date(newLog.checkInTime || newLog.checkOutTime)) < 2000 // Within 2 seconds
              );

              if (isDuplicate) {
                console.log('⚠️ Duplicate detected, skipping...');
                return prevLogs;
              }

              // Prepend new log to existing list
              return [newLog, ...prevLogs];
            });

            // Update stats locally (only if not duplicate)
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
        console.error('❌ WebSocket connection error:', error);
      }
    );

    // Cleanup on unmount
    return () => {
      console.log('🔌 Unsubscribing from WebSocket...');
      if (unsubscribe) {
        unsubscribe();
      }
    };
  }, []); // Only run once on mount

  const fetchAllUsers = async () => {
    try {
      const users = await userService.getAllUsers();
      setAllUsers(users);
    } catch (error) {
      console.error('Failed to fetch users:', error);
    }
  };

  const fetchData = async () => {
    try {
      setLoading(true);

      let logsData;
      // If any date is selected, use date range API
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

  // Format datetime
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

    // Filter by check in/check out status
    if (filterSort === 'checkin') {
      // Show only those who have checked in but not checked out yet
      data = data.filter(log => log.checkOutTime === null);
    } else if (filterSort === 'checkout') {
      // Show only those who have checked out
      data = data.filter(log => log.checkOutTime !== null);
    }

    return data;
  }, [searchTerm, filterSort, selectedZone, accessLogs]);

  // Pagination logic
  const totalPages = Math.ceil(displayedLogs.length / itemsPerPage);
  const paginatedLogs = useMemo(() => {
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    return displayedLogs.slice(startIndex, endIndex);
  }, [displayedLogs, currentPage, itemsPerPage]);

  // Reset to page 1 when search or filter changes
  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, filterSort, selectedZone]);

  const handleFilterClick = (type) => {
    // Toggle if clicking the same one, otherwise set new
    setFilterSort(prev => prev === type ? '' : type);
    setIsFilterOpen(false);
  };

  const handleZoneSelect = (zone) => {
    setSelectedZone(prev => prev === zone ? '' : zone);
    setIsFilterOpen(false);
  };

  const clearDateFilter = () => {
    setStartDate('');
    setEndDate('');
  };

  const handleUserClick = (log) => {
    // Find user by userId from allUsers
    const user = allUsers.find(u => u.id === log.userId);
    if (user) {
      setSelectedUser(user);
      setShowUserDetailsModal(true);
    } else {
      console.warn('User not found:', log.userId);
    }
  };

  const handleExportToExcel = async () => {
    try {
      // Build URL with query params
      let url = 'http://localhost:8080/slib/hce/access-logs/export';
      const params = new URLSearchParams();

      if (startDate) params.append('startDate', startDate);
      if (endDate) params.append('endDate', endDate);

      if (params.toString()) {
        url += '?' + params.toString();
      }

      // Fetch the Excel file
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error('Không thể xuất báo cáo');
      }

      // Convert response to blob
      const blob = await response.blob();

      // Create download link
      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;

      // Get filename from Content-Disposition header or use default
      const contentDisposition = response.headers.get('Content-Disposition');
      let filename = 'BaoCao_CheckIn_CheckOut.xlsx';
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="?(.+)"?/i);
        if (filenameMatch) {
          filename = filenameMatch[1];
        }
      }

      link.download = filename;
      document.body.appendChild(link);
      link.click();

      // Cleanup
      document.body.removeChild(link);
      window.URL.revokeObjectURL(downloadUrl);

      console.log('Xuất báo cáo thành công!');
    } catch (error) {
      console.error('Lỗi khi xuất báo cáo:', error);
      alert('Không thể xuất báo cáo. Vui lòng thử lại.');
    }
  };

  // Calculate min and max dates (3 months ago to today)
  const getMinDate = () => {
    const date = new Date();
    date.setMonth(date.getMonth() - 3);
    return date.toISOString().split('T')[0];
  };

  const getMaxDate = () => {
    return new Date().toISOString().split('T')[0];
  };

  if (loading) {
    return (
      <>
        <Header
          onLogout={handleLogout}
        />
        <div style={{
          padding: '2rem',
          textAlign: 'center',
          fontSize: '16px',
          color: '#6b7280'
        }}>
          Đang tải dữ liệu...
        </div>
      </>
    );
  }

  return (
    <>
      <Header
        onLogout={handleLogout}
      />

      <div style={{
        padding: '2rem',
        maxWidth: '1400px',
        margin: '0 auto',
        backgroundColor: '#f9fafb',
        minHeight: 'auto'
      }}>
        {/* Page Title */}
        <h1 className="page-title" style={{
          fontSize: '28px',
          fontWeight: '700',
          color: '#1f2937',
          marginBottom: '24px'
        }}>Kiểm tra ra/vào</h1>

        {/* Stats Cards */}
        <section className="stats-row" style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: '20px',
          marginBottom: '24px',
          padding: '0 32px'
        }}>
          <StatCard
            number={stats.totalCheckInsToday}
            label="Đã check in hôm nay"
            icon={<LogIn size={28} color="#6366f1" />}
            iconBg="#e0e7ff"
          />
          <StatCard
            number={stats.totalCheckOutsToday}
            label="Đã check out hôm nay"
            icon={<LogOut size={28} color="#ec4899" />}
            iconBg="#fce7f3"
          />
          <StatCard
            number={stats.currentlyInLibrary}
            label="Đang trong thư viện"
            icon={<Users size={28} color="#8b5cf6" />}
            iconBg="#ede9fe"
          />
        </section>

        {/* Table Panel */}
        <section className="table-panel" style={{
          backgroundColor: '#fff',
          borderRadius: '12px',
          padding: '24px',
          margin: '0 32px',
          boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
        }}>
          <div className="panel-header" style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: '20px'
          }}>
            <h2 style={{
              fontSize: '18px',
              fontWeight: '600',
              color: '#1f2937'
            }}>Danh sách sinh viên ra vào</h2>

            <div style={{ display: 'flex', gap: '12px', alignItems: 'flex-end' }}>
              {/* Date Filter Section */}
              <div style={{ display: 'flex', gap: '8px', alignItems: 'flex-end' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <label style={{ fontSize: '12px', color: '#6b7280', fontWeight: '500' }}>
                    Từ ngày
                  </label>
                  <div style={{ position: 'relative' }}>
                    <input
                      type="text"
                      value={formatDateDisplay(startDate)}
                      readOnly
                      placeholder="dd/mm/yyyy"
                      onClick={() => document.getElementById('startDatePicker').showPicker()}
                      style={{
                        padding: '8px 12px',
                        border: '1px solid #e5e7eb',
                        borderRadius: '8px',
                        fontSize: '14px',
                        cursor: 'pointer',
                        width: '150px',
                        backgroundColor: 'white'
                      }}
                    />
                    <input
                      id="startDatePicker"
                      type="date"
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      min={getMinDate()}
                      max={getMaxDate()}
                      style={{
                        position: 'absolute',
                        opacity: 0,
                        width: 0,
                        height: 0,
                        pointerEvents: 'none'
                      }}
                    />
                  </div>
                </div>
                <span style={{ color: '#6b7280', paddingBottom: '8px' }}>-</span>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <label style={{ fontSize: '12px', color: '#6b7280', fontWeight: '500' }}>
                    Đến ngày
                  </label>
                  <div style={{ position: 'relative' }}>
                    <input
                      type="text"
                      value={formatDateDisplay(endDate)}
                      readOnly
                      placeholder="dd/mm/yyyy"
                      onClick={() => document.getElementById('endDatePicker').showPicker()}
                      style={{
                        padding: '8px 12px',
                        border: '1px solid #e5e7eb',
                        borderRadius: '8px',
                        fontSize: '14px',
                        cursor: 'pointer',
                        width: '150px',
                        backgroundColor: 'white'
                      }}
                    />
                    <input
                      id="endDatePicker"
                      type="date"
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                      min={getMinDate()}
                      max={getMaxDate()}
                      style={{
                        position: 'absolute',
                        opacity: 0,
                        width: 0,
                        height: 0,
                        pointerEvents: 'none'
                      }}
                    />
                  </div>
                </div>
                {(startDate || endDate) && (
                  <button
                    onClick={clearDateFilter}
                    style={{
                      padding: '8px 16px',
                      backgroundColor: '#ef4444',
                      color: 'white',
                      border: 'none',
                      borderRadius: '8px',
                      fontSize: '14px',
                      cursor: 'pointer',
                      fontWeight: '500'
                    }}
                  >
                    Xóa lọc
                  </button>
                )}
              </div>

              {/* Search Input */}
              <div style={{ display: 'flex', alignItems: 'center', position: 'relative' }}>
                <Search size={18} style={{ position: 'absolute', left: '12px', color: '#9ca3af' }} />
                <input
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Tìm tên hoặc mã SV..."
                  style={{
                    padding: '8px 12px 8px 38px',
                    border: '1px solid #e5e7eb',
                    borderRadius: '8px',
                    fontSize: '14px',
                    width: '220px',
                    outline: 'none',
                    transition: 'border-color 0.2s',
                  }}
                  onFocus={(e) => e.target.style.borderColor = '#f76b1c'}
                  onBlur={(e) => e.target.style.borderColor = '#e5e7eb'}
                />
              </div>

              <div className="filter-container" style={{ position: 'relative' }}>
                <button className="filter-btn" onClick={() => setIsFilterOpen(!isFilterOpen)} style={{
                  padding: '8px 12px',
                  border: '1px solid #e5e7eb',
                  borderRadius: '8px',
                  backgroundColor: filterSort ? '#f76b1c' : '#fff',
                  color: filterSort ? '#fff' : '#1f2937',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px'
                }}>
                  <Filter size={18} />
                </button>
                {isFilterOpen && (
                  <div className="filter-dropdown" style={{
                    position: 'absolute',
                    top: '100%',
                    right: 0,
                    marginTop: '8px',
                    backgroundColor: '#fff',
                    border: '1px solid #e5e7eb',
                    borderRadius: '8px',
                    boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
                    minWidth: '180px',
                    zIndex: 10
                  }}>
                    <div
                      className={`filter-item ${filterSort === 'checkin' ? 'active' : ''}`}
                      onClick={() => handleFilterClick('checkin')}
                      style={{
                        padding: '10px 16px',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        fontSize: '14px',
                        color: filterSort === 'checkin' ? '#f76b1c' : '#1f2937',
                        backgroundColor: filterSort === 'checkin' ? '#fff5f0' : 'transparent',
                        borderBottom: '1px solid #e5e7eb'
                      }}
                    >
                      Check In
                      {filterSort === 'checkin' && <Check size={14} />}
                    </div>
                    <div
                      className={`filter-item ${filterSort === 'checkout' ? 'active' : ''}`}
                      onClick={() => handleFilterClick('checkout')}
                      style={{
                        padding: '10px 16px',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        fontSize: '14px',
                        color: filterSort === 'checkout' ? '#f76b1c' : '#1f2937',
                        backgroundColor: filterSort === 'checkout' ? '#fff5f0' : 'transparent'
                      }}
                    >
                      Check Out
                      {filterSort === 'checkout' && <Check size={14} />}
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>

          <div className="table-wrapper">
            <table className="log-table" style={{
              width: '100%',
              borderCollapse: 'collapse'
            }}>
              <thead>
                <tr style={{
                  borderBottom: '2px solid #e5e7eb'
                }}>
                  <th style={{
                    padding: '12px',
                    textAlign: 'left',
                    fontSize: '13px',
                    fontWeight: '600',
                    color: '#6b7280',
                    textTransform: 'uppercase'
                  }}>Tên sinh viên</th>
                  <th style={{
                    padding: '12px',
                    textAlign: 'left',
                    fontSize: '13px',
                    fontWeight: '600',
                    color: '#6b7280',
                    textTransform: 'uppercase'
                  }}>Mã số sinh viên</th>
                  <th style={{
                    padding: '12px',
                    textAlign: 'left',
                    fontSize: '13px',
                    fontWeight: '600',
                    color: '#6b7280',
                    textTransform: 'uppercase'
                  }}>Hành động</th>
                  <th style={{
                    padding: '12px',
                    textAlign: 'left',
                    fontSize: '13px',
                    fontWeight: '600',
                    color: '#6b7280',
                    textTransform: 'uppercase'
                  }}>Thời gian</th>
                </tr>
              </thead>
              <tbody>
                {paginatedLogs.length === 0 ? (
                  <tr>
                    <td colSpan="4" style={{
                      padding: '40px',
                      textAlign: 'center',
                      color: '#9ca3af',
                      fontSize: '14px'
                    }}>
                      Không có dữ liệu
                    </td>
                  </tr>
                ) : (
                  paginatedLogs.map((log) => (
                    <tr key={`${log.logId}-${log.action}`} onClick={() => handleUserClick(log)} style={{
                      borderBottom: '1px solid #f3f4f6',
                      cursor: 'pointer',
                      transition: 'background-color 0.2s'
                    }}
                      onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#fff5f0'}
                      onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                    >
                      <td className="fw-500" style={{
                        padding: '12px',
                        fontSize: '14px',
                        color: '#1f2937',
                        fontWeight: '500'
                      }}>{log.userName}</td>
                      <td className="code-cell" style={{
                        padding: '12px',
                        fontSize: '14px',
                        color: '#6b7280'
                      }}>{log.userCode}</td>
                      <td style={{
                        padding: '12px'
                      }}>
                        <span className={`badge action ${log.action === 'CHECK_IN' ? 'in' : 'out'}`} style={{
                          display: 'inline-block',
                          padding: '4px 12px',
                          borderRadius: '12px',
                          fontSize: '12px',
                          fontWeight: '600',
                          backgroundColor: log.action === 'CHECK_IN' ? '#dcfce7' : '#fee2e2',
                          color: log.action === 'CHECK_IN' ? '#166534' : '#991b1b'
                        }}>
                          {log.action === 'CHECK_IN' ? 'Check in' : 'Check out'}
                        </span>
                      </td>
                      <td className="time-cell" style={{
                        padding: '12px',
                        fontSize: '14px',
                        color: '#6b7280'
                      }}>
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

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div style={{
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              gap: '8px',
              marginTop: '24px',
              padding: '12px'
            }}>
              <button
                onClick={() => setCurrentPage(prev => Math.max(1, prev - 1))}
                disabled={currentPage === 1}
                style={{
                  padding: '8px 16px',
                  border: '1px solid #e5e7eb',
                  borderRadius: '8px',
                  backgroundColor: currentPage === 1 ? '#f3f4f6' : '#fff',
                  color: currentPage === 1 ? '#9ca3af' : '#1f2937',
                  cursor: currentPage === 1 ? 'not-allowed' : 'pointer',
                  fontSize: '14px',
                  fontWeight: '500'
                }}
              >
                Trước
              </button>

              <div style={{
                display: 'flex',
                gap: '4px'
              }}>
                {Array.from({ length: totalPages }, (_, i) => i + 1).map(page => (
                  <button
                    key={page}
                    onClick={() => setCurrentPage(page)}
                    style={{
                      padding: '8px 12px',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      backgroundColor: currentPage === page ? '#f76b1c' : '#fff',
                      color: currentPage === page ? '#fff' : '#1f2937',
                      cursor: 'pointer',
                      fontSize: '14px',
                      fontWeight: currentPage === page ? '600' : '500',
                      minWidth: '40px'
                    }}
                  >
                    {page}
                  </button>
                ))}
              </div>

              <button
                onClick={() => setCurrentPage(prev => Math.min(totalPages, prev + 1))}
                disabled={currentPage === totalPages}
                style={{
                  padding: '8px 16px',
                  border: '1px solid #e5e7eb',
                  borderRadius: '8px',
                  backgroundColor: currentPage === totalPages ? '#f3f4f6' : '#fff',
                  color: currentPage === totalPages ? '#9ca3af' : '#1f2937',
                  cursor: currentPage === totalPages ? 'not-allowed' : 'pointer',
                  fontSize: '14px',
                  fontWeight: '500'
                }}
              >
                Sau
              </button>

              <div style={{
                marginLeft: '16px',
                fontSize: '14px',
                color: '#6b7280'
              }}>
                Trang {currentPage} / {totalPages} ({displayedLogs.length} kết quả)
              </div>
            </div>
          )}

          {/* Export Button */}
          <div style={{
            display: 'flex',
            justifyContent: 'center',
            marginTop: '24px',
            paddingTop: '24px',
            borderTop: '1px solid #e5e7eb'
          }}>
            <button
              onClick={handleExportToExcel}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '12px 24px',
                backgroundColor: '#10b981',
                color: 'white',
                border: 'none',
                borderRadius: '8px',
                fontSize: '14px',
                fontWeight: '600',
                cursor: 'pointer',
                transition: 'background-color 0.2s',
              }}
              onMouseEnter={(e) => e.target.style.backgroundColor = '#059669'}
              onMouseLeave={(e) => e.target.style.backgroundColor = '#10b981'}
            >
              <FileDown size={18} />
              In báo cáo Excel
            </button>
          </div>
        </section>
      </div>

      {/* User Details Modal */}
      <UserDetailsModal
        user={selectedUser}
        isOpen={showUserDetailsModal}
        onClose={() => {
          setShowUserDetailsModal(false);
          setSelectedUser(null);
        }}
      />
    </>
  );
};

// --- Sub Components ---

const StatCard = ({ number, label, icon, iconBg }) => (
  <div className="stat-card" style={{
    backgroundColor: '#fff',
    borderRadius: '12px',
    padding: '20px',
    display: 'flex',
    alignItems: 'center',
    gap: '16px',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
  }}>
    <div className="stat-icon-wrapper" style={{
      backgroundColor: iconBg,
      padding: '12px',
      borderRadius: '12px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }}>
      {icon}
    </div>
    <div className="stat-info">
      <div className="stat-number" style={{
        fontSize: '24px',
        fontWeight: '700',
        color: '#1f2937',
        marginBottom: '4px'
      }}>{number}</div>
      <div className="stat-label" style={{
        fontSize: '14px',
        color: '#6b7280'
      }}>{label}</div>
    </div>
  </div>
);

export default CheckInOut;