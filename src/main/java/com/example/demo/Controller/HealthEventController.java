package com.example.demo.Controller;

import com.example.demo.Model.HealthEvent;
import com.example.demo.Repository.HealthEventRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/health-events")
@CrossOrigin
public class HealthEventController {

    private final HealthEventRepository healthEventRepository;
    private final MongoTemplate mongoTemplate;

    public HealthEventController(HealthEventRepository healthEventRepository, MongoTemplate mongoTemplate) {
        this.healthEventRepository = healthEventRepository;
        this.mongoTemplate = mongoTemplate;
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
}