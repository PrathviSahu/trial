package com.faceattendance.service;

import com.faceattendance.model.Attendance;
import com.faceattendance.model.Student;
import com.faceattendance.repository.AttendanceRepository;
import com.faceattendance.repository.StudentRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PDFReportService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StudentRepository studentRepository;

    /**
     * Generate comprehensive Excel report with charts
     */
    public byte[] generateExcelReport(String department, String startDate, String endDate) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        
        // Create summary sheet
        Sheet summarySheet = workbook.createSheet("Summary");
        createSummarySheet(summarySheet, department);
        
        // Create attendance data sheet
        Sheet dataSheet = workbook.createSheet("Attendance Data");
        createAttendanceDataSheet(dataSheet, department, startDate, endDate);
        
        // Create student list sheet
        Sheet studentSheet = workbook.createSheet("Student List");
        createStudentListSheet(studentSheet, department);
        
        // Create analytics sheet
        Sheet analyticsSheet = workbook.createSheet("Analytics");
        createAnalyticsSheet(analyticsSheet, department);

        // Write to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        return outputStream.toByteArray();
    }

    /**
     * Generate attendance summary report
     */
    public byte[] generateSummaryReport() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Attendance Summary");

        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Create title
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("FaceTrackU - Attendance Summary Report");
        titleCell.setCellStyle(headerStyle);

        // Add generation date
        Row dateRow = sheet.createRow(1);
        dateRow.createCell(0).setCellValue("Generated on: " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // Add summary statistics
        List<Student> allStudents = studentRepository.findAll();
        List<Attendance> allAttendance = attendanceRepository.findAll();

        Row statsHeaderRow = sheet.createRow(3);
        statsHeaderRow.createCell(0).setCellValue("Metric");
        statsHeaderRow.createCell(1).setCellValue("Value");

        int rowNum = 4;
        sheet.createRow(rowNum++).createCell(0).setCellValue("Total Students");
        sheet.getRow(rowNum - 1).createCell(1).setCellValue(allStudents.size());

        sheet.createRow(rowNum++).createCell(0).setCellValue("Total Attendance Records");
        sheet.getRow(rowNum - 1).createCell(1).setCellValue(allAttendance.size());

        sheet.createRow(rowNum++).createCell(0).setCellValue("Average Attendance Rate");
        double avgRate = allStudents.isEmpty() ? 0 : 
                (double) allAttendance.size() / (allStudents.size() * 50) * 100;
        sheet.getRow(rowNum - 1).createCell(1).setCellValue(String.format("%.2f%%", avgRate));

        // Department breakdown
        sheet.createRow(rowNum++ + 1).createCell(0).setCellValue("Department Breakdown");
        Row deptHeaderRow = sheet.createRow(rowNum++);
        deptHeaderRow.createCell(0).setCellValue("Department");
        deptHeaderRow.createCell(1).setCellValue("Students");
        deptHeaderRow.createCell(2).setCellValue("Attendance");

        allStudents.stream()
                .collect(Collectors.groupingBy(Student::getDepartment))
                .forEach((dept, students) -> {
                    Row deptRow = sheet.createRow(sheet.getLastRowNum() + 1);
                    deptRow.createCell(0).setCellValue(dept);
                    deptRow.createCell(1).setCellValue(students.size());
                    long deptAttendance = allAttendance.stream()
                            .filter(a -> a.getStudent() != null && 
                                    dept.equals(a.getStudent().getDepartment()))
                            .count();
                    deptRow.createCell(2).setCellValue(deptAttendance);
                });

        // Auto-size columns
        for (int i = 0; i < 3; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    /**
     * Generate department-wise report
     */
    public byte[] generateDepartmentReport(String department) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(department + " Report");

        // Create styled header
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        // Title
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Department: " + department);
        titleCell.setCellStyle(headerStyle);

        // Get data
        List<Student> students = studentRepository.findAll().stream()
                .filter(s -> department.equals(s.getDepartment()))
                .collect(Collectors.toList());
        
        List<Attendance> attendance = attendanceRepository.findAll().stream()
                .filter(a -> a.getStudent() != null && 
                        department.equals(a.getStudent().getDepartment()))
                .collect(Collectors.toList());

        // Statistics
        int rowNum = 2;
        sheet.createRow(rowNum++).createCell(0).setCellValue("Total Students: " + students.size());
        sheet.createRow(rowNum++).createCell(0).setCellValue("Total Attendance: " + attendance.size());
        sheet.createRow(rowNum++).createCell(0).setCellValue("Avg Rate: " + 
                String.format("%.2f%%", students.isEmpty() ? 0 : 
                        (double) attendance.size() / (students.size() * 50) * 100));

        // Student details
        rowNum += 2;
        Row studentHeaderRow = sheet.createRow(rowNum++);
        studentHeaderRow.createCell(0).setCellValue("IEN Number");
        studentHeaderRow.createCell(1).setCellValue("Name");
        studentHeaderRow.createCell(2).setCellValue("Year");
        studentHeaderRow.createCell(3).setCellValue("Attendance Count");
        studentHeaderRow.createCell(4).setCellValue("Rate");

        for (Student student : students) {
            long studentAttendance = attendance.stream()
                    .filter(a -> a.getStudent().getId().equals(student.getId()))
                    .count();
            
            Row dataRow = sheet.createRow(rowNum++);
            dataRow.createCell(0).setCellValue(student.getIenNumber() != null ? student.getIenNumber() : "N/A");
            dataRow.createCell(1).setCellValue(student.getFirstName() + " " + student.getLastName());
            dataRow.createCell(2).setCellValue(student.getYear() != null ? student.getYear() : 0);
            dataRow.createCell(3).setCellValue(studentAttendance);
            dataRow.createCell(4).setCellValue(String.format("%.2f%%", 
                    (double) studentAttendance / 50 * 100));
        }

        // Auto-size
        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    // Helper methods
    private void createSummarySheet(Sheet sheet, String department) {
        Row titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue("Attendance Report Summary");
        
        Row dateRow = sheet.createRow(1);
        dateRow.createCell(0).setCellValue("Generated: " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        if (department != null && !department.isEmpty()) {
            Row deptRow = sheet.createRow(2);
            deptRow.createCell(0).setCellValue("Department: " + department);
        }
    }

    private void createAttendanceDataSheet(Sheet sheet, String department, String startDate, String endDate) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Date");
        headerRow.createCell(1).setCellValue("IEN Number");
        headerRow.createCell(2).setCellValue("Student Name");
        headerRow.createCell(3).setCellValue("Department");
        headerRow.createCell(4).setCellValue("Subject");
        headerRow.createCell(5).setCellValue("Method");
        headerRow.createCell(6).setCellValue("Confidence");

        List<Attendance> attendanceList = attendanceRepository.findAll();
        if (department != null && !department.isEmpty()) {
            attendanceList = attendanceList.stream()
                    .filter(a -> a.getStudent() != null && 
                            department.equals(a.getStudent().getDepartment()))
                    .collect(Collectors.toList());
        }

        int rowNum = 1;
        for (Attendance attendance : attendanceList) {
            if (attendance.getStudent() == null) continue;
            
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(attendance.getTimestamp().toString());
            row.createCell(1).setCellValue(attendance.getStudent().getIenNumber() != null ? 
                    attendance.getStudent().getIenNumber() : "N/A");
            row.createCell(2).setCellValue(attendance.getStudent().getFirstName() + " " + 
                    attendance.getStudent().getLastName());
            row.createCell(3).setCellValue(attendance.getStudent().getDepartment());
            row.createCell(4).setCellValue(attendance.getSubject() != null ? attendance.getSubject() : "N/A");
            row.createCell(5).setCellValue(attendance.getMethod() != null ? attendance.getMethod() : "N/A");
            row.createCell(6).setCellValue(attendance.getConfidence() != null ? 
                    attendance.getConfidence() : 0);
        }

        for (int i = 0; i < 7; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createStudentListSheet(Sheet sheet, String department) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("IEN Number");
        headerRow.createCell(1).setCellValue("Name");
        headerRow.createCell(2).setCellValue("Department");
        headerRow.createCell(3).setCellValue("Year");
        headerRow.createCell(4).setCellValue("Email");
        headerRow.createCell(5).setCellValue("Face Enrolled");

        List<Student> students = studentRepository.findAll();
        if (department != null && !department.isEmpty()) {
            students = students.stream()
                    .filter(s -> department.equals(s.getDepartment()))
                    .collect(Collectors.toList());
        }

        int rowNum = 1;
        for (Student student : students) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(student.getIenNumber() != null ? student.getIenNumber() : "N/A");
            row.createCell(1).setCellValue(student.getFirstName() + " " + student.getLastName());
            row.createCell(2).setCellValue(student.getDepartment());
            row.createCell(3).setCellValue(student.getYear() != null ? student.getYear() : 0);
            row.createCell(4).setCellValue(student.getEmail() != null ? student.getEmail() : "N/A");
            row.createCell(5).setCellValue(Boolean.TRUE.equals(student.getFaceEnrolled()) ? "Yes" : "No");
        }

        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createAnalyticsSheet(Sheet sheet, String department) {
        List<Student> students = studentRepository.findAll();
        List<Attendance> attendance = attendanceRepository.findAll();

        if (department != null && !department.isEmpty()) {
            students = students.stream()
                    .filter(s -> department.equals(s.getDepartment()))
                    .collect(Collectors.toList());
            attendance = attendance.stream()
                    .filter(a -> a.getStudent() != null && 
                            department.equals(a.getStudent().getDepartment()))
                    .collect(Collectors.toList());
        }

        Row titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue("Analytics & Insights");

        int rowNum = 2;
        sheet.createRow(rowNum++).createCell(0).setCellValue("Total Students: " + students.size());
        sheet.createRow(rowNum++).createCell(0).setCellValue("Total Attendance Records: " + attendance.size());
        
        double avgRate = students.isEmpty() ? 0 : (double) attendance.size() / (students.size() * 50) * 100;
        sheet.createRow(rowNum++).createCell(0).setCellValue(
                "Average Attendance Rate: " + String.format("%.2f%%", avgRate));

        long faceEnrolled = students.stream().filter(s -> Boolean.TRUE.equals(s.getFaceEnrolled())).count();
        sheet.createRow(rowNum++).createCell(0).setCellValue("Face Enrolled: " + faceEnrolled);

        sheet.autoSizeColumn(0);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
