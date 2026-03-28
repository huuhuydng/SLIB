import React from 'react';
import { Outlet } from 'react-router-dom';
import { useKiosk } from '../../context/KioskContext';
import KioskLockScreen from '../../pages/kiosk/KioskLockScreen';
import '../../styles/kiosk/KioskGuard.css';

const KioskGuard = () => {
  const { isActivated, isLoading } = useKiosk();

  if (isLoading) {
    return (
      <div className="kiosk-guard__loading">
        <div className="kiosk-guard__spinner" />
        <span>Đang kiểm tra phiên kiosk...</span>
      </div>
    );
  }

  if (!isActivated) {
    return <KioskLockScreen />;
  }

  return <Outlet />;
};

export default KioskGuard;
