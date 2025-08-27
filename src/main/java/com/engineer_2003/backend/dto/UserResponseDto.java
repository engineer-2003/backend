package com.engineer_2003.backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class UserResponseDto {
    private Long userId;
    private String username;
    private LocalDateTime createdAt;
}