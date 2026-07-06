# MAPA ESTRUTURAL DO PROJETO - TRACKER ANIMES
Gerado em: traducao_animes_llm_local_quarkus
Este documento serve como mapa de contexto para LLMs atualizarem a documentação oficial.
Memória viva e estado recente: veja **CEREBRO_IA.md** na raiz do repositório.
---

## 📁 Pasta: `.codex-run/`
*(Nenhum script Python ou Java nesta pasta)*

## 📁 Pasta: `.playwright-mcp/`
*(Nenhum script Python ou Java nesta pasta)*

## 📁 Pasta: `.vscode/`
*(Nenhum script Python ou Java nesta pasta)*

## 📁 Pasta: `downloads/`
*(Nenhum script Python ou Java nesta pasta)*

## 📁 Pasta: `gradle/`
*(Nenhum script Python ou Java nesta pasta)*

## 📁 Pasta: `logs/`
*(Nenhum script Python ou Java nesta pasta)*

## 📁 Pasta: `relatorios/`
*(Nenhum script Python ou Java nesta pasta)*

## 📁 Pasta: `src/`
### 📄 Arquivo: `src/main/java/org/traducao/projeto/analisadorMidia/application/AnalisarMidiaUseCase.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/analisadorMidia/domain/AnalisadorException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/analisadorMidia/domain/AudioInfo.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/analisadorMidia/domain/AuditoriaResultado.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/analisadorMidia/domain/ContainerInfo.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/analisadorMidia/domain/exceptions/AnaliseStreamException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/analisadorMidia/domain/LegendaInfo.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/analisadorMidia/domain/ResultadoAnaliseLote.java`
```text
Resultado de uma execução de auditoria sobre um lote de vídeos, incluindo o
caminho do relatório de texto efetivamente gravado em disco (individual,
se um único arquivo foi analisado, ou consolidado, se foram vários).
{@code relatorioPrincipal} é {@code null} se nada foi gravado (ex.: falha de IO).
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/analisadorMidia/domain/VideoInfo.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/analisadorMidia/infrastructure/adapters/FfprobeAdapter.java`
```text
Executa ffprobe no vídeo e obtém o JSON com as informações gerais e faixas.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/analisadorMidia/presentation/AnalisadorMidiaCLI.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/analisadorMidia/presentation/ui/ConsoleAnalisadorLogger.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/apiDadosAnime/application/ObterMetadataAnimeUseCase.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/apiDadosAnime/domain/exceptions/AnimeNaoEncontradoException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/apiDadosAnime/domain/exceptions/ApiDadosAnimeException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/apiDadosAnime/domain/model/AnimeMetadata.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/apiDadosAnime/infrastructure/adapters/JikanApiClientAdapter.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/apiDadosAnime/infrastructure/adapters/TmdbApiClientAdapter.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/apiDadosAnime/presentation/web/AnimeMetadataController.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/config/AppConfig.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/config/ExecucaoCli.java`
```text
Contrato para modos de execucao em linha de comando (substituto do CommandLineRunner do Spring Boot).
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/config/ModoExecucaoStartup.java`
```text
Dispara o modo CLI configurado em {@code app.modo}. No modo WEB nenhuma CLI e executada.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/core/exception/BasePipelineException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/core/exception/web/BasePipelineExceptionMapper.java`
```text
Converte qualquer exceção de domínio do pipeline (uma por pacote, todas
estendendo {@link BasePipelineException}) em uma resposta JSON estruturada
e rastreável, em vez de cada endpoint precisar capturar e formatar erro
manualmente. O {@code errorId} permite cruzar a resposta HTTP com a
entrada correspondente no log do servidor.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/core/execucao/FilaExecucaoPipeline.java`
```text
Fila única (single-thread) para todos os jobs pesados do pipeline —
tradução, correção, revisões (concordância/lore), análise, extração, remux.
<p>
Ter UMA fila compartilhada é requisito de corretude, não só de desempenho:
o contexto de tradução ativo ({@code GerenciadorContexto}) e o modelo LLM
configurado são estado global mutado no início de cada job. Quando cada
controller tinha seu próprio executor (ou rodava na thread HTTP), dois jobs
podiam rodar em paralelo e um trocava a lore/modelo no meio do outro — além
de disputarem a GPU do LM Studio, que atende uma inferência por vez.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/core/util/ProcessoExternoUtil.java`
```text
Executa processos externos (ffmpeg, ffprobe, mkvmerge, mkvextract) de forma segura:
drena stdout e stderr em threads separadas (evita o deadlock classico de ProcessBuilder,
em que o processo filho trava escrevendo em um pipe cujo buffer do SO enche enquanto o
pai ainda le o outro stream) e aplica um timeout que mata o processo (destroyForcibly)
caso ele nao termine a tempo, em vez de travar o pipeline indefinidamente.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/correcaoLegendas/application/CorretorTraducaoLlmService.java`
```text
Retorna a tradução corrigida via LLM apenas se a tradução atual estiver com
resíduo em inglês/preâmbulo (ValidadorTraducaoService) — evita chamar o LLM
para falas que já estão corretas.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/correcaoLegendas/application/CorrigirLegendasUseCase.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/correcaoLegendas/application/SanitizadorTagsService.java`
```text
LLM costuma alucinar chaves {texto} como marcação de pensamento, o que quebra a linha no Aegisub.
Início válido de bloco ASS: "\" (override), "=" (marcador do Kara Templater)
ou "*" (loop do Kara Templater, ex.: {*\c&H24249D&} — visto no Gundam 0083).
Tags de timing de karaoke ASS: \k, \K, \kf, \ko seguidas de duração (centissegundos).
Legado: LLM (ou versões antigas deste código) corrompiam a tag do Kara Templater
"{=X}" para "\N=X".
Formatação de tela (pos, cor, an8 etc.) sempre fica no prefixo {...} do início da linha.
Forçamos a tradução a ter exatamente o mesmo prefixo do original — inclusive quando o
original não tem prefixo nenhum, caso em que qualquer {...} que apareça na tradução é
alucinação do LLM e precisa ser descartado, não preservado.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/correcaoLegendas/domain/CorrecaoLegendasRelatorioJson.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/correcaoLegendas/domain/LogEventoCorrecaoLegendas.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/correcaoLegendas/domain/ResultadoCorrecaoLegendas.java`
```text
Resultado da correção: {@code curados} conta ARQUIVOS modificados;
{@code falasCuradas} e {@code corrigidosLlm} contam FALAS (linhas) — a
telemetria usa apenas contagens de falas para não misturar unidades.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/correcaoLegendas/infrastructure/CorrecaoLegendasLogPersistencia.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/correcaoLegendas/presentation/CorrecaoLegendasController.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/legendasExtracao/application/ExtrairLegendaUseCase.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/legendasExtracao/application/strategy/ExtratorAssStrategy.java`
```text
1. Tentar por palavras-chave
2. Tentar a última candidata (geralmente a faixa completa em ASS, a primeira é signs)
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/legendasExtracao/application/strategy/ExtratorPgsStrategy.java`
```text
Para PGS, geralmente pega a primeira encontrada ou a marcada como default
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/legendasExtracao/application/strategy/ExtratorSrtStrategy.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/legendasExtracao/application/strategy/ExtratorStrategy.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/legendasExtracao/domain/exceptions/FormatoLegendaInvalidoException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/legendasExtracao/domain/ExtratorException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/legendasExtracao/domain/FaixaLegenda.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/legendasExtracao/domain/FormatoLegenda.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/legendasExtracao/domain/ports/ExtratorVideoPort.java`
```text
Abstrai a ferramenta usada para identificar e extrair faixas de legenda de um
vídeo. Cada implementação é responsável por um conjunto de contêineres
(ex.: MKVToolNix para Matroska, ffmpeg para os demais formatos).
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/legendasExtracao/domain/RelatorioExtracao.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/legendasExtracao/infrastructure/adapters/FfmpegAdapter.java`
```text
Extrai legendas de contêineres que o MKVToolNix não lê (mkvextract só opera
sobre Matroska/WebM). Cobre MP4, MOV, AVI e afins via ffmpeg/ffprobe.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/legendasExtracao/infrastructure/adapters/MkvToolNixAdapter.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/legendasExtracao/infrastructure/config/ExtratorProperties.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/legendasExtracao/presentation/ExtratorCLI.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/legendasExtracao/presentation/ui/ConsoleExtratorLogger.java`
```text
Tag colorida em negrito (chama atenção), corpo da mensagem em peso normal
(mais fácil de ler em blocos de texto maiores) — INFO fica sem cor nenhuma.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/mapaProjeto/application/GeradorMapaProjetoUseCase.java`
```text
Lista e ordena pastas imediatas na raiz
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/mapaProjeto/application/MapeadorDiretorioUseCase.java`
```text
Cabeçalho Técnico
PARTE 1: CAMINHO ABSOLUTO COMPLETO NO SISTEMA LOCAL
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/mapaProjeto/domain/exceptions/MapaProjetoException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/mapaProjeto/presentation/MapaProjetoCLI.java`
```text
Determina a raiz a ser mapeada
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/raspagemCorrecao/application/CorrigirComGoogleUseCase.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/raspagemCorrecao/CorretorRaspagemCLI.java`
```text
CommandLineRunner que realiza a tradução das falas residuais pendentes em inglês
utilizando raspagem na API gratuita e sem chaves do Google Translate.
Ativado quando a propriedade app.modo é configurada como "RASPAGEM_CORRECAO".
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/raspagemCorrecao/domain/exceptions/RaspagemCorrecaoException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/raspagemCorrecao/infrastructure/GoogleTranslateScraper.java`
```text
Traduz texto via API pública do Google Translate (scraping), preservando
tags ASS mascaradas e quebras {@code \N}.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/raspagemRevisao/application/AuditorProblemasLegendaService.java`
```text
Agrega detecção de resíduo em inglês, falas não traduzidas e erros de
concordância PT-BR.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/raspagemRevisao/application/DetectorConcordanciaService.java`
```text
Heurísticas para calques de gênero do inglês: concordância nominal,
pronomes pessoais/objetos, tratamentos e predicados verbais.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/raspagemRevisao/application/ResultadoRevisaoLegendas.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/raspagemRevisao/application/RevisarCacheUseCase.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/raspagemRevisao/application/RevisarLegendasUseCase.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/raspagemRevisao/domain/exceptions/RaspagemRevisaoException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/raspagemRevisao/domain/ResultadoDeteccaoConcordancia.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/raspagemRevisao/RevisorLegendasCLI.java`
```text
Revisa arquivos .ass/.ssa já traduzidos, detecta resíduos em inglês e erros
de concordância, e corrige via Google Translate.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/raspagemRevisao/RevisorRaspagemCLI.java`
```text
Revisa falas já traduzidas no cache, corrigindo concordância de gênero,
pronomes e adjetivos — erros comuns quando o LLM traduz literalmente do inglês.
Ativado quando {@code app.modo=RASPAGEM_REVISAO}.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/remuxer/application/MapeadorMidiaService.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/remuxer/application/RemuxarLoteUseCase.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/remuxer/domain/MkvToolNixNaoEncontradoException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/remuxer/domain/RelatorioRemux.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/remuxer/domain/RemuxerException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/remuxer/domain/RemuxTarefa.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/remuxer/infrastructure/adapters/MkvmergeAdapter.java`
```text
Tentar os caminhos padrões do Windows
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/remuxer/infrastructure/config/RemuxerProperties.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/remuxer/presentation/RemuxerCLI.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/remuxer/presentation/ui/ConsoleRemuxerLogger.java`
```text
Tag colorida em negrito (chama atenção), corpo da mensagem em peso normal
(mais fácil de ler em blocos de texto maiores) — INFO/DEBUG ficam sem cor.
Exemplo: [10:20:30] [INFO   ] Mensagem...
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/renomearArquivos/application/RenomeadorUseCase.java`
```text
Regex para pegar o episódio de trackers.
Ex: "[SubsPlease] Nome Anime - 01 (1080p).mkv" -> 01
Ex: "[DB]86_-_01_(Dual Audio_10bit_BD1080p_x265)_PTBR.mkv" -> 01
Ex: "Anime - 02.mkv" -> 02
Ex: "Anime Ep 03.mkv" -> 03
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/renomearArquivos/domain/OperacaoRenomeacao.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/renomearArquivos/presentation/web/RenomearArquivosController.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/renomearArquivos/presentation/web/RenomearArquivosRequest.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/application/DetectorTermosLoreService.java`
```text
Heuristica leve para priorizar falas com possivel erro de lore/terminologia
antes de chamar o LLM (nomes em ingles remanescentes, grafias suspeitas, etc.).
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/application/GerenciadorPromptRevisaoLore.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/application/PromptRevisaoLore.java`
```text
Monta os prompts de sistema e usuario para revisao de terminologia/lore
(nomes proprios, locais, faccoes, mechas) com base na lore da obra ativa.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/application/RevisarLoreUseCase.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/contexto/ContextoRevisaoLore86.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/contexto/ContextoRevisaoLoreDanMachi.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/contexto/ContextoRevisaoLoreDanMachiS4.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/contexto/ContextoRevisaoLoreDanMachiS5.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/contexto/ContextoRevisaoLoreGundam0080.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/contexto/ContextoRevisaoLoreGundam0083.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/contexto/ContextoRevisaoLoreGundam08thMSTeam.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/contexto/ContextoRevisaoLoreGundamCCA.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/contexto/ContextoRevisaoLoreGundamNT.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/domain/EntradaAuditoriaRevisaoLore.java`
```text
Registro granular, append-only, de cada fala enviada ao LLM na revisão de lore.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/domain/exceptions/RevisaoLoreException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/domain/LogEventoRevisaoLore.java`
```text
Entrada estruturada do log de sessao da revisao de lore (serializavel em JSON).
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/domain/ports/ProvedorPromptRevisaoLore.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/domain/ResultadoDeteccaoLore.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/domain/ResultadoRevisaoLore.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/domain/RevisaoLoreRelatorioJson.java`
```text
Relatorio completo da revisao de lore em JSON: telemetria, metricas, contexto e log da sessao.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/infrastructure/RevisaoLoreAuditoriaCache.java`
```text
Cache append-only para mineração posterior das decisões da revisão de lore.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/infrastructure/RevisaoLoreLogPersistencia.java`
```text
Persiste relatorio e log de sessao da revisao de lore exclusivamente em JSON.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/revisaoLore/presentation/RevisaoLoreController.java`
```text
Fila única compartilhada do pipeline: impede que a revisão de lore rode
em paralelo com uma tradução/correção e troque o contexto LLM global no
meio do outro job (ver FilaExecucaoPipeline).
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/sistema/application/EncerrarAplicacaoUseCase.java`
```text
Encerra a aplicação de forma ordenada a partir do botão "Sair" da UI.
<p>
Sequência: sinaliza parada cooperativa da fila do pipeline (o job em
execução encerra no próximo ponto seguro, preservando cache e arquivos já
concluídos), espera um curto período para a resposta HTTP chegar ao
navegador e então derruba o Quarkus. Se o shutdown normal não terminar o
processo (ex.: modo dev segura a JVM viva), um fallback força a saída.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/sistema/presentation/SistemaController.java`
```text
Endpoints de controle do processo da aplicação (menu "Sair" da UI).
Operações de trabalho do pipeline ficam nos controllers de cada módulo;
aqui entra apenas o ciclo de vida do servidor em si.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/telemetria/LlmTelemetria.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/telemetria/MidiaTelemetria.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/telemetria/OperacaoHistorico.java`
```text
Uma linha da tabela de histórico de operações exibida no painel de Telemetria.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/telemetria/OperacaoTelemetria.java`
```text
Registro persistido de operações do pipeline que não passam pelo LLM de tradução
(revisão de legendas, correção Google, limpeza de cache, etc.).
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/telemetria/RevisaoLoreTelemetriaResumo.java`
```text
Métricas agregadas das sessões de Revisão de Lore para o painel de Telemetria.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/telemetria/TelemetriaResumo.java`
```text
Resumo serializável da telemetria acumulada na sessão atual do servidor,
consumido pelo painel "Telemetria" da interface web.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/telemetria/TelemetriaService.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducaoCorrige/application/LimparCacheUseCase.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducaoCorrige/CorretorCache.java`
```text
Programa Utilitário que realiza a limpeza seletiva do cache de tradução.
Remove traduções que falharam e foram salvas com o texto original em inglês (fallbacks),
permitindo que sejam reprocessadas com a nova lógica e prompts corrigidos.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducaoCorrige/CorretorCacheCLI.java`
```text
CommandLineRunner que realiza a limpeza do cache de tradução integrado ao fluxo do Spring.
Ativado quando a propriedade app.modo é configurada como "CORRIGIR_CACHE".
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducaoCorrige/domain/exceptions/CorretorCacheException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/Application.java`
```text
Utilitarios de inicializacao compartilhados entre modos CLI.
O Quarkus e o container principal; nao ha {@code SpringApplication.run} aqui.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/application/DetectorEfeitoKaraokeService.java`
```text
Reconhece eventos que são efeito de karaokê/música, e não fala de diálogo.
Cobre as duas formas em que o karaokê aparece nos arquivos .ass:
<ul>
<li>Karaokê "cru": tags de timing {@code \k}, {@code \kf}, {@code \ko}
por sílaba, como sai do fansub antes de aplicar template.</li>
<li>Saída do Kara Templater do Aegisub: as tags {@code \k} são consumidas
na aplicação do template e viram uma linha por sílaba/letra com
transformações animadas ({@code \t(...)}, {@code \frx}, {@code \fad},
{@code \pos}) e quase nenhum texto visível.</li>
</ul>
Regra única compartilhada pelos módulos de tradução, revisão e correção —
nenhum deles deve mexer em música; isso é responsabilidade do módulo de
karaokê.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/application/DetectorTraducaoIdenticaService.java`
```text
Decide se uma fala pode legitimamente permanecer idêntica ao original (nomes
próprios, números, siglas, termos de lore) ou se a igualdade é sinal de que o
LLM simplesmente devolveu a fala sem traduzir.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/application/ProcessarArquivoUseCase.java`
```text
Orquestra a tradução de um único arquivo de legenda: le -> reaproveita o
cache existente -> traduz só o que falta (deduplicando falas repetidas) ->
valida -> escreve a legenda final em PT-BR -> grava/atualiza o cache.
<p>
Correções manuais feitas pelo usuário no JSON de cache são respeitadas na
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/application/ProcessarEpisodioUseCase.java`
```text
Quantas tentativas extras (alem da primeira) sao feitas numa fala isolada
(lote de tamanho 1) antes de desistir e manter o texto original sem traducao.
Temperatura por tentativa numa fala isolada: null = a configurada.
Repetir a mesma requisicao com a mesma temperatura tende a reproduzir a
mesma alucinacao; subir a temperatura muda a amostragem e da chance real
de recuperacao antes de desistir da fala.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/application/ValidadorTraducaoService.java`
```text
Regras robustas importadas do pipeline Python, ampliadas após observar em
produção o Mistral Nemo deixar fragmentos como "exactly the same" sem
traduzir mesmo traduzindo o resto da fala corretamente.

UNICODE_CHARACTER_CLASS e necessario aqui: sem ela, \b no Java so reconhece
[a-zA-Z0-9_] como caractere de palavra, entao letras acentuadas (ç, ã, é...)
contam como "fronteira", e palavras em portugues como "força" ou "esforço"
batem com "\bfor\b" e disparam falso positivo de "resíduo em inglês".
Contrações inglesas: resíduo inequívoco (nenhuma colide com PT-BR) que a
lista de palavras soltas não pega — caso real do 86: "Se você terminou sua
missão, it's seu dever me dar um relatório." passava sem disparar nada.
Aceita apóstrofo ASCII (') e tipográfico (’).
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/ContextoPrompt.java`
```text
Cada ContextoXxx monta seu PROMPT uma unica vez (campo static final), na
inicializacao da classe; este mapa guarda a lore "crua" por traz de cada
prompt completo para que outros usos (ex.: revisao de concordancia) nao
precisem reenviar o prompt de traducao inteiro - que ja inclui lore +
RegrasConcordanciaPtBr.BLOCO_TRADUCAO + regras de saida - como se fosse
so a lore, o que estourava o contexto do LLM (ver MistralClientAdapter).
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/danmachi/ContextoDanMachi.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/danmachi/ContextoDanMachiOrion.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/danmachi/ContextoDanMachiS1.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/danmachi/ContextoDanMachiS2.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/danmachi/ContextoDanMachiS3.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/danmachi/ContextoDanMachiS4.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/danmachi/ContextoDanMachiS5.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/danmachi/ContextoDanMachiSwordOratoria.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/eightsix/Contexto86.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/evangelion/ContextoEvangelion111.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/evangelion/ContextoEvangelion222.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/evangelion/ContextoEvangelion3010.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/evangelion/ContextoEvangelion333.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/evangelion/ContextoEvangelionTV.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/guiltycrown/ContextoGuiltyCrown.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/gundam/chars/ContextoCharsCounterattack.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/gundam/ContextoGundam0079.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/gundam/ContextoGundamF91.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/gundam/ContextoGundamHathaway.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/gundam/ContextoGundamNT.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/gundam/ContextoGundamOrigin.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/gundam/ContextoGundamSEED.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/gundam/ContextoGundamSEEDAstray.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/gundam/ContextoGundamSEEDDestiny.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/gundam/ContextoGundamSEEDFreedom.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/gundam/ContextoGundamSEEDStargazer.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/gundam/ContextoGundamUnicorn.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/gundam/ContextoGundamVictory.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/gundam/msteam/ContextoGundam08thMSTeam.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/gundam/reconguista/ContextoGundamReconguista.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/gundam/stardust/ContextoGundam0083.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/gundam/warInpocket/ContextoWarInPocket.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/gundam/zeta/ContextoGundamZeta.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/gundam/zz/ContextoGundamZZ.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/macross/ContextoMacross2.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/macross/ContextoMacross7.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/macross/ContextoMacross7Encore.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/macross/ContextoMacross7Filme.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/macross/ContextoMacross7Filmes.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/macross/ContextoMacrossAnime.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/macross/ContextoMacrossDelta.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/macross/ContextoMacrossDeltaFilme1.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/macross/ContextoMacrossDeltaFilme2.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/macross/ContextoMacrossDeltaFilmes.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/macross/ContextoMacrossDynamite7.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/macross/ContextoMacrossDYRL.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/macross/ContextoMacrossFilme1.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/macross/ContextoMacrossFilme2.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/macross/ContextoMacrossFrontier.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/macross/ContextoMacrossFrontierFilme1.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/macross/ContextoMacrossFrontierFilme2.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/macross/ContextoMacrossFrontierFilmes.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/macross/ContextoMacrossPlus.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/macross/ContextoMacrossZero.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/RegrasConcordanciaPtBr.java`
```text
Regras de concordância de gênero, pronomes, tratamentos e verbos aplicáveis a
qualquer obra — o inglês não marca gênero em adjetivos/participios e usa
"you" genérico, o que leva o LLM a masculinizar tudo.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/sidonia/ContextoKnightsOfSidonia.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/contexto/sidonia/ContextoSidoniaFilme.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/domain/exceptions/AlucinacaoDetectadaException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/domain/exceptions/ArquivoLegendaException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/domain/exceptions/ContextoNaoEncontradoException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/domain/exceptions/DivergenciaLinhasException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/domain/exceptions/LlmFalhaComunicacaoException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/domain/exceptions/LmStudioOfflineException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/domain/exceptions/RespostaLlmVaziaException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/domain/exceptions/TraducaoParcialException.java`
```text
Construtor usado pela camada do Episódio (nível de Lotes)
Construtor usado pela camada de Arquivo (nível de Falas Mascaradas)
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/domain/exceptions/TradutorException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/domain/legenda/DocumentoLegenda.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/domain/legenda/EventoLegenda.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/domain/Lote.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/domain/ports/MistralPort.java`
```text
Variante com temperatura explícita, usada nas retentativas de uma fala
isolada: repetir a MESMA requisição com a mesma temperatura tende a
reproduzir a mesma alucinação; subir a temperatura muda a amostragem e
dá chance real de recuperação. {@code null} usa a temperatura configurada.
Verifica, antes de iniciar a tradução, se o servidor LLM local está
online e se o modelo configurado está efetivamente carregado em
memória — evita descobrir isso só depois de várias tentativas/timeouts
já no meio da tradução do primeiro episódio.
Revisa uma fala já traduzida, corrigindo concordância de gênero/pronomes.
Retorna vazio se o LLM falhar ou a resposta for inválida.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/domain/ports/ProvedorContexto.java`
```text
Retorna o ID único para seleção via UI.
Retorna o nome amigável para exibição no combo box da UI.
Retorna o prompt de sistema completo para o LLM, com regras gerais e lore especifico da midia.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/domain/StatusLlm.java`
```text
Resultado da checagem de disponibilidade do servidor LLM local (ex: LM Studio)
feita no início da execução, antes de começar a traduzir qualquer episódio.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/domain/TraducaoLote.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/infrastructure/adapters/MistralClientAdapter.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/infrastructure/cache/CacheTraducaoService.java`
```text
Persiste, por arquivo de legenda, o par (texto original em ingles -> texto
traduzido) em JSON. Serve a dois propositos: (1) permitir que o usuario
revise/corrija falhas de traducao manualmente editando o JSON e (2) evitar
chamar o LLM de novo para falas ja traduzidas em uma execucao anterior -
uma correcao manual no cache e respeitada na proxima execucao.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/infrastructure/cache/EntradaCache.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/infrastructure/config/LlmProperties.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/infrastructure/config/RestClientConfig.java`
```text
Beans de agregacao e suporte para injecao CDI/Quarkus.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/infrastructure/config/TradutorProperties.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/infrastructure/contexto/GerenciadorContexto.java`
```text
Mutado pela thread única do executor de background (ApiController) e lido
pela mesma thread ao montar o prompt do LLM (MistralClientAdapter). O
volatile aqui é uma garantia defensiva de visibilidade, não uma alegação
de que múltiplas threads concorrem por este campo.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/infrastructure/dtos/RecordsMistral.java`
```text
Shape da API estendida da LM Studio ({@code /api/v0/models}, fora do
prefixo {@code /v1}), que — diferente do endpoint OpenAI-compatible
{@code /v1/models} — informa o campo {@code state} ("loaded" /
"not-loaded"), permitindo saber com certeza qual modelo está de fato
carregado em memória.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/infrastructure/http/JsonHttpClient.java`
```text
Cliente HTTP JSON baseado em {@link HttpClient} do JDK (sem Spring RestClient).
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/infrastructure/legenda/EscritorLegendaAss.java`
```text
Reconstroi o arquivo .ass a partir do {@link DocumentoLegenda}, repetindo o
cabecalho original e as linhas nao traduziveis byte a byte, e so trocando o
campo Text dos eventos Dialogue pela versao traduzida.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/infrastructure/legenda/LeitorLegendaAss.java`
```text
Le arquivos .ass/.ssa preservando byte a byte tudo que nao for o campo Text
dos eventos Dialogue (estilos, timestamps, secoes de metadados). So o campo
Text e exposto para traducao; o resto e reconstruido identico pelo
{@link EscritorLegendaAss}.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/infrastructure/legenda/MascaradorTags.java`
```text
Isola tags de formatação ASS/SSA (ex: {\i1}, {\pos(...)}) e códigos de quebra
(\N, \n, \h) do texto antes de enviar ao LLM, trocando-os por marcadores
[[TAGn]] que o modelo é instruído a preservar literalmente. Sem isso o LLM
tende a "traduzir" ou descartar as tags, corrompendo a legenda renderizada.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/presentation/TradutorCLI.java`
```text
Ponto de entrada da CLI: varre a pasta de entrada por arquivos .ass/.ssa
e traduz cada um sequencialmente.
<p>
Se {@code tradutor.diretorio-entrada} estiver vazio, o {@link Application#main}
pede os caminhos via {@link ConsoleEntrada}
antes do Spring subir.
<p>
Arquivos são processados um por vez de propósito: todos compartilham o
mesmo LLM local (GPU única). Lotes dentro de cada episódio também são
sequenciais (ver {@code ProcessarEpisodioUseCase}).
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/presentation/ui/AnsiCores.java`
```text
Cores ANSI compartilhadas entre o prompt interativo e o {@link ConsoleUILogger}.
Usar apenas caracteres ASCII nos textos do prompt evita problemas de encoding
no console do Windows (cp1252 vs UTF-8).
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/presentation/ui/ConsoleEntrada.java`
```text
Numeração segue a ordem natural do pipeline: primeiro auditar a
mídia, depois extrair/traduzir/corrigir a legenda e por fim remuxar.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/presentation/ui/ConsoleUILogger.java`
```text
Wrapper thread-safe em torno da barra de progresso (estilo tqdm). Todo
acesso a {@code pb} e sincronizado porque mensagens podem chegar
durante a tradução de um episódio.
<p>
O console e efêmero (a barra de progresso sobrescreve linhas antigas), por
isso toda mensagem também é espelhada no logger SLF4J, que persiste em
arquivo (ver {@code logging.file.name}) e sobrevive para análise posterior.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/presentation/ui/PastasExecucao.java`
```text
Pastas efetivas da execução atual. Preenchidas pelo {@code TradutorCLI} a
partir do diálogo Swing ou das propriedades/linha de comando.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/presentation/web/ApiController.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/presentation/web/BrowserLauncher.java`
```text
Abre o navegador apos a inicializacao do Quarkus quando {@code app.modo=WEB}.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/presentation/web/ConsoleRedirector.java`
```text
Interceptador global de System.out.
Redireciona tudo que é impresso no console padrão para o LogStreamService (SSE)
sem deixar de imprimir no console físico (terminal do CMD/PowerShell original).
<p>
No Spring Boot este bean era instanciado eagerly (singleton), e o redirecionamento
acontecia no construtor. No Quarkus/CDI (ARC) beans normais são lazy: como nada
injeta {@code ConsoleRedirector}, o bean nunca era construído e o redirecionamento
nunca era ativado (o console web parava de receber logs). O fix é o mesmo padrão
já usado por {@link BrowserLauncher} no mesmo pacote: reagir a {@link StartupEvent}
força a criação do bean na subida do Quarkus e também o protege da remoção de
beans não-usados em build-time (beans com método {@code @Observes} nunca são
removidos).
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/presentation/web/DialogoArquivoController.java`
```text
Usa OpenFileDialog (UI moderna do Explorer) em vez de FolderBrowserDialog/Shell.Application,
cuja janela de seleção de pasta ainda usa a interface antiga (estilo Windows 95).
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/presentation/web/DocumentacaoController.java`
```text
Serve o conteúdo bruto das páginas de documentação (pasta {@code docs/} na
raiz do projeto) para o painel "Documentação" da SPA, que renderiza o
markdown no navegador (ver static/documentacao/documentacao.js). O README
raiz é o índice canônico no GitHub; este endpoint espelha a mesma pasta
docs/ dentro do próprio app, sem precisar sair dele.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/presentation/web/LogStreamResource.java`
```text
Endpoint SSE nativo do Quarkus (substitui SseEmitter do Spring MVC).
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/presentation/web/LogStreamService.java`
```text
Gerencia conexoes SSE (JAX-RS) e despacha logs em tempo real para clientes web.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/traducao/presentation/web/TelemetriaStreamResource.java`
```text
Endpoint Server-Sent Events (SSE) reativo para streaming da telemetria da KRONOS em tempo real.
Rota mapeada especificamente para evitar colisões com o Controller Spring.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/trocaTipoLegenda/application/AuditoriaFontesService.java`
```text
Mapeamento de fontes vietnamitas/ANSI problemáticas para Arial como padrão seguro.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/trocaTipoLegenda/application/TrocaTipoLegendaUseCase.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/trocaTipoLegenda/domain/AuditoriaFonteInfo.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/trocaTipoLegenda/domain/AuditoriaLegendaResultado.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/trocaTipoLegenda/domain/EntradaAuditoriaTrocaFonte.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/trocaTipoLegenda/domain/exceptions/TrocaTipoLegendaException.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/trocaTipoLegenda/domain/ResultadoGeralAuditoria.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/trocaTipoLegenda/domain/ResultadoTrocaFonte.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/main/java/org/traducao/projeto/trocaTipoLegenda/infrastructure/TrocaTipoLegendaAuditoriaCache.java`
```text
Cache append-only para gravação de auditoria histórica e granular de cada alteração de fonte aplicada.
```

