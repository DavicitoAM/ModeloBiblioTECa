# Interfaz y recorridos — 21 de octubre de 2026

El pedido actual amplía expresamente la práctica de modelo a una interfaz operativa. La indicación posterior elimina el login en esta versión; la autenticación institucional se resolverá al integrar el sistema. El rework del 21 de octubre mantiene ese acceso directo, reorganiza las acciones y adapta el menú al rol recibido del servidor. No introduce un selector ficticio de roles.

## Diseño vigente

- Logo institucional destacado en un bloque propio; recursos `logo.png`, `header.png`, `footer.png` y `Background.jpg` del proyecto.
- Navegación azul con apartados desplegables. Abre el apartado de la ruta activa, conserva las preferencias de plegado y ofrece menú lateral en móvil con cierre mediante Escape y control del foco.
- Inicio con búsqueda, indicadores reales, acciones de préstamo/devolución/reserva/espacio, préstamos recientes, pendientes y avisos.
- Componentes mayores: acciones principales de 48 px en escritorio; formularios de 48–49 px, títulos destacados y tablas con más espacio. Se retiran las etiquetas decorativas sobre los títulos, conservando etiquetas de formulario, unidades, estados y ayudas necesarias.
- Noto Sans para lectura y Patria para títulos destacados. Azul `#1B396A`, gris `#807E82`, verde `#1e5b4f`, guinda `#9b2247` y dorado `#a57f2c` / `#e6d194`.
- Los atajos abren la sección concreta: agenda, horarios, bloqueos, políticas, plantillas, nuevo préstamo, devolución y detalle de un préstamo. El buscador también encuentra secciones con Ctrl+K.

## Menú por rol

| Apartado | Opciones | Acceso |
|---|---|---|
| Inicio | Resumen y acciones frecuentes | Contexto operativo |
| Colección | Catálogo; ejemplares y ubicaciones | Catálogo para todos; inventario para gestión |
| Circulación | Préstamos/devoluciones; reservas; incidencias/sanciones | Lector sobre sus registros; gestión sobre la operación |
| Espacios de estudio | Cubículos; agenda; horarios; bloqueos | Bloqueos solo gestión |
| Comunidad | Personas/perfiles; entradas/salidas; cuentas/permisos | Gestión; cuentas solo administrador en modo integrado |
| Comunicaciones | Notificaciones; avisos; plantillas/canales | Plantillas/canales solo administrador |
| Administración | Reportes; políticas; configuración; auditoría; respaldos; incidentes | Reportes para gestión; resto administrador |

En el modo directo autorizado, la identidad técnica tiene contexto de gestión y se oculta la administración de credenciales humanas. Las restricciones del servidor siguen siendo la autoridad de cada operación. El centro de comunicaciones supervisa las entregas sin marcar como leídos los mensajes de otras personas.

## Cobertura funcional y fuentes

Las páginas se indican como **impresa / física del visor**. La interfaz utiliza la API; no modifica entidades, cardinalidades ni migraciones.

| Módulo y fuente | Vistas y recorridos |
|---|---|
| M01, pp. 8–14 / 9–15; RF-M01-02/03/04, 38–45; RN-M01-01/03/17 | Comunidad, subtipos, carreras, departamentos, motivos de visita, entradas/salidas y corrección auditada |
| M02, pp. 15–21 / 16–22; RF-M02-02/49; RN-M02-11/12/16/22 | Búsqueda, filtros, ficha bibliográfica, autores/categorías/editorial/idioma, ejemplares, conservación y ubicación |
| M03, pp. 22–28 / 23–29; RF-M03-63; RN-M03-03/13/14 | Préstamo múltiple, políticas por perfil, devolución parcial/final, renovación e historial |
| M04, pp. 29–35 / 30–36; RF-M04-16/26; RN-M04-10/11/27 | Reservar, esperar, asignar, cancelar, plazo de recogida y entrega que crea préstamo |
| M05, pp. 36–43 / 37–44; RF-M05-04/60/62; RN-M05-06/10/15 | Cubículos, equipamiento, agenda, horarios especiales/semanales, bloqueos, asistencia, uso e inasistencia |
| M06, pp. 44–51 / 45–52; RF-M06-28/29/35/61/67; RN-M06-19/20/26/28 | Incidencia, política sancionadora, restricciones, multa, pagos parciales, reposición y resolución |
| M07, pp. 52–59 / 53–60; RN-M07-10/14/24/25 | Comunicaciones, lectura/archivo propios, supervisión local, avisos por audiencia, plantillas y canales |
| M08, pp. 60–67 / 61–68; RN-M08-09/25 | Panel y reportes por periodo; exportación PDF, XLSX, CSV y JSON |
| M09, pp. 68–76 / 69–77; RF-M09-29/43/46/62; RN-M09-08/09/14/22/32 | Configuración, parámetros tipados, auditoría, incidentes e instantáneas JSON verificadas |

