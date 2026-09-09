package com.example.demo.Service;

import com.example.demo.Model.FoodLog;
import com.example.demo.Model.Glucose;
import com.example.demo.Model.HealthEvent;
import com.example.demo.Repository.FoodLogRepository;
import com.example.demo.Repository.GlucoseRepository;
import com.example.demo.Repository.HealthEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MongoToolService {

    @Autowired
    private GlucoseRepository glucoseRepo;

    @Autowired
    private FoodLogRepository foodLogRepo;

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private HealthEventRepository healthEventRepo;

    private final ObjectMapper mapper = new ObjectMapper();

    public String executeTool(String name, Map<String, Object> input) {
        try {
            return switch (name) {
                case "get_latest_glucose"      -> getLatestGlucose();
                case "get_glucose_readings"    -> getGlucoseReadings(input);
                case "search_food_logs"        -> searchFoodLogs(input);
                case "get_food_logs_in_range"  -> getFoodLogsInRange(input);
                case "get_glucose_around_meal" -> getGlucoseAroundMeal(input);
                case "search_knowledge_base"   -> searchKnowledgeBase(input);
                case "get_time_of_day_patterns"-> getTimeOfDayPatterns();
                case "get_meal_type_patterns"  -> getMealTypePatterns();
                case "get_health_events"       -> getHealthEvents(input);
                case "get_daily_health_summary"-> getDailyHealthSummary(input);
                default -> "{\"error\":\"Unknown tool: " + name + "\"}";
            };
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private String getLatestGlucose() throws Exception {
        Glucose g = glucoseRepo.findTopByOrderByDateTimeDesc();
        if (g == null) return "{\"error\":\"No glucose readings found\"}";
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("glucose_mgdl", g.getGlucose());
        result.put("datetime", g.getDateTime().toString());
        return mapper.writeValueAsString(result);
    }

    private String getGlucoseReadings(Map<String, Object> input) throws Exception {
        Date[] range = parseDateRange(input);
        if (range == null) return "{\"error\":\"Provide startDate and endDate in yyyy-MM-dd format\"}";

        List<Glucose> readings = glucoseRepo.findByDateTimeBetween(range[0], range[1]);
        if (readings.isEmpty()) return "{\"count\":0,\"readings\":[]}";

        double avg = readings.stream().mapToInt(Glucose::getGlucose).average().orElse(0);
        int min = readings.stream().mapToInt(Glucose::getGlucose).min().orElse(0);
        int max = readings.stream().mapToInt(Glucose::getGlucose).max().orElse(0);
        long inRange = readings.stream().filter(r -> r.getGlucose() >= 70 && r.getGlucose() <= 180).count();

        List<Map<String, Object>> sample = readings.stream()
                .limit(60)
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("datetime", r.getDateTime().toString());
                    m.put("glucose_mgdl", r.getGlucose());
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", readings.size());
        result.put("average_mgdl", Math.round(avg * 10.0) / 10.0);
        result.put("min_mgdl", min);
        result.put("max_mgdl", max);
        result.put("time_in_range_pct", Math.round(inRange * 100.0 / readings.size() * 10.0) / 10.0);
        result.put("readings", sample);
        return mapper.writeValueAsString(result);
    }

    private String searchFoodLogs(Map<String, Object> input) throws Exception {
        String query = (String) input.getOrDefault("query", "");
        int limit = input.containsKey("limit") ? ((Number) input.get("limit")).intValue() : 5;

        List<FoodLog> logs = foodLogRepo.findByFoodNameRegex(query).stream()
                .limit(limit)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", logs.size());
        result.put("food_logs", logs.stream().map(this::foodLogToMap).collect(Collectors.toList()));
        return mapper.writeValueAsString(result);
    }

    private String getFoodLogsInRange(Map<String, Object> input) throws Exception {
        Date[] range = parseDateRange(input);
        if (range == null) return "{\"error\":\"Provide startDate and endDate in yyyy-MM-dd format\"}";

        LocalDateTime start = range[0].toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime end = range[1].toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        List<FoodLog> logs = foodLogRepo.findByTimestampBetween(start, end);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", logs.size());
        result.put("food_logs", logs.stream().map(this::foodLogToMap).collect(Collectors.toList()));
        return mapper.writeValueAsString(result);
    }

    private String getGlucoseAroundMeal(Map<String, Object> input) throws Exception {
        String timestamp = (String) input.get("meal_timestamp");
        if (timestamp == null) return "{\"error\":\"Provide meal_timestamp (ISO format, e.g. 2026-09-05T18:30:00)\"}";

        int hoursBefore = input.containsKey("hours_before") ? ((Number) input.get("hours_before")).intValue() : 1;
        int hoursAfter  = input.containsKey("hours_after")  ? ((Number) input.get("hours_after")).intValue()  : 3;

        // Normalize timestamp — accept both "T" and space separators, trim to seconds
        String normalized = timestamp.replace(" ", "T");
        if (normalized.length() > 19) normalized = normalized.substring(0, 19);
        LocalDateTime mealTime = LocalDateTime.parse(normalized);

        Date from = Date.from(mealTime.minusHours(hoursBefore).atZone(ZoneId.systemDefault()).toInstant());
        Date to   = Date.from(mealTime.plusHours(hoursAfter).atZone(ZoneId.systemDefault()).toInstant());

        List<Glucose> readings = glucoseRepo.findByDateTimeBetween(from, to);
        if (readings.isEmpty()) return "{\"count\":0,\"readings\":[]}";

        int baseline = readings.get(0).getGlucose();
        int peak = readings.stream().mapToInt(Glucose::getGlucose).max().orElse(0);

        List<Map<String, Object>> readingsList = readings.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("datetime", r.getDateTime().toString());
            m.put("glucose_mgdl", r.getGlucose());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("baseline_mgdl", baseline);
        result.put("peak_mgdl", peak);
        result.put("spike_mgdl", peak - baseline);
        result.put("count", readings.size());
        result.put("readings", readingsList);
        return mapper.writeValueAsString(result);
    }

    private String searchKnowledgeBase(Map<String, Object> input) throws Exception {
        String query = (String) input.getOrDefault("query", "");
        int limit = input.containsKey("limit") ? ((Number) input.get("limit")).intValue() : 5;

        List<String> insights = knowledgeService.search(query, limit);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", insights.size());
        result.put("insights", insights);
        return mapper.writeValueAsString(result);
    }

    private String getTimeOfDayPatterns() throws Exception {
        Date monthStart = Date.from(LocalDateTime.now().minusDays(30).atZone(ZoneId.systemDefault()).toInstant());
        List<Glucose> all = glucoseRepo.findByDateTimeBetween(monthStart, new Date());

        Map<String, List<Integer>> groups = new LinkedHashMap<>();
        groups.put("Morning (6AM-12PM)", new ArrayList<>());
        groups.put("Afternoon (12PM-5PM)", new ArrayList<>());
        groups.put("Evening (5PM-9PM)", new ArrayList<>());
        groups.put("Night (9PM-6AM)", new ArrayList<>());

        for (Glucose r : all) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(r.getDateTime());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            if      (hour >= 6  && hour < 12) groups.get("Morning (6AM-12PM)").add(r.getGlucose());
            else if (hour >= 12 && hour < 17) groups.get("Afternoon (12PM-5PM)").add(r.getGlucose());
            else if (hour >= 17 && hour < 21) groups.get("Evening (5PM-9PM)").add(r.getGlucose());
            else                              groups.get("Night (9PM-6AM)").add(r.getGlucose());
        }

        Map<String, Object> patterns = new LinkedHashMap<>();
        for (Map.Entry<String, List<Integer>> e : groups.entrySet()) {
            List<Integer> vals = e.getValue();
            if (vals.isEmpty()) continue;
            long above = vals.stream().filter(v -> v > 180).count();
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("average_mgdl", Math.round(vals.stream().mapToInt(Integer::intValue).average().orElse(0) * 10.0) / 10.0);
            stats.put("min_mgdl",     vals.stream().mapToInt(Integer::intValue).min().orElse(0));
            stats.put("max_mgdl",     vals.stream().mapToInt(Integer::intValue).max().orElse(0));
            stats.put("count",        vals.size());
            stats.put("above_180_pct", Math.round(above * 100.0 / vals.size() * 10.0) / 10.0);
            patterns.put(e.getKey(), stats);
        }
        return mapper.writeValueAsString(Map.of("time_of_day_patterns", patterns));
    }

    private String getMealTypePatterns() throws Exception {
        List<FoodLog> all = foodLogRepo.findAllByOrderByTimestampDesc();

        Map<String, List<Integer>> spikes = new LinkedHashMap<>();
        Map<String, List<String>> foods   = new LinkedHashMap<>();
        for (String t : List.of("breakfast", "lunch", "dinner", "snack")) {
            spikes.put(t, new ArrayList<>());
            foods.put(t, new ArrayList<>());
        }

        for (FoodLog meal : all) {
            if (meal.getMealType() == null || meal.getTimestamp() == null) continue;
            String type = meal.getMealType().toLowerCase();
            if (!spikes.containsKey(type)) continue;

            Date from = Date.from(meal.getTimestamp().atZone(ZoneId.systemDefault()).toInstant());
            Date to   = Date.from(meal.getTimestamp().plusHours(3).atZone(ZoneId.systemDefault()).toInstant());
            List<Glucose> readings = glucoseRepo.findByDateTimeBetween(from, to);

            if (!readings.isEmpty()) {
                int baseline = readings.get(0).getGlucose();
                int peak = readings.stream().mapToInt(Glucose::getGlucose).max().orElse(0);
                int spike = peak - baseline;
                spikes.get(type).add(spike);
                foods.get(type).add(meal.getFoodName() + " (spike: " + spike + " mg/dL)");
            }
        }

        Map<String, Object> patterns = new LinkedHashMap<>();
        for (String type : spikes.keySet()) {
            List<Integer> s = spikes.get(type);
            if (s.isEmpty()) continue;
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("avg_spike_mgdl", Math.round(s.stream().mapToInt(Integer::intValue).average().orElse(0) * 10.0) / 10.0);
            stats.put("max_spike_mgdl", s.stream().mapToInt(Integer::intValue).max().orElse(0));
            stats.put("meals_tracked",  s.size());
            stats.put("top_foods",      foods.get(type).stream().limit(5).collect(Collectors.toList()));
            patterns.put(type, stats);
        }
        return mapper.writeValueAsString(Map.of("meal_type_patterns", patterns));
    }

    private String getHealthEvents(Map<String, Object> input) throws Exception {
        String type      = (String) input.get("type");
        String startDate = (String) input.get("startDate");
        String endDate   = (String) input.get("endDate");

        List<HealthEvent> events;

        if (type != null && !type.isEmpty() && startDate != null) {
            // Fetch by type within a date range using the date prefix index
            events = new ArrayList<>();
            // Generate every date in range and query per-date
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            java.time.LocalDate start = java.time.LocalDate.parse(startDate, fmt);
            java.time.LocalDate end   = endDate != null
                    ? java.time.LocalDate.parse(endDate, fmt)
                    : start;
            for (java.time.LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                events.addAll(healthEventRepo.findByTypeAndDateStartingWith(type, d.toString()));
            }
        } else if (type != null && !type.isEmpty()) {
            events = healthEventRepo.findByType(type);
        } else {
            events = healthEventRepo.findAll();
        }

        List<Map<String, Object>> result = events.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type",  e.getType());
            m.put("date",  e.getDate());
            m.put("units", e.getUnits());
            if (e.getData() != null) m.put("data", e.getData());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("count", result.size());
        out.put("events", result);
        return mapper.writeValueAsString(out);
    }

    private String getDailyHealthSummary(Map<String, Object> input) throws Exception {
        String date = (String) input.get("date");
        if (date == null) return "{\"error\":\"Provide date in yyyy-MM-dd format\"}";

        List<HealthEvent> events = healthEventRepo.findAll().stream()
                .filter(e -> date.equals(e.getDate()) || (e.getDate() != null && e.getDate().startsWith(date)))
                .collect(Collectors.toList());

        // Also get glucose readings for that day
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        java.time.LocalDate d = java.time.LocalDate.parse(date, fmt);
        Date dayStart = Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date dayEnd   = Date.from(d.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());
        List<Glucose> glucoseReadings = glucoseRepo.findByDateTimeBetween(dayStart, dayEnd);

        // Also get food logs for that day
        List<FoodLog> foodLogs = foodLogRepo.findByTimestampBetween(
                d.atStartOfDay(), d.atTime(23, 59, 59));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("date", date);

        // Health events grouped by type
        Map<String, Object> healthByType = new LinkedHashMap<>();
        for (HealthEvent e : events) {
            healthByType.put(e.getType(), Map.of("units", e.getUnits() != null ? e.getUnits() : "", "data", e.getData() != null ? e.getData() : Map.of()));
        }
        summary.put("health_events", healthByType);

        // Glucose stats for the day
        if (!glucoseReadings.isEmpty()) {
            double avg = glucoseReadings.stream().mapToInt(Glucose::getGlucose).average().orElse(0);
            int min = glucoseReadings.stream().mapToInt(Glucose::getGlucose).min().orElse(0);
            int max = glucoseReadings.stream().mapToInt(Glucose::getGlucose).max().orElse(0);
            long inRange = glucoseReadings.stream().filter(r -> r.getGlucose() >= 70 && r.getGlucose() <= 180).count();
            Map<String, Object> glucoseStats = new LinkedHashMap<>();
            glucoseStats.put("average_mgdl",    Math.round(avg * 10.0) / 10.0);
            glucoseStats.put("min_mgdl",         min);
            glucoseStats.put("max_mgdl",         max);
            glucoseStats.put("readings_count",   glucoseReadings.size());
            glucoseStats.put("time_in_range_pct", Math.round(inRange * 100.0 / glucoseReadings.size() * 10.0) / 10.0);
            summary.put("glucose", glucoseStats);
        }

        // Food logs for the day
        if (!foodLogs.isEmpty()) {
            summary.put("meals", foodLogs.stream().map(this::foodLogToMap).collect(Collectors.toList()));
        }

        return mapper.writeValueAsString(summary);
    }

    // ─── helpers ───────────────────────────────────────────────────────────

    private Map<String, Object> foodLogToMap(FoodLog meal) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (meal.getId()         != null) m.put("id",           meal.getId());
        if (meal.getTimestamp()  != null) m.put("timestamp",    meal.getTimestamp().toString());
        if (meal.getFoodName()   != null) m.put("food_name",    meal.getFoodName());
        if (meal.getMealType()   != null) m.put("meal_type",    meal.getMealType());
        if (meal.getPortionSize()!= null) m.put("portion_size", meal.getPortionSize());
        if (meal.getCarbs()      != null) m.put("carbs_g",      meal.getCarbs());
        if (meal.getProtein()    != null) m.put("protein_g",    meal.getProtein());
        if (meal.getFiber()      != null) m.put("fiber_g",      meal.getFiber());
        if (meal.getNotes()      != null && !meal.getNotes().isEmpty()) m.put("notes", meal.getNotes());
        return m;
    }

    private Date[] parseDateRange(Map<String, Object> input) {
        try {
            String start = (String) input.get("startDate");
            String end   = (String) input.get("endDate");
            if (start == null || end == null) return null;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDateTime s = java.time.LocalDate.parse(start, fmt).atStartOfDay();
            LocalDateTime e = java.time.LocalDate.parse(end,   fmt).atTime(23, 59, 59);
            return new Date[]{
                Date.from(s.atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(e.atZone(ZoneId.systemDefault()).toInstant())
            };
        } catch (Exception ex) {
            return null;
        }
    }

    // ─── tool schema definitions for Claude ────────────────────────────────

    public List<Map<String, Object>> getToolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();

        tools.add(tool("get_latest_glucose",
            "Get the most recent glucose reading from the CGM sensor",
            emptySchema()));

        tools.add(tool("get_glucose_readings",
            "Get glucose readings between two dates. Returns average, min, max, time-in-range, and individual readings.",
            schema(
                prop("startDate", "string", "Start date yyyy-MM-dd"),
                prop("endDate",   "string", "End date yyyy-MM-dd")
            ), "startDate", "endDate"));

        tools.add(tool("search_food_logs",
            "Search food logs by food name (partial/regex match). Returns matching meals with nutrition info and timestamps.",
            schema(
                prop("query", "string",  "Food name to search for"),
                prop("limit", "integer", "Max results to return (default 5)")
            ), "query"));

        tools.add(tool("get_food_logs_in_range",
            "Get all food logs within a date range.",
            schema(
                prop("startDate", "string", "Start date yyyy-MM-dd"),
                prop("endDate",   "string", "End date yyyy-MM-dd")
            ), "startDate", "endDate"));

        tools.add(tool("get_glucose_around_meal",
            "Get glucose readings around a specific meal time to analyse post-meal spikes. Use after search_food_logs to get meal timestamps.",
            schema(
                prop("meal_timestamp", "string",  "Meal datetime ISO format, e.g. 2026-09-05T18:30:00"),
                prop("hours_before",   "integer", "Hours before meal to include (default 1)"),
                prop("hours_after",    "integer", "Hours after meal to include (default 3)")
            ), "meal_timestamp"));

        tools.add(tool("search_knowledge_base",
            "Semantic search of personal health insights and past AI analysis stored from previous conversations.",
            schema(
                prop("query", "string",  "What to search for"),
                prop("limit", "integer", "Max results (default 5)")
            ), "query"));

        tools.add(tool("get_time_of_day_patterns",
            "Get glucose patterns grouped by time of day (Morning/Afternoon/Evening/Night) across the last 30 days.",
            emptySchema()));

        tools.add(tool("get_meal_type_patterns",
            "Get average glucose spikes grouped by meal type (breakfast, lunch, dinner, snack) across all logged meals.",
            emptySchema()));

        tools.add(tool("get_health_events",
            "Fetch health events (steps, heart_rate, sleep, exercise, etc.) by type and optional date range. " +
            "Use to correlate activity, sleep, and heart rate with glucose levels for in-depth analysis.",
            schema(
                prop("type",      "string", "Event type: steps, heart_rate, sleep, exercise, or leave empty for all types"),
                prop("startDate", "string", "Start date yyyy-MM-dd"),
                prop("endDate",   "string", "End date yyyy-MM-dd (optional, defaults to startDate)")
            ), "startDate"));

        tools.add(tool("get_daily_health_summary",
            "Get a complete picture of one day: all health events (steps, sleep, heart rate), glucose stats, and meals logged. " +
            "Best for deep daily analysis and multi-signal correlations.",
            schema(
                prop("date", "string", "Date in yyyy-MM-dd format")
            ), "date"));

        return tools;
    }

    private Map<String, Object> tool(String name, String description, Map<String, Object> inputSchema, String... required) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("name", name);
        t.put("description", description);
        if (required.length > 0) {
            inputSchema.put("required", List.of(required));
        }
        t.put("input_schema", inputSchema);
        return t;
    }

    private Map<String, Object> emptySchema() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("type", "object");
        s.put("properties", new LinkedHashMap<>());
        return s;
    }

    private Map<String, Object> schema(Map<String, Object>... props) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (Map<String, Object> p : props) {
            properties.put((String) p.remove("_key"), p);
        }
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("type", "object");
        s.put("properties", properties);
        return s;
    }

    private Map<String, Object> prop(String key, String type, String description) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("_key", key);
        p.put("type", type);
        p.put("description", description);
        return p;
    }
}