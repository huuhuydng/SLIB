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
    takeOverConversation as takeOverConversationAPI,
    resolveConversation as resolveConversationAPI,
    sendConversationMessage,
    getConversationMessages
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
    const conversationSubscriptionRef = useRef(null); // For /topic/conversation/{id}
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
    // CHÚ Ý: Nếu có selectedConversation thì KHÔNG load từ user-to-user history
    // vì conversation messages sẽ được load bởi useEffect riêng
    useEffect(() => {
        selectedUserRef.current = selectedUser;
        if (selectedUser && !selectedConversation) {
            // Chỉ load user-to-user chat khi KHÔNG có conversation
            setPage(0);
            setHasMore(true);
            setMessages([]);
            loadHistory(selectedUser, 0);
            handleMarkAsRead(selectedUser);
        }
    }, [selectedUser, selectedConversation]);

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

    // Subscribe to conversation topic for real-time messages
    useEffect(() => {
        // Find conversation ID based on selectedUser from activeConversations
        const activeConv = activeConversations.find(c => c.studentId === selectedUser);
        const conversationId = selectedConversation?.id || activeConv?.id;

        if (!conversationId || !stompClientRef.current?.connected) return;

        // Unsubscribe from previous conversation
        if (conversationSubscriptionRef.current) {
            conversationSubscriptionRef.current.unsubscribe();
            conversationSubscriptionRef.current = null;
        }

        // Subscribe to new conversation topic
        console.log('[WS] Subscribing to conversation:', conversationId);
        conversationSubscriptionRef.current = stompClientRef.current.subscribe(
            `/topic/conversation/${conversationId}`,
            (message) => {
                const newMsg = JSON.parse(message.body);
                console.log('[WS] Received message from conversation:', newMsg);

                // Add message to list if not duplicate
                setMessages(prev => {
                    if (prev.some(m => m.id === newMsg.id)) return prev;
                    return [...prev, newMsg];
                });

                // Scroll to bottom
                requestAnimationFrame(() => {
                    if (messagesContainerRef.current) {
                        messagesContainerRef.current.scrollTo({
                            top: messagesContainerRef.current.scrollHeight,
                            behavior: 'smooth'
                        });
                    }
                });
            }
        );

        return () => {
            if (conversationSubscriptionRef.current) {
                conversationSubscriptionRef.current.unsubscribe();
                conversationSubscriptionRef.current = null;
            }
        };
    }, [selectedConversation?.id, selectedUser, activeConversations]);

    // Load messages từ conversation khi selectedConversation thay đổi
    // Quan trọng: Đây là nơi load tin nhắn của sinh viên từ conversation API
    useEffect(() => {
        const loadConversationMessages = async () => {
            if (!selectedConversation?.id) return;

            console.log('[Chat] Loading messages for conversation:', selectedConversation.id);
            try {
                const res = await getConversationMessages(selectedConversation.id);
                console.log('[Chat] Loaded', res.data?.length || 0, 'messages from conversation');
                if (res.data) {
                    setMessages(res.data);
                    setPage(0);
                    setHasMore(false); // Conversation messages không phân trang
                }
            } catch (e) {
                console.error('Error loading conversation messages:', e);
            }
        };

        loadConversationMessages();
    }, [selectedConversation?.id]);

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

    // Kết thúc cuộc hội thoại
    const handleEndConversation = async () => {
        if (!selectedConversation?.id) {
            // Tìm conversation từ activeConversations theo selectedUser
            const activeConv = activeConversations.find(c => c.studentId === selectedUser);
            if (!activeConv?.id) {
                alert('Không tìm thấy cuộc hội thoại để kết thúc!');
                return;
            }
            try {
                if (!confirm('Bạn có chắc chắn muốn kết thúc cuộc hội thoại này?')) return;
                await resolveConversationAPI(activeConv.id);
                console.log('✅ Ended conversation:', activeConv.id);
                // Clear selection and refresh
                setSelectedUser(null);
                setSelectedConversation(null);
                setMessages([]);
                loadWaitingConversations();
                loadActiveConversations();
                alert('Đã kết thúc cuộc hội thoại!');
            } catch (e) {
                console.error('Error ending conversation:', e);
                alert('Không thể kết thúc cuộc hội thoại!');
            }
            return;
        }

        try {
            if (!confirm('Bạn có chắc chắn muốn kết thúc cuộc hội thoại này?')) return;
            await resolveConversationAPI(selectedConversation.id);
            console.log('✅ Ended conversation:', selectedConversation.id);
            // Clear selection and refresh
            setSelectedUser(null);
            setSelectedConversation(null);
            setMessages([]);
            loadWaitingConversations();
            loadActiveConversations();
            alert('Đã kết thúc cuộc hội thoại!');
        } catch (e) {
            console.error('Error ending conversation:', e);
            alert('Không thể kết thúc cuộc hội thoại!');
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

    const sendToWebSocket = async (content, attachmentUrl, type) => {
        // Nếu đang chat trong conversation (escalation), gửi qua API để lưu vào conversation
        if (selectedConversation?.id) {
            try {
                console.log('📤 Sending to conversation:', selectedConversation.id);
                const res = await sendConversationMessage(selectedConversation.id, content, 'LIBRARIAN');
                if (res.data) {
                    // Thêm tin nhắn vào UI ngay lập tức (check duplicate vì WebSocket cũng có thể nhận)
                    setMessages(prev => {
                        if (prev.some(m => m.id === res.data.id)) return prev;
                        return [...prev, res.data];
                    });
                }
            } catch (err) {
                console.error('Lỗi gửi tin nhắn:', err);
            }
            return;
        }

        // Fallback: user-to-user chat qua STOMP
        if (stompClientRef.current?.connected) {
            const chatMessage = {
                senderId: MY_ID,
                receiverId: selectedUser,
                content,
                attachmentUrl,
                type,
                senderType: 'LIBRARIAN'
            };
            stompClientRef.current.publish({ destination: '/app/chat', body: JSON.stringify(chatMessage) });
            console.log('📤 Sent via STOMP:', chatMessage);
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
                        Lịch sử
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
                            onClick={() => {
                                // Quan trọng: set conversation TRƯỚC user để tránh race condition
                                setSelectedConversation(conv);
                                setSelectedUser(conv.studentId);
                            }}
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
                    <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                        {selectedUser && activeConversations.some(c => c.studentId === selectedUser) && (
                            <button
                                className="btn-end-chat"
                                onClick={handleEndConversation}
                                title="Kết thúc cuộc hội thoại"
                                style={{
                                    padding: '6px 12px',
                                    backgroundColor: '#dc3545',
                                    color: 'white',
                                    border: 'none',
                                    borderRadius: '6px',
                                    cursor: 'pointer',
                                    fontSize: '13px',
                                    fontWeight: '500'
                                }}
                            >
                                Kết thúc
                            </button>
                        )}
                        {selectedUser && (
                            <button className="btn-header-icon" onClick={() => setShowRightSidebar(!showRightSidebar)}>ⓘ</button>
                        )}
                    </div>
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
                            // Xác định vị trí tin nhắn dựa trên senderType
                            // STUDENT: bên trái (received), AI/LIBRARIAN: bên phải (sent)
                            const senderType = msg.senderType || 'STUDENT';
                            const isFromStudent = senderType === 'STUDENT';
                            const isFromAI = senderType === 'AI';
                            const isFromLibrarian = senderType === 'LIBRARIAN';

                            // Sinh viên bên trái, AI/Thủ thư bên phải
                            const messagePosition = isFromStudent ? 'received' : 'sent';
                            const isLastMsg = idx === messages.length - 1;

                            // Xác định avatar
                            const getAvatar = () => {
                                if (isFromStudent) {
                                    // Avatar sinh viên: chữ cái đầu tên hoặc emoji
                                    const studentName = msg.senderName || currentPartnerInfo?.fullName || '';
                                    return studentName.charAt(0).toUpperCase() || '👤';
                                } else if (isFromAI) {
                                    return '🤖';
                                } else {
                                    return '👨‍💼'; // Thủ thư
                                }
                            };

                            return (
                                <React.Fragment key={msg.id || idx}>
                                    {isDifferentDay(msg, idx > 0 ? messages[idx - 1] : null) && (
                                        <div className="date-separator"><span>{getDateLabel(msg.createdAt)}</span></div>
                                    )}

                                    <div id={`msg-${msg.id}`} className={`message-row ${messagePosition}`}>
                                        {/* Avatar - chỉ hiện cho tin nhắn bên trái (sinh viên) */}
                                        {isFromStudent && (
                                            <div className="message-avatar student-avatar">
                                                {getAvatar()}
                                            </div>
                                        )}

                                        {/* Container bọc ngang: Cho phép bubble và giờ nằm cạnh nhau */}
                                        <div className="message-wrapper-horizontal">
                                            <div className={`message-bubble ${isFromAI ? 'ai-bubble' : ''}`}>
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

                                        {/* Avatar - chỉ hiện cho tin nhắn bên phải (AI/Thủ thư) */}
                                        {!isFromStudent && (
                                            <div className={`message-avatar ${isFromAI ? 'ai-avatar' : 'librarian-avatar'}`}>
                                                {getAvatar()}
                                            </div>
                                        )}
                                    </div>

                                    {isFromLibrarian && isLastMsg && (
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

                {/* Ẩn input khi ở tab Lịch sử (all) - chỉ cho xem, không cho nhắn */}
                {selectedUser && activeTab !== 'all' && (
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