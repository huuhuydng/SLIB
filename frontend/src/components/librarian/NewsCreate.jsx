import React, { useState } from 'react';
import { 
  ArrowLeft, 
  Bold, 
  Italic, 
  Underline, 
  List, 
  Link2, 
  Image as ImageIcon,
  CloudUpload,
  X,
  Send
} from 'lucide-react';

const NewsCreate = ({ onBack, onSubmit }) => {
  const [formData, setFormData] = useState({
    title: '',
    summary: '',
    content: '',
    category: '',
    isPinned: false,
    status: 'public',
    thumbnail: null
  });

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (onSubmit) {
      onSubmit(formData);
    }
  };

  const containerStyle = {
    padding: '2rem 2rem 2rem 0',
    minHeight: 'calc(100vh - 80px)',
    backgroundColor: '#f6f7fb'
  };

  const headerStyle = {
    display: 'flex',
    alignItems: 'center',
    gap: '15px',
    marginBottom: '25px',
    padding: '0'
  };

  const titleStyle = {
    fontSize: '32px',
    fontWeight: '700',
    color: '#2c3e50',
    margin: 0
  };

  const backBtnStyle = {
    cursor: 'pointer',
    color: '#7f8c8d',
    transition: 'color 0.2s'
  };

  const cardStyle = {
    background: 'white',
    borderRadius: '12px',
    padding: '40px',
    boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
    border: '1px solid #e5e7eb',
    margin: '0'
  };

  const gridStyle = {
    display: 'grid',
    gridTemplateColumns: '1.5fr 1fr',
    gap: '50px'
  };

  const formGroupStyle = {
    marginBottom: '25px'
  };

  const labelStyle = {
    display: 'block',
    marginBottom: '8px',
    fontWeight: '600',
    color: '#555',
    fontSize: '14px'
  };

  const inputStyle = {
    width: '100%',
    padding: '14px 16px',
    border: '1px solid #ddd',
    borderRadius: '12px',
    backgroundColor: '#FAFAFA',
    fontSize: '15px',
    fontFamily: 'Quicksand, sans-serif',
    transition: 'all 0.3s',
    boxSizing: 'border-box'
  };

  const textareaStyle = {
    ...inputStyle,
    resize: 'vertical',
    minHeight: '100px'
  };

  const editorContainerStyle = {
    border: '1px solid #ddd',
    borderRadius: '12px',
    backgroundColor: '#FAFAFA',
    overflow: 'hidden'
  };

  const toolbarStyle = {
    padding: '6px 8px',
    borderBottom: '1px solid #ddd',
    backgroundColor: '#f1f1f1',
    display: 'flex',
    gap: '5px',
    alignItems: 'center'
  };

  const toolBtnStyle = {
    width: '30px',
    height: '30px',
    border: 'none',
    background: 'white',
    borderRadius: '4px',
    cursor: 'pointer',
    color: '#555',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center'
  };

  const editorTextareaStyle = {
    ...inputStyle,
    border: 'none',
    borderRadius: 0,
    minHeight: '200px',
    maxHeight: '350px',
    backgroundColor: 'white'
  };

  const uploadZoneStyle = {
    border: '2px dashed #cbd5e0',
    borderRadius: '12px',
    padding: '40px 20px',
    textAlign: 'center',
    cursor: 'pointer',
    backgroundColor: '#FAFAFA',
    minHeight: '220px',
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'center',
    alignItems: 'center'
  };

  const toggleGroupStyle = {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#fafafa',
    padding: '18px 16px',
    borderRadius: '12px',
    border: '1px solid #eee'
  };

  const toggleSwitchStyle = {
    position: 'relative',
    display: 'inline-block',
    width: '56px',
    height: '30px'
  };

  const sliderStyle = {
    position: 'absolute',
    cursor: 'pointer',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: '#ccc',
    transition: '0.4s',
    borderRadius: '34px'
  };

  const btnGroupStyle = {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: '12px',
    marginTop: '15px',
    paddingTop: '15px',
    borderTop: '1px solid #eee'
  };

  const btnStyle = {
    padding: '13px 28px',
    borderRadius: '25px',
    fontWeight: '700',
    cursor: 'pointer',
    border: 'none',
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    fontSize: '15px',
    fontFamily: 'Quicksand, sans-serif'
  };

  const btnSecondaryStyle = {
    ...btnStyle,
    background: '#f1f1f1',
    color: '#555'
  };

  const btnPrimaryStyle = {
    ...btnStyle,
    background: 'linear-gradient(135deg, #F39C12 0%, #D35400 100%)',
    color: 'white',
    boxShadow: '0 4px 10px rgba(230, 126, 34, 0.3)'
  };

  return (
    <div style={containerStyle}>
      <div style={headerStyle}>
        <ArrowLeft 
          style={backBtnStyle}
          onClick={onBack}
          size={28}
        />
        <h2 style={titleStyle}>Đăng tin tức mới</h2>
      </div>

      <div style={cardStyle}>
        <form onSubmit={handleSubmit}>
          <div style={gridStyle}>
            {/* Left Column */}
            <div>
              <div style={formGroupStyle}>
                <label style={labelStyle}>
                  Tiêu đề bài viết <span style={{ color: 'red' }}>*</span>
                </label>
                <input
                  type="text"
                  name="title"
                  style={inputStyle}
                  placeholder="Nhập tiêu đề tin tức..."
                  value={formData.title}
                  onChange={handleInputChange}
                  required
                />
              </div>

              <div style={formGroupStyle}>
                <label style={labelStyle}>Tóm tắt ngắn</label>
                <textarea
                  name="summary"
                  style={textareaStyle}
                  placeholder="Mô tả ngắn gọn nội dung hiển thị ở danh sách..."
                  value={formData.summary}
                  onChange={handleInputChange}
                />
              </div>

              <div style={formGroupStyle}>
                <label style={labelStyle}>Nội dung chi tiết</label>
                <div style={editorContainerStyle}>
                  <div style={toolbarStyle}>
                    <button type="button" style={toolBtnStyle}>
                      <Bold size={16} />
                    </button>
                    <button type="button" style={toolBtnStyle}>
                      <Italic size={16} />
                    </button>
                    <button type="button" style={toolBtnStyle}>
                      <Underline size={16} />
                    </button>
                    <div style={{ width: '1px', background: '#ddd', height: '20px', margin: '0 5px' }}></div>
                    <button type="button" style={toolBtnStyle}>
                      <List size={16} />
                    </button>
                    <button type="button" style={toolBtnStyle}>
                      <Link2 size={16} />
                    </button>
                    <button type="button" style={toolBtnStyle}>
                      <ImageIcon size={16} />
                    </button>
                  </div>
                  <textarea
                    name="content"
                    style={editorTextareaStyle}
                    placeholder="Soạn thảo nội dung tại đây..."
                    value={formData.content}
                    onChange={handleInputChange}
                  />
                </div>
              </div>
            </div>

            {/* Right Column */}
            <div>
              <div style={formGroupStyle}>
                <label style={labelStyle}>Ảnh bìa</label>
                <div style={uploadZoneStyle}>
                  <CloudUpload size={40} color="#a0aec0" style={{ marginBottom: '12px' }} />
                  <div style={{ color: '#718096', fontSize: '15px', marginBottom: '5px' }}>
                    Kéo thả ảnh vào đây
                  </div>
                  <div style={{ color: '#718096', fontSize: '13px', margin: '5px 0' }}>
                    hoặc
                  </div>
                  <button 
                    type="button" 
                    style={{
                      marginTop: '10px',
                      background: 'white',
                      border: '1px solid #ddd',
                      padding: '6px 12px',
                      borderRadius: '6px',
                      fontSize: '12px',
                      cursor: 'pointer',
                      fontFamily: 'Quicksand, sans-serif',
                      fontWeight: '500'
                    }}
                  >
                    Chọn file
                  </button>
                </div>
              </div>

              <div style={formGroupStyle}>
                <label style={labelStyle}>Danh mục</label>
                <select
                  name="category"
                  style={inputStyle}
                  value={formData.category}
                  onChange={handleInputChange}
                >
                  <option value="">-- Chọn danh mục --</option>
                  <option value="event">Sự kiện</option>
                  <option value="important">Thông báo quan trọng</option>
                  <option value="books">Sách mới</option>
                  <option value="promotion">Ưu đãi</option>
                </select>
              </div>

              <div style={toggleGroupStyle}>
                <div>
                  <label style={{ ...labelStyle, margin: 0 }}>
                    Ghim tin tức
                  </label>
                  <div style={{ fontSize: '13px', color: '#888', marginTop: '4px' }}>
                    Đưa tin này lên đầu trang chủ
                  </div>
                </div>
                <label style={toggleSwitchStyle}>
                  <input
                    type="checkbox"
                    name="isPinned"
                    checked={formData.isPinned}
                    onChange={handleInputChange}
                    style={{ opacity: 0, width: 0, height: 0 }}
                  />
                  <span style={{
                    ...sliderStyle,
                    backgroundColor: formData.isPinned ? '#E67E22' : '#ccc'
                  }}>
                    <span style={{
                      position: 'absolute',
                      height: '22px',
                      width: '22px',
                      left: formData.isPinned ? '30px' : '4px',
                      bottom: '4px',
                      backgroundColor: 'white',
                      transition: '0.4s',
                      borderRadius: '50%'
                    }}></span>
                  </span>
                </label>
              </div>

              <div style={{ ...formGroupStyle, marginTop: '20px' }}>
                <label style={labelStyle}>Trạng thái</label>
                <select
                  name="status"
                  style={inputStyle}
                  value={formData.status}
                  onChange={handleInputChange}
                >
                  <option value="public">Công khai</option>
                  <option value="draft">Nháp</option>
                  <option value="private">Ẩn</option>
                </select>
              </div>
            </div>
          </div>

          <div style={btnGroupStyle}>
            <button
              type="button"
              style={btnSecondaryStyle}
              onClick={onBack}
            >
              <X size={18} /> Hủy bỏ
            </button>
            <button
              type="submit"
              style={btnPrimaryStyle}
            >
              <Send size={18} /> Đăng tin ngay
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default NewsCreate;
