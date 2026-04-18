package com.faceattendance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple Authentication Controller for Testing
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class SimpleAuthController {

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("usernameOrEmail");
        String password = loginRequest.get("password");
        
        // Simple validation for demo
        if ("admin".equals(username) && "admin123".equals(password)) {
            Map<String, Object> response = new HashMap<>();
            response.put("token", "demo-jwt-token-12345");
            response.put("type", "Bearer");
            response.put("refreshToken", "demo-refresh-token");
            response.put("id", 1L);
            response.put("username", "admin");
            response.put("email", "admin@faceattendance.com");
            response.put("firstName", "System");
            response.put("lastName", "Administrator");
            response.put("role", "SUPER_ADMIN");
            response.put("authorities", new String[]{"ROLE_ADMIN"});
            
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Invalid username or password");
            return ResponseEntity.status(401).body(error);
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }
}
