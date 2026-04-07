package com.example.demo.Controller;

import com.example.demo.Model.HealthEvent;
import com.example.demo.Repository.HealthEventRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/health-events")
@CrossOrigin
public class HealthEventController {

    private final HealthEventRepository healthEventRepository;

    public HealthEventController(HealthEventRepository healthEventRepository) {
        this.healthEventRepository = healthEventRepository;
    }

    @GetMapping("/{type}")
    public List<HealthEvent> getByType(@PathVariable String type) {
        return healthEventRepository.findByType(type);
    }

    @GetMapping
    public List<HealthEvent> getAll() {
        return healthEventRepository.findAll();
    }
}