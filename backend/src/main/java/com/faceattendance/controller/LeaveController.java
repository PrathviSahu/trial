package com.faceattendance.controller;

import com.faceattendance.model.LeaveRequest;
import com.faceattendance.model.Student;
import com.faceattendance.repository.LeaveRepository;
import com.faceattendance.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/leave")
@CrossOrigin(origins = "*")
public class LeaveController {

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private StudentRepository studentRepository;

    /**
     * Submit a new leave request
     */
    @PostMapping("/apply")
    public ResponseEntity<?> applyLeave(@RequestBody Map<String, Object> request) {
        try {
            Long studentId = Long.valueOf(request.get("studentId").toString());
            String leaveType = (String) request.getOrDefault("leaveType", "OTHER");
            String startDate = (String) request.get("startDate");
            String endDate = (String) request.get("endDate");
            String reason = (String) request.getOrDefault("reason", "");

            Optional<Student> studentOpt = studentRepository.findById(studentId);
            if (studentOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Student not found"));
            }

            LeaveRequest leave = new LeaveRequest();
            leave.setStudent(studentOpt.get());
            leave.setLeaveType(leaveType);
            leave.setStartDate(LocalDate.parse(startDate));
            leave.setEndDate(LocalDate.parse(endDate));
            leave.setReason(reason);
            leave.setStatus("PENDING");

            leaveRepository.save(leave);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Leave request submitted successfully");
            response.put("leaveId", leave.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Get all leave requests
     */
    @GetMapping
    public ResponseEntity<?> getAllLeaves(@RequestParam(required = false) String status) {
        try {
            List<LeaveRequest> leaves;
            if (status != null && !status.isEmpty()) {
                leaves = leaveRepository.findByStatusOrderByCreatedAtDesc(status);
            } else {
                leaves = leaveRepository.findAllByOrderByCreatedAtDesc();
            }

            List<Map<String, Object>> result = leaves.stream().map(this::mapLeave).collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("success", true, "data", result, "total", result.size()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Get leave requests for a specific student
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentLeaves(@PathVariable Long studentId) {
        try {
            List<LeaveRequest> leaves = leaveRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
            List<Map<String, Object>> result = leaves.stream().map(this::mapLeave).collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Approve or reject a leave request
     */
    @PutMapping("/{leaveId}/review")
    public ResponseEntity<?> reviewLeave(
            @PathVariable Long leaveId,
            @RequestBody Map<String, Object> request) {
        try {
            String action = (String) request.get("action"); // "APPROVED" or "REJECTED"
            String remarks = (String) request.getOrDefault("remarks", "");
            String approvedBy = (String) request.getOrDefault("approvedBy", "Admin");

            Optional<LeaveRequest> leaveOpt = leaveRepository.findById(leaveId);
            if (leaveOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Leave request not found"));
            }

            LeaveRequest leave = leaveOpt.get();
            leave.setStatus(action);
            leave.setAdminRemarks(remarks);
            leave.setApprovedBy(approvedBy);
            leaveRepository.save(leave);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Leave request " + action.toLowerCase()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Delete a leave request
     */
    @DeleteMapping("/{leaveId}")
    public ResponseEntity<?> deleteLeave(@PathVariable Long leaveId) {
        try {
            leaveRepository.deleteById(leaveId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Leave request deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false, "message", "Error: " + e.getMessage()));
        }
    }

    private Map<String, Object> mapLeave(LeaveRequest leave) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", leave.getId());
        map.put("studentId", leave.getStudent().getId());
        map.put("studentName", leave.getStudent().getFirstName() + " " + leave.getStudent().getLastName());
        map.put("ienNumber", leave.getStudent().getIenNumber());
        map.put("department", leave.getStudent().getDepartment());
        map.put("leaveType", leave.getLeaveType());
        map.put("startDate", leave.getStartDate().toString());
        map.put("endDate", leave.getEndDate().toString());
        map.put("reason", leave.getReason());
        map.put("status", leave.getStatus());
        map.put("approvedBy", leave.getApprovedBy());
        map.put("adminRemarks", leave.getAdminRemarks());
        map.put("createdAt", leave.getCreatedAt() != null ? leave.getCreatedAt().toString() : null);
        return map;
    }
}
