package com.faceattendance.dto;

public record StudentFaceDto(
        Long id,
        String ienNumber,
        String rollNumber,
        String firstName,
        String lastName,
        String email,
        String department,
        String branch,
        Integer year,
        Integer semester,
        Boolean faceEnrolled,
        String faceDescriptor
) {}

