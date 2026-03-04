import { useEffect, useState } from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import KioskModeSelect from '../pages/kiosk/KioskModeSelect';
import KioskStudentMode from '../pages/kiosk/KioskStudentMode';
import KioskQrAuth from '../pages/kiosk/KioskQrAuth';
import KioskDashboard from '../pages/kiosk/KioskDashboard';
import KioskHelp from '../pages/kiosk/KioskHelp';
import Attendance from '../pages/AttendanceWaitingScreen';
import KioskSeatBooking from '../pages/kiosk/KioskSeatBooking';
import KioskAccessDenied from '../pages/kiosk/KioskAccessDenied';

/**
 * Kiosk Routes - Routes cho Kiosk
 *
 * Luồng:
 * 1. /kiosk/ -> KioskModeSelect (Chọn chế độ)
 * 2. /kiosk/student-mode -> KioskStudentMode (Xác minh QR / Hướng dẫn)
 * 3. /kiosk/qr-auth -> KioskQrAuth (Hiển thị QR)
 * 4. /kiosk/dashboard -> KioskDashboard (Check-in/out + Đặt chỗ)
 * 5. /kiosk/seat-manage -> KioskSeatBooking (Sơ đồ ghế theo cấu hình admin)
 * 6. /kiosk/help -> KioskHelp (Hướng dẫn)
 * 7. /kiosk/monitor -> Attendance (Bảng giám sát)
 */
function KioskRoutes() {
  const location = useLocation();
  const [accessDenied, setAccessDenied] = useState(false);

  // Reset access denied khi chuyển trang
  useEffect(() => {
    setAccessDenied(false);
  }, [location.pathname]);

  useEffect(() => {
    // Lắng nghe sự kiện kiosk auth error
    const handleAuthError = () => {
      setAccessDenied(true);
    };

    window.addEventListener('kiosk-auth-error', handleAuthError);
    return () => window.removeEventListener('kiosk-auth-error', handleAuthError);
  }, []);

  // Nếu bị từ chối truy cập, hiển thị trang access denied
  if (accessDenied) {
    return <KioskAccessDenied />;
  }

  return (
    <Routes>
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

      {/* Access Denied - hiển thị khi kiosk không được phép truy cập */}
      <Route path="access-denied" element={<KioskAccessDenied />} />

      {/* Redirect any unmatched routes */}
      <Route path="*" element={<Navigate to="/kiosk/" replace />} />
    </Routes>
  );
}

export default KioskRoutes;
