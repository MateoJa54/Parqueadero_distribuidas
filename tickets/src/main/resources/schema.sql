-- ============================================================
-- Objetos de BD que Hibernate (ddl-auto) no puede expresar.
-- Se ejecuta DESPUES de que Hibernate crea la tabla 'tickets'
-- (spring.jpa.defer-datasource-initialization=true).
-- Todo es idempotente (IF NOT EXISTS) para soportar reinicios.
-- ============================================================

-- Garantia ATOMICA de los invariantes del parqueadero: como maximo
-- UN ticket ACTIVO por vehiculo y UN ticket ACTIVO por espacio. Esto
-- cierra la ventana de carrera de las validaciones en memoria (dos
-- ingresos concurrentes ya no pueden crear dos tickets activos).
CREATE UNIQUE INDEX IF NOT EXISTS ux_ticket_vehiculo_activo
    ON tickets (id_vehiculo)
    WHERE estado_ticket = 'ACTIVO';

CREATE UNIQUE INDEX IF NOT EXISTS ux_ticket_espacio_activo
    ON tickets (id_espacio)
    WHERE estado_ticket = 'ACTIVO';

-- Secuencia para el numero del codigo de ticket (TKT-000001). Es
-- atomica y thread-safe, reemplazando el inseguro count()+1.
CREATE SEQUENCE IF NOT EXISTS ticket_codigo_seq START 1;
