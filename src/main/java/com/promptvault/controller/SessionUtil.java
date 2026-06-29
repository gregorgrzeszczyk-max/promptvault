package com.promptvault.controller;

import jakarta.servlet.http.HttpSession;
import com.promptvault.entity.User;

/**
 * Small helper used by the controllers to read the currently logged in user
 * from the HTTP session and to check whether that user is an administrator.
 */
final class SessionUtil {

    static final String SESSION_USER = "currentUser";

    private SessionUtil() {
    }

    static User currentUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_USER);
        return value instanceof User ? (User) value : null;
    }

    static boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }
}
