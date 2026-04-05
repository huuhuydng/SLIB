import React, { createContext, useContext, useState, useEffect } from 'react';
import { API_BASE_URL } from '../config/apiConfig';
import kioskService from '../services/kiosk/kioskService';

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

    fetch(`${API_BASE_URL}/slib/kiosk/session/activate`, {
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

  useEffect(() => {
    if (!kioskToken || !isActivated) {
      return undefined;
    }

    const heartbeat = async () => {
      try {
        await kioskService.heartbeat();
      } catch {
        // Để interceptor hiện tại xử lý 401/403 và đưa kiosk về màn hình khóa nếu cần.
      }
    };

    heartbeat();
    const timer = setInterval(heartbeat, 60000);
    return () => clearInterval(timer);
  }, [kioskToken, isActivated]);

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
