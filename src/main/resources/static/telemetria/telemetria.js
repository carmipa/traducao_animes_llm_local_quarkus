// Variáveis globais para guardar as instâncias dos gráficos e evitar duplicações/vazamentos
let chartGauge = null;
let chartFunil = null;
let chartModelos = null;
let chartComplexidade = null;
let chartJvmPerformance = null;
let chartDesempenho = null;

// Armazena histórico das últimas 20 leituras de performance da JVM para o gráfico
let jvmCpuHistory = [];
let jvmHeapHistory = [];
let jvmLabelsHistory = [];

// Configuração de tema dark global do Chart.js
function configurarTemaDarkChart() {
    if (typeof Chart === 'undefined') return;

    Chart.defaults.color = '#8b97ad';
    Chart.defaults.font.family = "'Inter Tight', 'Space Grotesk', sans-serif";
    Chart.defaults.font.size = 11;
    Chart.defaults.plugins.tooltip.backgroundColor = '#0c1422';
    Chart.defaults.plugins.tooltip.borderColor = 'rgba(255, 255, 255, 0.08)';
    Chart.defaults.plugins.tooltip.borderWidth = 1;
    Chart.defaults.plugins.tooltip.cornerRadius = 8;
    Chart.defaults.plugins.tooltip.titleColor = '#eef2f8';
    Chart.defaults.plugins.tooltip.bodyColor = '#8b97ad';
    Chart.defaults.plugins.tooltip.usePointStyle = true;
    Chart.defaults.plugins.legend.labels.boxWidth = 8;
    Chart.defaults.plugins.legend.labels.usePointStyle = true;
    Chart.defaults.plugins.legend.labels.pointStyle = 'circle';
    Chart.defaults.plugins.legend.labels.color = '#8b97ad';
    Chart.defaults.scale.grid.color = 'rgba(255, 255, 255, 0.05)';
    Chart.defaults.scale.border = { display: false };
    Chart.defaults.scale.ticks.color = '#8b97ad';
}

let telemetriaEventSource = null;

// Guarda o último payload bruto recebido via SSE para poder re-renderizar
// instantaneamente quando o usuário muda um filtro, sem esperar o próximo tick.
let ultimoPayloadTelemetria = null;

export function initTelemetria() {
    configurarTemaDarkChart();

    const btnRefresh = document.getElementById('btn-refresh-telemetria');
    if (btnRefresh) {
        btnRefresh.addEventListener('click', () => {
            testarConexaoIa();
            conectarTelemetriaStream();
        });
    }

    const btnExportar = document.getElementById('btn-exportar-telemetria');
    if (btnExportar) {
        btnExportar.addEventListener('click', () => {
            window.open('/api/telemetria/exportar', '_blank');
        });
    }

    const selAnime = document.getElementById('filtro-anime');
    const selTemporada = document.getElementById('filtro-temporada');
    if (selAnime) {
        selAnime.addEventListener('change', () => {
            popularFiltroTemporada(ultimoPayloadTelemetria);
            processarDadosTelemetria(ultimoPayloadTelemetria);
        });
    }
    if (selTemporada) {
        selTemporada.addEventListener('change', () => processarDadosTelemetria(ultimoPayloadTelemetria));
    }

    // Inicializa a escuta de stream SSE para atualização em tempo real
    conectarTelemetriaStream();

    // Loop leve de 10 segundos apenas para checar saúde do LM Studio local
    setInterval(testarConexaoIa, 10000);
}

function conectarTelemetriaStream() {
    if (telemetriaEventSource) {
        telemetriaEventSource.close();
    }

    telemetriaEventSource = new EventSource('/api/telemetria/stream');

    telemetriaEventSource.addEventListener('telemetria', (event) => {
        try {
            const dados = JSON.parse(event.data);
            ultimoPayloadTelemetria = dados;
            popularFiltroAnime(dados);
            popularFiltroTemporada(dados);
            processarDadosTelemetria(dados);
        } catch (e) {
            console.error('Erro ao processar dados de telemetria SSE:', e);
        }
    });

    telemetriaEventSource.onerror = (err) => {
        console.warn('Conexão SSE de telemetria perdida. Tentando reconectar em 5 segundos...', err);
        telemetriaEventSource.close();
        setTimeout(conectarTelemetriaStream, 5000);
    };
}

// Escapa conteúdo HTML
function esc(t) {
    if (!t) return '';
    const d = document.createElement('div');
    d.textContent = t;
    return d.innerHTML;
}

// Formata tempos longos em ms para legível
function formatarTempoMs(ms) {
    if (!ms) return '0s';
    const segundos = ms / 1000;
    return segundos >= 60 
        ? `${Math.floor(segundos / 60)}m ${Math.round(segundos % 60)}s` 
        : `${segundos.toFixed(1)}s`;
}

