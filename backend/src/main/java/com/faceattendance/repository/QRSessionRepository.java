package com.faceattendance.repository;

import com.faceattendance.model.QRSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QRSessionRepository extends JpaRepository<QRSession, Long> {
    Optional<QRSession> findBySessionCode(String sessionCode);
}
