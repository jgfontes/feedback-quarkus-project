package org.fiap.com.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.annotations.RegisterForReflection;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

@RegisterForReflection
public class Feedback {

    private Long id;
    private String description;
    private Integer grade;

    public Feedback() {
    }

    public static Feedback from(Map<String, AttributeValue> item) {
        Feedback feedback = new Feedback();
        if (item.containsKey("id")) {
            feedback.setId(Long.parseLong(item.get("id").n()));
        }
        if (item.containsKey("description")) {
            feedback.setDescription(item.get("description").s());
        }
        if (item.containsKey("grade")) {
            feedback.setGrade(Integer.parseInt(item.get("grade").n()));
        }
        return feedback;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }
}

