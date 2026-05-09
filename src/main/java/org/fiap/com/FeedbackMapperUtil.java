package org.fiap.com;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fiap.com.models.Feedback;

public class FeedbackMapperUtil {

    private FeedbackMapperUtil() {
        /* This utility class should not be instantiated */
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static Feedback fromJson(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("body is required");
        }

        try {
            return OBJECT_MAPPER.readValue(body, Feedback.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid feedback JSON", e);
        }
    }

    public static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to convert feedback to JSON", e);
        }
    }
}
