package promptvault.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import promptvault.model.PolicyKeyword;
import promptvault.model.User;
import promptvault.repository.PolicyKeywordRepository;

@Controller
public class KeywordController {

    private final PolicyKeywordRepository policyKeywordRepository;

    public KeywordController(PolicyKeywordRepository policyKeywordRepository) {
        this.policyKeywordRepository = policyKeywordRepository;
    }

    @GetMapping("/admin/keywords")
    public String keywords(HttpSession session, Model model) {
        if (!isAdmin(currentUser(session))) {
            return "redirect:/login";
        }
        model.addAttribute("keywords", policyKeywordRepository.findAll());
        model.addAttribute("keyword", new PolicyKeyword());
        return "keywords";
    }

    @PostMapping("/admin/keywords")
    public String createKeyword(@ModelAttribute PolicyKeyword keyword, HttpSession session, Model model) {
        if (!isAdmin(currentUser(session))) {
            return "redirect:/login";
        }
        if (keyword.getWord() == null || keyword.getWord().isBlank() || policyKeywordRepository.existsByWordIgnoreCase(keyword.getWord().trim())) {
            model.addAttribute("error", "Keyword is required and must be unique");
            model.addAttribute("keywords", policyKeywordRepository.findAll());
            model.addAttribute("keyword", keyword);
            return "keywords";
        }
        keyword.setWord(keyword.getWord().trim());
        policyKeywordRepository.save(keyword);
        return "redirect:/admin/keywords";
    }

    @PostMapping("/admin/keywords/{id}/delete")
    public String deleteKeyword(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(currentUser(session))) {
            return "redirect:/login";
        }
        policyKeywordRepository.findById(id).ifPresent(policyKeywordRepository::delete);
        return "redirect:/admin/keywords";
    }

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private User currentUser(HttpSession session) {
        Object value = session.getAttribute("currentUser");
        return value instanceof User ? (User) value : null;
    }
}
