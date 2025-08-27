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

    @PostMapping("/signup")
    public ResponseEntity<UserResponseDto> signup(@RequestBody UserRegistrationDto userDto) {
        // 수동 검증
        if (!userDto.isValid()) {
            throw new IllegalArgumentException(userDto.getValidationMessage());
        }

        UserResponseDto response = userService.registerUser(userDto);
        return ResponseEntity.ok(response);
    }
}