package promptvault.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import promptvault.model.User;
import promptvault.service.PromptService;

@Controller
public class AdminFlaggedController {

    private final PromptService promptService;

    public AdminFlaggedController(PromptService promptService) {
        this.promptService = promptService;
    }

    @GetMapping("/admin/flagged")
    public String flaggedPrompts(HttpSession session, Model model) {
        User currentUser = currentUser(session);
        if (currentUser == null || !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            return "redirect:/login";
        }
        model.addAttribute("flaggedPrompts", promptService.findFlaggedPrompts());
        return "admin-flagged";
    }

    private User currentUser(HttpSession session) {
        Object value = session.getAttribute("currentUser");
        return value instanceof User ? (User) value : null;
    }
}
