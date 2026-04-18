package com.faceattendance.service;

import com.faceattendance.model.Attendance;
import com.faceattendance.model.Student;
import com.faceattendance.repository.AttendanceRepository;
import com.faceattendance.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendancePredictionService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StudentRepository studentRepository;

    /**
     * Predict students who are likely to be absent
     */
    public List<Map<String, Object>> predictAbsentStudents() {
        List<Student> allStudents = studentRepository.findAll();
        List<Attendance> allAttendance = attendanceRepository.findAll();
        List<Map<String, Object>> predictions = new ArrayList<>();

        for (Student student : allStudents) {
            // Get student's attendance history
            List<Attendance> studentAttendance = allAttendance.stream()
                    .filter(a -> a.getStudent() != null && a.getStudent().getId().equals(student.getId()))
                    .collect(Collectors.toList());

            if (studentAttendance.isEmpty()) {
                continue;
            }

            // Calculate metrics
            double attendanceRate = calculateAttendanceRate(student.getId(), allAttendance);
            double recentTrend = calculateRecentTrend(studentAttendance);
            int consecutiveAbsences = calculateConsecutiveAbsences(student.getId(), allAttendance);
            String dayOfWeekPattern = analyzeDayOfWeekPattern(studentAttendance);

            // Prediction logic
            double absentProbability = calculateAbsentProbability(attendanceRate, recentTrend, consecutiveAbsences);

            if (absentProbability > 0.5) { // High risk threshold
                Map<String, Object> prediction = new HashMap<>();
                prediction.put("studentId", student.getId());
                prediction.put("studentName", student.getFirstName() + " " + student.getLastName());
                prediction.put("ienNumber", student.getIenNumber());
                prediction.put("department", student.getDepartment());
                prediction.put("absentProbability", Math.round(absentProbability * 100));
                prediction.put("attendanceRate", Math.round(attendanceRate * 100));
                prediction.put("trend", recentTrend > 0 ? "improving" : recentTrend < 0 ? "declining" : "stable");
                prediction.put("riskLevel", absentProbability > 0.8 ? "high" : absentProbability > 0.65 ? "medium" : "low");
                prediction.put("consecutiveAbsences", consecutiveAbsences);
                prediction.put("weakDay", dayOfWeekPattern);
                prediction.put("recommendation", generateRecommendation(absentProbability, recentTrend, consecutiveAbsences));
                
                predictions.add(prediction);
            }
        }

        // Sort by probability descending
        predictions.sort((a, b) -> Long.compare(
                ((Number) b.get("absentProbability")).longValue(), 
                ((Number) a.get("absentProbability")).longValue()
        ));

        return predictions;
    }

    /**
     * Predict best days for important lectures based on attendance patterns
     */
    public Map<String, Object> predictBestDaysForLectures() {
        List<Attendance> allAttendance = attendanceRepository.findAll();
        Map<DayOfWeek, List<Attendance>> attendanceByDay = new HashMap<>();

        // Group attendance by day of week
        for (Attendance attendance : allAttendance) {
            DayOfWeek day = attendance.getTimestamp().getDayOfWeek();
            attendanceByDay.computeIfAbsent(day, k -> new ArrayList<>()).add(attendance);
        }

        // Calculate average attendance for each day
        List<Map<String, Object>> dayStats = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            List<Attendance> dayAttendance = attendanceByDay.getOrDefault(day, new ArrayList<>());
            
            Map<String, Object> dayStat = new HashMap<>();
            dayStat.put("day", day.toString());
            dayStat.put("averageAttendance", dayAttendance.size());
            dayStat.put("totalRecords", dayAttendance.size());
            dayStat.put("percentage", calculateDayAttendancePercentage(day, allAttendance));
            
            dayStats.add(dayStat);
        }

        // Sort by percentage descending
        dayStats.sort((a, b) -> Double.compare(
                (Double) b.get("percentage"), 
                (Double) a.get("percentage")
        ));

        Map<String, Object> result = new HashMap<>();
        result.put("bestDays", dayStats.subList(0, Math.min(3, dayStats.size())));
        result.put("worstDays", dayStats.subList(Math.max(0, dayStats.size() - 3), dayStats.size()));
        result.put("allDays", dayStats);
        result.put("recommendation", "Schedule important lectures on " + 
                ((Map<String, Object>) dayStats.get(0)).get("day"));

        return result;
    }

    /**
     * Generate attendance trend forecast
     */
    public Map<String, Object> forecastAttendanceTrend() {
        List<Attendance> allAttendance = attendanceRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        
        // Get last 30 days of attendance
        List<Attendance> recentAttendance = allAttendance.stream()
                .filter(a -> ChronoUnit.DAYS.between(a.getTimestamp(), now) <= 30)
                .collect(Collectors.toList());

        // Calculate weekly averages
        List<Map<String, Object>> weeklyData = new ArrayList<>();
        for (int week = 0; week < 4; week++) {
            int weekStart = week * 7;
            int weekEnd = weekStart + 7;
            
            int finalWeekStart = weekStart;
            int finalWeekEnd = weekEnd;
            long weekAttendance = recentAttendance.stream()
                    .filter(a -> {
                        long days = ChronoUnit.DAYS.between(a.getTimestamp(), now);
                        return days >= finalWeekStart && days < finalWeekEnd;
                    })
                    .count();

            Map<String, Object> weekData = new HashMap<>();
            weekData.put("week", "Week " + (week + 1));
            weekData.put("attendance", weekAttendance);
            weekData.put("weekNumber", week + 1);
            weeklyData.add(weekData);
        }

        // Calculate trend
        double trend = calculateOverallTrend(weeklyData);
        
        // Forecast next week
        long lastWeekAttendance = weeklyData.isEmpty() ? 0 : 
                ((Number) weeklyData.get(weeklyData.size() - 1).get("attendance")).longValue();
        long forecastedAttendance = Math.max(0, Math.round(lastWeekAttendance * (1 + trend)));

        Map<String, Object> forecast = new HashMap<>();
        forecast.put("historicalData", weeklyData);
        forecast.put("trend", trend > 0 ? "increasing" : trend < 0 ? "decreasing" : "stable");
        forecast.put("trendPercentage", Math.round(trend * 100));
        forecast.put("forecastedNextWeek", forecastedAttendance);
        forecast.put("confidence", calculateForecastConfidence(weeklyData));
        forecast.put("insights", generateTrendInsights(trend, weeklyData));

        return forecast;
    }

    /**
     * Get comprehensive attendance insights
     */
    public Map<String, Object> getAttendanceInsights() {
        List<Attendance> allAttendance = attendanceRepository.findAll();
        List<Student> allStudents = studentRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        Map<String, Object> insights = new HashMap<>();

        // Overall statistics
        long todayAttendance = allAttendance.stream()
                .filter(a -> a.getTimestamp().toLocalDate().equals(now.toLocalDate()))
                .count();
        
        double overallRate = allStudents.isEmpty() ? 0 : 
                (double) todayAttendance / allStudents.size() * 100;

        insights.put("overallAttendanceRate", Math.round(overallRate));
        insights.put("totalStudents", allStudents.size());
        insights.put("presentToday", todayAttendance);
        insights.put("absentToday", allStudents.size() - todayAttendance);

        // Risk analysis
        long highRiskStudents = allStudents.stream()
                .filter(s -> calculateAttendanceRate(s.getId(), allAttendance) < 0.75)
                .count();
        
        insights.put("highRiskStudents", highRiskStudents);
        insights.put("lowRiskStudents", allStudents.size() - highRiskStudents);

        // Department analysis
        Map<String, Long> deptAttendance = allStudents.stream()
                .collect(Collectors.groupingBy(
                        Student::getDepartment,
                        Collectors.counting()
                ));
        insights.put("departmentDistribution", deptAttendance);

        // Generated insights
        List<String> autoInsights = new ArrayList<>();
        if (overallRate < 75) {
            autoInsights.add("⚠️ Overall attendance is below 75% threshold");
        }
        if (highRiskStudents > 5) {
            autoInsights.add("📊 " + highRiskStudents + " students need immediate attention");
        }
        if (overallRate > 90) {
            autoInsights.add("✅ Excellent attendance! Keep up the good work");
        }
        
        insights.put("aiInsights", autoInsights);
        insights.put("generatedAt", now);

        return insights;
    }

    // Helper methods
    private double calculateAttendanceRate(Long studentId, List<Attendance> allAttendance) {
        long count = allAttendance.stream()
                .filter(a -> a.getStudent() != null && a.getStudent().getId().equals(studentId))
                .count();
        return count > 0 ? (double) count / 50 : 0; // Assume 50 total classes
    }

    private double calculateRecentTrend(List<Attendance> studentAttendance) {
        if (studentAttendance.size() < 2) return 0;
        
        // Sort by timestamp
        studentAttendance.sort(Comparator.comparing(Attendance::getTimestamp));
        
        // Compare recent half vs older half
        int midPoint = studentAttendance.size() / 2;
        int recentCount = studentAttendance.size() - midPoint;
        int olderCount = midPoint;
        
        return (double) (recentCount - olderCount) / studentAttendance.size();
    }

    private int calculateConsecutiveAbsences(Long studentId, List<Attendance> allAttendance) {
        // This is simplified - in real scenario, track all class days
        LocalDateTime now = LocalDateTime.now();
        long recentAttendance = allAttendance.stream()
                .filter(a -> a.getStudent() != null && a.getStudent().getId().equals(studentId))
                .filter(a -> ChronoUnit.DAYS.between(a.getTimestamp(), now) <= 7)
                .count();
        
        return (int) (7 - recentAttendance);
    }

    private String analyzeDayOfWeekPattern(List<Attendance> studentAttendance) {
        Map<DayOfWeek, Long> dayCount = studentAttendance.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getTimestamp().getDayOfWeek(),
                        Collectors.counting()
                ));
        
        return dayCount.isEmpty() ? "N/A" : 
                Collections.min(dayCount.entrySet(), Map.Entry.comparingByValue())
                        .getKey().toString();
    }

    private double calculateAbsentProbability(double attendanceRate, double trend, int consecutiveAbsences) {
        double baseProbability = 1 - attendanceRate;
        double trendFactor = trend < 0 ? 0.2 : 0;
        double absenceFactor = consecutiveAbsences * 0.1;
        
        return Math.min(1.0, baseProbability + trendFactor + absenceFactor);
    }

    private String generateRecommendation(double probability, double trend, int absences) {
        if (probability > 0.8) {
            return "Immediate intervention required. Contact student urgently.";
        } else if (probability > 0.65) {
            return "Monitor closely. Consider counseling session.";
        } else {
            return "Low risk. Continue regular monitoring.";
        }
    }

    private double calculateDayAttendancePercentage(DayOfWeek day, List<Attendance> allAttendance) {
        long dayCount = allAttendance.stream()
                .filter(a -> a.getTimestamp().getDayOfWeek() == day)
                .count();
        
        return allAttendance.isEmpty() ? 0 : (double) dayCount / allAttendance.size() * 100;
    }

    private double calculateOverallTrend(List<Map<String, Object>> weeklyData) {
        if (weeklyData.size() < 2) return 0;
        
        long firstWeek = ((Number) weeklyData.get(0).get("attendance")).longValue();
        long lastWeek = ((Number) weeklyData.get(weeklyData.size() - 1).get("attendance")).longValue();
        
        return firstWeek == 0 ? 0 : (double) (lastWeek - firstWeek) / firstWeek;
    }

    private String calculateForecastConfidence(List<Map<String, Object>> weeklyData) {
        return weeklyData.size() >= 4 ? "High" : weeklyData.size() >= 2 ? "Medium" : "Low";
    }

    private List<String> generateTrendInsights(double trend, List<Map<String, Object>> weeklyData) {
        List<String> insights = new ArrayList<>();
        
        if (trend > 0.1) {
            insights.add("📈 Attendance is improving consistently");
        } else if (trend < -0.1) {
            insights.add("📉 Attendance is declining - action needed");
        } else {
            insights.add("➡️ Attendance is stable");
        }
        
        if (weeklyData.size() >= 4) {
            insights.add("✓ Sufficient data for reliable predictions");
        }
        
        return insights;
    }
}
