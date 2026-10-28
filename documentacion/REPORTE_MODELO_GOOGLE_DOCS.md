# Reporte de la capa de modelo en Google Docs

Documento nativo y editable: [Reporte del modelo BiblioTECa ITSPA — actores, entidades y prueba Libro–Ejemplar](https://docs.google.com/document/d/1P4nmuiTjjBGLzSr9EpMOgwBAYPSAY4yt_Fs3FcqnpoM/edit?usp=drivesdk).

Preparado el 17 de octubre de 2026 por encargo del responsable. Está en la carpeta ChatGPT de Google Drive. No se cambió su configuración de acceso ni se publicó para terceros.

## Contenido verificado

La exportación revisada tiene 11 páginas. El documento contiene 16 figuras insertadas, cuatro tablas nativas y once apartados: funcionalidad/actores, diagrama original, clases centrales, JPA/alias/Lombok, relación Libro–Ejemplar, repositorios, services, prueba de modelo, campos/FK, registros/migraciones y resultados/conclusión.

El reporte explica la función prevista de bibliotecario, usuario lector y administrador; distingue los roles de los subtipos de Persona. Desarrolla el código funcional de Libro/Ejemplar y ubica Persona, CuentaUsuario, Rol, Prestamo y DetallePrestamo en su alcance real. No presenta autenticación, préstamos ni otros módulos como operativos.

Fuentes: PDF ITSPA, M01 pp. impresas 8–14 / visor 9–15; M02 pp. 15–21 / visor 16–22, RF-M02-01/02/06/26/27/28/30 y RN-M02-03/04/11/12/13/14; RNF-MAN-07, impresa 81 / visor 82. Se explicitan acuerdos del modelo frente al diagrama, sin modificar las fuentes.

## Capturas y reproducibilidad

- [Originales, recortes y manifiesto](evidencias/reporte-2026-10-17/README.md).
- [Consultas SQL de solo lectura ejecutadas en pgAdmin](evidencias/reporte-2026-10-17/consultas_evidencia.sql).
- [Resultado real de las 77 pruebas](evidencias/resultado-postgresql-2026-10-17.txt).

Las capturas de código se tomaron en VS Code y las de tablas/campos en pgAdmin, sobre modeloBiblioteca. Los recortes eliminan márgenes y paneles ajenos; no reconstruyen ni alteran el código, las filas o las salidas. La figura de resultados es una captura del informe real basado en Surefire y el log Maven, identificada como tal. El diagrama se insertó desde el archivo original.

Las capturas fueron obtenidas por el asistente a petición del responsable, sin atribuirlas al estudiante. Las contraseñas no aparecen en el reporte. Esta tarea documentó las pruebas ya ejecutadas el mismo día y realizó consultas SELECT para las capturas; no modificó entidades, services, repositorios ni migraciones.

La exportación PDF usada para comprobar el diseño y sus renders están en `.itspa-tmp/`, fuera de Git. Se comprobó por lectura del documento que las 16 imágenes, las cuatro tablas y los once encabezados existen y que no quedaron marcadores de preparación. La revisión visual corrigió páginas de continuación casi vacías; la versión final conserva una sección por página.

Para continuar, editar este mismo Google Doc y conservar el contexto y pies de las evidencias. Si cambia la implementación, actualizar capturas y resultados antes de afirmar un nuevo comportamiento.
