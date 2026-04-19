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
            // Kiểm tra null để tránh lỗi crash
            if (checkIn.getLastDate() != null) {
                if (checkIn.getLastDate().equals(today)) {
                    return ResponseEntity.badRequest().body("Hôm nay bạn đã điểm danh rồi!");
                }
                
                if (checkIn.getLastDate().equals(today.minusDays(1))) {
                    checkIn.setStreak(checkIn.getStreak() + 1);
                    if (checkIn.getStreak() >= 2) {
                        user.setSpins(user.getSpins() + 1);
                        userRepository.save(user);
                    }
                } else {
                    checkIn.setStreak(1);
                }
            } else {
                checkIn.setStreak(1);
            }
        }

        checkIn.setLastDate(today);
        checkInRepository.save(checkIn);
        return ResponseEntity.ok(checkIn);
    }

    // Lấy trạng thái nộp bài của cả 3 bữa trong ngày
    @GetMapping("/daily-status/{username}")
    public List<DailyQuest> getDailyStatus(@PathVariable String username) {
        User user = userRepository.findByUsername(username);
        return dailyQuestRepository.findByUserAndQuestDate(user, LocalDate.now());
    }

    // --- LOGIC NHIỆM VỤ ẢNH ---
    @PostMapping("/submit/{username}")
    public ResponseEntity<?> submitQuest(@PathVariable String username, @RequestBody DailyQuest requestBody) {
        User user = userRepository.findByUsername(username);
        LocalTime now = LocalTime.now();
        String type = requestBody.getQuestType(); // Nhận type từ Frontend gửi lên

        // Chặn khung giờ nghiêm ngặt
        boolean onTime = false;
        if (type.equals("BREAKFAST") && now.isAfter(LocalTime.of(6, 0)) && now.isBefore(LocalTime.of(10, 0))) onTime = true;
        else if (type.equals("LUNCH") && now.isAfter(LocalTime.of(12, 0)) && now.isBefore(LocalTime.of(14, 0))) onTime = true;
        else if (type.equals("DINNER") && now.isAfter(LocalTime.of(17, 0)) && now.isBefore(LocalTime.of(21, 0))) onTime = true;

        if (!onTime) return ResponseEntity.badRequest().body("Không đúng khung giờ của nhiệm vụ này!");

        Optional<DailyQuest> existing = dailyQuestRepository.findByUserAndQuestTypeAndQuestDate(user, type, LocalDate.now());
        if (existing.isPresent()) return ResponseEntity.badRequest().body("Bạn đã gửi nhiệm vụ này rồi!");

        DailyQuest quest = new DailyQuest();
        quest.setUser(user);
        quest.setQuestType(type);
        quest.setImageData(requestBody.getImageData());
        quest.setNote(requestBody.getNote());
        quest.setStatus("PENDING");
        quest.setQuestDate(LocalDate.now());
        
        dailyQuestRepository.save(quest);
        return ResponseEntity.ok("Gửi thành công!");
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
