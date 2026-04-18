package com.faceattendance.repository;

import com.faceattendance.model.Attendance;
import com.faceattendance.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByStudent(Student student);

    List<Attendance> findByStudentId(Long studentId);

    @Query("SELECT a FROM Attendance a WHERE a.timestamp >= :start AND a.timestamp <= :end")
    List<Attendance> findByTimestampBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT a FROM Attendance a WHERE a.student.id = :studentId AND DATE(a.timestamp) = CURRENT_DATE")
    Optional<Attendance> findTodayAttendanceForStudent(@Param("studentId") Long studentId);

    @Query("SELECT a FROM Attendance a WHERE a.student.id = :studentId AND DATE(a.timestamp) = :date AND a.subject = :subject")
    Optional<Attendance> findByStudentAndDateAndSubject(
            @Param("studentId") Long studentId,
            @Param("date") LocalDate date,
            @Param("subject") String subject);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE DATE(a.timestamp) = CURRENT_DATE")
    Long countTodayAttendance();

    @Query("SELECT a FROM Attendance a WHERE a.subject = :subject AND DATE(a.timestamp) = :date")
    List<Attendance> findBySubjectAndDate(
            @Param("subject") String subject,
            @Param("date") LocalDate date);
}
