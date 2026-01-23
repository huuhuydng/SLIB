import React, { createContext, useContext, useState, useCallback } from 'react';
import { X, AlertCircle, CheckCircle, Info, AlertTriangle } from 'lucide-react';

const ModalContext = createContext();

export const useModal = () => {
    const context = useContext(ModalContext);
    if (!context) {
        throw new Error('useModal must be used within a ModalProvider');
    }
    return context;
};

export const ModalProvider = ({ children }) => {
    const [modal, setModal] = useState(null);

    const showAlert = useCallback((message, options = {}) => {
        return new Promise((resolve) => {
            setModal({
                type: 'alert',
                message,
                title: options.title || 'SLIB',
                icon: options.icon || 'info', // 'info', 'success', 'warning', 'error'
                onClose: () => {
                    setModal(null);
                    resolve();
                }
            });
        });
    }, []);

    const showConfirm = useCallback((message, options = {}) => {
        return new Promise((resolve) => {
            setModal({
                type: 'confirm',
                message,
                title: options.title || 'Xác nhận',
                icon: options.icon || 'warning',
                confirmText: options.confirmText || 'Xác nhận',
                cancelText: options.cancelText || 'Hủy',
                onConfirm: () => {
                    setModal(null);
                    resolve(true);
                },
                onCancel: () => {
                    setModal(null);
                    resolve(false);
                }
            });
        });
    }, []);

    const closeModal = useCallback(() => {
        setModal(null);
    }, []);

    const getIconComponent = (iconType) => {
        const iconStyle = { width: 28, height: 28 };
        switch (iconType) {
            case 'success':
                return <CheckCircle style={{ ...iconStyle, color: '#22c55e' }} />;
            case 'warning':
                return <AlertTriangle style={{ ...iconStyle, color: '#f59e0b' }} />;
            case 'error':
                return <AlertCircle style={{ ...iconStyle, color: '#ef4444' }} />;
            case 'info':
            default:
                return <Info style={{ ...iconStyle, color: '#3b82f6' }} />;
        }
    };

    const getIconBgColor = (iconType) => {
        switch (iconType) {
            case 'success': return '#dcfce7';
            case 'warning': return '#fef3c7';
            case 'error': return '#fee2e2';
            case 'info':
            default: return '#dbeafe';
        }
    };

    return (
        <ModalContext.Provider value={{ showAlert, showConfirm, closeModal }}>
            {children}

            {/* Modal Overlay */}
            {modal && (
                <div
                    style={{
                        position: 'fixed',
                        top: 0,
                        left: 0,
                        right: 0,
                        bottom: 0,
                        backgroundColor: 'rgba(0, 0, 0, 0.5)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        zIndex: 10000,
                        animation: 'fadeIn 0.2s ease-out'
                    }}
                    onClick={(e) => {
                        if (e.target === e.currentTarget) {
                            modal.type === 'alert' ? modal.onClose() : modal.onCancel?.();
                        }
                    }}
                >
                    <div
                        style={{
                            background: 'white',
                            borderRadius: '16px',
                            padding: '24px',
                            maxWidth: '420px',
                            width: '90%',
                            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
                            animation: 'slideUp 0.25s ease-out'
                        }}
                    >
                        {/* Header */}
                        <div style={{ display: 'flex', alignItems: 'flex-start', gap: '16px' }}>
                            {/* Icon */}
                            <div style={{
                                padding: '12px',
                                borderRadius: '12px',
                                background: getIconBgColor(modal.icon),
                                flexShrink: 0
                            }}>
                                {getIconComponent(modal.icon)}
                            </div>

                            {/* Content */}
                            <div style={{ flex: 1 }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <h3 style={{
                                        margin: 0,
                                        fontSize: '18px',
                                        fontWeight: 600,
                                        color: '#1e293b'
                                    }}>
                                        {modal.title}
                                    </h3>
                                    <button
                                        onClick={() => modal.type === 'alert' ? modal.onClose() : modal.onCancel?.()}
                                        style={{
                                            padding: '4px',
                                            border: 'none',
                                            background: 'transparent',
                                            cursor: 'pointer',
                                            color: '#94a3b8',
                                            borderRadius: '6px'
                                        }}
                                    >
                                        <X size={20} />
                                    </button>
                                </div>
                                <p style={{
                                    margin: '8px 0 0 0',
                                    fontSize: '14px',
                                    color: '#64748b',
                                    lineHeight: 1.5
                                }}>
                                    {modal.message}
                                </p>
                            </div>
                        </div>

                        {/* Actions */}
                        <div style={{
                            display: 'flex',
                            justifyContent: 'flex-end',
                            gap: '12px',
                            marginTop: '24px'
                        }}>
                            {modal.type === 'confirm' && (
                                <button
                                    onClick={modal.onCancel}
                                    style={{
                                        padding: '10px 20px',
                                        border: '1px solid #e2e8f0',
                                        background: 'white',
                                        borderRadius: '10px',
                                        cursor: 'pointer',
                                        fontSize: '14px',
                                        fontWeight: 500,
                                        color: '#64748b',
                                        transition: 'all 0.2s'
                                    }}
                                >
                                    {modal.cancelText}
                                </button>
                            )}
                            <button
                                onClick={modal.type === 'alert' ? modal.onClose : modal.onConfirm}
                                style={{
                                    padding: '10px 24px',
                                    border: 'none',
                                    background: modal.icon === 'error' ? 'linear-gradient(135deg, #ef4444 0%, #dc2626 100%)'
                                        : modal.icon === 'warning' ? 'linear-gradient(135deg, #f97316 0%, #ea580c 100%)'
                                            : 'linear-gradient(135deg, #f97316 0%, #ea580c 100%)',
                                    borderRadius: '10px',
                                    cursor: 'pointer',
                                    fontSize: '14px',
                                    fontWeight: 600,
                                    color: 'white',
                                    boxShadow: '0 4px 12px rgba(249, 115, 22, 0.3)',
                                    transition: 'all 0.2s'
                                }}
                            >
                                {modal.type === 'confirm' ? modal.confirmText : 'OK'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Animations */}
            <style>{`
        @keyframes fadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        @keyframes slideUp {
          from { 
            opacity: 0; 
            transform: translateY(20px) scale(0.95); 
          }
          to { 
            opacity: 1; 
            transform: translateY(0) scale(1); 
          }
        }
      `}</style>
        </ModalContext.Provider>
    );
};

export default ModalProvider;
