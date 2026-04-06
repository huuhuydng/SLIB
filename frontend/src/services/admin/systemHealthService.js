import axios from 'axios';
import { API_BASE_URL as API_BASE } from '../../config/apiConfig';
import { getStaffAuthHeaders } from '../shared/staffAuth';

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

    exportLogs: async ({ level, category, search, startDate, endDate } = {}) => {
        const params = {};
        if (level) params.level = level;
        if (category) params.category = category;
        if (search) params.search = search;
        if (startDate) params.startDate = startDate;
        if (endDate) params.endDate = endDate;

        const res = await axios.get(`${API_BASE}/slib/system/logs/export`, {
            params,
            headers: getAuthHeaders(),
            responseType: 'blob'
        });

        const blobUrl = window.URL.createObjectURL(new Blob([res.data], {
            type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        }));
        const anchor = document.createElement('a');
        anchor.href = blobUrl;
        anchor.download = `nhat-ky-he-thong-${new Date().toISOString().slice(0, 10)}.xlsx`;
        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();
        window.URL.revokeObjectURL(blobUrl);
    },

    cleanupLogs: async (beforeDate) => {
        const res = await axios.delete(`${API_BASE}/slib/system/logs/cleanup`, {
            headers: getAuthHeaders(),
            data: { beforeDate }
        });
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

    downloadBackup: async (backupId, fileName = null) => {
        const res = await axios.get(`${API_BASE}/slib/system/backup/download/${backupId}`, {
            headers: getAuthHeaders(),
            responseType: 'blob'
        });

        const blobUrl = window.URL.createObjectURL(new Blob([res.data]));
        const anchor = document.createElement('a');
        anchor.href = blobUrl;
        anchor.download = fileName || `slib-backup-${backupId}.dump`;
        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();
        window.URL.revokeObjectURL(blobUrl);
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
