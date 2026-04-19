package com.example.demo.controller;

import com.example.demo.entity.CheckIn;
import com.example.demo.entity.DailyQuest;
import com.example.demo.entity.User;
import com.example.demo.repository.CheckInRepository;
import com.example.demo.repository.DailyQuestRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/quests")
@CrossOrigin(origins = "*")
public class QuestController {

    @Autowired private UserRepository userRepository;
    @Autowired private CheckInRepository checkInRepository;
    @Autowired private DailyQuestRepository dailyQuestRepository;

    // --- LOGIC ĐIỂM DANH ---
    @PostMapping("/checkin/{username}")
    public ResponseEntity<?> checkIn(@PathVariable String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) return ResponseEntity.badRequest().body("User không tồn tại");

        LocalDate today = LocalDate.now();
        CheckIn checkIn = checkInRepository.findByUser(user).orElse(new CheckIn());
        
        if (checkIn.getUser() == null) {
            checkIn.setUser(user);
            checkIn.setStreak(1);
        } else {
            if (checkIn.getLastDate().equals(today)) {
                return ResponseEntity.badRequest().body("Hôm nay bạn đã điểm danh rồi!");
            }
            
            if (checkIn.getLastDate().equals(today.minusDays(1))) {
                checkIn.setStreak(checkIn.getStreak() + 1);
                // Điểm danh liên tiếp từ ngày thứ 2 trở đi là được cộng 1 lượt
                if (checkIn.getStreak() >= 2) {
                    user.setSpins(user.getSpins() + 1);
                    userRepository.save(user);
                }
            } else {
                checkIn.setStreak(1);
            }
        }

        checkIn.setLastDate(today);
        checkInRepository.save(checkIn);
        return ResponseEntity.ok(checkIn);
    }

    @GetMapping("/checkin/status/{username}")
    public ResponseEntity<?> getCheckInStatus(@PathVariable String username) {
        User user = userRepository.findByUsername(username);
        return ResponseEntity.ok(checkInRepository.findByUser(user).orElse(null));
    }

    // --- LOGIC NHIỆM VỤ ẢNH ---
    @PostMapping("/submit/{username}")
    public ResponseEntity<?> submitQuest(@PathVariable String username, @RequestBody DailyQuest request) {
        User user = userRepository.findByUsername(username);
        LocalTime now = LocalTime.now();
        String type = "";

        // Xác định khung giờ
        if (now.isAfter(LocalTime.of(6, 0)) && now.isBefore(LocalTime.of(10, 0))) type = "BREAKFAST";
        else if (now.isAfter(LocalTime.of(12, 0)) && now.isBefore(LocalTime.of(14, 0))) type = "LUNCH";
        else if (now.isAfter(LocalTime.of(17, 0)) && now.isBefore(LocalTime.of(21, 0))) type = "DINNER";
        else return ResponseEntity.badRequest().body("Hiện không trong khung giờ nhiệm vụ (6-10h, 12-14h, 17-21h)");

        // Kiểm tra xem hôm nay đã gửi chưa
        Optional<DailyQuest> existing = dailyQuestRepository.findByUserAndQuestTypeAndQuestDate(user, type, LocalDate.now());
        if (existing.isPresent()) return ResponseEntity.badRequest().body("Bạn đã gửi ảnh cho nhiệm vụ này rồi!");

        DailyQuest quest = new DailyQuest();
        quest.setUser(user);
        quest.setQuestType(type);
        quest.setImageData(request.getImageData());
        quest.setNote(request.getNote());
        quest.setStatus("PENDING");
        quest.setQuestDate(LocalDate.now());
        
        dailyQuestRepository.save(quest);
        return ResponseEntity.ok("Đã gửi nhiệm vụ đang chờ Admin duyệt!");
    }

    @GetMapping("/admin/pending")
    public List<DailyQuest> getPendingQuests() {
        return dailyQuestRepository.findByStatus("PENDING");
    }

    @PutMapping("/admin/approve/{id}")
    public ResponseEntity<?> approveQuest(@PathVariable Long id) {
        DailyQuest quest = dailyQuestRepository.findById(id).orElse(null);
        if (quest == null) return ResponseEntity.notFound().build();

        quest.setStatus("APPROVED");
        dailyQuestRepository.save(quest);

        // Kiểm tra xem đã đủ 3 nhiệm vụ APPROVED trong cùng 1 ngày chưa
        User user = quest.getUser();
        List<DailyQuest> approvedToday = dailyQuestRepository.findByUserAndQuestDate(user, quest.getQuestDate())
                .stream().filter(q -> q.getStatus().equals("APPROVED")).toList();

        if (approvedToday.size() == 3) {
            user.setSpins(user.getSpins() + 1);
            userRepository.save(user);
        }

        return ResponseEntity.ok("Đã duyệt nhiệm vụ!");
    }

    @PutMapping("/admin/reject/{id}")
    public ResponseEntity<?> rejectQuest(@PathVariable Long id) {
        DailyQuest quest = dailyQuestRepository.findById(id).orElse(null);
        if (quest == null) return ResponseEntity.notFound().build();
        quest.setStatus("REJECTED");
        dailyQuestRepository.save(quest);
        return ResponseEntity.ok("Đã từ chối nhiệm vụ!");
    }
}
