package com.example.demo.functional;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Black-box functional tests run by Harness against the service deployed to QA.
 * Target is set with -Dapp.baseUrl=http://host:port (defaults to localhost:8080).
 */
class ProductApiFT {

    @BeforeAll
    static void configure() {
        RestAssured.baseURI = System.getProperty("app.baseUrl", "http://localhost:8080");
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void healthIsUp() {
        given().when().get("/actuator/health")
                .then().statusCode(200).body("status", equalTo("UP"));
    }

    @Test
    void infoExposesAppName() {
        given().when().get("/api/info")
                .then().statusCode(200).body("app", equalTo("springboot-harness-demo"));
    }

    @Test
    void productLifecycle() {
        Integer id = given().contentType(ContentType.JSON)
                .body("{\"name\":\"Functional Test Widget\",\"price\":9.99}")
                .when().post("/api/products")
                .then().statusCode(201).body("id", notNullValue())
                .extract().path("id");

        given().when().get("/api/products/{id}", id)
                .then().statusCode(200).body("name", equalTo("Functional Test Widget"));

        given().when().get("/api/products")
                .then().statusCode(200).body("id", hasItem(id));

        given().when().delete("/api/products/{id}", id).then().statusCode(204);
        given().when().get("/api/products/{id}", id).then().statusCode(404);
    }

    @Test
    void invalidProductIsRejected() {
        given().contentType(ContentType.JSON)
                .body("{\"name\":\"\",\"price\":0}")
                .when().post("/api/products")
                .then().statusCode(400);
    }
}
