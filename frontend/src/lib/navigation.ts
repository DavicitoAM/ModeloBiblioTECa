import { BookMarked, BookOpen, CalendarDays, ChartNoAxesCombined, ClipboardList, DatabaseBackup, FileClock, Home, LayoutGrid, Megaphone, Package, Settings2, ShieldAlert, ShieldCheck, UsersRound, Bell, Clock3, Wrench, SlidersHorizontal, Mail } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

export type NavigationItem = { to: string; text: string; icon: LucideIcon; access?: 'staff' | 'admin'; integratedOnly?: boolean }
export type NavigationGroup = { id: string; label: string; icon: LucideIcon; items: NavigationItem[] }
export const navigation: NavigationGroup[] = [
  { id: 'collection', label: 'Colección', icon: BookOpen, items: [
    { to: '/catalogo', text: 'Catálogo de libros', icon: BookOpen },
    { to: '/inventario', text: 'Ejemplares', icon: Package, access: 'staff' },
    { to: '/inventario?tab=ubicaciones', text: 'Ubicaciones del acervo', icon: LayoutGrid, access: 'staff' },
    { to: '/configuracion?tab=catalogos', text: 'Catálogos bibliográficos', icon: ClipboardList, access: 'admin' },
  ] },
  { id: 'circulation', label: 'Circulación', icon: BookMarked, items: [
    { to: '/prestamos', text: 'Préstamos y devoluciones', icon: BookMarked },
    { to: '/reservas', text: 'Reservas de libros', icon: CalendarDays },
    { to: '/sanciones', text: 'Incidencias y sanciones', icon: ShieldAlert },
    { to: '/sanciones?tab=incidencias', text: 'Registro de incidencias', icon: ClipboardList },
    { to: '/sanciones?tab=tipos', text: 'Tipos de sanción', icon: SlidersHorizontal, access: 'staff' },
  ] },
  { id: 'spaces', label: 'Espacios de estudio', icon: LayoutGrid, items: [
    { to: '/cubiculos', text: 'Cubículos', icon: LayoutGrid },
    { to: '/cubiculos?tab=reservas', text: 'Agenda de reservaciones', icon: CalendarDays },
    { to: '/cubiculos?tab=horarios', text: 'Horarios de biblioteca', icon: Clock3 },
    { to: '/cubiculos?tab=bloqueos', text: 'Bloqueos de espacios', icon: Wrench, access: 'staff' },
    { to: '/cubiculos?tab=equipo', text: 'Equipamiento', icon: Wrench, access: 'staff' },
  ] },
  { id: 'community', label: 'Comunidad', icon: UsersRound, items: [
    { to: '/personas', text: 'Personas y perfiles', icon: UsersRound, access: 'staff' },
    { to: '/visitas', text: 'Entradas y salidas', icon: ClipboardList, access: 'staff' },
    { to: '/personas?tab=carreras', text: 'Carreras', icon: BookOpen, access: 'staff' },
    { to: '/personas?tab=departamentos', text: 'Departamentos', icon: LayoutGrid, access: 'staff' },
    { to: '/personas?tab=motivos_visita', text: 'Motivos de visita', icon: ClipboardList, access: 'staff' },
    { to: '/cuentas', text: 'Cuentas y permisos', icon: ShieldCheck, access: 'admin', integratedOnly: true },
  ] },
  { id: 'communications', label: 'Comunicaciones', icon: Megaphone, items: [
    { to: '/notificaciones', text: 'Notificaciones', icon: Bell },
    { to: '/avisos', text: 'Avisos de biblioteca', icon: Megaphone },
    { to: '/configuracion?tab=plantillas', text: 'Plantillas de mensajes', icon: Mail, access: 'admin' },
    { to: '/configuracion?tab=canales', text: 'Canales de comunicación', icon: Mail, access: 'admin' },
  ] },
  { id: 'management', label: 'Administración', icon: Settings2, items: [
    { to: '/reportes', text: 'Reportes y estadísticas', icon: ChartNoAxesCombined, access: 'staff' },
    { to: '/configuracion?tab=politicas', text: 'Políticas de servicio', icon: SlidersHorizontal, access: 'admin' },
    { to: '/configuracion', text: 'Configuración', icon: Settings2, access: 'admin' },
    { to: '/configuracion?tab=parametros', text: 'Parámetros generales', icon: SlidersHorizontal, access: 'admin' },
    { to: '/auditoria', text: 'Auditoría', icon: FileClock, access: 'admin' },
    { to: '/respaldos', text: 'Respaldos de datos', icon: DatabaseBackup, access: 'admin' },
    { to: '/incidentes', text: 'Incidentes del sistema', icon: ShieldAlert, access: 'admin' },
  ] },
]
export function visibleNavigation(roles: string[], direct = false): NavigationGroup[] {
  const admin = roles.includes('ADMINISTRADOR'), staff = admin || roles.includes('BIBLIOTECARIO')
  return navigation.map(group => ({ ...group, items: group.items.filter(item => (!item.integratedOnly || !direct) && (!item.access || (item.access === 'admin' ? admin : staff))) })).filter(group => group.items.length)
}
export function matchesNavigation(to: string, pathname: string, search: string) {
  const [path, query] = to.split('?')
  if (pathname !== path) return false
  const activeTab = new URLSearchParams(search).get('tab'), targetTab = new URLSearchParams(query).get('tab')
  return targetTab ? activeTab === targetTab : !activeTab
}
export const routeNames: Record<string, string> = { '/': 'Inicio', '/catalogo': 'Catálogo', '/prestamos': 'Préstamos y devoluciones', '/reservas': 'Reservas de libros', '/cubiculos': 'Espacios de estudio', '/sanciones': 'Incidencias y sanciones', '/personas': 'Comunidad', '/inventario': 'Inventario', '/visitas': 'Entradas y salidas', '/reportes': 'Reportes', '/avisos': 'Avisos', '/cuentas': 'Cuentas y permisos', '/configuracion': 'Configuración', '/auditoria': 'Auditoría', '/respaldos': 'Respaldos', '/incidentes': 'Incidentes', '/notificaciones': 'Notificaciones', '/perfil': 'Mi perfil', '/ayuda': 'Ayuda' }
export const homeNavigation: NavigationItem = { to: '/', text: 'Inicio', icon: Home }
