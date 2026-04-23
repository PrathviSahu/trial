package com.faceattendance.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Health Check Controller
 * 
 * Provides system health and status information
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping
@Slf4j
public class HealthController {

    private static final ExecutorService HEALTH_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "db-health-check");
        thread.setDaemon(true);
        return thread;
    });

    @Autowired
    private DataSource dataSource;

    @Autowired(required = false)
    private BuildProperties buildProperties;

    /**
     * Basic health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        try {
            Map<String, Object> healthInfo = new HashMap<>();
            healthInfo.put("status", "UP");
            healthInfo.put("timestamp", LocalDateTime.now());
            healthInfo.put("service", "AI Face Attendance System Backend");
            healthInfo.put("version", buildProperties != null ? buildProperties.getVersion() : "1.0.0");
            
            // Check database connectivity
            Future<Map<String, Object>> dbCheck = HEALTH_EXECUTOR.submit(() -> {
                try (Connection connection = dataSource.getConnection()) {
                    Map<String, Object> dbInfo = new HashMap<>();
                    dbInfo.put("database", "Connected");
                    dbInfo.put("databaseUrl", connection.getMetaData().getURL());
                    return dbInfo;
                }
            });

            try {
                healthInfo.putAll(dbCheck.get(4, TimeUnit.SECONDS));
            } catch (TimeoutException timeoutException) {
                dbCheck.cancel(true);
                healthInfo.put("database", "Waking up");
                healthInfo.put("databaseError", "Database check timed out while the backend was starting up");
            } catch (Exception e) {
                dbCheck.cancel(true);
                healthInfo.put("database", "Disconnected");
                healthInfo.put("databaseError", e.getMessage());
            }
            
            return ResponseEntity.ok(healthInfo);
            
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage());
            
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("status", "DOWN");
            errorInfo.put("timestamp", LocalDateTime.now());
            errorInfo.put("error", e.getMessage());
            
            return ResponseEntity.status(500)
                    .body(Map.of("status", "DOWN", "message", "System health check failed"));
        }
    }

    /**
     * Detailed system information endpoint
     */
    @GetMapping("/info")
    public ResponseEntity<?> info() {
        try {
            Map<String, Object> systemInfo = new HashMap<>();
            
            // Application info
            systemInfo.put("application", "AI Face Attendance System");
            systemInfo.put("description", "AI-powered Face Recognition Attendance System Backend");
            systemInfo.put("version", buildProperties != null ? buildProperties.getVersion() : "1.0.0");
            systemInfo.put("buildTime", buildProperties != null ? buildProperties.getTime() : "Unknown");
            
            // System info
            systemInfo.put("javaVersion", System.getProperty("java.version"));
            systemInfo.put("javaVendor", System.getProperty("java.vendor"));
            systemInfo.put("osName", System.getProperty("os.name"));
            systemInfo.put("osVersion", System.getProperty("os.version"));
            
            // Runtime info
            Runtime runtime = Runtime.getRuntime();
            systemInfo.put("maxMemory", runtime.maxMemory() / (1024 * 1024) + " MB");
            systemInfo.put("totalMemory", runtime.totalMemory() / (1024 * 1024) + " MB");
            systemInfo.put("freeMemory", runtime.freeMemory() / (1024 * 1024) + " MB");
            systemInfo.put("availableProcessors", runtime.availableProcessors());
            
            // Features
            Map<String, String> features = new HashMap<>();
            features.put("authentication", "JWT-based admin authentication");
            features.put("database", "MySQL with JPA/Hibernate");
            features.put("faceRecognition", "OpenCV-based face recognition");
            features.put("fileProcessing", "Excel/CSV import/export with Apache POI");
            features.put("departments", "CE, CSD, AIDS, MECHATRONICS, CIVIL, IT");
            systemInfo.put("features", features);
            
            systemInfo.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(systemInfo);
            
        } catch (Exception e) {
            log.error("Error getting system info: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("status", "DOWN", "message", "Error retrieving system information"));
        }
    }
}
