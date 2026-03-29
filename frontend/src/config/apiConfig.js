/**
 * Centralized API base URL configuration.
 * Auto-detects production domain at runtime so builds with
 * VITE_API_BASE_URL=localhost still work when served from slibsystem.site.
 */
function resolveApiBaseUrl() {
  const envUrl = import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL;

  if (envUrl && !envUrl.includes('localhost')) {
    return envUrl;
  }

  const hostname = typeof window !== 'undefined' ? window.location.hostname : '';
  if (hostname.includes('slibsystem.site')) {
    return 'https://api.slibsystem.site';
  }

  return envUrl || 'http://localhost:8080';
}

function resolveWsBaseUrl() {
  const base = resolveApiBaseUrl();
  return base.replace(/^http/, 'ws');
}

export const API_BASE_URL = resolveApiBaseUrl();
export const WS_BASE_URL = resolveWsBaseUrl();

function resolveAiApiBaseUrl() {
  const envUrl = import.meta.env.VITE_AI_API_BASE_URL;

  if (envUrl) {
    return envUrl;
  }

  const hostname = typeof window !== 'undefined' ? window.location.hostname : '';
  if (hostname.includes('slibsystem.site')) {
    return 'https://ai.slibsystem.site';
  }

  return '';
}

export const AI_API_BASE_URL = resolveAiApiBaseUrl();
