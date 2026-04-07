package dev.luhwani.cookieLoginApi.security;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import dev.luhwani.cookieLoginApi.dto.UserRecord;
import dev.luhwani.cookieLoginApi.repositories.UserRepository;

@Service
public class JdbcUserDetailsService implements UserDetailsService {

    private final UserRepository userRepo;

    public JdbcUserDetailsService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    // the unless is so that if the email isn't found, we won't cache
    // a null response
    
    @Cacheable(value = "user-details", key = "#email", unless = "#result == null")
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserRecord user = userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return CustomUserPrincipal.fromUser(user);
    }
}