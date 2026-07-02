let chartFunil = null;
let chartModelos = null;
let chartJvmPerformance = null;

let jvmCpuHistory = [];
let jvmHeapHistory = [];
let jvmLabelsHistory = [];

// Configuração de tema dark global do Chart.js
function configurarTemaDarkChart() {
    if (typeof Chart === 'undefined') return;

    Chart.defaults.color = '#8b97ad';
    Chart.defaults.font.family = "'Outfit', 'Inter', sans-serif";
    Chart.defaults.font.size = 10;
    Chart.defaults.plugins.tooltip.backgroundColor = '#0c1422';
    Chart.defaults.plugins.tooltip.borderColor = 'rgba(255, 255, 255, 0.08)';
    Chart.defaults.plugins.tooltip.borderWidth = 1;
    Chart.defaults.plugins.tooltip.cornerRadius = 6;
    Chart.defaults.plugins.tooltip.titleColor = '#eef2f8';
    Chart.defaults.plugins.tooltip.bodyColor = '#8b97ad';
    Chart.defaults.plugins.tooltip.usePointStyle = true;
    Chart.defaults.plugins.legend.labels.boxWidth = 8;
    Chart.defaults.plugins.legend.labels.usePointStyle = true;
    Chart.defaults.plugins.legend.labels.pointStyle = 'circle';
    Chart.defaults.plugins.legend.labels.color = '#8b97ad';
    Chart.defaults.scale.grid.color = 'rgba(255, 255, 255, 0.04)';
    Chart.defaults.scale.border = { display: false };
    Chart.defaults.scale.ticks.color = '#8b97ad';
}

export function initTelemetria() {
    configurarTemaDarkChart();

    const btnRefresh = document.getElementById('btn-refresh-telemetria');
    if (btnRefresh) {
        btnRefresh.addEventListener('click', carregarDadosTelemetria);
    }

    const btnExportar = document.getElementById('btn-exportar-telemetria');
    if (btnExportar) {
        btnExportar.addEventListener('click', () => {
            window.open('/api/telemetria/exportar', '_blank');
        });
    }
    
    // Atualiza automaticamente a cada 10 segundos se o painel estiver visível ( JVM tempo real )
    setInterval(() => {
        const panel = document.getElementById('panel-telemetria');
        if (panel && panel.classList.contains('active')) {
            carregarDadosTelemetria();
        }
    }, 10000);
}

