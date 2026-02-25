import React, { useState, useEffect, useCallback, useMemo } from "react";
import { Search, Loader2 } from "lucide-react";
import "../../../styles/librarian/librarian-shared.css";
import "../../../styles/librarian/BookingManage.css";
import StudentDetailModal from "../../../components/librarian/StudentDetailModal";

const API_BASE = `${import.meta.env.VITE_API_URL || "http://localhost:8080"}/slib/bookings`;

const STATUS_LABELS = {
    PROCESSING: "Đang xử lý",
    BOOKED: "Đã đặt",
    CONFIRMED: "Đã xác nhận",
    CANCELLED: "Đã huỷ",
    CANCEL: "Đã huỷ",
    EXPIRED: "Hết hạn",
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

const TAB_LIST = [
    { key: "ALL", label: "Tất cả" },
    { key: "BOOKED", label: "Đã đặt" },
    { key: "CONFIRMED", label: "Đã xác nhận" },
    { key: "CANCELLED", label: "Đã huỷ" },
    { key: "EXPIRED", label: "Hết hạn" },
    { key: "COMPLETED", label: "Hoàn thành" },
];

function BookingManage() {
    const [bookings, setBookings] = useState([]);
    const [activeTab, setActiveTab] = useState("ALL");
    const [loading, setLoading] = useState(true);
    const [selectedBooking, setSelectedBooking] = useState(null);
    const [searchText, setSearchText] = useState("");
    const [submitting, setSubmitting] = useState(false);

    // Date filter - mặc định hôm nay
    const todayStr = new Date().toISOString().split("T")[0];
    const [startDate, setStartDate] = useState(todayStr);
    const [endDate, setEndDate] = useState(todayStr);

    // Pagination
    const [currentPage, setCurrentPage] = useState(1);
    const itemsPerPage = 15;

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
        return d.toLocaleDateString("vi-VN", {
            day: "2-digit", month: "2-digit", year: "numeric",
        });
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

    const clearDateFilter = () => {
        setStartDate('');
        setEndDate('');
    };

    const filteredBookings = useMemo(() => {
        let list = bookings;

        // Lọc theo ngày
        if (startDate || endDate) {
            list = list.filter((b) => {
                const bDate = b.startTime ? b.startTime.split("T")[0] : (b.createdAt ? b.createdAt.split("T")[0] : '');
                if (startDate && bDate < startDate) return false;
                if (endDate && bDate > endDate) return false;
                return true;
            });
        }

        // Lọc theo status
        if (activeTab !== "ALL") {
            list = list.filter((b) => b.status === activeTab);
        }

        // Tìm kiếm
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

        return list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    }, [bookings, activeTab, searchText, startDate, endDate]);

    // Counts dựa trên dữ liệu đã lọc theo ngày
    const counts = useMemo(() => {
        let dateFiltered = bookings;
        if (startDate || endDate) {
            dateFiltered = dateFiltered.filter((b) => {
                const bDate = b.startTime ? b.startTime.split("T")[0] : (b.createdAt ? b.createdAt.split("T")[0] : '');
                if (startDate && bDate < startDate) return false;
                if (endDate && bDate > endDate) return false;
                return true;
            });
        }
        const c = { ALL: dateFiltered.length, BOOKED: 0, CONFIRMED: 0, CANCELLED: 0, EXPIRED: 0, COMPLETED: 0 };
        dateFiltered.forEach((b) => {
            if (c[b.status] !== undefined) c[b.status]++;
        });
        return c;
    }, [bookings, startDate, endDate]);

    // Pagination
    const totalPages = Math.ceil(filteredBookings.length / itemsPerPage);
    const paginatedBookings = filteredBookings.slice(
        (currentPage - 1) * itemsPerPage,
        currentPage * itemsPerPage
    );

    useEffect(() => {
        setCurrentPage(1);
    }, [activeTab, searchText, startDate, endDate]);

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

    const formatIsoDate = (isoDate) => {
        if (!isoDate) return '';
        const [year, month, day] = isoDate.split('-');
        return `${day}/${month}/${year}`;
    };

    return (
        <div className="lib-container">
            {/* Page Title + Inline Stats */}
            <div className="lib-page-title">
                <h1>Quản lý đặt chỗ</h1>
                <div className="lib-inline-stats">
                    <span className="lib-inline-stat">
                        <span className="dot blue"></span>
                        Tổng <strong>{counts.ALL}</strong>
                    </span>
                    <span className="lib-inline-stat">
                        <span className="dot amber"></span>
                        Đã đặt <strong>{counts.BOOKED}</strong>
                    </span>
                    <span className="lib-inline-stat">
                        <span className="dot green"></span>
                        Đã xác nhận <strong>{counts.CONFIRMED}</strong>
                    </span>
                    {counts.CANCELLED > 0 && (
                        <span className="lib-inline-stat">
                            <span className="dot red"></span>
                            Đã huỷ <strong>{counts.CANCELLED}</strong>
                        </span>
                    )}
                </div>
            </div>

            {/* Controls Panel */}
            <div className="lib-panel">
                <div className="lib-panel-header">
                    <h3 className="lib-panel-title">Danh sách đặt chỗ</h3>
                </div>

                {/* Date Filter + Search */}
                <div className="bm-filter-row">
                    <div className="bm-date-filters">
                        <div className="bm-date-group">
                            <label className="bm-date-label">Từ ngày</label>
                            <input
                                type="date"
                                className="bm-date-input"
                                value={startDate}
                                onChange={(e) => setStartDate(e.target.value)}
                            />
                        </div>
                        <span className="bm-date-sep">-</span>
                        <div className="bm-date-group">
                            <label className="bm-date-label">Đến ngày</label>
                            <input
                                type="date"
                                className="bm-date-input"
                                value={endDate}
                                onChange={(e) => setEndDate(e.target.value)}
                            />
                        </div>
                        {(startDate || endDate) && (
                            <button className="bm-clear-btn" onClick={clearDateFilter}>
                                Xóa lọc
                            </button>
                        )}
                    </div>
                    <div className="lib-search">
                        <Search size={16} className="lib-search-icon" />
                        <input
                            type="text"
                            placeholder="Tìm tên, mã SV, mã ghế, khu vực..."
                            value={searchText}
                            onChange={(e) => setSearchText(e.target.value)}
                        />
                    </div>
                </div>

                {/* Status Tabs */}
                <div className="lib-tabs" style={{ marginTop: 12 }}>
                    {TAB_LIST.map((tab) => (
                        <button
                            key={tab.key}
                            className={`lib-tab ${activeTab === tab.key ? "active" : ""}`}
                            onClick={() => setActiveTab(tab.key)}
                        >
                            {tab.label}
                            {counts[tab.key] > 0 && (
                                <span className="lib-tab-count">{counts[tab.key]}</span>
                            )}
                        </button>
                    ))}
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
                                    <th>Sinh viên</th>
                                    <th>Ghế</th>
                                    <th>Khu vực</th>
                                    <th className="center">Thời gian</th>
                                    <th className="center">Trạng thái</th>
                                    <th className="center">Ngày tạo</th>
                                    <th className="center">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                {paginatedBookings.map((booking) => (
                                    <tr
                                        key={booking.reservationId}
                                        className="bm-table-row"
                                        onClick={() => setSelectedBooking(booking)}
                                    >
                                        <td>
                                            <div
                                                className="bm-student-cell"
                                                onClick={(e) => handleUserClick(e, booking.user?.id)}
                                            >
                                                {booking.user?.avtUrl ? (
                                                    <img
                                                        src={booking.user.avtUrl}
                                                        alt=""
                                                        className="bm-avatar"
                                                    />
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
                                        <td className="bm-seat-cell">
                                            <span className="bm-seat-code">
                                                {booking.seat?.seatCode || "N/A"}
                                            </span>
                                        </td>
                                        <td className="bm-zone-cell">
                                            {booking.seat?.zone?.zoneName || "-"}
                                            {booking.seat?.zone?.area?.areaName &&
                                                <span className="bm-area-name"> ({booking.seat.zone.area.areaName})</span>
                                            }
                                        </td>
                                        <td className="center">
                                            <span className="bm-time-badge">
                                                {formatTime(booking.startTime)} - {formatTime(booking.endTime)}
                                            </span>
                                        </td>
                                        <td className="center">
                                            <span className={`bm-status-badge ${STATUS_CLASSES[booking.status] || ''}`}>
                                                {STATUS_LABELS[booking.status] || booking.status}
                                            </span>
                                        </td>
                                        <td className="center bm-date-cell">
                                            {formatDateTime(booking.createdAt)}
                                        </td>
                                        <td className="center" onClick={(e) => e.stopPropagation()}>
                                            {(booking.status === "BOOKED" || booking.status === "PROCESSING") && (
                                                <button
                                                    className="bm-cancel-btn"
                                                    onClick={() => handleCancelBooking(booking.reservationId)}
                                                    disabled={submitting}
                                                >
                                                    Huỷ
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                        {filteredBookings.length === 0 && (
                            <div className="bm-table-empty">
                                {searchText
                                    ? "Không tìm thấy đặt chỗ phù hợp."
                                    : startDate || endDate
                                        ? `Không có đặt chỗ nào trong khoảng ${formatIsoDate(startDate)} - ${formatIsoDate(endDate)}.`
                                        : "Chưa có đặt chỗ nào."
                                }
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
                            {(currentPage - 1) * itemsPerPage + 1}-{Math.min(currentPage * itemsPerPage, filteredBookings.length)} / {filteredBookings.length}
                        </span>
                    </div>
                )}
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
                                <div className="lib-slide-label">Ngày tạo</div>
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
        </div>
    );
}

export default BookingManage;
