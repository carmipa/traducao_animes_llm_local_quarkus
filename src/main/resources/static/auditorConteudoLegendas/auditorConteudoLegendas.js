import { logNoConsole, mostrarAlerta } from '../js/app.js?v=3.4';

const PAINEL_HTML = 'auditorConteudoLegendas/auditorConteudoLegendas.html?v=3.5';

const ORDEM_SEVERIDADE = { CRITICAL: 0, ERROR: 1, WARNING: 2 };

// Configuração visual única por severidade (ícone, rótulo e sufixo de classe CSS)
const CONFIG_SEVERIDADE = {
    CRITICAL: { icone: 'gpp_bad', rotulo: 'Crítico', classe: 'critical' },
    ERROR: { icone: 'error', rotulo: 'Erro', classe: 'error' },
    WARNING: { icone: 'warning', rotulo: 'Aviso', classe: 'warning' }
};

let ultimoRelatorio = null;
let filtroAtual = 'TODOS';

async function carregarPainelHtml() {
    const painel = document.getElementById('panel-auditor-conteudo');
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

export async function initAuditorConteudo() {
    try {
        await carregarPainelHtml();
        vincularEventos();
    } catch (err) {
        console.error('[Análise de Conteúdo] Erro ao carregar painel:', err);
        const painel = document.getElementById('panel-auditor-conteudo');
        if (painel) {
            painel.innerHTML = '<div class="glass-card"><p class="card-desc">Não foi possível carregar o painel de Análise de Conteúdo.</p></div>';
        }
    }
}

function vincularEventos() {
    const formAuditor = document.getElementById('form-auditor-conteudo');
    const lista = document.getElementById('auditor-anomalias-lista');
    const statusBadge = document.getElementById('auditor-status-badge');
    const btnExportarMd = document.getElementById('btn-exportar-auditor-md');
    const btnExportarJson = document.getElementById('btn-exportar-auditor-json');
    const btnLimpar = document.getElementById('btn-limpar-auditor');
    const CONSOLE_ID = 'console-auditor-conteudo';

    if (btnExportarMd) {
        btnExportarMd.addEventListener('click', () => exportarRelatorio('md'));
    }
    if (btnExportarJson) {
        btnExportarJson.addEventListener('click', () => exportarRelatorio('json'));
    }
    if (btnLimpar) {
        btnLimpar.addEventListener('click', () => {
            setTimeout(() => resetarRelatorioVisual(lista, statusBadge), 0);
        });
    }

    if (!formAuditor || !lista || !statusBadge) return;

    formAuditor.addEventListener('submit', async (e) => {
        e.preventDefault();

        const original = document.getElementById('auditor-original').value.trim();
        const traduzido = document.getElementById('auditor-traduzido').value.trim();

        if (!original || !traduzido) {
            mostrarAlerta('Forneça os caminhos dos arquivos original e traduzido.', 'aviso');
            return;
        }

        ultimoRelatorio = null;
        filtroAtual = 'TODOS';
        ocultarResumoFiltrosELimpo();
        statusBadge.textContent = 'Auditando...';
        statusBadge.className = 'status-badge pulse-purple';
        lista.innerHTML = '<div class="auditor-lista-vazia">Analisando arquivos...</div>';
        logNoConsole(CONSOLE_ID, 'Iniciando auditoria de conteúdo...', 'info');

        try {
            const response = await fetch('/api/auditoria-conteudo', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    caminhoOriginal: original,
                    caminhoTraduzido: traduzido
                })
            });

            if (!response.ok) {
                const errMsg = await response.text();
                throw new Error(errMsg || 'Falha ao auditar.');
            }

            const relatorio = await response.json();
            ultimoRelatorio = relatorio;
            renderizarRelatorio(relatorio, lista, statusBadge);

            if (relatorio.limpo) {
                logNoConsole(CONSOLE_ID, 'Auditoria concluída — nenhuma anomalia detectada.', 'sucesso');
            } else {
                logNoConsole(CONSOLE_ID, `Auditoria concluída — ${relatorio.anomalias.length} anomalia(s) encontrada(s).`, 'aviso');
            }
            if (relatorio.caminhoRelatorioJson) {
                logNoConsole(CONSOLE_ID, `Relatório JSON salvo em disco: ${relatorio.caminhoRelatorioJson}`, 'info');
            }
        } catch (err) {
            ultimoRelatorio = null;
            ocultarResumoFiltrosELimpo();
            statusBadge.textContent = 'Erro';
            statusBadge.className = 'status-badge pulse-red';
            lista.innerHTML = `<div class="auditor-lista-vazia auditor-lista-erro">Erro: ${escapeHtml(err.message)}</div>`;
            logNoConsole(CONSOLE_ID, `Erro na auditoria: ${err.message}`, 'erro');
            console.error(err);
        }
    });
}

