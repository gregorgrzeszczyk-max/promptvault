package com.promptvault.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.promptvault.entity.PolicyKeyword;
import com.promptvault.entity.Prompt;
import com.promptvault.entity.SubmissionHistory;
import com.promptvault.entity.User;
import com.promptvault.repository.PolicyKeywordRepository;
import com.promptvault.repository.PromptRepository;
import com.promptvault.repository.SubmissionHistoryRepository;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
public class AiSimulationService {

    private final PolicyKeywordRepository policyKeywordRepository;
    private final PromptRepository promptRepository;
    private final SubmissionHistoryRepository submissionHistoryRepository;
    private final SecurityAuditLogger auditLogger;

    public AiSimulationService(PolicyKeywordRepository policyKeywordRepository, PromptRepository promptRepository, SubmissionHistoryRepository submissionHistoryRepository, SecurityAuditLogger auditLogger) {
        this.policyKeywordRepository = policyKeywordRepository;
        this.promptRepository = promptRepository;
        this.submissionHistoryRepository = submissionHistoryRepository;
        this.auditLogger = auditLogger;
    }

    public Optional<String> findMatchingPolicyKeyword(String promptText) {
        if (promptText == null || promptText.isBlank()) {
            return Optional.empty();
        }
        String normalizedPrompt = promptText.toLowerCase(Locale.ROOT);
        return policyKeywordRepository.findAll().stream()
                .map(PolicyKeyword::getWord)
                .filter(word -> word != null && !word.isBlank())
                .filter(word -> normalizedPrompt.contains(word.toLowerCase(Locale.ROOT)))
                .findFirst();
    }

    public String simulateAiResponse(String promptText) {
        if (promptText == null || promptText.isBlank()) {
            return "No prompt text was supplied.";
        }
        return "Simulated AI response: " + promptText.trim();
    }

    @Transactional
    public Prompt submitPromptToAiAndStoreHistory(Prompt prompt, User user) {
        if (prompt == null || prompt.getId() == null || user == null || user.getId() == null) {
            throw new IllegalArgumentException("Prompt and user are required");
        }
        Prompt managedPrompt = promptRepository.findByIdAndUser(prompt.getId(), user).orElseThrow(() -> new IllegalArgumentException("Prompt not found"));
        // PVAULT-P3-18 — Fail-secure policy check (OWASP A04, CWE-636): if the
        // keyword lookup fails, the prompt is flagged for admin review instead
        // of silently passing the policy gate.
        String aiResponse = simulateAiResponse(managedPrompt.getPromptText());
        try {
            Optional<String> matchedKeyword = findMatchingPolicyKeyword(managedPrompt.getPromptText());
            managedPrompt.setFlagged(matchedKeyword.isPresent());
            managedPrompt.setFlaggedKeyword(matchedKeyword.orElse(null));
            matchedKeyword.ifPresent(word -> auditLogger.promptFlagged(
                    user.getUsername(), managedPrompt.getId(), word, managedPrompt.getPromptText()));
        } catch (RuntimeException ex) {
            managedPrompt.setFlagged(true);
            managedPrompt.setFlaggedKeyword("policy-check-unavailable");
            auditLogger.policyCheckFailure(managedPrompt.getId(), ex.getClass().getSimpleName());
        }
        managedPrompt.setAiResponse(aiResponse);
        managedPrompt.setSubmissionDate(LocalDateTime.now());
        Prompt savedPrompt = promptRepository.save(managedPrompt);
        SubmissionHistory history = new SubmissionHistory();
        history.setPrompt(savedPrompt);
        history.setUser(user);
        history.setAiResponse(aiResponse);
        history.setSubmissionDate(savedPrompt.getSubmissionDate());
        submissionHistoryRepository.save(history);
        return savedPrompt;
    }
}
