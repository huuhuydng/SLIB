import React, { useState, useEffect, useCallback, useMemo, useRef } from "react";
import { Search, Loader2, ArrowUpDown, ArrowUp, ArrowDown, Filter, X, SlidersHorizontal, LayoutGrid, LayoutList, Trash2, Star, Eye } from "lucide-react";
import "../../../styles/librarian/librarian-shared.css";
import "../../../styles/librarian/CheckInOut.css";
import "../../../styles/librarian/FeedbackManage.css";
import { useToast } from '../../../components/common/ToastProvider';
import { useConfirm } from '../../../components/common/ConfirmDialog';

import { API_BASE_URL } from '../../../config/apiConfig';

const API_BASE = `${API_BASE_URL}/slib/feedbacks`;

const STATUS_LABELS = {
    NEW: "Mới",
    REVIEWED: "Đã xem",
    ACTED: "Đã xử lý",
};

const STATUS_OPTIONS = [
    { value: "", label: "Tất cả" },
    { value: "NEW", label: "Mới" },
    { value: "REVIEWED", label: "Đã xem" },
    { value: "ACTED", label: "Đã xử lý" },
];

const CATEGORY_LABELS = {
    FACILITY: "Cơ sở vật chất",
    SERVICE: "Dịch vụ",
    GENERAL: "Chung",
    MESSAGE: "Tin nhắn hỗ trợ",
};

const CATEGORY_OPTIONS = [
    { value: "", label: "Tất cả" },
    { value: "FACILITY", label: "Cơ sở vật chất" },
    { value: "SERVICE", label: "Dịch vụ" },
    { value: "GENERAL", label: "Chung" },
    { value: "MESSAGE", label: "Tin nhắn hỗ trợ" },
];

