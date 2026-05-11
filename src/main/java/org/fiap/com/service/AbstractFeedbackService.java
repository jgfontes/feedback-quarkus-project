package org.fiap.com.service;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.Map;

public abstract class AbstractFeedbackService {

    public static final String FEEDBACK_TABLE_NAME = "feedback";
    public static final String FEEDBACK_ID_COL = "feedback_id";
    public static final String FEEDBACK_DESCRIPTION_COL = "description";
    public static final String FEEDBACK_GRADE_COL = "grade";
    public static final String FEEDBACK_CREATED_AT_COL = "createdAt";

    protected ScanRequest scanRequest() {
        return ScanRequest.builder()
                .tableName(FEEDBACK_TABLE_NAME)
                .build();
    }

    protected PutItemRequest putItemRequest(String id, String description, Integer grade, String createdAt) {
        return PutItemRequest.builder()
                .tableName(FEEDBACK_TABLE_NAME)
                .item(Map.of(
                        FEEDBACK_ID_COL, AttributeValue.builder().s(id).build(),
                        FEEDBACK_DESCRIPTION_COL, AttributeValue.builder().s(description).build(),
                        FEEDBACK_GRADE_COL, AttributeValue.builder().n(grade.toString()).build(),
                        FEEDBACK_CREATED_AT_COL, AttributeValue.builder().s(createdAt).build()))
                .build();
    }

    protected GetItemRequest getRequest(String id) {
        return GetItemRequest.builder()
                .tableName(FEEDBACK_TABLE_NAME)
                .key(Map.of(FEEDBACK_ID_COL, AttributeValue.builder().s(id).build()))
                .build();
    }
}
