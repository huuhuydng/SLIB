import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import AuthPage from "./components/auth/AuthPage";
import AdminRoutes from "./routes/AdminRoutes";
import LibrarianRoutes from "./routes/LibrarianRoutes";

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

    if (loading) {
        return <div>Đang tải...</div>;
    }

    return (
        <BrowserRouter>
            <Routes>
                {/* Unified Login Route */}
                <Route path="/login" element={
                    isLoggedIn
                        ? (userRole === 'ADMIN'
                            ? <Navigate to="/admin/dashboard" replace />
                            : <Navigate to="/librarian/dashboard" replace />)
                        : <AuthPage onLogin={handleLogin} />
                } />

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
                <Route path="/" element={
                    isLoggedIn
                        ? (userRole === 'ADMIN' ? <Navigate to="/admin/dashboard" /> : <Navigate to="/librarian/dashboard" />)
                        : <Navigate to="/login" />
                } />

                {/* Fallback */}
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;