## Preparación y límites que conserva esta versión

Las políticas y horarios deben introducir los valores aprobados por la biblioteca; no se precargan duraciones, montos ni límites institucionales. La carga optativa incorpora ocho fichas académicas y dieciséis copias rotuladas como demostración, sin datos escolares personales.

Los canales externos necesitan un proveedor real de correo/SMS. Los respaldos son instantáneas de negocio sin contraseñas y con SHA-256; no incluyen restauración destructiva ni sustituyen un respaldo administrativo completo de PostgreSQL. Los vencimientos de reservas se procesan al operar/consultar su módulo. La autenticación del sistema externo queda pendiente de la integración que solicitó el responsable.

El nuevo punto de entrada es `frontend/src/main.tsx` → `App.tsx`. Los prototipos previos no importados se conservan sin usarlos como rutas vigentes. `design.css` define la capa visual vigente sobre los componentes operativos existentes de `styles.css`.

## Verificación

- `npm test`: cuatro pruebas de permisos del menú y selección inequívoca de rutas con pestañas.
- `npm run build`: TypeScript y Vite; salida real en `frontend/dist`.
- `mvn clean verify`: 131 pruebas correctas con Java 21 y PostgreSQL `biblioteca_modelo_test`, sin fallos, errores u omisiones; registro del 21 de octubre a las 09:25:44.
- Revisión visual interactiva pendiente: el control del navegador rechazó el acceso a la página local en esta sesión. No se atribuyen capturas ni pruebas visuales que no se pudieron realizar.

Las evidencias HTTP y la lista de capturas personales se registran en `PRUEBA_Y_EVIDENCIAS.md`.

## Renovación completa de la interfaz — 26 de octubre de 2026

Esta sección sustituye la descripción visual del 21 de octubre. El pedido actual autoriza renovar el frontend local completo con blanco y azul y los complementos solicitados; conserva las operaciones HTTP existentes, la identidad resuelta por el servidor y el acceso directo sin login. No cambia entidades, relaciones, tablas, migraciones ni políticas institucionales.

### Módulos, fuentes y dependencias

Se mantiene la cobertura M01–M09 de la tabla anterior: M01 pp. 8–14/9–15; M02 pp. 15–21/16–22, especialmente RF-M02-49 (idioma, impresa 19/física 20); M03 pp. 22–28/23–29, RF-M03-63; M04 pp. 29–35/30–36, RF-M04-16/26; M05 pp. 36–43/37–44, RF-M05-62; M06 pp. 44–51/45–52; M07 pp. 52–59/53–60, RN-M07-10; M08 pp. 60–67/61–68, RN-M08-25; M09 pp. 68–76/69–77, RF-M09-62. M09 del texto sigue siendo M10 en el gráfico.

Entidades consultadas por las vistas: Persona y subtipos, catálogos de personas, visitas; Libro, Ejemplar y catálogos bibliográficos; Prestamo/DetallePrestamo/Devolucion; reservas de libros; Cubiculo, equipamiento, horarios, bloqueos y reservas de espacios; incidencias, sanciones, multas y pagos; notificaciones, avisos, plantillas y canales; indicadores/reportes; configuración, políticas, auditoría, instantáneas e incidentes. Se consumen los DTOs y recursos existentes de `/api/app`; esta renovación no modifica su persistencia.

