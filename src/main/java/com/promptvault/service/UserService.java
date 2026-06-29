package com.promptvault.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.promptvault.entity.User;
import com.promptvault.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordUtil passwordUtil;

    public UserService(UserRepository userRepository, PasswordUtil passwordUtil) {
        this.userRepository = userRepository;
        this.passwordUtil = passwordUtil;
    }

    @Transactional
    public Optional<User> authenticate(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            return Optional.empty();
        }
        Optional<User> user = userRepository.findByUsernameIgnoreCase(username.trim())
                .filter(User::isActive)
                .filter(candidate -> passwordUtil.matches(password, candidate.getPassword()));
        user.ifPresent(candidate -> {
            if (!passwordUtil.isHash(candidate.getPassword())) {
                candidate.setPassword(passwordUtil.hash(password));
                userRepository.save(candidate);
            }
        });
        return user;
    }

    @Transactional
    public User registerUser(User user) {
        validateRegistration(user);
        user.setUsername(user.getUsername().trim());
        user.setEmail(user.getEmail().trim().toLowerCase());
        user.setFirstName(user.getFirstName().trim());
        user.setLastName(user.getLastName().trim());
        user.setRole("USER");
        user.setActive(true);
        user.setPassword(passwordUtil.hash(user.getPassword()));
        return userRepository.save(user);
    }

    public void validateRegistration(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User details are required");
        }
        if (isBlank(user.getUsername()) || isBlank(user.getPassword()) || isBlank(user.getEmail()) || isBlank(user.getFirstName()) || isBlank(user.getLastName())) {
            throw new IllegalArgumentException("All registration fields are required");
        }
        if (user.getPassword().trim().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (userRepository.existsByUsernameIgnoreCase(user.getUsername().trim())) {
            throw new IllegalArgumentException("Username is already in use");
        }
        if (userRepository.existsByEmailIgnoreCase(user.getEmail().trim())) {
            throw new IllegalArgumentException("Email is already in use");
        }
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return id == null ? Optional.empty() : userRepository.findById(id);
    }

    @Transactional
    public void setUserActive(Long userId, boolean active, User currentUser) {
        User target = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (currentUser != null && currentUser.getId() != null && currentUser.getId().equals(target.getId()) && !active) {
            throw new IllegalArgumentException("Administrators cannot disable their own account");
        }
        target.setActive(active);
        userRepository.save(target);
    }

    /** Flips a user's enabled/disabled status. Used by the admin user list. */
    @Transactional
    public void toggleUserActive(Long userId, User currentUser) {
        User target = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        setUserActive(userId, !target.isActive(), currentUser);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
