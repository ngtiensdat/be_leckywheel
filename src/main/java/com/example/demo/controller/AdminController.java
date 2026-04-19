package com.example.demo.controller;

import com.example.demo.entity.Inventory;
import com.example.demo.entity.User;
import com.example.demo.repository.InventoryRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;

    public AdminController(UserRepository userRepository, InventoryRepository inventoryRepository) {
        this.userRepository = userRepository;
        this.inventoryRepository = inventoryRepository;
    }

    // 1. Quản lý người dùng
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping("/users")
    public User addUser(@RequestBody User user) {
        return userRepository.save(user);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User newUserData) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isPresent()) {
            User u = opt.get();
            u.setUsername(newUserData.getUsername());
            if (newUserData.getPassword() != null && !newUserData.getPassword().isEmpty()) {
                u.setPassword(newUserData.getPassword());
            }
            u.setSpins(newUserData.getSpins());
            userRepository.save(u);
            return ResponseEntity.ok(u);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if(userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // 2. Quản lý Inventory (Duyệt vé)
    @GetMapping("/inventory/requests")
    public List<Inventory> getAllRequests() {
        return inventoryRepository.findAll(); // Lấy hết trả về để dùng filter ở frontend
    }

    @PutMapping("/inventory/{id}/approve")
    public ResponseEntity<?> approveInventory(@PathVariable Long id) {
        Optional<Inventory> opt = inventoryRepository.findById(id);
        if (opt.isPresent()) {
            Inventory i = opt.get();
            i.setStatus("Đã duyệt");
            inventoryRepository.save(i);
            return ResponseEntity.ok(i);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/inventory/{id}/reject")
    public ResponseEntity<?> rejectInventory(@PathVariable Long id) {
        Optional<Inventory> opt = inventoryRepository.findById(id);
        if (opt.isPresent()) {
            Inventory i = opt.get();
            i.setStatus("Bị từ chối");
            inventoryRepository.save(i);
            return ResponseEntity.ok(i);
        }
        return ResponseEntity.notFound().build();
    }
}