### Propuesta aplicada

- Panel administrativo con tipografía Noto Sans, superficies blancas, fondo azul muy claro y azul principal `#2563eb`. Indicadores reales, búsqueda bibliográfica, accesos rápidos, actividad semanal, préstamos recientes, pendientes y avisos.
- Menú lateral blanco, plegable en escritorio, con preferencia local persistente. Secciones desplegables, búsqueda de apartados, opción activa, scroll propio y navegación móvil con Escape, retorno del foco y contención del teclado.
- Vistas y acciones conservadas: colección; circulación; espacios de estudio; comunidad; comunicaciones; administración. Se incorporan enlaces directos a ubicaciones, catálogos bibliográficos, equipamiento, carreras, departamentos, motivos de visita, tipos de sanción, plantillas, canales y parámetros.
- Personas, inventario y sanciones sincronizan sus pestañas con la URL, incluyendo atrás/adelante y recarga. Reservas procesa también un nuevo atajo de creación sobre la misma ruta. Catálogo añade filtro por idioma, combinado con búsqueda, categoría y disponibilidad.
- Estados vacíos, errores con reintento, botones ocupados, validación y restricciones del servidor siguen disponibles. El panel muestra ausencia de conexión sin inventar datos de operación.

### Tecnologías y usos verificables

| Tecnología | Uso en esta entrega |
|---|---|
| Bootstrap 4.6 / Bootstrap Grid | Base compatible con AdminLTE 3; rejilla adaptable de indicadores, accesos y paneles |
| AdminLTE 3.2 | Estilos y runtime administrativo; integrado sobre React, sin duplicar la gestión del DOM del menú |
| jQuery 3.7 | Una instancia global para los complementos UMD, servidos desde el build local |
| DataTables 3 + adaptador React 2 / Bootstrap 4 | Tablas operativas con búsqueda, orden, tamaño de página, paginación, desplazamiento y celdas/acciones React |
| Chosen | Selecciones simples y múltiples con búsqueda; preserva IDs y validación requerida |
| Date Range Picker | Periodos y rangos rápidos en reportes |
| OverlayScrollbars | Desplazamiento del menú lateral, con desmontaje del componente |
| Ionicons | Marca e iconos SVG incluidos localmente |
| Tempus Dominus Bootstrap 4 | Fechas, horas y fechas/horas de formularios; payload compatible con la API |
| jqvmap | Mapa informativo en Ayuda: México y Campus Pátzcuaro, sin métricas geográficas inventadas |
| Summernote 0.9 | Editor de avisos, deshacer/rehacer y pantalla completa; salida de texto compatible con la API, pegado como texto, sin carga de imágenes ni HTML arbitrario |
| SweetAlert2 | Confirmación de salida y retirada de avisos; errores de API escapados, espera y reintento |
| Toastr | Mensajes de operación con cierre, progreso y escape de HTML |

