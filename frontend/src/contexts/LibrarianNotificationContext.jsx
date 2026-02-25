import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import websocketService from '../services/websocketService';

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
    SUPPORT_REQUEST: '/slib/support-requests?status=PENDING',
    COMPLAINT: '/slib/complaints?status=PENDING',
    FEEDBACK: '/slib/feedbacks?status=NEW',
    CHAT: '/slib/chat/conversations/waiting',
    VIOLATION: '/slib/violation-reports?status=PENDING',
};

export function LibrarianNotificationProvider({ children }) {
    const [pendingCounts, setPendingCounts] = useState({
        supportRequests: 0,
        complaints: 0,
        feedbacks: 0,
        chats: 0,
        violations: 0,
        total: 0,
    });
    const [notifications, setNotifications] = useState([]);
    const [pendingItems, setPendingItems] = useState({
        SUPPORT_REQUEST: null,
        COMPLAINT: null,
        FEEDBACK: null,
        CHAT: null,
        VIOLATION: null,
    });
    const mountedRef = useRef(true);

    const getToken = useCallback(() => {
        return localStorage.getItem('librarian_token') || sessionStorage.getItem('librarian_token');
    }, []);

    // Fetch pending counts from REST API
    const fetchPendingCounts = useCallback(async () => {
        try {
            const token = getToken();
            if (!token) return;
            const res = await fetch('/slib/librarian/pending-counts', {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
            });
            if (res.ok && mountedRef.current) {
                const data = await res.json();
                console.log('[LibrarianNotification] Fetched counts:', data);
                setPendingCounts(data);
            }
        } catch (err) {
            console.warn('[LibrarianNotification] Fetch error:', err);
        }
    }, [getToken]);

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
                setPendingItems(prev => ({ ...prev, [category]: data }));
                return data;
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
            CHAT: null,
            VIOLATION: null,
        });
    }, []);

    // Fetch on mount
    useEffect(() => {
        mountedRef.current = true;
        fetchPendingCounts();
        return () => { mountedRef.current = false; };
    }, [fetchPendingCounts]);

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
            }
        };

        const trySubscribe = () => {
            if (websocketService.isConnected()) {
                unsubFn = websocketService.subscribe('/topic/librarian-notifications', handleMessage);
                console.log('[LibrarianNotification] Subscribed to WebSocket topic');
            } else {
                // WebSocket not ready yet, try connecting
                websocketService.connect(
                    () => {
                        unsubFn = websocketService.subscribe('/topic/librarian-notifications', handleMessage);
                        console.log('[LibrarianNotification] Connected & subscribed');
                    },
                    (err) => console.error('[LibrarianNotification] WebSocket error:', err)
                );
            }
        };

        trySubscribe();

        return () => {
            if (unsubFn) unsubFn();
        };
    }, [clearPendingItems]);

    const value = {
        pendingCounts,
        notifications,
        pendingItems,
        fetchPendingItems,
        refetch: fetchPendingCounts,
    };

    return (
        <LibrarianNotificationContext.Provider value={value}>
            {children}
        </LibrarianNotificationContext.Provider>
    );
}
