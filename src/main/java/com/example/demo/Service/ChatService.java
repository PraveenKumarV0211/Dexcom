package com.example.demo.Service;

import com.example.demo.Model.ChatIntent;
import com.example.demo.Model.FoodLog;
import com.example.demo.Model.Glucose;
import com.example.demo.Model.KnowledgeDocument;
import com.example.demo.Repository.FoodLogRepository;
import com.example.demo.Repository.GlucoseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Calendar;

@Service
public class ChatService {

    @Autowired
    private FoodLogRepository foodLogRepo;

    @Autowired
    private GlucoseRepository glucoseRepo;

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private GeminiService geminiService;

    public String chat(String userQuestion, List<Map<String, String>> history) {
        String lowerQuestion = userQuestion.toLowerCase();

        // Handle last reading
        if (lowerQuestion.contains("last reading") || lowerQuestion.contains("latest reading")
                || lowerQuestion.contains("current reading") || lowerQuestion.contains("recent reading")) {
            Glucose latest = glucoseRepo.findTopByOrderByDateTimeDesc();
            if (latest != null) {
                String dataPrompt = "USER DATA:\nLatest glucose reading: " + latest.getGlucose()
                        + " mg/dL at " + latest.getDateTime() + "\n\n"
                        + buildHistoryContext(history)
                        + "User question: " + userQuestion;
                List<String> knowledge = List.of();
                try { knowledge = knowledgeService.search("glucose reading", 3); } catch (Exception e) {}
                return geminiService.call(buildSystemPrompt(knowledge), dataPrompt);
            }
        }

        // Handle personal health questions
        if (lowerQuestion.contains("complication") || lowerQuestion.contains("condition")
                || lowerQuestion.contains("about me") || lowerQuestion.contains("my profile")) {
            Date weekStart = toDate(LocalDateTime.now().minusDays(7));
            Date now = toDate(LocalDateTime.now());
            List<Glucose> recentReadings = glucoseRepo.findByDateTimeBetween(weekStart, now);

            List<String> knowledge = List.of();
            try { knowledge = knowledgeService.search("diabetic complications health profile", 5); } catch (Exception e) {}

            StringBuilder dataPrompt = new StringBuilder();
            if (!recentReadings.isEmpty()) {
                double avg = recentReadings.stream().mapToInt(Glucose::getGlucose).average().orElse(0);
                int max = recentReadings.stream().mapToInt(Glucose::getGlucose).max().orElse(0);
                int min = recentReadings.stream().mapToInt(Glucose::getGlucose).min().orElse(0);
                long aboveTarget = recentReadings.stream().filter(r -> r.getGlucose() > 180).count();
                double abovePct = (aboveTarget * 100.0) / recentReadings.size();

                dataPrompt.append("USER DATA (last 7 days):\n");
                dataPrompt.append("Average: ").append(String.format("%.1f", avg)).append(" mg/dL\n");
                dataPrompt.append("Min: ").append(min).append(" mg/dL\n");
                dataPrompt.append("Max: ").append(max).append(" mg/dL\n");
                dataPrompt.append("Time above 180 mg/dL: ").append(String.format("%.1f", abovePct)).append("%\n\n");
            }
            dataPrompt.append(buildHistoryContext(history));
            dataPrompt.append("User question: ").append(userQuestion);
            return geminiService.call(buildSystemPrompt(knowledge), dataPrompt.toString());
        }

        // Extract intent using LLM
        ChatIntent intent = extractIntent(userQuestion);

        List<FoodLog> meals = List.of();

        // Food specific
        if ("food_specific".equals(intent.getType()) && intent.getFood() != null && !intent.getFood().isEmpty()) {
            List<FoodLog> allMatches = foodLogRepo.findByFoodNameRegex(intent.getFood());
            meals = allMatches.stream().limit(intent.getLimit()).collect(Collectors.toList());

            Map<FoodLog, List<Glucose>> glucoseMap = new LinkedHashMap<>();
            for (FoodLog meal : meals) {
                Date from = toDate(meal.getTimestamp().minusHours(1));
                Date to = toDate(meal.getTimestamp().plusHours(3));
                glucoseMap.put(meal, glucoseRepo.findByDateTimeBetween(from, to));
            }

            List<String> knowledge = List.of();
            try { knowledge = knowledgeService.search(intent.getFood() + " glucose spike", 5); } catch (Exception e) {}

            String systemPrompt = buildSystemPrompt(knowledge);
            String dataPrompt = buildDataPrompt(glucoseMap, userQuestion, history);
            String answer = geminiService.call(systemPrompt, dataPrompt);
            autoSaveFinding(userQuestion, answer);
            return answer;
        }
        // Time of day analysis
        if ("time_of_day".equals(intent.getType())) {
            Date monthStart = toDate(LocalDateTime.now().minusDays(30));
            Date now = toDate(LocalDateTime.now());
            List<Glucose> allReadings = glucoseRepo.findByDateTimeBetween(monthStart, now);

            // Group by time of day
            Map<String, List<Integer>> grouped = new LinkedHashMap<>();
            grouped.put("Morning (6AM-12PM)", new ArrayList<>());
            grouped.put("Afternoon (12PM-5PM)", new ArrayList<>());
            grouped.put("Evening (5PM-9PM)", new ArrayList<>());
            grouped.put("Night (9PM-6AM)", new ArrayList<>());

            for (Glucose r : allReadings) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(r.getDateTime());
                int hour = cal.get(Calendar.HOUR_OF_DAY);

                if (hour >= 6 && hour < 12) grouped.get("Morning (6AM-12PM)").add(r.getGlucose());
                else if (hour >= 12 && hour < 17) grouped.get("Afternoon (12PM-5PM)").add(r.getGlucose());
                else if (hour >= 17 && hour < 21) grouped.get("Evening (5PM-9PM)").add(r.getGlucose());
                else grouped.get("Night (9PM-6AM)").add(r.getGlucose());
            }

            StringBuilder dataPrompt = new StringBuilder();
            dataPrompt.append("USER TIME-OF-DAY GLUCOSE PATTERNS (last 30 days):\n\n");

            for (Map.Entry<String, List<Integer>> entry : grouped.entrySet()) {
                List<Integer> values = entry.getValue();
                if (!values.isEmpty()) {
                    double avg = values.stream().mapToInt(Integer::intValue).average().orElse(0);
                    int min = values.stream().mapToInt(Integer::intValue).min().orElse(0);
                    int max = values.stream().mapToInt(Integer::intValue).max().orElse(0);
                    long aboveTarget = values.stream().filter(v -> v > 180).count();
                    double abovePct = (aboveTarget * 100.0) / values.size();

                    dataPrompt.append(entry.getKey()).append(":\n");
                    dataPrompt.append("  Average: ").append(String.format("%.1f", avg)).append(" mg/dL\n");
                    dataPrompt.append("  Min: ").append(min).append(" | Max: ").append(max).append(" mg/dL\n");
                    dataPrompt.append("  Readings: ").append(values.size()).append("\n");
                    dataPrompt.append("  Above 180: ").append(String.format("%.1f", abovePct)).append("%\n\n");
                }
            }
            dataPrompt.append(buildHistoryContext(history));
            dataPrompt.append("User question: ").append(userQuestion);

            List<String> knowledge = List.of();
            try { knowledge = knowledgeService.search("glucose time of day pattern morning evening", 3); } catch (Exception e) {}

            String answer = geminiService.call(buildSystemPrompt(knowledge), dataPrompt.toString());
            autoSaveFinding(userQuestion, answer);
            return answer;
        }

// Meal type comparison
        if ("meal_type".equals(intent.getType())) {
            List<FoodLog> allLogs = foodLogRepo.findAll();

            Map<String, List<Integer>> mealSpikes = new LinkedHashMap<>();
            mealSpikes.put("breakfast", new ArrayList<>());
            mealSpikes.put("lunch", new ArrayList<>());
            mealSpikes.put("dinner", new ArrayList<>());
            mealSpikes.put("snack", new ArrayList<>());

            Map<String, List<String>> mealFoods = new LinkedHashMap<>();
            mealFoods.put("breakfast", new ArrayList<>());
            mealFoods.put("lunch", new ArrayList<>());
            mealFoods.put("dinner", new ArrayList<>());
            mealFoods.put("snack", new ArrayList<>());

            for (FoodLog meal : allLogs) {
                if (meal.getMealType() == null || meal.getTimestamp() == null) continue;
                String type = meal.getMealType().toLowerCase();
                if (!mealSpikes.containsKey(type)) continue;

                Date from = toDate(meal.getTimestamp());
                Date to = toDate(meal.getTimestamp().plusHours(3));
                List<Glucose> readings = glucoseRepo.findByDateTimeBetween(from, to);

                if (!readings.isEmpty()) {
                    int baseline = readings.get(0).getGlucose();
                    int peak = readings.stream().mapToInt(Glucose::getGlucose).max().orElse(0);
                    int spike = peak - baseline;
                    mealSpikes.get(type).add(spike);
                    mealFoods.get(type).add(meal.getFoodName() + " (spike: " + spike + ")");
                }
            }

            StringBuilder dataPrompt = new StringBuilder();
            dataPrompt.append("USER MEAL TYPE GLUCOSE IMPACT:\n\n");

            for (String type : mealSpikes.keySet()) {
                List<Integer> spikes = mealSpikes.get(type);
                List<String> foods = mealFoods.get(type);

                dataPrompt.append(type.toUpperCase()).append(":\n");
                if (!spikes.isEmpty()) {
                    double avgSpike = spikes.stream().mapToInt(Integer::intValue).average().orElse(0);
                    int maxSpike = spikes.stream().mapToInt(Integer::intValue).max().orElse(0);
                    dataPrompt.append("  Avg spike: ").append(String.format("%.1f", avgSpike)).append(" mg/dL\n");
                    dataPrompt.append("  Max spike: ").append(maxSpike).append(" mg/dL\n");
                    dataPrompt.append("  Meals tracked: ").append(spikes.size()).append("\n");
                    dataPrompt.append("  Details: ").append(String.join(", ", foods)).append("\n\n");
                } else {
                    dataPrompt.append("  No meals tracked\n\n");
                }
            }
            dataPrompt.append(buildHistoryContext(history));
            dataPrompt.append("User question: ").append(userQuestion);

            List<String> knowledge = List.of();
            try { knowledge = knowledgeService.search("meal type breakfast dinner glucose spike", 3); } catch (Exception e) {}

            String answer = geminiService.call(buildSystemPrompt(knowledge), dataPrompt.toString());
            autoSaveFinding(userQuestion, answer);
            return answer;
        }

// A1C estimation
        if ("a1c".equals(intent.getType())) {
            Date threeMonthsAgo = toDate(LocalDateTime.now().minusDays(90));
            Date now = toDate(LocalDateTime.now());
            List<Glucose> readings = glucoseRepo.findByDateTimeBetween(threeMonthsAgo, now);

            StringBuilder dataPrompt = new StringBuilder();

            if (!readings.isEmpty()) {
                double avg = readings.stream().mapToInt(Glucose::getGlucose).average().orElse(0);
                // A1C formula: (avg glucose + 46.7) / 28.7
                double estimatedA1c = (avg + 46.7) / 28.7;

                int min = readings.stream().mapToInt(Glucose::getGlucose).min().orElse(0);
                int max = readings.stream().mapToInt(Glucose::getGlucose).max().orElse(0);
                long inRange = readings.stream().filter(r -> r.getGlucose() >= 70 && r.getGlucose() <= 180).count();
                double inRangePct = (inRange * 100.0) / readings.size();

                dataPrompt.append("USER A1C ESTIMATION DATA (last 90 days):\n\n");
                dataPrompt.append("Average glucose: ").append(String.format("%.1f", avg)).append(" mg/dL\n");
                dataPrompt.append("Estimated A1C: ").append(String.format("%.1f", estimatedA1c)).append("%\n");
                dataPrompt.append("Min: ").append(min).append(" mg/dL\n");
                dataPrompt.append("Max: ").append(max).append(" mg/dL\n");
                dataPrompt.append("Total readings: ").append(readings.size()).append("\n");
                dataPrompt.append("Time in range (70-180): ").append(String.format("%.1f", inRangePct)).append("%\n\n");
                dataPrompt.append("Note: Estimated A1C uses the formula (avg glucose + 46.7) / 28.7. ");
                dataPrompt.append("This is an approximation and may differ from lab results.\n\n");
            } else {
                dataPrompt.append("No glucose readings found for the last 90 days.\n\n");
            }
            dataPrompt.append(buildHistoryContext(history));
            dataPrompt.append("User question: ").append(userQuestion);

            List<String> knowledge = List.of();
            try { knowledge = knowledgeService.search("a1c hba1c glucose management target", 3); } catch (Exception e) {}

            String answer = geminiService.call(buildSystemPrompt(knowledge), dataPrompt.toString());
            autoSaveFinding(userQuestion, answer);
            return answer;
        }

