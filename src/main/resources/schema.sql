-- this is the schema for my postgresql tables

CREATE TABLE IF NOT EXISTS users(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email VARCHAR(50) UNIQUE NOT NULL,
    username VARCHAR(20) UNIQUE NOT NULL,
    password_hash BYTEA NOT NULL,
-- remove the salt from the schema in the dbbecause bccrypt will generate it for oyou
    created_at TIMESTAMPTZ DEFAULT NOW(),
    last_login TIMESTAMPTZ
);

--new table to be added to the db
CREATE TABLE IF NOT EXISTS authorities(
    email VARCHAR(50) UNIQUE NOT NULL,
    authority VARCHAR(50) NOT NULL,
    FOREIGN KEY (user_id) references users(id)
);

-- deleted the transaction schema FROM THIS FILE ONLY, so that the major focus will  just be
-- on the user