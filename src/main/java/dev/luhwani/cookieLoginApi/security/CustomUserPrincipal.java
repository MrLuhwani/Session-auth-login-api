package dev.luhwani.cookieLoginApi.security;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import dev.luhwani.cookieLoginApi.dto.UserRecord;

public class CustomUserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String username;
    private final String passwordHash;
    private final boolean enabled;
    private final boolean notLocked;
    private final List<SimpleGrantedAuthority> authorities;


    public CustomUserPrincipal(Long id, String email, String username, String passwordHash, boolean enabled,
            boolean notLocked, List<SimpleGrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.notLocked = notLocked;
        this.authorities = authorities;
    }

    public static CustomUserPrincipal fromUser(UserRecord user) {
        List<SimpleGrantedAuthority> grantedAuthorities = user.authorities().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        LocalDateTime lockTime = user.lockedUntil().toLocalDateTime();
        boolean notLocked = lockTime == null || LocalDateTime.now().isAfter(lockTime) ? true : false;
        return new CustomUserPrincipal(
                user.id(),
                user.email(),
                user.username(),
                user.passwordHash(),
                user.enabled(),
                notLocked,
                grantedAuthorities
        );
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

    public boolean isAccountNonLocked() {
      return notLocked;
   }

    @Override
    public Collection<? extends SimpleGrantedAuthority> getAuthorities() {
        return authorities;
    }

}
