import React from 'react';
import { useNavigate } from 'react-router-dom';
import IdleSlideshow from '../../components/common/IdleSlideshow';
import { ArrowLeft } from 'lucide-react';
import '../../styles/kiosk/Kiosk.css';

/**
 * KioskHelp - Hướng dẫn sử dụng Kiosk
 */
const KioskHelp = () => {
  const navigate = useNavigate();

  const handleBack = () => {
    navigate('/kiosk/student-mode');
  };

  return (
    <IdleSlideshow idleTimeMs={60000}>
      <div className="kioskHome__wrapper">
        {/* Header */}
        <header className="kioskHome__header">
          <button className="kiosk-back-btn" onClick={handleBack}>
            <ArrowLeft size={24} />
          </button>
          <div className="kioskHome__title" style={{ marginLeft: 24 }}>
            <h1>Hướng dẫn sử dụng</h1>
          </div>
        </header>

        {/* Main */}
        <main className="kioskHome__main" style={{ padding: 60 }}>
          <div className="help-content">
            <section className="help-section">
              <h2>Cách sử dụng Kiosk</h2>

              <div className="help-step">
                <div className="help-step-number">1</div>
                <div className="help-step-content">
                  <h3>Đăng nhập bằng QR</h3>
                  <p>Quét mã QR trên màn hình bằng ứng dụng SLIB trên điện thoại để đăng nhập.</p>
                </div>
              </div>

              <div className="help-step">
                <div className="help-step-number">2</div>
                <div className="help-step-content">
                  <h3>Check-in</h3>
                  <p>Sau khi đăng nhập, nhấn nút "Check-in" để xác nhận có mặt tại thư viện.</p>
                </div>
              </div>

              <div className="help-step">
                <div className="help-step-number">3</div>
                <div className="help-step-content">
                  <h3>Đặt chỗ</h3>
                  <p>Nhấn nút "Đặt chỗ" để xem sơ đồ và chọn ghế ngồi theo nhu cầu.</p>
                </div>
              </div>

              <div className="help-step">
                <div className="help-step-number">4</div>
                <div className="help-step-content">
                  <h3>Check-out</h3>
                  <p>Khi rời khỏi thư viện, nhấn nút "Check-out" để kết thúc phiên làm việc.</p>
                </div>
              </div>
            </section>

            <section className="help-section">
              <h2>Lưu ý</h2>
              <ul>
                <li>Mã QR sẽ tự động làm mới sau 10 phút</li>
                <li>Hệ thống sẽ tự đăng xuất sau 30 giây không tương tác</li>
                <li>Vui lòng giữ thẻ sinh viên bên mình khi sử dụng thư viện</li>
                <li>Liên hệ thủ thư nếu cần hỗ trợ</li>
              </ul>
            </section>

            <section className="help-section">
              <h2>Giờ mở cửa</h2>
              <p><strong>Thứ 2 - Thứ 7:</strong> 07:00 - 22:00</p>
              <p><strong>Chủ nhật:</strong> 08:00 - 17:00</p>
            </section>
          </div>
        </main>

        {/* Footer */}
        <footer className="kioskHome__footer">
          <button className="kiosk-back-link" onClick={handleBack}>
            Quay lại
          </button>
        </footer>
      </div>

      <style>{`
        .help-content {
          max-width: 800px;
          background: #fff;
          border-radius: 24px;
          padding: 48px;
          box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
        }

        .help-section {
          margin-bottom: 40px;
        }

        .help-section:last-child {
          margin-bottom: 0;
        }

        .help-section h2 {
          font-size: 28px;
          color: #1a1a1a;
          margin: 0 0 24px;
          padding-bottom: 12px;
          border-bottom: 2px solid #f0f0f0;
        }

        .help-step {
          display: flex;
          gap: 20px;
          margin-bottom: 24px;
        }

        .help-step-number {
          width: 48px;
          height: 48px;
          background: #FF751F;
          color: #fff;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 24px;
          font-weight: 700;
          flex-shrink: 0;
        }

        .help-step-content h3 {
          font-size: 20px;
          color: #1a1a1a;
          margin: 0 0 8px;
        }

        .help-step-content p {
          font-size: 16px;
          color: #555;
          margin: 0;
          line-height: 1.5;
        }

        .help-section ul {
          padding-left: 20px;
        }

        .help-section li {
          font-size: 16px;
          color: #555;
          margin-bottom: 12px;
          line-height: 1.5;
        }

        .help-section p {
          font-size: 16px;
          color: #555;
          margin: 8px 0;
        }
      `}</style>
    </IdleSlideshow>
  );
};

export default KioskHelp;
