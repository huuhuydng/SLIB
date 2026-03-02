import React from "react";
import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom";
import AuthPage from "./components/auth/AuthPage";
import AdminRoutes from "./routes/AdminRoutes";
import LibrarianRoutes from "./routes/LibrarianRoutes";
import { ModalProvider } from "./components/shared/ModalContext";
import ChatWidget from "./components/ChatWidget";
import { isTokenExpired } from "./utils/auth";


const ConditionalChatWidget = () => {
    const location = useLocation();

    // Danh sach cac duong dan muon AN bong bong chat
    const hiddenRoutes = [
        '/admin/chat',
        '/librarian/chat',
        '/login',                   // Trang login chung
        '/admin/login',             // Redirect cu
        '/librarian/login'          // Redirect cu
    ];

    const shouldHide = hiddenRoutes.some(route => location.pathname.startsWith(route));

    return shouldHide ? null : <ChatWidget />;
};

function App() {
    const [isLoggedIn, setIsLoggedIn] = React.useState(false);
    const [userRole, setUserRole] = React.useState(null);
    const [loading, setLoading] = React.useState(true);

    // Ham xoa toan bo token va logout
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
        // Check for existing auth - kiem tra ca localStorage va sessionStorage
        const token = localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');
        const userStr = localStorage.getItem('librarian_user') || sessionStorage.getItem('librarian_user');

        if (token && userStr) {
            // Kiem tra token het han
            if (isTokenExpired(token)) {
                console.warn('[Auth] Token da het han, yeu cau dang nhap lai');
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

    // Kiem tra token het han dinh ky moi 60 giay
    React.useEffect(() => {
        if (!isLoggedIn) return;

        const intervalId = setInterval(() => {
            const token = localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');
            if (isTokenExpired(token)) {
                console.warn('[Auth] Token het han, tu dong dang xuat');
                performLogout();
                alert('Phien dang nhap da het han. Vui long dang nhap lai.');
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
            document.title = 'SLIB - Admin';
        } else if (isLoggedIn && userRole === 'LIBRARIAN') {
            document.title = 'SLIB - Thu Thu';
        } else {
            document.title = 'SLIB';
        }
    }, [userRole, isLoggedIn]);

    if (loading) {
        return <div>Dang tai...</div>;
    }

    // Redirect to appropriate dashboard based on role after login
    const getDefaultRedirect = () => {
        if (!isLoggedIn) return '/login';
        return userRole === 'ADMIN' ? '/admin/dashboard' : '/librarian/dashboard';
    };

    return (
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

                    {/* Root redirects based on role */}
                    <Route path="/" element={<Navigate to={getDefaultRedirect()} replace />} />

                    {/* Fallback */}
                    <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
            </BrowserRouter>
        </ModalProvider>
    );
}

export default App;