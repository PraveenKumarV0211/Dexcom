package com.example.demo.Controller;

import com.example.demo.Service.ElasticsearchQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/health-insights")
@CrossOrigin
public class HealthInsightController {

    private final ElasticsearchQueryService esQueryService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${grok.api.token}")
    private String groqApiKey;

    public HealthInsightController(ElasticsearchQueryService esQueryService) {
        this.esQueryService = esQueryService;
    }

    @GetMapping("/{date}")
    public Map<String, String> getInsight(@PathVariable String date) {
        String context = esQueryService.getMultiSignalContext(date + "T00:00:00", date + "T23:59:59");

        String prompt = "You are a health analytics AI for a glucose monitoring platform. "
                + "Analyze the following multi-signal health data for date: " + date + ".\n\n"
                + context + "\n\n"
                + "Provide a JSON response with exactly these 3 fields (no markdown, no code fences, raw JSON only):\n"
                + "{\n"
                + "  \"summary\": \"2-3 sentence daily health summary correlating glucose, heart rate, steps, and active energy\",\n"
                + "  \"anomalies\": \"Any unusual patterns or flags detected (or 'No anomalies detected' if none)\",\n"
                + "  \"tips\": \"2-3 personalized actionable tips for exercise, recovery, or lifestyle based on the data\"\n"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> message = Map.of("role", "user", "content", prompt);
        Map<String, Object> body = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(message),
                "temperature", 0.3,
                "max_tokens", 500
        );

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.groq.com/openai/v1/chat/completions",
                    HttpMethod.POST, request, Map.class);

            Map responseBody = response.getBody();
            List<Map> choices = (List<Map>) responseBody.get("choices");
            Map messageResp = (Map) choices.get(0).get("message");
            String content = (String) messageResp.get("content");

            content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            return Map.of("insight", content);
        } catch (Exception e) {
            return Map.of("insight", "{\"summary\":\"Unable to generate insights at this time.\",\"anomalies\":\"Service unavailable\",\"tips\":\"Please try again later.\"}");
        }
    }
}