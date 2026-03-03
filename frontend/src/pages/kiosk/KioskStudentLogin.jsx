import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertCircle } from 'lucide-react';
import VirtualKeyboard from '../../components/common/VirtualKeyboard';
import { useIdleTimer } from '../../hooks/useIdleTimer';
import logo from '../../assets/logo.png';
import '../../styles/kiosk/KioskLogin.css';

/**
 * KioskStudentLogin - Trang login cho Kiosk
 * Sinh viên nhập MSSV (DExxxxxx) để vào hệ thống Kiosk
 */
const KioskStudentLogin = () => {
  const navigate = useNavigate();
  const [studentId, setStudentId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Auto logout after 5 minutes of inactivity on login page
  const isIdle = useIdleTimer(300000); // 5 minutes = 300000ms

  useEffect(() => {
    if (isIdle) {
      // Clear session and redirect to login
      sessionStorage.removeItem('kiosk_student_id');
      sessionStorage.removeItem('kiosk_session_time');
      console.log('⏱️ Kiosk login idle timeout - Resetting');
    }
  }, [isIdle]);

  // Validate MSSV format: XXxxxxxx (2 chữ + 6 chữ số, vd: DE123456, CD999999)
  const validateMSSV = (value) => {
    const mssvRegex = /^[A-Z]{2}\d{6}$/;
    return mssvRegex.test(value);
  };

  // ONLY called when SUBMIT button is explicitly clicked on VirtualKeyboard
  const handleSubmit = async () => {
    if (!studentId.trim()) {
      setError('Vui lòng nhập mã sinh viên (2 ký tự + 6 chữ số)');
      return;
    }

    // Validate MSSV format - CHỈ kiểm tra, không tự động nhảy
    if (!validateMSSV(studentId)) {
      setError('Mã sinh viên không hợp lệ. Định dạng: XXxxxxxx (X=chữ, x=số)');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      // TODO: Call API verify student
      // const response = await kioskService.verifyStudent(studentId);
      
      // Tạm thời mock - sau sẽ replace với API thực
      console.log('🎫 Verifying student:', studentId);
      
      // Lưu vào session
      sessionStorage.setItem('kiosk_student_id', studentId);
      sessionStorage.setItem('kiosk_session_time', new Date().getTime());
      
      // 🔹 CHỈ NHẢY SAU KHI BẤM XÁC NHẬN (VIA SUBMIT button click)
      navigate('/kiosk/menu');
    } catch (err) {
      console.error('❌ Login error:', err);
      setError(err.message || 'Xác thực thất bại. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="kioskLogin__wrapper">
      {/* Background Pattern */}
      <div className="kioskLogin__bg-pattern"></div>

      {/* Login Card */}
      <div className="kioskLogin__container">
        {/* Logo + Title */}
        <div className="kioskLogin__header">
          <img src={logo} alt="SLIB Logo" className="kioskLogin__logo" />
          <h1>Thư viện Thông minh SLIB</h1>
          <p>Hệ thống Kiosk Tự phục vụ</p>
        </div>

        {/* Login Form - NOT submitted via form, only via VirtualKeyboard SUBMIT button click */}
        <form className="kioskLogin__form" onSubmit={(e) => e.preventDefault()}>
          <h2>Xác thực sinh viên</h2>

          {/* Error Alert */}
          {error && (
            <div className="kioskLogin__alert kioskLogin__alert--error">
              <AlertCircle size={20} />
              <span>{error}</span>
            </div>
          )}

          {/* Virtual Keyboard for Student ID Input */}
          <VirtualKeyboard
            value={studentId}
            onChange={(newValue) => {
              setStudentId(newValue);
              setError(null); // Clear error khi user nhập
            }}
            onSubmit={handleSubmit}
            placeholder="VD: AB123456"
          />
        </form>

        {/* Info Section */}
        <div className="kioskLogin__info">
          <h3>Hướng dẫn sử dụng</h3>
          <ul>
            <li>✓ Nhập mã sinh viên (2 ký tự + 6 chữ số)</li>
            <li>✓ Chọn chức năng cần sử dụng</li>
            <li>✓ Tương tác sẽ tự đặt lại nếu không hoạt động</li>
          </ul>
        </div>
      </div>

      {/* Footer */}
      <footer className="kioskLogin__footer">
        <p>Giờ hoạt động: 07:00 - 22:00 | Hỗ trợ: ext. 1234</p>
      </footer>
    </div>
  );
};

export default KioskStudentLogin;
