const CHAVE_IDIOMA = 'kronos.idioma.interface';
const CHAVE_CACHE_PREFIXO = 'kronos.i18n.automatico.v1.';
const IDIOMAS = Object.freeze(['pt-BR', 'en-US', 'es-ES']);
const ATRIBUTOS = Object.freeze(['placeholder', 'title', 'aria-label', 'data-tooltip', 'alt']);
const originaisTexto = new WeakMap();
const originaisAtributos = new WeakMap();
const sessoes = new Map();

let idiomaAtual = 'pt-BR';
let observador = null;
let filaTraducao = Promise.resolve();
let geracaoTraducao = 0;

/**
 * PROPÓSITO DE NEGÓCIO: converte a preferência regional do navegador para um
 * dos idiomas oferecidos pelas bandeiras do KRONOS.
 * INVARIANTES DO DOMÍNIO: português resolve para pt-BR, espanhol para es-ES e
 * qualquer outro idioma usa en-US como fallback internacional.
 * COMPORTAMENTO EM CASO DE FALHA: entrada nula ou ilegível retorna en-US.
 */
export function normalizarIdioma(valor) {
    const idioma = String(valor || '').trim().toLowerCase();
    if (idioma.startsWith('pt')) return 'pt-BR';
    if (idioma.startsWith('es')) return 'es-ES';
    if (idioma.startsWith('en')) return 'en-US';
    return 'en-US';
}

/**
 * PROPÓSITO DE NEGÓCIO: escolhe o idioma inicial priorizando a seleção manual
 * salva e usando a região do navegador somente na primeira visita.
 * INVARIANTES DO DOMÍNIO: somente os três idiomas das bandeiras são aceitos.
 * COMPORTAMENTO EM CASO DE FALHA: lista vazia ou preferência inválida usa en-US.
 */
export function resolverIdiomaInicial(preferencia, idiomasNavegador = []) {
    if (IDIOMAS.includes(preferencia)) return preferencia;
    const candidatos = Array.isArray(idiomasNavegador) ? idiomasNavegador : [idiomasNavegador];
    return normalizarIdioma(candidatos.find(Boolean));
}

/**
 * PROPÓSITO DE NEGÓCIO: informa se o navegador pode traduzir a interface
 * localmente, sem catálogo manual e sem enviar os textos a um servidor.
 * INVARIANTES DO DOMÍNIO: exige contexto seguro e a Translator API nativa.
 * COMPORTAMENTO EM CASO DE FALHA: qualquer ausência de recurso retorna falso.
 */
export function navegadorSuportaTraducao() {
    return Boolean(globalThis.isSecureContext && 'Translator' in globalThis);
}

/**
 * PROPÓSITO DE NEGÓCIO: impede que legendas, caminhos, código, logs e dados de
 * telemetria sejam traduzidos como se fossem controles da interface.
 * INVARIANTES DO DOMÍNIO: elementos com translate=no ou data-i18n-ignore
 * protegem toda a sua subárvore; ícones Material Symbols nunca são alterados.
 * COMPORTAMENTO EM CASO DE FALHA: nó sem ancestral é aceito para posterior
 * validação textual conservadora.
 */
function deveIgnorar(no) {
    const elemento = no?.nodeType === Node.ELEMENT_NODE ? no : no?.parentElement;
    return Boolean(elemento?.closest(
        'script, style, code, pre, textarea, [translate="no"], [data-i18n-ignore], '
        + '.material-symbols-outlined, .console-body, .auditor-evento-texto, '
        + '.auditor-anomalias-lista, .markdown-body, .mapa-code, .mapa-tree, '
        + '.anime-meta-banner, .telemetria-log'
    ));
}

/**
 * PROPÓSITO DE NEGÓCIO: filtra o conteúdo textual que pode ser enviado ao
 * tradutor local, evitando símbolos, URLs, caminhos e identificadores técnicos.
 * INVARIANTES DO DOMÍNIO: somente texto humano com letras e até 2.000 caracteres
 * entra na fila; valores de campos nunca são lidos.
 * COMPORTAMENTO EM CASO DE FALHA: texto ausente ou ambíguo retorna falso.
 */
