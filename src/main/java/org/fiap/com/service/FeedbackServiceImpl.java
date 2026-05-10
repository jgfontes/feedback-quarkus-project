package org.fiap.com.service;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.fiap.com.FeedbackMapperUtil;
import org.fiap.com.models.Feedback;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class FeedbackServiceImpl extends AbstractFeedbackService {

    @Inject
    DynamoDbClient dynamoDb;

    public List<Feedback> findAll() {
        System.out.println(scanRequest());
        return dynamoDb.scanPaginator(scanRequest()).items().stream().map(Feedback::from).toList();
    }

    public Feedback add(Feedback feedback) {
        System.out.println("Adding feedback: " + feedback);
        Long id = nextId();
        String description = Objects.requireNonNull(feedback.getDescription(), "description is required");
        Integer grade = Objects.requireNonNull(feedback.getGrade(), "grade is required");
        dynamoDb.putItem(putItemRequest(id, description, grade));
        feedback.setId(id);
        return feedback;
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
                    return response(200, FeedbackMapperUtil.toJson(findById(Long.parseLong(id))));
                }
                return response(200, FeedbackMapperUtil.toJson(findAll()));
            }

            return response(405, "Method Not Allowed");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return response(400, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return response(500, e.getMessage());
        }
    }

    private Long nextId() {
        return dynamoDb.scanPaginator(scanRequest()).items().stream()
                .map(Feedback::from)
                .map(Feedback::getId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1;
    }

    public Feedback findById(Long id) {
        System.out.println("Finding feedback with id: " + id);
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
