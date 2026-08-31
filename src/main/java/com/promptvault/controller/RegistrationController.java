package com.promptvault.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import com.promptvault.dto.UserRegistrationDTO;
import com.promptvault.entity.User;
import com.promptvault.service.UserService;

/**
 * PVAULT-007 — Mass-Assignment Fix
 *
 * Accepts a UserRegistrationDTO instead of the full User entity so that
 * sensitive fields (role, active, id …) cannot be supplied by the caller.
 * The DTO is manually mapped to a new User with only the permitted fields.
 */
@Controller
public class RegistrationController {

    private final UserService userService;

    public RegistrationController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registrationForm", new UserRegistrationDTO());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("registrationForm") UserRegistrationDTO form, Model model) {
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
            return "redirect:/login?registered";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("registrationForm", form);
            return "register";
        }
    }
}
