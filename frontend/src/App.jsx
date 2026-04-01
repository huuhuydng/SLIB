import React, { Suspense, lazy } from "react";
import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom";
import { ModalProvider } from "./components/shared/ModalContext";
import { ToastProvider } from "./components/common/ToastProvider";
import { ConfirmProvider } from "./components/common/ConfirmDialog";
import { isTokenExpired } from "./utils/auth";
const AuthPage = lazy(() => import("./components/auth/AuthPage"));
const AdminRoutes = lazy(() => import("./routes/AdminRoutes"));
const LibrarianRoutes = lazy(() => import("./routes/LibrarianRoutes"));
const KioskRoutes = lazy(() => import("./routes/KioskRoutes"));
const ChatWidget = lazy(() => import("./components/chat/ChatWidget"));
const SessionExpired = lazy(() => import("./pages/errors/ErrorPages").then((module) => ({ default: module.SessionExpired })));
const TokenExpired = lazy(() => import("./pages/errors/ErrorPages").then((module) => ({ default: module.TokenExpired })));
const NotFound = lazy(() => import("./pages/errors/ErrorPages").then((module) => ({ default: module.NotFound })));
const ServerError = lazy(() => import("./pages/errors/ErrorPages").then((module) => ({ default: module.ServerError })));
const Forbidden = lazy(() => import("./pages/errors/ErrorPages").then((module) => ({ default: module.Forbidden })));
const SessionTimeout = lazy(() => import("./pages/errors/ErrorPages").then((module) => ({ default: module.SessionTimeout })));

const LoadingScreen = () => <div>Đang tải...</div>;


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

    const getDefaultRedirect = React.useCallback(() => {
        if (!isLoggedIn) {
            return "/login";
        }

        if (userRole === "ADMIN") {
            return "/admin/dashboard";
        }

        if (userRole === "LIBRARIAN") {
            return "/librarian/dashboard";
        }

        return "/login";
    }, [isLoggedIn, userRole]);

    const getLegacyRedirect = React.useCallback((legacyPath) => {
        if (!isLoggedIn) {
            return "/login";
        }

        if (userRole === "ADMIN") {
            const adminRouteMap = {
                "/dashboard": "/admin/dashboard",
                "/library-map": "/admin/library-map",
                "/users": "/admin/users",
                "/devices": "/admin/devices",
                "/config": "/admin/config",
                "/health": "/admin/health",
                "/ai-config": "/admin/ai-config",
                "/nfc-management": "/admin/nfc-management",
                "/kiosk": "/admin/kiosk",
                "/settings": "/admin/settings",
            };

            return adminRouteMap[legacyPath] || "/admin/dashboard";
        }

        if (userRole === "LIBRARIAN") {
            const librarianRouteMap = {
                "/dashboard": "/librarian/dashboard",
                "/chat": "/librarian/chat",
                "/checkinout": "/librarian/checkinout",
                "/seatmanage": "/librarian/seatmanage",
                "/bookings": "/librarian/bookings",
                "/students": "/librarian/students",
                "/violation": "/librarian/violation",
                "/support-requests": "/librarian/support-requests",
                "/complaints": "/librarian/complaints",
                "/seat-status-reports": "/librarian/seat-status-reports",
                "/feedback": "/librarian/feedback",
                "/statistic": "/librarian/statistic",
                "/news": "/librarian/news",
                "/new-books": "/librarian/new-books",
                "/settings": "/librarian/settings",
            };

            return librarianRouteMap[legacyPath] || "/librarian/dashboard";
        }

        return "/login";
    }, [isLoggedIn, userRole]);

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
        return <LoadingScreen />;
    }

    return (
        <ToastProvider>
            <ConfirmProvider>
            <ModalProvider>
                <BrowserRouter>
                    <Suspense fallback={<LoadingScreen />}>
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

                            {/* Legacy bare routes - keep old bookmarks working */}
                            <Route path="/dashboard" element={<Navigate to={getLegacyRedirect("/dashboard")} replace />} />
                            <Route path="/library-map" element={<Navigate to={getLegacyRedirect("/library-map")} replace />} />
                            <Route path="/users" element={<Navigate to={getLegacyRedirect("/users")} replace />} />
                            <Route path="/devices" element={<Navigate to={getLegacyRedirect("/devices")} replace />} />
                            <Route path="/config" element={<Navigate to={getLegacyRedirect("/config")} replace />} />
                            <Route path="/health" element={<Navigate to={getLegacyRedirect("/health")} replace />} />
                            <Route path="/ai-config" element={<Navigate to={getLegacyRedirect("/ai-config")} replace />} />
                            <Route path="/chat" element={<Navigate to={getLegacyRedirect("/chat")} replace />} />
                            <Route path="/checkinout" element={<Navigate to={getLegacyRedirect("/checkinout")} replace />} />
                            <Route path="/seatmanage" element={<Navigate to={getLegacyRedirect("/seatmanage")} replace />} />
                            <Route path="/bookings" element={<Navigate to={getLegacyRedirect("/bookings")} replace />} />
                            <Route path="/students" element={<Navigate to={getLegacyRedirect("/students")} replace />} />
                            <Route path="/violation" element={<Navigate to={getLegacyRedirect("/violation")} replace />} />
                            <Route path="/support-requests" element={<Navigate to={getLegacyRedirect("/support-requests")} replace />} />
                            <Route path="/complaints" element={<Navigate to={getLegacyRedirect("/complaints")} replace />} />
                            <Route path="/seat-status-reports" element={<Navigate to={getLegacyRedirect("/seat-status-reports")} replace />} />
                            <Route path="/feedback" element={<Navigate to={getLegacyRedirect("/feedback")} replace />} />
                            <Route path="/statistic" element={<Navigate to={getLegacyRedirect("/statistic")} replace />} />
                            <Route path="/news" element={<Navigate to={getLegacyRedirect("/news")} replace />} />
                            <Route path="/new-books" element={<Navigate to={getLegacyRedirect("/new-books")} replace />} />
                            <Route path="/nfc-management" element={<Navigate to={getLegacyRedirect("/nfc-management")} replace />} />

                            {/* Error Pages */}
                            <Route path="/session-expired" element={<SessionExpired />} />
                            <Route path="/token-expired" element={<TokenExpired />} />
                            <Route path="/server-error" element={<ServerError />} />
                            <Route path="/forbidden" element={<Forbidden />} />
                            <Route path="/session-timeout" element={<SessionTimeout />} />

                            {/* Fallback - 404 */}
                            <Route path="*" element={<NotFound />} />
                        </Routes>
                    </Suspense>
                </BrowserRouter>
            </ModalProvider>
            </ConfirmProvider>
        </ToastProvider>
    );
}

export default App;
