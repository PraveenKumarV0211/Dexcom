package com.example.demo.Model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class HealthMetric {
    private String name;
    private String units;
    private List<Map<String, Object>> data;
}