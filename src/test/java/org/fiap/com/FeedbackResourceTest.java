package org.fiap.com;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class FeedbackResourceTest {

    @Test
    void deveAdicionarEListarFeedbacks() {
        String description = "Aula de cloud " + System.currentTimeMillis();

        // POST - adicionar feedback
        Integer generatedId = given()
            .contentType(ContentType.JSON)
            .body("{\"description\":\"%s\",\"grade\":8}".formatted(description))
        .when()
            .post("/feedback")
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0))
            .body("find { it.description == '%s' && it.grade == 8 }.id".formatted(description), notNullValue())
            .extract()
            .path("find { it.description == '%s' && it.grade == 8 }.id".formatted(description));

        assertNotNull(generatedId);

        // GET - listar todos
        given()
        .when()
            .get("/feedback")
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0));

        // GET by id
        given()
        .when()
            .get("/feedback/{id}", generatedId)
        .then()
            .statusCode(200)
            .body("id", equalTo(generatedId))
            .body("description", equalTo(description))
            .body("grade", equalTo(8));
    }
}
