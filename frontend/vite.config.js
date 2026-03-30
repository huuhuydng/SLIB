import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  define: {
    'global': 'globalThis',
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return;
          }

          if (
            id.includes('@tiptap') ||
            id.includes('prosemirror')
          ) {
            return 'editor';
          }

          if (
            id.includes('xlsx') ||
            id.includes('jszip')
          ) {
            return 'spreadsheet';
          }

          if (
            id.includes('sockjs-client') ||
            id.includes('@stomp/stompjs')
          ) {
            return 'realtime';
          }

          if (
            id.includes('react') ||
            id.includes('scheduler')
          ) {
            return 'react-vendor';
          }
        },
      },
    },
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