function FeedbackManage() {
    const toast = useToast();
    const { confirm } = useConfirm();
    const [feedbacks, setFeedbacks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedFeedback, setSelectedFeedback] = useState(null);
    const [submitting, setSubmitting] = useState(false);
    const [searchTerm, setSearchTerm] = useState("");
    const [erroredAvatars, setErroredAvatars] = useState(new Set());

    // View mode
    const [viewMode, setViewMode] = useState("table");

    // Selection for batch delete
    const [selectedIds, setSelectedIds] = useState(new Set());
    const [deleting, setDeleting] = useState(false);

    // Pagination
    const [currentPage, setCurrentPage] = useState(1);
    const [pageSize] = useState(10);

    // Sort
    const [sortField, setSortField] = useState("createdAt");
    const [sortDir, setSortDir] = useState("desc");

    // Per-column filters
    const [columnFilters, setColumnFilters] = useState({
        student: "",
        rating: "",
        category: "",
        status: "",
        createdAt: "",
    });

    // Column visibility
    const [visibleColumns, setVisibleColumns] = useState({
        student: true,
        rating: true,
        content: true,
        category: true,
        status: true,
        createdAt: true,
    });

    // Dropdown refs
    const [openFilter, setOpenFilter] = useState(null);
    const [showColumnMenu, setShowColumnMenu] = useState(false);
    const filterRef = useRef(null);
    const columnMenuRef = useRef(null);

    const getToken = () =>
        sessionStorage.getItem("librarian_token") || localStorage.getItem("librarian_token");

    const fetchFeedbacks = useCallback(async () => {
        setLoading(true);
        try {
            const token = getToken();
            const res = await fetch(API_BASE, {
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

    // Close dropdowns on outside click
    useEffect(() => {
        const handler = (e) => {
            if (filterRef.current && !filterRef.current.contains(e.target)) setOpenFilter(null);
            if (columnMenuRef.current && !columnMenuRef.current.contains(e.target)) setShowColumnMenu(false);
        };
        document.addEventListener("mousedown", handler);
        return () => document.removeEventListener("mousedown", handler);
    }, []);

    // Actions
    const handleMarkReviewed = async (id) => {
        setSubmitting(true);
        try {
            const token = getToken();
            const res = await fetch(`${API_BASE}/${id}/review`, {
                method: "PUT",
                headers: { Authorization: `Bearer ${token}` },
            });
            if (res.ok) {
                toast.success('Đã đánh dấu phản hồi là đã xem');
                fetchFeedbacks();
                setSelectedFeedback(null);
            } else {
                toast.error('Không thể đánh dấu phản hồi. Vui lòng thử lại.');
            }
        } catch (err) {
            console.error("Lỗi đánh dấu đã xem:", err);
            toast.error('Lỗi: ' + err.message);
        } finally {
            setSubmitting(false);
        }
    };

    const handleBatchDelete = async () => {
        if (selectedIds.size === 0) return;
        const confirmed = await confirm({
            title: 'Xoá phản hồi',
            message: `Xoá ${selectedIds.size} phản hồi đã chọn?`,
            variant: 'danger',
            confirmText: 'Xoá',
            cancelText: 'Huỷ',
        });
        if (!confirmed) return;
        setDeleting(true);
        try {
            const token = getToken();
            const res = await fetch(`${API_BASE}/batch`, {
                method: "DELETE",
                headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
                body: JSON.stringify({ ids: [...selectedIds] }),
            });
            if (res.ok) {
                toast.success(`Đã xoá ${selectedIds.size} phản hồi thành công`);
                setSelectedIds(new Set());
                fetchFeedbacks();
            } else {
                toast.error('Không thể xoá phản hồi. Vui lòng thử lại.');
            }
        } catch (err) {
            console.error("Lỗi xoá hàng loạt:", err);
            toast.error('Lỗi: ' + err.message);
        } finally {
            setDeleting(false);
        }
    };

    // Helpers
    const formatDate = (iso) => {
        if (!iso) return "";
        const d = new Date(iso);
        return `${d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })} ${d.toLocaleDateString("vi-VN")}`;
    };

    const getInitial = (name) => (name ? name.charAt(0).toUpperCase() : "?");

    const renderAvatar = (avatarUrl, name, fallbackName) => {
        const displayName = name || fallbackName || "Chưa xác định";
        if (avatarUrl && !erroredAvatars.has(avatarUrl)) {
            return (
                <img
                    src={avatarUrl}
                    alt=""
                    className="sr-avatar"
                    onError={() => setErroredAvatars(prev => new Set(prev).add(avatarUrl))}
                />
            );
        }
        return <div className="sr-avatar-placeholder">{getInitial(displayName)}</div>;
    };

    const getStatusDot = (status) => {
        switch (status) {
            case "NEW": return "#f59e0b";
            case "REVIEWED": return "#3b82f6";
            case "ACTED": return "#22c55e";
            default: return "#94a3b8";
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

    const getFeedbackValue = (fb, field) => {
        switch (field) {
            case "student": return fb.studentName || "";
            case "rating": return fb.rating || 0;
            case "content": return fb.content || "";
            case "category": return fb.category || "";
            case "status": return STATUS_LABELS[fb.status] || fb.status;
            case "createdAt": return fb.createdAt || "";
            default: return "";
        }
    };

    // Filtered + sorted
    const processedFeedbacks = useMemo(() => {
        let list = [...feedbacks];
        // Search
        const q = searchTerm.trim().toLowerCase();
        if (q) {
            list = list.filter(f =>
                (f.studentName || "").toLowerCase().includes(q) ||
                (f.studentCode || "").toLowerCase().includes(q) ||
                (f.content || "").toLowerCase().includes(q) ||
                (f.category || "").toLowerCase().includes(q)
            );
        }
        // Column filters
        Object.entries(columnFilters).forEach(([col, val]) => {
            if (!val) return;
            const v = val.toLowerCase();
            list = list.filter(f => {
                if (col === "student") {
                    return (f.studentName || "").toLowerCase().includes(v) ||
                        (f.studentCode || "").toLowerCase().includes(v);
                }
                if (col === "rating") {
                    return String(f.rating || 0).includes(v);
                }
                if (col === "category") {
                    return (f.category || "") === val;
                }
                if (col === "status") {
                    return (f.status || "") === val;
                }
                return String(getFeedbackValue(f, col)).toLowerCase().includes(v);
            });
        });
        // Sort
        list.sort((a, b) => {
            let av = getFeedbackValue(a, sortField);
            let bv = getFeedbackValue(b, sortField);
            if (sortField === "createdAt") {
                av = new Date(av || 0).getTime();
                bv = new Date(bv || 0).getTime();
            }
            if (typeof av === "string") av = av.toLowerCase();
            if (typeof bv === "string") bv = bv.toLowerCase();
            if (av < bv) return sortDir === "asc" ? -1 : 1;
            if (av > bv) return sortDir === "asc" ? 1 : -1;
            return 0;
        });
        return list;
    }, [feedbacks, searchTerm, columnFilters, sortField, sortDir]);

    // Pagination
    const totalPages = Math.ceil(processedFeedbacks.length / pageSize) || 1;
    const pagedFeedbacks = useMemo(() => {
        const start = (currentPage - 1) * pageSize;
        return processedFeedbacks.slice(start, start + pageSize);
    }, [processedFeedbacks, currentPage, pageSize]);

    useEffect(() => { setCurrentPage(1); }, [searchTerm, columnFilters, sortField, sortDir]);

    // Selection
    const toggleSelect = (id) => {
        setSelectedIds(prev => {
            const next = new Set(prev);
            next.has(id) ? next.delete(id) : next.add(id);
            return next;
        });
    };
    const toggleSelectAll = () => {
        const ids = pagedFeedbacks.map(f => f.id);
        const allSelected = ids.every(id => selectedIds.has(id));
        setSelectedIds(prev => {
            const next = new Set(prev);
            ids.forEach(id => allSelected ? next.delete(id) : next.add(id));
            return next;
        });
    };

    // Sort handler
    const handleSort = (field) => {
        if (sortField === field) {
            setSortDir(d => d === "asc" ? "desc" : "asc");
        } else {
            setSortField(field);
            setSortDir("asc");
        }
    };

    const SortIcon = ({ field }) => {
        if (sortField !== field) return <ArrowUpDown size={13} style={{ opacity: 0.3 }} />;
        return sortDir === "asc" ? <ArrowUp size={13} /> : <ArrowDown size={13} />;
    };

    // Column defs
    const columns = [
        { key: "student", label: "Sinh viên" },
        { key: "rating", label: "Đánh giá" },
        { key: "content", label: "Nội dung" },
        { key: "category", label: "Danh mục" },
        { key: "status", label: "Trạng thái" },
        { key: "createdAt", label: "Thời gian" },
    ];

    return (
        <div className="lib-container">
            <div className="lib-page-title">
                <h1>PHẢN HỒI SINH VIÊN</h1>
            </div>

            <div className="lib-panel">
                {/* Toolbar */}
                <div className="sr-toolbar">
                    <div className="lib-search">
                        <Search size={16} className="lib-search-icon" />
                        <input
                            type="text"
                            placeholder="Tìm tên, mã SV, nội dung..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>

                    <div className="sr-view-toggle">
                        <button className={`sr-view-btn${viewMode === "table" ? " active" : ""}`} onClick={() => setViewMode("table")} title="Dạng bảng">
                            <LayoutList size={16} />
                        </button>
                        <button className={`sr-view-btn${viewMode === "card" ? " active" : ""}`} onClick={() => setViewMode("card")} title="Dạng thẻ">
                            <LayoutGrid size={16} />
                        </button>
                    </div>

                    {viewMode === "table" && (
                        <div style={{ position: "relative" }} ref={columnMenuRef}>
                            <button className="cio-column-toggle" onClick={() => setShowColumnMenu(!showColumnMenu)}>
                                <SlidersHorizontal size={15} /> Hiển thị cột
                            </button>
                            {showColumnMenu && (
                                <div className="cio-column-menu">
                                    {columns.map(col => (
                                        <label key={col.key} className="cio-column-item">
                                            <input
                                                type="checkbox"
                                                checked={visibleColumns[col.key]}
                                                onChange={() => setVisibleColumns(prev => ({ ...prev, [col.key]: !prev[col.key] }))}
                                            />
                                            {col.label}
                                        </label>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}

                    {selectedIds.size > 0 && (
                        <button className="sr-delete-btn" onClick={handleBatchDelete} disabled={deleting}>
                            <Trash2 size={14} /> Xoá {selectedIds.size}
                        </button>
                    )}

                    <span className="sr-result-count">
                        Tổng số <strong>{processedFeedbacks.length}</strong> kết quả
                    </span>
                </div>

                {/* Content */}
                {loading ? (
                    <div className="lib-loading"><div className="lib-spinner" /></div>
                ) : processedFeedbacks.length === 0 ? (
                    <div className="lib-empty">
                        <h3>Chưa có phản hồi nào</h3>
                        <p>Phản hồi từ sinh viên sau khi rời thư viện sẽ xuất hiện ở đây</p>
                    </div>
                ) : viewMode === "table" ? (
                    /* TABLE VIEW */
                    <>
                        <div className="sr-table-wrapper">
                            <table className="sr-table">
                                <thead>
                                    <tr>
                                        <th className="sr-checkbox-col">
                                            <input type="checkbox"
                                                checked={pagedFeedbacks.length > 0 && pagedFeedbacks.every(f => selectedIds.has(f.id))}
                                                onChange={toggleSelectAll}
                                            />
                                        </th>
                                        {columns.filter(c => visibleColumns[c.key]).map(col => (
                                            <th key={col.key}>
                                                <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                                                    <span style={{ cursor: "pointer" }} onClick={() => handleSort(col.key)}>
                                                        {col.label}
                                                    </span>
                                                    <span style={{ cursor: "pointer" }} onClick={() => handleSort(col.key)}>
                                                        <SortIcon field={col.key} />
                                                    </span>
                                                    {col.key !== "content" && (
                                                        <span style={{ cursor: "pointer", position: "relative" }} ref={openFilter === col.key ? filterRef : null}>
                                                            <Filter
                                                                size={13}
                                                                style={{ opacity: columnFilters[col.key] ? 1 : 0.3 }}
                                                                onClick={() => setOpenFilter(openFilter === col.key ? null : col.key)}
                                                            />
                                                            {openFilter === col.key && (
                                                                <div className="cio-filter-dropdown" style={{ position: "absolute", top: "100%", left: 0, zIndex: 10 }}>
                                                                    {col.key === "status" ? (
                                                                        <select
                                                                            value={columnFilters.status}
                                                                            onChange={(e) => setColumnFilters(prev => ({ ...prev, status: e.target.value }))}
                                                                            autoFocus
                                                                        >
                                                                            {STATUS_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                                                                        </select>
                                                                    ) : col.key === "category" ? (
                                                                        <select
                                                                            value={columnFilters.category || ""}
                                                                            onChange={(e) => setColumnFilters(prev => ({ ...prev, category: e.target.value }))}
                                                                            autoFocus
                                                                        >
                                                                            {CATEGORY_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                                                                        </select>
                                                                    ) : (
                                                                        <input
                                                                            type="text"
                                                                            placeholder={`Lọc ${col.label.toLowerCase()}...`}
                                                                            value={columnFilters[col.key] || ""}
                                                                            onChange={(e) => setColumnFilters(prev => ({ ...prev, [col.key]: e.target.value }))}
                                                                            autoFocus
                                                                        />
                                                                    )}
                                                                    {columnFilters[col.key] && (
                                                                        <button onClick={() => setColumnFilters(prev => ({ ...prev, [col.key]: "" }))}>
                                                                            <X size={12} /> Xoá
                                                                        </button>
                                                                    )}
                                                                </div>
                                                            )}
                                                        </span>
                                                    )}
                                                </div>
                                            </th>
                                        ))}
                                    </tr>
                                </thead>
                                <tbody>
                                    {pagedFeedbacks.length === 0 ? (
                                        <tr><td colSpan={columns.length + 1} className="sr-table-empty-cell">Không tìm thấy phản hồi nào</td></tr>
                                    ) : pagedFeedbacks.map(fb => (
                                        <tr key={fb.id} className={`sr-table-row${selectedIds.has(fb.id) ? " selected" : ""}`} onClick={() => setSelectedFeedback(fb)}>
                                            <td className="sr-checkbox-col" onClick={e => e.stopPropagation()}>
                                                <input type="checkbox" checked={selectedIds.has(fb.id)} onChange={() => toggleSelect(fb.id)} />
                                            </td>

                                            {visibleColumns.student && (
                                                <td>
                                                    <div className="sr-student-cell">
                                                        {renderAvatar(fb.studentAvatar, fb.studentName)}
                                                        <div>
                                                            <div className="sr-student-name">{fb.studentName || "Sinh viên"}</div>
                                                            <div className="sr-student-code">{fb.studentCode}</div>
                                                        </div>
                                                    </div>
                                                </td>
                                            )}
                                            {visibleColumns.rating && (
                                                <td>{renderStars(fb.rating)}</td>
                                            )}
                                            {visibleColumns.content && (
                                                <td><div className="sr-desc-cell">{fb.content || "-"}</div></td>
                                            )}
                                            {visibleColumns.category && (
                                                <td>
                                                    {fb.category ? (
                                                        <span className="fm-seat-tag">{CATEGORY_LABELS[fb.category] || fb.category}</span>
                                                    ) : "-"}
                                                </td>
                                            )}
                                            {visibleColumns.status && (
                                                <td>
                                                    <span className="sr-status-text">
                                                        <span className="sr-status-dot" style={{ background: getStatusDot(fb.status) }} />
                                                        {STATUS_LABELS[fb.status] || fb.status}
                                                    </span>
                                                </td>
                                            )}
                                            {visibleColumns.createdAt && (
                                                <td className="sr-date-cell">{formatDate(fb.createdAt)}</td>
                                            )}
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>

                        {/* Pagination */}
                        {totalPages > 1 && (
                            <div className="cio-pagination">
                                <button disabled={currentPage === 1} onClick={() => setCurrentPage(p => p - 1)}>&laquo;</button>
                                {Array.from({ length: totalPages }, (_, i) => (
                                    <button key={i + 1} className={currentPage === i + 1 ? "active" : ""} onClick={() => setCurrentPage(i + 1)}>
                                        {i + 1}
                                    </button>
                                ))}
                                <button disabled={currentPage === totalPages} onClick={() => setCurrentPage(p => p + 1)}>&raquo;</button>
                            </div>
                        )}
                    </>
                ) : (
                    /* CARD VIEW */
                    <div className="sr-card-grid">
                        {pagedFeedbacks.map(fb => (
                            <div key={fb.id} className={`sr-card${selectedIds.has(fb.id) ? " selected" : ""}`} onClick={() => setSelectedFeedback(fb)}>
                                <div className="sr-card-check" onClick={e => e.stopPropagation()}>
                                    <input type="checkbox" checked={selectedIds.has(fb.id)} onChange={() => toggleSelect(fb.id)} />
                                </div>

                                <div className="sr-card-header">
                                    <div className="sr-student-cell">
                                        {renderAvatar(fb.studentAvatar, fb.studentName)}
                                        <div>
                                            <div className="sr-student-name">{fb.studentName || "Sinh viên"}</div>
                                            <div className="sr-student-code">{fb.studentCode}</div>
                                        </div>
                                    </div>
                                    <span className="sr-status-text">
                                        <span className="sr-status-dot" style={{ background: getStatusDot(fb.status) }} />
                                        {STATUS_LABELS[fb.status] || fb.status}
                                    </span>
                                </div>

                                <div className="fm-feedback-content">
                                    {renderStars(fb.rating)}
                                    {fb.content && <div className="sr-card-desc">{fb.content}</div>}
                                </div>

                                {fb.category && (
                                    <div style={{ marginTop: 6 }}>
                                        <span className="fm-seat-tag">{CATEGORY_LABELS[fb.category] || fb.category}</span>
                                    </div>
                                )}

                                <div className="sr-card-footer">
                                    <span className="sr-card-time">{formatDate(fb.createdAt)}</span>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {/* MODAL */}
            {selectedFeedback && (
                <div className="sr-modal-overlay" onClick={() => setSelectedFeedback(null)}>
                    <div className="sr-modal" onClick={e => e.stopPropagation()}>
                        <div className="sr-modal-header">
                            <h2>Chi tiết phản hồi</h2>
                            <button className="sr-modal-close" onClick={() => setSelectedFeedback(null)}>&times;</button>
                        </div>
                        <div className="sr-modal-body">
                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Sinh viên</div>
                                <div className="sr-student-cell">
                                    {renderAvatar(selectedFeedback.studentAvatar, selectedFeedback.studentName)}
                                    <div>
                                        <div className="sr-student-name">{selectedFeedback.studentName || "Sinh viên"}</div>
                                        <div className="sr-student-code">{selectedFeedback.studentCode}</div>
                                    </div>
                                </div>
                            </div>

                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Đánh giá</div>
                                <div>{renderStars(selectedFeedback.rating)}</div>
                            </div>

                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Trạng thái</div>
                                <div className="sr-modal-value">
                                    <span className="sr-status-text">
                                        <span className="sr-status-dot" style={{ background: getStatusDot(selectedFeedback.status) }} />
                                        {STATUS_LABELS[selectedFeedback.status] || selectedFeedback.status}
                                    </span>
                                </div>
                            </div>

                            {selectedFeedback.content && (
                                <div className="sr-modal-section">
                                    <div className="sr-modal-label">Nội dung</div>
                                    <div className="sr-modal-value">{selectedFeedback.content}</div>
                                </div>
                            )}

                            {selectedFeedback.category && (
                                <div className="sr-modal-section">
                                    <div className="sr-modal-label">Danh mục</div>
                                    <div className="sr-modal-value">{CATEGORY_LABELS[selectedFeedback.category] || selectedFeedback.category}</div>
                                </div>
                            )}

                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Thời gian</div>
                                <div className="sr-modal-value">{formatDate(selectedFeedback.createdAt)}</div>
                            </div>

                            {selectedFeedback.reviewedByName && (
                                <div className="sr-modal-section">
                                    <div className="sr-modal-label">Xem bởi</div>
                                    <div className="sr-modal-value">
                                        {selectedFeedback.reviewedByName} — {formatDate(selectedFeedback.reviewedAt)}
                                    </div>
                                </div>
                            )}
                        </div>
                        <div className="sr-modal-footer">
                            {selectedFeedback.status === "NEW" && (
                                <button className="sr-modal-btn primary" onClick={() => handleMarkReviewed(selectedFeedback.id)} disabled={submitting}>
                                    {submitting ? "Đang xử lý..." : "Đánh dấu đã xem"}
                                </button>
                            )}
                            <button className="sr-modal-btn ghost" onClick={() => setSelectedFeedback(null)}>Đóng</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default FeedbackManage;
