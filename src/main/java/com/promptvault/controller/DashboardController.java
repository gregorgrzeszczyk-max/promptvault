package promptvault.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import promptvault.model.User;
import promptvault.repository.SubmissionHistoryRepository;
import promptvault.service.PromptService;

@Controller
public class DashboardController {

    private final PromptService promptService;
    private final SubmissionHistoryRepository submissionHistoryRepository;

    public DashboardController(PromptService promptService, SubmissionHistoryRepository submissionHistoryRepository) {
        this.promptService = promptService;
        this.submissionHistoryRepository = submissionHistoryRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User currentUser = currentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("prompts", promptService.findPromptsForUser(currentUser));
        model.addAttribute("publicPrompts", promptService.findPublicPrompts());
        model.addAttribute("history", submissionHistoryRepository.findByUserOrderBySubmissionDateDesc(currentUser));
        return "dashboard";
    }

    private User currentUser(HttpSession session) {
        Object value = session.getAttribute("currentUser");
        return value instanceof User ? (User) value : null;
    }
}
