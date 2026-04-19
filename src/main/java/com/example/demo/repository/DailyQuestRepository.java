package com.example.demo.repository;

import com.example.demo.entity.DailyQuest;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyQuestRepository extends JpaRepository<DailyQuest, Long> {
    List<DailyQuest> findByUserAndQuestDate(User user, LocalDate date);
    Optional<DailyQuest> findByUserAndQuestTypeAndQuestDate(User user, String type, LocalDate date);
    List<DailyQuest> findByStatus(String status);
}
