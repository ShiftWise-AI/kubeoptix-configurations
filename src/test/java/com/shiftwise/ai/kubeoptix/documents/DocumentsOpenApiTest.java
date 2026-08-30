package com.shiftwise.ai.kubeoptix.documents;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class DocumentsOpenApiTest {

    @Test
    void openApiContainsDocumentCrudEndpoints() {
        given()
                .accept(MediaType.APPLICATION_JSON)
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
                .body(containsString("delete"))
                .body("components.schemas.DocumentRequest.properties", not(hasKey("markdownContent")))
                .body("components.schemas.VersionRequest.properties", hasKey("markdownContent"));
    }
}