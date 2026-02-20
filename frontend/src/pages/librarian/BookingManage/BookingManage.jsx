import React, { useState, useEffect, useCallback, useMemo } from "react";
import { Search } from "lucide-react";
import "../../../styles/librarian/librarian-shared.css";
import "../../../styles/librarian/BookingManage.css";

const API_BASE = `${import.meta.env.VITE_API_URL || "http://localhost:8080"}/slib/bookings`;

const STATUS_LABELS = {
    PROCESSING: "Đang xử lý",
    BOOKED: "Đã đặt",
    CONFIRMED: "Đã xác nhận",
    CANCELLED: "Đã huỷ",
    EXPIRED: "Hết hạn",
    COMPLETED: "Hoàn thành",
};

const TAB_LIST = [
    { key: "ALL", label: "Tất cả" },
    { key: "BOOKED", label: "Đã đặt" },
    { key: "CONFIRMED", label: "Đã xác nhận" },
    { key: "CANCELLED", label: "Đã huỷ" },
    { key: "EXPIRED", label: "Hết hạn" },
];

function BookingManage() {
    const [bookings, setBookings] = useState([]);
    const [activeTab, setActiveTab] = useState("ALL");
    const [loading, setLoading] = useState(true);
    const [selectedBooking, setSelectedBooking] = useState(null);
    const [searchText, setSearchText] = useState("");
    const [submitting, setSubmitting] = useState(false);

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

    const formatDateTime = (iso) => {
        if (!iso) return "";
        const d = new Date(iso);
        return d.toLocaleString("vi-VN", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        });
    };

    const formatTime = (iso) => {
        if (!iso) return "";
        const d = new Date(iso);
        return d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
    };

    const getInitial = (name) => (name ? name.charAt(0).toUpperCase() : "?");

    const filteredBookings = useMemo(() => {
        let list = bookings;
        if (activeTab !== "ALL") {
            list = list.filter((b) => b.status === activeTab);
        }
        const q = searchText.trim().toLowerCase();
        if (q) {
            list = list.filter(
                (b) =>
                    (b.user?.fullName || "").toLowerCase().includes(q) ||
                    (b.user?.studentId || "").toLowerCase().includes(q) ||
                    (b.seat?.seatCode || "").toLowerCase().includes(q)
            );
        }
        return list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    }, [bookings, activeTab, searchText]);

    const counts = useMemo(() => {
        const c = { BOOKED: 0, CONFIRMED: 0, CANCELLED: 0, EXPIRED: 0 };
        bookings.forEach((b) => {
            if (c[b.status] !== undefined) c[b.status]++;
        });
        return c;
    }, [bookings]);

    return (
        <div className="lib-container">
            {/* Header */}
            <div className="lib-header">
                <div className="lib-header-left">
                    <h1>Quản lý đặt chỗ</h1>
                    <p className="lib-header-subtitle">
                        Danh sách đặt chỗ sinh viên trong hệ thống
                    </p>
                </div>
                <div className="lib-header-right">
                    <div className="lib-stats">
                        <span className="lib-stat-badge pending">
                            Đã đặt: {counts.BOOKED}
                        </span>
                        <span className="lib-stat-badge verified">
                            Đã xác nhận: {counts.CONFIRMED}
                        </span>
                        <span className="lib-stat-badge rejected">
                            Đã huỷ: {counts.CANCELLED}
                        </span>
                    </div>
                </div>
            </div>

            {/* Search + Tabs */}
            <div className="bm-controls">
                <div className="lib-search">
                    <Search size={16} className="lib-search-icon" />
                    <input
                        type="text"
                        placeholder="Tìm theo tên, mã sinh viên, mã ghế..."
                        value={searchText}
                        onChange={(e) => setSearchText(e.target.value)}
                    />
                </div>
            </div>

            <div className="lib-tabs">
                {TAB_LIST.map((tab) => (
                    <button
                        key={tab.key}
                        className={`lib-tab ${activeTab === tab.key ? "active" : ""}`}
                        onClick={() => setActiveTab(tab.key)}
                    >
                        {tab.label}
                        {tab.key !== "ALL" && counts[tab.key] > 0 && (
                            <span className="lib-tab-count">{counts[tab.key]}</span>
                        )}
                    </button>
                ))}
            </div>

            {/* Booking List */}
            {loading ? (
                <div className="lib-loading">
                    <div className="lib-spinner" />
                </div>
            ) : filteredBookings.length === 0 ? (
                <div className="lib-empty">
                    <div className="lib-empty-icon">&#128197;</div>
                    <h3>Chưa có đặt chỗ nào</h3>
                    <p>Các đặt chỗ từ sinh viên sẽ xuất hiện ở đây</p>
                </div>
            ) : (
                <div className="lib-card-list">
                    {filteredBookings.map((booking) => (
                        <div
                            key={booking.reservationId}
                            className="lib-card"
                            onClick={() => setSelectedBooking(booking)}
                        >
                            <div className="lib-card-header">
                                <div className="lib-user-info">
                                    {booking.user?.avatarUrl ? (
                                        <img
                                            src={booking.user.avatarUrl}
                                            alt=""
                                            className="lib-avatar"
                                        />
                                    ) : (
                                        <div className="lib-avatar-placeholder">
                                            {getInitial(booking.user?.fullName)}
                                        </div>
                                    )}
                                    <div>
                                        <h3>{booking.user?.fullName || "Sinh viên"}</h3>
                                        <div className="lib-user-code">
                                            {booking.user?.studentId || ""}
                                        </div>
                                    </div>
                                </div>
                                <span
                                    className={`lib-status-badge ${(booking.status || "").toLowerCase()}`}
                                >
                                    {STATUS_LABELS[booking.status] || booking.status}
                                </span>
                            </div>

                            <div className="bm-booking-info">
                                <span className="bm-seat-info">
                                    Ghế {booking.seat?.seatCode || "N/A"}
                                    {booking.seat?.zone?.zoneName &&
                                        ` - ${booking.seat.zone.zoneName}`}
                                </span>
                                <span className="bm-time-range">
                                    {formatTime(booking.startTime)} -{" "}
                                    {formatTime(booking.endTime)}
                                </span>
                            </div>

                            <div className="lib-card-footer">
                                <span className="lib-time">
                                    {formatDateTime(booking.createdAt)}
                                </span>
                                <div
                                    className="bm-actions"
                                    onClick={(e) => e.stopPropagation()}
                                >
                                    {(booking.status === "BOOKED" ||
                                        booking.status === "PROCESSING") && (
                                            <button
                                                className="lib-btn danger"
                                                onClick={() =>
                                                    handleCancelBooking(booking.reservationId)
                                                }
                                                disabled={submitting}
                                            >
                                                Huỷ đặt chỗ
                                            </button>
                                        )}
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Detail Modal */}
            {selectedBooking && (
                <div
                    className="lib-modal-overlay"
                    onClick={() => setSelectedBooking(null)}
                >
                    <div
                        className="lib-modal"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="lib-modal-header">
                            <h2>Chi tiết đặt chỗ</h2>
                            <button
                                className="lib-modal-close"
                                onClick={() => setSelectedBooking(null)}
                            >
                                &times;
                            </button>
                        </div>
                        <div className="lib-modal-body">
                            <div className="lib-modal-section">
                                <div className="lib-modal-label">Sinh viên</div>
                                <div className="lib-user-info">
                                    {selectedBooking.user?.avatarUrl ? (
                                        <img
                                            src={selectedBooking.user.avatarUrl}
                                            alt=""
                                            className="lib-avatar"
                                        />
                                    ) : (
                                        <div className="lib-avatar-placeholder">
                                            {getInitial(selectedBooking.user?.fullName)}
                                        </div>
                                    )}
                                    <div>
                                        <h3>{selectedBooking.user?.fullName || "Sinh viên"}</h3>
                                        <div className="lib-user-code">
                                            {selectedBooking.user?.studentId}
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div className="lib-modal-section">
                                <div className="lib-modal-label">Trạng thái</div>
                                <span
                                    className={`lib-status-badge ${(
                                        selectedBooking.status || ""
                                    ).toLowerCase()}`}
                                >
                                    {STATUS_LABELS[selectedBooking.status] ||
                                        selectedBooking.status}
                                </span>
                            </div>

                            <div className="lib-modal-section">
                                <div className="lib-modal-label">Chỗ ngồi</div>
                                <div className="lib-modal-text">
                                    Ghế {selectedBooking.seat?.seatCode || "N/A"}
                                    {selectedBooking.seat?.zone?.zoneName &&
                                        ` - ${selectedBooking.seat.zone.zoneName}`}
                                    {selectedBooking.seat?.zone?.area?.areaName &&
                                        ` (${selectedBooking.seat.zone.area.areaName})`}
                                </div>
                            </div>

                            <div className="lib-modal-section">
                                <div className="lib-modal-label">Thời gian</div>
                                <div className="lib-modal-text">
                                    {formatDateTime(selectedBooking.startTime)} -{" "}
                                    {formatDateTime(selectedBooking.endTime)}
                                </div>
                            </div>

                            <div className="lib-modal-section">
                                <div className="lib-modal-label">Ngày tạo</div>
                                <div className="lib-modal-text">
                                    {formatDateTime(selectedBooking.createdAt)}
                                </div>
                            </div>
                        </div>

                        <div className="lib-modal-footer">
                            {(selectedBooking.status === "BOOKED" ||
                                selectedBooking.status === "PROCESSING") && (
                                    <button
                                        className="lib-btn danger"
                                        onClick={() =>
                                            handleCancelBooking(selectedBooking.reservationId)
                                        }
                                        disabled={submitting}
                                    >
                                        {submitting ? "Đang xử lý..." : "Huỷ đặt chỗ"}
                                    </button>
                                )}
                            <button
                                className="lib-btn secondary"
                                onClick={() => setSelectedBooking(null)}
                            >
                                Đóng
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default BookingManage;