function resetarRelatorioVisual(lista, statusBadge) {
    ultimoRelatorio = null;
    filtroAtual = 'TODOS';
    ocultarResumoFiltrosELimpo();
    if (statusBadge) {
        statusBadge.textContent = 'Aguardando...';
        statusBadge.className = 'status-badge';
    }
    if (lista) {
        lista.innerHTML = '<div class="auditor-lista-vazia">Nenhuma auditoria realizada ainda. Execute a auditoria para ver o relatório aqui.</div>';
    }
}

function ocultarResumoFiltrosELimpo() {
    const resumo = document.getElementById('auditor-resumo');
    const filtros = document.getElementById('auditor-filtros');
    const limpo = document.getElementById('auditor-alerta-limpo');
    if (resumo) {
        resumo.innerHTML = '';
        resumo.classList.add('hidden');
    }
    if (filtros) {
        filtros.innerHTML = '';
        filtros.classList.add('hidden');
    }
    if (limpo) limpo.classList.add('hidden');
}

function renderizarResumo(relatorio) {
    const resumo = document.getElementById('auditor-resumo');
    if (!resumo) return;

    resumo.innerHTML = `
        <div class="auditor-resumo-grid">
            <div class="auditor-resumo-item">
                <span class="auditor-resumo-label">Original</span>
                <strong class="auditor-resumo-valor" title="${escapeHtml(relatorio.arquivoOriginal || '')}">${escapeHtml(relatorio.arquivoOriginal || '—')}</strong>
            </div>
            <div class="auditor-resumo-item">
                <span class="auditor-resumo-label">Traduzido</span>
                <strong class="auditor-resumo-valor" title="${escapeHtml(relatorio.arquivoTraduzido || '')}">${escapeHtml(relatorio.arquivoTraduzido || '—')}</strong>
            </div>
            <div class="auditor-resumo-item">
                <span class="auditor-resumo-label">Regras</span>
                <strong class="auditor-resumo-valor">${relatorio.regrasExecutadas ?? '—'}</strong>
            </div>
            <div class="auditor-resumo-item">
                <span class="auditor-resumo-label">Duração</span>
                <strong class="auditor-resumo-valor">${relatorio.duracaoMs ?? 0} ms</strong>
            </div>
        </div>
    `;
    resumo.classList.remove('hidden');
}

