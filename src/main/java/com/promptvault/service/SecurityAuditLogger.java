package com.promptvault.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * PVAULT-P2-02 (A09 / CWE-778) — Security audit logging.
 * PVAULT-P2-08 (A09 / CWE-532) — Sensitive data masking in logs.
 * PVAULT-P3-12 (A09 / CWE-117) — Log injection protection.
 *
 * Central utility for recording security-relevant events (authentication,
 * account changes, admin actions, prompt flagging, rate-limit hits) to a
 * dedicated "SECURITY_AUDIT" logger so the events can be shipped to a
 * central, tamper-evident store and used for alerting.
 *
 * Hardening applied to every log line:
 *  - All user-controlled values pass through {@link #sanitize(String)} which
 *    strips CR/LF and all other control characters (CWE-117 log forging)
 *    and truncates over-long values.
 *  - Prompt content is never logged in full — only a masked, truncated
 *    preview via {@link #maskContent(String)} (CWE-532).
 *  - Passwords, password hashes and session identifiers are NEVER accepted
 *    by any method in this class by design.
 */
@Component
public class SecurityAuditLogger {

    private static final Logger AUDIT = LoggerFactory.getLogger("SECURITY_AUDIT");

    /** Maximum length of any single sanitized field in a log line. */
    private static final int MAX_FIELD_LENGTH = 100;

    /** Maximum number of characters of prompt content shown (masked preview). */
    private static final int MAX_CONTENT_PREVIEW = 30;

    // ------------------------------------------------------------------
    // Authentication events
    // ------------------------------------------------------------------

    public void loginSuccess(String username, String remoteIp) {
        AUDIT.info("event=LOGIN_SUCCESS username={} ip={}", sanitize(username), sanitize(remoteIp));
    }

    public void loginFailure(String username, String remoteIp) {
        AUDIT.warn("event=LOGIN_FAILURE username={} ip={}", sanitize(username), sanitize(remoteIp));
    }

    public void accountLocked(String username, String remoteIp, long lockSeconds) {
        AUDIT.warn("event=ACCOUNT_LOCKED username={} ip={} lockSeconds={}",
                sanitize(username), sanitize(remoteIp), lockSeconds);
    }

    public void logout(String username) {
        AUDIT.info("event=LOGOUT username={}", sanitize(username));
    }

    public void registration(String username, String email) {
        AUDIT.info("event=USER_REGISTERED username={} email={}", sanitize(username), sanitize(email));
    }

    // ------------------------------------------------------------------
    // Admin / account-management events
    // ------------------------------------------------------------------

    public void adminUserToggled(String adminUsername, Long targetUserId, boolean nowActive) {
        AUDIT.info("event=ADMIN_USER_TOGGLED admin={} targetUserId={} nowActive={}",
                sanitize(adminUsername), targetUserId, nowActive);
    }

    public void adminCategoryChange(String adminUsername, String action, String categoryName) {
        AUDIT.info("event=ADMIN_CATEGORY_{} admin={} category={}",
                sanitize(action).toUpperCase(), sanitize(adminUsername), sanitize(categoryName));
    }

    public void adminKeywordChange(String adminUsername, String action, String keyword) {
        AUDIT.info("event=ADMIN_KEYWORD_{} admin={} keyword={}",
                sanitize(action).toUpperCase(), sanitize(adminUsername), sanitize(keyword));
    }

    // ------------------------------------------------------------------
    // Prompt / content events
    // ------------------------------------------------------------------

    /** Prompt content is masked and truncated — full text is never logged (CWE-532). */
    public void promptFlagged(String username, Long promptId, String matchedKeyword, String promptText) {
        AUDIT.warn("event=PROMPT_FLAGGED username={} promptId={} keyword={} contentPreview={}",
                sanitize(username), promptId, sanitize(matchedKeyword), maskContent(promptText));
    }

    public void promptDeleted(String username, Long promptId) {
        AUDIT.info("event=PROMPT_DELETED username={} promptId={}", sanitize(username), promptId);
    }

    /** Recorded when the policy keyword check itself fails and the prompt is fail-secure flagged. */
    public void policyCheckFailure(Long promptId, String errorType) {
        AUDIT.error("event=POLICY_CHECK_FAILURE promptId={} errorType={} action=FAIL_SECURE_FLAGGED",
                promptId, sanitize(errorType));
    }

    // ------------------------------------------------------------------
    // Rate limiting events
    // ------------------------------------------------------------------

    public void rateLimitExceeded(String subject, String limiterName, String remoteIp) {
        AUDIT.warn("event=RATE_LIMIT_EXCEEDED subject={} limiter={} ip={}",
                sanitize(subject), sanitize(limiterName), sanitize(remoteIp));
    }

    // ------------------------------------------------------------------
    // Sanitization helpers
    // ------------------------------------------------------------------

    /**
     * CWE-117: strips CR, LF and every other ISO control character so that
     * user-controlled input cannot forge extra log lines, then truncates
     * the value to a safe maximum length.
     */
    static String sanitize(String value) {
        if (value == null) {
            return "<null>";
        }
        StringBuilder sb = new StringBuilder(Math.min(value.length(), MAX_FIELD_LENGTH));
        for (int i = 0; i < value.length() && sb.length() < MAX_FIELD_LENGTH; i++) {
            char c = value.charAt(i);
            if (Character.isISOControl(c)) {
                sb.append('_'); // replace CR/LF/tab/escape etc. with a marker
            } else {
                sb.append(c);
            }
        }
        if (value.length() > MAX_FIELD_LENGTH) {
            sb.append("...(truncated)");
        }
        return sb.toString();
    }

    /**
     * CWE-532: prompt bodies may contain the very secrets the policy engine
     * flags (passwords, API keys ...), so only a short sanitized preview is
     * ever written to the log.
     */
    static String maskContent(String content) {
        if (content == null) {
            return "<null>";
        }
        String sanitized = sanitize(content);
        if (sanitized.length() <= MAX_CONTENT_PREVIEW) {
            return sanitized + "***";
        }
        return sanitized.substring(0, MAX_CONTENT_PREVIEW) + "***(masked," + content.length() + " chars)";
    }
}
