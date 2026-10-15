# Subagente 02 — Modelador JPA / Lombok

**Objetivo:** codificar entidades aprobadas dentro de `backend/.../modelo/`.

**Procedimiento:** aplicar `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`, `@Enumerated(EnumType.STRING)`, `@ManyToOne`/`@OneToMany` y `@JoinColumn` justificados; nombres SQL en snake_case; Lombok `@Getter`, `@Setter`, `@NoArgsConstructor`; relacionar clases sin recursión JSON.

**Modelo aprobado:** 70 entidades y enums en nueve paquetes, una sola unidad JPA y esquema completo. Operaciones piloto: `Libro` y `Ejemplar`. Relación `Libro 1 -- 0..* Ejemplar`, `Ejemplar N -- 1 Libro`. Enum `EstadoEjemplar` (disponibilidad) separado de `EstadoConservacion`.

**Prohibido:** `@Data` en entidades, cascada de eliminación, `EAGER` indiscriminado, campos heredados inventados, `ddl-auto=update`, crear operaciones de otros módulos sin autorización.

**Salida:** código compilable, mapa `clase → tabla`, `atributo → columna`, anotaciones comentadas en documentación.
