package com.example.demo.Service;

import com.example.demo.Model.Glucose;
import com.example.demo.Model.GlucoseRangeCount;
import com.example.demo.Repository.GlucoseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
        Double result = repository.findOverallAverageGlucose();
        if (result == null) return null;
        return Math.round(result * 100.0) / 100.0;
    }

    public Double getGlucoseManagementIndicator() {
        Double average = getOverallAverageGlucose();
        Double result = 3.31 + (0.02392 * average);
        return Math.round(result * 100.0) / 100.0;
    }

    public Double getGlucoseStandardDeviation() {
        Double result = repository.findStandardDeviation();
        if (result == null) return null;
        return Math.round(result * 100.0) / 100.0;
    }

    public Double getTodayAverage() {
        ZoneId zone = ZoneId.of("UTC");
        Instant startOfDay = LocalDate.now(zone)
                .atStartOfDay(zone)
                .toInstant();
        Instant endOfDay = LocalDate.now(zone)
                .atTime(LocalTime.MAX)
                .atZone(zone)
                .toInstant();

        Double result = repository.findAverageReadingValueForToday(Date.from(startOfDay), Date.from(endOfDay));
        if (result == null) return null;
        return Math.round(result * 100.0) / 100.0;
    }

    public List<Glucose> getReadingsByDuration(Integer hours) {
        Date[] durationWindow = getDurationWindow(hours);
        return repository.findReadingsByDateTimeBetweenOrderByDateTimeAsc(durationWindow[0], durationWindow[1]);
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

    public Double getAverageByDuration(Integer hours) {
        List<Glucose> glucoseInRange = getReadingsByDuration(hours);
        if (glucoseInRange.isEmpty()) {
            return 0.0;
        }
        double result = glucoseInRange.stream()
                .filter(reading -> reading.getGlucose() != null)
                .mapToDouble(Glucose::getGlucose)
                .average()
                .orElse(0.0);
        return Math.round(result * 100.0) / 100.0;
    }


    public Page<Glucose> getPaginatedData(Date startDate, Date endDate, int page, int size) {
        return repository.findByDateTimeBetween(startDate, endDate, PageRequest.of(page, size));
    }

    public boolean addGlucoseValue(int value, Date time) {
        boolean result = true;
        try {
            repository.save(new Glucose(time, value));
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    private Date[] getDurationWindow(Integer hours) {
        int safeHours = hours == null ? 24 : Math.max(hours, 0);
        Date end = new Date();
        Date start = new Date(end.getTime() - (safeHours * 60L * 60L * 1000L));
        return new Date[]{start, end};
    }
}
