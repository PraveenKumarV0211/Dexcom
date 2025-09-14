package com.example.demo.Service;

import com.example.demo.Model.Glucose;
import com.example.demo.Model.GlucoseRangeCount;
import com.example.demo.Repository.GlucoseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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

    public Double getGlucoseStandardDeviation() {
        return repository.findStandardDeviation();
    }

    public Double getTodayAverage() {
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = LocalDate.now().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();
        return repository.findAverageReadingValueForToday(Date.from(startOfDay), Date.from(endOfDay));
    }

    public List<Glucose> getReadingsByDuration(Integer hours) {
        List<Glucose> allReadings = repository.findAll();

        if (allReadings.isEmpty()) {
            return new ArrayList<>();
        }

        Date currentTime = new Date();

        // Filter readings within the time window
        List<Glucose> filteredReadings = allReadings.stream()
                .filter(reading -> {
                    if (reading.getDateTime() == null) return false;

                    // Calculate time difference in hours
                    long diffInMillies = Math.abs(currentTime.getTime() - reading.getDateTime().getTime());
                    long diffInHours = diffInMillies / (60 * 60 * 1000);

                    return diffInHours <= hours;
                })
                .sorted((r1, r2) -> {
                    // Sort by DateTime ascending
                    if (r1.getDateTime() == null) return 1;
                    if (r2.getDateTime() == null) return -1;
                    return r1.getDateTime().compareTo(r2.getDateTime());
                })
                .collect(Collectors.toList());

        return filteredReadings;
    }

    public GlucoseRangeCount getRangeCount() {
        List<Glucose> glucoseValues = getReadings();
        int low = 0;
        int mid = 0;
        int high = 0;
        int veryHigh = 0;
        for (Glucose glucose : glucoseValues) {
            if (glucose != null && glucose.getGlucose() != null) {
                double value = glucose.getGlucose();
                if (value < 120) low++;
                else if (value >= 120 && value <= 180) mid++;
                else if (value > 180 && value <= 250) high++;
                else if (value > 250) veryHigh++;
            }
        }
        return new GlucoseRangeCount(low, mid, high, veryHigh);
    }
}
