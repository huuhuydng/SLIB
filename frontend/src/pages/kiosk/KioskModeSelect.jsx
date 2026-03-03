import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import IdleSlideshow from '../../components/common/IdleSlideshow';
import { Monitor, QrCode, Info, ArrowLeft } from 'lucide-react';
import logo from '../../assets/logo.png';
import '../../styles/kiosk/Kiosk.css';

/**
 * KioskModeSelect - Chọn chế độ Kiosk
 * Dành cho Thủ thư khi thiết lập Kiosk
 */
const KioskModeSelect = () => {
  const navigate = useNavigate();

  const handleSelectMode = (mode) => {
    if (mode === 'student') {
      // Chế độ Kiosk Sinh viên - Mở tab mới để sinh viên không quay lại được
      window.open('/kiosk/student-mode', '_blank');
    } else if (mode === 'monitor') {
      // Chế độ Bảng Giám sát - Chuyển đến màn hình monitoring
      navigate('/kiosk/monitor');
    }
  };

  return (
    <IdleSlideshow idleTimeMs={60000}>
      <div className="kioskHome__wrapper">
        {/* Header */}
        <header className="kioskHome__header">
          <div className="kioskHome__brand">
            <img src={logo} alt="SLIB Logo" className="kioskHome__logo" />
            <div className="kioskHome__title">
              <h1>Thư viện Thông minh SLIB</h1>
              <p>Thiết lập chế độ Kiosk</p>
            </div>
          </div>
          <div className="kioskHome__time">
            {new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
          </div>
        </header>

        {/* Main */}
        <main className="kioskHome__main">
          <h2 className="kioskHome__welcome">Chọn chế độ hoạt động</h2>

          <div className="kioskHome__grid kioskHome__grid--2col">
            {/* Chế độ 1: Kiosk Sinh viên */}
            <button
              className="kioskHome__card kioskHome__card--primary"
              onClick={() => handleSelectMode('student')}
            >
              <div className="kioskHome__icon">
                <QrCode size={64} />
              </div>
              <h3>Kiosk Sinh viên</h3>
              <p>Máy đặt tại sảnh</p>
              <p className="kioskHome__card-desc">
                Dành cho sinh viên đặt chỗ,<br />check-in/check-out
              </p>
            </button>

            {/* Chế độ 2: Bảng Giám sát */}
            <button
              className="kioskHome__card kioskHome__card--monitor"
              onClick={() => handleSelectMode('monitor')}
            >
              <div className="kioskHome__icon">
                <Monitor size={64} />
              </div>
              <h3>Bảng Giám sát</h3>
              <p>Màn hình TV lớn</p>
              <p className="kioskHome__card-desc">
                Theo dõi sinh viên ra/vào<br />thư viện theo thời gian thực
              </p>
            </button>
          </div>
        </main>

        {/* Footer */}
        <footer className="kioskHome__footer">
          <p>Chọn chế độ phù hợp với vị trí đặt máy Kiosk</p>
        </footer>
      </div>
    </IdleSlideshow>
  );
};

export default KioskModeSelect;
