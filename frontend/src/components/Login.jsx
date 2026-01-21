import { useState, useEffect, useRef } from "react";
import librarianService from "../services/librarianService";
import "../styles/Auth.css";

const GOOGLE_CLIENT_ID = '1071538292660-pf2ma4esd8lt1d2rclm27ipe1n3ch098.apps.googleusercontent.com';

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

  // ============ GOOGLE LOGIN CALLBACK ============
  const handleGoogleCallback = async (response) => {
    try {
      setLoading(true);

      const idToken = response.credential;
      console.log("✅ Google ID Token received:", idToken?.substring(0, 20) + "...");

      const backendResponse = await librarianService.googleLogin(idToken);
      console.log("✅ GOOGLE BACKEND RESPONSE:", backendResponse);

      // Lưu token và user info (backend trả về camelCase: accessToken)
      const token = backendResponse.accessToken ||
        backendResponse.access_token ||
        backendResponse.token;

      const user = backendResponse.user || {
        id: backendResponse.id,
        email: backendResponse.email,
        fullName: backendResponse.fullName,
        role: backendResponse.role,
        studentCode: backendResponse.studentCode
      };

      if (token) {
        localStorage.setItem('librarian_token', token);
        localStorage.setItem('librarian_user', JSON.stringify(user));
        console.log("✅ Token saved successfully");

        if (onLogin) {
          onLogin();
        }
      } else {
        throw new Error('No token received from server');
      }

    } catch (err) {
      console.error("❌ Google login error:", err);
      alert(
        err.response?.data?.message ||
        err.message ||
        "Đăng nhập Google thất bại. Vui lòng thử lại."
      );
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