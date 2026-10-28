import $ from 'jquery'
import moment from 'moment'
import 'moment/dist/locale/es-mx'
import toastr from 'toastr'
import 'bootstrap/dist/css/bootstrap.min.css'
import 'admin-lte/dist/css/adminlte.min.css'
import 'datatables.net-bs4/css/dataTables.bootstrap4.css'
import 'chosen-js/chosen.css'
import 'daterangepicker/daterangepicker.css'
import 'overlayscrollbars/css/OverlayScrollbars.css'
import 'tempusdominus-bootstrap-4/build/css/tempusdominus-bootstrap-4.min.css'
import 'jqvmap/dist/jqvmap.css'
import 'summernote/dist/summernote-bs4.css'
import 'toastr/build/toastr.min.css'
import bootstrapScript from 'bootstrap/dist/js/bootstrap.bundle.min.js?url'
import adminScript from 'admin-lte/dist/js/adminlte.min.js?url'
import chosenScript from 'chosen-js/chosen.jquery.min.js?url'
import rangeScript from 'daterangepicker/daterangepicker.js?url'
import scrollScript from 'overlayscrollbars/js/jquery.overlayScrollbars.min.js?url'
import timeScript from 'tempusdominus-bootstrap-4/build/js/tempusdominus-bootstrap-4.min.js?url'
import mapScript from 'jqvmap/dist/jquery.vmap.min.js?url'
import worldScript from 'jqvmap/dist/maps/jquery.vmap.world.js?url'
import editorScript from 'summernote/dist/summernote-bs4.min.js?url'
import editorLocale from 'summernote/dist/lang/summernote-es-ES.min.js?url'

// One jQuery instance, loaded before the legacy UMD plugins. No remote CDN at runtime.
Object.assign(window, { $, jQuery: $, moment })
moment.locale('es-mx')
toastr.options = { closeButton: true, progressBar: true, positionClass: 'toast-bottom-right', timeOut: 6500, extendedTimeOut: 1500, escapeHtml: true, preventDuplicates: true }

function script(src: string) {
  return new Promise<void>((resolve, reject) => {
    const element = document.createElement('script')
    element.src = src
    element.onload = () => resolve()
    element.onerror = () => reject(new Error(`No se pudo cargar ${src.split('/').at(-1)}`))
    document.head.appendChild(element)
  })
}
export async function preparePlugins() {
  for (const src of [bootstrapScript, scrollScript, adminScript, chosenScript, rangeScript, timeScript, mapScript, worldScript, editorScript, editorLocale]) await script(src)
}
export function notify(message: string, error = false) { error ? toastr.error(message) : toastr.success(message) }
export { $, moment }
// Legacy libraries do not publish complete TypeScript declarations.
export const plugin = (element: Element | null) => $(element as HTMLElement) as any
