package com.example.demo.Controller;

import com.example.demo.Model.Glucose;
import com.example.demo.Service.GlucoseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/glucose")
@CrossOrigin(origins = "*")
public class GlucoseController {

    @Autowired
    GlucoseService glucoseService;

    @GetMapping
    public ResponseEntity<List<Glucose>> getAllGlucoseRecords() {
        List<Glucose> glucoseValues = glucoseService.getReadings();
        return new ResponseEntity<>(glucoseValues, HttpStatus.OK);
    }
}

