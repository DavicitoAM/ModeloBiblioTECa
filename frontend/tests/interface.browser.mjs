import test from 'node:test'
import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { setTimeout as delay } from 'node:timers/promises'
import { mkdir } from 'node:fs/promises'
import { createServer } from 'node:net'
import { chromium } from 'playwright'
import { navigation } from '../src/lib/navigation.ts'

// Browser-only fixtures: every API call is intercepted. No application DB is used.
const books = Array.from({ length: 13 }, (_, i) => ({ id: i + 1, titulo: `Libro de prueba UI ${String(i + 1).padStart(2, '0')}`, autores: 'Autor de prueba', isbn: `TEST-UI-${i}`, categorias: 'Pruebas', idioma: i % 2 ? 'Inglés' : 'Español', estatus: true, disponibles: 2, ejemplares: 2, anio_publicacion: 2026 }))
const copies = Array.from({ length: 26 }, (_, i) => ({ id: i + 1, libro_id: i % 13 + 1, codigo_barras: `UI-${String(i + 1).padStart(3, '0')}`, titulo: books[i % 13].titulo, estado: 'DISPONIBLE', estado_conservacion: 'BUENO', estatus: true, ubicacion: 'Ubicación de prueba', estante: 'A' }))
const people = [{ id: 1, tipo: 'ESTUDIANTE', nombre: 'Persona de prueba', apellidos: 'UI', matricula: 'UI-TEST', estatus: true }]
const catalogs = { autores: [{ id: 1, nombre: 'Autor de prueba', estatus: true }], categorias: [{ id: 1, nombre: 'Pruebas', estatus: true }], editoriales: [{ id: 1, nombre: 'Editorial de prueba', estatus: true }], idiomas: [{ id: 1, nombre: 'Español', estatus: true }], ubicaciones: [{ id: 1, nombre: 'Ubicación de prueba', seccion: 'A', estante: 'A', estatus: true }], equipamientos: [], tipos_sancion: [] }
const fixtures = {
  '/auth/estado': { inicializado: true, accesoDirecto: true, usuario: { id: 9001, personaId: 1, usuario: 'ui-test', nombre: 'Contexto de prueba UI', roles: ['ADMINISTRADOR'] } },
  '/inicio': { estadisticas: { titulos: 13, disponibles: 26 }, avisos: [] },
  '/panel': { indicadores: { prestamos_activos: 2, disponibles: 26, reservas_pendientes: 0, visitas_hoy: 3 }, actividad_semanal: Array.from({ length: 7 }, (_, i) => ({ fecha: `2026-10-${20 + i}`, prestamos: [1, 3, 2, 5, 4, 2, 2][i], devoluciones: [0, 2, 1, 3, 2, 1, 1][i] })), prestamos_recientes: [{ id: 1, titulos: books[0].titulo, ejemplares: 1, persona: 'Persona de prueba UI', estado: 'ACTIVO', fecha_vencimiento: '2026-10-30' }], alertas: [], avisos: [] },
  '/catalogo': books, '/ejemplares': copies, '/catalogos': catalogs, '/personas': people,
  '/catalogos-personas': { carreras: [{ id: 1, nombre: 'Carrera de prueba', estatus: true }], departamentos: [], motivos_visita: [] },
  '/configuracion': { institucion: { nombre_biblioteca: 'Biblioteca de prueba UI', nombre_institucion: 'ITSPA' }, parametros: [], estado_servicios: { base_datos: 'CONECTADA' } },
  '/plantillas': { items: [], tipos: [], canales: [] }, '/notificaciones': { items: [], no_leidas: 0 }, '/roles': [], '/avisos': [{ id: 1, titulo: 'Aviso de prueba UI', mensaje: 'Contenido de prueba\nSegundo párrafo', fecha_publicacion: '2026-10-20T08:00:00', estatus: true, publico: true }],
}

