package com.dejavu.backend.repository;

import com.dejavu.backend.model.GuessAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuessAttemptRepository extends JpaRepository<GuessAttempt, Long> {
}
