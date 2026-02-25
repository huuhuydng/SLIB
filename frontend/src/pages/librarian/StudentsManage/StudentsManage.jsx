import React, { useState, useMemo, useEffect } from 'react';
import { Search, Users, Loader2, LogIn, Clock } from 'lucide-react';
import '../../../styles/librarian/librarian-shared.css';
import '../../../styles/librarian/StudentsManage.css';
import userService from '../../../services/userService';
import librarianService from '../../../services/librarianService';
import StudentDetailModal from '../../../components/librarian/StudentDetailModal';

const StudentsManage = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [filterRank, setFilterRank] = useState('all');
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 15;

  // Sinh viên đang có mặt
  const [inLibrary, setInLibrary] = useState([]);
  const [stats, setStats] = useState({ totalCheckInsToday: 0, totalCheckOutsToday: 0, currentlyInLibrary: 0 });

  // Active view: 'inLibrary' or 'all'
  const [activeView, setActiveView] = useState('inLibrary');

  // Modal
  const [showModal, setShowModal] = useState(false);
  const [selectedUserId, setSelectedUserId] = useState(null);

  useEffect(() => {
    fetchStudents();
    fetchInLibrary();
  }, []);

  const fetchStudents = async () => {
    try {
      setLoading(true);
      const allUsers = await userService.getAllUsers({ role: 'STUDENT' });
      const studentList = (allUsers || []).filter(u => u.role === 'STUDENT');
      setStudents(studentList);
    } catch (error) {
      console.error('Lỗi tải danh sách sinh viên:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchInLibrary = async () => {
    try {
      // Lấy stats
      const statsData = await librarianService.getAccessLogStats();
      setStats(statsData);

      // Lấy logs hôm nay, lọc đang có mặt (chưa checkout)
      const todayStr = new Date().toISOString().split('T')[0];
      const logs = await librarianService.getAccessLogsByDateRange(todayStr, todayStr);

      // Group theo userId, kiểm tra trạng thái cuối
      const userMap = new Map();
      for (const log of logs) {
        if (!userMap.has(log.userId)) {
          userMap.set(log.userId, { ...log, lastAction: log.action });
        }
        // Log mới nhất (đầu mảng vì đã sort DESC)
      }

      // Lọc: sinh viên có action cuối là CHECK_IN (chưa check out)
      const inLibraryList = [];
      const seen = new Set();
      for (const log of logs) {
        if (!seen.has(log.userId)) {
          seen.add(log.userId);
          if (log.action === 'CHECK_IN') {
            inLibraryList.push(log);
          }
        }
      }
      setInLibrary(inLibraryList);
    } catch (error) {
      console.error('Lỗi tải SV đang có mặt:', error);
    }
  };

  // Lọc và tìm kiếm cho tab "Tất cả"
  const filteredStudents = useMemo(() => {
    let data = students;
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      data = data.filter(s =>
        (s.fullName || '').toLowerCase().includes(term) ||
        (s.userCode || '').toLowerCase().includes(term) ||
        (s.email || '').toLowerCase().includes(term)
      );
    }
    if (filterRank === 'active') {
      data = data.filter(s => s.isActive !== false);
    } else if (filterRank === 'locked') {
      data = data.filter(s => s.isActive === false);
    }
    return data;
  }, [students, searchTerm, filterRank]);

  // Lọc tìm kiếm cho tab "Đang có mặt"
  const filteredInLibrary = useMemo(() => {
    if (!searchTerm) return inLibrary;
    const term = searchTerm.toLowerCase();
    return inLibrary.filter(log =>
      (log.userName || '').toLowerCase().includes(term) ||
      (log.userCode || '').toLowerCase().includes(term)
    );
  }, [inLibrary, searchTerm]);

  // Pagination cho tab hiện tại
  const currentData = activeView === 'inLibrary' ? filteredInLibrary : filteredStudents;
  const totalPages = Math.ceil(currentData.length / itemsPerPage);
  const paginatedData = currentData.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, filterRank, activeView]);

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

  // Stats
  const totalStudents = students.length;
  const activeStudents = students.filter(s => s.isActive !== false).length;

  return (
    <div className="lib-container">
      {/* Page Title */}
      <div className="lib-page-title">
        <h1>Quản lý sinh viên</h1>
        <div className="lib-inline-stats">
          <span className="lib-inline-stat">
            <span className="dot green"></span>
            Đang trong TV <strong>{stats.currentlyInLibrary}</strong>
          </span>
          <span className="lib-inline-stat">
            <span className="dot blue"></span>
            Tổng SV <strong>{totalStudents}</strong>
          </span>
          <span className="lib-inline-stat">
            <span className="dot amber"></span>
            Lượt vào hôm nay <strong>{stats.totalCheckInsToday}</strong>
          </span>
        </div>
      </div>

      {/* View Tabs */}
      <div className="lib-panel">
        <div className="lib-panel-header">
          <h3 className="lib-panel-title">
            {activeView === 'inLibrary' ? 'Sinh viên đang có mặt' : 'Danh sách sinh viên'}
          </h3>
          <div className="sm-controls">
            <div className="lib-search">
              <Search size={16} className="lib-search-icon" />
              <input
                type="text"
                placeholder="Tìm tên, mã SV, email..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            {activeView === 'inLibrary' ? (
              <div className="lib-tabs" style={{ margin: 0 }}>
                <button
                  className="lib-tab active"
                  onClick={() => setActiveView('inLibrary')}
                >
                  Đang có mặt
                </button>
                <button
                  className="lib-tab"
                  onClick={() => setActiveView('all')}
                >
                  Tất cả SV
                </button>
              </div>
            ) : (
              <div className="lib-tabs" style={{ margin: 0 }}>
                <button
                  className="lib-tab"
                  onClick={() => setActiveView('inLibrary')}
                >
                  Đang có mặt
                </button>
                <button
                  className={`lib-tab ${filterRank === 'all' ? 'active' : ''}`}
                  onClick={() => { setActiveView('all'); setFilterRank('all'); }}
                >
                  Tất cả
                </button>
                <button
                  className={`lib-tab ${filterRank === 'active' ? 'active' : ''}`}
                  onClick={() => { setActiveView('all'); setFilterRank('active'); }}
                >
                  Hoạt động
                </button>
                <button
                  className={`lib-tab ${filterRank === 'locked' ? 'active' : ''}`}
                  onClick={() => { setActiveView('all'); setFilterRank('locked'); }}
                >
                  Đã khóa
                </button>
              </div>
            )}
          </div>
        </div>

        {loading && activeView === 'all' ? (
          <div className="sm-loading">
            <Loader2 size={28} className="sm-spinner" />
            <span>Đang tải danh sách...</span>
          </div>
        ) : activeView === 'inLibrary' ? (
          /* === Tab: Đang có mặt === */
          <div className="sm-table-wrapper">
            <table className="sm-table">
              <thead>
                <tr>
                  <th>Sinh viên</th>
                  <th>Mã số</th>
                  <th className="center">Giờ vào</th>
                  <th className="center">Thời gian có mặt</th>
                  <th className="center">Cổng</th>
                </tr>
              </thead>
              <tbody>
                {paginatedData.map((log, idx) => (
                  <tr
                    key={`${log.userId}-${idx}`}
                    onClick={() => handleRowClick(log.userId)}
                    className="sm-table-row"
                  >
                    <td>
                      <div className="sm-student-cell">
                        <div className="sm-avatar sm-avatar-in">
                          {initials(log.userName)}
                        </div>
                        <span className="sm-student-name">{log.userName || 'N/A'}</span>
                      </div>
                    </td>
                    <td className="sm-code-cell">{log.userCode || 'N/A'}</td>
                    <td className="center sm-time-cell">{formatTime(log.checkInTime)}</td>
                    <td className="center">
                      <span className="sm-duration-badge">{getDuration(log.checkInTime)}</span>
                    </td>
                    <td className="center sm-code-cell">{log.deviceId || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {filteredInLibrary.length === 0 && (
              <div className="sm-table-empty">Hiện không có sinh viên nào trong thư viện.</div>
            )}
          </div>
        ) : (
          /* === Tab: Tất cả SV === */
          <div className="sm-table-wrapper">
            <table className="sm-table">
              <thead>
                <tr>
                  <th>Sinh viên</th>
                  <th>Mã số</th>
                  <th>Email</th>
                  <th className="center">Trạng thái</th>
                  <th className="center">Ngày tạo</th>
                </tr>
              </thead>
              <tbody>
                {paginatedData.map((student) => (
                  <tr
                    key={student.id}
                    onClick={() => handleRowClick(student.id)}
                    className="sm-table-row"
                  >
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
                    <td className="sm-code-cell">{student.userCode || 'N/A'}</td>
                    <td className="sm-email-cell">{student.email || 'N/A'}</td>
                    <td className="center">
                      <span className={`sm-status-badge ${student.isActive !== false ? 'active' : 'locked'}`}>
                        {student.isActive !== false ? 'Hoạt động' : 'Đã khóa'}
                      </span>
                    </td>
                    <td className="center sm-date-cell">{formatDate(student.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {filteredStudents.length === 0 && (
              <div className="sm-table-empty">
                {searchTerm ? 'Không tìm thấy sinh viên nào.' : 'Chưa có sinh viên trong hệ thống.'}
              </div>
            )}
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="cio-pagination">
            <button
              className="cio-page-btn"
              onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
              disabled={currentPage === 1}
            >
              &lt;
            </button>
            {getPageNumbers().map((page, idx) =>
              page === '...' ? (
                <span key={`ellipsis-${idx}`} className="cio-page-ellipsis">...</span>
              ) : (
                <button
                  key={page}
                  className={`cio-page-btn ${currentPage === page ? 'active' : ''}`}
                  onClick={() => setCurrentPage(page)}
                >
                  {page}
                </button>
              )
            )}
            <button
              className="cio-page-btn"
              onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
              disabled={currentPage === totalPages}
            >
              &gt;
            </button>
            <span className="cio-page-info">
              {(currentPage - 1) * itemsPerPage + 1}-{Math.min(currentPage * itemsPerPage, currentData.length)} / {currentData.length}
            </span>
          </div>
        )}
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
    </div>
  );
};

export default StudentsManage;