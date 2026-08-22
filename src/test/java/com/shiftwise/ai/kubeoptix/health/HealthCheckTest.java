package com.shiftwise.ai.kubeoptix.health;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

@QuarkusTest
class HealthCheckTest {

    @Test
    void livenessProbeIsUp() {
        given()
                .when().get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks.name", hasItem("kubeoptix-configurations-live"));
    }

    @Test
    void readinessProbeIsUp() {
        given()
                .when().get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks.name", hasItem("kubeoptix-configurations-ready"));
    }

    @Test
    void startupProbeIsUp() {
        given()
                .when().get("/q/health/started")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks.name", hasItem("kubeoptix-configurations-started"));
    }
}
