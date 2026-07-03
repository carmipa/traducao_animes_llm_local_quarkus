# 📦 Módulo: Remuxer

[← Cura de Tags](07-modulo-cura-tags.md) | [Contextos & Lore →](09-contextos-lore.md)

---

## Para que serve

Última etapa do pipeline: combina o **vídeo original** com a **legenda traduzida** (já revisada/curada) num novo arquivo `.mkv`, via `mkvmerge` (MKVToolNix), preservando todas as faixas de vídeo/áudio originais e adicionando a faixa de legenda PT-BR — sem re-encodar nada (remuxagem, não transcodificação: rápido e sem perda de qualidade).

---

## Pacote e classes principais

| Classe | Papel |
|--------|-------|
| `RemuxarLoteUseCase` (`application`) | Orquestra a fila de tarefas de remux do lote |
| `MapeadorMidiaService` | Pareia cada vídeo com sua legenda correspondente na pasta |
| `MkvmergeAdapter` (`infrastructure/adapters`) | Monta e executa o comando `mkvmerge` |

---

## Pareamento vídeo ↔ legenda

```mermaid
graph TD
    A["Pasta de vídeos + Pasta de legendas"] --> B{"Quantos vídeos<br/>e legendas na pasta?"}
    B -->|"1 vídeo + 1 legenda"| C["Pareamento por arquivo único<br/>(ex.: filme) — casa direto,<br/>mesmo com nomes de release diferentes"]
    B -->|"Vários de cada"| D["Pareamento por nome/código de episódio<br/>(ex.: S01E01, _01_, Track2)"]
    C --> E["Fila de remux"]
    D --> E
```

> ⚠️ **Ponto de atenção conhecido:** o fallback "1 vídeo + 1 legenda na pasta" pareia os dois **sem checar se vêm do mesmo release** — se a pasta tiver, por engano, um vídeo de uma fonte (ex.: encode AV1 de um grupo) e uma legenda extraída de outra fonte completamente diferente (ex.: BDRip AMZN de outro grupo), o pareamento "por arquivo único" ainda os casa, e o resultado final fica objetivamente dessincronizado — **não por bug de cálculo de tempo, mas por combinar fontes diferentes**. Sempre confira o [relatório de Análise de Mídia](03-modulo-analise-midia.md) do vídeo final antes de considerar o remux definitivo. Ver [Solução de Problemas](15-solucao-problemas.md#legenda-dessincronizada-desde-o-inicio) para o caso real que motivou este aviso.

---

## Sincronismo manual (offset)

O formulário do Remuxer aceita um campo opcional de **sincronismo manual em milissegundos**:

- Positivo → **atrasa** a legenda
- Negativo → **adianta** a legenda

Esse valor é passado como `--sync 0:<ms>` ao `mkvmerge`, que desloca **linearmente todos os timestamps** da faixa de legenda pelo valor informado.

> ⚠️ O offset é aplicado **igualmente a todos os itens da fila do lote** — não é por arquivo individual. Se o valor foi calculado/ajustado para um episódio específico e a mesma execução processa um lote com outros arquivos (ou um filme com timing diferente), todos recebem o mesmo deslocamento. Confira o campo antes de cada execução, especialmente ao misturar um filme com uma leva de episódios na mesma operação.

O [relatório de Análise de Mídia](03-modulo-analise-midia.md#o-que-é-auditado-por-faixa) já sugere o valor de offset em ms quando detecta um "atraso constante" — use esse número como ponto de partida.

---

## Fluxo de execução

```mermaid
sequenceDiagram
    actor Op as Operador
    participant UI as Painel Remuxer
    participant API as ApiController
    participant UC as RemuxarLoteUseCase
    participant Map as MapeadorMidiaService
    participant AD as MkvmergeAdapter
    participant MKV as mkvmerge (processo externo)

    Op->>UI: Pasta de vídeos + pasta de legendas + offset (ms, opcional)
    UI->>API: POST /api/remuxar {pathVideos, pathLegendas, sincronismoMs}
    API-->>UI: 200 "Remux iniciado" (job assíncrono)
    API->>Map: parear(videos, legendas)
    Map-->>API: fila de RemuxTarefa
    loop Para cada tarefa da fila
        API->>AD: executarRemux(tarefa, sincronismoMs)
        AD->>MKV: mkvmerge -o saida.mkv video.mkv --sync 0:Xms legenda.ass
        MKV-->>AD: novo .mkv (vídeo + áudio original + legenda PT-BR)
    end
```

---

## Endpoint REST

### `POST /api/remuxar`

```json
{
  "pathVideos": "C:/animes/Gundam-Narrative-NT",
  "pathLegendas": "C:/animes/Gundam-Narrative-NT/legendas-ptbr",
  "sincronismoMs": 0
}
```

| Campo | Obrigatório | Descrição |
|-------|:-----------:|-----------|
| `pathVideos` | ✅ | Pasta com os vídeos originais |
| `pathLegendas` | ✅ | Pasta com as legendas traduzidas finais |
| `sincronismoMs` | ⚪ | Offset em ms aplicado a **todo o lote** (positivo atrasa, negativo adianta) |

**Saída:** novos `.mkv` gravados na pasta configurada de saída do remux (padrão `mkv_final_ptbr/` dentro da pasta de vídeos).

---

## Navegação

| Anterior | Próximo |
|----------|---------|
| [← Cura de Tags](07-modulo-cura-tags.md) | [Contextos & Lore →](09-contextos-lore.md) |
