import React, { useState, useEffect, useCallback, useMemo, useRef } from "react";
import { Search, Loader2, ArrowUpDown, ArrowUp, ArrowDown, Filter, X, SlidersHorizontal } from "lucide-react";
import "../../../styles/librarian/librarian-shared.css";
import "../../../styles/librarian/CheckInOut.css";
import "../../../styles/librarian/BookingManage.css";
import StudentDetailModal from "../../../components/librarian/StudentDetailModal";

const API_BASE = `${import.meta.env.VITE_API_URL || "http://localhost:8080"}/slib/bookings`;

const STATUS_LABELS = {
    PROCESSING: "Đang xử lý",
    BOOKED: "Đã đặt",
    CONFIRMED: "Đã xác nhận",
    CANCELLED: "Đã huỷ",
    CANCEL: "Đã huỷ",
    EXPIRED: "Không đến",
    COMPLETED: "Hoàn thành",
};

const STATUS_CLASSES = {
    PROCESSING: "processing",
    BOOKED: "booked",
    CONFIRMED: "confirmed",
    CANCELLED: "cancelled",
    CANCEL: "cancelled",
    EXPIRED: "expired",
    COMPLETED: "completed",
};

const STATUS_OPTIONS = [
    { value: "", label: "Tất cả" },
    { value: "BOOKED", label: "Đã đặt" },
    { value: "CONFIRMED", label: "Đã xác nhận" },
    { value: "CANCELLED", label: "Đã huỷ" },
    { value: "EXPIRED", label: "Không đến" },
    { value: "COMPLETED", label: "Hoàn thành" },
];

