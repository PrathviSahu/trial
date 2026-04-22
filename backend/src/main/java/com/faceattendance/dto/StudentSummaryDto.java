package com.faceattendance.dto;

import java.time.LocalDateTime;

public record StudentSummaryDto(
        Long id,
        String ienNumber,
        String rollNumber,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String department,
        String branch,
        Integer year,
        Integer semester,
        Boolean isActive,
        Boolean faceEnrolled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

