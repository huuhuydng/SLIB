import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Plus,
  Pin,
  Pencil,
  Search,
  Trash2,
  EyeOff,
  Eye,
  ArrowUpDown,
  ArrowUp,
  ArrowDown,
  Filter,
  X,
  SlidersHorizontal,
  Loader2,
} from 'lucide-react';
import { useToast } from '../../../components/common/ToastProvider';
import { useConfirm } from '../../../components/common/ConfirmDialog';
import {
  deleteNewBook,
  getAllNewBooksForAdmin,
  toggleNewBookActive,
  toggleNewBookPin,
  batchDeleteNewBooks,
} from '../../../services/librarian/newBookService';
import '../../../styles/librarian/librarian-shared.css';
import '../../../styles/librarian/CheckInOut.css';
import '../../../styles/librarian/BookingManage.css';
import '../../../styles/librarian/NewBookManage.css';

const STATUS_OPTIONS = [
  { value: '', label: 'Tất cả' },
  { value: 'active', label: 'Đang hiển thị' },
  { value: 'hidden', label: 'Đang ẩn' },
];

const formatDate = (value) => {
  if (!value) return 'Chưa có';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('vi-VN', {
    hour: '2-digit', minute: '2-digit',
    day: '2-digit', month: '2-digit', year: 'numeric',
  });
};

