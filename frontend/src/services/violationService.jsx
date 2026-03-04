import axios from 'axios';

const API_BASE_URL = 'http://localhost:8081/slib';

const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: false
});

// Request interceptor - add auth token
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('librarian_token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response interceptor - handle errors
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            localStorage.removeItem('librarian_token');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

// ==================== VIOLATION APIs ====================

/**
 * Staff: Create a new violation record for a student
 */
export const createViolation = async (violationData) => {
  const response = await api.post('/violations', violationData);
  return response.data;
};

/**
 * Staff: Get all violations for a specific student
 */
export const getViolationsByStudent = async (studentId) => {
  const response = await api.get(`/violations/student/${studentId}`);
  return response.data;
};

/**
 * Student: Get my violations
 */
export const getMyViolations = async () => {
  const response = await api.get('/violations/my-violations');
  return response.data;
};

/**
 * Get a specific violation by ID
 */
export const getViolationById = async (violationId) => {
  const response = await api.get(`/violations/${violationId}`);
  return response.data;
};

// ==================== APPEAL APIs ====================

/**
 * Student: Create an appeal for a violation
 */
export const createAppeal = async (appealData) => {
  const response = await api.post('/appeals', appealData);
  return response.data;
};

/**
 * Staff: Get all appeals
 */
export const getAllAppeals = async () => {
  const response = await api.get('/appeals');
  return response.data;
};

/**
 * Staff: Get all pending appeals
 */
export const getPendingAppeals = async () => {
  const response = await api.get('/appeals/pending');
  return response.data;
};

/**
 * Student: Get my appeals
 */
export const getMyAppeals = async () => {
  const response = await api.get('/appeals/my-appeals');
  return response.data;
};

/**
 * Staff: Get appeals by student ID
 */
export const getAppealsByStudent = async (studentId) => {
  const response = await api.get(`/appeals/student/${studentId}`);
  return response.data;
};

/**
 * Staff: Review an appeal (approve or reject)
 */
export const reviewAppeal = async (appealId, reviewData) => {
  const response = await api.put(`/appeals/${appealId}/review`, reviewData);
  return response.data;
};

/**
 * Get appeals for a specific violation
 */
export const getAppealsByViolation = async (violationId) => {
  const response = await api.get(`/appeals/violation/${violationId}`);
  return response.data;
};

export default {
  // Violations
  createViolation,
  getViolationsByStudent,
  getMyViolations,
  getViolationById,
  
  // Appeals
  createAppeal,
  getAllAppeals,
  getPendingAppeals,
  getMyAppeals,
  getAppealsByStudent,
  reviewAppeal,
  getAppealsByViolation,
};
