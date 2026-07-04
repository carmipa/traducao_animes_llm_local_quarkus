package org.traducao.projeto.core.execucao;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fila única (single-thread) para todos os jobs pesados do pipeline —
 * tradução, correção, revisões (concordância/lore), análise, extração, remux.
 * <p>
 * Ter UMA fila compartilhada é requisito de corretude, não só de desempenho:
 * o contexto de tradução ativo ({@code GerenciadorContexto}) e o modelo LLM
 * configurado são estado global mutado no início de cada job. Quando cada
 * controller tinha seu próprio executor (ou rodava na thread HTTP), dois jobs
 * podiam rodar em paralelo e um trocava a lore/modelo no meio do outro — além
 * de disputarem a GPU do LM Studio, que atende uma inferência por vez.
 */
@Component
public class FilaExecucaoPipeline {

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "pipeline-fila-execucao");
        t.setDaemon(true);
        return t;
    });

    // Tarefas na fila + em execução; permite recusar operações síncronas
    // (ex.: correção de legendas via HTTP) em vez de deixá-las penduradas
    // atrás de uma tradução de horas.
    private final AtomicInteger tarefasAtivas = new AtomicInteger();

    public Future<?> submeter(Runnable tarefa) {
        tarefasAtivas.incrementAndGet();
        return executor.submit(() -> {
            try {
                tarefa.run();
            } finally {
                tarefasAtivas.decrementAndGet();
            }
        });
    }

    /**
     * Executa a tarefa na fila e bloqueia até o resultado — para endpoints que
     * respondem o resultado no próprio request HTTP. Verifique
     * {@link #ocupada()} antes, para não bloquear atrás de um job longo.
     */
    public <T> T executarEAguardar(Callable<T> tarefa) throws Exception {
        tarefasAtivas.incrementAndGet();
        Future<T> future = executor.submit(() -> {
            try {
                return tarefa.call();
            } finally {
                tarefasAtivas.decrementAndGet();
            }
        });
        try {
            return future.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception causa) {
                throw causa;
            }
            throw e;
        }
    }

    public boolean ocupada() {
        return tarefasAtivas.get() > 0;
    }

    @PreDestroy
    void encerrar() {
        executor.shutdownNow();
    }
}