const NewBookManage = () => {
  const navigate = useNavigate();
  const toast = useToast();
  const { confirm } = useConfirm();

  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  // Sort & Filter & Pagination
  const [sortConfig, setSortConfig] = useState({ column: null, direction: null });
  const [columnFilters, setColumnFilters] = useState({ title: '', author: '', category: '', status: '', arrivalDate: '' });
  const [activeFilterCol, setActiveFilterCol] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [visibleColumns, setVisibleColumns] = useState({
    title: true, author: true, category: true, status: true, arrivalDate: true, actions: true,
  });
  const [showColumnMenu, setShowColumnMenu] = useState(false);
  const filterRef = useRef(null);

  // Selection for batch delete
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [deleting, setDeleting] = useState(false);

  const loadBooks = async () => {
    try {
      setLoading(true);
      const data = await getAllNewBooksForAdmin();
      setBooks(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error(error);
      toast.error('Không thể tải danh sách sách mới');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadBooks(); }, []);

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (filterRef.current && !filterRef.current.contains(e.target)) {
        setActiveFilterCol(null);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const getStatusKey = (book) => book.isActive ? 'active' : 'hidden';
  const getStatusLabel = (book) => book.isActive ? 'Đang hiển thị' : 'Đang ẩn';

  const getBookValue = (book, column) => {
    switch (column) {
      case 'title': return book.title || '';
      case 'author': return book.author || '';
      case 'category': return book.category || '';
      case 'status': return getStatusKey(book);
      case 'arrivalDate': return book.arrivalDate || '';
      default: return '';
    }
  };

  // Sort handler
  const handleSort = (column) => {
    setSortConfig((prev) => {
      if (prev.column === column) {
        if (prev.direction === 'asc') return { column, direction: 'desc' };
        if (prev.direction === 'desc') return { column: null, direction: null };
      }
      return { column, direction: 'asc' };
    });
  };

  const handleFilterChange = (column, value) => {
    setColumnFilters((prev) => ({ ...prev, [column]: value }));
    setCurrentPage(1);
  };

  const clearColumnFilter = (column) => {
    setColumnFilters((prev) => ({ ...prev, [column]: '' }));
  };

  // Filtered + Sorted data
  const filteredBooks = useMemo(() => {
    let result = [...books];

    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      result = result.filter((book) =>
        [book.title, book.author, book.publisher, book.category]
          .filter(Boolean)
          .some((v) => v.toLowerCase().includes(q))
      );
    }

    Object.entries(columnFilters).forEach(([col, val]) => {
      if (!val) return;
      if (col === 'status') {
        result = result.filter((book) => getStatusKey(book) === val);
      } else {
        const v = val.toLowerCase();
        result = result.filter((book) => getBookValue(book, col).toLowerCase().includes(v));
      }
    });

    if (sortConfig.column) {
      result.sort((a, b) => {
        let aVal = getBookValue(a, sortConfig.column);
        let bVal = getBookValue(b, sortConfig.column);
        if (sortConfig.column === 'arrivalDate') {
          aVal = new Date(aVal || 0).getTime();
          bVal = new Date(bVal || 0).getTime();
        } else {
          aVal = String(aVal).toLowerCase();
          bVal = String(bVal).toLowerCase();
        }
        if (aVal < bVal) return sortConfig.direction === 'asc' ? -1 : 1;
        if (aVal > bVal) return sortConfig.direction === 'asc' ? 1 : -1;
        return 0;
      });
    }

    return result;
  }, [books, searchQuery, columnFilters, sortConfig]);

  // Pagination
  const totalPages = Math.ceil(filteredBooks.length / pageSize) || 1;
  const paginatedBooks = filteredBooks.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  const getPageNumbers = () => {
    const pages = [];
    const max = 5;
    let start = Math.max(1, currentPage - Math.floor(max / 2));
    let end = Math.min(totalPages, start + max - 1);
    if (end - start < max - 1) start = Math.max(1, end - max + 1);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  };

  const handleDelete = async (book) => {
    const ok = await confirm({
      title: 'Xoá sách mới',
      message: `Bạn có chắc muốn xoá "${book.title}" khỏi danh sách quảng bá không?`,
      variant: 'danger',
      confirmText: 'Xoá',
    });
    if (!ok) return;

    try {
      await deleteNewBook(book.id);
      toast.success('Đã xoá sách mới');
      loadBooks();
    } catch (error) {
      console.error(error);
      toast.error('Không thể xoá sách mới');
    }
  };

  const handleToggleActive = async (book, event) => {
    event.stopPropagation();
    try {
      await toggleNewBookActive(book.id);
      toast.success(book.isActive ? 'Đã ẩn sách mới' : 'Đã hiển thị sách mới');
      loadBooks();
    } catch (error) {
      console.error(error);
      toast.error('Không thể cập nhật trạng thái hiển thị');
    }
  };

  const handleTogglePin = async (book, event) => {
    event.stopPropagation();
    try {
      await toggleNewBookPin(book.id);
      toast.success(book.isPinned ? 'Đã bỏ ghim sách mới' : 'Đã ghim sách mới');
      loadBooks();
    } catch (error) {
      console.error(error);
      toast.error('Không thể ghim hoặc bỏ ghim sách mới');
    }
  };

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
                setActiveFilterCol((prev) => prev === column ? null : column);
              }}
              title="Lọc"
            >
              <Filter size={13} className={hasFilter ? 'cio-filter-active' : ''} />
            </button>
          </div>
          {activeFilterCol === column && (
            <div className="cio-filter-dropdown" ref={filterRef} onClick={(e) => e.stopPropagation()}>
              {column === 'status' ? (
                <div className="cio-filter-options">
                  {STATUS_OPTIONS.map((opt) => (
                    <label key={opt.value} className="cio-filter-option">
                      <input
                        type="radio"
                        name="status-filter"
                        checked={columnFilters.status === opt.value}
                        onChange={() => { handleFilterChange('status', opt.value); setActiveFilterCol(null); }}
                      />
                      {opt.label}
                    </label>
                  ))}
                </div>
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

  // Selection logic
  const toggleSelect = (id) => {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const toggleSelectAll = () => {
    if (selectedIds.size === paginatedBooks.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(paginatedBooks.map(b => b.id)));
    }
  };

  const isAllSelected = paginatedBooks.length > 0 && selectedIds.size === paginatedBooks.length;

  const handleDeleteBatch = async () => {
    if (selectedIds.size === 0) return;
    const ok = await confirm({
      title: 'Xoá sách mới',
      message: `Bạn có chắc muốn xoá ${selectedIds.size} sách mới đã chọn?`,
      variant: 'danger',
      confirmText: 'Xoá',
      cancelText: 'Huỷ',
    });
    if (!ok) return;
    setDeleting(true);
    try {
      await batchDeleteNewBooks(Array.from(selectedIds));
      toast.success(`Đã xoá ${selectedIds.size} sách mới thành công.`);
      setSelectedIds(new Set());
      loadBooks();
    } catch (err) {
      toast.error('Không thể xoá sách mới: ' + (err.message || 'Lỗi không xác định'));
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="lib-container">
      <div className="nt-fade-in">
        <div className="lib-page-title">
          <h1>QUẢN LÝ SÁCH MỚI</h1>
        </div>

        <div className="lib-panel">
          {/* Toolbar */}
          <div className="cio-toolbar">
            <div className="lib-search">
              <Search size={16} className="lib-search-icon" />
              <input
                type="text"
                placeholder="Tìm theo tên sách, tác giả, thể loại..."
                value={searchQuery}
                onChange={(e) => { setSearchQuery(e.target.value); setCurrentPage(1); }}
              />
            </div>
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
                    { key: 'title', label: 'Tên sách' },
                    { key: 'author', label: 'Tác giả' },
                    { key: 'category', label: 'Thể loại' },
                    { key: 'status', label: 'Trạng thái' },
                    { key: 'arrivalDate', label: 'Ngày thêm' },
                    { key: 'actions', label: 'Thao tác' },
                  ].map((col) => (
                    <label key={col.key} className="cio-column-menu-item">
                      <input
                        type="checkbox"
                        checked={visibleColumns[col.key]}
                        onChange={() => setVisibleColumns((prev) => ({ ...prev, [col.key]: !prev[col.key] }))}
                        style={{ accentColor: '#FF751F' }}
                      />
                      {col.label}
                    </label>
                  ))}
                </div>
              )}
            </div>

            {/* Batch delete */}
            {selectedIds.size > 0 && (
              <button className="sr-delete-btn" onClick={handleDeleteBatch} disabled={deleting}>
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
              Tổng số <strong>{filteredBooks.length}</strong> kết quả
            </span>
            <button
              className="lib-btn primary"
              style={{ marginLeft: 'auto' }}
              onClick={() => navigate('/librarian/new-books/create')}
            >
              <Plus size={16} /> Thêm sách mới
            </button>
          </div>

          {/* Table */}
          {loading ? (
            <div className="sm-loading">
              <Loader2 size={28} className="sm-spinner" />
              <span>Đang tải...</span>
            </div>
          ) : (
            <div className="bm-table-wrapper">
              <table className="bm-table">
                <thead>
                  <tr>
                    <th className="sr-checkbox-col">
                      <input type="checkbox" checked={isAllSelected} onChange={toggleSelectAll} style={{ accentColor: '#FF751F' }} />
                    </th>
                    {visibleColumns.title && renderColumnHeader('title', 'Tên sách')}
                    {visibleColumns.author && renderColumnHeader('author', 'Tác giả')}
                    {visibleColumns.category && renderColumnHeader('category', 'Thể loại')}
                    {visibleColumns.status && renderColumnHeader('status', 'Trạng thái', true)}
                    {visibleColumns.arrivalDate && renderColumnHeader('arrivalDate', 'Ngày thêm', true)}
                    {visibleColumns.actions && <th className="center"><span className="cio-th-label">Thao tác</span></th>}
                  </tr>
                </thead>
                <tbody>
                  {paginatedBooks.length === 0 ? (
                    <tr>
                      <td colSpan={visibleColumnCount} className="bm-table-empty-cell">
                        {searchQuery ? `Không tìm thấy kết quả cho "${searchQuery}"` : 'Chưa có sách mới nào.'}
                      </td>
                    </tr>
                  ) : (
                    paginatedBooks.map((book) => (
                      <tr
                        key={book.id}
                        className={`bm-table-row${selectedIds.has(book.id) ? ' selected' : ''}`}
                        onClick={() => navigate(`/librarian/new-books/edit/${book.id}`)}
                      >
                        <td className="sr-checkbox-col" onClick={(e) => e.stopPropagation()}>
                          <input type="checkbox" checked={selectedIds.has(book.id)} onChange={() => toggleSelect(book.id)} style={{ accentColor: '#FF751F' }} />
                        </td>
                        {visibleColumns.title && (
                          <td>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                              {book.coverUrl && (
                                <img
                                  src={book.coverUrl}
                                  alt={book.title}
                                  style={{
                                    width: 36, height: 48,
                                    objectFit: 'cover',
                                    borderRadius: 6,
                                    flexShrink: 0,
                                  }}
                                />
                              )}
                              <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                                <span style={{ fontWeight: 500, color: '#1e293b' }}>{book.title}</span>
                                {book.isPinned && <span className="nt-pin-badge">Ghim</span>}
                              </div>
                            </div>
                          </td>
                        )}
                        {visibleColumns.author && (
                          <td style={{ color: '#64748b' }}>{book.author || 'Chưa có'}</td>
                        )}
                        {visibleColumns.category && (
                          <td>
                            <span className="nt-category-tag">{book.category || 'Sách mới'}</span>
                          </td>
                        )}
                        {visibleColumns.status && (
                          <td className="center">
                            <span className="sr-status-text">
                              <span className="sr-status-dot" style={{ background: book.isActive ? '#22c55e' : '#94a3b8' }} />
                              {getStatusLabel(book)}
                            </span>
                          </td>
                        )}
                        {visibleColumns.arrivalDate && (
                          <td className="center" style={{ color: '#64748b', fontSize: 13 }}>
                            {formatDate(book.arrivalDate)}
                          </td>
                        )}
                        {visibleColumns.actions && (
                          <td className="center">
                            <div style={{ display: 'flex', gap: 8, justifyContent: 'center' }}>
                              <button
                                className="cio-th-btn"
                                style={{ width: 32, height: 32, color: book.isPinned ? '#92400e' : '#64748b' }}
                                onClick={(e) => handleTogglePin(book, e)}
                                title={book.isPinned ? 'Bỏ ghim' : 'Ghim'}
                              >
                                <Pin size={15} />
                              </button>
                              <button
                                className="cio-th-btn"
                                style={{ width: 32, height: 32, color: book.isActive ? '#64748b' : '#059669' }}
                                onClick={(e) => handleToggleActive(book, e)}
                                title={book.isActive ? 'Ẩn' : 'Hiện'}
                              >
                                {book.isActive ? <EyeOff size={15} /> : <Eye size={15} />}
                              </button>
                              <button
                                className="cio-th-btn"
                                style={{ width: 32, height: 32, color: '#059669' }}
                                onClick={(e) => { e.stopPropagation(); navigate(`/librarian/new-books/edit/${book.id}`); }}
                              >
                                <Pencil size={15} />
                              </button>
                              <button
                                className="cio-th-btn"
                                style={{ width: 32, height: 32, color: '#dc2626' }}
                                onClick={(e) => { e.stopPropagation(); handleDelete(book); }}
                              >
                                <Trash2 size={15} />
                              </button>
                            </div>
                          </td>
                        )}
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          )}

          {/* Pagination */}
          <div className="cio-pagination">
            <div className="cio-page-size">
              <span>Hiển thị</span>
              <select value={pageSize} onChange={(e) => { setPageSize(Number(e.target.value)); setCurrentPage(1); }}>
                <option value={10}>10</option>
                <option value={20}>20</option>
                <option value={50}>50</option>
              </select>
              <span>dòng</span>
            </div>
            <div className="cio-pagination-right">
              <button className="cio-page-btn" disabled={currentPage <= 1} onClick={() => setCurrentPage(1)}>«</button>
              <button className="cio-page-btn" disabled={currentPage <= 1} onClick={() => setCurrentPage((p) => p - 1)}>‹</button>
              <div className="cio-page-numbers">
                {getPageNumbers().map((p) => (
                  <button key={p} className={`cio-page-btn${p === currentPage ? ' active' : ''}`} onClick={() => setCurrentPage(p)}>{p}</button>
                ))}
              </div>
              <button className="cio-page-btn" disabled={currentPage >= totalPages} onClick={() => setCurrentPage((p) => p + 1)}>›</button>
              <button className="cio-page-btn" disabled={currentPage >= totalPages} onClick={() => setCurrentPage(totalPages)}>»</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default NewBookManage;
