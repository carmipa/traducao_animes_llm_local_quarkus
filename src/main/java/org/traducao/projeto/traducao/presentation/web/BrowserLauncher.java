package org.traducao.projeto.traducao.presentation.web;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Abre o navegador apos a inicializacao do Quarkus quando {@code app.modo=WEB}.
 */
@Component
public class BrowserLauncher {

    private static final Logger log = LoggerFactory.getLogger(BrowserLauncher.class);

    @ConfigProperty(name = "app.modo", defaultValue = "")
    String modoExecucao;

    @ConfigProperty(name = "quarkus.http.host", defaultValue = "127.0.0.1")
    String httpHost;

    @ConfigProperty(name = "quarkus.http.port", defaultValue = "8080")
    int httpPort;

    @ConfigProperty(name = "app.browser.auto-open", defaultValue = "true")
    boolean abrirNavegadorAutomatico;

    void onStart(@Observes StartupEvent event) {
        if (!"WEB".equals(modoExecucao)) {
            return;
        }

        String host = "0.0.0.0".equals(httpHost) ? "localhost" : httpHost;
        String url = "http://" + host + ":" + httpPort;

        System.out.println("\n==============================================================");
        System.out.println("   SERVIDOR WEB INICIADO COM SUCESSO!");
        System.out.println("   Acesse a interface visual em: \u001B[36m" + url + "\u001B[0m");
        System.out.println("==============================================================\n");

        if (abrirNavegadorAutomatico) {
            abrirNavegador(url);
        }
    }

    private void abrirNavegador(String url) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                // rundll32 é o meio nativo mais resiliente no ecossistema Java para Windows,
                // acionando o FileProtocolHandler diretamente sem envolver subprocessos de shell bloqueados do cmd.
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                log.info("Navegador aberto automaticamente via rundll32: {}", url);
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
                log.info("Navegador aberto automaticamente via open: {}", url);
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
                log.info("Navegador aberto automaticamente via xdg-open: {}", url);
            }
        } catch (IOException e) {
            log.warn("Nao foi possivel abrir o navegador automaticamente: {}", e.getMessage());
        }
    }
}