function textoEhTraduzivel(texto) {
    const valor = String(texto || '').trim();
    if (valor.length < 2 || valor.length > 2000 || !/\p{L}/u.test(valor)) return false;
    if (/^(https?:|file:|[A-Za-z]:\\|\\\\|\/api\/)/i.test(valor)) return false;
    if (/^[\w.-]+\.(ass|ssa|srt|mkv|json|md|txt|java|js|css)$/i.test(valor)) return false;
    return true;
}

/**
 * PROPÓSITO DE NEGÓCIO: conserva o espaçamento de layout do HTML ao substituir
 * somente o conteúdo humano pela tradução automática.
 * INVARIANTES DO DOMÍNIO: prefixo e sufixo em branco permanecem byte a byte.
 * COMPORTAMENTO EM CASO DE FALHA: valor não textual é convertido com segurança.
 */
function aplicarEspacamento(original, traducao) {
    const partes = String(original).match(/^(\s*)([\s\S]*?)(\s*)$/);
    return `${partes?.[1] || ''}${traducao}${partes?.[3] || ''}`;
}

/**
 * PROPÓSITO DE NEGÓCIO: lê o cache local de traduções da UI para que reabrir a
 * aplicação não repita centenas de inferências no navegador.
 * INVARIANTES DO DOMÍNIO: o cache é separado por idioma e contém somente pares
 * de rótulos da interface, nunca textos de legenda ou logs.
 * COMPORTAMENTO EM CASO DE FALHA: JSON inválido ou localStorage indisponível
 * produz um mapa vazio e autorrecuperável.
 */
function carregarCache(idioma) {
    try {
        const salvo = JSON.parse(localStorage.getItem(CHAVE_CACHE_PREFIXO + idioma) || '{}');
        return salvo && typeof salvo === 'object' ? new Map(Object.entries(salvo)) : new Map();
    } catch (ignored) {
        return new Map();
    }
}

/**
 * PROPÓSITO DE NEGÓCIO: persiste as traduções produzidas pelo próprio navegador
 * para acelerar navegações futuras e reduzir consumo de CPU.
 * INVARIANTES DO DOMÍNIO: grava somente strings da interface no idioma alvo.
 * COMPORTAMENTO EM CASO DE FALHA: quota ou bloqueio do armazenamento é ignorado
 * sem interromper a troca de idioma da sessão atual.
 */
function salvarCache(idioma, cache) {
    try {
        localStorage.setItem(CHAVE_CACHE_PREFIXO + idioma, JSON.stringify(Object.fromEntries(cache)));
    } catch (ignored) {
        // A tradução continua válida em memória nesta sessão.
    }
}

/**
 * PROPÓSITO DE NEGÓCIO: transforma falhas técnicas do navegador em orientação
 * curta e compreensível para quem troca o idioma da interface.
 * INVARIANTES DO DOMÍNIO: mensagens internas em inglês não são expostas quando
 * correspondem a restrições conhecidas de ativação ou compatibilidade.
 * COMPORTAMENTO EM CASO DE FALHA: erro desconhecido recebe uma mensagem neutra
 * em português e a causa original permanece disponível no console do navegador.
 */
function mensagemAmigavelErro(erro) {
    const mensagem = String(erro?.message || '');
    if (/user gesture|user activation|ativação do usuário/i.test(mensagem)) {
        return 'O navegador exige um clique direto na bandeira para baixar o tradutor local.';
    }
    if (/not supported|unavailable|indisponível/i.test(mensagem)) {
        return 'A tradução automática local não está disponível neste navegador.';
    }
    return 'Não foi possível traduzir a interface. Ela foi restaurada para português.';
}

/**
 * PROPÓSITO DE NEGÓCIO: apresenta progresso e falhas da tradução automática no
 * próprio seletor, sem depender do backend ou do sistema global de toasts.
 * INVARIANTES DO DOMÍNIO: mensagens são curtas, não bloqueiam a navegação e o
 * estado vazio remove o texto anterior.
 * COMPORTAMENTO EM CASO DE FALHA: seletor ausente é tolerado.
 */
function atualizarStatus(mensagem = '', erro = false) {
    const status = document.getElementById('idioma-status');
    if (!status) return;
    status.textContent = mensagem;
    status.classList.toggle('erro', erro);
    status.classList.toggle('visivel', Boolean(mensagem));
}

