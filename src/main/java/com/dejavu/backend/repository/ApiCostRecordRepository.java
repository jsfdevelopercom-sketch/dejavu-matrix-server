package com.dejavu.backend.repository;

import com.dejavu.backend.model.ApiCostRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ApiCostRecordRepository extends JpaRepository<ApiCostRecord, Long> {
    
    @Query("SELECT COALESCE(SUM(c.costIncurred), 0.0) FROM ApiCostRecord c")
    double getTotalCostTillDate();

    @Query("SELECT COALESCE(SUM(c.costIncurred), 0.0) FROM ApiCostRecord c WHERE c.timestamp >= :since")
    double getTotalCostSince(LocalDateTime since);
}
