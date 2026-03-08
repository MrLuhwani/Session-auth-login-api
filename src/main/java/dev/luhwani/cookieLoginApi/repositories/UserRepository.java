package dev.luhwani.cookieLoginApi.repositories;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import dev.luhwani.cookieLoginApi.customExceptions.UnknownDBException;
import dev.luhwani.cookieLoginApi.dto.RegisterRequest;
import dev.luhwani.cookieLoginApi.dto.User;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?;";
        Integer count = jdbc.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?;";
        Integer count = jdbc.queryForObject(sql, Integer.class, username);
        return count != null && count > 0;
    }

    public Long registerUserAndReturnId(RegisterRequest req, String passwordHash) {
        String sql = """
                INSERT INTO users (email, username, password_hash, enabled, created_at, last_login, authority)
                VALUES (?, ?, ?, true, NOW(), NULL, 'ROLE_USER');
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, req.email());
            ps.setString(2, req.username());
            ps.setString(3, passwordHash);
            return ps;
        }, keyHolder);
        Number idKey = keyHolder.getKey();
        if (idKey == null) {
                throw new UnknownDBException("Error occured while generating user id");
            }
        return idKey.longValue();
    }

    public Optional<User> findByEmail(String emailInput) {
        String sql = """
                SELECT id, username, password_hash, enabled, authority FROM users WHERE email = ?;""";
        RowMapper<User> rowMapper = (r, i) -> {
            Long id= r.getLong("id");
            String username = r.getString("username");
            String passwordHash = r.getString("password_hash");
            boolean enabled = r.getBoolean("enabled");
            String authority = r.getString("authority");
            return new User(emailInput, id, username, passwordHash, enabled, authority);
        };
        ;
        List<User> authCreds = jdbc.query(sql, rowMapper, emailInput);
        return authCreds.stream().findFirst();
    }

}

/*
    public Long createUser(RegisterRequest req, String passwordHash) {
        String sql = """
                insert into users (email, username, password_hash, enabled, created_at, authority)
                values (?, ?, ?, true, now(), 'ROLE_USER')
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, req.email().trim());
            ps.setString(2, req.username().trim());
            ps.setString(3, passwordHash);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("User was inserted but no generated id was returned");
        }
        return key.longValue();
    }

    public Optional<UserRecord> findByEmail(String email) {
        String sql = """
                select id, email, username, password_hash, enabled, authority
                from users
                where email = ?
                """;

        try {
            UserRecord user = jdbc.queryForObject(sql, (rs, rowNum) ->
                    new UserRecord(
                            rs.getLong("id"),
                            rs.getString("email"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getBoolean("enabled"),
                            rs.getString("authority")
                    ), email);

            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void updateLastLogin(Long userId) {
        String sql = "update users set last_login = now() where id = ?";
        jdbc.update(sql, userId);
    }
*/