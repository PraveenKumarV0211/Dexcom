package com.example.demo.Repository;

import com.example.demo.Model.HealthEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface HealthEventRepository extends MongoRepository<HealthEvent, String> {
    List<HealthEvent> findByType(String type);
}