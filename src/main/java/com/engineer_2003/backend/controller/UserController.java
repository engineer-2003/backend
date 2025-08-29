package com.engineer_2003.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.engineer_2003.backend.dto.UserRegistrationDto;
import com.engineer_2003.backend.dto.UserResponseDto;
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
    public ResponseEntity<UserResponseDto> signup(@RequestBody UserRegistrationDto userDto) {
        if (!userDto.isValid()) {
            throw new IllegalArgumentException(userDto.getValidationMessage());
        }
        UserResponseDto response = userService.registerUser(userDto);
        return ResponseEntity.ok(response);
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<UserResponseDto> login(@RequestBody UserRegistrationDto loginDto) {
        UserResponseDto response = userService.login(loginDto.getUsername(), loginDto.getPassword());
        if (response == null) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }
        return ResponseEntity.ok(response);
    }
}
