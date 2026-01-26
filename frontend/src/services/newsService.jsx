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

// Toggle pin status
export const toggleNewsPin = async (id) => {
  try {
    const response = await axios.patch(`${API_URL}/admin/${id}/pin`);
    return response.data;
  } catch (error) {
    console.error('Error toggling pin:', error);
    throw error;
  }
};

// ============================================
// CATEGORY APIs
// ============================================

const CATEGORY_URL = 'http://localhost:8080/slib/categories';

// Lấy tất cả categories
export const getAllCategories = async () => {
  try {
    const response = await axios.get(CATEGORY_URL);
    return response.data;
  } catch (error) {
    console.error('Error fetching categories:', error);
    throw error;
  }
};

// Tạo category mới
export const createCategory = async (name, colorCode = null) => {
  try {
    const response = await axios.post(CATEGORY_URL, { name, colorCode });
    return response.data;
  } catch (error) {
    console.error('Error creating category:', error);
    throw error;
  }
};

// Xoá category
export const deleteCategory = async (id) => {
  try {
    const response = await axios.delete(`${CATEGORY_URL}/${id}`);
    return response.data;
  } catch (error) {
    console.error('Error deleting category:', error);
    throw error;
  }
};

// ============================================
// IMAGE UPLOAD API (via Backend -> Cloudinary)
// ============================================

const UPLOAD_URL = 'http://localhost:8080/slib/files/upload_news_image';

// Upload ảnh qua Backend
export const uploadImage = async (file) => {
  if (!file) {
    throw new Error('No file provided');
  }

  if (!file.type.startsWith('image/')) {
    throw new Error('Invalid file type. Only images are allowed.');
  }

  const MAX_SIZE = 10 * 1024 * 1024;
  if (file.size > MAX_SIZE) {
    throw new Error('File too large. Maximum size is 10MB.');
  }

  const formData = new FormData();
  formData.append('file', file);

  try {
    const response = await axios.post(UPLOAD_URL, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    console.log('✅ Image upload success:', response.data);
    return response.data;
  } catch (error) {
    console.error('❌ Image upload error:', error);
    throw new Error(error.response?.data?.message || 'Upload failed');
  }
};
