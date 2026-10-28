import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Archive, Bell, Check, CheckCheck, ChevronRight, DatabaseBackup, Download, Eye, FileBarChart2, FileSpreadsheet, FileText, Mail, Pencil, Plus, Settings2, ShieldCheck, SlidersHorizontal } from 'lucide-react'
import { api, date, download, human, money, rows, today } from '../lib/api'
import type { Row } from '../lib/api'
import { useSession } from '../lib/session'
import { Badge, Confirm, DataTable, Detail, Empty, ErrorState, FormDialog, Modal, PageTitle, Spinner, opt, options, useResource, useUI } from '../components/ui'
import type { Field } from '../components/ui'
import { ChosenSelect, DateRangeField } from '../components/LibraryWidgets'

const monthStart = () => `${today().slice(0, 7)}-01`
const localTime = (value: unknown) => value ? String(value).slice(0, 16) : ''
const query = (values: Row) => new URLSearchParams(Object.entries(values).filter(([, v]) => v !== '' && v != null).map(([k, v]) => [k, String(v)])).toString()

function Value({ value, field }: { value: any; field: string }) {
  if (value == null) return <>—</>
  if (/^fecha|_en$|ultimo_acceso/.test(field)) return <>{date(value, String(value).includes('T'))}</>
  if (/monto|saldo|costo/.test(field)) return <>{money(value)}</>
  if (/^estado$|^estatus$/.test(field)) return <Badge value={value} />
  return <>{String(value)}</>
}

export function ReportsPage() {
  const { admin, notify } = useUI()
  const [filters, setFilters] = useState<Row>({ tipo: 'catalogo', desde: monthStart(), hasta: today() })
  const [report, setReport] = useState<Row | null>(null), [error, setError] = useState(''), [busy, setBusy] = useState(false), [exporting, setExporting] = useState('')
  const kinds = [
    { value: 'catalogo', label: 'Catálogo e inventario' }, { value: 'prestamos', label: 'Préstamos' },
    { value: 'devoluciones', label: 'Devoluciones' }, { value: 'reservas', label: 'Reservaciones de libros' },
    { value: 'cubiculos', label: 'Uso y reservas de cubículos' }, { value: 'visitas', label: 'Visitas a biblioteca' },
    { value: 'sanciones', label: 'Sanciones y multas' }, { value: 'usuarios', label: 'Usuarios registrados' },
    ...(admin ? [{ value: 'auditoria', label: 'Auditoría administrativa' }] : []),
  ]
  async function generate(e: FormEvent) {
    e.preventDefault(); if (busy) return; setBusy(true); setError('')
    try { setReport(await api(`/reportes?${query(filters)}`)) } catch (e: any) { setError(e.message) } finally { setBusy(false) }
  }
  async function exportReport(format: string) {
    if (!report || exporting) return; setExporting(format)
    try { await download(`/reportes/exportar?${query({ tipo: report.tipo, desde: report.desde, hasta: report.hasta, formato: format })}`, `reporte-itspa.${format}`); notify('El archivo está listo para descargar.') } catch (e: any) { notify(e.message, true) } finally { setExporting('') }
  }
  return <>
    <PageTitle eyebrow="Información para decidir" title="Reportes y estadísticas" description="Consulta la actividad real de la biblioteca y prepara tus informes en unos pasos." />
    <section className="panel"><div className="section-heading"><div><h2>Prepara tu reporte</h2><p>Elige la información y el periodo que necesitas.</p></div><SlidersHorizontal size={22} /></div>
      <form onSubmit={generate} className="form-grid">
        <label className="field"><span>Tipo de reporte</span><ChosenSelect label="Tipo de reporte" value={filters.tipo} options={kinds} required onChange={tipo => setFilters(v => ({ ...v, tipo }))} /></label>
        <DateRangeField from={filters.desde} to={filters.hasta} onChange={(desde, hasta) => setFilters(v => ({ ...v, desde, hasta }))} />
        <div className="field"><span>&nbsp;</span><button className="btn primary" disabled={busy} type="submit"><FileBarChart2 size={18} />{busy ? 'Consultando…' : 'Generar reporte'}</button></div>
      </form>
      {filters.tipo === 'catalogo' && <p className="form-note">El inventario muestra la existencia actual del acervo. Los demás reportes filtran movimientos por el periodo indicado.</p>}
      {error && <div className="inline-error" role="alert">{error}</div>}
    </section>
    {busy ? <Spinner label="Consultando registros y preparando el informe…" /> : report ? <div className="stack">
      <section className="panel"><div className="section-heading"><div><span className="eyebrow">Reporte generado</span><h2>{kinds.find(t => t.value === report.tipo)?.label || human(report.tipo)}</h2><p>{date(report.desde)} — {date(report.hasta)} · {report.total_registros} registros · {date(report.generado_en, true)}</p></div><div className="page-actions">{['pdf', 'xlsx', 'csv', 'json'].map(format => <button className="btn secondary small" key={format} disabled={!!exporting} onClick={() => exportReport(format)}>{format === 'pdf' ? <FileText size={16} /> : format === 'xlsx' ? <FileSpreadsheet size={16} /> : <Download size={16} />}{exporting === format ? 'Preparando…' : format.toUpperCase()}</button>)}</div></div></section>
      <DataTable data={rows(report)} columns={(report.columnas || []).map((field: string) => ({ key: field, label: human(field), render: (r: Row) => <Value value={r[field]} field={field} /> }))} emptyTitle="Sin movimientos en este periodo" emptyDescription="Amplía las fechas o selecciona otro tipo de reporte. Puedes descargar el informe vacío con sus encabezados." />
    </div> : <section className="panel"><Empty title="Tu próximo informe, aquí" description="Selecciona un reporte y un periodo. Podrás revisar sus registros y descargarlo como PDF, Excel, CSV o JSON." /></section>}
  </>
}

