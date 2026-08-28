package com.shiftwise.ai.kubeoptix.documents;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class DocumentsOpenApiTest {

    @Test
    void openApiContainsDocumentCrudEndpoints() {
        given()
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body(containsString("/authors"))
                .body(containsString("/costumers-list"))
                .body(containsString("/versions"))
                .body(containsString("/documents"))
                .body(containsString("documentName"))
                .body(containsString("markdownContent"))
                .body(containsString("costumer"))
                .body(containsString("costumersListId"))
                .body(containsString("post"))
                .body(containsString("put"))
                .body(containsString("delete"));
    }
}