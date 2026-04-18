package com.faceattendance.service;

import com.faceattendance.entity.Admin;
import com.faceattendance.repository.AdminRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin Details Service for Spring Security
 * 
 * Loads admin user details for authentication
 */
@Service
@Slf4j
public class AdminDetailsService implements UserDetailsService {

    @Autowired
    private AdminRepository adminRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user details for username: {}", username);
        
        Admin admin = adminRepository.findByUsernameOrEmail(username)
                .orElseThrow(() -> {
                    log.error("Admin not found with username or email: {}", username);
                    return new UsernameNotFoundException("Admin not found with username or email: " + username);
                });

        log.debug("Admin found: {} (ID: {})", admin.getUsername(), admin.getId());
        
        // Check if account is currently locked
        if (admin.isCurrentlyLocked()) {
            log.warn("Admin account is locked: {}", username);
            throw new UsernameNotFoundException("Account is temporarily locked due to multiple failed login attempts");
        }
        
        // Check if account is active
        if (!admin.getIsActive()) {
            log.warn("Admin account is inactive: {}", username);
            throw new UsernameNotFoundException("Account is inactive");
        }
        
        // Check if account is enabled
        if (!admin.getIsEnabled()) {
            log.warn("Admin account is disabled: {}", username);
            throw new UsernameNotFoundException("Account is disabled");
        }

        return admin;
    }
}
