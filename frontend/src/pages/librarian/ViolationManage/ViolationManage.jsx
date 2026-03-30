import React, { useState, useEffect, useCallback, useMemo, useRef } from "react";
import { Search, Loader2, ArrowUpDown, ArrowUp, ArrowDown, Filter, X, SlidersHorizontal, LayoutGrid, LayoutList, Trash2 } from "lucide-react";
import "../../../styles/librarian/librarian-shared.css";
import "../../../styles/librarian/CheckInOut.css";
import "../../../styles/librarian/ViolationManage.css";
import { useToast } from '../../../components/common/ToastProvider';
import { useConfirm } from '../../../components/common/ConfirmDialog';

import { API_BASE_URL } from '../../../config/apiConfig';

const API_BASE = `${API_BASE_URL}/slib/violation-reports`;

const STATUS_LABELS = {
    PENDING: "Chờ xử lý",
    VERIFIED: "Đã xác minh",
    RESOLVED: "Đã xử lý",
    REJECTED: "Từ chối",
};

const STATUS_OPTIONS = [
    { value: "", label: "Tất cả" },
    { value: "PENDING", label: "Chờ xử lý" },
    { value: "VERIFIED", label: "Đã xác minh" },
    { value: "REJECTED", label: "Từ chối" },
];

const VIOLATION_TYPE_LABELS = {
    UNAUTHORIZED_USE: "Sử dụng ghế không đúng",
    LEFT_BELONGINGS: "Để đồ giữ chỗ",
    NOISE: "Gây ồn ào",
    FEET_ON_SEAT: "Gác chân lên ghế/bàn",
    FOOD_DRINK: "Ăn uống trong thư viện",
    SLEEPING: "Ngủ tại chỗ ngồi",
    OTHER: "Khác",
};

