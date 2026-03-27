import axios from "axios";
import { API_BASE_URL } from '../../config/apiConfig';

const chatApi = axios.create({
  baseURL: `${API_BASE_URL}/slib`,
  headers: {
    "Content-Type": "application/json",
  },
});

chatApi.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("librarian_token");

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

/* ========================== CÁC HÀM GỌI API ========================= */

// Lấy lịch sử
export const getChatHistory = (otherUserId, page = 0, size = 20) =>
  chatApi.get(`/chat/history/${otherUserId}`, {
    params: { page, size }
  });

// Lấy danh sách người đã chat
export const getConversations = () =>
  chatApi.get(`/chat/conversations`);

// Lấy danh sách tìm kiếm
export const searchMessages = (partnerId, keyword) =>
  chatApi.get(`/chat/search`, {
    params: { partnerId, keyword }
  });

// Tìm xem tin nhắn nằm ở trang số mấy
export const findMessagePage = (partnerId, messageId) =>
  chatApi.get(`/chat/find-page`, {
    params: { partnerId, messageId }
  });

// Hàm lấy số tin nhắn chưa đọc
export const getUnreadCount = () => {
  return chatApi.get('/chat/unread-count');
};

// Hàm đánh dấu đã đọc
export const markMessagesAsRead = (senderId) => {
  return chatApi.post('/chat/mark-read', { senderId });
};

// Upload ảnh (Đoạn này bạn sửa đúng rồi)
export const uploadFile = (file) => {
  const formData = new FormData();
  formData.append('file', file);

  return chatApi.post('/files/upload_chat_image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
};

// Lấy kho lưu trữ (Ảnh hoặc File)
export const getConversationMedia = (partnerId, type) =>
  chatApi.get(`/chat/media/${partnerId}`, {
    params: { type } // type là 'IMAGE' hoặc 'FILE'
  });

//hàm này để upload tài liệu (PDF, Word...)
export const uploadDocument = (file) => {
  const formData = new FormData();
  formData.append('file', file);

  // Đảm bảo /files/upload_document là chính xác ở Backend
  return chatApi.post('/files/upload_document', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
};

// ==================== CONVERSATION MANAGEMENT (AI-to-Human Escalation) ====================

// Lấy danh sách conversation đang chờ xử lý
export const getWaitingConversations = () =>
  chatApi.get('/chat/conversations/waiting');

// Lấy danh sách conversation đang được librarian xử lý
export const getActiveConversations = () =>
  chatApi.get('/chat/conversations/active');

// Lấy tất cả conversations (waiting + active)
export const getAllConversations = () =>
  chatApi.get('/chat/conversations/all');

// Librarian tiếp nhận conversation
export const takeOverConversation = (conversationId) =>
  chatApi.post(`/chat/conversations/${conversationId}/take-over`);

// Đánh dấu conversation đã hoàn thành
export const resolveConversation = (conversationId) =>
  chatApi.post(`/chat/conversations/${conversationId}/resolve`);

// Gửi tin nhắn vào conversation (cho librarian chat với student)
export const sendConversationMessage = (conversationId, content, senderType = 'LIBRARIAN') =>
  chatApi.post(`/chat/conversations/${conversationId}/messages`, { content, senderType });

// Đếm số conversation đang chờ
export const getWaitingCount = () =>
  chatApi.get('/chat/conversations/waiting/count');

// Lấy danh sách messages của conversation
export const getConversationMessages = (conversationId) =>
  chatApi.get(`/chat/conversations/${conversationId}/messages`);

export default chatApi;
