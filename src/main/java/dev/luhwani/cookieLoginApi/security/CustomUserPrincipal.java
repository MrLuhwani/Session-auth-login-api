package dev.luhwani.cookieLoginApi.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import dev.luhwani.cookieLoginApi.dto.User;

public class CustomUserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String username;
    private final String passwordHash;
    private final boolean enabled;
    private final List<GrantedAuthority> authorities;

    public CustomUserPrincipal(Long id, String email, String username, String passwordHash, boolean enabled,
            List<GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.authorities = authorities;
    }

    public static CustomUserPrincipal fromUser(User creds) {
        return new CustomUserPrincipal(
                creds.id(),
                creds.email(),
                creds.username(),
                (String) creds.passwordHash(),
                creds.enabled(),
                List.of(new SimpleGrantedAuthority(creds.authority())));
    }

    public Long getId() {
        return id;
    }

    // Spring calls this the "username"
    // but for us, login is by email
    @Override
    public String getUsername() {
        return email;
    }

    public String getEmail() {
        return email;
    }

    // this is the app username, not Spring Security's login key
    public String getDisplayUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

}

/*
package dev.luhwani.cookieLoginApi.security;

import dev.luhwani.cookieLoginApi.dto.UserRecord;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String username;
    private final String passwordHash;
    private final boolean enabled;
    private final List<GrantedAuthority> authorities;

    public CustomUserPrincipal(
            Long id,
            String email,
            String username,
            String passwordHash,
            boolean enabled,
            List<GrantedAuthority> authorities
    ) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.authorities = authorities;
    }

    public static CustomUserPrincipal fromUser(UserRecord user) {
        return new CustomUserPrincipal(
                user.id(),
                user.email(),
                user.username(),
                user.passwordHash(),
                user.enabled(),
                List.of(new SimpleGrantedAuthority(user.authority()))
        );
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayUsername() {
        return username;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
} */