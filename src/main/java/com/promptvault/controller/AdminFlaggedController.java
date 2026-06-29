package com.promptvault.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.promptvault.entity.User;
import com.promptvault.service.PromptService;

/**
 * Lets an admin review prompts that were flagged for containing policy keywords.
 */
@Controller
public class AdminFlaggedController {

    private final PromptService promptService;

    public AdminFlaggedController(PromptService promptService) {
        this.promptService = promptService;
    }

    @GetMapping("/admin/flagged")
    public String flaggedPrompts(HttpSession session, Model model) {
        User currentUser = SessionUtil.currentUser(session);
        if (!SessionUtil.isAdmin(currentUser)) {
            return "redirect:/login";
        }
        model.addAttribute("flaggedList", promptService.findFlaggedPrompts());
        return "admin-flagged";
    }
}
