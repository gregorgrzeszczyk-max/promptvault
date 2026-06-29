package promptvault.component;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import promptvault.model.User;
import promptvault.repository.UserRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // This pulls in your database and your encryption tool
    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {

        // 1. Create the Admin Account
        if (!userRepository.existsByUsernameIgnoreCase("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            // Here is the magic: we encrypt "admin123" before saving it!
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            admin.setFirstName("Admin");
            admin.setLastName("Account");
            admin.setEmail("admin@promptvault.local");
            admin.setActive(true); // <--- Fixed line

            userRepository.save(admin);
            System.out.println("✅ Default Admin account created!");
        }

        // 2. Create the Student Account
        if (!userRepository.existsByUsernameIgnoreCase("student")) {
            User student = new User();
            student.setUsername("student");
            student.setPassword(passwordEncoder.encode("password123"));
            student.setRole("STUDENT");
            student.setFirstName("Student");
            student.setLastName("Account");
            student.setEmail("student@promptvault.local");
            student.setActive(true); // <--- Fixed line

            userRepository.save(student);
            System.out.println("✅ Default Student account created!");
        }
    }
}