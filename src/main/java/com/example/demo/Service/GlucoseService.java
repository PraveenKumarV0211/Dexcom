package com.example.demo.Service;

import com.example.demo.Model.Glucose;
import com.example.demo.Repository.GlucoseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
public class GlucoseService {

    @Autowired
    GlucoseRepository repository;

    public List<Glucose> getReadings() {
        return repository.findAll();
    }

    public Double getOverallAverageGlucose() {
        return repository.findOverallAverageGlucose();
    }

    public Double getGlucoseManagementIndicator() {
        Double average = getOverallAverageGlucose();
        return (3.31 + 0.02392) * average;
    }

    public Double getGlucoseStandardDeviation(){
        return repository.findStandardDeviation();
    }

    public Double getTodayAverage() {
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = LocalDate.now().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();
        return repository.findAverageReadingValueForToday(Date.from(startOfDay), Date.from(endOfDay));
    }

}
