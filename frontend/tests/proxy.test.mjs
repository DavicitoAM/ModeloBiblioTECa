import test from 'node:test'
import assert from 'node:assert/strict'
import { createServer as createHttpServer } from 'node:http'
import { createServer as createViteServer } from 'vite'

test('un puerto API alternativo configura el proxy y el diagnóstico del lanzador', { timeout: 30000 }, async () => {
  // HTTP fixture only: this test never connects to Spring Boot or PostgreSQL.
  const paths = []
  const backend = createHttpServer((request, response) => {
    paths.push(request.url)
    response.writeHead(200, { 'Content-Type': 'application/json' })
    response.end(JSON.stringify({ inicializado: true, accesoDirecto: true, usuario: null }))
  })
  await new Promise(resolve => backend.listen(0, '127.0.0.1', resolve))
  const apiTarget = `http://127.0.0.1:${backend.address().port}`
  const probe = createHttpServer()
  await new Promise(resolve => probe.listen(0, '127.0.0.1', resolve))
  const frontendPort = probe.address().port
  await new Promise(resolve => probe.close(resolve))
  const previous = process.env.ITSPA_API_TARGET
  let frontend
  try {
    process.env.ITSPA_API_TARGET = apiTarget
    frontend = await createViteServer({ server: { host: '127.0.0.1', port: frontendPort, strictPort: true }, logLevel: 'silent' })
    await frontend.listen()
    const origin = `http://127.0.0.1:${frontend.httpServer.address().port}`
    const diagnostic = await fetch(`${origin}/__itspa_runtime`)
    assert.equal(diagnostic.status, 200)
    assert.deepEqual(await diagnostic.json(), { apiTarget })
    const state = await fetch(`${origin}/api/app/auth/estado`)
    assert.equal(state.status, 200)
    assert.equal((await state.json()).accesoDirecto, true)
    assert.deepEqual(paths, ['/api/app/auth/estado'])
  } finally {
    await frontend?.close()
    await new Promise(resolve => backend.close(resolve))
    if (previous === undefined) delete process.env.ITSPA_API_TARGET; else process.env.ITSPA_API_TARGET = previous
  }
})