### 📄 Arquivo: `src/main/java/org/traducao/projeto/trocaTipoLegenda/presentation/TrocaTipoLegendaController.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/test/java/org/traducao/projeto/ApiControllerTest.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/test/java/org/traducao/projeto/apiDadosAnime/application/ObterMetadataAnimeUseCaseTest.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/test/java/org/traducao/projeto/ApiEndpointsTest.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/test/java/org/traducao/projeto/core/exception/BasePipelineExceptionTest.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/test/java/org/traducao/projeto/correcaoLegendas/application/CorrigirLegendasUseCaseTest.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/test/java/org/traducao/projeto/mapaProjeto/application/GeradorMapaProjetoUseCaseTest.java`
```text
{@code Files.list} (usado por {@code executar}, diferente de
{@code Files.walk} usado em outros use cases deste projeto) lança
{@code NotDirectoryException} quando o caminho informado não é um
diretório — e, ao contrário dos demais use cases, {@code executar} aqui
não tem nenhuma checagem prévia que intercepte esse caso. Isso o torna o
único, entre as lacunas de exceção corrigidas nesta auditoria, em que a
falha real é reproduzível de forma determinística e portátil num teste.
```

### 📄 Arquivo: `src/test/java/org/traducao/projeto/remuxer/application/MapeadorMidiaServiceTest.java`
```text
Criar arquivos de vídeo MKV com padrão "EpsXX" (como nos arquivos de 86 do usuário)
Criar arquivos de legenda ASS com padrão "_-_XX" e colchetes
```

