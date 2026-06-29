package com.promptvault.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Minimal security configuration.
 *
 * This assignment explicitly does not require authentication or access control
 * to be enforced at the framework level (see the brief). Authentication is
 * handled with a simple session based mechanism in the controllers, so here we
 * simply disable the default Spring Security login form and allow all requests.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF so the plain HTML forms can post without a token.
                .csrf(csrf -> csrf.disable())
                // Disable the built-in form login so POST /login reaches AuthController.
                .formLogin(form -> form.disable())
                // Disable HTTP Basic auth.
                .httpBasic(basic -> basic.disable())
                // Allow same-origin frames so the H2 console renders in the demo profile.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                // Let the application's own controllers manage access.
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
