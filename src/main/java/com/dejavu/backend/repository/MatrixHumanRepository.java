package com.dejavu.backend.repository;

import com.dejavu.backend.model.MatrixHuman;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatrixHumanRepository extends JpaRepository<MatrixHuman, Long> {
}