        // Time based OR comparison — unified handler
        if ("time_based".equals(intent.getType()) || "comparison".equals(intent.getType())) {
            StringBuilder dataPrompt = new StringBuilder();

            Date[] primaryRange = parseDateRange(intent.getStartDate(), intent.getEndDate());
            if (primaryRange != null) {
                List<Glucose> readings = glucoseRepo.findByDateTimeBetween(primaryRange[0], primaryRange[1]);
                List<FoodLog> foodLogs = foodLogRepo.findByTimestampBetween(
                        toLocalDateTime(primaryRange[0]), toLocalDateTime(primaryRange[1]));

                String label = "comparison".equals(intent.getType()) ? "PERIOD 1" : "DATA";
                dataPrompt.append(label).append(" (").append(intent.getStartDate())
                        .append(" to ").append(intent.getEndDate()).append("):\n");
                appendStats(dataPrompt, readings, foodLogs);
            }

            // Comparison second period
            if ("comparison".equals(intent.getType()) && intent.getCompStartDate() != null) {
                Date[] compRange = parseDateRange(intent.getCompStartDate(), intent.getCompEndDate());
                if (compRange != null) {
                    List<Glucose> compReadings = glucoseRepo.findByDateTimeBetween(compRange[0], compRange[1]);
                    List<FoodLog> compFoodLogs = foodLogRepo.findByTimestampBetween(
                            toLocalDateTime(compRange[0]), toLocalDateTime(compRange[1]));

                    dataPrompt.append("\nPERIOD 2 (").append(intent.getCompStartDate())
                            .append(" to ").append(intent.getCompEndDate()).append("):\n");
                    appendStats(dataPrompt, compReadings, compFoodLogs);
                }
            }
            dataPrompt.append(buildHistoryContext(history));
            dataPrompt.append("\nUser question: ").append(userQuestion);

            List<String> knowledge = List.of();
            try { knowledge = knowledgeService.search("glucose trend improvement", 3); } catch (Exception e) {}

            String answer = geminiService.call(buildSystemPrompt(knowledge), dataPrompt.toString());
            autoSaveFinding(userQuestion, answer);
            return answer;
        }

