import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { useState, useEffect, useCallback } from "react";
import { isTokenExpired } from "../utils/auth";

// Auth Page
import AuthPage from "../components/auth/AuthPage";

// Admin Routes
import AdminRoutes from "./AdminRoutes";

// Librarian Routes  
import LibrarianRoutes from "./LibrarianRoutes";

function AppRoutes() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [userRole, setUserRole] = useState(null);
  const [loading, setLoading] = useState(true);

  // Hàm xóa toàn bộ token và logout
  const performLogout = useCallback(() => {
    localStorage.removeItem('librarian_token');
    localStorage.removeItem('librarian_user');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('temp_reset_token');
    sessionStorage.removeItem('librarian_token');
    sessionStorage.removeItem('librarian_user');
    sessionStorage.removeItem('refresh_token');
    setIsLoggedIn(false);
    setUserRole(null);
  }, []);

  useEffect(() => {
    // Dọn dẹp token cũ nếu còn lưu ở localStorage (tránh tự động đăng nhập từ session trước)
    localStorage.removeItem('librarian_token');
    localStorage.removeItem('librarian_user');

    // Chỉ đọc token từ sessionStorage để buộc đăng nhập lại mỗi phiên mới
    const token = sessionStorage.getItem('librarian_token');
    const userStr = sessionStorage.getItem('librarian_user');

    if (token && userStr) {
      // Kiểm tra token hết hạn
      if (isTokenExpired(token)) {
        console.warn('[Auth] Token đã hết hạn, yêu cầu đăng nhập lại');
        performLogout();
        setLoading(false);
        return;
      }

      try {
        const user = JSON.parse(userStr);
        const rawRole = user.role
          || user.user_role
          || user.userRole
          || user.roleName
          || user.user_role_name
          || user.roles?.[0]?.role
          || user.roles?.[0]?.name;
        const role = rawRole ? rawRole.toString().toUpperCase() : null;

        console.log('[Auth] User loaded:', user);
        console.log('[Auth] Role detected:', role);

        // Chỉ cho phép LIBRARIAN và ADMIN đăng nhập
        if (role === 'LIBRARIAN' || role === 'ADMIN') {
          setIsLoggedIn(true);
          setUserRole(role);
        } else if (role === 'STUDENT') {
          // Không cho phép STUDENT đăng nhập vào web
          console.warn('[Auth] STUDENT không được phép đăng nhập vào hệ thống web');
          performLogout();
          alert('Sinh viên không được phép truy cập hệ thống web. Vui lòng sử dụng ứng dụng mobile.');
        }
      } catch (error) {
        console.error('[Auth] Error parsing user data:', error);
      }
    }
    setLoading(false);
  }, [performLogout]);

  // Kiểm tra token hết hạn định kỳ mỗi 60 giây
  useEffect(() => {
    if (!isLoggedIn) return;

    const intervalId = setInterval(() => {
      const token = sessionStorage.getItem('librarian_token');
      if (isTokenExpired(token)) {
        console.warn('[Auth] Token hết hạn, tự động đăng xuất');
        performLogout();
        alert('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
      }
    }, 60 * 1000);

    return () => clearInterval(intervalId);
  }, [isLoggedIn, performLogout]);

  const handleLogin = (role) => {
    const normalizedRole = role ? role.toString().toUpperCase() : null;
    setIsLoggedIn(true);
    setUserRole(normalizedRole);
  };

  if (loading) {
    return (
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
        fontSize: '18px',
        color: '#666',
        flexDirection: 'column',
        gap: '16px'
      }}>
        <div className="spinner" style={{
          border: '4px solid #f3f3f3',
          borderTop: '4px solid #F27125',
          borderRadius: '50%',
          width: '40px',
          height: '40px',
          animation: 'spin 1s linear infinite'
        }}></div>
        <p>Đang tải...</p>
        <style>{`
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }
        `}</style>
      </div>
    );
  }

  // Nếu chưa đăng nhập → Hiển thị trang Login
  if (!isLoggedIn) {
    return (
      <BrowserRouter>
        <Routes>
          <Route path="*" element={<AuthPage onLogin={handleLogin} />} />
        </Routes>
      </BrowserRouter>
    );
  }

  // Đã đăng nhập → Route theo role
  return (
    <BrowserRouter>
      <Routes>
        {/* LIBRARIAN Routes */}
        {userRole === 'LIBRARIAN' && (
          <Route path="/*" element={<LibrarianRoutes />} />
        )}

        {/* ADMIN Routes */}
        {userRole === 'ADMIN' && (
          <Route path="/*" element={<AdminRoutes />} />
        )}

        {/* Default redirect */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default AppRoutes;
