package org.fiap.com.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FeedbackNotifyLambdaTest {

    private FeedbackNotifyLambda lambda;
    private SesClient sesMock;
    private Context contextMock;

    @BeforeEach
    void setUp() throws Exception {
        lambda = new FeedbackNotifyLambda();
        sesMock = mock(SesClient.class);
        contextMock = mock(Context.class);
        when(contextMock.getLogger()).thenReturn(mock(LambdaLogger.class));
        when(sesMock.sendEmail(any(SendEmailRequest.class))).thenReturn(SendEmailResponse.builder().build());

        Field field = FeedbackNotifyLambda.class.getDeclaredField("ses");
        field.setAccessible(true);
        field.set(lambda, sesMock);
    }

    @Test
    void deveEnviarEmailParaFeedbackCritico() {
        ScheduledEvent event = new ScheduledEvent();
        event.setDetail(Map.of(
                "feedbackId", "abc-123",
                "description", "Aula confusa",
                "grade", 3,
                "urgency", "HIGH",
                "createdAt", "2026-05-11T12:00:00Z"
        ));

        String result = lambda.handleRequest(event, contextMock);

        assertEquals("OK", result);
        verify(sesMock).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void deveFalharQuandoEventoInvalido() {
        ScheduledEvent event = new ScheduledEvent();
        event.setDetail(Map.of());

        assertThrows(RuntimeException.class, () -> lambda.handleRequest(event, contextMock));
    }
}
