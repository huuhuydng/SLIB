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
  Clock,
  AlignLeft,
  AlignCenter,
  AlignRight,
  ListOrdered,
  Code,
  Strikethrough,
  Type,
  Heading1,
  Heading2,
  Heading3
} from 'lucide-react';
import { Save } from 'lucide-react';
import Header from "../../../components/shared/Header";
import '../../../styles/librarian/NewsCreate.css';
import { handleLogout } from "../../../utils/auth";
import { createNews, updateNews, getNewsDetailForAdmin, getNewsImage } from '../../../services/newsService';
import { createNewsTemplate } from '../../../utils/newsTemplate';
import { API_BASE_URL } from '../../../config/apiConfig';

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
  const [thumbnailFile, setThumbnailFile] = useState(null);
  const [statusMode, setStatusMode] = useState('draft'); // 'public', 'draft', 'scheduled'
  const contentImageInputRef = useRef(null);

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
    contentImageInputRef.current?.click();
  };
  
  const handleContentImageChange = (e) => {
    const file = e.target.files[0];
    if (file && file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onloadend = () => {
        execCommand('insertImage', reader.result);
      };
      reader.readAsDataURL(file);
    }
  };
  
  // More toolbar functions
  const handleStrikethrough = () => execCommand('strikeThrough');
  const handleAlignLeft = () => execCommand('justifyLeft');
  const handleAlignCenter = () => execCommand('justifyCenter');
  const handleAlignRight = () => execCommand('justifyRight');
  const handleOrderedList = () => execCommand('insertOrderedList');
  const handleHeading = (level) => execCommand('formatBlock', `<h${level}>`);
  const handleCode = () => execCommand('formatBlock', '<pre>');
  const handleClearFormat = () => execCommand('removeFormat');

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

      // Store file và create preview
      setThumbnailFile(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagePreview(reader.result);
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
      if (file.size > 5 * 1024 * 1024) {
        alert('Kích thước file không được vượt quá 5MB!');
        return;
      }
      setThumbnailFile(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagePreview(reader.result);
      };
      reader.readAsDataURL(file);
    } else {
      alert('Vui lòng chọn file ảnh!');
    }
  };

  const handleRemoveImage = () => {
    setImagePreview(null);
    setThumbnailFile(null);
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

      let finalImageUrl = formData.imageUrl;
      
      // Upload ảnh lên Cloudinary nếu có file
      if (thumbnailFile) {
        try {
          const formDataToUpload = new FormData();
          formDataToUpload.append('file', thumbnailFile);
          const token = sessionStorage.getItem('librarian_token') || localStorage.getItem('librarian_token');
          
          const uploadResponse = await fetch(`${API_BASE_URL}/slib/cloudinary/upload-news-image`, {
            method: 'POST',
            headers: token ? { Authorization: `Bearer ${token}` } : {},
            body: formDataToUpload
          });
          
          if (!uploadResponse.ok) {
            throw new Error('Upload ảnh thất bại');
          }
          
          finalImageUrl = await uploadResponse.text();
          console.log('✅ Uploaded image URL:', finalImageUrl);
        } catch (uploadErr) {
          setError('Không thể tải ảnh bìa lên hệ thống');
          console.error(uploadErr);
          return;
        }
      }

      // Tạo HTML content từ template
      const htmlContent = createNewsTemplate({
        title: formData.title,
        summary: formData.summary,
        content: formData.content,
        categoryName: formData.categoryId === '1' ? 'Thông báo' : 
                      formData.categoryId === '2' ? 'Sự kiện' : 'Tin tức',
        imageUrl: finalImageUrl
      });

      const isDraftFlag = isDraft || statusMode === 'draft';
      const hasSchedule = statusMode === 'scheduled' && !!formData.scheduledPublishTime;

      const newsData = {
        title: formData.title,
        summary: formData.summary || null,
        content: htmlContent,
        imageUrl: finalImageUrl || null,
        category: formData.categoryId ? { id: parseInt(formData.categoryId) } : null,
        isPublished: statusMode === 'public' && !hasSchedule,
        isPinned: formData.isPinned || false,
        viewCount: 0,
        publishedAt: (statusMode === 'scheduled' && hasSchedule)
          ? `${formData.scheduledPublishTime}${formData.scheduledPublishTime.length === 16 ? ':00' : ''}`
          : null
      };

      console.log('Sending news data:', newsData);

      if (isEditMode) {
        await updateNews(id, newsData);
      } else {
        await createNews(newsData);
      }

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
                      {/* Text Formatting */}
                      <button type="button" className="news-tool-btn" onClick={handleBold} title="Bold (Ctrl+B)">
                        <Bold size={14} />
                      </button>
                      <button type="button" className="news-tool-btn" onClick={handleItalic} title="Italic (Ctrl+I)">
                        <Italic size={14} />
                      </button>
                      <button type="button" className="news-tool-btn" onClick={handleUnderline} title="Underline (Ctrl+U)">
                        <Underline size={14} />
                      </button>
                      <button type="button" className="news-tool-btn" onClick={handleStrikethrough} title="Strikethrough">
                        <Strikethrough size={14} />
                      </button>
                      
                      <div className="news-tool-divider"></div>
                      
                      {/* Headings */}
                      <button type="button" className="news-tool-btn" onClick={() => handleHeading(1)} title="Heading 1">
                        <Heading1 size={14} />
                      </button>
                      <button type="button" className="news-tool-btn" onClick={() => handleHeading(2)} title="Heading 2">
                        <Heading2 size={14} />
                      </button>
                      <button type="button" className="news-tool-btn" onClick={() => handleHeading(3)} title="Heading 3">
                        <Heading3 size={14} />
                      </button>
                      
                      <div className="news-tool-divider"></div>
                      
                      {/* Alignment */}
                      <button type="button" className="news-tool-btn" onClick={handleAlignLeft} title="Align Left">
                        <AlignLeft size={14} />
                      </button>
                      <button type="button" className="news-tool-btn" onClick={handleAlignCenter} title="Align Center">
                        <AlignCenter size={14} />
                      </button>
                      <button type="button" className="news-tool-btn" onClick={handleAlignRight} title="Align Right">
                        <AlignRight size={14} />
                      </button>
                      
                      <div className="news-tool-divider"></div>
                      
                      {/* Lists */}
                      <button type="button" className="news-tool-btn" onClick={handleList} title="Bullet List">
                        <List size={14} />
                      </button>
                      <button type="button" className="news-tool-btn" onClick={handleOrderedList} title="Numbered List">
                        <ListOrdered size={14} />
                      </button>
                      
                      <div className="news-tool-divider"></div>
                      
                      {/* Insert */}
                      <button type="button" className="news-tool-btn" onClick={handleLink} title="Insert Link">
                        <Link size={14} />
                      </button>
                      <button type="button" className="news-tool-btn" onClick={handleImage} title="Insert Image">
                        <Image size={14} />
                      </button>
                      <button type="button" className="news-tool-btn" onClick={handleCode} title="Code Block">
                        <Code size={14} />
                      </button>
                      
                      <div className="news-tool-divider"></div>
                      
                      {/* Clear Formatting */}
                      <button type="button" className="news-tool-btn" onClick={handleClearFormat} title="Clear Formatting">
                        <Type size={14} />
                      </button>
                    </div>
                    <input
                      ref={contentImageInputRef}
                      type="file"
                      accept="image/*"
                      onChange={handleContentImageChange}
                      style={{ display: 'none' }}
                    />
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
                  <label className="news-form-label">Ảnh bìa</label>
                  
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
                  <label className="news-form-label">Trạng thái đăng</label>
                  <select
                    className="news-form-select"
                    value={statusMode}
                    onChange={(e) => {
                      setStatusMode(e.target.value);
                      if (e.target.value === 'scheduled') {
                        setShowScheduleForm(true);
                        // Set default schedule time if not set
                        if (!formData.scheduledPublishTime) {
                          const now = new Date();
                          const dateStr = now.toISOString().split('T')[0];
                          setFormData(prev => ({
                            ...prev,
                            scheduledPublishTime: `${dateStr}T00:00`
                          }));
                        }
                      } else {
                        setShowScheduleForm(false);
                      }
                    }}
                  >
                    <option value="public">📢 Công khai ngay</option>
                    <option value="draft">📝 Lưu nháp</option>
                    <option value="scheduled">⏰ Hẹn lịch đăng</option>
                  </select>
                </div>

                {statusMode === 'scheduled' && (
                  <div className="news-form-group" style={{
                    padding: '15px',
                    background: '#f0f9ff',
                    borderRadius: '8px',
                    border: '1px solid #bae6fd'
                  }}>
                    <label className="news-form-label" style={{ marginBottom: '10px', display: 'block' }}>
                      Chọn thời gian đăng
                    </label>
                    
                    <div style={{ marginBottom: '15px' }}>
                      <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '500', color: '#334155' }}>
                        Ngày đăng
                      </label>
                      <input
                        type="date"
                        className="news-form-input"
                        value={getScheduleDate()}
                        min={new Date().toISOString().split('T')[0]}
                        onChange={(e) => {
                          if (e.target.value) {
                            const currentTime = getScheduleTime();
                            setFormData(prev => ({
                              ...prev,
                              scheduledPublishTime: `${e.target.value}T${currentTime.hh}:${currentTime.mm}`
                            }));
                          }
                        }}
                      />
                    </div>

                    <div style={{ marginBottom: '15px' }}>
                      <label style={{ display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '500', color: '#334155' }}>
                        Thời gian (giờ : phút)
                      </label>
                      <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                        <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                          <input
                            type="number"
                            min="0"
                            max="24"
                            className="news-form-input"
                            value={getScheduleTime().hh}
                            onChange={(e) => {
                              let val = parseInt(e.target.value) || 0;
                              if (val > 24) val = 24;
                              if (val < 0) val = 0;
                              updateScheduleTime(String(val).padStart(2, '0'), null);
                            }}
                            placeholder="00"
                            style={{ textAlign: 'center', fontSize: '16px' }}
                          />
                          <span style={{ fontSize: '11px', color: '#6b7280', marginTop: '4px', textAlign: 'center' }}>Giờ</span>
                        </div>
                        <span style={{ fontSize: '24px', fontWeight: 'bold', marginBottom: '20px' }}>:</span>
                        <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                          <input
                            type="number"
                            min="0"
                            max="59"
                            className="news-form-input"
                            value={getScheduleTime().mm}
                            onChange={(e) => {
                              let val = parseInt(e.target.value) || 0;
                              if (val > 59) val = 59;
                              if (val < 0) val = 0;
                              updateScheduleTime(null, String(val).padStart(2, '0'));
                            }}
                            placeholder="00"
                            style={{ textAlign: 'center', fontSize: '16px' }}
                          />
                          <span style={{ fontSize: '11px', color: '#6b7280', marginTop: '4px', textAlign: 'center' }}>Phút</span>
                        </div>
                      </div>
                    </div>

                    {formData.scheduledPublishTime && (
                      <div style={{
                        padding: '12px',
                        background: '#dcfce7',
                        borderRadius: '6px',
                        color: '#166534',
                        fontSize: '13px',
                        fontWeight: '500',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px'
                      }}>
                        <Clock size={16} />
                        <span>
                          Sẽ đăng vào: {new Date(formData.scheduledPublishTime).toLocaleString('vi-VN', {
                            year: 'numeric',
                            month: '2-digit',
                            day: '2-digit',
                            hour: '2-digit',
                            minute: '2-digit'
                          })}
                        </span>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>

            <div className="news-btn-group">
              <button
                type="button"
                className="news-btn news-btn-secondary"
                onClick={() => navigate('/notification')}
                disabled={loading}
              >
                <X size={16} />
                Hủy bỏ
              </button>
              
              {statusMode === 'draft' ? (
                <button
                  type="button"
                  className="news-btn news-btn-primary"
                  onClick={() => handleSubmit(true)}
                  disabled={loading || !formData.title || !formData.content}
                  style={{ background: '#6b7280' }}
                >
                  <Save size={16} />
                  {loading ? 'Đang lưu...' : 'Lưu nháp'}
                </button>
              ) : statusMode === 'scheduled' ? (
                <button
                  type="button"
                  className="news-btn news-btn-primary"
                  onClick={() => handleSubmit(false)}
                  disabled={loading || !formData.title || !formData.content || !formData.scheduledPublishTime}
                  style={{ background: '#3b82f6' }}
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
