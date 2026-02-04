import axios from 'axios';

const API_URL = 'http://localhost:8080/slib/news';

// Lấy danh sách tất cả news cho admin/librarian
export const getAllNewsForAdmin = async () => {
  try {
    const response = await axios.get(`${API_URL}/admin/all`);
    return response.data;
  } catch (error) {
    console.error('Error fetching news:', error);
    throw error;
  }
};

// Lấy detail cho admin (không có imageUrl)
export const getNewsDetailForAdmin = async (id) => {
  try {
    const response = await axios.get(`${API_URL}/admin/detail/${id}`);
    return response.data;
  } catch (error) {
    console.error('Error fetching news detail:', error);
    throw error;
  }
};

// Lấy image URL riêng
export const getNewsImage = async (id) => {
  try {
    const response = await axios.get(`${API_URL}/admin/image/${id}`);
    return response.data;
  } catch (error) {
    console.error('Error fetching news image:', error);
    return null;
  }
};

// Tạo news mới
export const createNews = async (newsData) => {
  try {
    console.log('📤 Sending to backend:', JSON.stringify(newsData, null, 2));
    const response = await axios.post(`${API_URL}/admin`, newsData);
    console.log('✅ Success:', response.data);
    return response.data;
  } catch (error) {
    console.error('❌ Error creating news:', error);
    console.error('📋 Error response:', error.response?.data);
    console.error('📋 Error status:', error.response?.status);
    console.error('📋 Error message:', error.response?.data?.message || error.message);
    throw error;
  }
};

// Cập nhật news
export const updateNews = async (id, newsData) => {
  try {
    const response = await axios.put(`${API_URL}/admin/${id}`, newsData);
    return response.data;
  } catch (error) {
    console.error('Error updating news:', error);
    throw error;
  }
};

// Xóa news
export const deleteNews = async (id) => {
  try {
    const response = await axios.delete(`${API_URL}/admin/${id}`);
    return response.data;
  } catch (error) {
    console.error('Error deleting news:', error);
    throw error;
  }
};

// Lấy news public (cho student)
export const getPublicNews = async () => {
  try {
    const response = await axios.get(`${API_URL}/public`);
    return response.data;
  } catch (error) {
    console.error('Error fetching public news:', error);
    throw error;
  }
};

// Lấy chi tiết news
export const getNewsDetail = async (id) => {
  try {
    const response = await axios.get(`${API_URL}/public/detail/${id}`);
    return response.data;
  } catch (error) {
    console.error('Error fetching news detail:', error);
    throw error;
  }
};

// Toggle pin status của news
export const togglePinNews = async (id) => {
  try {
    const response = await axios.put(`${API_URL}/admin/${id}/toggle-pin`);
    return response.data;
  } catch (error) {
    console.error('Error toggling pin status:', error);
    throw error;
  }
};

// ============== CATEGORY APIs ==============

const CATEGORY_URL = 'http://localhost:8080/slib/news-category';

// Lay danh sach tat ca categories
export const getAllCategories = async () => {
  try {
    const response = await axios.get(`${CATEGORY_URL}`);
    return response.data;
  } catch (error) {
    console.error('Error fetching categories:', error);
    throw error;
  }
};

// Tao category moi
export const createCategory = async (name, color = '#3b82f6') => {
  try {
    const response = await axios.post(`${CATEGORY_URL}`, { name, color });
    return response.data;
  } catch (error) {
    console.error('Error creating category:', error);
    throw error;
  }
};

// Xoa category
export const deleteCategory = async (id) => {
  try {
    const response = await axios.delete(`${CATEGORY_URL}/${id}`);
    return response.data;
  } catch (error) {
    console.error('Error deleting category:', error);
    throw error;
  }
};

// ============== UPLOAD IMAGE API ==============

// Upload image va tra ve URL
export const uploadImage = async (file) => {
  try {
    const formData = new FormData();
    formData.append('file', file);
    const response = await axios.post('http://localhost:8080/slib/news/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return response.data;
  } catch (error) {
    console.error('Error uploading image:', error);
    throw error;
  }
};
