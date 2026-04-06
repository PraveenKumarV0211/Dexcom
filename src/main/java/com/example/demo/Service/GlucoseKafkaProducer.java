package com.example.demo.Service;

import com.example.demo.Model.Glucose;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class GlucoseKafkaProducer {

    private final KafkaTemplate<String, Glucose> kafkaTemplate;

    public GlucoseKafkaProducer(KafkaTemplate<String, Glucose> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(Glucose glucose) {
        kafkaTemplate.send("glucose-readings", glucose);
    }
}