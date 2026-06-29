package com.promptvault.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.promptvault.entity.Prompt;
import com.promptvault.entity.User;
import com.promptvault.repository.CategoryRepository;
import com.promptvault.service.PromptService;

/**
 * Handles all user facing prompt operations: create, view, edit, delete,
 * browse shared prompts and submit a prompt to the simulated AI assistant.
 */
@Controller
public class PromptController {

    private final PromptService promptService;
    private final CategoryRepository categoryRepository;

    public PromptController(PromptService promptService, CategoryRepository categoryRepository) {
        this.promptService = promptService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/user/prompts/new")
    public String newPrompt(HttpSession session, Model model) {
        if (SessionUtil.currentUser(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("prompt", new Prompt());
        model.addAttribute("categoriesList", categoryRepository.findAll());
        return "create-prompt";
    }

    @PostMapping("/user/prompts/save")
    public String createPrompt(@ModelAttribute Prompt prompt,
                               @RequestParam Long categoryId,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        User currentUser = SessionUtil.currentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        try {
            Prompt saved = promptService.savePrompt(prompt, categoryId, currentUser);
            addFlagFeedback(saved, redirectAttributes, "Prompt saved to your vault.");
            return "redirect:/dashboard";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("prompt", prompt);
            model.addAttribute("categoriesList", categoryRepository.findAll());
            return "create-prompt";
        }
    }

    @GetMapping("/user/prompts/view")
    public String viewPrompt(@RequestParam Long id, HttpSession session, Model model) {
        User currentUser = SessionUtil.currentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        return promptService.findOwnedPrompt(id, currentUser)
                .map(prompt -> {
                    model.addAttribute("prompt", prompt);
                    return "view-prompt";
                })
                .orElse("redirect:/dashboard");
    }

    @GetMapping("/user/prompts/edit")
    public String editPrompt(@RequestParam Long id, HttpSession session, Model model) {
        User currentUser = SessionUtil.currentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        return promptService.findOwnedPrompt(id, currentUser)
                .map(prompt -> {
                    model.addAttribute("prompt", prompt);
                    model.addAttribute("categoriesList", categoryRepository.findAll());
                    return "edit-prompt";
                })
                .orElse("redirect:/dashboard");
    }

    @PostMapping("/user/prompts/update")
    public String updatePrompt(@RequestParam Long id,
                               @ModelAttribute Prompt prompt,
                               @RequestParam Long categoryId,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        User currentUser = SessionUtil.currentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        try {
            Prompt saved = promptService.updateOwnedPrompt(id, prompt, categoryId, currentUser);
            addFlagFeedback(saved, redirectAttributes, "Prompt updated.");
            return "redirect:/dashboard";
        } catch (IllegalArgumentException ex) {
            prompt.setId(id);
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("prompt", prompt);
            model.addAttribute("categoriesList", categoryRepository.findAll());
            return "edit-prompt";
        }
    }

    @PostMapping("/user/prompts/submit")
    public String submitPrompt(@RequestParam Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = SessionUtil.currentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        try {
            Prompt submitted = promptService.submitOwnedPrompt(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Prompt submitted to the AI assistant.");
            if (submitted.isFlagged()) {
                redirectAttributes.addFlashAttribute("warningMessage",
                        "Warning: this prompt may contain sensitive information (matched keyword: \""
                                + submitted.getFlaggedKeyword() + "\"). It has been flagged for admin review.");
            }
            redirectAttributes.addAttribute("id", id);
            return "redirect:/user/prompts/view";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("warningMessage", ex.getMessage());
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/user/prompts/delete")
    public String deletePrompt(@RequestParam Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = SessionUtil.currentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        try {
            promptService.deleteOwnedPrompt(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Prompt deleted.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("warningMessage", ex.getMessage());
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/user/prompts/shared")
    public String sharedPrompts(HttpSession session, Model model) {
        if (SessionUtil.currentUser(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("sharedList", promptService.findSharedPrompts());
        return "shared-prompts";
    }

    @PostMapping("/user/prompts/clear")
    public String clearHistory(HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = SessionUtil.currentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        promptService.clearHistoryForUser(currentUser);
        redirectAttributes.addFlashAttribute("successMessage", "Your submission history was cleared.");
        return "redirect:/dashboard";
    }

    private void addFlagFeedback(Prompt prompt, RedirectAttributes redirectAttributes, String successMessage) {
        if (prompt.isFlagged()) {
            redirectAttributes.addFlashAttribute("warningMessage",
                    "Warning: this prompt may contain sensitive information (matched keyword: \""
                            + prompt.getFlaggedKeyword() + "\"). It has been flagged for admin review.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
        }
    }
}
