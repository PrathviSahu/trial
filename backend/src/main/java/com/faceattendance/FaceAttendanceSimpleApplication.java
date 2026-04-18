package com.faceattendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Simple Face Attendance Application
 * Minimal version for frontend integration
 */
@SpringBootApplication
public class FaceAttendanceSimpleApplication {

    public static void main(String[] args) {
        SpringApplication.run(FaceAttendanceSimpleApplication.class, args);
        System.out.println("\n🚀 FaceTrackU Backend Started Successfully!");
        System.out.println("📊 Health Check: http://localhost:8080/api/actuator/health");
        System.out.println("🔐 Login Endpoint: http://localhost:8080/api/auth/login");
        System.out.println("🌐 Frontend: http://localhost:3001");
    }

}