/**
 * PROPÓSITO DE NEGÓCIO: inicia o tradutor local no mesmo gesto do clique para
 * permitir que o navegador baixe o pacote de idioma quando necessário.
 * INVARIANTES DO DOMÍNIO: existe no máximo uma promessa de sessão por idioma e
 * todo processamento permanece no dispositivo do operador.
 * COMPORTAMENTO EM CASO DE FALHA: remove a sessão rejeitada para permitir nova
 * tentativa e propaga o erro ao fluxo que restaura a interface em português.
 */
function criarSessaoLocal(idioma) {
    const alvo = idioma === 'es-ES' ? 'es' : 'en';
    if (sessoes.has(alvo)) return sessoes.get(alvo);
    if (!navegadorSuportaTraducao()) {
        throw new Error('Tradução automática indisponível. Use Chrome 138 ou superior.');
    }
    const criada = globalThis.Translator.create({
        sourceLanguage: 'pt',
        targetLanguage: alvo,
        monitor(monitor) {
            monitor.addEventListener('downloadprogress', evento => {
                const progresso = Math.round((evento.loaded || 0) * 100);
                atualizarStatus(`Baixando tradutor local… ${progresso}%`);
            });
        }
    });
    const promessa = Promise.resolve(criada).catch(erro => {
        sessoes.delete(alvo);
        throw erro;
    });
    sessoes.set(alvo, promessa);
    return promessa;
}

/**
 * PROPÓSITO DE NEGÓCIO: obtém uma sessão de tradução pt→idioma gerenciada pelo
 * navegador e acompanha o download inicial do modelo local.
 * INVARIANTES DO DOMÍNIO: no máximo uma sessão é criada por idioma alvo.
 * COMPORTAMENTO EM CASO DE FALHA: API indisponível ou par não suportado lança
 * erro didático para o seletor manter a página em português.
 */
async function obterSessao(idioma) {
    const alvo = idioma === 'es-ES' ? 'es' : 'en';
    if (sessoes.has(alvo)) return sessoes.get(alvo);
    if (!navegadorSuportaTraducao()) {
        throw new Error('Tradução automática indisponível. Use Chrome 138 ou superior.');
    }
    const disponibilidade = await globalThis.Translator.availability({
        sourceLanguage: 'pt', targetLanguage: alvo
    });
    if (disponibilidade === 'unavailable') {
        throw new Error('O navegador não oferece o pacote de idioma solicitado.');
    }
    if (disponibilidade === 'downloadable' || disponibilidade === 'downloading') {
        throw new Error('Clique na bandeira para autorizar o download do tradutor local.');
    }
    return criarSessaoLocal(idioma);
}

/**
 * PROPÓSITO DE NEGÓCIO: coleta rótulos, descrições e atributos traduzíveis de
 * uma área da SPA e registra seus valores canônicos em português.
 * INVARIANTES DO DOMÍNIO: cada alvo mantém referência ao original e elementos
 * operacionais protegidos nunca entram no conjunto.
 * COMPORTAMENTO EM CASO DE FALHA: raiz inexistente devolve lista vazia.
 */
function coletarAlvos(raiz) {
    if (!raiz) return [];
    const alvos = [];
    const processarElemento = elemento => {
        if (!(elemento instanceof Element) || deveIgnorar(elemento)) return;
        let mapa = originaisAtributos.get(elemento);
        if (!mapa) {
            mapa = new Map();
            originaisAtributos.set(elemento, mapa);
        }
        ATRIBUTOS.forEach(atributo => {
            if (!elemento.hasAttribute(atributo)) return;
            if (!mapa.has(atributo)) mapa.set(atributo, elemento.getAttribute(atributo));
            const original = mapa.get(atributo);
            if (textoEhTraduzivel(original)) {
                alvos.push({ original, aplicar: valor => elemento.setAttribute(atributo, valor) });
            }
        });
    };
    if (raiz instanceof Element) processarElemento(raiz);
    if (raiz.nodeType === Node.TEXT_NODE) {
        if (!deveIgnorar(raiz) && textoEhTraduzivel(raiz.nodeValue)) {
            if (!originaisTexto.has(raiz)) originaisTexto.set(raiz, raiz.nodeValue);
            alvos.push({ original: originaisTexto.get(raiz), aplicar: valor => { raiz.nodeValue = valor; } });
        }
        return alvos;
    }
    const walker = document.createTreeWalker(raiz, NodeFilter.SHOW_ELEMENT | NodeFilter.SHOW_TEXT);
    let no = walker.nextNode();
    while (no) {
        if (no.nodeType === Node.ELEMENT_NODE) processarElemento(no);
        if (no.nodeType === Node.TEXT_NODE && !deveIgnorar(no) && textoEhTraduzivel(no.nodeValue)) {
            const noTexto = no;
            if (!originaisTexto.has(noTexto)) originaisTexto.set(noTexto, noTexto.nodeValue);
            alvos.push({
                original: originaisTexto.get(noTexto),
                aplicar: valor => { noTexto.nodeValue = valor; }
            });
        }
        no = walker.nextNode();
    }
    return alvos;
}

