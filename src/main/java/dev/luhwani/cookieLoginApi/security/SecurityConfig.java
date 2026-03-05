package dev.luhwani.cookieLoginApi.security;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    //the hardenSessionvmethod was created so that sessions are more secure
    //the migrate session ensures that spring creates a new session
    //and assigns the user more frequently
    //maximum session, set to 2, meaninng you can be logged max, in two places, e.g phone and laptop
    //an addition of a third device deletes the session on the device with the oldest cookie session, and allocates
    //a new session to the new device

    private void hardenSession(SessionManagementConfigurer<HttpSecurity> session) {
        session.sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::migrateSession)
                .maximumSessions(2).maxSessionsPreventsLogin(false);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        //every other endpoint requires a session id other than those in the requestMatcher bracket
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/", "/public/**", "/auth/login", "/register", "/csrf")
                .permitAll().anyRequest().authenticated())
                //thecsrf token repo ensures that the request is specifically from our site
                //so attackers from another site can't forge requests
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .sessionManagement(this::hardenSession).formLogin(form -> form.loginPage("/auth/login")
                //the loginPage() represents the GET endpoint that returns your login page,
                //the loginProcessingUrl() represents the POST endpoint login credentials should be sent to if a user tries
                //to login
                //the /me and true ensures the user is always redirected here after successful login
                        .loginProcessingUrl("/auth/login").defaultSuccessUrl("/me", true)
                        .failureUrl("/auth/login?error=true")
                        .permitAll())
                .logout(logout -> logout.logoutUrl("/auth/logout").logoutSuccessUrl("/"));
                //logoutSuccesUrl is where youll be redirected to if logout is successful
        return http.build();
    }

    @Bean
    UserDetailsService jdbcUserDetailsService(DataSource dataSource) {
        return new JdbcUserDetailsManager(dataSource);
    }
}
