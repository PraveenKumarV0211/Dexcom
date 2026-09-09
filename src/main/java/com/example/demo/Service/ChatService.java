package com.example.demo.Service;

import com.example.demo.Model.KnowledgeDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ChatService {

    @Autowired
    private ClaudeService claudeService;

    @Autowired
    private MongoToolService mongoToolService;

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private GroqService groqService;

    @Autowired
    private ElasticsearchQueryService esQueryService;

    public Map<String, String> chat(String userQuestion, List<Map<String, String>> history, String summary) {
        String updatedSummary = updateSummary(summary, history);

        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String multiSignal = esQueryService.getMultiSignalContext(null, null);

        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("You are a personalized diabetic health assistant. Today's date is ").append(today).append(".\n\n");
        systemPrompt.append("You have access to tools to fetch the user's real health data from their MongoDB database:\n");
        systemPrompt.append("- Glucose readings from their Dexcom CGM sensor\n");
        systemPrompt.append("- Food logs with nutrition information\n");
        systemPrompt.append("- Personal health insights from past AI analyses\n\n");

        if (!multiSignal.isEmpty()) {
            systemPrompt.append(multiSignal).append("\n");
        }

        systemPrompt.append("GUIDELINES:\n");
        systemPrompt.append("- Always fetch real data using tools before answering any health or data question\n");
        systemPrompt.append("- For food questions: call search_food_logs first, then get_glucose_around_meal for each result\n");
        systemPrompt.append("- For time-based questions: use get_glucose_readings with the appropriate date range\n");
        systemPrompt.append("- For pattern questions: use get_time_of_day_patterns or get_meal_type_patterns\n");
        systemPrompt.append("- Search the knowledge_base for relevant past insights on any topic\n");
        systemPrompt.append("- Lead with key findings using actual numbers from the data\n");
        systemPrompt.append("- Use **bold** for important numbers and findings\n");
        systemPrompt.append("- Be concise and give 1-2 actionable suggestions max\n");
        systemPrompt.append("- If no data is found, say so clearly rather than guessing\n");

        List<Map<String, Object>> messages = new ArrayList<>();
        String historyContext = buildHistoryContext(history, summary);
        String userContent = historyContext.isEmpty()
                ? userQuestion
                : historyContext + "Current question: " + userQuestion;
        messages.add(Map.of("role", "user", "content", userContent));

        List<Map<String, Object>> tools = mongoToolService.getToolDefinitions();
        String answer = claudeService.chatWithTools(systemPrompt.toString(), messages, tools, mongoToolService::executeTool);

        autoSaveFinding(userQuestion, answer);
        return Map.of("answer", answer, "updatedSummary", updatedSummary);
    }

    private String updateSummary(String existingSummary, List<Map<String, String>> history) {
        if (history == null || history.size() < 6) return existingSummary != null ? existingSummary : "";
        String oldestExchange = "User: " + history.get(0).get("content") + "\n"
                              + "Assistant: " + history.get(1).get("content");
        String input = (existingSummary == null || existingSummary.isEmpty())
                ? oldestExchange
                : "Existing summary: " + existingSummary + "\n\nNew exchange to incorporate:\n" + oldestExchange;
        try {
            return groqService.call(
                    "Update the conversation summary by incorporating the new exchange. " +
                    "2-3 sentences max. Focus on health insights discussed. Reply with ONLY the updated summary.",
                    input
            );
        } catch (Exception e) {
            return existingSummary != null ? existingSummary : "";
        }
    }

    private void autoSaveFinding(String question, String answer) {
        try {
            String finding = groqService.call(
                    "Summarize the key finding from this Q&A in one short sentence. " +
                    "Focus on the personal health insight. Respond with ONLY the sentence, nothing else.",
                    "Question: " + question + "\nAnswer: " + answer
            );
            if (finding != null && finding.length() > 10) {
                KnowledgeDocument saved = knowledgeService.save(finding.trim(), "auto_insight", "chat_history");
                System.out.println("Saved to knowledge_base: " + saved.getId());
            }
        } catch (Exception e) {
            System.out.println("Auto-save failed: " + e.getMessage());
        }
    }

    private String buildHistoryContext(List<Map<String, String>> history, String summary) {
        boolean hasSummary = summary != null && !summary.isEmpty();
        boolean hasHistory = history != null && history.size() > 1;

        if (!hasSummary && !hasHistory) return "";

        StringBuilder sb = new StringBuilder();
        if (hasSummary) {
            sb.append("CONVERSATION SUMMARY:\n").append(summary).append("\n\n");
        }
        if (hasHistory) {
            sb.append(hasSummary ? "RECENT CONVERSATION:\n" : "CONVERSATION HISTORY:\n");
            for (int i = 0; i < history.size() - 1; i++) {
                Map<String, String> msg = history.get(i);
                String role = msg.get("role");
                sb.append(role.equals("user") ? "User" : "Assistant")
                        .append(": ").append(msg.get("content")).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}