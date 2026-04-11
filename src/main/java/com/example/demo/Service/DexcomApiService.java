package com.example.demo.Service;

import com.example.demo.Model.DexcomEgvRecord;
import com.example.demo.Model.DexcomEgvResponse;
import com.example.demo.Model.Glucose;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

@Service
public class DexcomApiService {

    private final GlucoseKafkaProducer kafkaProducer;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${dexcom.client-id}")
    private String clientId;

    @Value("${dexcom.client-secret}")
    private String clientSecret;

    @Value("${dexcom.redirect-uri}")
    private String redirectUri;

    @Value("${dexcom.base-url}")
    private String baseUrl;

    private String accessToken;
    private String refreshToken;

    public String getClientId() {
        return clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public DexcomApiService(GlucoseKafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    public String exchangeCode(String code) {
        String tokenUrl = baseUrl + "/v3/oauth2/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", redirectUri);

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        Map<String, Object> tokenResponse = response.getBody();
        this.accessToken = (String) tokenResponse.get("access_token");
        this.refreshToken = (String) tokenResponse.get("refresh_token");

        return "Token obtained successfully";
    }

    private void refreshAccessToken() {
        String tokenUrl = baseUrl + "/v3/oauth2/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", this.refreshToken);
        body.add("grant_type", "refresh_token");
        body.add("redirect_uri", redirectUri);

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        Map<String, Object> tokenResponse = response.getBody();
        this.accessToken = (String) tokenResponse.get("access_token");
        this.refreshToken = (String) tokenResponse.get("refresh_token");
    }

    @Scheduled(fixedRate = 300000) // 5 minutes
    public void pollDexcomApi() {
        if (accessToken == null) {
            System.out.println("No access token. Authorize first via /api/dexcom/authorize");
            return;
        }

        try {
            Instant now = Instant.now();
            Instant start = now.minus(10, ChronoUnit.MINUTES);
            String startDate = start.toString().replace("Z", "");
            String endDate = now.toString().replace("Z", "");

            String url = baseUrl + "/v3/users/self/egvs?startDate=" + startDate + "&endDate=" + endDate;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            ResponseEntity<DexcomEgvResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), DexcomEgvResponse.class);

            DexcomEgvResponse egvResponse = response.getBody();
            if (egvResponse != null && egvResponse.getRecords() != null) {
                for (DexcomEgvRecord record : egvResponse.getRecords()) {
                    Glucose glucose = new Glucose(
                            Date.from(Instant.parse(record.getSystemTime())),
                            record.getValue()
                    );
                    kafkaProducer.send(glucose);
                    System.out.println("Produced to Kafka: " + record.getValue() + " mg/dL at " + record.getSystemTime());
                }
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                System.out.println("Token expired, refreshing...");
                refreshAccessToken();
            } else {
                System.err.println("Dexcom poll error: " + e.getMessage());
            }
        }
    }
}