package com.example.demo.Controller;

import com.example.demo.Model.HealthEvent;
import com.example.demo.Repository.HealthEventRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import com.example.demo.Model.Glucose;
import com.example.demo.Repository.GlucoseRepository;
import java.time.*;
import java.util.*;
import java.util.stream.*;

@RestController
@RequestMapping("/api/health-events")
@CrossOrigin
public class HealthEventController {

    private final HealthEventRepository healthEventRepository;
    private final MongoTemplate mongoTemplate;
    private final GlucoseRepository glucoseRepository;

    public HealthEventController(HealthEventRepository healthEventRepository, MongoTemplate mongoTemplate, GlucoseRepository glucoseRepository) {
        this.healthEventRepository = healthEventRepository;
        this.mongoTemplate = mongoTemplate;
        this.glucoseRepository = glucoseRepository;
    }

    @GetMapping("/days")
    public List<String> getAvailableDays() {
        return mongoTemplate.findDistinct(new Query(), "date", HealthEvent.class, String.class)
                .stream()
                .filter(d -> d != null && d.length() >= 10)
                .map(d -> d.substring(0, 10))
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }

    @GetMapping("/{type}")
    public List<HealthEvent> getByType(@PathVariable String type,
                                       @RequestParam(required = false) String date) {
        if (date != null && !date.isEmpty()) {
            return healthEventRepository.findByTypeAndDateStartingWith(type, date);
        }
        return healthEventRepository.findByType(type);
    }

    @GetMapping
    public List<HealthEvent> getAll() {
        return healthEventRepository.findAll();
    }
    @GetMapping("/score/{date}")
    public Map<String, Object> getHealthScore(@PathVariable String date) {
        LocalDate targetDate = LocalDate.parse(date);
        ZonedDateTime startOfDay = targetDate.atStartOfDay(ZoneId.of("America/New_York"));
        ZonedDateTime endOfDay = startOfDay.plusDays(1);
        Date start = Date.from(startOfDay.toInstant());
        Date end = Date.from(endOfDay.toInstant());

        List<Glucose> glucoseReadings = glucoseRepository.findByDateTimeBetween(start, end);
        double glucoseScore = computeGlucoseScore(glucoseReadings);

        List<HealthEvent> dayHR = healthEventRepository.findByTypeAndDateStartingWith("heart_rate", date);
        double hrScore = computeHRScore(dayHR);

        List<HealthEvent> daySteps = healthEventRepository.findByTypeAndDateStartingWith("steps", date);
        List<HealthEvent> dayEnergy = healthEventRepository.findByTypeAndDateStartingWith("active_energy", date);
        double activityScore = computeActivityScore(daySteps, dayEnergy);

        List<HealthEvent> dayResting = healthEventRepository.findByTypeAndDateStartingWith("resting_heart_rate", date);
        double recoveryScore = computeRecoveryScore(dayResting, dayHR);

        double overall = glucoseScore * 0.40 + hrScore * 0.20 + activityScore * 0.25 + recoveryScore * 0.15;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", Math.round(overall));
        result.put("glucose", Map.of("score", Math.round(glucoseScore), "weight", "40%", "label", "Glucose Stability", "readings", glucoseReadings.size()));
        result.put("heartRate", Map.of("score", Math.round(hrScore), "weight", "20%", "label", "Heart Rate", "readings", dayHR.size()));
        result.put("activity", Map.of("score", Math.round(activityScore), "weight", "25%", "label", "Activity", "steps", sumQty(daySteps)));
        result.put("recovery", Map.of("score", Math.round(recoveryScore), "weight", "15%", "label", "Recovery"));
        return result;
    }

    private double computeGlucoseScore(List<Glucose> readings) {
        if (readings.isEmpty()) return 0;
        long inRange = readings.stream().filter(r -> r.getGlucose() >= 70 && r.getGlucose() <= 180).count();
        double tirPercent = (double) inRange / readings.size() * 100;
        double mean = readings.stream().mapToInt(Glucose::getGlucose).average().orElse(0);
        double variance = readings.stream().mapToDouble(v -> Math.pow(v.getGlucose() - mean, 2)).average().orElse(0);
        double stdDev = Math.sqrt(variance);
        double tirScore = Math.min(tirPercent, 100);
        double stabilityScore = Math.max(0, 100 - stdDev * 2);
        return tirScore * 0.6 + stabilityScore * 0.4;
    }

    private double computeHRScore(List<HealthEvent> hrEvents) {
        if (hrEvents.isEmpty()) return 0;
        List<Double> avgs = hrEvents.stream()
                .map(e -> { Object avg = e.getData().get("Avg"); if (avg == null) avg = e.getData().get("avg"); return avg != null ? Double.parseDouble(avg.toString()) : 0.0; })
                .filter(v -> v > 0).collect(Collectors.toList());
        if (avgs.isEmpty()) return 0;
        double meanHR = avgs.stream().mapToDouble(d -> d).average().orElse(0);
        if (meanHR >= 60 && meanHR <= 80) return 100;
        if (meanHR < 60) return Math.max(0, 100 - (60 - meanHR) * 3);
        return Math.max(0, 100 - (meanHR - 80) * 2);
    }

    private double computeActivityScore(List<HealthEvent> steps, List<HealthEvent> energy) {
        int totalSteps = sumQty(steps);
        double stepScore = Math.min((double) totalSteps / 10000 * 100, 100);
        double totalCal = energy.stream().mapToDouble(e -> { Object qty = e.getData().get("qty"); return qty != null ? Double.parseDouble(qty.toString()) : 0; }).sum();
        double calScore = Math.min(totalCal / 500 * 100, 100);
        return stepScore * 0.7 + calScore * 0.3;
    }

    private double computeRecoveryScore(List<HealthEvent> resting, List<HealthEvent> hr) {
        double restingScore = 50;
        if (!resting.isEmpty()) {
            Object qty = resting.get(0).getData().get("qty");
            double restingHR = qty != null ? Double.parseDouble(qty.toString()) : 75;
            if (restingHR <= 60) restingScore = 100;
            else if (restingHR <= 70) restingScore = 80;
            else if (restingHR <= 80) restingScore = 60;
            else restingScore = Math.max(0, 100 - (restingHR - 60) * 2);
        }
        double hrvScore = 50;
        List<Double> avgs = hr.stream()
                .map(e -> { Object avg = e.getData().get("Avg"); if (avg == null) avg = e.getData().get("avg"); return avg != null ? Double.parseDouble(avg.toString()) : 0.0; })
                .filter(v -> v > 0).collect(Collectors.toList());
        if (avgs.size() >= 2) {
            double variability = 0;
            for (int i = 1; i < avgs.size(); i++) variability += Math.abs(avgs.get(i) - avgs.get(i - 1));
            variability /= (avgs.size() - 1);
            hrvScore = Math.min(variability * 5, 100);
        }
        return restingScore * 0.6 + hrvScore * 0.4;
    }

    private int sumQty(List<HealthEvent> events) {
        return (int) events.stream().mapToDouble(e -> { Object qty = e.getData().get("qty"); return qty != null ? Double.parseDouble(qty.toString()) : 0; }).sum();
    }
}