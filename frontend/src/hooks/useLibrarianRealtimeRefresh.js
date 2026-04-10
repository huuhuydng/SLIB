import { useEffect, useRef } from "react";
import websocketService from "../services/shared/websocketService";

export default function useLibrarianRealtimeRefresh({
    onRefresh,
    subscriptions = [],
    debounceMs = 700,
}) {
    const refreshRef = useRef(onRefresh);
    const timeoutRef = useRef(null);

    useEffect(() => {
        refreshRef.current = onRefresh;
    }, [onRefresh]);

    useEffect(() => {
        if (!Array.isArray(subscriptions) || subscriptions.length === 0) {
            return undefined;
        }

        const scheduleRefresh = () => {
            if (timeoutRef.current) {
                window.clearTimeout(timeoutRef.current);
            }

            timeoutRef.current = window.setTimeout(() => {
                refreshRef.current?.();
            }, debounceMs);
        };

        const subscribeAll = () => subscriptions
            .map(({ topic, shouldRefresh }) => websocketService.subscribe(topic, (message) => {
                if (!shouldRefresh || shouldRefresh(message)) {
                    scheduleRefresh();
                }
            }))
            .filter(Boolean);

        let unsubscribers = [];

        if (websocketService.isConnected()) {
            unsubscribers = subscribeAll();
        } else {
            websocketService.connect(
                () => {
                    unsubscribers = subscribeAll();
                },
                (error) => {
                    console.error("[RealtimeRefresh] WebSocket error:", error);
                }
            );
        }

        return () => {
            if (timeoutRef.current) {
                window.clearTimeout(timeoutRef.current);
            }
            unsubscribers.forEach((unsubscribe) => unsubscribe?.());
        };
    }, [debounceMs, subscriptions]);
}
