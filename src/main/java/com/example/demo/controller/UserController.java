package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public List<User> getAll() {
        return service.getAll();
    }

    @GetMapping("/admin/users")
    public List<User> getAdminUsers() {
        return service.getAll();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        return service.create(user);
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User request) {
        User user = service.getByUsername(request.getUsername());

        if (user == null || !request.getPassword().equals(user.getPassword())) {
            return ResponseEntity.status(401).body("Sai tài khoản hoặc mật khẩu");
        }

        return ResponseEntity.ok(user);
    }
    // 🔥 FIX LỖI: dùng service thay vì repository
    @GetMapping("/{username}")
    public User getUser(@PathVariable String username) {
        return service.getByUsername(username);
    }

    // API dành cho Admin chỉnh sửa lượt quay
    @PutMapping("/{username}/spins")
    public ResponseEntity<?> updateSpins(@PathVariable String username, @RequestBody java.util.Map<String, Integer> body) {
        User user = service.getByUsername(username);
        if (user == null) return ResponseEntity.badRequest().body("User không tồn tại");
        
        Integer newSpins = body.get("spins");
        if (newSpins == null) return ResponseEntity.badRequest().body("Thiếu tham số spins");
        
        user.setSpins(newSpins);
        service.create(user); // Lưu lại thay đổi
        return ResponseEntity.ok("Cập nhật thành công số lượt quay thành: " + newSpins);
    }
}