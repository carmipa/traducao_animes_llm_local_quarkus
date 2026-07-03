<div align="center">

<img src="src/main/resources/static/img/kronos_logo.png" alt="KRONOS CORE Logo" width="160"/>

# KRONOS CORE

### Pipeline Industrial de Processamento & Tradução de Animes
**Tradução de legendas por IA rodando 100% local — sem nuvem, sem custo por token**

---

[![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/25/)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.37-4695EB?style=for-the-badge&logo=quarkus&logoColor=white)](https://quarkus.io/)
[![Gradle](https://img.shields.io/badge/Gradle-Wrapper-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org/)
[![LM Studio](https://img.shields.io/badge/LLM-LM_Studio_Local-8B5CF6?style=for-the-badge)](https://lmstudio.ai/)
[![MKVToolNix](https://img.shields.io/badge/MKVToolNix-Remux-3B82F6?style=for-the-badge)](https://mkvtoolnix.download/)
[![FFmpeg](https://img.shields.io/badge/FFmpeg-Analysis-007808?style=for-the-badge&logo=ffmpeg&logoColor=white)](https://ffmpeg.org/)

[![Repository](https://img.shields.io/badge/GitHub-carmipa%2Ftraducao__animes__llm__local__quarkus-181717?style=flat-square&logo=github&logoColor=white)](https://github.com/carmipa/traducao_animes_llm_local_quarkus)
[![Autor](https://img.shields.io/badge/Autor-Paulo_André_Carminati-0A66C2?style=flat-square&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/paulo-andr%C3%A9-carminati-47712340/)
[![GitHub](https://img.shields.io/badge/GitHub-carmipa-181717?style=flat-square&logo=github&logoColor=white)](https://github.com/carmipa?tab=repositories)

</div>

---

## O que é o KRONOS CORE?

O **KRONOS CORE** é uma plataforma de automação para **tradução industrial de legendas de anime**, cobrindo o pipeline completo do fã-sub: da mídia crua ao MKV final remuxado. Ele combina:

- 🔍 **Auditoria técnica de mídia** (ffprobe) com detecção automática de dessincronismo de legenda
- ✂️ **Extração em lote** de faixas de legenda (ASS/SRT/PGS) de MKV/MP4/qualquer contêiner comum
- 🌐 **Tradução por LLM 100% local** (LM Studio) com cache persistente e lore por anime (56+ contextos)
- 🩹 **Três fluxos de correção/revisão** (LLM, Google Translate, heurística de concordância PT-BR)
- 🧵 **Restauração estrutural de tags ASS** corrompidas por alucinação de IA (Aegisub/Kara Templater)
- 📦 **Remuxagem automatizada** com preservação total de qualidade original
- 📊 **Telemetria em tempo real** (SSE) de todas as etapas do pipeline

Tudo rodando sobre **Java 25 + Quarkus** com uma SPA própria (HTML/CSS/JS puro, sem framework de frontend), pensado para operação **desktop-first e 100% offline** — a única dependência de rede é opcional (metadados de anime via Jikan/TMDB).

![Painel Inicial do KRONOS CORE](src/main/resources/static/img/screenshots/painel-inicial.webp)

---

## Navegação da Documentação

> Clique em qualquer seção para ir à documentação detalhada.

[![Arquitetura](https://img.shields.io/badge/Docs-Arquitetura-3B82F6?style=flat-square&logo=readthedocs&logoColor=white)](docs/01-arquitetura.md)
[![Instalação](https://img.shields.io/badge/Docs-Instalação-10B981?style=flat-square&logo=bookstack&logoColor=white)](docs/02-instalacao.md)
[![API REST](https://img.shields.io/badge/Docs-API_REST-8B5CF6?style=flat-square&logo=swagger&logoColor=white)](docs/13-api-endpoints.md)
[![Configuração](https://img.shields.io/badge/Docs-Configuração-F59E0B?style=flat-square&logo=gnometerminal&logoColor=white)](docs/14-configuracao.md)
[![Troubleshooting](https://img.shields.io/badge/Docs-Solução_de_Problemas-F43F5E?style=flat-square&logo=githubactions&logoColor=white)](docs/15-solucao-problemas.md)

| # | Módulo | Descrição |
|---|--------|-----------|
| 📐 | [**Arquitetura**](docs/01-arquitetura.md) | Visão geral, diagramas de componentes e fluxos de dados |
| 🚀 | [**Instalação & Configuração**](docs/02-instalacao.md) | Pré-requisitos, setup local e primeiros passos |
| 🔍 | [**Análise de Mídia**](docs/03-modulo-analise-midia.md) | Auditoria ffprobe e detecção de dessincronismo de legenda |
| ✂️ | [**Extração de Legendas**](docs/04-modulo-extracao-legendas.md) | Extração em lote ASS/SRT/PGS via MKVToolNix/ffmpeg |
| 🌐 | [**Tradução Local (LLM)**](docs/05-modulo-traducao-llm.md) | Núcleo: LM Studio, cache, proteção de tags, contextos |
| 🩹 | [**Correção & Revisão**](docs/06-modulo-correcao-revisao.md) | Os 3 fluxos: LLM, Google Translate, concordância PT-BR |
| 🧵 | [**Cura de Tags**](docs/07-modulo-cura-tags.md) | Restauração estrutural de tags ASS/Kara Templater |
| 📖 | [**Revisão de Lore**](docs/16-modulo-revisao-lore.md) | Corrige nomes, locais e termos de lore comparando com o original em inglês |
| 📦 | [**Remuxer**](docs/08-modulo-remuxer.md) | Combina vídeo + legenda em MKV final |
| 🎭 | [**Contextos & Lore**](docs/09-contextos-lore.md) | Sistema de lore por anime — 56+ contextos cadastrados |
| 📊 | [**Telemetria**](docs/10-modulo-telemetria.md) | Rastreamento de operações e métricas de JVM em tempo real |
| 🎬 | [**Metadados de Anime**](docs/11-modulo-metadados-anime.md) | Integração Jikan/MAL e TMDB para pôster/sinopse na UI |
| 🗺️ | [**Mapa do Projeto**](docs/12-modulo-mapa-projeto.md) | Gerador automático do índice de código-fonte |
| 📋 | [**API REST — Referência**](docs/13-api-endpoints.md) | Todos os endpoints documentados com exemplos |
| ⚙️ | [**Configuração**](docs/14-configuracao.md) | Referência completa de `application.yml` |
| 🩺 | [**Solução de Problemas**](docs/15-solucao-problemas.md) | Diagnósticos reais: dessincronismo, LM Studio, SSE |

> A mesma navegação está disponível **dentro da aplicação**, no menu **📖 Documentação** da interface web.

---

## Início Rápido

### Pré-requisitos

| Ferramenta | Versão mínima |
|------------|---------------|
| Java (JDK) | 25 |
| Gradle | Incluído via Wrapper |
| FFmpeg / FFprobe | Qualquer build recente |
| MKVToolNix | Qualquer build recente |
| LM Studio | Com servidor local ativo |

### Executar em modo desenvolvimento

```shell
git clone <url-do-repositorio>
cd traducao_animes_llm_local_quarkus

./gradlew quarkusDev
```

> O servidor sobe em **`http://127.0.0.1:8080`** e o navegador abre automaticamente. Detalhes completos em [Instalação & Configuração](docs/02-instalacao.md).

---

## Arquitetura em 30 Segundos

```
┌──────────────────────────────────────────────────────────────────────┐
│                           KRONOS CORE                                 │
│                                                                        │
│  ┌──────────┐    ┌────────────────┐    ┌───────────────────────┐    │
│  │   SPA    │───▶│ ApiController  │───▶│  Use Cases (13 pacotes)│    │
│  │ (HTML/JS)│    │  REST + SSE    │    │  análise → extração →  │    │
│  └──────────┘    └───────┬────────┘    │  tradução → correção → │    │
│                          │              │  cura → remuxer        │    │
│              ┌───────────┼───────────┐  └───────────────────────┘    │
│              ▼           ▼           ▼                                │
│       ┌──────────┐ ┌──────────┐ ┌──────────┐                        │
│       │LM Studio │ │MKVToolNix│ │  FFmpeg  │                        │
│       │ (GPU/LOC)│ │ (remux)  │ │(análise) │                        │
│       └──────────┘ └──────────┘ └──────────┘                        │
└──────────────────────────────────────────────────────────────────────┘
```

> Diagrama completo com fluxo de dados e decisões de arquitetura em [docs/01-arquitetura.md](docs/01-arquitetura.md).

---

## Pipeline de Trabalho

```mermaid
graph LR
    A["📼 Vídeo"] --> B["🔍 Análise"]
    B --> C["✂️ Extração"]
    C --> D["🌐 Tradução"]
    D --> E["🩹 Correção/Revisão"]
    E --> F["🧵 Cura de Tags"]
    F --> G["📦 Remuxer"]
    G --> H["🎬 MKV Final"]
```

Cada etapa é **independente e re-executável** — rode só a etapa que precisar, sem repetir o pipeline inteiro. Detalhes em [Arquitetura — Pipeline Completo](docs/01-arquitetura.md#diagrama-de-fluxo--pipeline-completo-visão-de-negócio).

---

## Stack Tecnológica

```
Backend:    Java 25 + Quarkus 3.37 (compatibilidade Spring: DI, Web, Config)
Frontend:   HTML/CSS/JS puro (SPA sem build step), Server-Sent Events (SSE)
IA:         LM Studio (OpenAI-compatible local), qualquer modelo GGUF servido nele
Mídia:      FFmpeg/FFprobe (análise), MKVToolNix (extração + remux)
Metadados:  Jikan (MyAnimeList) + TMDB (opcional, com chave de API)
Build:      Gradle com Quarkus Plugin
```

---

## Estrutura do Projeto

```
traducao_animes_llm_local_quarkus/
├── src/
│   ├── main/
│   │   ├── java/org/traducao/projeto/
│   │   │   ├── analisadorMidia/       ← Auditoria ffprobe
│   │   │   ├── legendasExtracao/      ← Extração ASS/SRT/PGS
│   │   │   ├── traducao/              ← Núcleo: LLM, cache, contextos, ApiController
│   │   │   ├── raspagemCorrecao/      ← Correção via Google Translate
│   │   │   ├── raspagemRevisao/       ← Revisão de concordância PT-BR
│   │   │   ├── curatags/              ← Restauração de tags ASS
│   │   │   ├── remuxer/               ← Combina vídeo + legenda
│   │   │   ├── telemetria/            ← Rastreamento de operações
│   │   │   ├── mapaProjeto/           ← Gerador de mapa_projeto.md
│   │   │   ├── apiDadosAnime/         ← Metadados (Jikan/TMDB)
│   │   │   └── core/, config/         ← Utilitários e bootstrap compartilhados
│   │   └── resources/
│   │       ├── static/                ← SPA (HTML/CSS/JS por painel)
│   │       ├── application.yml        ← Configuração principal
│   │       └── application-local.yml  ← Chaves privadas (git-ignored)
│   └── test/
├── docs/                               ← Esta documentação
├── build.gradle
└── gradle.properties
```

---

## Navegação Interna (dentro do app)

A aplicação web tem 11 painéis navegáveis pela barra lateral — cada um mapeia a um módulo desta documentação:

`Início` · `Análise de Mídia` · `Extração` · `Tradução Local` · `Correção` · `Revisão` · `Cura de Legendas` · `Remuxer` · `Mapa do Projeto` · `Telemetria` · **`Documentação`**

O menu **Documentação** renderiza esta mesma pasta `docs/` dentro da própria aplicação (incluindo os diagramas Mermaid), sem precisar sair do app ou abrir o GitHub.

![Painel de Documentação dentro do app](src/main/resources/static/img/screenshots/documentacao.webp)

---

<div align="center">

**[⬆ Voltar ao topo](#kronos-core)**

[![Java](https://img.shields.io/badge/Java-25-ED8B00?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.37-4695EB?style=flat-square&logo=quarkus)](https://quarkus.io/)
[![LM Studio](https://img.shields.io/badge/LLM-Local-8B5CF6?style=flat-square)](https://lmstudio.ai/)

</div>
