import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In dev, /api requests are proxied to the Spring Boot backend.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // Not "localhost": it can resolve to IPv6, and the backend only listens on 127.0.0.1.
      '/api': 'http://127.0.0.1:8080',
    },
  },
});
