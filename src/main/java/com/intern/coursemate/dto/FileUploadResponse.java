package com.intern.coursemate.dto;

import org.joda.time.LocalDateTime;

import lombok.Data;

@Data
public class FileUploadResponse {
    private String filePath;
    private LocalDateTime dateTime;
}