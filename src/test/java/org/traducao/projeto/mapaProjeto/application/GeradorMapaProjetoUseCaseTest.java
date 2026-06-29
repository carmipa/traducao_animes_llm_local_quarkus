package org.traducao.projeto.mapaProjeto.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.mapaProjeto.domain.exceptions.MapaProjetoException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Files.list} (usado por {@code executar}, diferente de
 * {@code Files.walk} usado em outros use cases deste projeto) lança
 * {@code NotDirectoryException} quando o caminho informado não é um
 * diretório — e, ao contrário dos demais use cases, {@code executar} aqui
 * não tem nenhuma checagem prévia que intercepte esse caso. Isso o torna o
 * único, entre as lacunas de exceção corrigidas nesta auditoria, em que a
 * falha real é reproduzível de forma determinística e portátil num teste.
 */
@QuarkusTest
class GeradorMapaProjetoUseCaseTest {

    @Inject
    GeradorMapaProjetoUseCase geradorMapaProjetoUseCase;

    @Test
    void executarLancaMapaProjetoExceptionQuandoCaminhoNaoEhDiretorio(@TempDir Path tempDir) throws IOException {
        Path arquivoRegular = tempDir.resolve("nao-e-uma-pasta.txt");
        Files.writeString(arquivoRegular, "conteudo");

        MapaProjetoException exception = assertThrows(MapaProjetoException.class,
            () -> geradorMapaProjetoUseCase.executar(arquivoRegular));

        assertTrue(exception.getMessage().contains(arquivoRegular.toString()));
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void executarGeraMapaProjetoMdParaUmaPastaValida(@TempDir Path tempDir) throws IOException {
        geradorMapaProjetoUseCase.executar(tempDir);

        Path destino = tempDir.resolve("mapa_projeto.md");
        assertTrue(Files.exists(destino), "mapa_projeto.md deveria ter sido gerado");
        assertFalse(Files.readAllLines(destino).isEmpty(), "mapa_projeto.md gerado não deveria estar vazio");
    }
}
