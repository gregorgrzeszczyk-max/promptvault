package promptvault.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import promptvault.model.Category;
import promptvault.model.Prompt;
import promptvault.model.User;
import promptvault.repository.CategoryRepository;
import promptvault.repository.PromptRepository;
import promptvault.repository.SubmissionHistoryRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PromptService {

    private final PromptRepository promptRepository;
    private final CategoryRepository categoryRepository;
    private final SubmissionHistoryRepository submissionHistoryRepository;
    private final AiSimulationService aiSimulationService;

    public PromptService(PromptRepository promptRepository, CategoryRepository categoryRepository, SubmissionHistoryRepository submissionHistoryRepository, AiSimulationService aiSimulationService) {
        this.promptRepository = promptRepository;
        this.categoryRepository = categoryRepository;
        this.submissionHistoryRepository = submissionHistoryRepository;
        this.aiSimulationService = aiSimulationService;
    }

    public List<Prompt> findPromptsForUser(User user) {
        return user == null ? List.of() : promptRepository.findByUserOrderBySubmissionDateDesc(user);
    }

    public List<Prompt> findPublicPrompts() {
        return promptRepository.findByVisibilityIgnoreCaseOrderBySubmissionDateDesc("PUBLIC");
    }

    public List<Prompt> findFlaggedPrompts() {
        return promptRepository.findByIsFlaggedTrueOrderBySubmissionDateDesc();
    }

    public Optional<Prompt> findOwnedPrompt(Long id, User user) {
        if (id == null || user == null) {
            return Optional.empty();
        }
        return promptRepository.findByIdAndUser(id, user);
    }

    @Transactional
    public Prompt savePrompt(Prompt prompt, Long categoryId, User user) {
        validatePrompt(prompt, categoryId, user);
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new IllegalArgumentException("Category not found"));
        prompt.setTitle(prompt.getTitle().trim());
        prompt.setPromptText(prompt.getPromptText().trim());
        prompt.setVisibility(normalizeVisibility(prompt.getVisibility()));
        prompt.setCategory(category);
        prompt.setUser(user);
        prompt.setSubmissionDate(LocalDateTime.now());
        aiSimulationService.findMatchingPolicyKeyword(prompt.getPromptText()).ifPresentOrElse(
                word -> {
                    prompt.setFlagged(true);
                    prompt.setFlaggedKeyword(word);
                },
                () -> {
                    prompt.setFlagged(false);
                    prompt.setFlaggedKeyword(null);
                }
        );
        return promptRepository.save(prompt);
    }

    @Transactional
    public Prompt updateOwnedPrompt(Long id, Prompt updatedPrompt, Long categoryId, User user) {
        Prompt existing = findOwnedPrompt(id, user).orElseThrow(() -> new IllegalArgumentException("Prompt not found"));
        existing.setTitle(updatedPrompt.getTitle());
        existing.setPromptText(updatedPrompt.getPromptText());
        existing.setVisibility(updatedPrompt.getVisibility());
        return savePrompt(existing, categoryId, user);
    }

    @Transactional
    public Prompt submitOwnedPrompt(Long id, User user) {
        Prompt prompt = findOwnedPrompt(id, user).orElseThrow(() -> new IllegalArgumentException("Prompt not found"));
        return aiSimulationService.submitPromptToAiAndStoreHistory(prompt, user);
    }

    @Transactional
    public void deleteOwnedPrompt(Long id, User user) {
        Prompt prompt = findOwnedPrompt(id, user).orElseThrow(() -> new IllegalArgumentException("Prompt not found"));
        submissionHistoryRepository.deleteByPrompt(prompt);
        promptRepository.delete(prompt);
    }

    public long countByCategory(Category category) {
        return category == null ? 0 : promptRepository.countByCategory(category);
    }

    private void validatePrompt(Prompt prompt, Long categoryId, User user) {
        if (prompt == null || user == null || user.getId() == null || categoryId == null) {
            throw new IllegalArgumentException("Prompt, category and user are required");
        }
        if (prompt.getTitle() == null || prompt.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt title is required");
        }
        if (prompt.getPromptText() == null || prompt.getPromptText().trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt text is required");
        }
    }

    private String normalizeVisibility(String visibility) {
        if (visibility == null || visibility.isBlank()) {
            return "PRIVATE";
        }
        return "PUBLIC".equalsIgnoreCase(visibility.trim()) ? "PUBLIC" : "PRIVATE";
    }
}