export function NotificationsPage() {
  const { reload, notify, gestion } = useUI(), { auth } = useSession(), supervision = !!auth.accesoDirecto && gestion
  const [tab, setTab] = useState('todas'), [detail, setDetail] = useState<Row | null>(null), [showPreferences, setShowPreferences] = useState(false)
  const [filters, setFilters] = useState<Row>({ tipo: '', desde: '', hasta: '', cuenta_id: '' }), [applied, setApplied] = useState<Row>({})
  const resource = useResource<Row>(`/notificaciones?${query({ ...applied, gestion: supervision, archivadas: tab === 'archivadas', estado: tab === 'pendientes' ? 'NO_LEIDA' : null })}`)
  const preferences = useResource<Row>(supervision ? null : '/preferencias')
  const [busy, setBusy] = useState<number | string | null>(null)
  async function act(path: string, key: number | string, message: string) {
    if (busy) return; setBusy(key)
    try { await api(path, 'POST', {}); reload(); notify(message) } catch (e: any) { notify(e.message, true) } finally { setBusy(null) }
  }
  async function open(item: Row) { setDetail(item); if (!supervision && !item.fecha_lectura) { try { const updated = await api(`/notificaciones/${item.id}/leer`, 'POST', {}); setDetail({ ...item, ...updated }); reload() } catch (e: any) { notify(e.message, true) } } }
  return <>
    <PageTitle eyebrow="Tu biblioteca, al día" title={supervision ? 'Centro de comunicaciones' : 'Notificaciones'} description={supervision ? 'Supervisa los mensajes de servicio y su destinatario sin modificar el estado de lectura de la comunidad.' : 'Tus confirmaciones, recordatorios y novedades importantes en un solo lugar.'} actions={supervision ? <button className="btn secondary" onClick={reload}><Bell size={17} />Actualizar comunicaciones</button> : <><button className="btn secondary" onClick={() => setShowPreferences(true)}><Settings2 size={17} />Preferencias</button><button className="btn primary" disabled={!resource.data?.no_leidas || !!busy} onClick={() => act('/notificaciones/todas-leidas', 'all', 'Todas tus notificaciones visibles están marcadas como leídas.')}><CheckCheck size={17} />Marcar todas como leídas</button></>} />
    <div className="tabs" role="tablist" aria-label="Filtrar notificaciones">{[{ key: 'todas', label: 'Bandeja de entrada' }, { key: 'pendientes', label: `No leídas${resource.data ? ` (${resource.data.no_leidas})` : ''}` }, { key: 'archivadas', label: 'Archivadas' }].map(t => <button key={t.key} role="tab" aria-selected={tab === t.key} className={tab === t.key ? 'active' : ''} onClick={() => setTab(t.key)}>{t.label}</button>)}</div>
    <section className="panel"><form className="form-grid" onSubmit={e => { e.preventDefault(); setApplied({ ...filters }) }}>{supervision && <label className="field"><span>Destinatario</span><select value={filters.cuenta_id} onChange={e => setFilters(v => ({ ...v, cuenta_id: e.target.value }))}><option value="">Todas las cuentas</option>{rows(resource.data?.destinatarios).map(d => <option key={d.id} value={d.id}>{d.nombre} · @{d.usuario}</option>)}</select></label>}<label className="field"><span>Tipo de notificación</span><select value={filters.tipo} onChange={e => setFilters(v => ({ ...v, tipo: e.target.value }))}><option value="">Todos los tipos</option>{rows(resource.data?.tipos).map(t => <option key={t.codigo} value={t.codigo}>{human(t.nombre)}</option>)}</select></label><label className="field"><span>Desde</span><input type="date" value={filters.desde} max={filters.hasta || undefined} onChange={e => setFilters(v => ({ ...v, desde: e.target.value }))} /></label><label className="field"><span>Hasta</span><input type="date" value={filters.hasta} min={filters.desde || undefined} onChange={e => setFilters(v => ({ ...v, hasta: e.target.value }))} /></label><div className="field"><span>&nbsp;</span><button className="btn secondary" type="submit"><SlidersHorizontal size={17} />Filtrar</button></div></form><p className="form-note">Puedes consultar hasta 500 mensajes por filtro. Acota las fechas para encontrar notificaciones anteriores.</p></section>
    {resource.loading ? <Spinner /> : resource.error ? <ErrorState message={resource.error} retry={resource.retry} /> : rows(resource.data).length ? <section className="panel notification-list">{rows(resource.data).map(item => <article key={item.id} className={`notification-item ${item.fecha_lectura ? '' : 'unread'}`}><div className="notification-symbol"><Bell size={20} /></div><button className="notification-content" onClick={() => open(item)}><div className="section-heading"><strong>{item.asunto}</strong><small>{date(item.fecha_creacion, true)}</small></div>{supervision && <small className="cell-subtitle">Para {item.destinatario} · @{item.destinatario_usuario}</small>}<p>{item.mensaje}</p><div className="row-actions"><span className="text-muted">{human(item.nombre_tipo || item.tipo)}</span>{!item.fecha_lectura && <span className="badge warning">Sin leer</span>}</div></button>{!supervision && <div className="row-actions">{!item.fecha_lectura && <button className="icon-button" title="Marcar como leída" aria-label={`Marcar ${item.asunto} como leída`} disabled={!!busy} onClick={() => act(`/notificaciones/${item.id}/leer`, item.id, 'Notificación marcada como leída.')}><Check size={18} /></button>}{!item.archivada && <button className="icon-button" title="Archivar" aria-label={`Archivar ${item.asunto}`} disabled={!!busy} onClick={() => act(`/notificaciones/${item.id}/archivar`, item.id, 'Notificación archivada. Puedes consultarla en Archivadas.')}><Archive size={18} /></button>}</div>}</article>)}</section> : <section className="panel"><Empty title={tab === 'pendientes' ? 'Sin mensajes pendientes de lectura' : tab === 'archivadas' ? 'Sin notificaciones archivadas' : supervision ? 'Todavía no hay comunicaciones' : 'Tu bandeja está en calma'} description={supervision ? 'Las confirmaciones de préstamos, reservas y servicios aparecerán al registrarse operaciones para cuentas de la comunidad.' : 'Las confirmaciones de tus préstamos, reservas y servicios aparecerán aquí cuando ocurran.'} /></section>}
    {detail && <Modal title={detail.asunto} subtitle={date(detail.fecha_creacion, true)} onClose={() => setDetail(null)}><div className="modal-body"><p className="message-content">{detail.mensaje}</p><Detail record={detail} exclude={['id', 'asunto', 'mensaje', 'nombre_tipo', 'tipo', 'fecha_creacion']} /></div><div className="modal-footer"><button className="btn primary" onClick={() => setDetail(null)}>Entendido</button></div></Modal>}
    {showPreferences && !supervision && <FormDialog title="Tus preferencias" subtitle="Los avisos obligatorios de tus servicios siempre se conservan en la bandeja interna." fields={[
      { key: 'permite_sistema', label: 'Recibir comunicaciones opcionales dentro del sistema', type: 'check', default: true, full: true },
      { key: 'permite_correo', label: 'Correo electrónico', type: 'check', disabled: true, help: 'Disponible cuando la institución configure el servicio de correo.', full: true },
      { key: 'permite_sms', label: 'Mensajes SMS', type: 'check', disabled: true, help: 'Disponible cuando la institución configure este canal.', full: true },
    ]} initial={preferences.data} onSave={v => api('/preferencias', 'PUT', v)} onClose={() => setShowPreferences(false)} />}
  </>
}

