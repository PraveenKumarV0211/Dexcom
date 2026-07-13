package com.example.demo.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class ClaudeService {

    @Value("${claude.api.key}")
    private String claudeKey;

    private static final String MODEL = "claude-sonnet-4-6";
    private static final int MAX_TOKENS = 2048;

    private final RestTemplate restTemplate = new RestTemplate();

    public String call(String systemPrompt, String userMessage) {
        String url = "https://api.anthropic.com/v1/messages";

        Map<String, Object> body = Map.of(
                "model", MODEL,
                "max_tokens", MAX_TOKENS,
                "system", systemPrompt,
                "messages", List.of(
                        Map.of("role", "user", "content", userMessage)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", claudeKey);
        headers.set("anthropic-version", "2023-06-01");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        return (String) content.get(0).get("text");
    }
}