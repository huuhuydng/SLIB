import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ArrowDown, ArrowUp, ArrowUpDown, CheckCircle2, Filter, LayoutGrid, LayoutList, Loader2, RefreshCw, Search, SlidersHorizontal, Trash2, Wrench, X, XCircle } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import { useToast } from '../../../components/common/ToastProvider';
import { useConfirm } from '../../../components/common/ConfirmDialog';
import "../../../styles/librarian/librarian-shared.css";
import "../../../styles/librarian/CheckInOut.css";
import "../../../styles/librarian/SeatStatusReportManage.css";

import { API_BASE_URL } from '../../../config/apiConfig';

const API_BASE = `${API_BASE_URL}/slib/seat-status-reports`;

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
  { value: "RESOLVED", label: "Đã xử lý" },
  { value: "REJECTED", label: "Từ chối" },
];

const ISSUE_LABELS = {
  BROKEN: "Hỏng hóc",
  DIRTY: "Bẩn",
  MISSING_EQUIPMENT: "Thiếu thiết bị",
  OTHER: "Khác",
};

const STATUS_COLORS = {
  PENDING: "#f59e0b",
  VERIFIED: "#2563eb",
  RESOLVED: "#16a34a",
  REJECTED: "#ef4444",
};

const COLUMN_OPTIONS = [
  { key: "reporter", label: "Người gửi" },
  { key: "issueType", label: "Loại sự cố" },
  { key: "location", label: "Vị trí" },
  { key: "status", label: "Trạng thái" },
  { key: "createdAt", label: "Thời gian gửi" },
];

const VALID_STATUS = new Set(Object.keys(STATUS_LABELS));

const normalizeStatus = (value) => (value && VALID_STATUS.has(value) ? value : "");

