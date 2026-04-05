const AUTH_NOTICE_KEY = 'slib_auth_notice';

/**
 * Check if a JWT token is expired
 * Decodes the payload (base64) and compares the exp claim with current time
 * Returns true if token is expired or invalid
 * @param {string} token - JWT token string
 * @returns {boolean}
 */
export const isTokenExpired = (token) => {
  if (!token) return true;

  try {
    // JWT format: header.payload.signature
    const parts = token.split('.');
    if (parts.length !== 3) return true;

    // Decode payload (base64url -> base64 -> JSON)
    const payload = parts[1]
      .replace(/-/g, '+')
      .replace(/_/g, '/');
    const decoded = JSON.parse(atob(payload));

    if (!decoded.exp) return true;

    // exp is in seconds, Date.now() is in milliseconds
    // Add 30 second buffer to avoid edge cases
    const now = Math.floor(Date.now() / 1000);
    return decoded.exp < (now + 30);
  } catch (error) {
    console.error('[Auth] Error decoding token:', error);
    return true;
  }
};

/**
 * Clear all authentication data for staff web flows.
 */
export const clearAuthStorage = () => {
  localStorage.removeItem('librarian_token');
  localStorage.removeItem('librarian_user');
  localStorage.removeItem('temp_reset_token');
  localStorage.removeItem('refresh_token');

  sessionStorage.removeItem('librarian_token');
  sessionStorage.removeItem('librarian_user');
  sessionStorage.removeItem('refresh_token');
};

export const setAuthNotice = (notice) => {
  try {
    sessionStorage.setItem(AUTH_NOTICE_KEY, JSON.stringify(notice));
  } catch (error) {
    console.warn('[Auth] Không thể lưu thông báo xác thực:', error);
  }
};

export const consumeAuthNotice = () => {
  try {
    const raw = sessionStorage.getItem(AUTH_NOTICE_KEY);
    if (!raw) {
      return null;
    }

    sessionStorage.removeItem(AUTH_NOTICE_KEY);
    return JSON.parse(raw);
  } catch (error) {
    sessionStorage.removeItem(AUTH_NOTICE_KEY);
    console.warn('[Auth] Không thể đọc thông báo xác thực:', error);
    return null;
  }
};

/**
 * Logout user and redirect to login page with a one-time notice.
 */
export const handleLogout = ({
  redirectTo = '/login',
  notice = {
    type: 'success',
    title: 'Đăng xuất thành công',
    message: 'Bạn đã đăng xuất khỏi hệ thống.'
  }
} = {}) => {
  clearAuthStorage();

  if (notice) {
    setAuthNotice(notice);
  }

  window.location.href = redirectTo;
};
