import axios from 'axios';
import { API_BASE_URL as API_BASE, NFC_BRIDGE_URL } from '../../config/apiConfig';

const getAuthHeaders = () => {
    const token = localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');
    return token ? { Authorization: `Bearer ${token}` } : {};
};

const nfcManagementService = {
    openBridgeApp: (action = 'open') => {
        if (typeof window === 'undefined') return false;
        window.location.href = `slib-nfc-bridge://${action}`;
        return true;
    },

    /**
     * Kiểm tra NFC Bridge cục bộ và trạng thái đầu đọc.
     */
    checkBridgeConnection: async () => {
        try {
            const [healthResult, readerResult] = await Promise.allSettled([
                axios.get(`${NFC_BRIDGE_URL}/health`, { timeout: 5000 }),
                axios.get(`${NFC_BRIDGE_URL}/reader-status`, { timeout: 5000 })
            ]);

            const health = healthResult.status === 'fulfilled' ? healthResult.value?.data : null;
            const readerStatus = readerResult.status === 'fulfilled' ? readerResult.value?.data : null;

            if (!health && !readerStatus) {
                return {
                    online: false,
                    readerConnected: false,
                    status: 'offline',
                    bridgeUrl: NFC_BRIDGE_URL,
                    readerName: null,
                    message: 'Chưa phát hiện NFC Bridge trên máy này.'
                };
            }

            const readerConnected = Boolean(health?.readerConnected ?? readerStatus?.connected);
            const readerName = health?.readerName || readerStatus?.readerName || null;

            return {
                online: true,
                readerConnected,
                status: readerConnected ? 'ready' : 'bridge_only',
                bridgeUrl: NFC_BRIDGE_URL,
                readerName,
                message: readerConnected
                    ? `NFC Bridge đã sẵn sàng${readerName ? `: ${readerName}` : ''}.`
                    : (readerStatus?.message || 'NFC Bridge đang chạy nhưng chưa phát hiện đầu đọc ACR122U.')
            };
        } catch (e) {
            console.error('Error checking NFC bridge:', e);
            return {
                online: false,
                readerConnected: false,
                status: 'offline',
                bridgeUrl: NFC_BRIDGE_URL,
                readerName: null,
                message: 'Chưa phát hiện NFC Bridge trên máy này.'
            };
        }
    },

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
