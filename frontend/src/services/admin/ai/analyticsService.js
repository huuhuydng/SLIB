import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const AI_SERVICE_URL = import.meta.env.VITE_AI_SERVICE_URL || 'http://localhost:8001';

const aiAnalyticsService = axios.create({
  baseURL: AI_SERVICE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Backend API service (gọi trực tiếp Spring Boot)
const backendService = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add auth interceptor for backend
backendService.interceptors.request.use((config) => {
  const token = localStorage.getItem('librarian_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Lấy phân tích hành vi sinh viên
export const getStudentBehaviorAnalytics = async (userId, days = 30) => {
  try {
    const response = await aiAnalyticsService.post('/api/ai/analytics/student-behavior', {
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
export const getDensityPrediction = async (zoneId = null) => {
  try {
    const response = await aiAnalyticsService.get('/api/ai/analytics/density-prediction', {
      params: { zone_id: zoneId },
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
    const response = await aiAnalyticsService.get('/api/ai/analytics/seat-recommendation', {
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
    const response = await aiAnalyticsService.get('/api/ai/analytics/usage-statistics', {
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
    const response = await aiAnalyticsService.get('/api/ai/analytics/realtime-capacity');
    return response.data;
  } catch (error) {
    console.error('Error fetching realtime capacity:', error);
    throw error;
  }
};

// Lấy tổng hợp behavior của tất cả sinh viên
export const getBehaviorSummary = async (days = 30) => {
  try {
    const response = await backendService.get('/slib/analytics/behavior-summary', {
      params: { days },
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching behavior summary:', error);
    throw error;
  }
};

// Lấy behavior analytics của một sinh viên
export const getStudentBehaviorAnalyticsFromBackend = async (userId, days = 30) => {
  try {
    const response = await backendService.get(`/slib/analytics/student/${userId}`, {
      params: { days },
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching student behavior from backend:', error);
    throw error;
  }
};

// Lấy danh sách sinh viên có vấn đề hành vi
export const getStudentsWithBehaviorIssues = async (days = 30, minNoShowRate = 0.3) => {
  try {
    const response = await backendService.get('/slib/analytics/behavior-issues', {
      params: { days, minNoShowRate },
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching behavior issues:', error);
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
  getStudentBehaviorAnalyticsFromBackend,
  getStudentsWithBehaviorIssues,
};
