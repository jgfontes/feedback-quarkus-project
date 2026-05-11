package org.fiap.com.models;

import io.quarkus.runtime.annotations.RegisterForReflection;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

@RegisterForReflection
public class Feedback {

    private String id;
    private String description;
    private Integer grade;
    private String createdAt;

    public Feedback() {
    }

    public static Feedback from(Map<String, AttributeValue> item) {
        Feedback feedback = new Feedback();
        if (item.containsKey("feedback_id")) {
            feedback.setId(item.get("feedback_id").s());
        }
        if (item.containsKey("description")) {
            feedback.setDescription(item.get("description").s());
        }
        if (item.containsKey("grade")) {
            feedback.setGrade(Integer.parseInt(item.get("grade").n()));
        }
        if (item.containsKey("createdAt")) {
            feedback.setCreatedAt(item.get("createdAt").s());
        }
        return feedback;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getUrgency() {
        if (grade == null) return null;
        return grade < 6 ? "HIGH" : "NORMAL";
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
