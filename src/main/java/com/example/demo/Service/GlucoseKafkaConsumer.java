package com.example.demo.Service;

import com.example.demo.Model.Glucose;
import com.example.demo.Repository.GlucoseRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class GlucoseKafkaConsumer {

    private final GlucoseRepository glucoseRepository;

    public GlucoseKafkaConsumer(GlucoseRepository glucoseRepository) {
        this.glucoseRepository = glucoseRepository;
    }

    @KafkaListener(topics = "glucose-readings", groupId = "glucolens-group")
    public void consume(Glucose glucose) {
        glucoseRepository.save(glucose);
        System.out.println("Saved glucose reading: " + glucose.getGlucose() + " at " + glucose.getDateTime());
    }
}