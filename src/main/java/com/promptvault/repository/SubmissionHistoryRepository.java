package com.promptvault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.promptvault.entity.Prompt;
import com.promptvault.entity.SubmissionHistory;
import com.promptvault.entity.User;

import java.util.List;

public interface SubmissionHistoryRepository extends JpaRepository<SubmissionHistory, Long> {
    List<SubmissionHistory> findByUserOrderBySubmissionDateDesc(User user);
    void deleteByPrompt(Prompt prompt);
    void deleteByUser(User user);
}
