package com.faceattendance.repository;

import com.faceattendance.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<LeaveRequest> findByStatusOrderByCreatedAtDesc(String status);

    List<LeaveRequest> findAllByOrderByCreatedAtDesc();
}
