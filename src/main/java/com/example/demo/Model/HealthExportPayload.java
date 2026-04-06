package com.example.demo.Model;

import lombok.Data;
import java.util.List;

@Data
public class HealthExportPayload {
    private Data data;

    @lombok.Data
    public static class Data {
        private List<HealthMetric> metrics;
    }
}