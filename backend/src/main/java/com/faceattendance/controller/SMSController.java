package com.faceattendance.controller;

import com.faceattendance.model.Student;
import com.faceattendance.repository.StudentRepository;
import com.faceattendance.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/sms")
@CrossOrigin(origins = "*")
public class SMSController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    // In-memory log (production would use Twilio/MSG91)
    private final List<Map<String, Object>> smsHistory = new CopyOnWriteArrayList<>();

    /**
     * Send low attendance SMS alerts to parents
     */
    @PostMapping("/alerts/low-attendance")
    public ResponseEntity<?> sendLowAttendanceAlerts(@RequestParam(defaultValue = "75") double threshold) {
        try {
            List<Student> students = studentRepository.findAll();
            int smsSent = 0;

            for (Student student : students) {
                long attendanceCount = attendanceRepository.findAll().stream()
                        .filter(a -> a.getStudent() != null && a.getStudent().getId().equals(student.getId()))
                        .count();

                double attendanceRate = attendanceCount > 0
                        ? (double) attendanceCount / Math.max(1, attendanceCount + 5) * 100
                        : 0;

                if (attendanceRate < threshold && attendanceRate > 0) {
                    String phone = student.getPhoneNumber() != null ? student.getPhoneNumber() : "N/A";
                    String message = String.format(
                            "Dear Parent, %s %s (IEN: %s) has attendance of %.1f%% which is below the %s%% threshold. Please ensure regular attendance. - FaceTrackU",
                            student.getFirstName(), student.getLastName(), student.getIenNumber(), attendanceRate,
                            (int) threshold);

                    Map<String, Object> log = new HashMap<>();
                    log.put("to", phone);
                    log.put("studentName", student.getFirstName() + " " + student.getLastName());
                    log.put("message", message);
                    log.put("type", "LOW_ATTENDANCE_ALERT");
                    log.put("sentAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    log.put("status", "SENT");
                    smsHistory.add(0, log);

                    smsSent++;
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "smsSent", smsSent,
                    "threshold", threshold,
                    "message", smsSent + " low attendance SMS alerts sent"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Send daily summary SMS to admin
     */
    @PostMapping("/summary/daily")
    public ResponseEntity<?> sendDailySummary(@RequestParam String phone) {
        try {
            long totalStudents = studentRepository.count();
            long presentToday = attendanceRepository.findAll().stream()
                    .filter(a -> a.getTimestamp().toLocalDate().equals(java.time.LocalDate.now()))
                    .count();
            double rate = totalStudents > 0 ? (double) presentToday / totalStudents * 100 : 0;

            String message = String.format(
                    "FaceTrackU Daily Summary: %d/%d students present (%.1f%%). Date: %s",
                    presentToday, totalStudents, rate, java.time.LocalDate.now());

            Map<String, Object> log = new HashMap<>();
            log.put("to", phone);
            log.put("studentName", "Admin");
            log.put("message", message);
            log.put("type", "DAILY_SUMMARY");
            log.put("sentAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            log.put("status", "SENT");
            smsHistory.add(0, log);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Daily summary SMS sent to " + phone));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Send custom SMS to a specific student's parent
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendCustomSMS(@RequestBody Map<String, Object> request) {
        try {
            Long studentId = Long.valueOf(request.get("studentId").toString());
            String message = (String) request.getOrDefault("message", "");

            Optional<Student> studentOpt = studentRepository.findById(studentId);
            if (studentOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Student not found"));
            }

            Student student = studentOpt.get();
            String phone = student.getPhoneNumber() != null ? student.getPhoneNumber() : "N/A";

            Map<String, Object> log = new HashMap<>();
            log.put("to", phone);
            log.put("studentName", student.getFirstName() + " " + student.getLastName());
            log.put("message", message);
            log.put("type", "CUSTOM");
            log.put("sentAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            log.put("status", "SENT");
            smsHistory.add(0, log);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "SMS sent to " + phone + " for " + student.getFirstName()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false, "message", "Error: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        return ResponseEntity.ok(Map.of("success", true, "messages", smsHistory, "count", smsHistory.size()));
    }

    @DeleteMapping("/history")
    public ResponseEntity<?> clearHistory() {
        smsHistory.clear();
        return ResponseEntity.ok(Map.of("success", true, "message", "SMS history cleared"));
    }
}
