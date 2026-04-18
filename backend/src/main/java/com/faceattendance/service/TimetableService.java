package com.faceattendance.service;

import com.faceattendance.model.Student;
import com.faceattendance.model.TimetableSlot;
import com.faceattendance.repository.StudentRepository;
import com.faceattendance.repository.TimetableSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class TimetableService {
    
    @Autowired
    private TimetableSlotRepository timetableSlotRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    public List<TimetableSlot> getWeeklySchedule(String department, Integer year, Integer semester, String section) {
        return timetableSlotRepository.findByDepartmentAndYearAndSemesterAndSection(department, year, semester, section);
    }
    
    public List<TimetableSlot> getActiveSlotsForStudent(Long studentId) {
        Optional<Student> studentOpt = studentRepository.findById(studentId);
        if (!studentOpt.isPresent()) {
            return List.of();
        }
        
        Student student = studentOpt.get();
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        LocalTime now = LocalTime.now();
        
        return timetableSlotRepository.findActiveSlotsAtTime(
            student.getDepartment(),
            student.getYear(),
            student.getSemester(),
            student.getSection() != null ? student.getSection() : "",
            today,
            now
        );
    }
    
    public List<TimetableSlot> getActiveSlotsForStudentAtTime(Long studentId, LocalDateTime at) {
        Optional<Student> studentOpt = studentRepository.findById(studentId);
        if (!studentOpt.isPresent()) {
            return List.of();
        }
        
        Student student = studentOpt.get();
        DayOfWeek day = at.toLocalDate().getDayOfWeek();
        LocalTime time = at.toLocalTime();
        
        return timetableSlotRepository.findActiveSlotsAtTime(
            student.getDepartment(),
            student.getYear(),
            student.getSemester(),
            student.getSection() != null ? student.getSection() : "",
            day,
            time
        );
    }
    
    public Optional<TimetableSlot> getSlotById(Long slotId) {
        return timetableSlotRepository.findById(slotId);
    }
    
    public TimetableSlot saveSlot(TimetableSlot slot) {
        return timetableSlotRepository.save(slot);
    }
    
    public void deleteSlot(Long slotId) {
        timetableSlotRepository.deleteById(slotId);
    }
    
    public void deleteAllByDepartmentYearSemesterSection(String department, Integer year, Integer semester, String section) {
        timetableSlotRepository.deleteByDepartmentAndYearAndSemesterAndSection(department, year, semester, section);
    }
}
