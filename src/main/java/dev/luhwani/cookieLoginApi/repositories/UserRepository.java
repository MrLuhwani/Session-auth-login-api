package dev.luhwani.cookieLoginApi.repositories;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import dev.luhwani.cookieLoginApi.customExceptions.AuthInfrastructureException;
import dev.luhwani.cookieLoginApi.dto.RegisterRequest;
import dev.luhwani.cookieLoginApi.dto.UserRecord;

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
                INSERT INTO users (email, username, password_hash, enabled, created_at)
                VALUES (?, ?, ?, true, NOW())
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            // Specify the auto-increment column name explicitly
            PreparedStatement ps = connection.prepareStatement(sql, new String[] { "id" });
            ps.setString(1, req.email().trim());
            ps.setString(2, req.username().trim());
            ps.setString(3, passwordHash);
            return ps;
        }, keyHolder);

        Number idKey = keyHolder.getKey();
        if (idKey == null) {
            throw new AuthInfrastructureException("Failed to retrieve generated user ID.");
        }

        Long userId = idKey.longValue();
        String authoritySql = "INSERT INTO authorities (user_id, authority) VALUES (?, ?)";
        jdbc.update(authoritySql, userId, "ROLE_USER");
        return userId;
    }

    public void setLastLogin(Long userId) {
        String sql = "UPDATE users SET last_login = now() WHERE id = ?";
        jdbc.update(sql, userId);
    }

    public Optional<UserRecord> findByEmail(String email) {
        String sql = """
                select id, email, username, password_hash, enabled
                from users
                where email = ?
                """;

        try {
            List<UserRecord> users = jdbc.query(sql, (rs, rowNum) ->
                    new UserRecord(
                            rs.getString("email"),
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getBoolean("enabled"),
                            List.of()
                    ), email);

                    if (users.isEmpty()) {
                        return Optional.empty();
                    }

                    UserRecord baseUser = users.getFirst();

                    String authSql = """
                            SELECT authority
                            FROM authorities
                            WHERE user_id = ?
                            """;

                    List<String> authorities = jdbc.query(
                            authSql,
                            (rs, rowNum) -> rs.getString("authority"),
                            baseUser.id());

                    return Optional.of(new UserRecord(
                            baseUser.email(),
                            baseUser.id(),
                            baseUser.username(),
                            baseUser.passwordHash(),
                            baseUser.enabled(),
                            authorities));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

}
