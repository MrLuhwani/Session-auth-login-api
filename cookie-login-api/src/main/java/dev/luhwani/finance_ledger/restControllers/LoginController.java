package dev.luhwani.finance_ledger.restControllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dev.luhwani.finance_ledger.customExceptions.InvalidEmailException;
import dev.luhwani.finance_ledger.domain.user.LoginRequest;
import dev.luhwani.finance_ledger.domain.user.User;
import dev.luhwani.finance_ledger.services.UserService;
import dev.luhwani.finance_ledger.services.Utils;

@RestController
public class LoginController {

    // private static final Logger log
    private final UserService userService;

    public LoginController(UserService userService) {
        this.userService = userService;
    }
    

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody LoginRequest loginRequest) {
        if (loginRequest == null || loginRequest.getEmail() == null || loginRequest.getPassword() == null) {
            throw new InvalidEmailException();
        }
        if (!Utils.validEmail(loginRequest.getEmail())) {
            throw new InvalidEmailException();
        }
        return ResponseEntity.ok(userService.login(loginRequest));
    }

}
