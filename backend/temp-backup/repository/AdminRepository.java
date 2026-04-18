package com.faceattendance.repository;

import com.faceattendance.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Admin entity
 * 
 * Provides CRUD operations and authentication queries for admin management
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    
    /**
     * Find admin by username
     */
    Optional<Admin> findByUsername(String username);
    
    /**
     * Find admin by email
     */
    Optional<Admin> findByEmail(String email);
    
    /**
     * Find admin by username or email
     */
    @Query("SELECT a FROM Admin a WHERE a.username = :usernameOrEmail OR a.email = :usernameOrEmail")
    Optional<Admin> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);
    
    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Find active admins
     */
    List<Admin> findByIsActiveTrue();
    
    /**
     * Find enabled admins
     */
    List<Admin> findByIsEnabledTrue();
    
    /**
     * Find admins by role
     */
    List<Admin> findByRole(Admin.Role role);
    
    /**
     * Find locked admins (accounts locked until future date)
     */
    @Query("SELECT a FROM Admin a WHERE a.accountLockedUntil IS NOT NULL AND a.accountLockedUntil > :currentTime")
    List<Admin> findLockedAdmins(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find admins with failed login attempts above threshold
     */
    @Query("SELECT a FROM Admin a WHERE a.failedLoginAttempts >= :threshold")
    List<Admin> findAdminsWithFailedAttempts(@Param("threshold") Integer threshold);
    
    /**
     * Update last login time
     */
    @Modifying
    @Query("UPDATE Admin a SET a.lastLogin = :loginTime WHERE a.id = :adminId")
    void updateLastLogin(@Param("adminId") Long adminId, @Param("loginTime") LocalDateTime loginTime);
    
    /**
     * Reset failed login attempts
     */
    @Modifying
    @Query("UPDATE Admin a SET a.failedLoginAttempts = 0, a.accountLockedUntil = NULL WHERE a.id = :adminId")
    void resetFailedLoginAttempts(@Param("adminId") Long adminId);
    
    /**
     * Increment failed login attempts
     */
    @Modifying
    @Query("UPDATE Admin a SET a.failedLoginAttempts = COALESCE(a.failedLoginAttempts, 0) + 1 WHERE a.id = :adminId")
    void incrementFailedLoginAttempts(@Param("adminId") Long adminId);
    
    /**
     * Lock admin account
     */
    @Modifying
    @Query("UPDATE Admin a SET a.accountLockedUntil = :lockUntil WHERE a.id = :adminId")
    void lockAccount(@Param("adminId") Long adminId, @Param("lockUntil") LocalDateTime lockUntil);
    
    /**
     * Unlock admin account
     */
    @Modifying
    @Query("UPDATE Admin a SET a.accountLockedUntil = NULL, a.failedLoginAttempts = 0 WHERE a.id = :adminId")
    void unlockAccount(@Param("adminId") Long adminId);
    
    /**
     * Update admin status
     */
    @Modifying
    @Query("UPDATE Admin a SET a.isActive = :isActive, a.isEnabled = :isEnabled WHERE a.id = :adminId")
    void updateAdminStatus(@Param("adminId") Long adminId, 
                          @Param("isActive") Boolean isActive, 
                          @Param("isEnabled") Boolean isEnabled);
    
    /**
     * Count total active admins
     */
    @Query("SELECT COUNT(a) FROM Admin a WHERE a.isActive = true AND a.isEnabled = true")
    Long countActiveAdmins();
    
    /**
     * Find admins created in date range
     */
    @Query("SELECT a FROM Admin a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<Admin> findAdminsCreatedBetween(@Param("startDate") LocalDateTime startDate, 
                                        @Param("endDate") LocalDateTime endDate);
}
