-- this is the schema for my postgresql tables

CREATE TABLE IF NOT EXISTS users(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    username VARCHAR(20) UNIQUE NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    last_login TIMESTAMPTZ,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    locked_until TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS authorities(
    authority varchar(25) NOT NULL CHECK (authority IN ('ROLE_USER', 'ROLE_ADMIN')),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, authority)
);

-- Uncomment the below lines to create an admin account, and assign it the ROLE_ADMIN authority
-- then comment them back out after you have run them, so that the app
-- can run without trying to create the same admin account again, which will cause an error
--because of the unique constraints on the email and username fields

-- -- you run this in PgAdmin (or any SQL client) when you wish to create an admin
-- -- of course, you can always change the username and email to whatever you like
-- -- although, the email is expected to a valid email format
-- -- for the username, between 5 to 20 characters
-- --  for the passwordHash, in the services package, there is a class called EncryptString
-- -- run it, input the password of your choice, copy the output, and change the hash here to what was generated
-- INSERT INTO users (email, username, password_hash, created_at, last_login)
-- VALUES ('admin@business.com', 'admin123', '$2a$10$xNsUOMd0NYb4Hg.8g9x9C.DhHExwvkeoZgRVknWP03gAOD.toQDqW', NOW(), NOW());

-- -- run this next so that you can get the auto-generated id that postgres assigned to the admin account
-- -- of course, you change the email to the one you wanted

-- -- then you change the 4, to the user_id you see that was generated from the above query
-- INSERT INTO authorities (authority, user_id)
-- VALUES ('ROLE_ADMIN', 4);