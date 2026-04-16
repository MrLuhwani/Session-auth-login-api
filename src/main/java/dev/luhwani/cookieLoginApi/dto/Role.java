package dev.luhwani.cookieLoginApi.dto;

public enum Role {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");
    
    private final String authority;
    Role(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }

}
