package promptvault.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import promptvault.model.Category;
import promptvault.model.User;
import promptvault.repository.CategoryRepository;
import promptvault.service.PromptService;

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
        if (!isAdmin(currentUser(session))) {
            return "redirect:/login";
        }
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("category", new Category());
        return "categories";
    }

    @PostMapping("/admin/categories")
    public String createCategory(@ModelAttribute Category category, HttpSession session, Model model) {
        if (!isAdmin(currentUser(session))) {
            return "redirect:/login";
        }
        if (category.getName() == null || category.getName().isBlank() || categoryRepository.existsByNameIgnoreCase(category.getName().trim())) {
            model.addAttribute("error", "Category name is required and must be unique");
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("category", category);
            return "categories";
        }
        category.setName(category.getName().trim());
        category.setDescription(category.getDescription());
        categoryRepository.save(category);
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, HttpSession session, Model model) {
        if (!isAdmin(currentUser(session))) {
            return "redirect:/login";
        }
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            return "redirect:/admin/categories";
        }
        if (promptService.countByCategory(category) > 0) {
            model.addAttribute("error", "This category cannot be deleted because prompts are assigned to it.");
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("category", new Category());
            return "categories";
        }
        categoryRepository.delete(category);
        return "redirect:/admin/categories";
    }

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private User currentUser(HttpSession session) {
        Object value = session.getAttribute("currentUser");
        return value instanceof User ? (User) value : null;
    }
}
