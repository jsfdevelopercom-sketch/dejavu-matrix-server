package com.dejavu.backend.repository;

import com.dejavu.backend.model.RoomBlueprint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoomBlueprintRepository extends JpaRepository<RoomBlueprint, Long> {
    Optional<RoomBlueprint> findFirstByConfessionIdAndLanguage(Long confessionId, String language);
}
