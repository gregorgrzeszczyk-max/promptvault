package com.promptvault.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PVAULT-007 — Mass-Assignment Fix
 *
 * Replaces direct @ModelAttribute User binding in RegistrationController.
 * Only exposes the fields a self-registering user is allowed to supply;
 * sensitive fields such as role, active, and id are deliberately absent
 * and are assigned by UserService.registerUser() from server-side logic.
 *
 * PVAULT-P2-07 (A05 — CWE-20) — Bean Validation constraints added so every
 * registration field is validated declaratively via @Valid.
 * PVAULT-P3-16 (A07 — CWE-521) — Password strength policy: at least 8
 * characters including one letter and one digit (also enforced
 * server-side in UserService as defence in depth).
 */
public class UserRegistrationDTO {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Username may only contain letters, digits, dots, underscores and hyphens")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "Password must contain at least one letter and one digit")
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 80, message = "First name must be at most 80 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 80, message = "Last name must be at most 80 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Size(max = 120, message = "Email must be at most 120 characters")
    private String email;

    // ----------------------------------------------------------------
    // Getters and setters
    // ----------------------------------------------------------------

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
