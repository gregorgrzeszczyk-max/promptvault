package promptvault.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Size(max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String password;
    @Email
    @Column(nullable = false, unique = true, length = 120)
    private String email;
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String firstName;
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String lastName;

    public User() {
    }

    @PrePersist
    public void applyDefaults() {
        if (role == null || role.isBlank()) {
            role = "USER";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getName() {
        return firstName;
    }

    public void setName(String name) {
        this.firstName = name;
    }

    public String getSurname() {
        return lastName;
    }

    public void setSurname(String surname) {
        this.lastName = surname;
    }
}
