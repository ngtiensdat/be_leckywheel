package com.example.demo.controller;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/wheel")
@CrossOrigin(origins = "*")
public class WheelController {

    private final UserService userService;
    private final SpinHistoryRepository historyRepo;
    private final InventoryRepository inventoryRepo;
    private final GameRecordRepository gameRecordRepo;

    public WheelController(UserService userService,
                           SpinHistoryRepository historyRepo,
                           InventoryRepository inventoryRepo,
                           GameRecordRepository gameRecordRepo) {
        this.userService = userService;
        this.historyRepo = historyRepo;
        this.inventoryRepo = inventoryRepo;
        this.gameRecordRepo = gameRecordRepo;
    }

    // 🎯 QUAY
    @PostMapping("/spin/{username}")
    public ResponseEntity<?> spin(@PathVariable String username) {

        User user = userService.getByUsername(username);

        if (user == null) {
            return ResponseEntity.badRequest().body("User không tồn tại");
        }

        if (user.getSpins() <= 0) {
            return ResponseEntity.badRequest().body("Hết lượt quay");
        }

        // trừ lượt
        user.setSpins(user.getSpins() - 1);

        // 🎁 DANH SÁCH PHẦN THƯỞNG
        List<String> prizes = Arrays.asList(
                "½ Stabuck",
                "½ gội đầu",
                "Slot Machine",
                "½ gội đầu đắp mặt nạ",
                "½ massage mặt",
                "½ voucher mua sắm 100k",
                "+ 2 lượt quay",
                "Chúc may mắn lần sau",
                "½ hoa cắm bình",
                "Mini game"
        );

        // 🎯 TỶ LỆ (phải cùng length với prizes)
        int[] weights = {
                10,
                10,
                10, // Slot Machine tỷ lệ cao
                10,
                10,
                10,
                10,
                10,
                10,
                10  // Mini game tỷ lệ cao
        };

        // 🔥 RANDOM THEO TỶ LỆ
        int total = Arrays.stream(weights).sum();
        int rand = new Random().nextInt(total);

        int index = 0;
        for (int i = 0; i < weights.length; i++) {
            rand -= weights[i];
            if (rand < 0) {
                index = i;
                break;
            }
        }

        String prize = prizes.get(index);

        // 📜 LƯU HISTORY
        SpinHistory history = new SpinHistory();
        history.setUsername(username);
        history.setPrize(prize);
        historyRepo.save(history);

        // 🎯 XỬ LÝ PHẦN THƯỞNG ĐẶC BIỆT
        if (prize.equals("+ 2 lượt quay")) {
            user.setSpins(user.getSpins() + 2);
        } else if (prize.equals("Slot Machine")) {
            user.setPendingSlotMachine(user.getPendingSlotMachine() + 1);
        } else if (prize.equals("Mini game")) {
            user.setPendingMiniGame(user.getPendingMiniGame() + 1);
        }

        // 🎒 LƯU INVENTORY (chỉ lưu các vật phẩm thực tế)
        String finalOutputPrize = prize;
        boolean isGameTrigger = prize.equals("Slot Machine") || prize.equals("Mini game") || prize.equals("+ 2 lượt quay");
        if (!prize.equals("Chúc may mắn lần sau") && !isGameTrigger) {
            finalOutputPrize = addOrMergeInventory(username, prize);
        }

        // 💾 SAVE USER
        userService.save(user);

        // 🎯 RESPONSE TRẢ VỀ CHO FE
        Map<String, Object> res = new HashMap<>();
        res.put("prize", finalOutputPrize);
        res.put("spinsLeft", user.getSpins());
        res.put("index", index); // ⭐ QUAN TRỌNG

        return ResponseEntity.ok(res);
    }

    // 🧩 HÀM GHÉP MẢNH TỰ ĐỘNG
    private String addOrMergeInventory(String username, String prize) {
        if (!prize.startsWith("½ ")) {
            Inventory item = new Inventory();
            item.setUsername(username);
            item.setItemName(prize);
            inventoryRepo.save(item);
            return prize;
        }

        List<Inventory> userInventory = inventoryRepo.findByUsername(username);
        Inventory matchedHalf = null;

        for (Inventory inv : userInventory) {
            if (inv.getItemName().equals(prize)) {
                matchedHalf = inv;
                break;
            }
        }

        if (matchedHalf != null) {
            // Đã có 1 nửa, ghép thành 1
            inventoryRepo.delete(matchedHalf);
            String fullTitle = "1 " + prize.substring(2);
            Inventory fullItem = new Inventory();
            fullItem.setUsername(username);
            fullItem.setItemName(fullTitle);
            inventoryRepo.save(fullItem);
            
            return fullTitle + " (Ghép từ 2 thẻ ½)";
        } else {
            // Chưa có, lưu dạng ½
            Inventory item = new Inventory();
            item.setUsername(username);
            item.setItemName(prize);
            inventoryRepo.save(item);
            return prize;
        }
    }

