import axios from 'axios';
import { API_BASE_URL as BASE } from '../../config/apiConfig';

const API_BASE_URL = `${BASE}/api/librarian`;

const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: false
});

axiosInstance.interceptors.request.use(
  (config) => config,
  (error) => {
    console.error('❌ [Request Error]', error);
    return Promise.reject(error);
  }
);

axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('❌ [Response Error]', error.response?.status, error.response?.data);
    return Promise.reject(error);
  }
);

class LibrarianService {
  async login(email, password) {
    const response = await axiosInstance.post('/login', {
      email,
      password
    });

    const token = response.data.accessToken || response.data.token || response.data.access_token;
    const user = response.data.user || response.data.librarian;

    if (token) {
      localStorage.setItem('librarian_token', token);
    } else {
      console.error('❌ [Service] No token in response:', response.data);
    }

    if (user) {
      localStorage.setItem('librarian_user', JSON.stringify(user));
    }

    return response.data;
  }

  async loginWithPassword(identifier, password) {
    try {
      const response = await axios.post(`${BASE}/slib/auth/login`, {
        identifier: identifier,
        password: password
      }, {
        headers: {
          'Content-Type': 'application/json',
          'X-Device-Info': navigator.userAgent
        }
      });

      const { accessToken, refreshToken } = response.data;

      if (accessToken) {
        localStorage.setItem('librarian_token', accessToken);
      }

      if (refreshToken) {
        localStorage.setItem('refresh_token', refreshToken);
      }

      const user = {
        id: response.data.id,
        email: response.data.email,
        fullName: response.data.fullName,
        studentCode: response.data.studentCode || response.data.userCode,
        role: response.data.role
      };

      localStorage.setItem('librarian_user', JSON.stringify(user));
      return response.data;

    } catch (error) {
      console.error('[Service] loginWithPassword error:', error);
      throw error;
    }
  }

  async googleLogin(idToken) {
    try {
      const response = await axios.post(`${BASE}/slib/auth/google`, {
        idToken: idToken,
        fullName: "",
        fcmToken: "",
        deviceInfo: navigator.userAgent
      });

      // Backend trả về AuthResponse với accessToken và refreshToken
      const { accessToken, refreshToken } = response.data;

      if (accessToken) {
        localStorage.setItem('librarian_token', accessToken);
      }

      if (refreshToken) {
        localStorage.setItem('refresh_token', refreshToken);
      }

      // Lưu user info
      const user = {
        id: response.data.id,
        email: response.data.email,
        fullName: response.data.fullName,
        studentCode: response.data.studentCode,
        role: response.data.role
      };

      localStorage.setItem('librarian_user', JSON.stringify(user));
      return response.data;

    } catch (error) {
      console.error('❌ [Service] googleLogin error:', error);
      throw error;
    }
  }

  async forgotPassword(email) {
    try {
      const response = await axiosInstance.post('/forgot-password', {
        email: email.trim().toLowerCase()
      });

      return response.data;

    } catch (error) {
      console.error('❌ [Service] forgotPassword error:', error);
      throw error;
    }
  }

  async verifyOtp(email, token, type = 'recovery') {
    try {
      const response = await axiosInstance.post('/verify-otp', {
        email: email.trim().toLowerCase(),
        token: token.trim(),
        type: type
      });

      if (response.data.result) {
        try {
          const resultData = JSON.parse(response.data.result);
          if (resultData.access_token) {
            localStorage.setItem('temp_reset_token', resultData.access_token);
          }
        } catch (e) {
          console.warn('⚠️ [Service] Cannot parse result JSON:', e);
        }
      }

      return response.data;

    } catch (error) {
      console.error('❌ [Service] verifyOtp error:', error);
      throw error;
    }
  }

  async resendOtp(email, type = 'recovery') {
    try {
      const response = await axiosInstance.post('/resend-otp', {
        email: email.trim().toLowerCase(),
        type: type
      });

      return response.data;

    } catch (error) {
      console.error('❌ [Service] resendOtp error:', error);
      throw error;
    }
  }

  async updatePassword(newPassword) {
    try {
      const token = localStorage.getItem('temp_reset_token');

      if (!token) {
        throw new Error('Không tìm thấy token xác thực. Vui lòng xác thực OTP lại.');
      }

      const response = await axiosInstance.post('/update-password', {
        password: newPassword
      }, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      localStorage.removeItem('temp_reset_token');

      return response.data;

    } catch (error) {
      console.error('❌ [Service] updatePassword error:', error);
      throw error;
    }
  }

  getCurrentUser() {
    const userStr = localStorage.getItem('librarian_user');
    return userStr ? JSON.parse(userStr) : null;
  }

  // ========== Access Logs (HCE) ==========

  _getAuthHeaders() {
    const token = localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');
    return {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    };
  }

  get _hceBaseUrl() {
    return `${BASE}/slib/hce`;
  }

  async getStudentDetail(userId) {
    try {
      const response = await axios.get(`${this._hceBaseUrl}/student-detail/${userId}`, {
        headers: this._getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('[LibrarianService] getStudentDetail error:', error);
      return null;
    }
  }

  async getAllAccessLogs() {
    try {
      const response = await axios.get(`${this._hceBaseUrl}/access-logs`, {
        headers: this._getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('[LibrarianService] getAllAccessLogs error:', error);
      return [];
    }
  }

  async getAccessLogStats() {
    try {
      const response = await axios.get(`${this._hceBaseUrl}/access-logs/stats`, {
        headers: this._getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('[LibrarianService] getAccessLogStats error:', error);
      return { totalCheckInsToday: 0, totalCheckOutsToday: 0, currentlyInLibrary: 0 };
    }
  }

  async getAccessLogsByDateRange(startDate, endDate) {
    try {
      const response = await axios.get(`${this._hceBaseUrl}/access-logs/filter`, {
        params: { startDate, endDate },
        headers: this._getAuthHeaders()
      });
      return response.data;
    } catch (error) {
      console.error('[LibrarianService] getAccessLogsByDateRange error:', error);
      return [];
    }
  }

  logout() {
    localStorage.removeItem('librarian_token');
    localStorage.removeItem('librarian_user');
    localStorage.removeItem('temp_reset_token');
    localStorage.removeItem('refresh_token');
    sessionStorage.removeItem('librarian_token');
    sessionStorage.removeItem('librarian_user');
    sessionStorage.removeItem('refresh_token');
  }
}

export default new LibrarianService();
