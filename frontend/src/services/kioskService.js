import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const kioskApi = axios.create({
  baseURL: `${API_BASE_URL}/slib/kiosk`,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add auth token if available
kioskApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('librarian_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * Kiosk Service
 * Handles all kiosk-related API calls
 */
const kioskService = {
  /**
   * Generate QR code for kiosk
   * @param {string} kioskCode - Kiosk code (e.g., 'KIOSK_001')
   */
  generateQr: async (kioskCode) => {
    const response = await kioskApi.get(`/qr/generate/${kioskCode}`);
    return response.data;
  },

  /**
   * Validate QR code from mobile app
   * @param {string} qrPayload - The QR payload string
   * @param {string} kioskCode - Kiosk code
   */
  validateQr: async (qrPayload, kioskCode) => {
    const response = await kioskApi.post('/qr/validate', {
      qrPayload,
      kioskCode,
    });
    return response.data;
  },

  /**
   * Complete session after mobile authentication
   * @param {string} sessionToken - Session token
   * @param {string} userId - User ID
   */
  completeSession: async (sessionToken, userId) => {
    const response = await kioskApi.post('/session/complete', {
      sessionToken,
      userId,
    });
    return response.data;
  },

  /**
   * Get current active session for kiosk
   * @param {string} kioskCode - Kiosk code
   */
  getActiveSession: async (kioskCode) => {
    const response = await kioskApi.get(`/session/${kioskCode}`);
    return response.data;
  },

  /**
   * Check out and close session
   * @param {string} sessionToken - Session token
   */
  checkOut: async (sessionToken) => {
    const response = await kioskApi.post('/session/checkout', {
      sessionToken,
    });
    return response.data;
  },

  /**
   * Chỉ expire session kiosk (không check-out khỏi thư viện)
   * Dùng khi timeout hoặc bấm back
   */
  expireSession: async (sessionToken) => {
    const response = await kioskApi.post('/session/expire', {
      sessionToken,
    });
    return response.data;
  },

  /**
   * Check-in tại kiosk
   * @param {string} sessionToken - Session token
   */
  checkIn: async (sessionToken) => {
    const response = await kioskApi.post('/session/checkin', {
      sessionToken,
    });
    return response.data;
  },

  /**
   * Get today's statistics
   */
  getTodayStats: async () => {
    const response = await kioskApi.get('/monitor/stats');
    return response.data;
  },

  /**
   * Get recent entry logs
   * @param {number} limit - Number of records to fetch
   */
  getRecentLogs: async (limit = 10) => {
    const response = await kioskApi.get(`/monitor/logs?limit=${limit}`);
    return response.data;
  },
};

export default kioskService;
