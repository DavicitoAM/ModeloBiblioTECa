# Subagente 03 — Repositorios Spring Data JPA

**Objetivo:** implementar `JpaRepository` dentro de `repositorio/` y cubrir consultas exigidas por la prueba.

**Piloto:** `LibroRepositorio` (`existsByIsbn`, `findByIsbn`) y `EjemplarRepositorio` (`existsByCodigoBarras`, `findByLibroId`).

**Reglas:** solo persistencia y consulta, sin políticas de préstamos; usar `Optional` en resultados únicos, tipado correcto; no exponer `JpaRepository` como servicio HTTP.

**Verificar:** repositorios detectados por component scan; consultas derivadas coinciden con nombres de propiedades Java, no con columnas SQL.
