package com.faceattendance.repository;

import com.faceattendance.entity.AttendanceRecord;
import com.faceattendance.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AttendanceRecord entity
 * 
 * Provides CRUD operations and analytics queries for attendance management
 */
@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    
    /**
     * Find attendance record by student, subject, date and time slot
     */
    Optional<AttendanceRecord> findByStudentAndSubjectAndAttendanceDateAndTimeSlot(
            Student student, String subject, LocalDate attendanceDate, String timeSlot);
    
    /**
     * Check if attendance exists for student on specific date and time
     */
    boolean existsByStudentAndSubjectAndAttendanceDateAndTimeSlot(
            Student student, String subject, LocalDate attendanceDate, String timeSlot);
    
    /**
     * Find attendance records by student
     */
    List<AttendanceRecord> findByStudent(Student student);
    
    /**
     * Find attendance records by student with pagination
     */
    Page<AttendanceRecord> findByStudent(Student student, Pageable pageable);
    
    /**
     * Find attendance records by date
     */
    List<AttendanceRecord> findByAttendanceDate(LocalDate attendanceDate);
    
    /**
     * Find attendance records by date range
     */
    List<AttendanceRecord> findByAttendanceDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find attendance records by department and date
     */
    List<AttendanceRecord> findByDepartmentAndAttendanceDate(Student.Department department, LocalDate attendanceDate);
    
    /**
     * Find attendance records by department and date range
     */
    List<AttendanceRecord> findByDepartmentAndAttendanceDateBetween(
            Student.Department department, LocalDate startDate, LocalDate endDate);
    
    /**
     * Find attendance records by subject and date
     */
    List<AttendanceRecord> findBySubjectAndAttendanceDate(String subject, LocalDate attendanceDate);
    
    /**
     * Find attendance records by year and semester
     */
    List<AttendanceRecord> findByYearAndSemester(Integer year, Integer semester);
    
    /**
     * Get attendance statistics for a student
     */
    @Query("SELECT " +
           "COUNT(a) as totalClasses, " +
           "SUM(CASE WHEN a.attendanceStatus = 'PRESENT' THEN 1 ELSE 0 END) as presentCount, " +
           "SUM(CASE WHEN a.attendanceStatus = 'ABSENT' THEN 1 ELSE 0 END) as absentCount, " +
           "SUM(CASE WHEN a.attendanceStatus = 'LATE' THEN 1 ELSE 0 END) as lateCount " +
           "FROM AttendanceRecord a WHERE a.student = :student")
    Object[] getAttendanceStatsByStudent(@Param("student") Student student);
    
    /**
     * Get attendance statistics for a student in a subject
     */
    @Query("SELECT " +
           "COUNT(a) as totalClasses, " +
           "SUM(CASE WHEN a.attendanceStatus = 'PRESENT' THEN 1 ELSE 0 END) as presentCount, " +
           "SUM(CASE WHEN a.attendanceStatus = 'ABSENT' THEN 1 ELSE 0 END) as absentCount, " +
           "SUM(CASE WHEN a.attendanceStatus = 'LATE' THEN 1 ELSE 0 END) as lateCount " +
           "FROM AttendanceRecord a WHERE a.student = :student AND a.subject = :subject")
    Object[] getAttendanceStatsByStudentAndSubject(@Param("student") Student student, @Param("subject") String subject);
    
    /**
     * Get attendance statistics by department
     */
    @Query("SELECT " +
           "a.department, " +
           "COUNT(a) as totalClasses, " +
           "SUM(CASE WHEN a.attendanceStatus = 'PRESENT' THEN 1 ELSE 0 END) as presentCount, " +
           "SUM(CASE WHEN a.attendanceStatus = 'ABSENT' THEN 1 ELSE 0 END) as absentCount " +
           "FROM AttendanceRecord a " +
           "WHERE a.attendanceDate BETWEEN :startDate AND :endDate " +
           "GROUP BY a.department")
    List<Object[]> getAttendanceStatsByDepartment(@Param("startDate") LocalDate startDate, 
                                                 @Param("endDate") LocalDate endDate);
    
    /**
     * Get daily attendance summary
     */
    @Query("SELECT " +
           "a.attendanceDate, " +
           "a.department, " +
           "COUNT(a) as totalStudents, " +
           "SUM(CASE WHEN a.attendanceStatus = 'PRESENT' THEN 1 ELSE 0 END) as presentCount, " +
           "SUM(CASE WHEN a.attendanceStatus = 'ABSENT' THEN 1 ELSE 0 END) as absentCount " +
           "FROM AttendanceRecord a " +
           "WHERE a.attendanceDate = :date " +
           "GROUP BY a.attendanceDate, a.department")
    List<Object[]> getDailyAttendanceSummary(@Param("date") LocalDate date);
    
    /**
     * Get monthly attendance summary
     */
    @Query("SELECT " +
           "YEAR(a.attendanceDate) as year, " +
           "MONTH(a.attendanceDate) as month, " +
           "a.department, " +
           "COUNT(a) as totalClasses, " +
           "SUM(CASE WHEN a.attendanceStatus = 'PRESENT' THEN 1 ELSE 0 END) as presentCount " +
           "FROM AttendanceRecord a " +
           "WHERE a.attendanceDate BETWEEN :startDate AND :endDate " +
           "GROUP BY YEAR(a.attendanceDate), MONTH(a.attendanceDate), a.department")
    List<Object[]> getMonthlyAttendanceSummary(@Param("startDate") LocalDate startDate, 
                                              @Param("endDate") LocalDate endDate);
    
    /**
     * Get subject-wise attendance for a student
     */
    @Query("SELECT " +
           "a.subject, " +
           "COUNT(a) as totalClasses, " +
           "SUM(CASE WHEN a.attendanceStatus = 'PRESENT' THEN 1 ELSE 0 END) as presentCount, " +
           "ROUND((SUM(CASE WHEN a.attendanceStatus = 'PRESENT' THEN 1 ELSE 0 END) * 100.0 / COUNT(a)), 2) as attendancePercentage " +
           "FROM AttendanceRecord a " +
           "WHERE a.student = :student " +
           "GROUP BY a.subject")
    List<Object[]> getSubjectWiseAttendance(@Param("student") Student student);
    
    /**
     * Find students with low attendance (below threshold)
     */
    @Query("SELECT " +
           "a.student, " +
           "COUNT(a) as totalClasses, " +
           "SUM(CASE WHEN a.attendanceStatus = 'PRESENT' THEN 1 ELSE 0 END) as presentCount, " +
           "ROUND((SUM(CASE WHEN a.attendanceStatus = 'PRESENT' THEN 1 ELSE 0 END) * 100.0 / COUNT(a)), 2) as attendancePercentage " +
           "FROM AttendanceRecord a " +
           "WHERE a.attendanceDate BETWEEN :startDate AND :endDate " +
           "GROUP BY a.student " +
           "HAVING (SUM(CASE WHEN a.attendanceStatus = 'PRESENT' THEN 1 ELSE 0 END) * 100.0 / COUNT(a)) < :threshold")
    List<Object[]> findStudentsWithLowAttendance(@Param("startDate") LocalDate startDate, 
                                                @Param("endDate") LocalDate endDate, 
                                                @Param("threshold") Double threshold);
    
    /**
     * Get attendance records for export (with filters)
     */
    @Query("SELECT a FROM AttendanceRecord a WHERE " +
           "(:department IS NULL OR a.department = :department) AND " +
           "(:year IS NULL OR a.year = :year) AND " +
           "(:semester IS NULL OR a.semester = :semester) AND " +
           "(:subject IS NULL OR a.subject = :subject) AND " +
           "a.attendanceDate BETWEEN :startDate AND :endDate " +
           "ORDER BY a.attendanceDate DESC, a.student.rollNumber")
    List<AttendanceRecord> findAttendanceForExport(@Param("department") Student.Department department,
                                                  @Param("year") Integer year,
                                                  @Param("semester") Integer semester,
                                                  @Param("subject") String subject,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);
}