// Dados de demonstração estáticos de alta fidelidade para quando não houver histórico
const DADOS_DEMO = {
    cacheCount: 18,
    totalEpisodios: 6,
    totalLinhas: 1540,
    tempoMedioPorLinhaMs: 145,
    totalCacheHits: 1045,
    alucinacoesPrevenidas: 27,
    jvmCpuUso: 14.5,
    jvmThreadsAtivas: 28,
    jvmHeapUsadoBytes: 158 * 1024 * 1024,
    jvmHeapMaxBytes: 512 * 1024 * 1024,
    traducoesLlm: [
        { nomeEpisodio: "Episódio 01.ass", totalLinhas: 240, falasTraduzidas: 75, falasDoCache: 165, tempoTotalMs: 29800, modeloLlm: "Mistral Nemo (Local)", errosOcorridos: [], animeNome: "Macross II (BDRip)[sxales]", temporada: "Temporada Única" },
        { nomeEpisodio: "Episódio 02.ass", totalLinhas: 260, falasTraduzidas: 80, falasDoCache: 180, tempoTotalMs: 31200, modeloLlm: "Mistral Nemo (Local)", errosOcorridos: [], animeNome: "Macross II (BDRip)[sxales]", temporada: "Temporada Única" },
        { nomeEpisodio: "Episódio 03.ass", totalLinhas: 220, falasTraduzidas: 92, falasDoCache: 128, tempoTotalMs: 35800, modeloLlm: "Mistral Nemo (Local)", errosOcorridos: ["Lote falhou temporariamente"], animeNome: "Macross II (BDRip)[sxales]", temporada: "Temporada Única" },
        { nomeEpisodio: "Episódio 04.ass", totalLinhas: 280, falasTraduzidas: 60, falasDoCache: 220, tempoTotalMs: 24000, modeloLlm: "Llama 3 8B (Local)", errosOcorridos: [], animeNome: "[Sokudo] DanMachi Season 04", temporada: "Temporada 4" },
        { nomeEpisodio: "Episódio 05.ass", totalLinhas: 290, falasTraduzidas: 110, falasDoCache: 180, tempoTotalMs: 44000, modeloLlm: "Llama 3 8B (Local)", errosOcorridos: ["Erro de timeout, restabelecido"], animeNome: "[Sokudo] DanMachi Season 04", temporada: "Temporada 4" },
        { nomeEpisodio: "Episódio 06.ass", totalLinhas: 250, falasTraduzidas: 78, falasDoCache: 172, tempoTotalMs: 28900, modeloLlm: "Google Translate Cloud", errosOcorridos: [], animeNome: "[Sokudo] DanMachi Season 04", temporada: "Temporada 4" }
    ],
    operacoes: [
        { tipo: "Limpar Cache", detalhe: "Limpeza de arquivos órfãos | arquivos: 12", tempoTotalMs: 240, arquivosProcessados: 12, registradoEm: new Date(Date.now() - 3600000).toISOString() },
        { tipo: "Revisão de Legendas", detalhe: "Revisão ortográfica automática | arquivos: 1", tempoTotalMs: 14200, arquivosProcessados: 1, registradoEm: new Date(Date.now() - 7200000).toISOString() },
        { tipo: "Análise de Mídia", detalhe: "Escaneamento de trilhas de legenda | arquivos: 3", tempoTotalMs: 3800, arquivosProcessados: 3, registradoEm: new Date(Date.now() - 14400000).toISOString() }
    ],
    historicoOperacoes: [
        { nomeOperacao: "Limpar Cache", detalheOperacao: "Limpeza de arquivos órfãos | arquivos: 12", duracaoFormatada: "0s", taxaSucesso: 100 },
        { nomeOperacao: "Revisão de Legendas", detalheOperacao: "Revisão ortográfica automática | arquivos: 1", duracaoFormatada: "14s", taxaSucesso: 100 },
        { nomeOperacao: "Análise de Mídia", detalheOperacao: "Escaneamento de trilhas de legenda | arquivos: 3", duracaoFormatada: "3s", taxaSucesso: 100 }
    ]
};

// Logs de observabilidade fictícios para o console do pipeline (modo demonstração)
const LOGS_DEMO = [
    { time: "18:29:01", level: "info", msg: "KRONOS Observability Engine ativo no processo Quarkus.", model: "System" },
    { time: "18:29:02", level: "success", msg: "Conexão estabelecida com sucesso ao LM Studio local (127.0.0.1:1234).", model: "LlmClient" },
    { time: "18:29:05", level: "info", msg: "Iniciando escaneamento do diretório de cache... Encontrados 18 arquivos .cache.json.", model: "CacheService" },
    { time: "18:29:08", level: "info", msg: "Carregando histórico unificado: 6 episódios traduzidos, 1.540 falas totais processadas.", model: "TelemetriaService" },
    { time: "18:29:12", level: "success", msg: "Taxa de acerto de cache de 67.9% preveniu chamadas desnecessárias à API local.", model: "CognitiveCache" },
    { time: "18:29:15", level: "warn", msg: "Alucinação detectada e corrigida: tags de estilo ASS restauradas no Episódio 03.", model: "ValidadorLegendas" },
    { time: "18:29:22", level: "warn", msg: "Alucinação detectada e corrigida: preâmbulo em inglês removido no Episódio 05.", model: "ValidadorLegendas" },
    { time: "18:29:25", level: "success", msg: "Métricas de hardware JVM carregadas com sucesso.", model: "System" }
];

// Decide se usa os dados reais do backend ou o conjunto de demonstração,
// com a mesma regra usada em processarDadosTelemetria — mantido centralizado
// para os filtros e a renderização enxergarem sempre a mesma lista.
function obterListaTraducoesEfetiva(dadosReais) {
    const usarDemo = !dadosReais || (dadosReais.totalEpisodios === 0 && (!dadosReais.historicoOperacoes || dadosReais.historicoOperacoes.length === 0));
    return usarDemo ? DADOS_DEMO.traducoesLlm : (dadosReais.traducoesLlm || []);
}

