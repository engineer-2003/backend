package com.engineer_2003.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.engineer_2003.backend.dto.UserRegistrationDto;
import com.engineer_2003.backend.service.UserService;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody UserRegistrationDto userDto) {
        if (!userDto.isValid()) {
            throw new IllegalArgumentException(userDto.getValidationMessage());
        }
        userService.registerUser(userDto);
        return ResponseEntity.ok().build();
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserRegistrationDto loginDto) {
        String token = userService.login(loginDto.getUsername(), loginDto.getPassword());
        if (token == null) {
            return ResponseEntity.status(401).body("로그인 실패"); // Unauthorized
        }
        return ResponseEntity.ok(token);
    }
}