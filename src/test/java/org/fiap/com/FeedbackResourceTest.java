package org.fiap.com;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class FeedbackResourceTest {

    @Test
    void deveAdicionarEListarFeedbacks() {
        // POST - adicionar feedback
        given()
            .contentType(ContentType.JSON)
            .body("{\"id\":1,\"description\":\"Aula de cloud\",\"grade\":8}")
        .when()
            .post("/feedback")
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0));

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
            .get("/feedback/{id}", 1)
        .then()
            .statusCode(200)
            .body("id", equalTo(1))
            .body("description", equalTo("Aula de cloud"))
            .body("grade", equalTo(8));
    }
}
