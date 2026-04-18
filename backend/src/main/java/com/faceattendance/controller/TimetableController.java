package com.faceattendance.controller;

import com.faceattendance.model.TimetableSlot;
import com.faceattendance.service.TimetableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/timetable")
@CrossOrigin(origins = "*")
public class TimetableController {

    @Autowired
    private TimetableService timetableService;

    @GetMapping("/schedule/weekly")
    public ResponseEntity<Map<String, Object>> getWeeklySchedule(
            @RequestParam String department,
            @RequestParam Integer year,
            @RequestParam Integer semester,
            @RequestParam String section) {
        try {
            List<TimetableSlot> slots = timetableService.getWeeklySchedule(department, year, semester, section);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", slots);
            response.put("count", slots.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching timetable: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/current-class")
    public ResponseEntity<Map<String, Object>> getCurrentClass(
            @RequestParam Long studentId,
            @RequestParam(required = false) String at) {
        try {
            LocalDateTime atTime = null;
            if (at != null && !at.trim().isEmpty()) {
                atTime = LocalDateTime.parse(at);
            }

            List<TimetableSlot> activeSlots = (atTime == null)
                    ? timetableService.getActiveSlotsForStudent(studentId)
                    : timetableService.getActiveSlotsForStudentAtTime(studentId, atTime);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", activeSlots);
            response.put("count", activeSlots.size());
            response.put("requiresSelection", activeSlots.size() != 1);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error resolving current class: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSlot(@RequestBody TimetableSlot slot) {
        try {
            TimetableSlot saved = timetableService.saveSlot(slot);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Timetable slot created");
            response.put("data", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error creating slot: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> createSlotsBulk(@RequestBody List<TimetableSlot> slots) {
        try {
            int created = 0;
            for (TimetableSlot slot : slots) {
                timetableService.saveSlot(slot);
                created++;
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Timetable slots created");
            response.put("count", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error creating slots: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/{slotId}")
    public ResponseEntity<Map<String, Object>> deleteSlot(@PathVariable Long slotId) {
        try {
            timetableService.deleteSlot(slotId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Timetable slot deleted");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error deleting slot: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
