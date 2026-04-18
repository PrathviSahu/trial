package com.faceattendance.service;

import com.faceattendance.model.Student;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username:demo@facetrackU.com}")
    private String fromEmail;
    
    private final List<Map<String, String>> sentEmails = new java.util.ArrayList<>();
    private boolean emailConfigured = false;

    /**
     * Send low attendance alert to student
     */
    public void sendLowAttendanceAlert(Student student, double attendanceRate) {
        String subject = "⚠️ Low Attendance Alert - Action Required";
        String body = buildLowAttendanceEmail(student, attendanceRate);
        
        sendEmail(student.getEmail(), subject, body, "LOW_ATTENDANCE_ALERT");
    }

    /**
     * Send welcome email to new student
     */
    public void sendWelcomeEmail(Student student) {
        String subject = "🎓 Welcome to FaceTrackU - Face Attendance System";
        String body = buildWelcomeEmail(student);
        
        sendEmail(student.getEmail(), subject, body, "WELCOME");
    }

    /**
     * Send daily attendance summary to admin
     */
    public void sendDailySummary(String adminEmail, Map<String, Object> stats) {
        String subject = "📊 Daily Attendance Summary - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        String body = buildDailySummaryEmail(stats);
        
        sendEmail(adminEmail, subject, body, "DAILY_SUMMARY");
    }

    /**
     * Send weekly report to faculty
     */
    public void sendWeeklyReport(String facultyEmail, String department, Map<String, Object> stats) {
        String subject = "📈 Weekly Attendance Report - " + department;
        String body = buildWeeklyReportEmail(department, stats);
        
        sendEmail(facultyEmail, subject, body, "WEEKLY_REPORT");
    }

    /**
     * Send at-risk student notification
     */
    public void sendAtRiskNotification(String email, List<Student> atRiskStudents) {
        String subject = "🚨 At-Risk Students Alert - Immediate Action Required";
        String body = buildAtRiskEmail(atRiskStudents);
        
        sendEmail(email, subject, body, "AT_RISK_ALERT");
    }

    /**
     * Get all sent emails (for demo/testing)
     */
    public List<Map<String, String>> getSentEmails() {
        return sentEmails;
    }

    /**
     * Clear sent emails history
     */
    public void clearHistory() {
        sentEmails.clear();
    }

    // Private helper methods

    private void sendEmail(String to, String subject, String body, String type) {
        // Store in history
        Map<String, String> email = new HashMap<>();
        email.put("to", to);
        email.put("subject", subject);
        email.put("body", body);
        email.put("type", type);
        email.put("sentAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // Try to send real email if configured
        boolean sent = false;
        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                
                mailSender.send(message);
                sent = true;
                email.put("status", "SENT");
                System.out.println("✅ Real email sent to: " + to);
            } catch (Exception e) {
                email.put("status", "FAILED: " + e.getMessage());
                System.out.println("❌ Failed to send email: " + e.getMessage());
            }
        } else {
            email.put("status", "DEMO MODE ✅");
            System.out.println("⚠️ Email simulated in demo mode (no mail server configured)");
        }
        
        sentEmails.add(email);
        
        // Log for demo purposes
        System.out.println("\n📧 EMAIL " + (sent ? "SENT" : "LOGGED") + ":");
        System.out.println("To: " + to);
        System.out.println("Subject: " + subject);
        System.out.println("Type: " + type);
        System.out.println("Time: " + email.get("sentAt"));
        System.out.println("---");
    }

    private String buildLowAttendanceEmail(Student student, double attendanceRate) {
        return String.format("""
            Dear %s %s,
            
            This is an important notification regarding your attendance.
            
            📊 Current Attendance Status:
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            • Student ID: %s
            • Department: %s
            • Current Attendance: %.2f%%
            • Required Minimum: 75%%
            • Status: ⚠️ BELOW REQUIREMENT
            
            ⚡ Action Required:
            Your attendance has fallen below the required 75%% threshold. This may affect:
            - Your eligibility to appear in exams
            - Academic standing
            - Semester completion
            
            💡 Next Steps:
            1. Review your attendance record
            2. Contact your faculty advisor
            3. Plan to attend all upcoming classes
            4. Request academic counseling if needed
            
            📞 Support:
            If you have any concerns, please contact:
            - Academic Office: academic@college.edu
            - Student Support: support@college.edu
            
            Best regards,
            FaceTrackU Attendance System
            """,
            student.getFirstName(),
            student.getLastName(),
            student.getIenNumber(),
            student.getDepartment(),
            attendanceRate
        );
    }

    private String buildWelcomeEmail(Student student) {
        return String.format("""
            Dear %s %s,
            
            🎉 Welcome to FaceTrackU - AI-Powered Face Attendance System!
            
            👤 Your Account Details:
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            • Student ID: %s
            • Roll Number: %s
            • Department: %s
            • Year: %d | Semester: %d
            • Email: %s
            
            🚀 Getting Started:
            1. Complete your face enrollment at the enrollment station
            2. Ensure good lighting for accurate face recognition
            3. Check your attendance regularly on the dashboard
            4. Maintain 75%% minimum attendance requirement
            
            ✨ Features Available to You:
            • Real-time attendance tracking
            • Automated face recognition
            • Performance analytics
            • Attendance reports and insights
            • Email notifications and alerts
            
            📱 Access Your Dashboard:
            Visit: http://localhost:3000
            Your attendance records are updated in real-time!
            
            💡 Pro Tips:
            - Arrive on time for accurate attendance marking
            - Keep your face visible to the camera
            - Report any attendance discrepancies within 24 hours
            
            Need help? Contact support@college.edu
            
            Best regards,
            FaceTrackU Team
            """,
            student.getFirstName(),
            student.getLastName(),
            student.getIenNumber(),
            student.getRollNumber(),
            student.getDepartment(),
            student.getYear(),
            student.getSemester(),
            student.getEmail()
        );
    }

    private String buildDailySummaryEmail(Map<String, Object> stats) {
        return String.format("""
            📊 Daily Attendance Summary
            Generated: %s
            
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            📈 Today's Statistics:
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            
            👥 Attendance Overview:
            • Total Students: %d
            • Present Today: %d
            • Absent Today: %d
            • Attendance Rate: %.2f%%
            
            🎯 Department Breakdown:
            %s
            
            ⚠️ Alerts:
            • Low Attendance Students: %d
            • At-Risk Students: %d
            • Pending Face Enrollments: %d
            
            📊 Trends:
            • Compared to yesterday: %s
            • Weekly average: %.2f%%
            • Monthly average: %.2f%%
            
            🔔 Action Items:
            %s
            
            Access full dashboard: http://localhost:3000
            
            Best regards,
            FaceTrackU System
            """,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' hh:mm a")),
            stats.getOrDefault("totalStudents", 0),
            stats.getOrDefault("presentToday", 0),
            stats.getOrDefault("absentToday", 0),
            stats.getOrDefault("attendanceRate", 0.0),
            stats.getOrDefault("departmentBreakdown", "No data available"),
            stats.getOrDefault("lowAttendanceCount", 0),
            stats.getOrDefault("atRiskCount", 0),
            stats.getOrDefault("pendingEnrollments", 0),
            stats.getOrDefault("trend", "No change"),
            stats.getOrDefault("weeklyAverage", 0.0),
            stats.getOrDefault("monthlyAverage", 0.0),
            stats.getOrDefault("actionItems", "None")
        );
    }

    private String buildWeeklyReportEmail(String department, Map<String, Object> stats) {
        return String.format("""
            📈 Weekly Attendance Report - %s Department
            Week Ending: %s
            
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            📊 Weekly Performance:
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            
            🎯 Overall Statistics:
            • Total Students: %d
            • Average Attendance: %.2f%%
            • Classes Conducted: %d
            • Total Attendance Marked: %d
            
            📈 Daily Breakdown:
            %s
            
            ⭐ Top Performers:
            %s
            
            ⚠️ Needs Attention:
            %s
            
            📊 Trends:
            • Week-over-week: %s
            • Best Day: %s
            • Lowest Day: %s
            
            💡 Recommendations:
            %s
            
            Download detailed report: http://localhost:3000/reports
            
            Best regards,
            FaceTrackU Analytics
            """,
            department,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
            stats.getOrDefault("totalStudents", 0),
            stats.getOrDefault("averageAttendance", 0.0),
            stats.getOrDefault("classesCount", 0),
            stats.getOrDefault("totalMarked", 0),
            stats.getOrDefault("dailyBreakdown", "No data"),
            stats.getOrDefault("topPerformers", "No data"),
            stats.getOrDefault("needsAttention", "No data"),
            stats.getOrDefault("trend", "Stable"),
            stats.getOrDefault("bestDay", "N/A"),
            stats.getOrDefault("lowestDay", "N/A"),
            stats.getOrDefault("recommendations", "Continue current practices")
        );
    }

    private String buildAtRiskEmail(List<Student> atRiskStudents) {
        StringBuilder studentList = new StringBuilder();
        for (int i = 0; i < Math.min(atRiskStudents.size(), 10); i++) {
            Student s = atRiskStudents.get(i);
            studentList.append(String.format(
                "%d. %s %s (%s) - %s - Contact: %s\n",
                i + 1,
                s.getFirstName(),
                s.getLastName(),
                s.getIenNumber(),
                s.getDepartment(),
                s.getEmail()
            ));
        }
        
        return String.format("""
            🚨 At-Risk Students Alert
            Generated: %s
            
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            ⚠️ IMMEDIATE ATTENTION REQUIRED
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            
            Total At-Risk Students: %d
            
            These students have been identified by our AI system as having:
            • High probability of continued absence
            • Attendance below 75%% threshold
            • Declining attendance trends
            
            📋 Student List (Top 10):
            %s
            
            🎯 Recommended Actions:
            1. Contact students immediately
            2. Schedule counseling sessions
            3. Notify parents/guardians
            4. Create intervention plan
            5. Monitor progress daily
            
            📊 View full analysis: http://localhost:3000/predictions
            
            Best regards,
            FaceTrackU AI System
            """,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a")),
            atRiskStudents.size(),
            studentList.toString()
        );
    }
}
