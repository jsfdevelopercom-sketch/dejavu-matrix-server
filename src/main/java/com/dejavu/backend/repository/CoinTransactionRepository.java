package com.dejavu.backend.repository;

import com.dejavu.backend.model.CoinTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, Long> {
    List<CoinTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
