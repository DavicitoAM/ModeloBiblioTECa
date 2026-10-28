import { useEffect, useId, useRef } from 'react'
import { CalendarDays } from 'lucide-react'
import { bookOutline, gridOutline, peopleOutline, notificationsOutline, settingsOutline, libraryOutline, swapHorizontalOutline, chevronForwardOutline } from 'ionicons/icons'
import { $, moment, plugin } from '../lib/plugins'

const icons = { book: bookOutline, dashboard: gridOutline, people: peopleOutline, notifications: notificationsOutline, settings: settingsOutline, library: libraryOutline, circulation: swapHorizontalOutline, arrow: chevronForwardOutline }
export function IonIcon({ name, size = 22 }: { name: keyof typeof icons; size?: number }) {
  const svg = decodeURIComponent(icons[name].replace(/^data:image\/svg\+xml;utf8,/, ''))
  return <span className="ion-icon" style={{ width: size, height: size }} aria-hidden="true" dangerouslySetInnerHTML={{ __html: svg }} />
}

type Choice = { value: any; label: string }
export function ChosenSelect({ value, options, onChange, multiple = false, disabled = false, label, required = false }: { value: any; options: Choice[]; onChange: (value: any) => void; multiple?: boolean; disabled?: boolean; label: string; required?: boolean }) {
  const ref = useRef<HTMLSelectElement>(null), callback = useRef(onChange)
  callback.current = onChange
  useEffect(() => {
    const select = plugin(ref.current)
    if (!select.chosen) return
    select.chosen({ width: '100%', search_contains: true, placeholder_text_single: 'Selecciona una opción', placeholder_text_multiple: 'Selecciona una o varias opciones', no_results_text: 'Sin coincidencias para', allow_single_deselect: !required })
    select.on('change.itspa', () => {
      const selected = select.val()
      callback.current(multiple ? (selected || []).map((v: string) => options.find(o => String(o.value) === v)?.value ?? v) : selected || '')
    })
    const search = select.next('.chosen-container').find('input')
    search.attr('aria-label', `Buscar en ${label}`).attr('autocomplete', 'off')
    select.next('.chosen-container').find('a').attr('aria-label', label)
    return () => { select.off('.itspa'); select.chosen('destroy') }
  }, [multiple, JSON.stringify(options), label, required])
  useEffect(() => { plugin(ref.current).val(value ?? (multiple ? [] : '')).trigger('chosen:updated') }, [JSON.stringify(value), disabled, JSON.stringify(options)])
  return <div className="chosen-field"><select ref={ref} aria-label={label} multiple={multiple} value={multiple ? (value || []).map(String) : value ?? ''} disabled={disabled} onChange={e => onChange(multiple ? Array.from(e.target.selectedOptions).map(o => options.find(v => String(v.value) === o.value)?.value ?? o.value) : e.target.value)}>{!multiple && <option value="">Selecciona una opción</option>}{options.map(o => <option key={String(o.value)} value={o.value}>{o.label}</option>)}</select></div>
}

export function DateTimeField({ value, onChange, label, type = 'datetime-local', required, disabled }: { value: string; onChange: (value: string) => void; label: string; type?: 'date' | 'datetime-local' | 'time'; required?: boolean; disabled?: boolean }) {
  const ref = useRef<HTMLInputElement>(null), callback = useRef(onChange), id = `date-${useId().replaceAll(':', '')}`
  callback.current = onChange
  const format = type === 'date' ? 'YYYY-MM-DD' : type === 'time' ? 'HH:mm' : 'YYYY-MM-DD[T]HH:mm'
  useEffect(() => {
    const input = plugin(ref.current)
    if (!input.datetimepicker) return
    input.datetimepicker({ format, locale: 'es-mx', useCurrent: false, keepInvalid: true, useStrict: true, stepping: 1, allowInputToggle: true, icons: { time: 'td-clock', date: 'td-calendar', up: 'td-up', down: 'td-down', previous: 'td-prev', next: 'td-next', today: 'td-calendar', clear: 'td-clear', close: 'td-close' } })
    input.on('change.datetimepicker', (event: any) => { if (event.date && !event.date.isValid()) { ref.current?.setCustomValidity('Introduce una fecha y hora válidas.'); return }; ref.current?.setCustomValidity(''); callback.current(event.date ? event.date.format(format) : '') })
    return () => { input.off('change.datetimepicker'); input.datetimepicker('destroy') }
  }, [format])
  useEffect(() => {
    const input = plugin(ref.current), instance = input.data('datetimepicker')
    const valid = !value || moment(value, format, true).isValid()
    ref.current?.setCustomValidity(valid ? '' : 'Introduce una fecha y hora válidas.')
    if (valid && instance && (instance.date()?.format(format) || '') !== (value || '')) instance.date(value ? moment(value, format, true) : null)
  }, [value, format])
  return <div className="datetime-field"><input ref={ref} id={id} type="text" aria-label={label} className="datetimepicker-input" data-target={`#${id}`} value={value || ''} placeholder={type === 'time' ? 'HH:mm' : type === 'date' ? 'AAAA-MM-DD' : 'AAAA-MM-DDTHH:mm'} required={required} disabled={disabled} onChange={e => { e.target.setCustomValidity(e.target.value && !moment(e.target.value, format, true).isValid() ? 'Introduce una fecha y hora válidas.' : ''); onChange(e.target.value) }} /><button type="button" className="icon-button" disabled={disabled} aria-label={`Abrir calendario: ${label}`} onClick={() => plugin(ref.current).datetimepicker('toggle')}><CalendarDays size={18} /></button></div>
}