test('interfaz administrativa: navegación, tablas, formularios y móvil', { timeout: 180000 }, async t => {
  const port = await new Promise(resolve => { const probe = createServer(); probe.listen(0, '127.0.0.1', () => { const available = probe.address().port; probe.close(() => resolve(available)) }) })
  const url = `http://127.0.0.1:${port}`
  const server = spawn(process.execPath, ['node_modules/vite/bin/vite.js', ...(process.env.UI_USE_BUILD ? ['preview'] : []), '--host', '127.0.0.1', '--port', String(port), '--strictPort'], { stdio: 'ignore', windowsHide: true })
  let browser
  try {
    for (let i = 0; i < 60; i++) { try { if ((await fetch(url)).ok) break } catch {} await delay(300) }
    browser = await chromium.launch({ headless: true, ...(process.env.UI_BROWSER_CHANNEL ? { channel: process.env.UI_BROWSER_CHANNEL } : process.platform === 'win32' ? { channel: 'msedge' } : {}) })
    const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } })
    const errors = [], mutations = []
    let rejectRetirement = true
    let offline = 'proxy', failingModule = ''
    const requests = []
    page.on('pageerror', e => { errors.push(e.message); console.error(e.stack) })
    await page.route('**/api/**', async route => {
      const request = route.request(), path = new URL(request.url()).pathname.replace('/api/app', '')
      requests.push(path)
      if (offline === 'proxy') return route.fulfill({ status: 500, body: '', contentType: 'text/plain' })
      if (offline === 'network') return route.abort('connectionrefused')
      if (path === failingModule) return route.fulfill({ status: 500, json: { error: 'Error de prueba del módulo' } })
      if (request.method() !== 'GET') {
        mutations.push({ path, data: request.postDataJSON() })
        if (path === '/avisos/1' && rejectRetirement) { rejectRetirement = false; return route.fulfill({ status: 409, json: { error: '<img src=x onerror="window.uiInjection=true">Error de prueba' } }) }
        return route.fulfill({ json: { id: 100, ...request.postDataJSON() } })
      }
      if (/^\/catalogo\/\d+$/.test(path)) return route.fulfill({ json: { ...books.find(b => b.id === Number(path.split('/').at(-1))), ejemplares: copies.slice(0, 2) } })
      return route.fulfill({ json: fixtures[path] ?? [] })
    })
    await page.goto(url)
    await t.test('backend apagado: un solo aviso, sin vistas ni indicadores ficticios', async () => {
      await page.getByRole('heading', { name: 'Estamos esperando al servicio de biblioteca', exact: true }).waitFor()
      assert.equal(await page.getByRole('alert').count(), 1)
      assert.equal(await page.locator('.dashboard,.dashboard-kpi,.error-state').count(), 0)
      assert.ok(requests.every(path => path === '/auth/estado'))
      await page.getByRole('button', { name: 'Reintentar conexión', exact: true }).click()
      assert.equal(await page.getByRole('alert').count(), 1)
    })
    await t.test('recupera la sesión y el panel automáticamente al encender backend', async () => {
      offline = ''
      await page.getByRole('heading', { name: 'Panel de inicio', exact: true }).waitFor({ timeout: 15000 })
      assert.equal(await page.locator('.connection-state').count(), 0)
      assert.ok(await page.getByRole('button', { name: 'Nuevo préstamo', exact: true }).count() || await page.getByRole('link', { name: 'Nuevo préstamo', exact: true }).count())
    })
    await t.test('todos los complementos cargan y los iconos se presentan como SVG', async () => {
      const plugins = await page.evaluate(() => ['chosen', 'daterangepicker', 'overlayScrollbars', 'datetimepicker', 'vectorMap', 'summernote', 'PushMenu'].map(name => [name, typeof window.jQuery.fn[name]]))
      for (const [name, kind] of plugins) assert.equal(kind, 'function', name)
      assert.ok(await page.locator('.ion-icon svg').count())
      assert.ok(!(await page.locator('body').innerText()).includes('data:image/svg'))
      assert.equal(await page.locator('.mobile-menu').isVisible(), false)
    })
    await mkdir('../.itspa-tmp/frontend-2026-10-26', { recursive: true })
    await page.screenshot({ path: '../.itspa-tmp/frontend-2026-10-26/inicio-prueba.png', fullPage: true })
    await t.test('menú plegable persistente y búsqueda de secciones', async () => {
      await page.getByRole('button', { name: 'Plegar menú lateral', exact: true }).click()
      assert.ok(await page.locator('.app-shell').evaluate(el => el.classList.contains('sidebar-compact')))
      await page.reload()
      await page.getByRole('button', { name: 'Expandir menú lateral', exact: true }).click()
      await page.getByLabel('Filtrar secciones del menú').fill('equipamiento')
      assert.ok(await page.getByRole('link', { name: 'Equipamiento', exact: true }).isVisible())
      await page.getByLabel('Filtrar secciones del menú').fill('')
      await page.keyboard.press('Control+k')
      await page.getByRole('dialog').waitFor()
      await page.keyboard.press('Escape')
      assert.equal(await page.getByRole('dialog').count(), 0)
    })
    await t.test('todas las vistas del menú y pestañas son navegables', async () => {
      for (const item of navigation.flatMap(g => g.items).filter(i => i.to !== '/cuentas')) {
        await page.evaluate(to => { window.location.hash = to }, item.to)
        await page.locator('main h1').waitFor()
        await delay(120)
        assert.equal(await page.getByRole('heading', { name: 'No encontramos esta página' }).count(), 0, item.to)
        const tab = new URLSearchParams(item.to.split('?')[1]).get('tab')
        if (tab) assert.ok(await page.locator('button[aria-selected="true"],.tabs button.active').count(), item.to)
      }
      assert.deepEqual(errors, [])
    })
    await t.test('DataTables busca, pagina y conserva acciones React', async () => {
      await page.evaluate(() => { window.location.hash = '/inventario' })
      await page.locator('.dt-search input').waitFor()
      assert.match(await page.locator('.dt-info').innerText(), /26/)
      await page.locator('.dt-search input').fill('UI-026')
      await page.waitForFunction(() => document.querySelector('.dt-info')?.textContent?.includes('1–1'))
      assert.ok((await page.locator('.dt-scroll-body tbody').innerText()).includes('UI-026'))
      await page.getByRole('button', { name: 'Actualizar', exact: true }).click()
      await page.getByRole('dialog').waitFor()
      assert.ok(await page.locator('.chosen-container').count())
      await page.getByRole('button', { name: 'Cerrar ventana', exact: true }).click()
      await page.screenshot({ path: '../.itspa-tmp/frontend-2026-10-26/inventario-prueba.png', fullPage: true })
    })
    await t.test('catálogo filtra idiomas y la ficha conserva sus acciones', async () => {
      await page.evaluate(() => { window.location.hash = '/catalogo' })
      await page.getByLabel('Filtrar catálogo por idioma').selectOption('Inglés')
      assert.equal(await page.locator('.catalog-grid .book-card').count(), 6)
      await page.locator('.catalog-grid .book-card').first().click()
      await page.getByRole('dialog').waitFor()
      assert.ok(await page.getByRole('button', { name: 'Reservar este libro' }).isVisible())
      await page.getByRole('button', { name: 'Cerrar ventana' }).click()
    })
    await t.test('Chosen múltiple, Tempus Dominus y validación del formulario', async () => {
      await page.evaluate(() => { window.location.hash = '/catalogo?nuevo=1' })
      await page.getByRole('dialog').waitFor()
      await page.getByRole('textbox', { name: 'Título de la obra' }).fill('Registro de prueba UI')
      const author = page.locator('.field').filter({ has: page.locator('select[aria-label="Autoría"]') })
      await author.locator('.chosen-container').click()
      await author.locator('.active-result').filter({ hasText: 'Autor de prueba' }).click()
      assert.ok(await author.locator('.search-choice').count())
      await page.getByRole('button', { name: 'Crear libro', exact: true }).click()
      await page.waitForFunction(() => !document.querySelector('[role="dialog"]'))
      assert.deepEqual(mutations.at(-1).data.autores_ids, [1])
      assert.ok(await page.locator('#toast-container .toast-success').count())
      await page.evaluate(() => { window.location.hash = '/inventario' })
      await page.getByRole('button', { name: 'Agregar ejemplar', exact: true }).first().click()
      const dateInput = page.getByRole('textbox', { name: 'Fecha de adquisición', exact: true })
      await dateInput.fill('fecha inválida')
      assert.equal(await dateInput.evaluate(el => el.validity.valid), false)
      await dateInput.fill('2026-10-26')
      assert.equal(await dateInput.evaluate(el => el.validity.valid), true)
      await page.getByRole('button', { name: 'Abrir calendario: Fecha de adquisición', exact: true }).click()
      assert.ok(await page.locator('.bootstrap-datetimepicker-widget').isVisible())
      await page.keyboard.press('Escape')
    })
    await t.test('periodos de reporte, editor y mapa funcionan', async () => {
      await page.evaluate(() => { window.location.hash = '/reportes' })
      await page.getByLabel('Periodo del reporte').click()
      assert.ok(await page.locator('.daterangepicker').isVisible())
      await page.locator('.daterangepicker .ranges li').filter({ hasText: 'Personalizar' }).click()
      await page.locator('.daterangepicker .cancelBtn').click()
      await page.evaluate(() => { window.location.hash = '/avisos' })
      await page.getByRole('button', { name: 'Publicar aviso', exact: true }).click()
      await page.locator('.note-editable').fill('Contenido de prueba UI\nSegundo párrafo')
      assert.ok(await page.locator('.note-toolbar').isVisible())
      await page.getByRole('button', { name: 'Cerrar ventana' }).click()
      await page.getByRole('button', { name: 'Editar aviso', exact: true }).click()
      assert.match(await page.locator('.note-editable').innerText(), /Contenido de prueba\nSegundo párrafo/)
      await page.getByRole('button', { name: 'Cerrar ventana' }).click()
      await page.evaluate(() => { window.location.hash = '/ayuda' })
      await page.locator('.campus-map svg').waitFor()
      assert.ok(await page.locator('.campus-map path').count())
      const count = await page.evaluate(() => window.jQuery._data(window, 'events').resize.length)
      await page.evaluate(() => { window.location.hash = '/catalogo' })
      await page.locator('.catalog-grid').waitFor()
      await page.evaluate(() => { window.location.hash = '/ayuda' })
      await page.locator('.campus-map svg').waitFor()
      assert.equal(await page.evaluate(() => window.jQuery._data(window, 'events').resize.length), count)
    })
    await t.test('SweetAlert2 conserva confirmación, errores escapados y reintento', async () => {
      await page.evaluate(() => { window.location.hash = '/avisos' })
      await page.getByRole('button', { name: 'Retirar', exact: true }).click()
      await page.getByRole('button', { name: 'Retirar de publicación', exact: true }).click()
      await page.locator('.swal2-validation-message').waitFor()
      assert.equal(await page.locator('.swal2-validation-message img').count(), 0)
      assert.ok((await page.locator('.swal2-validation-message').innerText()).includes('<img'))
      assert.equal(await page.evaluate(() => window.uiInjection), undefined)
      await page.getByRole('button', { name: 'Retirar de publicación', exact: true }).click()
      await page.waitForFunction(() => !document.querySelector('.swal2-container'))
      assert.equal(mutations.at(-1).data.estatus, false)
    })
    await t.test('pérdida de conexión durante el uso y recuperación sin recargar la página', async () => {
      offline = 'network'
      await page.evaluate(() => { window.location.hash = '/inventario' })
      await page.locator('.connection-state').waitFor()
      assert.equal(await page.getByRole('alert').count(), 1)
      assert.equal(await page.locator('.datatable-card').count(), 0)
      offline = ''
      await page.evaluate(() => { window.dispatchEvent(new Event('online')) })
      await page.getByRole('heading', { name: 'Inventario del acervo', exact: true }).waitFor()
      await page.locator('.dt-search input').waitFor()
      assert.equal(await page.locator('.connection-state').count(), 0)
    })
    await t.test('errores reales del módulo tienen un aviso propio sin simular datos vacíos', async () => {
      failingModule = '/inicio'
      await page.evaluate(() => { window.location.hash = '/' })
      await page.getByText('Error de prueba del módulo', { exact: true }).waitFor()
      assert.equal(await page.getByRole('alert').count(), 1)
      assert.equal(await page.locator('.connection-state,.dashboard-kpi').count(), 0)
      failingModule = ''
      await page.getByRole('button', { name: 'Reintentar', exact: true }).click()
      await page.locator('.dashboard-kpi').first().waitFor()
    })
    await t.test('móvil: menú accesible, cierre y ausencia de desbordamientos', async () => {
      await page.setViewportSize({ width: 390, height: 844 })
      await page.evaluate(() => { window.location.hash = '/' })
      await page.getByRole('button', { name: 'Abrir menú de navegación', exact: true }).click()
      await page.getByRole('button', { name: 'Colección', exact: true }).focus()
      await page.keyboard.press('Escape')
      assert.equal(await page.locator('.sidebar-overlay').count(), 0)
      assert.ok(await page.getByRole('button', { name: 'Abrir menú de navegación' }).evaluate(el => el === document.activeElement))
      assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), 'sin desbordamiento horizontal')
      await page.screenshot({ path: '../.itspa-tmp/frontend-2026-10-26/movil-prueba.png', fullPage: true })
    })
    assert.deepEqual(errors, [])
  } finally { await browser?.close(); server.kill() }
})