Se interpretan las grafías del pedido `adminite`, `datrangepicker`, `tempusominus` y `jqmap` como AdminLTE, Date Range Picker, Tempus Dominus y jqvmap. La combinación Bootstrap 4/AdminLTE 3 sigue la [documentación de AdminLTE 3.2](https://www.adminlte.io/docs/3.2/); las tablas usan el [adaptador oficial de React](https://datatables.net/manual/components/react/). `package-lock.json` conserva las resoluciones. Los overrides comparten jQuery, Moment y Summernote actualizados entre dependencias; no se cargan las copias antiguas que arrastraban los paquetes heredados.

### Archivos y comprobación

Punto de entrada: `src/main.tsx` y `App.tsx`. Integración: `lib/plugins.ts`, `components/LibraryWidgets.tsx`, `components/ui.tsx`, `lib/useRouteTab.ts`. Navegación: `lib/navigation.ts`. Vistas activas: `pages/Dashboard.tsx`, `Catalog.tsx`, `People.tsx`, `Operations.tsx`, `Admin.tsx`. Tema: `design.css`; `styles.css` conserva la base de componentes. La división de bundles está en `vite.config.ts`; los prototipos no importados no se convierten en rutas alternativas.

Comandos desde `frontend`: `npm ci`, `npm test`, `npm run test:ui`, `npm run build`. La prueba de navegador usa Playwright, arranca un servidor en un puerto libre e intercepta **toda** llamada `/api`; no conecta ni escribe en PostgreSQL. En Windows usa Edge instalado; `UI_BROWSER_CHANNEL` permite seleccionar otro navegador disponible. Capturas automáticas de fixtures en `.itspa-tmp/frontend-2026-10-26/`, separadas de evidencias académicas reales.

Para incluir la interfaz en Spring Boot: `./scripts/preparar-interfaz.ps1`. Para desarrollo: `cd frontend; npm run dev`; mantiene el proxy local existente a `127.0.0.1:8081`. Para operar con datos reales deben estar disponibles el backend y PostgreSQL. Las pruebas UI no acreditan operaciones reales ni sustituyen la integración contra `biblioteca_modelo_test`.

## Corrección de conexión y arranque — 28 de octubre de 2026

La captura del responsable mostraba dos avisos del mismo fallo. Diagnóstico real: frontend en 5173 y PostgreSQL en 5432, pero ningún backend escuchando en 8081. La terminal de arranque estaba pendiente de la contraseña PostgreSQL; no era un conflicto de puertos.

Ámbito: inicio/panel M08 (pp. 60–67 impresas / 61–68 físicas, RN-M08-09/25) y soporte/configuración M09 (pp. 68–76 / 69–77; M10 en el gráfico). Corrección técnica de conectividad, sin cambiar entidades, cardinalidades, migraciones ni requisitos funcionales.

Cambios aplicados:

- `frontend/src/lib/api.ts`: diferencia fallo de red/proxy/timeout de errores JSON reales de los módulos; valida la respuesta de sesión y limita la espera a diez segundos. Ninguna escritura se reenvía automáticamente.
- `frontend/src/lib/session.tsx`: controla una sola comprobación de conexión en curso, escucha pérdidas de conexión y recupera la sesión al reintentar, cada cinco segundos mientras hay error y al volver el foco/red. No inventa usuarios ni roles.
- `frontend/src/components/ConnectionState.tsx`, `App.tsx`, `Dashboard.tsx`, `design.css`: un solo estado de desconexión, con reintento claro; las vistas operativas se montan después de conectar. No aparecen paneles vacíos ni ceros que simulen datos disponibles. Un error JSON propio del panel conserva un único aviso local.
- `scripts/iniciar-sistema.ps1`: un comando inicia backend y después frontend, espera respuestas válidas, reutiliza servicios correctos, solicita la clave de forma oculta y mantiene las credenciales solo en el entorno temporal del hijo Java. Ctrl+C limpia únicamente sus propios procesos. No cambia PostgreSQL ni su autenticación.
- `frontend/vite.config.ts`: destino privado del proxy configurable mediante `ITSPA_API_TARGET`; diagnóstico local de desarrollo en `/__itspa_runtime` para que el lanzador rechace una interfaz conectada al puerto equivocado.

El responsable autorizó cambiar puertos si fuera necesario. `-PuertoApi` y `-PuertoInterfaz` permiten, por ejemplo, 8082/5174, trasladando el mismo destino al proxy. No se cambió el puerto efectivo 8081 porque, tras introducir la clave localmente, funcionó correctamente.

Arranque recomendado desde la raíz:

```powershell
.\scripts\iniciar-sistema.ps1
```

Comprobación sin credenciales ni arranque:

```powershell
.\scripts\iniciar-sistema.ps1 -Comprobar
```

Alternativa cuando los puertos habituales estén ocupados:

```powershell
.\scripts\iniciar-sistema.ps1 -PuertoApi 8082 -PuertoInterfaz 5174
```

El lanzador no detiene servidores ajenos. Si una interfaz existente apunta a otro backend, pide reiniciar esa terminal o elegir otro puerto de interfaz. Mantener abierta la terminal que gestiona los procesos. Las claves se introducen localmente; no deben pegarse en el chat ni en archivos del repositorio.
