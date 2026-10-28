import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { ShieldCheck } from 'lucide-react'
import { useSession } from '../lib/session'
import { ErrorState, Spinner, useUI } from './ui'

/** El índice abre directamente. El servidor resuelve el contexto operativo. */
export function AccessGate({ children, admin = false, staff = false }: { children: ReactNode; admin?: boolean; staff?: boolean }) {
  const { auth, loading, refreshSession } = useSession(), { gestion, admin: isAdmin } = useUI()
  if (loading) return <Spinner label="Conectando con la biblioteca…" />
  if (!auth.usuario) return <ErrorState message="No se pudo obtener el contexto de operación. Comprueba que el servidor y PostgreSQL estén disponibles." retry={() => { refreshSession() }} />
  if ((admin && !isAdmin) || (staff && !gestion)) return <div className="access-card"><ShieldCheck size={42} /><h1>Permisos del sistema institucional</h1><p>La identidad recibida no tiene acceso a esta sección. La administración de permisos corresponde al sistema que integra esta biblioteca.</p><Link className="btn primary" to="/">Ir al inicio</Link></div>
  return children
}
