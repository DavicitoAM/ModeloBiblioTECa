import { Loader2, RefreshCw, Server, WifiOff } from 'lucide-react'
import { useSession } from '../lib/session'

export function ConnectionState() {
  const { error, reconnecting, refreshSession } = useSession()
  return <section className="connection-state" aria-labelledby="connection-title" aria-busy={reconnecting}>
    <span className="connection-symbol"><WifiOff size={35} strokeWidth={1.5} /></span>
    <span className="eyebrow">CONEXIÓN DEL SISTEMA</span>
    <h1 id="connection-title">Estamos esperando al servicio de biblioteca</h1>
    <p role="alert">{error}</p>
    <div className="connection-help"><Server size={21} /><div><strong>La interfaz está lista; falta conectar el servicio.</strong><p>Inicia el sistema y espera a que termine de arrancar. Si solicita la contraseña de PostgreSQL, introdúcela en tu terminal local.</p></div></div>
    <button className="btn primary" disabled={reconnecting} onClick={() => { void refreshSession() }}>{reconnecting ? <Loader2 className="spin" size={18} /> : <RefreshCw size={18} />}{reconnecting ? 'Comprobando conexión…' : 'Reintentar conexión'}</button>
    <small className="connection-status" role="status">Comprobaremos la conexión automáticamente. Tus datos aparecerán cuando el servicio esté disponible.</small>
  </section>
}
