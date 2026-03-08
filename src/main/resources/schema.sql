-- this is the schema for my postgresql tables

CREATE TABLE IF NOT EXISTS users(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    username VARCHAR(20) UNIQUE NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
-- remove the salt from the schema in the dbbecause bccrypt will generate it for oyou
    created_at TIMESTAMPTZ DEFAULT NOW(),
    last_login TIMESTAMPTZ,
-- add the enabled row to your schema
    enabled BOOLEAN NOT NULL DEFAULT true,
    authority VARCHAR(50) NOT NULL
);

-- deleted the transaction schema FROM THIS FILE ONLY, so that the major focus will  just be
-- on the user

create table if not exists users (
    id bigint generated always as identity primary key,
    email varchar(100) unique not null,
    username varchar(20) unique not null,
    password_hash varchar(100) not null,
    created_at timestamptz not null default now(),
    last_login timestamptz,
    enabled boolean not null default true,
    authority varchar(50) not null
);