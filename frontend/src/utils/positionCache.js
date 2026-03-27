/**
 * Position Cache Utility
 * Caches zone and factory positions in localStorage for instant rendering on page reload.
 * API data is source of truth after save; cache is for unsaved changes display.
 */

const CACHE_KEY = 'slib_position_cache';

/**
 * Get cached positions for all items
 */
export const getPositionCache = () => {
    try {
        const cached = localStorage.getItem(CACHE_KEY);
        return cached ? JSON.parse(cached) : { zones: {}, factories: {} };
    } catch (e) {
        console.warn('Failed to read position cache:', e);
        return { zones: {}, factories: {} };
    }
};

/**
 * Save zone position to cache
 */
export const cacheZonePosition = (zoneId, position) => {
    try {
        const cache = getPositionCache();
        cache.zones[zoneId] = {
            positionX: position.x,
            positionY: position.y,
            width: position.width,
            height: position.height,
            timestamp: Date.now(),
        };
        localStorage.setItem(CACHE_KEY, JSON.stringify(cache));
    } catch (e) {
        console.warn('Failed to cache zone position:', e);
    }
};

/**
 * Save factory position to cache
 */
export const cacheFactoryPosition = (factoryId, position) => {
    try {
        const cache = getPositionCache();
        cache.factories[factoryId] = {
            positionX: position.x,
            positionY: position.y,
            width: position.width,
            height: position.height,
            timestamp: Date.now(),
        };
        localStorage.setItem(CACHE_KEY, JSON.stringify(cache));
    } catch (e) {
        console.warn('Failed to cache factory position:', e);
    }
};

/**
 * Get cached position for a zone
 */
export const getCachedZonePosition = (zoneId) => {
    const cache = getPositionCache();
    return cache.zones[zoneId] || null;
};

/**
 * Get cached position for a factory
 */
export const getCachedFactoryPosition = (factoryId) => {
    const cache = getPositionCache();
    return cache.factories[factoryId] || null;
};

/**
 * Clear zone from cache (after save)
 */
export const clearZoneFromCache = (zoneId) => {
    try {
        const cache = getPositionCache();
        delete cache.zones[zoneId];
        localStorage.setItem(CACHE_KEY, JSON.stringify(cache));
    } catch (e) {
        console.warn('Failed to clear zone from cache:', e);
    }
};

/**
 * Clear factory from cache (after save)
 */
export const clearFactoryFromCache = (factoryId) => {
    try {
        const cache = getPositionCache();
        delete cache.factories[factoryId];
        localStorage.setItem(CACHE_KEY, JSON.stringify(cache));
    } catch (e) {
        console.warn('Failed to clear factory from cache:', e);
    }
};

/**
 * Clear all cached positions (after full save)
 */
export const clearAllPositionCache = () => {
    try {
        localStorage.removeItem(CACHE_KEY);
    } catch (e) {
        console.warn('Failed to clear position cache:', e);
    }
};

/**
 * Merge API zones with cached positions
 * Cache takes priority for unsaved changes
 */
export const mergeZonesWithCache = (apiZones) => {
    const cache = getPositionCache();
    return apiZones.map(zone => {
        const cached = cache.zones[zone.zoneId];
        if (cached) {
            return {
                ...zone,
                positionX: cached.positionX ?? zone.positionX,
                positionY: cached.positionY ?? zone.positionY,
                width: cached.width ?? zone.width,
                height: cached.height ?? zone.height,
            };
        }
        return zone;
    });
};

/**
 * Merge API factories with cached positions
 */
export const mergeFactoriesWithCache = (apiFactories) => {
    const cache = getPositionCache();
    return apiFactories.map(factory => {
        const cached = cache.factories[factory.factoryId];
        if (cached) {
            return {
                ...factory,
                positionX: cached.positionX ?? factory.positionX,
                positionY: cached.positionY ?? factory.positionY,
                width: cached.width ?? factory.width,
                height: cached.height ?? factory.height,
            };
        }
        return factory;
    });
};
