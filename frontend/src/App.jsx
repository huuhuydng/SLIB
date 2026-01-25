import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import AuthPage from "./components/auth/AuthPage";
import AdminRoutes from "./routes/AdminRoutes";
import LibrarianRoutes from "./routes/LibrarianRoutes";
import { ModalProvider } from "./components/shared/ModalContext";

function App() {
    const [isLoggedIn, setIsLoggedIn] = React.useState(false);
    const [userRole, setUserRole] = React.useState(null);
    const [loading, setLoading] = React.useState(true);

    React.useEffect(() => {
        // Check for existing auth
        const token = localStorage.getItem('librarian_token');
        const userStr = localStorage.getItem('librarian_user');

        if (token && userStr) {
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
    }, []);

    const handleLogin = (role) => {
        setIsLoggedIn(true);
        setUserRole(role);
    };

    // Update document title based on role (only when logged in)
    React.useEffect(() => {
        if (isLoggedIn && userRole === 'ADMIN') {
            document.title = 'SLIB - Admin';
        } else if (isLoggedIn && userRole === 'LIBRARIAN') {
            document.title = 'SLIB - Thủ Thư';
        } else {
            document.title = 'SLIB';
        }
    }, [userRole, isLoggedIn]);

    if (loading) {
        return <div>Đang tải...</div>;
    }

    return (
        <ModalProvider>
            <BrowserRouter>
                <Routes>
                    {/* Admin Routes */}
                    <Route path="/admin/login" element={
                        isLoggedIn && userRole === 'ADMIN'
                            ? <Navigate to="/admin/dashboard" replace />
                            : <AuthPage onLogin={handleLogin} />
                    } />
                    <Route path="/admin/*" element={
                        isLoggedIn && userRole === 'ADMIN'
                            ? <AdminRoutes />
                            : <Navigate to="/admin/login" replace />
                    } />

                    {/* Librarian Routes */}
                    <Route path="/librarian/login" element={
                        isLoggedIn && userRole === 'LIBRARIAN'
                            ? <Navigate to="/librarian/dashboard" replace />
                            : <AuthPage onLogin={handleLogin} />
                    } />
                    <Route path="/librarian/*" element={
                        isLoggedIn && userRole === 'LIBRARIAN'
                            ? <LibrarianRoutes />
                            : <Navigate to="/librarian/login" replace />
                    } />

                    {/* Root redirects based on role */}
                    <Route path="/" element={
                        isLoggedIn
                            ? (userRole === 'ADMIN' ? <Navigate to="/admin/dashboard" /> : <Navigate to="/librarian/dashboard" />)
                            : <Navigate to="/admin/login" />
                    } />

                    {/* Fallback */}
                    <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
            </BrowserRouter>
        </ModalProvider>
    );
}

export default App;