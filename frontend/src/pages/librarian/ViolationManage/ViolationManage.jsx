import React, { useState, useEffect, useCallback } from "react";
import { useSearchParams } from "react-router-dom";
import { Search } from "lucide-react";
import "../../../styles/librarian/librarian-shared.css";
import "../../../styles/librarian/ViolationManage.css";

const API_BASE = `${import.meta.env.VITE_API_URL || "http://localhost:8080"}/slib/violation-reports`;

const STATUS_LABELS = {
    PENDING: "Chờ xử lý",
    VERIFIED: "Đã xác minh",
    RESOLVED: "Đã xử lý",
    REJECTED: "Từ chối",
};

const VIOLATION_TYPE_LABELS = {
    UNAUTHORIZED_USE: "Sử dụng ghế không đúng",
    LEFT_BELONGINGS: "Để đồ giữ chỗ",
    NOISE: "Gây ồn ào",
    FEET_ON_SEAT: "Gác chân lên ghế/bàn",
    FOOD_DRINK: "Ăn uống trong thư viện",
    SLEEPING: "Ngủ tại chỗ ngồi",
    OTHER: "Khác",
};

const TAB_LIST = [
    { key: "ALL", label: "Tất cả" },
    { key: "PENDING", label: "Chờ xử lý" },
    { key: "VERIFIED", label: "Đã xác minh" },
    { key: "REJECTED", label: "Từ chối" },
];

