package com.example.demo.repository;

import com.example.demo.entity.SpinHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpinHistoryRepository extends JpaRepository<SpinHistory, Long> {

    List<SpinHistory> findByUsernameOrderByCreatedAtDesc(String username);
}