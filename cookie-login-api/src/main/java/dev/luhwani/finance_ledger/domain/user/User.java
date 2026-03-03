package dev.luhwani.finance_ledger.domain.user;

public class User {
    
    private final String email;
    private final Long id;
    private final String username;

    public User(Long id, String email, String username) {
        this.email = email;
        this.id = id;
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}
