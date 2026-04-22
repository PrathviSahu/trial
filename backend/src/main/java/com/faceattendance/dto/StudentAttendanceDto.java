package com.faceattendance.dto;

public record StudentAttendanceDto(
        Long id,
        String firstName,
        String lastName,
        String ienNumber,
        String department,
        String branch
) {}

