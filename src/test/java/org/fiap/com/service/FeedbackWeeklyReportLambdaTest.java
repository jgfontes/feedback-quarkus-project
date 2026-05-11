package org.fiap.com.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.paginators.ScanIterable;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FeedbackWeeklyReportLambdaTest {

    private FeedbackWeeklyReportLambda lambda;
    private DynamoDbClient dynamoDbMock;
    private SesClient sesMock;
    private Context contextMock;

    @BeforeEach
    void setUp() throws Exception {
        lambda = new FeedbackWeeklyReportLambda();
        dynamoDbMock = mock(DynamoDbClient.class);
        sesMock = mock(SesClient.class);
        contextMock = mock(Context.class);
        when(contextMock.getLogger()).thenReturn(mock(LambdaLogger.class));
        when(sesMock.sendEmail(any(SendEmailRequest.class))).thenReturn(SendEmailResponse.builder().build());

        setField("dynamoDb", dynamoDbMock);
        setField("ses", sesMock);
    }

    private void setField(String name, Object value) throws Exception {
        Field field = FeedbackWeeklyReportLambda.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(lambda, value);
    }

    @Test
    void deveEnviarRelatorioQuandoExistemFeedbacks() {
        Map<String, AttributeValue> item1 = Map.of(
                "feedback_id", AttributeValue.builder().s("1").build(),
                "description", AttributeValue.builder().s("Boa").build(),
                "grade", AttributeValue.builder().n("8").build(),
                "createdAt", AttributeValue.builder().s(Instant.now().toString()).build()
        );
        Map<String, AttributeValue> item2 = Map.of(
                "feedback_id", AttributeValue.builder().s("2").build(),
                "description", AttributeValue.builder().s("Ruim").build(),
                "grade", AttributeValue.builder().n("3").build(),
                "createdAt", AttributeValue.builder().s(Instant.now().toString()).build()
        );

        ScanIterable scanIterable = mock(ScanIterable.class);
        when(dynamoDbMock.scanPaginator(any(ScanRequest.class))).thenReturn(scanIterable);
        when(scanIterable.items()).thenReturn(new TestSdkIterable<>(List.of(item1, item2)));

        String result = lambda.handleRequest(new ScheduledEvent(), contextMock);

        assertEquals("OK", result);
        verify(sesMock).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void deveRetornarNoDataQuandoNaoExistemFeedbacks() {
        ScanIterable scanIterable = mock(ScanIterable.class);
        when(dynamoDbMock.scanPaginator(any(ScanRequest.class))).thenReturn(scanIterable);
        when(scanIterable.items()).thenReturn(new TestSdkIterable<>(List.of()));

        String result = lambda.handleRequest(new ScheduledEvent(), contextMock);

        assertEquals("NO_DATA", result);
        verify(sesMock, never()).sendEmail(any(SendEmailRequest.class));
    }
}