function BookingManage() {
    const [bookings, setBookings] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedBooking, setSelectedBooking] = useState(null);
    const [searchText, setSearchText] = useState("");
    const [submitting, setSubmitting] = useState(false);

    // Pagination
    const [currentPage, setCurrentPage] = useState(1);
    const [itemsPerPage, setItemsPerPage] = useState(10);

    // Sort state
    const [sortConfig, setSortConfig] = useState({ column: null, direction: null });

    // Column filters
    const [columnFilters, setColumnFilters] = useState({
        student: '',
        seat: '',
        zone: '',
        time: '',
        status: '',
        createdAt: '',
    });
    const [activeFilterCol, setActiveFilterCol] = useState(null);
    const filterRef = useRef(null);

    // Column visibility
    const [visibleColumns, setVisibleColumns] = useState({
        student: true,
        seat: true,
        zone: true,
        time: true,
        status: true,
        createdAt: true,
    });
    const [showColumnMenu, setShowColumnMenu] = useState(false);

    // Student detail modal
    const [showStudentModal, setShowStudentModal] = useState(false);
    const [selectedUserId, setSelectedUserId] = useState(null);

    const getToken = () =>
        sessionStorage.getItem("librarian_token") || localStorage.getItem("librarian_token");

    const fetchBookings = useCallback(async () => {
        setLoading(true);
        try {
            const token = getToken();
            const res = await fetch(`${API_BASE}/getall`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            if (res.ok) {
                const data = await res.json();
                setBookings(data);
            }
        } catch (err) {
            console.error("Lỗi tải danh sách đặt chỗ:", err);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchBookings();
    }, [fetchBookings]);

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

    const handleCancelBooking = async (reservationId) => {
        if (!window.confirm("Bạn có chắc muốn huỷ đặt chỗ này?")) return;
        setSubmitting(true);
        try {
            const token = getToken();
            const res = await fetch(`${API_BASE}/cancel/${reservationId}`, {
                method: "PUT",
                headers: { Authorization: `Bearer ${token}` },
            });
            if (res.ok) {
                fetchBookings();
                setSelectedBooking(null);
            }
        } catch (err) {
            console.error("Lỗi huỷ đặt chỗ:", err);
        } finally {
            setSubmitting(false);
        }
    };

    const formatDate = (iso) => {
        if (!iso) return "";
        const d = new Date(iso);
        return d.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
    };

    const formatTime = (iso) => {
        if (!iso) return "";
        const d = new Date(iso);
        return d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
    };

    const formatDateTime = (iso) => {
        if (!iso) return "";
        return `${formatTime(iso)} ${formatDate(iso)}`;
    };

    const getInitial = (name) => {
        if (!name) return "?";
        return name.split(' ').map(n => n[0]).slice(-2).join('').toUpperCase();
    };

    // Get value for sorting/filtering
    const getBookingValue = (booking, column) => {
        switch (column) {
            case 'student': return booking.user?.fullName || '';
            case 'seat': return booking.seat?.seatCode || '';
            case 'zone': return booking.seat?.zone?.zoneName || '';
            case 'time': return booking.startTime || '';
            case 'status': return STATUS_LABELS[booking.status] || booking.status || '';
            case 'createdAt': return booking.createdAt || '';
            default: return '';
        }
    };

    const filteredBookings = useMemo(() => {
        let list = [...bookings];

        // Global search
        const q = searchText.trim().toLowerCase();
        if (q) {
            list = list.filter(
                (b) =>
                    (b.user?.fullName || "").toLowerCase().includes(q) ||
                    (b.user?.userCode || "").toLowerCase().includes(q) ||
                    (b.seat?.seatCode || "").toLowerCase().includes(q) ||
                    (b.seat?.zone?.zoneName || "").toLowerCase().includes(q)
            );
        }

        // Column filters
        Object.entries(columnFilters).forEach(([col, filterVal]) => {
            if (!filterVal) return;
            const fq = filterVal.toLowerCase();

            if (col === 'status') {
                list = list.filter(b => b.status === filterVal);
            } else if (col === 'student') {
                list = list.filter(b =>
                    (b.user?.fullName || '').toLowerCase().includes(fq) ||
                    (b.user?.userCode || '').toLowerCase().includes(fq)
                );
            } else if (col === 'seat') {
                list = list.filter(b => (b.seat?.seatCode || '').toLowerCase().includes(fq));
            } else if (col === 'zone') {
                list = list.filter(b =>
                    (b.seat?.zone?.zoneName || '').toLowerCase().includes(fq) ||
                    (b.seat?.zone?.area?.areaName || '').toLowerCase().includes(fq)
                );
            } else if (col === 'time') {
                list = list.filter(b => {
                    const timeStr = `${formatTime(b.startTime)} - ${formatTime(b.endTime)}`;
                    return timeStr.toLowerCase().includes(fq);
                });
            } else if (col === 'createdAt') {
                list = list.filter(b => formatDateTime(b.createdAt).toLowerCase().includes(fq));
            }
        });

        // Sort
        if (sortConfig.column && sortConfig.direction) {
            list.sort((a, b) => {
                let valA = getBookingValue(a, sortConfig.column);
                let valB = getBookingValue(b, sortConfig.column);

                if (sortConfig.column === 'time' || sortConfig.column === 'createdAt') {
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
        } else {
            // Default sort: newest first
            list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        }

        return list;
    }, [bookings, searchText, columnFilters, sortConfig]);

    // Stats
    const counts = useMemo(() => {
        const c = { ALL: bookings.length, BOOKED: 0, CONFIRMED: 0, CANCELLED: 0, EXPIRED: 0, COMPLETED: 0 };
        bookings.forEach((b) => {
            if (c[b.status] !== undefined) c[b.status]++;
        });
        return c;
    }, [bookings]);

    // Pagination
    const totalPages = Math.ceil(filteredBookings.length / itemsPerPage);
    const paginatedBookings = filteredBookings.slice(
        (currentPage - 1) * itemsPerPage,
        currentPage * itemsPerPage
    );

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

    const handleUserClick = (e, userId) => {
        e.stopPropagation();
        if (userId) {
            setSelectedUserId(userId);
            setShowStudentModal(true);
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

    // Column header with sort + filter
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
    const visibleColumnCount = Object.values(visibleColumns).filter(Boolean).length;

    return (
        <div className="lib-container">
            {/* Page Title */}
            <div className="lib-page-title">
                <h1>QUẢN LÝ ĐẶT CHỖ</h1>
            </div>

            {/* Table Panel */}
            <div className="lib-panel">
                {/* Toolbar */}
                <div className="cio-toolbar">
                    <div className="lib-search">
                        <Search size={16} className="lib-search-icon" />
                        <input
                            type="text"
                            placeholder="Tìm tên, mã SV, mã ghế, khu vực..."
                            value={searchText}
                            onChange={(e) => setSearchText(e.target.value)}
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
                                    { key: 'student', label: 'Sinh viên' },
                                    { key: 'seat', label: 'Ghế' },
                                    { key: 'zone', label: 'Khu vực' },
                                    { key: 'time', label: 'Thời gian' },
                                    { key: 'status', label: 'Trạng thái' },
                                    { key: 'createdAt', label: 'Ngày tạo' },
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
                        Tổng số <strong>{filteredBookings.length}</strong> kết quả
                    </span>
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
                                    {visibleColumns.student && renderColumnHeader('student', 'Sinh viên')}
                                    {visibleColumns.seat && renderColumnHeader('seat', 'Ghế')}
                                    {visibleColumns.zone && renderColumnHeader('zone', 'Khu vực')}
                                    {visibleColumns.time && renderColumnHeader('time', 'Thời gian', true)}
                                    {visibleColumns.status && renderColumnHeader('status', 'Trạng thái', true)}
                                    {visibleColumns.createdAt && renderColumnHeader('createdAt', 'Ngày tạo', true)}

                                </tr>
                            </thead>
                            <tbody>
                                {paginatedBookings.length === 0 ? (
                                    <tr>
                                        <td colSpan={visibleColumnCount} className="bm-table-empty-cell">
                                            {searchText ? "Không tìm thấy đặt chỗ phù hợp." : "Chưa có đặt chỗ nào."}
                                        </td>
                                    </tr>
                                ) : (
                                    paginatedBookings.map((booking) => (
                                        <tr
                                            key={booking.reservationId}
                                            className="bm-table-row"
                                            onClick={() => setSelectedBooking(booking)}
                                        >
                                            {visibleColumns.student && (
                                                <td>
                                                    <div
                                                        className="bm-student-cell"
                                                        onClick={(e) => handleUserClick(e, booking.user?.id)}
                                                    >
                                                        {booking.user?.avtUrl ? (
                                                            <img src={booking.user.avtUrl} alt="" className="bm-avatar" />
                                                        ) : (
                                                            <div className="bm-avatar-placeholder">
                                                                {getInitial(booking.user?.fullName)}
                                                            </div>
                                                        )}
                                                        <div>
                                                            <div className="bm-student-name">
                                                                {booking.user?.fullName || "Sinh viên"}
                                                            </div>
                                                            <div className="bm-student-code">
                                                                {booking.user?.userCode || ""}
                                                            </div>
                                                        </div>
                                                    </div>
                                                </td>
                                            )}
                                            {visibleColumns.seat && (
                                                <td className="bm-seat-cell">
                                                    <span className="bm-seat-code">
                                                        {booking.seat?.seatCode || "N/A"}
                                                    </span>
                                                </td>
                                            )}
                                            {visibleColumns.zone && (
                                                <td className="bm-zone-cell">
                                                    {booking.seat?.zone?.zoneName || "-"}
                                                    {booking.seat?.zone?.area?.areaName &&
                                                        <span className="bm-area-name"> ({booking.seat.zone.area.areaName})</span>
                                                    }
                                                </td>
                                            )}
                                            {visibleColumns.time && (
                                                <td className="center">
                                                    <span className="bm-time-badge">
                                                        {formatTime(booking.startTime)} - {formatTime(booking.endTime)}
                                                    </span>
                                                </td>
                                            )}
                                            {visibleColumns.status && (
                                                <td className="center">
                                                    <span className={`bm-status-badge ${STATUS_CLASSES[booking.status] || ''}`}>
                                                        {STATUS_LABELS[booking.status] || booking.status}
                                                    </span>
                                                </td>
                                            )}
                                            {visibleColumns.createdAt && (
                                                <td className="center bm-date-cell">
                                                    {formatDateTime(booking.createdAt)}
                                                </td>
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

            {/* Slide Panel - Detail */}
            {selectedBooking && (
                <>
                    <div className="lib-slide-overlay" onClick={() => setSelectedBooking(null)} />
                    <div className="lib-slide-panel">
                        <div className="lib-slide-header">
                            <h2>Chi tiết đặt chỗ</h2>
                            <button
                                className="lib-slide-close"
                                onClick={() => setSelectedBooking(null)}
                            >
                                &times;
                            </button>
                        </div>
                        <div className="lib-slide-body">
                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Sinh viên</div>
                                <div className="lib-user-info">
                                    {selectedBooking.user?.avtUrl ? (
                                        <img src={selectedBooking.user.avtUrl} alt="" className="lib-avatar" />
                                    ) : (
                                        <div className="lib-avatar-placeholder">
                                            {getInitial(selectedBooking.user?.fullName)}
                                        </div>
                                    )}
                                    <div>
                                        <h3>{selectedBooking.user?.fullName || "Sinh viên"}</h3>
                                        <div className="lib-user-code">{selectedBooking.user?.userCode}</div>
                                    </div>
                                </div>
                            </div>

                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Trạng thái</div>
                                <span className={`bm-status-badge ${STATUS_CLASSES[selectedBooking.status] || ''}`}>
                                    {STATUS_LABELS[selectedBooking.status] || selectedBooking.status}
                                </span>
                            </div>

                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Chỗ ngồi</div>
                                <div className="lib-slide-value">
                                    Ghế {selectedBooking.seat?.seatCode || "N/A"}
                                </div>
                            </div>

                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Khu vực</div>
                                <div className="lib-slide-value">
                                    {selectedBooking.seat?.zone?.zoneName || "N/A"}
                                    {selectedBooking.seat?.zone?.area?.areaName && ` - ${selectedBooking.seat.zone.area.areaName}`}
                                </div>
                            </div>

                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Thời gian</div>
                                <div className="lib-slide-value">
                                    {formatDateTime(selectedBooking.startTime)} - {formatDateTime(selectedBooking.endTime)}
                                </div>
                            </div>

                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Ngày đặt</div>
                                <div className="lib-slide-value">{formatDateTime(selectedBooking.createdAt)}</div>
                            </div>
                        </div>

                        <div className="lib-slide-footer">
                            {(selectedBooking.status === "BOOKED" || selectedBooking.status === "PROCESSING") && (
                                <button
                                    className="lib-btn ghost danger"
                                    onClick={() => handleCancelBooking(selectedBooking.reservationId)}
                                    disabled={submitting}
                                >
                                    {submitting ? "Đang xử lý..." : "Huỷ đặt chỗ"}
                                </button>
                            )}
                            <button
                                className="lib-btn ghost"
                                onClick={() => setSelectedBooking(null)}
                            >
                                Đóng
                            </button>
                        </div>
                    </div>
                </>
            )}

            {/* Student Detail Modal */}
            <StudentDetailModal
                userId={selectedUserId}
                isOpen={showStudentModal}
                onClose={() => {
                    setShowStudentModal(false);
                    setSelectedUserId(null);
                }}
            />

            {/* Close column menu */}
            {showColumnMenu && (
                <div
                    style={{ position: 'fixed', inset: 0, zIndex: 50 }}
                    onClick={() => setShowColumnMenu(false)}
                />
            )}
        </div>
    );
}

export default BookingManage;
