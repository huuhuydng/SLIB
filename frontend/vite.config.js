import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  define: {
    'global': 'globalThis',
  },
  server: {
    allowedHosts: ['slibsystem.site', 'api.slibsystem.site', 'ai.slibsystem.site'],
    headers: {
      'Cross-Origin-Opener-Policy': 'same-origin-allow-popups',
      'Cross-Origin-Embedder-Policy': 'unsafe-none'
    },
    proxy: {
      '/slib': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/api/ai': {
        target: 'http://localhost:8001',
        changeOrigin: true,
        secure: false,
      },
      '/api/v1': {
        target: 'http://localhost:8001',
        changeOrigin: true,
        secure: false,
      },
      '/health': {
        target: 'http://localhost:8001',
        changeOrigin: true,
        secure: false,
      }
    }
  }
})