function ViolationManage() {
    const toast = useToast();
    const { confirm } = useConfirm();
    const [reports, setReports] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedReport, setSelectedReport] = useState(null);
    const [submitting, setSubmitting] = useState(false);
    const [lightboxImage, setLightboxImage] = useState(null);
    const [searchTerm, setSearchTerm] = useState("");
    const [erroredAvatars, setErroredAvatars] = useState(new Set());

    // View mode
    const [viewMode, setViewMode] = useState("table");

    // Selection for batch delete
    const [selectedIds, setSelectedIds] = useState(new Set());
    const [deleting, setDeleting] = useState(false);

    // Pagination
    const [currentPage, setCurrentPage] = useState(1);
    const [itemsPerPage, setItemsPerPage] = useState(10);

    // Sort
    const [sortConfig, setSortConfig] = useState({ column: null, direction: null });

    // Column filters
    const [columnFilters, setColumnFilters] = useState({
        violator: '',
        violationType: '',
        location: '',
        status: '',
        createdAt: '',
    });
    const [activeFilterCol, setActiveFilterCol] = useState(null);
    const filterRef = useRef(null);

    // Column visibility
    const [visibleColumns, setVisibleColumns] = useState({
        violator: true,
        violationType: true,
        location: true,
        status: true,
        createdAt: true,
    });
    const [showColumnMenu, setShowColumnMenu] = useState(false);

    const getToken = () => sessionStorage.getItem("librarian_token") || localStorage.getItem("librarian_token");

    const fetchReports = useCallback(async () => {
        setLoading(true);
        try {
            const token = getToken();
            const res = await fetch(API_BASE, {
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
    }, []);

    useEffect(() => {
        fetchReports();
    }, [fetchReports]);

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

    const handleVerify = async (id) => {
        setSubmitting(true);
        try {
            const token = getToken();
            const res = await fetch(`${API_BASE}/${id}/verify`, {
                method: "PUT",
                headers: { Authorization: `Bearer ${token}` },
            });
            if (res.ok) {
                toast.success('Đã xác minh báo cáo vi phạm thành công.');
                setSelectedReport(null);
                fetchReports();
            } else {
                toast.error('Không thể xác minh báo cáo. Vui lòng thử lại.');
            }
        } catch (err) {
            console.error("Error verifying report:", err);
            toast.error('Lỗi: ' + err.message);
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
                toast.success('Đã từ chối báo cáo vi phạm.');
                setSelectedReport(null);
                fetchReports();
            } else {
                toast.error('Không thể từ chối báo cáo. Vui lòng thử lại.');
            }
        } catch (err) {
            console.error("Error rejecting report:", err);
            toast.error('Lỗi: ' + err.message);
        } finally {
            setSubmitting(false);
        }
    };

    const handleDeleteBatch = async () => {
        if (selectedIds.size === 0) return;
        const confirmed = await confirm({
            title: 'Xoá báo cáo vi phạm',
            message: `Bạn có chắc muốn xoá ${selectedIds.size} báo cáo đã chọn?`,
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
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ ids: Array.from(selectedIds) }),
            });
            if (res.ok) {
                toast.success(`Đã xoá ${selectedIds.size} báo cáo vi phạm thành công.`);
                setSelectedIds(new Set());
                fetchReports();
            } else {
                toast.error('Không thể xoá báo cáo. Vui lòng thử lại.');
            }
        } catch (err) {
            console.error("Error deleting:", err);
            toast.error('Lỗi: ' + err.message);
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

    const renderAvatar = (avatarUrl, name, fallbackName) => {
        const displayName = name || fallbackName || 'Chưa xác định';
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

    const getLocation = (r) => {
        let loc = r.seatCode ? `Ghế ${r.seatCode}` : '';
        if (r.zoneName) loc += ` - ${r.zoneName}`;
        if (r.areaName) loc += ` (${r.areaName})`;
        return loc;
    };

    const getReportValue = (report, column) => {
        switch (column) {
            case 'violator': return report.violatorName || '';
            case 'violationType': return VIOLATION_TYPE_LABELS[report.violationType] || report.violationType || '';
            case 'location': return getLocation(report);
            case 'status': return STATUS_LABELS[report.status] || report.status || '';
            case 'createdAt': return report.createdAt || '';
            default: return '';
        }
    };

    const filteredReports = useMemo(() => {
        let list = [...reports];

        // Global search
        const q = searchTerm.trim().toLowerCase();
        if (q) {
            list = list.filter((r) =>
                (r.violatorName || "").toLowerCase().includes(q) ||
                (r.violatorCode || "").toLowerCase().includes(q) ||
                (r.description || "").toLowerCase().includes(q) ||
                (VIOLATION_TYPE_LABELS[r.violationType] || "").toLowerCase().includes(q)
            );
        }

        // Column filters
        Object.entries(columnFilters).forEach(([col, filterVal]) => {
            if (!filterVal) return;
            const fq = filterVal.toLowerCase();

            if (col === 'status') {
                list = list.filter(r => r.status === filterVal);
            } else if (col === 'violator') {
                list = list.filter(r =>
                    (r.violatorName || '').toLowerCase().includes(fq) ||
                    (r.violatorCode || '').toLowerCase().includes(fq)
                );
            } else if (col === 'violationType') {
                list = list.filter(r =>
                    (VIOLATION_TYPE_LABELS[r.violationType] || '').toLowerCase().includes(fq)
                );
            } else if (col === 'location') {
                list = list.filter(r => getLocation(r).toLowerCase().includes(fq));
            } else if (col === 'createdAt') {
                list = list.filter(r => formatTime(r.createdAt).toLowerCase().includes(fq));
            }
        });

        // Sort
        if (sortConfig.column && sortConfig.direction) {
            list.sort((a, b) => {
                let valA = getReportValue(a, sortConfig.column);
                let valB = getReportValue(b, sortConfig.column);
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
    }, [reports, searchTerm, columnFilters, sortConfig]);

    // Pagination
    const totalPages = Math.ceil(filteredReports.length / itemsPerPage);
    const paginatedReports = filteredReports.slice(
        (currentPage - 1) * itemsPerPage,
        currentPage * itemsPerPage
    );

    useEffect(() => {
        setCurrentPage(1);
    }, [searchTerm, columnFilters, sortConfig, itemsPerPage]);

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
            if (next.has(id)) next.delete(id); else next.add(id);
            return next;
        });
    };

    const toggleSelectAll = () => {
        if (selectedIds.size === paginatedReports.length) {
            setSelectedIds(new Set());
        } else {
            setSelectedIds(new Set(paginatedReports.map(r => r.id)));
        }
    };

    const isAllSelected = paginatedReports.length > 0 && selectedIds.size === paginatedReports.length;

    // Render sort icon
    const renderSortIcon = (column) => {
        if (sortConfig.column === column) {
            if (sortConfig.direction === 'asc') return <ArrowUp size={13} />;
            if (sortConfig.direction === 'desc') return <ArrowDown size={13} />;
        }
        return <ArrowUpDown size={13} />;
    };

    // Column header
    const renderColumnHeader = (column, label) => {
        const hasFilter = !!columnFilters[column];
        return (
            <th key={column}>
                <div className="cio-th-content">
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
    const visibleColumnCount = Object.values(visibleColumns).filter(Boolean).length + 1;

    const openDetail = (report) => setSelectedReport(report);

    // Status as plain text with dot
    const renderStatus = (status) => {
        const dotColors = {
            PENDING: '#f59e0b',
            VERIFIED: '#22c55e',
            RESOLVED: '#3b82f6',
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
            <div className="lib-page-title">
                <h1>QUẢN LÝ VI PHẠM</h1>
            </div>

            <div className="lib-panel">
                {/* Toolbar */}
                <div className="cio-toolbar">
                    <div className="lib-search">
                        <Search size={16} className="lib-search-icon" />
                        <input
                            type="text"
                            placeholder="Tìm tên, mã SV, loại vi phạm..."
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
                                        { key: 'violator', label: 'Người vi phạm' },
                                        { key: 'violationType', label: 'Loại vi phạm' },
                                        { key: 'location', label: 'Vị trí' },
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
                        <button className="sr-delete-btn" onClick={handleDeleteBatch} disabled={deleting}>
                            <Trash2 size={14} />
                            {deleting ? "Đang xoá..." : `Xoá (${selectedIds.size})`}
                        </button>
                    )}

                    <span className="cio-result-count">
                        {activeFilterCount > 0 && (
                            <span className="cio-active-filters">{activeFilterCount} bộ lọc |{' '}</span>
                        )}
                        Tổng số <strong>{filteredReports.length}</strong> kết quả
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
                                    {visibleColumns.violator && renderColumnHeader('violator', 'Người vi phạm')}
                                    {visibleColumns.violationType && renderColumnHeader('violationType', 'Loại vi phạm')}
                                    {visibleColumns.location && renderColumnHeader('location', 'Vị trí')}
                                    {visibleColumns.status && renderColumnHeader('status', 'Trạng thái')}
                                    {visibleColumns.createdAt && renderColumnHeader('createdAt', 'Thời gian')}
                                </tr>
                            </thead>
                            <tbody>
                                {paginatedReports.length === 0 ? (
                                    <tr>
                                        <td colSpan={visibleColumnCount} className="sr-table-empty-cell">
                                            {searchTerm ? "Không tìm thấy báo cáo phù hợp." : "Chưa có báo cáo vi phạm nào."}
                                        </td>
                                    </tr>
                                ) : (
                                    paginatedReports.map((report) => (
                                        <tr
                                            key={report.id}
                                            className={`sr-table-row${selectedIds.has(report.id) ? ' selected' : ''}`}
                                            onClick={() => openDetail(report)}
                                        >
                                            <td className="sr-checkbox-col" onClick={(e) => e.stopPropagation()}>
                                                <input
                                                    type="checkbox"
                                                    checked={selectedIds.has(report.id)}
                                                    onChange={() => toggleSelect(report.id)}
                                                    style={{ accentColor: '#FF751F' }}
                                                />
                                            </td>
                                            {visibleColumns.violator && (
                                                <td>
                                                    <div className="sr-student-cell">
                                                        {renderAvatar(report.violatorAvatar, report.violatorName)}
                                                        <div>
                                                            <div className="sr-student-name">{report.violatorName || 'Chưa xác định'}</div>
                                                            <div className="sr-student-code">{report.violatorCode || ''}</div>
                                                        </div>
                                                    </div>
                                                </td>
                                            )}
                                            {visibleColumns.violationType && (
                                                <td>
                                                    <span className="vr-type-text">
                                                        {VIOLATION_TYPE_LABELS[report.violationType] || report.violationType}
                                                    </span>
                                                </td>
                                            )}
                                            {visibleColumns.location && (
                                                <td className="sr-date-cell">{getLocation(report)}</td>
                                            )}
                                            {visibleColumns.status && (
                                                <td>{renderStatus(report.status)}</td>
                                            )}
                                            {visibleColumns.createdAt && (
                                                <td className="sr-date-cell">{formatTime(report.createdAt)}</td>
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
                        {paginatedReports.length === 0 ? (
                            <div className="lib-empty">
                                <h3>Chưa có báo cáo nào</h3>
                                <p>Các báo cáo vi phạm từ sinh viên sẽ xuất hiện ở đây</p>
                            </div>
                        ) : (
                            paginatedReports.map((report) => (
                                <div
                                    key={report.id}
                                    className={`sr-card${selectedIds.has(report.id) ? ' selected' : ''}`}
                                    onClick={() => openDetail(report)}
                                >
                                    <div className="sr-card-check" onClick={(e) => e.stopPropagation()}>
                                        <input
                                            type="checkbox"
                                            checked={selectedIds.has(report.id)}
                                            onChange={() => toggleSelect(report.id)}
                                            style={{ accentColor: '#FF751F' }}
                                        />
                                    </div>
                                    <div className="sr-card-header">
                                        <div className="sr-student-cell">
                                            {renderAvatar(report.violatorAvatar, report.violatorName)}
                                            <div>
                                                <div className="sr-student-name">{report.violatorName || 'Chưa xác định'}</div>
                                                <div className="sr-student-code">{report.violatorCode || ''}</div>
                                            </div>
                                        </div>
                                        {renderStatus(report.status)}
                                    </div>
                                    <div className="sr-card-desc">
                                        <strong>{VIOLATION_TYPE_LABELS[report.violationType] || report.violationType}</strong>
                                        {' - '}{getLocation(report)}
                                    </div>
                                    {report.description && (
                                        <div className="sr-card-desc" style={{ opacity: 0.75 }}>{report.description}</div>
                                    )}
                                    {report.evidenceUrl && (
                                        <div className="sr-card-images">
                                            <img src={report.evidenceUrl} alt="" className="sr-card-thumb" />
                                        </div>
                                    )}
                                    <div className="sr-card-footer">
                                        {report.pointDeducted > 0 && (
                                            <span className="vr-points-badge">-{report.pointDeducted} điểm</span>
                                        )}
                                        <span className="sr-card-time" style={{ marginLeft: 'auto' }}>{formatTime(report.createdAt)}</span>
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
                        <select value={itemsPerPage} onChange={(e) => setItemsPerPage(Number(e.target.value))}>
                            <option value={10}>10</option>
                            <option value={20}>20</option>
                            <option value={50}>50</option>
                        </select>
                    </div>
                    {totalPages > 1 && (
                        <div className="cio-pagination-right">
                            <button onClick={() => setCurrentPage(prev => Math.max(1, prev - 1))} disabled={currentPage === 1} className="cio-page-btn">&lt;</button>
                            <div className="cio-page-numbers">
                                {getPageNumbers().map((page, idx) => (
                                    page === '...' ? (
                                        <span key={`ellipsis-${idx}`} className="cio-page-ellipsis">...</span>
                                    ) : (
                                        <button key={page} onClick={() => setCurrentPage(page)} className={`cio-page-btn ${currentPage === page ? 'active' : ''}`}>{page}</button>
                                    )
                                ))}
                            </div>
                            <button onClick={() => setCurrentPage(prev => Math.min(totalPages, prev + 1))} disabled={currentPage === totalPages} className="cio-page-btn">&gt;</button>
                        </div>
                    )}
                </div>
            </div>

            {/* ========== MODAL ========== */}
            {selectedReport && (
                <div className="sr-modal-overlay" onClick={() => setSelectedReport(null)}>
                    <div className="sr-modal" onClick={(e) => e.stopPropagation()}>
                        <div className="sr-modal-header">
                            <h2>Chi tiết báo cáo vi phạm</h2>
                            <button className="sr-modal-close" onClick={() => setSelectedReport(null)}>&times;</button>
                        </div>
                        <div className="sr-modal-body">
                            {/* Reporter */}
                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Người báo cáo</div>
                                <div className="sr-student-cell">
                                    {renderAvatar(selectedReport.reporterAvatar, selectedReport.reporterName)}
                                    <div>
                                        <div className="sr-student-name">{selectedReport.reporterName}</div>
                                        <div className="sr-student-code">{selectedReport.reporterCode}</div>
                                    </div>
                                </div>
                            </div>

                            {/* Violator */}
                            {selectedReport.violatorName && (
                                <div className="sr-modal-section">
                                    <div className="sr-modal-label">Người vi phạm</div>
                                    <div className="sr-student-cell">
                                        {renderAvatar(selectedReport.violatorAvatar, selectedReport.violatorName)}
                                        <div>
                                            <div className="sr-student-name">{selectedReport.violatorName}</div>
                                            <div className="sr-student-code">{selectedReport.violatorCode}</div>
                                        </div>
                                    </div>
                                </div>
                            )}

                            {/* Type */}
                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Loại vi phạm</div>
                                <div className="sr-modal-value">
                                    {VIOLATION_TYPE_LABELS[selectedReport.violationType] || selectedReport.violationType}
                                </div>
                            </div>

                            {/* Location */}
                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Vị trí</div>
                                <div className="sr-modal-value">{getLocation(selectedReport)}</div>
                            </div>

                            {/* Status */}
                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Trạng thái</div>
                                {renderStatus(selectedReport.status)}
                            </div>

                            {/* Description */}
                            {selectedReport.description && (
                                <div className="sr-modal-section">
                                    <div className="sr-modal-label">Mô tả</div>
                                    <div className="sr-modal-value">{selectedReport.description}</div>
                                </div>
                            )}

                            {/* Evidence */}
                            {selectedReport.evidenceUrl && (
                                <div className="sr-modal-section">
                                    <div className="sr-modal-label">Bằng chứng</div>
                                    <div className="sr-modal-images">
                                        <img
                                            src={selectedReport.evidenceUrl}
                                            alt=""
                                            className="sr-modal-thumb"
                                            onClick={() => setLightboxImage(selectedReport.evidenceUrl)}
                                        />
                                    </div>
                                </div>
                            )}

                            {/* Time */}
                            <div className="sr-modal-section">
                                <div className="sr-modal-label">Thời gian báo cáo</div>
                                <div className="sr-modal-value">{formatTime(selectedReport.createdAt)}</div>
                            </div>

                            {/* Verified by */}
                            {selectedReport.verifiedByName && (
                                <div className="sr-modal-section">
                                    <div className="sr-modal-label">Xử lý bởi</div>
                                    <div className="sr-modal-value">
                                        {selectedReport.verifiedByName} - {formatTime(selectedReport.verifiedAt)}
                                    </div>
                                </div>
                            )}

                            {/* Points */}
                            {selectedReport.pointDeducted > 0 && (
                                <div className="sr-modal-section">
                                    <div className="sr-modal-label">Điểm trừ</div>
                                    <span className="vr-points-badge" style={{ fontSize: 14 }}>
                                        -{selectedReport.pointDeducted} điểm
                                    </span>
                                </div>
                            )}
                        </div>

                        <div className="sr-modal-footer">
                            {selectedReport.status === "PENDING" && (
                                <>
                                    <button
                                        className="sr-modal-btn primary"
                                        onClick={() => handleVerify(selectedReport.id)}
                                        disabled={submitting}
                                    >
                                        {submitting ? "Đang xử lý..." : "Xác minh"}
                                    </button>
                                    <button
                                        className="sr-modal-btn ghost"
                                        onClick={() => handleReject(selectedReport.id)}
                                        disabled={submitting}
                                        style={{ borderColor: '#ef4444', color: '#ef4444' }}
                                    >
                                        {submitting ? "Đang xử lý..." : "Từ chối"}
                                    </button>
                                </>
                            )}
                            <button className="sr-modal-btn ghost" onClick={() => setSelectedReport(null)}>
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

            {showColumnMenu && (
                <div style={{ position: 'fixed', inset: 0, zIndex: 50 }} onClick={() => setShowColumnMenu(false)} />
            )}
        </div>
    );
}

export default ViolationManage;