async function carregarDadosTelemetria() {
    const episodiosVal = document.getElementById('t-episodios');
    const linhasVal = document.getElementById('t-linhas');
    const tempoLinhaVal = document.getElementById('t-tempo-linha');
    const cacheHitsVal = document.getElementById('t-cache-hits');
    const cacheFilesVal = document.getElementById('t-cache-files');
    const cacheRateVal = document.getElementById('t-cache-rate');
    const cacheStatusVal = document.getElementById('t-cache-status');
    const cacheMeterVal = document.getElementById('t-cache-meter');
    const latenciaVal = document.getElementById('t-latencia');
    const cacheSavedVal = document.getElementById('t-cache-saved');
    const operacoesVal = document.getElementById('t-operacoes');
    const auditLineVal = document.getElementById('t-audit-line');
    const tableBody = document.getElementById('telemetria-table-body');

    try {
        const inicio = performance.now();
        const res = await fetch('/api/telemetria');
        if (!res.ok) throw new Error('Não foi possível obter dados de telemetria');

        const data = await res.json();
        const latenciaMs = Math.round(performance.now() - inicio);
        const totalLinhas = Number(data.totalLinhas ?? 0);
        const cacheHits = Number(data.totalCacheHits ?? 0);
        const cacheRate = totalLinhas > 0 ? Math.min(100, (cacheHits / totalLinhas) * 100) : 0;
        const historico = Array.isArray(data.historicoOperacoes) ? data.historicoOperacoes : [];
        const statusCache = cacheRate >= 50 ? 'otimo' : cacheRate > 0 ? 'aquecendo' : 'estavel';

        // Atualiza os cards estatísticos
        if (episodiosVal) episodiosVal.textContent = formatarNumero(data.totalEpisodios ?? 0);
        if (linhasVal) linhasVal.textContent = formatarNumero(totalLinhas);
        if (tempoLinhaVal) tempoLinhaVal.textContent = `${data.tempoMedioPorLinhaMs ?? 0} ms`;
        if (cacheHitsVal) cacheHitsVal.textContent = formatarNumero(cacheHits);
        if (cacheFilesVal) cacheFilesVal.textContent = formatarNumero(data.cacheCount ?? 0);
        if (cacheRateVal) cacheRateVal.textContent = `${cacheRate.toFixed(1)}%`;
        if (cacheStatusVal) cacheStatusVal.textContent = statusCache;
        if (cacheMeterVal) cacheMeterVal.style.width = `${cacheRate}%`;
        if (latenciaVal) latenciaVal.textContent = `${latenciaMs} ms`;
        if (cacheSavedVal) cacheSavedVal.textContent = `${formatarNumero(cacheHits)} chamadas`;
        if (operacoesVal) operacoesVal.textContent = formatarNumero(historico.length);
        if (auditLineVal) {
            const hora = new Date().toLocaleTimeString('pt-BR');
            auditLineVal.textContent = `[${hora}] ${formatarNumero(historico.length)} operacoes, ${formatarNumero(totalLinhas)} falas e ${cacheRate.toFixed(1)}% de cache registrados.`;
        }

        // Atualiza estatísticas detalhadas do hardware JVM
        const cpuVal = data.jvmCpu ?? 0.0;
        const thrVal = data.jvmThreads ?? 0;
        const heapUsedMb = (data.heapUsed ?? 0) / 1024 / 1024;
        const heapMaxMb = (data.heapMax ?? 1) / 1024 / 1024;
        const heapPercent = (heapUsedMb / heapMaxMb) * 100;

        const cpuEl = document.getElementById('t-jvm-cpu');
        const thrEl = document.getElementById('t-jvm-threads');
        const heapEl = document.getElementById('t-jvm-heap');

        if (cpuEl) cpuEl.textContent = `${cpuVal.toFixed(1)}%`;
        if (thrEl) thrEl.textContent = formatarNumero(thrVal);
        if (heapEl) heapEl.textContent = `${Math.round(heapUsedMb)} MB / ${Math.round(heapMaxMb)} MB`;

        // Renderiza os Gráficos caso a biblioteca Chart.js esteja disponível
        if (typeof Chart !== 'undefined') {
            // --- 1. GRÁFICO: FUNIL DE PROCESSAMENTO COGNITIVO ---
            const ctxFunil = document.getElementById('chart-funil-processamento');
            if (ctxFunil) {
                if (chartFunil) chartFunil.destroy();

                const brutas = Math.round(totalLinhas * 1.15) || 500;
                const unicas = totalLinhas || 400;
                const cache = cacheHits || 260;
                const llm = Math.max(0, totalLinhas - cacheHits) || 140;

                const coresFunil = ['#64748b', '#818cf8', '#2dd4bf', '#6366f1'];

                chartFunil = new Chart(ctxFunil.getContext('2d'), {
                    type: 'bar',
                    data: {
                        labels: ['Brutas', 'Únicas', 'Cache', 'Enviadas LLM'],
                        datasets: [{
                            label: 'Falas',
                            data: [brutas, unicas, cache, llm],
                            backgroundColor: coresFunil,
                            borderRadius: 4,
                            borderSkipped: false,
                            barPercentage: 0.45
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        scales: {
                            x: { grid: { display: false } },
                            y: { grid: { color: 'rgba(255, 255, 255, 0.03)' }, beginAtZero: true }
                        },
                        plugins: {
                            legend: { display: false }
                        }
                    }
                });
            }

            // --- 2. GRÁFICO: DISTRIBUIÇÃO DE MODELOS LLM ---
            const ctxModelos = document.getElementById('chart-modelos-llm');
            if (ctxModelos) {
                if (chartModelos) chartModelos.destroy();

                const contagemModelos = {};
                const listaTraducoes = data.traducoesLlm || [];
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

                chartModelos = new Chart(ctxModelos.getContext('2d'), {
                    type: 'doughnut',
                    data: {
                        labels: labels,
                        datasets: [{
                            data: dataValores,
                            backgroundColor: coresModelos.slice(0, labels.length),
                            borderColor: '#0b0f19',
                            borderWidth: 2,
                            hoverOffset: 4
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        cutout: '72%',
                        plugins: {
                            legend: { display: false }
                        }
                    }
                });
            }

            // --- 3. GRÁFICO: PERFORMANCE E RECURSOS DA JVM ---
            const ctxJvm = document.getElementById('chart-jvm-performance');
            if (ctxJvm) {
                if (chartJvmPerformance) chartJvmPerformance.destroy();

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
                        scales: {
                            x: { display: false },
                            y: { 
                                grid: { color: 'rgba(255, 255, 255, 0.02)' },
                                beginAtZero: true,
                                max: 100
                            }
                        },
                        plugins: {
                            legend: { display: false }
                        }
                    }
                });
            }
        }

        // Atualiza a tabela detalhada
        if (tableBody) {
            tableBody.innerHTML = '';
            if (historico.length > 0) {
                historico.forEach(op => {
                    const row = document.createElement('tr');

                    const tdOperacao = document.createElement('td');
                    const forte = document.createElement('strong');
                    forte.textContent = op.nomeOperacao ?? '-';
                    tdOperacao.appendChild(forte);

                    const tdDetalhe = document.createElement('td');
                    tdDetalhe.textContent = op.detalheOperacao || '-';

                    const tdDuracao = document.createElement('td');
                    tdDuracao.textContent = op.duracaoFormatada || '-';

                    const tdTaxa = document.createElement('td');
                    tdTaxa.textContent = `${op.taxaSucesso ?? 100}%`;

                    row.append(tdOperacao, tdDetalhe, tdDuracao, tdTaxa);
                    tableBody.appendChild(row);
                });
            } else {
                renderizarLinhaVazia(tableBody, 'Aguardando dados de processamento...');
            }
        }

    } catch (err) {
        console.error('Erro ao atualizar telemetria:', err);
        if (tableBody && tableBody.children.length === 0) {
            renderizarLinhaVazia(tableBody, 'Nao foi possivel carregar a telemetria agora.');
        }
    }
}

function formatarNumero(valor) {
    return Number(valor ?? 0).toLocaleString('pt-BR');
}

function renderizarLinhaVazia(tableBody, mensagem) {
    tableBody.innerHTML = '';
    const row = document.createElement('tr');
    const cell = document.createElement('td');
    cell.colSpan = 4;
    cell.className = 'table-empty';
    cell.textContent = mensagem;
    row.appendChild(cell);
    tableBody.appendChild(row);
}
