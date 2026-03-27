import axios from "axios";

const api = axios.create({
  baseURL: "/slib",
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("librarian_token") || localStorage.getItem("token");
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
