import { logNoConsole, mostrarAlerta } from '../js/app.js';

export function initCura() {
    const btnIniciarCura = document.getElementById('btn-iniciar-cura');
    const inputOriginal = document.getElementById('cura-entrada-original');
    const inputTraduzida = document.getElementById('cura-entrada-traduzida');
    const selectContexto = document.getElementById('cura-contexto');

    if (!btnIniciarCura || !inputOriginal || !inputTraduzida) return;

    btnIniciarCura.addEventListener('click', async () => {
        const diretorioOriginal = inputOriginal.value.trim();
        const diretorioTraduzido = inputTraduzida.value.trim();
        if (!diretorioOriginal || !diretorioTraduzido) {
            mostrarAlerta('Informe as pastas com as legendas originais e traduzidas!', 'erro');
            return;
        }

        const contextoId = selectContexto ? selectContexto.value : '';

        logNoConsole('console-cura', `Iniciando correção de legendas. Original: ${diretorioOriginal} | Traduzida: ${diretorioTraduzido}`, 'info');
        if (contextoId) {
            logNoConsole('console-cura', `Contexto LLM ativo: ${contextoId} (também corrige erros de tradução)`, 'info');
        } else {
            logNoConsole('console-cura', 'Modo estrutural: sem LLM, usando a original apenas como referência.', 'info');
        }
        btnIniciarCura.disabled = true;
        const textoBotao = btnIniciarCura.querySelector('span');
        const textoOriginalBotao = textoBotao ? textoBotao.textContent : '';
        if (textoBotao) textoBotao.textContent = 'Corrigindo...';

        try {
            const body = { diretorioOriginal, diretorioTraduzido };
            if (contextoId) body.contextoId = contextoId;

            const res = await fetch('/api/correcao-legendas', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });

            if (!res.ok) {
                const erro = await res.json();
                if (erro.relatorioJson) {
                    logNoConsole('console-cura', `Relatório JSON: ${erro.relatorioJson}`, 'info');
                }
                if (Array.isArray(erro.detalhesErros)) {
                    erro.detalhesErros.forEach(detalhe => logNoConsole('console-cura', detalhe, 'erro'));
                }
                throw new Error(erro.erro || 'Falha desconhecida no servidor');
            }

            const data = await res.json();
            logNoConsole('console-cura', data.mensagem || 'Correção concluída.', 'sucesso');
            if (data.relatorioJson) {
                logNoConsole('console-cura', `Relatório JSON: ${data.relatorioJson}`, 'info');
            }
            mostrarAlerta('Correção de legendas finalizada!', 'sucesso');

        } catch (err) {
            logNoConsole('console-cura', `Erro durante a correção: ${err.message}`, 'erro');
            mostrarAlerta('Erro ao corrigir legendas.', 'erro');
        } finally {
            btnIniciarCura.disabled = false;
            if (textoBotao) textoBotao.textContent = textoOriginalBotao;
        }
    });
}
