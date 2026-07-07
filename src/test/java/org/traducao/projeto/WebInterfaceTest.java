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
            .when().get("/css/base.css")
            .then()
            .statusCode(200)
            .contentType(containsString("text/css"))
            .body(containsString("--bg-primary"));
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
            "/auditorConteudoLegendas/auditorConteudoLegendas.js",
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
    void revisaoLoreHtmlDisponivel() {
        given()
            .when().get("/revisaoLore/revisaoLore.html")
            .then()
            .statusCode(200)
            .contentType(containsString("html"))
            .body(containsString("Revisão de Lore"));
    }

    @Test
    void indexContemRevisaoLore() {
        given()
            .when().get("/")
            .then()
            .statusCode(200)
            .body(containsString("data-modulo=\"revisaoLore\""));
    }

    @Test
    void indexContemAuditorConteudoModulo() {
        given()
            .when().get("/")
            .then()
            .statusCode(200)
            .body(containsString("data-modulo=\"auditorConteudoLegendas\""));
    }

    @Test
    void auditorConteudoHtmlDisponivel() {
        given()
            .when().get("/auditorConteudoLegendas/auditorConteudoLegendas.html")
            .then()
            .statusCode(200)
            .contentType(containsString("html"))
            .body(containsString("Análise de Conteúdo de Legendas"))
            .body(containsString("id=\"btn-exportar-auditor-md\""))
            .body(containsString("Relatório de Anomalias"));
    }

    @Test
    void indexSidebarComEstruturaNavMenuValida() {
        String html = given()
            .when().get("/")
            .then()
            .statusCode(200)
            .extract().asString();

        int abreNavMenu = html.split("<nav class=\"nav-menu\">", -1).length - 1;
        int fechaNav = html.split("</nav>", -1).length - 1;
        org.junit.jupiter.api.Assertions.assertEquals(1, abreNavMenu, "Deve haver exatamente um nav-menu");
        org.junit.jupiter.api.Assertions.assertTrue(fechaNav >= 1, "nav-menu deve fechar corretamente");

        String[] grupos = {"preparacao", "traducao", "qualidade", "finalizacao", "sistema"};
        for (String grupo : grupos) {
            org.junit.jupiter.api.Assertions.assertTrue(
                html.contains("data-grupo=\"" + grupo + "\""),
                "Grupo do menu ausente: " + grupo
            );
            org.junit.jupiter.api.Assertions.assertTrue(
                html.contains("data-grupo=\"" + grupo + "\"") && html.contains("nav-group-itens"),
                "Menu deve usar nav-group-itens nos grupos"
            );
        }

        org.junit.jupiter.api.Assertions.assertTrue(
            html.contains("data-target=\"auditor-conteudo\""),
            "Item de menu da auditoria de conteudo ausente"
        );
        org.junit.jupiter.api.Assertions.assertTrue(
            html.contains("data-modulo=\"auditorConteudoLegendas\""),
            "Shell do modulo auditoria de conteudo ausente no index"
        );

        int grupoPreparacao = html.indexOf("data-grupo=\"preparacao\"");
        int grupoTraducao = html.indexOf("data-grupo=\"traducao\"");
        int grupoQualidade = html.indexOf("data-grupo=\"qualidade\"");
        int grupoFinalizacao = html.indexOf("data-grupo=\"finalizacao\"");
        int itemAuditor = html.indexOf("data-target=\"auditor-conteudo\"");
        org.junit.jupiter.api.Assertions.assertTrue(
            itemAuditor > grupoQualidade && itemAuditor < grupoFinalizacao,
            "Análise de Conteúdo deve ficar no grupo Qualidade"
        );
        org.junit.jupiter.api.Assertions.assertTrue(
            grupoPreparacao < grupoTraducao && grupoTraducao < grupoQualidade,
            "Ordem dos grupos principais do pipeline ficou inconsistente"
        );
        org.junit.jupiter.api.Assertions.assertTrue(
            html.contains("<span>5. Análise de Conteúdo</span>"),
            "Numeração da Análise de Conteúdo deve refletir etapa de Qualidade"
        );
    }
}
