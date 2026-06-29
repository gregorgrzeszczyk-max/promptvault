package promptvault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import promptvault.model.PolicyKeyword;

import java.util.Optional;

public interface PolicyKeywordRepository extends JpaRepository<PolicyKeyword, Long> {
    Optional<PolicyKeyword> findByWordIgnoreCase(String word);
    boolean existsByWordIgnoreCase(String word);
}
