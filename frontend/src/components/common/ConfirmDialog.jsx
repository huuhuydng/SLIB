import React, { createContext, useContext, useState, useCallback, useMemo, useEffect } from 'react';
import { AlertTriangle, X } from 'lucide-react';

const ConfirmContext = createContext(null);

const VARIANT_CONFIG = {
    danger: {
        color: '#DC2626',
        bg: 'linear-gradient(135deg, #EF4444, #DC2626)',
        lightBg: '#FEF2F2',
        btnBg: '#DC2626',
        btnHover: '#B91C1C',
    },
    warning: {
        color: '#D97706',
        bg: 'linear-gradient(135deg, #F59E0B, #D97706)',
        lightBg: '#FFFBEB',
        btnBg: '#D97706',
        btnHover: '#B45309',
    },
    info: {
        color: '#2563EB',
        bg: 'linear-gradient(135deg, #3B82F6, #2563EB)',
        lightBg: '#EFF6FF',
        btnBg: '#2563EB',
        btnHover: '#1D4ED8',
    },
};

const ConfirmDialogModal = ({ config, onConfirm, onCancel }) => {
    const variant = VARIANT_CONFIG[config.variant] || VARIANT_CONFIG.warning;

    useEffect(() => {
        const handleKey = (e) => {
            if (e.key === 'Escape') onCancel();
            if (e.key === 'Enter') onConfirm();
        };
        window.addEventListener('keydown', handleKey);
        return () => window.removeEventListener('keydown', handleKey);
    }, [onConfirm, onCancel]);

    return (
        <div
            style={{
                position: 'fixed',
                inset: 0,
                zIndex: 100000,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: 'rgba(0, 0, 0, 0.35)',
                backdropFilter: 'blur(6px)',
                animation: 'confirmFadeIn 0.2s ease',
            }}
            onClick={onCancel}
        >
            <div
                style={{
                    background: '#fff',
                    borderRadius: '28px',
                    boxShadow: '0 25px 80px rgba(0,0,0,0.15), 0 8px 24px rgba(0,0,0,0.08)',
                    width: '540px',
                    maxWidth: '90vw',
                    overflow: 'hidden',
                    animation: 'confirmSlideUp 0.3s cubic-bezier(0.21, 1.02, 0.73, 1)',
                    textAlign: 'center',
                    position: 'relative',
                }}
                onClick={(e) => e.stopPropagation()}
            >
                {/* Close button */}
                <button
                    onClick={onCancel}
                    style={{
                        position: 'absolute',
                        top: '18px',
                        right: '18px',
                        background: 'none',
                        border: 'none',
                        padding: '6px',
                        cursor: 'pointer',
                        color: '#B0B0B0',
                        display: 'flex',
                        borderRadius: '8px',
                        transition: 'color 0.2s',
                        zIndex: 1,
                    }}
                    onMouseEnter={(e) => (e.target.style.color = '#666')}
                    onMouseLeave={(e) => (e.target.style.color = '#B0B0B0')}
                >
                    <X size={22} />
                </button>

                {/* Icon */}
                <div style={{ paddingTop: '40px', display: 'flex', justifyContent: 'center' }}>
                    <div
                        style={{
                            width: '64px',
                            height: '64px',
                            borderRadius: '50%',
                            background: variant.lightBg,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                        }}
                    >
                        <div
                            style={{
                                width: '44px',
                                height: '44px',
                                borderRadius: '50%',
                                background: variant.bg,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                            }}
                        >
                            <AlertTriangle size={22} color="#fff" />
                        </div>
                    </div>
                </div>

                {/* Title */}
                <div
                    style={{
                        fontSize: '22px',
                        fontWeight: '700',
                        color: '#1A1A1A',
                        marginTop: '24px',
                        padding: '0 44px',
                    }}
                >
                    {config.title || 'Xác nhận'}
                </div>

                {/* Message */}
                <div
                    style={{
                        fontSize: '15px',
                        color: '#6B7280',
                        lineHeight: '1.7',
                        marginTop: '14px',
                        padding: '0 44px',
                    }}
                >
                    {config.message}
                </div>

                {/* Buttons */}
                <div
                    style={{
                        display: 'flex',
                        gap: '12px',
                        padding: '32px 44px 40px',
                    }}
                >
                    <button
                        onClick={onConfirm}
                        style={{
                            flex: 1,
                            padding: '14px 20px',
                            borderRadius: '14px',
                            border: 'none',
                            background: variant.btnBg,
                            color: '#fff',
                            fontSize: '15px',
                            fontWeight: '600',
                            cursor: 'pointer',
                            transition: 'background 0.2s, transform 0.1s',
                        }}
                        onMouseEnter={(e) => (e.target.style.background = variant.btnHover)}
                        onMouseLeave={(e) => (e.target.style.background = variant.btnBg)}
                    >
                        {config.confirmText || 'Xác nhận'}
                    </button>
                    <button
                        onClick={onCancel}
                        style={{
                            flex: 1,
                            padding: '14px 20px',
                            borderRadius: '14px',
                            border: '1.5px solid #E5E7EB',
                            background: '#fff',
                            color: '#374151',
                            fontSize: '15px',
                            fontWeight: '600',
                            cursor: 'pointer',
                            transition: 'background 0.2s',
                        }}
                        onMouseEnter={(e) => (e.target.style.background = '#F9FAFB')}
                        onMouseLeave={(e) => (e.target.style.background = '#fff')}
                    >
                        {config.cancelText || 'Huỷ'}
                    </button>
                </div>
            </div>

            <style>{`
                @keyframes confirmFadeIn {
                    from { opacity: 0; }
                    to { opacity: 1; }
                }
                @keyframes confirmSlideUp {
                    from { opacity: 0; transform: translateY(24px) scale(0.95); }
                    to { opacity: 1; transform: translateY(0) scale(1); }
                }
            `}</style>
        </div>
    );
};

export const ConfirmProvider = ({ children }) => {
    const [dialogState, setDialogState] = useState(null);

    const confirm = useCallback((options) => {
        return new Promise((resolve) => {
            setDialogState({ ...options, resolve });
        });
    }, []);

    const handleConfirm = useCallback(() => {
        if (dialogState?.resolve) dialogState.resolve(true);
        setDialogState(null);
    }, [dialogState]);

    const handleCancel = useCallback(() => {
        if (dialogState?.resolve) dialogState.resolve(false);
        setDialogState(null);
    }, [dialogState]);

    const value = useMemo(() => ({ confirm }), [confirm]);

    return (
        <ConfirmContext.Provider value={value}>
            {children}
            {dialogState && (
                <ConfirmDialogModal
                    config={dialogState}
                    onConfirm={handleConfirm}
                    onCancel={handleCancel}
                />
            )}
        </ConfirmContext.Provider>
    );
};

export const useConfirm = () => {
    const context = useContext(ConfirmContext);
    if (!context) {
        return {
            confirm: async (options) => window.confirm(options.message || options),
        };
    }
    return context;
};

export default ConfirmProvider;
