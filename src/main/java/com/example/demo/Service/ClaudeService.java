package com.example.demo.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.function.BiFunction;

@Service
public class ClaudeService {

    @Value("${claude.api.key}")
    private String claudeKey;

    private static final String MODEL      = "claude-sonnet-4-6";
    private static final String URL        = "https://api.anthropic.com/v1/messages";
    private static final int    MAX_TOKENS = 4096;

    private final RestTemplate restTemplate = new RestTemplate();

    /** Single-turn call with no tools — kept for backward compatibility. */
    public String call(String systemPrompt, String userMessage) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", MODEL);
        body.put("max_tokens", MAX_TOKENS);
        body.put("system", systemPrompt);
        body.put("messages", List.of(Map.of("role", "user", "content", userMessage)));

        ResponseEntity<Map> response = restTemplate.postForEntity(URL, toRequest(body), Map.class);
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        return (String) content.get(0).get("text");
    }

    /**
     * Agentic loop: Claude can call tools, we execute them, and feed results back
     * until stop_reason is "end_turn" (max 10 iterations to prevent runaway loops).
     */
    public String chatWithTools(String systemPrompt,
                                 List<Map<String, Object>> messages,
                                 List<Map<String, Object>> tools,
                                 BiFunction<String, Map<String, Object>, String> toolExecutor) {

        List<Map<String, Object>> conversation = new ArrayList<>(messages);

        for (int i = 0; i < 10; i++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", MODEL);
            body.put("max_tokens", MAX_TOKENS);
            body.put("system", systemPrompt);
            body.put("messages", conversation);
            if (tools != null && !tools.isEmpty()) {
                body.put("tools", tools);
            }

            ResponseEntity<Map> response = restTemplate.postForEntity(URL, toRequest(body), Map.class);
            Map<String, Object> respBody = response.getBody();

            String stopReason = (String) respBody.get("stop_reason");
            List<Map<String, Object>> content = (List<Map<String, Object>>) respBody.get("content");

            // Append assistant turn to conversation
            Map<String, Object> assistantMsg = new LinkedHashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", content);
            conversation.add(assistantMsg);

            if ("end_turn".equals(stopReason) || content == null) {
                return extractText(content);
            }

            if ("tool_use".equals(stopReason)) {
                List<Map<String, Object>> toolResults = new ArrayList<>();
                for (Map<String, Object> block : content) {
                    if (!"tool_use".equals(block.get("type"))) continue;

                    String toolName  = (String) block.get("name");
                    String toolUseId = (String) block.get("id");
                    Map<String, Object> toolInput = (Map<String, Object>) block.get("input");

                    String result = toolExecutor.apply(toolName, toolInput != null ? toolInput : Map.of());

                    Map<String, Object> toolResult = new LinkedHashMap<>();
                    toolResult.put("type",        "tool_result");
                    toolResult.put("tool_use_id", toolUseId);
                    toolResult.put("content",     result);
                    toolResults.add(toolResult);
                }

                Map<String, Object> userMsg = new LinkedHashMap<>();
                userMsg.put("role",    "user");
                userMsg.put("content", toolResults);
                conversation.add(userMsg);
                continue;
            }

            // Any other stop reason (e.g. max_tokens) — return whatever text we have
            return extractText(content);
        }

        return "I was unable to complete the response after multiple tool calls.";
    }

    private String extractText(List<Map<String, Object>> content) {
        if (content == null) return "";
        return content.stream()
                .filter(b -> "text".equals(b.get("type")))
                .map(b -> (String) b.get("text"))
                .findFirst()
                .orElse("");
    }

    private HttpEntity<Map<String, Object>> toRequest(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", claudeKey);
        headers.set("anthropic-version", "2023-06-01");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
