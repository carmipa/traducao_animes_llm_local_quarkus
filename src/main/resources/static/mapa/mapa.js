export function initMapa() {
    const btnGerar = document.getElementById('btn-gerar-mapa');
    const viewer = document.getElementById('viewer-mapa');

    if (btnGerar) {
        btnGerar.addEventListener('click', async () => {
            if (viewer) viewer.textContent = 'Mapeando estrutura do projeto. Por favor, aguarde...';

            try {
                const res = await fetch('/api/mapa', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' }
                });

                if (!res.ok) {
                    const erro = await res.text();
                    throw new Error(erro || 'Erro ao gerar o mapa');
                }

                const data = await res.json();
                if (viewer) {
                    viewer.textContent = data.conteudo || 'Nenhum mapa retornado.';
                }
            } catch (err) {
                if (viewer) viewer.textContent = `Erro ao gerar mapa: ${err.message}`;
            }
        });
    }
}
