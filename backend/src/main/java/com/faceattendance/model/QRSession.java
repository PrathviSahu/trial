package com.faceattendance.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "qr_sessions")
public class QRSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_code", unique = true, nullable = false)
    private String sessionCode;

    @Column(name = "subject")
    private String subject;

    @Column(name = "department")
    private String department;

    @Column(name = "faculty")
    private String faculty;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "qr_session_marked_students", joinColumns = @JoinColumn(name = "qr_session_id"))
    @Column(name = "student_id")
    private Set<Long> markedStudents = new HashSet<>();

    public QRSession() {
    }

    public QRSession(String sessionCode, String subject, String department, String faculty, LocalDateTime expiresAt) {
        this.sessionCode = sessionCode;
        this.subject = subject;
        this.department = department;
        this.faculty = faculty;
        this.expiresAt = expiresAt;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getFaculty() {
        return faculty;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Set<Long> getMarkedStudents() {
        return markedStudents;
    }

    public void setMarkedStudents(Set<Long> markedStudents) {
        this.markedStudents = markedStudents;
    }
}
