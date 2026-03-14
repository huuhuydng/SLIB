import React from "react";
import { BrowserRouter, Routes, Route, Navigate, useLocation, useNavigate } from "react-router-dom";
import AuthPage from "./components/auth/AuthPage";
import AdminRoutes from "./routes/AdminRoutes";
import LibrarianRoutes from "./routes/LibrarianRoutes";
import KioskRoutes from "./routes/KioskRoutes";
import { ModalProvider } from "./components/shared/ModalContext";
import { ToastProvider } from "./components/common/ToastProvider";
import { ConfirmProvider } from "./components/common/ConfirmDialog";
import ChatWidget from "./components/chat/ChatWidget";
import { isTokenExpired } from "./utils/auth";
import { SessionExpired, TokenExpired, NotFound, ServerError, Forbidden, SessionTimeout } from "./pages/errors/ErrorPages";


const ConditionalChatWidget = () => {
    const location = useLocation();

    // Danh sách các đường dẫn muốn ẨN bóng bóng chat
    const hiddenRoutes = [
        '/admin/chat',
        '/librarian/chat',
        '/kiosk',                   // Kiosk mode
        '/login',                   // Trang login chung
        '/admin/login',             // Redirect cho admin
        '/librarian/login'          // Redirect cho librarian
    ];

    const shouldHide = hiddenRoutes.some(route => location.pathname.startsWith(route));

    return shouldHide ? null : <ChatWidget />;
};

function App() {
    const [isLoggedIn, setIsLoggedIn] = React.useState(false);
    const [userRole, setUserRole] = React.useState(null);
    const [loading, setLoading] = React.useState(true);

    // Hàm xóa toàn bộ token và logout
    const performLogout = React.useCallback(() => {
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

    React.useEffect(() => {
        // Check for existing auth - kiểm tra cả localStorage và sessionStorage
        const token = localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');
        const userStr = localStorage.getItem('librarian_user') || sessionStorage.getItem('librarian_user');

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
                const role = user.role;

                if (role === 'LIBRARIAN' || role === 'ADMIN') {
                    setIsLoggedIn(true);
                    setUserRole(role);
                }
            } catch (error) {
                console.error('Error parsing user:', error);
            }
        }
        setLoading(false);
    }, [performLogout]);

    // Kiểm tra token hết hạn định kỳ mỗi 60 giây
    React.useEffect(() => {
        if (!isLoggedIn) return;

        const intervalId = setInterval(() => {
            const token = localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');
            if (isTokenExpired(token)) {
                console.warn('[Auth] Token hết hạn, tự động đăng xuất');
                performLogout();
                window.location.href = '/session-expired';
            }
        }, 60 * 1000);

        return () => clearInterval(intervalId);
    }, [isLoggedIn, performLogout]);

    const handleLogin = (role) => {
        setIsLoggedIn(true);
        setUserRole(role);
    };

    // Update document title based on role (only when logged in)
    React.useEffect(() => {
        if (isLoggedIn && userRole === 'ADMIN') {
            document.title = 'SLIB - Quản trị viên';
        } else if (isLoggedIn && userRole === 'LIBRARIAN') {
            document.title = 'SLIB - Thủ thư';
        } else {
            document.title = 'SLIB';
        }
    }, [userRole, isLoggedIn]);

    if (loading) {
        return <div>Đang tải...</div>;
    }

    // Redirect to appropriate dashboard based on role after login
    const getDefaultRedirect = () => {
        if (!isLoggedIn) return '/login';
        return userRole === 'ADMIN' ? '/admin/dashboard' : '/librarian/dashboard';
    };

    return (
        <ToastProvider>
            <ConfirmProvider>
            <ModalProvider>
                <BrowserRouter>
                    <Routes>
                        {/* Unified Login Route */}
                        <Route path="/login" element={
                            isLoggedIn
                                ? <Navigate to={getDefaultRedirect()} replace />
                                : <AuthPage onLogin={handleLogin} />
                        } />

                        {/* Legacy login routes - redirect to unified login */}
                        <Route path="/admin/login" element={<Navigate to="/login" replace />} />
                        <Route path="/librarian/login" element={<Navigate to="/login" replace />} />

                        {/* Admin Routes */}
                        <Route path="/admin/*" element={
                            isLoggedIn && userRole === 'ADMIN'
                                ? <AdminRoutes />
                                : <Navigate to="/login" replace />
                        } />

                        {/* Librarian Routes */}
                        <Route path="/librarian/*" element={
                            isLoggedIn && userRole === 'LIBRARIAN'
                                ? <LibrarianRoutes />
                                : <Navigate to="/login" replace />
                        } />

                        {/* Kiosk Routes - Public, không cần đăng nhập */}
                        <Route path="/kiosk/*" element={<KioskRoutes />} />

                        {/* Root redirects based on role */}
                        <Route path="/" element={<Navigate to={getDefaultRedirect()} replace />} />

                        {/* Error Pages */}
                        <Route path="/session-expired" element={<SessionExpired />} />
                        <Route path="/token-expired" element={<TokenExpired />} />
                        <Route path="/server-error" element={<ServerError />} />
                        <Route path="/forbidden" element={<Forbidden />} />
                        <Route path="/session-timeout" element={<SessionTimeout />} />

                        {/* Fallback - 404 */}
                        <Route path="*" element={<NotFound />} />
                    </Routes>
                </BrowserRouter>
            </ModalProvider>
            </ConfirmProvider>
        </ToastProvider>
    );
}

export default App;