/**
 * PROPÓSITO DE NEGÓCIO: restaura imediatamente a fonte pt-BR antes de trocar
 * para outro idioma ou quando o operador escolhe a bandeira do Brasil.
 * INVARIANTES DO DOMÍNIO: somente nós previamente capturados são restaurados.
 * COMPORTAMENTO EM CASO DE FALHA: nós removidos do DOM não afetam os demais.
 */
function restaurarPortugues(raiz = document) {
    coletarAlvos(raiz).forEach(alvo => alvo.aplicar(alvo.original));
}

/**
 * PROPÓSITO DE NEGÓCIO: traduz uma subárvore completa por inferência local do
 * navegador, reutilizando resultados idênticos e o cache persistente.
 * INVARIANTES DO DOMÍNIO: aplicações pertencentes a uma troca antiga são
 * descartadas e nenhum texto sem tradução válida é apagado.
 * COMPORTAMENTO EM CASO DE FALHA: mantém o português, informa a causa e não
 * bloqueia as funções operacionais do KRONOS.
 */
async function traduzirSubarvore(raiz, idioma, geracao) {
    if (idioma === 'pt-BR' || geracao !== geracaoTraducao) return;
    const alvos = coletarAlvos(raiz);
    if (!alvos.length) return;
    const porTexto = new Map();
    alvos.forEach(alvo => {
        const chave = String(alvo.original).trim().replace(/\s+/g, ' ');
        if (!porTexto.has(chave)) porTexto.set(chave, []);
        porTexto.get(chave).push(alvo);
    });
    try {
        const sessao = await obterSessao(idioma);
        const cache = carregarCache(idioma);
        let concluidos = 0;
        const total = porTexto.size;
        for (const [texto, destinos] of porTexto) {
            if (geracao !== geracaoTraducao || idiomaAtual !== idioma) return;
            let traducao = cache.get(texto);
            if (!traducao) {
                traducao = await sessao.translate(texto);
                if (traducao?.trim()) cache.set(texto, traducao.trim());
            }
            if (traducao?.trim()) {
                destinos.forEach(destino => destino.aplicar(aplicarEspacamento(destino.original, traducao.trim())));
            }
            concluidos++;
            if (concluidos === 1 || concluidos === total || concluidos % 20 === 0) {
                atualizarStatus(`Traduzindo interface… ${concluidos}/${total}`);
            }
        }
        salvarCache(idioma, cache);
        atualizarStatus('');
    } catch (erro) {
        if (geracao !== geracaoTraducao) return;
        idiomaAtual = 'pt-BR';
        document.documentElement.lang = idiomaAtual;
        restaurarPortugues(document);
        atualizarBandeiras();
        console.warn('Falha na tradução automática local da interface:', erro);
        atualizarStatus(mensagemAmigavelErro(erro), true);
    }
}

/**
 * PROPÓSITO DE NEGÓCIO: mantém seleção visual e atributos de acessibilidade das
 * bandeiras coerentes com o idioma efetivamente apresentado.
 * INVARIANTES DO DOMÍNIO: exatamente uma bandeira fica pressionada.
 * COMPORTAMENTO EM CASO DE FALHA: seletor não carregado é ignorado.
 */
function atualizarBandeiras() {
    document.querySelectorAll('[data-idioma]').forEach(botao => {
        const ativo = botao.dataset.idioma === idiomaAtual;
        botao.classList.toggle('ativo', ativo);
        botao.setAttribute('aria-pressed', String(ativo));
    });
}

/**
 * PROPÓSITO DE NEGÓCIO: aplica a escolha da bandeira, persiste a preferência e
 * agenda a tradução automática sem travar a navegação da SPA.
 * INVARIANTES DO DOMÍNIO: pt-BR sempre restaura o conteúdo canônico e toda nova
 * troca invalida traduções assíncronas anteriores.
 * COMPORTAMENTO EM CASO DE FALHA: idioma desconhecido usa o fallback en-US.
 */
