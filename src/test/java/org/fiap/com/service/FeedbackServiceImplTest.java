package org.fiap.com.service;

import org.fiap.com.models.Feedback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FeedbackServiceImplTest {

    private FeedbackServiceImpl service;
    private DynamoDbClient dynamoDbMock;

    @BeforeEach
    void setUp() throws Exception {
        service = new FeedbackServiceImpl();
        dynamoDbMock = mock(DynamoDbClient.class);
        when(dynamoDbMock.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());
        Field field = FeedbackServiceImpl.class.getDeclaredField("dynamoDb");
        field.setAccessible(true);
        field.set(service, dynamoDbMock);
    }

    @Test
    void deveAdicionarFeedbackValido() {
        Feedback feedback = new Feedback();
        feedback.setDescription("Aula excelente");
        feedback.setGrade(9);

        Feedback result = service.add(feedback);

        assertNotNull(result.getId());
        assertNotNull(result.getCreatedAt());
        verify(dynamoDbMock).putItem(any(PutItemRequest.class));
    }

    @Test
    void deveFalharQuandoDescriptionNula() {
        Feedback feedback = new Feedback();
        feedback.setGrade(5);

        assertThrows(NullPointerException.class, () -> service.add(feedback));
    }

    @Test
    void deveFalharQuandoDescriptionCurta() {
        Feedback feedback = new Feedback();
        feedback.setDescription("ab");
        feedback.setGrade(5);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.add(feedback));
        assertTrue(ex.getMessage().contains("at least 3 characters"));
    }

    @Test
    void deveFalharQuandoGradeNula() {
        Feedback feedback = new Feedback();
        feedback.setDescription("Boa aula");

        assertThrows(NullPointerException.class, () -> service.add(feedback));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 11, 15, -5})
    void deveFalharQuandoGradeForaDoRange(int grade) {
        Feedback feedback = new Feedback();
        feedback.setDescription("Boa aula");
        feedback.setGrade(grade);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.add(feedback));
        assertTrue(ex.getMessage().contains("between 0 and 10"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 5, 10})
    void deveAceitarGradesDentroDoRange(int grade) {
        Feedback feedback = new Feedback();
        feedback.setDescription("Boa aula");
        feedback.setGrade(grade);

        Feedback result = service.add(feedback);
        assertNotNull(result.getId());
    }
}
