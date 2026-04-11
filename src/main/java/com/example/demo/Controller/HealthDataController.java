package com.example.demo.Controller;

import com.example.demo.Model.HealthExportPayload;
import com.example.demo.Model.HealthMetric;
import com.example.demo.Service.HealthKafkaProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthDataController {

    private final HealthKafkaProducer healthKafkaProducer;

    public HealthDataController(HealthKafkaProducer healthKafkaProducer) {
        this.healthKafkaProducer = healthKafkaProducer;
    }

    @PostMapping("/api/health-data")
    public ResponseEntity<String> receiveHealthData(@RequestBody HealthExportPayload payload) {
        if (payload.getData() == null || payload.getData().getMetrics() == null) {
            return ResponseEntity.badRequest().body("No metrics found");
        }

        for (HealthMetric metric : payload.getData().getMetrics()) {
            String topic = switch (metric.getName()) {
                case "heart_rate" -> "heart-rate";
                case "step_count" -> "steps";
                case "sleep_analysis" -> "sleep";
                case "resting_heart_rate" -> "resting-heart-rate";
                case "respiratory_rate" -> "respiratory-rate";
                case "active_energy" -> "active-energy";
                case "walking_running_distance" -> "walking-running-distance";
                case "apple_exercise_time" -> "exercise-time";
                default -> "health-events";
            };

            for (Map<String, Object> dataPoint : metric.getData()) {
                dataPoint.put("metric", metric.getName());
                dataPoint.put("units", metric.getUnits());
                healthKafkaProducer.send(topic, dataPoint);
            }

            System.out.println("Produced " + metric.getData().size() + " records to topic: " + topic);
        }

        return ResponseEntity.ok("Received " + payload.getData().getMetrics().size() + " metrics");
    }
}