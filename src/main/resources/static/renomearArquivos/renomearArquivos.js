import { mostrarAlerta, logNoConsole } from '../js/app.js?v=3.0';

const PAINEL_HTML = 'renomearArquivos/renomearArquivos.html?v=3.0';

async function carregarPainelHtml() {
    const painel = document.getElementById('panel-renomear-arquivos');
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

export async function initRenomearArquivos() {
    try {
        await carregarPainelHtml();
        vincularEventos();
        document.dispatchEvent(new CustomEvent('renomear-arquivos:painel-carregado'));
    } catch (err) {
        console.error('[Renomear Arquivos] Erro ao carregar painel:', err);
        const painel = document.getElementById('panel-renomear-arquivos');
        if (painel) {
            painel.innerHTML = '<div class="glass-card"><p class="card-desc">Não foi possível carregar o painel de Renomear Arquivos.</p></div>';
        }
    }
}

function vincularEventos() {
    const form = document.getElementById('form-renomear-arquivos');
    if (!form) return;

    const btnSimular = document.getElementById('btn-limpanome-simular');
    const btnAplicar = document.getElementById('btn-limpanome-aplicar');
    const btnReverter = document.getElementById('btn-limpanome-reverter');
    const selectContexto = document.getElementById('renomear-arquivos-contexto');
    const inputPadrao = document.getElementById('limpanome-padrao');
    const consoleId = 'console-renomear-arquivos';

    // Ao mudar o select, preencher automaticamente o nome padrão
    if (selectContexto && inputPadrao) {
        selectContexto.addEventListener('change', () => {
            const optText = selectContexto.options[selectContexto.selectedIndex]?.text;
            if (optText && !optText.includes('Carregando') && !optText.includes('Selecione')) {
                const limpo = optText.replace(/\s*-\s*Revis[aã]o\s+de\s+Lore\s*$/i, '')
                                     .replace(/\s+Revis[aã]o\s+de\s+Lore\s*$/i, '')
                                     .trim();
                inputPadrao.value = limpo;
            }
        });
    }

    if (btnSimular) {
        btnSimular.addEventListener('click', async () => {
            if (!validarForm()) return;
            await executarOperacao('/api/renomear-arquivos/simular', 'Simulação de Renomeação (Dry-Run)');
        });
    }

    if (btnAplicar) {
        btnAplicar.addEventListener('click', async (e) => {
            e.preventDefault();
            if (!validarForm()) return;
            await executarOperacao('/api/renomear-arquivos/aplicar', 'Aplicar Renomeação');
        });
    }

    if (btnReverter) {
        btnReverter.addEventListener('click', async () => {
            const entrada = document.getElementById('limpanome-entrada').value.trim();
            if (!entrada) {
                mostrarAlerta('Preencha a pasta dos arquivos para buscar o backup de reversão.', 'aviso');
                return;
            }
            if (confirm('Tem certeza que deseja reverter a última renomeação nesta pasta?')) {
                await executarOperacao('/api/renomear-arquivos/reverter', 'Reverter Renomeação');
            }
        });
    }

    function validarForm() {
        const entrada = document.getElementById('limpanome-entrada').value.trim();
        const padrao = document.getElementById('limpanome-padrao').value.trim();
        
        if (!entrada || !padrao) {
            mostrarAlerta('Preencha os campos obrigatórios (Pasta e Nome Padrão).', 'aviso');
            return false;
        }
        return true;
    }

    async function executarOperacao(url, descricao) {
        const entrada = document.getElementById('limpanome-entrada').value.trim();
        const padrao = document.getElementById('limpanome-padrao').value.trim();

        logNoConsole(consoleId, `Iniciando ${descricao} em: ${entrada}`, 'info');

        // Desabilita botões
        const botoes = form.querySelectorAll('button');
        botoes.forEach(b => b.disabled = true);

        try {
            const resp = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    caminhoOrigem: entrada,
                    nomePadrao: padrao
                })
            });

            // SSE capturará os logs visuais caso a api lance eventos no backend,
            // mas nós podemos imprimir a resposta também caso retorne um body curto
            if (resp.ok) {
                const contentType = resp.headers.get("content-type");
                if (contentType && contentType.includes("application/json")) {
                    const dados = await resp.json();
                    if (dados.mensagem) {
                        logNoConsole(consoleId, dados.mensagem, 'sucesso');
                        mostrarAlerta(dados.mensagem, 'sucesso');
                    }
                }
            } else {
                let msgErro = `Erro HTTP ${resp.status}`;
                try {
                    const errorObj = await resp.json();
                    msgErro = errorObj.error || msgErro;
                } catch (e) {
                    msgErro = await resp.text();
                }
                logNoConsole(consoleId, `Falha na operação: ${msgErro}`, 'erro');
                mostrarAlerta(`Erro: ${msgErro}`, 'erro');
            }
        } catch (e) {
            logNoConsole(consoleId, `Erro de rede: ${e.message}`, 'erro');
            mostrarAlerta('Erro de conexão ao servidor.', 'erro');
        } finally {
            botoes.forEach(b => b.disabled = false);
        }
    }
}
