package org.fiap.com.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.fiap.com.models.Feedback;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class FeedbackServiceImpl extends AbstractFeedbackService {

    @Inject
    DynamoDbClient dynamoDb;

    public List<Feedback> findAll() {
        return dynamoDb.scanPaginator(scanRequest()).items().stream()
                .map(Feedback::from).toList();
    }

    public void add(Feedback feedback) {
        Long id = Objects.requireNonNull(feedback.getId(), "id is required");
        String description = Objects.requireNonNull(feedback.getDescription(), "description is required");
        Integer grade = Objects.requireNonNull(feedback.getGrade(), "grade is required");
        dynamoDb.putItem(putItemRequest(id, description, grade));
    }

    public Feedback findById(Long id) {
        return Feedback.from(dynamoDb.getItem(getRequest(id)).item());
    }
}
