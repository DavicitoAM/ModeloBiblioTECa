# Evidencias reales del reporte de modelo

[Google Docs editable](https://docs.google.com/document/d/1P4nmuiTjjBGLzSr9EpMOgwBAYPSAY4yt_Fs3FcqnpoM/edit?usp=drivesdk).

Las imágenes con sufijo `-original.png` son capturas reales de VS Code o pgAdmin tomadas el 17 de octubre de 2026 por encargo del responsable. Los archivos con nombres cortos son sus recortes para el reporte; no se redibujó ni sustituyó contenido. [MANIFIESTO_CAPTURAS.json](MANIFIESTO_CAPTURAS.json) identifica originales, SHA-256 y coordenadas de recorte. El diagrama procede directamente de `documentacion/fuentes/Diagrama_de_clases_ITSPA.png`, que permanece intacto.

| Evidencia | Archivos originales |
|---|---|
| Libro, Ejemplar y asociación inversa | 01–03 |
| Persona y JOINED | 04 |
| Repositorios | 05–06 |
| Services y transacciones | 07–08 |
| Contexto de integración y recarga de copias | 09–10 |
| Informe de ejecución real de Maven/Surefire | 11 |
| Registros persistentes en pgAdmin | 12, vista compacta |
| Columnas físicas y nulabilidad | 13 |
| FK ejemplares.libro_id → libros.id | 14 |
| Historial Flyway y 77 tablas de negocio | 15 |

El informe capturado de resultados se conserva en `../resultado-postgresql-2026-10-17.txt`. Esa figura no se presenta como una terminal ejecutándose. Las pruebas se ejecutaron en biblioteca_modelo_test; las capturas pgAdmin consultan modeloBiblioteca, con un libro académico y dos copias. [consultas_evidencia.sql](consultas_evidencia.sql) contiene las sentencias SELECT usadas para campos, FK, versiones y JOIN.

Se conservan tanto la captura amplia original de registros como su vista compacta. Para Libro, `libro-corto.png` es el recorte final que mantiene legibles @Entity, @Table, Lombok, identificador y los primeros campos; la captura completa y el recorte más extenso siguen disponibles. Los demás alias se explican en la tabla nativa del documento y en la captura de columnas PostgreSQL.
