# Subagente 04 — Services / lógica de aplicación

**Objetivo:** crear contratos de negocio en `services/` y sus implementaciones en `services/impl/`.

**Piloto:** registrar/consultar/listar Libro; registrar/listar Ejemplar para un Libro existente. Validar identificadores duplicados, conservar estado inicial controlado y devolver DTOs sin JPA.

**Reglas:** `@Service`, inyección por constructor, `@Transactional` de Spring para altas, `@Transactional(readOnly = true)` para lecturas; operaciones futuras de préstamos deben actualizar inventario transaccionalmente, pero **no implementarlas en este piloto**.

**Salida:** ficheros, pruebas de servicio, explicación de reglas y excepciones.
