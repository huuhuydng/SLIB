import React, { createContext, useContext, useState, useEffect } from 'react';
import librarianService from '../services/librarianService';

const AuthContext = createContext(null);

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [token, setToken] = useState(null);
    const [isLoading, setIsLoading] = useState(true);

    // Check for existing token on mount.
    useEffect(() => {
        const storedToken = localStorage.getItem('librarian_token');
        const storedUser = localStorage.getItem('librarian_user');

        if (storedToken && storedUser) {
            try {
                setToken(storedToken);
                setUser(JSON.parse(storedUser));
            } catch (error) {
                console.error('Error parsing stored user:', error);
                // Clear invalid data.
                localStorage.removeItem('librarian_token');
                localStorage.removeItem('librarian_user');
            }
        }
        setIsLoading(false);
    }, []);

    const login = () => {
        // Called after successful Google login.
        const storedToken = localStorage.getItem('librarian_token');
        const storedUser = localStorage.getItem('librarian_user');

        if (storedToken && storedUser) {
            setToken(storedToken);
            try {
                setUser(JSON.parse(storedUser));
            } catch (e) {
                setUser({ email: 'admin' });
            }
        }
    };

    const logout = () => {
        librarianService.logout();
        setUser(null);
        setToken(null);
        // Redirect to login.
        window.location.href = '/login';
    };

    const isAuthenticated = !!token && !!user;

    const value = {
        user,
        token,
        isLoading,
        isAuthenticated,
        login,
        logout,
    };

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};

export default AuthContext;
