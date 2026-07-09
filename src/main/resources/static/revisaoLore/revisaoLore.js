import { logNoConsole, mostrarAlerta } from '../js/app.js';

const PAINEL_HTML = 'revisaoLore/revisaoLore.html?v=3.1';

async function carregarPainelHtml() {
    const painel = document.getElementById('panel-revisao-lore');
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
    const btnIniciar = document.getElementById('btn-iniciar-revisao-lore');
    const inputOriginal = document.getElementById('revisao-lore-entrada-original');
    const inputTraduzida = document.getElementById('revisao-lore-entrada-traduzida');
    const selectContexto = document.getElementById('revisao-lore-contexto');
    const chkTodasFalas = document.getElementById('revisao-lore-todas-falas');

    if (!btnIniciar || !inputOriginal || !inputTraduzida || !selectContexto) return;

    btnIniciar.addEventListener('click', async () => {
        const diretorioOriginal = inputOriginal.value.trim();
        const diretorioTraduzido = inputTraduzida.value.trim();
        const contextoId = selectContexto.value;

        if (!diretorioOriginal || !diretorioTraduzido) {
            mostrarAlerta('Informe as pastas com legendas originais e traduzidas!', 'erro');
            return;
        }
        if (!contextoId) {
            mostrarAlerta('Selecione a obra/contexto para carregar a lore oficial.', 'erro');
            return;
        }

        const revisarTodasFalas = chkTodasFalas ? chkTodasFalas.checked : false;
        const nomeObra = selectContexto.options[selectContexto.selectedIndex]?.text || contextoId;

        logNoConsole('console-revisao-lore', `Iniciando revisão de lore — Obra: ${nomeObra}`, 'info');
        logNoConsole('console-revisao-lore', `Original: ${diretorioOriginal} | Traduzida: ${diretorioTraduzido}`, 'info');
        btnIniciar.disabled = true;

        try {
            const res = await fetch('/api/revisar-lore', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    diretorioOriginal,
                    diretorioTraduzido,
                    contextoId,
                    revisarTodasFalas
                })
            });

            const data = await res.json().catch(() => ({}));

            if (!res.ok) {
                throw new Error(data.erro || 'Falha ao iniciar revisão de lore');
            }

            logNoConsole('console-revisao-lore', data.mensagem || 'Revisão de lore iniciada.', 'sucesso');
            mostrarAlerta('Revisão de lore iniciada! Acompanhe os logs.', 'sucesso');
        } catch (err) {
            logNoConsole('console-revisao-lore', `Erro: ${err.message}`, 'erro');
            mostrarAlerta(err.message, 'erro');
        } finally {
            btnIniciar.disabled = false;
        }
    });
}

export async function initRevisaoLore() {
    try {
        await carregarPainelHtml();
        vincularEventos();
        document.dispatchEvent(new CustomEvent('revisao-lore:painel-carregado'));
    } catch (err) {
        console.error('[Revisão de Lore] Erro ao carregar painel:', err);
        const painel = document.getElementById('panel-revisao-lore');
        if (painel) {
            painel.innerHTML = '<div class="glass-card"><p class="card-desc">Não foi possível carregar o painel de Revisão de Lore.</p></div>';
        }
    }
}
