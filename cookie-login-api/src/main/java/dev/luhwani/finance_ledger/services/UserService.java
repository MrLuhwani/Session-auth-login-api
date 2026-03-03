package dev.luhwani.finance_ledger.services;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.stereotype.Service;

import dev.luhwani.finance_ledger.customExceptions.AuthInfrastructureException;
import dev.luhwani.finance_ledger.customExceptions.InvalidLoginException;
import dev.luhwani.finance_ledger.customExceptions.UnknownDBException;
import dev.luhwani.finance_ledger.domain.user.AuthCredentials;
import dev.luhwani.finance_ledger.domain.user.LoginRequest;
import dev.luhwani.finance_ledger.domain.user.User;
import dev.luhwani.finance_ledger.repos.UserRepository;

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
