package org.traducao.projeto.sistema.application;

import io.quarkus.runtime.Quarkus;
import org.springframework.stereotype.Service;
import org.traducao.projeto.core.execucao.FilaExecucaoPipeline;
import org.traducao.projeto.traducao.presentation.ui.AnsiCores;

/**
 * Encerra a aplicação de forma ordenada a partir do botão "Sair" da UI.
 * <p>
 * Sequência: sinaliza parada cooperativa da fila do pipeline (o job em
 * execução encerra no próximo ponto seguro, preservando cache e arquivos já
 * concluídos), espera um curto período para a resposta HTTP chegar ao
 * navegador e então derruba o Quarkus. Se o shutdown normal não terminar o
 * processo (ex.: modo dev segura a JVM viva), um fallback força a saída.
 */
@Service
public class EncerrarAplicacaoUseCase {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EncerrarAplicacaoUseCase.class);

    /** Tempo para a resposta HTTP do /sistema/sair ser entregue antes do shutdown. */
    private static final long GRACE_RESPOSTA_MS = 700;

    /** Tempo máximo aguardando o shutdown ordenado antes de forçar a saída da JVM. */
    private static final long FALLBACK_SAIDA_MS = 10_000;

    private final FilaExecucaoPipeline filaExecucao;

    public EncerrarAplicacaoUseCase(FilaExecucaoPipeline filaExecucao) {
        this.filaExecucao = filaExecucao;
    }

    public String encerrar() {
        boolean haviaTrabalho = filaExecucao.ocupada();
        int canceladas = filaExecucao.parar();
        log.info("Encerramento solicitado pelo usuário ({} tarefa(s) cancelada(s) na fila).", canceladas);
        System.out.println(AnsiCores.YELLOW
            + "[EXIT] Encerramento solicitado pelo usuário — a aplicação será desligada."
            + (haviaTrabalho
                ? " O trabalho em execução para no próximo ponto seguro; o progresso já salvo é preservado."
                : "")
            + AnsiCores.RESET);

        Thread desligamento = new Thread(this::desligarComFallback, "encerramento-aplicacao");
        desligamento.setDaemon(true);
        desligamento.start();

        return haviaTrabalho
            ? "Encerrando a aplicação. O trabalho em execução foi interrompido no próximo ponto seguro "
                + "(" + canceladas + " tarefa(s) cancelada(s)) e o progresso já salvo foi preservado."
            : "Encerrando a aplicação. Nenhum trabalho estava em execução.";
    }

    private void desligarComFallback() {
        dormir(GRACE_RESPOSTA_MS);
        Quarkus.asyncExit(0);
        // No modo dev o Quarkus pode parar só a aplicação e manter a JVM do
        // dev-mode viva; aqui garantimos que "Sair" fecha o processo de fato.
        dormir(FALLBACK_SAIDA_MS);
        System.exit(0);
    }

    private void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
