import { useState, useEffect, useRef } from "react";
import librarianService from "../../services/librarianService";
import "../../styles/Auth.css";

const GOOGLE_CLIENT_ID = '262933313086-mhbevhu0b7hfqekchf6a99vnebjfr8b5.apps.googleusercontent.com';

function Login({ onLogin }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
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
                shape: "pill",
                logo_alignment: "center",
                width: 300
              }
            );
          }

          console.log('✅ Google Sign-In initialized');
        } catch (error) {
          console.error('❌ Error initializing Google:', error);
          setError('Không thể khởi tạo đăng nhập Google. Vui lòng tải lại trang.');
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

      setTimeout(() => {
        clearInterval(checkGoogle);
        if (!window.google) {
          setError('Không thể tải Google Sign-In. Vui lòng kiểm tra kết nối mạng.');
        }
      }, 10000);
    }
  }, []);

  // Clear error after 8 seconds
  useEffect(() => {
    if (error) {
      const timer = setTimeout(() => setError(null), 8000);
      return () => clearTimeout(timer);
    }
  }, [error]);

  // ============ GOOGLE LOGIN CALLBACK ============
  const handleGoogleCallback = async (response) => {
    try {
      setLoading(true);
      setError(null);

      const idToken = response.credential;
      console.log("✅ Google ID Token received:", idToken?.substring(0, 20) + "...");

      const backendResponse = await librarianService.googleLogin(idToken);
      console.log("✅ GOOGLE BACKEND RESPONSE:", backendResponse);

      // Backend trả về AuthResponse: { accessToken, refreshToken, id, email, fullName, studentCode, role, expiresIn }
      const token = backendResponse.accessToken ||
        backendResponse.access_token ||
        backendResponse.token;

      // Build user object from flat response
      const user = {
        id: backendResponse.id,
        email: backendResponse.email,
        fullName: backendResponse.fullName,
        role: backendResponse.role,
        studentCode: backendResponse.studentCode
      };

      if (token && user.role) {
        // Check role permissions
        if (user.role === 'STUDENT') {
          setError({
            type: 'warning',
            title: 'Không có quyền truy cập',
            message: 'Sinh viên không được phép truy cập hệ thống quản trị. Vui lòng sử dụng ứng dụng mobile SLIB.'
          });
          return;
        }

        // Check if current page is admin or librarian based on URL
        const isAdminPage = window.location.pathname.includes('/admin');

        if (isAdminPage && user.role === 'LIBRARIAN') {
          setError({
            type: 'warning',
            title: 'Không có quyền truy cập',
            message: 'Thủ thư không được phép truy cập trang quản trị Admin. Vui lòng đăng nhập tại trang dành cho Thủ thư.'
          });
          return;
        }

        if (!isAdminPage && user.role === 'ADMIN') {
          setError({
            type: 'warning',
            title: 'Không có quyền truy cập',
            message: 'Quản trị viên vui lòng đăng nhập tại trang Admin.'
          });
          return;
        }

        if (user.role !== 'LIBRARIAN' && user.role !== 'ADMIN') {
          setError({
            type: 'error',
            title: 'Tài khoản không hợp lệ',
            message: `Tài khoản của bạn có vai trò "${user.role}" và không được phép truy cập hệ thống này. Chỉ Thủ thư và Quản trị viên mới có thể đăng nhập.`
          });
          return;
        }

        localStorage.setItem('librarian_token', token);
        localStorage.setItem('librarian_user', JSON.stringify(user));
        console.log("✅ Token saved successfully");
        console.log("✅ User:", user);

        if (onLogin) {
          onLogin(user.role);
        }
      } else {
        console.error("❌ Missing token or role:", { token: !!token, role: user.role });
        setError({
          type: 'error',
          title: 'Lỗi hệ thống',
          message: 'Không nhận được thông tin xác thực từ máy chủ. Vui lòng thử lại sau.'
        });
      }

    } catch (err) {
      console.error("❌ Google login error:", err);

      const errorMessage = err.response?.data?.message || err.message || '';
      const errorCode = err.code || '';

      // Check for network errors first
      if (errorMessage.includes('Network Error') || errorCode === 'ERR_NETWORK') {
        setError({
          type: 'error',
          title: 'Lỗi kết nối mạng',
          message: 'Không thể kết nối đến máy chủ.'
        });
      } else if (errorMessage.includes('timeout') || errorCode === 'ECONNABORTED') {
        setError({
          type: 'error',
          title: 'Hết thời gian chờ',
          message: 'Máy chủ phản hồi quá chậm. Vui lòng thử lại sau.'
        });
      } else if (errorMessage.includes('not found') || errorMessage.includes('không tồn tại')) {
        setError({
          type: 'error',
          title: 'Tài khoản không tồn tại',
          message: 'Email này chưa được đăng ký trong hệ thống. Vui lòng liên hệ quản trị viên để được cấp quyền.'
        });
      } else if (errorMessage.includes('disabled') || errorMessage.includes('blocked')) {
        setError({
          type: 'error',
          title: 'Tài khoản bị khóa',
          message: 'Tài khoản của bạn đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên.'
        });
      } else if (err.response?.status === 401) {
        setError({
          type: 'error',
          title: 'Xác thực thất bại',
          message: 'Token Google không hợp lệ. Vui lòng thử đăng nhập lại.'
        });
      } else if (err.response?.status === 403) {
        setError({
          type: 'warning',
          title: 'Không có quyền truy cập',
          message: 'Tài khoản của bạn không được phép truy cập hệ thống này.'
        });
      } else if (err.response?.status >= 500) {
        setError({
          type: 'error',
          title: 'Lỗi máy chủ',
          message: 'Máy chủ gặp sự cố. Vui lòng thử lại sau hoặc liên hệ quản trị viên.'
        });
      } else {
        setError({
          type: 'error',
          title: 'Đăng nhập thất bại',
          message: errorMessage || 'Đã xảy ra lỗi khi đăng nhập. Vui lòng thử lại.'
        });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-form-container">
      <div className="auth-form-box login-box-modern">
        {/* Header with icon */}
        <div className="login-header">
          <div className="login-icon-wrapper">
            <svg className="login-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 12C14.7614 12 17 9.76142 17 7C17 4.23858 14.7614 2 12 2C9.23858 2 7 4.23858 7 7C7 9.76142 9.23858 12 12 12Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
              <path d="M20.5899 22C20.5899 18.13 16.7399 15 11.9999 15C7.25991 15 3.40991 18.13 3.40991 22" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </div>
          <h2 className="form-title-modern">Đăng Nhập Hệ Thống</h2>
          <p className="form-subtitle">Dành cho Thủ thư & Quản trị viên SLIB</p>
        </div>

        {/* Error message */}
        {error && (
          <div className={`login-error-box ${error.type === 'warning' ? 'warning' : 'error'}`}>
            <div className="error-icon">
              {error.type === 'warning' ? (
                <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M12 9V14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                  <path d="M12 17.5V18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                  <path d="M10.29 3.86L1.82 18C1.64 18.3 1.55 18.64 1.55 19C1.55 19.36 1.64 19.7 1.82 20C2 20.3 2.26 20.56 2.56 20.74C2.87 20.92 3.22 21.01 3.58 21H20.52C20.88 21.01 21.23 20.92 21.54 20.74C21.84 20.56 22.1 20.3 22.28 20C22.46 19.7 22.55 19.36 22.55 19C22.55 18.64 22.46 18.3 22.28 18L13.81 3.86C13.62 3.56 13.35 3.32 13.04 3.16C12.72 3 12.37 2.91 12.01 2.91C11.65 2.91 11.3 3 10.98 3.16C10.67 3.32 10.4 3.56 10.21 3.86H10.29Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              ) : (
                <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" />
                  <path d="M15 9L9 15" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                  <path d="M9 9L15 15" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                </svg>
              )}
            </div>
            <div className="error-content">
              <div className="error-title">{error.title}</div>
              <div className="error-message">{error.message}</div>
            </div>
            <button className="error-close" onClick={() => setError(null)}>
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M18 6L6 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                <path d="M6 6L18 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
            </button>
          </div>
        )}

        {/* Loading indicator */}
        {loading && (
          <div className="login-loading">
            <div className="loading-spinner"></div>
            <span>Đang xác thực...</span>
          </div>
        )}

        {/* Divider */}
        <div className="login-divider">
          <span>Đăng nhập bằng</span>
        </div>

        {/* GOOGLE BUTTON - RENDER BỞI GOOGLE */}
        <div
          ref={googleButtonRef}
          className="google-button-wrapper"
        ></div>

        {/* Footer info */}
        <div className="login-footer">
          <div className="footer-icon">
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z" stroke="currentColor" strokeWidth="1.5" />
              <path d="M12 8V12" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
              <path d="M12 16H12.01" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </div>
          <p>Chỉ tài khoản được ủy quyền mới có thể truy cập hệ thống</p>
        </div>
      </div>
    </div>
  );
}

export default Login;