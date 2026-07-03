# 🎬 Módulo: Metadados de Anime

[← Módulo Telemetria](10-modulo-telemetria.md) | [Mapa do Projeto →](12-modulo-mapa-projeto.md)

---

## Para que serve

Módulo **decorativo/informativo** — não afeta tradução, cache ou lore. Busca pôster, título, sinopse, ano, episódios e score de um anime a partir do caminho da pasta informada em qualquer painel, para que o operador confirme visualmente que está prestes a processar o anime certo antes de disparar uma operação em lote.

![Cartão de metadados exibido ao selecionar uma obra (exemplo: DanMachi Season 4)](../src/main/resources/static/img/screenshots/metadados-anime.webp)

---

## Fluxo de resolução

```mermaid
graph TD
    A["Caminho da pasta<br/>ex: E:/animes/[Sokudo] DanMachi/Season 05"] --> B["extrairNomeTermoBusca()<br/>remove tags de release, temporada, idioma"]
    B --> C{"Cache local em<br/>cache/metadata/&lt;slug&gt;.json?"}
    C -->|Existe| D["Retorna direto — sem chamar API externa"]
    C -->|Não existe| E{"tmdb.api-key<br/>configurada?"}
    E -->|Sim| F["TmdbApiClientAdapter<br/>/search/tv → /search/movie (pt-BR)"]
    E -->|Não| G["JikanApiClientAdapter<br/>api.jikan.moe (MyAnimeList, sem chave)"]
    F -->|Falhou/sem resultado| G
    F --> H["Salva em cache/metadata/&lt;slug&gt;.json"]
    G --> H
    H --> D
```

- **Normalização do nome:** remove tags de release (`[1080p]`, `(BD)`, `S01E01`, `ENG`, `PTBR` etc.) e extrai o segmento de pasta mais provável como nome do anime.
- **TMDB** é tentado primeiro (melhor cobertura de sinopse em português), só se `tmdb.api-key` estiver configurada — ver [Configuração](14-configuracao.md).
- **Jikan/MyAnimeList** é o fallback sempre disponível, sem necessidade de chave de API.

---

## Onde aparece na UI

Cada painel com um campo de "pasta de entrada" (`analise-entrada`, `traducao-entrada`, `correcao-entrada`, `revisao-entrada`, `cura-entrada-original`) tem um `.anime-meta-banner` associado. Ao digitar/selecionar um caminho, o frontend (`inicializarMetadadosDinamicos()` em `js/app.js`) chama `GET /api/metadata?caminho=...` e renderiza o banner (pôster, título, sinopse, score) acima do formulário. Se a busca falhar, o banner simplesmente fica oculto — nunca bloqueia a operação.

---

## Endpoint REST

### `GET /api/metadata?caminho=<pasta_ou_nome>`

```json
{
  "titulo": "DanMachi: Is It Wrong to Try to Pick Up Girls in a Dungeon?",
  "tituloIngles": "DanMachi",
  "tituloJapones": "ダンジョンに出会いを求めるのは間違っているだろうか",
  "posterUrl": "https://...",
  "ano": 2015,
  "episodios": 13,
  "score": 7.8,
  "sinopse": "...",
  "generos": ["Ação", "Aventura", "Fantasia"]
}
```

---

## Navegação

| Anterior | Próximo |
|----------|---------|
| [← Módulo Telemetria](10-modulo-telemetria.md) | [Mapa do Projeto →](12-modulo-mapa-projeto.md) |
