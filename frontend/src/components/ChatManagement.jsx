import React, { useState, useEffect, useRef, useLayoutEffect } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import '../styles/ChatManagement.css';
import {
    getChatHistory,
    getConversations,
    uploadFile,
    uploadDocument,
    searchMessages,
    findMessagePage,
    markMessagesAsRead,
    getWaitingConversations,
    getActiveConversations,
    takeOverConversation as takeOverConversationAPI
} from '../services/admin/apiChat';
import { formatTime, getDateLabel, isDifferentDay, highlightText } from '../utils/dateUtils';
import { useLocation } from 'react-router-dom';
import attach from '../assets/attach.svg';
import file from '../assets/file.svg';
import image from '../assets/image.svg';

// IMPORT COMPONENT SIDEBAR PHẢI
import ChatSidebarRight from './ChatSidebarRight';

const ChatManagement = () => {
    // ================= STATE =================
    const [conversations, setConversations] = useState([]);
    const [selectedUser, setSelectedUser] = useState(null);
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState("");
    const [isUploading, setIsUploading] = useState(false);

    // State phân trang & Scroll
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);
    const [isReady, setIsReady] = useState(false);
    const [processing, setProcessing] = useState(false);
    const [isScrolling, setIsScrolling] = useState(false);

    const [selectedFullImage, setSelectedFullImage] = useState(null);

    // Quản lý Sidebar Phải
    const [showRightSidebar, setShowRightSidebar] = useState(false);
    const [searchResults, setSearchResults] = useState([]);
    const [isSearching, setIsSearching] = useState(false);
    const [highlightKeyword, setHighlightKeyword] = useState("");

    // ================= AI-to-Human Escalation State =================
    const [activeTab, setActiveTab] = useState('all'); // 'all' | 'waiting' | 'active'
    const [waitingConversations, setWaitingConversations] = useState([]);
    const [activeConversations, setActiveConversations] = useState([]);
    const [selectedConversation, setSelectedConversation] = useState(null);
    const [showEscalateToast, setShowEscalateToast] = useState(false);
    const [escalateToastData, setEscalateToastData] = useState(null);

    // ================= REFS =================
    const stompClientRef = useRef(null);
    const fileInputRef = useRef(null);
    const imageInputRef = useRef(null);
    const selectedUserRef = useRef(null);
    const messagesContainerRef = useRef(null);
    const scrollToMessageId = useRef(null);

    const location = useLocation();

    // Auth Data
    const TOKEN = localStorage.getItem("librarian_token");
    const userStr = localStorage.getItem("librarian_user");
    let MY_ID = null;
    try { if (userStr) MY_ID = JSON.parse(userStr).id; } catch (e) { console.error(e); }

    // ================= LOGIC HANDLERS =================

    const handleMarkAsRead = async (partnerId) => {
        try {
            await markMessagesAsRead(partnerId);
            setConversations(prev => prev.map(c =>
                c.id === partnerId ? { ...c, unreadCount: 0 } : c
            ));
        } catch (e) { console.error("Lỗi đánh dấu đã đọc:", e); }
    };

    // ================= EFFECT: SETUP SOCKET & LOAD LIST =================
    // 1. useEffect bắt ID từ Widget (Chèn mới)
    useEffect(() => {
        const targetId = location.state?.targetUserId;
        if (targetId) {
            console.log("🎯 Chuyển hướng chat tới:", targetId);
            setSelectedUser(targetId);
            window.history.replaceState({}, document.title);
        }
    }, [location.state]);

    // 2. useEffect Socket & List (Cái bạn đã có)
    useEffect(() => {
        if (!MY_ID || !TOKEN) return;
        setupWebSocket();
        loadConversations();
        return () => { if (stompClientRef.current) stompClientRef.current.deactivate(); };
    }, [MY_ID]);

    // 3. useEffect Load tin nhắn khi selectedUser thay đổi (Cái bạn đã có)
    useEffect(() => {
        selectedUserRef.current = selectedUser;
        if (selectedUser) {
            setPage(0);
            setHasMore(true);
            setMessages([]);
            loadHistory(selectedUser, 0);
            handleMarkAsRead(selectedUser);
        }
    }, [selectedUser]);

    // ================= LOGIC CUỘN THÔNG MINH =================
    useLayoutEffect(() => {
        const container = messagesContainerRef.current;
        if (selectedUser && messages.length === 0 && !loadingMore) {
            setIsReady(true);
            setIsScrolling(false);
            setProcessing(false);
            return;
        }
        if (!container || messages.length === 0 || !selectedUser || page !== 0) return;

        const executeScrollAndShow = (source) => {
            const performScroll = () => {
                if (!container) return;
                container.scrollTop = container.scrollHeight;
                setIsReady(true);
                setIsScrolling(false);
                setProcessing(false);
                requestAnimationFrame(() => {
                    if (container) container.scrollTop = container.scrollHeight;
                });
            };
            setTimeout(performScroll, 30);
        };

        const images = container.querySelectorAll('.message-image');
        if (images.length === 0) {
            executeScrollAndShow("Không có ảnh");
        } else {
            let loadedCount = 0;
            const checkAllLoaded = () => {
                loadedCount++;
                if (loadedCount === images.length) executeScrollAndShow("Ảnh đã tải");
            };
            images.forEach(img => {
                if (img.complete) checkAllLoaded();
                else {
                    img.addEventListener('load', checkAllLoaded);
                    img.addEventListener('error', checkAllLoaded);
                }
            });
            const timer = setTimeout(() => {
                if (processing) executeScrollAndShow("Fallback");
            }, 800);
            return () => clearTimeout(timer);
        }
    }, [messages, page, selectedUser]);

    // ================= WEBSOCKET =================
    const setupWebSocket = () => {
        if (stompClientRef.current && stompClientRef.current.connected) return;
        const client = new Client({
            webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
            connectHeaders: { Authorization: `Bearer ${TOKEN}` },
            onConnect: () => {
                client.subscribe(`/topic/chat/${MY_ID}`, (message) => {
                    const receivedMsg = JSON.parse(message.body);
                    const isMyMsg = receivedMsg.senderId === MY_ID;
                    const partnerId = isMyMsg ? receivedMsg.receiverId : receivedMsg.senderId;

                    setMessages((prev) => {
                        if (partnerId === selectedUserRef.current) {
                            if (!isMyMsg) markMessagesAsRead(partnerId).catch(e => console.error(e));
                            requestAnimationFrame(() => {
                                if (messagesContainerRef.current) {
                                    messagesContainerRef.current.scrollTo({
                                        top: messagesContainerRef.current.scrollHeight,
                                        behavior: 'smooth'
                                    });
                                }
                            });
                            return [...prev, receivedMsg];
                        }
                        return prev;
                    });
                });

                client.subscribe(`/topic/chat/seen/${MY_ID}`, (notification) => {
                    const seenData = JSON.parse(notification.body);
                    const { partnerId } = seenData;
                    setMessages(prev => prev.map(msg =>
                        (msg.senderId === MY_ID && msg.receiverId === partnerId && !msg.isRead)
                            ? { ...msg, isRead: true } : msg
                    ));
                });

                // 🔔 Subscribe để nhận thông báo escalation từ AI
                client.subscribe('/topic/escalate', (message) => {
                    const escalateData = JSON.parse(message.body);
                    console.log('🔔 Escalation received:', escalateData);
                    setEscalateToastData(escalateData);
                    setShowEscalateToast(true);
                    // Refresh danh sách waiting
                    loadWaitingConversations();
                    // Auto-hide toast after 5 seconds
                    setTimeout(() => setShowEscalateToast(false), 5000);
                });
            }
        });
        client.activate();
        stompClientRef.current = client;
    };

    // Load danh sách conversations đang chờ
    const loadWaitingConversations = async () => {
        try {
            const res = await getWaitingConversations();
            setWaitingConversations(res.data || []);
        } catch (e) { console.error('Error loading waiting conversations:', e); }
    };

    // Load danh sách conversations đang active
    const loadActiveConversations = async () => {
        try {
            const res = await getActiveConversations();
            setActiveConversations(res.data || []);
        } catch (e) { console.error('Error loading active conversations:', e); }
    };

    // Load conversations khi component mount
    useEffect(() => {
        if (MY_ID && TOKEN) {
            loadWaitingConversations();
            loadActiveConversations();
        }
    }, [MY_ID]);

    // Librarian tiếp nhận conversation
    const handleTakeOver = async (conversationId) => {
        try {
            const res = await takeOverConversationAPI(conversationId);
            console.log('✅ Took over conversation:', res.data);
            // Refresh lists
            loadWaitingConversations();
            loadActiveConversations();
            // Set selected user to chat with
            if (res.data.studentId) {
                setSelectedUser(res.data.studentId);
                setSelectedConversation(res.data);
            }
        } catch (e) {
            console.error('Error taking over conversation:', e);
            alert('Không thể tiếp nhận cuộc hội thoại này!');
        }
    };

    const loadConversations = async () => {
        try {
            const res = await getConversations();
            const sorted = res.data.sort((a, b) => new Date(b.latestMessageTime || 0) - new Date(a.latestMessageTime || 0));
            setConversations(sorted);
        } catch (e) { }
    };

    const loadHistory = async (uid, pageNum) => {
        try {
            const res = await getChatHistory(uid, pageNum, 20);
            const newMessages = res.data.content;
            if (pageNum === 0) {
                setMessages(newMessages.reverse());
            } else {
                const container = messagesContainerRef.current;
                const oldScrollHeight = container?.scrollHeight || 0;
                const oldScrollTop = container?.scrollTop || 0;
                setMessages(prev => [...newMessages.reverse(), ...prev]);
                requestAnimationFrame(() => {
                    if (container) container.scrollTop = container.scrollHeight - oldScrollHeight + oldScrollTop;
                });
            }
            setHasMore(pageNum < res.data.totalPages - 1);
            setLoadingMore(false);
        } catch (error) {
            setLoadingMore(false); setIsReady(true); setIsScrolling(false);
        }
    };

    const handleSearch = async (keyword) => {
        if (!keyword.trim()) { setSearchResults([]); setHighlightKeyword(""); return; }
        setIsSearching(true);
        setHighlightKeyword(keyword);
        try {
            const res = await searchMessages(selectedUser, keyword);
            setSearchResults(res.data);
        } catch (error) { console.error(error); }
        finally { setIsSearching(false); }
    };

    const handleResultClick = async (msgId) => {
        const existingMsg = messages.find(m => m.id === msgId);
        if (existingMsg) scrollToMessage(msgId);
        else {
            try {
                const res = await findMessagePage(selectedUser, msgId);
                const targetPage = res.data;
                const historyRes = await getChatHistory(selectedUser, targetPage, 20);
                setMessages(historyRes.data.content.reverse());
                setPage(targetPage);
                setHasMore(targetPage < historyRes.data.totalPages - 1);
                setTimeout(() => scrollToMessage(msgId), 500);
            } catch (error) { alert("Không thể tải tin nhắn."); }
        }
    };

    const scrollToMessage = (msgId) => {
        const element = document.getElementById(`msg-${msgId}`);
        if (element) {
            element.scrollIntoView({ behavior: 'smooth', block: 'center' });
            element.classList.add('highlight-message');
            setTimeout(() => element.classList.remove('highlight-message'), 2500);
        }
    };

    const handleScroll = (e) => {
        const { scrollTop } = e.target;
        if (scrollTop === 0 && hasMore && !loadingMore) {
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

    // ================= CẬP NHẬT HÀM UPLOAD FILE ĐA NĂNG =================
    const handleFileUpload = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        setIsUploading(true);
        try {
            const isImage = file.type.startsWith('image/');
            let res;

            // 1. Tự động chọn API upload ảnh hoặc tài liệu
            if (isImage) {
                res = await uploadFile(file);
            } else {
                res = await uploadDocument(file);
            }

            // 2. Nhận dữ liệu (đã xử lý parse JSON từ axios)
            const data = res.data;
            const finalData = typeof data === 'string' ? JSON.parse(data) : data;

            // 3. Gửi qua WebSocket
            if (finalData && finalData.url) {
                sendToWebSocket(file.name, finalData.url, finalData.type || (isImage ? "IMAGE" : "FILE"));
            }
        } catch (error) {
            console.error("Lỗi upload:", error);
            alert("Upload thất bại!");
        } finally {
            setIsUploading(false);
            e.target.value = ""; // Reset để chọn lại cùng 1 file nếu cần
        }
    };

    const sendToWebSocket = (content, attachmentUrl, type) => {
        if (stompClientRef.current?.connected) {
            const chatMessage = { senderId: MY_ID, receiverId: selectedUser, content, attachmentUrl, type };
            stompClientRef.current.publish({ destination: '/app/chat', body: JSON.stringify(chatMessage) });
        }
    };

    const currentPartnerInfo = conversations.find(c => c.id === selectedUser);

    return (
        <div className="chat-container">
            {/* 🔔 Toast thông báo escalation */}
            {showEscalateToast && escalateToastData && (
                <div className="escalate-toast">
                    <span>🔔</span>
                    <div>
                        <strong>Có sinh viên cần hỗ trợ!</strong>
                        <p>{escalateToastData.studentName || 'Sinh viên'} đang chờ thủ thư.</p>
                    </div>
                    <button onClick={() => setShowEscalateToast(false)}>✕</button>
                </div>
            )}

            <div className="chat-sidebar">
                <div className="sidebar-header">Đoạn chat</div>

                {/* Tabs cho escalation */}
                <div className="escalation-tabs">
                    <button
                        className={`tab-btn ${activeTab === 'all' ? 'active' : ''}`}
                        onClick={() => setActiveTab('all')}
                    >
                        Tất cả
                    </button>
                    <button
                        className={`tab-btn ${activeTab === 'waiting' ? 'active' : ''}`}
                        onClick={() => setActiveTab('waiting')}
                    >
                        Chờ xử lý
                        {waitingConversations.length > 0 && (
                            <span className="waiting-badge">{waitingConversations.length}</span>
                        )}
                    </button>
                    <button
                        className={`tab-btn ${activeTab === 'active' ? 'active' : ''}`}
                        onClick={() => setActiveTab('active')}
                    >
                        Đang chat
                    </button>
                </div>

                <div className="conversation-list">
                    {/* Hiển thị waiting conversations nếu tab = waiting */}
                    {activeTab === 'waiting' && waitingConversations.map((conv) => (
                        <div
                            key={conv.id}
                            className="conversation-item waiting-item"
                            onClick={() => handleTakeOver(conv.id)}
                        >
                            <div className="user-avatar-placeholder waiting-avatar">
                                {conv.studentName?.charAt(0).toUpperCase() || '?'}
                            </div>
                            <div style={{ flex: 1, overflow: 'hidden' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <div style={{ fontWeight: 600 }}>{conv.studentName || 'Sinh viên'}</div>
                                    <span className="waiting-badge-mini">⏳ Chờ</span>
                                </div>
                                <div className="conv-subtext">{conv.escalationReason || 'Cần hỗ trợ'}</div>
                            </div>
                        </div>
                    ))}

                    {/* Hiển thị active conversations nếu tab = active */}
                    {activeTab === 'active' && activeConversations.map((conv) => (
                        <div
                            key={conv.id}
                            className={`conversation-item ${selectedUser === conv.studentId ? 'active' : ''}`}
                            onClick={() => setSelectedUser(conv.studentId)}
                        >
                            <div className="user-avatar-placeholder">
                                {conv.studentName?.charAt(0).toUpperCase() || '?'}
                            </div>
                            <div style={{ flex: 1, overflow: 'hidden' }}>
                                <div style={{ fontWeight: 600 }}>{conv.studentName || 'Sinh viên'}</div>
                                <div className="conv-subtext">{conv.studentEmail}</div>
                            </div>
                        </div>
                    ))}

                    {/* Hiển thị tất cả conversations nếu tab = all */}
                    {activeTab === 'all' && conversations.map((partner) => (
                        <div
                            key={partner.id}
                            className={`conversation-item ${selectedUser === partner.id ? 'active' : ''}`}
                            onClick={() => setSelectedUser(partner.id)}
                        >
                            <div className="user-avatar-placeholder">{partner.fullName?.charAt(0).toUpperCase()}</div>
                            <div style={{ flex: 1, overflow: 'hidden' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <div style={{ fontWeight: 600 }}>{partner.fullName}</div>
                                    {partner.unreadCount > 0 && selectedUser !== partner.id && (
                                        <span className="unread-badge-mini">{partner.unreadCount}</span>
                                    )}
                                </div>
                                <div className="conv-subtext">{partner.email}</div>
                            </div>
                        </div>
                    ))}
                </div>
            </div>


            <div className="chat-main">
                <div className="chat-header">
                    <div style={{ fontWeight: 'bold' }}>
                        {selectedUser && currentPartnerInfo ? `${currentPartnerInfo.fullName}` : "Tin nhắn"}
                    </div>
                    {selectedUser && (
                        <button className="btn-header-icon" onClick={() => setShowRightSidebar(!showRightSidebar)}>ⓘ</button>
                    )}
                </div>

                <div
                    className="chat-messages-area"
                    ref={messagesContainerRef}
                    onScroll={handleScroll}
                    style={{
                        opacity: (isReady && !isScrolling) ? 1 : 0,
                        transition: 'opacity 0.15s ease-in-out'
                    }}
                >
                    {loadingMore && <div className="loading-center">⏳</div>}
                    {!selectedUser ? (
                        <div className="empty-state"><h3>Chọn một cuộc trò chuyện 👋</h3></div>
                    ) : (
                        messages.map((msg, idx) => {
                            const isMyMsg = msg.senderId === MY_ID;
                            const isLastMsg = idx === messages.length - 1;
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
                                                        {highlightText(msg.content, highlightKeyword)}
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
                                        <div className="seen-status-container">
                                            <span className={msg.isRead ? "seen-text" : "sent-text"}>
                                                {msg.isRead ? `${currentPartnerInfo?.fullName} đã xem` : "Đã gửi"}
                                            </span>
                                        </div>
                                    )}
                                </React.Fragment>
                            );
                        })
                    )}
                </div>

                {selectedUser && (
                    <div className="chat-input-area">
                        {/* INPUT FILE ĐA NĂNG */}
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

                        {/* NÚT KẸP GIẤY (DÙNG CHUNG INPUT) */}
                        <button
                            className="btn-icon"
                            onClick={() => fileInputRef.current.click()}
                            disabled={isUploading}
                            title="Đính kèm tài liệu"
                        >
                            {isUploading ? (
                                <span className="spinner-mini"></span>
                            ) : (
                                <img src={attach} alt="attach" className="icon-svg" />
                            )}
                        </button>

                        <button
                            className="btn-icon"
                            onClick={() => imageInputRef.current.click()}
                            disabled={isUploading}
                            title="Gửi hình ảnh"
                        >
                            {isUploading ? (
                                <span className="spinner-mini"></span>
                            ) : (
                                <img src={image} alt="image" className="icon-svg" />
                            )}
                        </button>

                        <input className="input-field" value={input}
                            onChange={(e) => setInput(e.target.value)}
                            onKeyDown={(e) => e.key === 'Enter' && handleSendMessage()}
                            placeholder="Nhập tin nhắn..."
                        />
                        <button className="btn-send" onClick={handleSendMessage}>Gửi</button>
                    </div>
                )}
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

            <ChatSidebarRight
                isOpen={showRightSidebar && selectedUser}
                onClose={() => setShowRightSidebar(false)}
                currentPartner={currentPartnerInfo}
                myId={MY_ID}
                onSearch={handleSearch}
                searchResults={searchResults}
                isSearching={isSearching}
                onResultClick={handleResultClick}
            />
        </div>
    );
};

export default ChatManagement;