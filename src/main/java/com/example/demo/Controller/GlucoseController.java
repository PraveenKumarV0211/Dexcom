package com.example.demo.Controller;

import com.example.demo.Model.Glucose;
import com.example.demo.Model.GlucoseRangeCount;
import com.example.demo.Service.GlucoseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@CrossOrigin(origins = "*")
public class GlucoseController {

    @Autowired
    GlucoseService glucoseService;

    @GetMapping(value = "/allData")
    public ResponseEntity<List<Glucose>> getAllGlucoseRecords() {
        List<Glucose> glucoseValues = glucoseService.getReadings();
        if (glucoseValues.isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(glucoseValues, HttpStatus.OK);
    }

    @GetMapping(value = "/currentDayAverage")
    public ResponseEntity<Double> getCurrentDayAverage() {
        Double currentDayAverage = glucoseService.getTodayAverage();
        if (currentDayAverage != null) {
            return new ResponseEntity<>(currentDayAverage, HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

    @GetMapping(value = "/glucoseByDuration")
    public ResponseEntity<List<Glucose>> getGlucoseByDuration(@RequestParam(value = "hours", defaultValue = "24") Integer hours) {
        List<Glucose> glucoseValues = glucoseService.getReadingsByDuration(hours);
        if (glucoseValues.isEmpty()) {
            return new ResponseEntity<>(glucoseValues, HttpStatus.OK);
        }
        return new ResponseEntity<>(glucoseValues, HttpStatus.OK);
    }

    @GetMapping(value = "/overallAverage")
    public ResponseEntity<Double> getOverallAverage() {
        Double overallAverage = glucoseService.getOverallAverageGlucose();
        if (overallAverage != null) {
            return new ResponseEntity<>(overallAverage, HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }

    @GetMapping(value = "/getGMI")
    public ResponseEntity<Double> getGMI() {
        Double glucoseManagementIndicator = glucoseService.getGlucoseManagementIndicator();
        if (glucoseManagementIndicator != null) {
            return new ResponseEntity<>(glucoseManagementIndicator, HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }

    @GetMapping(value = "/getStandardDeviation")
    public ResponseEntity<Double> getStandardDeviation() {
        Double standardDeviation = glucoseService.getGlucoseStandardDeviation();
        if (standardDeviation != null) {
            return new ResponseEntity<>(standardDeviation, HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }


    @GetMapping(value = "/getDataForPieChart")
    public ResponseEntity<GlucoseRangeCount> getDataForPieChart() {
        GlucoseRangeCount rangeCount = glucoseService.getRangeCount();
        return new ResponseEntity<>(rangeCount, HttpStatus.OK);
    }

}

