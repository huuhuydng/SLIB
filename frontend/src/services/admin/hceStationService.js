import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const getAuthHeaders = () => {
    const token = localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');
    return token ? { Authorization: `Bearer ${token}` } : {};
};

const hceStationService = {
    /**
     * Lấy danh sách trạm quét HCE
     */
    getAllStations: async (search = '', status = '', deviceType = '') => {
        try {
            const params = {};
            if (search) params.search = search;
            if (status) params.status = status;
            if (deviceType) params.deviceType = deviceType;

            const res = await axios.get(`${API_BASE}/slib/hce/stations`, {
                params,
                headers: getAuthHeaders()
            });
            return res.data;
        } catch (e) {
            console.error('Error fetching HCE stations:', e);
            throw e.response?.data || { message: 'Lỗi kết nối đến server' };
        }
    },

    /**
     * Lấy chi tiết trạm quét theo ID
     */
    getStationById: async (id) => {
        try {
            const res = await axios.get(`${API_BASE}/slib/hce/stations/${id}`, {
                headers: getAuthHeaders()
            });
            return res.data;
        } catch (e) {
            console.error('Error fetching station detail:', e);
            throw e.response?.data || { message: 'Không thể tải thông tin trạm quét' };
        }
    },

    /**
     * Tạo trạm quét mới
     */
    createStation: async (data) => {
        try {
            const res = await axios.post(`${API_BASE}/slib/hce/stations`, data, {
                headers: {
                    ...getAuthHeaders(),
                    'Content-Type': 'application/json'
                }
            });
            return res.data;
        } catch (e) {
            console.error('Error creating station:', e);
            throw e.response?.data || { message: 'Không thể tạo trạm quét' };
        }
    },

    /**
     * Cập nhật trạm quét
     */
    updateStation: async (id, data) => {
        try {
            const res = await axios.put(`${API_BASE}/slib/hce/stations/${id}`, data, {
                headers: {
                    ...getAuthHeaders(),
                    'Content-Type': 'application/json'
                }
            });
            return res.data;
        } catch (e) {
            console.error('Error updating station:', e);
            throw e.response?.data || { message: 'Không thể cập nhật trạm quét' };
        }
    },

    /**
     * Cập nhật trạng thái trạm quét
     */
    updateStationStatus: async (id, status) => {
        try {
            const res = await axios.patch(`${API_BASE}/slib/hce/stations/${id}/status`, { status }, {
                headers: {
                    ...getAuthHeaders(),
                    'Content-Type': 'application/json'
                }
            });
            return res.data;
        } catch (e) {
            console.error('Error updating station status:', e);
            throw e.response?.data || { message: 'Không thể cập nhật trạng thái' };
        }
    }
};

export default hceStationService;
