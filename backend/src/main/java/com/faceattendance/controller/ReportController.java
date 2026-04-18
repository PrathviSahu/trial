package com.faceattendance.controller;

import com.faceattendance.service.PDFReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private PDFReportService pdfReportService;

    /**
     * Generate comprehensive Excel report
     */
    @GetMapping("/excel/comprehensive")
    public ResponseEntity<byte[]> generateComprehensiveReport(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            byte[] excelData = pdfReportService.generateExcelReport(department, startDate, endDate);
            
            String filename = "FaceTrackU_Report_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Generate summary report
     */
    @GetMapping("/excel/summary")
    public ResponseEntity<byte[]> generateSummaryReport() {
        try {
            byte[] excelData = pdfReportService.generateSummaryReport();
            
            String filename = "Attendance_Summary_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Generate department-specific report
     */
    @GetMapping("/excel/department/{department}")
    public ResponseEntity<byte[]> generateDepartmentReport(@PathVariable String department) {
        try {
            byte[] excelData = pdfReportService.generateDepartmentReport(department);
            
            String filename = department + "_Report_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get available report formats
     */
    @GetMapping("/formats")
    public ResponseEntity<?> getAvailableFormats() {
        return ResponseEntity.ok(new String[]{
            "Excel (XLSX) - Comprehensive Report",
            "Excel (XLSX) - Summary Report", 
            "Excel (XLSX) - Department Report",
            "CSV - Coming Soon",
            "PDF - Coming Soon"
        });
    }
}
