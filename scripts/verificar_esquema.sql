-- Ejecutar en modeloBiblioteca (demostracion) o biblioteca_modelo_test (tests).
-- Solo consultas. Las filas de los tests transaccionales se revierten.
SELECT current_database() AS base_actual, current_user AS usuario,
       inet_server_addr() AS servidor, inet_server_port() AS puerto;
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
SELECT count(*) AS tablas_negocio
FROM information_schema.tables
WHERE table_schema = 'public' AND table_type = 'BASE TABLE' AND table_name <> 'flyway_schema_history';
SELECT tc.table_name, tc.constraint_name, kcu.column_name,
       ccu.table_name AS tabla_referida, ccu.column_name AS columna_referida
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu
  ON tc.constraint_name = kcu.constraint_name AND tc.constraint_schema = kcu.constraint_schema
JOIN information_schema.constraint_column_usage ccu
  ON ccu.constraint_name = tc.constraint_name AND ccu.constraint_schema = tc.constraint_schema
WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = 'public'
ORDER BY tc.table_name, tc.constraint_name;
SELECT l.id, l.titulo, e.id AS ejemplar_id, e.codigo_barras, e.estado, e.estado_conservacion
FROM libros l LEFT JOIN ejemplares e ON e.libro_id = l.id ORDER BY l.id, e.id;
