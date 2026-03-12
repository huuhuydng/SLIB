import React, { useState } from 'react';
import { ShieldOff, KeyRound, Loader2, Hash } from 'lucide-react';
import { useKiosk } from '../../context/KioskContext';
import logo from '../../assets/logo.png';
import './KioskLockScreen.css';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const KioskLockScreen = () => {
  const { activate } = useKiosk();
  const [mode, setMode] = useState('code'); // 'code' | 'token'
  const [code, setCode] = useState('');
  const [token, setToken] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleActivateByCode = async (e) => {
    e.preventDefault();
    const trimmed = code.trim().toUpperCase();
    if (!trimmed || trimmed.length !== 6) {
      setError('Vui lòng nhập mã kích hoạt 6 ký tự');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const res = await fetch(`${API_BASE}/slib/kiosk/session/activate-code`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code: trimmed }),
      });

      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message || 'Mã kích hoạt không hợp lệ hoặc đã hết hạn');
      }

      const data = await res.json();
      // data should contain { deviceToken, ...kioskConfig }
      const deviceToken = data.deviceToken || data.token;
      activate(deviceToken, data);
    } catch (err) {
      setError(err.message || 'Không thể kích hoạt thiết bị. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  const handleActivateByToken = async (e) => {
    e.preventDefault();
    const trimmed = token.trim();
    if (!trimmed) {
      setError('Vui lòng nhập mã kích hoạt');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const res = await fetch(`${API_BASE}/slib/kiosk/session/activate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: trimmed }),
      });

      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message || 'Mã kích hoạt không hợp lệ hoặc đã hết hạn');
      }

      const data = await res.json();
      activate(trimmed, data);
    } catch (err) {
      setError(err.message || 'Không thể kích hoạt thiết bị. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  const handleCodeChange = (e) => {
    const val = e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 6);
    setCode(val);
    setError('');
  };

  return (
    <div className="kiosk-lock">
      <div className="kiosk-lock__card">
        <div className="kiosk-lock__logo-row">
          <img src={logo} alt="SLIB" className="kiosk-lock__logo" />
        </div>

        <div className="kiosk-lock__icon-ring">
          <ShieldOff size={48} strokeWidth={1.5} />
        </div>

        <h1 className="kiosk-lock__title">Thiết bị chưa được kích hoạt</h1>
        <p className="kiosk-lock__subtitle">
          Vui lòng nhập mã kích hoạt do quản trị viên cung cấp.
        </p>

        {/* Mode tabs */}
        <div className="kiosk-lock__tabs">
          <button
            className={`kiosk-lock__tab${mode === 'code' ? ' kiosk-lock__tab--active' : ''}`}
            onClick={() => { setMode('code'); setError(''); }}
            disabled={loading}
          >
            <Hash size={14} />
            Mã kích hoạt
          </button>
          <button
            className={`kiosk-lock__tab${mode === 'token' ? ' kiosk-lock__tab--active' : ''}`}
            onClick={() => { setMode('token'); setError(''); }}
            disabled={loading}
          >
            <KeyRound size={14} />
            Token đầy đủ
          </button>
        </div>

        {mode === 'code' ? (
          <form className="kiosk-lock__form" onSubmit={handleActivateByCode}>
            <label className="kiosk-lock__label" htmlFor="kiosk-activation-code">
              <Hash size={16} />
              Nhập mã 6 ký tự
            </label>
            <input
              id="kiosk-activation-code"
              className="kiosk-lock__input kiosk-lock__input--code"
              type="text"
              value={code}
              onChange={handleCodeChange}
              placeholder="VD: A3F9K2"
              autoComplete="off"
              disabled={loading}
              maxLength={6}
            />

            {error && <p className="kiosk-lock__error">{error}</p>}

            <button
              type="submit"
              className="kiosk-lock__btn"
              disabled={loading || code.length !== 6}
            >
              {loading ? (
                <>
                  <Loader2 size={16} className="kiosk-lock__spinner" />
                  Đang kích hoạt...
                </>
              ) : (
                'Kích hoạt thiết bị'
              )}
            </button>
          </form>
        ) : (
          <form className="kiosk-lock__form" onSubmit={handleActivateByToken}>
            <label className="kiosk-lock__label" htmlFor="kiosk-activation-token">
              <KeyRound size={16} />
              Token kích hoạt
            </label>
            <input
              id="kiosk-activation-token"
              className="kiosk-lock__input"
              type="text"
              value={token}
              onChange={(e) => { setToken(e.target.value); setError(''); }}
              placeholder="Nhập token do quản trị viên cung cấp"
              autoComplete="off"
              disabled={loading}
            />

            {error && <p className="kiosk-lock__error">{error}</p>}

            <button
              type="submit"
              className="kiosk-lock__btn"
              disabled={loading || !token.trim()}
            >
              {loading ? (
                <>
                  <Loader2 size={16} className="kiosk-lock__spinner" />
                  Đang kích hoạt...
                </>
              ) : (
                'Kích hoạt thiết bị'
              )}
            </button>
          </form>
        )}

        <p className="kiosk-lock__hint">
          Nếu cần hỗ trợ, vui lòng liên hệ thủ thư hoặc quản trị viên hệ thống SLIB.
        </p>
      </div>
    </div>
  );
};

export default KioskLockScreen;
