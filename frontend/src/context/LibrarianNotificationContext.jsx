import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import websocketService from '../services/shared/websocketService';
import { ClipboardCheck } from 'lucide-react';
import { API_BASE_URL } from '../config/apiConfig';

const LibrarianNotificationContext = createContext(null);

export function useLibrarianNotification() {
    const context = useContext(LibrarianNotificationContext);
    if (!context) {
        throw new Error('useLibrarianNotification must be used within LibrarianNotificationProvider');
    }
    return context;
}

// API endpoints map cho từng category
const PENDING_API_MAP = {
    SUPPORT_REQUEST: `${API_BASE_URL}/slib/support-requests`,
    COMPLAINT: `${API_BASE_URL}/slib/complaints?status=PENDING`,
    FEEDBACK: `${API_BASE_URL}/slib/feedbacks?status=NEW`,
    SEAT_STATUS_REPORT: `${API_BASE_URL}/slib/seat-status-reports`,
    CHAT: `${API_BASE_URL}/slib/chat/conversations/waiting`,
    VIOLATION: `${API_BASE_URL}/slib/violation-reports?status=PENDING`,
};

const ACTIVE_ITEM_FILTERS = {
    SUPPORT_REQUEST: (item) => ['PENDING', 'IN_PROGRESS'].includes(item?.status),
    COMPLAINT: (item) => item?.status === 'PENDING',
    FEEDBACK: (item) => item?.status === 'NEW',
    SEAT_STATUS_REPORT: (item) => ['PENDING', 'VERIFIED'].includes(item?.status),
    CHAT: () => true,
    VIOLATION: (item) => item?.status === 'PENDING',
};

const NOTIF_ICON_MAP = {
    SUPPORT_REQUEST: { icon: null, cls: 'support', label: 'Yêu cầu hỗ trợ' },
    COMPLAINT: { icon: null, cls: 'complaint', label: 'Khiếu nại' },
    FEEDBACK: { icon: null, cls: 'feedback', label: 'Phản hồi' },
    SEAT_STATUS_REPORT: { icon: ClipboardCheck, cls: 'feedback', label: 'Tình trạng ghế' },
    CHAT: { icon: null, cls: 'chat', label: 'Trò chuyện' },
    VIOLATION: { icon: null, cls: 'violation', label: 'Vi phạm' },
};

const resolveNotificationSource = (payload) => payload?.referenceType || payload?.notificationType || null;

