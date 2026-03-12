import React, { createContext, useContext, useState, useEffect } from 'react';

const KioskContext = createContext(null);

export const useKiosk = () => useContext(KioskContext);

export const KioskProvider = ({ children }) => {
  const [kioskToken, setKioskToken] = useState(() => localStorage.getItem('kiosk_device_token'));
  const [kioskConfig, setKioskConfig] = useState(() => {
    const stored = localStorage.getItem('kiosk_config');
    try {
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });
  const [isActivated, setIsActivated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  // Validate token on mount or when token changes
  useEffect(() => {
    if (!kioskToken) {
      setIsActivated(false);
      setIsLoading(false);
      return;
    }

    const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
    fetch(`${API_BASE}/slib/kiosk/session/activate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token: kioskToken }),
    })
      .then(res => {
        if (res.ok) return res.json();
        throw new Error('Token không hợp lệ');
      })
      .then(data => {
        setKioskConfig(data);
        localStorage.setItem('kiosk_config', JSON.stringify(data));
        setIsActivated(true);
      })
      .catch(() => {
        localStorage.removeItem('kiosk_device_token');
        localStorage.removeItem('kiosk_config');
        setKioskToken(null);
        setKioskConfig(null);
        setIsActivated(false);
      })
      .finally(() => setIsLoading(false));
  }, [kioskToken]);

  const activate = (token, config) => {
    localStorage.setItem('kiosk_device_token', token);
    localStorage.setItem('kiosk_config', JSON.stringify(config));
    setKioskToken(token);
    setKioskConfig(config);
    setIsActivated(true);
  };

  const deactivate = () => {
    localStorage.removeItem('kiosk_device_token');
    localStorage.removeItem('kiosk_config');
    setKioskToken(null);
    setKioskConfig(null);
    setIsActivated(false);
  };

  const getKioskCode = () => {
    if (kioskConfig && kioskConfig.kioskCode) return kioskConfig.kioskCode;
    return 'KIOSK_001';
  };

  return (
    <KioskContext.Provider value={{ kioskToken, kioskConfig, isActivated, isLoading, activate, deactivate, getKioskCode }}>
      {children}
    </KioskContext.Provider>
  );
};
