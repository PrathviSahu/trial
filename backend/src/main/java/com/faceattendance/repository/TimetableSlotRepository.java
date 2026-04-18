package com.faceattendance.repository;

import com.faceattendance.model.TimetableSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimetableSlotRepository extends JpaRepository<TimetableSlot, Long> {
    
    List<TimetableSlot> findByDepartmentAndYearAndSemesterAndSection(
        String department, Integer year, Integer semester, String section);
    
    List<TimetableSlot> findByDepartmentAndYearAndSemesterAndSectionAndDayOfWeek(
        String department, Integer year, Integer semester, String section, DayOfWeek dayOfWeek);
    
    @Query("SELECT t FROM TimetableSlot t WHERE " +
           "t.department = :department AND t.year = :year AND t.semester = :semester AND " +
           "t.section = :section AND t.dayOfWeek = :dayOfWeek AND " +
           "t.startTime <= :time AND t.endTime > :time")
    List<TimetableSlot> findActiveSlotsAtTime(
        @Param("department") String department,
        @Param("year") Integer year,
        @Param("semester") Integer semester,
        @Param("section") String section,
        @Param("dayOfWeek") DayOfWeek dayOfWeek,
        @Param("time") LocalTime time);
    
    Optional<TimetableSlot> findByDepartmentAndYearAndSemesterAndSectionAndDayOfWeekAndStartTimeAndEndTime(
        String department, Integer year, Integer semester, String section,
        DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime);
    
    void deleteByDepartmentAndYearAndSemesterAndSection(String department, Integer year, Integer semester, String section);
}
