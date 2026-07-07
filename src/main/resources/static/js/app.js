/**
 * ==========================================================================
 * KRONOS CORE - ORQUESTRADOR GLOBAL FRONTEND (SPA & SSE STREAM LOGS)
 * ==========================================================================
 */

import { initAnalise } from '../analise/analise.js?v=3.0';
import { initExtracao } from '../extracao/extracao.js?v=3.0';
import { initAuditorConteudo } from '../auditorConteudoLegendas/auditorConteudoLegendas.js?v=3.4';
import { initTraducao } from '../traducao/traducao.js?v=3.0';
import { initCorrecao } from '../correcao/correcao.js?v=3.0';
import { initRevisao } from '../revisao/revisao.js?v=3.0';
import { initCura } from '../cura/cura.js?v=3.0';
import { initRevisaoLore } from '../revisaoLore/revisaoLore.js?v=3.0';
import { initTrocaTipoLegenda } from '../trocaTipoLegenda/trocaTipoLegenda.js?v=3.0';
import { initRemuxer } from '../remuxer/remuxer.js?v=3.0';
import { initMapa } from '../mapa/mapa.js?v=3.0';
import { initTelemetria } from '../telemetria/telemetria.js?v=3.0';
import { initDocumentacao } from '../documentacao/documentacao.js?v=3.0';
import { initSobre } from '../sobre/sobre.js?v=3.0';
import { initRenomearArquivos } from '../renomearArquivos/renomearArquivos.js?v=3.0';

// Definições de Títulos e Subtítulos por seção do menu
const CONFIG_SECOES = {
    inicio: {
        titulo: "Painel Inicial",
        subtitulo: "Orquestrador automatizado e pipeline industrial de processamento de animes"
    },
    analise: {
        titulo: "1. Análise de Mídia",
        subtitulo: "Auditoria técnica de codecs, sincronia e taxas de bits de vídeos"
    },
    extracao: {
        titulo: "2. Extração de Legendas",
        subtitulo: "Extração industrial de faixas de legendas embutidas in vídeos (MKV, MP4 e outros)"
    },
    "auditor-conteudo": {
        titulo: "3. Análise de Conteúdo",
        subtitulo: "Auditoria de legendas .ass traduzidas: vazamento de efeitos, alucinações de IA e metadados"
    },
    traducao: {
        titulo: "3. Tradução Local via LLM",
        subtitulo: "Traduzir legendas originais em inglês usando inteligência artificial local"
    },
    correcao: {
        titulo: "4. Correção do Cache de Tradução",
        subtitulo: "Limpeza de inconsistências e preenchimento via raspagem do Google Tradutor"
    },
    revisao: {
        titulo: "5. Revisão de Legendas",
        subtitulo: "Concordância PT-BR via LLM local e correção de inglês residual via Google"
    },
    cura: {
        titulo: "6. Correção de Legendas",
        subtitulo: "Corrige a legenda PT-BR usando a original como referência imutável"
    },
    "revisao-lore": {
        titulo: "7. Revisão de Lore",
        subtitulo: "Padronização de nomes, locais e termos de mundo nas legendas via LLM e lore oficial"
    },
    "troca-tipo-legenda": {
        titulo: "8. Troca Tipo Legenda",
        subtitulo: "Auditoria e substituição em lote de fontes vietnamitas ou ANSI legadas por fontes Unicode seguras"
    },
    remuxer: {
        titulo: "9. Remuxer Industrial",
        subtitulo: "Junção de vídeos originais e novas legendas traduzidas em novos MKVs"
    },
    "renomear-arquivos": {
        titulo: "10. Renomear Arquivos de Vídeo",
        subtitulo: "Limpeza de nomes de arquivo usando regex e metadados S01E01"
    },
    mapa: {
        titulo: "Mapeamento do Projeto",
        subtitulo: "Auditoria de taxonomia e visualização da árvore de estrutura do código"
    },
    telemetria: {
        titulo: "Telemetria KRONOS",
        subtitulo: "Observabilidade da traducao, cache local e historico operacional"
    },
    documentacao: {
        titulo: "Documentação",
        subtitulo: "Arquitetura, módulos, API REST e diagramas do KRONOS CORE"
    },
    sobre: {
        titulo: "Sobre o Autor",
        subtitulo: "Paulo André Carminati, contatos, formação e repositórios"
    }
};

let logsEventSource = null;
let logsReconnectTimer = null;

document.addEventListener('DOMContentLoaded', async () => {
    inicializarNavegacao();
    inicializarGruposMenu();
    await inicializarModulos();
    atualizarStatusConexao();
    buscarContadoresGlobais();
    conectarFluxoLugsSSE();

    inicializarMetadadosDinamicos();
    inicializarBotoesLimpezaFormularios();
    inicializarControlesConsole();
    inicializarBotoesProcurarCaminho();
    inicializarBotaoSair();
});

