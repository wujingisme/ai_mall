import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', 'VITE_');
  return {
    plugins: [react()],
    server: {
      port: 5174,
      proxy: { '/api': { target: env.VITE_API_BASE_URL, changeOrigin: true } },
    },
  };
});
