const API_BASE_URL = 'http://localhost:8080/api';
  
const getAuthHeaders = () => {
  const token = localStorage.getItem('token');
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
        ? `${API_BASE_URL}/seat-management/seats?${queryString}`
        : `${API_BASE_URL}/seat-management/seats`;
      
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
   * Thêm hạn chế cho ghế
   */
  async addRestriction(restrictionData) {
    try {
      console.log('📤 POST /seat-management/restrictions', restrictionData);
      
      const response = await fetch(`${API_BASE_URL}/seat-management/restrictions`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(restrictionData)
      });
      
      if (!response.ok) {
        const errorText = await response.text();
        console.error('❌ Add restriction failed:', errorText);
        throw new Error(`Failed to add restriction (${response.status}): ${errorText}`);
      }
      
      const result = await response.json();
      console.log('✅ Add restriction success:', result);
      return result;
    } catch (error) {
      console.error('❌ Error adding restriction:', error);
      throw error;
    }
  },

  /**
   * Bỏ hạn chế cho ghế
   */
  async removeRestriction(seatCode) {
    try {
      console.log('📤 DELETE /seat-management/restrictions/' + seatCode);
      
      const response = await fetch(`${API_BASE_URL}/seat-management/restrictions/${seatCode}`, {
        method: 'DELETE',
        headers: getAuthHeaders()
      });
      
      if (!response.ok) {
        const errorText = await response.text();
        console.error('❌ Remove restriction failed:', errorText);
        throw new Error(`Failed to remove restriction (${response.status}): ${errorText}`);
      }
      
      const result = await response.json();
      console.log('✅ Remove restriction success:', result);
      return result;
    } catch (error) {
      console.error('❌ Error removing restriction:', error);
      throw error;
    }
  }
};