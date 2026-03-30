import React, { useState, useEffect, useCallback, useRef } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { RefreshCw, Clock, AlertCircle } from 'lucide-react';
import kioskService from '../../services/kiosk/kioskService';
import slibLogo from '../../assets/logo.png';
import './DynamicQrCode.css';

/**
 * DynamicQrCode Component
 * Hiển thị QR code và tự động làm mới mỗi 10 phút
 */
const DynamicQrCode = ({
  onSessionUpdate,
  onError,
  kioskCode,
  refreshIntervalMs = 600000 // 10 phút mặc định
}) => {
  // Lấy kiosk code từ props hoặc localStorage nếu không được truyền
  const resolvedKioskCode = kioskCode || kioskService.getKioskCode();
  const [qrData, setQrData] = useState(null);
  const [expiresAt, setExpiresAt] = useState(null);
  const [timeLeft, setTimeLeft] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  // Dùng ref cho onError để tránh tạo lại generateQr khi parent re-render
  const onErrorRef = useRef(onError);
  useEffect(() => {
    onErrorRef.current = onError;
  }, [onError]);

  // Generate QR code
  const generateQr = useCallback(async () => {
    try {
      setRefreshing(true);
      setError(null);

      const response = await kioskService.generateQr(resolvedKioskCode);

      setQrData(response.qrPayload);
      setExpiresAt(new Date(response.expiresAt));
      setLoading(false);
    } catch (err) {
      setError(err.message || 'Không thể tải mã QR');
      setLoading(false);
      if (onErrorRef.current) onErrorRef.current(err);
    } finally {
      setRefreshing(false);
    }
  }, [resolvedKioskCode]);

  // Timer to countdown and refresh
  useEffect(() => {
    if (!expiresAt) return;

    const calculateTimeLeft = () => {
      const now = new Date();
      const diff = expiresAt - now;
      return Math.max(0, Math.floor(diff / 1000)); // seconds
    };

    setTimeLeft(calculateTimeLeft());

    const timer = setInterval(() => {
      const remaining = calculateTimeLeft();
      setTimeLeft(remaining);

      // Auto refresh when 30 seconds left
      if (remaining <= 30 && remaining > 0) {
        generateQr();
      }
    }, 1000);

    return () => clearInterval(timer);
  }, [expiresAt, generateQr]);

  // Initial generation
  useEffect(() => {
    generateQr();

    // Set up auto-refresh interval
    const refreshInterval = setInterval(() => {
      generateQr();
    }, refreshIntervalMs);

    return () => clearInterval(refreshInterval);
  }, [generateQr, refreshIntervalMs]);

  // Format time left as MM:SS
  const formatTimeLeft = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  // Get progress percentage
  const getProgress = () => {
    if (!expiresAt) return 0;
    const totalSeconds = refreshIntervalMs / 1000;
    return (timeLeft / totalSeconds) * 100;
  };

  if (loading && !qrData) {
    return (
      <div className="dynamic-qr__loading">
        <RefreshCw className="spin" size={48} />
        <p>Đang tải mã QR...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="dynamic-qr__error">
        <AlertCircle size={48} />
        <p>{error}</p>
        <button onClick={generateQr} className="retry-button">
          Thử lại
        </button>
      </div>
    );
  }

  return (
    <div className="dynamic-qr">
      {/* QR Code Display */}
      <div className="dynamic-qr__code">
        {refreshing && <div className="dynamic-qr__refreshing-overlay" />}
        <QRCodeSVG
          value={qrData || ''}
          size={280}
          level="H"
          includeMargin={true}
          imageSettings={{
            src: slibLogo,
            height: 48,
            width: 48,
            excavate: true,
          }}
        />
      </div>

      {/* Timer */}
      <div className="dynamic-qr__timer">
        <Clock size={20} />
        <span>Hết hạn sau: {formatTimeLeft(timeLeft)}</span>
      </div>

      {/* Progress Bar */}
      <div className="dynamic-qr__progress">
        <div
          className="dynamic-qr__progress-bar"
          style={{ width: `${getProgress()}%` }}
        />
      </div>

      {/* Instructions */}
      <div className="dynamic-qr__instructions">
        <p>Quét mã QR bằng App SLIB để đăng nhập</p>
        <p className="dynamic-qr__hint">
          Mã QR tự động làm mới mỗi 10 phút
        </p>
      </div>

      {/* Refreshing indicator */}
      {refreshing && (
        <div className="dynamic-qr__refreshing">
          <RefreshCw className="spin" size={16} />
          <span>Đang làm mới...</span>
        </div>
      )}
    </div>
  );
};

export default DynamicQrCode;
