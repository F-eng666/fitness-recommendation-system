package com.fitness.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class RegisterRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private String nickname;
    private String email;
    private String phone;
    private String role;
}