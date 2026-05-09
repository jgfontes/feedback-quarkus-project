package org.fiap.com.models;

import org.fiap.com.FeedbackMapperUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeedbackTest {

    @Test
    void deveConverterJsonParaFeedbackComId() {
        Feedback feedback = FeedbackMapperUtil.fromJson("{\"id\":10,\"description\":\"Aula excelente\",\"grade\":9}");

        assertEquals(10L, feedback.getId());
        assertEquals("Aula excelente", feedback.getDescription());
        assertEquals(9, feedback.getGrade());
    }

    @Test
    void deveConverterJsonParaFeedbackSemId() {
        Feedback feedback = FeedbackMapperUtil.fromJson("{\"description\":\"Aula boa\",\"grade\":8}");

        assertNull(feedback.getId());
        assertEquals("Aula boa", feedback.getDescription());
        assertEquals(8, feedback.getGrade());
    }

    @Test
    void deveFalharQuandoJsonInvalido() {
        assertThrows(IllegalArgumentException.class, () -> FeedbackMapperUtil.fromJson("{descricao"));
    }
}

