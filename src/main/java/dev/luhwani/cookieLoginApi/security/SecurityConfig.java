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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JsonAccessDeniedHandler accessDeniedHandler;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAuthenticationFailureHandler authenticationFailureHandler;
    private final JsonAuthenticationSuccessHandler authenticationSuccessHandler;
    private final JsonLogoutSuccessHandler logoutSuccessHandler;

    public SecurityConfig(JsonAccessDeniedHandler accessDeniedHandler,
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            JsonAuthenticationFailureHandler authenticationFailureHandler,
            JsonAuthenticationSuccessHandler authenticationSuccessHandler,
            JsonLogoutSuccessHandler logoutSuccessHandler) {
        this.accessDeniedHandler = accessDeniedHandler;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
    }

    @Configuration
    public static class JacksonConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // the hardenSessionvmethod was created so that sessions are more secure
    // the migrate session ensures that on authentication, Spring migrates
    // //to a new session ID to prevent session fixation attacks
    // maximum session, set to 2, meaninng you can be logged max, in two places, e.g
    // phone and laptop
    // an addition of a third device deletes the session on the device with the
    // oldest cookie session, and allocates
    // a new session to the new device

    // also look into the concurrent session control, which can be used to prevent
    // multiple logins from the same user, and can be configured to either prevent
    // new logins or expire old sessions when the maximum is reached.

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
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/public/**", "/login", "/register", "/csrf").permitAll()
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                        .anyRequest().authenticated())
                // the csrf token repo ensures that the request is specifically from our site
                // so attackers from another site can't forge requests
                /*
                 * for easier testing of endpoints with postman, you can change uncomment this
                 * code,
                 * and comment the .csrf directly below it
                 * .csrf(csrf -> csrf
                 * .ignoringRequestMatchers("/login", "/register")
                 * .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                 * 
                 * //
                 */
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository()))
                .sessionManagement(this::hardenSession)
                .formLogin(form -> form
                        .loginProcessingUrl("/login")
                        .usernameParameter("email").passwordParameter("password")
                        .successHandler(authenticationSuccessHandler).failureHandler(authenticationFailureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout").invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(logoutSuccessHandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

}
