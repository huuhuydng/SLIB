import React, { useState, useEffect, useCallback } from "react";
import "../../../styles/librarian/ViolationManage.css";

const API_BASE = `${import.meta.env.VITE_API_URL || "http://localhost:8080"}/slib/violation-reports`;

const STATUS_LABELS = {
    PENDING: "Chờ xử lý",
    VERIFIED: "Đã xác minh",
    RESOLVED: "Đã xử lý",
    REJECTED: "Từ chối",
};

const STATUS_COLORS = {
    PENDING: "#f59e0b",
    VERIFIED: "#16a34a",
    RESOLVED: "#3b82f6",
    REJECTED: "#dc2626",
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
    const [reports, setReports] = useState([]);
    const [counts, setCounts] = useState({ pending: 0, verified: 0, resolved: 0, rejected: 0 });
    const [activeTab, setActiveTab] = useState("ALL");
    const [loading, setLoading] = useState(true);
    const [selectedReport, setSelectedReport] = useState(null);
    const [submitting, setSubmitting] = useState(false);
    const [lightboxImage, setLightboxImage] = useState(null);

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

    return (
        <div className="vr-container">
            {/* Header */}
            <div className="vr-header">
                <h1>Quản lý báo cáo vi phạm</h1>
                <div className="vr-stats">
                    <span className="vr-stat-badge pending">
                        Chờ xử lý: {counts.pending}
                    </span>
                    <span className="vr-stat-badge verified">
                        Đã xác minh: {counts.verified}
                    </span>
                    <span className="vr-stat-badge rejected">
                        Từ chối: {counts.rejected}
                    </span>
                </div>
            </div>

            {/* Tabs */}
            <div className="vr-tabs">
                {TAB_LIST.map((tab) => (
                    <button
                        key={tab.key}
                        className={`vr-tab ${activeTab === tab.key ? "active" : ""}`}
                        onClick={() => setActiveTab(tab.key)}
                    >
                        {tab.label}
                        {tab.key === "PENDING" && counts.pending > 0 && (
                            <span className="vr-tab-count">{counts.pending}</span>
                        )}
                    </button>
                ))}
            </div>

            {/* Report List */}
            {loading ? (
                <div className="vr-loading">
                    <div className="vr-spinner" />
                </div>
            ) : reports.length === 0 ? (
                <div className="vr-empty">
                    <div className="vr-empty-icon">&#128466;</div>
                    <h3>Chưa có báo cáo nào</h3>
                    <p>Các báo cáo vi phạm từ sinh viên sẽ xuất hiện ở đây</p>
                </div>
            ) : (
                <div className="vr-list">
                    {reports.map((report) => (
                        <div
                            key={report.id}
                            className="vr-card"
                            onClick={() => setSelectedReport(report)}
                        >
                            <div className="vr-card-header">
                                <div className="vr-reporter-info">
                                    {report.reporterAvatar ? (
                                        <img
                                            src={report.reporterAvatar}
                                            alt=""
                                            className="vr-avatar"
                                        />
                                    ) : (
                                        <div className="vr-avatar-placeholder">
                                            {getInitial(report.reporterName)}
                                        </div>
                                    )}
                                    <div className="vr-reporter-details">
                                        <h3>{report.reporterName}</h3>
                                        <div className="vr-reporter-code">{report.reporterCode}</div>
                                    </div>
                                </div>
                                <span
                                    className={`vr-status-badge ${report.status.toLowerCase()}`}
                                >
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
                                <div className="vr-description">{report.description}</div>
                            )}

                            {report.evidenceUrl && (
                                <div className="vr-images">
                                    <img
                                        src={report.evidenceUrl}
                                        alt="Bằng chứng"
                                        className="vr-image-thumbnail"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            setLightboxImage(report.evidenceUrl);
                                        }}
                                    />
                                </div>
                            )}

                            <div className="vr-card-footer">
                                <span className="vr-time">{formatTime(report.createdAt)}</span>
                                <div className="vr-actions" onClick={(e) => e.stopPropagation()}>
                                    {report.status === "PENDING" && (
                                        <>
                                            <button
                                                className="vr-btn success"
                                                onClick={() => handleVerify(report.id)}
                                                disabled={submitting}
                                            >
                                                Xác minh
                                            </button>
                                            <button
                                                className="vr-btn danger"
                                                onClick={() => handleReject(report.id)}
                                                disabled={submitting}
                                            >
                                                Từ chối
                                            </button>
                                        </>
                                    )}
                                    {report.pointDeducted > 0 && (
                                        <span className="vr-points-badge">
                                            -{report.pointDeducted} điểm
                                        </span>
                                    )}
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Detail Modal */}
            {selectedReport && (
                <div className="vr-modal-overlay" onClick={() => setSelectedReport(null)}>
                    <div className="vr-modal" onClick={(e) => e.stopPropagation()}>
                        <div className="vr-modal-header">
                            <h2>Chi tiết báo cáo vi phạm</h2>
                            <button
                                className="vr-modal-close"
                                onClick={() => setSelectedReport(null)}
                            >
                                &times;
                            </button>
                        </div>
                        <div className="vr-modal-body">
                            {/* Reporter Info */}
                            <div className="vr-modal-section">
                                <div className="vr-modal-label">Người báo cáo</div>
                                <div className="vr-modal-student-info">
                                    {selectedReport.reporterAvatar ? (
                                        <img
                                            src={selectedReport.reporterAvatar}
                                            alt=""
                                            className="vr-avatar"
                                        />
                                    ) : (
                                        <div className="vr-avatar-placeholder">
                                            {getInitial(selectedReport.reporterName)}
                                        </div>
                                    )}
                                    <div>
                                        <strong>{selectedReport.reporterName}</strong>
                                        <div style={{ fontSize: 13, color: "#666" }}>
                                            {selectedReport.reporterCode}
                                        </div>
                                    </div>
                                </div>
                            </div>

                            {/* Violator Info */}
                            {selectedReport.violatorName && (
                                <div className="vr-modal-section">
                                    <div className="vr-modal-label">Người vi phạm (tự động nhận diện)</div>
                                    <div className="vr-modal-student-info">
                                        {selectedReport.violatorAvatar ? (
                                            <img
                                                src={selectedReport.violatorAvatar}
                                                alt=""
                                                className="vr-avatar"
                                            />
                                        ) : (
                                            <div className="vr-avatar-placeholder">
                                                {getInitial(selectedReport.violatorName)}
                                            </div>
                                        )}
                                        <div>
                                            <strong>{selectedReport.violatorName}</strong>
                                            <div style={{ fontSize: 13, color: "#666" }}>
                                                {selectedReport.violatorCode}
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            )}

                            {/* Violation Details */}
                            <div className="vr-modal-section">
                                <div className="vr-modal-label">Loại vi phạm</div>
                                <div className="vr-modal-text">
                                    {VIOLATION_TYPE_LABELS[selectedReport.violationType] || selectedReport.violationType}
                                </div>
                            </div>

                            <div className="vr-modal-section">
                                <div className="vr-modal-label">Vị trí</div>
                                <div className="vr-modal-text">
                                    Ghế {selectedReport.seatCode}
                                    {selectedReport.zoneName && ` - ${selectedReport.zoneName}`}
                                    {selectedReport.areaName && ` (${selectedReport.areaName})`}
                                </div>
                            </div>

                            {/* Status */}
                            <div className="vr-modal-section">
                                <div className="vr-modal-label">Trạng thái</div>
                                <span
                                    className={`vr-status-badge ${selectedReport.status.toLowerCase()}`}
                                >
                                    {STATUS_LABELS[selectedReport.status]}
                                </span>
                            </div>

                            {/* Description */}
                            {selectedReport.description && (
                                <div className="vr-modal-section">
                                    <div className="vr-modal-label">Mô tả</div>
                                    <div className="vr-modal-text">{selectedReport.description}</div>
                                </div>
                            )}

                            {/* Evidence */}
                            {selectedReport.evidenceUrl && (
                                <div className="vr-modal-section">
                                    <div className="vr-modal-label">Bằng chứng</div>
                                    <img
                                        src={selectedReport.evidenceUrl}
                                        alt="Bằng chứng"
                                        className="vr-modal-image"
                                        onClick={() => setLightboxImage(selectedReport.evidenceUrl)}
                                    />
                                </div>
                            )}

                            {/* Time */}
                            <div className="vr-modal-section">
                                <div className="vr-modal-label">Thời gian báo cáo</div>
                                <div className="vr-modal-text">{formatTime(selectedReport.createdAt)}</div>
                            </div>

                            {/* Verification Info */}
                            {selectedReport.verifiedByName && (
                                <div className="vr-modal-section">
                                    <div className="vr-modal-label">Xử lý bởi</div>
                                    <div className="vr-modal-text">
                                        {selectedReport.verifiedByName} - {formatTime(selectedReport.verifiedAt)}
                                    </div>
                                </div>
                            )}

                            {/* Points Deducted */}
                            {selectedReport.pointDeducted > 0 && (
                                <div className="vr-modal-section">
                                    <div className="vr-modal-label">Điểm trừ</div>
                                    <span className="vr-points-badge-large">
                                        -{selectedReport.pointDeducted} điểm
                                    </span>
                                </div>
                            )}
                        </div>

                        <div className="vr-modal-footer">
                            {selectedReport.status === "PENDING" && (
                                <>
                                    <button
                                        className="vr-btn success"
                                        onClick={() => handleVerify(selectedReport.id)}
                                        disabled={submitting}
                                    >
                                        {submitting ? "Đang xử lý..." : "Xác minh & Trừ điểm"}
                                    </button>
                                    <button
                                        className="vr-btn danger"
                                        onClick={() => handleReject(selectedReport.id)}
                                        disabled={submitting}
                                    >
                                        {submitting ? "Đang xử lý..." : "Từ chối"}
                                    </button>
                                </>
                            )}
                            <button
                                className="vr-btn secondary"
                                onClick={() => setSelectedReport(null)}
                            >
                                Đóng
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Lightbox */}
            {lightboxImage && (
                <div className="vr-lightbox" onClick={() => setLightboxImage(null)}>
                    <img src={lightboxImage} alt="" />
                </div>
            )}
        </div>
    );
}

export default ViolationManage;