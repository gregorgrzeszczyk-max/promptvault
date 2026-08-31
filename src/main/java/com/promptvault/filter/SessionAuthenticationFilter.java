package com.promptvault.filter;

import com.promptvault.controller.SessionUtil;
import com.promptvault.entity.User;
import com.promptvault.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * PVAULT-003: Bridges the manual session-based auth model into the Spring Security
 * SecurityContext so that framework RBAC rules (hasRole, hasAnyRole) apply correctly.
 *
 * On each request, if a User object is present in the HTTP session, a
 * UsernamePasswordAuthenticationToken is created with the appropriate ROLE_USER or
 * ROLE_ADMIN authority and set on the SecurityContextHolder.
 */
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public SessionAuthenticationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        User sessionUser = SessionUtil.currentUser(request.getSession(false));

        if (sessionUser != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Determine the Spring Security role (must be prefixed with ROLE_).
            String springRole = "ROLE_" + sessionUser.getRole().toUpperCase();
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(springRole));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            sessionUser.getUsername(), null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
