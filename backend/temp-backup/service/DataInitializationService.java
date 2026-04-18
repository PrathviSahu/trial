package com.faceattendance.service;

import com.faceattendance.entity.Admin;
import com.faceattendance.repository.AdminRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Data Initialization Service
 * 
 * Creates default admin user and initial data on application startup
 */
@Service
@Slf4j
public class DataInitializationService implements CommandLineRunner {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("🚀 Starting data initialization...");
        
        createDefaultAdmin();
        
        log.info("✅ Data initialization completed successfully!");
    }

    /**
     * Create default admin user if not exists
     */
    private void createDefaultAdmin() {
        try {
            // Check if any admin exists
            long adminCount = adminRepository.count();
            
            if (adminCount == 0) {
                log.info("No admin users found. Creating default admin...");
                
                Admin defaultAdmin = new Admin();
                defaultAdmin.setUsername("admin");
                defaultAdmin.setEmail("admin@faceattendance.com");
                defaultAdmin.setPassword(passwordEncoder.encode("admin123"));
                defaultAdmin.setFirstName("System");
                defaultAdmin.setLastName("Administrator");
                defaultAdmin.setPhoneNumber("+91-9999999999");
                defaultAdmin.setRole(Admin.Role.SUPER_ADMIN);
                defaultAdmin.setIsActive(true);
                defaultAdmin.setIsAccountNonExpired(true);
                defaultAdmin.setIsAccountNonLocked(true);
                defaultAdmin.setIsCredentialsNonExpired(true);
                defaultAdmin.setIsEnabled(true);
                defaultAdmin.setFailedLoginAttempts(0);
                defaultAdmin.setCreatedBy("SYSTEM");
                defaultAdmin.setCreatedAt(LocalDateTime.now());
                
                adminRepository.save(defaultAdmin);
                
                log.info("✅ Default admin created successfully!");
                log.info("📧 Username: admin");
                log.info("📧 Email: admin@faceattendance.com");
                log.info("🔑 Password: admin123");
                log.info("⚠️  Please change the default password after first login!");
                
            } else {
                log.info("Admin users already exist. Skipping default admin creation.");
            }
            
        } catch (Exception e) {
            log.error("❌ Error creating default admin: {}", e.getMessage());
        }
    }
}
