import React, { useState, useEffect, useCallback, useMemo } from "react";
import { useSearchParams } from "react-router-dom";
import { Search, Star, Eye } from "lucide-react";
import "../../../styles/librarian/librarian-shared.css";
import "../../../styles/librarian/FeedbackManage.css";

const API_BASE = `${import.meta.env.VITE_API_URL || "http://localhost:8080"}/slib`;

const TAB_LIST = [
    { key: "ALL", label: "Tất cả" },
    { key: "NEW", label: "Mới" },
    { key: "REVIEWED", label: "Đã xem" },
    { key: "ACTED", label: "Đã xử lý" },
];

function FeedbackManage() {
    const [searchParams] = useSearchParams();
    const [feedbacks, setFeedbacks] = useState([]);
    const [activeTab, setActiveTab] = useState(() => {
        const tabParam = searchParams.get("tab");
        if (tabParam && TAB_LIST.some(t => t.key === tabParam)) return tabParam;
        return "ALL";
    });
    const [loading, setLoading] = useState(true);
    const [selectedFeedback, setSelectedFeedback] = useState(null);
    const [searchText, setSearchText] = useState("");
    const [submitting, setSubmitting] = useState(false);

    const getToken = () =>
        sessionStorage.getItem("librarian_token") || localStorage.getItem("librarian_token");

    const fetchFeedbacks = useCallback(async () => {
        setLoading(true);
        try {
            const token = getToken();
            const res = await fetch(`${API_BASE}/feedbacks`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            if (res.ok) {
                setFeedbacks(await res.json());
            } else {
                setFeedbacks([]);
            }
        } catch (err) {
            console.error("Lỗi tải danh sách phản hồi:", err);
            setFeedbacks([]);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchFeedbacks();
    }, [fetchFeedbacks]);

    const handleMarkReviewed = async (id) => {
        setSubmitting(true);
        try {
            const token = getToken();
            const res = await fetch(`${API_BASE}/feedbacks/${id}/review`, {
                method: "PUT",
                headers: { Authorization: `Bearer ${token}` },
            });
            if (res.ok) {
                fetchFeedbacks();
                setSelectedFeedback(null);
            }
        } catch (err) {
            console.error("Lỗi đánh dấu đã xem:", err);
        } finally {
            setSubmitting(false);
        }
    };

    const formatDateTime = (iso) => {
        if (!iso) return "";
        return new Date(iso).toLocaleString("vi-VN", {
            day: "2-digit", month: "2-digit", year: "numeric",
            hour: "2-digit", minute: "2-digit",
        });
    };

    const getInitial = (name) => (name ? name.charAt(0).toUpperCase() : "?");

    const getStatusLabel = (status) => {
        switch (status) {
            case "NEW": return "Mới";
            case "REVIEWED": return "Đã xem";
            case "ACTED": return "Đã xử lý";
            default: return status || "Mới";
        }
    };

    const getStatusClass = (status) => {
        switch (status) {
            case "NEW": return "pending";
            case "REVIEWED": return "in-progress";
            case "ACTED": return "verified";
            default: return "pending";
        }
    };

    const getRatingLabel = (rating) => {
        if (!rating) return null;
        if (rating >= 4) return "Tích cực";
        if (rating >= 3) return "Trung lập";
        return "Tiêu cực";
    };

    const renderStars = (rating) => {
        if (!rating) return null;
        return (
            <div className="fm-stars">
                {[1, 2, 3, 4, 5].map((i) => (
                    <Star
                        key={i}
                        size={14}
                        fill={i <= rating ? "#f59e0b" : "none"}
                        color={i <= rating ? "#f59e0b" : "#d1d5db"}
                    />
                ))}
                <span className="fm-rating-text">({getRatingLabel(rating)})</span>
            </div>
        );
    };

    const filteredFeedbacks = useMemo(() => {
        let list = feedbacks;
        if (activeTab !== "ALL") {
            list = list.filter((f) => f.status === activeTab);
        }
        const q = searchText.trim().toLowerCase();
        if (q) {
            list = list.filter(
                (f) =>
                    (f.studentName || "").toLowerCase().includes(q) ||
                    (f.content || "").toLowerCase().includes(q)
            );
        }
        return list;
    }, [feedbacks, activeTab, searchText]);

    const counts = useMemo(() => {
        const c = { NEW: 0, REVIEWED: 0, ACTED: 0 };
        feedbacks.forEach((f) => { if (c[f.status] !== undefined) c[f.status]++; });
        return c;
    }, [feedbacks]);

    return (
        <div className="lib-container">
            <div className="lib-page-title">
                <h1>Phản hồi sinh viên</h1>
                <div className="lib-inline-stats">
                    <span className="lib-inline-stat">
                        <span className="dot amber"></span>
                        Mới <strong>{counts.NEW}</strong>
                    </span>
                    <span className="lib-inline-stat">
                        <span className="dot blue"></span>
                        Đã xem <strong>{counts.REVIEWED}</strong>
                    </span>
                    <span className="lib-inline-stat">
                        <span className="dot green"></span>
                        Tổng <strong>{feedbacks.length}</strong>
                    </span>
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

            <div className="lib-controls">
                <div className="lib-search">
                    <Search size={16} className="lib-search-icon" />
                    <input
                        type="text"
                        placeholder="Tìm theo tên sinh viên, nội dung..."
                        value={searchText}
                        onChange={(e) => setSearchText(e.target.value)}
                    />
                </div>
            </div>

            {loading ? (
                <div className="lib-loading"><div className="lib-spinner" /></div>
            ) : filteredFeedbacks.length === 0 ? (
                <div className="lib-empty">
                    <div className="lib-empty-icon">&#11088;</div>
                    <h3>Chưa có phản hồi nào</h3>
                    <p>Phản hồi từ sinh viên sau khi rời thư viện sẽ xuất hiện ở đây</p>
                </div>
            ) : (
                <div className="lib-card-list">
                    {filteredFeedbacks.map((fb, idx) => (
                        <div key={fb.id || idx} className="lib-card" onClick={() => setSelectedFeedback(fb)}>
                            <div className="lib-card-header">
                                <div className="lib-user-info">
                                    {fb.studentAvatar ? (
                                        <img src={fb.studentAvatar} alt="" className="lib-avatar" />
                                    ) : (
                                        <div className="lib-avatar-placeholder">{getInitial(fb.studentName)}</div>
                                    )}
                                    <div>
                                        <h3>{fb.studentName || "Sinh viên"}</h3>
                                        <div className="lib-user-code">{fb.studentCode}</div>
                                    </div>
                                </div>
                                <span className={`lib-status-badge ${getStatusClass(fb.status)}`}>
                                    {getStatusLabel(fb.status)}
                                </span>
                            </div>

                            <div className="fm-feedback-content">
                                {renderStars(fb.rating)}
                                {fb.content && <p className="lib-description">{fb.content}</p>}
                            </div>

                            {fb.category && (
                                <div style={{ marginTop: 6 }}>
                                    <span className="fm-seat-tag">{fb.category}</span>
                                </div>
                            )}

                            <div className="lib-card-footer">
                                <span className="lib-time">{formatDateTime(fb.createdAt)}</span>
                                {fb.status === "NEW" && (
                                    <div onClick={(e) => e.stopPropagation()}>
                                        <button
                                            className="lib-btn primary"
                                            onClick={() => handleMarkReviewed(fb.id)}
                                            disabled={submitting}
                                        >
                                            <Eye size={14} /> Đánh dấu đã xem
                                        </button>
                                    </div>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {selectedFeedback && (
                <>
                    <div className="lib-slide-overlay" onClick={() => setSelectedFeedback(null)} />
                    <div className="lib-slide-panel">
                        <div className="lib-slide-header">
                            <h2>Chi tiết phản hồi</h2>
                            <button className="lib-slide-close" onClick={() => setSelectedFeedback(null)}>&times;</button>
                        </div>
                        <div className="lib-slide-body">
                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Sinh viên</div>
                                <div className="lib-user-info">
                                    {selectedFeedback.studentAvatar ? (
                                        <img src={selectedFeedback.studentAvatar} alt="" className="lib-avatar" />
                                    ) : (
                                        <div className="lib-avatar-placeholder">{getInitial(selectedFeedback.studentName)}</div>
                                    )}
                                    <div>
                                        <h3>{selectedFeedback.studentName || "Sinh viên"}</h3>
                                        <div className="lib-user-code">{selectedFeedback.studentCode}</div>
                                    </div>
                                </div>
                            </div>

                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Đánh giá</div>
                                <div>{renderStars(selectedFeedback.rating)}</div>
                            </div>

                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Trạng thái</div>
                                <span className={`lib-status-badge ${getStatusClass(selectedFeedback.status)}`}>
                                    {getStatusLabel(selectedFeedback.status)}
                                </span>
                            </div>

                            {selectedFeedback.content && (
                                <div className="lib-slide-section">
                                    <div className="lib-slide-label">Nội dung</div>
                                    <div className="lib-slide-value">{selectedFeedback.content}</div>
                                </div>
                            )}

                            {selectedFeedback.category && (
                                <div className="lib-slide-section">
                                    <div className="lib-slide-label">Danh mục</div>
                                    <div className="lib-slide-value">{selectedFeedback.category}</div>
                                </div>
                            )}

                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Thời gian</div>
                                <div className="lib-slide-value">{formatDateTime(selectedFeedback.createdAt)}</div>
                            </div>

                            {selectedFeedback.reviewedByName && (
                                <div className="lib-slide-section">
                                    <div className="lib-slide-label">Xem bởi</div>
                                    <div className="lib-slide-value">
                                        {selectedFeedback.reviewedByName} - {formatDateTime(selectedFeedback.reviewedAt)}
                                    </div>
                                </div>
                            )}
                        </div>
                        <div className="lib-slide-footer">
                            {selectedFeedback.status === "NEW" && (
                                <button className="lib-btn primary" onClick={() => handleMarkReviewed(selectedFeedback.id)} disabled={submitting}>
                                    {submitting ? "Đang xử lý..." : "Đánh dấu đã xem"}
                                </button>
                            )}
                            <button className="lib-btn ghost" onClick={() => setSelectedFeedback(null)}>Đóng</button>
                        </div>
                    </div>
                </>
            )}
        </div>
    );
}

export default FeedbackManage;
