import React from 'react';
import { Routes, Route, useNavigate } from 'react-router-dom';
import IdleSlideshow from './IdleSlideShow';
import { CalendarDays, QrCode, Map, Info } from 'lucide-react';
import logo from '../../../assets/logonencam.png';
import '../../../styles/admin/KioskHome.css';

// Import các trang chức năng con của Kiosk
import SeatManage from '../../../pages/librarian/SeatManage/SeatManage';
import CheckInOut from '../../../pages/librarian/CheckInOut/CheckInOut';
import HeatMap from '../../../pages/librarian/HeatMap/HeatMap';

const KioskHome = () => {
  return (
    <Routes>
      {/* Trang chủ Menu (path gốc của /kiosk/) */}
      <Route path="/" element={<KioskMenu />} />

      {/* Các trang chức năng con */}
      <Route path="seat-manage" element={
        <IdleSlideshow idleTimeMs={30000}>
          <SeatManage isKiosk={true} />
        </IdleSlideshow>
      } />
      
      <Route path="check-in" element={
        <IdleSlideshow idleTimeMs={30000}>
          <CheckInOut isKiosk={true} />
        </IdleSlideshow>
      } />
      
      <Route path="map" element={
        <IdleSlideshow idleTimeMs={30000}>
          <HeatMap isKiosk={true} />
        </IdleSlideshow>
      } />

      <Route path="help" element={
         <IdleSlideshow idleTimeMs={30000}>
           <div style={{padding: 40, textAlign: 'center', fontSize: 24}}>
              <h1>Hướng dẫn sử dụng</h1>
              <p>Nội dung đang cập nhật...</p>
           </div>
         </IdleSlideshow>
      } />
    </Routes>
  );
};

// Component giao diện Menu chính (Tách ra từ KioskHome cũ)
const KioskMenu = () => {
  const navigate = useNavigate();

  return (
    // Bọc toàn bộ trang Kiosk trong IdleSlideshow
    // Khi không tương tác (10 giây), slide sẽ hiện lên
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
            {/* Nút Đặt chỗ - Dẫn tới SeatManage */}
            <button 
              className="kioskHome__card kioskHome__card--primary"
              onClick={() => navigate('/kiosk/seat-manage')}
            >
              <div className="kioskHome__icon">
                <CalendarDays size={64} />
              </div>
              <h3>Đặt chỗ ngồi</h3>
              <p>Xem sơ đồ và chọn ghế</p>
            </button>

            {/* Nút Check-in - Tính năng tương lai */}
            <button 
              className="kioskHome__card kioskHome__card--secondary"
              onClick={() => navigate('/kiosk/check-in')}
            >
              <div className="kioskHome__icon">
                <QrCode size={64} />
              </div>
              <h3>Check-in</h3>
              <p>Xác nhận vị trí đã đặt</p>
            </button>

            {/* Nút Sơ đồ - Tính năng tương lai */}
            <button 
              className="kioskHome__card kioskHome__card--info"
              onClick={() => navigate('/kiosk/map')}
            >
              <div className="kioskHome__icon">
                <Map size={64} />
              </div>
              <h3>Sơ đồ thư viện</h3>
              <p>Tìm khu vực chức năng</p>
            </button>

             {/* Nút Hướng dẫn */}
             <button 
              className="kioskHome__card kioskHome__card--info"
              onClick={() => navigate('/kiosk/help')}
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
