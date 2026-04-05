import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useToast } from '../../../components/common/ToastProvider';
import { useConfirm } from '../../../components/common/ConfirmDialog';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import {
  ArrowLeft,
  X,
  Send,
  CloudUpload,
  Save,
  Clock,
  Plus,
  Trash2,
  Eye,
  EyeOff,
  Check
} from 'lucide-react';

import TipTapEditor from "../../../components/editor/TipTapEditor";
import '../../../styles/librarian/NewsCreate.css';
import { handleLogout } from "../../../utils/auth";
import {
  createNews,
  updateNews,
  getNewsDetailForAdmin,
  getNewsImage,
  getAllCategories,
  createCategory,
  uploadImage
} from '../../../services/librarian/newsService';
import { sanitizeHtml } from '../../../utils/sanitizeHtml';
import { getApiErrorMessage, normalizeText, validateNewsPayload } from '../../../utils/formValidation';

const NewCreate = () => {
  const toast = useToast();
  const { confirm } = useConfirm();
  const navigate = useNavigate();
  const location = useLocation();
  const { id } = useParams();
  const isEditMode = Boolean(id);
  const fileInputRef = useRef(null);

  const basePath = location.pathname.startsWith('/librarian/news')
    ? '/librarian/news'
    : '/librarian/notification';

  // Form state
  const [formData, setFormData] = useState({
    title: '',
    summary: '',
    content: '',
    imageUrl: '',
    categoryId: '',
    isPinned: false, // Track pin status for edit mode
  });

  // Status: 'draft' | 'publish' | 'schedule'
  const [publishStatus, setPublishStatus] = useState('publish');
  const [scheduleDate, setScheduleDate] = useState('');
  const [scheduleTime, setScheduleTime] = useState('');

  // Categories state
  const [categories, setCategories] = useState([]);

  // UI state
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [uploading, setUploading] = useState(false);

  // Preview mode
  const [showPreview, setShowPreview] = useState(false);

  // Unsaved changes tracking - use ref to store initial data
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const initialFormDataRef = useRef(null);
  const isInitializedRef = useRef(false);

  // Auto-save state
  const [autoSaveStatus, setAutoSaveStatus] = useState(''); // '' | 'saving' | 'saved' | 'error'
  const autoSaveTimerRef = useRef(null);
  const AUTO_SAVE_DELAY = 30000; // 30 seconds

  // Load categories on mount
  useEffect(() => {
    const init = async () => {
      loadCategories();
      if (isEditMode) {
        loadNewsData();
      } else {
        // Load draft from localStorage for new posts
        const savedDraft = localStorage.getItem('news_draft');
        if (savedDraft) {
          try {
            const draft = JSON.parse(savedDraft);
            if (draft && draft.title) {
              const useDraft = await confirm({
                title: 'Khôi phục bản nháp',
                message: 'Phát hiện bản nháp chưa lưu. Bạn có muốn khôi phục?',
                variant: 'warning',
                confirmText: 'Khôi phục',
              });
              if (useDraft) {
                setFormData(draft.formData || {});
                setPublishStatus(draft.publishStatus || 'publish');
                setScheduleDate(draft.scheduleDate || '');
                setScheduleTime(draft.scheduleTime || '');
                if (draft.imagePreview) setImagePreview(draft.imagePreview);
              } else {
                localStorage.removeItem('news_draft');
              }
            }
          } catch (e) {
            localStorage.removeItem('news_draft');
          }
        }
      }
    };
    init();
  }, [id]);

  // Track unsaved changes - only after initial data is set
  useEffect(() => {
    // Skip if not initialized yet
    if (!isInitializedRef.current) return;

    // Compare current data with initial data
    if (initialFormDataRef.current) {
      const currentData = JSON.stringify({
        formData,
        publishStatus,
        scheduleDate,
        scheduleTime
      });
      const initialData = JSON.stringify(initialFormDataRef.current);
      setHasUnsavedChanges(currentData !== initialData);
    }
  }, [formData, publishStatus, scheduleDate, scheduleTime]);

  // Set initial data after loading (for edit mode) or on mount (for new mode)
  useEffect(() => {
    // For edit mode: set initial after loadNewsData completes
    if (isEditMode && formData.title && !isInitializedRef.current) {
      initialFormDataRef.current = {
        formData: { ...formData },
        publishStatus,
        scheduleDate,
        scheduleTime
      };
      isInitializedRef.current = true;
    }
  }, [isEditMode, formData.title, formData, publishStatus, scheduleDate, scheduleTime]);

  // For new mode: set initial on first mount only
  useEffect(() => {
    if (!isEditMode && !isInitializedRef.current) {
      // Use timeout to ensure this runs after initial render
      const timer = setTimeout(() => {
        initialFormDataRef.current = {
          formData: { ...formData },
          publishStatus,
          scheduleDate,
          scheduleTime
        };
        isInitializedRef.current = true;
      }, 100);
      return () => clearTimeout(timer);
    }
  }, []);

  // Warn before leaving with unsaved changes
  useEffect(() => {
    const handleBeforeUnload = (e) => {
      if (hasUnsavedChanges) {
        e.preventDefault();
        e.returnValue = 'Bạn có thay đổi chưa lưu. Bạn có chắc muốn thoát?';
        return e.returnValue;
      }
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [hasUnsavedChanges]);

  // Auto-save draft every 30 seconds (only for new posts)
  useEffect(() => {
    if (!isEditMode && hasUnsavedChanges && formData.title) {
      if (autoSaveTimerRef.current) {
        clearTimeout(autoSaveTimerRef.current);
      }
      autoSaveTimerRef.current = setTimeout(() => {
        saveDraftToLocalStorage();
      }, AUTO_SAVE_DELAY);
    }
    return () => {
      if (autoSaveTimerRef.current) {
        clearTimeout(autoSaveTimerRef.current);
      }
    };
  }, [formData, publishStatus, scheduleDate, scheduleTime, hasUnsavedChanges, isEditMode]);

  // Save draft to localStorage
  const saveDraftToLocalStorage = useCallback(() => {
    if (!isEditMode && formData.title) {
      setAutoSaveStatus('saving');
      try {
        const draft = {
          formData,
          publishStatus,
          scheduleDate,
          scheduleTime,
          imagePreview,
          savedAt: new Date().toISOString()
        };
        localStorage.setItem('news_draft', JSON.stringify(draft));
        setAutoSaveStatus('saved');
        setTimeout(() => setAutoSaveStatus(''), 3000);
      } catch (e) {
        setAutoSaveStatus('error');
      }
    }
  }, [formData, publishStatus, scheduleDate, scheduleTime, imagePreview, isEditMode]);

  // Clear draft after successful submit
  const clearDraft = () => {
    localStorage.removeItem('news_draft');
    setHasUnsavedChanges(false);
  };

  const loadCategories = async () => {
    try {
      const data = await getAllCategories();
      setCategories(data);
    } catch (err) {
      console.error('Error loading categories:', err);
    }
  };

  const loadNewsData = async () => {
    try {
      setLoading(true);
      const data = await getNewsDetailForAdmin(id);
      const imageUrl = await getNewsImage(id);

      setFormData({
        title: data.title || '',
        summary: data.summary || '',
        content: data.content || '',
        imageUrl: imageUrl || '',
        categoryId: data.categoryId?.toString() || '',
        isPinned: data.isPinned || false, // Preserve pin status
      });

      if (imageUrl) {
        setImagePreview(imageUrl);
      }

      // Determine status based on data
      if (!data.isPublished && !data.publishedAt) {
        setPublishStatus('draft');
      } else if (data.publishedAt && new Date(data.publishedAt) > new Date()) {
        setPublishStatus('schedule');
        const dt = new Date(data.publishedAt);
        setScheduleDate(dt.toISOString().split('T')[0]);
        setScheduleTime(dt.toTimeString().slice(0, 5));
      } else {
        setPublishStatus('publish');
      }
    } catch (err) {
      setError('Không thể tải dữ liệu tin tức');
    } finally {
      setLoading(false);
    }
  };

  // Handle image upload via Backend API
  const handleImageUpload = async (file) => {
    try {
      setUploading(true);
      const url = await uploadImage(file);
      setFormData(prev => ({ ...prev, imageUrl: url }));
      setImagePreview(url);
      return url;
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Lỗi upload ảnh'));
      return null;
    } finally {
      setUploading(false);
    }
  };

  // Handle thumbnail file selection
  const handleThumbnailSelect = async (e) => {
    const file = e.target.files[0];
    if (file) {
      await handleImageUpload(file);
    }
  };

  // Remove thumbnail
  const handleRemoveThumbnail = () => {
    setFormData(prev => ({ ...prev, imageUrl: '' }));
    setImagePreview(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  // Generate random color for new categories
  const generateRandomColor = () => {
    const colors = ['#ef4444', '#f97316', '#eab308', '#22c55e', '#14b8a6', '#3b82f6', '#8b5cf6', '#ec4899', '#6366f1', '#06b6d4'];
    return colors[Math.floor(Math.random() * colors.length)];
  };

  // Handle form submit
  const handleSubmit = async () => {
    try {
      setLoading(true);
      setError(null);
      const validationMessage = validateNewsPayload({
        title: formData.title,
        summary: formData.summary,
        imageUrl: formData.imageUrl,
        publishStatus,
        scheduleDate,
        scheduleTime,
        customCategory: formData.customCategory,
      });
      if (validationMessage) {
        setError(validationMessage);
        setLoading(false);
        return;
      }

      let isPublished = false;
      let publishedAt = null;

      if (publishStatus === 'publish') {
        isPublished = true;
        publishedAt = new Date().toISOString();
      } else if (publishStatus === 'schedule') {
        if (!scheduleDate || !scheduleTime) {
          setError('Vui lòng chọn ngày và giờ đăng');
          setLoading(false);
          return;
        }
        isPublished = false;
        publishedAt = `${scheduleDate}T${scheduleTime}:00`;
      }
      // draft: isPublished = false, publishedAt = null

      // Handle category - create new if 'other' is selected
      let categoryId = null;
      if (formData.categoryId === 'other' && formData.customCategory?.trim()) {
        try {
          const newCat = await createCategory(normalizeText(formData.customCategory), generateRandomColor());
          categoryId = newCat.id;
          await loadCategories(); // Refresh categories list
        } catch (err) {
          console.error('Error creating category:', err);
          // If category already exists, try to find it
          const normalizedCustomCategory = normalizeText(formData.customCategory).toLowerCase();
          const existingCat = categories.find(c => c.name.toLowerCase() === normalizedCustomCategory);
          if (existingCat) {
            categoryId = existingCat.id;
          }
        }
      } else if (formData.categoryId && formData.categoryId !== 'other') {
        categoryId = parseInt(formData.categoryId);
      }

      const newsData = {
        title: normalizeText(formData.title),
        summary: normalizeText(formData.summary) || null,
        content: formData.content || null,
        imageUrl: normalizeText(formData.imageUrl) || null,
        categoryId,
        isPublished,
        isPinned: isEditMode ? formData.isPinned : false, // Preserve pin status when editing
        publishedAt
      };

      if (isEditMode) {
        await updateNews(id, newsData);
      } else {
        await createNews(newsData);
      }

      // Clear draft and unsaved changes flag
      clearDraft();
      setHasUnsavedChanges(false);
      navigate(basePath);
    } catch (err) {
      setError(getApiErrorMessage(err, isEditMode ? 'Không thể cập nhật tin tức' : 'Không thể tạo tin tức mới'));
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // Render submit button based on status
  const renderSubmitButton = () => {
    if (publishStatus === 'draft') {
      return (
        <button
          type="button"
          className="news-btn news-btn-primary"
          onClick={handleSubmit}
          disabled={loading || !formData.title.trim()}
        >
          <Save size={16} />
          {loading ? 'Đang lưu...' : 'Lưu nháp'}
        </button>
      );
    } else if (publishStatus === 'schedule') {
      return (
        <button
          type="button"
          className="news-btn news-btn-primary"
          onClick={handleSubmit}
          disabled={loading || !formData.title.trim() || !scheduleDate || !scheduleTime}
        >
          <Clock size={16} />
          {loading ? 'Đang lên lịch...' : 'Lên lịch gửi'}
        </button>
      );
    } else {
      return (
        <button
          type="button"
          className="news-btn news-btn-primary"
          onClick={handleSubmit}
          disabled={loading || !formData.title.trim()}
        >
          <Send size={16} />
          {loading ? 'Đang đăng...' : (isEditMode ? 'Cập nhật' : 'Đăng tin ngay')}
        </button>
      );
    }
  };

  // Get minimum datetime for schedule
  const getMinDate = () => {
    const now = new Date();
    return now.toISOString().split('T')[0];
  };

  return (
    <div className="news-create-container">

      <div className="news-card">
        {error && (
          <div className="news-error-alert">
            <X size={18} />
            {error}
          </div>
        )}

        <div className="news-form-grid">
          {/* Left Column - Main Content */}
          <div className="news-form-left">
            <div className="news-form-group">
              {/* Unsaved indicator - show above title */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                <label className="news-form-label" style={{ margin: 0 }}>
                  Tiêu đề bài viết <span style={{ color: '#ef4444' }}>*</span>
                </label>
                {/* Status indicators */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  {autoSaveStatus === 'saving' && (
                    <span style={{ fontSize: '12px', color: '#64748b', display: 'flex', alignItems: 'center', gap: '4px' }}>
                      💾 Đang lưu...
                    </span>
                  )}
                  {autoSaveStatus === 'saved' && (
                    <span style={{ fontSize: '12px', color: '#16a34a', display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <Check size={14} /> Đã lưu nháp
                    </span>
                  )}
                  {hasUnsavedChanges && !autoSaveStatus && (
                    <span style={{
                      fontSize: '12px',
                      color: '#f59e0b',
                      background: '#fef3c7',
                      padding: '2px 8px',
                      borderRadius: '12px',
                      fontWeight: 500
                    }}>
                      ● Chưa lưu
                    </span>
                  )}
                </div>
              </div>
              <input
                type="text"
                className="news-form-input"
                placeholder="Nhập tiêu đề tin tức..."
                value={formData.title}
                onChange={(e) => setFormData(prev => ({ ...prev, title: e.target.value }))}
              />
            </div>

            <div className="news-form-group">
              <label className="news-form-label">Tóm tắt ngắn</label>
              <textarea
                className="news-form-textarea"
                placeholder="Mô tả ngắn gọn nội dung hiển thị ở danh sách..."
                value={formData.summary}
                onChange={(e) => setFormData(prev => ({ ...prev, summary: e.target.value }))}
                rows={3}
              />
            </div>

            <div className="news-form-group">
              <label className="news-form-label">Nội dung chi tiết</label>
              <TipTapEditor
                content={formData.content}
                onChange={(html) => setFormData(prev => ({ ...prev, content: html }))}
                onImageUpload={handleImageUpload}
              />
            </div>
          </div>

          {/* Right Column - Settings */}
          <div className="news-form-right">
            {/* Thumbnail Upload */}
            <div className="news-form-group">
              <label className="news-form-label">Ảnh bìa</label>
              {imagePreview ? (
                <div className="news-image-preview">
                  <img src={imagePreview} alt="Thumbnail" />
                  <button
                    type="button"
                    className="news-remove-image"
                    onClick={handleRemoveThumbnail}
                  >
                    <X size={18} />
                  </button>
                </div>
              ) : (
                <div
                  className="news-upload-zone"
                  onClick={() => fileInputRef.current?.click()}
                >
                  <CloudUpload size={40} className="news-upload-icon" />
                  <p className="news-upload-text">Kéo thả ảnh vào đây</p>
                  <p className="news-upload-text">hoặc</p>
                  <button type="button" className="news-upload-btn-mini">
                    {uploading ? 'Đang tải...' : 'Chọn file'}
                  </button>
                </div>
              )}
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                style={{ display: 'none' }}
                onChange={handleThumbnailSelect}
              />
              <input
                type="text"
                className="news-form-input"
                placeholder="Hoặc nhập URL hình ảnh..."
                value={formData.imageUrl}
                onChange={(e) => {
                  setFormData(prev => ({ ...prev, imageUrl: e.target.value }));
                  setImagePreview(e.target.value);
                }}
                style={{ marginTop: '12px' }}
              />
            </div>

            {/* Category Selection - Simplified */}
            <div className="news-form-group">
              <label className="news-form-label">Danh mục</label>

              <select
                className="news-form-select"
                value={formData.categoryId}
                onChange={(e) => {
                  const val = e.target.value;
                  if (val === 'other') {
                    setFormData(prev => ({ ...prev, categoryId: 'other' }));
                  } else {
                    setFormData(prev => ({ ...prev, categoryId: val, customCategory: '' }));
                  }
                }}
              >
                <option value="">-- Chọn danh mục --</option>
                {categories.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.name}
                  </option>
                ))}
                <option value="other">Khác...</option>
              </select>

              {/* Custom category input when 'Khác' is selected */}
              {formData.categoryId === 'other' && (
                <input
                  type="text"
                  className="news-form-input"
                  placeholder="Nhập tên danh mục mới..."
                  value={formData.customCategory || ''}
                  onChange={(e) => setFormData(prev => ({ ...prev, customCategory: e.target.value }))}
                  style={{ marginTop: '12px' }}
                />
              )}
            </div>

            {/* Publish Status */}
            <div className="news-form-group">
              <label className="news-form-label">Trạng thái</label>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
                  <input
                    type="radio"
                    name="publishStatus"
                    value="draft"
                    checked={publishStatus === 'draft'}
                    onChange={(e) => setPublishStatus(e.target.value)}
                  />
                  <span>Lưu nháp</span>
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
                  <input
                    type="radio"
                    name="publishStatus"
                    value="publish"
                    checked={publishStatus === 'publish'}
                    onChange={(e) => setPublishStatus(e.target.value)}
                  />
                  <span>Gửi ngay</span>
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
                  <input
                    type="radio"
                    name="publishStatus"
                    value="schedule"
                    checked={publishStatus === 'schedule'}
                    onChange={(e) => setPublishStatus(e.target.value)}
                  />
                  <span>Lên lịch gửi</span>
                </label>
              </div>

              {/* Schedule DateTime Picker */}
              {publishStatus === 'schedule' && (
                <div style={{
                  marginTop: '16px',
                  padding: '16px',
                  background: '#f8fafc',
                  borderRadius: '12px',
                  border: '1px solid #e2e8f0'
                }}>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                    <div>
                      <label style={{ fontSize: '12px', color: '#64748b', fontWeight: 600 }}>Ngày đăng</label>
                      <input
                        type="date"
                        className="news-form-input"
                        value={scheduleDate}
                        min={getMinDate()}
                        onChange={(e) => setScheduleDate(e.target.value)}
                        style={{ marginTop: '6px' }}
                      />
                    </div>
                    <div>
                      <label style={{ fontSize: '12px', color: '#64748b', fontWeight: 600 }}>Giờ đăng</label>
                      <input
                        type="time"
                        className="news-form-input"
                        value={scheduleTime}
                        onChange={(e) => setScheduleTime(e.target.value)}
                        style={{ marginTop: '6px' }}
                      />
                    </div>
                  </div>
                  {scheduleDate && scheduleTime && (
                    <p style={{ marginTop: '12px', fontSize: '13px', color: '#f97316', fontWeight: 500 }}>
                      Tin sẽ được đăng vào: {new Date(`${scheduleDate}T${scheduleTime}`).toLocaleString('vi-VN')}
                    </p>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="news-btn-group">
          {/* Left side - Cancel and Auto-save status */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <button
              type="button"
              className="news-btn news-btn-secondary"
              onClick={async () => {
                if (hasUnsavedChanges) {
                  const leave = await confirm({
                    title: 'Thoát không lưu',
                    message: 'Bạn có thay đổi chưa lưu. Bạn có chắc muốn thoát?',
                    variant: 'warning',
                    confirmText: 'Thoát',
                  });
                  if (!leave) return;
                }
                clearDraft();
                navigate(basePath);
              }}
              disabled={loading}
            >
              <X size={16} />
              Huỷ bỏ
            </button>
          </div>

          {/* Right side - Preview and Submit */}
          <div style={{ display: 'flex', gap: '12px' }}>
            {/* Preview button */}
            <button
              type="button"
              className="news-btn news-btn-outline"
              onClick={() => setShowPreview(true)}
              disabled={!formData.title.trim()}
              style={{
                background: 'white',
                border: '2px solid #e2e8f0',
                color: '#64748b'
              }}
            >
              <Eye size={16} />
              Xem trước
            </button>

            {renderSubmitButton()}
          </div>
        </div>
      </div>

      {/* Preview Modal */}
      {showPreview && (
        <div
          className="news-preview-overlay"
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0,0,0,0.7)',
            zIndex: 1000,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px'
          }}
          onClick={() => setShowPreview(false)}
        >
          <div
            className="news-preview-modal"
            style={{
              background: 'white',
              borderRadius: '16px',
              maxWidth: '800px',
              width: '100%',
              maxHeight: '90vh',
              overflow: 'auto',
              boxShadow: '0 25px 50px rgba(0,0,0,0.25)'
            }}
            onClick={(e) => e.stopPropagation()}
          >
            {/* Preview Header */}
            <div style={{
              padding: '20px 24px',
              borderBottom: '1px solid #e2e8f0',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              background: 'linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%)'
            }}>
              <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 600, color: '#1e293b' }}>
                <Eye size={20} style={{ marginRight: '8px', verticalAlign: 'middle' }} />
                Xem trước bài viết
              </h3>
              <button
                onClick={() => setShowPreview(false)}
                style={{
                  padding: '8px',
                  border: 'none',
                  background: '#f1f5f9',
                  borderRadius: '8px',
                  cursor: 'pointer',
                  color: '#64748b'
                }}
              >
                <X size={20} />
              </button>
            </div>

            {/* Preview Content */}
            <div style={{ padding: '24px' }}>
              {/* Category badge */}
              {formData.categoryId && categories.find(c => c.id.toString() === formData.categoryId) && (
                <span style={{
                  display: 'inline-block',
                  padding: '4px 12px',
                  background: categories.find(c => c.id.toString() === formData.categoryId)?.colorCode || '#f97316',
                  color: 'white',
                  borderRadius: '20px',
                  fontSize: '12px',
                  fontWeight: 600,
                  marginBottom: '12px'
                }}>
                  {categories.find(c => c.id.toString() === formData.categoryId)?.name}
                </span>
              )}

              {/* Title */}
              <h1 style={{
                fontSize: '28px',
                fontWeight: 700,
                color: '#0f172a',
                marginBottom: '12px',
                lineHeight: 1.3
              }}>
                {formData.title || 'Tiêu đề bài viết'}
              </h1>

              {/* Summary */}
              {formData.summary && (
                <p style={{
                  fontSize: '16px',
                  color: '#64748b',
                  marginBottom: '20px',
                  fontStyle: 'italic',
                  lineHeight: 1.6
                }}>
                  {formData.summary}
                </p>
              )}

              {/* Content - images will be displayed as part of HTML content */}
              <div
                className="news-preview-content"
                style={{
                  fontSize: '15px',
                  lineHeight: 1.8,
                  color: '#334155'
                }}
                dangerouslySetInnerHTML={{ __html: sanitizeHtml(formData.content || '<p style="color:#94a3b8">Chưa có nội dung...</p>') }}
              />

              {/* Style for images inside preview content */}
              <style>{`
                .news-preview-content img {
                  max-width: 100%;
                  height: auto;
                  border-radius: 8px;
                  margin: 12px 0;
                }
                .news-preview-content p {
                  margin-bottom: 12px;
                }
              `}</style>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default NewCreate;