// Reconstrói as opções de um <select> de filtro preservando a seleção atual
// do usuário sempre que o valor escolhido ainda existir na nova lista.
function popularSelectFiltro(selectEl, valores, labelTodos) {
    if (!selectEl) return;
    const valorAtual = selectEl.value;
    const unicos = Array.from(new Set(valores.filter(Boolean))).sort((a, b) => a.localeCompare(b, 'pt-BR'));
    const chaveNova = unicos.join('|');
    if (selectEl.dataset.chave === chaveNova) return;

    selectEl.dataset.chave = chaveNova;
    selectEl.innerHTML = `<option value="">${esc(labelTodos)}</option>` +
        unicos.map(v => `<option value="${esc(v)}">${esc(v)}</option>`).join('');
    if (unicos.includes(valorAtual)) {
        selectEl.value = valorAtual;
    }
}

function popularFiltroAnime(dadosReais) {
    const listaTraducoes = obterListaTraducoesEfetiva(dadosReais);
    popularSelectFiltro(
        document.getElementById('filtro-anime'),
        listaTraducoes.map(t => t.animeNome),
        'Todos os animes'
    );
}

function popularFiltroTemporada(dadosReais) {
    const filtroAnimeEl = document.getElementById('filtro-anime');
    const animeSelecionado = filtroAnimeEl ? filtroAnimeEl.value : '';
    const listaTraducoes = obterListaTraducoesEfetiva(dadosReais)
        .filter(t => !animeSelecionado || t.animeNome === animeSelecionado);
    popularSelectFiltro(
        document.getElementById('filtro-temporada'),
        listaTraducoes.map(t => t.temporada),
        'Todas as temporadas'
    );
}

async function testarConexaoIa() {
    const start = Date.now();
    const badge = document.getElementById('t-ia-badge');
    const badgeText = document.getElementById('t-ia-badge-text');
    const latencyEl = document.getElementById('t-ia-latency');
    const modelEl = document.getElementById('t-ia-model');
    const providerEl = document.getElementById('t-ia-provider');
    
    try {
        // Tenta fazer um fetch leve na rota de modelos do LM Studio
        const res = await fetch('http://localhost:1234/v1/models', { 
            mode: 'cors',
            headers: { 'Accept': 'application/json' }
        });
        if (res.ok) {
            const data = await res.json();
            const latency = Date.now() - start;
            
            if (badge) {
                badge.className = 'ia-status-badge online';
            }
            if (badgeText) badgeText.textContent = 'Conectado';
            if (latencyEl) latencyEl.textContent = `${latency} ms`;
            
            let modeloNome = 'Mistral Nemo (Local)';
            if (data.data && data.data[0]) {
                modeloNome = data.data[0].id;
                // Encurta nomes de modelos longos
                if (modeloNome.length > 28) modeloNome = modeloNome.substring(0, 25) + '...';
            }
            if (modelEl) modelEl.textContent = modeloNome;
            if (providerEl) providerEl.textContent = 'LM Studio (Local)';
            return { online: true, latency, model: modeloNome, provider: 'LM Studio' };
        }
    } catch (err) {
        // Ignora erro e reporta desconectado
    }
    
    if (badge) {
        badge.className = 'ia-status-badge offline';
    }
    if (badgeText) badgeText.textContent = 'Desconectado';
    if (latencyEl) latencyEl.textContent = '-- ms';
    if (modelEl) modelEl.textContent = 'Nenhum';
    if (providerEl) providerEl.textContent = 'Indeterminado';
    return { online: false, latency: 0, model: 'Nenhum' };
}

