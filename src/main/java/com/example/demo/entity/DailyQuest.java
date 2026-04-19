package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "daily_quests")
@Data
public class DailyQuest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    private String questType; // BREAKFAST, LUNCH, DINNER
    
    @Column(columnDefinition = "LONGTEXT")
    private String imageData;
    
    private String note;
    private String status; // PENDING, APPROVED, REJECTED
    private LocalDate questDate;
}
