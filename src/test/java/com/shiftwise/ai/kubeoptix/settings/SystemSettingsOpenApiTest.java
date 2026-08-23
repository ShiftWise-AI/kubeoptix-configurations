package com.shiftwise.ai.kubeoptix.settings;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class SystemSettingsOpenApiTest {

    @Test
    void openApiContainsSystemSettingsEndpoints() {
        given()
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body(containsString("/system-settings"))
                .body(containsString("/system-settings/status"))
                .body(containsString("Settings payload"));
    }
}