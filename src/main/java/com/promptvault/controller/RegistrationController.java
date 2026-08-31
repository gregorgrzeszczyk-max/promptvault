package com.promptvault.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import com.promptvault.dto.UserRegistrationDTO;
import com.promptvault.entity.User;
import com.promptvault.service.SecurityAuditLogger;
import com.promptvault.service.UserService;

/**
 * PVAULT-007 — Mass-Assignment Fix
 * PVAULT-P2-07 — Bean Validation (CWE-20): the registration DTO is now
 * validated with {@code @Valid}; constraint violations are surfaced to the
 * form instead of reaching the service layer.
 * PVAULT-P2-02 — Security audit logging (CWE-778): successful registrations
 * are recorded in the security audit log.
 *
 * Accepts a UserRegistrationDTO instead of the full User entity so that
 * sensitive fields (role, active, id …) cannot be supplied by the caller.
 * The DTO is manually mapped to a new User with only the permitted fields.
 */
@Controller
public class RegistrationController {

    private final UserService userService;
    private final SecurityAuditLogger auditLogger;

    public RegistrationController(UserService userService, SecurityAuditLogger auditLogger) {
        this.userService = userService;
        this.auditLogger = auditLogger;
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registrationForm", new UserRegistrationDTO());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registrationForm") UserRegistrationDTO form,
                           BindingResult bindingResult,
                           Model model) {
        // PVAULT-P2-07: reject invalid input before it reaches the service layer.
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", bindingResult.getAllErrors().get(0).getDefaultMessage());
            model.addAttribute("registrationForm", form);
            return "register";
        }
        try {
            // Map only the explicitly allowed fields to a fresh User entity.
            // role, active, and id are NOT carried over from the request.
            User user = new User();
            user.setUsername(form.getUsername());
            user.setPassword(form.getPassword());
            user.setFirstName(form.getFirstName());
            user.setLastName(form.getLastName());
            user.setEmail(form.getEmail());

            userService.registerUser(user);
            auditLogger.registration(form.getUsername(), form.getEmail());
            return "redirect:/login?registered";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("registrationForm", form);
            return "register";
        }
    }
}
