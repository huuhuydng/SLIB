import axios from "axios";
import { API_BASE_URL } from "../../config/apiConfig";

const api = axios.create({
  baseURL: `${API_BASE_URL}/slib`,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use((config) => {
  const token =
    localStorage.getItem("kiosk_device_token") ||
    sessionStorage.getItem("kiosk_device_token") ||
    sessionStorage.getItem("librarian_token") ||
    localStorage.getItem("librarian_token") ||
    localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const seatPlanService = {
  getAreas: () => api.get("/areas"),

  getZonesByArea: (areaId) => api.get("/zones", { params: { areaId } }),

  getSeats: (params = {}) => api.get("/seats", { params }),
};
