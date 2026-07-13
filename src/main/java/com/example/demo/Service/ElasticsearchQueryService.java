package com.example.demo.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

@Service
public class ElasticsearchQueryService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String ES_URL = "http://localhost:9200";
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    private String cachedContext = null;
    private long cacheExpiry = 0;

    public String getMultiSignalContext(String startTime, String endTime) {
        if (startTime == null && endTime == null
                && cachedContext != null && System.currentTimeMillis() < cacheExpiry) {
            return cachedContext;
        }
        try {
            Map<String, Object> glucoseStats = getStatsByType("glucose", "value");
            Map<String, Object> hrStats = getStatsByType("heart_rate", "Avg");
            Map<String, Object> stepStats = getStatsByType("steps", "qty");

            StringBuilder context = new StringBuilder();
            context.append("MULTI-SIGNAL HEALTH DATA (recent):\n\n");

            if (glucoseStats != null) {
                context.append("GLUCOSE:\n");
                context.append("  Avg: ").append(fmt(glucoseStats.get("avg"))).append(" mg/dL\n");
                context.append("  Min: ").append(fmt(glucoseStats.get("min"))).append(" mg/dL\n");
                context.append("  Max: ").append(fmt(glucoseStats.get("max"))).append(" mg/dL\n");
                context.append("  Readings: ").append(glucoseStats.get("count")).append("\n\n");
            }

            if (hrStats != null) {
                context.append("HEART RATE:\n");
                context.append("  Avg: ").append(fmt(hrStats.get("avg"))).append(" bpm\n");
                context.append("  Min: ").append(fmt(hrStats.get("min"))).append(" bpm\n");
                context.append("  Max: ").append(fmt(hrStats.get("max"))).append(" bpm\n");
                context.append("  Readings: ").append(hrStats.get("count")).append("\n\n");
            }

            if (stepStats != null) {
                context.append("STEPS:\n");
                context.append("  Total: ").append(fmt(stepStats.get("sum"))).append("\n");
                context.append("  Readings: ").append(stepStats.get("count")).append("\n\n");
            }

            String result = context.toString();
            if (startTime == null && endTime == null) {
                cachedContext = result;
                cacheExpiry = System.currentTimeMillis() + CACHE_TTL_MS;
            }
            return result;
        } catch (Exception e) {
            System.err.println("ES query error: " + e.getMessage());
            return "";
        }
    }

    private Map<String, Object> getStatsByType(String type, String field) {
        try {
            String query = """
                {
                  "size": 0,
                  "query": {
                    "term": {"type": "%s"}
                  },
                  "aggs": {
                    "stats": {
                      "stats": {"field": "%s"}
                    }
                  }
                }
                """.formatted(type, field);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map> response = restTemplate.exchange(
                    ES_URL + "/health-events/_search", HttpMethod.POST,
                    new HttpEntity<>(query, headers), Map.class);

            Map body = response.getBody();
            Map aggs = (Map) body.get("aggregations");
            Map stats = (Map) aggs.get("stats");

            if (((Number) stats.get("count")).intValue() == 0) return null;
            return stats;
        } catch (Exception e) {
            return null;
        }
    }

    private String fmt(Object val) {
        if (val instanceof Double) return String.format("%.1f", (Double) val);
        if (val instanceof Number) return val.toString();
        return String.valueOf(val);
    }
}