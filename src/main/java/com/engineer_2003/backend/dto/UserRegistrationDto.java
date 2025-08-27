package com.engineer_2003.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegistrationDto {
    private String username;
    private String password;

    // 간단한 검증 메서드
    public boolean isValid() {
        return username != null && !username.trim().isEmpty()
                && username.length() >= 3 && username.length() <= 50
                && password != null && password.length() >= 6;
    }

    public String getValidationMessage() {
        if (username == null || username.trim().isEmpty()) {
            return "사용자명은 필수입니다.";
        }
        if (username.length() < 3 || username.length() > 50) {
            return "사용자명은 3-50자 사이여야 합니다.";
        }
        if (password == null || password.length() < 6) {
            return "비밀번호는 최소 6자 이상이어야 합니다.";
        }
        return null;
    }
}
