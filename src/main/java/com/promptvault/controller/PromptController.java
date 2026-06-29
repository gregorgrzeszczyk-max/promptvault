package promptvault.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import promptvault.model.Prompt;
import promptvault.model.User;
import promptvault.repository.CategoryRepository;
import promptvault.service.PromptService;

@Controller
public class PromptController {

    private final PromptService promptService;
    private final CategoryRepository categoryRepository;

    public PromptController(PromptService promptService, CategoryRepository categoryRepository) {
        this.promptService = promptService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/prompts/new")
    public String newPrompt(HttpSession session, Model model) {
        if (currentUser(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("prompt", new Prompt());
        model.addAttribute("categories", categoryRepository.findAll());
        return "prompt-form";
    }

    @PostMapping("/prompts")
    public String createPrompt(@ModelAttribute Prompt prompt, @RequestParam Long categoryId, HttpSession session, Model model) {
        User currentUser = currentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        try {
            promptService.savePrompt(prompt, categoryId, currentUser);
            return "redirect:/dashboard";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("prompt", prompt);
            model.addAttribute("categories", categoryRepository.findAll());
            return "prompt-form";
        }
    }

    @GetMapping("/prompts/{id}/edit")
    public String editPrompt(@PathVariable Long id, HttpSession session, Model model) {
        User currentUser = currentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        return promptService.findOwnedPrompt(id, currentUser).map(prompt -> {
            model.addAttribute("prompt", prompt);
            model.addAttribute("categories", categoryRepository.findAll());
            return "prompt-form";
        }).orElse("redirect:/dashboard");
    }

    @PostMapping("/prompts/{id}")
    public String updatePrompt(@PathVariable Long id, @ModelAttribute Prompt prompt, @RequestParam Long categoryId, HttpSession session, Model model) {
        User currentUser = currentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        try {
            promptService.updateOwnedPrompt(id, prompt, categoryId, currentUser);
            return "redirect:/dashboard";
        } catch (IllegalArgumentException ex) {
            prompt.setId(id);
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("prompt", prompt);
            model.addAttribute("categories", categoryRepository.findAll());
            return "prompt-form";
        }
    }

    @PostMapping("/prompts/{id}/submit")
    public String submitPrompt(@PathVariable Long id, HttpSession session) {
        User currentUser = currentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        promptService.submitOwnedPrompt(id, currentUser);
        return "redirect:/dashboard";
    }

    @PostMapping("/prompts/{id}/delete")
    public String deletePrompt(@PathVariable Long id, HttpSession session) {
        User currentUser = currentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        promptService.deleteOwnedPrompt(id, currentUser);
        return "redirect:/dashboard";
    }

    private User currentUser(HttpSession session) {
        Object value = session.getAttribute("currentUser");
        return value instanceof User ? (User) value : null;
    }
}
