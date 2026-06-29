package com.promptvault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.promptvault.entity.Category;
import com.promptvault.entity.Prompt;
import com.promptvault.entity.User;

import java.util.List;
import java.util.Optional;

public interface PromptRepository extends JpaRepository<Prompt, Long> {
    List<Prompt> findByUserOrderBySubmissionDateDesc(User user);
    List<Prompt> findByVisibilityIgnoreCaseOrderBySubmissionDateDesc(String visibility);
    List<Prompt> findByIsFlaggedTrueOrderBySubmissionDateDesc();
    Optional<Prompt> findByIdAndUser(Long id, User user);
    long countByUser(User user);
    long countByCategory(Category category);
}
