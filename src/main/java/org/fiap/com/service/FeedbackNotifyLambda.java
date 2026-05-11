package org.fiap.com.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Named("feedbackNotify")
@ApplicationScoped
public class FeedbackNotifyLambda implements RequestHandler<ScheduledEvent, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ADMIN_EMAIL = System.getenv("ADMIN_EMAIL") != null
            ? System.getenv("ADMIN_EMAIL") : "admin@feedback.platform";
    private static final String SENDER_EMAIL = System.getenv("SENDER_EMAIL") != null
            ? System.getenv("SENDER_EMAIL") : "noreply@feedback.platform";

    @Inject
    SesClient ses;

    @Override
    public String handleRequest(ScheduledEvent event, Context context) {
        try {
            JsonNode detail = MAPPER.readTree(MAPPER.writeValueAsString(event.getDetail()));
            String description = detail.get("description").asText();
            String urgency = detail.get("urgency").asText();
            String createdAt = detail.get("createdAt").asText();
            int grade = detail.get("grade").asInt();
            String feedbackId = detail.get("feedbackId").asText();

            String subject = String.format("[%s] Feedback crítico recebido - Nota %d", urgency, grade);
            String body = String.format(
                    "Feedback ID: %s\nDescrição: %s\nNota: %d\nUrgência: %s\nData: %s",
                    feedbackId, description, grade, urgency, createdAt);

            ses.sendEmail(SendEmailRequest.builder()
                    .source(SENDER_EMAIL)
                    .destination(Destination.builder().toAddresses(ADMIN_EMAIL).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).charset("UTF-8").build())
                            .body(Body.builder()
                                    .text(Content.builder().data(body).charset("UTF-8").build())
                                    .build())
                            .build())
                    .build());

            context.getLogger().log("Email sent for feedback: " + feedbackId);
            return "OK";
        } catch (Exception e) {
            context.getLogger().log("Error processing event: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
