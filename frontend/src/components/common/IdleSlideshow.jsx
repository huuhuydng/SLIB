import React, { useState, useEffect } from 'react';
import { useIdleTimer } from '../../hooks/useIdleTimer';
import '../../styles/common/IdleSlideshow.css';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export default function IdleSlideshow({ children, idleTimeMs = 5000, forceActive = false }) {
  const isIdleByTimer = useIdleTimer(idleTimeMs);
  const isIdle = forceActive || isIdleByTimer; // Kích hoạt nếu forceActive=true hoặc timer idle
  const [images, setImages] = useState([]);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [loading, setLoading] = useState(false);
  const [slideDuration, setSlideDuration] = useState(5000);

  // Fetch config duration
  useEffect(() => {
    const fetchConfig = async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/api/slideshow/config`);
        const data = await response.json();
        if (data.success && data.config.duration) {
          setSlideDuration(data.config.duration);
        }
      } catch (e) {}
    };
    fetchConfig();
  }, []);

  // Fetch ảnh từ Cloudinary qua backend API
  useEffect(() => {
    if (isIdle) {
      fetchImages();
    }
  }, [isIdle]);

  const fetchImages = async () => {
    try {
      setLoading(true);
      const response = await fetch(`${API_BASE_URL}/api/slideshow/images`);
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      
      if (data.success && data.images && data.images.length > 0) {
        const now = new Date();
        const activeImages = data.images.filter(img => {
          if (!img.isActive) return false;
          // Kiểm tra ngày hiệu lực
          const start = img.startDate ? new Date(img.startDate) : null;
          const end = img.endDate ? new Date(img.endDate) : null;
          if (start && now < start) return false;
          if (end && now > end) return false;
          return true;
        });
        setImages(activeImages);
        setCurrentImageIndex(0); // Reset về ảnh đầu tiên mỗi khi kích hoạt lại
        console.log(`✅ Slideshow activated - Loaded ${activeImages.length} active images`);
      } else {
        console.warn('⚠️ No images found in Cloudinary');
        setImages([]);
      }
    } catch (error) {
      console.error('❌ Error fetching slideshow images:', error);
      setImages([]);
    } finally {
      setLoading(false);
    }
  };

  // Auto rotate ảnh
  useEffect(() => {
    if (!isIdle || images.length === 0) return;

    const interval = setInterval(() => {
      setCurrentImageIndex(prev => (prev + 1) % images.length);
    }, slideDuration); // Sử dụng thời gian từ config

    return () => clearInterval(interval);
  }, [isIdle, images.length, slideDuration]);

  return (
    <>
      {children}
      
      {isIdle && (
        <div className="idleSlideshow">
          {loading ? (
            <div className="slideshowLoading">Loading images...</div>
          ) : images.length > 0 ? (
            <>
              <div 
                className="slideshowSlider"
                style={{ transform: `translateX(${-currentImageIndex * 100}%)` }}
              >
                {images.map((img, idx) => (
                  <img 
                    key={img.id || idx}
                    src={img.imageUrl} 
                    alt={`Slide ${idx + 1}`}
                    className="slideshowImage"
                  />
                ))}
              </div>
              <div className="slideshowIndicators">
                {images.map((_, idx) => (
                  <span 
                    key={idx} 
                    className={`slideshowDot ${idx === currentImageIndex ? "slideshowDot--active" : ""}`}
                  />
                ))}
              </div>
            </>
          ) : (
            <div className="slideshowEmpty">No images available</div>
          )}
        </div>
      )}
    </>
  );
}
