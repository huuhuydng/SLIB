import axios from 'axios';
import { API_BASE_URL } from '../../config/apiConfig';

const kioskApi = axios.create({
  baseURL: `${API_BASE_URL}/slib/kiosk`,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Sử dụng kiosk device token thay vì librarian token
kioskApi.interceptors.request.use((config) => {
  const kioskToken = localStorage.getItem('kiosk_device_token');
  if (kioskToken) {
    config.headers.Authorization = `Bearer ${kioskToken}`;
  }
  return config;
});

// Xử lý lỗi 401 - token hết hạn hoặc không hợp lệ
kioskApi.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Xóa token kiosk và chuyển về màn hình khóa
      localStorage.removeItem('kiosk_device_token');
      localStorage.removeItem('kiosk_config');
      window.location.href = '/kiosk/';
    }
    if (error.response?.status === 403) {
      // Kiosk bị từ chối truy cập
      window.dispatchEvent(new CustomEvent('kiosk-auth-error', {
        detail: error.response.data
      }));
    }
    return Promise.reject(error);
  }
);

/**
 * Lấy kiosk code từ localStorage (fallback an toàn)
 */
const getKioskCode = () => {
  const config = localStorage.getItem('kiosk_config');
  if (config) {
    try {
      return JSON.parse(config).kioskCode || 'KIOSK_001';
    } catch {
      // ignore
    }
  }
  return 'KIOSK_001';
};

/**
 * Kiosk Service
 * Xử lý tất cả API calls liên quan đến kiosk
 */
const kioskService = {
  /**
   * Lấy kiosk code hiện tại
   */
  getKioskCode,

  /**
   * Tạo QR code cho kiosk
   * @param {string} kioskCode - Kiosk code
   */
  generateQr: async (kioskCode) => {
    const response = await kioskApi.get(`/qr/generate/${kioskCode}`);
    return response.data;
  },

  /**
   * Xác thực QR code từ mobile app
   * @param {string} qrPayload - Chuỗi QR payload
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
   * Hoàn tất phiên sau khi xác thực mobile
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
   * Lấy phiên hoạt động hiện tại của kiosk
   * @param {string} kioskCode - Kiosk code
   */
  getActiveSession: async (kioskCode) => {
    const response = await kioskApi.get(`/session/${kioskCode}`);
    return response.data;
  },

  /**
   * Check-out và đóng phiên
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
   * Lấy thống kê trong ngày
   */
  getTodayStats: async () => {
    const response = await kioskApi.get('/monitor/stats');
    return response.data;
  },

  /**
   * Lấy nhật ký ra/vào gần đây
   * @param {number} limit - Số bản ghi cần lấy
   */
  getRecentLogs: async (limit = 10) => {
    const response = await kioskApi.get(`/monitor/logs?limit=${limit}`);
    return response.data;
  },
};

export default kioskService;
