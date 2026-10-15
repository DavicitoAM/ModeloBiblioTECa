# Anotaciones JPA, Lombok y alias del modelo completo

## Correspondencia Java–SQL

La [matriz completa](MATRIZ_MODELO_COMPLETO.md) contiene las 70 clases, sus tablas, campos, asociaciones y referencias. Los nombres físicos usan snake_case; las clases PascalCase y los atributos camelCase.

| Anotación / tipo | Uso académico y decisión |
|---|---|
| @Entity | Incluye cada una de las 70 clases persistentes en el mismo modelo JPA |
| @Table(name="...") | Nombre físico de tabla, creada mediante Flyway |
| @Id | PK de tipo común Long |
| @GeneratedValue(strategy=IDENTITY) | Generación de PK en tablas raíz mediante BIGSERIAL |
| @Inheritance(strategy=JOINED) | Persona y subtipos tienen tablas propias con el mismo ID |
| @PrimaryKeyJoinColumn(name="id") | La PK del subtipo también es FK a Persona; sin generación independiente |
| @Column | Alias, longitud, nullabilidad y unicidad del atributo escalar |
| @ManyToOne(fetch=LAZY) / @JoinColumn | Asociación dueña de FK; optional=false y nullable=false señalan obligatoriedad |
| @OneToMany(mappedBy=...) | Colección inversa: no crea otra FK ni exige por sí sola filas hijas |
| @OneToOne y FK UNIQUE | Cuenta por persona y devolución definitiva por detalle |
| @ManyToMany / @JoinTable | Siete tablas de unión, sin siete entidades adicionales |
| @Enumerated(EnumType.STRING) | Nombre enum en VARCHAR; no posición ordinal |
| LocalDate / LocalDateTime / LocalTime | Fecha, fecha/hora y hora de calendario |
| BigDecimal, precisión 12 y escala 2 | Importes: SQL NUMERIC(12,2) |
| @Getter / @Setter / @NoArgsConstructor | Accesores y constructor Lombok; sin @Data |

No se utiliza eliminación en cascada ni FetchType.EAGER explícito. Las colecciones no implementan reglas transaccionales: una lista puede estar vacía aunque RN-M03-03 exija uno o más detalles al confirmar el préstamo.

## Circuito Libro–Ejemplar

| Propiedad | Columna / asociación |
|---|---|
| Libro.id | libros.id |
| Libro.titulo | libros.titulo |
| Libro.anioPublicacion | libros.anio_publicacion |
| Libro.numPaginas | libros.num_paginas |
| Libro.editorial | libros.editorial_id, opcional |
| Libro.idioma | libros.idioma_id, opcional por decisión expresa |
| Libro.autores / Libro.categorias | libros_autores / libros_categorias |
| Libro.ejemplares | Inversa de Ejemplar.libro, sin columna propia |
| Ejemplar.codigoBarras | ejemplares.codigo_barras, NOT NULL y UNIQUE |
| Ejemplar.fechaAdquisicion | ejemplares.fecha_adquisicion |
| Ejemplar.estado | ejemplares.estado, enum de circulación STRING |
| Ejemplar.estadoConservacion | ejemplares.estado_conservacion, enum físico independiente |
| Ejemplar.estatus | ejemplares.estatus, indicador booleano distinto de los estados |
| Ejemplar.libro | ejemplares.libro_id, FK obligatoria |
| Ejemplar.ubicacion | ejemplares.ubicacion_id, ubicación actual opcional |

Libro.agregarEjemplar coordina ambos extremos en memoria; la FK pertenece a Ejemplar. El servicio persiste la copia mediante su repositorio dentro de la transacción. No se borra en cascada ni se persiste automáticamente toda la colección.

## DDL, validación y límites

V1 conserva las tablas originales. V2 añade las restantes y las columnas del catálogo. FK, UNIQUE, CHECK e índices se declaran en SQL, incluidos los de visitas abiertas y relaciones únicas. Hibernate validate no sustituye Flyway ni comprueba todas las reglas de negocio.

Los tipos/estados sin vocabulario cerrado siguen como String. Los nueve enums se enumeran en [MAPA_DE_MODELOS.md](MAPA_DE_MODELOS.md); no son tablas. Las diferencias y decisiones están en [DECISIONES_PENDIENTES.md](DECISIONES_PENDIENTES.md).

Compilación, metadatos sin JDBC e integración PostgreSQL son comprobaciones distintas. Sus resultados reales corresponden a la [bitácora](PRUEBA_Y_EVIDENCIAS.md). La existencia de anotaciones o SQL no acredita ejecución.
