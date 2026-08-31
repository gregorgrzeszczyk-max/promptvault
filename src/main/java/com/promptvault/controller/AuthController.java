package com.promptvault.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.promptvault.entity.User;
import com.promptvault.service.RateLimitService;
import com.promptvault.service.SecurityAuditLogger;
import com.promptvault.service.UserService;

import java.util.Optional;

/**
 * Login / logout controller.
 *
 * PVAULT-P2-01 (A06/A07 — CWE-307): login attempts are rate limited per
 * username AND per client IP; after repeated failures the account/IP is
 * temporarily locked out.
 * PVAULT-P2-02 (A09 — CWE-778): all authentication events (success, failure,
 * lockout, logout) are recorded via SecurityAuditLogger.
 * PVAULT-P3-11 (A07/A08): logout now requires POST (with CSRF token) —
 * the GET mapping has been removed so logout cannot be forged via a link/img.
 */
@Controller
public class AuthController {

    private final UserService userService;
    private final RateLimitService rateLimitService;
    private final SecurityAuditLogger auditLogger;

    public AuthController(UserService userService,
                          RateLimitService rateLimitService,
                          SecurityAuditLogger auditLogger) {
        this.userService = userService;
        this.rateLimitService = rateLimitService;
        this.auditLogger = auditLogger;
    }

    @GetMapping({"/", "/login"})
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpServletRequest request, Model model) {
        String remoteIp = clientIp(request);
        String usernameKey = "user:" + (username == null ? "" : username.trim());
        String ipKey = "ip:" + remoteIp;

        // PVAULT-P2-01: refuse the attempt outright while a lockout is active.
        if (rateLimitService.isLoginLocked(usernameKey) || rateLimitService.isLoginLocked(ipKey)) {
            long waitSeconds = Math.max(
                    rateLimitService.lockSecondsRemaining(usernameKey),
                    rateLimitService.lockSecondsRemaining(ipKey));
            auditLogger.rateLimitExceeded(username, "login", remoteIp);
            model.addAttribute("error",
                    "Too many failed login attempts. Please try again in about "
                            + Math.max(1, waitSeconds / 60 + 1) + " minute(s).");
            return "login";
        }

        Optional<User> user = userService.authenticate(username, password);
        if (user.isEmpty()) {
            // PVAULT-P2-01: count the failure against both the username and the IP.
            boolean lockedUser = rateLimitService.recordFailedLogin(usernameKey);
            boolean lockedIp = rateLimitService.recordFailedLogin(ipKey);
            auditLogger.loginFailure(username, remoteIp);
            if (lockedUser || lockedIp) {
                auditLogger.accountLocked(username, remoteIp,
                        Math.max(rateLimitService.lockSecondsRemaining(usernameKey),
                                rateLimitService.lockSecondsRemaining(ipKey)));
                model.addAttribute("error",
                        "Too many failed login attempts. The account is temporarily locked — please try again later.");
            } else {
                model.addAttribute("error", "Invalid username, password, or inactive account");
            }
            return "login";
        }

        rateLimitService.recordSuccessfulLogin(usernameKey);
        rateLimitService.recordSuccessfulLogin(ipKey);
        auditLogger.loginSuccess(user.get().getUsername(), remoteIp);

        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession newSession = request.getSession(true);
        newSession.setAttribute(SessionUtil.SESSION_USER, user.get());
        return SessionUtil.isAdmin(user.get()) ? "redirect:/admin" : "redirect:/dashboard";
    }

    /**
     * PVAULT-P3-11: logout is POST-only so it is covered by CSRF protection.
     * All logout links in the templates are POST forms carrying the CSRF token.
     */
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        User current = SessionUtil.currentUser(session);
        if (current != null) {
            auditLogger.logout(current.getUsername());
        }
        session.invalidate();
        return "redirect:/login?logout";
    }

    /** Best-effort client IP (direct connection; X-Forwarded-For handling belongs at the proxy). */
    private String clientIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return ip == null ? "unknown" : ip;
    }
}
