package com.promptvault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.promptvault.entity.Category;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
