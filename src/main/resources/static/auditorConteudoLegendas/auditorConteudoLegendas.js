import { logNoConsole, mostrarAlerta } from '../js/app.js?v=3.4';

const PAINEL_HTML = 'auditorConteudoLegendas/auditorConteudoLegendas.html?v=3.4';

const ORDEM_SEVERIDADE = { CRITICAL: 0, ERROR: 1, WARNING: 2 };

let ultimoRelatorio = null;

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
    const tableBody = document.querySelector('#table-auditor-conteudo tbody');
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
            setTimeout(() => resetarRelatorioVisual(tableBody, statusBadge), 0);
        });
    }

    if (!formAuditor || !tableBody || !statusBadge) return;

    formAuditor.addEventListener('submit', async (e) => {
        e.preventDefault();

        const original = document.getElementById('auditor-original').value.trim();
        const traduzido = document.getElementById('auditor-traduzido').value.trim();

        if (!original || !traduzido) {
            mostrarAlerta('Forneça os caminhos dos arquivos original e traduzido.', 'aviso');
            return;
        }

        ultimoRelatorio = null;
        ocultarResumoELimpo();
        statusBadge.textContent = 'Auditando...';
        statusBadge.className = 'status-badge pulse-purple';
        tableBody.innerHTML = '<tr><td colspan="6" class="auditor-empty-row">Analisando arquivos...</td></tr>';
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
            renderizarRelatorio(relatorio, tableBody, statusBadge);

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
            ocultarResumoELimpo();
            statusBadge.textContent = 'Erro';
            statusBadge.className = 'status-badge pulse-red';
            tableBody.innerHTML = `<tr><td colspan="6" class="auditor-empty-row auditor-row-erro">Erro: ${escapeHtml(err.message)}</td></tr>`;
            logNoConsole(CONSOLE_ID, `Erro na auditoria: ${err.message}`, 'erro');
            console.error(err);
        }
    });
}

function resetarRelatorioVisual(tableBody, statusBadge) {
    ultimoRelatorio = null;
    ocultarResumoELimpo();
    if (statusBadge) {
        statusBadge.textContent = 'Aguardando...';
        statusBadge.className = 'status-badge';
    }
    if (tableBody) {
        tableBody.innerHTML = '<tr class="auditor-row-empty"><td colspan="6" class="auditor-empty-row">Nenhuma auditoria realizada ainda. Execute a auditoria para ver o relatório aqui.</td></tr>';
    }
}

function ocultarResumoELimpo() {
    const resumo = document.getElementById('auditor-resumo');
    const limpo = document.getElementById('auditor-alerta-limpo');
    if (resumo) {
        resumo.innerHTML = '';
        resumo.classList.add('hidden');
    }
    if (limpo) limpo.classList.add('hidden');
}

