package dev.luhwani.cookieLoginApi.services;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.stereotype.Service;

import dev.luhwani.cookieLoginApi.customExceptions.AuthInfrastructureException;
import dev.luhwani.cookieLoginApi.customExceptions.InvalidLoginException;
import dev.luhwani.cookieLoginApi.customExceptions.UnknownDBException;
import dev.luhwani.cookieLoginApi.domain.user.AuthCredentials;
import dev.luhwani.cookieLoginApi.domain.user.LoginRequest;
import dev.luhwani.cookieLoginApi.domain.user.User;
import dev.luhwani.cookieLoginApi.repos.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final SecurityService securityService;

    public UserService(UserRepository userRepo, SecurityService securityService) {
        this.userRepo = userRepo;
        this.securityService = securityService;
    }

    public User login(LoginRequest loginRequest) {
        try {
            String email = loginRequest.getEmail();
            AuthCredentials authCreds = userRepo.findAuthByEmail(email)
                    .orElseThrow(() -> new InvalidLoginException());
            String passwordInputAndSalt = loginRequest.getPassword() + authCreds.salt();
            byte[] hashedPassword = securityService.hashText(passwordInputAndSalt);
            if (!securityService.passwordMatch(hashedPassword, authCreds.passwordHash())) {
                throw new InvalidLoginException();
            }
            User user = new User(authCreds.id(), authCreds.email(), authCreds.username());
            return user;
            // later we will get the users transactions after login
            
        } catch (DataAccessException e) {
            
            // for me to know what error occured
            e.printStackTrace();
            System.err.println(e.getCause());
            if (e instanceof BadSqlGrammarException || e instanceof CannotGetJdbcConnectionException) {
                throw new AuthInfrastructureException();
            } else {
                throw new UnknownDBException("Check the stack trace at userService.login()", e);
            }
        }
    }

}
