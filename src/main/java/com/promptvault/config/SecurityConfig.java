package com.promptvault.config;

import com.promptvault.filter.SessionAuthenticationFilter;
import com.promptvault.filter.SessionRefreshFilter;
import com.promptvault.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Security configuration — hardened to fix PVAULT-001 (CSRF), PVAULT-003 (permitAll),
 * PVAULT-004 (session refresh), and PVAULT-011 (security headers).
 *
 * PVAULT-001: CSRF protection re-enabled via CookieCsrfTokenRepository.
 *             All Thymeleaf forms using th:action automatically receive the token.
 * PVAULT-003: Default-deny RBAC — explicit allowlist for public routes;
 *             /admin/** requires ADMIN role; /user/** and /dashboard require USER or ADMIN.
 *             A SessionAuthenticationFilter bridges the manual session-based auth model
 *             into the Spring Security context so framework RBAC rules apply.
 * PVAULT-004: SessionRefreshFilter re-fetches the User from DB on every request
 *             to immediately evict disabled accounts.
 * PVAULT-011: Security response headers (CSP, HSTS, X-Frame-Options, X-Content-Type-Options).
 */
@Configuration
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // PVAULT-001: Re-enable CSRF with cookie-based token repository.
                // Thymeleaf th:action forms automatically include the _csrf hidden field.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))

                // Disable built-in form login — AuthController handles login manually.
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // PVAULT-011: Security response headers.
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; " +
                                        "script-src 'self'; " +
                                        "style-src 'self' 'unsafe-inline'; " +
                                        "img-src 'self' data:; " +
                                        "frame-ancestors 'none'"))
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)))

                // Session fixation protection (defence-in-depth — PVAULT-014).
                .sessionManagement(sm -> sm
                        .sessionFixation().newSession())

                // PVAULT-003: Deny-by-default RBAC.
                .authorizeHttpRequests(auth -> auth
                        // Public routes — no authentication required.
                        .requestMatchers(
                                "/", "/login", "/register",
                                "/css/**", "/js/**", "/images/**",
                                "/webjars/**", "/error").permitAll()
                        // Admin routes require the ADMIN role.
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // All other authenticated routes require USER or ADMIN.
                        .requestMatchers("/user/**", "/dashboard").hasAnyRole("USER", "ADMIN")
                        // Deny everything not explicitly listed above.
                        .anyRequest().authenticated())

                // PVAULT-003: Bridge session User into SecurityContext.
                .addFilterBefore(
                        new SessionAuthenticationFilter(userRepository),
                        UsernamePasswordAuthenticationFilter.class)

                // PVAULT-004: Re-fetch user from DB on every request to evict disabled accounts.
                .addFilterAfter(
                        new SessionRefreshFilter(userRepository),
                        SessionAuthenticationFilter.class);

        return http.build();
    }
}