export function NoticesPage() {
  const { gestion, reload, notify } = useUI(), resource = useResource<Row[]>(`/avisos?gestion=${gestion}`)
  const roles = useResource<Row[]>(gestion ? '/roles' : null)
  const [editing, setEditing] = useState<Row | null>(null), [detail, setDetail] = useState<Row | null>(null), [retiring, setRetiring] = useState<Row | null>(null)
  const fields: Field[] = [
    { key: 'titulo', label: 'Título del aviso', required: true, full: true }, { key: 'mensaje', label: 'Contenido', type: 'editor', required: true },
    { key: 'fecha_publicacion', label: 'Fecha de publicación', type: 'datetime-local', required: true, default: `${today()}T08:00` },
    { key: 'fecha_expiracion', label: 'Fecha de expiración', type: 'datetime-local', help: 'Opcional. Debe ser posterior a la publicación.' },
    { key: 'estatus', label: 'Publicación activa', type: 'check', default: true }, { key: 'publico', label: 'Mostrar también a visitantes sin cuenta', type: 'check', default: true },
    { key: 'roles', label: 'Dirigido a estos roles', type: 'multi', options: rows(roles.data).map(r => ({ value: r.nombre, label: human(r.nombre) })), help: 'El aviso se muestra a cualquiera de los roles o grupos elegidos. Un aviso público se muestra a todo visitante.', showWhen: v => !v.publico },
    { key: 'grupos', label: 'Grupos de la comunidad', type: 'multi', options: options('ESTUDIANTE', 'DOCENTE', 'EMPLEADO', 'EXTERNO'), help: 'Selecciona grupos o roles para restringir la audiencia. Sin destinatarios se muestra a todas las cuentas.', showWhen: v => !v.publico },
  ]
  const status = (r: Row) => !r.estatus ? 'RETIRADO' : r.fecha_expiracion && new Date(r.fecha_expiracion) <= new Date() ? 'EXPIRADO' : new Date(r.fecha_publicacion) > new Date() ? 'PROGRAMADO' : 'ACTIVO'
  return <>
    <PageTitle eyebrow="Comunidad ITSPA" title="Avisos de la biblioteca" description="Horarios, actividades y comunicados para nuestra comunidad." actions={gestion && <button className="btn primary" onClick={() => setEditing({})}><Plus size={18} />Publicar aviso</button>} />
    {resource.loading ? <Spinner /> : resource.error ? <ErrorState message={resource.error} retry={resource.retry} /> : gestion ? <DataTable data={rows(resource.data)} columns={[
      { key: 'titulo', label: 'Comunicado', render: r => <div><strong>{r.titulo}</strong><small className="cell-subtitle">{r.publico ? 'Público' : [...(r.roles || []), ...(r.grupos || [])].length ? [...(r.roles || []), ...(r.grupos || [])].map(human).join(', ') : 'Todas las cuentas'}</small></div> },
      { key: 'fecha_publicacion', label: 'Publicación', render: r => date(r.fecha_publicacion, true) }, { key: 'fecha_expiracion', label: 'Expira', render: r => date(r.fecha_expiracion, true) },
      { key: 'estatus', label: 'Estado', render: r => <Badge value={status(r)} /> },
    ]} actions={r => <><button className="icon-button" aria-label="Ver aviso completo" onClick={() => setDetail(r)}><Eye size={17} /></button><button className="icon-button" aria-label="Editar aviso" onClick={() => setEditing(r)}><Pencil size={17} /></button>{r.estatus && <button className="btn secondary small" onClick={() => setRetiring(r)}>Retirar</button>}</>} emptyAction={<button className="btn primary" onClick={() => setEditing({})}><Plus size={17} />Crear primer aviso</button>} /> : <div className="notice-grid">{rows(resource.data).map(r => <article className="panel notice-card" key={r.id}><span className="eyebrow">Aviso institucional</span><h2>{r.titulo}</h2><p className="message-content">{r.mensaje}</p><div className="section-heading"><small>{date(r.fecha_publicacion)}</small><button className="btn ghost small" onClick={() => setDetail(r)}>Ver detalle <ChevronRight size={16} /></button></div></article>)}{!rows(resource.data).length && <section className="panel"><Empty title="No hay avisos vigentes" description="Consulta este espacio para conocer próximas actividades y cambios de horario." /></section>}</div>}
    {editing && <FormDialog title={editing.id ? 'Editar aviso' : 'Publicar un aviso'} subtitle="El aviso se mostrará durante el periodo indicado y conservará el historial de cambios." fields={fields} initial={{ ...editing, fecha_publicacion: localTime(editing.fecha_publicacion) || `${today()}T08:00`, fecha_expiracion: localTime(editing.fecha_expiracion) }} wide onSave={v => api(`/avisos${editing.id ? `/${editing.id}` : ''}`, editing.id ? 'PUT' : 'POST', { ...v, roles: v.publico ? [] : v.roles, grupos: v.publico ? [] : v.grupos })} onClose={() => setEditing(null)} />}
    {retiring && <Confirm title="Retirar aviso" description={`“${retiring.titulo}” dejará de mostrarse. Su registro e historial se conservarán.`} onConfirm={async () => { await api(`/avisos/${retiring.id}`, 'PUT', { ...retiring, estatus: false }); reload(); notify('Aviso retirado de publicación.') }} onClose={() => setRetiring(null)} label="Retirar de publicación" />}
    {detail && <Modal title={detail.titulo} subtitle={`Publicado el ${date(detail.fecha_publicacion, true)}`} onClose={() => setDetail(null)}><div className="modal-body"><p className="message-content">{detail.mensaje}</p>{detail.fecha_expiracion && <p className="form-note">Vigente hasta el {date(detail.fecha_expiracion, true)}.</p>}</div></Modal>}
  </>
}

