package org.fiap.com.models;

import org.fiap.com.FeedbackMapperUtil;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FeedbackTest {

    @Test
    void deveConverterJsonParaFeedbackComId() {
        Feedback feedback = FeedbackMapperUtil.fromJson("{\"id\":\"10\",\"description\":\"Aula excelente\",\"grade\":9}");

        assertNotNull(feedback.getId());
        assertEquals("Aula excelente", feedback.getDescription());
        assertEquals(9, feedback.getGrade());
    }

    @Test
    void deveConverterJsonParaFeedbackSemId() {
        Feedback feedback = FeedbackMapperUtil.fromJson("{\"description\":\"Aula boa\",\"grade\":8}");

        assertNull(feedback.getId());
        assertEquals("Aula boa", feedback.getDescription());
        assertNotNull(feedback.getGrade());
    }

    @Test
    void deveFalharQuandoJsonInvalido() {
        assertThrows(IllegalArgumentException.class, () -> FeedbackMapperUtil.fromJson("{descricao"));
    }

    @Test
    void deveFalharQuandoBodyNuloOuVazio() {
        assertThrows(IllegalArgumentException.class, () -> FeedbackMapperUtil.fromJson(null));
        assertThrows(IllegalArgumentException.class, () -> FeedbackMapperUtil.fromJson(""));
        assertThrows(IllegalArgumentException.class, () -> FeedbackMapperUtil.fromJson("   "));
    }

    @Test
    void deveConverterMapaDynamoDbParaFeedback() {
        Map<String, AttributeValue> item = Map.of(
                "feedback_id", AttributeValue.builder().s("abc-123").build(),
                "description", AttributeValue.builder().s("Boa aula").build(),
                "grade", AttributeValue.builder().n("8").build(),
                "createdAt", AttributeValue.builder().s("2026-05-11T12:00:00Z").build()
        );

        Feedback feedback = Feedback.from(item);

        assertEquals("abc-123", feedback.getId());
        assertEquals("Boa aula", feedback.getDescription());
        assertEquals(8, feedback.getGrade());
        assertEquals("2026-05-11T12:00:00Z", feedback.getCreatedAt());
    }

    @Test
    void deveRetornarFeedbackVazioQuandoMapaVazio() {
        Feedback feedback = Feedback.from(Map.of());

        assertNull(feedback.getId());
        assertNull(feedback.getDescription());
        assertNull(feedback.getGrade());
    }

    @Test
    void deveRetornarUrgenciaHighQuandoNotaMenorQue6() {
        Feedback feedback = new Feedback();
        feedback.setGrade(5);
        assertEquals("HIGH", feedback.getUrgency());

        feedback.setGrade(0);
        assertEquals("HIGH", feedback.getUrgency());
    }

    @Test
    void deveRetornarUrgenciaNormalQuandoNotaMaiorOuIgual6() {
        Feedback feedback = new Feedback();
        feedback.setGrade(6);
        assertEquals("NORMAL", feedback.getUrgency());

        feedback.setGrade(10);
        assertEquals("NORMAL", feedback.getUrgency());
    }

    @Test
    void deveRetornarUrgenciaNulaQuandoGradeNula() {
        Feedback feedback = new Feedback();
        assertNull(feedback.getUrgency());
    }
}
