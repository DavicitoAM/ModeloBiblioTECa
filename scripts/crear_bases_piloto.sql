-- Ejecutar en pgAdmin conectado a una BD administrativa (por ejemplo postgres).
-- Bases nuevas de este proyecto; no borrar ni importar bases anteriores.
-- Las comillas conservan el nombre con B mayuscula solicitado por el responsable.
-- Si una base ya existe, ejecutar solamente la sentencia de la base faltante.
CREATE DATABASE "modeloBiblioteca";
CREATE DATABASE biblioteca_modelo_test;
-- Flyway aplica V1 y V2: esquema completo; pruebas funcionales Libro/Ejemplar.
