package promptvault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import promptvault.model.Prompt;
import promptvault.model.SubmissionHistory;
import promptvault.model.User;

import java.util.List;

public interface SubmissionHistoryRepository extends JpaRepository<SubmissionHistory, Long> {
    List<SubmissionHistory> findByUserOrderBySubmissionDateDesc(User user);
    void deleteByPrompt(Prompt prompt);
}
