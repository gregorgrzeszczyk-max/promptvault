package com.promptvault.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.promptvault.entity.User;
import com.promptvault.service.UserService;

/**
 * Admin landing page and user management (view users, enable/disable accounts).
 */
@Controller
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/admin")
    public String adminDashboard(HttpSession session, Model model) {
        User currentUser = SessionUtil.currentUser(session);
        if (!SessionUtil.isAdmin(currentUser)) {
            return "redirect:/login";
        }
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("usersList", userService.findAllUsers());
        return "admin-dashboard";
    }

    @PostMapping("/admin/toggle-user")
    public String toggleUser(@RequestParam Long userId, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = SessionUtil.currentUser(session);
        if (!SessionUtil.isAdmin(currentUser)) {
            return "redirect:/login";
        }
        try {
            userService.toggleUserActive(userId, currentUser);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin";
    }
}
