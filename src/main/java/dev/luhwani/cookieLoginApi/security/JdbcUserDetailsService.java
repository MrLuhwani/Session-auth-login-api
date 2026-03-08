package dev.luhwani.cookieLoginApi.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import dev.luhwani.cookieLoginApi.dto.User;
import dev.luhwani.cookieLoginApi.repositories.UserRepository;

@Service
public class JdbcUserDetailsService implements UserDetailsService {

    private final UserRepository userRepo;

    public JdbcUserDetailsService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return CustomUserPrincipal.fromUser(user);
    }
}

/*
package dev.luhwani.cookieLoginApi.security;

import dev.luhwani.cookieLoginApi.dto.UserRecord;
import dev.luhwani.cookieLoginApi.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class JdbcUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public JdbcUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserRecord user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return CustomUserPrincipal.fromUser(user);
    }
} */