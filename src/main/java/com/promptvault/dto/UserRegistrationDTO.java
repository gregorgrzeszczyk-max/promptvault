package com.promptvault.dto;

/**
 * PVAULT-007 — Mass-Assignment Fix
 *
 * Replaces direct @ModelAttribute User binding in RegistrationController.
 * Only exposes the fields a self-registering user is allowed to supply;
 * sensitive fields such as role, active, and id are deliberately absent
 * and are assigned by UserService.registerUser() from server-side logic.
 */
public class UserRegistrationDTO {

    private String username;
    private String password;
    private String firstName;
    private String lastName;
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
