package promptvault;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import promptvault.model.Category;
import promptvault.model.PolicyKeyword;
import promptvault.model.User;
import promptvault.repository.CategoryRepository;
import promptvault.repository.PolicyKeywordRepository;
import promptvault.repository.UserRepository;
import promptvault.service.PasswordUtil;

import java.util.List;

@SpringBootApplication
public class PromptvaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromptvaultApplication.class, args);
    }

    @Bean
    public CommandLineRunner loadData(UserRepository userRepository, CategoryRepository categoryRepository, PolicyKeywordRepository policyKeywordRepository, PasswordUtil passwordUtil) {
        return args -> {
            seedUsers(userRepository, passwordUtil);
            seedCategories(categoryRepository);
            seedPolicyKeywords(policyKeywordRepository);
        };
    }

    private void seedUsers(UserRepository userRepository, PasswordUtil passwordUtil) {
        if (userRepository.findByUsernameIgnoreCase("admin").isEmpty()) {
            User admin = new User();
            admin.setFirstName("Admin");
            admin.setLastName("Vault");
            admin.setUsername("admin");
            admin.setPassword(passwordUtil.hash("admin123"));
            admin.setRole("ADMIN");
            admin.setEmail("admin@promptvault.com");
            admin.setActive(true);
            userRepository.save(admin);
        }
        if (userRepository.findByUsernameIgnoreCase("student").isEmpty()) {
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setUsername("student");
            user.setPassword(passwordUtil.hash("password123"));
            user.setRole("USER");
            user.setEmail("john.doe@student.com");
            user.setActive(true);
            userRepository.save(user);
        }
    }

    private void seedCategories(CategoryRepository categoryRepository) {
        List<String> categories = List.of("General Productivity", "Code Assistance", "Academic Writing", "Business Analysis", "Creative Writing");
        for (String categoryName : categories) {
            if (categoryRepository.findByNameIgnoreCase(categoryName).isEmpty()) {
                Category category = new Category();
                category.setName(categoryName);
                category.setDescription(categoryName + " prompts");
                categoryRepository.save(category);
            }
        }
    }

    private void seedPolicyKeywords(PolicyKeywordRepository policyKeywordRepository) {
        List<String> words = List.of("password", "api key", "secret", "private key", "credit card", "passport", "national insurance", "medical record");
        for (String word : words) {
            if (policyKeywordRepository.findByWordIgnoreCase(word).isEmpty()) {
                PolicyKeyword keyword = new PolicyKeyword();
                keyword.setWord(word);
                policyKeywordRepository.save(keyword);
            }
        }
    }
}
