package com.dejavu.backend.repository;

import com.dejavu.backend.model.PromptConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromptConfigRepository extends JpaRepository<PromptConfig, String> {
}
