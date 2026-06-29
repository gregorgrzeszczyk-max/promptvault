package com.promptvault.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import jakarta.validation.constraints.Size;

@Entity
@Table(
        name = "prompts",
        indexes = {
                @Index(name = "idx_prompt_user", columnList = "user_id"),
                @Index(name = "idx_prompt_visibility", columnList = "visibility"),
                @Index(name = "idx_prompt_flagged", columnList = "is_flagged"),
                @Index(name = "idx_prompt_submission_date", columnList = "submission_date")
        }
)
public class Prompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String promptText;
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String visibility;

    @Column(name = "is_flagged", nullable = false)
    private boolean isFlagged;
    @Column(length = 120)
    private String flaggedKeyword;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "submission_date")
    private LocalDateTime submissionDate;

    @Column(columnDefinition = "TEXT")
    private String aiResponse;

    public Prompt() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPromptText() { return promptText; }
    public void setPromptText(String promptText) { this.promptText = promptText; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public boolean isFlagged() { return isFlagged; }
    public void setFlagged(boolean flagged) { isFlagged = flagged; }
    public String getFlaggedKeyword() { return flaggedKeyword; }
    public void setFlaggedKeyword(String flaggedKeyword) { this.flaggedKeyword = flaggedKeyword; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getSubmissionDate() { return submissionDate; }
    public void setSubmissionDate(LocalDateTime submissionDate) { this.submissionDate = submissionDate; }
    public String getAiResponse() { return aiResponse; }
    public void setAiResponse(String aiResponse) { this.aiResponse = aiResponse; }
}
