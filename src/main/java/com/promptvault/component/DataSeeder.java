package com.promptvault.component;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.promptvault.entity.Category;
import com.promptvault.entity.PolicyKeyword;
import com.promptvault.entity.Prompt;
import com.promptvault.entity.SubmissionHistory;
import com.promptvault.entity.User;
import com.promptvault.repository.CategoryRepository;
import com.promptvault.repository.PolicyKeywordRepository;
import com.promptvault.repository.PromptRepository;
import com.promptvault.repository.SubmissionHistoryRepository;
import com.promptvault.repository.UserRepository;
import com.promptvault.service.PasswordUtil;
import com.promptvault.service.PromptService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds the database with sample content on first startup so the application
 * can be tested immediately. It creates the predefined admin account, two demo
 * users, prompt categories, policy keywords and a mix of private and shared
 * prompts (including some that are flagged for containing policy keywords).
 *
 * Every section is guarded so the seeder does nothing once the data exists,
 * making it safe to run on every restart.
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PolicyKeywordRepository policyKeywordRepository;
    private final PromptRepository promptRepository;
    private final SubmissionHistoryRepository submissionHistoryRepository;
    private final PasswordUtil passwordUtil;

    public DataSeeder(UserRepository userRepository,
                      CategoryRepository categoryRepository,
                      PolicyKeywordRepository policyKeywordRepository,
                      PromptRepository promptRepository,
                      SubmissionHistoryRepository submissionHistoryRepository,
                      PasswordUtil passwordUtil) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.policyKeywordRepository = policyKeywordRepository;
        this.promptRepository = promptRepository;
        this.submissionHistoryRepository = submissionHistoryRepository;
        this.passwordUtil = passwordUtil;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedCategories();
        seedKeywords();
        seedPrompts();
    }

    private void seedUsers() {
        createUserIfMissing("admin", "admin123", "ADMIN", "System", "Administrator", "admin@promptvault.local");
        createUserIfMissing("alice", "password123", "USER", "Alice", "Johnson", "alice@promptvault.local");
        createUserIfMissing("bob", "password123", "USER", "Bob", "Smith", "bob@promptvault.local");
    }

    private void createUserIfMissing(String username, String rawPassword, String role,
                                     String firstName, String lastName, String email) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordUtil.hash(rawPassword));
        user.setRole(role);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setActive(true);
        userRepository.save(user);
    }

    private void seedCategories() {
        createCategoryIfMissing("Coding", "Programming, debugging and software development prompts");
        createCategoryIfMissing("Research", "Academic research, literature reviews and study prompts");
        createCategoryIfMissing("Cybersecurity", "Security, privacy and safe handling of information");
        createCategoryIfMissing("Legal", "Contracts, policies and legal drafting prompts");
        createCategoryIfMissing("HR", "Recruitment, onboarding and people management prompts");
        createCategoryIfMissing("Personal productivity", "Planning, organisation and day to day productivity");
    }

    private void createCategoryIfMissing(String name, String description) {
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            return;
        }
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        categoryRepository.save(category);
    }

    private void seedKeywords() {
        List<String> words = List.of(
                "password", "api key", "secret", "credit card",
                "private key", "confidential", "medical record", "student number");
        for (String word : words) {
            if (policyKeywordRepository.findByWordIgnoreCase(word).isEmpty()) {
                PolicyKeyword keyword = new PolicyKeyword();
                keyword.setWord(word);
                policyKeywordRepository.save(keyword);
            }
        }
    }

    private void seedPrompts() {
        if (promptRepository.count() > 0) {
            return;
        }
        User alice = userRepository.findByUsernameIgnoreCase("alice").orElse(null);
        User bob = userRepository.findByUsernameIgnoreCase("bob").orElse(null);
        if (alice == null || bob == null) {
            return;
        }

        savePrompt(alice, "Coding", PromptService.VISIBILITY_SHARED,
                "Refactor a Python function",
                "Refactor this Python function to be more readable and add type hints.",
                null);

        savePrompt(alice, "Cybersecurity", PromptService.VISIBILITY_PRIVATE,
                "Store an API key safely",
                "What is the safest way to store my api key inside a Spring Boot project?",
                "api key");

        savePrompt(alice, "Legal", PromptService.VISIBILITY_SHARED,
                "Draft a simple NDA clause",
                "Draft a short non disclosure clause suitable for a student software project.",
                null);

        savePrompt(bob, "Research", PromptService.VISIBILITY_SHARED,
                "Literature review outline",
                "Create an outline for a literature review about large language models in education.",
                null);

        savePrompt(bob, "Personal productivity", PromptService.VISIBILITY_PRIVATE,
                "Reset my password steps",
                "List the steps to reset my password and recover access to my account securely.",
                "password");

        savePrompt(bob, "HR", PromptService.VISIBILITY_PRIVATE,
                "New starter onboarding checklist",
                "Write an onboarding checklist for a new software engineering intern.",
                null);

        // A couple of submission history entries so the history view is populated.
        promptRepository.findByUserOrderBySubmissionDateDesc(alice).stream()
                .filter(p -> "Refactor a Python function".equals(p.getTitle()))
                .findFirst()
                .ifPresent(p -> recordHistory(alice, p));
        promptRepository.findByUserOrderBySubmissionDateDesc(bob).stream()
                .filter(p -> "Literature review outline".equals(p.getTitle()))
                .findFirst()
                .ifPresent(p -> recordHistory(bob, p));
    }

    private void savePrompt(User owner, String categoryName, String visibility,
                            String title, String text, String flaggedKeyword) {
        Category category = categoryRepository.findByNameIgnoreCase(categoryName).orElse(null);
        if (category == null) {
            return;
        }
        Prompt prompt = new Prompt();
        prompt.setUser(owner);
        prompt.setCategory(category);
        prompt.setVisibility(visibility);
        prompt.setTitle(title);
        prompt.setPromptText(text);
        prompt.setSubmissionDate(LocalDateTime.now());
        prompt.setFlagged(flaggedKeyword != null);
        prompt.setFlaggedKeyword(flaggedKeyword);
        promptRepository.save(prompt);
    }

    private void recordHistory(User user, Prompt prompt) {
        String response = "Simulated AI response: " + prompt.getPromptText();
        prompt.setAiResponse(response);
        promptRepository.save(prompt);

        SubmissionHistory history = new SubmissionHistory();
        history.setUser(user);
        history.setPrompt(prompt);
        history.setAiResponse(response);
        history.setSubmissionDate(LocalDateTime.now());
        submissionHistoryRepository.save(history);
    }
}
