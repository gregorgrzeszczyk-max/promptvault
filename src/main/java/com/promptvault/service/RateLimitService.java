package com.promptvault.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * PVAULT-P2-01 (A06/A07 — CWE-307) — Login brute-force protection.
 * PVAULT-P2-06 (A06 — CWE-307) — Prompt submission rate limiting.
 *
 * Simple in-memory, thread-safe rate limiter suitable for this single-node
 * demo application. Two independent controls are provided:
 *
 * 1. Login attempt tracking with temporary lockout:
 *    after {@link #MAX_LOGIN_ATTEMPTS} failed attempts for a given
 *    username (or client IP) within {@link #LOGIN_WINDOW}, further attempts
 *    are rejected for {@link #LOCKOUT_DURATION}.
 *
 * 2. Sliding-window action limiting (prompt save / AI submission):
 *    at most {@link #MAX_ACTIONS_PER_WINDOW} actions per user within
 *    {@link #ACTION_WINDOW}.
 *
 * For a clustered production deployment this should be replaced with a
 * shared store (e.g. Redis + a token-bucket library such as Bucket4j).
 */
@Service
public class RateLimitService {

    // ---- Login lockout policy -----------------------------------------
    static final int MAX_LOGIN_ATTEMPTS = 5;
    static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);
    static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    // ---- Prompt action policy -----------------------------------------
    static final int MAX_ACTIONS_PER_WINDOW = 20;
    static final Duration ACTION_WINDOW = Duration.ofMinutes(5);

    private final Map<String, LoginAttemptState> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> actionTimestamps = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------
    // Login brute-force protection (CWE-307)
    // ------------------------------------------------------------------

    /** True when the given login key (username or IP) is currently locked out. */
    public boolean isLoginLocked(String key) {
        LoginAttemptState state = loginAttempts.get(normalize(key));
        if (state == null) {
            return false;
        }
        synchronized (state) {
            if (state.lockedUntil != null && Instant.now().isBefore(state.lockedUntil)) {
                return true;
            }
            return false;
        }
    }

    /** Seconds remaining on the lockout for the given key (0 when not locked). */
    public long lockSecondsRemaining(String key) {
        LoginAttemptState state = loginAttempts.get(normalize(key));
        if (state == null || state.lockedUntil == null) {
            return 0;
        }
        long secs = Duration.between(Instant.now(), state.lockedUntil).getSeconds();
        return Math.max(0, secs);
    }

    /**
     * Records a failed login attempt. Returns true when this failure
     * triggered a lockout.
     */
    public boolean recordFailedLogin(String key) {
        LoginAttemptState state = loginAttempts.computeIfAbsent(normalize(key), k -> new LoginAttemptState());
        synchronized (state) {
            Instant now = Instant.now();
            // Reset the counter when the observation window has passed.
            if (state.windowStart == null || Duration.between(state.windowStart, now).compareTo(LOGIN_WINDOW) > 0) {
                state.windowStart = now;
                state.failures = 0;
            }
            state.failures++;
            if (state.failures >= MAX_LOGIN_ATTEMPTS) {
                state.lockedUntil = now.plus(LOCKOUT_DURATION);
                return true;
            }
            return false;
        }
    }

    /** Clears the failure history after a successful login. */
    public void recordSuccessfulLogin(String key) {
        loginAttempts.remove(normalize(key));
    }

    // ------------------------------------------------------------------
    // Prompt save / submission rate limiting (CWE-307)
    // ------------------------------------------------------------------

    /**
     * Sliding-window limiter. Returns true when the action is allowed and
     * records it; returns false when the caller has exceeded the limit.
     */
    public boolean tryAcquireAction(String userKey) {
        Deque<Instant> stamps = actionTimestamps.computeIfAbsent(normalize(userKey), k -> new ConcurrentLinkedDeque<>());
        Instant now = Instant.now();
        Instant cutoff = now.minus(ACTION_WINDOW);
        synchronized (stamps) {
            // Evict entries that fell out of the sliding window.
            while (!stamps.isEmpty() && stamps.peekFirst().isBefore(cutoff)) {
                stamps.pollFirst();
            }
            if (stamps.size() >= MAX_ACTIONS_PER_WINDOW) {
                return false;
            }
            stamps.addLast(now);
            return true;
        }
    }

    // ------------------------------------------------------------------

    private String normalize(String key) {
        return key == null ? "<null>" : key.trim().toLowerCase(Locale.ROOT);
    }

    private static final class LoginAttemptState {
        Instant windowStart;
        int failures;
        Instant lockedUntil;
    }
}
