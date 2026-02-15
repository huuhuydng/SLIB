import React, { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import "../../../styles/librarian/SupportRequestManage.css";

const API_BASE = `${import.meta.env.VITE_API_URL || "http://localhost:8080"}/slib/support-requests`;

// Status Labels tiếng Việt
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
    const [requests, setRequests] = useState([]);
    const [counts, setCounts] = useState({ pending: 0, inProgress: 0, resolved: 0 });
    const [activeTab, setActiveTab] = useState("ALL");
    const [loading, setLoading] = useState(true);
    const [selectedRequest, setSelectedRequest] = useState(null);
    const [responseText, setResponseText] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [lightboxImage, setLightboxImage] = useState(null);
    const [chatLoading, setChatLoading] = useState(false);

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
                const data = await res.json();
                setRequests(data);
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

    const getInitial = (name) => {
        return name ? name.charAt(0).toUpperCase() : "?";
    };

    return (
        <div className="sr-container">
            {/* Header */}
            <div className="sr-header">
                <h1>Yêu cầu hỗ trợ</h1>
                <div className="sr-stats">
                    <span className="sr-stat-badge pending">
                        Chờ xử lý: {counts.pending}
                    </span>
                    <span className="sr-stat-badge in-progress">
                        Đang xử lý: {counts.inProgress}
                    </span>
                    <span className="sr-stat-badge resolved">
                        Đã giải quyết: {counts.resolved}
                    </span>
                </div>
            </div>

            {/* Tabs */}
            <div className="sr-tabs">
                {TAB_LIST.map((tab) => (
                    <button
                        key={tab.key}
                        className={`sr-tab ${activeTab === tab.key ? "active" : ""}`}
                        onClick={() => setActiveTab(tab.key)}
                    >
                        {tab.label}
                        {tab.key === "PENDING" && counts.pending > 0 && (
                            <span className="sr-tab-count">{counts.pending}</span>
                        )}
                    </button>
                ))}
            </div>

            {/* Request List */}
            {loading ? (
                <div className="sr-loading">
                    <div className="sr-spinner" />
                </div>
            ) : requests.length === 0 ? (
                <div className="sr-empty">
                    <div className="sr-empty-icon">&#128233;</div>
                    <h3>Chưa có yêu cầu nào</h3>
                    <p>Các yêu cầu hỗ trợ từ sinh viên sẽ xuất hiện ở đây</p>
                </div>
            ) : (
                <div className="sr-list">
                    {requests.map((req) => (
                        <div
                            key={req.id}
                            className="sr-card"
                            onClick={() => {
                                setSelectedRequest(req);
                                setResponseText(req.adminResponse || "");
                            }}
                        >
                            <div className="sr-card-header">
                                <div className="sr-student-info">
                                    {req.studentAvatar ? (
                                        <img
                                            src={req.studentAvatar}
                                            alt=""
                                            className="sr-avatar"
                                        />
                                    ) : (
                                        <div className="sr-avatar-placeholder">
                                            {getInitial(req.studentName)}
                                        </div>
                                    )}
                                    <div className="sr-student-details">
                                        <h3>{req.studentName}</h3>
                                        <div className="sr-student-code">{req.studentCode}</div>
                                    </div>
                                </div>
                                <span
                                    className={`sr-status-badge ${req.status.toLowerCase()}`}
                                >
                                    {STATUS_LABELS[req.status]}
                                </span>
                            </div>

                            <div className="sr-description">{req.description}</div>

                            {req.imageUrls && req.imageUrls.length > 0 && (
                                <div className="sr-images">
                                    {req.imageUrls.slice(0, 4).map((url, idx) => (
                                        <img
                                            key={idx}
                                            src={url}
                                            alt=""
                                            className="sr-image-thumbnail"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                setLightboxImage(url);
                                            }}
                                        />
                                    ))}
                                    {req.imageUrls.length > 4 && (
                                        <div
                                            className="sr-image-thumbnail"
                                            style={{
                                                display: "flex",
                                                alignItems: "center",
                                                justifyContent: "center",
                                                background: "#f5f5f5",
                                                color: "#666",
                                                fontWeight: 600,
                                            }}
                                        >
                                            +{req.imageUrls.length - 4}
                                        </div>
                                    )}
                                </div>
                            )}

                            <div className="sr-card-footer">
                                <span className="sr-time">{formatTime(req.createdAt)}</span>
                                <div className="sr-actions" onClick={(e) => e.stopPropagation()}>
                                    {req.status === "PENDING" && (
                                        <>
                                            <button
                                                className="sr-btn primary"
                                                onClick={() => handleUpdateStatus(req.id, "IN_PROGRESS")}
                                            >
                                                Tiếp nhận
                                            </button>
                                            <button
                                                className="sr-btn danger"
                                                onClick={() => handleUpdateStatus(req.id, "REJECTED")}
                                            >
                                                Từ chối
                                            </button>
                                        </>
                                    )}
                                    {req.status === "IN_PROGRESS" && (
                                        <>
                                            <button
                                                className="sr-btn chat"
                                                onClick={() => handleStartChat(req.id)}
                                                disabled={chatLoading}
                                            >
                                                {chatLoading ? "Đang mở..." : "Chat"}
                                            </button>
                                            <button
                                                className="sr-btn success"
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

            {/* Detail Modal */}
            {selectedRequest && (
                <div className="sr-modal-overlay" onClick={() => setSelectedRequest(null)}>
                    <div className="sr-modal" onClick={(e) => e.stopPropagation()}>
                        <div className="sr-modal-header">
                            <h2>Chi tiết yêu cầu</h2>
                            <button
                                className="sr-modal-close"
                                onClick={() => setSelectedRequest(null)}
                            >
                                &times;
                            </button>
                        </div>
                        <div className="sr-modal-body">
                            {/* Student Info */}
                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Sinh viên</div>
                                <div className="sr-student-info">
                                    {selectedRequest.studentAvatar ? (
                                        <img
                                            src={selectedRequest.studentAvatar}
                                            alt=""
                                            className="sr-avatar"
                                        />
                                    ) : (
                                        <div className="sr-avatar-placeholder">
                                            {getInitial(selectedRequest.studentName)}
                                        </div>
                                    )}
                                    <div className="sr-student-details">
                                        <h3>{selectedRequest.studentName}</h3>
                                        <div className="sr-student-code">
                                            {selectedRequest.studentCode}
                                        </div>
                                    </div>
                                </div>
                            </div>

                            {/* Status */}
                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Trạng thái</div>
                                <span className={`sr-status-badge ${selectedRequest.status.toLowerCase()}`}>
                                    {STATUS_LABELS[selectedRequest.status]}
                                </span>
                            </div>

                            {/* Description */}
                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Mô tả vấn đề</div>
                                <div className="sr-modal-text">{selectedRequest.description}</div>
                            </div>

                            {/* Images */}
                            {selectedRequest.imageUrls && selectedRequest.imageUrls.length > 0 && (
                                <div className="sr-modal-section">
                                    <div className="sr-modal-label">
                                        Hình ảnh ({selectedRequest.imageUrls.length})
                                    </div>
                                    <div className="sr-modal-images">
                                        {selectedRequest.imageUrls.map((url, idx) => (
                                            <img
                                                key={idx}
                                                src={url}
                                                alt=""
                                                className="sr-modal-image"
                                                onClick={() => setLightboxImage(url)}
                                            />
                                        ))}
                                    </div>
                                </div>
                            )}

                            {/* Time */}
                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Thời gian gửi</div>
                                <div className="sr-modal-text">{formatTime(selectedRequest.createdAt)}</div>
                            </div>

                            {/* Existing Response */}
                            {selectedRequest.adminResponse && (
                                <div className="sr-modal-section">
                                    <div className="sr-modal-label">Phản hồi từ thủ thư</div>
                                    <div className="sr-modal-text">{selectedRequest.adminResponse}</div>
                                    {selectedRequest.resolvedByName && (
                                        <div style={{ fontSize: 12, color: "#888", marginTop: 4 }}>
                                            Phản hồi bởi: {selectedRequest.resolvedByName} - {formatTime(selectedRequest.resolvedAt)}
                                        </div>
                                    )}
                                </div>
                            )}

                            {/* Response Form (for PENDING/IN_PROGRESS) */}
                            {(selectedRequest.status === "PENDING" || selectedRequest.status === "IN_PROGRESS") && (
                                <div className="sr-modal-section sr-modal-response">
                                    <div className="sr-modal-label">Phản hồi yêu cầu</div>
                                    <textarea
                                        value={responseText}
                                        onChange={(e) => setResponseText(e.target.value)}
                                        placeholder="Nhập phản hồi cho sinh viên..."
                                    />
                                </div>
                            )}
                        </div>

                        <div className="sr-modal-footer">
                            {selectedRequest.status === "PENDING" && (
                                <>
                                    <button
                                        className="sr-btn primary"
                                        onClick={() =>
                                            handleUpdateStatus(selectedRequest.id, "IN_PROGRESS")
                                        }
                                    >
                                        Tiếp nhận
                                    </button>
                                    <button
                                        className="sr-btn danger"
                                        onClick={() =>
                                            handleUpdateStatus(selectedRequest.id, "REJECTED")
                                        }
                                    >
                                        Từ chối
                                    </button>
                                </>
                            )}
                            {(selectedRequest.status === "PENDING" || selectedRequest.status === "IN_PROGRESS") && (
                                <>
                                    <button
                                        className="sr-btn chat"
                                        onClick={() => handleStartChat(selectedRequest.id)}
                                        disabled={chatLoading}
                                    >
                                        {chatLoading ? "Đang mở..." : "Chat với sinh viên"}
                                    </button>
                                    <button
                                        className="sr-btn success"
                                        onClick={handleRespond}
                                        disabled={submitting || !responseText.trim()}
                                    >
                                        {submitting ? "Đang gửi..." : "Gửi phản hồi và giải quyết"}
                                    </button>
                                </>
                            )}
                            <button
                                className="sr-btn secondary"
                                onClick={() => setSelectedRequest(null)}
                            >
                                Đóng
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Lightbox */}
            {lightboxImage && (
                <div className="sr-lightbox" onClick={() => setLightboxImage(null)}>
                    <img src={lightboxImage} alt="" />
                </div>
            )}
        </div>
    );
}

export default SupportRequestManage;
