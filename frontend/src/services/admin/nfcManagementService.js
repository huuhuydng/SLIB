import axios from 'axios';
import { API_BASE_URL as API_BASE, NFC_BRIDGE_URL } from '../../config/apiConfig';

const getAuthHeaders = () => {
    const token = localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');
    return token ? { Authorization: `Bearer ${token}` } : {};
};

const nfcManagementService = {
    /**
     * Lấy danh sách NFC mapping (tất cả ghế + trạng thái NFC)
     * GET /slib/seats/nfc-mappings?zoneId=&areaId=&hasNfc=&search=
     */
    getNfcMappings: async ({ zoneId, areaId, hasNfc, search } = {}) => {
        try {
            const params = {};
            if (zoneId) params.zoneId = zoneId;
            if (areaId) params.areaId = areaId;
            if (hasNfc !== undefined && hasNfc !== null && hasNfc !== '') params.hasNfc = hasNfc;
            if (search) params.search = search;

            const res = await axios.get(`${API_BASE}/slib/seats/nfc-mappings`, {
                params,
                headers: getAuthHeaders()
            });
            return res.data;
        } catch (e) {
            console.error('Error fetching NFC mappings:', e);
            throw e.response?.data || { message: 'Lỗi kết nối đến server' };
        }
    },

    /**
     * Lấy chi tiết NFC của một ghế
     * GET /slib/seats/{seatId}/nfc-info
     */
    getNfcInfo: async (seatId) => {
        try {
            const res = await axios.get(`${API_BASE}/slib/seats/${seatId}/nfc-info`, {
                headers: getAuthHeaders()
            });
            return res.data;
        } catch (e) {
            console.error('Error fetching NFC info:', e);
            throw e.response?.data || { message: 'Không thể tải thông tin NFC' };
        }
    },

    /**
     * Gán NFC UID cho ghế (quét từ bridge)
     * PUT /slib/seats/{seatId}/nfc-uid
     */
    assignNfcUid: async (seatId, nfcUid) => {
        try {
            const res = await axios.put(`${API_BASE}/slib/seats/${seatId}/nfc-uid`, {
                nfcTagUid: nfcUid,
            }, {
                headers: getAuthHeaders()
            });
            return res.data;
        } catch (e) {
            console.error('Error assigning NFC UID:', e);
            throw e.response?.data || { message: 'Không thể gán NFC UID' };
        }
    },

    /**
     * Xóa NFC UID khỏi ghế
     * DELETE /slib/seats/{seatId}/nfc-uid
     */
    clearNfcUid: async (seatId) => {
        try {
            const res = await axios.delete(`${API_BASE}/slib/seats/${seatId}/nfc-uid`, {
                headers: getAuthHeaders()
            });
            return res.data;
        } catch (e) {
            console.error('Error clearing NFC UID:', e);
            throw e.response?.data || { message: 'Không thể xóa NFC UID' };
        }
    },

    /**
     * Quét NFC từ bridge tool cục bộ trên máy thủ thư
     */
    scanNfcFromBridge: async () => {
        try {
            const res = await axios.get(`${NFC_BRIDGE_URL}/scan-uid`, {
                timeout: 30000
            });
            return res.data;
        } catch (e) {
            console.error('Error scanning NFC from bridge:', e);
            if (e.code === 'ECONNREFUSED' || e.code === 'ERR_NETWORK') {
                throw { message: 'NFC Bridge không hoạt động trên máy này. Hãy khởi động bridge trước khi quét thẻ.' };
            }
            throw e.response?.data || { message: 'Lỗi khi quét NFC' };
        }
    }
};

export default nfcManagementService;
