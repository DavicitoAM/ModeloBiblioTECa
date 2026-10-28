import test, { afterEach } from 'node:test'
import assert from 'node:assert/strict'
import { api, authStatus, ConnectionError, ApiError } from '../src/lib/api.ts'

const originalFetch = globalThis.fetch
afterEach(() => { globalThis.fetch = originalFetch })

test('un proxy sin backend produce un error de conexión claro', async () => {
  globalThis.fetch = async () => new Response('', { status: 500, headers: { 'Content-Type': 'text/plain' } })
  await assert.rejects(api('/inicio'), error => error instanceof ConnectionError && /servicio de la biblioteca/.test(error.message))
})
test('un fallo de red o tiempo agotado no se presenta como un error del formulario', async () => {
  globalThis.fetch = async () => { throw new TypeError('Failed to fetch') }
  await assert.rejects(api('/inicio'), ConnectionError)
  globalThis.fetch = async () => { throw new DOMException('Timeout', 'TimeoutError') }
  await assert.rejects(api('/inicio'), error => error instanceof ConnectionError && /tarda demasiado/.test(error.message))
})
test('HTML de un puerto equivocado no se acepta como datos de la API', async () => {
  globalThis.fetch = async () => new Response('<html>Otro servidor</html>', { headers: { 'Content-Type': 'text/html' } })
  await assert.rejects(authStatus(), ConnectionError)
})
test('el error JSON real de un módulo mantiene su mensaje y no se confunde con desconexión', async () => {
  globalThis.fetch = async () => Response.json({ error: 'Error del módulo' }, { status: 500 })
  await assert.rejects(api('/inicio'), error => error instanceof ApiError && !(error instanceof ConnectionError) && error.message === 'Error del módulo')
})
test('la sesión debe tener la estructura del servidor antes de abrir vistas', async () => {
  globalThis.fetch = async () => Response.json({ ok: true })
  await assert.rejects(authStatus(), ConnectionError)
  globalThis.fetch = async () => Response.json({ inicializado: true, usuario: null })
  assert.deepEqual(await authStatus(), { inicializado: true, usuario: null })
})
test('no se reenvían escrituras cuando falla la conexión', async () => {
  let calls = 0
  globalThis.fetch = async () => { calls++; throw new TypeError('Failed to fetch') }
  await assert.rejects(api('/prestamos', 'POST', {}), ConnectionError)
  assert.equal(calls, 1)
})
test('una respuesta 204 sigue siendo una operación correcta', async () => {
  globalThis.fetch = async () => new Response(null, { status: 204 })
  assert.equal(await api('/recurso', 'DELETE'), null)
})
