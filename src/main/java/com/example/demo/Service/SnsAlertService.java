package com.example.demo.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Service
public class SnsAlertService {

    private final SnsClient snsClient;
    private final String topicArn;

    public SnsAlertService(@Value("${aws.sns.topic-arn}") String topicArn,
                           @Value("${aws.region}") String region) {
        this.topicArn = topicArn;
        this.snsClient = SnsClient.builder()
                .region(Region.of(region))
                .build();
    }

    public void sendAlert(String subject, String message) {
        snsClient.publish(PublishRequest.builder()
                .topicArn(topicArn)
                .subject(subject)
                .message(message)
                .build());
        System.out.println("SNS alert sent: " + subject);
    }
}