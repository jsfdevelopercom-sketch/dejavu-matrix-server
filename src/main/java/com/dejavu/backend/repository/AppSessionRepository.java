package com.dejavu.backend.repository;

import com.dejavu.backend.model.AppSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppSessionRepository extends JpaRepository<AppSession, Long> {
    List<AppSession> findByUserId(Long userId);

    @Query("SELECT s FROM AppSession s WHERE s.startTime >= :since")
    List<AppSession> findSessionsSince(@Param("since") LocalDateTime since);
}
