package dev.luhwani.cookieLoginApi.repositories;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Long registerUserAndReturnId(RegisterRequest req, String passwordHash) {
        String sql = """
                INSERT INTO users (email, username, password_hash, enabled, created_at)
                VALUES (?, ?, ?, true, NOW())
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            // confused of why I needed to create this "id"
            PreparedStatement ps = connection.prepareStatement(sql, new String[] { "id" });
            ps.setString(1, req.email().trim());
            ps.setString(2, req.username().trim());
            ps.setString(3, passwordHash);
            return ps;
        }, keyHolder);

        Number idKey = keyHolder.getKey();
        if (idKey == null) {

            log.error("Failed to generate user Id for {}", req.email());

            throw new AuthInfrastructureException("Failed to retrieve generated user ID.");
        }

        Long userId = idKey.longValue();
        String authoritySql = "INSERT INTO authorities (user_id, authority) VALUES (?, ?)";
        jdbc.update(authoritySql, userId, "ROLE_USER");

        log.info("New registered user with id: {}", userId);

        return userId;
    }

    public void setLastLogin(Long userId) {
        String sql = "UPDATE users SET last_login = now() WHERE id = ?";
        jdbc.update(sql, userId);

        log.info("Login from user id: {}", userId);
    }

    public Optional<UserRecord> findByEmail(String email) {
        String sql = """
                SELECT id, email, username, password_hash, enabled, locked_until
                FROM users
                WHERE email = ?
                """;

        try {
            List<UserRecord> users = jdbc.query(sql, (rs, rowNum) ->
                    new UserRecord(
                            rs.getString("email"),
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getBoolean("enabled"),
                            rs.getTimestamp("locked_until"),
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
                            baseUser.lockedUntil(),
                            authorities));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void tempAcctLock(String email, LocalDateTime until) {
        String sql = "UPDATE users SET locked_until = ? WHERE email = ?";
        jdbc.update(sql, Timestamp.valueOf(until), email);
    }

}
