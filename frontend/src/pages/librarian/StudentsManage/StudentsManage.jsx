import React, { useState, useMemo, useEffect, useCallback, useRef } from 'react';
import { Search, Loader2, ArrowUpDown, ArrowUp, ArrowDown, Filter, X, SlidersHorizontal } from 'lucide-react';
import '../../../styles/librarian/librarian-shared.css';
import '../../../styles/librarian/CheckInOut.css';
import '../../../styles/librarian/StudentsManage.css';
import userService from '../../../services/auth/userService';
import librarianService from '../../../services/librarian/librarianService';
import StudentDetailModal from '../../../components/librarian/StudentDetailModal';

const STATUS_OPTIONS = [
  { value: '', label: 'Tất cả' },
  { value: 'active', label: 'Hoạt động' },
  { value: 'locked', label: 'Đã khóa' },
];

const StudentsManage = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);

  // Người dùng thư viện đang có mặt
  const [inLibrary, setInLibrary] = useState([]);
  const [stats, setStats] = useState({ totalCheckInsToday: 0, totalCheckOutsToday: 0, currentlyInLibrary: 0 });

  // Active view: 'inLibrary' or 'all'
  const [activeView, setActiveView] = useState('inLibrary');

  // Sort state
  const [sortConfig, setSortConfig] = useState({ column: null, direction: null });

  // Column filters
  const [columnFilters, setColumnFilters] = useState({});
  const [activeFilterCol, setActiveFilterCol] = useState(null);
  const filterRef = useRef(null);

  // Column visibility
  const [visibleColumnsInLib, setVisibleColumnsInLib] = useState({
    student: true, userCode: true, checkInTime: true, duration: true,
  });
  const [visibleColumnsAll, setVisibleColumnsAll] = useState({
    student: true, userCode: true, email: true, status: true, createdAt: true,
  });
  const [showColumnMenu, setShowColumnMenu] = useState(false);

  // Modal
  const [showModal, setShowModal] = useState(false);
  const [selectedUserId, setSelectedUserId] = useState(null);

  // Timer để cập nhật thời gian có mặt realtime
  const [, setTick] = useState(0);
  useEffect(() => {
    const timer = setInterval(() => setTick(t => t + 1), 60000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => { fetchStudents(); }, []);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const uid = params.get('userId');
    if (uid) {
      setSelectedUserId(uid);
      setShowModal(true);
      setActiveView('all');
      window.history.replaceState({}, '', window.location.pathname);
    }
  }, []);

  useEffect(() => {
    if (students.length > 0) fetchInLibrary();
  }, [students]);

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

  const fetchStudents = async () => {
    try {
      setLoading(true);
      const allUsers = await userService.getAllUsers();
      const studentList = (allUsers || []).filter(
        u => u.role === 'STUDENT' || u.role === 'TEACHER'
      );
      setStudents(studentList);
    } catch (error) {
      console.error('Lỗi tải danh sách người dùng thư viện:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchInLibrary = async () => {
    try {
      const statsData = await librarianService.getAccessLogStats();
      setStats(statsData);
      const todayStr = new Date().toISOString().split('T')[0];
      const logs = await librarianService.getAccessLogsByDateRange(todayStr, todayStr);
      const inLibraryList = [];
      const seen = new Set();
      for (const log of logs) {
        if (!seen.has(log.userId)) {
          seen.add(log.userId);
          if (log.action === 'CHECK_IN') {
            const student = students.find(s => s.id === log.userId);
            inLibraryList.push({ ...log, avtUrl: student?.avtUrl || null });
          }
        }
      }
      setInLibrary(inLibraryList);
    } catch (error) {
      console.error('Lỗi tải SV đang có mặt:', error);
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A';
    const d = new Date(dateStr);
    return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
  };

  const formatTime = (dateStr) => {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  };

  const getDuration = (checkInTime) => {
    if (!checkInTime) return '';
    const diff = Math.floor((new Date() - new Date(checkInTime)) / 1000 / 60);
    if (diff < 1) return 'Vừa vào';
    if (diff < 60) return `${diff} phút`;
    const h = Math.floor(diff / 60);
    const m = diff % 60;
    return m > 0 ? `${h}h ${m}ph` : `${h} giờ`;
  };

  const initials = (name) => {
    if (!name) return '?';
    return name.split(' ').map(n => n[0]).slice(-2).join('').toUpperCase();
  };

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

  // Get value for sorting/filtering - In Library view
  const getInLibValue = (log, column) => {
    switch (column) {
      case 'student': return log.userName || '';
      case 'userCode': return log.userCode || '';
      case 'checkInTime': return log.checkInTime || '';
      case 'duration': return log.checkInTime ? new Date() - new Date(log.checkInTime) : 0;
      default: return '';
    }
  };

  // Get value for sorting/filtering - All Students view
  const getAllValue = (s, column) => {
    switch (column) {
      case 'student': return s.fullName || '';
      case 'userCode': return s.userCode || '';
      case 'email': return s.email || '';
      case 'status': return s.isActive !== false ? 'Hoạt động' : 'Đã khóa';
      case 'createdAt': return s.createdAt || '';
      default: return '';
    }
  };

  // === In Library data ===
  const filteredInLibrary = useMemo(() => {
    let list = [...inLibrary];
    const q = searchTerm.trim().toLowerCase();
    if (q) {
      list = list.filter(log =>
        (log.userName || '').toLowerCase().includes(q) ||
        (log.userCode || '').toLowerCase().includes(q)
      );
    }
    // Column filters
    Object.entries(columnFilters).forEach(([col, val]) => {
      if (!val) return;
      const fq = val.toLowerCase();
      list = list.filter(log => {
        const v = getInLibValue(log, col);
        if (col === 'checkInTime') return formatTime(v).includes(fq);
        if (col === 'duration') return getDuration(log.checkInTime).toLowerCase().includes(fq);
        return String(v).toLowerCase().includes(fq);
      });
    });
    // Sort
    if (sortConfig.column && sortConfig.direction) {
      list.sort((a, b) => {
        let va = getInLibValue(a, sortConfig.column);
        let vb = getInLibValue(b, sortConfig.column);
        if (sortConfig.column === 'checkInTime') {
          va = va ? new Date(va).getTime() : 0;
          vb = vb ? new Date(vb).getTime() : 0;
        } else if (typeof va === 'string') {
          va = va.toLowerCase();
          vb = String(vb).toLowerCase();
        }
        if (va < vb) return sortConfig.direction === 'asc' ? -1 : 1;
        if (va > vb) return sortConfig.direction === 'asc' ? 1 : -1;
        return 0;
      });
    }
    return list;
  }, [inLibrary, searchTerm, columnFilters, sortConfig]);

  // === All Students data ===
  const filteredStudents = useMemo(() => {
    let data = [...students];
    const q = searchTerm.trim().toLowerCase();
    if (q) {
      data = data.filter(s =>
        (s.fullName || '').toLowerCase().includes(q) ||
        (s.userCode || '').toLowerCase().includes(q) ||
        (s.email || '').toLowerCase().includes(q)
      );
    }
    // Column filters
    Object.entries(columnFilters).forEach(([col, val]) => {
      if (!val) return;
      if (col === 'status') {
        if (val === 'active') data = data.filter(s => s.isActive !== false);
        else if (val === 'locked') data = data.filter(s => s.isActive === false);
      } else {
        const fq = val.toLowerCase();
        data = data.filter(s => {
          const v = getAllValue(s, col);
          if (col === 'createdAt') return formatDate(v).includes(fq);
          return String(v).toLowerCase().includes(fq);
        });
      }
    });
    // Sort
    if (sortConfig.column && sortConfig.direction) {
      data.sort((a, b) => {
        let va = getAllValue(a, sortConfig.column);
        let vb = getAllValue(b, sortConfig.column);
        if (sortConfig.column === 'createdAt') {
          va = va ? new Date(va).getTime() : 0;
          vb = vb ? new Date(vb).getTime() : 0;
        } else {
          va = String(va).toLowerCase();
          vb = String(vb).toLowerCase();
        }
        if (va < vb) return sortConfig.direction === 'asc' ? -1 : 1;
        if (va > vb) return sortConfig.direction === 'asc' ? 1 : -1;
        return 0;
      });
    }
    return data;
  }, [students, searchTerm, columnFilters, sortConfig]);

  // Current data based on view
  const currentData = activeView === 'inLibrary' ? filteredInLibrary : filteredStudents;
  const totalPages = Math.ceil(currentData.length / itemsPerPage);
  const paginatedData = currentData.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, columnFilters, sortConfig, activeView, itemsPerPage]);

  // Reset filters/sort when switching views
  useEffect(() => {
    setColumnFilters({});
    setSortConfig({ column: null, direction: null });
    setActiveFilterCol(null);
  }, [activeView]);

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

  const handleRowClick = (userId) => {
    if (userId) {
      setSelectedUserId(userId);
      setShowModal(true);
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

  // Column header with sort + filter (reuses cio-* classes)
  const renderColumnHeader = (column, label, isCenter = false, filterType = 'text', filterOptions = null) => {
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
              {filterType === 'radio' && filterOptions ? (
                <div className="cio-filter-options">
                  {filterOptions.map(opt => (
                    <label key={opt.value} className="cio-filter-option">
                      <input
                        type="radio"
                        name={`${column}-filter`}
                        checked={columnFilters[column] === opt.value}
                        onChange={() => { handleFilterChange(column, opt.value); setActiveFilterCol(null); }}
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
                    value={columnFilters[column] || ''}
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
  const visibleColumns = activeView === 'inLibrary' ? visibleColumnsInLib : visibleColumnsAll;
  const setVisibleColumns = activeView === 'inLibrary' ? setVisibleColumnsInLib : setVisibleColumnsAll;

  const columnDefs = activeView === 'inLibrary'
    ? [
      { key: 'student', label: 'Người dùng' },
      { key: 'userCode', label: 'Mã số' },
      { key: 'checkInTime', label: 'Giờ vào' },
      { key: 'duration', label: 'Thời gian có mặt' },
    ]
    : [
      { key: 'student', label: 'Người dùng' },
      { key: 'userCode', label: 'Mã số' },
      { key: 'email', label: 'Email' },
      { key: 'status', label: 'Trạng thái' },
      { key: 'createdAt', label: 'Ngày tạo' },
    ];

  const visibleColumnCount = Object.values(visibleColumns).filter(Boolean).length;

  return (
    <div className="lib-container">
      {/* Page Title */}
      <div className="lib-page-title">
        <h1>QUẢN LÝ SINH VIÊN</h1>
      </div>

      <div className="lib-panel">
        {/* Toolbar */}
        <div className="cio-toolbar">
          <div className="lib-search">
            <Search size={16} className="lib-search-icon" />
            <input
              type="text"
              placeholder="Tìm tên, mã SV, email..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          {/* View toggle tabs */}
          <div className="sm-view-tabs">
            <button
              className={`sm-view-tab${activeView === 'inLibrary' ? ' active' : ''}`}
              onClick={() => setActiveView('inLibrary')}
            >
              Đang có mặt ({inLibrary.length})
            </button>
            <button
              className={`sm-view-tab${activeView === 'all' ? ' active' : ''}`}
              onClick={() => setActiveView('all')}
            >
              Tất cả người dùng ({students.length})
            </button>
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
                {columnDefs.map(col => (
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
            Tổng số <strong>{currentData.length}</strong> kết quả
          </span>
        </div>

        {/* Table */}
        {loading && activeView === 'all' ? (
          <div className="sm-loading">
            <Loader2 size={28} className="sm-spinner" />
            <span>Đang tải danh sách...</span>
          </div>
        ) : activeView === 'inLibrary' ? (
          /* === In Library View === */
          <div className="sm-table-wrapper">
            <table className="sm-table">
              <thead>
                <tr>
                  {visibleColumns.student && renderColumnHeader('student', 'Người dùng')}
                  {visibleColumns.userCode && renderColumnHeader('userCode', 'Mã số')}
                  {visibleColumns.checkInTime && renderColumnHeader('checkInTime', 'Giờ vào', true)}
                  {visibleColumns.duration && renderColumnHeader('duration', 'Thời gian có mặt', true)}
                </tr>
              </thead>
              <tbody>
                {paginatedData.length === 0 ? (
                  <tr>
                    <td colSpan={visibleColumnCount} className="sm-table-empty-cell">
                      {searchTerm ? 'Không tìm thấy sinh viên nào.' : 'Hiện không có sinh viên nào trong thư viện.'}
                    </td>
                  </tr>
                ) : (
                  paginatedData.map((log, idx) => (
                    <tr
                      key={`${log.userId}-${idx}`}
                      onClick={() => handleRowClick(log.userId)}
                      className="sm-table-row"
                    >
                      {visibleColumns.student && (
                        <td>
                          <div className="sm-student-cell">
                            <div
                              className="sm-avatar sm-avatar-in"
                              style={log.avtUrl ? {
                                backgroundImage: `url(${log.avtUrl})`,
                                backgroundSize: 'cover',
                                backgroundPosition: 'center'
                              } : {}}
                            >
                              {!log.avtUrl && initials(log.userName)}
                            </div>
                            <span className="sm-student-name">{log.userName || 'N/A'}</span>
                          </div>
                        </td>
                      )}
                      {visibleColumns.userCode && (
                        <td className="sm-code-cell">{log.userCode || 'N/A'}</td>
                      )}
                      {visibleColumns.checkInTime && (
                        <td className="center sm-time-cell">{formatTime(log.checkInTime)}</td>
                      )}
                      {visibleColumns.duration && (
                        <td className="center">
                          <span className="sm-duration-badge">{getDuration(log.checkInTime)}</span>
                        </td>
                      )}
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        ) : (
          /* === All Students View === */
          <div className="sm-table-wrapper">
            <table className="sm-table">
              <thead>
                <tr>
                  {visibleColumns.student && renderColumnHeader('student', 'Người dùng')}
                  {visibleColumns.userCode && renderColumnHeader('userCode', 'Mã số')}
                  {visibleColumns.email && renderColumnHeader('email', 'Email')}
                  {visibleColumns.status && renderColumnHeader('status', 'Trạng thái', true, 'radio', STATUS_OPTIONS)}
                  {visibleColumns.createdAt && renderColumnHeader('createdAt', 'Ngày tạo', true)}
                </tr>
              </thead>
              <tbody>
                {paginatedData.length === 0 ? (
                  <tr>
                    <td colSpan={visibleColumnCount} className="sm-table-empty-cell">
                      {searchTerm ? 'Không tìm thấy sinh viên nào.' : 'Chưa có sinh viên trong hệ thống.'}
                    </td>
                  </tr>
                ) : (
                  paginatedData.map((student) => (
                    <tr
                      key={student.id}
                      onClick={() => handleRowClick(student.id)}
                      className="sm-table-row"
                    >
                      {visibleColumns.student && (
                        <td>
                          <div className="sm-student-cell">
                            <div
                              className="sm-avatar"
                              style={student.avtUrl ? {
                                backgroundImage: `url(${student.avtUrl})`,
                                backgroundSize: 'cover',
                                backgroundPosition: 'center'
                              } : {}}
                            >
                              {!student.avtUrl && initials(student.fullName)}
                            </div>
                            <span className="sm-student-name">{student.fullName || 'Chưa có tên'}</span>
                          </div>
                        </td>
                      )}
                      {visibleColumns.userCode && (
                        <td className="sm-code-cell">{student.userCode || 'N/A'}</td>
                      )}
                      {visibleColumns.email && (
                        <td className="sm-email-cell">{student.email || 'N/A'}</td>
                      )}
                      {visibleColumns.status && (
                        <td className="center">
                          <span className={`sm-status-badge ${student.isActive !== false ? 'active' : 'locked'}`}>
                            {student.isActive !== false ? 'Hoạt động' : 'Đã khóa'}
                          </span>
                        </td>
                      )}
                      {visibleColumns.createdAt && (
                        <td className="center sm-date-cell">{formatDate(student.createdAt)}</td>
                      )}
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}

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
        isOpen={showModal}
        onClose={() => {
          setShowModal(false);
          setSelectedUserId(null);
        }}
      />

      {/* Close column menu overlay */}
      {showColumnMenu && (
        <div
          style={{ position: 'fixed', inset: 0, zIndex: 50 }}
          onClick={() => setShowColumnMenu(false)}
        />
      )}
    </div>
  );
};

export default StudentsManage;
