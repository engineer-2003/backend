package com.engineer_2003.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.engineer_2003.backend.dto.UserRegistrationDto;
import com.engineer_2003.backend.dto.UserResponseDto;
import com.engineer_2003.backend.entity.User;
import com.engineer_2003.backend.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 회원가입
    @Transactional
    public UserResponseDto registerUser(UserRegistrationDto userDto) {
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        User user = User.builder()
                .username(userDto.getUsername())
                .password(userDto.getPassword()) // 실제 서비스에서는 반드시 암호화 필요
                .build();

        User savedUser = userRepository.save(user);

        return UserResponseDto.builder()
                .userId(savedUser.getUserId())
                .username(savedUser.getUsername())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    // 로그인
    public UserResponseDto login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            return UserResponseDto.builder()
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .createdAt(user.getCreatedAt())
                    .build();
        }
        return null;
    }
}
