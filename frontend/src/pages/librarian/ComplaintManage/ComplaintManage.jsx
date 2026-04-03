import React, { useState, useEffect, useCallback, useMemo, useRef } from "react";
import { Search, Loader2, ArrowUpDown, ArrowUp, ArrowDown, Filter, X, SlidersHorizontal, LayoutGrid, LayoutList, Trash2 } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import "../../../styles/librarian/librarian-shared.css";
import "../../../styles/librarian/CheckInOut.css";
import "../../../styles/librarian/ComplaintManage.css";
import { useToast } from '../../../components/common/ToastProvider';
import { useConfirm } from '../../../components/common/ConfirmDialog';

import { API_BASE_URL } from '../../../config/apiConfig';

const API_BASE = `${API_BASE_URL}/slib/complaints`;

const STATUS_LABELS = {
    PENDING: "Chờ xử lý",
    ACCEPTED: "Chấp nhận",
    DENIED: "Từ chối",
};

const STATUS_OPTIONS = [
    { value: "", label: "Tất cả" },
    { value: "PENDING", label: "Chờ xử lý" },
    { value: "ACCEPTED", label: "Chấp nhận" },
    { value: "DENIED", label: "Từ chối" },
];

function ComplaintManage() {
    const toast = useToast();
    const { confirm } = useConfirm();
    const [searchParams, setSearchParams] = useSearchParams();
    const [complaints, setComplaints] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedComplaint, setSelectedComplaint] = useState(null);
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
        subject: "",
        status: "",
        createdAt: "",
    });

    // Column visibility
    const [visibleColumns, setVisibleColumns] = useState({
        student: true,
        subject: true,
        content: true,
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

    const fetchComplaints = useCallback(async () => {
        setLoading(true);
        try {
            const token = getToken();
            const res = await fetch(API_BASE, {
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

    // Auto-open detail modal from URL param (e.g. ?detail=<id>)
    useEffect(() => {
        if (loading || complaints.length === 0) return;
        const detailId = searchParams.get("detail");
        if (detailId) {
            const target = complaints.find((c) => String(c.id) === detailId);
            if (target) {
                setSelectedComplaint(target);
            }
            const nextParams = new URLSearchParams(searchParams);
            nextParams.delete("detail");
            setSearchParams(nextParams, { replace: true });
        }
    }, [loading, complaints, searchParams, setSearchParams]);

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
    const handleAccept = async (id) => {
        setSubmitting(true);
        try {
            const token = getToken();
            const res = await fetch(`${API_BASE}/${id}/accept`, {
                method: "PUT",
                headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
                body: JSON.stringify({ note: "Chấp nhận khiếu nại" }),
            });
            if (res.ok) {
                toast.success('Đã chấp nhận khiếu nại thành công');
                fetchComplaints();
                setSelectedComplaint(null);
            } else {
                toast.error('Không thể chấp nhận khiếu nại. Vui lòng thử lại');
            }
        } catch (err) {
            console.error("Lỗi chấp nhận khiếu nại:", err);
            toast.error('Lỗi: ' + err.message);
        } finally {
            setSubmitting(false);
        }
    };

    const handleDeny = async (id) => {
        setSubmitting(true);
        try {
            const token = getToken();
            const res = await fetch(`${API_BASE}/${id}/deny`, {
                method: "PUT",
                headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
                body: JSON.stringify({ note: "Từ chối khiếu nại" }),
            });
            if (res.ok) {
                toast.success('Đã từ chối khiếu nại');
                fetchComplaints();
                setSelectedComplaint(null);
            } else {
                toast.error('Không thể từ chối khiếu nại. Vui lòng thử lại');
            }
        } catch (err) {
            console.error("Lỗi từ chối khiếu nại:", err);
            toast.error('Lỗi: ' + err.message);
        } finally {
            setSubmitting(false);
        }
    };

    const handleBatchDelete = async () => {
        if (selectedIds.size === 0) return;
        const confirmed = await confirm({
            title: 'Xoá khiếu nại',
            message: `Xoá ${selectedIds.size} khiếu nại đã chọn?`,
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
                toast.success(`Đã xoá ${selectedIds.size} khiếu nại thành công`);
                setSelectedIds(new Set());
                fetchComplaints();
            } else {
                toast.error('Không thể xoá khiếu nại. Vui lòng thử lại');
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
            case "PENDING": return "#f59e0b";
            case "ACCEPTED": return "#22c55e";
            case "DENIED": return "#ef4444";
            default: return "#94a3b8";
        }
    };

    const renderStatus = (status) => {
        const tone = status?.toLowerCase() || "unknown";
        return (
            <span className={`sr-status-text sr-status-text--${tone}`}>
                <span className="sr-status-dot" style={{ background: getStatusDot(status) }} />
                {STATUS_LABELS[status] || status}
            </span>
        );
    };

    const getComplaintValue = (complaint, field) => {
        switch (field) {
            case "student": return complaint.studentName || "";
            case "subject": return complaint.subject || "";
            case "content": return complaint.content || "";
            case "status": return STATUS_LABELS[complaint.status] || complaint.status;
            case "createdAt": return complaint.createdAt || "";
            default: return "";
        }
    };

    // Filtered + sorted data
    const processedComplaints = useMemo(() => {
        let list = [...complaints];
        // Search
        const q = searchTerm.trim().toLowerCase();
        if (q) {
            list = list.filter(c =>
                (c.studentName || "").toLowerCase().includes(q) ||
                (c.studentCode || "").toLowerCase().includes(q) ||
                (c.subject || "").toLowerCase().includes(q) ||
                (c.content || "").toLowerCase().includes(q)
            );
        }
        // Column filters
        Object.entries(columnFilters).forEach(([col, val]) => {
            if (!val) return;
            const v = val.toLowerCase();
            list = list.filter(c => {
                if (col === "student") {
                    return (c.studentName || "").toLowerCase().includes(v) ||
                        (c.studentCode || "").toLowerCase().includes(v);
                }
                return String(getComplaintValue(c, col)).toLowerCase().includes(v);
            });
        });
        // Sort
        list.sort((a, b) => {
            let av = getComplaintValue(a, sortField);
            let bv = getComplaintValue(b, sortField);
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
    }, [complaints, searchTerm, columnFilters, sortField, sortDir]);

    // Pagination
    const totalPages = Math.ceil(processedComplaints.length / pageSize) || 1;
    const pagedComplaints = useMemo(() => {
        const start = (currentPage - 1) * pageSize;
        return processedComplaints.slice(start, start + pageSize);
    }, [processedComplaints, currentPage, pageSize]);

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
        const ids = pagedComplaints.map(c => c.id);
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

    const handleFilterChange = (field, value) => {
        setColumnFilters(prev => ({ ...prev, [field]: value }));
    };

    const clearColumnFilter = (field) => {
        setColumnFilters(prev => ({ ...prev, [field]: "" }));
        setOpenFilter(null);
    };

    const renderColumnHeader = (column, label) => {
        const hasFilter = !!columnFilters[column];

        return (
            <th key={column}>
                <div className="cio-th-content">
                    <span className="cio-th-label">{label}</span>
                    <div className="cio-th-actions">
                        <button
                            className={`cio-th-btn${sortField === column ? " active" : ""}`}
                            onClick={(e) => { e.stopPropagation(); handleSort(column); }}
                            title="Sắp xếp"
                        >
                            <SortIcon field={column} />
                        </button>
                        {column !== "content" && (
                            <button
                                className={`cio-th-btn${hasFilter ? " active" : ""}${openFilter === column ? " open" : ""}`}
                                onClick={(e) => {
                                    e.stopPropagation();
                                    setOpenFilter(openFilter === column ? null : column);
                                }}
                                title="Lọc"
                            >
                                <Filter size={13} className={hasFilter ? "cio-filter-active" : ""} />
                            </button>
                        )}
                    </div>
                    {openFilter === column && column !== "content" && (
                        <div className="cio-filter-dropdown" ref={filterRef} onClick={(e) => e.stopPropagation()}>
                            {column === "status" ? (
                                <select
                                    value={columnFilters.status}
                                    onChange={(e) => { handleFilterChange("status", e.target.value); setOpenFilter(null); }}
                                    autoFocus
                                    className="cio-filter-input"
                                >
                                    {STATUS_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                                </select>
                            ) : (
                                <>
                                    <input
                                        type="text"
                                        className="cio-filter-input"
                                        placeholder={`Lọc ${label.toLowerCase()}...`}
                                        value={columnFilters[column] || ""}
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

    // Column defs
    const columns = [
        { key: "student", label: "Sinh viên" },
        { key: "subject", label: "Tiêu đề" },
        { key: "content", label: "Nội dung" },
        { key: "status", label: "Trạng thái" },
        { key: "createdAt", label: "Thời gian" },
    ];

    return (
        <div className="lib-container">
            <div className="lib-page-title">
                <h1>KHIẾU NẠI</h1>
            </div>

            <div className="lib-panel">
                {/* Toolbar */}
                <div className="sr-toolbar">
                    <div className="lib-search">
                        <Search size={16} className="lib-search-icon" />
                        <input
                            type="text"
                            placeholder="Tìm tên, mã SV, tiêu đề..."
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
                        Tổng số <strong>{processedComplaints.length}</strong> kết quả
                    </span>
                </div>

                {/* Content */}
                {loading ? (
                    <div className="lib-loading"><div className="lib-spinner" /></div>
                ) : processedComplaints.length === 0 ? (
                    <div className="lib-empty">
                        <h3>Chưa có khiếu nại nào</h3>
                        <p>Khiếu nại từ sinh viên khi bị trừ điểm sẽ xuất hiện ở đây</p>
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
                                                checked={pagedComplaints.length > 0 && pagedComplaints.every(c => selectedIds.has(c.id))}
                                                onChange={toggleSelectAll}
                                            />
                                        </th>
                                        {columns.filter(c => visibleColumns[c.key]).map(col => renderColumnHeader(col.key, col.label))}
                                    </tr>
                                </thead>
                                <tbody>
                                    {pagedComplaints.length === 0 ? (
                                        <tr><td colSpan={columns.length + 1} className="sr-table-empty-cell">Không tìm thấy khiếu nại nào</td></tr>
                                    ) : pagedComplaints.map(c => (
                                        <tr key={c.id} className={`sr-table-row${selectedIds.has(c.id) ? " selected" : ""}`} onClick={() => setSelectedComplaint(c)}>
                                            <td className="sr-checkbox-col" onClick={e => e.stopPropagation()}>
                                                <input type="checkbox" checked={selectedIds.has(c.id)} onChange={() => toggleSelect(c.id)} />
                                            </td>

                                            {visibleColumns.student && (
                                                <td>
                                                    <div className="sr-student-cell">
                                                        {renderAvatar(c.studentAvatar, c.studentName)}
                                                        <div>
                                                            <div className="sr-student-name">{c.studentName || "Sinh viên"}</div>
                                                            <div className="sr-student-code">{c.studentCode}</div>
                                                        </div>
                                                    </div>
                                                </td>
                                            )}
                                            {visibleColumns.subject && (
                                                <td style={{ fontWeight: 600 }}>{c.subject || "-"}</td>
                                            )}
                                            {visibleColumns.content && (
                                                <td><div className="sr-desc-cell">{c.content || "-"}</div></td>
                                            )}
                                            {visibleColumns.status && (
                                                <td>{renderStatus(c.status)}</td>
                                            )}
                                            {visibleColumns.createdAt && (
                                                <td className="sr-date-cell">{formatDate(c.createdAt)}</td>
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
                        {pagedComplaints.map(c => (
                            <div key={c.id} className={`sr-card${selectedIds.has(c.id) ? " selected" : ""}`} onClick={() => setSelectedComplaint(c)}>
                                <div className="sr-card-check" onClick={e => e.stopPropagation()}>
                                    <input type="checkbox" checked={selectedIds.has(c.id)} onChange={() => toggleSelect(c.id)} />
                                </div>

                                <div className="sr-card-header">
                                    <div className="sr-student-cell">
                                        {renderAvatar(c.studentAvatar, c.studentName)}
                                        <div>
                                            <div className="sr-student-name">{c.studentName || "Sinh viên"}</div>
                                            <div className="sr-student-code">{c.studentCode}</div>
                                        </div>
                                    </div>
                                    {renderStatus(c.status)}
                                </div>

                                <div className="cm-complaint-content">
                                    <div className="cm-violation-type">{c.subject}</div>
                                    {c.content && <div className="sr-card-desc">{c.content}</div>}
                                </div>

                                <div className="sr-card-footer">
                                    <span className="sr-card-time">{formatDate(c.createdAt)}</span>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {/* MODAL */}
            {selectedComplaint && (
                <div className="sr-modal-overlay" onClick={() => setSelectedComplaint(null)}>
                    <div className="sr-modal" onClick={e => e.stopPropagation()}>
                        <div className="sr-modal-header">
                            <h2>Chi tiết khiếu nại</h2>
                            <button className="sr-modal-close" onClick={() => setSelectedComplaint(null)}>&times;</button>
                        </div>
                        <div className="sr-modal-body">
                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Sinh viên</div>
                                <div className="sr-student-cell">
                                    {renderAvatar(selectedComplaint.studentAvatar, selectedComplaint.studentName)}
                                    <div>
                                        <div className="sr-student-name">{selectedComplaint.studentName || "Sinh viên"}</div>
                                        <div className="sr-student-code">{selectedComplaint.studentCode}</div>
                                    </div>
                                </div>
                            </div>

                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Trạng thái</div>
                                <div className="sr-modal-value">
                                    {renderStatus(selectedComplaint.status)}
                                </div>
                            </div>

                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Tiêu đề</div>
                                <div className="sr-modal-value" style={{ fontWeight: 600 }}>{selectedComplaint.subject}</div>
                            </div>

                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Nội dung</div>
                                <div className="sr-modal-value">{selectedComplaint.content || "-"}</div>
                            </div>

                            {selectedComplaint.evidenceUrl && (
                                <div className="sr-modal-section">
                                    <div className="sr-modal-label">Bằng chứng</div>
                                    <a href={selectedComplaint.evidenceUrl} target="_blank" rel="noopener noreferrer" className="lib-btn ghost" style={{ display: "inline-flex" }}>
                                        Xem bằng chứng
                                    </a>
                                </div>
                            )}

                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Thời gian gửi</div>
                                <div className="sr-modal-value">{formatDate(selectedComplaint.createdAt)}</div>
                            </div>

                            {selectedComplaint.resolvedByName && (
                                <div className="sr-modal-section">
                                    <div className="sr-modal-label">Xử lý bởi</div>
                                    <div className="sr-modal-value">
                                        {selectedComplaint.resolvedByName} — {formatDate(selectedComplaint.resolvedAt)}
                                    </div>
                                </div>
                            )}

                            {selectedComplaint.resolutionNote && (
                                <div className="sr-modal-section">
                                    <div className="sr-modal-label">Ghi chú xử lý</div>
                                    <div className="sr-modal-value">{selectedComplaint.resolutionNote}</div>
                                </div>
                            )}
                        </div>
                        <div className="sr-modal-footer">
                            {selectedComplaint.status === "PENDING" && (
                                <>
                                    <button className="sr-modal-btn primary" onClick={() => handleAccept(selectedComplaint.id)} disabled={submitting}>
                                        {submitting ? "Đang xử lý..." : "Chấp nhận"}
                                    </button>
                                    <button className="sr-modal-btn ghost" onClick={() => handleDeny(selectedComplaint.id)} disabled={submitting} style={{ color: "#ef4444", borderColor: "#ef4444" }}>
                                        {submitting ? "Đang xử lý..." : "Từ chối"}
                                    </button>
                                </>
                            )}
                            <button className="sr-modal-btn ghost" onClick={() => setSelectedComplaint(null)}>Đóng</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default ComplaintManage;
