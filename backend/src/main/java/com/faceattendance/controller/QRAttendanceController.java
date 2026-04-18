package com.faceattendance.controller;

import com.faceattendance.model.Attendance;
import com.faceattendance.model.Student;
import com.faceattendance.model.QRSession;
import com.faceattendance.repository.AttendanceRepository;
import com.faceattendance.repository.StudentRepository;
import com.faceattendance.repository.QRSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/qr-attendance")
@CrossOrigin(origins = "*")
public class QRAttendanceController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private QRSessionRepository qrSessionRepository;

    /**
     * Generate a QR attendance session
     */
    @PostMapping("/session/generate")
    public ResponseEntity<?> generateSession(@RequestBody Map<String, Object> request) {
        try {
            String subject = (String) request.getOrDefault("subject", "General");
            String department = (String) request.getOrDefault("department", "");
            String faculty = (String) request.getOrDefault("faculty", "System");
            int validMinutes = (int) request.getOrDefault("validMinutes", 15);

            String sessionCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(validMinutes);

            QRSession session = new QRSession(sessionCode, subject, department, faculty, expiresAt);
            qrSessionRepository.save(session);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessionCode", sessionCode);
            response.put("subject", subject);
            response.put("department", department);
            response.put("faculty", faculty);
            response.put("expiresAt", expiresAt.toString());
            response.put("validMinutes", validMinutes);
            response.put("qrData", "FACETRACK_QR:" + sessionCode);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error generating QR session: " + e.getMessage()));
        }
    }

    /**
     * Rotate QR code — invalidate old code and generate a new one (anti-fraud)
     */
    @PostMapping("/session/rotate")
    public ResponseEntity<?> rotateSession(@RequestBody Map<String, Object> request) {
        try {
            String oldCode = (String) request.get("sessionCode");
            Optional<QRSession> oldSessionOpt = qrSessionRepository.findBySessionCode(oldCode);

            if (oldSessionOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false, "message", "Session not found"));
            }

            QRSession oldSession = oldSessionOpt.get();

            // Generate new code
            String newCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            // Create new session preserving marked students
            QRSession newSession = new QRSession(newCode, oldSession.getSubject(), oldSession.getDepartment(), oldSession.getFaculty(),
                    oldSession.getExpiresAt());
            newSession.setMarkedStudents(new HashSet<>(oldSession.getMarkedStudents())); // carry over

            // Swap: remove old, add new
            qrSessionRepository.delete(oldSession);
            qrSessionRepository.save(newSession);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessionCode", newCode);
            response.put("subject", newSession.getSubject());
            response.put("expiresAt", newSession.getExpiresAt().toString());
            response.put("markedCount", newSession.getMarkedStudents().size());
            response.put("qrData", "FACETRACK_QR:" + newCode);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false, "message", "Error rotating: " + e.getMessage()));
        }
    }

    /**
     * Mark attendance via QR code scan
     */
    @PostMapping("/mark")
    public ResponseEntity<?> markAttendance(@RequestBody Map<String, Object> request) {
        try {
            String sessionCode = (String) request.get("sessionCode");
            Long studentId = Long.valueOf(request.get("studentId").toString());

            if (sessionCode == null || sessionCode.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false, "message", "Session code is required"));
            }

            Optional<QRSession> sessionOpt = qrSessionRepository.findBySessionCode(sessionCode);
            if (sessionOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false, "message", "Invalid or expired QR code"));
            }

            QRSession session = sessionOpt.get();

            if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
                qrSessionRepository.delete(session);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false, "message", "QR code has expired"));
            }

            Optional<Student> studentOpt = studentRepository.findById(studentId);
            if (studentOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false, "message", "Student not found"));
            }

            // Check if already marked in this session
            if (session.getMarkedStudents().contains(studentId)) {
                return ResponseEntity.ok(Map.of(
                        "success", true, "message", "Attendance already marked for this session",
                        "alreadyMarked", true));
            }

            Student student = studentOpt.get();
            Attendance attendance = new Attendance();
            attendance.setStudent(student);
            attendance.setConfidence(1.0);
            attendance.setMethod("QR_CODE");
            attendance.setSubject(session.getSubject());
            attendance.setMarkedBy(session.getFaculty());
            attendance.setStatus("PRESENT");
            attendance.setTimestamp(LocalDateTime.now());
            attendanceRepository.save(attendance);

            session.getMarkedStudents().add(studentId);
            qrSessionRepository.save(session); // Update marked students

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Attendance marked successfully via QR code");
            response.put("studentName", student.getFirstName() + " " + student.getLastName());
            response.put("subject", session.getSubject());
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error marking attendance: " + e.getMessage()));
        }
    }

    /**
     * Get active session info
     */
    @GetMapping("/session/{code}")
    public ResponseEntity<?> getSession(@PathVariable String code) {
        Optional<QRSession> sessionOpt = qrSessionRepository.findBySessionCode(code);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Session not found"));
        }
        
        QRSession session = sessionOpt.get();
        if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            qrSessionRepository.delete(session);
            return ResponseEntity.ok(Map.of("success", false, "message", "Session expired"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("sessionCode", session.getSessionCode());
        response.put("subject", session.getSubject());
        response.put("department", session.getDepartment());
        response.put("faculty", session.getFaculty());
        response.put("expiresAt", session.getExpiresAt().toString());
        response.put("markedCount", session.getMarkedStudents().size());
        response.put("markedStudentIds", session.getMarkedStudents());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all active sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getAllSessions() {
        List<QRSession> allSessions = qrSessionRepository.findAll();
        List<Map<String, Object>> activeSessionsList = new ArrayList<>();
        
        for (QRSession session : allSessions) {
            if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
                qrSessionRepository.delete(session); // Clean up expired
            } else {
                Map<String, Object> s = new HashMap<>();
                s.put("sessionCode", session.getSessionCode());
                s.put("subject", session.getSubject());
                s.put("department", session.getDepartment());
                s.put("faculty", session.getFaculty());
                s.put("expiresAt", session.getExpiresAt().toString());
                s.put("markedCount", session.getMarkedStudents().size());
                activeSessionsList.add(s);
            }
        }

        return ResponseEntity.ok(Map.of("success", true, "sessions", activeSessionsList));
    }
}
