package com.promptvault.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.promptvault.entity.User;
import com.promptvault.service.PromptService;

/**
 * Shows the logged in user's personal dashboard: their own prompts and their
 * submission history.
 */
@Controller
public class DashboardController {

    private final PromptService promptService;

    public DashboardController(PromptService promptService) {
        this.promptService = promptService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User currentUser = SessionUtil.currentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("promptsList", promptService.findPromptsForUser(currentUser));
        model.addAttribute("historyList", promptService.findHistoryForUser(currentUser));
        return "dashboard";
    }
}