function renderizarResumo(relatorio) {
    const resumo = document.getElementById('auditor-resumo');
    if (!resumo) return;

    const contagem = contarPorSeveridade(relatorio.anomalias || []);
    const chips = [];
    if (contagem.CRITICAL > 0) {
        chips.push(`<span class="auditor-chip auditor-chip-critical"><span class="material-symbols-outlined">dangerous</span>${contagem.CRITICAL} crítica(s)</span>`);
    }
    if (contagem.ERROR > 0) {
        chips.push(`<span class="auditor-chip auditor-chip-error"><span class="material-symbols-outlined">error</span>${contagem.ERROR} erro(s)</span>`);
    }
    if (contagem.WARNING > 0) {
        chips.push(`<span class="auditor-chip auditor-chip-warning"><span class="material-symbols-outlined">warning</span>${contagem.WARNING} aviso(s)</span>`);
    }

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
        ${chips.length ? `<div class="auditor-resumo-chips">${chips.join('')}</div>` : ''}
    `;
    resumo.classList.remove('hidden');
}

function renderizarRelatorio(relatorio, tableBody, statusBadge) {
    renderizarResumo(relatorio);
    tableBody.innerHTML = '';

    const alertaLimpo = document.getElementById('auditor-alerta-limpo');
    if (relatorio.limpo) {
        statusBadge.textContent = 'Limpo';
        statusBadge.className = 'status-badge pulse-green';
        if (alertaLimpo) alertaLimpo.classList.remove('hidden');
        tableBody.innerHTML = '<tr class="auditor-row-empty"><td colspan="6" class="auditor-empty-row">Tabela vazia — nenhuma anomalia para listar.</td></tr>';
        return;
    }

    if (alertaLimpo) alertaLimpo.classList.add('hidden');
    statusBadge.textContent = `${relatorio.anomalias.length} anomalia(s)`;
    statusBadge.className = 'status-badge pulse-red';

    const ordenadas = [...relatorio.anomalias].sort((a, b) => {
        const pa = ORDEM_SEVERIDADE[a.severidade] ?? 99;
        const pb = ORDEM_SEVERIDADE[b.severidade] ?? 99;
        return pa - pb;
    });

    ordenadas.forEach((anom, idx) => {
        const tr = document.createElement('tr');
        tr.className = `auditor-linha auditor-linha-${(anom.severidade || 'warning').toLowerCase()}`;

        const tdNum = document.createElement('td');
        tdNum.className = 'col-num';
        tdNum.textContent = String(idx + 1);

        const tdSev = document.createElement('td');
        tdSev.className = 'col-sev';
        tdSev.appendChild(criarBadgeSeveridade(anom.severidade));

        const tdLinha = document.createElement('td');
        tdLinha.className = 'col-linha';
        const numLinha = anom.eventoTraduzido?.indice ?? anom.eventoOriginal?.indice;
        if (numLinha != null) {
            tdLinha.innerHTML = `<span class="auditor-linha-badge pulse-cyan"><span class="material-symbols-outlined status-badge-icon">tag</span>#${numLinha}</span>`;
        } else {
            tdLinha.textContent = '—';
        }

        const tdRegra = document.createElement('td');
        tdRegra.className = 'col-regra';
        tdRegra.innerHTML = `<span class="auditor-regra-chip"><span class="material-symbols-outlined">rule</span>${escapeHtml(anom.regra || '—')}</span>`;

        const tdDesc = document.createElement('td');
        tdDesc.className = 'col-desc';
        tdDesc.appendChild(criarBlocoDescricao(anom));

        const tdRec = document.createElement('td');
        tdRec.className = 'col-rec';
        if (anom.sugestaoCorrecao) {
            tdRec.innerHTML = `<span class="auditor-rec-texto"><span class="material-symbols-outlined">lightbulb</span>${escapeHtml(anom.sugestaoCorrecao)}</span>`;
        } else {
            tdRec.textContent = '—';
        }

        tr.append(tdNum, tdSev, tdLinha, tdRegra, tdDesc, tdRec);
        tableBody.appendChild(tr);
    });
}

function criarBadgeSeveridade(sev) {
    const cfg = {
        CRITICAL: { pulse: 'pulse-red', icon: 'dangerous', label: 'Crítico' },
        ERROR: { pulse: 'pulse-magenta', icon: 'error', label: 'Erro' },
        WARNING: { pulse: 'pulse-yellow', icon: 'warning', label: 'Aviso' }
    }[sev] || { pulse: 'pulse-yellow', icon: 'info', label: rotuloSeveridade(sev) };

    const span = document.createElement('span');
    span.className = `status-badge ${cfg.pulse} auditor-sev-badge`;
    span.innerHTML = `<span class="material-symbols-outlined status-badge-icon">${cfg.icon}</span>${cfg.label}`;
    return span;
}

function criarBlocoDescricao(anom) {
    const wrap = document.createElement('div');
    wrap.className = 'auditor-desc-block';

    const p = document.createElement('p');
    p.className = 'auditor-desc-texto';
    p.textContent = anom.descricao || '';
    wrap.appendChild(p);

    if (anom.eventoOriginal?.texto) {
        wrap.appendChild(criarTrechoEvento('Original', anom.eventoOriginal, 'auditor-evento-orig'));
    }
    if (anom.eventoTraduzido?.texto) {
        wrap.appendChild(criarTrechoEvento('Traduzido', anom.eventoTraduzido, 'auditor-evento-trad'));
    }

    return wrap;
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
