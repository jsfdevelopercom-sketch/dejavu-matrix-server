package com.dejavu.backend.repository.game;

import com.dejavu.backend.model.game.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSessionRepository extends JpaRepository<GameSession, Long> {
}
