import axios from 'axios';
import { API_BASE_URL } from '../../config/apiConfig';

const API_URL = `${API_BASE_URL}/slib/new-books`;
const FILE_API_URL = `${API_BASE_URL}/slib/files`;

const getAuthHeaders = () => {
  const token = sessionStorage.getItem('librarian_token') || localStorage.getItem('librarian_token');
  return token ? { Authorization: `Bearer ${token}` } : {};
};

export const getAllNewBooksForAdmin = async () => {
  const response = await axios.get(`${API_URL}/admin`, { headers: getAuthHeaders() });
  return response.data;
};

export const getNewBookDetailForAdmin = async (id) => {
  const response = await axios.get(`${API_URL}/admin/${id}`, { headers: getAuthHeaders() });
  return response.data;
};

export const previewNewBookFromUrl = async (url) => {
  const response = await axios.post(
    `${API_URL}/admin/preview`,
    { url },
    { headers: getAuthHeaders() },
  );
  return response.data;
};

export const createNewBook = async (payload) => {
  const response = await axios.post(`${API_URL}/admin`, payload, { headers: getAuthHeaders() });
  return response.data;
};

export const updateNewBook = async (id, payload) => {
  const response = await axios.put(`${API_URL}/admin/${id}`, payload, { headers: getAuthHeaders() });
  return response.data;
};

export const toggleNewBookActive = async (id) => {
  const response = await axios.patch(`${API_URL}/admin/${id}/toggle-active`, null, { headers: getAuthHeaders() });
  return response.data;
};

export const toggleNewBookPin = async (id) => {
  const response = await axios.patch(`${API_URL}/admin/${id}/pin`, null, { headers: getAuthHeaders() });
  return response.data;
};

export const deleteNewBook = async (id) => {
  await axios.delete(`${API_URL}/admin/${id}`, { headers: getAuthHeaders() });
};

export const batchDeleteNewBooks = async (ids) => {
  const response = await axios.delete(`${API_URL}/admin/batch`, {
    headers: getAuthHeaders(),
    data: { ids },
  });
  return response.data;
};

export const uploadNewBookCover = async (file) => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await axios.post(`${FILE_API_URL}/upload_news_image`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      ...getAuthHeaders(),
    },
  });

  return response.data.url;
};
