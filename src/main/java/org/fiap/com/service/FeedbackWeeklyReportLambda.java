package org.fiap.com.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.fiap.com.models.Feedback;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Named("feedbackWeeklyReport")
@ApplicationScoped
public class FeedbackWeeklyReportLambda implements RequestHandler<ScheduledEvent, String> {

    private static final String ADMIN_EMAIL = System.getenv("ADMIN_EMAIL") != null
            ? System.getenv("ADMIN_EMAIL") : "admin@feedback.platform";
    private static final String SENDER_EMAIL = System.getenv("SENDER_EMAIL") != null
            ? System.getenv("SENDER_EMAIL") : "noreply@feedback.platform";

    @Inject
    DynamoDbClient dynamoDb;

    @Inject
    SesClient ses;

    @Override
    public String handleRequest(ScheduledEvent event, Context context) {
        String tableName = System.getenv("FEEDBACK_TABLE_NAME") != null
                ? System.getenv("FEEDBACK_TABLE_NAME") : "feedback";
        String oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS).toString();

        List<Feedback> feedbacks = dynamoDb.scanPaginator(ScanRequest.builder()
                        .tableName(tableName)
                        .filterExpression("createdAt >= :since")
                        .expressionAttributeValues(Map.of(":since", AttributeValue.builder().s(oneWeekAgo).build()))
                        .build())
                .items().stream().map(Feedback::from).toList();

        if (feedbacks.isEmpty()) {
            context.getLogger().log("No feedbacks in the last 7 days.");
            return "NO_DATA";
        }

        double avgGrade = feedbacks.stream().mapToInt(Feedback::getGrade).average().orElse(0);

        Map<String, Long> perDay = feedbacks.stream()
                .collect(Collectors.groupingBy(f -> f.getCreatedAt().substring(0, 10), Collectors.counting()));

        Map<String, Long> perUrgency = feedbacks.stream()
                .collect(Collectors.groupingBy(Feedback::getUrgency, Collectors.counting()));

        StringBuilder body = new StringBuilder();
        body.append("=== Relatório Semanal de Feedbacks ===\n\n");
        body.append(String.format("Total de avaliações: %d\n", feedbacks.size()));
        body.append(String.format("Média de notas: %.2f\n\n", avgGrade));
        body.append("Quantidade por dia:\n");
        perDay.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> body.append(String.format("  %s: %d\n", e.getKey(), e.getValue())));
        body.append("\nQuantidade por urgência:\n");
        perUrgency.forEach((k, v) -> body.append(String.format("  %s: %d\n", k, v)));

        body.append("\n--- Detalhamento dos Feedbacks ---\n\n");
        feedbacks.stream().sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .forEach(f -> body.append(String.format("• [%s] %s | Nota: %d | Urgência: %s\n",
                        f.getCreatedAt().substring(0, 10), f.getDescription(), f.getGrade(), f.getUrgency())));

        ses.sendEmail(SendEmailRequest.builder()
                .source(SENDER_EMAIL)
                .destination(Destination.builder().toAddresses(ADMIN_EMAIL).build())
                .message(Message.builder()
                        .subject(Content.builder().data("Relatório Semanal - Plataforma de Feedback").charset("UTF-8").build())
                        .body(Body.builder()
                                .text(Content.builder().data(body.toString()).charset("UTF-8").build())
                                .build())
                        .build())
                .build());

        context.getLogger().log("Weekly report sent. Total feedbacks: " + feedbacks.size());
        return "OK";
    }
}
