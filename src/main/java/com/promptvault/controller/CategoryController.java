package com.promptvault.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.promptvault.entity.Category;
import com.promptvault.repository.CategoryRepository;
import com.promptvault.service.PromptService;

/**
 * Admin management of prompt categories: add, edit and delete.
 */
@Controller
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final PromptService promptService;

    public CategoryController(CategoryRepository categoryRepository, PromptService promptService) {
        this.categoryRepository = categoryRepository;
        this.promptService = promptService;
    }

    @GetMapping("/admin/categories")
    public String categories(HttpSession session, Model model) {
        if (!SessionUtil.isAdmin(SessionUtil.currentUser(session))) {
            return "redirect:/login";
        }
        model.addAttribute("categoriesList", categoryRepository.findAll());
        return "admin-categories";
    }

    @PostMapping("/admin/categories/add")
    public String addCategory(@RequestParam String name,
                              @RequestParam(required = false) String description,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!SessionUtil.isAdmin(SessionUtil.currentUser(session))) {
            return "redirect:/login";
        }
        if (name == null || name.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Category name is required.");
        } else if (categoryRepository.existsByNameIgnoreCase(name.trim())) {
            redirectAttributes.addFlashAttribute("error", "A category with that name already exists.");
        } else {
            Category category = new Category();
            category.setName(name.trim());
            category.setDescription(description);
            categoryRepository.save(category);
            redirectAttributes.addFlashAttribute("success", "Category added.");
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/admin/categories/edit")
    public String editCategory(@RequestParam Long id, HttpSession session, Model model) {
        if (!SessionUtil.isAdmin(SessionUtil.currentUser(session))) {
            return "redirect:/login";
        }
        return categoryRepository.findById(id)
                .map(category -> {
                    model.addAttribute("category", category);
                    return "admin-category-edit";
                })
                .orElse("redirect:/admin/categories");
    }

    @PostMapping("/admin/categories/update")
    public String updateCategory(@RequestParam Long id,
                                 @RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (!SessionUtil.isAdmin(SessionUtil.currentUser(session))) {
            return "redirect:/login";
        }
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            return "redirect:/admin/categories";
        }
        if (name == null || name.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Category name is required.");
            return "redirect:/admin/categories/edit?id=" + id;
        }
        categoryRepository.findByNameIgnoreCase(name.trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresentOrElse(
                        existing -> redirectAttributes.addFlashAttribute("error",
                                "Another category already uses that name."),
                        () -> {
                            category.setName(name.trim());
                            category.setDescription(description);
                            categoryRepository.save(category);
                            redirectAttributes.addFlashAttribute("success", "Category updated.");
                        });
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/categories/delete")
    public String deleteCategory(@RequestParam Long categoryId, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!SessionUtil.isAdmin(SessionUtil.currentUser(session))) {
            return "redirect:/login";
        }
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            return "redirect:/admin/categories";
        }
        if (promptService.countByCategory(category) > 0) {
            redirectAttributes.addFlashAttribute("error",
                    "This category cannot be deleted because prompts are assigned to it.");
        } else {
            categoryRepository.delete(category);
            redirectAttributes.addFlashAttribute("success", "Category deleted.");
        }
        return "redirect:/admin/categories";
    }
}