export function SettingsPage() {
  const { notify } = useUI(), resource = useResource<Row>('/configuracion'), templateResource = useResource<Row>('/plantillas')
  const [params, setParams] = useSearchParams(), { auth } = useSession()
  const [tab, setTab] = useState(params.get('tab') || 'institucion'), [dialog, setDialog] = useState<string | null>(null), [editing, setEditing] = useState<Row>({}), [preview, setPreview] = useState<Row | null>(null)
  useEffect(() => { const next = params.get('tab') || 'institucion'; setTab(['institucion','politicas','catalogos','parametros','plantillas','canales', ...(!auth.accesoDirecto ? ['seguridad'] : [])].includes(next) ? next : 'institucion') }, [params.toString(), auth.accesoDirecto])
  function selectTab(next: string) { setTab(next); const query = new URLSearchParams(params); if (next === 'institucion') query.delete('tab'); else query.set('tab', next); setParams(query) }
  const config = resource.data || {}, institution = config.institucion || {}, templates = templateResource.data || {}
  function open(mode: string, row: Row = {}) { setEditing(row); setDialog(mode) }
  async function showPreview(row: Row) { try { setPreview(await api('/plantillas/previsualizar', 'POST', { asunto: row.asunto, mensaje: row.mensaje })) } catch (e: any) { notify(e.message, true) } }
  const tabs = [{ key: 'institucion', label: 'Institución' }, { key: 'politicas', label: 'Políticas de servicio' }, { key: 'catalogos', label: 'Catálogos' }, { key: 'seguridad', label: 'Acceso y seguridad' }, { key: 'parametros', label: 'Parámetros' }, { key: 'plantillas', label: 'Plantillas' }, { key: 'canales', label: 'Canales' }]
  const institutionFields: Field[] = [
    { key: 'nombre_institucion', label: 'Nombre de la institución', required: true, full: true }, { key: 'nombre_biblioteca', label: 'Nombre de la biblioteca', required: true, full: true },
    { key: 'correo_contacto', label: 'Correo de contacto', type: 'email' }, { key: 'telefono_contacto', label: 'Teléfono de contacto' },
    { key: 'direccion_contacto', label: 'Dirección', full: true }, { key: 'zona_horaria', label: 'Zona horaria', required: true, help: 'Ejemplo: America/Mexico_City.' },
    { key: 'formato_fecha', label: 'Formato de fecha', help: 'Ejemplo: dd/MM/yyyy.' }, { key: 'formato_hora', label: 'Formato de hora', help: 'Ejemplo: HH:mm.' },
    { key: 'descripcion', label: 'Descripción institucional', type: 'textarea' }, { key: 'motivo', label: 'Motivo del cambio', required: true, full: true },
  ]
  const securityFields: Field[] = [
    { key: 'inactividad_maxima_minutos', label: 'Inactividad máxima de sesión (minutos)', type: 'number', required: true, min: 1, max: 1440, default: 30 },
    { key: 'intentos_fallidos_maximos', label: 'Intentos fallidos antes del bloqueo', type: 'number', required: true, min: 1, max: 100, default: 5 },
    { key: 'bloqueo_temporal_minutos', label: 'Duración del bloqueo (minutos)', type: 'number', required: true, min: 1, max: 1440, default: 15 },
  ]
  const parameterFields: Field[] = [
    { key: 'clave', label: 'Clave única', required: true, disabled: !!editing.id, help: 'Letras, números, puntos o guiones; no guardar secretos.' },
    { key: 'tipo_dato', label: 'Tipo de dato', type: 'select', required: true, options: options('TEXTO', 'ENTERO', 'DECIMAL', 'BOOLEANO') },
    { key: 'valor', label: 'Valor', required: true, full: true, help: 'Para booleanos usa true o false; decimales con punto.' }, { key: 'descripcion', label: 'Descripción', type: 'textarea' },
    { key: 'obligatorio', label: 'Parámetro obligatorio', type: 'check' }, { key: 'esencial', label: 'Parámetro esencial (no eliminable)', type: 'check', disabled: !!editing.esencial },
  ]
  const templateFields: Field[] = [
    { key: 'nombre', label: 'Nombre de plantilla', required: true, full: true }, { key: 'tipo_notificacion_id', label: 'Tipo de evento', type: 'select', options: opt(rows(templates.tipos), r => human(r.nombre)), required: true },
    { key: 'canal_id', label: 'Canal', type: 'select', options: opt(rows(templates.canales)), required: true },
    { key: 'asunto', label: 'Asunto', required: true, full: true }, { key: 'mensaje', label: 'Mensaje', type: 'textarea', required: true, help: `Variables disponibles: ${(templates.variables || []).map((v: string) => `{{${v}}}`).join(', ')}. No incluyas contraseñas ni información confidencial.` },
    { key: 'estatus', label: 'Plantilla activa', type: 'check', default: true },
  ]
  async function save(values: Row) {
    if (dialog === 'institucion') { const { motivo, ...institucion } = values; return api('/configuracion', 'PUT', { institucion: { ...institution, ...institucion }, motivo }) }
    if (dialog === 'seguridad') return api('/configuracion', 'PUT', { institucion: institution, seguridad: values, motivo: 'Actualización de política de acceso' })
    if (dialog === 'parametro') return api('/configuracion', 'PUT', { institucion: institution, parametros: [values], motivo: `Parámetro ${values.clave}` })
    if (dialog === 'plantilla') return api(`/plantillas${editing.id ? `/${editing.id}` : ''}`, editing.id ? 'PUT' : 'POST', values)
    if (dialog === 'canal') return api(`/canales/${editing.id}`, 'PUT', values)
  }
  return <>
    <PageTitle eyebrow="Administración institucional" title="Configuración" description="Personaliza la biblioteca, sus políticas de acceso y las comunicaciones del sistema." />
    <div className="tabs" role="tablist" aria-label="Secciones de configuración">{tabs.filter(t => !auth.accesoDirecto || t.key !== 'seguridad').map(t => <button role="tab" aria-selected={tab === t.key} className={tab === t.key ? 'active' : ''} key={t.key} onClick={() => selectTab(t.key)}>{t.label}</button>)}</div>
    {resource.loading ? <Spinner /> : resource.error ? <ErrorState message={resource.error} retry={resource.retry} /> : <>
      {tab === 'institucion' && <div className="stack"><section className="panel"><div className="section-heading"><div><span className="eyebrow">Identidad y contacto</span><h2>{institution.nombre_biblioteca}</h2><p>{institution.nombre_institucion}</p></div><button className="btn primary" onClick={() => open('institucion', institution)}><Pencil size={17} />Editar información</button></div><Detail record={institution} exclude={['id', 'nombre', 'estatus', 'responsable_modificacion_id']} /></section><section className="panel"><div className="section-heading"><h2>Estado de los servicios</h2><Badge value={config.estado_servicios?.base_datos} /></div><Detail record={config.estado_servicios || {}} /></section></div>}
      {tab === 'seguridad' && <section className="panel"><div className="section-heading"><div><span className="eyebrow">Protección de cuentas</span><h2>Sesiones y autenticación</h2><p>Los cambios se aplican a las validaciones de acceso del servidor.</p></div><button className="btn primary" onClick={() => open('seguridad', config.seguridad || {})}><ShieldCheck size={17} />Editar política</button></div>{config.seguridad?.id ? <Detail record={config.seguridad} exclude={['id']} /> : <p className="form-note">Aún no se ha guardado una política institucional. El acceso usa los límites técnicos iniciales: 30 minutos de inactividad, 5 intentos fallidos y 15 minutos de bloqueo. Configura los valores que apruebe la institución.</p>}</section>}
      {tab === 'politicas' && <PolicySettings />}
      {tab === 'catalogos' && <CatalogSettings />}
      {tab === 'parametros' && <div className="stack"><div className="section-heading"><div><h2>Parámetros generales</h2><p>Valores tipados con historial de modificaciones. Las reglas de préstamos y reservas se administran en Políticas.</p></div><button className="btn primary" onClick={() => open('parametro')}><Plus size={17} />Agregar parámetro</button></div><DataTable data={rows(config.parametros)} columns={[{ key: 'clave', label: 'Clave', render: r => <div><strong>{r.clave}</strong><small className="cell-subtitle">{r.descripcion}</small></div> }, { key: 'valor', label: 'Valor' }, { key: 'tipo_dato', label: 'Tipo', render: r => human(r.tipo_dato) }, { key: 'esencial', label: 'Esencial' }, { key: 'fecha_modificacion', label: 'Último cambio', render: r => date(r.fecha_modificacion, true) }]} actions={r => <button className="icon-button" aria-label={`Editar parámetro ${r.clave}`} onClick={() => open('parametro', r)}><Pencil size={17} /></button>} /></div>}
      {(tab === 'plantillas' || tab === 'canales') && (templateResource.loading ? <Spinner /> : templateResource.error ? <ErrorState message={templateResource.error} retry={templateResource.retry} /> : tab === 'plantillas' ? <div className="stack"><div className="section-heading"><div><h2>Mensajes de servicio</h2><p>Personaliza los eventos confirmados con variables autorizadas.</p></div><button className="btn primary" disabled={!rows(templates.tipos).length || !rows(templates.canales).length} onClick={() => open('plantilla')}><Plus size={17} />Crear plantilla</button></div>{!rows(templates.tipos).length && <p className="form-note">Los tipos de evento y el canal interno se registran cuando ocurre el primer préstamo, reserva u otra operación notificable.</p>}<DataTable data={rows(templates.items)} columns={[{ key: 'nombre', label: 'Plantilla' }, { key: 'asunto', label: 'Asunto' }, { key: 'tipo', label: 'Evento', render: r => human(r.tipo) }, { key: 'canal', label: 'Canal' }, { key: 'estatus', label: 'Estado' }]} actions={r => <><button className="icon-button" aria-label="Previsualizar plantilla" onClick={() => showPreview(r)}><Eye size={17} /></button><button className="icon-button" aria-label="Editar plantilla" onClick={() => open('plantilla', r)}><Pencil size={17} /></button></>} /></div> : <div className="stack"><section className="panel"><div className="section-heading"><div><h2>Canales de comunicación</h2><p>La bandeja interna conserva las notificaciones de servicio.</p></div><Mail size={23} /></div><p className="form-note">Correo electrónico y SMS necesitan un proveedor institucional antes de habilitarse. Los envíos internos registran su disponibilidad por separado del estado de lectura.</p></section><DataTable data={rows(templates.canales)} columns={[{ key: 'nombre', label: 'Canal' }, { key: 'descripcion', label: 'Descripción' }, { key: 'estatus', label: 'Estado' }, { key: 'max_reintentos', label: 'Máx. reintentos' }, { key: 'intervalo_reintento_minutos', label: 'Intervalo (min)' }]} actions={r => <button className="icon-button" aria-label="Configurar canal" onClick={() => open('canal', r)}><Settings2 size={17} /></button>} /></div>)}
    </>}
    {dialog && <FormDialog title={{ institucion: 'Información institucional', seguridad: 'Política de acceso', parametro: editing.id ? 'Editar parámetro' : 'Nuevo parámetro', plantilla: editing.id ? 'Editar plantilla' : 'Nueva plantilla', canal: 'Configurar canal' }[dialog] || 'Configuración'} fields={dialog === 'institucion' ? institutionFields : dialog === 'seguridad' ? securityFields : dialog === 'parametro' ? parameterFields : dialog === 'plantilla' ? templateFields : [{ key: 'estatus', label: 'Canal habilitado', type: 'check', default: true }, { key: 'max_reintentos', label: 'Máximo de reintentos', type: 'number', required: true, min: 0, max: 10, default: 0 }, { key: 'intervalo_reintento_minutos', label: 'Intervalo de reintento (minutos)', type: 'number', required: true, min: 0, max: 1440, default: 0 }]} initial={editing} wide={dialog === 'institucion' || dialog === 'plantilla'} onSave={save} onClose={() => setDialog(null)} />}
    {preview && <Modal title="Vista previa de la plantilla" subtitle="Datos de ejemplo. Esta acción no envía ninguna notificación." onClose={() => setPreview(null)}><div className="modal-body"><h3>{preview.asunto}</h3><p className="message-content">{preview.mensaje}</p></div></Modal>}
  </>
}