    // 🎰 QUAY SLOT MACHINE
    @PostMapping("/spin-slot/{username}")
    public ResponseEntity<?> spinSlot(@PathVariable String username) {
        User user = userService.getByUsername(username);

        if (user == null) {
            return ResponseEntity.badRequest().body("User không tồn tại");
        }

        if (user.getPendingSlotMachine() <= 0) {
            return ResponseEntity.badRequest().body("Bạn không có lượt quay Slot Machine!");
        }

        // trừ lượt slot
        user.setPendingSlotMachine(user.getPendingSlotMachine() - 1);

        Random random = new Random();
        int r1 = random.nextInt(5) + 1; // Số từ 1 đến 5
        int r2 = random.nextInt(5) + 1;
        int r3 = random.nextInt(5) + 1;

        boolean isWin = (r1 == r2 && r2 == r3);
        String prize = "Chúc may mắn lần sau";

        if (isWin) {
            // lưu 3 số giống nhau đó vào túi quà
            prize = "" + r1 + r1 + r1; 
        }

        // 📜 LƯU HISTORY
        SpinHistory history = new SpinHistory();
        history.setUsername(username);
        history.setPrize(isWin ? "Slot Trúng: " + prize : prize);
        historyRepo.save(history);

        // 🎒 LƯU INVENTORY
        if (isWin) {
            addOrMergeInventory(username, prize);
        }

        // 💾 SAVE USER
        userService.save(user);

        // 🎯 RESPONSE TRẢ VỀ CHO FE
        Map<String, Object> res = new HashMap<>();
        res.put("prize", prize);
        res.put("spinsLeft", user.getSpins());
        res.put("reels", Arrays.asList(r1, r2, r3));

        return ResponseEntity.ok(res);
    }

    @PostMapping("/consume-mini-game/{username}")
    public ResponseEntity<?> consumeMiniGame(@PathVariable String username) {
        User user = userService.getByUsername(username);
        if (user != null && user.getPendingMiniGame() > 0) {
            user.setPendingMiniGame(user.getPendingMiniGame() - 1);
            userService.save(user);
            return ResponseEntity.ok(Collections.singletonMap("pendingMiniGame", user.getPendingMiniGame()));
        }
        return ResponseEntity.badRequest().body("Không có lượt chơi mini game");
    }

    // 📜 HISTORY
    @GetMapping("/history/{username}")
    public List<SpinHistory> history(@PathVariable String username) {
        return historyRepo.findByUsernameOrderByCreatedAtDesc(username);
    }

    // 🎒 INVENTORY
    @GetMapping("/inventory/{username}")
    public List<Inventory> inventory(@PathVariable String username) {
        return inventoryRepo.findByUsername(username);
    }

    // ❌ XÓA ITEM
    @DeleteMapping("/inventory/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable Long id) {
        inventoryRepo.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }

    // 📩 YÊU CẦU SỬ DỤNG ITEM
    @PutMapping("/inventory/{id}/use")
    public ResponseEntity<?> useItem(@PathVariable Long id) {
        Optional<Inventory> opt = inventoryRepo.findById(id);
        if (opt.isPresent()) {
            Inventory i = opt.get();
            i.setStatus("Đang chờ duyệt");
            inventoryRepo.save(i);
            return ResponseEntity.ok(Collections.singletonMap("msg", "Đã gửi yêu cầu!"));
        }
        return ResponseEntity.notFound().build();
    }

    // 🏆 GAME RECORDS
    @GetMapping("/records/{username}")
    public List<GameRecord> getRecords(@PathVariable String username) {
        return gameRecordRepo.findByUsernameOrderByScoreDesc(username);
    }

    @PostMapping("/record")
    public ResponseEntity<?> addRecord(@RequestBody GameRecord record) {
        gameRecordRepo.save(record);
        return ResponseEntity.ok("Record saved");
    }

    // 🧩 GHÉP VÉ
    @PostMapping("/inventory/merge")
    public ResponseEntity<?> mergeTickets(@RequestBody Map<String, Object> body) {
        Long id1 = Long.valueOf(body.get("id1").toString());
        Long id2 = Long.valueOf(body.get("id2").toString());
        String newItemName = body.get("newItemName").toString();
        String username = body.get("username").toString();

        inventoryRepo.deleteById(id1);
        inventoryRepo.deleteById(id2);

        Inventory newItem = new Inventory();
        newItem.setUsername(username);
        newItem.setItemName(newItemName);
        newItem.setStatus("Mới");
        inventoryRepo.save(newItem);

        return ResponseEntity.ok(Collections.singletonMap("msg", "Đã ghép vé thành công!"));
    }

    // ⭐️ CỘNG ĐIỂM MINI GAME
    @PostMapping("/add-points/{username}")
    public ResponseEntity<?> addPoints(@PathVariable String username, @RequestBody Map<String, Integer> body) {
        int add = body.get("points");
        com.example.demo.entity.User user = userService.getByUsername(username);
        if (user != null) {
            int newTotal = user.getMiniGamePoints() + add;
            int chestsAwarded = 0;
            while (newTotal >= 100) {
                newTotal -= 100;
                chestsAwarded++;
                // Thưởng một rương báu
                Inventory chest = new Inventory();
                chest.setUsername(username);
                chest.setItemName("Rương Báu (Đạt 100đ NL)");
                chest.setStatus("Mới");
                inventoryRepo.save(chest);
            }
            user.setMiniGamePoints(newTotal);
            userService.save(user);

            Map<String, Object> res = new HashMap<>();
            res.put("points", newTotal);
            res.put("chestsAwarded", chestsAwarded);
            return ResponseEntity.ok(res);
        }
        return ResponseEntity.notFound().build();
    }

    // 🎁 GAME PHỤ THÊM QUÀ (Snake, Guess Number)
    @PostMapping("/add-inventory/{username}")
    public ResponseEntity<?> addInventory(@PathVariable String username, @RequestBody Map<String, String> body) {
        String prize = body.get("itemName");
        if (prize != null && !prize.trim().isEmpty()) {
            addOrMergeInventory(username, prize);

            SpinHistory history = new SpinHistory();
            history.setUsername(username);
            history.setPrize(prize);
            historyRepo.save(history);
        }
        return ResponseEntity.ok(Collections.singletonMap("msg", "Tặng quà thành công"));
    }
}