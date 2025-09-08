package com.example.demo.Controller;

import com.example.demo.Model.Glucose;
import com.example.demo.Service.GlucoseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }

    @GetMapping(value = "/overallAverage")
    public ResponseEntity<Double> getOverallAverage() {
        Double overallAverage = glucoseService.getOverallAverageGlucose();
        if (overallAverage != null) {
            return new ResponseEntity<>(overallAverage, HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }
}