const communityTypes = options('ESTUDIANTE', 'DOCENTE', 'EMPLEADO', 'EXTERNO')
const splitCsv = (value: any): string[] => String(value || '').split(',').map(s => s.trim()).filter(Boolean)

function PolicySettings() {
  const [type, setType] = useState('prestamo'), [editing, setEditing] = useState<Row | null>(null), [detail, setDetail] = useState<Row | null>(null)
  const resource = useResource<Row[]>(`/politicas/${type}`), catalogs = useResource<Row>('/catalogos')
  const policyTypes = [{ value: 'prestamo', label: 'Préstamos' }, { value: 'reserva-libro', label: 'Reservas de libros' }, { value: 'cubiculo', label: 'Reservas de cubículos' }, { value: 'sancion', label: 'Sanciones' }]
  const incidentTypes = options('RETRASO', 'DANO', 'EXTRAVIO', 'INCUMPLIMIENTO_RESERVACION', 'AUSENCIA_RESERVACION_CUBICULO', 'USO_INADECUADO_CUBICULO')
  let fields: Field[]
  let columns: { key: string; label: string; render?: (r: Row) => any }[]
  if (type === 'prestamo') {
    fields = [
      { key: 'tipo_usuario', label: 'Tipo de usuario', type: 'select', required: true, options: communityTypes },
      { key: 'max_libros', label: 'Máximo de ejemplares activos', type: 'number', required: true, min: 1, max: 1000 },
      { key: 'dias_prestamo', label: 'Duración del préstamo (días)', type: 'number', required: true, min: 1, max: 3650 },
      { key: 'permite_renovacion', label: 'Permitir renovaciones', type: 'check' },
      { key: 'renovaciones_max', label: 'Máximo de renovaciones por ejemplar', type: 'number', required: true, min: 0, max: 100, default: 0, showWhen: v => v.permite_renovacion },
      { key: 'restricciones_material', label: 'Categorías excluidas del préstamo', type: 'multi', options: rows(catalogs.data?.categorias).map(c => ({ value: c.nombre, label: c.nombre })), help: 'Opcional. Las categorías seleccionadas no se podrán prestar con esta política.' },
    ]
    columns = [{ key: 'tipo_usuario', label: 'Usuario', render: r => human(r.tipo_usuario) }, { key: 'max_libros', label: 'Máx. ejemplares' }, { key: 'dias_prestamo', label: 'Duración', render: r => `${r.dias_prestamo} días` }, { key: 'permite_renovacion', label: 'Renovaciones', render: r => r.permite_renovacion ? `Hasta ${r.renovaciones_max}` : 'No permitidas' }]
  } else if (type === 'reserva-libro') {
    fields = [
      { key: 'tiempo_limite', label: 'Plazo para recoger (días)', type: 'number', required: true, min: 1, max: 3650 },
      { key: 'max_reservas', label: 'Máximo de reservas activas por persona', type: 'number', required: true, min: 1, max: 1000 },
      { key: 'dias_anticipacion', label: 'Anticipación máxima (días)', type: 'number', min: 0, max: 3650, help: 'Opcional. Límite de anticipación que autorice la institución.' },
      { key: 'prioridad', label: 'Orden de atención', type: 'select', required: true, default: 'FIFO', options: [{ value: 'FIFO', label: 'Orden de llegada (FIFO)' }, { value: 'PERSONALIZADA', label: 'Prioridad por tipo de usuario' }] },
      { key: 'orden_prioridad', label: 'Tipos de usuario en orden de prioridad', required: true, full: true, showWhen: v => v.prioridad === 'PERSONALIZADA', help: 'Escribe los cuatro tipos una sola vez, separados por comas. Ejemplo de formato: DOCENTE,ESTUDIANTE,EMPLEADO,EXTERNO. La fecha resuelve empates.' },
      { key: 'tipos_usuario_permitidos', label: 'Usuarios autorizados para reservar', type: 'multi', required: true, options: communityTypes },
    ]
    columns = [{ key: 'tiempo_limite', label: 'Recogida', render: r => `${r.tiempo_limite} días` }, { key: 'max_reservas', label: 'Máx. reservas' }, { key: 'reglas_prioridad', label: 'Prioridad', render: r => r.reglas_prioridad === 'FIFO' ? 'Orden de llegada' : splitCsv(r.reglas_prioridad).map(human).join(' → ') }, { key: 'tipos_usuario_permitidos', label: 'Usuarios', render: r => splitCsv(r.tipos_usuario_permitidos).map(human).join(', ') }]
  } else if (type === 'cubiculo') {
    fields = [
      { key: 'tiempo_minimo', label: 'Duración mínima (minutos)', type: 'number', min: 1, max: 1440 },
      { key: 'tiempo_maximo', label: 'Duración máxima (minutos)', type: 'number', required: true, min: 1, max: 1440 },
      { key: 'anticipacion_max', label: 'Anticipación máxima (días)', type: 'number', required: true, min: 0, max: 3650, help: '0 permite únicamente reservaciones para el día actual.' },
      { key: 'max_reservas', label: 'Máximo de reservas activas por persona', type: 'number', required: true, min: 1, max: 1000 },
      { key: 'tolerancia_llegada', label: 'Tolerancia de llegada (minutos)', type: 'number', min: 0, max: 1440, help: 'Si se define, una reserva sin asistencia vence tras este plazo desde su inicio.' },
      { key: 'tipos_usuario_permitidos', label: 'Usuarios autorizados para reservar', type: 'multi', required: true, options: communityTypes },
    ]
    columns = [{ key: 'tiempo_minimo', label: 'Mínimo', render: r => r.tiempo_minimo ? `${r.tiempo_minimo} min` : 'Sin mínimo' }, { key: 'tiempo_maximo', label: 'Máximo', render: r => `${r.tiempo_maximo} min` }, { key: 'anticipacion_max', label: 'Anticipación', render: r => `${r.anticipacion_max} días` }, { key: 'max_reservas', label: 'Máx. reservas' }, { key: 'tolerancia_llegada', label: 'Tolerancia', render: r => r.tolerancia_llegada == null ? 'Hasta fin de reserva' : `${r.tolerancia_llegada} min` }]
  } else {
    fields = [
      { key: 'tipo_incidencia', label: 'Incidencia que origina la sanción', type: 'select', required: true, options: incidentTypes },
      { key: 'tipo_sancion_id', label: 'Tipo de sanción', type: 'select', required: true, options: opt(rows(catalogs.data?.tipos_sancion).filter(t => t.estatus)) },
      { key: 'tipo_usuario', label: 'Aplicar a este tipo de usuario', type: 'select', options: communityTypes, help: 'Vacío: todos los tipos de usuario.' },
      { key: 'dias_suspension', label: 'Duración de restricciones (días)', type: 'number', min: 0, max: 3650, help: 'Vacío o 0 no establece una fecha de levantamiento automática.' },
      { key: 'reglas', label: 'Condiciones de cumplimiento', type: 'textarea', required: true, help: 'Explicación para el responsable y el usuario. No se interpreta como fórmula o código.' },
      { key: 'servicios_restringidos', label: 'Servicios que se restringen', type: 'multi', options: [{ value: 'PRESTAMO', label: 'Préstamos y renovaciones' }, { value: 'RENOVACION', label: 'Sólo renovaciones' }, { value: 'RESERVA_LIBRO', label: 'Reservas de libros' }, { value: 'CUBICULO', label: 'Reservas de cubículos' }], help: 'Préstamos bloquea también las renovaciones. Sólo renovaciones conserva la posibilidad de un préstamo nuevo.' },
      { key: 'multas_autorizadas', label: 'Autorizar multa económica expresamente', type: 'check', full: true },
      { key: 'monto_multa', label: 'Importe fijo de la multa (MXN)', type: 'number', min: 0, step: '0.01', showWhen: v => v.multas_autorizadas, help: 'Importe fijo por la incidencia. No es una tarifa diaria.' },
      { key: 'permite_pagos_parciales', label: 'Permitir pagos parciales', type: 'check', showWhen: v => v.multas_autorizadas },
    ]
    columns = [{ key: 'tipo_incidencia', label: 'Incidencia', render: r => human(r.tipo_incidencia) }, { key: 'tipo_sancion_id', label: 'Sanción', render: r => rows(catalogs.data?.tipos_sancion).find(t => t.id === r.tipo_sancion_id)?.nombre || `#${r.tipo_sancion_id}` }, { key: 'tipo_usuario', label: 'Usuario', render: r => r.tipo_usuario ? human(r.tipo_usuario) : 'Todos' }, { key: 'monto_multa', label: 'Multa fija', render: r => r.multas_autorizadas ? money(r.monto_multa) : 'Sin multa' }, { key: 'dias_suspension', label: 'Restricción', render: r => r.dias_suspension ? `${r.dias_suspension} días` : 'Sin fecha automática' }]
  }
  fields.push({ key: 'estatus', label: 'Política activa', type: 'check', default: true, full: true })
  columns.push({ key: 'estatus', label: 'Estado' })
  function edit(row: Row) {
    setEditing({ ...row, tipos_usuario_permitidos: splitCsv(row.tipos_usuario_permitidos), restricciones_material: splitCsv(row.restricciones_material), servicios_restringidos: splitCsv(row.servicios_restringidos), prioridad: !row.reglas_prioridad || row.reglas_prioridad === 'FIFO' ? 'FIFO' : 'PERSONALIZADA', orden_prioridad: row.reglas_prioridad === 'FIFO' ? '' : row.reglas_prioridad })
  }
  async function save(values: Row) {
    const payload = { ...values }
    if (type === 'prestamo') { payload.restricciones_material = values.restricciones_material?.join(',') || null; if (!payload.permite_renovacion) payload.renovaciones_max = 0 }
    if (type === 'reserva-libro' || type === 'cubiculo') { if (!values.tipos_usuario_permitidos?.length) throw new Error('Selecciona al menos un tipo de usuario autorizado.'); payload.tipos_usuario_permitidos = values.tipos_usuario_permitidos.join(',') }
    if (type === 'reserva-libro') { payload.reglas_prioridad = values.prioridad === 'PERSONALIZADA' ? String(values.orden_prioridad || '').toUpperCase() : 'FIFO'; delete payload.prioridad; delete payload.orden_prioridad }
    if (type === 'sancion') { payload.servicios_restringidos = values.servicios_restringidos?.join(',') || null; if (!values.multas_autorizadas) { payload.monto_multa = null; payload.permite_pagos_parciales = false } }
    return api(`/politicas/${type}${editing?.id ? `/${editing.id}` : ''}`, editing?.id ? 'PUT' : 'POST', payload)
  }
  return <div className="stack"><section className="panel"><div className="section-heading"><div><h2>Condiciones para cada servicio</h2><p>Registra únicamente los límites y condiciones aprobados por la institución.</p></div><button className="btn primary" onClick={() => edit({})}><Plus size={17} />Nueva política</button></div><label className="field"><span>Servicio que deseas configurar</span><select value={type} onChange={e => { setType(e.target.value); setEditing(null) }}>{policyTypes.map(t => <option value={t.value} key={t.value}>{t.label}</option>)}</select></label><p className="form-note">Se utiliza la última política activa que corresponda al usuario. Los préstamos y reservas existentes conservan las condiciones aplicadas al registrarse.</p>{type === 'cubiculo' && <Link to="/cubiculos" className="btn secondary small">Configurar horarios y bloqueos <ChevronRight size={16} /></Link>}</section>
    {resource.loading ? <Spinner /> : resource.error ? <ErrorState message={resource.error} retry={resource.retry} /> : <DataTable key={type} data={rows(resource.data)} columns={columns} actions={r => <><button className="icon-button" aria-label="Ver condiciones completas" onClick={() => setDetail(r)}><Eye size={17} /></button><button className="icon-button" aria-label="Editar política" onClick={() => edit(r)}><Pencil size={17} /></button></>} emptyTitle="Falta configurar este servicio" emptyDescription="Sin una política activa, el sistema informa que se requiere configuración antes de operar." emptyAction={<button className="btn primary" onClick={() => edit({})}><Plus size={17} />Configurar política</button>} />}
    {editing && <FormDialog title={`${editing.id ? 'Editar' : 'Nueva'} política · ${policyTypes.find(t => t.value === type)?.label}`} subtitle="Las unidades se indican en cada campo. No se aplican valores institucionales supuestos." fields={fields} initial={editing} onSave={save} onClose={() => setEditing(null)} wide />}
    {detail && <Modal title={`Política #${detail.id}`} onClose={() => setDetail(null)} wide><div className="modal-body"><Detail record={detail} /></div></Modal>}
  </div>
}

