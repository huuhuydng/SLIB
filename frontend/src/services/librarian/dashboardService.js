import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const dashboardService = {
    /**
     * Lấy toàn bộ thống kê dashboard từ API tổng hợp
     */
    getDashboardStats: async () => {
        try {
            const token = localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');
            const res = await axios.get(`${API_BASE}/slib/dashboard/stats`, {
                headers: token ? { Authorization: `Bearer ${token}` } : {}
            });
            return res.data;
        } catch (e) {
            console.error('Error fetching dashboard stats:', e);
            return null;
        }
    },

    /**
     * Lấy danh sách tin tức mới nhất
     */
    getRecentNews: async () => {
        try {
            const token = localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');
            const res = await axios.get(`${API_BASE}/slib/news/admin/all`, {
                headers: token ? { Authorization: `Bearer ${token}` } : {}
            });
            // Chỉ lấy 5 tin tức mới nhất
            return (res.data || []).slice(0, 5);
        } catch (e) {
            console.error('Error fetching news:', e);
            return [];
        }
    },

    /**
     * Lấy dữ liệu chart thống kê theo range: week, month, year
     */
    getChartStats: async (range = 'week') => {
        try {
            const token = localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');
            const res = await axios.get(`${API_BASE}/slib/dashboard/chart-stats`, {
                params: { range },
                headers: token ? { Authorization: `Bearer ${token}` } : {}
            });
            return res.data || [];
        } catch (e) {
            console.error('Error fetching chart stats:', e);
            return [];
        }
    },

    /**
     * Lấy top 5 sinh viên xuất sắc theo range: week, month, year
     */
    getTopStudents: async (range = 'month') => {
        try {
            const token = localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');
            const res = await axios.get(`${API_BASE}/slib/dashboard/top-students`, {
                params: { range },
                headers: token ? { Authorization: `Bearer ${token}` } : {}
            });
            return res.data || [];
        } catch (e) {
            console.error('Error fetching top students:', e);
            return [];
        }
    }
};

export default dashboardService;
