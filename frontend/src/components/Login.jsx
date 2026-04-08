import { useState, useEffect, useRef } from "react";
import librarianService from "../services/librarianService";
import { isStaffRole, normalizeRole } from "../utils/roles";
import useAppDialog from "../hooks/useAppDialog";
import "../styles/Auth.css";

const GOOGLE_CLIENT_ID = '262933313086-mhbevhu0b7hfqekchf6a99vnebjfr8b5.apps.googleusercontent.com';

function Login({ onLogin }) {
  const [loading, setLoading] = useState(false);
  const googleButtonRef = useRef(null);
  const { alert } = useAppDialog();

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

      // Backend trả về AuthResponse: { accessToken, refreshToken, id, email, fullName, studentCode, role, expiresIn }
      const token = backendResponse.accessToken ||
        backendResponse.access_token ||
        backendResponse.token;

      // Bắt mọi biến thể tên trường role từ backend
      const derivedRoleRaw = backendResponse.role
        || backendResponse.userRole
        || backendResponse.user_role
        || backendResponse.roleName
        || backendResponse.user_role_name
        || backendResponse.roles?.[0]?.role
        || backendResponse.roles?.[0]?.name;
      const derivedRole = normalizeRole(derivedRoleRaw);

      // Build user object from flat response
      const user = {
        id: backendResponse.id,
        email: backendResponse.email,
        fullName: backendResponse.fullName,
        role: derivedRole,
        studentCode: backendResponse.studentCode
      };

      if (token && user.role) {
        localStorage.setItem('librarian_token', token);
        localStorage.setItem('librarian_user', JSON.stringify(user));
        console.log("✅ Token saved successfully");
        console.log("✅ User:", user);

        if (onLogin) {
          onLogin(user.role);
        }

        // Force redirect sau khi login thành công
        setTimeout(() => {
          if (user.role === 'ADMIN') {
            window.location.href = '/admin/dashboard';
          } else if (isStaffRole(user.role)) {
            window.location.href = '/librarian/dashboard';
          } else {
            window.location.href = '/';
          }
        }, 100);
      } else {
        console.error("❌ Missing token or role:", { token: !!token, role: user.role });
        throw new Error('No token or user data received from server');
      }

    } catch (err) {
      console.error("❌ Google login error:", err);
      const errorMessage =
        err.response?.data?.message ||
        err.message ||
        "Đăng nhập Google thất bại. Vui lòng thử lại.";
      alert({
        title: 'Đăng nhập Google thất bại',
        message: errorMessage,
        icon: 'error',
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-form-container">
      <div className="auth-form-box">
        <h2 className="form-title">Đăng Nhập Hệ Thống</h2>

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
          <p>Dành cho Admin và Thủ thư có tài khoản được ủy quyền</p>
        </div>
      </div>
    </div>
  );
}

export default Login;