function CatalogSettings() {
  const resource = useResource<Row>('/catalogos'), [type, setType] = useState('autores'), [editing, setEditing] = useState<Row | null>(null)
  const catalogTypes = [{ value: 'autores', label: 'Autores' }, { value: 'categorias', label: 'Categorías' }, { value: 'editoriales', label: 'Editoriales' }, { value: 'idiomas', label: 'Idiomas' }, { value: 'ubicaciones', label: 'Ubicaciones del acervo' }, { value: 'equipamientos', label: 'Equipamiento de cubículos' }, { value: 'tipos_sancion', label: 'Tipos de sanción' }]
  const fields: Field[] = [{ key: 'nombre', label: type === 'autores' ? 'Nombre(s)' : 'Nombre', required: true, full: type !== 'autores' }]
  const columns: { key: string; label: string; render?: (r: Row) => any }[] = [{ key: 'nombre', label: type === 'autores' ? 'Nombre(s)' : 'Nombre' }]
  if (type === 'autores') { fields.push({ key: 'apellidos', label: 'Apellido(s)' }); columns.push({ key: 'apellidos', label: 'Apellidos' }) }
  if (type === 'editoriales') { fields.push({ key: 'pais', label: 'País' }); columns.push({ key: 'pais', label: 'País' }) }
  if (type === 'ubicaciones') { fields.push({ key: 'seccion', label: 'Sección', required: true }, { key: 'estante', label: 'Estante', required: true }, { key: 'nivel', label: 'Nivel' }); columns.push({ key: 'seccion', label: 'Sección' }, { key: 'estante', label: 'Estante' }, { key: 'nivel', label: 'Nivel' }) }
  if (type === 'tipos_sancion') { fields.unshift({ key: 'codigo', label: 'Código único', required: true, help: 'Usa un código institucional. Si la sanción exige reemplazar el material, debe contener REPOSICION.' }); columns.unshift({ key: 'codigo', label: 'Código' }) }
  if (['categorias', 'equipamientos', 'tipos_sancion'].includes(type)) { fields.push({ key: 'descripcion', label: 'Descripción', type: 'textarea' }); columns.push({ key: 'descripcion', label: 'Descripción' }) }
  fields.push({ key: 'estatus', label: 'Registro activo', type: 'check', default: true }); columns.push({ key: 'estatus', label: 'Estado' })
  return <div className="stack"><section className="panel"><div className="section-heading"><div><h2>Datos que organizan la biblioteca</h2><p>Administra las referencias utilizadas por libros, ejemplares, espacios y sanciones.</p></div><button className="btn primary" onClick={() => setEditing({})}><Plus size={17} />Agregar registro</button></div><label className="field"><span>Catálogo</span><select value={type} onChange={e => { setType(e.target.value); setEditing(null) }}>{catalogTypes.map(c => <option key={c.value} value={c.value}>{c.label}</option>)}</select></label><p className="form-note">Puedes desactivar un registro desde Editar para conservar sus relaciones e historial.</p></section>
    {resource.loading ? <Spinner /> : resource.error ? <ErrorState message={resource.error} retry={resource.retry} /> : <DataTable key={type} data={rows(resource.data?.[type])} columns={columns} actions={r => <button className="icon-button" aria-label={`Editar ${r.nombre}`} onClick={() => setEditing(r)}><Pencil size={17} /></button>} emptyTitle={`Todavía no hay registros en ${catalogTypes.find(c => c.value === type)?.label.toLowerCase()}`} emptyAction={<button className="btn primary" onClick={() => setEditing({})}><Plus size={17} />Agregar primer registro</button>} />}
    {editing && <FormDialog title={`${editing.id ? 'Editar' : 'Agregar'} · ${catalogTypes.find(c => c.value === type)?.label}`} fields={fields} initial={editing} onSave={v => api(`/catalogos/${type}${editing.id ? `/${editing.id}` : ''}`, editing.id ? 'PUT' : 'POST', v)} onClose={() => setEditing(null)} wide={type === 'ubicaciones'} />}
  </div>
}

