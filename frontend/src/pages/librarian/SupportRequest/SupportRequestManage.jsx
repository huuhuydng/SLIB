import React, { useState, useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Search } from "lucide-react";
import "../../../styles/librarian/librarian-shared.css";
import "../../../styles/librarian/SupportRequestManage.css";

const API_BASE = `${import.meta.env.VITE_API_URL || "http://localhost:8080"}/slib/support-requests`;

const STATUS_LABELS = {
    PENDING: "Chờ xử lý",
    IN_PROGRESS: "Đang xử lý",
    RESOLVED: "Đã giải quyết",
    REJECTED: "Từ chối",
};

const TAB_LIST = [
    { key: "ALL", label: "Tất cả" },
    { key: "PENDING", label: "Chờ xử lý" },
    { key: "IN_PROGRESS", label: "Đang xử lý" },
    { key: "RESOLVED", label: "Đã giải quyết" },
];

function SupportRequestManage() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [requests, setRequests] = useState([]);
    const [counts, setCounts] = useState({ pending: 0, inProgress: 0, resolved: 0 });
    const [activeTab, setActiveTab] = useState(() => {
        const tabParam = searchParams.get("tab");
        if (tabParam && TAB_LIST.some(t => t.key === tabParam)) return tabParam;
        return "ALL";
    });
    const [loading, setLoading] = useState(true);
    const [selectedRequest, setSelectedRequest] = useState(null);
    const [responseText, setResponseText] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [lightboxImage, setLightboxImage] = useState(null);
    const [chatLoading, setChatLoading] = useState(false);
    const [searchTerm, setSearchTerm] = useState("");

    const getToken = () => sessionStorage.getItem("librarian_token") || localStorage.getItem("librarian_token");

    const fetchRequests = useCallback(async () => {
        setLoading(true);
        try {
            const token = getToken();
            const url = activeTab === "ALL" ? API_BASE : `${API_BASE}?status=${activeTab}`;
            const res = await fetch(url, {
                headers: { Authorization: `Bearer ${token}` },
            });
            if (res.ok) {
                setRequests(await res.json());
            }
        } catch (err) {
            console.error("Error fetching support requests:", err);
        } finally {
            setLoading(false);
        }
    }, [activeTab]);

    const fetchCounts = useCallback(async () => {
        try {
            const token = getToken();
            const res = await fetch(`${API_BASE}/count`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            if (res.ok) {
                setCounts(await res.json());
            }
        } catch (err) {
            console.error("Error fetching counts:", err);
        }
    }, []);

    useEffect(() => {
        fetchRequests();
        fetchCounts();
    }, [fetchRequests, fetchCounts]);

    const handleUpdateStatus = async (id, status) => {
        try {
            const token = getToken();
            const res = await fetch(`${API_BASE}/${id}/status`, {
                method: "PUT",
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ status }),
            });
            if (res.ok) {
                fetchRequests();
                fetchCounts();
                if (selectedRequest?.id === id) {
                    setSelectedRequest(await res.json());
                }
            }
        } catch (err) {
            console.error("Error updating status:", err);
        }
    };

    const handleRespond = async () => {
        if (!responseText.trim() || !selectedRequest) return;
        setSubmitting(true);
        try {
            const token = getToken();
            const res = await fetch(`${API_BASE}/${selectedRequest.id}/respond`, {
                method: "PUT",
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ response: responseText }),
            });
            if (res.ok) {
                setSelectedRequest(null);
                setResponseText("");
                fetchRequests();
                fetchCounts();
            }
        } catch (err) {
            console.error("Error responding:", err);
        } finally {
            setSubmitting(false);
        }
    };

    const handleStartChat = async (requestId) => {
        setChatLoading(true);
        try {
            const token = getToken();
            const res = await fetch(`${API_BASE}/${requestId}/chat`, {
                method: "POST",
                headers: { Authorization: `Bearer ${token}` },
            });
            if (res.ok) {
                const data = await res.json();
                navigate(`/librarian/chat?conversationId=${data.conversationId}`);
            }
        } catch (err) {
            console.error("Error starting chat:", err);
        } finally {
            setChatLoading(false);
        }
    };

    const formatTime = (iso) => {
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

    const getInitial = (name) => (name ? name.charAt(0).toUpperCase() : "?");

    const getStatusClass = (status) => {
        const map = { PENDING: "pending", IN_PROGRESS: "in-progress", RESOLVED: "confirmed", REJECTED: "rejected" };
        return map[status] || "pending";
    };

    const filteredRequests = requests.filter((r) => {
        if (!searchTerm) return true;
        const term = searchTerm.toLowerCase();
        return (
            (r.studentName || "").toLowerCase().includes(term) ||
            (r.studentCode || "").toLowerCase().includes(term) ||
            (r.description || "").toLowerCase().includes(term)
        );
    });

    return (
        <div className="lib-container">
            {/* Page Title + Inline Stats */}
            <div className="lib-page-title">
                <h1>Yêu cầu hỗ trợ</h1>
                <div className="lib-inline-stats">
                    <span className="lib-inline-stat">
                        <span className="dot amber"></span>
                        Chờ xử lý <strong>{counts.pending}</strong>
                    </span>
                    <span className="lib-inline-stat">
                        <span className="dot blue"></span>
                        Đang xử lý <strong>{counts.inProgress}</strong>
                    </span>
                    <span className="lib-inline-stat">
                        <span className="dot green"></span>
                        Đã giải quyết <strong>{counts.resolved}</strong>
                    </span>
                </div>
            </div>

            {/* Tabs */}
            <div className="lib-tabs">
                {TAB_LIST.map((tab) => (
                    <button
                        key={tab.key}
                        className={`lib-tab ${activeTab === tab.key ? "active" : ""}`}
                        onClick={() => setActiveTab(tab.key)}
                    >
                        {tab.label}
                        {tab.key === "PENDING" && counts.pending > 0 && (
                            <span className="lib-tab-count">{counts.pending}</span>
                        )}
                    </button>
                ))}
            </div>

            {/* Search */}
            <div className="lib-controls">
                <div className="lib-search">
                    <Search size={16} className="lib-search-icon" />
                    <input
                        type="text"
                        placeholder="Tìm kiếm yêu cầu..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                    />
                </div>
            </div>

            {/* Request List */}
            {loading ? (
                <div className="lib-loading">
                    <div className="lib-spinner" />
                </div>
            ) : filteredRequests.length === 0 ? (
                <div className="lib-empty">
                    <h3>Chưa có yêu cầu nào</h3>
                    <p>Các yêu cầu hỗ trợ từ sinh viên sẽ xuất hiện ở đây</p>
                </div>
            ) : (
                <div className="lib-card-list">
                    {filteredRequests.map((req) => (
                        <div
                            key={req.id}
                            className="lib-card"
                            onClick={() => {
                                setSelectedRequest(req);
                                setResponseText(req.adminResponse || "");
                            }}
                        >
                            <div className="lib-card-header">
                                <div className="lib-user-info">
                                    {req.studentAvatar ? (
                                        <img src={req.studentAvatar} alt="" className="lib-avatar" />
                                    ) : (
                                        <div className="lib-avatar-placeholder">
                                            {getInitial(req.studentName)}
                                        </div>
                                    )}
                                    <div>
                                        <h3>{req.studentName}</h3>
                                        <div className="lib-user-code">{req.studentCode}</div>
                                    </div>
                                </div>
                                <span className={`lib-status-badge ${getStatusClass(req.status)}`}>
                                    {STATUS_LABELS[req.status]}
                                </span>
                            </div>

                            <div className="lib-description">{req.description}</div>

                            {req.imageUrls && req.imageUrls.length > 0 && (
                                <div className="lib-images">
                                    {req.imageUrls.slice(0, 4).map((url, idx) => (
                                        <img
                                            key={idx}
                                            src={url}
                                            alt=""
                                            className="lib-image-thumbnail"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                setLightboxImage(url);
                                            }}
                                        />
                                    ))}
                                    {req.imageUrls.length > 4 && (
                                        <div className="lib-image-more">
                                            +{req.imageUrls.length - 4}
                                        </div>
                                    )}
                                </div>
                            )}

                            <div className="lib-card-footer">
                                <span className="lib-time">{formatTime(req.createdAt)}</span>
                                <div className="sr-actions" onClick={(e) => e.stopPropagation()}>
                                    {req.status === "PENDING" && (
                                        <>
                                            <button
                                                className="lib-btn primary"
                                                onClick={() => handleUpdateStatus(req.id, "IN_PROGRESS")}
                                            >
                                                Tiếp nhận
                                            </button>
                                            <button
                                                className="lib-btn ghost danger"
                                                onClick={() => handleUpdateStatus(req.id, "REJECTED")}
                                            >
                                                Từ chối
                                            </button>
                                        </>
                                    )}
                                    {req.status === "IN_PROGRESS" && (
                                        <>
                                            <button
                                                className="lib-btn ghost"
                                                onClick={() => handleStartChat(req.id)}
                                                disabled={chatLoading}
                                            >
                                                {chatLoading ? "Đang mở..." : "Chat"}
                                            </button>
                                            <button
                                                className="lib-btn primary"
                                                onClick={() => {
                                                    setSelectedRequest(req);
                                                    setResponseText("");
                                                }}
                                            >
                                                Phản hồi
                                            </button>
                                        </>
                                    )}
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Slide Panel - Detail */}
            {selectedRequest && (
                <>
                    <div className="lib-slide-overlay" onClick={() => setSelectedRequest(null)} />
                    <div className="lib-slide-panel">
                        <div className="lib-slide-header">
                            <h2>Chi tiết yêu cầu</h2>
                            <button
                                className="lib-slide-close"
                                onClick={() => setSelectedRequest(null)}
                            >
                                &times;
                            </button>
                        </div>
                        <div className="lib-slide-body">
                            {/* Student Info */}
                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Sinh viên</div>
                                <div className="lib-user-info">
                                    {selectedRequest.studentAvatar ? (
                                        <img src={selectedRequest.studentAvatar} alt="" className="lib-avatar" />
                                    ) : (
                                        <div className="lib-avatar-placeholder">
                                            {getInitial(selectedRequest.studentName)}
                                        </div>
                                    )}
                                    <div>
                                        <h3>{selectedRequest.studentName}</h3>
                                        <div className="lib-user-code">{selectedRequest.studentCode}</div>
                                    </div>
                                </div>
                            </div>

                            {/* Status */}
                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Trạng thái</div>
                                <span className={`lib-status-badge ${getStatusClass(selectedRequest.status)}`}>
                                    {STATUS_LABELS[selectedRequest.status]}
                                </span>
                            </div>

                            {/* Description */}
                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Mô tả vấn đề</div>
                                <div className="lib-slide-value">{selectedRequest.description}</div>
                            </div>

                            {/* Images */}
                            {selectedRequest.imageUrls && selectedRequest.imageUrls.length > 0 && (
                                <div className="lib-slide-section">
                                    <div className="lib-slide-label">
                                        Hình ảnh ({selectedRequest.imageUrls.length})
                                    </div>
                                    <div className="lib-images">
                                        {selectedRequest.imageUrls.map((url, idx) => (
                                            <img
                                                key={idx}
                                                src={url}
                                                alt=""
                                                className="lib-image-thumbnail"
                                                onClick={() => setLightboxImage(url)}
                                            />
                                        ))}
                                    </div>
                                </div>
                            )}

                            {/* Time */}
                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Thời gian gửi</div>
                                <div className="lib-slide-value">{formatTime(selectedRequest.createdAt)}</div>
                            </div>

                            {/* Existing Response */}
                            {selectedRequest.adminResponse && (
                                <div className="lib-slide-section">
                                    <div className="lib-slide-label">Phản hồi từ thủ thư</div>
                                    <div className="lib-slide-value">{selectedRequest.adminResponse}</div>
                                    {selectedRequest.resolvedByName && (
                                        <div className="sr-resolver-info">
                                            Phản hồi bởi: {selectedRequest.resolvedByName} - {formatTime(selectedRequest.resolvedAt)}
                                        </div>
                                    )}
                                </div>
                            )}

                            {/* Response Form */}
                            {(selectedRequest.status === "PENDING" || selectedRequest.status === "IN_PROGRESS") && (
                                <div className="lib-slide-section">
                                    <div className="lib-slide-label">Phản hồi yêu cầu</div>
                                    <textarea
                                        className="lib-textarea"
                                        value={responseText}
                                        onChange={(e) => setResponseText(e.target.value)}
                                        placeholder="Nhập phản hồi cho sinh viên..."
                                    />
                                </div>
                            )}
                        </div>

                        <div className="lib-slide-footer">
                            {selectedRequest.status === "PENDING" && (
                                <>
                                    <button
                                        className="lib-btn primary"
                                        onClick={() => handleUpdateStatus(selectedRequest.id, "IN_PROGRESS")}
                                    >
                                        Tiếp nhận
                                    </button>
                                    <button
                                        className="lib-btn ghost danger"
                                        onClick={() => handleUpdateStatus(selectedRequest.id, "REJECTED")}
                                    >
                                        Từ chối
                                    </button>
                                </>
                            )}
                            {(selectedRequest.status === "PENDING" || selectedRequest.status === "IN_PROGRESS") && (
                                <>
                                    <button
                                        className="lib-btn ghost"
                                        onClick={() => handleStartChat(selectedRequest.id)}
                                        disabled={chatLoading}
                                    >
                                        {chatLoading ? "Đang mở..." : "Chat với sinh viên"}
                                    </button>
                                    <button
                                        className="lib-btn primary"
                                        onClick={handleRespond}
                                        disabled={submitting || !responseText.trim()}
                                    >
                                        {submitting ? "Đang gửi..." : "Gửi phản hồi"}
                                    </button>
                                </>
                            )}
                            <button
                                className="lib-btn ghost"
                                onClick={() => setSelectedRequest(null)}
                            >
                                Đóng
                            </button>
                        </div>
                    </div>
                </>
            )}

            {/* Lightbox */}
            {lightboxImage && (
                <div className="lib-lightbox" onClick={() => setLightboxImage(null)}>
                    <img src={lightboxImage} alt="" />
                </div>
            )}
        </div>
    );
}

export default SupportRequestManage;
