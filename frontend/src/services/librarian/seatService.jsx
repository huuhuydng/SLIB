import { API_BASE_URL as BASE } from '../../config/apiConfig';

const API_BASE_URL = `${BASE}/api`;

const getAuthHeaders = () => {
  const token = localStorage.getItem('librarian_token');
  return {
    'Content-Type': 'application/json',
    'Authorization': token ? `Bearer ${token}` : ''
  };
};

export const seatService = {
  /**
   * Lấy tất cả ghế với trạng thái
   * @param {Object} params - Optional parameters
   * @param {string} params.startTime - ISO string for start time
   * @param {string} params.endTime - ISO string for end time
   * @param {string} params.zone - Zone filter
   */
  async getAllSeats(params = {}) {
    try {
      // ✅ Build query string từ params
      const queryParams = new URLSearchParams();

      if (params.startTime) {
        queryParams.append('startTime', params.startTime);
      }
      if (params.endTime) {
        queryParams.append('endTime', params.endTime);
      }
      if (params.zone) {
        queryParams.append('zone', params.zone);
      }

      const queryString = queryParams.toString();
      const url = queryString
        ? `${BASE}/slib/seats?${queryString}`
        : `${BASE}/slib/seats`;

      console.log('📡 API URL:', url);

      const response = await fetch(url, {
        method: 'GET',
        headers: getAuthHeaders()
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to fetch seats (${response.status}): ${errorText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('❌ Error fetching seats:', error);
      throw error;
    }
  },

  /**
   * Thêm hạn chế cho ghế (dùng seatId thay vì seatCode vì seatCode không unique)
   * POST /slib/seats/{seatId}/restrict
   */
  async addRestriction(seatId) {
    try {
      const url = `${BASE}/slib/seats/${seatId}/restrict`;

      console.log('🔒 Adding restriction for seatId:', seatId);

      const response = await fetch(url, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({})
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ error: 'Unknown error' }));
        throw new Error(errorData.error || `Failed to add restriction (${response.status})`);
      }

      return await response.json();
    } catch (error) {
      console.error('❌ Error adding restriction:', error);
      throw error;
    }
  },

  /**
   * Bỏ hạn chế cho ghế (dùng seatId)
   * DELETE /slib/seats/{seatId}/restrict
   */
  async removeRestriction(seatId) {
    try {
      const url = `${BASE}/slib/seats/${seatId}/restrict`;

      console.log('Removing restriction for seatId:', seatId);

      const response = await fetch(url, {
        method: 'DELETE',
        headers: getAuthHeaders()
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ error: 'Unknown error' }));
        throw new Error(errorData.error || `Failed to remove restriction (${response.status})`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error removing restriction:', error);
      throw error;
    }
  },

  /**
   * Lấy danh sách zones
   * GET /slib/zones
   */
  async getZones() {
    try {
      const url = `${BASE}/slib/zones`;
      const response = await fetch(url, {
        method: 'GET',
        headers: getAuthHeaders()
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch zones (${response.status})`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error fetching zones:', error);
      throw error;
    }
  }
};