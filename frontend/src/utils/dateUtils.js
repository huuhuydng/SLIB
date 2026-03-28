/**
 * src/utils/dateUtils.js
 */
import React from 'react'; // ⚠️ Quan trọng: Phải import React

// 1. Hàm format giờ
export const formatTime = (dateString) => {
    if (!dateString) return "";
    const date = new Date(dateString);
    return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false });
};

// 2. Hàm lấy nhãn ngày
export const getDateLabel = (dateString) => {
    if (!dateString) return "";
    const date = new Date(dateString);
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    if (date.toDateString() === today.toDateString()) {
        return "Hôm nay";
    } else if (date.toDateString() === yesterday.toDateString()) {
        return "Hôm qua";
    } else {
        return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
    }
};

// 3. Hàm kiểm tra khác ngày
export const isDifferentDay = (currentMsg, prevMsg) => {
    if (!prevMsg) return true;
    if (!currentMsg.createdAt || !prevMsg.createdAt) return false;
    
    const currentDate = new Date(currentMsg.createdAt).toDateString();
    const prevDate = new Date(prevMsg.createdAt).toDateString();
    return currentDate !== prevDate;
};

// 4. Hàm tô đậm từ khóa (Phiên bản dùng cho file .js)
export const highlightText = (text, keyword) => {
    if (!keyword || !text) return text;
    
    const parts = text.split(new RegExp(`(${keyword})`, 'gi'));
    
    return parts.map((part, index) => 
        part.toLowerCase() === keyword.toLowerCase() ? 
        React.createElement('span', { key: index, className: 'highlight-keyword' }, part) 
        : part
    );
};