export function DateRangeField({ from, to, onChange }: { from: string; to: string; onChange: (from: string, to: string) => void }) {
  const ref = useRef<HTMLInputElement>(null), callback = useRef(onChange)
  callback.current = onChange
  useEffect(() => {
    const input = plugin(ref.current)
    if (!input.daterangepicker) return
    input.daterangepicker({ startDate: moment(from), endDate: moment(to), opens: 'left', autoUpdateInput: false, locale: { format: 'DD/MM/YYYY', applyLabel: 'Aplicar', cancelLabel: 'Cancelar', fromLabel: 'Desde', toLabel: 'Hasta', customRangeLabel: 'Personalizar', daysOfWeek: ['Do', 'Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sá'], monthNames: moment.months(), firstDay: 1 }, ranges: { 'Hoy': [moment(), moment()], 'Últimos 7 días': [moment().subtract(6, 'days'), moment()], 'Este mes': [moment().startOf('month'), moment()] } })
    input.on('apply.daterangepicker', (_: unknown, picker: any) => callback.current(picker.startDate.format('YYYY-MM-DD'), picker.endDate.format('YYYY-MM-DD')))
    return () => { input.off('apply.daterangepicker'); input.data('daterangepicker')?.remove() }
  }, [])
  useEffect(() => { const picker = plugin(ref.current).data('daterangepicker'); picker?.setStartDate(moment(from)); picker?.setEndDate(moment(to)) }, [from, to])
  return <label className="field range-field"><span>Periodo del reporte</span><div className="input-icon"><CalendarDays size={18} /><input ref={ref} aria-label="Periodo del reporte" value={`${moment(from).format('DD/MM/YYYY')} — ${moment(to).format('DD/MM/YYYY')}`} readOnly /></div></label>
}

export function TextEditor({ value, onChange, label }: { value: string; onChange: (value: string) => void; label: string }) {
  const ref = useRef<HTMLDivElement>(null), callback = useRef(onChange)
  callback.current = onChange
  useEffect(() => {
    const editor = plugin(ref.current)
    if (!editor.summernote) return
    const initial = document.createElement('div')
    initial.textContent = value || ''
    editor.html(initial.innerHTML.replaceAll('\n', '<br>'))
    editor.summernote({ lang: 'es-ES', height: 170, disableDragAndDrop: true, shortcuts: false, toolbar: [['history', ['undo', 'redo']], ['view', ['fullscreen']]], callbacks: {
      onInit: () => editor.next('.note-editor').find('.note-editable').attr('aria-label', label).attr('role', 'textbox').attr('aria-multiline', 'true'),
      onChange: () => { const editable = editor.next('.note-editor').find('.note-editable')[0]; if (editable) callback.current(editable.innerText.replace(/\n{3,}/g, '\n\n').trimEnd()) },
      onPaste: (e: ClipboardEvent) => { e.preventDefault(); editor.summernote('insertText', (e as any).originalEvent?.clipboardData?.getData('text/plain') || e.clipboardData?.getData('text/plain') || '') },
    } })
    return () => { editor.summernote('destroy') }
  }, [])
  return <div className="text-editor"><div ref={ref} /><small>El contenido se publica como texto. Puedes deshacer, rehacer y ampliar el editor.</small></div>
}

export function CampusMap() {
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const map = plugin(ref.current)
    if (!map.vectorMap) return
    // jqvmap 1.5 has no destroy API and registers an unnamespaced resize handler.
    // Snapshot this pinned jQuery version's handlers to remove only this map's.
    const handlers = () => ((($ as any)._data(window, 'events')?.resize || []) as { handler: (...args: any[]) => void }[]).map(event => event.handler)
    const previous = new Set(handlers())
    map.vectorMap({ map: 'world_en', backgroundColor: 'transparent', color: '#dde7f3', hoverColor: '#afc9ee', selectedColor: '#2671df', selectedRegions: ['mx'], enableZoom: true, showTooltip: true, onRegionClick: (event: Event) => event.preventDefault(), onLabelShow: (event: Event, label: any, code: string) => { if (code === 'mx') label.text('México · Campus Pátzcuaro, Michoacán'); else event.preventDefault() } })
    const added = handlers().filter(handler => !previous.has(handler)), label = map.data('mapObject')?.label
    return () => { added.forEach(handler => $(window).off('resize', handler)); label?.remove(); map.off('.jqvmap').removeData('mapObject').empty() }
  }, [])
  return <section className="panel campus-map"><div><span className="eyebrow">Nuestra biblioteca</span><h2>Conocimiento desde Pátzcuaro</h2><p>Instituto Tecnológico Superior de Pátzcuaro.<br />Michoacán, México.</p><span className="badge neutral">Campus Pátzcuaro</span></div><div ref={ref} role="img" aria-label="Mapa mundial con México resaltado; campus en Pátzcuaro, Michoacán" /></section>
}