/**
 * Controla a troca de abas/painéis na sidebar e atualiza os títulos
 */
function inicializarNavegacao() {
    const botoesMenu = document.querySelectorAll('.nav-item');
    const paineis = document.querySelectorAll('.panel');
    const tituloPagina = document.getElementById('page-title');
    const subtituloPagina = document.getElementById('page-subtitle');

    botoesMenu.forEach(botao => {
        // "Sair" é uma ação, não uma aba — não participa da troca de painéis
        // (ver inicializarBotaoSair()).
        if (botao.id === 'btn-menu-sair') return;
        botao.addEventListener('click', () => {
            const target = botao.getAttribute('data-target');
            
            // 1. Atualizar classe ativa dos botões do menu
            botoesMenu.forEach(b => b.classList.remove('active'));
            botao.classList.add('active');

            // 2. Exibir painel correto
            paineis.forEach(painel => {
                painel.classList.remove('active');
                if (painel.id === `panel-${target}`) {
                    painel.classList.add('active');
                }
            });

            // 3. Atualizar títulos no cabeçalho
            if (CONFIG_SECOES[target]) {
                tituloPagina.textContent = CONFIG_SECOES[target].titulo;
                subtituloPagina.textContent = CONFIG_SECOES[target].subtitulo;
            }

            // Ações extras ao abrir painéis específicos
            if (target === 'telemetria') {
                document.getElementById('btn-refresh-telemetria').click();
            }
        });
    });
}

/**
 * Acordeão dos grupos do menu lateral: alterna abrir/fechar cada grupo e
 * persiste a escolha no localStorage, restaurando na próxima visita.
 */
function inicializarGruposMenu() {
    const CHAVE_ESTADO = 'kronos.menuGruposFechados';
    let fechados;
    try {
        fechados = new Set(JSON.parse(localStorage.getItem(CHAVE_ESTADO) || '[]'));
    } catch (e) {
        fechados = new Set();
    }

    document.querySelectorAll('.nav-group').forEach(grupo => {
        const id = grupo.getAttribute('data-grupo');
        const header = grupo.querySelector('.nav-group-header');
        if (!header) return;

        const aplicar = () => {
            const fechado = fechados.has(id);
            grupo.classList.toggle('fechado', fechado);
            header.setAttribute('aria-expanded', String(!fechado));
        };
        aplicar();

        header.addEventListener('click', () => {
            if (fechados.has(id)) {
                fechados.delete(id);
            } else {
                fechados.add(id);
            }
            try {
                localStorage.setItem(CHAVE_ESTADO, JSON.stringify([...fechados]));
            } catch (e) {
                // armazenamento indisponível: estado vive só nesta sessão
            }
            aplicar();
        });
    });
}

/**
 * Inicializa cada um dos módulos JavaScript específicos das pastas
 */
async function inicializarModulos() {
    initAnalise();
    initExtracao();
    await initAuditorConteudo();
    initTraducao();
    initCorrecao();
    initRevisao();
    initCura();
    await initRevisaoLore();
    await initTrocaTipoLegenda();
    initRemuxer();
    await initRenomearArquivos();
    initMapa();
    initTelemetria();
    initDocumentacao();
    await initSobre();
}

/**
 * Conecta ao Server-Sent Events (SSE) para receber os logs do terminal em tempo real
 */
