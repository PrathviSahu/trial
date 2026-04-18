package com.faceattendance.repository;

import com.faceattendance.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Student entity
 * 
 * Provides CRUD operations and custom queries for student management
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    /**
     * Find student by IEN number
     */
    Optional<Student> findByIenNumber(String ienNumber);
    
    /**
     * Find student by roll number
     */
    Optional<Student> findByRollNumber(String rollNumber);
    
    /**
     * Find student by email
     */
    Optional<Student> findByEmail(String email);
    
    /**
     * Check if IEN number exists
     */
    boolean existsByIenNumber(String ienNumber);
    
    /**
     * Check if roll number exists
     */
    boolean existsByRollNumber(String rollNumber);
    
    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Find students by department
     */
    List<Student> findByDepartment(Student.Department department);
    
    /**
     * Find students by department and year
     */
    List<Student> findByDepartmentAndYear(Student.Department department, Integer year);
    
    /**
     * Find students by department, year and semester
     */
    List<Student> findByDepartmentAndYearAndSemester(Student.Department department, Integer year, Integer semester);
    
    /**
     * Find students by branch
     */
    List<Student> findByBranch(String branch);
    
    /**
     * Find active students
     */
    List<Student> findByIsActiveTrue();
    
    /**
     * Find students with face enrolled
     */
    List<Student> findByFaceEnrolledTrue();
    
    /**
     * Find students without face enrolled
     */
    List<Student> findByFaceEnrolledFalse();
    
    /**
     * Search students by name (first name or last name)
     */
    @Query("SELECT s FROM Student s WHERE " +
           "LOWER(s.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(CONCAT(s.firstName, ' ', s.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Student> searchByName(@Param("searchTerm") String searchTerm);
    
    /**
     * Search students with pagination
     */
    @Query("SELECT s FROM Student s WHERE " +
           "(:searchTerm IS NULL OR " +
           "LOWER(s.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.ienNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "(:department IS NULL OR s.department = :department) AND " +
           "(:year IS NULL OR s.year = :year) AND " +
           "(:isActive IS NULL OR s.isActive = :isActive)")
    Page<Student> searchStudents(@Param("searchTerm") String searchTerm,
                                @Param("department") Student.Department department,
                                @Param("year") Integer year,
                                @Param("isActive") Boolean isActive,
                                Pageable pageable);
    
    /**
     * Count students by department
     */
    @Query("SELECT s.department, COUNT(s) FROM Student s WHERE s.isActive = true GROUP BY s.department")
    List<Object[]> countStudentsByDepartment();
    
    /**
     * Count students by year
     */
    @Query("SELECT s.year, COUNT(s) FROM Student s WHERE s.isActive = true GROUP BY s.year")
    List<Object[]> countStudentsByYear();
    
    /**
     * Get students for attendance marking (active students with face enrolled)
     */
    @Query("SELECT s FROM Student s WHERE s.isActive = true AND s.faceEnrolled = true AND " +
           "s.department = :department AND s.year = :year AND s.semester = :semester")
    List<Student> findStudentsForAttendance(@Param("department") Student.Department department,
                                          @Param("year") Integer year,
                                          @Param("semester") Integer semester);
    
    /**
     * Find students by multiple IEN numbers (for bulk operations)
     */
    List<Student> findByIenNumberIn(List<String> ienNumbers);
    
    /**
     * Find students by multiple roll numbers (for bulk operations)
     */
    List<Student> findByRollNumberIn(List<String> rollNumbers);
}
