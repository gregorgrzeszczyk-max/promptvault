package com.promptvault.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * PVAULT-P2-03 (A10 — CWE-209 / CWE-755) — Global exception handling.
 *
 * Catches every exception that escapes a controller and returns a safe,
 * user-friendly Thymeleaf error view instead of the default whitelabel
 * page. The real exception (including the stack trace) is logged
 * server-side only — no internal details, class names, SQL or stack
 * traces are ever rendered to the client.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 404 — unknown static resource / route that reached the dispatcher. */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoResourceFoundException ex) {
        log.info("404 Not Found: {}", ex.getResourcePath());
        return "error/404";
    }

    /** 400 — malformed request input (e.g. ?id=abc for a numeric parameter). */
    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HandlerMethodValidationException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(Exception ex, Model model) {
        // Log the technical detail server-side only.
        log.warn("400 Bad Request: {}", ex.getMessage());
        model.addAttribute("errorTitle", "Invalid request");
        model.addAttribute("errorMessage",
                "The request could not be processed because it contained invalid input.");
        return "error";
    }

    /**
     * 409 — optimistic locking conflict (PVAULT-P3-13, A10).
     * Raised when two users/tabs edit the same prompt concurrently.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleOptimisticLock(OptimisticLockingFailureException ex, Model model) {
        log.warn("409 Conflict — optimistic locking failure: {}", ex.getMessage());
        model.addAttribute("errorTitle", "Concurrent edit detected");
        model.addAttribute("errorMessage",
                "This item was modified by another session while you were editing it. "
                        + "Please go back, reload the page and re-apply your changes.");
        return "error";
    }

    /**
     * 500 — catch-all. CWE-209: the exception message and stack trace are
     * logged server-side and never exposed to the user.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneric(Exception ex) {
        log.error("Unhandled exception caught by GlobalExceptionHandler", ex);
        return "error/500";
    }
}
