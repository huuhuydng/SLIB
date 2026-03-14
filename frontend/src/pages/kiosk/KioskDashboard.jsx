import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { LogOut, CalendarDays, Clock, CheckCircle, XCircle, BookOpen } from 'lucide-react';
import { useIdleTimer } from '../../hooks/useIdleTimer';
import kioskService from '../../services/kiosk/kioskService';
import './KioskDashboard.css';

const IDLE_TIMEOUT_MS = 30000;

const KioskDashboard = () => {
  const navigate = useNavigate();
  const [sessionData, setSessionData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(null);
  const [countdown, setCountdown] = useState(30);
  const [isCheckedIn, setIsCheckedIn] = useState(false);
  const [checkInTime, setCheckInTime] = useState(null);
  const [currentTime, setCurrentTime] = useState(new Date());
  const [popup, setPopup] = useState(null);

  // Đồng hồ thời gian thực
  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  // Load session
  useEffect(() => {
    const session = sessionStorage.getItem('kiosk_session');
    if (!session) {
      navigate('/kiosk/student-mode');
      return;
    }
    const parsedSession = JSON.parse(session);
    setSessionData(parsedSession);
    setLoading(false);
  }, [navigate]);

  // Auto check-in: khi có session nghĩa là đã check-in qua QR scan
  useEffect(() => {
    if (!sessionData) return;
    setIsCheckedIn(true);
    setCheckInTime(sessionData.checkInTime || new Date().toISOString());
  }, [sessionData]);

  // ── handleLogout phải khai báo TRƯỚC các effect sử dụng nó ──
  const handleLogout = useCallback(async () => {
    if (sessionData?.sessionToken) {
      try { await kioskService.expireSession(sessionData.sessionToken); } catch (_) { }
    }
    sessionStorage.removeItem('kiosk_session');
    navigate('/kiosk/student-mode');
  }, [sessionData, navigate]);

  const isIdle = useIdleTimer(IDLE_TIMEOUT_MS);
  useEffect(() => { if (isIdle) handleLogout(); }, [isIdle, handleLogout]);

  // Countdown timer
  useEffect(() => {
    if (!sessionData) return;
    const timer = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          handleLogout();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [sessionData, handleLogout]);

  const resetCountdown = () => setCountdown(30);

  useEffect(() => {
    if (!popup) return;
    const timer = setTimeout(() => setPopup(null), 4000);
    return () => clearTimeout(timer);
  }, [popup]);

  const showPopup = (type, title, message) => setPopup({ type, title, message });

  const handleCheckOut = async () => {
    if (!isCheckedIn) { showPopup('info', 'Thông báo', 'Bạn chưa check-in!'); return; }
    setActionLoading('check-out');
    resetCountdown();
    try {
      await kioskService.checkOut(sessionData.sessionToken);
      showPopup('success', 'Check-out thành công!', 'Cảm ơn bạn đã sử dụng thư viện. Hẹn gặp lại!');
      setTimeout(() => {
        sessionStorage.removeItem('kiosk_session');
        navigate('/kiosk/student-mode');
      }, 2500);
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Vui lòng thử lại.';
      showPopup('error', 'Lỗi check-out', msg);
    } finally { setActionLoading(null); }
  };

  const handleBooking = () => {
    resetCountdown();
    const params = new URLSearchParams();
    if (sessionData?.studentId) params.set('studentId', sessionData.studentId);
    if (sessionData?.studentName) params.set('name', sessionData.studentName);
    navigate(`/kiosk/seat-manage?${params.toString()}`);
  };

  if (loading) {
    return (
      <div className="kd">
        <div className="kd__loading"><div className="kd__spinner" /><p>Đang tải...</p></div>
      </div>
    );
  }

  const timeStr = currentTime.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  const dateStr = currentTime.toLocaleDateString('vi-VN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });

  return (
    <div className="kd" onClick={resetCountdown}>
      {/* Header */}
      <header className="kd__header">
        <button className="kd__back" onClick={handleLogout} title="Đăng xuất">
          <LogOut size={22} />
        </button>
        <h1 className="kd__greeting">Xin chào, {sessionData?.studentName || 'Sinh viên'}</h1>
        <div className="kd__countdown">
          <Clock size={18} />
          <span>{countdown}s</span>
        </div>
      </header>

      {/* Content */}
      <main className="kd__body">
        {/* Welcome section */}
        <div className="kd__welcome">
          <div className="kd__clock">{timeStr}</div>
          <div className="kd__date">{dateStr}</div>
          <div className="kd__status-badge">
            <span className={`kd__dot ${isCheckedIn ? 'kd__dot--active' : 'kd__dot--inactive'}`} />
            <span>{isCheckedIn ? 'Đang học tập' : 'Chưa check-in'}</span>
            {isCheckedIn && checkInTime && (
              <span className="kd__checkin-time">
                (từ {new Date(checkInTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })})
              </span>
            )}
          </div>
        </div>

        {/* Action Cards */}
        <div className="kd__actions">
          <button className="kd__card kd__card--checkout kd-ripple" onClick={handleCheckOut} disabled={actionLoading}>
            <div className="kd__card-icon"><LogOut size={44} /></div>
            <div className="kd__card-text">
              <h3>Check-out</h3>
              <p>Kết thúc phiên làm việc</p>
            </div>
            {actionLoading === 'check-out' && <div className="kd__card-loading" />}
          </button>

          <button className="kd__card kd__card--booking kd-ripple" onClick={handleBooking} disabled={actionLoading}>
            <div className="kd__card-icon"><CalendarDays size={44} /></div>
            <div className="kd__card-text">
              <h3>Đặt chỗ</h3>
              <p>Đặt ghế ngồi trong thư viện</p>
            </div>
          </button>
        </div>

        {/* Library branding */}
        <div className="kd__branding">
          <BookOpen size={18} />
          <span>SLIB - Hệ thống Thư viện Thông minh</span>
        </div>
      </main>

      {/* Popup */}
      {popup && (
        <div className="kd__popup-overlay" onClick={() => setPopup(null)}>
          <div className={`kd__popup kd__popup--${popup.type}`} onClick={(e) => e.stopPropagation()}>
            <div className="kd__popup-icon">
              {popup.type === 'success' ? <CheckCircle size={56} /> : popup.type === 'error' ? <XCircle size={56} /> : <CheckCircle size={56} />}
            </div>
            <h2>{popup.title}</h2>
            <p>{popup.message}</p>
            <button className="kd__popup-close" onClick={() => setPopup(null)}>Đóng</button>
          </div>
        </div>
      )}
    </div>
  );
};

export default KioskDashboard;
