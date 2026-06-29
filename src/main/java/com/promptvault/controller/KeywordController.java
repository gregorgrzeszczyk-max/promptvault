package com.promptvault.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.promptvault.entity.PolicyKeyword;
import com.promptvault.repository.PolicyKeywordRepository;

/**
 * Admin management of policy keywords used to flag potentially sensitive
 * prompts: add, edit and delete.
 */
@Controller
public class KeywordController {

    private final PolicyKeywordRepository policyKeywordRepository;

    public KeywordController(PolicyKeywordRepository policyKeywordRepository) {
        this.policyKeywordRepository = policyKeywordRepository;
    }

    @GetMapping("/admin/keywords")
    public String keywords(HttpSession session, Model model) {
        if (!SessionUtil.isAdmin(SessionUtil.currentUser(session))) {
            return "redirect:/login";
        }
        model.addAttribute("keywordsList", policyKeywordRepository.findAll());
        return "admin-keywords";
    }

    @PostMapping("/admin/keywords/add")
    public String addKeyword(@RequestParam String word, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!SessionUtil.isAdmin(SessionUtil.currentUser(session))) {
            return "redirect:/login";
        }
        if (word == null || word.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Keyword is required.");
        } else if (policyKeywordRepository.existsByWordIgnoreCase(word.trim())) {
            redirectAttributes.addFlashAttribute("error", "That keyword already exists.");
        } else {
            PolicyKeyword keyword = new PolicyKeyword();
            keyword.setWord(word.trim());
            policyKeywordRepository.save(keyword);
            redirectAttributes.addFlashAttribute("success", "Keyword added.");
        }
        return "redirect:/admin/keywords";
    }

    @GetMapping("/admin/keywords/edit")
    public String editKeyword(@RequestParam Long id, HttpSession session, Model model) {
        if (!SessionUtil.isAdmin(SessionUtil.currentUser(session))) {
            return "redirect:/login";
        }
        return policyKeywordRepository.findById(id)
                .map(keyword -> {
                    model.addAttribute("keyword", keyword);
                    return "admin-keyword-edit";
                })
                .orElse("redirect:/admin/keywords");
    }

    @PostMapping("/admin/keywords/update")
    public String updateKeyword(@RequestParam Long id,
                                @RequestParam String word,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (!SessionUtil.isAdmin(SessionUtil.currentUser(session))) {
            return "redirect:/login";
        }
        PolicyKeyword keyword = policyKeywordRepository.findById(id).orElse(null);
        if (keyword == null) {
            return "redirect:/admin/keywords";
        }
        if (word == null || word.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Keyword is required.");
            return "redirect:/admin/keywords/edit?id=" + id;
        }
        policyKeywordRepository.findByWordIgnoreCase(word.trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresentOrElse(
                        existing -> redirectAttributes.addFlashAttribute("error",
                                "Another keyword with that text already exists."),
                        () -> {
                            keyword.setWord(word.trim());
                            policyKeywordRepository.save(keyword);
                            redirectAttributes.addFlashAttribute("success", "Keyword updated.");
                        });
        return "redirect:/admin/keywords";
    }

    @PostMapping("/admin/keywords/delete")
    public String deleteKeyword(@RequestParam Long keywordId, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!SessionUtil.isAdmin(SessionUtil.currentUser(session))) {
            return "redirect:/login";
        }
        policyKeywordRepository.findById(keywordId).ifPresent(policyKeywordRepository::delete);
        redirectAttributes.addFlashAttribute("success", "Keyword deleted.");
        return "redirect:/admin/keywords";
    }
}
