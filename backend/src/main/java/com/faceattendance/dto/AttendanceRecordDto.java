package com.faceattendance.dto;

import java.time.LocalDateTime;

public record AttendanceRecordDto(
        Long id,
        LocalDateTime timestamp,
        Double confidence,
        String method,
        String markedBy,
        String subject,
        String classId,
        String status,
        StudentAttendanceDto student
) {}

