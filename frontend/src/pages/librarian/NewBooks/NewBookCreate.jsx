import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Download, ImageUp, Save, Sparkles, X } from 'lucide-react';
import { useToast } from '../../../components/common/ToastProvider';
import {
  createNewBook,
  getNewBookDetailForAdmin,
  previewNewBookFromUrl,
  uploadNewBookCover,
  updateNewBook,
} from '../../../services/librarian/newBookService';
import '../../../styles/librarian/NewsCreate.css';
import '../../../styles/librarian/NewBookManage.css';

const emptyForm = {
  title: '',
  author: '',
  isbn: '',
  coverUrl: '',
  description: '',
  category: '',
  publishYear: '',
  arrivalDate: new Date().toISOString().slice(0, 10),
  isActive: true,
  isPinned: false,
  sourceUrl: '',
  publisher: '',
};

const NewBookCreate = () => {
  const navigate = useNavigate();
  const toast = useToast();
  const { id } = useParams();
  const isEditMode = Boolean(id);

  const [formData, setFormData] = useState(emptyForm);
  const [opacUrl, setOpacUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [importing, setImporting] = useState(false);
  const [uploadingCover, setUploadingCover] = useState(false);
  const coverInputRef = useRef(null);

  useEffect(() => {
    if (!isEditMode) return;
    const loadDetail = async () => {
      try {
        setLoading(true);
        const detail = await getNewBookDetailForAdmin(id);
        setFormData({
          title: detail.title || '',
          author: detail.author || '',
          isbn: detail.isbn || '',
          coverUrl: detail.coverUrl || '',
          description: detail.description || '',
          category: detail.category || '',
          publishYear: detail.publishYear?.toString() || '',
          arrivalDate: detail.arrivalDate || new Date().toISOString().slice(0, 10),
          isActive: detail.isActive ?? true,
          isPinned: detail.isPinned ?? false,
          sourceUrl: detail.sourceUrl || '',
          publisher: detail.publisher || '',
        });
        setOpacUrl(detail.sourceUrl || '');
      } catch (error) {
        console.error(error);
        toast.error('Không thể tải thông tin sách mới');
      } finally {
        setLoading(false);
      }
    };
    loadDetail();
  }, [id, isEditMode, toast]);

  const previewMeta = useMemo(() => {
    if (formData.publishYear) {
      return `${formData.publisher || 'Chưa có NXB'} • ${formData.publishYear}`;
    }
    return formData.publisher || 'Chưa có nhà xuất bản';
  }, [formData.publishYear, formData.publisher]);

  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleImport = async () => {
    if (!opacUrl.trim()) {
      toast.error('Vui lòng nhập link OPAC để lấy dữ liệu');
      return;
    }
    try {
      setImporting(true);
      const preview = await previewNewBookFromUrl(opacUrl.trim());
      setFormData({
        title: preview.title || '',
        author: preview.author || '',
        isbn: preview.isbn || '',
        coverUrl: preview.coverUrl || '',
        description: preview.description || '',
        category: preview.category || '',
        publishYear: preview.publishYear?.toString() || '',
        arrivalDate: preview.arrivalDate || new Date().toISOString().slice(0, 10),
        isActive: preview.isActive ?? true,
        isPinned: preview.isPinned ?? false,
        sourceUrl: preview.sourceUrl || opacUrl.trim(),
        publisher: preview.publisher || '',
      });
      toast.success('Đã lấy dữ liệu từ OPAC. Bạn có thể chỉnh sửa trước khi lưu.');
    } catch (error) {
      console.error(error);
      toast.error(error.response?.data?.message || error.response?.data?.error || 'Không thể lấy dữ liệu từ link OPAC');
    } finally {
      setImporting(false);
    }
  };

  const handleSubmit = async () => {
    if (!formData.title.trim()) {
      toast.error('Tiêu đề sách không được để trống');
      return;
    }

    try {
      setLoading(true);
      const payload = {
        ...formData,
        publishYear: formData.publishYear ? Number(formData.publishYear) : null,
        arrivalDate: formData.arrivalDate || null,
      };

      if (isEditMode) {
        await updateNewBook(id, payload);
        toast.success('Đã cập nhật sách mới');
      } else {
        await createNewBook(payload);
        toast.success('Đã đăng sách mới lên hệ thống');
      }
      navigate('/librarian/new-books');
    } catch (error) {
      console.error(error);
      toast.error(error.response?.data?.message || 'Không thể lưu sách mới');
    } finally {
      setLoading(false);
    }
  };

  const handleSelectLocalCover = () => {
    coverInputRef.current?.click();
  };

  const handleCoverUpload = async (event) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }

    try {
      setUploadingCover(true);
      const uploadedUrl = await uploadNewBookCover(file);
      handleChange('coverUrl', uploadedUrl);
      toast.success('Đã tải ảnh bìa lên hệ thống');
    } catch (error) {
      console.error(error);
      toast.error(error.response?.data?.message || error.response?.data?.error || 'Không thể tải ảnh bìa từ máy tính');
    } finally {
      setUploadingCover(false);
      event.target.value = '';
    }
  };

  return (
    <div className="news-create-container">
      <div className="news-card">
        {/* OPAC Import Section */}
        <div className="news-form-group" style={{
          background: 'linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%)',
          borderRadius: '16px',
          padding: '20px 24px',
          border: '1px solid #fed7aa',
          marginBottom: '32px'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '12px' }}>
            <Sparkles size={18} color="#f97316" />
            <span className="news-form-label" style={{ margin: 0 }}>
              Tự động lấy dữ liệu từ link OPAC
            </span>
          </div>
          <p style={{ margin: '0 0 14px', fontSize: '13px', color: '#78716c' }}>
            Ví dụ: https://library.fpt.edu.vn/NewMaterial/Detail?detail_id=23895
          </p>
          <div style={{ display: 'flex', gap: '12px' }}>
            <input
              type="url"
              className="news-form-input"
              value={opacUrl}
              onChange={(e) => setOpacUrl(e.target.value)}
              placeholder="Dán link OPAC của sách vào đây..."
              style={{ flex: 1 }}
            />
            <button
              type="button"
              className="news-btn news-btn-primary"
              onClick={handleImport}
              disabled={importing}
              style={{ whiteSpace: 'nowrap' }}
            >
              <Download size={16} />
              {importing ? 'Đang lấy...' : 'Lấy thông tin'}
            </button>
          </div>
        </div>

        {/* Main form grid */}
        <div className="news-form-grid">
          {/* Left Column - Form fields */}
          <div className="news-form-left">
            <div className="news-form-group">
              <label className="news-form-label">
                Tiêu đề sách <span style={{ color: '#ef4444' }}>*</span>
              </label>
              <input
                type="text"
                className="news-form-input"
                placeholder="Nhập tiêu đề sách..."
                value={formData.title}
                onChange={(e) => handleChange('title', e.target.value)}
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '20px' }}>
              <div className="news-form-group">
                <label className="news-form-label">Tác giả</label>
                <input
                  type="text"
                  className="news-form-input"
                  placeholder="Tên tác giả..."
                  value={formData.author}
                  onChange={(e) => handleChange('author', e.target.value)}
                />
              </div>
              <div className="news-form-group">
                <label className="news-form-label">ISBN</label>
                <input
                  type="text"
                  className="news-form-input"
                  placeholder="VD: 9781718503267"
                  value={formData.isbn}
                  onChange={(e) => handleChange('isbn', e.target.value)}
                />
              </div>
              <div className="news-form-group">
                <label className="news-form-label">Nhà xuất bản</label>
                <input
                  type="text"
                  className="news-form-input"
                  placeholder="Tên nhà xuất bản..."
                  value={formData.publisher}
                  onChange={(e) => handleChange('publisher', e.target.value)}
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '20px' }}>
              <div className="news-form-group">
                <label className="news-form-label">Năm phát hành</label>
                <input
                  type="number"
                  className="news-form-input"
                  min="1000"
                  max="3000"
                  placeholder="VD: 2024"
                  value={formData.publishYear}
                  onChange={(e) => handleChange('publishYear', e.target.value)}
                />
              </div>
              <div className="news-form-group">
                <label className="news-form-label">Ngày thêm</label>
                <input
                  type="date"
                  className="news-form-input"
                  value={formData.arrivalDate}
                  onChange={(e) => handleChange('arrivalDate', e.target.value)}
                />
              </div>
              <div className="news-form-group">
                <label className="news-form-label">Thể loại / từ khoá</label>
                <input
                  type="text"
                  className="news-form-input"
                  value={formData.category}
                  onChange={(e) => handleChange('category', e.target.value)}
                  placeholder="VD: CNTT, AI..."
                />
              </div>
            </div>

            <div className="news-form-group">
              <label className="news-form-label">Mô tả / lời giới thiệu</label>
              <textarea
                className="news-form-textarea"
                rows="5"
                value={formData.description}
                onChange={(e) => handleChange('description', e.target.value)}
                placeholder="Viết ngắn gọn để thủ thư quảng bá sách trên mobile..."
              />
            </div>

            <div className="news-form-group">
              <label className="news-form-label">Link nguồn OPAC</label>
              <input
                type="url"
                className="news-form-input"
                value={formData.sourceUrl}
                onChange={(e) => handleChange('sourceUrl', e.target.value)}
                placeholder="Link chi tiết từ OPAC..."
              />
            </div>

            <div className="news-form-group">
              <label style={{
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
                cursor: 'pointer',
                fontWeight: 600,
                color: '#374151',
                fontSize: '14px'
              }}>
                <input
                  type="checkbox"
                  checked={formData.isActive}
                  onChange={(e) => handleChange('isActive', e.target.checked)}
                  style={{ width: '20px', height: '20px', accentColor: '#f97316' }}
                />
                Hiển thị sách này trên mobile
              </label>
            </div>

            <div className="news-form-group">
              <label style={{
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
                cursor: 'pointer',
                fontWeight: 600,
                color: '#374151',
                fontSize: '14px'
              }}>
                <input
                  type="checkbox"
                  checked={formData.isPinned}
                  onChange={(e) => handleChange('isPinned', e.target.checked)}
                  style={{ width: '20px', height: '20px', accentColor: '#f97316' }}
                />
                Ghim sách này lên đầu danh sách
              </label>
            </div>
          </div>

          {/* Right Column - Preview & Cover */}
          <div className="news-form-right">
            {/* Cover URL input */}
            <div className="news-form-group">
              <label className="news-form-label">Ảnh bìa (Cover URL)</label>
              <input
                ref={coverInputRef}
                type="file"
                accept="image/*"
                onChange={handleCoverUpload}
                style={{ display: 'none' }}
              />
              {formData.coverUrl ? (
                <div className="news-image-preview">
                  <img src={formData.coverUrl} alt={formData.title || 'Preview cover'} />
                  <button
                    type="button"
                    className="news-remove-image"
                    onClick={() => handleChange('coverUrl', '')}
                  >
                    <X size={18} />
                  </button>
                </div>
              ) : (
                <div className="news-upload-zone" style={{ minHeight: '160px', padding: '32px 24px' }}>
                  <p className="news-upload-text">Chưa có ảnh bìa</p>
                  <p style={{ fontSize: '13px', color: '#94a3b8' }}>Nhập URL bên dưới</p>
                </div>
              )}
              <div className="lib-book-cover-actions">
                <button
                  type="button"
                  className="news-btn news-btn-secondary"
                  onClick={handleSelectLocalCover}
                  disabled={uploadingCover}
                >
                  <ImageUp size={16} />
                  {uploadingCover ? 'Đang tải ảnh...' : 'Chọn ảnh từ máy'}
                </button>
                <span className="lib-book-cover-hint">
                  Ảnh sẽ được tải thẳng lên Cloudinary để lưu ổn định.
                </span>
              </div>
              <input
                type="text"
                className="news-form-input"
                placeholder="Nhập URL ảnh bìa..."
                value={formData.coverUrl}
                onChange={(e) => handleChange('coverUrl', e.target.value)}
                style={{ marginTop: '12px' }}
              />
            </div>

            {/* Mobile Preview */}
            <div className="news-form-group">
              <label className="news-form-label">Xem trước trên mobile</label>
              <div className="lib-book-mobile-preview">
                {formData.coverUrl ? (
                  <img src={formData.coverUrl} alt={formData.title || 'Preview cover'} className="lib-book-preview-image" />
                ) : (
                  <div className="lib-book-preview-image lib-book-preview-image--empty">Chưa có ảnh</div>
                )}
                <div className="lib-book-preview-body">
                  <span className="lib-book-chip">{formData.category || 'Sách mới'}</span>
                  <h3>{formData.title || 'Tiêu đề sách sẽ hiển thị ở đây'}</h3>
                  <p>{formData.author || 'Tác giả'}</p>
                  <p>{previewMeta}</p>
                  <div className="lib-book-preview-divider" />
                  <p className="lib-book-preview-desc">
                    {formData.description || 'Sinh viên sẽ xem mô tả ngắn tại đây và có thể bấm sang OPAC để xem chi tiết hoặc mượn sách.'}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="news-btn-group">
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <button
              type="button"
              className="news-btn news-btn-secondary"
              onClick={() => navigate('/librarian/new-books')}
              disabled={loading}
            >
              <X size={16} />
              Huỷ bỏ
            </button>
          </div>
          <div style={{ display: 'flex', gap: '12px' }}>
            <button
              type="button"
              className="news-btn news-btn-primary"
              onClick={handleSubmit}
              disabled={loading}
            >
              <Save size={16} />
              {loading ? 'Đang lưu...' : isEditMode ? 'Lưu thay đổi' : 'Xác nhận đăng'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default NewBookCreate;
