import { useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { Link, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { ArrowRight, Bell, BookOpen, ChevronDown, CircleHelp, Home, LogOut, Menu, Search, ShieldCheck, UserRound, X, CheckCircle2, AlertCircle, PanelLeftClose, PanelLeftOpen, Maximize, Minimize, ChevronRight } from 'lucide-react'
import { api, type Row } from './lib/api'
import { visibleNavigation, matchesNavigation, routeNames } from './lib/navigation'
import { useSession } from './lib/session'
import { UIContext, useResource, Spinner, ErrorState, Modal } from './components/ui'
import { AccessGate } from './components/Auth'
import { Dashboard, HelpPage } from './pages/Dashboard'
import { CatalogPage } from './pages/Catalog'
import { PeoplePage, AccountsPage, VisitsPage, ProfilePage } from './pages/People'
import { LoansPage, ReservationsPage, CubiclesPage, SanctionsPage, InventoryPage } from './pages/Operations'
import { ReportsPage, NotificationsPage, NoticesPage, SettingsPage, AuditPage, BackupsPage, IncidentsPage } from './pages/Admin'
import institutionalLogo from './components/imgs/logo.png'
import footerArt from './components/imgs/footer.png'
import { notify as pluginNotify, plugin } from './lib/plugins'
import { IonIcon } from './components/LibraryWidgets'
import { ConnectionState } from './components/ConnectionState'

export default function App() {
  const { auth, loading, error, refreshSession, update } = useSession()
  const [refresh, setRefresh] = useState(0)
  const [mobile, setMobile] = useState(false), [commands, setCommands] = useState(false), [userMenu, setUserMenu] = useState(false), [search, setSearch] = useState('')
  const [collapsed, setCollapsed] = useState(() => { try { return localStorage.getItem('itspa.sidebar.collapsed') === 'true' } catch { return false } }), [navFilter, setNavFilter] = useState(''), [fullscreen, setFullscreen] = useState(false)
  const scrollRef = useRef<HTMLDivElement>(null)
  const [opened, setOpened] = useState<string[]>(() => { try { const stored = JSON.parse(localStorage.getItem('itspa.navigation.v2') || '["collection","circulation"]'); return Array.isArray(stored) ? stored.filter((v: unknown) => typeof v === 'string') : ['collection', 'circulation'] } catch { return ['collection', 'circulation'] } })
  const location = useLocation(), navigate = useNavigate(), userMenuRef = useRef<HTMLDivElement>(null), menuRef = useRef<HTMLDivElement>(null), menuButton = useRef<HTMLButtonElement>(null)
  const roles = auth.usuario?.roles || [], admin = roles.includes('ADMINISTRADOR'), gestion = admin || roles.includes('BIBLIOTECARIO')
  const groups = visibleNavigation(roles, !!auth.accesoDirecto)
  const activeGroup = groups.find(g => g.items.some(i => matchesNavigation(i.to, location.pathname, location.search)))
  const tabTitle = activeGroup?.items.find(i => matchesNavigation(i.to, location.pathname, location.search))?.text
  const notify = pluginNotify
  const reload = () => setRefresh(v => v + 1)
  useEffect(() => { try { localStorage.setItem('itspa.sidebar.collapsed', String(collapsed)) } catch { /* El menú también funciona sin almacenamiento. */ } }, [collapsed])
  useEffect(() => { const element = plugin(scrollRef.current); const scroll = element.overlayScrollbars?.({ className: 'os-theme-dark', scrollbars: { autoHide: 'leave' }, overflowBehavior: { x: 'hidden', y: 'scroll' } }).overlayScrollbars(); return () => scroll?.destroy() }, [])
  useEffect(() => { const changed = () => setFullscreen(!!document.fullscreenElement); document.addEventListener('fullscreenchange', changed); return () => document.removeEventListener('fullscreenchange', changed) }, [])
  async function toggleFullscreen() { try { if (document.fullscreenElement) await document.exitFullscreen(); else await document.documentElement.requestFullscreen() } catch { notify('La pantalla completa no está disponible en esta ventana.', true) } }
  useEffect(() => { setMobile(false); setUserMenu(false); document.title = `${tabTitle || routeNames[location.pathname] || 'Biblioteca'} · BiblioTECa ITSPA`; window.scrollTo(0, 0) }, [location.pathname, location.search, tabTitle])
  useEffect(() => { if (activeGroup) setOpened(v => v.includes(activeGroup.id) ? v : [...v, activeGroup.id]) }, [activeGroup?.id, location.pathname, location.search])
  useEffect(() => { try { localStorage.setItem('itspa.navigation.v2', JSON.stringify(opened)) } catch { /* La navegación sigue disponible sin almacenamiento local. */ } }, [opened])
  useEffect(() => {
    const key = (e: KeyboardEvent) => { if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); setCommands(v => !v) } if (e.key === 'Escape') { setMobile(false); setUserMenu(false); menuButton.current?.focus() } }
    const click = (e: MouseEvent) => { if (!userMenuRef.current?.contains(e.target as Node)) setUserMenu(false) }
    document.addEventListener('keydown', key); document.addEventListener('mousedown', click)
    return () => { document.removeEventListener('keydown', key); document.removeEventListener('mousedown', click) }
  }, [])
  useEffect(() => {
    if (!mobile) return
    const old = document.body.style.overflow; document.body.style.overflow = 'hidden'
    const first = menuRef.current?.querySelector<HTMLElement>('a,button'); first?.focus()
    const trap = (e: KeyboardEvent) => { if (e.key !== 'Tab') return; const items = Array.from(menuRef.current?.querySelectorAll<HTMLElement>('a[href],button:not(:disabled)') || []).filter(el => el.getClientRects().length); const last = items.at(-1); if (e.shiftKey && document.activeElement === items[0]) { e.preventDefault(); last?.focus() } else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); items[0]?.focus() } }
    document.addEventListener('keydown', trap); return () => { document.body.style.overflow = old; document.removeEventListener('keydown', trap) }
  }, [mobile])
  const guard = (page: ReactNode, staff = false, onlyAdmin = false) => <AccessGate staff={staff} admin={onlyAdmin}>{page}</AccessGate>
  const toggle = (id: string) => { if (collapsed) { setCollapsed(false); setOpened(v => v.includes(id) ? v : [...v, id]) } else setOpened(v => v.includes(id) ? v.filter(i => i !== id) : [...v, id]) }
  const active = (to: string) => matchesNavigation(to, location.pathname, location.search)
  const menuItem = (item: { to: string; text: string; icon: typeof Home }) => <Link key={item.to} aria-label={item.text} title={item.text} className={`nav-item ${active(item.to) ? 'active' : ''}`} aria-current={active(item.to) ? 'page' : undefined} to={item.to}><item.icon size={19} strokeWidth={1.7} /><span>{item.text}</span>{active(item.to) && <span className="nav-active-dot" />}</Link>
  return <UIContext.Provider value={{ refresh, reload, notify, gestion, admin }}>
    <a href="#main-content" className="skip-link" onClick={e => { e.preventDefault(); document.getElementById('main-content')?.focus() }}>Saltar al contenido</a>
    <div className={`app-shell adminlte-blue ${collapsed ? 'sidebar-compact' : ''}`}>
      {mobile && <button className="sidebar-overlay" aria-label="Cerrar menú" onClick={() => { setMobile(false); menuButton.current?.focus() }} />}
      <aside className={`sidebar ${mobile ? 'open' : ''}`} aria-label="Navegación principal" ref={menuRef}>
        <div className="brand-area"><button className="icon-button close-navigation" aria-label="Cerrar navegación" onClick={() => { setMobile(false); menuButton.current?.focus() }}><X size={22} /></button><Link to="/" className="product-brand" aria-label="BiblioTECa, inicio"><span className="product-icon"><IonIcon name="library" size={25} /></span><span className="brand-word">Biblio<strong>TECa</strong><small>Gestión bibliotecaria · ITSPA</small></span></Link></div>
        <div className="sidebar-search"><Search size={17} /><input aria-label="Filtrar secciones del menú" placeholder="Buscar una sección…" value={navFilter} onChange={e => setNavFilter(e.target.value)} /><kbd>/</kbd></div>
        <div className="sidebar-scroll" ref={scrollRef}><nav aria-label="Secciones de biblioteca"><span className="nav-caption">ESPACIO DE TRABAJO</span><div className="nav-home">{menuItem({ to: '/', text: 'Panel de inicio', icon: Home })}</div>{groups.filter(group => !navFilter || `${group.label} ${group.items.map(i => i.text).join(' ')}`.toLocaleLowerCase('es').includes(navFilter.toLocaleLowerCase('es'))).map(group => <section className={`nav-group ${opened.includes(group.id) || navFilter ? 'expanded' : ''} ${activeGroup?.id === group.id ? 'has-active' : ''}`} key={group.id}><button className="nav-group-toggle" title={group.label} aria-label={group.label} aria-expanded={!(collapsed && !mobile) && (!!navFilter || opened.includes(group.id))} aria-controls={`nav-${group.id}`} onClick={() => toggle(group.id)}><group.icon size={20} strokeWidth={1.7} /><span>{group.label}</span><ChevronDown size={16} /></button><div id={`nav-${group.id}`} className="nav-group-items" hidden={(collapsed && !mobile) || (!opened.includes(group.id) && !navFilter)}>{group.items.filter(i => !navFilter || `${group.label} ${i.text}`.toLocaleLowerCase('es').includes(navFilter.toLocaleLowerCase('es'))).map(menuItem)}</div></section>)}{navFilter && !groups.some(g => `${g.label} ${g.items.map(i => i.text).join(' ')}`.toLocaleLowerCase('es').includes(navFilter.toLocaleLowerCase('es'))) && <p className="nav-no-results">No hay secciones con ese nombre.</p>}</nav></div>
        <div className="sidebar-bottom"><Link title="Centro de ayuda" className={`nav-item ${location.pathname === '/ayuda' ? 'active' : ''}`} to="/ayuda"><CircleHelp size={19} /><span>Centro de ayuda</span><ArrowRight size={16} /></Link><div className="sidebar-campus"><img src={institutionalLogo} alt="Tecnológico Nacional de México" /><div><strong>Campus Pátzcuaro</strong><span>Biblioteca institucional</span></div></div></div>
      </aside>
      <div className="workspace">
        <header className="topbar"><div className="topbar-location"><button className="icon-button desktop-menu" aria-label={collapsed ? 'Expandir menú lateral' : 'Plegar menú lateral'} aria-expanded={!collapsed} onClick={() => setCollapsed(v => !v)}>{collapsed ? <PanelLeftOpen size={21} /> : <PanelLeftClose size={21} />}</button><button ref={menuButton} className="icon-button mobile-menu" aria-label="Abrir menú de navegación" aria-expanded={mobile} onClick={() => setMobile(true)}><Menu size={25} /></button><div className="breadcrumb"><Link to="/" aria-label="Inicio de biblioteca">Biblioteca</Link><ChevronRight size={14} /><strong>{tabTitle || routeNames[location.pathname] || 'Panel de inicio'}</strong></div></div><button className="command-trigger" onClick={() => { setSearch(''); setCommands(true) }}><Search size={18} /><span>Buscar libros o secciones…</span><kbd>Ctrl K</kbd></button><div className="topbar-actions"><button className="icon-button fullscreen-toggle" aria-label={fullscreen ? 'Salir de pantalla completa' : 'Pantalla completa'} onClick={toggleFullscreen}>{fullscreen ? <Minimize size={20} /> : <Maximize size={20} />}</button>{auth.usuario && !error && !loading && <NotificationsIndicator />}{auth.accesoDirecto ? <div className="direct-context"><span className="avatar"><IonIcon name="library" size={21} /></span><div><strong>Biblioteca ITSPA</strong><small>Gestión bibliotecaria</small></div></div> : auth.usuario ? <div className="user-menu" ref={userMenuRef}><button className="user-button" aria-expanded={userMenu} onClick={() => setUserMenu(v => !v)} aria-label="Opciones de mi cuenta"><span className="avatar">{auth.usuario.nombre.split(' ').filter(Boolean).slice(0, 2).map(n => n[0]).join('')}</span><span className="user-label"><strong>{auth.usuario.nombre.split(' ')[0]}</strong><small>{admin ? 'Administración' : gestion ? 'Bibliotecario' : 'Comunidad'}</small></span><ChevronDown size={16} /></button>{userMenu && <div className="dropdown-menu"><div className="dropdown-identity"><strong>{auth.usuario.nombre}</strong></div><Link to="/perfil"><UserRound size={18} />Mi perfil</Link><Link to="/notificaciones"><Bell size={18} />Notificaciones</Link><button onClick={async () => { try { update(await api('/auth/logout', 'POST', {})); reload(); navigate('/') } catch (e: any) { notify(e.message, true) } }}><LogOut size={18} />Cerrar sesión</button></div>}</div> : null}</div></header>
        <main id="main-content" tabIndex={-1}>
          {loading ? <Spinner label="Conectando con la biblioteca…" /> : error ? <ConnectionState /> : <Routes>
            <Route path="/" element={<Dashboard />} /><Route path="/catalogo" element={<CatalogPage />} /><Route path="/prestamos" element={guard(<LoansPage />)} /><Route path="/reservas" element={guard(<ReservationsPage />)} /><Route path="/cubiculos" element={guard(<CubiclesPage />)} /><Route path="/sanciones" element={guard(<SanctionsPage />)} /><Route path="/inventario" element={guard(<InventoryPage />, true)} /><Route path="/personas" element={guard(<PeoplePage />, true)} /><Route path="/cuentas" element={auth.accesoDirecto ? <Navigate to="/personas" replace /> : guard(<AccountsPage />, false, true)} /><Route path="/visitas" element={guard(<VisitsPage />, true)} /><Route path="/perfil" element={auth.accesoDirecto ? <Navigate to="/" replace /> : guard(<ProfilePage />)} /><Route path="/reportes" element={guard(<ReportsPage />, true)} /><Route path="/notificaciones" element={guard(<NotificationsPage />)} /><Route path="/avisos" element={guard(<NoticesPage />)} /><Route path="/configuracion" element={guard(<SettingsPage />, false, true)} /><Route path="/auditoria" element={guard(<AuditPage />, false, true)} /><Route path="/respaldos" element={guard(<BackupsPage />, false, true)} /><Route path="/incidentes" element={guard(<IncidentsPage />, false, true)} /><Route path="/ayuda" element={<HelpPage />} />
            <Route path="*" element={<div className="access-card"><BookOpen size={48} /><h1>No encontramos esta página</h1><p>Vuelve al inicio para encontrar el servicio que necesitas.</p><Link to="/" className="btn primary">Ir al inicio</Link></div>} />
          </Routes>}
        </main>
        <footer className="app-footer"><div><strong>Biblioteca · ITSPA</strong><Link to="/ayuda">Ayuda<ArrowRight size={15} /></Link></div><img src={footerArt} alt="Tecnológico Nacional de México" /></footer>
      </div>
    </div>
    {commands && <Modal title="Encuentra lo que necesitas" onClose={() => setCommands(false)}><div className="modal-body command-list"><label className="input-icon"><Search size={21} /><input aria-label="Buscar un libro o sección" placeholder="Un libro, una sección…" value={search} onChange={e => setSearch(e.target.value)} /></label>{[{ to: '/', text: 'Inicio', icon: Home }, ...groups.flatMap(g => g.items)].filter(i => i.text.toLowerCase().includes(search.toLowerCase())).map(i => <button key={i.to} onClick={() => { navigate(i.to); setCommands(false) }}><i.icon size={20} /><span>{i.text}</span><ArrowRight size={18} /></button>)}{search && <button className="search-catalog-command" onClick={() => { navigate(`/catalogo?q=${encodeURIComponent(search)}`); setCommands(false) }}><BookOpen size={22} /><span>Buscar «{search}» en el catálogo</span><ArrowRight size={18} /></button>}</div></Modal>}
  </UIContext.Provider>
}
function NotificationsIndicator() { const { auth } = useSession(); const { data } = useResource<Row>(`/notificaciones${auth.accesoDirecto ? '?gestion=true' : ''}`); return <Link className="notification-button icon-button" to="/notificaciones" aria-label={`Notificaciones${data?.no_leidas ? `, ${data.no_leidas} sin leer` : ''}`}><Bell size={23} />{!!data?.no_leidas && <span>{data.no_leidas > 9 ? '9+' : data.no_leidas}</span>}</Link> }
