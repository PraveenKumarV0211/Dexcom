package com.example.demo.Service;

import com.example.demo.Model.HealthEvent;
import com.example.demo.Repository.HealthEventRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class HealthKafkaConsumer {

    private final HealthEventRepository healthEventRepository;

    public HealthKafkaConsumer(HealthEventRepository healthEventRepository) {
        this.healthEventRepository = healthEventRepository;
    }

    @KafkaListener(topics = "heart-rate", groupId = "health-mongo-group")
    public void consumeHeartRate(Map<String, Object> record) {
        saveEvent("heart_rate", record);
    }

    @KafkaListener(topics = "steps", groupId = "health-mongo-group")
    public void consumeSteps(Map<String, Object> record) {
        saveEvent("steps", record);
    }

    @KafkaListener(topics = "sleep", groupId = "health-mongo-group")
    public void consumeSleep(Map<String, Object> record) {
        saveEvent("sleep", record);
    }
    @KafkaListener(topics = "resting-heart-rate", groupId = "health-mongo-group")
    public void consumeRestingHR(Map<String, Object> record) {
        saveEvent("resting_heart_rate", record);
    }

    @KafkaListener(topics = "respiratory-rate", groupId = "health-mongo-group")
    public void consumeRespiratoryRate(Map<String, Object> record) {
        saveEvent("respiratory_rate", record);
    }

    @KafkaListener(topics = "active-energy", groupId = "health-mongo-group")
    public void consumeActiveEnergy(Map<String, Object> record) {
        saveEvent("active_energy", record);
    }

    @KafkaListener(topics = "walking-running-distance", groupId = "health-mongo-group")
    public void consumeDistance(Map<String, Object> record) {
        saveEvent("walking_running_distance", record);
    }

    private void saveEvent(String type, Map<String, Object> record) {
        try {
            HealthEvent event = new HealthEvent();
            event.setType(type);
            event.setUnits((String) record.get("units"));
            event.setDate((String) record.get("date"));
            event.setData(record);
            healthEventRepository.save(event);
        } catch (org.springframework.dao.DuplicateKeyException e) {

        }
    }
}