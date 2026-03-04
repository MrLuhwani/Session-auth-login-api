package dev.luhwani.cookieLoginApi.repos;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import dev.luhwani.cookieLoginApi.domain.user.AuthCredentials;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AuthCredentials> findAuthByEmail(String emailInput) {
        String sql = """
                SELECT id, username, password_hash, password_salt FROM users WHERE email = ?;""";

        //I wanted to avoid a DataAccessException, so I queried for a list
        //instead of a single object, then I use the optional findFirst()
        //to get the single element in the list
        RowMapper<AuthCredentials> rowMapper = (r, i) -> {
            Long id= r.getLong("id");
            String username = r.getString("username");
            byte[] passwordHash = r.getBytes("password_hash");
            String salt = r.getString("password_salt");
            return new AuthCredentials(id, emailInput, username, passwordHash, salt);
        };
        ;
        List<AuthCredentials> authCreds = jdbc.query(sql, rowMapper, emailInput);
        return authCreds.stream().findFirst();
    }

}
