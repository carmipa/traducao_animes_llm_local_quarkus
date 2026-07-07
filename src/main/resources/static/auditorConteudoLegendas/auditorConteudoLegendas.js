export function initAuditorConteudo() {
    const formAuditor = document.getElementById('form-auditor-conteudo');
    const tableBody = document.querySelector('#table-auditor-conteudo tbody');
    const statusBadge = document.getElementById('auditor-status-badge');

    if (formAuditor) {
        formAuditor.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const original = document.getElementById('auditor-original').value;
            const traduzido = document.getElementById('auditor-traduzido').value;
            
            if (!original || !traduzido) {
                alert('Forneça os caminhos dos arquivos original e traduzido.');
                return;
            }

            statusBadge.textContent = 'Auditando...';
            statusBadge.className = 'status-badge pulse-purple';
            tableBody.innerHTML = '<tr><td colspan="4" style="text-align: center;">Analisando arquivos...</td></tr>';

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
                renderizarRelatorio(relatorio);
                
            } catch (err) {
                statusBadge.textContent = 'Erro';
                statusBadge.className = 'status-badge pulse-red';
                tableBody.innerHTML = `<tr><td colspan="4" style="text-align: center; color: var(--danger-color);">Erro: ${err.message}</td></tr>`;
                console.error(err);
            }
        });
    }

    function renderizarRelatorio(relatorio) {
        tableBody.innerHTML = '';
        
        if (relatorio.limpo) {
            statusBadge.textContent = 'Limpo';
            statusBadge.className = 'status-badge pulse-green';
            tableBody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: var(--success-color);">Nenhuma anomalia detectada! O arquivo está limpo.</td></tr>';
            return;
        }

        statusBadge.textContent = `${relatorio.anomalias.length} Anomalias`;
        statusBadge.className = 'status-badge pulse-red';

        relatorio.anomalias.forEach(anom => {
            const tr = document.createElement('tr');
            
            // Severidade com Badge
            const tdSev = document.createElement('td');
            const spanSev = document.createElement('span');
            spanSev.className = `status-badge ${getSeveridadeClass(anom.severidade)}`;
            spanSev.textContent = anom.severidade;
            tdSev.appendChild(spanSev);

            // Regra
            const tdRegra = document.createElement('td');
            tdRegra.textContent = anom.regra;

            // Descrição (com preview do original/traduzido)
            const tdDesc = document.createElement('td');
            let descHtml = `<strong>${anom.descricao}</strong>`;
            
            if (anom.eventoOriginal) {
                descHtml += `<div style="font-family: monospace; font-size: 0.85em; margin-top: 5px; color: var(--text-muted); background: rgba(255,255,255,0.05); padding: 5px; border-radius: 4px;">
                    <strong>[${anom.eventoOriginal.indice}] Orig:</strong> ${escapeHtml(anom.eventoOriginal.texto)}
                </div>`;
            }
            if (anom.eventoTraduzido) {
                descHtml += `<div style="font-family: monospace; font-size: 0.85em; margin-top: 5px; color: var(--accent-red); background: rgba(255,255,255,0.05); padding: 5px; border-radius: 4px;">
                    <strong>[${anom.eventoTraduzido.indice}] Trad:</strong> ${escapeHtml(anom.eventoTraduzido.texto)}
                </div>`;
            }
            tdDesc.innerHTML = descHtml;

            // Recomendação
            const tdRec = document.createElement('td');
            tdRec.textContent = anom.sugestaoCorrecao;

            tr.appendChild(tdSev);
            tr.appendChild(tdRegra);
            tr.appendChild(tdDesc);
            tr.appendChild(tdRec);
            
            tableBody.appendChild(tr);
        });
    }

    function getSeveridadeClass(sev) {
        if (sev === 'CRITICAL') return 'pulse-red';
        if (sev === 'ERROR') return 'pulse-red';
        if (sev === 'WARNING') return 'pulse-yellow';
        return 'pulse-green';
    }

    function escapeHtml(unsafe) {
        return unsafe
             .replace(/&/g, "&amp;")
             .replace(/</g, "&lt;")
             .replace(/>/g, "&gt;")
             .replace(/"/g, "&quot;")
             .replace(/'/g, "&#039;");
    }
}
