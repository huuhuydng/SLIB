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
    },

    exportStatistics: async (range = 'week') => {
        try {
            const token = localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');
            const response = await axios.get(`${API_BASE}/slib/statistics/export`, {
                params: { range },
                headers: token ? { Authorization: `Bearer ${token}` } : {},
                responseType: 'blob'
            });

            const blobUrl = window.URL.createObjectURL(new Blob([response.data], {
                type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
            }));
            const anchor = document.createElement('a');
            anchor.href = blobUrl;
            anchor.download = `bao-cao-thong-ke-thu-vien-${new Date().toISOString().slice(0, 10)}.xlsx`;
            document.body.appendChild(anchor);
            anchor.click();
            anchor.remove();
            window.URL.revokeObjectURL(blobUrl);
        } catch (e) {
            console.error('Error exporting statistics:', e);
            throw e;
        }
    }
};

export default statisticService;
