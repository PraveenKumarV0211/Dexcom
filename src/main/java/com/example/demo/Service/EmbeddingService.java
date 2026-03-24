package com.example.demo.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    @Value("${huggingface.api.token}")
    private String hfToken;

    private static final String HF_URL = "https://router.huggingface.co/hf-inference/models/sentence-transformers/all-MiniLM-L6-v2/pipeline/feature-extraction";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public List<Double> getEmbedding(String text) throws Exception {
        String body = mapper.writeValueAsString(Map.of("inputs", text));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HF_URL))
                .header("Authorization", "Bearer " + hfToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return mapper.readValue(response.body(), List.class);
    }
}