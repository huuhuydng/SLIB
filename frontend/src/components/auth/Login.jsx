import { useState, useEffect, useRef } from "react";
import librarianService from "../../services/librarianService";
import "../../styles/Auth.css";

const GOOGLE_CLIENT_ID = '262933313086-mhbevhu0b7hfqekchf6a99vnebjfr8b5.apps.googleusercontent.com';

function Login({ onLogin }) {
  const [loading, setLoading] = useState(false);
  const googleButtonRef = useRef(null);

  // ============ INIT GOOGLE SIGN-IN ============
  useEffect(() => {
    const initializeGoogle = () => {
      if (window.google?.accounts?.id) {
        try {
          window.google.accounts.id.initialize({
            client_id: GOOGLE_CLIENT_ID,
            callback: handleGoogleCallback,
          });
          
          // Render button Google chính thức
          if (googleButtonRef.current) {
            window.google.accounts.id.renderButton(
              googleButtonRef.current,
              {
                theme: "outline",
                size: "large",
                text: "continue_with",
                shape: "rectangular",
                logo_alignment: "left",
                width: 350
              }
            );
          }
          
          console.log('✅ Google Sign-In initialized');
        } catch (error) {
          console.error('❌ Error initializing Google:', error);
        }
      }
    };

    if (window.google) {
      initializeGoogle();
    } else {
      // Đợi Google script load
      const checkGoogle = setInterval(() => {
        if (window.google) {
          clearInterval(checkGoogle);
          initializeGoogle();
        }
      }, 100);
      
      setTimeout(() => clearInterval(checkGoogle), 10000);
    }
  }, []);

 // ============ GOOGLE LOGIN CALLBACK (ĐÃ THÊM FULL LOG) ============
  const handleGoogleCallback = async (response) => {
    try {
      setLoading(true);
      
      // 1. Nhận Google ID Token
      const idToken = response.credential;
      console.log("🔹 BƯỚC 1: Nhận Google ID Token thành công");

      // 2. Gọi Backend
      console.log("🔹 BƯỚC 2: Đang gọi API googleLogin...");
      const backendResponse = await librarianService.googleLogin(idToken);
      
      // 🔥 LOG QUAN TRỌNG: Xem cấu trúc dữ liệu Backend trả về
      console.group("🔥 RAW BACKEND RESPONSE (Dữ liệu gốc)");
      console.log(backendResponse);
      console.groupEnd();

      // 3. Lấy Token
      const token = backendResponse.accessToken || 
                    backendResponse.access_token ||
                    backendResponse.token;
      
      // 4. Build User Object
      // ⚠️ CHÚ Ý: Kiểm tra kỹ xem backend trả về 'id' hay 'userId' hay 'uuid'
      const user = {
        id: backendResponse.id || backendResponse.userId || backendResponse.uuid, // Thử nhiều trường hợp
        email: backendResponse.email,
        fullName: backendResponse.fullName,
        studentCode: backendResponse.studentCode,
        role: backendResponse.role
      };
      
      // 🔥 LOG QUAN TRỌNG: Xem User Object sau khi parse
      console.group("👤 PARSED USER OBJECT (Sẽ lưu vào Storage)");
      console.log("Full Object:", user);
      console.log("👉 UUID (Cần cho Chat):", user.id); // <--- QUAN TRỌNG NHẤT
      console.log("👉 Email:", user.email);
      console.log("👉 Role:", user.role);
      console.groupEnd();
      
      if (token && user && user.role) {
        // Check Role
        if (user.role === 'STUDENT') {
          alert('Sinh viên không được phép truy cập hệ thống web.');
          return;
        }
        
        if (user.role !== 'LIBRARIAN' && user.role !== 'ADMIN') {
          alert('Bạn không có quyền truy cập hệ thống này.');
          return;
        }
        
        // 5. Lưu vào LocalStorage
        localStorage.setItem('librarian_token', token);
        localStorage.setItem('librarian_user', JSON.stringify(user));
        
        // 🔥 LOG KIỂM TRA STORAGE
        console.group("💾 KIỂM TRA LOCAL STORAGE (Verification)");
        const savedUser = JSON.parse(localStorage.getItem('librarian_user'));
        console.log("Đọc lại từ Storage:", savedUser);
        console.log("UUID trong Storage:", savedUser?.id);
        console.groupEnd();
        
        console.log("✅ Đăng nhập thành công! Đang chuyển hướng...");
        
        // Cập nhật State App
        if (onLogin) {
          onLogin(user.role);
        }

      } else {
        console.error("❌ Thiếu Token hoặc User info");
        throw new Error('No token or user data received from server');
      }

    } catch (err) {
      console.error("❌ Google login error:", err);
      alert(err.response?.data?.message || err.message || "Đăng nhập thất bại.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-form-container">
      <div className="auth-form-box">
        <h2 className="form-title">Đăng Nhập Thủ Thư</h2>
        
        <p style={{ 
          textAlign: 'center', 
          color: '#666', 
          marginBottom: '24px',
          fontSize: '14px' 
        }}>
          Sử dụng tài khoản Google để đăng nhập
        </p>

        {loading && (
          <div style={{ textAlign: 'center', marginBottom: '20px' }}>
            <p style={{ color: '#F27125' }}>Đang xử lý đăng nhập...</p>
          </div>
        )}

        {/* GOOGLE BUTTON - RENDER BỞI GOOGLE */}
        <div 
          ref={googleButtonRef}
          style={{ 
            display: 'flex', 
            justifyContent: 'center',
            marginTop: '16px',
            minHeight: '44px' // Đảm bảo có không gian cho button
          }}
        ></div>

        <div style={{ 
          marginTop: '20px', 
          textAlign: 'center',
          fontSize: '12px',
          color: '#999'
        }}>
          <p>Chỉ dành cho thủ thư có tài khoản Google được ủy quyền</p>
        </div>
      </div>
    </div>
  );
}

export default Login;