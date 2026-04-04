import axios from 'axios';
import { API_BASE_URL as API_BASE } from '../../config/apiConfig';
import { getStaffAuthHeaders, getStaffAuthToken } from '../shared/staffAuth';

const getAuthHeaders = () => getStaffAuthHeaders();

const systemHealthService = {
    // =========================================
    // === SYSTEM INFO (FE-55) ===
    // =========================================

    getSystemInfo: async () => {
        const res = await axios.get(`${API_BASE}/slib/system/info`, { headers: getAuthHeaders() });
        return res.data;
    },

    // =========================================
    // === SYSTEM LOGS (FE-56) ===
    // =========================================

    getLogs: async ({ level, category, search, startDate, endDate, page = 0, size = 20 } = {}) => {
        const params = { page, size };
        if (level) params.level = level;
        if (category) params.category = category;
        if (search) params.search = search;
        if (startDate) params.startDate = startDate;
        if (endDate) params.endDate = endDate;

        const res = await axios.get(`${API_BASE}/slib/system/logs`, {
            params,
            headers: getAuthHeaders()
        });
        return res.data;
    },

    getLogStats: async () => {
        const res = await axios.get(`${API_BASE}/slib/system/logs/stats`, { headers: getAuthHeaders() });
        return res.data;
    },

    // =========================================
    // === BACKUP (FE-57) ===
    // =========================================

    triggerBackup: async () => {
        const res = await axios.post(`${API_BASE}/slib/system/backup`, {}, { headers: getAuthHeaders() });
        return res.data;
    },

    getBackupHistory: async () => {
        const res = await axios.get(`${API_BASE}/slib/system/backup/history`, { headers: getAuthHeaders() });
        return res.data;
    },

    downloadBackup: (backupId) => {
        const token = getStaffAuthToken();
        window.open(`${API_BASE}/slib/system/backup/download/${backupId}?token=${token}`, '_blank');
    },

    // =========================================
    // === BACKUP SCHEDULE (FE-58) ===
    // =========================================

    getBackupSchedule: async () => {
        const res = await axios.get(`${API_BASE}/slib/system/backup/schedule`, { headers: getAuthHeaders() });
        return res.data;
    },

    updateBackupSchedule: async ({ time, retainDays, isActive }) => {
        const res = await axios.put(`${API_BASE}/slib/system/backup/schedule`,
            { time, retainDays, isActive },
            { headers: getAuthHeaders() }
        );
        return res.data;
    },
};

export default systemHealthService;