export function AuditPage() {
  const [filters, setFilters] = useState<Row>({ desde: monthStart(), hasta: today(), modulo: '', accion: '', usuario: '' }), [applied, setApplied] = useState(filters), [detail, setDetail] = useState<Row | null>(null)
  const resource = useResource<Row[]>(`/auditoria?${query(applied)}`)
  const { notify } = useUI()
  async function exportAudit(format: string) { try { await download(`/reportes/exportar?${query({ tipo: 'auditoria', desde: applied.desde, hasta: applied.hasta, formato: format })}`, `auditoria-itspa.${format}`); notify('Exportación de auditoría preparada.') } catch (e: any) { notify(e.message, true) } }
  return <>
    <PageTitle eyebrow="Transparencia y seguimiento" title="Bitácora de auditoría" description="Consulta quién realizó cada operación y revisa los cambios administrativos registrados." actions={<><button className="btn secondary" onClick={() => exportAudit('pdf')}><FileText size={17} />PDF</button><button className="btn secondary" onClick={() => exportAudit('xlsx')}><FileSpreadsheet size={17} />Excel</button></>} />
    <section className="panel"><form className="form-grid" onSubmit={e => { e.preventDefault(); setApplied({ ...filters }) }}>
      <label className="field"><span>Desde</span><input type="date" required value={filters.desde} max={filters.hasta} onChange={e => setFilters(v => ({ ...v, desde: e.target.value }))} /></label><label className="field"><span>Hasta</span><input type="date" required value={filters.hasta} min={filters.desde} onChange={e => setFilters(v => ({ ...v, hasta: e.target.value }))} /></label>
      <label className="field"><span>Módulo</span><select value={filters.modulo} onChange={e => setFilters(v => ({ ...v, modulo: e.target.value }))}><option value="">Todos</option>{Array.from({ length: 9 }, (_, i) => `M${String(i + 1).padStart(2, '0')}`).map(m => <option key={m}>{m}</option>)}</select></label>
      <label className="field"><span>Acción exacta</span><input value={filters.accion} placeholder="Ej. MODIFICAR" onChange={e => setFilters(v => ({ ...v, accion: e.target.value }))} /></label>
      <label className="field"><span>Usuario responsable</span><input value={filters.usuario} placeholder="Nombre de usuario" onChange={e => setFilters(v => ({ ...v, usuario: e.target.value }))} /></label><div className="field"><span>&nbsp;</span><button className="btn primary" type="submit"><SlidersHorizontal size={17} />Aplicar filtros</button></div>
    </form></section>
    {resource.loading ? <Spinner /> : resource.error ? <ErrorState message={resource.error} retry={resource.retry} /> : <DataTable data={rows(resource.data)} columns={[{ key: 'fecha', label: 'Fecha y hora', render: r => date(r.fecha, true) }, { key: 'responsable', label: 'Responsable', render: r => r.responsable || (r.operacion_sistema ? 'Sistema' : 'No identificado') }, { key: 'accion', label: 'Acción', render: r => human(r.accion) }, { key: 'modulo', label: 'Módulo' }, { key: 'entidad', label: 'Entidad', render: r => <div>{human(r.entidad)}<small className="cell-subtitle">{r.id_registro ? `Registro #${r.id_registro}` : 'Sin registro asociado'}</small></div> }, { key: 'resultado', label: 'Resultado', render: r => <Badge value={r.resultado} /> }]} actions={r => <button className="icon-button" aria-label="Ver detalles de auditoría" onClick={() => setDetail(r)}><Eye size={17} /></button>} emptyTitle="No hay eventos para estos filtros" emptyDescription="Prueba con un periodo más amplio o con otro usuario o módulo." />}
    <p className="form-note">Se muestran los últimos 500 eventos del filtro. Las exportaciones incluyen todos los eventos del periodo, con un máximo explícito de 10 000 filas.</p>
    {detail && <Modal title={`Evento de auditoría #${detail.id}`} subtitle={date(detail.fecha, true)} onClose={() => setDetail(null)} wide><div className="modal-body stack"><Detail record={detail} exclude={['id', 'cambios']} /><h3>Detalle de cambios</h3><DataTable data={rows(detail.cambios)} columns={[{ key: 'campo', label: 'Campo', render: r => human(r.campo) }, { key: 'valor_anterior', label: 'Valor anterior' }, { key: 'valor_nuevo', label: 'Valor nuevo' }]} searchable={false} emptyTitle="Sin cambios de campos" emptyDescription="Este evento describe una operación y no contiene modificaciones de campos." /></div></Modal>}
  </>
}

