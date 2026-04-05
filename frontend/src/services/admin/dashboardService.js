import axios from "axios";
import { API_BASE_URL as API_BASE } from "../../config/apiConfig";
import { getStaffAuthHeaders } from "../shared/staffAuth";

const adminDashboardService = {
  getDashboardStats: async () => {
    try {
      const res = await axios.get(`${API_BASE}/slib/dashboard/stats`, {
        headers: getStaffAuthHeaders(),
      });
      return res.data;
    } catch (e) {
      console.error("Error fetching admin dashboard stats:", e);
      return null;
    }
  },

  getRecentNews: async () => {
    try {
      const res = await axios.get(`${API_BASE}/slib/news/admin/all`, {
        headers: getStaffAuthHeaders(),
      });
      return (res.data || []).slice(0, 5);
    } catch (e) {
      console.error("Error fetching dashboard news:", e);
      return [];
    }
  },

  getRecentNewBooks: async () => {
    try {
      const res = await axios.get(`${API_BASE}/slib/new-books/admin`, {
        headers: getStaffAuthHeaders(),
      });
      return (res.data || [])
        .filter((book) => book?.isActive)
        .slice(0, 5);
    } catch (e) {
      console.error("Error fetching dashboard new books:", e);
      return [];
    }
  },

  getChartStats: async (range = "week") => {
    try {
      const res = await axios.get(`${API_BASE}/slib/dashboard/chart-stats`, {
        params: { range },
        headers: getStaffAuthHeaders(),
      });
      return res.data || [];
    } catch (e) {
      console.error("Error fetching dashboard chart stats:", e);
      return [];
    }
  },

  getKioskSessions: async () => {
    const res = await axios.get(`${API_BASE}/slib/kiosk/admin/sessions`, {
      headers: getStaffAuthHeaders(),
    });
    return res.data || [];
  },

  getTopStudents: async (range = "month") => {
    const res = await axios.get(`${API_BASE}/slib/dashboard/top-students`, {
      params: { range },
      headers: getStaffAuthHeaders(),
    });
    return res.data || [];
  },
};

export default adminDashboardService;
