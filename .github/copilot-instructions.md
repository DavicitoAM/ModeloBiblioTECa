# Instrucciones para asistente integrado al repositorio

Consulta siempre `AGENTS.md`, `agentes/COORDINADOR.md` y `skills/modelado-jpa-itspa/SKILL.md`. Este es un proyecto nuevo e independiente, basado exclusivamente en el PDF y diagrama de `documentacion/fuentes/`.

Capas Java: `modelo`, `repositorio`, `services` y `services.impl`. El alcance aprobado incluye las 70 entidades JPA y sus enums, una sola unidad de persistencia y el esquema completo mediante V1 intacta y V2. Las operaciones de repositorios/services/API y pruebas funcionales se concentran en Libro–Ejemplar. No crear endpoints vacíos ni lógica ficticia para los demás módulos.

Reporta módulo, páginas y RF/RN antes de modificar código. Documenta contradicciones y decisiones. Capturas y resultados de pruebas deben ser reales. Mantener `ddl-auto=validate`, JPA/Lombok sin `@Data` y fuentes originales intactas.