function conectarFluxoLugsSSE() {
    if (logsEventSource && logsEventSource.readyState !== EventSource.CLOSED) {
        return;
    }
    if (logsReconnectTimer) {
        clearTimeout(logsReconnectTimer);
        logsReconnectTimer = null;
    }

    console.log('Iniciando escuta de Server-Sent Events (SSE) para logs...');
    const eventSource = new EventSource('/api/logs/stream');
    logsEventSource = eventSource;

    // O backend publica cada operação em segundo plano sob um canal SSE com
    // o próprio nome (ver LogStreamService#definirCanalAtual no servidor),
    // então a rota para o console certo é direta — não depende de qual aba
    // está aberta no navegador no momento em que a linha de log chega.
    const consoleMap = {
        'analise': 'console-analise',
        'extracao': 'console-extracao',
        'auditor-conteudo': 'console-auditor-conteudo',
        'traducao': 'console-traducao',
        'correcao': 'console-correcao',
        'revisao': 'console-revisao',
        'revisao-lore': 'console-revisao-lore',
        'troca-tipo-legenda': 'console-troca-tipo-legenda',
        'correcao-legendas': 'console-cura',
        'cura': 'console-cura',
        'remuxer': 'console-remuxer',
        'renomear-arquivos': 'console-renomear-arquivos'
    };

    for (const [canal, consoleId] of Object.entries(consoleMap)) {
        eventSource.addEventListener(canal, (event) => {
            logNoConsoleFormatado(consoleId, event.data);
            verificarAlertaSSE(event.data);
        });
    }

    // Canal genérico de fallback, para qualquer log que não pertença a uma
    // operação específica das abas acima.
    eventSource.addEventListener('console', (event) => {
        const activeNav = document.querySelector('.nav-item.active');
        if (!activeNav) return;

        const target = activeNav.getAttribute('data-target');
        const consoleId = consoleMap[target];
        if (consoleId) {
            logNoConsoleFormatado(consoleId, event.data);
            verificarAlertaSSE(event.data);
        }
    });

    // Relatório final da Análise de Mídia: o backend lê de volta o .txt que
    // acabou de salvar em disco e manda o conteúdo completo em um único
    // evento. Diferente do canal 'analise' (log linha a linha ao vivo), aqui
    // substituímos o console inteiro pelo relatório salvo.
    eventSource.addEventListener('analise-relatorio', (event) => {
        exibirRelatorioSalvo('console-analise', event.data);
    });

    eventSource.addEventListener('sistema', (event) => {
        console.log('SSE Sistema:', event.data);
    });

    eventSource.onerror = (err) => {
        console.warn('Erro na conexão de stream SSE, tentando reconectar em 5s...', err);
        eventSource.close();
        if (logsEventSource === eventSource) {
            logsEventSource = null;
        }
        if (!logsReconnectTimer) {
            logsReconnectTimer = setTimeout(() => {
                logsReconnectTimer = null;
                conectarFluxoLugsSSE();
            }, 5000);
        }
    };
}

/**
 * Verifica se o servidor Spring Boot está respondendo
 */
async function atualizarStatusConexao() {
    const indicador = document.querySelector('.status-indicator');
    const statusText = document.querySelector('.status-text');
    
    try {
        const res = await fetch('/api/status', { method: 'GET' });
        if (res.ok) {
            indicador.className = 'status-indicator online';
            statusText.textContent = 'Backend Online';
        } else {
            throw new Error();
        }
    } catch (e) {
        indicador.className = 'status-indicator offline';
        statusText.textContent = 'Backend Offline';
    }

    // Repete a verificação a cada 10 segundos
    setTimeout(atualizarStatusConexao, 10000);
}

/**
 * Carrega estatísticas rápidas no cabeçalho
 */
async function buscarContadoresGlobais() {
    try {
        const res = await fetch('/api/telemetria');
        if (res.ok) {
            const dados = await res.json();
            
            // Atualiza cabeçalho global
            const cacheCount = document.getElementById('stat-cache-count');
            if (cacheCount && dados.cacheCount !== undefined) {
                cacheCount.textContent = `${dados.cacheCount} Arquivos`;
            }
            
            // Atualiza widget da home (Painel Inicial)
            const dashCacheCount = document.getElementById('dashboard-cache-count');
            if (dashCacheCount && dados.cacheCount !== undefined) {
                dashCacheCount.textContent = `${dados.cacheCount} Arquivos`;
            }
        }
    } catch (e) {
        console.warn('Não foi possível obter os contadores globais da telemetria.');
        const cacheCount = document.getElementById('stat-cache-count');
        if (cacheCount) cacheCount.textContent = 'Indisponível';
        
        const dashCacheCount = document.getElementById('dashboard-cache-count');
        if (dashCacheCount) dashCacheCount.textContent = 'Indisponível';
    }
}

/**
 * Escapa caracteres especiais de HTML para impedir que conteúdo vindo do
 * backend (nomes de arquivo, mensagens de erro, texto raspado do Google
 * Translate) seja interpretado como markup/script ao cair no innerHTML.
 */