export function BackupsPage() {
  const resource = useResource<Row[]>('/respaldos'), { notify, reload } = useUI(), [busy, setBusy] = useState(false), [detail, setDetail] = useState<Row | null>(null)
  async function create() {
    if (busy) return; setBusy(true)
    try { const result = await api('/respaldos', 'POST', {}); setDetail(result); reload(); notify('La instantánea se generó y su integridad fue verificada.') } catch (e: any) { notify(e.message, true); reload() } finally { setBusy(false) }
  }
  async function get(row: Row) { try { await download(`/respaldos/${row.id}/descargar`, `respaldo-itspa-${row.id}.json`); notify('Archivo verificado y listo para descargar.') } catch (e: any) { notify(e.message, true) } }
  return <>
    <PageTitle eyebrow="Protección de información" title="Respaldos de información" description="Genera y descarga una instantánea lógica de los registros de la biblioteca." actions={<button className="btn primary" disabled={busy} onClick={create}><DatabaseBackup size={18} />{busy ? 'Generando instantánea…' : 'Crear instantánea segura'}</button>} />
    <section className="panel"><div className="section-heading"><div><h2>Una copia protegida y verificable</h2><p>El archivo JSON conserva tablas y relaciones y se valida con SHA-256 antes de cada descarga.</p></div><ShieldCheck size={28} /></div><p className="form-note">Por protección de cuentas, esta exportación excluye contraseñas, hashes y tokens. Es una copia de los datos operativos; la recuperación completa de autenticación requiere el procedimiento institucional de respaldo de PostgreSQL.</p></section>
    {resource.loading ? <Spinner /> : resource.error ? <ErrorState message={resource.error} retry={resource.retry} /> : <DataTable data={rows(resource.data)} columns={[{ key: 'id', label: 'Folio', render: r => `#${r.id}` }, { key: 'fecha', label: 'Generado el', render: r => date(r.fecha, true) }, { key: 'responsable', label: 'Responsable' }, { key: 'estado', label: 'Resultado', render: r => <Badge value={r.estado} /> }, { key: 'resultado', label: 'Descripción' }]} actions={r => <><button className="icon-button" aria-label="Ver integridad del respaldo" onClick={() => setDetail(r)}><Eye size={17} /></button>{r.descarga && <button className="btn secondary small" onClick={() => get(r)}><Download size={16} />Descargar</button>}</>} emptyAction={<button className="btn primary" disabled={busy} onClick={create}><Plus size={17} />Crear primera instantánea</button>} emptyTitle="Aún no hay instantáneas" emptyDescription="Crea una copia segura para conservar la información operativa actual." />}
    {detail && <Modal title={`Respaldo #${detail.id}`} onClose={() => setDetail(null)}><div className="modal-body"><Detail record={detail} exclude={['descarga']} /></div><div className="modal-footer">{detail.descarga && <button className="btn primary" onClick={() => get(detail)}><Download size={17} />Descargar archivo verificado</button>}</div></Modal>}
  </>
}

export function IncidentsPage() {
  const resource = useResource<Row[]>('/incidentes'), [editing, setEditing] = useState<Row | null>(null), [detail, setDetail] = useState<Row | null>(null)
  const fields: Field[] = [
    { key: 'descripcion', label: 'Descripción del incidente', type: 'textarea', required: true }, { key: 'gravedad', label: 'Gravedad', type: 'select', required: true, options: options('BAJA', 'MEDIA', 'ALTA', 'CRITICA'), default: 'MEDIA' },
    { key: 'estado', label: 'Estado de atención', type: 'select', required: true, options: options('ABIERTO', 'EN_REVISION', 'RESUELTO'), default: 'ABIERTO' },
    { key: 'observaciones', label: 'Observaciones de seguimiento', type: 'textarea' }, { key: 'acciones_correctivas', label: 'Acciones correctivas', type: 'textarea', help: 'Describe las medidas realizadas. Evita incluir contraseñas o secretos.' },
  ]
  return <>
    <PageTitle eyebrow="Seguridad institucional" title="Incidentes de seguridad" description="Registra situaciones relevantes y da seguimiento a su revisión y resolución." actions={<button className="btn primary" onClick={() => setEditing({})}><Plus size={18} />Registrar incidente</button>} />
    {resource.loading ? <Spinner /> : resource.error ? <ErrorState message={resource.error} retry={resource.retry} /> : <DataTable data={rows(resource.data)} columns={[{ key: 'id', label: 'Folio', render: r => `#${r.id}` }, { key: 'descripcion', label: 'Incidente' }, { key: 'fecha', label: 'Fecha', render: r => date(r.fecha, true) }, { key: 'gravedad', label: 'Gravedad', render: r => <Badge value={r.gravedad} /> }, { key: 'estado', label: 'Atención', render: r => <Badge value={r.estado} /> }, { key: 'responsable', label: 'Responsable' }]} actions={r => <><button className="icon-button" aria-label="Consultar incidente" onClick={() => setDetail(r)}><Eye size={17} /></button><button className="icon-button" aria-label="Actualizar atención del incidente" onClick={() => setEditing(r)}><Pencil size={17} /></button></>} emptyTitle="No hay incidentes registrados" emptyDescription="Los incidentes que registres conservarán su responsable y un historial auditable de modificaciones." />}
    {editing && <FormDialog title={editing.id ? 'Seguimiento del incidente' : 'Registrar incidente'} fields={fields} initial={editing} wide onSave={v => api(`/incidentes${editing.id ? `/${editing.id}` : ''}`, editing.id ? 'PUT' : 'POST', { ...v, cuenta_afectada_id: editing.cuenta_afectada_id || null })} onClose={() => setEditing(null)} />}
    {detail && <Modal title={`Incidente #${detail.id}`} subtitle={date(detail.fecha, true)} onClose={() => setDetail(null)} wide><div className="modal-body"><Detail record={detail} exclude={['id']} /></div><div className="modal-footer"><button className="btn primary" onClick={() => { setEditing(detail); setDetail(null) }}><Pencil size={17} />Actualizar seguimiento</button></div></Modal>}
  </>
}
