package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String email;
    private String password;
    private int spins; // 🔥 thêm lượt quay
    
    // Tổng điểm từ các mini-game (100 điểm = 1 rương)
    private int miniGamePoints = 0;

    // Lưu trữ lượt chơi chưa hoàn thành khi thoát web
    private int pendingMiniGame = 0;
    private int pendingSlotMachine = 0;
}