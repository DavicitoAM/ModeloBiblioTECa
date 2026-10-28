import { createContext, useContext, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode, FormEvent } from 'react'
import { createPortal } from 'react-dom'
import { AlertCircle, ArrowDown, ArrowUp, ArrowLeft, ArrowRight, Check, CheckCircle2, ChevronDown, Download, Loader2, Search, SlidersHorizontal, X } from 'lucide-react'
import { api, date, human, rows } from '../lib/api'
import type { Row } from '../lib/api'
import ReactDataTable from 'datatables.net-react'
import DataTablesBootstrap from 'datatables.net-bs4'
import Swal from 'sweetalert2'
import { ChosenSelect, DateTimeField, TextEditor } from './LibraryWidgets'

ReactDataTable.use(DataTablesBootstrap)

export const UIContext = createContext({ refresh: 0, reload: () => {}, notify: (_message: string, _error?: boolean) => {}, gestion: false, admin: false })
export const useUI = () => useContext(UIContext)
export function useResource<T = any>(path: string | null, initial?: T) {
  const { refresh } = useUI(), [data, setData] = useState<T | undefined>(initial), [loading, setLoading] = useState(true), [error, setError] = useState(''), [retry, setRetry] = useState(0)
  useEffect(() => { let alive = true; if (!path) { setLoading(false); return }
    setLoading(true); setError('')
    api<T>(path).then(v => { if (alive) setData(v) }).catch(e => { if (alive) setError(e.message) }).finally(() => { if (alive) setLoading(false) })
    return () => { alive = false }
  }, [path, refresh, retry])
  return { data, loading, error, retry: () => setRetry(v => v + 1), setData }
}
export function Spinner({ label = 'Cargando información…' }: { label?: string }) { return <div className="loading-state" role="status"><Loader2 className="spin" size={22} /><span>{label}</span></div> }
export function ErrorState({ message, retry }: { message: string; retry?: () => void }) { return <div className="error-state" role="alert"><AlertCircle size={23} /><div><strong>No pudimos cargar la información</strong><p>{message}</p></div>{retry && <button className="btn secondary small" onClick={retry}>Reintentar</button>}</div> }
export function Empty({ title = 'Todavía no hay registros', description = 'Cuando agregues información, aparecerá aquí.', action }: { title?: string; description?: string; action?: ReactNode }) { return <div className="empty-state"><div className="empty-icon"><Search size={28} strokeWidth={1.5} /></div><h3>{title}</h3><p>{description}</p>{action}</div> }
export function Badge({ value }: { value: any }) {
  const s = String(value ?? ''), state = s.toUpperCase()
  const good = /^(DISPONIBLE|BUENO|EXCELENTE|DEVUELTO|FINALIZADO|FINALIZADA|ACTIVO|ACTIVA|HABILITADO|RESUELTO|RESUELTA|CUMPLIDA|VERIFICADA|TRUE|CORRECTO|CONECTADA)$/.test(state)
  const bad = /VENCID|RETRASO|EXTRAVIAD|BAJA|CANCELAD|ERROR|DANADO|BLOQUEAD|RECHAZAD|INACTIV/.test(state)
  const pending = /RESERV|PENDIENT|ESPERA|MANTENIMIENTO|EN_REVISION|PRESTADO|CONFIRMADA/.test(state)
  return <span className={`badge ${good ? 'success' : bad ? 'danger' : pending ? 'warning' : 'neutral'}`}><i />{s === 'true' ? 'Activo' : s === 'false' ? 'Inactivo' : human(s) || 'Sin estado'}</span>
}
export function PageTitle({ eyebrow, title, description, actions }: { eyebrow?: string; title: string; description?: string; actions?: ReactNode }) { return <div className="page-heading"><div>{eyebrow && <span className="eyebrow">{eyebrow}</span>}<h1>{title}</h1>{description && <p>{description}</p>}</div><div className="page-actions">{actions}</div></div> }
export function Modal({ title, subtitle, children, onClose, wide = false }: { title: string; subtitle?: string; children: ReactNode; onClose: () => void; wide?: boolean }) {
  const ref = useRef<HTMLDivElement>(null), closeRef = useRef(onClose); closeRef.current = onClose
  useEffect(() => { const previous = document.activeElement as HTMLElement; const oldOverflow = document.body.style.overflow; document.body.style.overflow = 'hidden'
    const focusable = () => Array.from(ref.current?.querySelectorAll<HTMLElement>('button:not(:disabled),input:not(:disabled),select:not(:disabled),textarea:not(:disabled),a[href],[tabindex="0"],[contenteditable="true"]') || []).filter(el => el.getClientRects().length > 0)
    const timer = setTimeout(() => focusable()[0]?.focus(), 50)
    const listener = (e: KeyboardEvent) => { if (e.key === 'Escape') closeRef.current(); if (e.key === 'Tab') {
      const items = focusable()
      if (!items.length) return; const first = items[0], last = items[items.length - 1]
      if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus() } else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus() }
    }}
    document.addEventListener('keydown', listener); return () => { clearTimeout(timer); document.removeEventListener('keydown', listener); document.body.style.overflow = oldOverflow; previous?.focus() }
  }, [])
  return createPortal(<div className="modal-backdrop" onMouseDown={e => { if (e.target === e.currentTarget) onClose() }}><div className={`modal ${wide ? 'wide' : ''}`} role="dialog" aria-modal="true" aria-labelledby="modal-heading" ref={ref}><div className="modal-heading"><div><h2 id="modal-heading">{title}</h2>{subtitle && <p>{subtitle}</p>}</div><button className="icon-button" aria-label="Cerrar ventana" onClick={onClose}><X size={21} /></button></div>{children}</div></div>, document.body)
}
export type Field = { key: string; label: string; type?: 'text' | 'textarea' | 'editor' | 'number' | 'email' | 'password' | 'date' | 'datetime-local' | 'time' | 'select' | 'multi' | 'check'; required?: boolean; options?: { value: any; label: string }[]; help?: string; min?: number; max?: number; step?: string; default?: any; full?: boolean; disabled?: boolean; placeholder?: string; showWhen?: (values: Row) => boolean }
export function FormDialog({ title, subtitle, fields, initial = {}, onSave, onClose, submit = 'Guardar cambios', wide = false }: { title: string; subtitle?: string; fields: Field[]; initial?: Row; onSave: (v: Row) => Promise<unknown>; onClose: () => void; submit?: string; wide?: boolean }) {
  const { notify, reload } = useUI(), [values, setValues] = useState<Row>(() => Object.fromEntries(fields.map(f => [f.key, initial[f.key] ?? f.default ?? (f.type === 'check' ? false : f.type === 'multi' ? [] : '')]))), [busy, setBusy] = useState(false), [error, setError] = useState('')
  async function save(e: FormEvent) { e.preventDefault(); if (busy) return; setBusy(true); setError('')
    const missing = fields.find(f => (!f.showWhen || f.showWhen(values)) && !f.disabled && f.required && (f.type === 'multi' ? !values[f.key]?.length : !String(values[f.key] ?? '').trim()))
    if (missing) { setError(`Completa el campo «${missing.label}».`); setBusy(false); return }
    const payload: Row = {}
    fields.forEach(f => { let v = values[f.key]; if ((f.type === 'number' || f.type === 'select') && v !== '' && v != null && /^\d+(\.\d+)?$/.test(String(v))) v = Number(v); if (v === '' && !f.required) v = null; payload[f.key] = v })
    try { await onSave(payload); reload(); notify('Los cambios se guardaron correctamente.'); onClose() } catch (e: any) { setError(e.message) } finally { setBusy(false) }
  }
  return <Modal title={title} subtitle={subtitle} onClose={() => { if (!busy) onClose() }} wide={wide}><form onSubmit={save}><div className="modal-body"><div className="form-grid">{fields.filter(f => !f.showWhen || f.showWhen(values)).map(f => <label className={`field ${f.full || f.type === 'textarea' || f.type === 'editor' || f.type === 'multi' ? 'full' : ''} ${f.type === 'check' ? 'check-field' : ''}`} key={f.key}>
    {f.type === 'check' ? <><input type="checkbox" checked={!!values[f.key]} onChange={e => setValues(v => ({ ...v, [f.key]: e.target.checked }))} disabled={f.disabled} /><span>{f.label}</span></> : <><span>{f.label}{f.required && <b aria-label="obligatorio"> *</b>}</span>
      {f.type === 'select' || f.type === 'multi' ? <ChosenSelect label={f.label} value={values[f.key]} options={f.options || []} multiple={f.type === 'multi'} disabled={f.disabled} required={f.required} onChange={value => setValues(v => ({ ...v, [f.key]: value }))} /> : f.type === 'editor' ? <TextEditor value={values[f.key] || ''} label={f.label} onChange={value => setValues(v => ({ ...v, [f.key]: value }))} /> : f.type === 'date' || f.type === 'datetime-local' || f.type === 'time' ? <DateTimeField label={f.label} type={f.type} value={values[f.key] || ''} required={f.required} disabled={f.disabled} onChange={value => setValues(v => ({ ...v, [f.key]: value }))} /> : f.type === 'textarea' ? <textarea rows={3} value={values[f.key] || ''} required={f.required} placeholder={f.placeholder} onChange={e => setValues(v => ({ ...v, [f.key]: e.target.value }))} /> : <input type={f.type || 'text'} autoComplete={f.type === 'password' ? 'new-password' : undefined} value={values[f.key] ?? ''} required={f.required} min={f.min} max={f.max} step={f.step} disabled={f.disabled} placeholder={f.placeholder} onChange={e => setValues(v => ({ ...v, [f.key]: e.target.value }))} />}</>}
    {f.help && <small>{f.help}</small>}
  </label>)}</div>{error && <div className="inline-error" role="alert"><AlertCircle size={18} />{error}</div>}</div><div className="modal-footer"><span className="form-note">Los campos con * son obligatorios.</span><button type="button" className="btn secondary" disabled={busy} onClick={onClose}>Cancelar</button><button type="submit" className="btn primary" disabled={busy}>{busy ? <Loader2 className="spin" size={17} /> : <Check size={17} />}{busy ? 'Guardando…' : submit}</button></div></form></Modal>
}
export function Confirm({ title, description, onConfirm, onClose, label = 'Confirmar' }: { title: string; description: string; onConfirm: () => Promise<unknown>; onClose: () => void; label?: string }) {
  const { reload, notify } = useUI(), callbacks = useRef({ onConfirm, onClose }); callbacks.current = { onConfirm, onClose }
  useEffect(() => {
    let mounted = true
    Swal.fire({ title, text: description, icon: 'question', showCancelButton: true, confirmButtonText: label, cancelButtonText: 'Volver', confirmButtonColor: '#2563eb', reverseButtons: true, showLoaderOnConfirm: true, allowOutsideClick: () => !Swal.isLoading(), allowEscapeKey: () => !Swal.isLoading(), preConfirm: async () => {
      try { await callbacks.current.onConfirm(); reload(); notify('La operación se completó correctamente.'); return true } catch (error: any) { const message = document.createElement('span'); message.textContent = error.message || 'No se pudo completar la operación.'; Swal.showValidationMessage(message.innerHTML); return false }
    } }).then(() => { if (mounted) callbacks.current.onClose() })
    return () => { mounted = false; Swal.close() }
  }, [])
  return null
}
export type Column = { key: string; label: string; render?: (r: Row) => ReactNode }
export function DataTable({ data, columns, actions, searchable = true, emptyAction, emptyTitle, emptyDescription }: { data: Row[]; columns: Column[]; actions?: (r: Row) => ReactNode; searchable?: boolean; emptyAction?: ReactNode; emptyTitle?: string; emptyDescription?: string }) {
  const schema = columns.map(c => `${c.key}:${c.label}`).join('|') + !!actions
  const tableColumns = useMemo(() => [...columns.map(c => ({ data: c.key, title: c.label, defaultContent: '' })), ...(actions ? [{ data: null, title: 'Acciones', orderable: false, searchable: false, defaultContent: '' }] : [])], [schema])
  const slots = Object.fromEntries(columns.map((c, index) => [index, (_: unknown, type: string, row: Row) => type === 'display' ? <>{c.render ? c.render(row) : typeof row[c.key] === 'boolean' ? <Badge value={row[c.key]} /> : String(row[c.key] ?? '—')}</> : type === 'filter' ? JSON.stringify(row) : row[c.key] ?? '']))
  if (actions) slots[columns.length] = (_: unknown, type: string, row: Row) => type === 'display' ? <div className="row-actions">{actions(row)}</div> : ''
  if (!data.length) return <section className="table-card"><Empty title={emptyTitle} description={emptyDescription} action={emptyAction} /></section>
  return <section className="table-card datatable-card"><ReactDataTable key={schema} className="table table-hover" data={data} columns={tableColumns} slots={slots} options={{ order: [], pageLength: 10, lengthMenu: [10, 25, 50, 100], searching: searchable, scrollX: true, autoWidth: false, language: { search: 'Buscar:', searchPlaceholder: 'Buscar en esta lista…', lengthMenu: 'Mostrar _MENU_', info: '_START_–_END_ de _TOTAL_ registros', infoEmpty: 'Sin registros', infoFiltered: '(de _MAX_ en total)', zeroRecords: 'Sin coincidencias. Prueba con otro término.', emptyTable: 'Todavía no hay registros', paginate: { first: 'Primera', last: 'Última', next: 'Siguiente', previous: 'Anterior' } } }} /></section>
}
export function Detail({ record, exclude = [] }: { record: Row; exclude?: string[] }) { return <dl className="detail-grid">{Object.entries(record).filter(([k, v]) => !exclude.includes(k) && v !== null && typeof v !== 'object').map(([k, v]) => <div key={k}><dt>{human(k)}</dt><dd>{typeof v === 'boolean' ? <Badge value={v} /> : /fecha/.test(k) ? date(v, String(v).includes('T')) : String(v)}</dd></div>)}</dl> }
export const opt = (items: Row[], label: (r: Row) => string = r => r.nombre || r.titulo || r.codigo || `#${r.id}`) => items.map(r => ({ value: r.id, label: label(r) }))
export const options = (...values: string[]) => values.map(v => ({ value: v, label: human(v) }))
