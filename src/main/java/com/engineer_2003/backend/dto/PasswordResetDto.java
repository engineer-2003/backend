// src/main/java/.../dto/PasswordResetDto.java

package com.engineer_2003.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetDto {
    private String token;
    private String newPassword;
}