import axios from 'axios';
import * as XLSX from 'xlsx';
import JSZip from 'jszip';
import { API_BASE_URL as BASE } from '../../config/apiConfig';
import { getStaffAuthToken } from '../shared/staffAuth';
import {
    normalizeEmail,
    normalizeFullName,
    normalizePhone,
    normalizeUserCode,
    validateEmail,
    validateFullName,
    validatePhone,
    validateUserCode,
} from '../../utils/userValidation';
import { VALID_USER_ROLES, normalizeRole } from '../../utils/roles';

const API_BASE_URL = `${BASE}/slib`;

const axiosInstance = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: false
});

// Request interceptor - add auth token
axiosInstance.interceptors.request.use(
    (config) => {
        const token = getStaffAuthToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        console.error('❌ [UserService Request Error]', error);
        return Promise.reject(error);
    }
);

// Response interceptor - detect token expiry
axiosInstance.interceptors.response.use(
    (response) => response,
    (error) => {
        console.error('[UserService Response Error]', error.response?.status, error.response?.data);

        // Check for token expiry (401 Unauthorized or 403 Forbidden)
        if (error.response?.status === 401 || error.response?.status === 403) {
            const token = getStaffAuthToken();
            if (token) {
                // Try to decode and check expiry
                try {
                    const payload = JSON.parse(atob(token.split('.')[1]));
                    const now = Math.floor(Date.now() / 1000);

                    if (payload.exp && payload.exp < now) {
                        console.warn('[UserService] Token hết hạn! Đăng xuất...');
                        // Redirect tới trang token-expired
                        window.location.href = '/token-expired';
                        return Promise.reject(new Error('Token expired'));
                    }
                } catch (e) {
                    console.error('[UserService] Loi decode token:', e);
                }
            }
        }

        return Promise.reject(error);
    }
);

class UserService {
    /**
     * Get all users with optional filters
     */
    async getAllUsers(filters = {}) {
        try {
            const params = new URLSearchParams();
            if (filters.role) params.append('role', filters.role);
            if (filters.status) params.append('status', filters.status);
            if (filters.search) params.append('search', filters.search);

            const queryString = params.toString();
            const url = queryString ? `/users/getall?${queryString}` : '/users/getall';

            const response = await axiosInstance.get(url);
            return response.data;
        } catch (error) {
            console.error('❌ [UserService] getAllUsers error:', error);
            throw error;
        }
    }

    async getAdminUsers(filters = {}) {
        try {
            const params = new URLSearchParams();
            if (filters.role) params.append('role', filters.role);
            if (filters.status) params.append('status', filters.status);
            if (filters.search) params.append('search', filters.search);

            const queryString = params.toString();
            const url = queryString ? `/users/admin/list?${queryString}` : '/users/admin/list';

            const response = await axiosInstance.get(url);
            return response.data;
        } catch (error) {
            console.error('❌ [UserService] getAdminUsers error:', error);
            throw error;
        }
    }

    /**
     * Import multiple users from CSV/JSON data
     */
    async importUsers(users) {
        try {
            const response = await axiosInstance.post('/users/import', users);
            return response.data;
        } catch (error) {
            console.error('❌ [UserService] importUsers error:', error);
            throw error;
        }
    }

    /**
     * Validate users before import (check duplicates)
     */
    async validateUsers(users) {
        try {
            const response = await axiosInstance.post('/users/validate', users);
            return response.data;
        } catch (error) {
            console.error('❌ [UserService] validateUsers error:', error);
            throw error;
        }
    }

