package promptvault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import promptvault.model.Keyword;

import java.util.Optional;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {
    Optional<Keyword> findByWordIgnoreCase(String word);
    boolean existsByWordIgnoreCase(String word);
}
