package com.faceattendance.repository;

import com.faceattendance.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {
    
    Optional<Student> findByIenNumber(String ienNumber);
    
    List<Student> findByDepartment(String department);
    
    List<Student> findByIsActive(Boolean isActive);

    long countByFaceEnrolledTrue();
    
    @Query("SELECT s FROM Student s WHERE s.faceEnrolled = true")
    List<Student> findAllEnrolledStudents();
    
    @Query("SELECT s FROM Student s WHERE s.faceDescriptor IS NOT NULL AND s.faceDescriptor != ''")
    List<Student> findStudentsWithFaceData();

    @Query("SELECT s.department, COUNT(s) FROM Student s WHERE s.department IS NOT NULL AND s.department <> '' GROUP BY s.department")
    List<Object[]> countByDepartment();
}
