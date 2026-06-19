package com.dejavu.backend.repository;
import com.dejavu.backend.model.RamonNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RamonNotificationRepository extends JpaRepository<RamonNotification, Long> {
    List<RamonNotification> findByIsReadFalse();
}
