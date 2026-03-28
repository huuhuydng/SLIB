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
  (config) => {
    console.log('📤 [Request]', config.method.toUpperCase(), config.url);
    console.log('📤 [Data]', config.data);
    return config;
  },
  (error) => {
    console.error('❌ [Request Error]', error);
    return Promise.reject(error);
  }
);

axiosInstance.interceptors.response.use(
  (response) => {
    console.log('📥 [Response]', response.status, response.data);
    return response;
  },
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
      console.log('✅ [Service] Token saved:', token.substring(0, 20) + '...');
    } else {
      console.error('❌ [Service] No token in response:', response.data);
    }

    if (user) {
      localStorage.setItem('librarian_user', JSON.stringify(user));
      console.log('✅ [Service] User saved:', user);
    }

    return response.data;
  }

  async loginWithPassword(identifier, password) {
    try {
      console.log('[Service] Calling loginWithPassword with identifier:', identifier);

      const response = await axios.post(`${BASE}/slib/auth/login`, {
        identifier: identifier,
        password: password
      }, {
        headers: {
          'Content-Type': 'application/json',
          'X-Device-Info': navigator.userAgent
        }
      });

      console.log('[Service] loginWithPassword success:', response.data);

      const { accessToken, refreshToken } = response.data;

      if (accessToken) {
        localStorage.setItem('librarian_token', accessToken);
        console.log('[Service] Access token saved');
      }

      if (refreshToken) {
        localStorage.setItem('refresh_token', refreshToken);
        console.log('[Service] Refresh token saved');
      }

      const user = {
        id: response.data.id,
        email: response.data.email,
        fullName: response.data.fullName,
        studentCode: response.data.studentCode || response.data.userCode,
        role: response.data.role
      };

      localStorage.setItem('librarian_user', JSON.stringify(user));
      console.log('[Service] User saved:', user);

      return response.data;

    } catch (error) {
      console.error('[Service] loginWithPassword error:', error);
      throw error;
    }
  }

  async googleLogin(idToken) {
    try {
      console.log('🟡 [Service] Calling googleLogin with ID Token');

      const response = await axios.post(`${BASE}/slib/auth/google`, {
        idToken: idToken,
        fullName: "",
        fcmToken: "",
        deviceInfo: navigator.userAgent
      });

      console.log('✅ [Service] googleLogin success:', response.data);

      // Backend trả về AuthResponse với accessToken và refreshToken
      const { accessToken, refreshToken } = response.data;

      if (accessToken) {
        sessionStorage.setItem('librarian_token', accessToken);
        console.log('✅ [Service] Access token saved:', accessToken.substring(0, 20) + '...');
      }

      if (refreshToken) {
        sessionStorage.setItem('refresh_token', refreshToken);
        console.log('✅ [Service] Refresh token saved');
      }

      // Lưu user info
      const user = {
        id: response.data.id,
        email: response.data.email,
        fullName: response.data.fullName,
        studentCode: response.data.studentCode,
        role: response.data.role
      };
      
      sessionStorage.setItem('librarian_user', JSON.stringify(user));
      console.log('✅ [Service] Google user saved:', user);

      return response.data;

    } catch (error) {
      console.error('❌ [Service] googleLogin error:', error);
      throw error;
    }
  }

  async forgotPassword(email) {
    try {
      console.log('🟡 [Service] Calling forgotPassword with:', email);

      const response = await axiosInstance.post('/forgot-password', {
        email: email.trim().toLowerCase()
      });

      console.log('✅ [Service] forgotPassword success:', response.data);
      return response.data;

    } catch (error) {
      console.error('❌ [Service] forgotPassword error:', error);
      throw error;
    }
  }

  async verifyOtp(email, token, type = 'recovery') {
    try {
      console.log('🟡 [Service] Calling verifyOtp:', { email, token, type });

      const response = await axiosInstance.post('/verify-otp', {
        email: email.trim().toLowerCase(),
        token: token.trim(),
        type: type
      });

      console.log('✅ [Service] verifyOtp success:', response.data);

      if (response.data.result) {
        try {
          const resultData = JSON.parse(response.data.result);
          if (resultData.access_token) {
            localStorage.setItem('temp_reset_token', resultData.access_token);
            console.log('✅ [Service] Saved access_token to localStorage');
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
      console.log('🟡 [Service] Calling resendOtp:', { email, type });

      const response = await axiosInstance.post('/resend-otp', {
        email: email.trim().toLowerCase(),
        type: type
      });

      console.log('✅ [Service] resendOtp success:', response.data);
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

      console.log('🟡 [Service] Calling updatePassword');

      const response = await axiosInstance.post('/update-password', {
        password: newPassword
      }, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      console.log('✅ [Service] updatePassword success:', response.data);

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