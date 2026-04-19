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
}