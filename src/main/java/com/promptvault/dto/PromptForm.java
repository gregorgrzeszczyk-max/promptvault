package com.promptvault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PVAULT-P2-07 (A05 — CWE-20) — Input validation on prompt forms.
 * Also hardens PVAULT-A08 mass assignment: unlike binding the Prompt
 * entity directly, this DTO only exposes the fields a user is allowed to
 * supply. Server-managed fields (isFlagged, flaggedKeyword, aiResponse,
 * user, submissionDate, version) can no longer be injected via the form.
 */
public class PromptForm {

    /** Prompt id — only used on the edit/update flow; ownership is re-checked server-side. */
    private Long id;

    @NotBlank(message = "Prompt title is required")
    @Size(max = 150, message = "Prompt title must be at most 150 characters")
    private String title;

    @NotBlank(message = "Prompt text is required")
    @Size(max = 10000, message = "Prompt text must be at most 10,000 characters")
    private String promptText;

    @NotNull(message = "Please choose a category")
    private Long categoryId;

    @Pattern(regexp = "PRIVATE|SHARED", message = "Visibility must be PRIVATE or SHARED")
    private String visibility = "PRIVATE";

    public PromptForm() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPromptText() { return promptText; }
    public void setPromptText(String promptText) { this.promptText = promptText; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
}
