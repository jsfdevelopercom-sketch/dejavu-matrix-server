package com.dejavu.backend.repository;

import com.dejavu.backend.model.RoomSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomSessionRepository extends JpaRepository<RoomSession, Long> {
}
