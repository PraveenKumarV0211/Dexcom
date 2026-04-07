package com.example.demo.Service;

import com.example.demo.Model.Glucose;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.LinkedList;

@Service
public class GlucoseAlertConsumer {

    private final SnsAlertService snsAlertService;
    private final ElasticsearchQueryService esQueryService;
    private final GeminiService geminiService;
    private final LinkedList<Integer> recentReadings = new LinkedList<>();
    private boolean highAlertSent = false;
    private boolean lowAlertSent = false;

    public GlucoseAlertConsumer(SnsAlertService snsAlertService,
                                ElasticsearchQueryService esQueryService,
                                GeminiService geminiService) {
        this.snsAlertService = snsAlertService;
        this.esQueryService = esQueryService;
        this.geminiService = geminiService;
    }

    @KafkaListener(topics = "glucose-readings", groupId = "alert-group")
    public void checkAlerts(Glucose glucose) {
        recentReadings.addLast(glucose.getGlucose());
        if (recentReadings.size() > 6) recentReadings.removeFirst();

        if (recentReadings.size() == 6) {
            boolean allHigh = recentReadings.stream().allMatch(v -> v > 350);
            boolean allLow = recentReadings.stream().allMatch(v -> v < 70);

            if (allHigh && !highAlertSent) {
                snsAlertService.sendAlert(
                        "⚠️ SUSTAINED HIGH Glucose Alert",
                        "Your last 6 readings have all been above 350 mg/dL.\n"
                                + "Latest: " + glucose.getGlucose() + " mg/dL at " + glucose.getDateTime()
                                + "\nPlease check and take action."
                );
                highAlertSent = true;
            } else if (!allHigh) {
                highAlertSent = false;
            }

            if (allLow && !lowAlertSent) {
                snsAlertService.sendAlert(
                        "🚨 SUSTAINED LOW Glucose Alert",
                        "Your last 6 readings have all been below 70 mg/dL.\n"
                                + "Latest: " + glucose.getGlucose() + " mg/dL at " + glucose.getDateTime()
                                + "\nPlease eat something immediately."
                );
                lowAlertSent = true;
            } else if (!allLow) {
                lowAlertSent = false;
            }
        }
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void sendDailyDigest() {
        try {
            String multiSignal = esQueryService.getMultiSignalContext(null, null);

            String summary = geminiService.call(
                    "You are a health assistant. Write a brief daily health digest email based on the data below. "
                            + "Include: glucose summary (avg, min, max, time in range), heart rate summary, step count. "
                            + "Keep it concise and friendly. End with one actionable tip.",
                    "Yesterday's health data:\n" + multiSignal
            );

            snsAlertService.sendAlert("📊 GlucoLens Daily Digest", summary);
            System.out.println("Daily digest sent.");
        } catch (Exception e) {
            System.err.println("Daily digest error: " + e.getMessage());
        }
    }
}