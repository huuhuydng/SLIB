import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import IdleSlideshow from '../../components/common/IdleSlideshow';
import DynamicQrCode from '../../components/kiosk/DynamicQrCode';
import { HelpCircle, X, CheckCircle2 } from 'lucide-react';
import websocketService from '../../services/websocketService';
import kioskService from '../../services/kioskService';
import { useKiosk } from '../../context/KioskContext';
import logo from '../../assets/logo.png';
import fptLogo from '../../assets/fpt_logo.png';
import '../../styles/kiosk/Kiosk.css';
import './KioskStudentMode.css';

/**
 * KioskStudentMode - Chế độ Kiosk Sinh viên
 * Mặc định hiển thị QR Code, kèm nút Hướng dẫn ở góc
 */
const KioskStudentMode = () => {
  const navigate = useNavigate();
  const { getKioskCode } = useKiosk();
  const KIOSK_CODE = getKioskCode();
  const [sessionData, setSessionData] = useState(null);
  const [showHelp, setShowHelp] = useState(false);
  const [showSuccessPopup, setShowSuccessPopup] = useState(false);
  const [progressWidth, setProgressWidth] = useState(0);
  const successTimeoutRef = useRef(null);
  const processedSessionRef = useRef(null);

  // Subscribe WebSocket nhận sự kiện quét QR thành công
  useEffect(() => {
    const topic = `/topic/kiosk/${KIOSK_CODE}/session-updated`;
    const subscribeTime = Date.now();

    const handleSessionUpdate = (data) => {
      console.log('Session updated:', data);

      // Bỏ qua nếu đã xử lý session này
      if (processedSessionRef.current === data.sessionToken) return;

      // Bỏ qua message cũ (trước thời điểm subscribe)
      if (data.timestamp) {
        const msgTime = new Date(data.timestamp).getTime();
        if (msgTime < subscribeTime - 10000) {
          console.log('Ignoring old WebSocket message', data.sessionToken);
          return;
        }
      }

      if (data.action === 'CHECK_IN' || data.action === 'SESSION_CREATED' || data.action === 'NONE') {
        processedSessionRef.current = data.sessionToken;
        sessionStorage.setItem('kiosk_session', JSON.stringify({
          sessionToken: data.sessionToken,
          studentCode: data.studentCode,
          studentName: data.studentName,
          studentAvatar: data.studentAvatar,
          studentId: data.studentId,
          accessLogId: data.accessLogId,
          currentAction: data.action,
          checkInTime: data.timestamp,
        }));

        setSessionData(data);
        setShowSuccessPopup(true);

        requestAnimationFrame(() => {
          setProgressWidth(100);
        });

        successTimeoutRef.current = setTimeout(() => {
          navigate('/kiosk/dashboard');
        }, 3000);
      }
    };

    const connectWs = () => {
      websocketService.connect(
        () => {
          console.log('WebSocket connected at', new Date(subscribeTime).toISOString());
          websocketService.subscribe(topic, handleSessionUpdate);
        },
        (error) => {
          console.error('WebSocket error:', error);
        }
      );
    };

    connectWs();

    return () => {
      websocketService.unsubscribeCallback(topic, handleSessionUpdate);
    };
  }, [navigate]);

  // Polling fallback - kiểm tra session mỗi 5 giây (phòng trường hợp WebSocket miss)
  useEffect(() => {
    const startTime = Date.now();

    const checkExistingSession = async () => {
      // Không check nếu đang hiện popup
      if (showSuccessPopup) return;

      try {
        const session = await kioskService.getActiveSession(KIOSK_CODE);
        if (session && session.sessionToken && session.studentCode) {
          // Bỏ qua nếu đã xử lý session này
          if (processedSessionRef.current === session.sessionToken) return;

          // Bỏ qua session cũ (checkInTime trước lúc load trang)
          if (session.checkInTime) {
            const checkInMs = new Date(session.checkInTime).getTime();
            if (checkInMs < startTime - 15000) {
              console.log('Ignoring old session from polling', session.sessionToken);
              processedSessionRef.current = session.sessionToken;
              return;
            }
          }

          console.log('Polling found new active session:', session);
          processedSessionRef.current = session.sessionToken;
          sessionStorage.setItem('kiosk_session', JSON.stringify(session));

          setSessionData(session);
          setShowSuccessPopup(true);
          requestAnimationFrame(() => {
            setProgressWidth(100);
          });
          successTimeoutRef.current = setTimeout(() => {
            navigate('/kiosk/dashboard');
          }, 3000);
        }
      } catch (err) {
        // No session - ignore
      }
    };

    // Delay check lần đầu 2 giây (cho WebSocket ưu tiên)
    const firstCheck = setTimeout(checkExistingSession, 2000);

    // Poll mỗi 5 giây
    const pollInterval = setInterval(checkExistingSession, 5000);

    return () => {
      clearTimeout(firstCheck);
      clearInterval(pollInterval);
    };
  }, [navigate, showSuccessPopup]);

  // Đồng hồ realtime
  const [currentTime, setCurrentTime] = useState(
    new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
  );

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(
        new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
      );
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  return (
    <IdleSlideshow idleTimeMs={60000}>
      <div className="ksm">
        {/* Header */}
        <header className="ksm__header">
          <div className="ksm__header-left">
            <div className="ksm__brand">
              <img src={logo} alt="SLIB" className="ksm__logo" />
              <div className="ksm__title-block">
                <h1>SLIB - Thư viện Thông minh</h1>
                <p>Quét mã QR để đăng nhập</p>
              </div>
            </div>
          </div>

          <div className="ksm__header-right">
            <div className="ksm__clock">{currentTime}</div>
            <button
              className="ksm__help-btn"
              onClick={() => setShowHelp(true)}
              title="Hướng dẫn sử dụng"
            >
              <HelpCircle size={22} />
              <span>Hướng dẫn</span>
            </button>
          </div>
        </header>

        {/* Main - QR Code */}
        <main className="ksm__main">
          <div className="ksm__qr-area">
            <DynamicQrCode
              kioskCode={KIOSK_CODE}
              onError={(err) => console.error('QR Error:', err)}
            />
          </div>

          {/* Pop-up xác nhận thành công */}
          {showSuccessPopup && sessionData && (
            <div className="ksm__success-overlay">
              <div className="ksm__success-popup">
                <div className="ksm__success-icon-ring">
                  <CheckCircle2 size={64} strokeWidth={2} />
                </div>
                <h2 className="ksm__success-title">Xác nhận thành công!</h2>
                <p className="ksm__success-subtitle">Chào mừng bạn đến thư viện</p>
                <div className="ksm__success-info">
                  <p className="ksm__success-name">{sessionData.studentName}</p>
                  <p className="ksm__success-code">{sessionData.studentCode}</p>
                </div>
                <div className="ksm__success-progress-track">
                  <div
                    className="ksm__success-progress-bar"
                    style={{ width: `${progressWidth}%` }}
                  />
                </div>
                <p className="ksm__success-redirect">Đang chuyển tới trang chính...</p>
              </div>
            </div>
          )}
        </main>

        {/* Footer */}
        <footer className="ksm__footer">
          <img src={fptLogo} alt="FPT" className="ksm__footer-logo" />
        </footer>

        {/* Modal Hướng dẫn */}
        {showHelp && (
          <div className="ksm__help-overlay" onClick={() => setShowHelp(false)}>
            <div className="ksm__help-modal" onClick={(e) => e.stopPropagation()}>
              <button className="ksm__help-close" onClick={() => setShowHelp(false)}>
                <X size={24} />
              </button>

              <h2>Hướng dẫn sử dụng Kiosk</h2>

              <div className="ksm__help-steps">
                <div className="ksm__help-step">
                  <div className="ksm__step-num">1</div>
                  <div>
                    <h3>Mở ứng dụng SLIB trên điện thoại</h3>
                    <p>Mở ứng dụng <strong>SLIB</strong> trên điện thoại, nhấn vào biểu tượng <strong>Quét QR</strong> trên trang chủ.</p>
                  </div>
                </div>

                <div className="ksm__help-step">
                  <div className="ksm__step-num">2</div>
                  <div>
                    <h3>Quét mã QR trên màn hình Kiosk</h3>
                    <p>Hướng camera điện thoại vào mã QR đang hiển thị. Hệ thống sẽ tự động <strong>check-in</strong> và chuyển bạn tới trang quản lý.</p>
                  </div>
                </div>

                <div className="ksm__help-step">
                  <div className="ksm__step-num">3</div>
                  <div>
                    <h3>Đặt chỗ ngồi hoặc Check-out</h3>
                    <p>Tại trang quản lý, bạn có thể chọn <strong>"Đặt chỗ"</strong> để xem sơ đồ và chọn ghế trống, hoặc nhấn <strong>"Check-out"</strong> trước khi rời khỏi thư viện.</p>
                  </div>
                </div>
              </div>

              <div className="ksm__help-notes">
                <h3>Lưu ý</h3>
                <ul>
                  <li>Mã QR được tự động làm mới định kỳ để đảm bảo an toàn</li>
                  <li>Sau khi check-in, Kiosk sẽ tự đăng xuất nếu không thao tác trong 30 giây</li>
                  <li>Nếu gặp sự cố, vui lòng liên hệ thủ thư để được hỗ trợ</li>
                </ul>
              </div>
            </div>
          </div>
        )}
      </div>
    </IdleSlideshow>
  );
};

export default KioskStudentMode;
