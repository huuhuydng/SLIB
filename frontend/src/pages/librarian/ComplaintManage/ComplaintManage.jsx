import React, { useState, useEffect, useCallback, useMemo } from "react";
import { useSearchParams } from "react-router-dom";
import { Search } from "lucide-react";
import "../../../styles/librarian/librarian-shared.css";
import "../../../styles/librarian/ComplaintManage.css";

const API_BASE = `${import.meta.env.VITE_API_URL || "http://localhost:8080"}/slib`;

const STATUS_LABELS = {
    PENDING: "Chờ xử lý",
    ACCEPTED: "Chấp nhận",
    DENIED: "Từ chối",
};

const TAB_LIST = [
    { key: "ALL", label: "Tất cả" },
    { key: "PENDING", label: "Chờ xử lý" },
    { key: "ACCEPTED", label: "Chấp nhận" },
    { key: "DENIED", label: "Từ chối" },
];

function ComplaintManage() {
    const [searchParams] = useSearchParams();
    const [complaints, setComplaints] = useState([]);
    const [activeTab, setActiveTab] = useState(() => {
        const tabParam = searchParams.get("tab");
        if (tabParam && TAB_LIST.some(t => t.key === tabParam)) return tabParam;
        return "ALL";
    });
    const [loading, setLoading] = useState(true);
    const [selectedComplaint, setSelectedComplaint] = useState(null);
    const [searchText, setSearchText] = useState("");
    const [submitting, setSubmitting] = useState(false);

    const getToken = () =>
        sessionStorage.getItem("librarian_token") || localStorage.getItem("librarian_token");

    const fetchComplaints = useCallback(async () => {
        setLoading(true);
        try {
            const token = getToken();
            const res = await fetch(`${API_BASE}/complaints`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            if (res.ok) {
                setComplaints(await res.json());
            } else {
                setComplaints([]);
            }
        } catch (err) {
            console.error("Lỗi tải danh sách khiếu nại:", err);
            setComplaints([]);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchComplaints();
    }, [fetchComplaints]);

    const handleAccept = async (id) => {
        setSubmitting(true);
        try {
            const token = getToken();
            const res = await fetch(`${API_BASE}/complaints/${id}/accept`, {
                method: "PUT",
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ note: "Chấp nhận khiếu nại" }),
            });
            if (res.ok) {
                fetchComplaints();
                setSelectedComplaint(null);
            }
        } catch (err) {
            console.error("Lỗi chấp nhận khiếu nại:", err);
        } finally {
            setSubmitting(false);
        }
    };

    const handleDeny = async (id) => {
        setSubmitting(true);
        try {
            const token = getToken();
            const res = await fetch(`${API_BASE}/complaints/${id}/deny`, {
                method: "PUT",
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ note: "Từ chối khiếu nại" }),
            });
            if (res.ok) {
                fetchComplaints();
                setSelectedComplaint(null);
            }
        } catch (err) {
            console.error("Lỗi từ chối khiếu nại:", err);
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

    const getStatusClass = (status) => {
        switch (status) {
            case "PENDING": return "pending";
            case "ACCEPTED": return "verified";
            case "DENIED": return "rejected";
            default: return "pending";
        }
    };

    const filteredComplaints = useMemo(() => {
        let list = complaints;
        if (activeTab !== "ALL") {
            list = list.filter((c) => c.status === activeTab);
        }
        const q = searchText.trim().toLowerCase();
        if (q) {
            list = list.filter(
                (c) =>
                    (c.studentName || "").toLowerCase().includes(q) ||
                    (c.subject || "").toLowerCase().includes(q) ||
                    (c.content || "").toLowerCase().includes(q)
            );
        }
        return list;
    }, [complaints, activeTab, searchText]);

    const counts = useMemo(() => {
        const c = { PENDING: 0, ACCEPTED: 0, DENIED: 0 };
        complaints.forEach((item) => { if (c[item.status] !== undefined) c[item.status]++; });
        return c;
    }, [complaints]);

    return (
        <div className="lib-container">
            <div className="lib-page-title">
                <h1>Khiếu nại</h1>
                <div className="lib-inline-stats">
                    <span className="lib-inline-stat">
                        <span className="dot amber"></span>
                        Chờ xử lý <strong>{counts.PENDING}</strong>
                    </span>
                    <span className="lib-inline-stat">
                        <span className="dot green"></span>
                        Chấp nhận <strong>{counts.ACCEPTED}</strong>
                    </span>
                    <span className="lib-inline-stat">
                        <span className="dot red"></span>
                        Từ chối <strong>{counts.DENIED}</strong>
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
                        {tab.key === "PENDING" && counts.PENDING > 0 && (
                            <span className="lib-tab-count">{counts.PENDING}</span>
                        )}
                    </button>
                ))}
            </div>

            <div className="lib-controls">
                <div className="lib-search">
                    <Search size={16} className="lib-search-icon" />
                    <input
                        type="text"
                        placeholder="Tìm theo tên sinh viên, tiêu đề..."
                        value={searchText}
                        onChange={(e) => setSearchText(e.target.value)}
                    />
                </div>
            </div>

            {loading ? (
                <div className="lib-loading"><div className="lib-spinner" /></div>
            ) : filteredComplaints.length === 0 ? (
                <div className="lib-empty">
                    <div className="lib-empty-icon">&#128221;</div>
                    <h3>Chưa có khiếu nại nào</h3>
                    <p>Khiếu nại từ sinh viên khi bị trừ điểm sẽ xuất hiện ở đây</p>
                </div>
            ) : (
                <div className="lib-card-list">
                    {filteredComplaints.map((complaint, idx) => (
                        <div key={complaint.id || idx} className="lib-card" onClick={() => setSelectedComplaint(complaint)}>
                            <div className="lib-card-header">
                                <div className="lib-user-info">
                                    {complaint.studentAvatar ? (
                                        <img src={complaint.studentAvatar} alt="" className="lib-avatar" />
                                    ) : (
                                        <div className="lib-avatar-placeholder">{getInitial(complaint.studentName)}</div>
                                    )}
                                    <div>
                                        <h3>{complaint.studentName || "Sinh viên"}</h3>
                                        <div className="lib-user-code">{complaint.studentCode}</div>
                                    </div>
                                </div>
                                <span className={`lib-status-badge ${getStatusClass(complaint.status)}`}>
                                    {STATUS_LABELS[complaint.status] || complaint.status}
                                </span>
                            </div>

                            <div className="cm-complaint-content">
                                <div className="cm-violation-type">{complaint.subject}</div>
                                {complaint.content && <p className="lib-description">{complaint.content}</p>}
                            </div>

                            <div className="lib-card-footer">
                                <span className="lib-time">{formatDateTime(complaint.createdAt)}</span>
                                <div onClick={(e) => e.stopPropagation()}>
                                    {complaint.status === "PENDING" && (
                                        <div style={{ display: "flex", gap: 8 }}>
                                            <button className="lib-btn primary" onClick={() => handleAccept(complaint.id)} disabled={submitting}>
                                                Chấp nhận
                                            </button>
                                            <button className="lib-btn ghost danger" onClick={() => handleDeny(complaint.id)} disabled={submitting}>
                                                Từ chối
                                            </button>
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {selectedComplaint && (
                <>
                    <div className="lib-slide-overlay" onClick={() => setSelectedComplaint(null)} />
                    <div className="lib-slide-panel">
                        <div className="lib-slide-header">
                            <h2>Chi tiết khiếu nại</h2>
                            <button className="lib-slide-close" onClick={() => setSelectedComplaint(null)}>&times;</button>
                        </div>
                        <div className="lib-slide-body">
                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Sinh viên</div>
                                <div className="lib-user-info">
                                    {selectedComplaint.studentAvatar ? (
                                        <img src={selectedComplaint.studentAvatar} alt="" className="lib-avatar" />
                                    ) : (
                                        <div className="lib-avatar-placeholder">{getInitial(selectedComplaint.studentName)}</div>
                                    )}
                                    <div>
                                        <h3>{selectedComplaint.studentName || "Sinh viên"}</h3>
                                        <div className="lib-user-code">{selectedComplaint.studentCode}</div>
                                    </div>
                                </div>
                            </div>

                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Trạng thái</div>
                                <span className={`lib-status-badge ${getStatusClass(selectedComplaint.status)}`}>
                                    {STATUS_LABELS[selectedComplaint.status] || selectedComplaint.status}
                                </span>
                            </div>

                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Tiêu đề</div>
                                <div className="lib-slide-value">{selectedComplaint.subject}</div>
                            </div>

                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Nội dung</div>
                                <div className="lib-slide-value">{selectedComplaint.content}</div>
                            </div>

                            {selectedComplaint.evidenceUrl && (
                                <div className="lib-slide-section">
                                    <div className="lib-slide-label">Bằng chứng</div>
                                    <a href={selectedComplaint.evidenceUrl} target="_blank" rel="noopener noreferrer" className="lib-btn ghost">
                                        Xem bằng chứng
                                    </a>
                                </div>
                            )}

                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Thời gian gửi</div>
                                <div className="lib-slide-value">{formatDateTime(selectedComplaint.createdAt)}</div>
                            </div>

                            {selectedComplaint.resolvedByName && (
                                <div className="lib-slide-section">
                                    <div className="lib-slide-label">Xử lý bởi</div>
                                    <div className="lib-slide-value">
                                        {selectedComplaint.resolvedByName} - {formatDateTime(selectedComplaint.resolvedAt)}
                                    </div>
                                </div>
                            )}

                            {selectedComplaint.resolutionNote && (
                                <div className="lib-slide-section">
                                    <div className="lib-slide-label">Ghi chú xử lý</div>
                                    <div className="lib-slide-value">{selectedComplaint.resolutionNote}</div>
                                </div>
                            )}
                        </div>
                        <div className="lib-slide-footer">
                            {selectedComplaint.status === "PENDING" && (
                                <>
                                    <button className="lib-btn primary" onClick={() => handleAccept(selectedComplaint.id)} disabled={submitting}>
                                        {submitting ? "Đang xử lý..." : "Chấp nhận"}
                                    </button>
                                    <button className="lib-btn ghost danger" onClick={() => handleDeny(selectedComplaint.id)} disabled={submitting}>
                                        {submitting ? "Đang xử lý..." : "Từ chối"}
                                    </button>
                                </>
                            )}
                            <button className="lib-btn ghost" onClick={() => setSelectedComplaint(null)}>Đóng</button>
                        </div>
                    </div>
                </>
            )}
        </div>
    );
}

export default ComplaintManage;