### 📄 Arquivo: `src/test/java/org/traducao/projeto/renomearArquivos/application/RenomeadorUseCaseTest.java`
```text
Ignora
```

### 📄 Arquivo: `src/test/java/org/traducao/projeto/revisaoLore/application/DetectorTermosLoreServiceTest.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/test/java/org/traducao/projeto/revisaoLore/infrastructure/RevisaoLoreAuditoriaCacheTest.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/test/java/org/traducao/projeto/telemetria/TelemetriaServiceRevisaoLoreTest.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/test/java/org/traducao/projeto/traducao/application/DetectorEfeitoKaraokeServiceTest.java`
```text
Linha real que escapou da revisão: letra "I" afogada em transformações.
Linha com \pos e fscx/fscy onde o texto visível é curto em relação às tags.
```

### 📄 Arquivo: `src/test/java/org/traducao/projeto/traducao/application/ValidadorTraducaoServiceTest.java`
```text
Caso real (Gundam Narrative): LLM rotulou a resposta em vez de só traduzir.
Caso real (G-Reconguista): marcador do pipeline Python antigo na legenda final.
Caso real (86): linha metade PT, metade EN.
```

### 📄 Arquivo: `src/test/java/org/traducao/projeto/traducao/infrastructure/legenda/EscritorLegendaAssTest.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/test/java/org/traducao/projeto/traducao/presentation/web/ConsoleRedirectorTest.java`
```text
Teste de regressão para o bug pós-migração Spring Boot -> Quarkus: o
console web parou de exibir logs de sucesso/alerta porque
{@link ConsoleRedirector} (um bean cujo construtor chamava
{@code System.setOut}) nunca era instanciado pelo CDI/ARC, já que nada o
injetava em lugar nenhum. Sem o redirecionamento ativo, nada que os use
cases imprimem com {@code System.out.println} chega ao
{@code LogStreamService} (SSE) nem ao espelho em arquivo.
<p>
Este teste falha sem o fix (em {@code @Observes StartupEvent}) e passa com
ele, pois depende exclusivamente do bean ter sido ativado automaticamente
na subida do Quarkus — nenhuma injeção explícita de
{@code ConsoleRedirector} é feita aqui.
```

### 📄 Arquivo: `src/test/java/org/traducao/projeto/traducao/presentation/web/LogStreamServiceTest.java`
```text
Sem nenhum SSE client conectado, {@code publicarLog} ainda deve persistir a
linha em {@code logs/console-web.log} (espelho em disco usado por
{@link ConsoleRedirector} e pelos consoles web). Isso é o que prova que o
pipeline de publicação/persistência em arquivo funciona independente de
haver navegador conectado via SSE.
```

### 📄 Arquivo: `src/test/java/org/traducao/projeto/trocaTipoLegenda/application/AuditoriaFontesServiceTest.java`
```text
Default: Arial (Unicode Seguro)
```

### 📄 Arquivo: `src/test/java/org/traducao/projeto/trocaTipoLegenda/application/TrocaTipoLegendaUseCaseTest.java`
*(Sem docstring ou cabeçalho explicativo)*

### 📄 Arquivo: `src/test/java/org/traducao/projeto/WebInterfaceTest.java`
*(Sem docstring ou cabeçalho explicativo)*

---
