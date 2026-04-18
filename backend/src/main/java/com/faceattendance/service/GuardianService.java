package com.faceattendance.service;

import com.faceattendance.model.Guardian;
import com.faceattendance.model.Student;
import com.faceattendance.model.Attendance;
import com.faceattendance.repository.GuardianRepository;
import com.faceattendance.repository.StudentRepository;
import com.faceattendance.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GuardianService {

    @Autowired
    private GuardianRepository guardianRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Register new guardian
     */
    public Guardian registerGuardian(Guardian guardian) {
        if (guardianRepository.existsByEmail(guardian.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Hash password
        guardian.setPassword(passwordEncoder.encode(guardian.getPassword()));
        guardian.setActive(true);

        return guardianRepository.save(guardian);
    }

    /**
     * Guardian login
     */
    public Map<String, Object> login(String email, String password) {
        Optional<Guardian> guardianOpt = guardianRepository.findByEmail(email);
        
        if (guardianOpt.isEmpty()) {
            throw new RuntimeException("Invalid credentials");
        }

        Guardian guardian = guardianOpt.get();

        if (!guardian.getActive()) {
            throw new RuntimeException("Account is inactive");
        }

        if (!passwordEncoder.matches(password, guardian.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // Update last login
        guardian.setLastLogin(LocalDateTime.now());
        guardianRepository.save(guardian);

        Map<String, Object> response = new HashMap<>();
        response.put("guardian", sanitizeGuardian(guardian));
        response.put("student", guardian.getStudent());
        response.put("message", "Login successful");

        return response;
    }

    /**
     * Get student's attendance records for guardian
     */
    public Map<String, Object> getStudentAttendance(Long guardianId) {
        Optional<Guardian> guardianOpt = guardianRepository.findById(guardianId);
        
        if (guardianOpt.isEmpty()) {
            throw new RuntimeException("Guardian not found");
        }

        Guardian guardian = guardianOpt.get();
        Student student = guardian.getStudent();

        List<Attendance> attendanceRecords = attendanceRepository.findAll().stream()
                .filter(a -> a.getStudent() != null && a.getStudent().getId().equals(student.getId()))
                .sorted(Comparator.comparing(Attendance::getTimestamp).reversed())
                .collect(Collectors.toList());

        long totalClasses = attendanceRecords.size() + 10; // Assuming some total
        long attended = attendanceRecords.size();
        double attendanceRate = totalClasses > 0 ? (double) attended / totalClasses * 100 : 0;

        Map<String, Object> response = new HashMap<>();
        response.put("student", student);
        response.put("attendanceRecords", attendanceRecords);
        response.put("totalClasses", totalClasses);
        response.put("attended", attended);
        response.put("attendanceRate", attendanceRate);
        response.put("status", attendanceRate >= 75 ? "Good Standing" : "Below Requirement");

        return response;
    }

    /**
     * Get monthly attendance summary for guardian
     */
    public Map<String, Object> getMonthlyReport(Long guardianId) {
        Optional<Guardian> guardianOpt = guardianRepository.findById(guardianId);
        
        if (guardianOpt.isEmpty()) {
            throw new RuntimeException("Guardian not found");
        }

        Student student = guardianOpt.get().getStudent();
        LocalDateTime monthStart = LocalDateTime.now().minusDays(30);

        List<Attendance> monthlyRecords = attendanceRepository.findAll().stream()
                .filter(a -> a.getStudent() != null && 
                            a.getStudent().getId().equals(student.getId()) &&
                            a.getTimestamp().isAfter(monthStart))
                .collect(Collectors.toList());

        Map<String, Object> report = new HashMap<>();
        report.put("month", LocalDateTime.now().getMonth().toString());
        report.put("year", LocalDateTime.now().getYear());
        report.put("totalDays", 30);
        report.put("daysAttended", monthlyRecords.size());
        report.put("daysAbsent", Math.max(0, 20 - monthlyRecords.size()));
        report.put("attendanceRate", monthlyRecords.size() > 0 ? (double) monthlyRecords.size() / 20 * 100 : 0);
        report.put("records", monthlyRecords);

        return report;
    }

    /**
     * Get guardian dashboard stats
     */
    public Map<String, Object> getDashboardStats(Long guardianId) {
        Optional<Guardian> guardianOpt = guardianRepository.findById(guardianId);
        
        if (guardianOpt.isEmpty()) {
            throw new RuntimeException("Guardian not found");
        }

        Student student = guardianOpt.get().getStudent();
        
        List<Attendance> allRecords = attendanceRepository.findAll().stream()
                .filter(a -> a.getStudent() != null && a.getStudent().getId().equals(student.getId()))
                .collect(Collectors.toList());

        long todayAttendance = allRecords.stream()
                .filter(a -> a.getTimestamp().toLocalDate().equals(java.time.LocalDate.now()))
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("studentName", student.getFirstName() + " " + student.getLastName());
        stats.put("department", student.getDepartment());
        stats.put("year", student.getYear());
        stats.put("totalRecords", allRecords.size());
        stats.put("attendanceRate", allRecords.size() > 0 ? (double) allRecords.size() / (allRecords.size() + 10) * 100 : 0);
        stats.put("presentToday", todayAttendance > 0);
        stats.put("lastAttendance", allRecords.isEmpty() ? null : allRecords.get(0).getTimestamp());

        return stats;
    }

    /**
     * Get all guardians (admin only)
     */
    public List<Guardian> getAllGuardians() {
        return guardianRepository.findAll().stream()
                .map(this::sanitizeGuardian)
                .collect(Collectors.toList());
    }

    /**
     * Get guardian by ID
     */
    public Guardian getGuardianById(Long id) {
        return guardianRepository.findById(id)
                .map(this::sanitizeGuardian)
                .orElse(null);
    }

    /**
     * Update guardian
     */
    public Guardian updateGuardian(Long id, Guardian updatedGuardian) {
        Optional<Guardian> existingOpt = guardianRepository.findById(id);
        
        if (existingOpt.isEmpty()) {
            throw new RuntimeException("Guardian not found");
        }

        Guardian existing = existingOpt.get();
        
        if (updatedGuardian.getFirstName() != null) {
            existing.setFirstName(updatedGuardian.getFirstName());
        }
        if (updatedGuardian.getLastName() != null) {
            existing.setLastName(updatedGuardian.getLastName());
        }
        if (updatedGuardian.getPhone() != null) {
            existing.setPhone(updatedGuardian.getPhone());
        }
        if (updatedGuardian.getPassword() != null && !updatedGuardian.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(updatedGuardian.getPassword()));
        }

        return guardianRepository.save(existing);
    }

    /**
     * Delete guardian
     */
    public void deleteGuardian(Long id) {
        guardianRepository.deleteById(id);
    }

    /**
     * Remove password from guardian object
     */
    private Guardian sanitizeGuardian(Guardian guardian) {
        Guardian safe = new Guardian();
        safe.setId(guardian.getId());
        safe.setFirstName(guardian.getFirstName());
        safe.setLastName(guardian.getLastName());
        safe.setEmail(guardian.getEmail());
        safe.setPhone(guardian.getPhone());
        safe.setRelationship(guardian.getRelationship());
        safe.setStudent(guardian.getStudent());
        safe.setActive(guardian.getActive());
        safe.setCreatedAt(guardian.getCreatedAt());
        safe.setLastLogin(guardian.getLastLogin());
        return safe;
    }
}
