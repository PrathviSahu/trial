package com.faceattendance.controller;

import com.faceattendance.service.AttendancePredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/predictions")
@CrossOrigin(origins = "*")
public class PredictionController {

    @Autowired
    private AttendancePredictionService predictionService;

    /**
     * Get students predicted to be absent
     */
    @GetMapping("/absent-students")
    public ResponseEntity<Map<String, Object>> getPredictedAbsentStudents() {
        try {
            List<Map<String, Object>> predictions = predictionService.predictAbsentStudents();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", predictions);
            response.put("count", predictions.size());
            response.put("message", "Successfully predicted " + predictions.size() + " at-risk students");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error generating predictions: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get best days for important lectures
     */
    @GetMapping("/best-days")
    public ResponseEntity<Map<String, Object>> getBestDaysForLectures() {
        try {
            Map<String, Object> predictions = predictionService.predictBestDaysForLectures();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", predictions);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error analyzing day patterns: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get attendance trend forecast
     */
    @GetMapping("/forecast")
    public ResponseEntity<Map<String, Object>> getAttendanceForecast() {
        try {
            Map<String, Object> forecast = predictionService.forecastAttendanceTrend();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", forecast);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error generating forecast: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get comprehensive attendance insights
     */
    @GetMapping("/insights")
    public ResponseEntity<Map<String, Object>> getAttendanceInsights() {
        try {
            Map<String, Object> insights = predictionService.getAttendanceInsights();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", insights);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error generating insights: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get all predictions in one call
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllPredictions() {
        try {
            Map<String, Object> allPredictions = new HashMap<>();
            allPredictions.put("absentStudents", predictionService.predictAbsentStudents());
            allPredictions.put("bestDays", predictionService.predictBestDaysForLectures());
            allPredictions.put("forecast", predictionService.forecastAttendanceTrend());
            allPredictions.put("insights", predictionService.getAttendanceInsights());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", allPredictions);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error generating predictions: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
