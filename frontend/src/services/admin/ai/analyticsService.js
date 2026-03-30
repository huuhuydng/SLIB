import axios from 'axios';
import { AI_API_BASE_URL } from '../../../config/apiConfig';

const aiAnalyticsService = axios.create({
  baseURL: AI_API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
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
export const getDensityPrediction = async (zoneId = null, days = 7) => {
  try {
    const response = await aiAnalyticsService.get('/api/ai/analytics/density-prediction', {
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
export const getBehaviorSummary = async (days = 7) => {
  try {
    const response = await aiAnalyticsService.get('/api/ai/analytics/behavior-summary', {
      params: { days },
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching behavior summary:', error);
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
};
