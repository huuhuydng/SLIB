import axios from 'axios';
import { API_BASE_URL as API_BASE } from '../../config/apiConfig';

const statisticService = {
    /**
     * Lấy toàn bộ thống kê theo khoảng thời gian
     * @param {string} range - week | month | year
     */
    getStatistics: async (range = 'week') => {
        try {
            const token = localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');
            const res = await axios.get(`${API_BASE}/slib/statistics`, {
                params: { range },
                headers: token ? { Authorization: `Bearer ${token}` } : {}
            });
            return res.data;
        } catch (e) {
            console.error('Error fetching statistics:', e);
            throw e;
        }
    }
};

export default statisticService;
