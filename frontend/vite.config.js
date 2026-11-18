import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd()) // only VITE_* variables are read

  return {
    plugins: [react(), tailwindcss()],
    server: {
      // The backend has no CORS configuration, so during development the browser talks to
      // this dev server and "/api" requests are forwarded to the Spring Boot backend.
      proxy: {
        '/api': {
          target: env.VITE_DEV_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