// Chips de filtro por severidade com contagem (Crítico · Erro · Aviso · Todos)
function renderizarFiltros(relatorio, lista) {
    const filtros = document.getElementById('auditor-filtros');
    if (!filtros) return;

    const contagem = contarPorSeveridade(relatorio.anomalias || []);
    const total = (relatorio.anomalias || []).length;
    filtros.innerHTML = '';

    const definicoes = [
        { chave: 'CRITICAL', icone: CONFIG_SEVERIDADE.CRITICAL.icone, rotulo: 'Crítico', quantidade: contagem.CRITICAL, classe: 'critical' },
        { chave: 'ERROR', icone: CONFIG_SEVERIDADE.ERROR.icone, rotulo: 'Erro', quantidade: contagem.ERROR, classe: 'error' },
        { chave: 'WARNING', icone: CONFIG_SEVERIDADE.WARNING.icone, rotulo: 'Aviso', quantidade: contagem.WARNING, classe: 'warning' },
        { chave: 'TODOS', icone: 'filter_list', rotulo: 'Todos', quantidade: total, classe: 'todos' }
    ];

    definicoes.forEach(def => {
        const chip = document.createElement('button');
        chip.type = 'button';
        chip.className = `auditor-filtro-chip auditor-filtro-${def.classe}`;
        chip.dataset.severidade = def.chave;
        chip.setAttribute('aria-pressed', String(filtroAtual === def.chave));
        if (filtroAtual === def.chave) chip.classList.add('ativo');
        chip.innerHTML = `<span class="material-symbols-outlined">${def.icone}</span>${def.rotulo}<span class="auditor-filtro-contagem">${def.quantidade}</span>`;

        chip.addEventListener('click', () => {
            filtroAtual = def.chave;
            filtros.querySelectorAll('.auditor-filtro-chip').forEach(c => {
                const ativo = c.dataset.severidade === filtroAtual;
                c.classList.toggle('ativo', ativo);
                c.setAttribute('aria-pressed', String(ativo));
            });
            renderizarLista(relatorio, lista);
        });

        filtros.appendChild(chip);
    });

    filtros.classList.remove('hidden');
}

function renderizarRelatorio(relatorio, lista, statusBadge) {
    renderizarResumo(relatorio);

    const alertaLimpo = document.getElementById('auditor-alerta-limpo');

    // Estado "limpo" só quando realmente não há nenhuma anomalia
    if (relatorio.limpo) {
        statusBadge.textContent = 'Limpo';
        statusBadge.className = 'status-badge pulse-green';
        if (alertaLimpo) alertaLimpo.classList.remove('hidden');
        lista.innerHTML = '<div class="auditor-lista-vazia">Nenhuma anomalia para listar.</div>';
        return;
    }

    if (alertaLimpo) alertaLimpo.classList.add('hidden');
    const total = relatorio.anomalias.length;
    statusBadge.textContent = total === 1 ? '1 ANOMALIA' : `${total} ANOMALIAS`;
    statusBadge.className = 'status-badge pulse-red';

    renderizarFiltros(relatorio, lista);
    renderizarLista(relatorio, lista);
}

function renderizarLista(relatorio, lista) {
    const ordenadas = [...relatorio.anomalias].sort((a, b) => {
        const pa = ORDEM_SEVERIDADE[a.severidade] ?? 99;
        const pb = ORDEM_SEVERIDADE[b.severidade] ?? 99;
        return pa - pb;
    });

    const visiveis = filtroAtual === 'TODOS'
        ? ordenadas
        : ordenadas.filter(anom => anom.severidade === filtroAtual);

    lista.innerHTML = '';

    if (!visiveis.length) {
        lista.innerHTML = '<div class="auditor-lista-vazia">Nenhuma anomalia nesta severidade.</div>';
        return;
    }

    visiveis.forEach(anom => lista.appendChild(criarCardAnomalia(anom)));
}

