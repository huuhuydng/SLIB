import axios from "axios";


const chatApi = axios.create({
  baseURL: "http://localhost:8080/slib", 
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
    
    console.log("[ChatAPI Request]", config.method?.toUpperCase(), config.url);
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

export default chatApi;