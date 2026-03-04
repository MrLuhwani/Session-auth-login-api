package dev.luhwani.cookieLoginApi.services;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Service;

//I created securityService as an interface so that when I start
//using proper encryption, it won't be hard for me to update the code

@Service
public class LoginSecurityService implements SecurityService {

    @Override
    public byte[] hashText(String input) {
        try {
            MessageDigest digester = MessageDigest.getInstance("SHA-256");
            byte[] inputByte = input.getBytes();
            byte[] hashByte = digester.digest(inputByte);
            return hashByte;
        } catch (NoSuchAlgorithmException e) {
            // this should never happen since SHA-256 is a standard algorithm
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    @Override
    public boolean passwordMatch(byte[] userInput, byte[] storedHash) {
        return MessageDigest.isEqual(userInput, storedHash);
    }
}
