package com.example.demo.Service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import com.example.demo.Model.Glucose;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

@Service
public class ElasticsearchConsumer {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String ES_URL = "http://localhost:9200";

    @KafkaListener(topics = "glucose-readings", groupId = "es-indexer-group")
    public void indexGlucose(Glucose glucose) { 
        Map<String, Object> doc = new HashMap<>();
        doc.put("type", "glucose");
        doc.put("value", glucose.getGlucose());
        doc.put("dateTime", glucose.getDateTime());
        index("health-events", doc);
    }

    @KafkaListener(topics = "heart-rate", groupId = "es-indexer-group")
    public void indexHeartRate(Map<String, Object> record) {
        Map<String, Object> doc = new HashMap<>(record);
        doc.put("type", "heart_rate");
        index("health-events", doc);
    }

    @KafkaListener(topics = "steps", groupId = "es-indexer-group")
    public void indexSteps(Map<String, Object> record) {
        Map<String, Object> doc = new HashMap<>(record);
        doc.put("type", "steps");
        index("health-events", doc);
    }

    @KafkaListener(topics = "sleep", groupId = "es-indexer-group")
    public void indexSleep(Map<String, Object> record) {
        Map<String, Object> doc = new HashMap<>(record);
        doc.put("type", "sleep");
        index("health-events", doc);
    }

    private void index(String indexName, Map<String, Object> doc) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(doc, headers);
            restTemplate.postForEntity(ES_URL + "/" + indexName + "/_doc", request, String.class);
        } catch (Exception e) {
            System.err.println("ES index error: " + e.getMessage());
        }
    }
}