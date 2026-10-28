import test from 'node:test'
import assert from 'node:assert/strict'
import { visibleNavigation, matchesNavigation } from '../src/lib/navigation.ts'

const links = (roles, direct = false) => visibleNavigation(roles, direct).flatMap(group => group.items.map(item => item.to))

test('la comunidad encuentra servicios personales y avisos sin administración', () => {
  const allowed = links(['LECTOR'])
  for (const route of ['/catalogo', '/prestamos', '/reservas', '/cubiculos', '/sanciones', '/notificaciones', '/avisos']) assert.ok(allowed.includes(route), route)
  for (const route of ['/personas', '/inventario', '/visitas', '/reportes', '/configuracion', '/auditoria', '/respaldos', '/cuentas']) assert.ok(!allowed.includes(route), route)
})
test('bibliotecario accede a operación completa sin configuración ni credenciales', () => {
  const allowed = links(['BIBLIOTECARIO'])
  for (const route of ['/personas', '/inventario', '/visitas', '/reportes', '/cubiculos?tab=bloqueos']) assert.ok(allowed.includes(route), route)
  assert.ok(!allowed.includes('/configuracion'))
  assert.ok(!allowed.includes('/cuentas'))
  assert.ok(!allowed.includes('/auditoria'))
})
test('administración local conserva todos los módulos sin pantalla de cuentas', () => {
  const allowed = links(['ADMINISTRADOR'], true)
  for (const route of ['/configuracion', '/configuracion?tab=politicas', '/configuracion?tab=plantillas', '/auditoria', '/respaldos', '/incidentes', '/reportes']) assert.ok(allowed.includes(route), route)
  assert.ok(!allowed.includes('/cuentas'))
  assert.ok(links(['ADMINISTRADOR']).includes('/cuentas'))
})
test('las rutas a agenda y políticas seleccionan una única opción del menú', () => {
  assert.equal(matchesNavigation('/cubiculos', '/cubiculos', '?tab=reservas'), false)
  assert.equal(matchesNavigation('/cubiculos?tab=reservas', '/cubiculos', '?tab=reservas'), true)
  assert.equal(matchesNavigation('/cubiculos?tab=horarios', '/cubiculos', '?tab=reservas'), false)
  assert.equal(matchesNavigation('/configuracion', '/configuracion', '?tab=politicas'), false)
  assert.equal(matchesNavigation('/configuracion?tab=politicas', '/configuracion', '?tab=politicas'), true)
  assert.equal(matchesNavigation('/catalogo', '/catalogo', '?q=algebra'), true)
})
