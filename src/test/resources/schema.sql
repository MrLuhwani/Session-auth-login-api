-- H2-compatible schema for testing (PostgreSQL MODE)
-- Mirrors schema.sql but uses TIMESTAMP instead of TIMESTAMPTZ

CREATE TABLE IF NOT EXISTS users (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email      VARCHAR(100) UNIQUE NOT NULL,
    username   VARCHAR(20)  UNIQUE NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP,
    last_login TIMESTAMP,
    enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    locked_until TIMESTAMP
);

CREATE TABLE IF NOT EXISTS authorities (
    authority VARCHAR(25) NOT NULL CHECK (authority IN ('ROLE_USER', 'ROLE_ADMIN')),
    user_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, authority)
);