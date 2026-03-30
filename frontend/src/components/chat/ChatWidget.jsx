import React, { useState, useEffect, useRef, useLayoutEffect } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useNavigate } from 'react-router-dom';
import { API_BASE_URL } from '../../config/apiConfig';
import '../../styles/ChatWidget.css';
import { getChatHistory, getConversations, uploadFile, uploadDocument, getUnreadCount, markMessagesAsRead } from '../../services/admin/apiChat';
import { formatTime, getDateLabel, isDifferentDay } from '../../utils/dateUtils';

import attach from '../../assets/attach.svg';
import file from '../../assets/file.svg';
import image from '../../assets/image.svg';
import back from '../../assets/back.svg';
import dropdown from '../../assets/dropdown.svg';

const ChatWidget = () => {
    // ================= STATE =================
    const [isOpen, setIsOpen] = useState(false);
    const [conversations, setConversations] = useState([]);
    const [selectedUser, setSelectedUser] = useState(null);
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState("");
    const [isUploading, setIsUploading] = useState(false);
    const [unreadCount, setUnreadCount] = useState(0); // Tổng số tin chưa đọc cho Badge Launcher

    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);
    const [isReady, setIsReady] = useState(false);
    const [showHeaderMenu, setShowHeaderMenu] = useState(false);

    // Lưu trữ partnerId đã đọc tin nhắn 
    const [seenByPartners, setSeenByPartners] = useState(new Set());

    const [selectedFullImage, setSelectedFullImage] = useState(null);

    // ================= REFS =================
    const navigate = useNavigate();
    const stompClientRef = useRef(null);
    const fileInputRef = useRef(null);
    const selectedUserRef = useRef(null);
    const isOpenRef = useRef(false);
    const messagesContainerRef = useRef(null);
    const chatWindowRef = useRef(null);
    const launcherRef = useRef(null);
    const menuRef = useRef(null);
    const prevScrollHeightRef = useRef(0);
    // Ngăn cách 2 input file
    const imageInputRef = useRef(null);

    const TOKEN = localStorage.getItem("librarian_token");
    const userStr = localStorage.getItem("librarian_user");
    let MY_ID = null;
    try { if (userStr) MY_ID = JSON.parse(userStr).id; } catch (e) { }

    // ================= LOGIC HANDLERS =================

    const fetchTotalUnread = async () => {
        try {
            const res = await getUnreadCount();
            setUnreadCount(res.data);
        } catch (e) { console.error(e); }
    };

    const loadConversations = async () => {
        try {
            const res = await getConversations();
            // Sắp xếp ưu tiên người có tin nhắn mới nhất lên đầu
            const sorted = res.data.sort((a, b) =>
                new Date(b.latestMessageTime || 0) - new Date(a.latestMessageTime || 0)
            );
            setConversations(sorted);
        } catch (e) { console.error(e); }
    };

    const handleMarkAsReadSync = async (partnerId) => {
        if (!partnerId) return;
        try {
            await markMessagesAsRead(partnerId);
            await fetchTotalUnread();
            // Cập nhật local state của list conversations để xóa số unread của người này
            setConversations(prev => prev.map(c =>
                c.id === partnerId ? { ...c, unreadCount: 0 } : c
            ));
            // Cập nhật messages: đánh dấu tất cả tin nhắn từ partner là đã đọc
            setMessages(prev => prev.map(msg =>
                msg.senderId === partnerId && !msg.isRead ? { ...msg, isRead: true } : msg
            ));
        } catch (e) { }
    };

    const handleCloseAll = () => {
        setIsOpen(false);
        isOpenRef.current = false; // Sync ref
        setSelectedUser(null);
        setMessages([]);
        setIsReady(false);
        // Fetch lại badge khi đóng để đảm bảo số liệu chính xác từ server
        fetchTotalUnread();
    };

    // ================= EFFECT =================

    // Sync isOpen với ref
    useEffect(() => {
        isOpenRef.current = isOpen;
    }, [isOpen]);

    useEffect(() => {
        if (MY_ID) fetchTotalUnread();
        if (MY_ID && TOKEN) {
            setupWebSocket();
            loadConversations();
        }
        return () => { if (stompClientRef.current) stompClientRef.current.deactivate(); };
    }, [MY_ID]);

    useEffect(() => {
        selectedUserRef.current = selectedUser;
        if (selectedUser) {
            setPage(0);
            setHasMore(true);
            setMessages([]);
            setShowHeaderMenu(false);
            setIsReady(false);

            loadHistory(selectedUser, 0);
            // Click vào là mark read ngay
            handleMarkAsReadSync(selectedUser);
        }
    }, [selectedUser]);

    useLayoutEffect(() => {
        const container = messagesContainerRef.current;
        if (!container || messages.length === 0) return;

        if (page === 0 && !isReady) {
            // Đợi tất cả ảnh load xong trước khi scroll
            const images = container.querySelectorAll('.message-image');

            if (images.length === 0) {
                // Không có ảnh -> scroll ngay
                requestAnimationFrame(() => {
                    if (container) {
                        container.scrollTop = container.scrollHeight;
                        setIsReady(true);
                    }
                });
            } else {
                // Có ảnh -> đợi tất cả ảnh load xong
                let loadedCount = 0;
                const totalImages = images.length;
                let timeoutId = null;

                const checkAllLoaded = () => {
                    loadedCount++;
                    if (loadedCount === totalImages && container) {
                        if (timeoutId) clearTimeout(timeoutId);
                        requestAnimationFrame(() => {
                            if (container) {
                                container.scrollTop = container.scrollHeight;
                                setIsReady(true);
                            }
                        });
                    }
                };

                const listeners = [];
                images.forEach(img => {
                    if (img.complete) {
                        checkAllLoaded();
                    } else {
                        const loadHandler = () => checkAllLoaded();
                        const errorHandler = () => checkAllLoaded();
                        img.addEventListener('load', loadHandler);
                        img.addEventListener('error', errorHandler);
                        listeners.push({ img, loadHandler, errorHandler });
                    }
                });

                // Timeout fallback: scroll sau 1.5s dù ảnh chưa load xong
                timeoutId = setTimeout(() => {
                    if (container) {
                        container.scrollTop = container.scrollHeight;
                        setIsReady(true);
                    }
                }, 1500);

                // Cleanup function
                return () => {
                    if (timeoutId) clearTimeout(timeoutId);
                    listeners.forEach(({ img, loadHandler, errorHandler }) => {
                        img.removeEventListener('load', loadHandler);
                        img.removeEventListener('error', errorHandler);
                    });
                };
            }
        } else if (page > 0 && prevScrollHeightRef.current > 0) {
            const diff = container.scrollHeight - prevScrollHeightRef.current;
            container.scrollTop = diff;
            prevScrollHeightRef.current = 0;
        }
    }, [messages, page, isReady]);

    // ================= WEBSOCKET =================
    const setupWebSocket = () => {
        if (stompClientRef.current && stompClientRef.current.connected) return;
        const client = new Client({
            webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws`),
            connectHeaders: { Authorization: `Bearer ${TOKEN}` },
            onConnect: () => {
                client.subscribe(`/topic/chat/${MY_ID}`, (message) => {
                    const receivedMsg = JSON.parse(message.body);
                    const isMyMessage = (receivedMsg.senderId === MY_ID);
                    const partnerId = isMyMessage ? receivedMsg.receiverId : receivedMsg.senderId;

                    // 1. Cập nhật danh sách hội thoại: Đẩy lên đầu & tăng unread
                    setConversations(prev => {
                        const updatedList = [...prev];
                        const idx = updatedList.findIndex(c => c.id === partnerId);

                        if (idx > -1) {
                            const item = { ...updatedList[idx] };
                            // Nếu người khác nhắn và mình không đang mở chat với họ -> Tăng unread từng người
                            if (!isMyMessage && selectedUserRef.current !== partnerId) {
                                item.unreadCount = (item.unreadCount || 0) + 1;
                            }
                            item.latestMessageTime = receivedMsg.createdAt;
                            updatedList.splice(idx, 1);
                            updatedList.unshift(item);
                        } else {
                            loadConversations(); // Người mới nhắn lần đầu -> Load lại list
                        }
                        return updatedList;
                    });

                    // 2. Xử lý Badge Launcher & Khung chat
                    if (!isMyMessage) {
                        if (isOpenRef.current && selectedUserRef.current === partnerId) {
                            // Đang mở chat với người này → mark as read và cập nhật badge
                            markMessagesAsRead(partnerId)
                                .then(() => {
                                    fetchTotalUnread();
                                })
                                .catch(err => console.error("Lỗi mark as read:", err));
                        } else {
                            // Không mở chat → tăng badge
                            setUnreadCount(prev => prev + 1);
                        }
                    }

                    setMessages((prev) => {
                        if (partnerId === selectedUserRef.current) {
                            setTimeout(() => {
                                if (messagesContainerRef.current)
                                    messagesContainerRef.current.scrollTop = messagesContainerRef.current.scrollHeight;
                            }, 100);
                            return [...prev, receivedMsg];
                        }
                        return prev;
                    });
                });

                // Subscribe thông báo "Đã xem"
                client.subscribe(`/topic/chat/seen/${MY_ID}`, (notification) => {
                    const seenData = JSON.parse(notification.body);
                    const { partnerId } = seenData;

                    // Lưu partnerId vào Set để apply sau
                    setSeenByPartners(prev => new Set(prev).add(partnerId));

                    // Cập nhật messages: đánh dấu tất cả tin của mình gửi cho partnerId là đã đọc
                    setMessages(prev => {
                        return prev.map(msg =>
                            (msg.senderId === MY_ID && msg.receiverId === partnerId && !msg.isRead)
                                ? { ...msg, isRead: true }
                                : msg
                        );
                    });
                });
            }
        });
        client.activate();
        stompClientRef.current = client;
    };

    // ... (Các hàm loadHistory, handleScroll, handleSendMessage giữ nguyên giống bản trước) ...
    const loadHistory = async (uid, pageNum) => {
        try {
            if (pageNum > 0 && messagesContainerRef.current) prevScrollHeightRef.current = messagesContainerRef.current.scrollHeight;
            const res = await getChatHistory(uid, pageNum, 20);
            if (pageNum === 0) {
                let loadedMessages = res.data.content.reverse();

                // Apply các notification "seen" đã nhận trước đó
                if (seenByPartners.has(uid)) {
                    loadedMessages = loadedMessages.map(msg =>
                        (msg.senderId !== uid && !msg.isRead)
                            ? { ...msg, isRead: true }
                            : msg
                    );
                }

                setMessages(loadedMessages);
                if (res.data.content.length === 0) setIsReady(true);
            } else {
                setMessages(prev => [...res.data.content.reverse(), ...prev]);
            }
            setHasMore(pageNum < res.data.totalPages - 1);
            setLoadingMore(false);
        } catch (e) { setLoadingMore(false); setIsReady(true); }
    };

    const handleScroll = (e) => {
        const { scrollTop } = e.target;
        if (scrollTop === 0 && hasMore && !loadingMore && isReady) {
            setLoadingMore(true);
            const nextPage = page + 1;
            setPage(nextPage);
            loadHistory(selectedUser, nextPage);
        }
    };

    const handleSendMessage = () => {
        if (!input.trim() || !selectedUser) return;
        sendToWebSocket(input, null, "TEXT");
        setInput("");
    };

    const handleFileUpload = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        setIsUploading(true);

        try {
            const isImage = file.type.startsWith('image/');
            let res;

            // 1. Gọi API Upload
            if (isImage) {
                res = await uploadFile(file);
            } else {
                res = await uploadDocument(file);
            }

            // 2. Kiểm tra dữ liệu phản hồi
            const data = res.data;

            if (data && data.url) {
                sendToWebSocket(file.name, data.url, data.type);
            } else {
                console.warn("⚠️ [WARNING] Upload thành công nhưng Server không trả về URL đính kèm.");
            }
        } catch (error) {
            console.error("❌ [ERROR] Quá trình tải file thất bại:");
            if (error.response) {
                console.error("Status:", error.response.status, "Data:", error.response.data);
            } else {
                console.error("Message:", error.message);
            }
            console.error("Upload thất bại! Vui lòng kiểm tra lại kết nối Server.");
        } finally {
            setIsUploading(false);
            e.target.value = ""; // Reset input
        }
    };

    const sendToWebSocket = (content, attachmentUrl, type) => {
        if (stompClientRef.current?.connected) {
            const chatMessage = { senderId: MY_ID, receiverId: selectedUser, content, attachmentUrl, type };
            stompClientRef.current.publish({ destination: '/app/chat', body: JSON.stringify(chatMessage) });
        }
    };

    const handleOpenFullChat = (e) => {
        e.stopPropagation();

        // Kiểm tra kỹ selectedUser có giá trị không
        if (!selectedUser) return;

        // Đóng widget trước khi chuyển trang
        handleCloseAll();

        // Điều hướng và truyền state
        navigate('/librarian/chat', {
            state: { targetUserId: selectedUser },
            replace: true // Sử dụng replace nếu bạn không muốn người dùng bấm back lại widget đang đóng
        });
    };

    useEffect(() => {
        function handleClickOutside(event) {
            if (isOpen && chatWindowRef.current && !chatWindowRef.current.contains(event.target) && launcherRef.current && !launcherRef.current.contains(event.target)) {
                setIsOpen(false);
                isOpenRef.current = false; // Sync ref
            }
            if (showHeaderMenu && menuRef.current && !menuRef.current.contains(event.target)) setShowHeaderMenu(false);
        }
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, [isOpen, showHeaderMenu]);

    return (
        <>
            {!isOpen && (
                <div ref={launcherRef} className="chat-widget-launcher" onClick={() => { setIsOpen(true); isOpenRef.current = true; }}>
                    💬
                    {unreadCount > 0 && <div className="unread-badge">{unreadCount > 99 ? '99+' : unreadCount}</div>}
                </div>
            )}

            {isOpen && (
                <div ref={chatWindowRef} className="chat-window">
                    <div className="widget-header">
                        <div className="header-left">
                            {selectedUser && (
                                <button className="back-btn-new" onClick={() => setSelectedUser(null)}>
                                    <img src={back} alt="back" />
                                </button>
                            )}
                            <div className="header-user-info" ref={menuRef} onClick={() => selectedUser && setShowHeaderMenu(!showHeaderMenu)}>
                                <span className="user-name">
                                    {selectedUser
                                        ? (conversations.find(c => c.id === selectedUser)?.fullName || "Người dùng")
                                        : "Tin nhắn"}
                                </span>

                                {selectedUser && (
                                    <>
                                        <img
                                            src={dropdown}
                                            alt="more"
                                            className={`dropdown-icon ${showHeaderMenu ? 'rotate' : ''}`}
                                        />
                                        {showHeaderMenu && (
                                            <div className="header-dropdown-menu">
                                                <div className="menu-item" onClick={handleOpenFullChat}>
                                                    ↗ Mở trong cửa sổ chat
                                                </div>
                                            </div>
                                        )}
                                    </>
                                )}
                            </div>
                        </div>
                        <button className="close-btn-new" onClick={handleCloseAll}>✕</button>
                    </div>

                    {!selectedUser ? (
                        <div className="conversation-list">
                            {conversations.length === 0 && <div style={{ padding: 20, textAlign: 'center', color: '#999', fontSize: 12 }}>Chưa có tin nhắn</div>}
                            {conversations.map((partner) => (
                                <div key={partner.id} className="conversation-item" onClick={() => setSelectedUser(partner.id)}>
                                    <div className="avatar-circle" style={{ width: 35, height: 35, fontSize: 14 }}>{partner.fullName?.charAt(0).toUpperCase() || "U"}</div>
                                    <div className="conv-info" style={{ flex: 1, marginLeft: 10, overflow: 'hidden' }}>
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                            <span style={{ fontWeight: '600', fontSize: '13px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{partner.fullName}</span>
                                            {partner.unreadCount > 0 && (
                                                <span className="conv-unread-dot">{partner.unreadCount}</span>
                                            )}
                                        </div>
                                        <div style={{ fontSize: '11px', color: '#888', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{partner.email}</div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <>
                            <div className="messages-area" ref={messagesContainerRef} onScroll={handleScroll} style={{ opacity: isReady ? 1 : 0, transition: 'opacity 0.2s ease-in' }}>
                                {loadingMore && <div style={{ textAlign: 'center', fontSize: 10, color: '#999', padding: '5px 0' }}>⏳...</div>}
                                {messages.map((msg, idx) => {
                                    const isMyMsg = msg.senderId === MY_ID;
                                    const isLastMsg = idx === messages.length - 1;
                                    const currentPartner = conversations.find(c => c.id === selectedUser);

                                    return (
                                        <React.Fragment key={msg.id || idx}>
                                            {isDifferentDay(msg, idx > 0 ? messages[idx - 1] : null) && (
                                                <div className="date-separator"><span>{getDateLabel(msg.createdAt)}</span></div>
                                            )}

                                            <div id={`msg-${msg.id}`} className={`message-row ${isMyMsg ? 'sent' : 'received'}`}>
                                                {/* Container bọc ngang: Cho phép bubble và giờ nằm cạnh nhau */}
                                                <div className="message-wrapper-horizontal">
                                                    <div className="message-bubble">
                                                        {/* Hiển thị đính kèm (Ảnh/File) */}
                                                        {msg.attachmentUrl && (
                                                            msg.type === 'IMAGE' || msg.attachmentUrl.match(/\.(jpeg|jpg|gif|png)$/) != null ? (
                                                                <img src={msg.attachmentUrl} alt="" className="message-image" onClick={() => setSelectedFullImage(msg.attachmentUrl)} />
                                                            ) : (
                                                                <div className="file-attachment-box">
                                                                    <span style={{ marginRight: '8px' }}>📄</span>
                                                                    <a href={msg.attachmentUrl} target="_blank" rel="noopener noreferrer">
                                                                        {msg.content || "Tài liệu đính kèm"}
                                                                    </a>
                                                                </div>
                                                            )
                                                        )}

                                                        {/* Hiển thị nội dung text (chỉ khi không có file) */}
                                                        {!msg.attachmentUrl && msg.content && (
                                                            <div className="msg-text">
                                                                {msg.content}
                                                            </div>
                                                        )}
                                                    </div>

                                                    {/* Bong bóng thời gian nhỏ nằm bên ngoài cạnh bubble */}
                                                    <div className="msg-timestamp-bubble">
                                                        {formatTime(msg.createdAt)}
                                                    </div>
                                                </div>
                                            </div>

                                            {isMyMsg && isLastMsg && (
                                                <div className="widget-seen-status">
                                                    {msg.isRead ? (
                                                        <span className="widget-seen-text">{currentPartner?.fullName} đã xem</span>
                                                    ) : (
                                                        <span className="widget-sent-text">Đã gửi</span>
                                                    )}
                                                </div>
                                            )}
                                        </React.Fragment>
                                    );
                                })}
                            </div>

                            <div className="chat-input-area">
                                {/* Input ẩn để chọn file đa năng */}
                                <input
                                    type="file"
                                    ref={fileInputRef}
                                    hidden
                                    onChange={handleFileUpload}
                                    accept="image/*, .pdf, .doc, .docx, .xls, .xlsx, .zip"
                                />

                                {/* 2. Input ẩn dành riêng cho HÌNH ẢNH */}
                                <input
                                    type="file"
                                    ref={imageInputRef}
                                    hidden
                                    onChange={handleFileUpload}
                                    accept="image/*"
                                />

                                <div className="input-actions-group">
                                    {/* Nút kẹp giấy để Up tài liệu - Dùng icon file.svg */}
                                    <button
                                        className="btn-icon"
                                        onClick={() => fileInputRef.current.click()}
                                        disabled={isUploading}
                                        title="Đính kèm tài liệu"
                                    >
                                        {isUploading ? (
                                            <span className="spinner-mini"></span>
                                        ) : (
                                            <img src={attach} alt="attach" className="icon-svg-black" />
                                        )}
                                    </button>

                                    {/* Nút Máy ảnh chuyên Up ảnh - Dùng icon attach.svg */}
                                    <button
                                        className="btn-icon"
                                        onClick={() => imageInputRef.current.click()}
                                        disabled={isUploading}
                                        title="Gửi hình ảnh"
                                    >
                                        {isUploading ? (
                                            <span className="spinner-mini"></span>
                                        ) : (
                                            <img src={image} alt="image" className="icon-svg-black" />
                                        )}
                                    </button>
                                </div>

                                <input
                                    className="input-field"
                                    value={input}
                                    onChange={e => setInput(e.target.value)}
                                    onKeyDown={e => e.key === 'Enter' && handleSendMessage()}
                                    placeholder="Nhập tin..."
                                    style={{ flex: 1 }}
                                />
                                <button className="btn-send-text" onClick={handleSendMessage}>Gửi</button>
                            </div>
                            {selectedFullImage && (
                                <div className="image-lightbox-overlay" onClick={() => setSelectedFullImage(null)}>
                                    <button className="close-lightbox" onClick={() => setSelectedFullImage(null)}>✕</button>
                                    <div className="lightbox-content" onClick={(e) => e.stopPropagation()}>
                                        <img src={selectedFullImage} alt="Full view" />
                                        <a href={selectedFullImage} download target="_blank" rel="noreferrer" className="download-full-btn">
                                            Xem ảnh
                                        </a>
                                    </div>
                                </div>
                            )}
                        </>
                    )}
                </div>
            )}
        </>
    );
};

export default ChatWidget;
