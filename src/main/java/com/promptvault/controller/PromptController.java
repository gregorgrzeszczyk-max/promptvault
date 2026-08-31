package com.promptvault.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.promptvault.dto.PromptForm;
import com.promptvault.entity.Prompt;
import com.promptvault.entity.User;
import com.promptvault.repository.CategoryRepository;
import com.promptvault.service.PromptService;
import com.promptvault.service.RateLimitService;
import com.promptvault.service.SecurityAuditLogger;

/**
 * Handles all user facing prompt operations: create, view, edit, delete,
 * browse shared prompts and submit a prompt to the simulated AI assistant.
 *
 * Security hardening:
 * - PVAULT-P2-07 — Bean Validation (CWE-20, OWASP A03/A04): prompt forms bind
 *   to a validated {@link PromptForm} DTO instead of the JPA entity, which
 *   also removes a mass-assignment vector (id/user/flagged fields can no
 *   longer be supplied by the client).
 * - PVAULT-P2-06 — Rate limiting (OWASP A04): prompt creation, update and AI
 *   submission are rate limited per user to prevent abuse/flooding.
 * - PVAULT-P2-02 — Security audit logging (CWE-778): deletions and rate limit
 *   violations are recorded.
 */
@Controller
public class PromptController {

    private final PromptService promptService;
    private final CategoryRepository categoryRepository;
    private final RateLimitService rateLimitService;
    private final SecurityAuditLogger auditLogger;

    private static final String RATE_LIMIT_MESSAGE =
            "You are performing this action too often. Please wait a few minutes and try again.";

    public PromptController(PromptService promptService,
                            CategoryRepository categoryRepository,
                            RateLimitService rateLimitService,
                            SecurityAuditLogger auditLogger) {
        this.promptService = promptService;
        this.categoryRepository = categoryRepository;
        this.rateLimitService = rateLimitService;
        this.auditLogger = auditLogger;
    }

    @GetMapping("/user/prompts/new")
    public String newPrompt(HttpSession session, Model model) {
        if (SessionUtil.currentUser(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("prompt", new PromptForm());
        model.addAttribute("categoriesList", categoryRepository.findAll());
        return "create-prompt";
    }

    @PostMapping("/user/prompts/save")
    public String createPrompt(@Valid @ModelAttribute("prompt") PromptForm form,
                               BindingResult bindingResult,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        User currentUser = SessionUtil.currentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", bindingResult.getAllErrors().get(0).getDefaultMessage());
            model.addAttribute("prompt", form);
            model.addAttribute("categoriesList", categoryRepository.findAll());
            return "create-prompt";
        }
        // PVAULT-P2-06: per-user rate limit on prompt creation.
        if (!rateLimitService.tryAcquireAction("prompt:" + currentUser.getId())) {
            auditLogger.rateLimitExceeded(currentUser.getUsername(), "prompt-actions", null);
            model.addAttribute("error", RATE_LIMIT_MESSAGE);
            model.addAttribute("prompt", form);
            model.addAttribute("categoriesList", categoryRepository.findAll());
            return "create-prompt";
        }
        try {
            Prompt saved = promptService.savePrompt(toEntity(form), form.getCategoryId(), currentUser);
            addFlagFeedback(saved, redirectAttributes, "Prompt saved to your vault.");
            return "redirect:/dashboard";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("prompt", form);
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
                    model.addAttribute("prompt", toForm(prompt));
                    model.addAttribute("categoriesList", categoryRepository.findAll());
                    return "edit-prompt";
                })
                .orElse("redirect:/dashboard");
    }

    @PostMapping("/user/prompts/update")
    public String updatePrompt(@RequestParam Long id,
                               @Valid @ModelAttribute("prompt") PromptForm form,
                               BindingResult bindingResult,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        User currentUser = SessionUtil.currentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        form.setId(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", bindingResult.getAllErrors().get(0).getDefaultMessage());
            model.addAttribute("prompt", form);
            model.addAttribute("categoriesList", categoryRepository.findAll());
            return "edit-prompt";
        }
        // PVAULT-P2-06: per-user rate limit on prompt updates.
        if (!rateLimitService.tryAcquireAction("prompt:" + currentUser.getId())) {
            auditLogger.rateLimitExceeded(currentUser.getUsername(), "prompt-actions", null);
            model.addAttribute("error", RATE_LIMIT_MESSAGE);
            model.addAttribute("prompt", form);
            model.addAttribute("categoriesList", categoryRepository.findAll());
            return "edit-prompt";
        }
        try {
            Prompt saved = promptService.updateOwnedPrompt(id, toEntity(form), form.getCategoryId(), currentUser);
            addFlagFeedback(saved, redirectAttributes, "Prompt updated.");
            return "redirect:/dashboard";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("prompt", form);
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
        // PVAULT-P2-06: per-user rate limit on AI submissions.
        if (!rateLimitService.tryAcquireAction("prompt:" + currentUser.getId())) {
            auditLogger.rateLimitExceeded(currentUser.getUsername(), "prompt-actions", null);
            redirectAttributes.addFlashAttribute("warningMessage", RATE_LIMIT_MESSAGE);
            return "redirect:/dashboard";
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
            // PVAULT-P2-02: audit trail for destructive actions.
            auditLogger.promptDeleted(currentUser.getUsername(), id);
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

    /** Maps the validated form DTO onto a transient Prompt entity (allow-listed fields only). */
    private Prompt toEntity(PromptForm form) {
        Prompt prompt = new Prompt();
        prompt.setTitle(form.getTitle());
        prompt.setPromptText(form.getPromptText());
        prompt.setVisibility(form.getVisibility());
        return prompt;
    }

    /** Maps a persisted Prompt entity to the form DTO used by the edit view. */
    private PromptForm toForm(Prompt prompt) {
        PromptForm form = new PromptForm();
        form.setId(prompt.getId());
        form.setTitle(prompt.getTitle());
        form.setPromptText(prompt.getPromptText());
        form.setVisibility(prompt.getVisibility());
        form.setCategoryId(prompt.getCategory() != null ? prompt.getCategory().getId() : null);
        return form;
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