    /**
     * Upload avatar to Cloudinary
     */
    async uploadAvatar(file, userCode) {
        try {
            const formData = new FormData();
            formData.append('file', file);
            formData.append('userCode', userCode);

            const response = await axiosInstance.post('/users/avatar', formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            });
            return response.data;
        } catch (error) {
            console.error('❌ [UserService] uploadAvatar error:', error);
            throw error;
        }
    }

    /**
     * Batch upload avatars (faster - single request)
     * Files are named by userCode (e.g., SE123456.jpg)
     * @param {Object} avatarFiles - { userCode: File }
     * @returns {Promise<Array>} Array of { userCode, url, success }
     */
    async uploadAvatarsBatch(avatarFiles) {
        try {
            const formData = new FormData();

            // Add all files to formData
            // Rename files to userCode for backend to extract
            Object.entries(avatarFiles).forEach(([userCode, file]) => {
                const ext = file.name.split('.').pop();
                const renamedFile = new File([file], `${userCode}.${ext}`, { type: file.type });
                formData.append('files', renamedFile);
            });

            const response = await axiosInstance.post('/users/avatars/batch', formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            });

            return response.data;
        } catch (error) {
            console.error('[UserService] uploadAvatarsBatch error:', error);
            throw error;
        }
    }

    /**
     * Delete avatars by URLs (for rollback when import fails)
     * @param {string[]} urls - Array of Cloudinary URLs to delete
     * @returns {Promise<{total: number, deleted: number, failed: number}>}
     */
    async deleteAvatarsBatch(urls) {
        try {
            const response = await axiosInstance.delete('/users/avatars/batch', {
                data: { urls }
            });
            return response.data;
        } catch (error) {
            console.error('[UserService] deleteAvatarsBatch error:', error);
            throw error;
        }
    }

    // =====================================================
    // ADVANCED IMPORT (Async, Streaming, Progress Tracking)
    // =====================================================

    /**
     * Start async Excel import with streaming parser
     * Returns immediately with batchId for tracking progress
     * @param {File} excelFile - .xlsx or .xls file
     * @returns {Promise<{batchId: string, status: string}>}
     */
    async importExcelAsync(excelFile) {
        try {
            const formData = new FormData();
            formData.append('file', excelFile);

            const response = await axiosInstance.post('/users/import/excel', formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            });

            return response.data;
        } catch (error) {
            console.error('❌ [UserService] importExcelAsync error:', error);
            throw error;
        }
    }

    /**
     * Get import job status and progress
     * @param {string} batchId - UUID from importExcelAsync
     * @returns {Promise<ImportStatus>}
     */
    async getImportStatus(batchId) {
        try {
            const response = await axiosInstance.get(`/users/import/${batchId}/status`);
            return response.data;
        } catch (error) {
            console.error('❌ [UserService] getImportStatus error:', error);
            throw error;
        }
    }

    /**
     * Get failed/invalid rows from import
     * @param {string} batchId - UUID from importExcelAsync
     * @returns {Promise<{count: number, errors: Array}>}
     */
    async getImportErrors(batchId) {
        try {
            const response = await axiosInstance.get(`/users/import/${batchId}/errors`);
            return response.data;
        } catch (error) {
            console.error('❌ [UserService] getImportErrors error:', error);
            throw error;
        }
    }

    /**
     * Poll import status until completed or failed
     * @param {string} batchId - UUID from importExcelAsync
     * @param {function} onProgress - Callback for progress updates
     * @param {number} intervalMs - Polling interval (default 1000ms)
     * @returns {Promise<ImportStatus>} Final status when completed
     */
    async pollImportStatus(batchId, onProgress, intervalMs = 1000) {
        return new Promise((resolve, reject) => {
            const pollInterval = setInterval(async () => {
                try {
                    const status = await this.getImportStatus(batchId);

                    if (onProgress) {
                        onProgress(status);
                    }

                    // Check if completed or failed
                    if (status.status === 'COMPLETED' || status.status === 'FAILED') {
                        clearInterval(pollInterval);
                        resolve(status);
                    }
                } catch (error) {
                    clearInterval(pollInterval);
                    reject(error);
                }
            }, intervalMs);
        });
    }

    /**
     * Lock or unlock a user account
     */
    async updateUserStatus(userId, isActive, reason = null) {
        try {
            const response = await axiosInstance.patch(`/users/${userId}/status`, { isActive, reason });
            return response.data;
        } catch (error) {
            console.error('❌ [UserService] updateUserStatus error:', error);
            throw error;
        }
    }

    /**
     * Admin update user profile (Admin only)
     * @param {string} userId - User ID
     * @param {Object} data - { fullName, phone, email, dob, role }
     */
    async adminUpdateUser(userId, data) {
        try {
            const response = await axiosInstance.patch(`/users/${userId}`, data);
            return response.data;
        } catch (error) {
            console.error('❌ [UserService] adminUpdateUser error:', error);
            throw error;
        }
    }

    /**
     * Manual reputation adjustment (Admin only)
     */
    async adjustUserReputation(userId, points, reason) {
        try {
            const response = await axiosInstance.patch(
                `/student-profile/${userId}/reputation/manual-adjust`,
                { points, reason },
            );
            return response.data;
        } catch (error) {
            console.error('❌ [UserService] adjustUserReputation error:', error);
            throw error;
        }
    }

    /**
     * Reset user password to default (Admin only)
     */
    async resetPasswordToDefault(email) {
        try {
            const response = await axiosInstance.post('/auth/admin-reset-password', { email });
            return response.data;
        } catch (error) {
            console.error('❌ [UserService] resetPasswordToDefault error:', error);
            throw error;
        }
    }

    /**
     * Create a new librarian account
     */
    async createUser(data) {
        try {
            const response = await axiosInstance.post('/users', data);
            return response.data.user;
        } catch (error) {
            console.error('❌ [UserService] createUser error:', error);
            throw error;
        }
    }

    /**
     * Delete a user account (soft delete)
     * @param {string} userId - User ID to delete
     * @param {boolean} hardDelete - If true, permanently delete (default: false)
     * @returns {Promise<{success: boolean, message: string}>}
     */
    async deleteUser(userId) {
        try {
            const response = await axiosInstance.delete(`/users/${userId}`);
            return response.data;
        } catch (error) {
            console.error('❌ [UserService] deleteUser error:', error);
            throw error;
        }
    }

    /**
     * Check if user has active bookings
     * @param {string} userId - User ID to check
     * @returns {Promise<{hasActiveBookings: boolean, count: number}>}
     */
    async checkUserActiveBookings(userId) {
        try {
            const response = await axiosInstance.get(`/users/${userId}/active-bookings`);
            return response.data;
        } catch (error) {
            // If endpoint doesn't exist, assume no active bookings
            console.warn('[UserService] checkUserActiveBookings failed:', error);
            return { hasActiveBookings: false, count: 0 };
        }
    }

    /**
     * Parse Excel file (.xlsx) with Vietnamese column headers
     * @param {File} file - Excel file
     * @returns {Promise<Array>} Array of user objects
     */
    async parseExcelFile(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = (e) => {
                try {
                    const data = new Uint8Array(e.target.result);
                    const workbook = XLSX.read(data, { type: 'array' });

                    const sheetName = workbook.SheetNames[0];
                    const worksheet = workbook.Sheets[sheetName];
                    const jsonData = XLSX.utils.sheet_to_json(worksheet, { header: 1 });

                    // Filter out empty rows
                    const nonEmptyRows = jsonData.filter(row =>
                        row && row.length > 0 && row.some(cell => cell !== null && cell !== undefined && String(cell).trim() !== '')
                    );

                    if (nonEmptyRows.length < 2) {
                        throw new Error('File Excel không có dữ liệu. Vui lòng kiểm tra file có header và ít nhất 1 dòng dữ liệu.');
                    }

                    // First non-empty row is header
                    const headers = nonEmptyRows[0].map(h => String(h || '').trim().toLowerCase());
                    const users = this.parseRowsWithHeaders(headers, nonEmptyRows.slice(1));
                    resolve(users);
                } catch (error) {
                    console.error('❌ [Excel] Parse error:', error);
                    reject(error);
                }
            };
            reader.onerror = () => reject(new Error('Không thể đọc file'));
            reader.readAsArrayBuffer(file);
        });
    }

    /**
     * Parse Zip file containing Excel and avatar images
     * @param {File} file - Zip file
     * @returns {Promise<{users: Array, avatars: Object}>}
     */
    async parseZipFile(file) {
        const zip = await JSZip.loadAsync(file);
        let excelFile = null;
        const avatars = {};

        // Find Excel file and image files
        for (const [filename, zipEntry] of Object.entries(zip.files)) {
            if (zipEntry.dir) continue;

            const lowerName = filename.toLowerCase();
            const baseName = filename.split('/').pop(); // Get filename without path

            // Skip Mac metadata files and __MACOSX folder
            if (baseName.startsWith('._') || lowerName.includes('__macosx') || baseName.startsWith('.')) {
                continue;
            }

            if (lowerName.endsWith('.xlsx') || lowerName.endsWith('.xls')) {
                excelFile = zipEntry;
            } else if (lowerName.match(/\.(jpg|jpeg|png|gif|webp)$/)) {
                // Extract userCode from filename (remove extension)
                const userCode = baseName.replace(/\.(jpg|jpeg|png|gif|webp)$/i, '');
                const blob = await zipEntry.async('blob');
                avatars[userCode.toUpperCase()] = new File([blob], baseName, { type: this.getMimeType(baseName) });
            }
        }

        if (!excelFile) {
            throw new Error('Không tìm thấy file Excel (.xlsx) trong file Zip');
        }

        // Parse Excel file from zip
        const excelBlob = await excelFile.async('blob');
        const excelFileObj = new File([excelBlob], 'data.xlsx');
        const users = await this.parseExcelFile(excelFileObj);

        return { users, avatars };
    }

    /**
     * Get MIME type from filename
     */
    getMimeType(filename) {
        const ext = filename.split('.').pop().toLowerCase();
        const mimeTypes = {
            'jpg': 'image/jpeg',
            'jpeg': 'image/jpeg',
            'png': 'image/png',
            'gif': 'image/gif',
            'webp': 'image/webp'
        };
        return mimeTypes[ext] || 'image/jpeg';
    }

    /**
     * Parse rows with Vietnamese column mapping
     */
    parseRowsWithHeaders(headers, rows) {
        // Vietnamese and English column mapping (support multiple variations)
        const columnMapping = {
            // Full name variations
            'họ và tên': 'fullName',
            'ho và tên': 'fullName',  // Mixed accents
            'ho va ten': 'fullName',
            'hoten': 'fullName',
            'full_name': 'fullName',
            'fullname': 'fullName',
            'name': 'fullName',
            'ten': 'fullName',

            // User code variations  
            'mã số': 'userCode',
            'ma so': 'userCode',
            'maso': 'userCode',
            'user_code': 'userCode',
            'usercode': 'userCode',
            'mssv': 'userCode',
            'code': 'userCode',

            // Role variations
            'vai trò': 'role',
            'vai tro': 'role',
            'vaitro': 'role',
            'role': 'role',

            // Date of birth variations
            'ngày sinh': 'dob',
            'ngay sinh': 'dob',
            'ngaysinh': 'dob',
            'dob': 'dob',
            'birthday': 'dob',
            'date_of_birth': 'dob',

            // Email
            'email': 'email',
            'mail': 'email',

            // Phone variations
            'số điện thoại': 'phone',
            'so dien thoai': 'phone',
            'sodienthoai': 'phone',
            'số điện thoai': 'phone',  // Mixed accents
            'điện thoại': 'phone',
            'dien thoai': 'phone',
            'phone': 'phone',
            'sdt': 'phone',
            'sđt': 'phone'
        };

        // Role mapping Vietnamese to English
        const roleMapping = {
            'sinh viên': 'STUDENT',
            'sinh vien': 'STUDENT',
            'student': 'STUDENT',
            'giáo viên': 'TEACHER',
            'giao vien': 'TEACHER',
            'teacher': 'TEACHER',
            'thủ thư': 'LIBRARIAN',
            'thu thu': 'LIBRARIAN',
            'librarian': 'LIBRARIAN',
            'admin': 'ADMIN',
            'quản trị viên': 'ADMIN',
            'quan tri vien': 'ADMIN'
        };

        // Map headers to field names
        const headerMap = {};
        headers.forEach((header, idx) => {
            const normalizedHeader = header.replace(/\s+/g, ' ').trim().toLowerCase();
            if (columnMapping[normalizedHeader]) {
                headerMap[columnMapping[normalizedHeader]] = idx;
            }
        });

        // Check required fields
        const requiredFields = ['userCode', 'email', 'fullName'];
        const missingFields = requiredFields.filter(f => headerMap[f] === undefined);
        if (missingFields.length > 0) {
            throw new Error(`Thiếu cột bắt buộc: ${missingFields.join(', ')}`);
        }

        // Parse rows
        const users = [];
        rows.forEach((row, rowIndex) => {
            if (!row || row.length === 0) return;

            const getValue = (field) => {
                const idx = headerMap[field];
                return idx !== undefined && row[idx] !== undefined ? String(row[idx]).trim() : '';
            };

            const rawRole = getValue('role').toLowerCase();
            const role = roleMapping[rawRole] || 'STUDENT';

            const user = {
                fullName: normalizeFullName(getValue('fullName')),
                userCode: normalizeUserCode(getValue('userCode')),
                email: normalizeEmail(getValue('email')),
                role: role,
                phone: normalizePhone(getValue('phone')) || null,
                dob: this.parseDate(getValue('dob')),
                _rowIndex: rowIndex + 2 // For error reporting (1-indexed + header row)
            };

            // Only add if has required fields
            if (user.userCode && user.email && user.fullName) {
                users.push(user);
            }
        });

        return users;
    }

    /**
     * Validate users locally (check for duplicates within the file)
     */
    validateUsersLocally(users) {
        const errors = {};
        const seenUserCodes = new Map();
        const seenEmails = new Map();

        users.forEach((user, index) => {
            const userErrors = {};
            const normalizedUserCode = normalizeUserCode(user.userCode);
            const normalizedEmail = normalizeEmail(user.email);
            const normalizedFullName = normalizeFullName(user.fullName);
            const normalizedPhone = normalizePhone(user.phone);

            // Check duplicate userCode within file
            if (seenUserCodes.has(normalizedUserCode)) {
                userErrors.userCode = `Mã số bị trùng với dòng ${seenUserCodes.get(normalizedUserCode)}`;
            } else {
                seenUserCodes.set(normalizedUserCode, index + 2); // +2 because row 1 is header
            }

            // Check duplicate email within file
            if (seenEmails.has(normalizedEmail)) {
                userErrors.email = `Email bị trùng với dòng ${seenEmails.get(normalizedEmail)}`;
            } else {
                seenEmails.set(normalizedEmail, index + 2); // +2 because row 1 is header
            }

            const fullNameError = validateFullName(normalizedFullName);
            if (fullNameError) userErrors.fullName = fullNameError;

            const emailError = validateEmail(normalizedEmail);
            if (emailError) userErrors.email = emailError;

            const userCodeError = validateUserCode(normalizedUserCode);
            if (userCodeError) userErrors.userCode = userCodeError;

            const phoneError = validatePhone(normalizedPhone);
            if (phoneError) userErrors.phone = phoneError;

            // Validate role
            const normalizedRole = normalizeRole(user.role);
            if (!VALID_USER_ROLES.includes(normalizedRole)) {
                userErrors.role = 'Vai trò không hợp lệ';
            } else {
                user.role = normalizedRole;
            }

            if (Object.keys(userErrors).length > 0) {
                errors[user.userCode || `row_${index}`] = userErrors;
            }
        });

        return errors;
    }

    /**
     * Parse CSV content to user objects
     */
    parseCSV(csvContent) {
        const lines = csvContent.trim().split('\n');
        if (lines.length < 2) {
            throw new Error('File CSV không có dữ liệu');
        }

        const headers = lines[0].split(',').map(h => h.trim().toLowerCase());
        const rows = lines.slice(1).map(line => this.parseCSVLine(line));

        return this.parseRowsWithHeaders(headers, rows);
    }

    /**
     * Parse a single CSV line handling quoted values
     */
    parseCSVLine(line) {
        const result = [];
        let current = '';
        let inQuotes = false;

        for (let i = 0; i < line.length; i++) {
            const char = line[i];

            if (char === '"') {
                inQuotes = !inQuotes;
            } else if (char === ',' && !inQuotes) {
                result.push(current);
                current = '';
            } else {
                current += char;
            }
        }
        result.push(current);

        return result;
    }

    /**
     * Parse various date formats to yyyy-MM-dd
     */
    parseDate(dateStr) {
        if (!dateStr) return null;
        dateStr = String(dateStr).trim();

        // Try dd/MM/yyyy format
        const dmyMatch = dateStr.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
        if (dmyMatch) {
            const [, d, m, y] = dmyMatch;
            return `${y}-${m.padStart(2, '0')}-${d.padStart(2, '0')}`;
        }

        // Try yyyy-MM-dd format (already correct)
        if (/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) {
            return dateStr;
        }

        // Try Excel serial date format
        const numDate = Number(dateStr);
        if (!isNaN(numDate) && numDate > 10000 && numDate < 100000) {
            const date = new Date((numDate - 25569) * 86400 * 1000);
            return date.toISOString().split('T')[0];
        }

        return null;
    }

    /**
     * Download XLSX template file from backend API
     */
    async downloadTemplate() {
        try {
            const response = await axiosInstance.get('/users/import/template', {
                responseType: 'blob'
            });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.download = 'slib_user_import_template.xlsx';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
        } catch (error) {
            console.error('❌ [UserService] downloadTemplate error:', error);
            throw error;
        }
    }
}

export default new UserService();
