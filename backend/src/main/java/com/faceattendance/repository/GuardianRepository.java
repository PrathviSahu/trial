package com.faceattendance.repository;

import com.faceattendance.model.Guardian;
import com.faceattendance.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface GuardianRepository extends JpaRepository<Guardian, Long> {
    Optional<Guardian> findByEmail(String email);
    List<Guardian> findByStudent(Student student);
    List<Guardian> findByActive(Boolean active);
    boolean existsByEmail(String email);
}