function escapeHtml(texto) {
    return texto
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/**
 * Realiza o parse de códigos ANSI para tags HTML estilizadas
 */
function ansiParaHtml(texto) {
    let html = escapeHtml(texto);

    // Sanitização contra caracteres de controle de cursor
    html = html.replace(/\r?\033\[K/g, '');
    html = html.replace(/\r/g, '');
    
    // Substitui quebras de linha literais se houver
    html = html.replace(/\\n/g, '<br>');

    // Negritos e Resets
    html = html.replace(/\033\[1m/g, '<span style="font-weight: 700;">');
    html = html.replace(/\u001b\[1m/g, '<span style="font-weight: 700;">');
    html = html.replace(/\033\[2m/g, '<span style="opacity: 0.72;">');
    html = html.replace(/\u001b\[2m/g, '<span style="opacity: 0.72;">');
    html = html.replace(/\033\[0m/g, '</span>');
    html = html.replace(/\u001b\[0m/g, '</span>');

    // Mapeamento de Cores ANSI
    const cores = {
        '30': 'var(--text-muted)',
        '31': 'rgba(239, 68, 68, 0.95)', // Vermelho elegante
        '32': 'var(--accent-green)',
        '33': 'var(--accent-yellow)',
        '34': 'var(--accent-blue)',
        '35': 'var(--accent-purple)',
        '36': 'var(--accent-cyan)',
        '37': 'var(--text-primary)',
        '90': 'var(--text-muted)'
    };

    for (let code in cores) {
        const regex1 = new RegExp('\\033\\[' + code + 'm', 'g');
        const regex2 = new RegExp('\\u001b\\[' + code + 'm', 'g');
        const replacement = `<span style="color: ${cores[code]};">`;
        html = html.replace(regex1, replacement);
        html = html.replace(regex2, replacement);
    }

    return html;
}

/**
 * Auxiliar para formatar e exibir mensagens nos painéis de console (Padrão SSE)
 */
function logNoConsoleFormatado(consoleId, rawMessage) {
    const consoleDiv = document.getElementById(consoleId);
    if (!consoleDiv) return;

    // Remove mensagem "Aguardando..." se existir
    const sysMsg = consoleDiv.querySelector('.system-message');
    if (sysMsg) {
        consoleDiv.removeChild(sysMsg);
    }

    const timestamp = new Date().toLocaleTimeString();
    const htmlMensagem = ansiParaHtml(rawMessage);
    
    const linhaLog = document.createElement('div');
    linhaLog.className = 'log-line';
    linhaLog.innerHTML = `<span style="color: var(--text-muted); font-size: 0.75rem;">[${timestamp}]</span> ${htmlMensagem}`;
    
    consoleDiv.appendChild(linhaLog);
    
    // Proteção extrema contra travamentos: Limita a 1000 linhas visíveis no console HTML
    while (consoleDiv.childElementCount > 1000) {
        consoleDiv.removeChild(consoleDiv.firstChild);
    }

    consoleDiv.scrollTop = consoleDiv.scrollHeight;
}

/**
 * Substitui todo o conteúdo de um painel de console pelo texto de um
 * relatório já salvo em disco (ex.: relatório de Análise de Mídia), em vez
 * de acrescentar mais uma linha de log. Usado quando o backend manda o
 * conteúdo integral de um arquivo via SSE, não uma linha de log ao vivo.
 */
function exibirRelatorioSalvo(consoleId, textoRelatorio) {
    const consoleDiv = document.getElementById(consoleId);
    if (!consoleDiv) return;

    consoleDiv.innerHTML = '';

    const pre = document.createElement('pre');
    pre.className = 'relatorio-salvo';
    pre.textContent = textoRelatorio;
    consoleDiv.appendChild(pre);

    consoleDiv.scrollTop = 0;
}

/**
 * Método genérico clássico para logs manuais do frontend
 */
export function logNoConsole(consoleId, mensagem, tipo = 'info') {
    let corAnsi = '\u001b[37m'; // Branco padrão
    if (tipo === 'erro') corAnsi = '\u001b[31m';
    if (tipo === 'aviso') corAnsi = '\u001b[33m';
    if (tipo === 'sucesso') corAnsi = '\u001b[32m';
    if (tipo === 'info') corAnsi = '\u001b[36m'; // Ciano para comandos do sistema

    logNoConsoleFormatado(consoleId, `${corAnsi}${mensagem}\u001b[0m`);
}

// Configura funcionalidade de limpar os consoles
document.addEventListener('click', (e) => {
    const btn = e.target.closest('.btn-clear-console');
    if (!btn) return;

    const consoleId = btn.getAttribute('data-target');
    const consoleDiv = document.getElementById(consoleId);
    if (consoleDiv) {
        consoleDiv.innerHTML = '<div class="system-message">Console limpo. Aguardando novos logs...</div>';
    }
});

// Botão "Parar Execução" dos menus: interrompe o trabalho em execução na
// fila única do pipeline. A parada é cooperativa — o job encerra no próximo
// ponto seguro e o progresso já salvo (cache, arquivos concluídos) é
// preservado. Todos os menus usam o mesmo endpoint porque só um job roda
// por vez na fila.
// Delegação no document: o painel de Revisão de Lore é injetado
// dinamicamente (revisaoLore.html), então listeners registrados no load da
// página não alcançariam o botão dele.
document.addEventListener('click', async (e) => {
    const btn = e.target.closest('.btn-parar-pipeline');
    if (!btn) return;
    const consoleId = btn.getAttribute('data-console');
    logNoConsole(consoleId, 'Solicitando parada do trabalho em execução...', 'aviso');
    try {
        const res = await fetch('/api/pipeline/parar', { method: 'POST' });
        const data = await res.json();
        logNoConsole(consoleId, data.mensagem || 'Parada solicitada.', 'aviso');
    } catch (err) {
        logNoConsole(consoleId, `Erro ao solicitar parada: ${err.message}`, 'erro');
    }
});

// Configura funcionalidade de copiar o conteúdo de um console/relatório
document.addEventListener('click', async (e) => {
    const btn = e.target.closest('.btn-copy-console');
    if (!btn) return;

    const consoleId = btn.getAttribute('data-target');
    const consoleDiv = document.getElementById(consoleId);
    if (!consoleDiv) return;

    const texto = consoleDiv.innerText.trim();
    if (!texto) {
        mostrarAlerta('Não há conteúdo para copiar.', 'aviso');
        return;
    }

    try {
        await navigator.clipboard.writeText(texto);
        mostrarAlerta('Conteúdo copiado para a área de transferência!', 'sucesso');
    } catch (err) {
        console.warn('Falha ao copiar via Clipboard API, usando fallback.', err);
        const textarea = document.createElement('textarea');
        textarea.value = texto;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        try {
            document.execCommand('copy');
            mostrarAlerta('Conteúdo copiado para a área de transferência!', 'sucesso');
        } catch (fallbackErr) {
            mostrarAlerta('Não foi possível copiar automaticamente. Selecione o texto manualmente.', 'erro');
        } finally {
            document.body.removeChild(textarea);
        }
    }
});

/**
 * Exibe um alerta flutuante (Toast) na tela
 */
export function mostrarAlerta(mensagem, tipo = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast toast-${tipo}`;

    // Ícone do toast em Material Symbols (regra do design: nunca emojis)
    let icon = 'info';
    if (tipo === 'erro') icon = 'error';
    if (tipo === 'sucesso') icon = 'check_circle';
    if (tipo === 'aviso') icon = 'warning';

    toast.innerHTML = `
        <div class="toast-content">
            <span class="material-symbols-outlined toast-icon">${icon}</span> &nbsp; ${escapeHtml(mensagem)}
        </div>
        <button class="toast-close" title="Fechar">&times;</button>
    `;

    container.appendChild(toast);
    
    // Força reflow para animação
    toast.offsetHeight;
    toast.classList.add('show');

    // Auto-destruir após 7 segundos
    const timeout = setTimeout(() => fecharToast(toast), 7000);

    toast.querySelector('.toast-close').addEventListener('click', () => {
        clearTimeout(timeout);
        fecharToast(toast);
    });
}

function fecharToast(toast) {
    toast.classList.remove('show');
    toast.classList.add('hiding');
    toast.addEventListener('transitionend', () => {
        if (toast.parentNode) {
            toast.parentNode.removeChild(toast);
        }
    });
}

/**
 * Analisa as mensagens recebidas via SSE e dispara alertas caso sejam de erro fatal ou sucesso
 */
export function verificarAlertaSSE(mensagem) {
    // Remove códigos ANSI limpos para exibir no Toast
    const msgLimpa = mensagem.replace(/\u001B\[[0-9;]*m/g, '').replace(/\033\[[0-9;]*m/g, '').trim();
    
    if (msgLimpa.includes('[ERRO]') || msgLimpa.includes('[FAIL]')) {
        const erroMsg = msgLimpa.replace(/\[ERRO\]|\[FAIL\]/, '').trim();
        mostrarAlerta(erroMsg, 'erro');
    } 
    else if (msgLimpa.includes('PROCESSAMENTO FINALIZADO') || msgLimpa.includes('[SUCESSO]')) {
        mostrarAlerta(msgLimpa, 'sucesso');
        const btnRefresh = document.getElementById('btn-refresh-telemetria');
        if (btnRefresh) btnRefresh.click();
    }
}

/**
 * Monitora inputs de caminho/pasta e selects de contexto para carregar dinamicamente os metadados e capas de animes
 */
function limparTermoMetadata(texto) {
    if (!texto) return '';

    return texto
        .replace(/\s*-\s*Revis[aã]o\s+de\s+Lore\s*$/i, '')
        .replace(/\s+Revis[aã]o\s+de\s+Lore\s*$/i, '')
        .replace(/\s+/g, ' ')
        .trim();
}

function inicializarMetadadosDinamicos() {
    const mapeamentoFormularios = [
        { inputId: 'analise-entrada', selectId: 'analise-contexto', bannerId: 'meta-banner-analise' },
        { inputId: 'traducao-entrada', selectId: 'traducao-contexto', bannerId: 'meta-banner-traducao' },
        { inputId: 'correcao-entrada', selectId: 'correcao-contexto', bannerId: 'meta-banner-correcao' },
        { inputId: 'revisao-entrada', selectId: 'revisao-contexto', bannerId: 'meta-banner-revisao' },
        { inputId: 'cura-entrada-original', selectId: 'cura-contexto', bannerId: 'meta-banner-cura' },
        { inputId: 'revisao-lore-entrada-original', selectId: 'revisao-lore-contexto', bannerId: 'meta-banner-revisao-lore' },
        { inputId: 'troca-tipo-legenda-entrada', selectId: 'troca-tipo-legenda-contexto', bannerId: 'meta-banner-troca-tipo-legenda' },
        { inputId: 'limpanome-entrada', selectId: 'renomear-arquivos-contexto', bannerId: 'meta-banner-limpanome' }
    ];

    const atualizarItem = (item) => {
        const input = document.getElementById(item.inputId);
        const select = item.selectId ? document.getElementById(item.selectId) : null;
        let termo = '';

        // Se houver select preenchido com obra válida, usa o contexto como fonte principal de lore
        if (select && select.value && select.selectedIndex >= 0) {
            const option = select.options[select.selectedIndex];
            const optText = option.text;
            if (optText && !optText.includes('Carregando') && !optText.includes('Selecione')) {
                termo = option.dataset.metadataQuery || limparTermoMetadata(optText);
            }
        }

        // Se não houver contexto no select, ou se o usuário digitou um caminho completo de mídia (que não seja termos genéricos de pasta)
        if (!termo && input && input.value.trim().length > 3) {
            const val = input.value.trim();
            if (!['cache', 'logs', 'relatorios'].includes(val.toLowerCase())) {
                termo = val;
            }
        }

        if (termo && termo.length > 2) {
            carregarMetadataAnime(termo, item.bannerId);
        } else {
            const banner = document.getElementById(item.bannerId);
            if (banner) banner.classList.add('hidden');
        }
    };

    // Popula automaticamente todos os selects de contexto dos módulos auxiliares.
    const popularContextos = () => {
        carregarContextosAuxiliares(['analise-contexto', 'traducao-contexto', 'correcao-contexto', 'revisao-contexto', 'cura-contexto', 'revisao-lore-contexto', 'troca-tipo-legenda-contexto', 'renomear-arquivos-contexto'], () => {
            mapeamentoFormularios.forEach(atualizarItem);
        });
    };

    popularContextos();
    document.addEventListener('revisao-lore:painel-carregado', popularContextos);
    document.addEventListener('troca-tipo-legenda:painel-carregado', popularContextos);
    document.addEventListener('renomear-arquivos:painel-carregado', popularContextos);

    mapeamentoFormularios.forEach(item => {
        const input = document.getElementById(item.inputId);
        const select = item.selectId ? document.getElementById(item.selectId) : null;

        if (input) {
            input.addEventListener('input', () => atualizarItem(item));
            input.addEventListener('change', () => atualizarItem(item));
            input.addEventListener('blur', () => atualizarItem(item));
        }

        if (select) {
            select.addEventListener('change', () => atualizarItem(item));
            const observer = new MutationObserver(() => {
                setTimeout(() => atualizarItem(item), 100);
            });
            observer.observe(select, { childList: true });
        }

        // Dispara verificação inicial
        setTimeout(() => atualizarItem(item), 500);
    });

    // Emite carregado ao trocar de aba no menu lateral
    document.querySelectorAll('.nav-menu .nav-item, .sidebar-nav .nav-item').forEach(nav => {
        nav.addEventListener('click', () => {
            setTimeout(() => mapeamentoFormularios.forEach(atualizarItem), 150);
        });
    });
}

async function carregarContextosAuxiliares(idsSelects, onComplete) {
    try {
        const [response, responseRevisaoLore] = await Promise.all([
            fetch('/api/contextos', { cache: 'no-store' }),
            fetch('/api/revisao-lore/contextos', { cache: 'no-store' }).catch(() => null)
        ]);
        if (!response.ok) return;
        const contextos = await response.json();
        if (!Array.isArray(contextos) || contextos.length === 0) return;
        const contextosRevisaoLore = responseRevisaoLore?.ok
            ? await responseRevisaoLore.json()
            : contextos;

        const todosSelects = ['analise-contexto', 'traducao-contexto', 'correcao-contexto', 'revisao-contexto', 'cura-contexto', 'revisao-lore-contexto', 'troca-tipo-legenda-contexto', 'renomear-arquivos-contexto'];
        todosSelects.forEach(id => {
            const select = document.getElementById(id);
            if (!select) return;

            const ehAuxiliar = (id === 'analise-contexto' || id === 'correcao-contexto' || id === 'cura-contexto' || id === 'troca-tipo-legenda-contexto' || id === 'renomear-arquivos-contexto');
            const ehRevisaoLore = (id === 'revisao-lore-contexto');
            select.innerHTML = '';
            
            if (ehAuxiliar) {
                const optDefault = document.createElement('option');
                optDefault.value = '';
                optDefault.textContent = '-- Selecione uma obra para visualizar --';
                select.appendChild(optDefault);
            }

            if (ehRevisaoLore) {
                const optObrigatorio = document.createElement('option');
                optObrigatorio.value = '';
                optObrigatorio.textContent = '-- Selecione a obra (obrigatório) --';
                optObrigatorio.disabled = true;
                optObrigatorio.selected = true;
                select.appendChild(optObrigatorio);
            }

            const fonteContextos = ehRevisaoLore && Array.isArray(contextosRevisaoLore) && contextosRevisaoLore.length > 0
                ? contextosRevisaoLore
                : contextos;

            fonteContextos.forEach(ctx => {
                const opt = document.createElement('option');
                opt.value = ctx.id;
                opt.textContent = ctx.nome;
                if (ctx.termoMetadata) {
                    opt.dataset.metadataQuery = ctx.termoMetadata;
                }
                if (!ehAuxiliar && !ehRevisaoLore && ctx.padrao) {
                    opt.selected = true;
                }
                select.appendChild(opt);
            });
        });

        if (onComplete && typeof onComplete === 'function') {
            onComplete();
        }
    } catch (e) {
        console.warn('Falha ao carregar contextos auxiliares:', e);
    }
}

async function carregarMetadataAnime(caminho, bannerId) {
    const banner = document.getElementById(bannerId);
    if (!banner) return;

    try {
        const resp = await fetch(`/api/metadata?caminho=${encodeURIComponent(caminho)}`, { cache: 'no-store' });
        if (!resp.ok) {
            banner.classList.add('hidden');
            return;
        }

        const meta = await resp.json();
        renderizarBannerMetadata(banner, meta);
    } catch (e) {
        console.warn('Erro ao carregar metadata:', e);
        banner.classList.add('hidden');
    }
}

function renderizarBannerMetadata(banner, meta) {
    if (!meta || !meta.titulo) {
        banner.classList.add('hidden');
        return;
    }

    const posterHtml = meta.posterUrl 
        ? `<div class="meta-poster-container"><img src="${escapeHtml(meta.posterUrl)}" alt="${escapeHtml(meta.titulo)}" class="meta-poster-img" onerror="this.src='img/kronos_logo.svg'"></div>`
        : '';

    const scoreHtml = meta.score ? `<span class="meta-badge score"><span class="material-symbols-outlined">star</span> ${meta.score}</span>` : '';
    const anoHtml = meta.ano ? `<span class="meta-badge">${meta.ano}</span>` : '';
    const epsHtml = meta.episodios ? `<span class="meta-badge">${meta.episodios} eps</span>` : '';
    const subTitle = meta.tituloJapones || meta.tituloIngles || '';

    let generosHtml = '';
    if (meta.generos && meta.generos.length > 0) {
        generosHtml = meta.generos.slice(0, 3).map(g => `<span class="meta-badge genre">${escapeHtml(g)}</span>`).join('');
    }

    banner.innerHTML = `
        ${posterHtml}
        <div class="meta-info-container">
            <div class="meta-header-titles">
                <div class="meta-title-main">${escapeHtml(meta.titulo)}</div>
                ${subTitle ? `<div class="meta-title-sub">${escapeHtml(subTitle)}</div>` : ''}
            </div>
            <div class="meta-badges-row">
                ${scoreHtml}
                ${anoHtml}
                ${epsHtml}
                ${generosHtml}
            </div>
            ${meta.sinopse ? `<div class="meta-synopsis">${escapeHtml(meta.sinopse)}</div>` : ''}
        </div>
    `;

    banner.classList.remove('hidden');
}

function inicializarBotoesLimpezaFormularios() {
    document.querySelectorAll('.btn-clear-form').forEach(btn => {
        btn.addEventListener('click', () => {
            const formId = btn.getAttribute('data-form');
            const form = formId ? document.getElementById(formId) : btn.closest('form');
            if (!form) return;

            // Reseta todos os inputs de texto e selects do formulário
            form.querySelectorAll('input[type="text"]').forEach(input => input.value = '');
            form.querySelectorAll('select').forEach(select => {
                select.selectedIndex = 0;
            });

            // Oculta o banner de metadados associado se houver
            const cardParent = form.closest('.glass-card');
            if (cardParent) {
                const banner = cardParent.querySelector('.anime-meta-banner');
                if (banner) banner.classList.add('hidden');
            }
        });
    });
}

function inicializarControlesConsole() {
    document.addEventListener('click', (e) => {
        const btn = e.target.closest('.btn-toggle-console');
        if (!btn) return;

        const targetId = btn.getAttribute('data-target');
        const consoleBody = targetId ? document.getElementById(targetId) : null;
        if (!consoleBody) return;

        consoleBody.classList.toggle('expanded');
        if (consoleBody.classList.contains('expanded')) {
            btn.innerHTML = '<span class="material-symbols-outlined console-action-icon">unfold_less</span>Encolher';
        } else {
            btn.innerHTML = '<span class="material-symbols-outlined console-action-icon">unfold_more</span>Expandir';
        }
    });
}

function inicializarBotoesProcurarCaminho() {
    document.body.addEventListener('click', async (e) => {
        const btn = e.target.closest('.btn-procurar');
        if (!btn) return;

        const targetId = btn.getAttribute('data-target');
        const tipo = btn.getAttribute('data-type') || 'pasta';
        const inputTarget = document.getElementById(targetId);
        if (!inputTarget) return;

        const textoOriginal = btn.innerHTML;
        btn.innerHTML = '⏳ Abrindo...';
        btn.disabled = true;

        try {
            const endpoint = tipo === 'arquivo' ? '/api/dialogo/selecionar-arquivo' : '/api/dialogo/selecionar-pasta';
            const res = await fetch(endpoint);
            if (res.ok) {
                const data = await res.json();
                if (data.caminho) {
                    inputTarget.value = data.caminho;
                    inputTarget.dispatchEvent(new Event('input', { bubbles: true }));
                    inputTarget.dispatchEvent(new Event('change', { bubbles: true }));
                }
            }
        } catch (err) {
            console.error('Erro ao abrir seletor nativo:', err);
        } finally {
            btn.innerHTML = textoOriginal;
            btn.disabled = false;
        }
    });
}

/**
 * Menu "Sair": encerra a aplicação inteira (servidor + trabalho em execução).
 * Fluxo: abre o modal de confirmação (avisando se a fila do pipeline está
 * ocupada), chama POST /api/sistema/sair e cobre a tela com o aviso final.
 * A parada do job em execução é cooperativa — mesmo comportamento do botão
 * "Parar Execução" dos menus.
 */
function inicializarBotaoSair() {
    const btnMenu = document.getElementById('btn-menu-sair');
    const modal = document.getElementById('modal-sair');
    const avisoPipeline = document.getElementById('modal-sair-aviso-pipeline');
    const btnCancelar = document.getElementById('btn-sair-cancelar');
    const btnConfirmar = document.getElementById('btn-sair-confirmar');
    const overlayEncerrado = document.getElementById('overlay-encerrado');
    if (!btnMenu || !modal || !btnCancelar || !btnConfirmar || !overlayEncerrado) return;

    btnMenu.addEventListener('click', async () => {
        modal.classList.remove('hidden');
        if (avisoPipeline) {
            avisoPipeline.classList.add('hidden');
            try {
                const res = await fetch('/api/pipeline/status');
                const data = await res.json();
                if (data.mensagem === 'ocupada') {
                    avisoPipeline.classList.remove('hidden');
                }
            } catch (e) {
                // Status indisponível não impede a confirmação de saída.
            }
        }
    });

    btnCancelar.addEventListener('click', () => modal.classList.add('hidden'));
    modal.addEventListener('click', (e) => {
        if (e.target === modal) modal.classList.add('hidden');
    });

    btnConfirmar.addEventListener('click', async () => {
        btnConfirmar.disabled = true;
        btnCancelar.disabled = true;
        try {
            const res = await fetch('/api/sistema/sair', { method: 'POST' });
            const data = await res.json();
            mostrarAlerta(data.mensagem || 'Encerrando a aplicação...', 'aviso');
            modal.classList.add('hidden');
            overlayEncerrado.classList.remove('hidden');
            // Navegadores só permitem window.close() em janelas abertas por
            // script; se não fechar, o overlay orienta a fechar manualmente.
            setTimeout(() => window.close(), 1500);
        } catch (err) {
            mostrarAlerta(`Erro ao encerrar a aplicação: ${err.message}`, 'erro');
            btnConfirmar.disabled = false;
            btnCancelar.disabled = false;
        }
    });
}

// Metadados e utilitários de formulário são inicializados no DOMContentLoaded principal acima.
