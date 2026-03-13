import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
  Film,
  Upload,
  Trash2,
  Eye,
  X,
  Play,
  RefreshCw,
  Plus,
  AlertCircle,
  Check,
  Loader,
  Edit,
  Save,
  GripVertical,
  Power
} from 'lucide-react';

import '../../../styles/admin/SlideshowManagement.css';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const getToken = () => sessionStorage.getItem('librarian_token') || localStorage.getItem('librarian_token');
const authHeaders = () => {
  const token = getToken();
  return token ? { 'Authorization': `Bearer ${token}` } : {};
};

const SlideshowManagement = () => {
  const [images, setImages] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [selectedImage, setSelectedImage] = useState(null);
  const [uploadingFiles, setUploadingFiles] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [editingImage, setEditingImage] = useState(null);
  const [editName, setEditName] = useState('');
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [isSelectingAll, setIsSelectingAll] = useState(false);

  // Fetch danh sách ảnh slideshow
  const fetchImages = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      console.log('Fetching images from:', `${API_BASE_URL}/api/slideshow/images`);
      const response = await fetch(`${API_BASE_URL}/api/slideshow/images`, {
        headers: authHeaders(),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();

      if (data.success && Array.isArray(data.images)) {
        // Dữ liệu từ backend DB trả về: { id, url, isActive, name, ... }
        setImages(data.images);
      } else {
        setImages([]);
      }
    } catch (err) {
      console.error('Error fetching images:', err);
      setError('Không thể tải danh sách ảnh. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchImages();
  }, [fetchImages]);

  // Xóa ảnh
  const handleDeleteImage = async (imageObj) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa ảnh này?')) return;

    try {
      // Gọi API xóa theo ID
      const response = await fetch(`${API_BASE_URL}/api/slideshow/images/${imageObj.id}`, {
        method: 'DELETE',
        headers: authHeaders(),
      });

      if (!response.ok) {
        throw new Error('Failed to delete image');
      }

      setImages(images.filter(img => img.id !== imageObj.id));
      setSuccessMsg('Xóa ảnh thành công!');
      setTimeout(() => setSuccessMsg(null), 3000);
    } catch (err) {
      console.error('Error deleting image:', err);
      setError('Không thể xóa ảnh. Vui lòng thử lại.');
      setTimeout(() => setError(null), 3000);
    }
  };

  // Upload ảnh
  const handleUploadImages = async (files) => {
    if (!files || files.length === 0) return;

    const validFiles = Array.from(files).filter(file =>
      file.type.startsWith('image/')
    );

    if (validFiles.length === 0) {
      setError('Vui lòng chọn file ảnh hợp lệ (JPG, PNG, GIF, WebP)');
      setTimeout(() => setError(null), 3000);
      return;
    }

    if (validFiles.length > 10) {
      setError('Tối đa 10 ảnh mỗi lần upload');
      setTimeout(() => setError(null), 3000);
      return;
    }

    // Check file size (max 10MB per file)
    const oversizedFiles = validFiles.filter(f => f.size > 10 * 1024 * 1024);
    if (oversizedFiles.length > 0) {
      setError('Mỗi ảnh tối đa 10MB');
      setTimeout(() => setError(null), 3000);
      return;
    }

    const formData = new FormData();
    validFiles.forEach(file => {
      formData.append('images', file);
    });

    try {
      setUploadingFiles(validFiles.map(f => f.name));

      const response = await fetch(`${API_BASE_URL}/api/slideshow/images`, {
        method: 'POST',
        headers: { ...authHeaders() },
        body: formData,
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || 'Upload failed');
      }

      const data = await response.json();

      if (data.success && data.images) {
        // Backend trả về danh sách ảnh mới đã lưu vào DB
        const newImages = data.images;
        setImages([...images, ...newImages]);
        setSuccessMsg(`Tải lên ${data.images.length} ảnh thành công!`);
        setShowUploadModal(false);
        setUploadingFiles([]);
        // Reset input file value để cho phép chọn lại cùng file nếu cần
        const fileInput = document.getElementById('uploadInput');
        if (fileInput) fileInput.value = '';

        setTimeout(() => setSuccessMsg(null), 3000);
      }
    } catch (err) {
      console.error('Error uploading images:', err);
      setError(err.message || 'Lỗi khi tải ảnh. Vui lòng thử lại.');
      setTimeout(() => setError(null), 3000);
    } finally {
      setUploadingFiles([]);
    }
  };

  // Xử lý đổi tên ảnh
  const handleStartEdit = (imageObj) => {
    setEditingImage(imageObj.id);
    // Ưu tiên lấy tên từ DB (imageObj.name), nếu không có thì parse từ URL
    const url = imageObj.imageUrl || '';
    const fileName = imageObj.imageName || (url ? decodeURIComponent(url.split('/').pop().split('.')[0]) : '');
    setEditName(fileName);
  };

  const handleCancelEdit = () => {
    setEditingImage(null);
    setEditName('');
  };

  const handleRenameImage = async () => {
    if (!editName.trim() || !editingImage) return;

    try {
      // Gọi API đổi tên theo ID
      const response = await fetch(`${API_BASE_URL}/api/slideshow/images/${editingImage}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', ...authHeaders() },
        body: JSON.stringify({
          newName: editName
        }),
      });

      const data = await response.json();
      if (data.success && data.image) {
        setImages(images.map(img =>
          img.id === editingImage
            ? data.image // Cập nhật object ảnh mới từ backend (chứa tên mới)
            : img
        ));
        setSuccessMsg('Đổi tên ảnh thành công!');
        handleCancelEdit();
        setTimeout(() => setSuccessMsg(null), 3000);
      } else {
        throw new Error(data.message || 'Lỗi khi đổi tên');
      }
    } catch (err) {
      setError(err.message || 'Không thể đổi tên ảnh.');
      setTimeout(() => setError(null), 3000);
    }
  };

  // Xử lý Toggle Trạng thái (Active <-> Backup)
  const handleToggleStatus = async (targetImage) => {
    try {
      const newStatus = !targetImage.isActive;

      // Gọi API PATCH để update status, backend sẽ kiểm tra giới hạn 5 ảnh
      const response = await fetch(`${API_BASE_URL}/api/slideshow/images/${targetImage.id}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json', ...authHeaders() },
        body: JSON.stringify({ isActive: newStatus }),
      });
      const data = await response.json();

      if (!response.ok || !data.success) {
        throw new Error(data.message || 'Không thể cập nhật trạng thái');
      }

      // Cập nhật state local nếu thành công
      setImages(images.map(img =>
        img.id === targetImage.id
          ? { ...img, isActive: newStatus }
          : img
      ));

    } catch (err) {
      setError(err.message);
      setTimeout(() => setError(null), 3000);
      return;
    }
  };

  // ========== MULTI-SELECT FUNCTIONS ==========
  const handleToggleSelect = (imageId) => {
    const newSelected = new Set(selectedIds);
    if (newSelected.has(imageId)) {
      newSelected.delete(imageId);
    } else {
      newSelected.add(imageId);
    }
    setSelectedIds(newSelected);
  };

  const handleSelectAll = (shouldSelectAll) => {
    if (shouldSelectAll) {
      setSelectedIds(new Set(filteredImages.map(img => img.id)));
      setIsSelectingAll(true);
    } else {
      setSelectedIds(new Set());
      setIsSelectingAll(false);
    }
  };

  const handleDeleteSelected = async () => {
    if (selectedIds.size === 0) return;
    if (!window.confirm(`Bạn có chắc chắn muốn xóa ${selectedIds.size} ảnh này?`)) return;

    try {
      const deletePromises = Array.from(selectedIds).map(id =>
        fetch(`${API_BASE_URL}/api/slideshow/images/${id}`, { method: 'DELETE', headers: { ...authHeaders() } })
      );

      const results = await Promise.allSettled(deletePromises);
      const failedCount = results.filter(r => r.status === 'rejected').length;

      setImages(images.filter(img => !selectedIds.has(img.id)));
      setSelectedIds(new Set());
      setIsSelectingAll(false);

      if (failedCount > 0) {
        setSuccessMsg(`Xóa thành công ${selectedIds.size - failedCount} ảnh (${failedCount} thất bại)`);
      } else {
        setSuccessMsg(`Xóa thành công ${selectedIds.size} ảnh!`);
      }
      setTimeout(() => setSuccessMsg(null), 3000);
    } catch (err) {
      setError('Lỗi khi xóa hàng loạt ảnh');
      setTimeout(() => setError(null), 3000);
    }
  };

  const handleActivateSelected = async (activateStatus) => {
    if (selectedIds.size === 0) return;

    try {
      const updatePromises = Array.from(selectedIds).map(id =>
        fetch(`${API_BASE_URL}/api/slideshow/images/${id}/status`, {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json', ...authHeaders() },
          body: JSON.stringify({ isActive: activateStatus }),
        })
      );

      const results = await Promise.allSettled(updatePromises);
      const failedCount = results.filter(r => r.status === 'rejected').length;

      setImages(images.map(img =>
        selectedIds.has(img.id)
          ? { ...img, isActive: activateStatus }
          : img
      ));
      setSelectedIds(new Set());
      setIsSelectingAll(false);

      const actionText = activateStatus ? 'kích hoạt' : 'vô hiệu hóa';
      if (failedCount > 0) {
        setSuccessMsg(`${actionText} thành công ${selectedIds.size - failedCount} ảnh (${failedCount} thất bại)`);
      } else {
        setSuccessMsg(`${actionText} thành công ${selectedIds.size} ảnh!`);
      }
      setTimeout(() => setSuccessMsg(null), 3000);
    } catch (err) {
      setError('Lỗi khi cập nhật hàng loạt');
      setTimeout(() => setError(null), 3000);
    }
  };

  // Xử lý Drag & Drop
  const handleDragStart = (e, index) => {
    e.dataTransfer.setData('text/plain', index);
  };

  const handleDragOver = (e) => {
    e.preventDefault(); // Cho phép drop
  };

  const handleDrop = async (e, targetIndex) => {
    e.preventDefault();
    const sourceIndex = parseInt(e.dataTransfer.getData('text/plain'));
    if (sourceIndex === targetIndex) return;

    const newImages = [...images];
    const [movedItem] = newImages.splice(sourceIndex, 1);
    newImages.splice(targetIndex, 0, movedItem);

    setImages(newImages); // Cập nhật UI ngay lập tức

    // Gọi API lưu thứ tự mới
    try {
      await fetch(`${API_BASE_URL}/api/slideshow/reorder`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', ...authHeaders() },
        body: JSON.stringify(newImages.map(img => img.id)),
      });
    } catch (err) {
      console.error('Error reordering:', err);
    }
  };

  const filteredImages = useMemo(() => {
    let result = images;

    if (searchTerm) {
      result = images.filter(img =>
        (img.imageName || img.imageUrl || '').toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    return result; // Backend đã sắp xếp theo displayOrder
  }, [images, searchTerm]);

  const stats = useMemo(() => ({
    total: images.length,
    active: images.filter(img => img.isActive).length,
    backup: images.filter(img => !img.isActive).length,
  }), [images]);

  return (
    <>

      <div className="slideshowManagement__wrapper">
        {/* Page Header */}
        <div className="slideshowManagement__header">
          <div className="slideshowManagement__headerLeft">
            <h1>Quản lý Slideshow</h1>
            <p>Quản lý ảnh hiển thị khi người dùng không tương tác với hệ thống</p>
            {selectedIds.size > 0 && (
              <p style={{ color: '#FF751F', marginTop: '8px', fontWeight: '600' }}>
                {selectedIds.size} ảnh được chọn
              </p>
            )}
          </div>
          <div className="slideshowManagement__headerActions">
            {selectedIds.size > 0 ? (
              <>
                <button
                  onClick={() => handleActivateSelected(true)}
                  className="slideshowManagement__btn slideshowManagement__btnSecondary"
                  title="Kích hoạt các ảnh được chọn"
                >
                  <Power size={18} />
                  Kích hoạt ({selectedIds.size})
                </button>
                <button
                  onClick={() => handleActivateSelected(false)}
                  className="slideshowManagement__btn slideshowManagement__btnSecondary"
                  title="Vô hiệu hóa các ảnh được chọn"
                >
                  <Power size={18} />
                  Vô hiệu hóa ({selectedIds.size})
                </button>
                <button
                  onClick={handleDeleteSelected}
                  className="slideshowManagement__btn slideshowManagement__btnSecondary"
                  title="Xóa các ảnh được chọn"
                  style={{ color: '#C53030', borderColor: '#FED7D7' }}
                >
                  <Trash2 size={18} />
                  Xóa ({selectedIds.size})
                </button>
                <button
                  onClick={() => {
                    setSelectedIds(new Set());
                    setIsSelectingAll(false);
                  }}
                  className="slideshowManagement__btn slideshowManagement__btnSecondary"
                  title="Bỏ chọn tất cả"
                >
                  <X size={18} />
                  Bỏ chọn
                </button>
              </>
            ) : (
              <>
                <button
                  onClick={() => {
                    const windowName = 'slideshowPreview';
                    const windowFeatures = 'width=1200,height=700,left=0,top=0,resizable=yes';
                    const previewWindow = window.open(
                      `${window.location.origin}/librarian/slideshow-preview`,
                      windowName,
                      windowFeatures
                    );
                    if (previewWindow) {
                      previewWindow.focus();
                      console.log('✅ Preview window opened and focused');
                    } else {
                      console.error('❌ Failed to open preview window - popup may be blocked');
                    }
                  }}
                  className="slideshowManagement__btn slideshowManagement__btnSecondary"
                  title="Xem trước slideshow trên màn hình Kiosk"
                >
                  <Play size={18} />
                  Xem trước
                </button>
                <button
                  onClick={fetchImages}
                  className="slideshowManagement__btn slideshowManagement__btnSecondary"
                >
                  <RefreshCw size={18} />
                  Làm mới
                </button>
                <button
                  onClick={() => setShowUploadModal(true)}
                  className="slideshowManagement__btn slideshowManagement__btnPrimary"
                >
                  <Plus size={18} />
                  Tải lên ảnh
                </button>
              </>
            )}
          </div>
        </div>

        {/* Alert Messages */}
        {error && (
          <div className="slideshowManagement__alert slideshowManagement__alert--error">
            <AlertCircle size={20} />
            <span>{error}</span>
            <button
              className="slideshowManagement__alertClose"
              onClick={() => setError(null)}
            >
              <X size={18} />
            </button>
          </div>
        )}

        {successMsg && (
          <div className="slideshowManagement__alert slideshowManagement__alert--success">
            <Check size={20} />
            <span>{successMsg}</span>
            <button
              className="slideshowManagement__alertClose"
              onClick={() => setSuccessMsg(null)}
            >
              <X size={18} />
            </button>
          </div>
        )}

        {/* Stats Cards */}
        <div className="slideshowManagement__statsGrid">
          {[
            { label: 'Tổng ảnh', value: stats.total, icon: Film, color: '#7C3AED', bg: '#F3E8FF', key: 'total' },
            { label: 'Đang hiển thị', value: stats.active, icon: Eye, color: '#059669', bg: '#D1FAE5', key: 'active' },
            { label: 'Đang dự phòng', value: stats.backup, icon: Upload, color: '#64748B', bg: '#F1F5F9', key: 'backup' },
          ].map((stat) => (
            <div key={stat.key} className="slideshowManagement__statCard">
              <div
                className="slideshowManagement__statIcon"
                style={{
                  '--stat-bg': stat.bg,
                  backgroundColor: stat.bg
                }}
              >
                <stat.icon size={22} color={stat.color} />
              </div>
              <div className="slideshowManagement__statContent">
                <h3>{stat.value}</h3>
                <p>{stat.label}</p>
              </div>
            </div>
          ))}
        </div>

        {/* Images Container */}
        <div className="slideshowManagement__container">
          {/* Content Area */}
          {loading ? (
            <div className="slideshowManagement__contentArea slideshowManagement__contentArea--center">
              <div className="slideshowManagement__loading">
                <Loader size={32} className="slideshowManagement__spinner" />
                <p>Đang tải danh sách ảnh...</p>
              </div>
            </div>
          ) : filteredImages.length === 0 ? (
            <div className="slideshowManagement__contentArea slideshowManagement__contentArea--center">
              <div className="slideshowManagement__empty">
                <Film size={48} />
                <p>{images.length === 0 ? 'Chưa có ảnh nào' : 'Không tìm thấy ảnh phù hợp'}</p>
              </div>
            </div>
          ) : (
            <div className="slideshowManagement__contentArea slideshowManagement__contentArea--table">
              <table className="slideshowManagement__table">
                <thead>
                  <tr className="slideshowManagement__tableHeader">
                    <th className="slideshowManagement__th" style={{ width: '40px', textAlign: 'center' }}>
                      <input
                        type="checkbox"
                        checked={isSelectingAll}
                        onChange={(e) => handleSelectAll(e.target.checked)}
                        style={{ cursor: 'pointer', width: '18px', height: '18px' }}
                      />
                    </th>
                    <th className="slideshowManagement__th" style={{ width: '40px' }}></th>
                    <th className="slideshowManagement__th" style={{ width: '80px' }}>STT</th>
                    <th className="slideshowManagement__th" style={{ width: '160px' }}>Hình ảnh</th>
                    <th className="slideshowManagement__th">Thông tin tệp</th>
                    <th className="slideshowManagement__th slideshowManagement__th--center" style={{ width: '180px' }}>Trạng thái</th>
                    <th className="slideshowManagement__th slideshowManagement__th--right" style={{ width: '280px' }}>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredImages.map((imageObj, idx) => (
                    <tr
                      key={imageObj.id}
                      draggable={!searchTerm && !selectedIds.has(imageObj.id)} // Chỉ cho phép kéo thả khi không tìm kiếm và không selected
                      onDragStart={(e) => handleDragStart(e, idx)}
                      onDragOver={handleDragOver}
                      onDrop={(e) => handleDrop(e, idx)}
                      className="slideshowManagement__tr"
                      style={{
                        backgroundColor: selectedIds.has(imageObj.id) ? '#F0F9FF' : 'transparent'
                      }}
                    >
                      <td className="slideshowManagement__td" style={{ textAlign: 'center' }}>
                        <input
                          type="checkbox"
                          checked={selectedIds.has(imageObj.id)}
                          onChange={() => handleToggleSelect(imageObj.id)}
                          style={{ cursor: 'pointer', width: '18px', height: '18px' }}
                        />
                      </td>
                      <td className="slideshowManagement__td">
                        <GripVertical size={16} color="#CBD5E1" style={{ cursor: 'grab' }} />
                      </td>
                      <td className="slideshowManagement__td slideshowManagement__tdText">
                        #{idx + 1}
                      </td>
                      <td className="slideshowManagement__td">
                        <div
                          className="slideshowManagement__tableImageWrapper"
                          onClick={() => setSelectedImage(imageObj.imageUrl)}
                        >
                          <img
                            src={imageObj.imageUrl || ''}
                            alt={`Slide ${idx + 1}`}
                            className="slideshowManagement__tableImage"
                          />
                        </div>
                      </td>
                      <td className="slideshowManagement__td">
                        {editingImage === imageObj.id ? (
                          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                            <input
                              type="text"
                              value={editName}
                              onChange={(e) => setEditName(e.target.value)}
                              className="slideshowManagement__editInput"
                              autoFocus
                              onKeyDown={(e) => {
                                if (e.key === 'Enter') handleRenameImage();
                                if (e.key === 'Escape') handleCancelEdit();
                              }}
                            />
                          </div>
                        ) : (
                          <>
                            <div className="slideshowManagement__fileName">
                              {imageObj.imageName || (imageObj.imageUrl ? decodeURIComponent(imageObj.imageUrl.split('/').pop()) : 'No Name')}
                            </div>
                            <div className="slideshowManagement__fileUrl">
                              {imageObj.imageUrl && imageObj.imageUrl.length > 60 ? imageObj.imageUrl.substring(0, 60) + '...' : (imageObj.imageUrl || '')}
                            </div>
                          </>
                        )}
                      </td>
                      <td className="slideshowManagement__td slideshowManagement__td--center">
                        <label className="slideshowManagement__switch">
                          <input
                            type="checkbox"
                            checked={imageObj.isActive}
                            onChange={() => handleToggleStatus(imageObj)}
                          />
                          <span className="slideshowManagement__slider"></span>
                        </label>
                        <span style={{
                          display: 'block',
                          fontSize: '11px',
                          marginTop: '4px',
                          color: imageObj.isActive ? '#059669' : '#64748B',
                          fontWeight: '600'
                        }}>
                          {imageObj.isActive ? 'Đang hiển thị' : 'Dự phòng'}
                        </span>
                      </td>
                      <td className="slideshowManagement__td slideshowManagement__td--right">
                        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                          {editingImage === imageObj.id ? (
                            <>
                              <button
                                onClick={handleRenameImage}
                                className="slideshowManagement__actionBtn slideshowManagement__actionBtn--save"
                              >
                                <Save size={16} />
                                Lưu
                              </button>
                              <button
                                onClick={handleCancelEdit}
                                className="slideshowManagement__actionBtn slideshowManagement__actionBtn--cancel"
                              >
                                <X size={16} />
                                Hủy
                              </button>
                            </>
                          ) : (
                            <>
                              <button
                                onClick={() => setSelectedImage(imageObj.imageUrl)}
                                className="slideshowManagement__actionBtn slideshowManagement__actionBtn--view"
                              >
                                <Eye size={16} />
                                Xem
                              </button>
                              <button
                                onClick={() => handleStartEdit(imageObj)}
                                className="slideshowManagement__actionBtn slideshowManagement__actionBtn--edit"
                              >
                                <Edit size={16} />
                                Sửa
                              </button>
                              <button
                                onClick={() => handleDeleteImage(imageObj)}
                                className="slideshowManagement__actionBtn slideshowManagement__actionBtn--delete"
                              >
                                <Trash2 size={16} />
                                Xóa
                              </button>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Upload Modal */}
      {showUploadModal && (
        <div className="slideshowManagement__modal">
          <div className="slideshowManagement__modalContent">
            <div className="slideshowManagement__modalHeader">
              <h2>Tải lên ảnh mới</h2>
              <button
                className="slideshowManagement__modalCloseBtn"
                onClick={() => setShowUploadModal(false)}
              >
                <X size={20} />
              </button>
            </div>

            <div className="slideshowManagement__modalBody">
              <div
                className="slideshowManagement__dropZone"
                onDragOver={(e) => {
                  e.preventDefault();
                  e.currentTarget.style.borderColor = '#FF751F';
                  e.currentTarget.style.background = '#FFF7F0';
                }}
                onDragLeave={(e) => {
                  e.currentTarget.style.borderColor = '#E5E7EB';
                  e.currentTarget.style.background = '#FAFBFC';
                }}
                onDrop={(e) => {
                  e.preventDefault();
                  e.currentTarget.style.borderColor = '#E5E7EB';
                  e.currentTarget.style.background = '#FAFBFC';
                  handleUploadImages(e.dataTransfer.files);
                }}
              >
                <Upload size={32} color="#FF751F" />
                <p className="slideshowManagement__dropZoneText">
                  Kéo ảnh vào đây
                </p>
                <p className="slideshowManagement__dropZoneSubtext">
                  hoặc click để chọn
                </p>
                <input
                  type="file"
                  multiple
                  accept="image/*"
                  className="slideshowManagement__fileInput"
                  id="uploadInput"
                  onChange={(e) => handleUploadImages(e.target.files)}
                />
              </div>
              <label htmlFor="uploadInput">
                <button
                  onClick={() => document.getElementById('uploadInput').click()}
                  type="button"
                  className="slideshowManagement__selectBtn"
                >
                  Chọn ảnh từ máy tính
                </button>
              </label>

              <p style={{
                fontSize: '12px',
                color: '#A0AEC0',
                textAlign: 'center',
                marginTop: '16px',
                marginBottom: 0
              }}>
                Hỗ trợ JPG, PNG, GIF, WebP • Tối đa 5MB/ảnh • 10 ảnh/lần
              </p>

              {uploadingFiles.length > 0 && (
                <div className="slideshowManagement__uploadingList">
                  <p className="slideshowManagement__uploadingTitle">Đang upload:</p>
                  {uploadingFiles.map((fileName, idx) => (
                    <div key={idx} className="slideshowManagement__uploadingItem">
                      <Loader size={14} className="slideshowManagement__uploadingItemIcon" />
                      <span>{fileName}</span>
                    </div>
                  ))}
                </div>
              )}

              <div className="slideshowManagement__modalActions">
                <button
                  onClick={() => setShowUploadModal(false)}
                  className="slideshowManagement__modalBtn slideshowManagement__modalBtnSecondary"
                >
                  Hủy
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Image Preview Modal */}
      {selectedImage && (
        <div className="slideshowManagement__previewModal">
          <div className="slideshowManagement__previewContent">
            <button
              className="slideshowManagement__previewCloseBtn"
              onClick={() => setSelectedImage(null)}
            >
              <X size={24} />
            </button>
            <img
              src={selectedImage}
              alt="Preview"
              className="slideshowManagement__previewImage"
            />
          </div>
        </div>
      )}
    </>
  );
};

export default SlideshowManagement;