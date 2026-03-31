import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import AuthPage from '../components/AuthPage';
import { useAuth } from '../context/AuthContext';

/**
 * Login Page wrapper.
 * Handles login success and redirect.
 */
const LoginPage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { login, isAuthenticated } = useAuth();

    // If already logged in, redirect to dashboard.
    React.useEffect(() => {
        if (isAuthenticated) {
            const from = location.state?.from?.pathname || '/dashboard';
            navigate(from, { replace: true });
        }
    }, [isAuthenticated, navigate, location]);

    const handleLogin = () => {
        login();
        // Navigate to the page they tried to access, or dashboard.
        const from = location.state?.from?.pathname || '/dashboard';
        navigate(from, { replace: true });
    };

    return <AuthPage onLogin={handleLogin} />;
};

export default LoginPage;
