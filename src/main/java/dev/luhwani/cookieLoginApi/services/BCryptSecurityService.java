package dev.luhwani.cookieLoginApi.services;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

//this service method may not need to exist

@Service
public class BCryptSecurityService implements SecurityService{

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public String hashPassword(CharSequence rawPassword){
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean passwordsMatch(CharSequence rawPassword, String storedHash) {
        return passwordEncoder.matches(rawPassword, storedHash);
    }
}
