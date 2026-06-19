package com.dejavu.backend.repository;

import com.dejavu.backend.model.Confession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface ConfessionRepository extends JpaRepository<Confession, Long> {
    Page<Confession> findAll(Pageable pageable);

    @Query(value = "SELECT * FROM confessions c WHERE c.id NOT IN (SELECT s.confession_id FROM room_sessions s WHERE s.user_id = :userId) ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Confession> findRandomUnseenConfessionForUser(@org.springframework.data.repository.query.Param("userId") Long userId);
}
