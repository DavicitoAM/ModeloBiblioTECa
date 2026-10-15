# Agente coordinador — Arquitectura de modelo del ITSPA

## Misión
Guiar la implementación fiel del diagrama y PDF de esta nueva versión del Sistema Integral de Gestión de Biblioteca del ITSPA. Coordinar subagentes como **roles de revisión/ejecución**: un archivo Markdown no instala automáticamente una plataforma multiagente.

## Inicio obligatorio
- Leer `AGENTS.md` y `README.md`.
- Consultar el PDF y la imagen original de las fuentes antes de decidir entidades o cardinalidades.
- Consultar `documentacion/MAPA_DE_MODELOS.md` y `documentacion/DECISIONES_PENDIENTES.md`.
- Verificar alcance aprobado: modelo JPA y esquema completos; operaciones de repositorios/services y pruebas funcionales Libro–Ejemplar.

## Delegación ordenada
1. **Analista**: `subagentes/01_analista_requisitos.md` valida nombres, reglas y requisitos.
2. **Modelo JPA**: `subagentes/02_modelador_jpa.md` define entidades, relaciones, enums y Lombok.
3. **Repositorio**: `subagentes/03_repositorios.md` define interfaces y consultas.
4. **Servicios**: `subagentes/04_services.md` conecta el caso de uso y las reglas.
5. **Pruebas**: `subagentes/05_pruebas_bd.md` valida migración, aislamiento y JUnit.
6. **Documentación**: `subagentes/06_documentacion.md` actualiza evidencias y matriz.

## Condiciones de entrega de cada subagente
- Archivos exactos tocados.
- Requisitos fuente y relaciones pertinentes.
- Decisiones explícitas vs. inferencias.
- Pruebas realizadas y pruebas pendientes.
- Advertencia si intentó exceder la práctica.

## Límites y obligaciones
- No inferir que `Administrador` y `Bibliotecario` son herencia de `Persona`; el PDF dice roles.
- Conservar V1 y crear V2 del esquema completo autorizado, sin inventar políticas ni datos.
- No mezclar los nombres M09 y M10 sin dejar registro.
- No crear services/endpoints vacíos o lógica funcional de otros módulos.
