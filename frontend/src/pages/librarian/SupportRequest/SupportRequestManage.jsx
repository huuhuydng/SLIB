import React, { useState, useEffect, useCallback, useMemo, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { Search, Loader2, ArrowUpDown, ArrowUp, ArrowDown, Filter, X, SlidersHorizontal, LayoutGrid, LayoutList, Trash2 } from "lucide-react";
import "../../../styles/librarian/librarian-shared.css";
import "../../../styles/librarian/CheckInOut.css";
import "../../../styles/librarian/SupportRequestManage.css";

const API_BASE = `${import.meta.env.VITE_API_URL || "http://localhost:8080"}/slib/support-requests`;

const STATUS_LABELS = {
    PENDING: "Mới",
    IN_PROGRESS: "Đang xử lý",
    RESOLVED: "Đã giải quyết",
    REJECTED: "Từ chối",
};

const STATUS_OPTIONS = [
    { value: "", label: "Tất cả" },
    { value: "PENDING", label: "Mới" },
    { value: "IN_PROGRESS", label: "Đang xử lý" },
    { value: "RESOLVED", label: "Đã giải quyết" },
];

function SupportRequestManage() {
    const navigate = useNavigate();
    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedRequest, setSelectedRequest] = useState(null);
    const [responseText, setResponseText] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [lightboxImage, setLightboxImage] = useState(null);
    const [chatLoading, setChatLoading] = useState(false);
    const [searchTerm, setSearchTerm] = useState("");

    // View mode: 'table' or 'card'
    const [viewMode, setViewMode] = useState("table");

    // Selection for batch delete
    const [selectedIds, setSelectedIds] = useState(new Set());
    const [deleting, setDeleting] = useState(false);

    // Pagination
    const [currentPage, setCurrentPage] = useState(1);
    const [itemsPerPage, setItemsPerPage] = useState(10);

    // Sort state
    const [sortConfig, setSortConfig] = useState({ column: null, direction: null });

    // Column filters
    const [columnFilters, setColumnFilters] = useState({
        student: '',
        description: '',
        status: '',
        createdAt: '',
    });
    const [activeFilterCol, setActiveFilterCol] = useState(null);
    const filterRef = useRef(null);

    // Column visibility
    const [visibleColumns, setVisibleColumns] = useState({
        student: true,
        description: true,
        status: true,
        createdAt: true,
    });
    const [showColumnMenu, setShowColumnMenu] = useState(false);

    const getToken = () => sessionStorage.getItem("librarian_token") || localStorage.getItem("librarian_token");

    const fetchRequests = useCallback(async () => {
        setLoading(true);
        try {
            const token = getToken();
            const res = await fetch(API_BASE, {
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
    }, []);

    useEffect(() => {
        fetchRequests();
    }, [fetchRequests]);

    // Close filter dropdown on outside click
    useEffect(() => {
        const handleClickOutside = (e) => {
            if (filterRef.current && !filterRef.current.contains(e.target)) {
                setActiveFilterCol(null);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

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
            // Update status to IN_PROGRESS first
            await fetch(`${API_BASE}/${requestId}/status`, {
                method: "PUT",
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ status: "IN_PROGRESS" }),
            });
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

    const handleDeleteBatch = async () => {
        if (selectedIds.size === 0) return;
        if (!window.confirm(`Bạn có chắc muốn xoá ${selectedIds.size} yêu cầu đã chọn?`)) return;
        setDeleting(true);
        try {
            const token = getToken();
            const res = await fetch(`${API_BASE}/batch`, {
                method: "DELETE",
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ ids: Array.from(selectedIds) }),
            });
            if (res.ok) {
                setSelectedIds(new Set());
                fetchRequests();
            }
        } catch (err) {
            console.error("Error deleting:", err);
        } finally {
            setDeleting(false);
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
        if (!name) return "?";
        return name.split(' ').map(n => n[0]).slice(-2).join('').toUpperCase();
    };

    // Get value for sorting/filtering
    const getRequestValue = (req, column) => {
        switch (column) {
            case 'student': return req.studentName || '';
            case 'description': return req.description || '';
            case 'status': return STATUS_LABELS[req.status] || req.status || '';
            case 'createdAt': return req.createdAt || '';
            default: return '';
        }
    };

    const filteredRequests = useMemo(() => {
        let list = [...requests];

        // Global search
        const q = searchTerm.trim().toLowerCase();
        if (q) {
            list = list.filter((r) =>
                (r.studentName || "").toLowerCase().includes(q) ||
                (r.studentCode || "").toLowerCase().includes(q) ||
                (r.description || "").toLowerCase().includes(q)
            );
        }

        // Column filters
        Object.entries(columnFilters).forEach(([col, filterVal]) => {
            if (!filterVal) return;
            const fq = filterVal.toLowerCase();

            if (col === 'status') {
                list = list.filter(r => r.status === filterVal);
            } else if (col === 'student') {
                list = list.filter(r =>
                    (r.studentName || '').toLowerCase().includes(fq) ||
                    (r.studentCode || '').toLowerCase().includes(fq)
                );
            } else if (col === 'description') {
                list = list.filter(r => (r.description || '').toLowerCase().includes(fq));
            } else if (col === 'createdAt') {
                list = list.filter(r => formatTime(r.createdAt).toLowerCase().includes(fq));
            }
        });

        // Sort
        if (sortConfig.column && sortConfig.direction) {
            list.sort((a, b) => {
                let valA = getRequestValue(a, sortConfig.column);
                let valB = getRequestValue(b, sortConfig.column);

                if (sortConfig.column === 'createdAt') {
                    valA = valA ? new Date(valA).getTime() : 0;
                    valB = valB ? new Date(valB).getTime() : 0;
                } else {
                    valA = valA.toLowerCase();
                    valB = valB.toLowerCase();
                }

                if (valA < valB) return sortConfig.direction === 'asc' ? -1 : 1;
                if (valA > valB) return sortConfig.direction === 'asc' ? 1 : -1;
                return 0;
            });
        } else {
            list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        }

        return list;
    }, [requests, searchTerm, columnFilters, sortConfig]);

    // Pagination
    const totalPages = Math.ceil(filteredRequests.length / itemsPerPage);
    const paginatedRequests = filteredRequests.slice(
        (currentPage - 1) * itemsPerPage,
        currentPage * itemsPerPage
    );

    useEffect(() => {
        setCurrentPage(1);
    }, [searchTerm, columnFilters, sortConfig, itemsPerPage]);

    // Sort handler
    const handleSort = (column) => {
        setSortConfig(prev => {
            if (prev.column !== column) return { column, direction: 'asc' };
            if (prev.direction === 'asc') return { column, direction: 'desc' };
            return { column: null, direction: null };
        });
    };

    const handleFilterChange = (column, value) => {
        setColumnFilters(prev => ({ ...prev, [column]: value }));
    };

    const clearColumnFilter = (column) => {
        setColumnFilters(prev => ({ ...prev, [column]: '' }));
        setActiveFilterCol(null);
    };

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

    // Selection logic
    const toggleSelect = (id) => {
        setSelectedIds(prev => {
            const next = new Set(prev);
            if (next.has(id)) next.delete(id);
            else next.add(id);
            return next;
        });
    };

    const toggleSelectAll = () => {
        if (selectedIds.size === paginatedRequests.length) {
            setSelectedIds(new Set());
        } else {
            setSelectedIds(new Set(paginatedRequests.map(r => r.id)));
        }
    };

    const isAllSelected = paginatedRequests.length > 0 && selectedIds.size === paginatedRequests.length;

    // Render sort icon
    const renderSortIcon = (column) => {
        if (sortConfig.column === column) {
            if (sortConfig.direction === 'asc') return <ArrowUp size={13} />;
            if (sortConfig.direction === 'desc') return <ArrowDown size={13} />;
        }
        return <ArrowUpDown size={13} />;
    };

    // Column header with sort + filter
    const renderColumnHeader = (column, label, isCenter = false) => {
        const hasFilter = !!columnFilters[column];
        return (
            <th key={column} className={isCenter ? 'center' : ''}>
                <div className="cio-th-content" style={isCenter ? { justifyContent: 'center' } : {}}>
                    <span className="cio-th-label">{label}</span>
                    <div className="cio-th-actions">
                        <button
                            className={`cio-th-btn${sortConfig.column === column ? ' active' : ''}`}
                            onClick={(e) => { e.stopPropagation(); handleSort(column); }}
                            title="Sắp xếp"
                        >
                            {renderSortIcon(column)}
                        </button>
                        <button
                            className={`cio-th-btn${hasFilter ? ' active' : ''}${activeFilterCol === column ? ' open' : ''}`}
                            onClick={(e) => {
                                e.stopPropagation();
                                setActiveFilterCol(prev => prev === column ? null : column);
                            }}
                            title="Lọc"
                        >
                            <Filter size={13} className={hasFilter ? 'cio-filter-active' : ''} />
                        </button>
                    </div>
                    {activeFilterCol === column && (
                        <div className="cio-filter-dropdown" ref={filterRef} onClick={e => e.stopPropagation()}>
                            {column === 'status' ? (
                                <select
                                    value={columnFilters.status}
                                    onChange={(e) => { handleFilterChange('status', e.target.value); setActiveFilterCol(null); }}
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
                                        value={columnFilters[column]}
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

    const activeFilterCount = Object.values(columnFilters).filter(Boolean).length;
    const visibleColumnCount = Object.values(visibleColumns).filter(Boolean).length + 1; // +1 for checkbox

    const openDetail = (req) => {
        setSelectedRequest(req);
        setResponseText(req.adminResponse || "");
    };

    // Status as plain text with dot
    const renderStatus = (status) => {
        const dotColors = {
            PENDING: '#f59e0b',
            IN_PROGRESS: '#3b82f6',
            RESOLVED: '#22c55e',
            REJECTED: '#ef4444',
        };
        return (
            <span className="sr-status-text">
                <span className="sr-status-dot" style={{ background: dotColors[status] || '#94a3b8' }} />
                {STATUS_LABELS[status] || status}
            </span>
        );
    };

    return (
        <div className="lib-container">
            {/* Page Title */}
            <div className="lib-page-title">
                <h1>YÊU CẦU HỖ TRỢ</h1>
            </div>

            {/* Table Panel */}
            <div className="lib-panel">
                {/* Toolbar */}
                <div className="cio-toolbar">
                    <div className="lib-search">
                        <Search size={16} className="lib-search-icon" />
                        <input
                            type="text"
                            placeholder="Tìm tên, mã SV, nội dung..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>

                    {/* View toggle */}
                    <div className="sr-view-toggle">
                        <button
                            className={`sr-view-btn${viewMode === 'table' ? ' active' : ''}`}
                            onClick={() => setViewMode('table')}
                            title="Dạng bảng"
                        >
                            <LayoutList size={16} />
                        </button>
                        <button
                            className={`sr-view-btn${viewMode === 'card' ? ' active' : ''}`}
                            onClick={() => setViewMode('card')}
                            title="Dạng thẻ"
                        >
                            <LayoutGrid size={16} />
                        </button>
                    </div>

                    {viewMode === 'table' && (
                        <div style={{ position: 'relative' }}>
                            <button
                                className="cio-column-toggle"
                                onClick={() => setShowColumnMenu(!showColumnMenu)}
                            >
                                <SlidersHorizontal size={14} />
                                Hiển thị cột
                            </button>
                            {showColumnMenu && (
                                <div className="cio-column-menu">
                                    {[
                                        { key: 'student', label: 'Sinh viên' },
                                        { key: 'description', label: 'Mô tả' },
                                        { key: 'status', label: 'Trạng thái' },
                                        { key: 'createdAt', label: 'Thời gian' },
                                    ].map(col => (
                                        <label key={col.key} className="cio-column-menu-item">
                                            <input
                                                type="checkbox"
                                                checked={visibleColumns[col.key]}
                                                onChange={() => setVisibleColumns(prev => ({ ...prev, [col.key]: !prev[col.key] }))}
                                                style={{ accentColor: '#FF751F' }}
                                            />
                                            {col.label}
                                        </label>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}

                    {/* Batch delete */}
                    {selectedIds.size > 0 && (
                        <button
                            className="sr-delete-btn"
                            onClick={handleDeleteBatch}
                            disabled={deleting}
                        >
                            <Trash2 size={14} />
                            {deleting ? "Đang xoá..." : `Xoá (${selectedIds.size})`}
                        </button>
                    )}

                    <span className="cio-result-count">
                        {activeFilterCount > 0 && (
                            <span className="cio-active-filters">
                                {activeFilterCount} bộ lọc |{' '}
                            </span>
                        )}
                        Tổng số <strong>{filteredRequests.length}</strong> kết quả
                    </span>
                </div>

                {/* Content */}
                {loading ? (
                    <div className="sm-loading">
                        <Loader2 size={28} className="sm-spinner" />
                        <span>Đang tải...</span>
                    </div>
                ) : viewMode === 'table' ? (
                    /* ========== TABLE VIEW ========== */
                    <div className="sr-table-wrapper">
                        <table className="sr-table">
                            <thead>
                                <tr>
                                    <th className="sr-checkbox-col">
                                        <input
                                            type="checkbox"
                                            checked={isAllSelected}
                                            onChange={toggleSelectAll}
                                            style={{ accentColor: '#FF751F' }}
                                        />
                                    </th>
                                    {visibleColumns.student && renderColumnHeader('student', 'Sinh viên')}
                                    {visibleColumns.description && renderColumnHeader('description', 'Mô tả')}
                                    {visibleColumns.status && renderColumnHeader('status', 'Trạng thái')}
                                    {visibleColumns.createdAt && renderColumnHeader('createdAt', 'Thời gian')}
                                </tr>
                            </thead>
                            <tbody>
                                {paginatedRequests.length === 0 ? (
                                    <tr>
                                        <td colSpan={visibleColumnCount} className="sr-table-empty-cell">
                                            {searchTerm ? "Không tìm thấy yêu cầu phù hợp." : "Chưa có yêu cầu hỗ trợ nào."}
                                        </td>
                                    </tr>
                                ) : (
                                    paginatedRequests.map((req) => (
                                        <tr
                                            key={req.id}
                                            className={`sr-table-row${selectedIds.has(req.id) ? ' selected' : ''}`}
                                            onClick={() => openDetail(req)}
                                        >
                                            <td className="sr-checkbox-col" onClick={(e) => e.stopPropagation()}>
                                                <input
                                                    type="checkbox"
                                                    checked={selectedIds.has(req.id)}
                                                    onChange={() => toggleSelect(req.id)}
                                                    style={{ accentColor: '#FF751F' }}
                                                />
                                            </td>
                                            {visibleColumns.student && (
                                                <td>
                                                    <div className="sr-student-cell">
                                                        {req.studentAvatar ? (
                                                            <img src={req.studentAvatar} alt="" className="sr-avatar" />
                                                        ) : (
                                                            <div className="sr-avatar-placeholder">
                                                                {getInitial(req.studentName)}
                                                            </div>
                                                        )}
                                                        <div>
                                                            <div className="sr-student-name">{req.studentName}</div>
                                                            <div className="sr-student-code">{req.studentCode}</div>
                                                        </div>
                                                    </div>
                                                </td>
                                            )}
                                            {visibleColumns.description && (
                                                <td>
                                                    <div className="sr-desc-cell">{req.description}</div>
                                                </td>
                                            )}
                                            {visibleColumns.status && (
                                                <td>{renderStatus(req.status)}</td>
                                            )}
                                            {visibleColumns.createdAt && (
                                                <td className="sr-date-cell">
                                                    {formatTime(req.createdAt)}
                                                </td>
                                            )}
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    /* ========== CARD VIEW ========== */
                    <div className="sr-card-grid">
                        {paginatedRequests.length === 0 ? (
                            <div className="lib-empty">
                                <h3>Chưa có yêu cầu nào</h3>
                                <p>Các yêu cầu hỗ trợ từ sinh viên sẽ xuất hiện ở đây</p>
                            </div>
                        ) : (
                            paginatedRequests.map((req) => (
                                <div
                                    key={req.id}
                                    className={`sr-card${selectedIds.has(req.id) ? ' selected' : ''}`}
                                    onClick={() => openDetail(req)}
                                >
                                    <div className="sr-card-check" onClick={(e) => e.stopPropagation()}>
                                        <input
                                            type="checkbox"
                                            checked={selectedIds.has(req.id)}
                                            onChange={() => toggleSelect(req.id)}
                                            style={{ accentColor: '#FF751F' }}
                                        />
                                    </div>
                                    <div className="sr-card-header">
                                        <div className="sr-student-cell">
                                            {req.studentAvatar ? (
                                                <img src={req.studentAvatar} alt="" className="sr-avatar" />
                                            ) : (
                                                <div className="sr-avatar-placeholder">
                                                    {getInitial(req.studentName)}
                                                </div>
                                            )}
                                            <div>
                                                <div className="sr-student-name">{req.studentName}</div>
                                                <div className="sr-student-code">{req.studentCode}</div>
                                            </div>
                                        </div>
                                        {renderStatus(req.status)}
                                    </div>
                                    <div className="sr-card-desc">{req.description}</div>
                                    {req.imageUrls && req.imageUrls.length > 0 && (
                                        <div className="sr-card-images">
                                            {req.imageUrls.slice(0, 3).map((url, idx) => (
                                                <img key={idx} src={url} alt="" className="sr-card-thumb" />
                                            ))}
                                            {req.imageUrls.length > 3 && (
                                                <span className="sr-card-more">+{req.imageUrls.length - 3}</span>
                                            )}
                                        </div>
                                    )}
                                    <div className="sr-card-footer">
                                        <span className="sr-card-time">{formatTime(req.createdAt)}</span>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                )}

                {/* Pagination */}
                <div className="cio-pagination">
                    <div className="cio-page-size">
                        <span>Số hàng mỗi trang:</span>
                        <select
                            value={itemsPerPage}
                            onChange={(e) => setItemsPerPage(Number(e.target.value))}
                        >
                            <option value={10}>10</option>
                            <option value={20}>20</option>
                            <option value={50}>50</option>
                        </select>
                    </div>
                    {totalPages > 1 && (
                        <div className="cio-pagination-right">
                            <button
                                onClick={() => setCurrentPage(prev => Math.max(1, prev - 1))}
                                disabled={currentPage === 1}
                                className="cio-page-btn"
                            >
                                &lt;
                            </button>
                            <div className="cio-page-numbers">
                                {getPageNumbers().map((page, idx) => (
                                    page === '...' ? (
                                        <span key={`ellipsis-${idx}`} className="cio-page-ellipsis">...</span>
                                    ) : (
                                        <button
                                            key={page}
                                            onClick={() => setCurrentPage(page)}
                                            className={`cio-page-btn ${currentPage === page ? 'active' : ''}`}
                                        >
                                            {page}
                                        </button>
                                    )
                                ))}
                            </div>
                            <button
                                onClick={() => setCurrentPage(prev => Math.min(totalPages, prev + 1))}
                                disabled={currentPage === totalPages}
                                className="cio-page-btn"
                            >
                                &gt;
                            </button>
                        </div>
                    )}
                </div>
            </div>

            {/* ========== MODAL - Detail ========== */}
            {selectedRequest && (
                <div className="sr-modal-overlay" onClick={() => setSelectedRequest(null)}>
                    <div className="sr-modal" onClick={(e) => e.stopPropagation()}>
                        <div className="sr-modal-header">
                            <h2>Chi tiết yêu cầu hỗ trợ</h2>
                            <button className="sr-modal-close" onClick={() => setSelectedRequest(null)}>&times;</button>
                        </div>
                        <div className="sr-modal-body">
                            {/* Student Info */}
                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Sinh viên</div>
                                <div className="sr-student-cell">
                                    {selectedRequest.studentAvatar ? (
                                        <img src={selectedRequest.studentAvatar} alt="" className="sr-avatar" />
                                    ) : (
                                        <div className="sr-avatar-placeholder">
                                            {getInitial(selectedRequest.studentName)}
                                        </div>
                                    )}
                                    <div>
                                        <div className="sr-student-name">{selectedRequest.studentName}</div>
                                        <div className="sr-student-code">{selectedRequest.studentCode}</div>
                                    </div>
                                </div>
                            </div>

                            {/* Status */}
                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Trạng thái</div>
                                {renderStatus(selectedRequest.status)}
                            </div>

                            {/* Description */}
                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Mô tả vấn đề</div>
                                <div className="sr-modal-value">{selectedRequest.description}</div>
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
                                                className="sr-modal-thumb"
                                                onClick={() => setLightboxImage(url)}
                                            />
                                        ))}
                                    </div>
                                </div>
                            )}

                            {/* Time */}
                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Thời gian gửi</div>
                                <div className="sr-modal-value">{formatTime(selectedRequest.createdAt)}</div>
                            </div>

                            {/* Existing Response */}
                            {selectedRequest.adminResponse && (
                                <div className="sr-modal-section">
                                    <div className="sr-modal-label">Phản hồi từ thủ thư</div>
                                    <div className="sr-modal-value">{selectedRequest.adminResponse}</div>
                                    {selectedRequest.resolvedByName && (
                                        <div className="sr-resolver-info">
                                            Phản hồi bởi: {selectedRequest.resolvedByName} - {formatTime(selectedRequest.resolvedAt)}
                                        </div>
                                    )}
                                </div>
                            )}

                            {/* Response Form - only for PENDING or IN_PROGRESS */}
                            {(selectedRequest.status === "PENDING" || selectedRequest.status === "IN_PROGRESS") && (
                                <div className="sr-modal-section">
                                    <div className="sr-modal-label">Phản hồi yêu cầu</div>
                                    <textarea
                                        className="sr-modal-textarea"
                                        value={responseText}
                                        onChange={(e) => setResponseText(e.target.value)}
                                        placeholder="Nhập phản hồi cho sinh viên..."
                                        rows={3}
                                    />
                                </div>
                            )}
                        </div>

                        <div className="sr-modal-footer">
                            {(selectedRequest.status === "PENDING" || selectedRequest.status === "IN_PROGRESS") && (
                                <>
                                    <button
                                        className="sr-modal-btn ghost"
                                        onClick={() => handleStartChat(selectedRequest.id)}
                                        disabled={chatLoading}
                                    >
                                        {chatLoading ? "Đang mở..." : "Chat với sinh viên"}
                                    </button>
                                    <button
                                        className="sr-modal-btn primary"
                                        onClick={handleRespond}
                                        disabled={submitting || !responseText.trim()}
                                    >
                                        {submitting ? "Đang gửi..." : "Gửi phản hồi"}
                                    </button>
                                </>
                            )}
                            <button
                                className="sr-modal-btn ghost"
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
                <div className="lib-lightbox" onClick={() => setLightboxImage(null)}>
                    <img src={lightboxImage} alt="" />
                </div>
            )}

            {/* Close column menu */}
            {showColumnMenu && (
                <div
                    style={{ position: 'fixed', inset: 0, zIndex: 50 }}
                    onClick={() => setShowColumnMenu(false)}
                />
            )}
        </div>
    );
}

export default SupportRequestManage;
