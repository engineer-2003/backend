package com.engineer_2003.backend.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserResponseDto {
    private Long userId;
    private String username;
    private LocalDateTime createdAt;
}