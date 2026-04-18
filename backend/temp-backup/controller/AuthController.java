package com.faceattendance.controller;

import com.faceattendance.dto.JwtResponse;
import com.faceattendance.dto.LoginRequest;
import com.faceattendance.dto.MessageResponse;
import com.faceattendance.entity.Admin;
import com.faceattendance.repository.AdminRepository;
import com.faceattendance.util.JwtUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Authentication Controller
 * 
 * Handles admin authentication endpoints
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * Admin login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateAdmin(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            log.info("Login attempt for user: {}", loginRequest.getUsernameOrEmail());
            
            // Find admin by username or email
            Admin admin = adminRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail())
                    .orElse(null);
            
            if (admin != null) {
                // Check if account is locked
                if (admin.isCurrentlyLocked()) {
                    log.warn("Login attempt for locked account: {}", loginRequest.getUsernameOrEmail());
                    return ResponseEntity.status(HttpStatus.LOCKED)
                            .body(MessageResponse.error("Account is temporarily locked due to multiple failed login attempts. Please try again later."));
                }
                
                // Check if account is active
                if (!admin.getIsActive() || !admin.getIsEnabled()) {
                    log.warn("Login attempt for inactive account: {}", loginRequest.getUsernameOrEmail());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(MessageResponse.error("Account is inactive or disabled. Please contact administrator."));
                }
            }
            
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsernameOrEmail(), 
                            loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Generate JWT token
            String jwt = jwtUtils.generateJwtToken(authentication);
            String refreshToken = jwtUtils.generateRefreshToken(authentication.getName());
            
            Admin authenticatedAdmin = (Admin) authentication.getPrincipal();
            List<String> authorities = authenticatedAdmin.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            // Update last login and reset failed attempts
            authenticatedAdmin.setLastLogin(LocalDateTime.now());
            authenticatedAdmin.resetFailedLoginAttempts();
            adminRepository.save(authenticatedAdmin);
            
            log.info("Successful login for user: {} (ID: {})", authenticatedAdmin.getUsername(), authenticatedAdmin.getId());
            
            JwtResponse jwtResponse = new JwtResponse(
                    jwt,
                    refreshToken,
                    authenticatedAdmin.getId(),
                    authenticatedAdmin.getUsername(),
                    authenticatedAdmin.getEmail(),
                    authenticatedAdmin.getFirstName(),
                    authenticatedAdmin.getLastName(),
                    authenticatedAdmin.getRole().name(),
                    authorities
            );
            
            return ResponseEntity.ok(jwtResponse);
            
        } catch (BadCredentialsException e) {
            log.warn("Invalid credentials for user: {}", loginRequest.getUsernameOrEmail());
            
            // Increment failed login attempts
            adminRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail())
                    .ifPresent(admin -> {
                        admin.incrementFailedLoginAttempts();
                        
                        // Lock account after 5 failed attempts
                        if (admin.getFailedLoginAttempts() >= 5) {
                            admin.lockAccount(30); // Lock for 30 minutes
                            log.warn("Account locked due to multiple failed attempts: {}", admin.getUsername());
                        }
                        
                        adminRepository.save(admin);
                    });
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponse.error("Invalid username/email or password"));
                    
        } catch (DisabledException e) {
            log.warn("Disabled account login attempt: {}", loginRequest.getUsernameOrEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(MessageResponse.error("Account is disabled"));
                    
        } catch (LockedException e) {
            log.warn("Locked account login attempt: {}", loginRequest.getUsernameOrEmail());
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(MessageResponse.error("Account is locked"));
                    
        } catch (AuthenticationException e) {
            log.error("Authentication error for user: {} - {}", loginRequest.getUsernameOrEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponse.error("Authentication failed: " + e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Unexpected error during login for user: {} - {}", loginRequest.getUsernameOrEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MessageResponse.error("An unexpected error occurred. Please try again."));
        }
    }

    /**
     * Admin logout endpoint
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logoutAdmin() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.getPrincipal() instanceof Admin) {
                Admin admin = (Admin) authentication.getPrincipal();
                log.info("Logout for user: {} (ID: {})", admin.getUsername(), admin.getId());
            }
            
            SecurityContextHolder.clearContext();
            
            return ResponseEntity.ok(MessageResponse.success("Logged out successfully"));
            
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MessageResponse.error("Error during logout"));
        }
    }

    /**
     * Refresh token endpoint
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestParam String refreshToken) {
        try {
            if (jwtUtils.validateJwtToken(refreshToken) && jwtUtils.isRefreshToken(refreshToken)) {
                String username = jwtUtils.getUsernameFromJwtToken(refreshToken);
                
                Admin admin = adminRepository.findByUsernameOrEmail(username)
                        .orElseThrow(() -> new RuntimeException("Admin not found"));
                
                String newAccessToken = jwtUtils.generateTokenFromUsername(username);
                String newRefreshToken = jwtUtils.generateRefreshToken(username);
                
                List<String> authorities = admin.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList());
                
                JwtResponse jwtResponse = new JwtResponse(
                        newAccessToken,
                        newRefreshToken,
                        admin.getId(),
                        admin.getUsername(),
                        admin.getEmail(),
                        admin.getFirstName(),
                        admin.getLastName(),
                        admin.getRole().name(),
                        authorities
                );
                
                return ResponseEntity.ok(jwtResponse);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(MessageResponse.error("Invalid refresh token"));
            }
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponse.error("Token refresh failed"));
        }
    }

    /**
     * Get current admin info
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentAdmin() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.getPrincipal() instanceof Admin) {
                Admin admin = (Admin) authentication.getPrincipal();
                
                // Refresh admin data from database
                Admin currentAdmin = adminRepository.findById(admin.getId())
                        .orElseThrow(() -> new RuntimeException("Admin not found"));
                
                return ResponseEntity.ok(MessageResponse.success("Admin info retrieved", currentAdmin));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(MessageResponse.error("No authenticated admin found"));
            }
        } catch (Exception e) {
            log.error("Error getting current admin: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MessageResponse.error("Error retrieving admin info"));
        }
    }
}
