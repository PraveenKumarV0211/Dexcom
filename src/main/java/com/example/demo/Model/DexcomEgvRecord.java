package com.example.demo.Model;

import lombok.Data;

@Data
public class DexcomEgvRecord {
    private String recordId;
    private String systemTime;
    private String displayTime;
    private Integer value;
    private String trend;
    private Double trendRate;
    private String unit;
    private String rateUnit;
}