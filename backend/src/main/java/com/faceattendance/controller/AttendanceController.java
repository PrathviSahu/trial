package com.faceattendance.controller;

import com.faceattendance.model.Attendance;
import com.faceattendance.model.Student;
import com.faceattendance.model.TimetableSlot;
import com.faceattendance.repository.AttendanceRepository;
import com.faceattendance.repository.StudentRepository;
import com.faceattendance.service.TimetableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/attendance")
@CrossOrigin(origins = "*")
public class AttendanceController {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TimetableService timetableService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> markAttendance(@RequestBody Map<String, Object> request) {
        try {
            Long studentId = Long.parseLong(request.get("studentId").toString());
            Double confidence = request.containsKey("confidence")
                    ? Double.parseDouble(request.get("confidence").toString())
                    : null;
            String method = request.getOrDefault("method", "FACE_RECOGNITION").toString();
            String subject = request.containsKey("subject") && request.get("subject") != null
                    ? request.get("subject").toString()
                    : null;
            Long slotId = request.containsKey("slotId") && request.get("slotId") != null
                    ? Long.parseLong(request.get("slotId").toString())
                    : null;

            Optional<Student> studentOpt = studentRepository.findById(studentId);
            if (!studentOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Student not found");
                return ResponseEntity.status(404).body(response);
            }

            Student student = studentOpt.get();

            // Resolve subject by timetable unless caller explicitly provides it
            if ((subject == null || subject.trim().isEmpty()) && slotId != null) {
                Optional<TimetableSlot> slotOpt = timetableService.getSlotById(slotId);
                if (slotOpt.isPresent()) {
                    TimetableSlot slot = slotOpt.get();
                    subject = slot.getSubjectCode() + " - " + slot.getSubjectName();
                }
            }

            if (subject == null || subject.trim().isEmpty()) {
                List<TimetableSlot> activeSlots = timetableService.getActiveSlotsForStudentAtTime(studentId,
                        LocalDateTime.now());
                if (activeSlots.size() == 1) {
                    TimetableSlot slot = activeSlots.get(0);
                    subject = slot.getSubjectCode() + " - " + slot.getSubjectName();
                } else {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("requiresSubjectSelection", true);
                    response.put("message", activeSlots.isEmpty()
                            ? "No lecture scheduled right now. Please select subject manually."
                            : "Multiple classes match this time. Please select subject manually.");
                    response.put("candidates", activeSlots);
                    return ResponseEntity.ok(response);
                }
            }

            // Prevent duplicate for same student + same subject + same day
            Optional<Attendance> existing = attendanceRepository.findByStudentAndDateAndSubject(studentId,
                    LocalDate.now(), subject);
            if (existing.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("alreadyMarked", true);
                response.put("message", "Attendance already marked for this subject today");
                response.put("data", existing.get());
                return ResponseEntity.ok(response);
            }

            // Create new attendance record
            Attendance attendance = new Attendance(student, confidence, method);
            attendance.setSubject(subject);
            Attendance savedAttendance = attendanceRepository.save(attendance);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Attendance marked successfully");
            response.put("data", savedAttendance);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error marking attendance: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllAttendance() {
        try {
            List<Attendance> attendanceList = attendanceRepository.findAll();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", attendanceList);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error fetching attendance: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<Map<String, Object>> getStudentAttendance(@PathVariable Long studentId) {
        try {
            List<Attendance> attendanceList = attendanceRepository.findByStudentId(studentId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", attendanceList);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error fetching student attendance: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodayAttendance() {
        try {
            // Try to get attendance, return empty list if error
            List<Attendance> todayAttendance = new java.util.ArrayList<>();

            try {
                List<Attendance> allAttendance = attendanceRepository.findAll();
                LocalDate today = LocalDate.now();

                todayAttendance = allAttendance.stream()
                        .filter(a -> a.getTimestamp() != null && a.getTimestamp().toLocalDate().equals(today))
                        .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                        .collect(java.util.stream.Collectors.toList());
            } catch (Exception e) {
                // If database error, return empty list instead of failing
                System.err.println("Error fetching attendance: " + e.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", todayAttendance);
            response.put("count", todayAttendance.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error fetching today's attendance: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/stats/today")
    public ResponseEntity<Map<String, Object>> getTodayStats() {
        try {
            Long presentCount = attendanceRepository.countTodayAttendance();
            Long totalStudents = studentRepository.count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("presentCount", presentCount != null ? presentCount : 0);
            stats.put("totalStudents", totalStudents != null ? totalStudents : 0);
            stats.put("date", LocalDate.now().toString());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error fetching stats: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/subjects")
    public ResponseEntity<Map<String, Object>> getUniqueSubjects() {
        try {
            List<Attendance> allAttendance = attendanceRepository.findAll();

            // Extract unique subjects
            List<String> subjects = allAttendance.stream()
                    .map(Attendance::getSubject)
                    .filter(subject -> subject != null && !subject.trim().isEmpty())
                    .distinct()
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", subjects);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error fetching subjects: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // ── Bulk attendance marking for a whole class/slot ─────────────────────────
    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> markBulkAttendance(@RequestBody Map<String, Object> request) {
        try {
            String subject = request.containsKey("subject") ? request.get("subject").toString() : null;
            String slotId = request.containsKey("slotId") ? request.get("slotId").toString() : null;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> records = (List<Map<String, Object>>) request.get("records");

            if (records == null || records.isEmpty()) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "No records provided");
                return ResponseEntity.badRequest().body(err);
            }

            int saved = 0;
            int skipped = 0;
            LocalDate today = LocalDate.now();

            for (Map<String, Object> rec : records) {
                Long studentId = Long.parseLong(rec.get("studentId").toString());
                String status = rec.getOrDefault("status", "PRESENT").toString();
                String recSubject = rec.containsKey("subject") && rec.get("subject") != null
                        ? rec.get("subject").toString()
                        : subject;

                Optional<Student> studentOpt = studentRepository.findById(studentId);
                if (!studentOpt.isPresent()) {
                    skipped++;
                    continue;
                }

                // Prevent duplicate for same student + subject + day
                Optional<Attendance> existing = attendanceRepository
                        .findByStudentAndDateAndSubject(studentId, today, recSubject);
                if (existing.isPresent()) {
                    // Update existing status instead of creating duplicate
                    Attendance att = existing.get();
                    att.setStatus(status);
                    attendanceRepository.save(att);
                    saved++;
                    continue;
                }

                Attendance att = new Attendance(studentOpt.get(), null, "MANUAL");
                att.setSubject(recSubject);
                att.setClassId(slotId);
                att.setStatus(status);
                attendanceRepository.save(att);
                saved++;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Bulk attendance saved");
            response.put("saved", saved);
            response.put("skipped", skipped);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error saving bulk attendance: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // ── Fetch attendance for a specific timetable slot on a date ───────────────
    @GetMapping("/slot")
    public ResponseEntity<Map<String, Object>> getSlotAttendance(
            @RequestParam String subject,
            @RequestParam(required = false) String date) {
        try {
            LocalDate targetDate = (date != null && !date.isEmpty())
                    ? LocalDate.parse(date)
                    : LocalDate.now();

            List<Attendance> records = attendanceRepository.findBySubjectAndDate(subject, targetDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", records);
            response.put("count", records.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error fetching slot attendance: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
