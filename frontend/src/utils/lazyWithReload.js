import React from "react";

const RELOAD_FLAG_KEY = "slib:lazy-chunk-reload";

const isChunkLoadError = (error) => {
    const message = error?.message || "";

    return [
        "Failed to fetch dynamically imported module",
        "Importing a module script failed",
        "ChunkLoadError",
        "Loading chunk",
        "Failed to load module script",
    ].some((fragment) => message.includes(fragment));
};

const hasRecentReloadAttempt = () => {
    const lastAttempt = sessionStorage.getItem(RELOAD_FLAG_KEY);

    if (!lastAttempt) {
        return false;
    }

    return Date.now() - Number(lastAttempt) < 15_000;
};

const markReloadAttempt = () => {
    sessionStorage.setItem(RELOAD_FLAG_KEY, String(Date.now()));
};

export const clearLazyReloadFlag = () => {
    sessionStorage.removeItem(RELOAD_FLAG_KEY);
};

export const lazyWithReload = (factory) =>
    React.lazy(async () => {
        try {
            const module = await factory();
            clearLazyReloadFlag();
            return module;
        } catch (error) {
            if (typeof window !== "undefined" && isChunkLoadError(error) && !hasRecentReloadAttempt()) {
                markReloadAttempt();
                window.location.reload();

                return new Promise(() => {});
            }

            throw error;
        }
    });