// Card fechado de anomalia: faixa de severidade, cabeçalho, mensagem, diff empilhado e dica
function criarCardAnomalia(anom) {
    const cfg = CONFIG_SEVERIDADE[anom.severidade] || CONFIG_SEVERIDADE.WARNING;

    const card = document.createElement('article');
    card.className = `auditor-anomalia-card auditor-anomalia-${cfg.classe}`;

    const head = document.createElement('header');
    head.className = 'auditor-anomalia-head';

    const pill = document.createElement('span');
    pill.className = `auditor-sev-pill auditor-sev-${cfg.classe}`;
    pill.innerHTML = `<span class="material-symbols-outlined">${cfg.icone}</span>${cfg.rotulo}`;

    const regra = document.createElement('span');
    regra.className = 'auditor-anomalia-regra';
    regra.innerHTML = `<span class="material-symbols-outlined">bug_report</span>${escapeHtml(anom.regra || '—')}`;

    head.append(pill, regra);

    const numLinha = anom.eventoTraduzido?.indice ?? anom.eventoOriginal?.indice;
    if (numLinha != null) {
        const badgeLinha = document.createElement('span');
        badgeLinha.className = 'auditor-anomalia-linha';
        badgeLinha.textContent = `LINHA #${numLinha}`;
        head.appendChild(badgeLinha);
    }

    card.appendChild(head);

    if (anom.descricao) {
        const msg = document.createElement('p');
        msg.className = 'auditor-anomalia-msg';
        msg.textContent = anom.descricao;
        card.appendChild(msg);
    }

    // Diff empilhado: bloco ORIGINAL (azul) sobre bloco TRADUZIDO (vermelho)
    if (anom.eventoOriginal?.texto || anom.eventoTraduzido?.texto) {
        const diff = document.createElement('div');
        diff.className = 'auditor-anomalia-diff';
        if (anom.eventoOriginal?.texto) {
            diff.appendChild(criarTrechoEvento('Original', anom.eventoOriginal, 'auditor-evento-orig'));
        }
        if (anom.eventoTraduzido?.texto) {
            diff.appendChild(criarTrechoEvento('Traduzido', anom.eventoTraduzido, 'auditor-evento-trad'));
        }
        card.appendChild(diff);
    }

    if (anom.sugestaoCorrecao) {
        const dica = document.createElement('footer');
        dica.className = 'auditor-anomalia-dica';
        dica.innerHTML = `<span class="material-symbols-outlined">lightbulb</span><span>${escapeHtml(anom.sugestaoCorrecao)}</span>`;
        card.appendChild(dica);
    }

    return card;
}

function criarTrechoEvento(rotulo, evento, classeExtra) {
    const bloco = document.createElement('div');
    bloco.className = `auditor-evento ${classeExtra}`;

    const titulo = document.createElement('div');
    titulo.className = 'auditor-evento-titulo';
    const icone = rotulo === 'Original' ? 'description' : 'translate';
    titulo.innerHTML = `<span class="material-symbols-outlined">${icone}</span><span>${rotulo} · linha #${evento.indice}</span>`;

    const pre = document.createElement('div');
    pre.className = 'auditor-evento-texto';
    pre.innerHTML = formatarTextoAssHtml(evento.texto);

    bloco.append(titulo, pre);
    return bloco;
}

function formatarTextoAss(texto) {
    if (!texto) return '';
    return texto.replace(/\\N/g, '\n');
}

function formatarTextoAssHtml(texto) {
    const escapado = escapeHtml(formatarTextoAss(texto));
    return escapado.replace(/\{[^}]*\}/g, '<span class="ass-tag-code">$&</span>');
}

function rotuloSeveridade(sev) {
    if (sev === 'CRITICAL') return 'Crítico';
    if (sev === 'ERROR') return 'Erro';
    if (sev === 'WARNING') return 'Aviso';
    return sev || '—';
}

function escapeHtml(unsafe) {
    if (unsafe == null) return '';
    return String(unsafe)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function exportarRelatorio(formato) {
    if (!ultimoRelatorio) {
        mostrarAlerta('Execute uma auditoria antes de exportar o relatório.', 'aviso');
        return;
    }

    const nomeBase = extrairNomeArquivo(ultimoRelatorio.arquivoTraduzido || 'auditoria');
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);

    if (formato === 'json') {
        baixarArquivo(JSON.stringify(ultimoRelatorio, null, 2), `auditoria_conteudo_${nomeBase}_${timestamp}.json`, 'application/json;charset=utf-8');
        mostrarAlerta('Relatório JSON exportado.', 'sucesso');
        return;
    }

    baixarArquivo(gerarRelatorioMarkdown(ultimoRelatorio), `auditoria_conteudo_${nomeBase}_${timestamp}.md`, 'text/markdown;charset=utf-8');
    mostrarAlerta('Relatório Markdown (.md) exportado.', 'sucesso');
}

