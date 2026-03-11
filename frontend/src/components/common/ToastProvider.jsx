import React, { createContext, useContext, useState, useCallback, useRef, useMemo } from 'react';
import { CheckCircle2, XCircle, AlertTriangle, Info, X } from 'lucide-react';

const ToastContext = createContext(null);

let toastIdCounter = 0;

const TOAST_CONFIG = {
    success: {
        icon: CheckCircle2,
        bg: 'linear-gradient(135deg, #10B981, #059669)',
        border: '#059669',
        lightBg: '#ECFDF5',
        color: '#065F46',
    },
    error: {
        icon: XCircle,
        bg: 'linear-gradient(135deg, #EF4444, #DC2626)',
        border: '#DC2626',
        lightBg: '#FEF2F2',
        color: '#991B1B',
    },
    warning: {
        icon: AlertTriangle,
        bg: 'linear-gradient(135deg, #F59E0B, #D97706)',
        border: '#D97706',
        lightBg: '#FFFBEB',
        color: '#92400E',
    },
    info: {
        icon: Info,
        bg: 'linear-gradient(135deg, #3B82F6, #2563EB)',
        border: '#2563EB',
        lightBg: '#EFF6FF',
        color: '#1E40AF',
    },
};

const Toast = ({ toast, onClose }) => {
    const config = TOAST_CONFIG[toast.type] || TOAST_CONFIG.info;
    const Icon = config.icon;

    return (
        <div
            style={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: '12px',
                padding: '16px 20px',
                background: '#fff',
                borderRadius: '14px',
                boxShadow: '0 8px 32px rgba(0,0,0,0.12), 0 2px 8px rgba(0,0,0,0.08)',
                borderLeft: `4px solid ${config.border}`,
                minWidth: '340px',
                maxWidth: '420px',
                animation: 'toastSlideIn 0.35s cubic-bezier(0.21, 1.02, 0.73, 1)',
                position: 'relative',
                overflow: 'hidden',
            }}
        >
            {/* Progress bar */}
            <div
                style={{
                    position: 'absolute',
                    bottom: 0,
                    left: 0,
                    height: '3px',
                    background: config.border,
                    borderRadius: '0 2px 2px 0',
                    animation: `toastProgress ${toast.duration || 4000}ms linear forwards`,
                    opacity: 0.6,
                }}
            />

            {/* Icon */}
            <div
                style={{
                    width: '36px',
                    height: '36px',
                    borderRadius: '10px',
                    background: config.bg,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                }}
            >
                <Icon size={20} color="#fff" />
            </div>

            {/* Content */}
            <div style={{ flex: 1, minWidth: 0 }}>
                {toast.title && (
                    <div
                        style={{
                            fontSize: '14px',
                            fontWeight: '700',
                            color: '#1A1A1A',
                            marginBottom: '2px',
                        }}
                    >
                        {toast.title}
                    </div>
                )}
                <div
                    style={{
                        fontSize: '13px',
                        color: '#6B7280',
                        lineHeight: '1.4',
                        wordBreak: 'break-word',
                    }}
                >
                    {toast.message}
                </div>
            </div>

            {/* Close */}
            <button
                onClick={() => onClose(toast.id)}
                style={{
                    background: 'none',
                    border: 'none',
                    padding: '4px',
                    cursor: 'pointer',
                    color: '#9CA3AF',
                    display: 'flex',
                    borderRadius: '6px',
                    transition: 'color 0.2s',
                    flexShrink: 0,
                }}
                onMouseEnter={(e) => (e.target.style.color = '#374151')}
                onMouseLeave={(e) => (e.target.style.color = '#9CA3AF')}
            >
                <X size={16} />
            </button>
        </div>
    );
};

export const ToastProvider = ({ children }) => {
    const [toasts, setToasts] = useState([]);
    const timersRef = useRef({});

    const removeToast = useCallback((id) => {
        if (timersRef.current[id]) {
            clearTimeout(timersRef.current[id]);
            delete timersRef.current[id];
        }
        setToasts((prev) => prev.filter((t) => t.id !== id));
    }, []);

    const addToast = useCallback(
        (type, message, options = {}) => {
            const id = ++toastIdCounter;
            const duration = options.duration || 4000;
            const toast = {
                id,
                type,
                message,
                title: options.title,
                duration,
            };

            setToasts((prev) => [...prev, toast]);

            timersRef.current[id] = setTimeout(() => {
                removeToast(id);
            }, duration);

            return id;
        },
        [removeToast]
    );

    const toast = useMemo(
        () => ({
            success: (message, options) => addToast('success', message, options),
            error: (message, options) => addToast('error', message, options),
            warning: (message, options) => addToast('warning', message, options),
            info: (message, options) => addToast('info', message, options),
        }),
        [addToast]
    );

    return (
        <ToastContext.Provider value={toast}>
            {children}

            {/* Toast Container */}
            {toasts.length > 0 && (
                <div
                    style={{
                        position: 'fixed',
                        top: '24px',
                        right: '24px',
                        zIndex: 99999,
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '10px',
                        pointerEvents: 'none',
                    }}
                >
                    {toasts.map((t) => (
                        <div key={t.id} style={{ pointerEvents: 'auto' }}>
                            <Toast toast={t} onClose={removeToast} />
                        </div>
                    ))}
                </div>
            )}

            {/* Animations */}
            <style>{`
        @keyframes toastSlideIn {
          from {
            opacity: 0;
            transform: translateX(100%);
          }
          to {
            opacity: 1;
            transform: translateX(0);
          }
        }
        @keyframes toastProgress {
          from { width: 100%; }
          to { width: 0%; }
        }
      `}</style>
        </ToastContext.Provider>
    );
};

export const useToast = () => {
    const context = useContext(ToastContext);
    if (!context) {
        // Fallback to alert if not inside ToastProvider
        return {
            success: (msg) => alert(msg),
            error: (msg) => alert(msg),
            warning: (msg) => alert(msg),
            info: (msg) => alert(msg),
        };
    }
    return context;
};

export default ToastProvider;
