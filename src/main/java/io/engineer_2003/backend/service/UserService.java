package io.engineer_2003.backend.service;

import org.springframework.stereotype.Service;

import io.engineer_2003.backend.entity.User;
import io.engineer_2003.backend.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        return userRepository.save(user);
    }
}
