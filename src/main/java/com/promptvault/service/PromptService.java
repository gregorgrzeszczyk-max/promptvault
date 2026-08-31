package com.promptvault.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.promptvault.entity.Category;
import com.promptvault.entity.Prompt;
import com.promptvault.entity.SubmissionHistory;
import com.promptvault.entity.User;
import com.promptvault.repository.CategoryRepository;
import com.promptvault.repository.PromptRepository;
import com.promptvault.repository.SubmissionHistoryRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for managing prompts, including creation, editing, deletion,
 * submission to the simulated AI assistant and the user's submission history.
 */
@Service
public class PromptService {

    public static final String VISIBILITY_PRIVATE = "PRIVATE";
    public static final String VISIBILITY_SHARED = "SHARED";

    private final PromptRepository promptRepository;
    private final CategoryRepository categoryRepository;
    private final SubmissionHistoryRepository submissionHistoryRepository;
    private final AiSimulationService aiSimulationService;
    private final SecurityAuditLogger auditLogger;

    public PromptService(PromptRepository promptRepository,
                         CategoryRepository categoryRepository,
                         SubmissionHistoryRepository submissionHistoryRepository,
                         AiSimulationService aiSimulationService,
                         SecurityAuditLogger auditLogger) {
        this.promptRepository = promptRepository;
        this.categoryRepository = categoryRepository;
        this.submissionHistoryRepository = submissionHistoryRepository;
        this.aiSimulationService = aiSimulationService;
        this.auditLogger = auditLogger;
    }

    /** Every prompt that belongs to the given user (private and shared). */
    public List<Prompt> findPromptsForUser(User user) {
        return user == null ? List.of() : promptRepository.findByUserOrderBySubmissionDateDesc(user);
    }

    /** Prompts that other users marked as shared, for the community vault. */
    public List<Prompt> findSharedPrompts() {
        return promptRepository.findByVisibilityIgnoreCaseOrderBySubmissionDateDesc(VISIBILITY_SHARED);
    }

    /** Prompts flagged for containing one or more policy keywords. */
    public List<Prompt> findFlaggedPrompts() {
        return promptRepository.findByIsFlaggedTrueOrderBySubmissionDateDesc();
    }

    /** Submission history for the given user, most recent first. */
    public List<SubmissionHistory> findHistoryForUser(User user) {
        return user == null ? List.of() : submissionHistoryRepository.findByUserOrderBySubmissionDateDesc(user);
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
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        prompt.setTitle(prompt.getTitle().trim());
        prompt.setPromptText(prompt.getPromptText().trim());
        prompt.setVisibility(normalizeVisibility(prompt.getVisibility()));
        prompt.setCategory(category);
        prompt.setUser(user);
        if (prompt.getSubmissionDate() == null) {
            prompt.setSubmissionDate(LocalDateTime.now());
        }
        applyPolicyFlag(prompt);
        return promptRepository.save(prompt);
    }

    @Transactional
    public Prompt updateOwnedPrompt(Long id, Prompt updatedPrompt, Long categoryId, User user) {
        Prompt existing = findOwnedPrompt(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found or not owned by you"));
        existing.setTitle(updatedPrompt.getTitle());
        existing.setPromptText(updatedPrompt.getPromptText());
        existing.setVisibility(updatedPrompt.getVisibility());
        return savePrompt(existing, categoryId, user);
    }

    /** Submits an owned prompt to the simulated AI and stores the history entry. */
    @Transactional
    public Prompt submitOwnedPrompt(Long id, User user) {
        Prompt prompt = findOwnedPrompt(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found or not owned by you"));
        return aiSimulationService.submitPromptToAiAndStoreHistory(prompt, user);
    }

    @Transactional
    public void deleteOwnedPrompt(Long id, User user) {
        Prompt prompt = findOwnedPrompt(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found or not owned by you"));
        submissionHistoryRepository.deleteByPrompt(prompt);
        promptRepository.delete(prompt);
    }

    /** Clears the submission history that belongs to the given user. */
    @Transactional
    public void clearHistoryForUser(User user) {
        if (user != null) {
            submissionHistoryRepository.deleteByUser(user);
        }
    }

    public long countByCategory(Category category) {
        return category == null ? 0 : promptRepository.countByCategory(category);
    }

    /**
     * Flags the prompt when its text contains a registered policy keyword.
     *
     * PVAULT-P3-18 — Fail-secure policy check (OWASP A04, CWE-636): if the
     * keyword lookup itself fails, the prompt is flagged for admin review
     * rather than silently passing the policy gate.
     * PVAULT-P2-02 — flagged prompts are recorded in the security audit log
     * with masked content (CWE-532).
     */
    private void applyPolicyFlag(Prompt prompt) {
        try {
            aiSimulationService.findMatchingPolicyKeyword(prompt.getPromptText()).ifPresentOrElse(
                    word -> {
                        prompt.setFlagged(true);
                        prompt.setFlaggedKeyword(word);
                        auditLogger.promptFlagged(
                                prompt.getUser() != null ? prompt.getUser().getUsername() : null,
                                prompt.getId(), word, prompt.getPromptText());
                    },
                    () -> {
                        prompt.setFlagged(false);
                        prompt.setFlaggedKeyword(null);
                    }
            );
        } catch (RuntimeException ex) {
            // Fail secure: treat an unavailable policy engine as a potential violation.
            prompt.setFlagged(true);
            prompt.setFlaggedKeyword("policy-check-unavailable");
            auditLogger.policyCheckFailure(prompt.getId(), ex.getClass().getSimpleName());
        }
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
            return VISIBILITY_PRIVATE;
        }
        return VISIBILITY_SHARED.equalsIgnoreCase(visibility.trim()) ? VISIBILITY_SHARED : VISIBILITY_PRIVATE;
    }
}
