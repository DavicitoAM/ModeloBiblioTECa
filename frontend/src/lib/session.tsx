import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { authStatus, CONNECTION_LOST, setCsrf } from './api'
import type { AuthState } from './api'

type Session = { auth: AuthState; loading: boolean; reconnecting: boolean; error: string; update: (a: AuthState) => void; refreshSession: () => Promise<boolean>; dialog: string | null; setDialog: (mode: string | null) => void }
const SessionContext = createContext<Session>(null!)
export const useSession = () => useContext(SessionContext)
export function SessionProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState>({ inicializado: true, usuario: null }), [loading, setLoading] = useState(true), [error, setError] = useState(''), [dialog, setDialog] = useState<string | null>(null)
  const [reconnecting, setReconnecting] = useState(false), mounted = useRef(false), pending = useRef<Promise<boolean> | null>(null)
  const update = useCallback((a: AuthState) => { setCsrf(a); setAuth(a); setError('') }, [])
  const refreshSession = useCallback(() => {
    if (pending.current) return pending.current
    setReconnecting(true)
    pending.current = (async () => {
      try { const result = await authStatus(); if (mounted.current) update(result); return true }
      catch (e: any) { if (mounted.current) setError(e.message || 'No hay conexión con el servidor.'); return false }
      finally { if (mounted.current) { setLoading(false); setReconnecting(false) }; pending.current = null }
    })()
    return pending.current
  }, [update])
  useEffect(() => {
    mounted.current = true
    const lost = (event: Event) => { setError((event as CustomEvent<Error>).detail.message); setLoading(false) }
    window.addEventListener(CONNECTION_LOST, lost)
    void refreshSession()
    return () => { mounted.current = false; window.removeEventListener(CONNECTION_LOST, lost) }
  }, [refreshSession])
  useEffect(() => {
    if (!error) return
    const retry = () => { if (document.visibilityState !== 'hidden') void refreshSession() }
    const timer = window.setInterval(retry, 5000)
    window.addEventListener('online', retry); window.addEventListener('focus', retry)
    return () => { clearInterval(timer); window.removeEventListener('online', retry); window.removeEventListener('focus', retry) }
  }, [error, refreshSession])
  return <SessionContext.Provider value={{ auth, loading, reconnecting, error, update, refreshSession, dialog, setDialog }}>{children}</SessionContext.Provider>
}
