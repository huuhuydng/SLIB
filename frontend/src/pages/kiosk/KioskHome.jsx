import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import IdleSlideshow from '../../components/common/IdleSlideshow';
import { CalendarDays, QrCode, Map, Info, Monitor, LogIn } from 'lucide-react';
import logo from '../../assets/logo.png';
import '../../styles/kiosk/Kiosk.css';

/**
 * KioskHome - Trang chủ Kiosk Menu
 * Không có nested Routes vì tất cả sub-routes được handle bởi KioskRoutes
 */
const KioskHome = () => {
  const navigate = useNavigate();
  const [currentPage, setCurrentPage] = useState('menu'); // 'menu', 'seat-manage', 'check-in', 'map', 'help'


  const handleNavigate = (page, path) => {
    setCurrentPage(page);
    navigate(path);
  };

  const handleBack = () => {
    setCurrentPage('menu');
    navigate('/kiosk/');
  };

  // Bọc toàn bộ trang Kiosk trong IdleSlideshow
  // Khi không tương tác (10 giây), slide sẽ hiện lên
  return (
    <IdleSlideshow idleTimeMs={10000}>
      <div className="kioskHome__wrapper">
        
        {/* Header Kiosk */}
        <header className="kioskHome__header">
          <div className="kioskHome__brand">
            <img src={logo} alt="SLIB Logo" className="kioskHome__logo" />
            <div className="kioskHome__title">
              <h1>Thư viện Thông minh SLIB</h1>
              <p>Hệ thống tự phục vụ sinh viên</p>
            </div>
          </div>
          <div className="kioskHome__time">
            {new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
          </div>
        </header>

        {/* Main Menu Grid */}
        <main className="kioskHome__main">
          <h2 className="kioskHome__welcome">Bạn muốn làm gì hôm nay?</h2>
          
          <div className="kioskHome__grid">
            {/* Nút Đăng nhập QR - Dẫn tới QR Auth */}
            <button
              className="kioskHome__card kioskHome__card--qr"
              onClick={() => handleNavigate('qr-auth', '/kiosk/qr-auth')}
            >
              <div className="kioskHome__icon">
                <LogIn size={64} />
              </div>
              <h3>Đăng nhập QR</h3>
              <p>Quét mã để đăng nhập</p>
            </button>

            {/* Nút Đặt chỗ - Dẫn tới SeatManage */}
            <button
              className="kioskHome__card kioskHome__card--primary"
              onClick={() => handleNavigate('seat-manage', '/kiosk/seat-manage')}
            >
              <div className="kioskHome__icon">
                <CalendarDays size={64} />
              </div>
              <h3>Đặt chỗ ngồi</h3>
              <p>Xem sơ đồ và chọn ghế</p>
            </button>

            {/* Nút Check-in */}
            <button
              className="kioskHome__card kioskHome__card--secondary"
              onClick={() => handleNavigate('check-in', '/kiosk/check-in')}
            >
              <div className="kioskHome__icon">
                <QrCode size={64} />
              </div>
              <h3>Check-in</h3>
              <p>Xác nhận vị trí đã đặt</p>
            </button>

            {/* Nút Sơ đồ */}
            <button
              className="kioskHome__card kioskHome__card--info"
              onClick={() => handleNavigate('map', '/kiosk/map')}
            >
              <div className="kioskHome__icon">
                <Map size={64} />
              </div>
              <h3>Sơ đồ thư viện</h3>
              <p>Tìm khu vực chức năng</p>
            </button>

            {/* Nút Giám sát - Dành cho màn hình TV */}
            <button
              className="kioskHome__card kioskHome__card--monitor"
              onClick={() => handleNavigate('monitor', '/kiosk/monitor')}
            >
              <div className="kioskHome__icon">
                <Monitor size={64} />
              </div>
              <h3>Bảng giám sát</h3>
              <p>Theo dõi ra/vào thư viện</p>
            </button>

             {/* Nút Hướng dẫn */}
             <button
              className="kioskHome__card kioskHome__card--info"
              onClick={() => handleNavigate('help', '/kiosk/help')}
            >
              <div className="kioskHome__icon">
                <Info size={64} />
              </div>
              <h3>Hướng dẫn</h3>
              <p>Quy định & Cách sử dụng</p>
            </button>
          </div>
        </main>

        {/* Footer */}
        <footer className="kioskHome__footer">
          <p>Chạm vào màn hình để chọn chức năng • Giờ mở cửa: 07:00 - 22:00</p>
        </footer>
      </div>
    </IdleSlideshow>
  );
};

export default KioskHome;
