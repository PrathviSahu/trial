package com.faceattendance.controller;

import com.faceattendance.model.Attendance;
import com.faceattendance.repository.AttendanceRepository;
import com.faceattendance.repository.StudentRepository;
import com.faceattendance.service.AttendancePredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private static final String[] DEPARTMENT_COLORS = {
            "#3B82F6", "#10B981", "#F59E0B", "#EF4444",
            "#8B5CF6", "#EC4899", "#14B8A6", "#F97316"
    };

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private AttendancePredictionService predictionService;

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        Map<String, Object> response = new HashMap<>();

        try {
            long totalStudents = studentRepository.count();
            long faceEnrolled = studentRepository.countByFaceEnrolledTrue();
            long presentToday = attendanceRepository.countTodayAttendance();

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalStudents", totalStudents);
            stats.put("presentToday", presentToday);
            stats.put("attendanceRate", totalStudents > 0 ? Math.round((presentToday * 100.0) / totalStudents) : 0);
            stats.put("faceEnrolled", faceEnrolled);

            Map<String, Object> alerts = new LinkedHashMap<>();
            alerts.put("pendingEnrollment", Math.max(0, totalStudents - faceEnrolled));
            alerts.put("lowAttendance", countHighRiskStudents());

            response.put("success", true);
            response.put("data", Map.of(
                    "stats", stats,
                    "alerts", alerts,
                    "departmentDistribution", buildDepartmentDistribution(),
                    "attendanceTrend", buildAttendanceTrend()
            ));
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching dashboard summary: " + e.getMessage());
            return response;
        }
    }

    private List<Map<String, Object>> buildDepartmentDistribution() {
        List<Map<String, Object>> stats = new ArrayList<>();
        List<Object[]> departmentCounts = studentRepository.countByDepartment();

        for (int i = 0; i < departmentCounts.size(); i++) {
            Object[] row = departmentCounts.get(i);
            String department = row[0] != null ? row[0].toString() : "Unknown";
            Number total = (Number) row[1];

            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("department", department);
            stat.put("name", department);
            stat.put("value", total.intValue());
            stat.put("totalStudents", total.intValue());
            stat.put("color", DEPARTMENT_COLORS[i % DEPARTMENT_COLORS.length]);
            stats.add(stat);
        }

        return stats;
    }

    private List<Map<String, Object>> buildAttendanceTrend() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay().minusSeconds(1);

        List<Attendance> attendanceList = attendanceRepository.findByTimestampBetween(start, end);
        Map<LocalDate, Integer> byDate = new HashMap<>();

        for (Attendance attendance : attendanceList) {
            if (attendance.getTimestamp() == null) {
                continue;
            }

            LocalDate date = attendance.getTimestamp().toLocalDate();
            byDate.put(date, byDate.getOrDefault(date, 0) + 1);
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", String.format("%02d-%02d", date.getMonthValue(), date.getDayOfMonth()));
            point.put("present", byDate.getOrDefault(date, 0));
            trend.add(point);
        }

        return trend;
    }

    private int countHighRiskStudents() {
        try {
            List<Map<String, Object>> predictions = predictionService.predictAbsentStudents();
            int count = 0;

            for (Map<String, Object> prediction : predictions) {
                Object riskLevel = prediction.get("riskLevel");
                if (riskLevel != null && "high".equalsIgnoreCase(riskLevel.toString())) {
                    count++;
                }
            }

            return count;
        } catch (Exception e) {
            return 0;
        }
    }
}