        // General fallback
        List<String> knowledge = List.of();
        try { knowledge = knowledgeService.search(userQuestion, 5); } catch (Exception e) {}
        String answer = geminiService.call(buildSystemPrompt(knowledge), buildHistoryContext(history) + "User question: " + userQuestion);
        autoSaveFinding(userQuestion, answer);
        return answer;
    }

    private Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    private ChatIntent extractIntent(String userQuestion) {
        try {
            String today = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            String json = geminiService.call(
                    "Today's date is " + today + ". " +
                            "Extract intent from the user's question about their glucose/food data. " +
                            "Determine the type: " +
                            "'food_specific' if asking about a specific food, " +
                            "'time_based' if asking about a time period, " +
                            "'comparison' if comparing two time periods, " +
                            "'time_of_day' if asking about morning/afternoon/evening/night patterns, " +
                            "'meal_type' if comparing meal types like breakfast vs dinner, " +
                            "'a1c' if asking about A1C or HbA1c estimation, " +
                            "'general' if a general health question. " +
                            "For time_of_day, set timeOfDay to one of: morning, afternoon, evening, night, or 'all' if comparing all. " +
                            "For meal_type, set mealType to: breakfast, lunch, dinner, snack, or 'all' if comparing all. " +
                            "For time_based and comparison, calculate actual startDate and endDate in yyyy-MM-dd format. " +
                            "For comparison, also provide compStartDate and compEndDate for the second period. " +
                            "Respond ONLY in JSON, no markdown: " +
                            "{\"type\": \"...\", \"food\": \"...\", \"limit\": 3, " +
                            "\"startDate\": \"yyyy-MM-dd\", \"endDate\": \"yyyy-MM-dd\", " +
                            "\"compStartDate\": \"yyyy-MM-dd\", \"compEndDate\": \"yyyy-MM-dd\", " +
                            "\"mealType\": \"...\", \"timeOfDay\": \"...\"}",
                    userQuestion
            );
            String clean = json.replaceAll("```json|```", "").trim();
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(clean, ChatIntent.class);
        } catch (Exception e) {
            ChatIntent fallback = new ChatIntent();
            fallback.setFood("");
            fallback.setType("general");
            fallback.setLimit(3);
            return fallback;
        }
    }

    private String buildSystemPrompt(List<String> knowledgeChunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a diabetic health assistant. Analyze the user's glucose readings ");
        sb.append("and food logs to provide personalized insights.\n\n");

        if (!knowledgeChunks.isEmpty()) {
            sb.append("PERSONAL KNOWLEDGE BASE:\n");
            for (int i = 0; i < knowledgeChunks.size(); i++) {
                sb.append(i + 1).append(". ").append(knowledgeChunks.get(i)).append("\n");
            }
            sb.append("\n");
        }

        sb.append("RESPONSE RULES:\n");
        sb.append("- Lead with the key finding immediately, no filler or preamble\n");
        sb.append("- Use actual numbers from the data (baseline, peak, spike, recovery time)\n");
        sb.append("- When comparing multiple meals, break down EACH meal with its date, time, baseline, peak, spike, and recovery\n");
        sb.append("- Include dates and times so the user can correlate with their experience\n");
        sb.append("- Give 1-2 actionable suggestions max, not a generic list\n");
        sb.append("- If no data is provided, give a short direct answer from general knowledge\n");
        sb.append("- Use **bold** for key numbers and findings\n");
        return sb.toString();
    }

    private String buildDataPrompt(Map<FoodLog, List<Glucose>> glucoseMap, String question, List<Map<String, String>> history)  {
        StringBuilder sb = new StringBuilder();

        if (!glucoseMap.isEmpty()) {
            sb.append("USER DATA:\n\n");
            for (Map.Entry<FoodLog, List<Glucose>> entry : glucoseMap.entrySet()) {
                FoodLog meal = entry.getKey();
                List<Glucose> readings = entry.getValue();

                sb.append("--- ").append(meal.getTimestamp());
                sb.append(" (").append(meal.getFoodName());
                sb.append(", ").append(meal.getPortionSize());
                if (meal.getSource() != null) sb.append(", ").append(meal.getSource());
                sb.append(", ").append(meal.getMealType()).append(") ---\n");

                if (meal.getCarbs() != null) sb.append("Carbs: ").append(meal.getCarbs()).append("g ");
                if (meal.getProtein() != null) sb.append("Protein: ").append(meal.getProtein()).append("g ");
                if (meal.getFiber() != null) sb.append("Fiber: ").append(meal.getFiber()).append("g");
                if (meal.getCarbs() != null || meal.getProtein() != null || meal.getFiber() != null) sb.append("\n");
                if (meal.getNotes() != null && !meal.getNotes().isEmpty()) sb.append("Notes: ").append(meal.getNotes()).append("\n");

                if (!readings.isEmpty()) {
                    int baseline = readings.get(0).getGlucose();
                    int peak = readings.stream().mapToInt(Glucose::getGlucose).max().orElse(0);
                    sb.append("Baseline: ").append(baseline).append(" mg/dL | Peak: ").append(peak);
                    sb.append(" mg/dL | Spike: ").append(peak - baseline).append("\n");
                    sb.append("Readings:\n");
                    for (Glucose r : readings) {
                        sb.append("  ").append(r.getDateTime()).append(": ").append(r.getGlucose()).append(" mg/dL\n");
                    }
                } else {
                    sb.append("No glucose readings found for this time window.\n");
                }
                sb.append("\n");
            }
        }

        sb.append(buildHistoryContext(history));
        sb.append("User question: ").append(question);
        return sb.toString();
    }

    private void autoSaveFinding(String question, String answer) {
        try {
            String finding = geminiService.call(
                    "Summarize the key finding from this Q&A in one short sentence. " +
                            "Focus on the personal health insight. Respond with ONLY the sentence, nothing else.",
                    "Question: " + question + "\nAnswer: " + answer
            );
            System.out.println("Auto-save finding: " + finding);
            if (finding != null && finding.length() > 10) {
                KnowledgeDocument saved = knowledgeService.save(finding.trim(), "auto_insight", "chat_history");
                System.out.println("Saved to knowledge_base: " + saved.getId());
            }
        } catch (Exception e) {
            System.out.println("Auto-save failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private Date[] getDateRange(List<String> timeRefs) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start;

        String ref = (timeRefs != null && !timeRefs.isEmpty()) ? timeRefs.get(0) : "today";

        switch (ref) {
            case "today": start = now.toLocalDate().atStartOfDay(); break;
            case "yesterday": start = now.minusDays(1).toLocalDate().atStartOfDay(); break;
            case "this_week": start = now.minusDays(7); break;
            case "last_3_days": start = now.minusDays(3); break;
            default: start = now.minusDays(7); break;
        }

        return new Date[]{ toDate(start), toDate(now) };
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private String buildTimeBasedPrompt(List<Glucose> readings, List<FoodLog> meals, String question) {
        StringBuilder sb = new StringBuilder();
        sb.append("USER DATA:\n\n");

        if (!meals.isEmpty()) {
            sb.append("FOOD LOGS:\n");
            for (FoodLog meal : meals) {
                sb.append("- ").append(meal.getTimestamp()).append(": ")
                        .append(meal.getFoodName()).append(", ").append(meal.getPortionSize());
                if (meal.getSource() != null) sb.append(", ").append(meal.getSource());
                sb.append(" (").append(meal.getMealType()).append(")\n");
            }
            sb.append("\n");
        }

        if (!readings.isEmpty()) {
            int min = readings.stream().mapToInt(Glucose::getGlucose).min().orElse(0);
            int max = readings.stream().mapToInt(Glucose::getGlucose).max().orElse(0);
            double avg = readings.stream().mapToInt(Glucose::getGlucose).average().orElse(0);

            sb.append("GLUCOSE SUMMARY:\n");
            sb.append("Total readings: ").append(readings.size()).append("\n");
            sb.append("Min: ").append(min).append(" mg/dL\n");
            sb.append("Max: ").append(max).append(" mg/dL\n");
            sb.append("Average: ").append(String.format("%.1f", avg)).append(" mg/dL\n");
            sb.append("Highest spike: ").append(max).append(" mg/dL\n\n");

            // Include a sample of readings (limit to avoid token overflow)
            sb.append("READINGS (sampled):\n");
            int step = Math.max(1, readings.size() / 50);
            for (int i = 0; i < readings.size(); i += step) {
                Glucose r = readings.get(i);
                sb.append("  ").append(r.getDateTime()).append(": ").append(r.getGlucose()).append(" mg/dL\n");
            }
            sb.append("\n");
        }

        sb.append("User question: ").append(question);
        return sb.toString();
    }
    private void appendStats(StringBuilder sb, List<Glucose> readings, List<FoodLog> meals) {
        if (!readings.isEmpty()) {
            int min = readings.stream().mapToInt(Glucose::getGlucose).min().orElse(0);
            int max = readings.stream().mapToInt(Glucose::getGlucose).max().orElse(0);
            double avg = readings.stream().mapToInt(Glucose::getGlucose).average().orElse(0);
            sb.append("Average: ").append(String.format("%.1f", avg)).append(" mg/dL\n");
            sb.append("Min: ").append(min).append(" mg/dL\n");
            sb.append("Max: ").append(max).append(" mg/dL\n");
            sb.append("Total readings: ").append(readings.size()).append("\n");
        } else {
            sb.append("No readings found.\n");
        }
        sb.append("Meals logged: ").append(meals.size()).append("\n");
        for (FoodLog meal : meals) {
            sb.append("- ").append(meal.getFoodName())
                    .append(" (").append(meal.getMealType()).append(")\n");
        }
    }
    private Date[] parseDateRange(String startStr, String endStr) {
        try {
            if (startStr == null || endStr == null) return null;
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDateTime start = java.time.LocalDate.parse(startStr, fmt).atStartOfDay();
            LocalDateTime end = java.time.LocalDate.parse(endStr, fmt).atTime(23, 59, 59);
            return new Date[]{ toDate(start), toDate(end) };
        } catch (Exception e) {
            return null;
        }
    }
    private String buildHistoryContext(List<Map<String, String>> history) {
        if (history == null || history.size() <= 1) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("CONVERSATION HISTORY:\n");
        for (int i = 0; i < history.size() - 1; i++) {
            Map<String, String> msg = history.get(i);
            String role = msg.get("role");
            sb.append(role.equals("user") ? "User" : "Assistant")
                    .append(": ").append(msg.get("content")).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }
}