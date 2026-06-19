package com.dejavu.backend.repository.game;

import com.dejavu.backend.model.game.ConfessionGameContent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfessionGameContentRepository extends JpaRepository<ConfessionGameContent, Long> {
    ConfessionGameContent findFirstByConfessionId(Long confessionId);
}
