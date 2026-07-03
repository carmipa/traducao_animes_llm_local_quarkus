package org.traducao.projeto;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
class WebInterfaceTest {

    @Test
    void indexHtmlDisponivel() {
        given()
            .when().get("/")
            .then()
            .statusCode(200)
            .body(containsString("KRONOS CORE"));
    }

    @Test
    void cssDisponivel() {
        given()
            .when().get("/css/style.css")
            .then()
            .statusCode(200)
            .contentType(containsString("text/css"));
    }

    @Test
    void appJsDisponivel() {
        given()
            .when().get("/js/app.js")
            .then()
            .statusCode(200)
            .contentType(containsString("javascript"));
    }

    @Test
    void logoDisponivel() {
        given()
            .when().get("/img/kronos_logo.svg")
            .then()
            .statusCode(200)
            .contentType(containsString("svg"));
    }

    @Test
    void modulosJsDisponiveis() {
        String[] modulos = {
            "/analise/analise.js",
            "/extracao/extracao.js",
            "/traducao/traducao.js",
            "/correcao/correcao.js",
            "/revisao/revisao.js",
            "/cura/cura.js",
            "/revisaoLore/revisaoLore.js",
            "/remuxer/remuxer.js",
            "/mapa/mapa.js",
            "/telemetria/telemetria.js"
        };
        for (String modulo : modulos) {
            given()
                .when().get(modulo)
                .then()
                .statusCode(200)
                .contentType(containsString("javascript"));
        }
    }

    @Test
    void indexContemRevisaoLore() {
        given()
            .when().get("/")
            .then()
            .statusCode(200)
            .body(containsString("Revisão de Lore"));
    }
}
