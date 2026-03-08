package dev.luhwani.cookieLoginApi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final AccessDeniedHandler accessDeniedHandler;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AuthenticationFailureHandler authenticationFailureHandler;
    private final AuthenticationSuccessHandler authenticationSuccessHandler;
    private final LogoutSuccessHandler logoutSuccessHandler;



    public SecurityConfig(AccessDeniedHandler accessDeniedHandler, AuthenticationEntryPoint authenticationEntryPoint,
            AuthenticationFailureHandler authenticationFailureHandler,
            AuthenticationSuccessHandler authenticationSuccessHandler, LogoutSuccessHandler logoutSuccessHandler) {
        this.accessDeniedHandler = accessDeniedHandler;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // the hardenSessionvmethod was created so that sessions are more secure
    // the migrate session ensures that spring creates a new session
    // and assigns the user more frequently
    // maximum session, set to 2, meaninng you can be logged max, in two places, e.g
    // phone and laptop
    // an addition of a third device deletes the session on the device with the
    // oldest cookie session, and allocates
    // a new session to the new device

    private void hardenSession(SessionManagementConfigurer<HttpSecurity> session) {
        session.sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::migrateSession)
                .maximumSessions(2).maxSessionsPreventsLogin(false);
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
    
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // every other endpoint requires a session id other than those in the
        // requestMatcher bracket
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/", "/public/**", "/login", "/register", "/csrf")
                .permitAll().requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                .anyRequest().authenticated())
                // the csrf token repo ensures that the request is specifically from our site
                // so attackers from another site can't forge requests
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .sessionManagement(this::hardenSession).formLogin(form ->
                        // the loginProcessingUrl() represents the POST endpoint login credentials
                        // should be sent to if a user tries
                        // to login. Although, it might not be useful since I have provided
                        //a custom success and failure handler
                        // the /me and true ensures the user is always redirected here after successful
                        // login
                        form.loginProcessingUrl("/login").successHandler(authenticationSuccessHandler)
                        .failureHandler(authenticationFailureHandler)
                        .permitAll())
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessHandler(logoutSuccessHandler)).exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        // you might prefer if spring handles the logout url
        return http.build();
    }

}

/*
package dev.luhwani.cookieLoginApi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JsonAccessDeniedHandler accessDeniedHandler;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAuthenticationFailureHandler authenticationFailureHandler;
    private final JsonAuthenticationSuccessHandler authenticationSuccessHandler;
    private final JsonLogoutSuccessHandler logoutSuccessHandler;

    public SecurityConfig(
            JsonAccessDeniedHandler accessDeniedHandler,
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            JsonAuthenticationFailureHandler authenticationFailureHandler,
            JsonAuthenticationSuccessHandler authenticationSuccessHandler,
            JsonLogoutSuccessHandler logoutSuccessHandler
    ) {
        this.accessDeniedHandler = accessDeniedHandler;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    private void hardenSession(SessionManagementConfigurer<HttpSecurity> session) {
        session
                .sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::migrateSession)
                .maximumSessions(2)
                .maxSessionsPreventsLogin(false);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/register", "/auth/login", "/csrf").permitAll()
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository())
                )
                .sessionManagement(this::hardenSession)
                .formLogin(form -> form
                        .loginProcessingUrl("/auth/login")
                        .successHandler(authenticationSuccessHandler)
                        .failureHandler(authenticationFailureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(logoutSuccessHandler)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }
} */