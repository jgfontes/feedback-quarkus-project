package org.fiap.com.models;

import org.fiap.com.FeedbackMapperUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeedbackTest {

    @Test
    void deveConverterJsonParaFeedbackComId() {
        Feedback feedback = FeedbackMapperUtil.fromJson("{\"id\":10,\"description\":\"Aula excelente\",\"grade\":9}");

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
}

