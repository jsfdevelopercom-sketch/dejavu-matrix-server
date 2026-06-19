package com.dejavu.backend.repository;

import com.dejavu.backend.model.CostWarningRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CostWarningRecordRepository extends JpaRepository<CostWarningRecord, Long> {
}