function gerarRelatorioMarkdown(relatorio) {
    const agora = new Date().toLocaleString('pt-BR');
    const linhas = [
        '# Relatório de Auditoria de Conteúdo — KRONOS CORE',
        '',
        'Relatório didático para revisão humana das legendas `.ass` traduzidas.',
        '',
        '## Contexto da auditoria',
        '',
        '| Campo | Valor |',
        '| --- | --- |',
        `| **Data** | ${agora} |`,
        `| **Arquivo original** | \`${relatorio.arquivoOriginal || '—'}\` |`,
        `| **Arquivo traduzido** | \`${relatorio.arquivoTraduzido || '—'}\` |`,
        `| **Regras executadas** | ${relatorio.regrasExecutadas ?? '—'} |`,
        `| **Duração** | ${relatorio.duracaoMs ?? 0} ms |`,
        `| **Resultado** | ${relatorio.limpo ? 'Limpo (sem anomalias)' : `${relatorio.anomalias.length} anomalia(s)`} |`
    ];

    if (relatorio.caminhoRelatorioJson) {
        linhas.push(`| **JSON em disco** | \`${relatorio.caminhoRelatorioJson}\` |`);
    }
    linhas.push('');

    if (relatorio.limpo) {
        linhas.push('## Conclusão', '', 'Nenhuma anomalia foi detectada.', '');
        return linhas.join('\n');
    }

    const resumo = contarPorSeveridade(relatorio.anomalias);
    linhas.push(
        '## Resumo por severidade', '',
        '| Severidade | Quantidade |',
        '| --- | ---: |',
        `| Crítico | ${resumo.CRITICAL} |`,
        `| Erro | ${resumo.ERROR} |`,
        `| Aviso | ${resumo.WARNING} |`,
        '', '## Anomalias detalhadas', ''
    );

    relatorio.anomalias.forEach((anom, indice) => {
        linhas.push(`### ${indice + 1}. [${rotuloSeveridade(anom.severidade)}] ${anom.regra}`, '');
        linhas.push(`**Descrição:** ${anom.descricao}`, '');
        if (anom.eventoOriginal) {
            linhas.push(`**Original (#${anom.eventoOriginal.indice}):**`, '```', formatarTextoAss(anom.eventoOriginal.texto), '```', '');
        }
        if (anom.eventoTraduzido) {
            linhas.push(`**Traduzido (#${anom.eventoTraduzido.indice}):**`, '```', formatarTextoAss(anom.eventoTraduzido.texto), '```', '');
        }
        if (anom.sugestaoCorrecao) {
            linhas.push(`**Recomendação:** ${anom.sugestaoCorrecao}`, '');
        }
        linhas.push('---', '');
    });

    return linhas.join('\n');
}

function contarPorSeveridade(anomalias) {
    const contagem = { CRITICAL: 0, ERROR: 0, WARNING: 0 };
    (anomalias || []).forEach(anom => {
        if (contagem[anom.severidade] !== undefined) {
            contagem[anom.severidade]++;
        }
    });
    return contagem;
}

function extrairNomeArquivo(caminho) {
    const normalizado = String(caminho).replace(/\\/g, '/');
    const partes = normalizado.split('/');
    const nome = partes[partes.length - 1] || 'auditoria';
    return nome.replace(/\.ass$/i, '').replace(/[^\w.-]+/g, '_');
}

function baixarArquivo(conteudo, nomeArquivo, mimeType) {
    const blob = new Blob([conteudo], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = nomeArquivo;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
}
