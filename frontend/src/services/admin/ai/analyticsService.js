import axios from 'axios';
import { API_BASE_URL } from '../../../config/apiConfig';
import { getStaffAuthToken } from '../../shared/staffAuth';

const aiAnalyticsService = axios.create({
  baseURL: `${API_BASE_URL}/slib/ai/analytics`,
  headers: {
    'Content-Type': 'application/json',
  },
});

aiAnalyticsService.interceptors.request.use((config) => {
  const token = getStaffAuthToken();

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

// Lấy phân tích hành vi sinh viên
export const getStudentBehaviorAnalytics = async (userId, days = 30) => {
  try {
    const response = await aiAnalyticsService.post('/student-behavior', {
      user_id: userId,
      days: days,
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching student behavior analytics:', error);
    throw error;
  }
};

// Lấy dự đoán mật độ
export const getDensityPrediction = async (zoneId = null, days = 7) => {
  try {
    const response = await aiAnalyticsService.get('/density-prediction', {
      params: { zone_id: zoneId, days },
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching density prediction:', error);
    throw error;
  }
};

// Lấy gợi ý chỗ ngồi
export const getSeatRecommendation = async (userId, zonePreference = null, timeSlot = null) => {
  try {
    const response = await aiAnalyticsService.get('/seat-recommendation', {
      params: {
        user_id: userId,
        zone_preference: zonePreference,
        time_slot: timeSlot,
      },
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching seat recommendation:', error);
    throw error;
  }
};

// Lấy thống kê sử dụng
export const getUsageStatistics = async (period = 'week') => {
  try {
    const response = await aiAnalyticsService.get('/usage-statistics', {
      params: { period },
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching usage statistics:', error);
    throw error;
  }
};

// Lấy sức chứa thời gian thực
export const getRealtimeCapacity = async () => {
  try {
    const response = await aiAnalyticsService.get('/realtime-capacity');
    return response.data;
  } catch (error) {
    console.error('Error fetching realtime capacity:', error);
    throw error;
  }
};

// Lấy tổng hợp behavior của tất cả sinh viên
export const getBehaviorSummary = async (days = 7) => {
  try {
    const response = await aiAnalyticsService.get('/behavior-summary', {
      params: { days },
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching behavior summary:', error);
    throw error;
  }
};

// Lấy danh sách sinh viên cần lưu ý cho dashboard thủ thư
export const getBehaviorIssues = async (limit = 3) => {
  try {
    const response = await aiAnalyticsService.get('/behavior-issues', {
      params: { limit },
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching behavior issues:', error);
    throw error;
  }
};

export const sendBehaviorWarning = async (userId, primaryIssue, detail) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/slib/notifications/staff/behavior-warning`, {
      userId,
      primaryIssue,
      detail,
    }, {
      headers: {
        Authorization: `Bearer ${getStaffAuthToken()}`,
        'Content-Type': 'application/json',
      },
    });
    return response.data;
  } catch (error) {
    console.error('Error sending behavior warning:', error);
    throw error;
  }
};

export default {
  getStudentBehaviorAnalytics,
  getDensityPrediction,
  getSeatRecommendation,
  getUsageStatistics,
  getRealtimeCapacity,
  getBehaviorSummary,
  getBehaviorIssues,
  sendBehaviorWarning,
};
