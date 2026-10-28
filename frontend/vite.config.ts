import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const environment = loadEnv(mode, process.cwd(), 'ITSPA_')
  const target = process.env.ITSPA_API_TARGET || environment.ITSPA_API_TARGET || 'http://127.0.0.1:8081'
  const url = new URL(target)
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.pathname !== '/' || url.search || url.hash) throw new Error('ITSPA_API_TARGET debe ser el origen HTTP del backend, sin credenciales ni rutas.')
  const apiTarget = url.origin
  return {
  plugins: [react(), { name: 'itspa-runtime', configureServer(server) {
    // Development launcher handshake; no credentials or user records are exposed.
    server.middlewares.use('/__itspa_runtime', (request, response) => {
      if (request.method !== 'GET' || (request.url !== '/' && request.url !== '')) { response.statusCode = 404; response.end(); return }
      response.setHeader('Content-Type', 'application/json')
      response.setHeader('Cache-Control', 'no-store')
      response.end(JSON.stringify({ apiTarget }))
    })
  } }],
  server: { proxy: { '/api': { target: apiTarget, changeOrigin: false } } },
  build: { outDir: 'dist', sourcemap: false, rollupOptions: { output: { manualChunks(id) {
    if (!id.includes('node_modules')) return
    if (/datatables/.test(id)) return 'tables'
    if (/sweetalert2/.test(id)) return 'confirmations'
    if (/moment/.test(id)) return 'dates'
    if (/react|scheduler/.test(id)) return 'react'
  } } } },
  }
})
