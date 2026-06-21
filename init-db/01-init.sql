-- ============================================================
-- Script de inicialización de la base de datos unificada
-- del sistema de parqueadero (APIs: zonas, usuarios, vehiculos)
--
-- Se ejecuta automáticamente SOLO la primera vez que el
-- contenedor crea el volumen de datos (carpeta vacía).
-- ============================================================

-- ---------- API zonas (Spring Boot) ----------
CREATE USER zonas WITH PASSWORD 'zonas123';
CREATE DATABASE zonas OWNER zonas;

-- ---------- API usuarios (Spring Boot) ----------
CREATE USER usuarios WITH PASSWORD 'usuarios123';
CREATE DATABASE usuarios OWNER usuarios;

-- ---------- API vehiculos (NestJS / TypeORM) ----------
-- Usa el superusuario 'postgres' (definido en docker-compose).
CREATE DATABASE vehiculos_db OWNER postgres;
