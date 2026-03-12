import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import DynamicQrCode from '../../components/kiosk/DynamicQrCode';
import { ArrowLeft, Wifi, WifiOff } from 'lucide-react';
import websocketService from '../../services/websocketService';
import kioskService from '../../services/kioskService';
import { useKiosk } from '../../context/KioskContext';
import './KioskQrAuth.css';

/**
 * KioskQrAuth - Màn hình QR Authentication cho Kiosk
 * Hiển thị QR động và đợi sinh viên quét
 */
const KioskQrAuth = () => {
  const navigate = useNavigate();
  const { getKioskCode } = useKiosk();
  const KIOSK_CODE = getKioskCode();
  const [connected, setConnected] = useState(false);
  const [sessionData, setSessionData] = useState(null);
  const [error, setError] = useState(null);

  // Subscribe to WebSocket for session updates
  useEffect(() => {
    const topic = `/topic/kiosk/${KIOSK_CODE}/session-updated`;

    const handleSessionUpdate = (data) => {
      console.log('Session updated:', data);

      if (data.action === 'CHECK_IN' || data.action === 'SESSION_CREATED' || data.action === 'NONE') {
        // Store session data and navigate to dashboard
        sessionStorage.setItem('kiosk_session', JSON.stringify({
          sessionToken: data.sessionToken,
          studentCode: data.studentCode,
          studentName: data.studentName,
          studentAvatar: data.studentAvatar,
          studentId: data.studentId,
          accessLogId: data.accessLogId,
          currentAction: data.action,
        }));

        setSessionData(data);

        // Navigate to dashboard after short delay
        setTimeout(() => {
          navigate('/kiosk/dashboard');
        }, 1500);
      }
    };

    // Connect and subscribe
    const connectWs = () => {
      websocketService.connect(
        () => {
          console.log('WebSocket connected');
          setConnected(true);
          websocketService.subscribe(topic, handleSessionUpdate);
        },
        (error) => {
          console.error('WebSocket error:', error);
          setConnected(false);
        }
      );
    };

    connectWs();

    return () => {
      websocketService.unsubscribeCallback(topic, handleSessionUpdate);
    };
  }, [navigate]);

  // Check for existing active session on mount
  useEffect(() => {
    const checkExistingSession = async () => {
      try {
        const session = await kioskService.getActiveSession(KIOSK_CODE);
        if (session && session.sessionToken) {
          // Already has active session, navigate to dashboard
          sessionStorage.setItem('kiosk_session', JSON.stringify(session));
          navigate('/kiosk/dashboard');
        }
      } catch (err) {
        console.log('No existing session');
      }
    };

    checkExistingSession();
  }, [navigate]);

  const handleBack = () => {
    navigate('/kiosk/student-mode');
  };

  const handleQrError = (err) => {
    setError(err.message);
    console.error('QR Error:', err);
  };

  return (
    <div className="kiosk-qr-auth">
      {/* Header */}
      <header className="kiosk-qr-auth__header">
        <button className="back-button" onClick={handleBack}>
          <ArrowLeft size={24} />
        </button>
        <h1>Đăng nhập bằng QR</h1>
        <div className={`connection-status ${connected ? 'connected' : 'disconnected'}`}>
          {connected ? <Wifi size={20} /> : <WifiOff size={20} />}
          <span>{connected ? 'Đã kết nối' : 'Mất kết nối'}</span>
        </div>
      </header>

      {/* Main Content */}
      <main className="kiosk-qr-auth__main">
        {/* Session Notification */}
        {sessionData && (
          <div className="session-notification">
            <div className="session-notification__content">
              <h2>Đăng nhập thành công!</h2>
              <p>Xin chào, {sessionData.studentName}</p>
              <p className="session-notification__code">{sessionData.studentCode}</p>
            </div>
          </div>
        )}

        {/* QR Code Display */}
        {!sessionData && (
          <>
            <div className="kiosk-qr-auth__title">
              <h2>Quét mã QR để đăng nhập</h2>
              <p>Sử dụng ứng dụng SLIB trên điện thoại</p>
            </div>

            <DynamicQrCode
              kioskCode={KIOSK_CODE}
              onError={handleQrError}
            />

            {error && (
              <div className="error-message">
                <p>{error}</p>
              </div>
            )}

            <div className="kiosk-qr-auth__help">
              <h3>Hướng dẫn</h3>
              <ol>
                <li>Mở ứng dụng SLIB trên điện thoại</li>
                <li>Tìm chức năng "Quét QR" trong ứng dụng</li>
                <li>Quét mã QR trên màn hình này</li>
                <li>Xác nhận đăng nhập trên điện thoại</li>
              </ol>
            </div>
          </>
        )}
      </main>

      {/* Footer */}
      <footer className="kiosk-qr-auth__footer">
        <p>Quay lại <button onClick={handleBack}>Menu chính</button></p>
      </footer>
    </div>
  );
};

export default KioskQrAuth;