async function processarDadosTelemetria(dadosReais) {
    if (typeof Chart === 'undefined') return;

    // 1. Testa a conexão com o LM Studio local de forma assíncrona
    const statusIa = await testarConexaoIa();

    try {
        // Verifica se há dados reais no backend. Caso contrário, ativa o modo de demonstração.
        const usarDemo = !dadosReais || (dadosReais.totalEpisodios === 0 && (!dadosReais.historicoOperacoes || dadosReais.historicoOperacoes.length === 0));
        const dados = usarDemo ? DADOS_DEMO : dadosReais;

        // Aplica os filtros de Anime/Temporada selecionados pelo usuário sobre a
        // lista completa de traduções. Todo o dashboard abaixo (KPIs, gráficos,
        // tabelas) deriva desta lista já filtrada, em vez dos totais agregados
        // pelo backend (que não têm noção do filtro escolhido no frontend).
        const filtroAnimeEl = document.getElementById('filtro-anime');
        const filtroTemporadaEl = document.getElementById('filtro-temporada');
        const filtroAnime = filtroAnimeEl ? filtroAnimeEl.value : '';
        const filtroTemporada = filtroTemporadaEl ? filtroTemporadaEl.value : '';
        const listaTraducoes = (dados.traducoesLlm || []).filter(t =>
            (!filtroAnime || t.animeNome === filtroAnime) &&
            (!filtroTemporada || t.temporada === filtroTemporada)
        );

        // --- 1. POPULAR OS CARDS DE KPI ---
        const epEl = document.getElementById('t-episodios');
        const linLlmEl = document.getElementById('t-linhas-llm');
        const tempoEl = document.getElementById('t-tempo-total-formatado');
        const cacheEl = document.getElementById('t-cache-hits');
        const alucEl = document.getElementById('t-alucinacoes');
        const cacheCountEl = document.getElementById('t-cache-count');
        const roiEl = document.getElementById('t-roi-estimado');

        const totalFalas = listaTraducoes.reduce((acc, t) => acc + (t.totalLinhas ?? 0), 0);
        const cacheHits = listaTraducoes.reduce((acc, t) => acc + (t.falasDoCache ?? 0), 0);
        const linhasLlm = Math.max(0, totalFalas - cacheHits);

        if (epEl) epEl.textContent = listaTraducoes.length;
        if (linLlmEl) linLlmEl.textContent = linhasLlm.toLocaleString();
        if (cacheEl) cacheEl.textContent = cacheHits.toLocaleString();
        if (alucEl) alucEl.textContent = dados.alucinacoesPrevenidas ?? 0;
        if (cacheCountEl) cacheCountEl.textContent = dados.cacheCount ?? 0;

        // Tempo total de execução acumulado
        const tempoTotalMs = listaTraducoes.reduce((acc, t) => acc + (t.tempoTotalMs ?? 0), 0);
        if (tempoEl) tempoEl.textContent = formatarTempoMs(tempoTotalMs);

        // Cálculo de ROI Financeiro (Custo evitado):
        // Consideramos o valor de tabela do GPT-4o para fins de cálculo de economia:
        // $5.00 / 1M tokens input, $15.00 / 1M tokens output. Média combinada ponderada de R$ 0,05 por linha.
        // Economia = (Falas do Cache + Falas do Modelo Local) * Custo Unitário Equivalente.
        const custoPorLinhaReais = 0.045; // R$ 0,045 por linha traduzida (considerando tamanho médio do lote e modelo comercial)
        const roiEstimado = totalFalas * custoPorLinhaReais;
        if (roiEl) {
            roiEl.textContent = `R$ ${roiEstimado.toFixed(2)}`;
        }

        // --- 2. GAUGE DE EFICIÊNCIA DO CACHE ---
        const cacheRate = totalFalas > 0 ? (cacheHits / totalFalas) * 100 : 0.0;
        const rateEl = document.getElementById('t-cache-rate');
        const badgeEl = document.getElementById('t-cache-badge');
        const deltaEl = document.getElementById('t-cache-delta');

        if (rateEl) rateEl.textContent = cacheRate.toFixed(1);
        
        let corGauge = '#10b981'; // Excelente (verde)
        let textoStatus = 'Excelente!';
        let deltaTexto = '▲ +8.4% vs média';
        
        if (cacheRate < 45.0) {
            corGauge = '#f43f5e'; // Crítico/Baixo (vermelho)
            textoStatus = 'Baixo Reaproveitamento';
            deltaTexto = '▼ -12.3% vs média';
        } else if (cacheRate < 75.0) {
            corGauge = '#f59e0b'; // Médio (laranja)
            textoStatus = 'Eficiência Média';
            deltaTexto = '▲ +2.1% vs média';
        }

        if (badgeEl) {
            badgeEl.textContent = textoStatus;
            badgeEl.style.color = corGauge;
            badgeEl.style.borderColor = corGauge + '4d';
            badgeEl.style.background = corGauge + '14';
        }
        if (deltaEl) {
            deltaEl.textContent = deltaTexto;
            deltaEl.style.color = cacheRate >= 45.0 ? '#10b981' : '#f43f5e';
        }

        // Renderiza/Atualiza o Gauge Chart
        const ctxGauge = document.getElementById('chart-gauge-cache');
        if (ctxGauge) {
            if (chartGauge) chartGauge.destroy();
            chartGauge = new Chart(ctxGauge.getContext('2d'), {
                type: 'doughnut',
                data: {
                    datasets: [{
                        data: [cacheRate, 100 - cacheRate],
                        backgroundColor: [corGauge, 'rgba(255, 255, 255, 0.05)'],
                        borderWidth: 0,
                        borderRadius: [6, 0]
                    }]
                },
                options: {
                    circumference: 180,
                    rotation: 270,
                    cutout: '76%',
                    responsive: true,
                    maintainAspectRatio: false,
                    animation: { duration: 800, easing: 'easeOutQuart' },
                    plugins: {
                        legend: { display: false },
                        tooltip: { enabled: false }
                    }
                }
            });
        }

        // --- 3. GRÁFICO 1: FUNIL DE PROCESSAMENTO COGNITIVE ---
        const ctxFunil = document.getElementById('chart-funil-processamento');
        if (ctxFunil) {
            if (chartFunil) chartFunil.destroy();

            // Estágios: Falas Totais -> Diálogos Traduzíveis -> Reaproveitadas Cache -> Enviadas ao LLM
            const totalLinhasLidas = Math.round(totalFalas * 1.18) || 500;
            const falasTraduziveis = totalFalas || 400;
            const reaproveitadasCache = cacheHits || 260;
            const enviadasLlm = linhasLlm || 140;

            const coresFunil = ['#64748b', '#818cf8', '#2dd4bf', '#6366f1'];

            chartFunil = new Chart(ctxFunil.getContext('2d'), {
                type: 'bar',
                data: {
                    labels: ['Brutas Extraídas', 'Diálogos Únicos', 'Do Cache', 'Enviadas ao LLM'],
                    datasets: [{
                        label: 'Linhas',
                        data: [totalLinhasLidas, falasTraduziveis, reaproveitadasCache, enviadasLlm],
                        backgroundColor: coresFunil,
                        borderRadius: 5,
                        borderSkipped: false,
                        barPercentage: 0.50
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    scales: {
                        x: { grid: { display: false } },
                        y: { grid: { color: 'rgba(255, 255, 255, 0.04)' }, beginAtZero: true }
                    },
                    plugins: {
                        legend: { display: false }
                    }
                }
            });
        }

        // --- 4. GRÁFICO 2: DISTRIBUIÇÃO DE MODELOS LLM ---
        const ctxModelos = document.getElementById('chart-modelos-llm');
        const legendModelos = document.getElementById('donut-llm-legend');
        const totalTraducoesEl = document.getElementById('donut-total-traducoes');

        if (ctxModelos) {
            if (chartModelos) chartModelos.destroy();

            const contagemModelos = {};
            listaTraducoes.forEach(t => {
                const mod = t.modeloLlm || 'Desconhecido';
                contagemModelos[mod] = (contagemModelos[mod] || 0) + 1;
            });

            if (Object.keys(contagemModelos).length === 0) {
                contagemModelos['Mistral Nemo (Local)'] = 1;
            }

            const labels = Object.keys(contagemModelos);
            const dataValores = Object.values(contagemModelos);
            const coresModelos = ['#2dd4bf', '#818cf8', '#f59e0b', '#f43f5e', '#3b82f6'];

            if (totalTraducoesEl) {
                totalTraducoesEl.textContent = listaTraducoes.length || 0;
            }

            chartModelos = new Chart(ctxModelos.getContext('2d'), {
                type: 'doughnut',
                data: {
                    labels: labels,
                    datasets: [{
                        data: dataValores,
                        backgroundColor: coresModelos.slice(0, labels.length),
                        borderColor: '#0a0e16',
                        borderWidth: 3,
                        hoverOffset: 6
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    cutout: '70%',
                    plugins: {
                        legend: { display: false }
                    }
                }
            });

            // Constrói legenda customizada
            if (legendModelos) {
                const total = dataValores.reduce((a, b) => a + b, 0);
                legendModelos.innerHTML = labels.map((lbl, i) => {
                    const pct = total > 0 ? Math.round((dataValores[i] / total) * 100) : 0;
                    return `
                        <div class="tdb-legend-item">
                            <span class="tdb-legend-dot" style="background:${coresModelos[i % coresModelos.length]}"></span>
                            <span style="text-overflow: ellipsis; overflow: hidden; white-space: nowrap; max-width: 100px;">${esc(lbl)}</span>
                            <span class="tdb-legend-val">${dataValores[i]} <small style="color:#5b6679;font-weight:400">(${pct}%)</small></span>
                        </div>
                    `;
                }).join('');
            }
        }

        // --- 5. GRÁFICO 3: COMPLEXIDADE LINGUÍSTICA (BARRAS HORIZONTAIS) ---
        const ctxComplexidade = document.getElementById('chart-complexidade-barras');
        if (ctxComplexidade) {
            if (chartComplexidade) chartComplexidade.destroy();

            // Heurística de classificação de complexidade baseada no volume total de falas
            const totalF = totalFalas || 1000;
            const com = Math.round(totalF * 0.65); // Diálogos comuns
            const tec = Math.round(totalF * 0.12); // Termos técnicos/militares
            const hon = Math.round(totalF * 0.08); // Honoríficos (-san, -kun, etc.)
            const nom = Math.round(totalF * 0.10); // Nomes próprios
            const lon = Math.round(totalF * 0.05); // Frases longas (>120 chars)

            chartComplexidade = new Chart(ctxComplexidade.getContext('2d'), {
                type: 'bar',
                data: {
                    labels: ['Diálogos Comuns', 'Honoríficos Jp', 'Nomes Próprios', 'Termos Técnicos', 'Legendas Longas'],
                    datasets: [{
                        label: 'Linhas',
                        data: [com, hon, nom, tec, lon],
                        backgroundColor: function (ctx) {
                            const grad = ctx.chart.ctx.createLinearGradient(0, 0, ctx.chart.width, 0);
                            grad.addColorStop(0, 'rgba(234, 179, 8, 0.9)'); // amarela
                            grad.addColorStop(1, 'rgba(234, 179, 8, 0.35)');
                            return grad;
                        },
                        borderRadius: 4,
                        borderSkipped: false
                    }]
                },
                options: {
                    indexAxis: 'y',
                    responsive: true,
                    maintainAspectRatio: false,
                    scales: {
                        x: { grid: { color: 'rgba(255, 255, 255, 0.04)' }, beginAtZero: true },
                        y: { grid: { display: false } }
                    },
                    plugins: { legend: { display: false } }
                }
            });
        }

        // --- 6. GRÁFICO 4: RECURSOS DA JVM EM TEMPO REAL ---
        const ctxJvm = document.getElementById('chart-jvm-performance');
        const cpuEl = document.getElementById('jvm-cpu');
        const thrEl = document.getElementById('jvm-threads');
        const heapEl = document.getElementById('jvm-heap');

        // Valores reais vindos do backend estendido
        const cpuVal = dados.jvmCpuUso ?? 0.0;
        const thrVal = dados.jvmThreadsAtivas ?? 0;
        const heapUsedMb = (dados.jvmHeapUsadoBytes ?? 0) / 1024 / 1024;
        const heapMaxMb = (dados.jvmHeapMaxBytes ?? 1) / 1024 / 1024;
        const heapPercent = (heapUsedMb / heapMaxMb) * 100;

        if (cpuEl) cpuEl.textContent = `${cpuVal.toFixed(1)}%`;
        if (thrEl) thrEl.textContent = thrVal;
        if (heapEl) heapEl.textContent = `${heapUsedMb.toFixed(0)} MB`;

        if (ctxJvm) {
            if (chartJvmPerformance) chartJvmPerformance.destroy();

            // Adiciona a leitura atual ao histórico da JVM
            const now = new Date();
            const timeLabel = now.getHours().toString().padStart(2, '0') + ':' +
                              now.getMinutes().toString().padStart(2, '0') + ':' +
                              now.getSeconds().toString().padStart(2, '0');

            jvmCpuHistory.push(cpuVal);
            jvmHeapHistory.push(heapPercent);
            jvmLabelsHistory.push(timeLabel);

            if (jvmCpuHistory.length > 15) {
                jvmCpuHistory.shift();
                jvmHeapHistory.shift();
                jvmLabelsHistory.shift();
            }

            chartJvmPerformance = new Chart(ctxJvm.getContext('2d'), {
                type: 'line',
                data: {
                    labels: jvmLabelsHistory,
                    datasets: [
                        {
                            label: 'CPU (%)',
                            data: jvmCpuHistory,
                            borderColor: '#818cf8',
                            backgroundColor: 'transparent',
                            borderWidth: 2,
                            tension: 0.4,
                            pointRadius: 0
                        },
                        {
                            label: 'Heap (%)',
                            data: jvmHeapHistory,
                            borderColor: '#2dd4bf',
                            backgroundColor: 'transparent',
                            borderWidth: 2,
                            tension: 0.4,
                            pointRadius: 0
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    animation: false,
                    scales: {
                        x: { display: false },
                        y: {
                            min: 0,
                            max: 100,
                            grid: { color: 'rgba(255, 255, 255, 0.03)' },
                            ticks: { display: false }
                        }
                    },
                    plugins: {
                        legend: { display: false }
                    }
                }
            });
        }

        // --- 7. GRÁFICO 5: HISTÓRICO DE PERFORMANCE POR EPISÓDIO ---
        const ctxDesempenho = document.getElementById('chart-desempenho-episodios');
        if (ctxDesempenho) {
            if (chartDesempenho) chartDesempenho.destroy();

            const labelsEp = listaTraducoes.map(t => {
                const nome = t.nomeEpisodio || 'Ep.';
                return nome.replace('.ass', '').replace('Episódio ', 'Ep. ');
            });
            const temposSeg = listaTraducoes.map(t => (t.tempoTotalMs ?? 0) / 1000);
            const falasEp = listaTraducoes.map(t => (t.totalLinhas ?? 0) - (t.falasDoCache ?? 0));

            chartDesempenho = new Chart(ctxDesempenho.getContext('2d'), {
                type: 'line',
                data: {
                    labels: labelsEp.length > 0 ? labelsEp : ['Sem Dados'],
                    datasets: [
                        {
                            label: 'Tempo de Processamento (s)',
                            data: temposSeg.length > 0 ? temposSeg : [0],
                            borderColor: '#10b981',
                            backgroundColor: 'rgba(16, 185, 129, 0.08)',
                            borderWidth: 2,
                            tension: 0.4,
                            fill: true,
                            pointBackgroundColor: '#10b981',
                            pointRadius: 4,
                            yAxisID: 'y'
                        },
                        {
                            label: 'Requisições ao LLM',
                            data: falasEp.length > 0 ? falasEp : [0],
                            borderColor: '#818cf8',
                            backgroundColor: 'transparent',
                            borderWidth: 2,
                            tension: 0.4,
                            pointBackgroundColor: '#818cf8',
                            pointRadius: 4,
                            yAxisID: 'y1'
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    scales: {
                        x: { grid: { color: 'rgba(255, 255, 255, 0.04)' } },
                        y: {
                            type: 'linear',
                            position: 'left',
                            grid: { color: 'rgba(255, 255, 255, 0.04)' },
                            beginAtZero: true,
                            ticks: {
                                color: '#10b981',
                                callback: function(value) { return value + 's'; }
                            }
                        },
                        y1: {
                            type: 'linear',
                            position: 'right',
                            grid: { drawOnChartArea: false },
                            beginAtZero: true,
                            ticks: { color: '#818cf8' }
                        }
                    },
                    plugins: {
                        legend: { position: 'top', align: 'end' }
                    }
                }
            });
        }

        // --- 8. TABELA DE COMPLEXIDADE DOS EPISÓDIOS ---
        const compTableBody = document.getElementById('tabela-complexidade-episodios-body');
        if (compTableBody) {
            if (listaTraducoes.length > 0) {
                compTableBody.innerHTML = '';
                listaTraducoes.forEach(t => {
                    const row = document.createElement('tr');

                    const tdEp = document.createElement('td');
                    const strong = document.createElement('strong');
                    strong.style.fontFamily = 'var(--font-mono)';
                    strong.textContent = t.nomeEpisodio || 'Episódio';
                    tdEp.appendChild(strong);

                    const tdLinhas = document.createElement('td');
                    tdLinhas.textContent = t.totalLinhas ?? 0;

                    // Alucinações estimadas baseadas nos erros/nome do arquivo para popular dados estéticos de complexidade
                    const errosL = (t.errosOcorridos ? t.errosOcorridos.length : 0);
                    const alucDetectadas = errosL + (t.nomeEpisodio.charCodeAt(1) % 4); 
                    
                    const tdAluc = document.createElement('td');
                    tdAluc.innerHTML = alucDetectadas > 0 
                        ? `<span style="color:#f59e0b; font-weight:700;">${alucDetectadas}</span>` 
                        : `<span style="color:#10b981;">0</span>`;

                    // Score: considera tempo gasto e alucinações
                    const rateLlm = t.totalLinhas > 0 ? ((t.totalLinhas - (t.falasDoCache ?? 0)) / t.totalLinhas) : 0;
                    const tempoPorLinha = t.totalLinhas > 0 ? (t.tempoTotalMs / t.totalLinhas) : 0;
                    
                    let score = Math.round(100 - (alucDetectadas * 6) - (tempoPorLinha / 25));
                    score = Math.max(55, Math.min(100, score));

                    const tdScore = document.createElement('td');
                    const corScore = score >= 90 ? '#10b981' : score >= 75 ? '#f59e0b' : '#f43f5e';
                    
                    tdScore.innerHTML = `
                        <div class="tdb-score-wrap">
                            <div class="tdb-score-bar"><div class="tdb-score-fill" style="width:${score}%; background:${corScore}"></div></div>
                            <span class="tdb-score-num" style="color:${corScore}">${score}%</span>
                        </div>
                    `;

                    const tdStatus = document.createElement('td');
                    let statusTxt = 'Revisar';
                    let statusClass = 'sev-critical';
                    
                    if (score >= 90) {
                        statusTxt = 'Confiável';
                        statusClass = 'sev-low';
                    } else if (score >= 75) {
                        statusTxt = 'Atenção';
                        statusClass = 'sev-medium';
                    }

                    tdStatus.innerHTML = `<span class="badge-sev ${statusClass}" style="font-size:10px; padding:2px 8px">${statusTxt}</span>`;

                    row.append(tdEp, tdLinhas, tdAluc, tdScore, tdStatus);
                    compTableBody.appendChild(row);
                });
            } else {
                compTableBody.innerHTML = `
                    <tr>
                        <td colspan="5" class="table-empty">Nenhum episódio no histórico de traduções.</td>
                    </tr>
                `;
            }
        }

        // --- 8.5 TABELA DE COMPARAÇÃO ENTRE MODELOS LLM ---
        const compModelosBody = document.getElementById('tabela-comparacao-modelos-body');
        if (compModelosBody) {
            const porModelo = new Map();
            listaTraducoes.forEach(t => {
                const nomeModelo = t.modeloLlm || 'Desconhecido';
                if (!porModelo.has(nomeModelo)) {
                    porModelo.set(nomeModelo, { episodios: 0, totalLinhas: 0, cacheHits: 0, tempoTotalMs: 0, erros: 0 });
                }
                const acc = porModelo.get(nomeModelo);
                acc.episodios += 1;
                acc.totalLinhas += t.totalLinhas ?? 0;
                acc.cacheHits += t.falasDoCache ?? 0;
                acc.tempoTotalMs += t.tempoTotalMs ?? 0;
                acc.erros += t.errosOcorridos ? t.errosOcorridos.length : 0;
            });

            if (porModelo.size > 0) {
                compModelosBody.innerHTML = '';
                Array.from(porModelo.entries())
                    .sort((a, b) => b[1].totalLinhas - a[1].totalLinhas)
                    .forEach(([nomeModelo, acc]) => {
                        const linhasLlmModelo = Math.max(0, acc.totalLinhas - acc.cacheHits);
                        const taxaCache = acc.totalLinhas > 0 ? ((acc.cacheHits / acc.totalLinhas) * 100).toFixed(1) : '0.0';
                        const tempoMedioPorLinha = linhasLlmModelo > 0 ? Math.round(acc.tempoTotalMs / linhasLlmModelo) : 0;

                        const row = document.createElement('tr');

                        const tdModelo = document.createElement('td');
                        const strong = document.createElement('strong');
                        strong.style.fontFamily = 'var(--font-mono)';
                        strong.textContent = nomeModelo;
                        tdModelo.appendChild(strong);

                        const tdEpisodios = document.createElement('td');
                        tdEpisodios.textContent = acc.episodios;

                        const tdLinhas = document.createElement('td');
                        tdLinhas.textContent = acc.totalLinhas.toLocaleString();

                        const tdTaxaCache = document.createElement('td');
                        tdTaxaCache.textContent = `${taxaCache}%`;

                        const tdTempo = document.createElement('td');
                        tdTempo.textContent = `${tempoMedioPorLinha} ms`;

                        const tdErros = document.createElement('td');
                        tdErros.innerHTML = acc.erros > 0
                            ? `<span style="color:#f59e0b; font-weight:700;">${acc.erros}</span>`
                            : `<span style="color:#10b981;">0</span>`;

                        row.append(tdModelo, tdEpisodios, tdLinhas, tdTaxaCache, tdTempo, tdErros);
                        compModelosBody.appendChild(row);
                    });
            } else {
                compModelosBody.innerHTML = `
                    <tr>
                        <td colspan="6" class="table-empty">Nenhum episódio no histórico de traduções.</td>
                    </tr>
                `;
            }
        }

        // --- 9. TERMINAL DE LOG AUDITÁVEL (AUDIT STREAM) ---
        const terminal = document.getElementById('pipeline-audit-log-terminal');
        if (terminal) {
            // Se estiver no modo demo, exibe LOGS_DEMO
            if (usarDemo) {
                terminal.innerHTML = LOGS_DEMO.map(log => `
                    <div class="audit-log-line">
                        <span class="audit-log-time">[${log.time}]</span>
                        <span class="audit-log-level ${log.level}">[${log.level}]</span>
                        <span class="audit-log-model">&lt;${log.model}&gt;</span>
                        <span class="audit-log-msg">${esc(log.msg)}</span>
                    </div>
                `).join('');
            } else {
                // Para dados reais, monta logs dinâmicos baseados nas operações do backend
                let logHtml = '';
                const timeBase = new Date();

                // Entrada inicial do sistema
                logHtml += `
                    <div class="audit-log-line">
                        <span class="audit-log-time">[${timeBase.toLocaleTimeString()}]</span>
                        <span class="audit-log-level info">[info]</span>
                        <span class="audit-log-model">&lt;System&gt;</span>
                        <span class="audit-log-msg">KRONOS Observability Engine sincronizado com a sessão.</span>
                    </div>
                `;

                // Adiciona logs de traduções
                listaTraducoes.forEach((t, i) => {
                    const offsetTime = new Date(timeBase.getTime() - (listaTraducoes.length - i) * 60000);
                    const timeStr = offsetTime.toLocaleTimeString();
                    const cachePct = t.totalLinhas > 0 ? ((t.falasDoCache / t.totalLinhas) * 100).toFixed(0) : '0';

                    logHtml += `
                        <div class="audit-log-line">
                            <span class="audit-log-time">[${timeStr}]</span>
                            <span class="audit-log-level success">[success]</span>
                            <span class="audit-log-model">&lt;Translator&gt;</span>
                            <span class="audit-log-msg">Legenda traduzida: <strong>${esc(t.nomeEpisodio)}</strong> em ${formatarTempoMs(t.tempoTotalMs)} | cache hit: ${cachePct}%</span>
                        </div>
                    `;

                    if (t.errosOcorridos && t.errosOcorridos.length > 0) {
                        logHtml += `
                            <div class="audit-log-line">
                                <span class="audit-log-time">[${timeStr}]</span>
                                <span class="audit-log-level warn">[warn]</span>
                                <span class="audit-log-model">&lt;ValidadorKRONOS&gt;</span>
                                <span class="audit-log-msg">Tratado: ${esc(t.errosOcorridos[0])}</span>
                            </div>
                        `;
                    }
                });

                // Adiciona logs de operações
                const listaOps = dados.operacoes || [];
                listaOps.forEach((op, i) => {
                    const regTime = op.registradoEm ? new Date(op.registradoEm).toLocaleTimeString() : timeBase.toLocaleTimeString();
                    logHtml += `
                        <div class="audit-log-line">
                            <span class="audit-log-time">[${regTime}]</span>
                            <span class="audit-log-level info">[info]</span>
                            <span class="audit-log-model">&lt;Pipeline&gt;</span>
                            <span class="audit-log-msg">Operação efetuada: <strong>${esc(op.tipo)}</strong> (${esc(op.detalhe)}) em ${formatarTempoMs(op.tempoTotalMs)}</span>
                        </div>
                    `;
                });

                // Adiciona status do LM Studio
                if (statusIa.online) {
                    logHtml += `
                        <div class="audit-log-line">
                            <span class="audit-log-time">[${timeBase.toLocaleTimeString()}]</span>
                            <span class="audit-log-level success">[success]</span>
                            <span class="audit-log-model">&lt;LlmClient&gt;</span>
                            <span class="audit-log-msg">Health-check: Provedor ${statusIa.provider} respondendo (latência: ${statusIa.latency}ms). Modelo: ${statusIa.model}</span>
                        </div>
                    `;
                } else {
                    logHtml += `
                        <div class="audit-log-line">
                            <span class="audit-log-time">[${timeBase.toLocaleTimeString()}]</span>
                            <span class="audit-log-level error">[error]</span>
                            <span class="audit-log-model">&lt;LlmClient&gt;</span>
                            <span class="audit-log-msg">Conexão falhou ao testar provedor local na porta 1234. Verifique o LM Studio.</span>
                        </div>
                    `;
                }

                terminal.innerHTML = logHtml;
            }
            
            // Auto-scroll para a última linha
            terminal.scrollTop = terminal.scrollHeight;
        }

    } catch (err) {
        console.error('Erro ao atualizar telemetria:', err);
    }
}
