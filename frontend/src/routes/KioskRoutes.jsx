import { useEffect, useState } from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { KioskProvider, useKiosk } from '../context/KioskContext';
import KioskGuard from '../components/kiosk/KioskGuard';
import KioskModeSelect from '../pages/kiosk/KioskModeSelect';
import KioskStudentMode from '../pages/kiosk/KioskStudentMode';
import KioskQrAuth from '../pages/kiosk/KioskQrAuth';
import KioskDashboard from '../pages/kiosk/KioskDashboard';
import KioskHelp from '../pages/kiosk/KioskHelp';
import Attendance from '../pages/kiosk/AttendanceWaitingScreen';
import KioskSeatBooking from '../pages/kiosk/KioskSeatBooking';
import KioskAccessDenied from '../pages/kiosk/KioskAccessDenied';
import KioskLockScreen from '../pages/kiosk/KioskLockScreen';
import { API_BASE_URL } from '../config/apiConfig';

/**
 * KioskTokenActivator - Xử lý token kích hoạt từ query param ?token=xxx
 * Phải nằm bên trong KioskProvider để truy cập context
 */
const KioskTokenActivator = ({ children }) => {
  const { activate, isActivated, isLoading } = useKiosk();
  const [processed, setProcessed] = useState(false);

  useEffect(() => {
    if (processed) return;
    const params = new URLSearchParams(window.location.search);
    const token = params.get('token');
    if (!token) {
      setProcessed(true);
      return;
    }

    fetch(`${API_BASE_URL}/slib/kiosk/session/activate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token }),
    })
      .then(res => {
        if (res.ok) return res.json();
        throw new Error('Token không hợp lệ');
      })
      .then(data => {
        activate(token, data);
        // Xóa token khỏi URL sau khi kích hoạt thành công
        window.history.replaceState({}, '', '/kiosk/');
      })
      .catch(() => {
        // Token không hợp lệ, xóa khỏi URL
        window.history.replaceState({}, '', '/kiosk/');
      })
      .finally(() => setProcessed(true));
  }, [activate, processed]);

  return children;
};

/**
 * KioskRoutesInner - Routes nội bộ, nằm trong KioskProvider
 */
function KioskRoutesInner() {
  const location = useLocation();
  const [accessDenied, setAccessDenied] = useState(false);

  // Reset access denied khi chuyển trang
  useEffect(() => {
    setAccessDenied(false);
  }, [location.pathname]);

  useEffect(() => {
    const handleAuthError = () => {
      setAccessDenied(true);
    };
    window.addEventListener('kiosk-auth-error', handleAuthError);
    return () => window.removeEventListener('kiosk-auth-error', handleAuthError);
  }, []);

  if (accessDenied) {
    return <KioskAccessDenied />;
  }

  return (
    <KioskTokenActivator>
      <Routes>
        {/* Route kích hoạt thủ công - không cần guard */}
        <Route path="activate" element={<KioskLockScreen />} />

        {/* Tất cả các route kiosk được bảo vệ bởi KioskGuard */}
        <Route element={<KioskGuard />}>
          {/* Chọn chế độ Kiosk */}
          <Route path="/" element={<KioskModeSelect />} />

          {/* Chế độ Kiosk Sinh viên */}
          <Route path="student-mode" element={<KioskStudentMode />} />

          {/* Xác minh QR Code */}
          <Route path="qr-auth" element={<KioskQrAuth />} />

          {/* Dashboard sau khi scan QR */}
          <Route path="dashboard" element={<KioskDashboard />} />

          {/* Hướng dẫn */}
          <Route path="help" element={<KioskHelp />} />

          {/* Đặt chỗ */}
          <Route path="seat-manage" element={<KioskSeatBooking />} />

          {/* Bảng giám sát (TV Mode) */}
          <Route path="monitor" element={<Attendance />} />
        </Route>

        {/* Access Denied */}
        <Route path="access-denied" element={<KioskAccessDenied />} />

        {/* Redirect any unmatched routes */}
        <Route path="*" element={<Navigate to="/kiosk/" replace />} />
      </Routes>
    </KioskTokenActivator>
  );
}

/**
 * KioskRoutes - Entry point với KioskProvider wrapper
 *
 * Luồng:
 * 1. URL có ?token=xxx -> KioskTokenActivator tự động kích hoạt
 * 2. /kiosk/ -> KioskGuard kiểm tra token -> KioskModeSelect
 * 3. /kiosk/student-mode -> KioskStudentMode (Xác minh QR)
 * 4. /kiosk/qr-auth -> KioskQrAuth (Hiển thị QR)
 * 5. /kiosk/dashboard -> KioskDashboard (Check-in/out + Đặt chỗ)
 * 6. /kiosk/seat-manage -> KioskSeatBooking (Sơ đồ ghế)
 * 7. /kiosk/help -> KioskHelp (Hướng dẫn)
 * 8. /kiosk/monitor -> Attendance (Bảng giám sát)
 */
function KioskRoutes() {
  return (
    <KioskProvider>
      <KioskRoutesInner />
    </KioskProvider>
  );
}

export default KioskRoutes;
