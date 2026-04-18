package com.resumepilot.auth.dto;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String fullName;
    private String email;
    private String password; // Plain text from UI
    private String phone;
}