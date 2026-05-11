package org.fiap.com.service;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.fiap.com.FeedbackMapperUtil;
import org.fiap.com.models.Feedback;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class FeedbackServiceImpl extends AbstractFeedbackService {

    @Inject
    DynamoDbClient dynamoDb;

    @Inject
    EventBridgeClient eventBridge;

    public List<Feedback> findAll() {
        return dynamoDb.scanPaginator(scanRequest()).items().stream().map(Feedback::from).toList();
    }

    public Feedback add(Feedback feedback) {
        String description = Objects.requireNonNull(feedback.getDescription(), "description is required");
        if (description.isBlank() || description.length() < 3) {
            throw new IllegalArgumentException("description must have at least 3 characters");
        }
        Integer grade = Objects.requireNonNull(feedback.getGrade(), "grade is required");
        if (grade < 0 || grade > 10) {
            throw new IllegalArgumentException("grade must be between 0 and 10");
        }
        String id = UUID.randomUUID().toString();
        String createdAt = Instant.now().toString();
        dynamoDb.putItem(putItemRequest(id, description, grade, createdAt));
        feedback.setId(id);
        feedback.setCreatedAt(createdAt);

        if (grade < 6) {
            publishCriticalEvent(feedback);
        }

        return feedback;
    }

    private void publishCriticalEvent(Feedback feedback) {
        String detail = String.format(
                "{\"eventId\":\"%s\",\"feedbackId\":\"%s\",\"description\":\"%s\",\"grade\":%d,\"urgency\":\"%s\",\"createdAt\":\"%s\"}",
                UUID.randomUUID(), feedback.getId(), feedback.getDescription(),
                feedback.getGrade(), feedback.getUrgency(), feedback.getCreatedAt());

        PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                .source("feedback.platform")
                .detailType("FeedbackCritical")
                .detail(detail)
                .build();

        eventBridge.putEvents(PutEventsRequest.builder().entries(entry).build());
    }

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent eventRequest) {
        try {
            String method = eventRequest.getHttpMethod();

            if ("POST".equalsIgnoreCase(method)) {
                Feedback created = add(FeedbackMapperUtil.fromJson(eventRequest.getBody()));
                return response(201, FeedbackMapperUtil.toJson(created));
            }

            if ("GET".equalsIgnoreCase(method)) {
                String id = pathParamId(eventRequest);
                if (id != null) {
                    return response(200, FeedbackMapperUtil.toJson(findById(id)));
                }
                return response(200, FeedbackMapperUtil.toJson(findAll()));
            }

            return response(405, "Method Not Allowed");
        } catch (IllegalArgumentException e) {
            return response(400, e.getMessage());
        } catch (Exception e) {
            return response(500, e.getMessage());
        }
    }

    public Feedback findById(String id) {
        return Feedback.from(dynamoDb.getItem(getRequest(id)).item());
    }

    private APIGatewayProxyResponseEvent response(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(body);
    }

    private String pathParamId(APIGatewayProxyRequestEvent eventRequest) {
        if (eventRequest.getPathParameters() == null) {
            return null;
        }
        return eventRequest.getPathParameters().get("id");
    }
}
