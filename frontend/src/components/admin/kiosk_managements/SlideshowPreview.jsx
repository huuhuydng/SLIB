import React, { useState, useEffect } from 'react';
import { X } from 'lucide-react';
import '../../../styles/admin/SlideshowPreview.css';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

/**
 * Trang này dùng để admin xem trước slideshow sẽ hiển thị trên Kiosk.
 * Hiển thị fullscreen slideshow với tất cả các ảnh hiện hoạt.
 */
const SlideshowPreview = () => {
  console.log('🎬 SlideshowPreview component mounted!');
  console.log('📍 API_BASE_URL:', API_BASE_URL);
  
  const [images, setImages] = useState([]);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [slideDuration, setSlideDuration] = useState(5000);
  const [debugInfo, setDebugInfo] = useState('');

  // Fetch config duration
  useEffect(() => {
    const fetchConfig = async () => {
      try {
        console.log('🔧 Fetching config from:', `${API_BASE_URL}/api/slideshow/config`);
        const response = await fetch(`${API_BASE_URL}/api/slideshow/config`);
        const data = await response.json();
        if (data.success && data.config.duration) {
          setSlideDuration(data.config.duration);
          console.log('✅ Config duration:', data.config.duration);
        }
      } catch (e) {
        console.warn('⚠️ Failed to fetch config:', e);
      }
    };
    fetchConfig();
  }, []);

  // Fetch ảnh từ backend API
  useEffect(() => {
    fetchImages();
  }, []);

  const fetchImages = async () => {
    try {
      setLoading(true);
      setError(null);
      console.log('🚀 Starting fetch from:', `${API_BASE_URL}/api/slideshow/images`);
      setDebugInfo(`🚀 Fetching from ${API_BASE_URL}`);
      
      const response = await fetch(`${API_BASE_URL}/api/slideshow/images`);
      console.log('📡 Response status:', response.status);
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      console.log('📦 Full API Response:', data);
      console.log('📊 Response type:', typeof data);
      
      // Handle different response formats
      let imagesToProcess = [];
      if (Array.isArray(data)) {
        imagesToProcess = data;
        console.log('📋 Response is array with', data.length, 'items');
      } else if (data.success && data.images && Array.isArray(data.images)) {
        imagesToProcess = data.images;
        console.log('📋 Response has images array with', data.images.length, 'items');
      } else if (data.images && Array.isArray(data.images)) {
        imagesToProcess = data.images;
        console.log('📋 Response has images property with', data.images.length, 'items');
      } else {
        console.warn('⚠️ Unexpected response format:', data);
        setError('Unexpected API response format');
        setDebugInfo('❌ Định dạng response không hợp lệ');
        setImages([]);
        return;
      }

      console.log('🔍 Filtering active images...');
      const activeImages = imagesToProcess.filter(img => {
        const isActive = img.isActive === true || img.isActive === 'true';
        console.log(`  📌 "${img.imageName}": isActive=${img.isActive} (type: ${typeof img.isActive}) → ${isActive ? '✅' : '❌'}`);
        
        if (!isActive) return false;
        
        // Kiểm tra ngày hiệu lực
        const now = new Date();
        const start = img.startDate ? new Date(img.startDate) : null;
        const end = img.endDate ? new Date(img.endDate) : null;
        if (start && now < start) {
          console.log(`    ⏰ Start date ${start} > now, skipping`);
          return false;
        }
        if (end && now > end) {
          console.log(`    ⏰ End date ${end} < now, skipping`);
          return false;
        }
        return true;
      });
      
      console.log(`✅ Total: ${imagesToProcess.length}, Active: ${activeImages.length}`);
      
      if (activeImages.length > 0) {
        setImages(activeImages);
        setCurrentImageIndex(0);
        setDebugInfo(`✅ ${activeImages.length}/${imagesToProcess.length} ảnh hoạt động`);
        console.log('✅ Preview loaded successfully with', activeImages.length, 'images');
      } else {
        console.warn('⚠️ No active images found');
        setError(`Không có ảnh nào (Tìm thấy: ${imagesToProcess.length} ảnh, nhưng 0 ảnh hoạt động)`);
        setImages([]);
        setDebugInfo(`⚠️ Total: ${imagesToProcess.length}, Active: 0`);
      }
    } catch (error) {
      console.error('❌ Error fetching slideshow images:', error);
      setError(error.message || 'Lỗi khi tải ảnh');
      setDebugInfo(`❌ ${error.message}`);
      setImages([]);
    } finally {
      setLoading(false);
    }
  };

  // Auto rotate ảnh
  useEffect(() => {
    if (images.length === 0) return;

    console.log('▶️ Starting auto-rotate with interval:', slideDuration);
    const interval = setInterval(() => {
      setCurrentImageIndex(prev => (prev + 1) % images.length);
    }, slideDuration);

    return () => clearInterval(interval);
  }, [images.length, slideDuration]);

  // Keyboard controls
  useEffect(() => {
    const handleKeyDown = (e) => {
      console.log('🎹 Key pressed:', e.key);
      if (e.key === 'ArrowLeft') {
        setCurrentImageIndex(prev => (prev - 1 + images.length) % images.length);
      } else if (e.key === 'ArrowRight') {
        setCurrentImageIndex(prev => (prev + 1) % images.length);
      } else if (e.key === 'Escape') {
        console.log('🔐 Closing window...');
        window.close();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [images.length]);

  // Log render state
  useEffect(() => {
    console.log('🎨 Component Render State:', {
      loading,
      error,
      imagesCount: images.length,
      currentIndex: currentImageIndex,
      slideDuration
    });
  }, [loading, error, images, currentImageIndex, slideDuration]);

  return (
    <div className="slideshowPreview__wrapper">
      {/* Close Button */}
      <button
        onClick={() => window.close()}
        className="slideshowPreview__closeBtn"
        title="Đóng (Esc)"
      >
        <X size={24} color="#1A1A1A" />
      </button>

      {loading ? (
        <div className="slideshowPreview__loading">
          <div className="slideshowPreview__spinner" />
          <div>Đang tải ảnh...</div>
          <div style={{ fontSize: '12px', marginTop: '10px', opacity: 0.7 }}>{debugInfo}</div>
        </div>
      ) : error ? (
        <div className="slideshowPreview__error">
          <div className="slideshowPreview__errorMessage">⚠️ {error}</div>
          <div style={{ fontSize: '12px', marginBottom: '10px', opacity: 0.7 }}>{debugInfo}</div>
          <button
            onClick={fetchImages}
            className="slideshowPreview__retryBtn"
          >
            Thử lại
          </button>
        </div>
      ) : images.length > 0 ? (
        <div className="slideshowPreview__imageContainer">
          <div 
            className="slideshowPreview__slider"
            style={{
              transform: `translateX(${-currentImageIndex * 100}%)`,
            }}
          >
            {images.map((img, idx) => (
              <img 
                key={img.id || idx}
                src={img.imageUrl} 
                alt={`Slide ${idx + 1}`}
                className="slideshowPreview__image"
              />
            ))}
          </div>
          <div className="slideshowPreview__indicators">
            {images.map((_, idx) => (
              <span 
                key={idx} 
                className={`slideshowPreview__dot ${idx === currentImageIndex ? "slideshowPreview__dot--active" : ""}`}
              />
            ))}
          </div>
          
          {/* Navigation Info */}
          <div className="slideshowPreview__navInfo">
            <p>⬅️ / ➡️ : Điều hướng | ESC: Đóng</p>
            <p>{currentImageIndex + 1} / {images.length}</p>
          </div>

          {/* Debug Info Panel */}
          <div style={{
            position: 'absolute',
            bottom: '10px',
            right: '10px',
            background: 'rgba(0, 0, 0, 0.7)',
            color: '#0f0',
            padding: '10px',
            fontSize: '11px',
            fontFamily: 'monospace',
            borderRadius: '4px',
            maxWidth: '300px',
            zIndex: 1000,
            lineHeight: '1.4'
          }}>
            <div>▶️ Playing: {currentImageIndex + 1}/{images.length}</div>
            <div>⏱️ Duration: {slideDuration}ms</div>
            <div>{debugInfo}</div>
          </div>
        </div>
      ) : (
        <div className="slideshowPreview__empty">
          Không có hình ảnh
        </div>
      )}
    </div>
  );
};

export default SlideshowPreview;
