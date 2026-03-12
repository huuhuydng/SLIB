import React, { useEffect } from 'react';
import { AlertTriangle, RotateCcw, Home } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

/**
 * KioskAccessDenied - Trang hiển thị khi kiosk không được phép truy cập
 * Hiển thị khi backend trả về 403 Forbidden
 */
const KioskAccessDenied = () => {
  const navigate = useNavigate();

  useEffect(() => {
    // Auto refresh để thử lại sau 30s
    const timer = setTimeout(() => {
      window.location.reload();
    }, 30000);

    return () => clearTimeout(timer);
  }, []);

  const handleRetry = () => {
    window.location.reload();
  };

  const handleGoHome = () => {
    navigate('/');
  };

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: '100vh',
      backgroundColor: '#f5f5f5',
      fontFamily: 'system-ui, -apple-system, sans-serif',
      padding: '20px',
    }}>
      <div style={{
        backgroundColor: 'white',
        borderRadius: '16px',
        padding: '48px',
        textAlign: 'center',
        boxShadow: '0 4px 20px rgba(0,0,0,0.1)',
        maxWidth: '500px',
      }}>
        {/* Icon */}
        <div style={{
          width: '80px',
          height: '80px',
          borderRadius: '50%',
          backgroundColor: '#fee2e2',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          margin: '0 auto 24px',
        }}>
          <AlertTriangle size={40} color="#dc2626" />
        </div>

        {/* Title */}
        <h1 style={{
          fontSize: '24px',
          fontWeight: '600',
          color: '#1f2937',
          marginBottom: '12px',
        }}>
          Không thể truy cập
        </h1>

        {/* Message */}
        <p style={{
          fontSize: '16px',
          color: '#6b7280',
          marginBottom: '32px',
          lineHeight: '1.6',
        }}>
          Thiết bị kiosk không được phép truy cập hệ thống.
          <br />
          Vui lòng liên hệ thủ thư để được hỗ trợ.
        </p>

        {/* Countdown */}
        <p style={{
          fontSize: '14px',
          color: '#9ca3af',
          marginBottom: '24px',
        }}>
          Sẽ tự động tải lại sau 30 giây...
        </p>

        {/* Buttons */}
        <div style={{
          display: 'flex',
          gap: '12px',
          justifyContent: 'center',
        }}>
          <button
            onClick={handleRetry}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '12px 24px',
              backgroundColor: '#3b82f6',
              color: 'white',
              border: 'none',
              borderRadius: '8px',
              fontSize: '16px',
              fontWeight: '500',
              cursor: 'pointer',
              transition: 'background-color 0.2s',
            }}
            onMouseOver={(e) => e.target.style.backgroundColor = '#2563eb'}
            onMouseOut={(e) => e.target.style.backgroundColor = '#3b82f6'}
          >
            <RotateCcw size={20} />
            Thử lại
          </button>

          <button
            onClick={handleGoHome}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '12px 24px',
              backgroundColor: 'white',
              color: '#4b5563',
              border: '1px solid #d1d5db',
              borderRadius: '8px',
              fontSize: '16px',
              fontWeight: '500',
              cursor: 'pointer',
              transition: 'all 0.2s',
            }}
            onMouseOver={(e) => {
              e.target.style.backgroundColor = '#f9fafb';
              e.target.style.borderColor = '#9ca3af';
            }}
            onMouseOut={(e) => {
              e.target.style.backgroundColor = 'white';
              e.target.style.borderColor = '#d1d5db';
            }}
          >
            <Home size={20} />
            Về trang chủ
          </button>
        </div>
      </div>
    </div>
  );
};

export default KioskAccessDenied;
