package promptvault.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import promptvault.model.PolicyKeyword;
import promptvault.model.Prompt;
import promptvault.model.SubmissionHistory;
import promptvault.model.User;
import promptvault.repository.PolicyKeywordRepository;
import promptvault.repository.PromptRepository;
import promptvault.repository.SubmissionHistoryRepository;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
public class AiSimulationService {

    private final PolicyKeywordRepository policyKeywordRepository;
    private final PromptRepository promptRepository;
    private final SubmissionHistoryRepository submissionHistoryRepository;

    public AiSimulationService(PolicyKeywordRepository policyKeywordRepository, PromptRepository promptRepository, SubmissionHistoryRepository submissionHistoryRepository) {
        this.policyKeywordRepository = policyKeywordRepository;
        this.promptRepository = promptRepository;
        this.submissionHistoryRepository = submissionHistoryRepository;
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
        Optional<String> matchedKeyword = findMatchingPolicyKeyword(managedPrompt.getPromptText());
        String aiResponse = simulateAiResponse(managedPrompt.getPromptText());
        managedPrompt.setFlagged(matchedKeyword.isPresent());
        managedPrompt.setFlaggedKeyword(matchedKeyword.orElse(null));
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
