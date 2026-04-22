package com.faceattendance.controller;

import com.faceattendance.model.Student;
import com.faceattendance.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/students")
@CrossOrigin(origins = "*")
public class StudentController {
    
    @Autowired
    private StudentRepository studentRepository;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Map<String, Object>> studentPage = studentRepository.findAll(pageable)
                    .map(this::toStudentSummary);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", studentPage);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error fetching students: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getStudentById(@PathVariable Long id) {
        return studentRepository.findById(id)
                .map(student -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", student);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Student not found");
                    return ResponseEntity.status(404).body(response);
                });
    }
    
    @GetMapping("/enrolled-faces")
    public ResponseEntity<Map<String, Object>> getEnrolledStudents() {
        try {
            List<Student> enrolledStudents = studentRepository.findStudentsWithFaceData();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", enrolledStudents);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error fetching enrolled students: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getStudentSummaries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return getAllStudents(page, size);
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createStudent(@RequestBody Student student) {
        try {
            Student savedStudent = studentRepository.save(student);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", savedStudent);
            response.put("message", "Student created successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error creating student: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @PostMapping("/{id}/face-enrollment")
    public ResponseEntity<Map<String, Object>> enrollFace(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        
        try {
            return studentRepository.findById(id)
                    .map(student -> {
                        String faceDescriptor = request.get("faceDescriptor");
                        student.setFaceDescriptor(faceDescriptor);
                        student.setFaceEnrolled(true);
                        studentRepository.save(student);
                        
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("message", "Face enrolled successfully");
                        response.put("data", student);
                        
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "Student not found");
                        return ResponseEntity.status(404).body(response);
                    });
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error enrolling face: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @DeleteMapping("/{id}/remove-face")
    public ResponseEntity<Map<String, Object>> removeFaceData(@PathVariable Long id) {
        try {
            return studentRepository.findById(id)
                    .map(student -> {
                        student.setFaceDescriptor(null);
                        student.setFaceEnrolled(false);
                        studentRepository.save(student);
                        
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("message", "Face data removed successfully");
                        response.put("data", student);
                        
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "Student not found");
                        return ResponseEntity.status(404).body(response);
                    });
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error removing face data: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateStudent(
            @PathVariable Long id,
            @RequestBody Student studentDetails) {
        
        try {
            return studentRepository.findById(id)
                    .map(student -> {
                        student.setFirstName(studentDetails.getFirstName());
                        student.setLastName(studentDetails.getLastName());
                        student.setEmail(studentDetails.getEmail());
                        student.setPhoneNumber(studentDetails.getPhoneNumber());
                        student.setDepartment(studentDetails.getDepartment());
                        student.setBranch(studentDetails.getBranch());
                        student.setYear(studentDetails.getYear());
                        student.setSemester(studentDetails.getSemester());
                        
                        Student updatedStudent = studentRepository.save(student);
                        
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("data", updatedStudent);
                        response.put("message", "Student updated successfully");
                        
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "Student not found");
                        return ResponseEntity.status(404).body(response);
                    });
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error updating student: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteStudent(@PathVariable Long id) {
        try {
            return studentRepository.findById(id)
                    .map(student -> {
                        studentRepository.delete(student);
                        
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("message", "Student deleted successfully");
                        
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "Student not found");
                        return ResponseEntity.status(404).body(response);
                    });
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error deleting student: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @GetMapping("/stats/department")
    public ResponseEntity<Map<String, Object>> getDepartmentStats() {
        try {
            String[] colors = {"#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6", "#EC4899", "#14B8A6", "#F97316"};
            List<Object[]> departmentCounts = studentRepository.countByDepartment();

            List<Map<String, Object>> stats = departmentCounts.stream()
                    .map(row -> {
                        Map<String, Object> stat = new LinkedHashMap<>();
                        stat.put("department", row[0]);
                        stat.put("name", row[0]);
                        stat.put("value", ((Number) row[1]).intValue());
                        stat.put("totalStudents", ((Number) row[1]).intValue());
                        return stat;
                    })
                    .collect(Collectors.toList());

            for (int i = 0; i < stats.size(); i++) {
                Map<String, Object> stat = stats.get(i);
                stat.put("color", colors[i % colors.length]);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error fetching department statistics: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    private Map<String, Object> toStudentSummary(Student student) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", student.getId());
        summary.put("ienNumber", student.getIenNumber());
        summary.put("rollNumber", student.getRollNumber());
        summary.put("firstName", student.getFirstName());
        summary.put("lastName", student.getLastName());
        summary.put("email", student.getEmail());
        summary.put("phoneNumber", student.getPhoneNumber());
        summary.put("department", student.getDepartment());
        summary.put("branch", student.getBranch());
        summary.put("year", student.getYear());
        summary.put("semester", student.getSemester());
        summary.put("isActive", student.getIsActive());
        summary.put("faceEnrolled", student.getFaceEnrolled());
        summary.put("createdAt", student.getCreatedAt());
        summary.put("updatedAt", student.getUpdatedAt());
        return summary;
    }
}
