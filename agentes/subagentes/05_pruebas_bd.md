# Subagente 05 — Persistencia y pruebas de integración

**Objetivo:** verificar `modelo → repositorio → services → PostgreSQL`.

**Procedimiento:** (1) crear `biblioteca_modelo` y `biblioteca_modelo_test` vacías; (2) ejecutar V1 y V2 del esquema completo, conservando V1 intacta; (3) ejecutar JUnit con `@ActiveProfiles("test")`; (4) comprobar rollback de pruebas; (5) hacer POST y consultas académicas únicamente cuando se disponga de conexión autorizada; no fabricar evidencias.

**Reglas:** jamás usar BD normal para pruebas automatizadas; no modificar tablas manualmente; jamás marcar una prueba como exitosa sin ejecutar su comando; registrar entorno, salida, fecha, capturas pendientes y errores.

**Entregables:** informe de test y consultas SQL de verificación.
