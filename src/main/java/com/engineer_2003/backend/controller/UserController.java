// src/main/java/com/engineer_2003/backend/controller/UserController.java
package com.engineer_2003.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping; // GetMapping 추가
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam; // RequestParam 추가
import org.springframework.web.bind.annotation.RestController;

import com.engineer_2003.backend.dto.GoogleLoginRequestDto;
import com.engineer_2003.backend.dto.PasswordResetDto;
import com.engineer_2003.backend.dto.UserRegistrationDto; 
import com.engineer_2003.backend.service.UserService;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 아이디 중복 확인 API
    @GetMapping("/check-username")
    public ResponseEntity<Boolean> checkUsername(@RequestParam String username) {
        boolean exists = userService.checkUsernameDuplication(username);
        return ResponseEntity.ok(exists);
    }
    
    // 이메일 중복 확인 API
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmail(@RequestParam String email) {
        boolean exists = userService.checkEmailDuplication(email);
        return ResponseEntity.ok(exists);
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
            return ResponseEntity.status(401).body("로그인 실패");
        }
        return ResponseEntity.ok(token);
    }

    // Google 로그인 엔드포인트 추가
    @PostMapping("/login/google")
    public ResponseEntity<String> googleLogin(@RequestBody GoogleLoginRequestDto requestDto) {
        String token = userService.processGoogleLogin(requestDto.getEmail());
        if (token == null) {
            return ResponseEntity.status(401).body("로그인 실패");
        }
        return ResponseEntity.ok(token);
    }
    
    // 비밀번호 찾기
    @PostMapping("/find-password")
    public ResponseEntity<Void> findPassword(@RequestBody UserRegistrationDto userDto) {
        userService.findPassword(userDto.getUsername(), userDto.getEmail());
        return ResponseEntity.ok().build();
    }
    
    // 비밀번호 재설정
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody PasswordResetDto resetDto) {
        userService.resetPassword(resetDto.getToken(), resetDto.getNewPassword());
        return ResponseEntity.ok().build();
    }
}