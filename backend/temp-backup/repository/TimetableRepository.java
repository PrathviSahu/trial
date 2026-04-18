package com.faceattendance.repository;

import com.faceattendance.entity.Student;
import com.faceattendance.entity.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Timetable entity
 * 
 * Provides CRUD operations and scheduling queries for timetable management
 */
@Repository
public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    
    /**
     * Find timetable by department, year, semester and day
     */
    List<Timetable> findByDepartmentAndYearAndSemesterAndDayOfWeek(
            Student.Department department, Integer year, Integer semester, DayOfWeek dayOfWeek);
    
    /**
     * Find timetable by department and year
     */
    List<Timetable> findByDepartmentAndYear(Student.Department department, Integer year);
    
    /**
     * Find timetable by department, year and semester
     */
    List<Timetable> findByDepartmentAndYearAndSemester(
            Student.Department department, Integer year, Integer semester);
    
    /**
     * Find active timetable entries
     */
    List<Timetable> findByIsActiveTrue();
    
    /**
     * Find timetable by subject
     */
    List<Timetable> findBySubject(String subject);
    
    /**
     * Find timetable by faculty
     */
    List<Timetable> findByFacultyName(String facultyName);
    
    /**
     * Find timetable by classroom
     */
    List<Timetable> findByClassroom(String classroom);
    
    /**
     * Find timetable by day of week
     */
    List<Timetable> findByDayOfWeek(DayOfWeek dayOfWeek);
    
    /**
     * Check for time slot conflicts in same classroom
     */
    @Query("SELECT t FROM Timetable t WHERE " +
           "t.classroom = :classroom AND " +
           "t.dayOfWeek = :dayOfWeek AND " +
           "t.isActive = true AND " +
           "(:id IS NULL OR t.id != :id) AND " +
           "((t.startTime <= :startTime AND t.endTime > :startTime) OR " +
           "(t.startTime < :endTime AND t.endTime >= :endTime) OR " +
           "(t.startTime >= :startTime AND t.endTime <= :endTime))")
    List<Timetable> findConflictingTimeSlots(@Param("classroom") String classroom,
                                           @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                           @Param("startTime") LocalTime startTime,
                                           @Param("endTime") LocalTime endTime,
                                           @Param("id") Long id);
    
    /**
     * Check for faculty conflicts (same faculty teaching at same time)
     */
    @Query("SELECT t FROM Timetable t WHERE " +
           "t.facultyName = :facultyName AND " +
           "t.dayOfWeek = :dayOfWeek AND " +
           "t.isActive = true AND " +
           "(:id IS NULL OR t.id != :id) AND " +
           "((t.startTime <= :startTime AND t.endTime > :startTime) OR " +
           "(t.startTime < :endTime AND t.endTime >= :endTime) OR " +
           "(t.startTime >= :startTime AND t.endTime <= :endTime))")
    List<Timetable> findFacultyConflicts(@Param("facultyName") String facultyName,
                                        @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                        @Param("startTime") LocalTime startTime,
                                        @Param("endTime") LocalTime endTime,
                                        @Param("id") Long id);
    
    /**
     * Get current active class for department
     */
    @Query("SELECT t FROM Timetable t WHERE " +
           "t.department = :department AND " +
           "t.year = :year AND " +
           "t.semester = :semester AND " +
           "t.dayOfWeek = :dayOfWeek AND " +
           "t.startTime <= :currentTime AND " +
           "t.endTime > :currentTime AND " +
           "t.isActive = true AND " +
           "(t.effectiveFrom IS NULL OR t.effectiveFrom <= :now) AND " +
           "(t.effectiveUntil IS NULL OR t.effectiveUntil > :now)")
    Optional<Timetable> findCurrentClass(@Param("department") Student.Department department,
                                        @Param("year") Integer year,
                                        @Param("semester") Integer semester,
                                        @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                        @Param("currentTime") LocalTime currentTime,
                                        @Param("now") LocalDateTime now);
    
    /**
     * Get next class for department
     */
    @Query("SELECT t FROM Timetable t WHERE " +
           "t.department = :department AND " +
           "t.year = :year AND " +
           "t.semester = :semester AND " +
           "t.dayOfWeek = :dayOfWeek AND " +
           "t.startTime > :currentTime AND " +
           "t.isActive = true AND " +
           "(t.effectiveFrom IS NULL OR t.effectiveFrom <= :now) AND " +
           "(t.effectiveUntil IS NULL OR t.effectiveUntil > :now) " +
           "ORDER BY t.startTime ASC")
    List<Timetable> findNextClasses(@Param("department") Student.Department department,
                                   @Param("year") Integer year,
                                   @Param("semester") Integer semester,
                                   @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                   @Param("currentTime") LocalTime currentTime,
                                   @Param("now") LocalDateTime now);
    
    /**
     * Get weekly schedule for department
     */
    @Query("SELECT t FROM Timetable t WHERE " +
           "t.department = :department AND " +
           "t.year = :year AND " +
           "t.semester = :semester AND " +
           "t.isActive = true AND " +
           "(t.effectiveFrom IS NULL OR t.effectiveFrom <= :now) AND " +
           "(t.effectiveUntil IS NULL OR t.effectiveUntil > :now) " +
           "ORDER BY t.dayOfWeek, t.startTime")
    List<Timetable> findWeeklySchedule(@Param("department") Student.Department department,
                                      @Param("year") Integer year,
                                      @Param("semester") Integer semester,
                                      @Param("now") LocalDateTime now);
    
    /**
     * Get all subjects for department and year
     */
    @Query("SELECT DISTINCT t.subject FROM Timetable t WHERE " +
           "t.department = :department AND " +
           "t.year = :year AND " +
           "t.semester = :semester AND " +
           "t.isActive = true")
    List<String> findSubjectsByDepartmentAndYear(@Param("department") Student.Department department,
                                                @Param("year") Integer year,
                                                @Param("semester") Integer semester);
    
    /**
     * Get classroom utilization statistics
     */
    @Query("SELECT " +
           "t.classroom, " +
           "COUNT(t) as totalSlots, " +
           "SUM(CASE WHEN t.classType = 'LECTURE' THEN 1 ELSE 0 END) as lectureSlots, " +
           "SUM(CASE WHEN t.classType = 'PRACTICAL' THEN 1 ELSE 0 END) as practicalSlots " +
           "FROM Timetable t WHERE t.isActive = true " +
           "GROUP BY t.classroom " +
           "ORDER BY totalSlots DESC")
    List<Object[]> getClassroomUtilization();
    
    /**
     * Get faculty workload statistics
     */
    @Query("SELECT " +
           "t.facultyName, " +
           "COUNT(t) as totalClasses, " +
           "COUNT(DISTINCT t.subject) as subjectCount, " +
           "COUNT(DISTINCT t.department) as departmentCount " +
           "FROM Timetable t WHERE t.isActive = true AND t.facultyName IS NOT NULL " +
           "GROUP BY t.facultyName " +
           "ORDER BY totalClasses DESC")
    List<Object[]> getFacultyWorkload();
    
    /**
     * Find timetable entries expiring soon
     */
    @Query("SELECT t FROM Timetable t WHERE " +
           "t.effectiveUntil IS NOT NULL AND " +
           "t.effectiveUntil BETWEEN :now AND :futureDate AND " +
           "t.isActive = true")
    List<Timetable> findExpiringSoon(@Param("now") LocalDateTime now, 
                                    @Param("futureDate") LocalDateTime futureDate);
}
