package com.promptvault.filter;

import com.promptvault.controller.SessionUtil;
import com.promptvault.entity.User;
import com.promptvault.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * PVAULT-004: Re-fetches the User from the database on every authenticated request
 * to immediately evict sessions for accounts that have been disabled or deleted
 * since the session was created.
 *
 * Without this filter, disabling a user in the admin panel only flips the DB flag;
 * the stale User in the HTTP session retains isActive=true and the user keeps full
 * access until their 30-minute session timeout.
 */
public class SessionRefreshFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public SessionRefreshFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null) {
            filterChain.doFilter(request, response);
            return;
        }

        User sessionUser = SessionUtil.currentUser(session);
        if (sessionUser == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Re-fetch the user from the database to get the latest isActive / role state.
        User freshUser = userRepository.findById(sessionUser.getId()).orElse(null);

        if (freshUser == null || !freshUser.isActive()) {
            // Account has been deleted or disabled since login — invalidate the session.
            session.invalidate();
            SecurityContextHolder.clearContext();
            response.sendRedirect(request.getContextPath() + "/login?disabled");
            return;
        }

        // Update the session object so subsequent requests see the latest role/state.
        session.setAttribute("user", freshUser);

        filterChain.doFilter(request, response);
    }
}
