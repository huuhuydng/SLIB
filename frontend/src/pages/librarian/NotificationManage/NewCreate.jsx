import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeft,
  X,
  Send,
  CloudUpload,
  Bold,
  Italic,
  Underline,
  List,
  Link,
  Image,
  Clock
} from 'lucide-react';
import { Save } from 'lucide-react';
import Header from "../../../components/shared/Header";
import '../../../styles/librarian/NewsCreate.css';
import { handleLogout } from "../../../utils/auth";
import { createNews, updateNews, getNewsDetailForAdmin, getNewsImage } from '../../../services/newsService';
import { createNewsTemplate } from '../../../utils/newsTemplate';

const NewCreate = () => {
  const LRM = '\u200E'; // Anchor to force LTR context inside contentEditable
  const navigate = useNavigate();
  const { id } = useParams(); // Nếu có id thì là edit mode
  const isEditMode = Boolean(id);
  const editorRef = useRef(null);
  const fileInputRef = useRef(null);

  const [formData, setFormData] = useState({
    title: '',
    summary: '',
    content: '',
    imageUrl: '',
    categoryId: null,
    isPublished: false,
    isPinned: false,
    scheduledPublishTime: null
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [showScheduleForm, setShowScheduleForm] = useState(false);

  const hours24 = Array.from({ length: 25 }, (_, i) => String(i).padStart(2, '0')); // 00-24
  const minuteSteps = Array.from({ length: 60 }, (_, i) => String(i).padStart(2, '0')); // 00-59

  const getScheduleDate = () => {
    if (formData.scheduledPublishTime) return formData.scheduledPublishTime.split('T')[0];
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  };

  const getScheduleTime = () => {
    if (formData.scheduledPublishTime) {
      const timePart = formData.scheduledPublishTime.split('T')[1] || '00:00';
      const [hh = '00', mm = '00'] = timePart.split(':');
      return { hh: hh.padStart(2, '0'), mm: mm.padStart(2, '0') };
    }
    return { hh: '00', mm: '00' };
  };

  const addOneDay = (dateStr) => {
    const d = new Date(dateStr);
    d.setDate(d.getDate() + 1);
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  };

  const updateScheduleTime = (newHour, newMinute) => {
    let datePart = getScheduleDate();
    let hh = newHour ?? getScheduleTime().hh;
    const mm = newMinute ?? getScheduleTime().mm;

    // Nếu chọn 24h, chuyển sang 00h ngày hôm sau để lưu hợp lệ
    if (hh === '24') {
      datePart = addOneDay(datePart);
      hh = '00';
    }

    setFormData(prev => ({
      ...prev,
      scheduledPublishTime: `${datePart}T${hh}:${mm}`
    }));
  };

  // Load data nếu là edit mode
  useEffect(() => {
    if (isEditMode) {
      loadNewsData();
    }
  }, [id]);
  useEffect(() => {
    // Đảm bảo editor luôn ở chế độ LTR sau khi mount
    if (editorRef.current) {
      editorRef.current.setAttribute('dir', 'ltr');
      editorRef.current.style.direction = 'ltr';
      editorRef.current.style.unicodeBidi = 'isolate';
      editorRef.current.style.textAlign = 'left';
      // Đặt anchor LRM đầu nội dung để khóa hướng LTR
      editorRef.current.innerHTML = LRM;
      placeCaretAtEnd(editorRef.current);
    }
  }, []);
  useEffect(() => {
    if (editorRef.current && formData.content) {
      // Gắn anchor khi load lại nội dung từ server
      editorRef.current.innerHTML = `${LRM}${formData.content}`;
    }
  }, [formData.content]);
  const loadNewsData = async () => {
    try {
      setLoading(true);
      const data = await getNewsDetailForAdmin(id);
      
      // Load image riêng
      try {
        const imageUrl = await getNewsImage(id);
        data.imageUrl = imageUrl;
        setImagePreview(imageUrl);
      } catch (error) {
        console.warn('Could not load image');
      }
      
      setFormData({
        title: data.title || '',
        summary: data.summary || '',
        content: data.content || '',
        imageUrl: data.imageUrl || '',
        categoryId: data.categoryId || null,
        isPublished: data.isPublished || false,
        isPinned: data.isPinned || false
      });
    } catch (err) {
      setError('Không thể tải thông tin tin tức');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };
  // Editor toolbar functions
  const execCommand = (command, value = null) => {
    document.execCommand(command, false, value);
    editorRef.current?.focus();
  };

  const handleBold = () => execCommand('bold');
  const handleItalic = () => execCommand('italic');
  const handleUnderline = () => execCommand('underline');
  
  const handleList = () => execCommand('insertUnorderedList');
  
  const handleLink = () => {
    const url = prompt('Nhập URL:');
    if (url) execCommand('createLink', url);
  };
  
  const handleImage = () => {
    const url = prompt('Nhập URL hình ảnh:');
    if (url) execCommand('insertImage', url);
  };

  const sanitizeBidi = (html) => {
    // Loại bỏ ký tự điều khiển bidi gây đảo ngược (bao gồm LRM/RLM/ALM)
    return html.replace(/[\u202A-\u202E\u2066-\u2069\u200E\u200F\u061C]/g, '');
  };

  const stripAnchor = (html) => html.replace(new RegExp(`^${LRM}+`), '');

  const placeCaretAtEnd = (el) => {
    if (!el) return;
    const range = document.createRange();
    range.selectNodeContents(el);
    range.collapse(false);
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
  };

  const handleEditorInput = () => {
    if (!editorRef.current) return;

    // Luôn ép LTR để caret đi về bên phải khi gõ
    editorRef.current.setAttribute('dir', 'ltr');
    editorRef.current.style.direction = 'ltr';
    editorRef.current.style.unicodeBidi = 'isolate';
    editorRef.current.style.textAlign = 'left';

    const raw = editorRef.current.innerHTML;
    const cleanHtml = sanitizeBidi(raw);
    const withoutAnchor = stripAnchor(cleanHtml);
    const anchored = `${LRM}${withoutAnchor}`;

    // Nếu DOM đang chứa ký tự bidi, ghi đè lại và đưa caret về cuối để tránh đảo
    if (anchored !== raw) {
      editorRef.current.innerHTML = anchored;
      placeCaretAtEnd(editorRef.current);
    }

    setFormData(prev => ({
      ...prev,
      content: withoutAnchor
    }));
  };

  const handleBeforeInput = (e) => {
    // Ngăn chèn ký tự điều khiển bidi ngay từ đầu
    if (e.data && /[\u202A-\u202E\u2066-\u2069\u200E\u200F\u061C]/.test(e.data)) {
      e.preventDefault();
      return;
    }
    // Bảo đảm anchor luôn tồn tại
    if (editorRef.current && editorRef.current.innerHTML === '') {
      editorRef.current.innerHTML = LRM;
      placeCaretAtEnd(editorRef.current);
    }
  };

  const handlePaste = (e) => {
    if (!editorRef.current) return;
    e.preventDefault();
    const text = e.clipboardData.getData('text/plain') || '';
    const clean = sanitizeBidi(text);
    document.execCommand('insertText', false, clean);
    handleEditorInput();
  };

  // File upload handlers
  const handleFileSelect = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      // Validate file type
      if (!file.type.startsWith('image/')) {
        alert('Vui lòng chọn file ảnh!');
        return;
      }

      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        alert('Kích thước file không được vượt quá 5MB!');
        return;
      }

      // Create preview
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagePreview(reader.result);
        setFormData(prev => ({
          ...prev,
          imageUrl: reader.result // Store base64 for now
        }));
      };
      reader.readAsDataURL(file);
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    
    const file = e.dataTransfer.files[0];
    if (file && file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagePreview(reader.result);
        setFormData(prev => ({
          ...prev,
          imageUrl: reader.result
        }));
      };
      reader.readAsDataURL(file);
    } else {
      alert('Vui lòng chọn file ảnh!');
    }
  };

  const handleRemoveImage = () => {
    setImagePreview(null);
    setFormData(prev => ({
      ...prev,
      imageUrl: ''
    }));
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };
  const handleSubmit = async (isDraft = false) => {
    try {
      setLoading(true);
      setError(null);

      // Validate required fields
      if (!formData.title.trim()) {
        setError('Vui lòng nhập tiêu đề');
        return;
      }
      if (!formData.content.trim()) {
        setError('Vui lòng nhập nội dung');
        return;
      }

      // Tạo HTML content từ template
      const htmlContent = createNewsTemplate({
        title: formData.title,
        summary: formData.summary,
        content: formData.content,
        categoryName: formData.categoryId === '1' ? 'Thông báo' : 
                      formData.categoryId === '2' ? 'Sự kiện' : 'Tin tức',
        imageUrl: formData.imageUrl
      });

      const isDraftFlag = isDraft || !formData.isPublished; // tôn trọng chọn Nháp hoặc nút Lưu nháp
      const hasSchedule = !!formData.scheduledPublishTime;

      const newsData = {
        title: formData.title,
        summary: formData.summary || null,
        content: htmlContent, // Gửi HTML đã format
        imageUrl: formData.imageUrl || null,
        category: formData.categoryId ? { id: parseInt(formData.categoryId) } : null,
        isPublished: !isDraftFlag && !hasSchedule, // nếu chọn Nháp hoặc có lịch tương lai thì không publish ngay
        isPinned: formData.isPinned || false,
        viewCount: 0,
        // Nếu có scheduledPublishTime và không phải draft thì set giờ đăng, còn lại null
        publishedAt: (!isDraftFlag && hasSchedule)
          ? `${formData.scheduledPublishTime}${formData.scheduledPublishTime.length === 16 ? ':00' : ''}`
          : null
      };

      console.log('Sending news data:', newsData);

      if (isEditMode) {
        await updateNews(id, newsData);
      } else {
        await createNews(newsData);
      }

      // Chuyển về trang quản lý notification
      navigate('/notification');
    } catch (err) {
      setError(isEditMode ? 'Không thể cập nhật tin tức' : 'Không thể tạo tin tức mới');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  if (loading && isEditMode) {
    return (
      <>
        <Header searchPlaceholder="Search for anything..." onLogout={handleLogout} />
        <div className="news-loading">
          Đang tải...
        </div>
      </>
    );
  }

  return (
    <>
      <Header searchPlaceholder="Search for anything..." onLogout={handleLogout} />
      
      <div className="news-create-container">
        <div className="news-card">
          {error && (
            <div className="news-error-alert">
              <span>⚠️</span>
              {error}
            </div>
          )}

          <form onSubmit={(e) => e.preventDefault()}>
            <div className="news-form-grid">
              {/* LEFT COLUMN */}
              <div className="col-left">
                <div className="news-form-group">
                  <label className="news-form-label">
                    Tiêu đề bài viết <span style={{ color: 'red' }}>*</span>
                  </label>
                  <input
                    type="text"
                    name="title"
                    className="news-form-input"
                    placeholder="Nhập tiêu đề tin tức..."
                    value={formData.title}
                    onChange={handleInputChange}
                    required
                  />
                </div>

                <div className="news-form-group">
                  <label className="news-form-label">Tóm tắt ngắn</label>
                  <textarea
                    name="summary"
                    className="news-form-textarea"
                    rows="3"
                    placeholder="Mô tả ngắn gọn nội dung hiển thị ở danh sách..."
                    value={formData.summary}
                    onChange={handleInputChange}
                  />
                </div>

                <div className="news-form-group">
                  <label className="news-form-label">Nội dung chi tiết</label>
                  <div className="news-editor-container">
                    <div className="news-editor-toolbar">
                      <button type="button" className="news-tool-btn" onClick={handleBold} title="Bold">
                        <Bold size={14} />
                      </button>
                      <button type="button" className="news-tool-btn" onClick={handleItalic} title="Italic">
                        <Italic size={14} />
                      </button>
                      <button type="button" className="news-tool-btn" onClick={handleUnderline} title="Underline">
                        <Underline size={14} />
                      </button>
                      <div className="news-tool-divider"></div>
                      <button type="button" className="news-tool-btn" onClick={handleList} title="List">
                        <List size={14} />
                      </button>
                      <button type="button" className="news-tool-btn" onClick={handleLink} title="Link">
                        <Link size={14} />
                      </button>
                      <button type="button" className="news-tool-btn" onClick={handleImage} title="Image">
                        <Image size={14} />
                      </button>
                    </div>
                    <div
                      ref={editorRef}
                      className="news-editor-content"
                      contentEditable={true}
                      onBeforeInput={handleBeforeInput}
                      onInput={handleEditorInput}
                      onPaste={handlePaste}
                      onFocus={() => {
                        if (editorRef.current) {
                          editorRef.current.setAttribute('dir', 'ltr');
                          editorRef.current.style.direction = 'ltr';
                          editorRef.current.style.unicodeBidi = 'isolate';
                          editorRef.current.style.textAlign = 'left';
                          placeCaretAtEnd(editorRef.current);
                        }
                      }}
                      data-placeholder="Soạn thảo nội dung tại đây..."
                      dir="ltr"
                      style={{ direction: 'ltr', textAlign: 'left', unicodeBidi: 'isolate' }}
                    />
                  </div>
                </div>
              </div>

              {/* RIGHT COLUMN */}
              <div className="col-right">
                <div className="news-form-group">
                  <label className="news-form-label">Ảnh bìa (Thumbnail)</label>
                  
                  {imagePreview ? (
                    <div className="news-image-preview">
                      <img src={imagePreview} alt="Preview" />
                      <button 
                        type="button" 
                        className="news-remove-image"
                        onClick={handleRemoveImage}
                      >
                        <X size={16} />
                      </button>
                    </div>
                  ) : (
                    <div 
                      className="news-upload-zone"
                      onDragOver={handleDragOver}
                      onDrop={handleDrop}
                      onClick={handleFileSelect}
                    >
                      <CloudUpload size={40} className="news-upload-icon" />
                      <div className="news-upload-text">Kéo thả ảnh vào đây</div>
                      <div className="news-upload-text" style={{ fontSize: '12px', marginTop: '5px' }}>
                        hoặc
                      </div>
                      <button type="button" className="news-upload-btn-mini">
                        Chọn file
                      </button>
                    </div>
                  )}
                  
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    onChange={handleFileChange}
                    style={{ display: 'none' }}
                  />
                  
                  <input
                    type="text"
                    name="imageUrl"
                    className="news-form-input"
                    placeholder="Hoặc nhập URL hình ảnh..."
                    value={imagePreview ? '' : formData.imageUrl}
                    onChange={handleInputChange}
                    disabled={!!imagePreview}
                    style={{ marginTop: '10px' }}
                  />
                </div>

                <div className="news-form-group">
                  <label className="news-form-label">Danh mục</label>
                  <select
                    name="categoryId"
                    className="news-form-select"
                    value={formData.categoryId || ''}
                    onChange={handleInputChange}
                  >
                    <option value="">-- Chọn danh mục --</option>
                    <option value="1">Sự kiện</option>
                    <option value="2">Thông báo quan trọng</option>
                    <option value="3">Sách mới</option>
                    <option value="4">Ưu đãi</option>
                  </select>
                </div>

                <div className="news-form-group">
                  <label className="news-form-label">Ghim tin tức</label>
                  <div className="news-toggle-desc" style={{ marginBottom: '8px' }}>
                    Đưa tin này lên đầu trang chủ
                  </div>
                  <label className="news-checkbox-container">
                    <input
                      type="checkbox"
                      name="isPinned"
                      checked={formData.isPinned}
                      onChange={handleInputChange}
                      className="news-checkbox"
                    />
                  </label>
                </div>

                <div className="news-form-group">
                  <label className="news-form-label">Trạng thái</label>
                  <select
                    name="isPublished"
                    className="news-form-select"
                    value={formData.isPublished ? 'public' : 'draft'}
                    onChange={(e) => setFormData(prev => ({ 
                      ...prev, 
                      isPublished: e.target.value === 'public' 
                    }))}
                  >
                    <option value="draft">Nháp (Draft)</option>
                    <option value="public">Công khai (Public)</option>
                  </select>
                </div>

                <div className="news-form-group">
                  <label className="news-form-label">Hẹn giờ đăng tin</label>
                  <button
                    type="button"
                    className="news-schedule-btn"
                    onClick={() => setShowScheduleForm(!showScheduleForm)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      padding: '10px 15px',
                      borderRadius: '6px',
                      border: '2px solid #2196F3',
                      background: formData.scheduledPublishTime ? '#e3f2fd' : '#fff',
                      color: '#2196F3',
                      cursor: 'pointer',
                      fontWeight: '500',
                      width: '100%',
                      justifyContent: 'center'
                    }}
                  >
                    <Clock size={16} />
                    {formData.scheduledPublishTime 
                      ? `Đã hẹn: ${new Date(formData.scheduledPublishTime).toLocaleString('vi-VN')}`
                      : 'Thiết lập lịch đăng'
                    }
                  </button>

                  {showScheduleForm && (
                    <div className="news-schedule-form" style={{
                      marginTop: '15px',
                      padding: '15px',
                      background: '#f5f5f5',
                      borderRadius: '6px',
                      border: '1px solid #ddd'
                    }}>
                      <div style={{ marginBottom: '12px' }}>
                        <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500', fontSize: '14px' }}>
                          Ngày đăng
                        </label>
                        <input
                          type="date"
                          className="news-form-input"
                          value={formData.scheduledPublishTime ? formData.scheduledPublishTime.split('T')[0] : ''}
                          onChange={(e) => {
                            if (e.target.value) {
                              const currentTime = formData.scheduledPublishTime
                                ? formData.scheduledPublishTime.split('T')[1] || '00:00'
                                : '00:00';
                              setFormData(prev => ({
                                ...prev,
                                scheduledPublishTime: `${e.target.value}T${currentTime.substring(0,5)}`
                              }));
                            }
                          }}
                        />
                      </div>

                      <div style={{ marginBottom: '12px' }}>
                        <label style={{ display: 'block', marginBottom: '6px', fontWeight: '500', fontSize: '14px' }}>
                          Giờ đăng (24h)
                        </label>
                        <div className="news-time-picker">
                          <div className="news-time-column">
                            <div className="news-time-label">Giờ</div>
                            <div className="news-time-grid">
                              {hours24.map(h => {
                                const isActive = getScheduleTime().hh === h;
                                return (
                                  <button
                                    type="button"
                                    key={h}
                                    className={`news-time-chip ${isActive ? 'active' : ''}`}
                                    onClick={() => updateScheduleTime(h, null)}
                                  >
                                    {h}
                                  </button>
                                );
                              })}
                            </div>
                          </div>
                          <div className="news-time-column">
                            <div className="news-time-label">Phút</div>
                            <div className="news-time-grid minutes">
                              {minuteSteps.map(m => {
                                const isActive = getScheduleTime().mm === m;
                                return (
                                  <button
                                    type="button"
                                    key={m}
                                    className={`news-time-chip ${isActive ? 'active' : ''}`}
                                    onClick={() => updateScheduleTime(null, m)}
                                  >
                                    {m}
                                  </button>
                                );
                              })}
                            </div>
                          </div>
                        </div>
                        <div style={{ marginTop: '6px', fontSize: '12px', color: '#6b7280' }}>
                          Chọn khung giờ 00:00 - 23:59 (bước 5 phút)
                        </div>
                      </div>

                      {formData.scheduledPublishTime && (
                        <div style={{
                          padding: '10px',
                          background: '#e8f5e9',
                          borderRadius: '4px',
                          color: '#2e7d32',
                          fontSize: '13px',
                          marginBottom: '10px'
                        }}>
                          ✓ Tin sẽ được đăng tự động vào: {formData.scheduledPublishTime.replace('T', ' ')}
                        </div>
                      )}

                      <button
                        type="button"
                        className="news-btn news-btn-secondary"
                        onClick={() => {
                          setFormData(prev => ({
                            ...prev,
                            scheduledPublishTime: null
                          }));
                          setShowScheduleForm(false);
                        }}
                        style={{ width: '100%', marginTop: '10px' }}
                      >
                        Hủy lịch đăng
                      </button>
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="news-btn-group">
              <button
                type="button"
                className="news-btn news-btn-secondary"
                onClick={() => handleSubmit(true)}
                disabled={loading || !formData.title || !formData.content}
              >
                <Save size={16} />
                Lưu nháp
              </button>
              <button
                type="button"
                className="news-btn news-btn-secondary"
                onClick={() => navigate('/notification')}
                disabled={loading}
              >
                <X size={16} />
                Hủy bỏ
              </button>
              {formData.scheduledPublishTime ? (
                <button
                  type="button"
                  className="news-btn news-btn-primary"
                  onClick={() => handleSubmit(false)}
                  disabled={loading || !formData.title || !formData.content}
                  style={{ background: '#4CAF50' }}
                >
                  <Clock size={16} />
                  {loading ? 'Đang lên lịch...' : 'Lên lịch đăng'}
                </button>
              ) : (
                <button
                  type="button"
                  className="news-btn news-btn-primary"
                  onClick={() => handleSubmit(false)}
                  disabled={loading || !formData.title || !formData.content}
                >
                  <Send size={16} />
                  {loading ? 'Đang đăng...' : 'Đăng tin ngay'}
                </button>
              )}
            </div>
          </form>
        </div>
      </div>
    </>
  );
};

export default NewCreate;
