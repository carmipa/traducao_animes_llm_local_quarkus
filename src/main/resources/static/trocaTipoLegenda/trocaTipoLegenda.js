import { logNoConsole, mostrarAlerta } from '../js/app.js';

const PAINEL_HTML = 'trocaTipoLegenda/trocaTipoLegenda.html';

async function carregarPainelHtml() {
    const painel = document.getElementById('panel-troca-tipo-legenda');
    if (!painel || painel.dataset.moduloCarregado === 'true') {
        return painel;
    }

    const resposta = await fetch(PAINEL_HTML);
    if (!resposta.ok) {
        throw new Error(`Falha ao carregar ${PAINEL_HTML}`);
    }

    painel.innerHTML = await resposta.text();
    painel.dataset.moduloCarregado = 'true';
    return painel;
}

function vincularEventos() {
    const btnEscanear = document.getElementById('btn-escanear-fontes');
    const btnAplicar = document.getElementById('btn-aplicar-substituicoes');
    const btnLimpar = document.querySelector('.btn-clear-form[data-form="form-troca-tipo-legenda"]');
    const inputEntrada = document.getElementById('troca-tipo-legenda-entrada');
    
    const areaResultado = document.getElementById('area-auditoria-resultado');
    const cardCorrecao = document.getElementById('card-correcao-desbloqueado');
    const tabelaFontesCorpo = document.querySelector('#tabela-fontes tbody');
    
    const badgeTotal = document.getElementById('badge-total-arquivos');
    const badgeProblemas = document.getElementById('badge-arquivos-problemas');

    if (!btnEscanear || !inputEntrada || !tabelaFontesCorpo) return;

    // Ação do Botão: Escanear Fontes
    btnEscanear.addEventListener('click', async () => {
        const caminho = inputEntrada.value.trim();
        if (!caminho) {
            mostrarAlerta('Informe a pasta com as legendas a serem auditadas!', 'erro');
            return;
        }

        logNoConsole('console-troca-tipo-legenda', `Iniciando escaneamento de fontes em: ${caminho}`, 'info');
        btnEscanear.disabled = true;

        // Oculta cards antigos
        areaResultado.classList.add('hidden');
        cardCorrecao.classList.add('hidden');
        tabelaFontesCorpo.innerHTML = '';

        try {
            const res = await fetch('/api/troca-legenda/escanear', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ diretorioLegendas: caminho })
            });

            const data = await res.json().catch(() => ({}));

            if (!res.ok) {
                throw new Error(data.erro || 'Falha ao realizar auditoria de fontes');
            }

            // Exibir a área de resultados
            areaResultado.classList.remove('hidden');
            badgeTotal.textContent = `${data.totalArquivosAnalisados} arquivos`;
            badgeProblemas.textContent = `${data.totalComProblemas} com problemas`;
            
            if (data.totalComProblemas > 0) {
                badgeProblemas.className = 'meta-badge score'; // Destaca em vermelho/rosa
            } else {
                badgeProblemas.className = 'meta-badge genre'; // Destaca em verde
            }

            // Popula a tabela
            if (data.arquivos && data.arquivos.length > 0) {
                data.arquivos.forEach(arq => {
                    const nomeArq = arq.arquivo;
                    
                    if (!arq.fontes || arq.fontes.length === 0) {
                        const tr = document.createElement('tr');
                        tr.innerHTML = `
                            <td class="td-arquivo-legenda" title="${nomeArq}">${nomeArq}</td>
                            <td colspan="4" style="color: var(--text-muted); font-style: italic; text-align: center;">Nenhum estilo/fonte cadastrada no cabeçalho.</td>
                        `;
                        tabelaFontesCorpo.appendChild(tr);
                    } else {
                        arq.fontes.forEach((fonteInfo, idx) => {
                            const tr = document.createElement('tr');
                            if (fonteInfo.problematica) {
                                tr.className = 'linha-auditoria-problema';
                            }
                            
                            // Apenas exibe o nome do arquivo na primeira linha do arquivo para limpeza visual
                            const tdArquivo = idx === 0 
                                ? `<td class="td-arquivo-legenda" rowspan="${arq.fontes.length}" title="${nomeArq}"><strong>${nomeArq}</strong></td>`
                                : '';

                            const statusHtml = fonteInfo.problematica
                                ? `<span class="status-badge pulse-red">Risco Alto</span>`
                                : `<span class="status-badge pulse-green">Unicode Seguro</span>`;

                            const fonteSugeridaHtml = fonteInfo.problematica
                                ? `<strong style="color: var(--accent-green);">${fonteInfo.fonteSugerida}</strong>`
                                : `<span style="color: var(--text-muted);">${fonteInfo.fonteSugerida}</span>`;

                            tr.innerHTML = `
                                ${tdArquivo}
                                <td><code>${fonteInfo.estilo}</code></td>
                                <td>${fonteInfo.fonteAtual}</td>
                                <td>${fonteSugeridaHtml}</td>
                                <td>${statusHtml}</td>
                            `;
                            tabelaFontesCorpo.appendChild(tr);
                        });
                    }
                });
            } else {
                tabelaFontesCorpo.innerHTML = `<tr><td colspan="5" style="text-align:center; color: var(--text-muted);">Nenhum arquivo de legenda encontrado na pasta.</td></tr>`;
            }

            // Desbloqueia a área de alteração se houver problemas
            if (data.totalComProblemas > 0) {
                cardCorrecao.classList.remove('hidden');
                logNoConsole('console-troca-tipo-legenda', `Auditoria concluída: ${data.totalComProblemas} de ${data.totalArquivosAnalisados} arquivos possuem fontes vietnamitas legadas de alto risco. Área de substituição liberada!`, 'aviso');
                mostrarAlerta('Auditoria concluída! Fontes legadas problemáticas foram detectadas.', 'aviso');
            } else {
                logNoConsole('console-troca-tipo-legenda', `Auditoria concluída: Todos os ${data.totalArquivosAnalisados} arquivos estão com fontes Unicode seguras. Nenhuma ação necessária!`, 'sucesso');
                mostrarAlerta('Parabéns! Todas as fontes analisadas são Unicode seguras.', 'sucesso');
            }

        } catch (err) {
            logNoConsole('console-troca-tipo-legenda', `Erro: ${err.message}`, 'erro');
            mostrarAlerta(err.message, 'erro');
        } finally {
            btnEscanear.disabled = false;
        }
    });

    // Ação do Botão: Aplicar Substituições
    if (btnAplicar) {
        btnAplicar.addEventListener('click', async () => {
            const caminho = inputEntrada.value.trim();
            if (!caminho) return;

            logNoConsole('console-troca-tipo-legenda', `Solicitando substituição de fontes em lote no pipeline...`, 'info');
            btnAplicar.disabled = true;

            try {
                const res = await fetch('/api/troca-legenda/aplicar', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ diretorioLegendas: caminho })
                });

                const data = await res.json().catch(() => ({}));

                if (!res.ok) {
                    throw new Error(data.erro || 'Falha ao iniciar substituição de fontes');
                }

                logNoConsole('console-troca-tipo-legenda', data.mensagem || 'Substituição iniciada.', 'sucesso');
                mostrarAlerta('Processo de substituição de fontes iniciado! Acompanhe os logs.', 'sucesso');
                
                // Oculta a área de substituição para evitar duplo clique
                cardCorrecao.classList.add('hidden');
            } catch (err) {
                logNoConsole('console-troca-tipo-legenda', `Erro: ${err.message}`, 'erro');
                mostrarAlerta(err.message, 'erro');
                btnAplicar.disabled = false;
            }
        });
    }

    // Ação de Limpeza do Formulário
    if (btnLimpar) {
        btnLimpar.addEventListener('click', () => {
            areaResultado.classList.add('hidden');
            cardCorrecao.classList.add('hidden');
            tabelaFontesCorpo.innerHTML = '';
            logNoConsole('console-troca-tipo-legenda', 'Formulário e resultados limpos.', 'info');
        });
    }
}

export async function initTrocaTipoLegenda() {
    try {
        await carregarPainelHtml();
        vincularEventos();
        document.dispatchEvent(new CustomEvent('troca-tipo-legenda:painel-carregado'));
    } catch (err) {
        console.error('[Menu Troca Tipo Legenda] Erro ao inicializar módulo:', err);
        const painel = document.getElementById('panel-troca-tipo-legenda');
        if (painel) {
            painel.innerHTML = '<div class="glass-card"><p class="card-desc">Não foi possível carregar a interface de Troca de Tipo de Legenda.</p></div>';
        }
    }
}
