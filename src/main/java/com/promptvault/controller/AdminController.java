package promptvault.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import promptvault.model.User;
import promptvault.service.PromptService;
import promptvault.service.UserService;

@Controller
public class AdminController {

    private final UserService userService;
    private final PromptService promptService;

    public AdminController(UserService userService, PromptService promptService) {
        this.userService = userService;
        this.promptService = promptService;
    }

    @GetMapping("/admin")
    public String adminDashboard(HttpSession session, Model model) {
        User currentUser = currentUser(session);
        if (!isAdmin(currentUser)) {
            return "redirect:/login";
        }
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("users", userService.findAllUsers());
        model.addAttribute("flaggedPrompts", promptService.findFlaggedPrompts());
        return "admin-dashboard";
    }

    @PostMapping("/admin/users/{id}/activate")
    public String activateUser(@PathVariable Long id, HttpSession session) {
        User currentUser = currentUser(session);
        if (!isAdmin(currentUser)) {
            return "redirect:/login";
        }
        userService.setUserActive(id, true, currentUser);
        return "redirect:/admin";
    }

    @PostMapping("/admin/users/{id}/deactivate")
    public String deactivateUser(@PathVariable Long id, HttpSession session) {
        User currentUser = currentUser(session);
        if (!isAdmin(currentUser)) {
            return "redirect:/login";
        }
        userService.setUserActive(id, false, currentUser);
        return "redirect:/admin";
    }

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private User currentUser(HttpSession session) {
        Object value = session.getAttribute("currentUser");
        return value instanceof User ? (User) value : null;
    }
}
