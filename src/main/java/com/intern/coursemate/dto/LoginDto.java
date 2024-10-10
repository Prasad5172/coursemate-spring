package com.intern.coursemate.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoginDto {
    @Email(message = "The email should valid")
    @NotNull(message = "The email should not null")
    @NotEmpty(message = "email should not be empty")
    private String email;
    @NotNull(message = "The password should not null")
    @NotEmpty(message = "The password should not empty")
    private String password;
}
