package com.faceattendance.controller;

import com.faceattendance.dto.StudentDetailDto;
import com.faceattendance.dto.StudentFaceDto;
import com.faceattendance.dto.StudentSummaryDto;
import com.faceattendance.model.Student;
import com.faceattendance.repository.StudentRepository;
import com.faceattendance.repository.spec.StudentSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Boolean faceEnrolled,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<StudentSummaryDto> studentPage = studentRepository
                    .findAll(StudentSpecifications.build(query, department, year, faceEnrolled, isActive), pageable)
                    .map(this::toStudentSummaryDto);
            
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
                    response.put("data", toStudentDetailDto(student));
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

            List<StudentFaceDto> dtoList = enrolledStudents.stream()
                    .map(this::toStudentFaceDto)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dtoList);
            
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
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Boolean faceEnrolled,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return getAllStudents(page, size, query, department, year, faceEnrolled, isActive, sortBy, sortDir);
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createStudent(@RequestBody Student student) {
        try {
            Student savedStudent = studentRepository.save(student);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", toStudentDetailDto(savedStudent));
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
                        response.put("data", toStudentDetailDto(student));
                        
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
                        response.put("data", toStudentDetailDto(student));
                        
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
                        response.put("data", toStudentDetailDto(updatedStudent));
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

    private StudentSummaryDto toStudentSummaryDto(Student student) {
        return new StudentSummaryDto(
                student.getId(),
                student.getIenNumber(),
                student.getRollNumber(),
                student.getFirstName(),
                student.getLastName(),
                student.getEmail(),
                student.getPhoneNumber(),
                student.getDepartment(),
                student.getBranch(),
                student.getYear(),
                student.getSemester(),
                student.getIsActive(),
                student.getFaceEnrolled(),
                student.getCreatedAt(),
                student.getUpdatedAt()
        );
    }

    private StudentDetailDto toStudentDetailDto(Student student) {
        return new StudentDetailDto(
                student.getId(),
                student.getIenNumber(),
                student.getRollNumber(),
                student.getFirstName(),
                student.getLastName(),
                student.getEmail(),
                student.getPhoneNumber(),
                student.getDepartment(),
                student.getBranch(),
                student.getYear(),
                student.getSemester(),
                student.getIsActive(),
                student.getFaceEnrolled(),
                student.getFaceDescriptor(),
                student.getCreatedAt(),
                student.getUpdatedAt()
        );
    }

    private StudentFaceDto toStudentFaceDto(Student student) {
        return new StudentFaceDto(
                student.getId(),
                student.getIenNumber(),
                student.getRollNumber(),
                student.getFirstName(),
                student.getLastName(),
                student.getEmail(),
                student.getDepartment(),
                student.getBranch(),
                student.getYear(),
                student.getSemester(),
                student.getFaceEnrolled(),
                student.getFaceDescriptor()
        );
    }
}
