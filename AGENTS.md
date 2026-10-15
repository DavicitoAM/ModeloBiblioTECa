# AGENTS.md — contrato de trabajo para asistentes (proyecto nuevo ITSPA)

## Identidad
Este repositorio es **independiente** de cualquier versión anterior de BiblioTECa. Nunca importar archivos, modelos, migraciones, puertos, datos ficticios o decisiones históricas ajenas a este proyecto.

## Orden de autoridad
1. Pedido actual del responsable del proyecto.
2. `documentacion/fuentes/Proyecto_Spring_Boot_Capa_de_Modelo.pdf`, con referencias a páginas / códigos RF-Mxx / RN-Mxx.
3. `documentacion/fuentes/Diagrama_de_clases_ITSPA.png`.
4. `documentacion/DECISIONES_PENDIENTES.md`, que registra conflictos para revisión, **no autorizaciones tácitas**.
5. Código existente en este repositorio.

Si dos fuentes discrepan, **señalarlo antes de modificar la relación o modelo**; no adivinar una cardinalidad.

## Restricciones de esta fase
- Priorizar la práctica de **capa de modelo**, alias de tablas/columnas, Lombok, relación JPA y test en PostgreSQL.
- Capas físicas Java **exactas**: `modelo`, `repositorio`, `services` (con `services.impl`). `controlador`, `dto`, `excepcion` son adaptadores mínimos para demostrar funcionamiento.
- Alcance ampliado expresamente por el responsable: modelo JPA completo (70 clases y enums), una sola unidad de persistencia y esquema completo.
- Conservar V1 intacta; V2 amplía `libros`/`ejemplares` y crea el resto del esquema aprobado. Sin clases piloto duplicadas ni mapeos alternativos.
- Repositorios, services, DTOs y controladores operativos se limitan al recorrido Libro–Ejemplar; los demás módulos aportan modelo persistente, sin lógica de negocio ficticia.
- Sin frontend, autenticación ficticia, datos personales reales ni lógicas avanzadas de préstamos.
- No tocar los PDFs ni el diagrama de referencia.

## Normas del código
- Java 21, Spring Boot, Spring Data JPA, Lombok, PostgreSQL y Flyway para el prototipo.
- Entidades: `@Entity`, `@Table(name=...)`, `@Column(name=...)`, `@Id`, relaciones explícitas con `@JoinColumn`.
- Lombok: `@Getter`, `@Setter`, `@NoArgsConstructor` donde corresponda; **no `@Data`** en entidades (riesgos de recursión/lazy loading en `equals`, `hashCode`, `toString`).
- No exponer entidades JPA directamente por API: utilizar DTOs.
- Evitar `FetchType.EAGER` por defecto; las lecturas relacionales ocurren dentro de `@Transactional`.
- Services poseen reglas y límites transaccionales. Controllers solo reciben/validan y delegan. Repositorios no contienen reglas de negocio.
- No utilizar `spring.jpa.hibernate.ddl-auto=create` ni `update`. Usar `validate`; estructuras mediante migraciones aprobadas.
- No subir secretos, hashes de credenciales o información escolar personal.
- Las tablas y columnas usan `snake_case`; clases y propiedades usan `PascalCase`/`camelCase`.

## Protocolo para cualquier tarea
1. Indicar módulo y requisitos fuente (página, `RF-*`, `RN-*`).
2. Identificar las entidades afectadas y dependencias.
3. Validar si la implementación está en el alcance autorizado.
4. Describir ficheros a crear/modificar y pruebas previstas.
5. Implementar **solo** lo autorizado.
6. Ejecutar build y test si el entorno lo permite; si no, decirlo explícitamente.
7. Anotar en `documentacion/PRUEBA_Y_EVIDENCIAS.md` qué capturas debe realizar la persona.
8. Actualizar `documentacion/DECISIONES_PENDIENTES.md` si surge una ambigüedad.

## Delegación
- Coordinación: `agentes/COORDINADOR.md`.
- Subagentes: `agentes/subagentes/*.md`.
- Skill especializado: `skills/modelado-jpa-itspa/SKILL.md`.

## Criterio de terminado de la práctica
- Se puede explicar cada anotación JPA y cada alias.
- Todas las entidades del inventario aprobado compilan y su esquema se valida; Libro–Ejemplar además demuestra persistencia funcional.
- Repositorios guardan y consultan; Services coordinan; API piloto demuestra flujo.
- Pruebas transaccionales contra una BD de **test** distinta.
- Evidencias reales capturadas por el estudiante, sin afirmar capturas que no existen.
