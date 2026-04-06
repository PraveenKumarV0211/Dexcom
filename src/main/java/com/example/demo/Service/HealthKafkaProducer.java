package com.example.demo.Service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class HealthKafkaProducer {

    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    public HealthKafkaProducer(KafkaTemplate<String, Map<String, Object>> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(String topic, Map<String, Object> data) {
        kafkaTemplate.send(topic, data);
    }
}