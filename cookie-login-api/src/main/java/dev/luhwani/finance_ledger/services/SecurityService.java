package dev.luhwani.finance_ledger.services;

public interface SecurityService {

    byte[] hashText(String input);

    boolean passwordMatch(byte[] userInput, byte[] storedHash);
    
}
