import { useState, useEffect, useRef } from "react";
import librarianService from "../../services/librarian/librarianService";
import { consumeAuthNotice } from "../../utils/auth";
import { isPatronRole, isStaffRole, normalizeRole } from "../../utils/roles";
import logo from "../../assets/logo.png";
import "../../styles/Auth.css";

const GOOGLE_CLIENT_ID = '262933313086-mhbevhu0b7hfqekchf6a99vnebjfr8b5.apps.googleusercontent.com';

function Login({ onLogin, onForgotPassword }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const googleButtonRef = useRef(null);

  // ============ KHỞI TẠO GOOGLE SIGN-IN ============
  useEffect(() => {
    const initializeGoogle = () => {
      if (window.google?.accounts?.id) {
        try {
          window.google.accounts.id.initialize({
            client_id: GOOGLE_CLIENT_ID,
            callback: handleGoogleCallback,
          });

          // Render nút Google chính thức
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

        } catch (error) {
          console.error('Lỗi khởi tạo Google:', error);
        }
      }
    };

    if (window.google) {
      initializeGoogle();
    } else {
      const checkGoogle = setInterval(() => {
        if (window.google) {
          clearInterval(checkGoogle);
          initializeGoogle();
        }
      }, 100);

      setTimeout(() => {
        clearInterval(checkGoogle);
      }, 10000);
    }
  }, []);

  // Xóa lỗi sau 8 giây
  useEffect(() => {
    if (error) {
      const timer = setTimeout(() => setError(null), 8000);
      return () => clearTimeout(timer);
    }
  }, [error]);

  useEffect(() => {
    if (success) {
      const timer = setTimeout(() => setSuccess(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [success]);

  useEffect(() => {
    const authNotice = consumeAuthNotice();
    if (!authNotice) {
      return;
    }

    if (authNotice.type === 'error' || authNotice.type === 'warning') {
      setError(authNotice);
      return;
    }

    setSuccess({
      title: authNotice.title || 'Thao tác thành công',
      message: authNotice.message || 'Thao tác đã được thực hiện thành công.'
    });
  }, []);

  // ============ XỬ LÝ ĐĂNG NHẬP THÀNH CÔNG ============
  const handleLoginSuccess = (role) => {
    const normalizedRole = normalizeRole(role);

    if (isPatronRole(normalizedRole)) {
      setError({
        type: 'warning',
        title: 'Không có quyền truy cập',
        message: 'Tài khoản người dùng thư viện không được phép truy cập hệ thống quản trị. Vui lòng sử dụng ứng dụng mobile SLIB.'
      });
      return false;
    }

    if (!isStaffRole(normalizedRole)) {
      setError({
        type: 'error',
        title: 'Tài khoản không hợp lệ',
        message: `Tài khoản của bạn có vai trò "${normalizedRole}" và không được phép truy cập hệ thống này.`
      });
      return false;
    }

    // Hiển thị thông báo thành công
    const roleName = normalizedRole === 'ADMIN' ? 'Quản trị viên' : 'Thủ thư';
    setSuccess({
      title: 'Đăng nhập thành công!',
      message: `Chào mừng ${roleName}. Đang chuyển hướng...`
    });

    // Delay 1.5s trước khi chuyển trang
    setTimeout(() => {
      if (onLogin) {
        onLogin(normalizedRole);
      }
    }, 1500);

    return true;
  };

  // ============ ĐĂNG NHẬP GOOGLE ============
  const handleGoogleCallback = async (response) => {
    try {
      setLoading(true);
      setError(null);

      const idToken = response.credential;
      const backendResponse = await librarianService.googleLogin(idToken);

      const token = backendResponse.accessToken ||
        backendResponse.access_token ||
        backendResponse.token;

      const user = {
        id: backendResponse.id || backendResponse.userId || backendResponse.uuid,
        email: backendResponse.email,
        fullName: backendResponse.fullName,
        role: backendResponse.role,
        studentCode: backendResponse.studentCode
      };

      if (token && user.role) {
        localStorage.setItem('librarian_token', token);
        localStorage.setItem('librarian_user', JSON.stringify(user));
        handleLoginSuccess(user.role);
      } else {
        setError({
          type: 'error',
          title: 'Lỗi hệ thống',
          message: 'Không nhận được thông tin xác thực từ máy chủ.'
        });
      }

    } catch (err) {
      console.error("Lỗi đăng nhập Google:", err);
      handleLoginError(err);
    } finally {
      setLoading(false);
    }
  };

  // ============ ĐĂNG NHẬP MẬT KHẨU ============
  const handlePasswordLogin = async (e) => {
    e.preventDefault();

    if (!identifier.trim()) {
      setError({
        type: 'error',
        title: 'Thiếu thông tin',
        message: 'Vui lòng nhập Email hoặc Mã số.'
      });
      return;
    }

    if (!password) {
      setError({
        type: 'error',
        title: 'Thiếu thông tin',
        message: 'Vui lòng nhập mật khẩu.'
      });
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const backendResponse = await librarianService.loginWithPassword(identifier, password);

      const user = {
        id: backendResponse.id,
        email: backendResponse.email,
        fullName: backendResponse.fullName,
        role: backendResponse.role,
        studentCode: backendResponse.studentCode || backendResponse.userCode
      };

      if (user.role) {
        handleLoginSuccess(user.role);
      } else {
        setError({
          type: 'error',
          title: 'Lỗi hệ thống',
          message: 'Không nhận được thông tin vai trò từ máy chủ.'
        });
      }

    } catch (err) {
      console.error("Lỗi đăng nhập:", err);
      handleLoginError(err);
    } finally {
      setLoading(false);
    }
  };

  // ============ XỬ LÝ LỖI ĐĂNG NHẬP ============
  const handleLoginError = (err) => {
    const errorMessage = err.response?.data?.message || err.message || '';
    const errorCode = err.code || '';

    if (errorMessage.includes('Network Error') || errorCode === 'ERR_NETWORK') {
      setError({
        type: 'error',
        title: 'Lỗi kết nối mạng',
        message: 'Không thể kết nối đến máy chủ.'
      });
    } else if (errorMessage.includes('not found') || errorMessage.includes('không tồn tại')) {
      setError({
        type: 'error',
        title: 'Tài khoản không tồn tại',
        message: 'Tài khoản này chưa được đăng ký trong hệ thống.'
      });
    } else if (errorMessage.includes('Invalid password') || errorMessage.includes('Sai mật khẩu') || errorMessage.includes('sai')) {
      setError({
        type: 'error',
        title: 'Sai mật khẩu',
        message: 'Mật khẩu không chính xác. Vui lòng kiểm tra lại.'
      });
    } else if (err.response?.status === 401) {
      setError({
        type: 'error',
        title: 'Xác thực thất bại',
        message: 'Thông tin đăng nhập không chính xác.'
      });
    } else if (err.response?.status === 403) {
      setError({
        type: 'warning',
        title: 'Không có quyền truy cập',
        message: 'Tài khoản của bạn không được phép truy cập hệ thống này.'
      });
    } else {
      setError({
        type: 'error',
        title: 'Đăng nhập thất bại',
        message: errorMessage || 'Đã xảy ra lỗi khi đăng nhập. Vui lòng thử lại.'
      });
    }
  };

  return (
    <div className="login-container">
      {/* Phần bên trái - Chào mừng */}
      <div className="login-left">
        <div className="login-left-content">
          <div className="login-welcome-text">
            <span className="welcome-italic">Chào mừng đến với</span>
            <img src={logo} alt="SLIB Logo" className="welcome-logo" />
          </div>
          <p className="login-subtitle">Hệ sinh thái Thư viện Thông minh</p>
        </div>
      </div>

      {/* Phần bên phải - Form đăng nhập */}
      <div className="login-right">
        <div className="login-form-wrapper">
          {/* Logo và tiêu đề */}
          <div className="login-form-header">
            <h1 className="login-form-title">Đăng nhập vào hệ thống</h1>
          </div>

          {/* Thông báo lỗi */}
          {error && (
            <div className={`login-alert ${error.type}`}>
              <div className="alert-icon">
                {error.type === 'warning' ? (
                  <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M12 9V14M12 17.5V18M10.29 3.86L1.82 18C1.64 18.3 1.55 18.64 1.55 19C1.55 19.36 1.64 19.7 1.82 20C2 20.3 2.26 20.56 2.56 20.74C2.87 20.92 3.22 21.01 3.58 21H20.52C20.88 21.01 21.23 20.92 21.54 20.74C21.84 20.56 22.1 20.3 22.28 20C22.46 19.7 22.55 19.36 22.55 19C22.55 18.64 22.46 18.3 22.28 18L13.81 3.86C13.62 3.56 13.35 3.32 13.04 3.16C12.72 3 12.37 2.91 12.01 2.91C11.65 2.91 11.3 3 10.98 3.16C10.67 3.32 10.4 3.56 10.21 3.86H10.29Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                ) : (
                  <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" />
                    <path d="M15 9L9 15M9 9L15 15" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                  </svg>
                )}
              </div>
              <div className="alert-content">
                <strong>{error.title}</strong>
                <p>{error.message}</p>
              </div>
              <button className="alert-close" onClick={() => setError(null)}>
                <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                </svg>
              </button>
            </div>
          )}

          {/* Thông báo thành công */}
          {success && (
            <div className="login-alert success">
              <div className="alert-icon">
                <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" />
                  <path d="M8 12L11 15L16 9" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </div>
              <div className="alert-content">
                <strong>{success.title}</strong>
                <p>{success.message}</p>
              </div>
            </div>
          )}

          {/* Form đăng nhập */}
          <form onSubmit={handlePasswordLogin} className="login-form">
            {/* Input Email/Mã số */}
            <div className="form-field">
              <input
                type="text"
                className="form-input"
                placeholder="Email hoặc Mã số"
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                disabled={loading}
                autoComplete="username"
              />
              <span className="form-input-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M20 21V19C20 16.79 18.21 15 16 15H8C5.79 15 4 16.79 4 19V21" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                  <circle cx="12" cy="7" r="4" stroke="currentColor" strokeWidth="1.5" />
                </svg>
              </span>
            </div>

            {/* Input Mật khẩu */}
            <div className="form-field">
              <input
                type={showPassword ? "text" : "password"}
                className="form-input"
                placeholder="Mật khẩu"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={loading}
                autoComplete="current-password"
              />
              <span
                className="form-input-icon clickable"
                onClick={() => setShowPassword(!showPassword)}
              >
                {showPassword ? (
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20C5 20 1 12 1 12A18.45 18.45 0 0 1 6.06 6.06M9.9 4.24A9.12 9.12 0 0 1 12 4C19 4 23 12 23 12A18.5 18.5 0 0 1 20.84 15.19M14.12 14.12A3 3 0 1 1 9.88 9.88" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                    <line x1="1" y1="1" x2="23" y2="23" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                  </svg>
                ) : (
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M1 12C1 12 5 4 12 4C19 4 23 12 23 12C23 12 19 20 12 20C5 20 1 12 1 12Z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                    <circle cx="12" cy="12" r="3" stroke="currentColor" strokeWidth="1.5" />
                  </svg>
                )}
              </span>
            </div>

            {/* Link Quên mật khẩu */}
            <div className="form-forgot">
              <span className="forgot-link" onClick={onForgotPassword}>
                Quên mật khẩu?
              </span>
            </div>

            {/* Nút Đăng nhập */}
            <button
              type="submit"
              className="login-button"
              disabled={loading}
            >
              {loading ? (
                <>
                  <span className="button-spinner"></span>
                  Đang xử lý...
                </>
              ) : 'Đăng nhập'}
            </button>
          </form>

          {/* Divider */}
          <div className="login-divider-line">
            <span>hoặc đăng nhập bằng</span>
          </div>

          {/* Nút Google */}
          <div ref={googleButtonRef} className="google-btn-container"></div>

          {/* Footer */}
          <div className="login-form-footer">
            <p>&copy; 2025 SLIB - Hệ sinh thái Thư viện Thông minh</p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Login;
