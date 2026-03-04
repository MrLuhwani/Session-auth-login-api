package dev.luhwani.cookieLoginApi.services;

public interface SecurityService {

    byte[] hashText(String input);

    boolean passwordMatch(byte[] userInput, byte[] storedHash);
    
}
