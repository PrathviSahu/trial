package com.faceattendance.controller;

import com.faceattendance.model.Guardian;
import com.faceattendance.service.GuardianService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/guardians")
@CrossOrigin(origins = "*")
public class GuardianController {

    @Autowired
    private GuardianService guardianService;

    /**
     * Register new guardian
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Guardian guardian) {
        try {
            Guardian registered = guardianService.registerGuardian(guardian);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Guardian registered successfully",
                "guardian", registered
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Guardian login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            String email = credentials.get("email");
            String password = credentials.get("password");
            
            Map<String, Object> response = guardianService.login(email, password);
            response.put("success", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get student attendance (guardian view)
     */
    @GetMapping("/{guardianId}/student-attendance")
    public ResponseEntity<?> getStudentAttendance(@PathVariable Long guardianId) {
        try {
            Map<String, Object> attendance = guardianService.getStudentAttendance(guardianId);
            attendance.put("success", true);
            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get monthly report
     */
    @GetMapping("/{guardianId}/monthly-report")
    public ResponseEntity<?> getMonthlyReport(@PathVariable Long guardianId) {
        try {
            Map<String, Object> report = guardianService.getMonthlyReport(guardianId);
            report.put("success", true);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get dashboard stats
     */
    @GetMapping("/{guardianId}/dashboard")
    public ResponseEntity<?> getDashboard(@PathVariable Long guardianId) {
        try {
            Map<String, Object> stats = guardianService.getDashboardStats(guardianId);
            stats.put("success", true);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get all guardians (admin only)
     */
    @GetMapping
    public ResponseEntity<?> getAllGuardians() {
        try {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "guardians", guardianService.getAllGuardians()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get guardian by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getGuardianById(@PathVariable Long id) {
        try {
            Guardian guardian = guardianService.getGuardianById(id);
            if (guardian == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(Map.of(
                "success", true,
                "guardian", guardian
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Update guardian
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateGuardian(@PathVariable Long id, @RequestBody Guardian guardian) {
        try {
            Guardian updated = guardianService.updateGuardian(id, guardian);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Guardian updated successfully",
                "guardian", updated
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Delete guardian
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGuardian(@PathVariable Long id) {
        try {
            guardianService.deleteGuardian(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Guardian deleted successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}
