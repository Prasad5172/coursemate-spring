package com.intern.coursemate.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RestError {
    private String statusCode;
    private String errorMessage;
}
