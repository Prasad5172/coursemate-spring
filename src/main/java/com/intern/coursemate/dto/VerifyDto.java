package com.intern.coursemate.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerifyDto {
    @Email(message = "The email should valid")
    @NotNull(message = "The email should not null")
    @NotEmpty(message = "email should not be empty")
    private String email;
    private String verificationCode;
}
