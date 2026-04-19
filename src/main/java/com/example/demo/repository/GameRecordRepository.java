package com.example.demo.repository;

import com.example.demo.entity.GameRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GameRecordRepository extends JpaRepository<GameRecord, Long> {
    List<GameRecord> findTop10ByOrderByScoreDesc();
    List<GameRecord> findByUsernameOrderByScoreDesc(String username);
}
