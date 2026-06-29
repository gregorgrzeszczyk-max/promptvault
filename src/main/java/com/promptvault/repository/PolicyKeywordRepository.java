package com.promptvault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.promptvault.entity.PolicyKeyword;

import java.util.Optional;

public interface PolicyKeywordRepository extends JpaRepository<PolicyKeyword, Long> {
    Optional<PolicyKeyword> findByWordIgnoreCase(String word);
    boolean existsByWordIgnoreCase(String word);
}
