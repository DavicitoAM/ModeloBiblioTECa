# Instrucciones reutilizables para continuar el proyecto nuevo

> Trabaja exclusivamente en el nuevo Sistema Integral de Gestión de Biblioteca del ITSPA de este repositorio. Lee primero AGENTS.md, README.md, documentacion/fuentes/Proyecto_Spring_Boot_Capa_de_Modelo.pdf y documentacion/fuentes/Diagrama_de_clases_ITSPA.png. Aplica skills/modelado-jpa-itspa/SKILL.md y consulta DECISIONES_PENDIENTES.md, MAPA_DE_MODELOS.md y MATRIZ_MODELO_COMPLETO.md.
>
> El alcance autorizado comprende 70 entidades JPA, nueve enums y todas sus tablas en una unidad de persistencia con ddl-auto=validate. Conserva V1 intacta y revisa V2__modelo_integral_itspa.sql: 75 tablas nuevas más dos iniciales, 77 de negocio incluidas siete de unión. No recrees clases piloto duplicadas ni un modelo persistente paralelo.
>
> Respeta modelo/modulo, repositorio, services y services/impl con JPA/Lombok, sin @Data, EAGER explícito ni eliminación en cascada. Repositorios, servicios, DTOs y API operativos se limitan a Libro/Ejemplar. Otros endpoints, servicios o motores de reglas requieren nueva petición expresa.
>
> Antes de cambiar código, cita módulo, página impresa/física y RF/RN. Distingue fuente de decisiones técnicas aprobadas. Señala contradicciones nuevas y conserva acuerdos vigentes; no repitas solicitudes de autorización ya concedidas.
>
> Contrasta atributos, tipos, herencia, nullabilidad, unicidad, relaciones y columnas con Flyway. Cada entidad debe tener contenido real y referencias en Javadoc; no uses clases vacías, placeholders o IDs sueltos para sustituir relaciones sin decisión expresa.
>
> Separa compilación, metadatos sin JDBC e integración PostgreSQL. La integración usa biblioteca_modelo_test y escribe datos solo Libro/Ejemplar, aunque Flyway cree todo el esquema. Consulta la bitácora antes de ejecutar y respeta negativas de permisos existentes; no uses rutas alternativas para eludirlas.
>
> Explica archivos modificados/creados, comando Maven efectivamente ejecutado o impedimento literal, alcance de la comprobación y capturas pendientes. Actualiza PRUEBA_Y_EVIDENCIAS.md con resultados reales; no atribuyas éxitos o capturas inexistentes. Modelo completo no equivale a sistema funcional completo.

## Delegación real

Los ficheros de agentes/ son instrucciones reutilizables y no crean procesos autónomos. La herramienta debe leer los roles y asignarles trabajo expreso. Si delega, debe señalar quién revisa requisitos, modelo/SQL, repositorios, servicios, pruebas y documentación, con límites de archivos. Si no puede delegar, debe explicar qué roles aplica directamente.

## Ejemplo de revisión acotada

> Revisa el mapeo completo contra V1/V2 y los acuerdos documentados. Revisa repositorios/services Libro–Ejemplar. Si hay acceso autorizado a la BD de test, ejecuta integración y registra el resultado real; si no, documenta el impedimento y evidencia faltante. No añadas API, servicios ni frontend de otros módulos.
