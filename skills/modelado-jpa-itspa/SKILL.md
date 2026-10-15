---
name: modelado-jpa-itspa
description: Modelar entidades del Sistema Integral de Gestión de Biblioteca del ITSPA desde el PDF y su diagrama, con JPA, alias de tabla/campos, Lombok, repositorios Spring Data JPA, services, pruebas PostgreSQL y evidencias, sin extender el esquema fuera del alcance autorizado.
---

# Skill: modelado JPA ITSPA

Usar al crear o revisar entidades, repositorios, services y pruebas de la práctica de capa de modelo.

## Insumos obligatorios
1. `AGENTS.md`
2. `documentacion/fuentes/Proyecto_Spring_Boot_Capa_de_Modelo.pdf`
3. `documentacion/fuentes/Diagrama_de_clases_ITSPA.png`
4. `documentacion/DECISIONES_PENDIENTES.md`
5. `documentacion/MAPA_DE_MODELOS.md`

## Flujo
1. Citar módulo, página, `RF-Mxx-yy` o `RN-Mxx-yy` que autoriza la clase.
2. Distinguir `@Entity` real, enum, DTO, interface de repositorio y clase `@Service`.
3. Determinar PK, nombre de tabla, columnas, nulos, unicidad y cardinalidades; ante conflicto del diagrama, preguntar.
4. Crear entidad en `modelo` con `@Entity`, `@Table`, `@Column`, `@JoinColumn`, `@Enumerated` según corresponda, usando Lombok sin `@Data`.
5. Crear `repositorio` como interface `JpaRepository` con consultas derivadas acotadas.
6. Crear `services` y `services/impl`, con validaciones y transacciones; evitar lógica en controllers.
7. Registrar DTO y controlador solo si hacen falta para probar la capa.
8. Alcance ampliado aprobado: todas las clases del modelo llevan JPA y pertenecen a una sola unidad. Conservar V1 y añadir V2 para el esquema completo; las operaciones y pruebas funcionales se concentran en Libro/Ejemplar.
9. Probar PostgreSQL con base de test separada; verificar resultados/errores y capturas reales.
10. Actualizar matriz y memoria. Explicar decisiones propias frente a texto fuente.

## Convenciones mínimas

```java
@Entity
@Table(name = "libros")
@Getter @Setter @NoArgsConstructor
public class Libro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titulo", nullable = false, length = 250)
    private String titulo;
}
```

Al tratar `Ejemplar`, hacer `@ManyToOne(fetch = FetchType.LAZY, optional = false)` con `@JoinColumn(name="libro_id")`, y la contraparte `@OneToMany(mappedBy="libro")` cuando sea necesaria.

## Salidas esperadas
- Archivos Java compilables bajo las capas acordadas.
- Correspondencia clase / tabla / atributo / columna.
- Test que guarda y consulta; evidencia de FK y estado.
- Registro de alcance y de decisiones en Markdown.

## Prohibiciones
No incluir clases del proyecto antiguo, no habilitar `ddl-auto=update`, no inventar políticas, no crear services/endpoints vacíos de otros módulos, no subir contraseñas, no modificar el diagrama sin autorización. El esquema completo ya está autorizado; su existencia no demuestra el cumplimiento funcional de todos los módulos.
