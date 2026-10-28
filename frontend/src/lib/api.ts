export type Row = Record<string, any>
export type AuthState = {
  inicializado: boolean
  accesoDirecto?: boolean
  usuario: { id: number; personaId: number; usuario: string; nombre: string; roles: string[]; estadoVerificacion?: string } | null
  csrfToken?: string; csrfHeader?: string
}

let csrf = { token: '', header: 'X-CSRF-TOKEN' }
export class ApiError extends Error {
  status: number
  details?: Row
  constructor(message: string, status: number, details?: Row) { super(message); this.status = status; this.details = details }
}
export const CONNECTION_LOST = 'itspa:connection-lost'
export class ConnectionError extends ApiError {
  constructor(message = 'No hay conexión con el servicio de la biblioteca. Comprueba que el sistema haya terminado de iniciar.') { super(message, 503); this.name = 'ConnectionError' }
}
function disconnected(error: ConnectionError): never {
  if (typeof window !== 'undefined') window.dispatchEvent(new CustomEvent(CONNECTION_LOST, { detail: error }))
  throw error
}
async function request(path: string, init: RequestInit = {}) {
  let response: Response
  try { response = await fetch(`/api/app${path}`, { ...init, credentials: 'same-origin', signal: AbortSignal.timeout(10000) }) }
  catch (error) { return disconnected(new ConnectionError(error instanceof DOMException && error.name === 'TimeoutError' ? 'El servicio de la biblioteca tarda demasiado en responder. Volveremos a comprobar la conexión.' : undefined)) }
  // Vite returns an empty/text 500 on ECONNREFUSED; a gateway may return HTML.
  // A JSON 500 from the application is a module error, not a lost connection.
  const json = /application\/(?:[\w.-]+\+)?json/i.test(response.headers.get('Content-Type') || '')
  const wantsJson = new Headers(init.headers).get('Accept') === 'application/json'
  if ([502, 503, 504].includes(response.status)) return disconnected(new ConnectionError())
  if (!json && (response.status >= 500 || (response.ok && response.status !== 204 && wantsJson))) return disconnected(new ConnectionError())
  return response
}
export function setCsrf(auth: AuthState) {
  if (auth.csrfToken) csrf = { token: auth.csrfToken, header: auth.csrfHeader || 'X-CSRF-TOKEN' }
}
export async function api<T = any>(path: string, method = 'GET', data?: unknown): Promise<T> {
  const response = await request(path, {
    method,
    headers: { Accept: 'application/json', ...(data !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...(method !== 'GET' && csrf.token ? { [csrf.header]: csrf.token } : {}) },
    ...(data !== undefined ? { body: JSON.stringify(data) } : {}),
  })
  const body = response.status === 204 ? null : await response.json().catch(() => null)
  if (response.ok && response.status !== 204 && body === null) return disconnected(new ConnectionError('El servicio devolvió una respuesta incompleta. Volveremos a comprobar la conexión.'))
  if (!response.ok) {
    throw new ApiError(body?.error || body?.message || body?.mensaje || (response.status === 401 ? 'Inicia sesión para continuar.' : response.status === 403 ? 'Tu cuenta no tiene permiso para realizar esta operación.' : 'No pudimos completar la solicitud.'), response.status, body)
  }
  return body as T
}
export async function authStatus() {
  const a = await api<AuthState>('/auth/estado')
  if (!a || typeof a.inicializado !== 'boolean' || !Object.hasOwn(a, 'usuario')) return disconnected(new ConnectionError('No se pudo verificar el servicio de la biblioteca. Comprueba que esté iniciado correctamente.'))
  setCsrf(a); return a
}
export async function download(path: string, fallbackName: string) {
  const r = await request(path)
  if ([502, 503, 504].includes(r.status)) return disconnected(new ConnectionError())
  if (!r.ok) { const b = await r.json().catch(() => ({})); throw new ApiError(b.error || b.message || 'No se pudo generar el archivo.', r.status) }
  const url = URL.createObjectURL(await r.blob()), a = document.createElement('a')
  const header = r.headers.get('Content-Disposition') || ''
  const filename = /filename\*?=(?:UTF-8''|\")?([^";]+)/i.exec(header)?.[1]
  a.href = url; a.download = filename ? decodeURIComponent(filename) : fallbackName; a.click(); setTimeout(() => URL.revokeObjectURL(url), 1000)
}
export function rows(data: any): Row[] { return Array.isArray(data) ? data : data?.items || data?.filas || [] }
export function name(r: Row | null | undefined) { return r?.nombre_completo || r?.persona || r?.titulo || r?.nombre || r?.usuario || r?.codigo || `#${r?.id ?? '—'}` }
export function personName(r: Row) { return r.nombre_completo || r.persona || [r.nombre, r.apellidos].filter(Boolean).join(' ') || r.usuario || `Persona ${r.id}` }
export const money = (n: any) => new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' }).format(Number(n || 0))
export function date(value: any, time = false) {
  if (!value) return '—'
  const d = new Date(String(value).length === 10 ? `${value}T12:00:00` : value)
  return Number.isNaN(d.getTime()) ? String(value) : new Intl.DateTimeFormat('es-MX', { day: '2-digit', month: 'short', year: 'numeric', ...(time ? { hour: '2-digit', minute: '2-digit' } as const : {}) }).format(d)
}
export function today() { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` }
export const human = (s: any) => String(s ?? '').replaceAll('_', ' ').toLowerCase().replace(/^./, c => c.toUpperCase())
