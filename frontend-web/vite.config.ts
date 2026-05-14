/// <reference types="vitest" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    strictPort: false,
    proxy: {
      // Proxy /api during local dev to the Spring Cloud Gateway (port 8080)
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // STOMP/WebSocket upgrades transit the same proxy.
      '/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
      },
    },
  },
  preview: {
    port: 5173,
  },
  build: {
    // Vite default is 500 KB; we trim it to surface regressions earlier.
    chunkSizeWarningLimit: 600,
    rollupOptions: {
      output: {
        // Manual chunk splitting — keeps the initial bundle lean by isolating
        // heavy vendor surface into separately-cacheable chunks. Per-page chunks
        // are produced automatically by React.lazy() in routes.tsx.
        manualChunks: (id: string) => {
          if (!id.includes('node_modules')) return undefined;
          // LiveKit is heavy and only used on /video routes (lazy-loaded) — keep
          // it isolated so it never lands in the initial bundle.
          if (id.includes('livekit-client') || id.includes('@livekit')) return 'livekit';
          // STOMP client for the chat WebSocket — only used on /chat detail pages.
          if (id.includes('@stomp/stompjs')) return 'chat-ws';
          // i18n stack.
          if (
            id.includes('i18next') ||
            id.includes('react-i18next') ||
            id.includes('i18next-browser-languagedetector')
          ) {
            return 'i18n';
          }
          // Auth & form stack — bundled together because they ship together on
          // the login / register / verify-otp pages.
          if (
            id.includes('zod') ||
            id.includes('react-hook-form') ||
            id.includes('@hookform/resolvers') ||
            id.includes('axios')
          ) {
            return 'auth';
          }
          // UI primitives — Radix, icons, classvar, tailwind-merge, day-picker.
          if (
            id.includes('@radix-ui') ||
            id.includes('lucide-react') ||
            id.includes('class-variance-authority') ||
            id.includes('tailwind-merge') ||
            id.includes('tailwindcss-animate') ||
            id.includes('react-day-picker') ||
            id.includes('date-fns') ||
            id.includes('clsx')
          ) {
            return 'ui';
          }
          // Core React + router + state — loaded on every page anyway.
          if (
            id.includes('/react/') ||
            id.includes('/react-dom/') ||
            id.includes('react-router') ||
            id.includes('@tanstack/react-query') ||
            id.includes('/zustand/')
          ) {
            return 'vendor';
          }
          return undefined;
        },
      },
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    // Exclude Playwright e2e suite from the Vitest run. Default exclude is
    // {node_modules,dist,...}; we add `e2e/` so the parser doesn't try to
    // load files that import @playwright/test.
    exclude: ['**/node_modules/**', '**/dist/**', 'e2e/**'],
  },
});
