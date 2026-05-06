package org.fiap.com.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.fiap.com.models.Feedback;

public class FeedbackLambda implements RequestHandler<Feedback, String> {

    @Override
    public String handleRequest(Feedback feedback, Context context) {
        String message = "Feedback created with description: %s and grade: %s".formatted(feedback.getDescription(), feedback.getGrade());
        System.out.println(message);
        return message;
    }

}
