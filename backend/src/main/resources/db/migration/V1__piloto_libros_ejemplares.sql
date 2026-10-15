-- Migración PILOTO de la práctica de modelo. Solo 2 entidades del M02.
-- No representa el esquema completo de la biblioteca.
CREATE TABLE libros (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(250) NOT NULL,
    descripcion VARCHAR(2000),
    edicion VARCHAR(100),
    anio_publicacion INTEGER,
    isbn VARCHAR(20) UNIQUE,
    num_paginas INTEGER,
    CONSTRAINT chk_libro_num_paginas CHECK (num_paginas IS NULL OR num_paginas > 0)
);

CREATE TABLE ejemplares (
    id BIGSERIAL PRIMARY KEY,
    codigo_barras VARCHAR(80) NOT NULL UNIQUE,
    fecha_adquisicion DATE NOT NULL,
    estado VARCHAR(30) NOT NULL,
    estado_conservacion VARCHAR(30) NOT NULL,
    estatus BOOLEAN NOT NULL DEFAULT TRUE,
    libro_id BIGINT NOT NULL,
    CONSTRAINT fk_ejemplares_libro FOREIGN KEY (libro_id) REFERENCES libros(id),
    CONSTRAINT chk_ejemplares_estado CHECK (estado IN
        ('DISPONIBLE', 'PRESTADO', 'RESERVADO', 'MANTENIMIENTO', 'EXTRAVIADO', 'BAJA')),
    CONSTRAINT chk_ejemplares_conservacion CHECK (estado_conservacion IN
        ('EXCELENTE', 'BUENO', 'REGULAR', 'DANADO'))
);

CREATE INDEX idx_ejemplares_libro ON ejemplares(libro_id);