function ViolationManage() {
    const [searchParams] = useSearchParams();
    const [reports, setReports] = useState([]);
    const [counts, setCounts] = useState({ pending: 0, verified: 0, resolved: 0, rejected: 0 });
    const [activeTab, setActiveTab] = useState(() => {
        const tabParam = searchParams.get("tab");
        if (tabParam && TAB_LIST.some(t => t.key === tabParam)) return tabParam;
        return "ALL";
    });
    const [loading, setLoading] = useState(true);
    const [selectedReport, setSelectedReport] = useState(null);
    const [submitting, setSubmitting] = useState(false);
    const [lightboxImage, setLightboxImage] = useState(null);
    const [searchTerm, setSearchTerm] = useState("");

    const getToken = () =>
        sessionStorage.getItem("librarian_token") || localStorage.getItem("librarian_token");

    const fetchReports = useCallback(async () => {
        setLoading(true);
        try {
            const token = getToken();
            const url = activeTab === "ALL" ? API_BASE : `${API_BASE}?status=${activeTab}`;
            const res = await fetch(url, {
                headers: { Authorization: `Bearer ${token}` },
            });
            if (res.ok) {
                setReports(await res.json());
            }
        } catch (err) {
            console.error("Error fetching violation reports:", err);
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
        fetchReports();
        fetchCounts();
    }, [fetchReports, fetchCounts]);

    const handleVerify = async (id) => {
        setSubmitting(true);
        try {
            const token = getToken();
            const res = await fetch(`${API_BASE}/${id}/verify`, {
                method: "PUT",
                headers: { Authorization: `Bearer ${token}` },
            });
            if (res.ok) {
                fetchReports();
                fetchCounts();
                setSelectedReport(null);
            }
        } catch (err) {
            console.error("Error verifying report:", err);
        } finally {
            setSubmitting(false);
        }
    };

    const handleReject = async (id) => {
        setSubmitting(true);
        try {
            const token = getToken();
            const res = await fetch(`${API_BASE}/${id}/reject`, {
                method: "PUT",
                headers: { Authorization: `Bearer ${token}` },
            });
            if (res.ok) {
                fetchReports();
                fetchCounts();
                setSelectedReport(null);
            }
        } catch (err) {
            console.error("Error rejecting report:", err);
        } finally {
            setSubmitting(false);
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
        const map = { PENDING: "pending", VERIFIED: "confirmed", RESOLVED: "confirmed", REJECTED: "rejected" };
        return map[status] || "pending";
    };

    const filteredReports = reports.filter((r) => {
        if (!searchTerm) return true;
        const term = searchTerm.toLowerCase();
        return (
            (r.reporterName || "").toLowerCase().includes(term) ||
            (r.reporterCode || "").toLowerCase().includes(term) ||
            (r.description || "").toLowerCase().includes(term)
        );
    });

    return (
        <div className="lib-container">
            {/* Page Title + Inline Stats */}
            <div className="lib-page-title">
                <h1>Quản lý vi phạm</h1>
                <div className="lib-inline-stats">
                    <span className="lib-inline-stat">
                        <span className="dot amber"></span>
                        Chờ xử lý <strong>{counts.pending}</strong>
                    </span>
                    <span className="lib-inline-stat">
                        <span className="dot green"></span>
                        Đã xác minh <strong>{counts.verified}</strong>
                    </span>
                    <span className="lib-inline-stat">
                        <span className="dot red"></span>
                        Từ chối <strong>{counts.rejected}</strong>
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
                        placeholder="Tìm kiếm báo cáo..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                    />
                </div>
            </div>

            {/* Report List */}
            {loading ? (
                <div className="lib-loading">
                    <div className="lib-spinner" />
                </div>
            ) : filteredReports.length === 0 ? (
                <div className="lib-empty">
                    <h3>Chưa có báo cáo nào</h3>
                    <p>Các báo cáo vi phạm từ sinh viên sẽ xuất hiện ở đây</p>
                </div>
            ) : (
                <div className="lib-card-list">
                    {filteredReports.map((report) => (
                        <div
                            key={report.id}
                            className="lib-card"
                            onClick={() => setSelectedReport(report)}
                        >
                            <div className="lib-card-header">
                                <div className="lib-user-info">
                                    {report.reporterAvatar ? (
                                        <img
                                            src={report.reporterAvatar}
                                            alt=""
                                            className="lib-avatar"
                                        />
                                    ) : (
                                        <div className="lib-avatar-placeholder">
                                            {getInitial(report.reporterName)}
                                        </div>
                                    )}
                                    <div>
                                        <h3>{report.reporterName}</h3>
                                        <div className="lib-user-code">{report.reporterCode}</div>
                                    </div>
                                </div>
                                <span className={`lib-status-badge ${getStatusClass(report.status)}`}>
                                    {STATUS_LABELS[report.status]}
                                </span>
                            </div>

                            <div className="vr-violation-info">
                                <span className="vr-violation-type">
                                    {VIOLATION_TYPE_LABELS[report.violationType] || report.violationType}
                                </span>
                                <span className="vr-seat-info">
                                    Ghế {report.seatCode}
                                    {report.zoneName && ` - ${report.zoneName}`}
                                    {report.areaName && ` (${report.areaName})`}
                                </span>
                            </div>

                            {report.description && (
                                <div className="lib-description">{report.description}</div>
                            )}

                            {report.evidenceUrl && (
                                <div className="lib-images">
                                    <img
                                        src={report.evidenceUrl}
                                        alt="Bằng chứng"
                                        className="lib-image-thumbnail"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            setLightboxImage(report.evidenceUrl);
                                        }}
                                    />
                                </div>
                            )}

                            <div className="lib-card-footer">
                                <span className="lib-time">{formatTime(report.createdAt)}</span>
                                <div className="vr-actions" onClick={(e) => e.stopPropagation()}>
                                    {report.status === "PENDING" && (
                                        <>
                                            <button
                                                className="lib-btn primary"
                                                onClick={() => handleVerify(report.id)}
                                                disabled={submitting}
                                            >
                                                Xác minh
                                            </button>
                                            <button
                                                className="lib-btn ghost danger"
                                                onClick={() => handleReject(report.id)}
                                                disabled={submitting}
                                            >
                                                Từ chối
                                            </button>
                                        </>
                                    )}
                                    {report.pointDeducted > 0 && (
                                        <span className="lib-points-badge">
                                            -{report.pointDeducted} điểm
                                        </span>
                                    )}
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Slide Panel - Detail */}
            {selectedReport && (
                <>
                    <div className="lib-slide-overlay" onClick={() => setSelectedReport(null)} />
                    <div className="lib-slide-panel">
                        <div className="lib-slide-header">
                            <h2>Chi tiết báo cáo vi phạm</h2>
                            <button
                                className="lib-slide-close"
                                onClick={() => setSelectedReport(null)}
                            >
                                &times;
                            </button>
                        </div>
                        <div className="lib-slide-body">
                            {/* Reporter */}
                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Người báo cáo</div>
                                <div className="lib-user-info">
                                    {selectedReport.reporterAvatar ? (
                                        <img src={selectedReport.reporterAvatar} alt="" className="lib-avatar" />
                                    ) : (
                                        <div className="lib-avatar-placeholder">
                                            {getInitial(selectedReport.reporterName)}
                                        </div>
                                    )}
                                    <div>
                                        <h3>{selectedReport.reporterName}</h3>
                                        <div className="lib-user-code">{selectedReport.reporterCode}</div>
                                    </div>
                                </div>
                            </div>

                            {/* Violator */}
                            {selectedReport.violatorName && (
                                <div className="lib-slide-section">
                                    <div className="lib-slide-label">Người vi phạm</div>
                                    <div className="lib-user-info">
                                        {selectedReport.violatorAvatar ? (
                                            <img src={selectedReport.violatorAvatar} alt="" className="lib-avatar" />
                                        ) : (
                                            <div className="lib-avatar-placeholder">
                                                {getInitial(selectedReport.violatorName)}
                                            </div>
                                        )}
                                        <div>
                                            <h3>{selectedReport.violatorName}</h3>
                                            <div className="lib-user-code">{selectedReport.violatorCode}</div>
                                        </div>
                                    </div>
                                </div>
                            )}

                            {/* Violation Type */}
                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Loại vi phạm</div>
                                <div className="lib-slide-value">
                                    {VIOLATION_TYPE_LABELS[selectedReport.violationType] || selectedReport.violationType}
                                </div>
                            </div>

                            {/* Location */}
                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Vị trí</div>
                                <div className="lib-slide-value">
                                    Ghế {selectedReport.seatCode}
                                    {selectedReport.zoneName && ` - ${selectedReport.zoneName}`}
                                    {selectedReport.areaName && ` (${selectedReport.areaName})`}
                                </div>
                            </div>

                            {/* Status */}
                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Trạng thái</div>
                                <span className={`lib-status-badge ${getStatusClass(selectedReport.status)}`}>
                                    {STATUS_LABELS[selectedReport.status]}
                                </span>
                            </div>

                            {/* Description */}
                            {selectedReport.description && (
                                <div className="lib-slide-section">
                                    <div className="lib-slide-label">Mô tả</div>
                                    <div className="lib-slide-value">{selectedReport.description}</div>
                                </div>
                            )}

                            {/* Evidence */}
                            {selectedReport.evidenceUrl && (
                                <div className="lib-slide-section">
                                    <div className="lib-slide-label">Bằng chứng</div>
                                    <img
                                        src={selectedReport.evidenceUrl}
                                        alt="Bằng chứng"
                                        className="vr-modal-image"
                                        onClick={() => setLightboxImage(selectedReport.evidenceUrl)}
                                    />
                                </div>
                            )}

                            {/* Time */}
                            <div className="lib-slide-section">
                                <div className="lib-slide-label">Thời gian báo cáo</div>
                                <div className="lib-slide-value">{formatTime(selectedReport.createdAt)}</div>
                            </div>

                            {/* Verified By */}
                            {selectedReport.verifiedByName && (
                                <div className="lib-slide-section">
                                    <div className="lib-slide-label">Xử lý bởi</div>
                                    <div className="lib-slide-value">
                                        {selectedReport.verifiedByName} - {formatTime(selectedReport.verifiedAt)}
                                    </div>
                                </div>
                            )}

                            {/* Points Deducted */}
                            {selectedReport.pointDeducted > 0 && (
                                <div className="lib-slide-section">
                                    <div className="lib-slide-label">Điểm trừ</div>
                                    <span className="lib-points-badge" style={{ fontSize: 14, padding: '6px 14px' }}>
                                        -{selectedReport.pointDeducted} điểm
                                    </span>
                                </div>
                            )}
                        </div>

                        <div className="lib-slide-footer">
                            {selectedReport.status === "PENDING" && (
                                <>
                                    <button
                                        className="lib-btn primary"
                                        onClick={() => handleVerify(selectedReport.id)}
                                        disabled={submitting}
                                    >
                                        {submitting ? "Đang xử lý..." : "Xác minh"}
                                    </button>
                                    <button
                                        className="lib-btn ghost danger"
                                        onClick={() => handleReject(selectedReport.id)}
                                        disabled={submitting}
                                    >
                                        {submitting ? "Đang xử lý..." : "Từ chối"}
                                    </button>
                                </>
                            )}
                            <button
                                className="lib-btn ghost"
                                onClick={() => setSelectedReport(null)}
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

export default ViolationManage;