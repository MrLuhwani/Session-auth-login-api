-- this is the schema for my postgresql tables

CREATE TABLE IF NOT EXISTS users(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email VARCHAR(50) UNIQUE NOT NULL,
    username VARCHAR(20) UNIQUE NOT NULL,
    password_hash BYTEA NOT NULL,
    password_salt VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    last_login TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS transactions(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    date DATE NOT NULL,
    kobo_amt BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    entry_type VARCHAR(10) NOT NULL CHECK (entry_type IN ('INCOME', 'EXPENSE')),
    category TEXT NOT NULL,
    description VARCHAR(50) NOT NULL,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    updated_at TIMESTAMPTZ
);