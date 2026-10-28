-- Consultas de solo lectura ejecutadas en pgAdmin sobre modeloBiblioteca.
-- Evidencias del reporte ITSPA, 17 de octubre de 2026.

SELECT table_name AS tabla, column_name AS campo,
       udt_name AS tipo, is_nullable AS acepta_nulos,
       character_maximum_length AS longitud
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN ('libros', 'ejemplares')
  AND column_name IN ('id','titulo','isbn','num_paginas',
  'codigo_barras','fecha_adquisicion','estado',
  'estado_conservacion','libro_id')
ORDER BY table_name, ordinal_position;

SELECT conname AS restriccion,
       pg_get_constraintdef(oid) AS definicion
FROM pg_constraint
WHERE conrelid = 'public.ejemplares'::regclass
  AND conname = 'fk_ejemplares_libro';

SELECT version, description, success,
 (SELECT count(*) FROM information_schema.tables
  WHERE table_schema='public' AND table_type='BASE TABLE'
  AND table_name<>'flyway_schema_history') AS tablas_negocio
FROM flyway_schema_history
ORDER BY installed_rank;

SELECT l.id AS libro, e.id AS ejemplar,
 e.codigo_barras, e.estado, e.estado_conservacion
FROM libros l JOIN ejemplares e ON e.libro_id=l.id
ORDER BY l.id, e.id;