export function definirIdioma(idioma, persistir = true) {
    idiomaAtual = IDIOMAS.includes(idioma) ? idioma : normalizarIdioma(idioma);
    geracaoTraducao++;
    const geracao = geracaoTraducao;
    restaurarPortugues(document);
    document.documentElement.lang = idiomaAtual;
    if (persistir) {
        try { localStorage.setItem(CHAVE_IDIOMA, idiomaAtual); } catch (ignored) { /* sessão continua */ }
    }
    atualizarBandeiras();
    atualizarStatus('');
    if (idiomaAtual !== 'pt-BR') {
        filaTraducao = filaTraducao.then(() => traduzirSubarvore(document, idiomaAtual, geracao));
    }
    document.dispatchEvent(new CustomEvent('kronos:idioma-alterado', { detail: { idioma: idiomaAtual } }));
    return idiomaAtual;
}

/**
 * PROPÓSITO DE NEGÓCIO: liga os botões 🇧🇷, 🇺🇸 e 🇪🇸 à troca de idioma sem
 * duplicar listeners quando módulos da SPA são reinicializados.
 * INVARIANTES DO DOMÍNIO: cada botão válido recebe um único listener.
 * COMPORTAMENTO EM CASO DE FALHA: botão sem data-idioma é ignorado.
 */
function vincularBandeiras() {
    document.querySelectorAll('[data-idioma]').forEach(botao => {
        if (botao.dataset.vinculado === 'true') return;
        botao.dataset.vinculado = 'true';
        botao.addEventListener('click', () => {
            const idioma = botao.dataset.idioma;
            if (idioma !== 'pt-BR' && navegadorSuportaTraducao()) {
                try {
                    criarSessaoLocal(idioma);
                } catch (ignored) {
                    // definirIdioma apresentará a falha sem bloquear o restante da interface.
                }
            }
            definirIdioma(idioma, true);
        });
    });
}

/**
 * PROPÓSITO DE NEGÓCIO: traduz automaticamente painéis injetados por fetch e
 * novos controles criados por JavaScript após a seleção de outro idioma.
 * INVARIANTES DO DOMÍNIO: existe um único observador global e cada lote respeita
 * a geração e o idioma vigentes.
 * COMPORTAMENTO EM CASO DE FALHA: navegador sem MutationObserver mantém a
 * tradução inicial e a troca manual disponíveis.
 */
function observarModulosDinamicos() {
    if (observador || typeof MutationObserver === 'undefined') return;
    observador = new MutationObserver(mutacoes => {
        if (idiomaAtual === 'pt-BR') return;
        const idioma = idiomaAtual;
        const geracao = geracaoTraducao;
        mutacoes.forEach(mutacao => mutacao.addedNodes.forEach(no => {
            filaTraducao = filaTraducao.then(() => traduzirSubarvore(no, idioma, geracao));
        }));
    });
    observador.observe(document.body, { childList: true, subtree: true });
}

/**
 * PROPÓSITO DE NEGÓCIO: inicializa detecção regional, preferência persistida,
 * bandeiras e tradução automática local antes dos demais módulos da interface.
 * INVARIANTES DO DOMÍNIO: nenhuma classe Java, arquivo de tradução ou chamada
 * externa é necessária; pt-BR permanece como fonte canônica.
 * COMPORTAMENTO EM CASO DE FALHA: localStorage indisponível usa navigator e a
 * ausência da Translator API mantém a interface funcional em português.
 */
export function inicializarI18n() {
    let preferencia = null;
    try { preferencia = localStorage.getItem(CHAVE_IDIOMA); } catch (ignored) { preferencia = null; }
    const navegadores = navigator.languages?.length ? navigator.languages : [navigator.language];
    vincularBandeiras();
    observarModulosDinamicos();
    return definirIdioma(resolverIdiomaInicial(preferencia, navegadores), false);
}

/**
 * PROPÓSITO DE NEGÓCIO: fornece o locale ativo para módulos que formatam datas
 * e números, sem expor detalhes internos da tradução automática.
 * INVARIANTES DO DOMÍNIO: retorno sempre pertence ao conjunto das bandeiras.
 * COMPORTAMENTO EM CASO DE FALHA: antes da inicialização retorna pt-BR.
 */
export function obterIdiomaAtual() {
    return idiomaAtual;
}
