package dev.luhwani.cookieLoginApi.services;

public interface SecurityService {

    String hashPassword(CharSequence rawpassword);

    boolean passwordsMatch(CharSequence storedHash, String rawPassword);

}

// this service method may not need to exist, because spring security is already
// doing authentication