function SeatStatusReportManage() {
  const toast = useToast();
  const { confirm } = useConfirm();
  const [searchParams, setSearchParams] = useSearchParams();
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");
  const [selectedReport, setSelectedReport] = useState(null);
  const [lightboxImage, setLightboxImage] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [erroredAvatars, setErroredAvatars] = useState(new Set());
  const [statusFilter, setStatusFilter] = useState(() => normalizeStatus(searchParams.get("status")));
  const [viewMode, setViewMode] = useState("table");
  const [submittingAction, setSubmittingAction] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);
  const [sortConfig, setSortConfig] = useState({ column: null, direction: null });
  const [columnFilters, setColumnFilters] = useState({
    reporter: "",
    issueType: "",
    location: "",
    createdAt: "",
  });
  const [activeFilterCol, setActiveFilterCol] = useState(null);
  const [visibleColumns, setVisibleColumns] = useState({
    reporter: true,
    issueType: true,
    location: true,
    status: true,
    createdAt: true,
  });
  const [showColumnMenu, setShowColumnMenu] = useState(false);
  const filterRef = useRef(null);
  const columnMenuRef = useRef(null);
  const syncingStatusRef = useRef(false);

  // Selection for batch delete
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [deleting, setDeleting] = useState(false);

  const getToken = () => sessionStorage.getItem("librarian_token") || localStorage.getItem("librarian_token");

  const getIssueLabel = useCallback((issueType) => ISSUE_LABELS[issueType] || issueType || "Không xác định", []);

  const formatTime = useCallback((value) => {
    if (!value) return "—";
    return new Date(value).toLocaleString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  }, []);

  const getLocation = useCallback((report) => {
    const segments = [];
    if (report.seatCode) segments.push(`Ghế ${report.seatCode}`);
    if (report.zoneName) segments.push(report.zoneName);
    if (report.areaName) segments.push(report.areaName);
    return segments.join(" - ") || "—";
  }, []);

  const getInitial = useCallback((name) => {
    if (!name) return "?";
    return name
      .split(" ")
      .map((part) => part[0])
      .slice(-2)
      .join("")
      .toUpperCase();
  }, []);

  const renderAvatar = useCallback(
    (avatarUrl, name) => {
      const displayName = name || "Người gửi";
      if (avatarUrl && !erroredAvatars.has(avatarUrl)) {
        return (
          <img
            src={avatarUrl}
            alt=""
            className="sr-avatar"
            onError={() => setErroredAvatars((previous) => new Set(previous).add(avatarUrl))}
          />
        );
      }
      return <div className="sr-avatar-placeholder">{getInitial(displayName)}</div>;
    },
    [erroredAvatars, getInitial]
  );

  const renderStatus = useCallback((status) => {
    const tone = status?.toLowerCase() || "unknown";
    return (
      <span className={`sr-status-text ssr-status-text ssr-status-text--${tone}`}>
        <span className="sr-status-dot" style={{ background: STATUS_COLORS[status] || "#94a3b8" }} />
        {STATUS_LABELS[status] || status || "Không xác định"}
      </span>
    );
  }, []);

  const fetchReports = useCallback(async () => {
    setLoading(true);
    setErrorMessage("");
    try {
      const token = getToken();
      const res = await fetch(API_BASE, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) {
        throw new Error("Không thể tải danh sách báo cáo tình trạng ghế.");
      }
      const data = await res.json();
      setReports(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error("Error fetching seat status reports:", error);
      setErrorMessage(error.message || "Không thể tải dữ liệu.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchReports();
  }, [fetchReports]);

  useEffect(() => {
    const nextStatus = normalizeStatus(searchParams.get("status"));

    setStatusFilter((previous) => {
      if (previous === nextStatus) {
        return previous;
      }
      return nextStatus;
    });

    if (syncingStatusRef.current) {
      syncingStatusRef.current = false;
    }
  }, [searchParams]);

  useEffect(() => {
    const nextParams = new URLSearchParams(searchParams);
    if (statusFilter) {
      nextParams.set("status", statusFilter);
    } else {
      nextParams.delete("status");
    }

    if (nextParams.toString() !== searchParams.toString()) {
      syncingStatusRef.current = true;
      setSearchParams(nextParams, { replace: true });
    }
  }, [searchParams, setSearchParams, statusFilter]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (filterRef.current && !filterRef.current.contains(event.target)) {
        setActiveFilterCol(null);
      }
      if (columnMenuRef.current && !columnMenuRef.current.contains(event.target)) {
        setShowColumnMenu(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  useEffect(() => {
    if (!selectedReport) return;
    const freshReport = reports.find((report) => report.id === selectedReport.id);
    if (freshReport) {
      setSelectedReport(freshReport);
    }
  }, [reports, selectedReport]);

  const summary = useMemo(() => {
    const counts = reports.reduce(
      (accumulator, report) => {
        accumulator.total += 1;
        if (accumulator[report.status] !== undefined) {
          accumulator[report.status] += 1;
        }
        return accumulator;
      },
      { total: 0, PENDING: 0, VERIFIED: 0, RESOLVED: 0, REJECTED: 0 }
    );

    return counts;
  }, [reports]);

  const getReportValue = useCallback(
    (report, column) => {
      switch (column) {
        case "reporter":
          return `${report.reporterName || ""} ${report.reporterCode || ""}`.trim();
        case "issueType":
          return getIssueLabel(report.issueType);
        case "location":
          return getLocation(report);
        case "status":
          return STATUS_LABELS[report.status] || report.status || "";
        case "createdAt":
          return report.createdAt || "";
        default:
          return "";
      }
    },
    [getIssueLabel, getLocation]
  );

  const filteredReports = useMemo(() => {
    let list = [...reports];
    const query = searchTerm.trim().toLowerCase();

    if (query) {
      list = list.filter((report) =>
        [
          report.seatCode,
          report.reporterName,
          report.reporterCode,
          report.areaName,
          report.zoneName,
          report.description,
          getIssueLabel(report.issueType),
          STATUS_LABELS[report.status],
        ]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(query))
      );
    }

    if (statusFilter) {
      list = list.filter((report) => report.status === statusFilter);
    }

    Object.entries(columnFilters).forEach(([column, filterValue]) => {
      if (!filterValue) return;
      const keyword = filterValue.toLowerCase();

      if (column === "reporter") {
        list = list.filter(
          (report) =>
            (report.reporterName || "").toLowerCase().includes(keyword) ||
            (report.reporterCode || "").toLowerCase().includes(keyword)
        );
        return;
      }

      if (column === "issueType") {
        list = list.filter((report) => getIssueLabel(report.issueType).toLowerCase().includes(keyword));
        return;
      }

      if (column === "location") {
        list = list.filter((report) => getLocation(report).toLowerCase().includes(keyword));
        return;
      }

      if (column === "createdAt") {
        list = list.filter((report) => formatTime(report.createdAt).toLowerCase().includes(keyword));
      }
    });

    if (sortConfig.column && sortConfig.direction) {
      list.sort((first, second) => {
        let firstValue = getReportValue(first, sortConfig.column);
        let secondValue = getReportValue(second, sortConfig.column);

        if (sortConfig.column === "createdAt") {
          firstValue = firstValue ? new Date(firstValue).getTime() : 0;
          secondValue = secondValue ? new Date(secondValue).getTime() : 0;
        } else {
          firstValue = String(firstValue).toLowerCase();
          secondValue = String(secondValue).toLowerCase();
        }

        if (firstValue < secondValue) return sortConfig.direction === "asc" ? -1 : 1;
        if (firstValue > secondValue) return sortConfig.direction === "asc" ? 1 : -1;
        return 0;
      });
    } else {
      list.sort((first, second) => new Date(second.createdAt) - new Date(first.createdAt));
    }

    return list;
  }, [columnFilters, formatTime, getIssueLabel, getLocation, getReportValue, reports, searchTerm, sortConfig, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredReports.length / itemsPerPage));
  const paginatedReports = filteredReports.slice((currentPage - 1) * itemsPerPage, currentPage * itemsPerPage);

  useEffect(() => {
    setCurrentPage(1);
  }, [columnFilters, itemsPerPage, searchTerm, sortConfig, statusFilter, viewMode]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  const handleSort = (column) => {
    setSortConfig((previous) => {
      if (previous.column !== column) return { column, direction: "asc" };
      if (previous.direction === "asc") return { column, direction: "desc" };
      return { column: null, direction: null };
    });
  };

  const handleFilterChange = (column, value) => {
    setColumnFilters((previous) => ({ ...previous, [column]: value }));
  };

  const clearColumnFilter = (column) => {
    setColumnFilters((previous) => ({ ...previous, [column]: "" }));
    setActiveFilterCol(null);
  };

  const handleToggleColumn = (column) => {
    setVisibleColumns((previous) => {
      const visibleCount = Object.values(previous).filter(Boolean).length;
      if (visibleCount === 1 && previous[column]) {
        return previous;
      }
      return { ...previous, [column]: !previous[column] };
    });
  };

  const getPageNumbers = () => {
    if (totalPages <= 7) return Array.from({ length: totalPages }, (_, index) => index + 1);

    const pages = [];
    if (currentPage <= 4) {
      for (let index = 1; index <= 5; index += 1) pages.push(index);
      pages.push("...");
      pages.push(totalPages);
      return pages;
    }

    if (currentPage >= totalPages - 3) {
      pages.push(1);
      pages.push("...");
      for (let index = totalPages - 4; index <= totalPages; index += 1) pages.push(index);
      return pages;
    }

    pages.push(1);
    pages.push("...");
    for (let index = currentPage - 1; index <= currentPage + 1; index += 1) pages.push(index);
    pages.push("...");
    pages.push(totalPages);
    return pages;
  };

  const renderSortIcon = (column) => {
    if (sortConfig.column === column) {
      if (sortConfig.direction === "asc") return <ArrowUp size={13} />;
      if (sortConfig.direction === "desc") return <ArrowDown size={13} />;
    }
    return <ArrowUpDown size={13} />;
  };

  const renderColumnHeader = (column, label) => {
    const hasFilter = column === "status" ? Boolean(statusFilter) : Boolean(columnFilters[column]);

    return (
      <th key={column}>
        <div className="cio-th-content">
          <span className="cio-th-label">{label}</span>
          <div className="cio-th-actions">
            <button
              className={`cio-th-btn${sortConfig.column === column ? " active" : ""}`}
              onClick={(event) => {
                event.stopPropagation();
                handleSort(column);
              }}
              title="Sắp xếp"
            >
              {renderSortIcon(column)}
            </button>
            <button
              className={`cio-th-btn${hasFilter ? " active" : ""}${activeFilterCol === column ? " open" : ""}`}
              onClick={(event) => {
                event.stopPropagation();
                setActiveFilterCol((previous) => (previous === column ? null : column));
              }}
              title="Lọc"
            >
              <Filter size={13} className={hasFilter ? "cio-filter-active" : ""} />
            </button>
          </div>

          {activeFilterCol === column && (
            <div className="cio-filter-dropdown" ref={filterRef} onClick={(event) => event.stopPropagation()}>
              {column === "status" ? (
                <select
                  value={statusFilter}
                  onChange={(event) => {
                    setStatusFilter(event.target.value);
                    setActiveFilterCol(null);
                  }}
                  className="cio-filter-input"
                  autoFocus
                >
                  {STATUS_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              ) : (
                <>
                  <input
                    type="text"
                    className="cio-filter-input"
                    placeholder={`Lọc ${label.toLowerCase()}...`}
                    value={columnFilters[column]}
                    onChange={(event) => handleFilterChange(column, event.target.value)}
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

  const ACTION_SUCCESS_MESSAGES = {
    verify: "Đã xác minh báo cáo tình trạng ghế thành công.",
    reject: "Đã từ chối báo cáo tình trạng ghế.",
    resolve: "Đã đánh dấu báo cáo là đã xử lý xong.",
  };

  const runAction = async (reportId, action) => {
    setSubmittingAction(`${reportId}:${action}`);
    setErrorMessage("");

    try {
      const token = getToken();
      const res = await fetch(`${API_BASE}/${reportId}/${action}`, {
        method: "PUT",
        headers: { Authorization: `Bearer ${token}` },
      });

      if (!res.ok) {
        throw new Error(`Không thể ${action === "verify" ? "xác minh" : action === "reject" ? "từ chối" : "đánh dấu đã xử lý"} báo cáo.`);
      }

      const updatedReport = await res.json();
      setReports((previous) => previous.map((report) => (report.id === updatedReport.id ? updatedReport : report)));
      setSelectedReport(updatedReport);
      toast.success(ACTION_SUCCESS_MESSAGES[action] || "Cập nhật trạng thái báo cáo thành công.");
      await fetchReports();
    } catch (error) {
      console.error(`Error running ${action}:`, error);
      setErrorMessage(error.message || "Không thể cập nhật trạng thái báo cáo.");
      toast.error("Lỗi: " + (error.message || "Không thể cập nhật trạng thái báo cáo."));
    } finally {
      setSubmittingAction("");
    }
  };

  const activeFilterCount = Object.values(columnFilters).filter(Boolean).length + (statusFilter ? 1 : 0);
  const visibleColumnCount = Math.max(1, Object.values(visibleColumns).filter(Boolean).length) + 1;

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

  const handleDeleteBatch = async () => {
    if (selectedIds.size === 0) return;
    const ok = await confirm({
      title: 'Xoá báo cáo',
      message: `Bạn có chắc muốn xoá ${selectedIds.size} báo cáo đã chọn?`,
      variant: 'danger',
      confirmText: 'Xoá',
      cancelText: 'Huỷ',
    });
    if (!ok) return;
    setDeleting(true);
    try {
      const token = getToken();
      const res = await fetch(`${API_BASE}/batch`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ ids: Array.from(selectedIds) }),
      });
      if (res.ok) {
        toast.success(`Đã xoá ${selectedIds.size} báo cáo thành công.`);
        setSelectedIds(new Set());
        fetchReports();
      } else {
        toast.error('Không thể xoá báo cáo.');
      }
    } catch (err) {
      toast.error('Lỗi: ' + err.message);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="lib-container">
      <div className="lib-page-title">
        <div>
          <h1>BÁO CÁO TÌNH TRẠNG GHẾ</h1>
        </div>

        <button className="cio-column-toggle ssr-refresh-btn" onClick={fetchReports} disabled={loading}>
          <RefreshCw size={15} className={loading ? "ssr-spin" : ""} />
          Làm mới
        </button>
      </div>

      <div className="lib-panel">
        <div className="cio-toolbar sr-toolbar ssr-toolbar">
          <div className="ssr-toolbar-left">
            <div className="lib-search">
              <Search size={16} className="lib-search-icon" />
              <input
                type="text"
                placeholder="Tìm theo ghế, người gửi, khu vực, mô tả..."
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
              />
            </div>

            <div className="sr-view-toggle">
              <button
                className={`sr-view-btn${viewMode === "table" ? " active" : ""}`}
                onClick={() => setViewMode("table")}
                title="Dạng bảng"
              >
                <LayoutList size={16} />
              </button>
              <button
                className={`sr-view-btn${viewMode === "card" ? " active" : ""}`}
                onClick={() => setViewMode("card")}
                title="Dạng thẻ"
              >
                <LayoutGrid size={16} />
              </button>
            </div>

            {viewMode === "table" && (
              <div className="ssr-column-menu-wrap" ref={columnMenuRef}>
                <button className="cio-column-toggle" onClick={() => setShowColumnMenu((previous) => !previous)}>
                  <SlidersHorizontal size={14} />
                  Hiển thị cột
                </button>

                {showColumnMenu && (
                  <div className="cio-column-menu">
                    {COLUMN_OPTIONS.map((column) => (
                      <label key={column.key} className="cio-column-menu-item">
                        <input
                          type="checkbox"
                          checked={visibleColumns[column.key]}
                          onChange={() => handleToggleColumn(column.key)}
                          style={{ accentColor: "#FF751F" }}
                        />
                        {column.label}
                      </label>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>

          <div className="cio-toolbar-right ssr-toolbar-right">
            {selectedIds.size > 0 && (
              <button className="sr-delete-btn" onClick={handleDeleteBatch} disabled={deleting}>
                <Trash2 size={14} />
                {deleting ? "Đang xoá..." : `Xoá (${selectedIds.size})`}
              </button>
            )}
            <span className="cio-result-count">
              {activeFilterCount > 0 && <span className="cio-active-filters">{activeFilterCount} bộ lọc | </span>}
              Tổng số <strong>{filteredReports.length}</strong> kết quả
            </span>
          </div>
        </div>

        {errorMessage && <div className="ssr-alert">{errorMessage}</div>}

        {loading ? (
          <div className="ssr-loading-state">
            <Loader2 size={26} className="ssr-spin" />
            <span>Đang tải dữ liệu báo cáo...</span>
          </div>
        ) : viewMode === "table" ? (
          <div className="sr-table-wrapper">
            <table className="sr-table ssr-table">
              <thead>
                <tr>
                  <th className="sr-checkbox-col">
                    <input type="checkbox" checked={isAllSelected} onChange={toggleSelectAll} style={{ accentColor: '#FF751F' }} />
                  </th>
                  {visibleColumns.reporter && renderColumnHeader("reporter", "Người gửi")}
                  {visibleColumns.issueType && renderColumnHeader("issueType", "Loại sự cố")}
                  {visibleColumns.location && renderColumnHeader("location", "Vị trí")}
                  {visibleColumns.status && renderColumnHeader("status", "Trạng thái")}
                  {visibleColumns.createdAt && renderColumnHeader("createdAt", "Thời gian gửi")}
                </tr>
              </thead>
              <tbody>
                {paginatedReports.length === 0 ? (
                  <tr>
                    <td colSpan={visibleColumnCount} className="sr-table-empty-cell">
                      {searchTerm || activeFilterCount > 0
                        ? "Không tìm thấy báo cáo phù hợp với bộ lọc hiện tại."
                        : "Chưa có báo cáo tình trạng ghế nào."}
                    </td>
                  </tr>
                ) : (
                  paginatedReports.map((report) => (
                    <tr key={report.id} className={`sr-table-row${selectedIds.has(report.id) ? ' selected' : ''}`} onClick={() => setSelectedReport(report)}>
                      <td className="sr-checkbox-col" onClick={(e) => e.stopPropagation()}>
                        <input type="checkbox" checked={selectedIds.has(report.id)} onChange={() => toggleSelect(report.id)} style={{ accentColor: '#FF751F' }} />
                      </td>
                      {visibleColumns.reporter && (
                        <td>
                          <div className="sr-student-cell">
                            {renderAvatar(report.reporterAvatar, report.reporterName)}
                            <div>
                              <div className="sr-student-name">{report.reporterName || "Chưa xác định"}</div>
                              <div className="sr-student-code">{report.reporterCode || "Không có mã sinh viên"}</div>
                            </div>
                          </div>
                        </td>
                      )}

                      {visibleColumns.issueType && (
                        <td>
                          <div className="ssr-issue-cell">
                            <span className="ssr-issue-pill">{getIssueLabel(report.issueType)}</span>
                            <div className="ssr-seat-meta">{report.seatCode ? `Ghế ${report.seatCode}` : "Chưa có mã ghế"}</div>
                          </div>
                        </td>
                      )}

                      {visibleColumns.location && (
                        <td>
                          <div className="ssr-location-cell">
                            <div>{getLocation(report)}</div>
                            {report.description && <div className="ssr-muted-line">{report.description}</div>}
                          </div>
                        </td>
                      )}

                      {visibleColumns.status && <td>{renderStatus(report.status)}</td>}

                      {visibleColumns.createdAt && (
                        <td className="sr-date-cell">
                          <div>{formatTime(report.createdAt)}</div>
                          <div className="ssr-muted-line">
                            {report.verifiedAt ? `Xác minh: ${formatTime(report.verifiedAt)}` : "Chưa xác minh"}
                          </div>
                        </td>
                      )}
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        ) : paginatedReports.length === 0 ? (
          <div className="lib-empty ssr-empty-state">
            <h3>Chưa có báo cáo phù hợp</h3>
            <p>Hãy thay đổi từ khóa tìm kiếm hoặc bộ lọc trạng thái để xem thêm kết quả.</p>
          </div>
        ) : (
          <div className="sr-card-grid ssr-card-grid">
            {paginatedReports.map((report) => (
              <div key={report.id} className="sr-card ssr-card" onClick={() => setSelectedReport(report)}>
                <div className="sr-card-header ssr-card-header">
                  <div>
                    <div className="ssr-card-seat">{report.seatCode ? `Ghế ${report.seatCode}` : "Chưa có mã ghế"}</div>
                    <div className="ssr-card-location">{getLocation(report)}</div>
                  </div>
                  {renderStatus(report.status)}
                </div>

                <div className="sr-student-cell ssr-card-reporter">
                  {renderAvatar(report.reporterAvatar, report.reporterName)}
                  <div>
                    <div className="sr-student-name">{report.reporterName || "Chưa xác định"}</div>
                    <div className="sr-student-code">{report.reporterCode || "Không có mã sinh viên"}</div>
                  </div>
                </div>

                <div className="ssr-card-tags">
                  <span className="ssr-issue-pill">{getIssueLabel(report.issueType)}</span>
                </div>

                <div className="sr-card-desc">{report.description || "Không có mô tả chi tiết."}</div>

                {report.imageUrl && (
                  <div className="sr-card-images">
                    <img
                      src={report.imageUrl}
                      alt="Báo cáo tình trạng ghế"
                      className="sr-card-thumb"
                      onClick={(event) => {
                        event.stopPropagation();
                        setLightboxImage(report.imageUrl);
                      }}
                    />
                  </div>
                )}

                <div className="sr-card-footer ssr-card-footer">
                  <span className="ssr-card-time">Gửi lúc {formatTime(report.createdAt)}</span>
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="cio-pagination">
          <div className="cio-page-size">
            <span>Số hàng mỗi trang:</span>
            <select value={itemsPerPage} onChange={(event) => setItemsPerPage(Number(event.target.value))}>
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
            </select>
          </div>

          {totalPages > 1 && (
            <div className="cio-pagination-right">
              <button
                onClick={() => setCurrentPage((previous) => Math.max(1, previous - 1))}
                disabled={currentPage === 1}
                className="cio-page-btn"
              >
                &lt;
              </button>

              <div className="cio-page-numbers">
                {getPageNumbers().map((page, index) =>
                  page === "..." ? (
                    <span key={`ellipsis-${index}`} className="cio-page-ellipsis">
                      ...
                    </span>
                  ) : (
                    <button
                      key={page}
                      onClick={() => setCurrentPage(page)}
                      className={`cio-page-btn ${currentPage === page ? "active" : ""}`}
                    >
                      {page}
                    </button>
                  )
                )}
              </div>

              <button
                onClick={() => setCurrentPage((previous) => Math.min(totalPages, previous + 1))}
                disabled={currentPage === totalPages}
                className="cio-page-btn"
              >
                &gt;
              </button>
            </div>
          )}
        </div>
      </div>

      {selectedReport && (
        <div className="sr-modal-overlay" onClick={() => setSelectedReport(null)}>
          <div className="sr-modal ssr-modal" onClick={(event) => event.stopPropagation()}>
            <div className="sr-modal-header">
              <div>
                <h2>Chi tiết báo cáo tình trạng ghế</h2>
                <p className="ssr-modal-subtitle">{selectedReport.seatCode ? `Ghế ${selectedReport.seatCode}` : "Chưa có mã ghế"}</p>
              </div>
              <button className="sr-modal-close" onClick={() => setSelectedReport(null)}>
                <X size={18} />
              </button>
            </div>

            <div className="sr-modal-body">
              <div className="ssr-modal-banner">
                <div className="ssr-modal-banner-main">
                  {renderStatus(selectedReport.status)}
                  <span className="ssr-issue-pill">{getIssueLabel(selectedReport.issueType)}</span>
                </div>
                <div className="ssr-modal-banner-time">Gửi lúc {formatTime(selectedReport.createdAt)}</div>
              </div>

              <div className="ssr-modal-grid">
                <div className="sr-modal-section">
                  <div className="sr-modal-label">Người gửi</div>
                  <div className="sr-student-cell">
                    {renderAvatar(selectedReport.reporterAvatar, selectedReport.reporterName)}
                    <div>
                      <div className="sr-student-name">{selectedReport.reporterName || "Chưa xác định"}</div>
                      <div className="sr-student-code">{selectedReport.reporterCode || "Không có mã sinh viên"}</div>
                    </div>
                  </div>
                </div>

                <div className="sr-modal-section">
                  <div className="sr-modal-label">Vị trí</div>
                  <div className="sr-modal-value">{getLocation(selectedReport)}</div>
                </div>

                <div className="sr-modal-section">
                  <div className="sr-modal-label">Thời gian xác minh</div>
                  <div className="sr-modal-value">{formatTime(selectedReport.verifiedAt)}</div>
                  {selectedReport.verifiedByName && <div className="sr-resolver-info">Bởi {selectedReport.verifiedByName}</div>}
                </div>

                <div className="sr-modal-section">
                  <div className="sr-modal-label">Thời gian xử lý</div>
                  <div className="sr-modal-value">{formatTime(selectedReport.resolvedAt)}</div>
                  {selectedReport.resolvedByName && <div className="sr-resolver-info">Bởi {selectedReport.resolvedByName}</div>}
                </div>
              </div>

              <div className="sr-modal-section">
                <div className="sr-modal-label">Mô tả</div>
                <div className="sr-modal-value">{selectedReport.description || "Không có mô tả chi tiết."}</div>
              </div>

              {selectedReport.imageUrl && (
                <div className="sr-modal-section">
                  <div className="sr-modal-label">Hình ảnh đính kèm</div>
                  <div className="sr-modal-images">
                    <img
                      src={selectedReport.imageUrl}
                      alt="Báo cáo tình trạng ghế"
                      className="sr-modal-thumb ssr-modal-thumb"
                      onClick={() => setLightboxImage(selectedReport.imageUrl)}
                    />
                  </div>
                </div>
              )}
            </div>

            <div className="sr-modal-footer">
              <button className="sr-modal-btn ghost" onClick={() => setSelectedReport(null)}>
                Đóng
              </button>

              {selectedReport.status === "PENDING" && (
                <>
                  <button
                    className="sr-modal-btn ghost ssr-modal-btn-danger"
                    onClick={() => runAction(selectedReport.id, "reject")}
                    disabled={Boolean(submittingAction)}
                  >
                    <XCircle size={15} />
                    {submittingAction === `${selectedReport.id}:reject` ? "Đang xử lý..." : "Từ chối"}
                  </button>
                  <button
                    className="sr-modal-btn primary ssr-modal-btn-icon"
                    onClick={() => runAction(selectedReport.id, "verify")}
                    disabled={Boolean(submittingAction)}
                  >
                    <CheckCircle2 size={15} />
                    {submittingAction === `${selectedReport.id}:verify` ? "Đang xử lý..." : "Xác minh"}
                  </button>
                </>
              )}

              {selectedReport.status === "VERIFIED" && (
                <button
                  className="sr-modal-btn primary ssr-modal-btn-icon"
                  onClick={() => runAction(selectedReport.id, "resolve")}
                  disabled={Boolean(submittingAction)}
                >
                  <Wrench size={15} />
                  {submittingAction === `${selectedReport.id}:resolve` ? "Đang xử lý..." : "Đánh dấu đã xử lý"}
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {lightboxImage && (
        <div className="lib-lightbox" onClick={() => setLightboxImage(null)}>
          <img src={lightboxImage} alt="Phóng to hình ảnh báo cáo" />
        </div>
      )}
    </div>
  );
}

export default SeatStatusReportManage;