export function LibrarianNotificationProvider({ children }) {
    const [pendingCounts, setPendingCounts] = useState({
        supportRequests: 0,
        complaints: 0,
        feedbacks: 0,
        seatStatusReports: 0,
        chats: 0,
        violations: 0,
        total: 0,
    });
    const [notifications, setNotifications] = useState([]);
    const [userNotifications, setUserNotifications] = useState([]);
    const [unreadNotificationCount, setUnreadNotificationCount] = useState(0);
    const [pendingItems, setPendingItems] = useState({
        SUPPORT_REQUEST: null,
        COMPLAINT: null,
        FEEDBACK: null,
        SEAT_STATUS_REPORT: null,
        CHAT: null,
        VIOLATION: null,
    });
    const [chatToast, setChatToast] = useState(null);
    const chatToastTimerRef = useRef(null);
    const mountedRef = useRef(true);

    // Danh sach tin nhan moi tu student (cho Header notification)
    const [chatMessages, setChatMessages] = useState([]);
    const [unreadChatCount, setUnreadChatCount] = useState(0);
    const [unreadChatConversationCount, setUnreadChatConversationCount] = useState(0);

    const getToken = useCallback(() => {
        return sessionStorage.getItem('librarian_token') || localStorage.getItem('librarian_token');
    }, []);

    const getCurrentUserId = useCallback(() => {
        try {
            const raw = sessionStorage.getItem('librarian_user') || localStorage.getItem('librarian_user');
            if (!raw) return null;
            const parsed = JSON.parse(raw);
            return parsed?.id || null;
        } catch {
            return null;
        }
    }, []);

    // Fetch pending counts from REST API
    const fetchPendingCounts = useCallback(async () => {
        try {
            const token = getToken();
            if (!token) return;
            const res = await fetch(`${API_BASE_URL}/slib/librarian/pending-counts`, {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
            });
            if (res.ok && mountedRef.current) {
                const data = await res.json();
                setPendingCounts(data);
            }
        } catch (err) {
            console.warn('[LibrarianNotification] Fetch error:', err);
        }
    }, [getToken]);

    // Fetch unread chat message count
    const fetchUnreadChatCount = useCallback(async () => {
        try {
            const token = getToken();
            if (!token) return;
            const res = await fetch(`${API_BASE_URL}/slib/librarian/unread-chat-count`, {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
            });
            if (res.ok && mountedRef.current) {
                const data = await res.json();
                setUnreadChatCount(data.unreadMessages ?? data.count ?? 0);
                setUnreadChatConversationCount(data.unreadConversations ?? 0);
            }
        } catch (err) {
            console.warn('[LibrarianNotification] Fetch unread chat error:', err);
        }
    }, [getToken]);

    const fetchUserNotifications = useCallback(async (limit = 20) => {
        try {
            const token = getToken();
            const userId = getCurrentUserId();
            if (!token || !userId) return [];

            const res = await fetch(`${API_BASE_URL}/slib/notifications/user/${userId}?limit=${limit}`, {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
            });

            if (res.ok && mountedRef.current) {
                const data = await res.json();
                setUserNotifications(Array.isArray(data) ? data : []);
                return Array.isArray(data) ? data : [];
            }
        } catch (err) {
            console.warn('[LibrarianNotification] Fetch user notifications error:', err);
        }
        return [];
    }, [getCurrentUserId, getToken]);

    const fetchUnreadNotificationCount = useCallback(async () => {
        try {
            const token = getToken();
            const userId = getCurrentUserId();
            if (!token || !userId) return;

            const res = await fetch(`${API_BASE_URL}/slib/notifications/unread-count/${userId}`, {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
            });

            if (res.ok && mountedRef.current) {
                const data = await res.json();
                setUnreadNotificationCount(data.count ?? 0);
            }
        } catch (err) {
            console.warn('[LibrarianNotification] Fetch unread notification count error:', err);
        }
    }, [getCurrentUserId, getToken]);

    const markNotificationAsRead = useCallback(async (notificationId) => {
        try {
            const token = getToken();
            const userId = getCurrentUserId();
            if (!token || !userId || !notificationId) return false;

            const res = await fetch(`${API_BASE_URL}/slib/notifications/mark-read/${notificationId}?userId=${userId}`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
            });

            if (!res.ok) return false;

            if (mountedRef.current) {
                setUserNotifications(prev => prev.map((item) => (
                    item.id === notificationId ? { ...item, isRead: true } : item
                )));
                setUnreadNotificationCount(prev => Math.max(0, prev - 1));
            }
            return true;
        } catch (err) {
            console.warn('[LibrarianNotification] Mark notification as read error:', err);
            return false;
        }
    }, [getCurrentUserId, getToken]);

    const deleteNotification = useCallback(async (notificationId) => {
        try {
            const token = getToken();
            const userId = getCurrentUserId();
            if (!token || !userId || !notificationId) return false;

            const target = userNotifications.find((item) => item.id === notificationId);
            const wasUnread = target?.isRead === false;

            const res = await fetch(`${API_BASE_URL}/slib/notifications/${notificationId}?userId=${userId}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
            });

            if (!res.ok) return false;

            if (mountedRef.current) {
                setUserNotifications(prev => prev.filter((item) => item.id !== notificationId));
                if (wasUnread) {
                    setUnreadNotificationCount(prev => Math.max(0, prev - 1));
                }
            }
            return true;
        } catch (err) {
            console.warn('[LibrarianNotification] Delete notification error:', err);
            return false;
        }
    }, [getCurrentUserId, getToken, userNotifications]);

    // Fetch danh sách chi tiết pending items theo category
    const fetchPendingItems = useCallback(async (category) => {
        try {
            const token = getToken();
            if (!token) return [];
            const url = PENDING_API_MAP[category];
            if (!url) return [];

            const res = await fetch(url, {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
            });
            if (res.ok && mountedRef.current) {
                const data = await res.json();
                const filterFn = ACTIVE_ITEM_FILTERS[category];
                const normalized = Array.isArray(data) && filterFn ? data.filter(filterFn) : data;
                setPendingItems(prev => ({ ...prev, [category]: normalized }));
                return normalized;
            }
        } catch (err) {
            console.warn(`[LibrarianNotification] Fetch ${category} items error:`, err);
        }
        return [];
    }, [getToken]);

    // Clear cached items khi counts thay đổi (để force re-fetch)
    const clearPendingItems = useCallback(() => {
        setPendingItems({
            SUPPORT_REQUEST: null,
            COMPLAINT: null,
            FEEDBACK: null,
            SEAT_STATUS_REPORT: null,
            CHAT: null,
            VIOLATION: null,
        });
    }, []);

    // Clear chat messages khi user mo dropdown
    const clearChatMessages = useCallback(() => {
        setChatMessages([]);
        setUnreadChatCount(0);
        setUnreadChatConversationCount(0);
    }, []);

    // Fetch on mount
    useEffect(() => {
        mountedRef.current = true;
        fetchPendingCounts();
        fetchUnreadChatCount();
        fetchUserNotifications();
        fetchUnreadNotificationCount();
        const intervalId = window.setInterval(() => {
            fetchPendingCounts();
            fetchUnreadChatCount();
            fetchUserNotifications();
            fetchUnreadNotificationCount();
        }, 30000);

        const handleVisibilityChange = () => {
            if (document.visibilityState === 'visible') {
                fetchPendingCounts();
                fetchUnreadChatCount();
                fetchUserNotifications();
                fetchUnreadNotificationCount();
            }
        };

        const handleWindowFocus = () => {
            fetchPendingCounts();
            fetchUnreadChatCount();
            fetchUserNotifications();
            fetchUnreadNotificationCount();
        };

        document.addEventListener('visibilitychange', handleVisibilityChange);
        window.addEventListener('focus', handleWindowFocus);

        return () => {
            mountedRef.current = false;
            window.clearInterval(intervalId);
            document.removeEventListener('visibilitychange', handleVisibilityChange);
            window.removeEventListener('focus', handleWindowFocus);
        };
    }, [fetchPendingCounts, fetchUnreadChatCount, fetchUnreadNotificationCount, fetchUserNotifications]);

    // Subscribe to WebSocket - separate effect
    useEffect(() => {
        let unsubFn = null;

        const handleMessage = (data) => {
            if (data.type === 'PENDING_COUNTS_UPDATE' && data.counts) {
                setPendingCounts(data.counts);
                clearPendingItems(); // clear cache khi có update
                setNotifications((prev) => [{
                    id: Date.now(),
                    source: data.source,
                    action: data.action,
                    timestamp: data.timestamp,
                }, ...prev].slice(0, 20));
                fetchUserNotifications();
                fetchUnreadNotificationCount();
            } else if (data.type === 'CHAT_NEW_MESSAGE') {
                // Toast thông báo tin nhắn mới từ student
                setChatToast({
                    eventType: 'CHAT_NEW_MESSAGE',
                    senderName: data.senderName,
                    content: data.content,
                    conversationId: data.conversationId,
                });
                if (chatToastTimerRef.current) clearTimeout(chatToastTimerRef.current);
                chatToastTimerRef.current = setTimeout(() => setChatToast(null), 5000);

                // Them vao danh sach chat messages cho Header
                setChatMessages(prev => [{
                    id: Date.now(),
                    senderName: data.senderName,
                    content: data.content,
                    conversationId: data.conversationId,
                    timestamp: data.timestamp || new Date().toISOString(),
                }, ...prev].slice(0, 20));
                fetchUnreadChatCount();
                fetchUserNotifications();
                fetchUnreadNotificationCount();

                // Also refresh pending counts
                fetchPendingCounts();
            } else if (data.type === 'CHAT_ENDED_BY_STUDENT') {
                setChatToast({
                    eventType: 'CHAT_ENDED_BY_STUDENT',
                    senderName: data.studentName,
                    content: 'đã kết thúc cuộc trò chuyện với thủ thư',
                    conversationId: data.conversationId,
                });
                if (chatToastTimerRef.current) clearTimeout(chatToastTimerRef.current);
                chatToastTimerRef.current = setTimeout(() => setChatToast(null), 5000);
                fetchPendingCounts();
            }
        };

        const handleStoredNotification = (payload) => {
            if (!mountedRef.current || !payload) return;

            const source = resolveNotificationSource(payload);
            setUserNotifications((prev) => {
                const next = [payload, ...prev.filter((item) => item.id !== payload.id)];
                return next.slice(0, 20);
            });
            if (payload.isRead === false) {
                if (typeof payload.unreadCount === 'number') {
                    setUnreadNotificationCount(payload.unreadCount);
                } else {
                    setUnreadNotificationCount((prev) => prev + 1);
                }
            } else {
                fetchUnreadNotificationCount();
            }

            if (source === 'FEEDBACK') {
                setNotifications((prev) => [{
                    id: `stored-${payload.id}`,
                    source,
                    action: 'CREATED',
                    title: payload.title,
                    content: payload.content,
                    timestamp: payload.createdAt || new Date().toISOString(),
                    fromStoredNotification: true,
                }, ...prev].slice(0, 20));
            }
        };

        const trySubscribe = () => {
            const userId = getCurrentUserId();
            if (websocketService.isConnected()) {
                unsubFn = websocketService.subscribe('/topic/librarian-notifications', handleMessage);
                if (userId) {
                    const unsubStored = websocketService.subscribe(`/topic/notifications/${userId}`, handleStoredNotification);
                    const prevUnsub = unsubFn;
                    unsubFn = () => {
                        if (prevUnsub) prevUnsub();
                        unsubStored?.();
                    };
                }
            } else {
                // WebSocket not ready yet, try connecting
                websocketService.connect(
                    () => {
                        unsubFn = websocketService.subscribe('/topic/librarian-notifications', handleMessage);
                        if (userId) {
                            const unsubStored = websocketService.subscribe(`/topic/notifications/${userId}`, handleStoredNotification);
                            const prevUnsub = unsubFn;
                            unsubFn = () => {
                                if (prevUnsub) prevUnsub();
                                unsubStored?.();
                            };
                        }
                    },
                    (err) => console.error('[LibrarianNotification] WebSocket error:', err)
                );
            }
        };

        trySubscribe();

        return () => {
            if (unsubFn) unsubFn();
        };
    }, [clearPendingItems, fetchPendingCounts, fetchUnreadChatCount, fetchUnreadNotificationCount, fetchUserNotifications, getCurrentUserId]);

    const value = {
        pendingCounts,
        notifications,
        userNotifications,
        unreadNotificationCount,
        pendingItems,
        fetchPendingItems,
        refetch: fetchPendingCounts,
        fetchUserNotifications,
        markNotificationAsRead,
        deleteNotification,
        chatToast,
        setChatToast,
        chatMessages,
        unreadChatCount,
        unreadChatConversationCount,
        clearChatMessages,
        refreshUnreadChatCount: fetchUnreadChatCount,
    };

    return (
        <LibrarianNotificationContext.Provider value={value}>
            {children}
        </LibrarianNotificationContext.Provider>
    );
}
