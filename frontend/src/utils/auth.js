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
 * Logout user and clear all authentication data
 */
export const handleLogout = () => {
  console.log('🔴 Logout triggered');

  // Clear ALL possible localStorage keys
  localStorage.removeItem('librarian_token');
  localStorage.removeItem('librarian_user');
  localStorage.removeItem('temp_reset_token');

  // Clear ALL possible sessionStorage keys
  sessionStorage.removeItem('librarian_token');
  sessionStorage.removeItem('librarian_user');

  console.log('✅ All tokens cleared');

  // Redirect to login page
  window.location.href = '/';
};
