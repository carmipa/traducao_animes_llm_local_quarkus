================================================================================
 MAPA ESTRUTURAL DO PROJETO - TRACKER ANIMES
================================================================================
 Raiz do repositorio      : traducao_animes_llm_local_quarkus
 Pastas mapeadas          : 228
 Arquivos (na arvore)     : 417
 Arquivos-fonte indexados : 316  (.java: 316 | .py: 0)
 Memoria viva do projeto  : CEREBRO_IA.md (na raiz do repositorio)

 Objetivo: mapa de contexto para LLMs navegarem os diretorios e
 atualizarem a documentacao oficial. Geracao estatica e automatica.
================================================================================

--------------------------------------------------------------------------------
 1. ARVORE DE DIRETORIOS
--------------------------------------------------------------------------------
traducao_animes_llm_local_quarkus/
├── .codex/
│   └── config.toml
├── .github/
│   └── workflows/
│       └── gradle-ci.yml
├── .vscode/
│   └── settings.json
├── downloads/
│   └── plano-mapas-saas.html
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── logs/
│   ├── renomear-arquivos/
│   │   └── undo/
│   ├── console-web.log
│   └── telemetria_compartilhada.json
├── relatorios/
│   └── junit-4832848609591537337/
│       ├── auditoria_conteudo_20260713_114719.json
│       └── telemetria_compartilhada.json
├── src/
│   ├── main/
│   │   ├── docker/
│   │   │   ├── Dockerfile.jvm
│   │   │   ├── Dockerfile.legacy-jar
│   │   │   ├── Dockerfile.native
│   │   │   └── Dockerfile.native-micro
│   │   ├── java/
│   │   │   └── org/
│   │   │       └── traducao/
│   │   │           └── projeto/
│   │   │               ├── analisadorMidia/
│   │   │               │   ├── application/
│   │   │               │   │   └── AnalisarMidiaUseCase.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── exceptions/
│   │   │               │   │   │   └── AnaliseStreamException.java
│   │   │               │   │   ├── AnalisadorException.java
│   │   │               │   │   ├── AudioInfo.java
│   │   │               │   │   ├── AuditoriaResultado.java
│   │   │               │   │   ├── ContainerInfo.java
│   │   │               │   │   ├── LegendaInfo.java
│   │   │               │   │   ├── ResultadoAnaliseLote.java
│   │   │               │   │   └── VideoInfo.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   └── adapters/
│   │   │               │   │       └── FfprobeAdapter.java
│   │   │               │   └── presentation/
│   │   │               │       ├── ui/
│   │   │               │       │   └── ConsoleAnalisadorLogger.java
│   │   │               │       └── AnalisadorMidiaCLI.java
│   │   │               ├── apiDadosAnime/
│   │   │               │   ├── application/
│   │   │               │   │   └── ObterMetadataAnimeUseCase.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── exceptions/
│   │   │               │   │   │   ├── AnimeNaoEncontradoException.java
│   │   │               │   │   │   └── ApiDadosAnimeException.java
│   │   │               │   │   └── model/
│   │   │               │   │       └── AnimeMetadata.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   └── adapters/
│   │   │               │   │       ├── JikanApiClientAdapter.java
│   │   │               │   │       └── TmdbApiClientAdapter.java
│   │   │               │   └── presentation/
│   │   │               │       └── web/
│   │   │               │           └── AnimeMetadataController.java
│   │   │               ├── auditorConteudoLegendas/
│   │   │               │   ├── application/
│   │   │               │   │   ├── regras/
│   │   │               │   │   │   ├── RegraAlucinacaoQuebraLinha.java
│   │   │               │   │   │   ├── RegraDanoKaraoke.java
│   │   │               │   │   │   ├── RegraEfeitoVazado.java
│   │   │               │   │   │   ├── RegraMetadadosAss.java
│   │   │               │   │   │   └── RegraSincroniaEstilos.java
│   │   │               │   │   ├── AuditorConteudoUseCase.java
│   │   │               │   │   └── TelemetriaAuditoriaService.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── AnomaliaConteudo.java
│   │   │               │   │   ├── AuditoriaConteudoRelatorioJson.java
│   │   │               │   │   ├── AuditoriaException.java
│   │   │               │   │   ├── RegraAuditoriaConteudo.java
│   │   │               │   │   └── RelatorioAuditoriaConteudo.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   └── AuditoriaConteudoPersistencia.java
│   │   │               │   └── presentation/
│   │   │               │       └── AuditorConteudoController.java
│   │   │               ├── config/
│   │   │               │   ├── AppConfig.java
│   │   │               │   ├── ExecucaoCli.java
│   │   │               │   └── ModoExecucaoStartup.java
│   │   │               ├── core/
│   │   │               │   ├── exception/
│   │   │               │   │   ├── web/
│   │   │               │   │   │   └── BasePipelineExceptionMapper.java
│   │   │               │   │   └── BasePipelineException.java
│   │   │               │   ├── execucao/
│   │   │               │   │   └── FilaExecucaoPipeline.java
│   │   │               │   └── util/
│   │   │               │       ├── ArquivoAtomicoUtil.java
│   │   │               │       ├── DuracaoUtil.java
│   │   │               │       └── ProcessoExternoUtil.java
│   │   │               ├── correcaoLegendas/
│   │   │               │   ├── application/
│   │   │               │   │   ├── CorretorTraducaoLlmService.java
│   │   │               │   │   ├── CorrigirLegendasUseCase.java
│   │   │               │   │   └── SanitizadorTagsService.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── CorrecaoLegendasRelatorioJson.java
│   │   │               │   │   ├── LogEventoCorrecaoLegendas.java
│   │   │               │   │   └── ResultadoCorrecaoLegendas.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   └── CorrecaoLegendasLogPersistencia.java
│   │   │               │   └── presentation/
│   │   │               │       └── CorrecaoLegendasController.java
│   │   │               ├── legendasExtracao/
│   │   │               │   ├── application/
│   │   │               │   │   ├── strategy/
│   │   │               │   │   │   ├── ExtratorAssStrategy.java
│   │   │               │   │   │   ├── ExtratorPgsStrategy.java
│   │   │               │   │   │   ├── ExtratorSrtStrategy.java
│   │   │               │   │   │   └── ExtratorStrategy.java
│   │   │               │   │   └── ExtrairLegendaUseCase.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── exceptions/
│   │   │               │   │   │   └── FormatoLegendaInvalidoException.java
│   │   │               │   │   ├── ports/
│   │   │               │   │   │   └── ExtratorVideoPort.java
│   │   │               │   │   ├── ExtratorException.java
│   │   │               │   │   ├── FaixaLegenda.java
│   │   │               │   │   ├── FormatoLegenda.java
│   │   │               │   │   └── RelatorioExtracao.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   ├── adapters/
│   │   │               │   │   │   ├── FfmpegAdapter.java
│   │   │               │   │   │   └── MkvToolNixAdapter.java
│   │   │               │   │   └── config/
│   │   │               │   │       └── ExtratorProperties.java
│   │   │               │   └── presentation/
│   │   │               │       ├── ui/
│   │   │               │       │   └── ConsoleExtratorLogger.java
│   │   │               │       └── ExtratorCLI.java
│   │   │               ├── mapaProjeto/
│   │   │               │   ├── application/
│   │   │               │   │   ├── GeradorMapaProjetoUseCase.java
│   │   │               │   │   └── MapeadorDiretorioUseCase.java
│   │   │               │   ├── domain/
│   │   │               │   │   └── exceptions/
│   │   │               │   │       └── MapaProjetoException.java
│   │   │               │   └── presentation/
│   │   │               │       └── MapaProjetoCLI.java
│   │   │               ├── mcp/
│   │   │               │   └── KronosMcpTools.java
│   │   │               ├── novoKaraoke/
│   │   │               │   ├── application/
│   │   │               │   │   └── ConversorKaraokeUseCase.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── EventoAss.java
│   │   │               │   │   ├── LinhaSimplesKaraoke.java
│   │   │               │   │   ├── NovoKaraokeException.java
│   │   │               │   │   └── ResultadoConversaoKaraoke.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   └── NovoKaraokePersistencia.java
│   │   │               │   └── presentation/
│   │   │               │       ├── NovoKaraokeController.java
│   │   │               │       └── NovoKaraokeRequest.java
│   │   │               ├── raspagemCorrecao/
│   │   │               │   ├── application/
│   │   │               │   │   └── CorrigirComGoogleUseCase.java
│   │   │               │   ├── domain/
│   │   │               │   │   └── exceptions/
│   │   │               │   │       └── RaspagemCorrecaoException.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   ├── GoogleTranslateScraper.java
│   │   │               │   │   ├── ResultadoRaspagem.java
│   │   │               │   │   └── StatusRaspagem.java
│   │   │               │   └── CorretorRaspagemCLI.java
│   │   │               ├── raspagemRevisao/
│   │   │               │   ├── application/
│   │   │               │   │   ├── AuditorProblemasLegendaService.java
│   │   │               │   │   ├── DetectorConcordanciaService.java
│   │   │               │   │   ├── ResultadoRevisaoLegendas.java
│   │   │               │   │   ├── RevisarCacheUseCase.java
│   │   │               │   │   └── RevisarLegendasUseCase.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── exceptions/
│   │   │               │   │   │   └── RaspagemRevisaoException.java
│   │   │               │   │   └── ResultadoDeteccaoConcordancia.java
│   │   │               │   ├── RevisorLegendasCLI.java
│   │   │               │   └── RevisorRaspagemCLI.java
│   │   │               ├── remuxer/
│   │   │               │   ├── application/
│   │   │               │   │   ├── MapeadorMidiaService.java
│   │   │               │   │   └── RemuxarLoteUseCase.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── MkvToolNixNaoEncontradoException.java
│   │   │               │   │   ├── RelatorioRemux.java
│   │   │               │   │   ├── RemuxerException.java
│   │   │               │   │   └── RemuxTarefa.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   ├── adapters/
│   │   │               │   │   │   └── MkvmergeAdapter.java
│   │   │               │   │   └── config/
│   │   │               │   │       └── RemuxerProperties.java
│   │   │               │   └── presentation/
│   │   │               │       ├── ui/
│   │   │               │       │   └── ConsoleRemuxerLogger.java
│   │   │               │       └── RemuxerCLI.java
│   │   │               ├── renomearArquivos/
│   │   │               │   ├── application/
│   │   │               │   │   └── RenomeadorUseCase.java
│   │   │               │   ├── domain/
│   │   │               │   │   └── OperacaoRenomeacao.java
│   │   │               │   └── presentation/
│   │   │               │       └── web/
│   │   │               │           ├── RenomearArquivosController.java
│   │   │               │           └── RenomearArquivosRequest.java
│   │   │               ├── revisaoLore/
│   │   │               │   ├── application/
│   │   │               │   │   ├── DetectorTermosLoreService.java
│   │   │               │   │   ├── GerenciadorPromptRevisaoLore.java
│   │   │               │   │   ├── PromptRevisaoLore.java
│   │   │               │   │   └── RevisarLoreUseCase.java
│   │   │               │   ├── contexto/
│   │   │               │   │   ├── ContextoRevisaoLore86.java
│   │   │               │   │   ├── ContextoRevisaoLoreDanMachi.java
│   │   │               │   │   ├── ContextoRevisaoLoreDanMachiS4.java
│   │   │               │   │   ├── ContextoRevisaoLoreDanMachiS5.java
│   │   │               │   │   ├── ContextoRevisaoLoreGuiltyCrown.java
│   │   │               │   │   ├── ContextoRevisaoLoreGundam0080.java
│   │   │               │   │   ├── ContextoRevisaoLoreGundam0083.java
│   │   │               │   │   ├── ContextoRevisaoLoreGundam08thMSTeam.java
│   │   │               │   │   ├── ContextoRevisaoLoreGundamCCA.java
│   │   │               │   │   ├── ContextoRevisaoLoreGundamNT.java
│   │   │               │   │   ├── ContextoRevisaoLoreGundamUnicorn.java
│   │   │               │   │   ├── ContextoRevisaoLoreGundamZeta.java
│   │   │               │   │   └── ContextoRevisaoLoreGundamZZ.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── exceptions/
│   │   │               │   │   │   └── RevisaoLoreException.java
│   │   │               │   │   ├── ports/
│   │   │               │   │   │   └── ProvedorPromptRevisaoLore.java
│   │   │               │   │   ├── EntradaAuditoriaRevisaoLore.java
│   │   │               │   │   ├── LogEventoRevisaoLore.java
│   │   │               │   │   ├── ResultadoDeteccaoLore.java
│   │   │               │   │   ├── ResultadoRevisaoLore.java
│   │   │               │   │   └── RevisaoLoreRelatorioJson.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   ├── RevisaoLoreAuditoriaCache.java
│   │   │               │   │   └── RevisaoLoreLogPersistencia.java
│   │   │               │   └── presentation/
│   │   │               │       └── RevisaoLoreController.java
│   │   │               ├── sistema/
│   │   │               │   ├── application/
│   │   │               │   │   └── EncerrarAplicacaoUseCase.java
│   │   │               │   └── presentation/
│   │   │               │       └── SistemaController.java
│   │   │               ├── telemetria/
│   │   │               │   ├── AmbienteExecucaoDataset.java
│   │   │               │   ├── AmbienteExecucaoDatasetService.java
│   │   │               │   ├── LlmTelemetria.java
│   │   │               │   ├── MidiaTelemetria.java
│   │   │               │   ├── OperacaoHistorico.java
│   │   │               │   ├── OperacaoTelemetria.java
│   │   │               │   ├── RevisaoLoreTelemetriaResumo.java
│   │   │               │   ├── TelemetriaDatasetProperties.java
│   │   │               │   ├── TelemetriaDatasetService.java
│   │   │               │   ├── TelemetriaResumo.java
│   │   │               │   └── TelemetriaService.java
│   │   │               ├── traducao/
│   │   │               │   ├── application/
│   │   │               │   │   ├── DetectorEfeitoKaraokeService.java
│   │   │               │   │   ├── DetectorTraducaoIdenticaService.java
│   │   │               │   │   ├── ProcessarArquivoUseCase.java
│   │   │               │   │   ├── ProcessarEpisodioUseCase.java
│   │   │               │   │   ├── ProtecaoLegendaAssService.java
│   │   │               │   │   └── ValidadorTraducaoService.java
│   │   │               │   ├── contexto/
│   │   │               │   │   ├── danmachi/
│   │   │               │   │   │   ├── ContextoDanMachi.java
│   │   │               │   │   │   ├── ContextoDanMachiOrion.java
│   │   │               │   │   │   ├── ContextoDanMachiS1.java
│   │   │               │   │   │   ├── ContextoDanMachiS2.java
│   │   │               │   │   │   ├── ContextoDanMachiS3.java
│   │   │               │   │   │   ├── ContextoDanMachiS4.java
│   │   │               │   │   │   ├── ContextoDanMachiS5.java
│   │   │               │   │   │   └── ContextoDanMachiSwordOratoria.java
│   │   │               │   │   ├── eightsix/
│   │   │               │   │   │   └── Contexto86.java
│   │   │               │   │   ├── evangelion/
│   │   │               │   │   │   ├── ContextoEvangelion111.java
│   │   │               │   │   │   ├── ContextoEvangelion222.java
│   │   │               │   │   │   ├── ContextoEvangelion3010.java
│   │   │               │   │   │   ├── ContextoEvangelion333.java
│   │   │               │   │   │   └── ContextoEvangelionTV.java
│   │   │               │   │   ├── guiltycrown/
│   │   │               │   │   │   └── ContextoGuiltyCrown.java
│   │   │               │   │   ├── gundam/
│   │   │               │   │   │   ├── chars/
│   │   │               │   │   │   │   └── ContextoCharsCounterattack.java
│   │   │               │   │   │   ├── msteam/
│   │   │               │   │   │   │   └── ContextoGundam08thMSTeam.java
│   │   │               │   │   │   ├── reconguista/
│   │   │               │   │   │   │   └── ContextoGundamReconguista.java
│   │   │               │   │   │   ├── stardust/
│   │   │               │   │   │   │   └── ContextoGundam0083.java
│   │   │               │   │   │   ├── warInpocket/
│   │   │               │   │   │   │   └── ContextoWarInPocket.java
│   │   │               │   │   │   ├── zeta/
│   │   │               │   │   │   │   └── ContextoGundamZeta.java
│   │   │               │   │   │   ├── zz/
│   │   │               │   │   │   │   └── ContextoGundamZZ.java
│   │   │               │   │   │   ├── ContextoGundam0079.java
│   │   │               │   │   │   ├── ContextoGundamF91.java
│   │   │               │   │   │   ├── ContextoGundamHathaway.java
│   │   │               │   │   │   ├── ContextoGundamNT.java
│   │   │               │   │   │   ├── ContextoGundamOrigin.java
│   │   │               │   │   │   ├── ContextoGundamSEED.java
│   │   │               │   │   │   ├── ContextoGundamSEEDAstray.java
│   │   │               │   │   │   ├── ContextoGundamSEEDDestiny.java
│   │   │               │   │   │   ├── ContextoGundamSEEDFreedom.java
│   │   │               │   │   │   ├── ContextoGundamSEEDStargazer.java
│   │   │               │   │   │   ├── ContextoGundamUnicorn.java
│   │   │               │   │   │   └── ContextoGundamVictory.java
│   │   │               │   │   ├── macross/
│   │   │               │   │   │   ├── ContextoMacross2.java
│   │   │               │   │   │   ├── ContextoMacross7.java
│   │   │               │   │   │   ├── ContextoMacross7Encore.java
│   │   │               │   │   │   ├── ContextoMacross7Filme.java
│   │   │               │   │   │   ├── ContextoMacross7Filmes.java
│   │   │               │   │   │   ├── ContextoMacrossAnime.java
│   │   │               │   │   │   ├── ContextoMacrossDelta.java
│   │   │               │   │   │   ├── ContextoMacrossDeltaFilme1.java
│   │   │               │   │   │   ├── ContextoMacrossDeltaFilme2.java
│   │   │               │   │   │   ├── ContextoMacrossDeltaFilmes.java
│   │   │               │   │   │   ├── ContextoMacrossDynamite7.java
│   │   │               │   │   │   ├── ContextoMacrossDYRL.java
│   │   │               │   │   │   ├── ContextoMacrossFilme1.java
│   │   │               │   │   │   ├── ContextoMacrossFilme2.java
│   │   │               │   │   │   ├── ContextoMacrossFrontier.java
│   │   │               │   │   │   ├── ContextoMacrossFrontierFilme1.java
│   │   │               │   │   │   ├── ContextoMacrossFrontierFilme2.java
│   │   │               │   │   │   ├── ContextoMacrossFrontierFilmes.java
│   │   │               │   │   │   ├── ContextoMacrossPlus.java
│   │   │               │   │   │   └── ContextoMacrossZero.java
│   │   │               │   │   ├── sidonia/
│   │   │               │   │   │   ├── ContextoKnightsOfSidonia.java
│   │   │               │   │   │   └── ContextoSidoniaFilme.java
│   │   │               │   │   ├── ContextoPrompt.java
│   │   │               │   │   └── RegrasConcordanciaPtBr.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── exceptions/
│   │   │               │   │   │   ├── AlucinacaoDetectadaException.java
│   │   │               │   │   │   ├── ArquivoLegendaException.java
│   │   │               │   │   │   ├── ContextoNaoEncontradoException.java
│   │   │               │   │   │   ├── DivergenciaLinhasException.java
│   │   │               │   │   │   ├── LlmFalhaComunicacaoException.java
│   │   │               │   │   │   ├── LmStudioOfflineException.java
│   │   │               │   │   │   ├── RespostaLlmVaziaException.java
│   │   │               │   │   │   ├── TraducaoParcialException.java
│   │   │               │   │   │   └── TradutorException.java
│   │   │               │   │   ├── legenda/
│   │   │               │   │   │   ├── DocumentoLegenda.java
│   │   │               │   │   │   └── EventoLegenda.java
│   │   │               │   │   ├── ports/
│   │   │               │   │   │   ├── MistralPort.java
│   │   │               │   │   │   └── ProvedorContexto.java
│   │   │               │   │   ├── Lote.java
│   │   │               │   │   ├── StatusLlm.java
│   │   │               │   │   └── TraducaoLote.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   ├── adapters/
│   │   │               │   │   │   └── MistralClientAdapter.java
│   │   │               │   │   ├── config/
│   │   │               │   │   │   ├── LlmProperties.java
│   │   │               │   │   │   ├── RestClientConfig.java
│   │   │               │   │   │   └── TradutorProperties.java
│   │   │               │   │   ├── contexto/
│   │   │               │   │   │   └── GerenciadorContexto.java
│   │   │               │   │   ├── dtos/
│   │   │               │   │   │   └── RecordsMistral.java
│   │   │               │   │   ├── http/
│   │   │               │   │   │   └── JsonHttpClient.java
│   │   │               │   │   └── legenda/
│   │   │               │   │       ├── EscritorLegendaAss.java
│   │   │               │   │       ├── LeitorLegendaAss.java
│   │   │               │   │       └── MascaradorTags.java
│   │   │               │   ├── presentation/
│   │   │               │   │   ├── ui/
│   │   │               │   │   │   ├── AnsiCores.java
│   │   │               │   │   │   ├── ConsoleEntrada.java
│   │   │               │   │   │   ├── ConsoleUILogger.java
│   │   │               │   │   │   └── PastasExecucao.java
│   │   │               │   │   ├── web/
│   │   │               │   │   │   ├── ApiController.java
│   │   │               │   │   │   ├── BrowserLauncher.java
│   │   │               │   │   │   ├── ConsoleRedirector.java
│   │   │               │   │   │   ├── DialogoArquivoController.java
│   │   │               │   │   │   ├── DocumentacaoController.java
│   │   │               │   │   │   ├── LogStreamResource.java
│   │   │               │   │   │   ├── LogStreamService.java
│   │   │               │   │   │   └── TelemetriaStreamResource.java
│   │   │               │   │   └── TradutorCLI.java
│   │   │               │   └── Application.java
│   │   │               ├── traducaoCorrige/
│   │   │               │   ├── application/
│   │   │               │   │   └── LimparCacheUseCase.java
│   │   │               │   ├── domain/
│   │   │               │   │   └── exceptions/
│   │   │               │   │       └── CorretorCacheException.java
│   │   │               │   ├── CorretorCache.java
│   │   │               │   └── CorretorCacheCLI.java
│   │   │               ├── traducaoKaraoke/
│   │   │               │   ├── application/
│   │   │               │   │   ├── ClassificadorLetraKaraokeService.java
│   │   │               │   │   └── TraduzirKaraokeUseCase.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── ClasseLinhaKaraoke.java
│   │   │               │   │   ├── ResultadoTraducaoKaraoke.java
│   │   │               │   │   └── TraducaoKaraokeException.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   └── TraducaoKaraokePersistencia.java
│   │   │               │   └── presentation/
│   │   │               │       ├── TraducaoKaraokeController.java
│   │   │               │       └── TraducaoKaraokeRequest.java
│   │   │               └── trocaTipoLegenda/
│   │   │                   ├── application/
│   │   │                   │   ├── AuditoriaFontesService.java
│   │   │                   │   └── TrocaTipoLegendaUseCase.java
│   │   │                   ├── domain/
│   │   │                   │   ├── exceptions/
│   │   │                   │   │   └── TrocaTipoLegendaException.java
│   │   │                   │   ├── AuditoriaFonteInfo.java
│   │   │                   │   ├── AuditoriaLegendaResultado.java
│   │   │                   │   ├── EntradaAuditoriaTrocaFonte.java
│   │   │                   │   ├── ResultadoGeralAuditoria.java
│   │   │                   │   └── ResultadoTrocaFonte.java
│   │   │                   ├── infrastructure/
│   │   │                   │   └── TrocaTipoLegendaAuditoriaCache.java
│   │   │                   └── presentation/
│   │   │                       └── TrocaTipoLegendaController.java
│   │   └── resources/
│   │       ├── static/
│   │       │   ├── analise/
│   │       │   │   ├── analise.css
│   │       │   │   └── analise.js
│   │       │   ├── auditorConteudoLegendas/
│   │       │   │   ├── auditorConteudoLegendas.css
│   │       │   │   ├── auditorConteudoLegendas.html
│   │       │   │   └── auditorConteudoLegendas.js
│   │       │   ├── correcao/
│   │       │   │   ├── correcao.css
│   │       │   │   └── correcao.js
│   │       │   ├── css/
│   │       │   │   └── base.css
│   │       │   ├── cura/
│   │       │   │   ├── cura.css
│   │       │   │   └── cura.js
│   │       │   ├── documentacao/
│   │       │   │   ├── documentacao.css
│   │       │   │   └── documentacao.js
│   │       │   ├── extracao/
│   │       │   │   ├── extracao.css
│   │       │   │   └── extracao.js
│   │       │   ├── img/
│   │       │   │   ├── screenshots/
│   │       │   │   │   ├── analise-conteudo.png
│   │       │   │   │   ├── analise-midia.webp
│   │       │   │   │   ├── correcao-cache.webp
│   │       │   │   │   ├── cura-legendas.webp
│   │       │   │   │   ├── documentacao.webp
│   │       │   │   │   ├── extracao.webp
│   │       │   │   │   ├── karaoke-simples.png
│   │       │   │   │   ├── mapa-projeto.webp
│   │       │   │   │   ├── metadados-anime.webp
│   │       │   │   │   ├── painel-inicial.webp
│   │       │   │   │   ├── remuxer.webp
│   │       │   │   │   ├── renomear-arquivos.png
│   │       │   │   │   ├── revisao-legendas.webp
│   │       │   │   │   ├── revisao-lore.webp
│   │       │   │   │   ├── telemetria.webp
│   │       │   │   │   ├── traducao-karaoke.png
│   │       │   │   │   ├── traducao-local.webp
│   │       │   │   │   └── troca-tipo-legenda.png
│   │       │   │   ├── antigravity_banner.png
│   │       │   │   ├── antigravity_logo.png
│   │       │   │   ├── kronos_banner.png
│   │       │   │   ├── kronos_logo.png
│   │       │   │   └── kronos_logo.svg
│   │       │   ├── inicio/
│   │       │   │   ├── inicio.css
│   │       │   │   └── inicio.js
│   │       │   ├── js/
│   │       │   │   ├── app.js
│   │       │   │   ├── chart.umd.min.js
│   │       │   │   ├── marked.min.js
│   │       │   │   └── mermaid.min.js
│   │       │   ├── mapa/
│   │       │   │   ├── mapa.css
│   │       │   │   └── mapa.js
│   │       │   ├── novoKaraoke/
│   │       │   │   ├── novoKaraoke.css
│   │       │   │   ├── novoKaraoke.html
│   │       │   │   └── novoKaraoke.js
│   │       │   ├── remuxer/
│   │       │   │   ├── remuxer.css
│   │       │   │   └── remuxer.js
│   │       │   ├── renomearArquivos/
│   │       │   │   ├── renomearArquivos.css
│   │       │   │   ├── renomearArquivos.html
│   │       │   │   └── renomearArquivos.js
│   │       │   ├── revisao/
│   │       │   │   ├── revisao.css
│   │       │   │   └── revisao.js
│   │       │   ├── revisaoLore/
│   │       │   │   ├── revisaoLore.css
│   │       │   │   ├── revisaoLore.html
│   │       │   │   └── revisaoLore.js
│   │       │   ├── sobre/
│   │       │   │   ├── sobre.css
│   │       │   │   ├── sobre.html
│   │       │   │   └── sobre.js
│   │       │   ├── telemetria/
│   │       │   │   ├── telemetria.css
│   │       │   │   └── telemetria.js
│   │       │   ├── traducao/
│   │       │   │   ├── traducao.css
│   │       │   │   └── traducao.js
│   │       │   ├── traducaoKaraoke/
│   │       │   │   ├── traducaoKaraoke.css
│   │       │   │   ├── traducaoKaraoke.html
│   │       │   │   └── traducaoKaraoke.js
│   │       │   ├── trocaTipoLegenda/
│   │       │   │   ├── trocaTipoLegenda.css
│   │       │   │   ├── trocaTipoLegenda.html
│   │       │   │   └── trocaTipoLegenda.js
│   │       │   └── index.html
│   │       ├── application-local.yml.example
│   │       ├── application.properties
│   │       └── application.yml
│   └── test/
│       └── java/
│           └── org/
│               └── traducao/
│                   └── projeto/
│                       ├── analisadorMidia/
│                       │   ├── application/
│                       │   │   └── AnalisarMidiaClassificacaoTest.java
│                       │   └── infrastructure/
│                       │       └── adapters/
│                       │           └── FfprobeAdapterTest.java
│                       ├── apiDadosAnime/
│                       │   └── application/
│                       │       └── ObterMetadataAnimeUseCaseTest.java
│                       ├── auditorConteudoLegendas/
│                       │   ├── application/
│                       │   │   ├── regras/
│                       │   │   │   ├── RegraAlucinacaoQuebraLinhaTest.java
│                       │   │   │   ├── RegraDanoKaraokeTest.java
│                       │   │   │   ├── RegraEfeitoVazadoTest.java
│                       │   │   │   ├── RegraMetadadosAssTest.java
│                       │   │   │   └── RegraSincroniaEstilosTest.java
│                       │   │   └── AuditorConteudoUseCaseTest.java
│                       │   └── support/
│                       │       └── AssAuditoriaFixtures.java
│                       ├── core/
│                       │   ├── exception/
│                       │   │   └── BasePipelineExceptionTest.java
│                       │   └── execucao/
│                       │       └── FilaExecucaoPipelineTest.java
│                       ├── correcaoLegendas/
│                       │   └── application/
│                       │       └── CorrigirLegendasUseCaseTest.java
│                       ├── legendasExtracao/
│                       │   └── infrastructure/
│                       │       └── adapters/
│                       │           ├── FfmpegAdapterTest.java
│                       │           └── MkvToolNixAdapterTest.java
│                       ├── mapaProjeto/
│                       │   └── application/
│                       │       └── GeradorMapaProjetoUseCaseTest.java
│                       ├── mcp/
│                       │   └── KronosMcpToolsTest.java
│                       ├── novoKaraoke/
│                       │   └── application/
│                       │       └── ConversorKaraokeUseCaseTest.java
│                       ├── raspagemCorrecao/
│                       │   └── infrastructure/
│                       │       └── GoogleTranslateScraperTest.java
│                       ├── remuxer/
│                       │   └── application/
│                       │       └── MapeadorMidiaServiceTest.java
│                       ├── renomearArquivos/
│                       │   └── application/
│                       │       └── RenomeadorUseCaseTest.java
│                       ├── revisaoLore/
│                       │   ├── application/
│                       │   │   ├── DetectorTermosLoreServiceTest.java
│                       │   │   └── RevisarLoreUseCaseTest.java
│                       │   ├── contexto/
│                       │   │   └── ContextosRevisaoLoreCatalogoTest.java
│                       │   └── infrastructure/
│                       │       └── RevisaoLoreAuditoriaCacheTest.java
│                       ├── telemetria/
│                       │   ├── TelemetriaDatasetPropertiesTest.java
│                       │   ├── TelemetriaDatasetServiceTest.java
│                       │   ├── TelemetriaServiceCompactacaoTest.java
│                       │   └── TelemetriaServiceRevisaoLoreTest.java
│                       ├── traducao/
│                       │   ├── application/
│                       │   │   ├── DetectorEfeitoKaraokeServiceTest.java
│                       │   │   ├── ProcessarArquivoUseCaseGuardTest.java
│                       │   │   └── ValidadorTraducaoServiceTest.java
│                       │   ├── infrastructure/
│                       │   │   └── legenda/
│                       │   │       └── EscritorLegendaAssTest.java
│                       │   └── presentation/
│                       │       └── web/
│                       │           ├── ConsoleRedirectorTest.java
│                       │           └── LogStreamServiceTest.java
│                       ├── traducaoKaraoke/
│                       │   └── application/
│                       │       ├── ClassificadorLetraKaraokeServiceTest.java
│                       │       └── TraduzirKaraokeUseCaseTest.java
│                       ├── trocaTipoLegenda/
│                       │   └── application/
│                       │       ├── AuditoriaFontesServiceTest.java
│                       │       └── TrocaTipoLegendaUseCaseTest.java
│                       ├── ApiControllerTest.java
│                       ├── ApiEndpointsTest.java
│                       └── WebInterfaceTest.java
├── .dockerignore
├── .gitignore
├── .mcp.json
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── hero-banner-atual.png
├── iniciar-kronos-dev.cmd
├── mapa_projeto.md
├── README.md
└── settings.gradle

--------------------------------------------------------------------------------
 2. TAXONOMIA DOS ARQUIVOS-FONTE (.java / .py)
--------------------------------------------------------------------------------

[PASTA] src/main/java/org/traducao/projeto/analisadorMidia/application/
  - AnalisarMidiaUseCase.java
      Classificação por traduzibilidade (o dado vital da análise): legenda de
      TEXTO é extraível e traduzível; BITMAP/hardsub exige OCR e não entra no
      pipeline de tradução direto. Baseado no tipoCurto de classificarLegenda().

[PASTA] src/main/java/org/traducao/projeto/analisadorMidia/domain/
  - AnalisadorException.java
      (sem cabecalho explicativo)
  - AudioInfo.java
      (sem cabecalho explicativo)
  - AuditoriaResultado.java
      (sem cabecalho explicativo)
  - ContainerInfo.java
      (sem cabecalho explicativo)
  - LegendaInfo.java
      (sem cabecalho explicativo)
  - ResultadoAnaliseLote.java
      Resultado de uma execução de auditoria sobre um lote de vídeos, incluindo o
      caminho do relatório de texto efetivamente gravado em disco (individual,
      se um único arquivo foi analisado, ou consolidado, se foram vários).
      {@code relatorioPrincipal} é {@code null} se nada foi gravado (ex.: falha de IO).
  - VideoInfo.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/analisadorMidia/domain/exceptions/
  - AnaliseStreamException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/analisadorMidia/infrastructure/adapters/
  - FfprobeAdapter.java
      Executa ffprobe no vídeo e obtém o JSON com as informações gerais e faixas.
      Parsing do Container
      Parsing das faixas (streams)

[PASTA] src/main/java/org/traducao/projeto/analisadorMidia/presentation/
  - AnalisadorMidiaCLI.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/analisadorMidia/presentation/ui/
  - ConsoleAnalisadorLogger.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/apiDadosAnime/application/
  - ObterMetadataAnimeUseCase.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/apiDadosAnime/domain/exceptions/
  - AnimeNaoEncontradoException.java
      (sem cabecalho explicativo)
  - ApiDadosAnimeException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/apiDadosAnime/domain/model/
  - AnimeMetadata.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/apiDadosAnime/infrastructure/adapters/
  - JikanApiClientAdapter.java
      (sem cabecalho explicativo)
  - TmdbApiClientAdapter.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/apiDadosAnime/presentation/web/
  - AnimeMetadataController.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/auditorConteudoLegendas/application/
  - AuditorConteudoUseCase.java
      (sem cabecalho explicativo)
  - TelemetriaAuditoriaService.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/auditorConteudoLegendas/application/regras/
  - RegraAlucinacaoQuebraLinha.java
      (sem cabecalho explicativo)
  - RegraDanoKaraoke.java
      Detecta dano de tradução em karaokê/música comparando cada evento traduzido
      com o original. Usa o {@link DetectorEfeitoKaraokeService} como fonte única
      de verdade, a mesma régua da tradução, correção e revisão.
  - RegraEfeitoVazado.java
      (sem cabecalho explicativo)
  - RegraMetadadosAss.java
      (sem cabecalho explicativo)
  - RegraSincroniaEstilos.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/auditorConteudoLegendas/domain/
  - AnomaliaConteudo.java
      (sem cabecalho explicativo)
  - AuditoriaConteudoRelatorioJson.java
      Relatório persistido em JSON da auditoria de conteúdo de legendas.
  - AuditoriaException.java
      (sem cabecalho explicativo)
  - RegraAuditoriaConteudo.java
      (sem cabecalho explicativo)
  - RelatorioAuditoriaConteudo.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/auditorConteudoLegendas/infrastructure/
  - AuditoriaConteudoPersistencia.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/auditorConteudoLegendas/presentation/
  - AuditorConteudoController.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/config/
  - AppConfig.java
      (sem cabecalho explicativo)
  - ExecucaoCli.java
      Contrato para modos de execucao em linha de comando (substituto do CommandLineRunner do Spring Boot).
  - ModoExecucaoStartup.java
      Dispara o modo CLI configurado em {@code app.modo}. No modo WEB nenhuma CLI e executada.

[PASTA] src/main/java/org/traducao/projeto/core/exception/
  - BasePipelineException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/core/exception/web/
  - BasePipelineExceptionMapper.java
      Converte qualquer exceção de domínio do pipeline (uma por pacote, todas
      estendendo {@link BasePipelineException}) em uma resposta JSON estruturada
      e rastreável, em vez de cada endpoint precisar capturar e formatar erro
      manualmente. O {@code errorId} permite cruzar a resposta HTTP com a
      entrada correspondente no log do servidor.

[PASTA] src/main/java/org/traducao/projeto/core/execucao/
  - FilaExecucaoPipeline.java
      Fila única (single-thread) para todos os jobs pesados do pipeline —
      tradução, correção, revisões (concordância/lore), análise, extração, remux.
      <p>
      Ter UMA fila compartilhada é requisito de corretude, não só de desempenho:
      o contexto de tradução ativo ({@code GerenciadorContexto}) e o modelo LLM
      configurado são estado global mutado no início de cada job. Quando cada
      controller tinha seu próprio executor (ou rodava na thread HTTP), dois jobs
      podiam rodar em paralelo e um trocava a lore/modelo no meio do outro — além
      de disputarem a GPU do LM Studio, que atende uma inferência por vez.

[PASTA] src/main/java/org/traducao/projeto/core/util/
  - ArquivoAtomicoUtil.java
      Substituição atômica de arquivo (temporário -&gt; destino) tolerante ao Windows.
      
      <p>No Windows, o move atômico ({@code MoveFileEx}) falha com
      {@link AccessDeniedException} quando o arquivo de destino está momentaneamente
      aberto por outro processo sem compartilhamento de exclusão — tipicamente
      antivírus ou indexador varrendo o arquivo recém-gravado. O travamento dura
      milissegundos, então algumas tentativas com espera crescente resolvem sem
      perder a garantia de "nunca deixa o destino truncado".</p>
  - DuracaoUtil.java
      Formata durações de jobs para o relatório final dos consoles da UI
      (ex.: "1h 04min 12s", "3min 08s", "45s", "0,8s"). Todos os módulos usam o
      mesmo formato para o usuário comparar execuções entre etapas do pipeline.
  - ProcessoExternoUtil.java
      Executa processos externos (ffmpeg, ffprobe, mkvmerge, mkvextract) de forma segura:
      drena stdout e stderr em threads separadas (evita o deadlock classico de ProcessBuilder,
      em que o processo filho trava escrevendo em um pipe cujo buffer do SO enche enquanto o
      pai ainda le o outro stream) e aplica um timeout que mata o processo (destroyForcibly)
      caso ele nao termine a tempo, em vez de travar o pipeline indefinidamente.

[PASTA] src/main/java/org/traducao/projeto/correcaoLegendas/application/
  - CorretorTraducaoLlmService.java
      Retorna a tradução corrigida via LLM apenas se a tradução atual estiver com
      resíduo em inglês/preâmbulo (ValidadorTraducaoService) — evita chamar o LLM
      para falas que já estão corretas.
  - CorrigirLegendasUseCase.java
      (sem cabecalho explicativo)
  - SanitizadorTagsService.java
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

[PASTA] src/main/java/org/traducao/projeto/correcaoLegendas/domain/
  - CorrecaoLegendasRelatorioJson.java
      (sem cabecalho explicativo)
  - LogEventoCorrecaoLegendas.java
      (sem cabecalho explicativo)
  - ResultadoCorrecaoLegendas.java
      Resultado da correção: {@code curados} conta ARQUIVOS modificados;
      {@code falasCuradas} e {@code corrigidosLlm} contam FALAS (linhas) — a
      telemetria usa apenas contagens de falas para não misturar unidades.

[PASTA] src/main/java/org/traducao/projeto/correcaoLegendas/infrastructure/
  - CorrecaoLegendasLogPersistencia.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/correcaoLegendas/presentation/
  - CorrecaoLegendasController.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/legendasExtracao/application/
  - ExtrairLegendaUseCase.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/legendasExtracao/application/strategy/
  - ExtratorAssStrategy.java
      1. Tentar por palavras-chave
      2. Tentar a última candidata (geralmente a faixa completa em ASS, a primeira é signs)
  - ExtratorPgsStrategy.java
      Para PGS, geralmente pega a primeira encontrada ou a marcada como default
  - ExtratorSrtStrategy.java
      (sem cabecalho explicativo)
  - ExtratorStrategy.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/legendasExtracao/domain/exceptions/
  - FormatoLegendaInvalidoException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/legendasExtracao/domain/
  - ExtratorException.java
      (sem cabecalho explicativo)
  - FaixaLegenda.java
      (sem cabecalho explicativo)
  - FormatoLegenda.java
      (sem cabecalho explicativo)
  - RelatorioExtracao.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/legendasExtracao/domain/ports/
  - ExtratorVideoPort.java
      Abstrai a ferramenta usada para identificar e extrair faixas de legenda de um
      vídeo. Cada implementação é responsável por um conjunto de contêineres
      (ex.: MKVToolNix para Matroska, ffmpeg para os demais formatos).

[PASTA] src/main/java/org/traducao/projeto/legendasExtracao/infrastructure/adapters/
  - FfmpegAdapter.java
      Extrai legendas de contêineres que o MKVToolNix não lê (mkvextract só opera
      sobre Matroska/WebM). Cobre MP4, MOV, AVI e afins via ffmpeg/ffprobe.
  - MkvToolNixAdapter.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/legendasExtracao/infrastructure/config/
  - ExtratorProperties.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/legendasExtracao/presentation/
  - ExtratorCLI.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/legendasExtracao/presentation/ui/
  - ConsoleExtratorLogger.java
      Tag colorida em negrito (chama atenção), corpo da mensagem em peso normal
      (mais fácil de ler em blocos de texto maiores) — INFO fica sem cor nenhuma.

[PASTA] src/main/java/org/traducao/projeto/mapaProjeto/application/
  - GeradorMapaProjetoUseCase.java
      Conectores de árvore (estilo `tree` do Unix) — linhas alinhadas em fonte monoespaçada.
  - MapeadorDiretorioUseCase.java
      Cabeçalho Técnico
      PARTE 1: CAMINHO ABSOLUTO COMPLETO NO SISTEMA LOCAL

[PASTA] src/main/java/org/traducao/projeto/mapaProjeto/domain/exceptions/
  - MapaProjetoException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/mapaProjeto/presentation/
  - MapaProjetoCLI.java
      Determina a raiz a ser mapeada

[PASTA] src/main/java/org/traducao/projeto/mcp/
  - KronosMcpTools.java
      Ferramentas MCP (Model Context Protocol) expostas pelo KRONOS via transporte
      SSE em {@code /mcp/sse}. Clientes MCP (ex.: Claude Code) acionam o pipeline
      enquanto o servidor web ja esta rodando em modo dev.
      <p>
      Toda operação pesada passa pela mesma {@link FilaExecucaoPipeline} da UI: o
      MCP não é uma porta paralela. Isso garante execução sequencial (MCP e UI não
      disputam GPU/estado global), torna o job visível a {@code ocupada()} e o deixa
      cancelável pelo "Parar".

[PASTA] src/main/java/org/traducao/projeto/novoKaraoke/application/
  - ConversorKaraokeUseCase.java
      Converte legendas .ass com karaokê KFX (milhares de eventos por sílaba/frame)
      em legendas simples: uma linha limpa por frase da música, no MESMO tempo do
      efeito original (início = menor início do bloco, fim = maior fim).
      <p>
      Garantias de segurança:
      <ul>
      <li>O arquivo original NUNCA é alterado — a saída vai para a pasta que o
      usuário escolher.</li>
      <li>Diálogo, placas e Comment são reemitidos byte a byte (linha crua).</li>

[PASTA] src/main/java/org/traducao/projeto/novoKaraoke/domain/
  - EventoAss.java
      Um evento {@code Dialogue:} de um arquivo .ass, com a linha crua preservada
      byte a byte. A conversão de karaokê NUNCA reescreve eventos que decide
      manter — ela reemite {@link #linhaCrua()} — para garantir que diálogo,
      placas e blocos preservados saiam idênticos ao arquivo de origem.
      
      @param linhaCrua  linha original completa, exatamente como lida do arquivo
      @param camada     campo Layer
      @param inicio     campo Start (mantido como texto para não perder precisão)
      @param fim        campo End
      @param estilo     campo Style
      @param texto      campo Text (último campo, pode conter vírgulas)
  - LinhaSimplesKaraoke.java
      Uma linha de letra de música reconstruída a partir do bloco de eventos KFX.
      O tempo é herdado literalmente dos eventos de origem: {@code inicioCs} é o
      menor início e {@code fimCs} o maior fim do grupo — nenhum deslocamento é
      introduzido, a legenda simples ocupa exatamente a janela do efeito original.
      
      @param texto            texto visível da linha (sem tags)
      @param inicioCs         menor início do grupo, em centésimos
      @param fimCs            maior fim do grupo, em centésimos
      @param eventosOrigem    quantos eventos KFX foram colapsados nesta linha
      @param variantesTexto   variantes divergentes encontradas na mesma janela (>1 indica voto majoritário)
  - NovoKaraokeException.java
      Falha de negócio na conversão de karaokê para legenda simples. /
  - ResultadoConversaoKaraoke.java
      Resultado da conversão de um arquivo .ass: contadores para o resumo do
      console/telemetria e o material do manifesto de auditoria.

[PASTA] src/main/java/org/traducao/projeto/novoKaraoke/infrastructure/
  - NovoKaraokePersistencia.java
      Manifesto de auditoria da conversão de karaokê: registra, por execução, o
      que foi removido/criado em cada arquivo. Fica em
      {@code logs/novo-karaoke/} dentro do projeto — junto com os originais
      intocados na pasta de origem, é a trilha completa para auditar (ou refazer)
      qualquer conversão.

[PASTA] src/main/java/org/traducao/projeto/novoKaraoke/presentation/
  - NovoKaraokeController.java
      Endpoints do módulo Karaokê Simples. Operação puramente local (sem LLM,
      sem estado global do pipeline), por isso roda async fora da fila — mesmo
      padrão do módulo de Renomear Arquivos.
  - NovoKaraokeRequest.java
      Requisição da conversão de karaokê: pasta das legendas .ass de origem e a
      pasta de destino (obrigatoriamente diferente — o original é preservado).

[PASTA] src/main/java/org/traducao/projeto/raspagemCorrecao/application/
  - CorrigirComGoogleUseCase.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/raspagemCorrecao/
  - CorretorRaspagemCLI.java
      CommandLineRunner que realiza a tradução das falas residuais pendentes em inglês
      utilizando raspagem na API gratuita e sem chaves do Google Translate.
      Ativado quando a propriedade app.modo é configurada como "RASPAGEM_CORRECAO".

[PASTA] src/main/java/org/traducao/projeto/raspagemCorrecao/domain/exceptions/
  - RaspagemCorrecaoException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/raspagemCorrecao/infrastructure/
  - GoogleTranslateScraper.java
      Traduz texto via API pública do Google Translate, preservando tags ASS
      mascaradas e quebras {@code \N}.
      <p>
      O retorno é tipado ({@link ResultadoRaspagem}): cada desfecho — sucesso, sem
      alteração, falha transitória, resposta inválida ou tag corrompida — vira um
      {@link StatusRaspagem} explícito, em vez de o chamador ter que adivinhar a
      partir de "o texto voltou igual". O transporte HTTP fica isolado em
      {@link #executarGet(String)} para poder ser substituído em testes.
  - ResultadoRaspagem.java
      Resultado tipado de {@link GoogleTranslateScraper#traduzir(String)}: o
      {@link StatusRaspagem} do desfecho e o texto associado.
      <p>
      Em {@link StatusRaspagem#SUCESSO}, {@code texto} é a tradução; em qualquer
      outro caso é o <b>texto original</b> (o chamador mantém a fala intacta), agora
      sabendo o MOTIVO em vez de inferir por igualdade de strings.
  - StatusRaspagem.java
      Desfecho semântico de uma tentativa de tradução via Google Translate.
      <p>
      Substitui a convenção antiga de "texto de saída == original" — que era
      ambígua e interpretada de formas <b>inconsistentes</b> pelos consumidores (um
      tratava como falha, outro como 'sem alteração'). Também é a base para um retry
      seletivo: só {@link #FALHA_TRANSITORIA} vale repetir; resposta estruturalmente
      inválida ou tag corrompida não deve ser retentada.
      Tradução válida e diferente do original. /
      Google devolveu texto idêntico ao original — nada a corrigir. /
      HTTP transitório (408/429/5xx), timeout ou falha de rede — pode valer retry. /
      HTTP não transitório, JSON inesperado ou resposta sem segmentos traduzíveis. /
      Marcador de tag/quebra mutilado ou tag ASS perdida na volta da tradução. /

[PASTA] src/main/java/org/traducao/projeto/raspagemRevisao/application/
  - AuditorProblemasLegendaService.java
      Agrega detecção de resíduo em inglês, falas não traduzidas e erros de
      concordância PT-BR.
  - DetectorConcordanciaService.java
      Heurísticas para calques de gênero do inglês: concordância nominal,
      pronomes pessoais/objetos, tratamentos e predicados verbais.
  - ResultadoRevisaoLegendas.java
      (sem cabecalho explicativo)
  - RevisarCacheUseCase.java
      (sem cabecalho explicativo)
  - RevisarLegendasUseCase.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/raspagemRevisao/domain/exceptions/
  - RaspagemRevisaoException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/raspagemRevisao/domain/
  - ResultadoDeteccaoConcordancia.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/raspagemRevisao/
  - RevisorLegendasCLI.java
      Revisa arquivos .ass/.ssa já traduzidos, detecta resíduos em inglês e erros
      de concordância, e corrige via Google Translate.
  - RevisorRaspagemCLI.java
      Revisa falas já traduzidas no cache, corrigindo concordância de gênero,
      pronomes e adjetivos — erros comuns quando o LLM traduz literalmente do inglês.
      Ativado quando {@code app.modo=RASPAGEM_REVISAO}.

[PASTA] src/main/java/org/traducao/projeto/remuxer/application/
  - MapeadorMidiaService.java
      (sem cabecalho explicativo)
  - RemuxarLoteUseCase.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/remuxer/domain/
  - MkvToolNixNaoEncontradoException.java
      (sem cabecalho explicativo)
  - RelatorioRemux.java
      (sem cabecalho explicativo)
  - RemuxerException.java
      (sem cabecalho explicativo)
  - RemuxTarefa.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/remuxer/infrastructure/adapters/
  - MkvmergeAdapter.java
      Tentar os caminhos padrões do Windows

[PASTA] src/main/java/org/traducao/projeto/remuxer/infrastructure/config/
  - RemuxerProperties.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/remuxer/presentation/
  - RemuxerCLI.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/remuxer/presentation/ui/
  - ConsoleRemuxerLogger.java
      Tag colorida em negrito (chama atenção), corpo da mensagem em peso normal
      (mais fácil de ler em blocos de texto maiores) — INFO/DEBUG ficam sem cor.
      Exemplo: [10:20:30] [INFO   ] Mensagem...

[PASTA] src/main/java/org/traducao/projeto/renomearArquivos/application/
  - RenomeadorUseCase.java
      Regex para pegar o episódio de trackers.
      Ex: "[SubsPlease] Nome Anime - 01 (1080p).mkv" -> 01

[PASTA] src/main/java/org/traducao/projeto/renomearArquivos/domain/
  - OperacaoRenomeacao.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/renomearArquivos/presentation/web/
  - RenomearArquivosController.java
      (sem cabecalho explicativo)
  - RenomearArquivosRequest.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/revisaoLore/application/
  - DetectorTermosLoreService.java
      Heuristica leve para priorizar falas com possivel erro de lore/terminologia
      antes de chamar o LLM (nomes em ingles remanescentes, grafias suspeitas, etc.).
  - GerenciadorPromptRevisaoLore.java
      (sem cabecalho explicativo)
  - PromptRevisaoLore.java
      Monta os prompts de sistema e usuario para revisao de terminologia/lore
      (nomes proprios, locais, faccoes, mechas) com base na lore da obra ativa.
  - RevisarLoreUseCase.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/revisaoLore/contexto/
  - ContextoRevisaoLore86.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreDanMachi.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreDanMachiS4.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreDanMachiS5.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreGuiltyCrown.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreGundam0080.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreGundam0083.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreGundam08thMSTeam.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreGundamCCA.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreGundamNT.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreGundamUnicorn.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreGundamZeta.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreGundamZZ.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/revisaoLore/domain/
  - EntradaAuditoriaRevisaoLore.java
      Registro granular, append-only, de cada fala enviada ao LLM na revisão de lore.
  - LogEventoRevisaoLore.java
      Entrada estruturada do log de sessao da revisao de lore (serializavel em JSON).
  - ResultadoDeteccaoLore.java
      (sem cabecalho explicativo)
  - ResultadoRevisaoLore.java
      (sem cabecalho explicativo)
  - RevisaoLoreRelatorioJson.java
      Relatorio completo da revisao de lore em JSON: telemetria, metricas, contexto e log da sessao.

[PASTA] src/main/java/org/traducao/projeto/revisaoLore/domain/exceptions/
  - RevisaoLoreException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/revisaoLore/domain/ports/
  - ProvedorPromptRevisaoLore.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/revisaoLore/infrastructure/
  - RevisaoLoreAuditoriaCache.java
      Cache append-only para mineração posterior das decisões da revisão de lore.
  - RevisaoLoreLogPersistencia.java
      Persiste relatorio e log de sessao da revisao de lore exclusivamente em JSON.

[PASTA] src/main/java/org/traducao/projeto/revisaoLore/presentation/
  - RevisaoLoreController.java
      Fila única compartilhada do pipeline: impede que a revisão de lore rode
      em paralelo com uma tradução/correção e troque o contexto LLM global no
      meio do outro job (ver FilaExecucaoPipeline).

[PASTA] src/main/java/org/traducao/projeto/sistema/application/
  - EncerrarAplicacaoUseCase.java
      Encerra a aplicação de forma ordenada a partir do botão "Sair" da UI.
      <p>
      Sequência: sinaliza parada cooperativa da fila do pipeline (o job em
      execução encerra no próximo ponto seguro, preservando cache e arquivos já
      concluídos), espera um curto período para a resposta HTTP chegar ao
      navegador e então derruba o Quarkus. Se o shutdown normal não terminar o
      processo (ex.: modo dev segura a JVM viva), um fallback força a saída.

[PASTA] src/main/java/org/traducao/projeto/sistema/presentation/
  - SistemaController.java
      Endpoints de controle do processo da aplicação (menu "Sair" da UI).
      Operações de trabalho do pipeline ficam nos controllers de cada módulo;
      aqui entra apenas o ciclo de vida do servidor em si.

[PASTA] src/main/java/org/traducao/projeto/telemetria/
  - AmbienteExecucaoDataset.java
      Fotografia sanitizada do ambiente que gerou o snapshot público.
      Nao inclui usuario, hostname, IP, serial, MAC, caminhos ou IDs de hardware.
  - AmbienteExecucaoDatasetService.java
      Detecta apenas metadados publicáveis do ambiente de execução.
  - LlmTelemetria.java
      (sem cabecalho explicativo)
  - MidiaTelemetria.java
      (sem cabecalho explicativo)
  - OperacaoHistorico.java
      Uma linha da tabela de histórico de operações exibida no painel de Telemetria.
  - OperacaoTelemetria.java
      Registro persistido de operações do pipeline que não passam pelo LLM de tradução
      (revisão de legendas, correção Google, limpeza de cache, etc.).
  - RevisaoLoreTelemetriaResumo.java
      Métricas agregadas das sessões de Revisão de Lore para o painel de Telemetria.
  - TelemetriaDatasetProperties.java
      Configuração do repositório dedicado do dataset público de telemetria
      (seção {@code telemetria-dataset} do application.yml). O nome segue a
      convenção da comunidade para datasets de telemetria:
      {@code [NomeDoSistema]-telemetry-dataset}.
  - TelemetriaDatasetService.java
      Publica a telemetria acumulada como DATASET PÚBLICO num repositório Git
      DEDICADO ({@code kronos-anime-translation-telemetry-dataset}, seguindo a
      convenção {@code [NomeDoSistema]-telemetry-dataset} para dados de pesquisa/ML).
      <p>
      O serviço é auto-suficiente: se o repositório local não existir, ele clona o
      remoto configurado (ou inicializa um novo e associa o remoto); na primeira
      publicação gera README com declaração de anonimização (LGPD/GDPR), LICENSE e
      a estrutura {@code metrics/}. Cada publicação = 1 commit + push, e o
      histórico Git é o versionamento natural dos snapshots.
      <p>
      Sanitização deliberada — o dataset é feito para consumo externo, então
      carrega apenas MÉTRICAS: nada de textos de legenda (os avisos viram
      contagem), nada de caminhos de máquina (o campo {@code detalhe} das
      operações é descartado e nomes de episódio perdem qualquer diretório).
  - TelemetriaResumo.java
      Resumo serializável da telemetria acumulada na sessão atual do servidor,
      consumido pelo painel "Telemetria" da interface web.
  - TelemetriaService.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducaoCorrige/application/
  - LimparCacheUseCase.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducaoCorrige/
  - CorretorCache.java
      Programa Utilitário que realiza a limpeza seletiva do cache de tradução.
      Remove traduções que falharam e foram salvas com o texto original em inglês (fallbacks),
      permitindo que sejam reprocessadas com a nova lógica e prompts corrigidos.
  - CorretorCacheCLI.java
      CommandLineRunner que realiza a limpeza do cache de tradução integrado ao fluxo do Spring.
      Ativado quando a propriedade app.modo é configurada como "CORRIGIR_CACHE".

[PASTA] src/main/java/org/traducao/projeto/traducaoCorrige/domain/exceptions/
  - CorretorCacheException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducaoKaraoke/application/
  - ClassificadorLetraKaraokeService.java
      Decide o destino de cada evento de música: preservar (letra original),
      traduzir (camada em inglês) ou não tocar (efeito KFX / já em PT-BR).
      <p>
      O problema central que este classificador resolve: cantores japoneses
      misturam inglês no meio da letra ("kimi no heart ni fly away"). A heurística
      estrita de romaji do {@link DetectorEfeitoKaraokeService} exige que TODAS as
      palavras sejam silabáveis em japonês — uma única palavra inglesa derruba a
      detecção e a letra original iria ao LLM. Aqui a decisão é por EVIDÊNCIA:
      partículas/palavras japonesas romanizadas inequívocas votam em "original",
      palavras gramaticais inequívocas de inglês votam em "tradução", e o estilo
      do evento (Romaji/JP vs English) decide antes de qualquer análise de texto.
      Em caso de dúvida o viés é PRESERVAR — o mesmo princípio de todo o projeto:
      deixar uma linha sem traduzir custa menos que destruir a letra original.
  - TraduzirKaraokeUseCase.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducaoKaraoke/domain/
  - ClasseLinhaKaraoke.java
      Classificação de cada evento da legenda sob a ótica da tradução de letras
      de música. O ponto delicado é a letra japonesa romanizada com palavras em
      inglês misturadas (comum em música japonesa: "kimi no heart ni fly away") —
      ela é ORIGINAL_JAPONES e nunca pode ir ao LLM, enquanto a camada de
      TRADUÇÃO em inglês da mesma música é TRADUZIVEL_INGLES.
      Não é música: diálogo, placa, Comment. Copiada byte a byte. /
      Efeito KFX (sílaba/frame, alta densidade de tags). Preservado intacto. /
      Letra original (kana/kanji/romaji, mesmo com inglês misturado). Nunca traduz. /
      Letra que já está em português. Nada a fazer. /
      Camada de tradução em inglês da música: é o que este módulo traduz. /
  - ResultadoTraducaoKaraoke.java
      Resumo, por arquivo .ass, do que a tradução de karaokê classificou e fez.
      Alimenta o console da UI, o manifesto de auditoria e a telemetria.
  - TraducaoKaraokeException.java
      Erro de negócio do módulo Tradução de Karaokê (validação de pastas,
      LLM indisponível, falha de leitura/escrita das legendas).

[PASTA] src/main/java/org/traducao/projeto/traducaoKaraoke/infrastructure/
  - TraducaoKaraokePersistencia.java
      Manifesto de auditoria da tradução de karaokê: registra, por execução, o
      que foi preservado/traduzido em cada arquivo. Fica em
      {@code logs/traducao-karaoke/manifestos} — junto com os originais intocados
      na pasta de origem e o cache JSON editável, é a trilha completa para
      auditar (ou refazer) qualquer tradução de letra.

[PASTA] src/main/java/org/traducao/projeto/traducaoKaraoke/presentation/
  - TraducaoKaraokeController.java
      Endpoints do módulo Tradução de Karaokê. A simulação só lê arquivos e roda
      async fora da fila (mesmo padrão do Karaokê Simples); a APLICAÇÃO chama o
      LLM e muda o contexto de lore ativo — estado global —, então
      obrigatoriamente entra na {@link FilaExecucaoPipeline}.
  - TraducaoKaraokeRequest.java
      Corpo das requisições do painel Tradução de Karaokê: a pasta com as
      legendas .ass e a obra (contexto de lore) selecionada na UI.

[PASTA] src/main/java/org/traducao/projeto/traducao/
  - Application.java
      Utilitarios de inicializacao compartilhados entre modos CLI.
      O Quarkus e o container principal; nao ha {@code SpringApplication.run} aqui.

[PASTA] src/main/java/org/traducao/projeto/traducao/application/
  - DetectorEfeitoKaraokeService.java
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
  - DetectorTraducaoIdenticaService.java
      Decide se uma fala pode legitimamente permanecer idêntica ao original (nomes
      próprios, números, siglas, termos de lore) ou se a igualdade é sinal de que o
      LLM simplesmente devolveu a fala sem traduzir.
  - ProcessarArquivoUseCase.java
      Orquestra a tradução de um único arquivo de legenda: le -> reaproveita o
      cache existente -> traduz só o que falta (deduplicando falas repetidas) ->
      valida -> escreve a legenda final em PT-BR -> grava/atualiza o cache.
      <p>
      Correções manuais feitas pelo usuário no JSON de cache são respeitadas na
  - ProcessarEpisodioUseCase.java
      Quantas tentativas extras (alem da primeira) sao feitas numa fala isolada
      (lote de tamanho 1) antes de desistir e manter o texto original sem traducao.
      Temperatura por tentativa numa fala isolada: null = a configurada.
      Repetir a mesma requisicao com a mesma temperatura tende a reproduzir a
      mesma alucinacao; subir a temperatura muda a amostragem e da chance real
      de recuperacao antes de desistir da fala.
  - ProtecaoLegendaAssService.java
      Blindagens compartilhadas para linhas ASS/SSA antes e depois de chamadas a
      IA/serviços externos. Centraliza os casos perigosos encontrados em fansubs:
      clips vetoriais longos, letras soltas pós-template e preâmbulos alucinados.
  - ValidadorTraducaoService.java
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

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/
  - ContextoPrompt.java
      Cada ContextoXxx monta seu PROMPT uma unica vez (campo static final), na
      inicializacao da classe; este mapa guarda a lore "crua" por traz de cada
      prompt completo para que outros usos (ex.: revisao de concordancia) nao
      precisem reenviar o prompt de traducao inteiro - que ja inclui lore +
      RegrasConcordanciaPtBr.BLOCO_TRADUCAO + regras de saida - como se fosse
      so a lore, o que estourava o contexto do LLM (ver MistralClientAdapter).
  - RegrasConcordanciaPtBr.java
      Regras de concordância de gênero, pronomes, tratamentos e verbos aplicáveis a
      qualquer obra — o inglês não marca gênero em adjetivos/participios e usa
      "you" genérico, o que leva o LLM a masculinizar tudo.

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/danmachi/
  - ContextoDanMachi.java
      (sem cabecalho explicativo)
  - ContextoDanMachiOrion.java
      (sem cabecalho explicativo)
  - ContextoDanMachiS1.java
      (sem cabecalho explicativo)
  - ContextoDanMachiS2.java
      (sem cabecalho explicativo)
  - ContextoDanMachiS3.java
      (sem cabecalho explicativo)
  - ContextoDanMachiS4.java
      (sem cabecalho explicativo)
  - ContextoDanMachiS5.java
      (sem cabecalho explicativo)
  - ContextoDanMachiSwordOratoria.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/eightsix/
  - Contexto86.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/evangelion/
  - ContextoEvangelion111.java
      (sem cabecalho explicativo)
  - ContextoEvangelion222.java
      (sem cabecalho explicativo)
  - ContextoEvangelion3010.java
      (sem cabecalho explicativo)
  - ContextoEvangelion333.java
      (sem cabecalho explicativo)
  - ContextoEvangelionTV.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/guiltycrown/
  - ContextoGuiltyCrown.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/gundam/chars/
  - ContextoCharsCounterattack.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/gundam/
  - ContextoGundam0079.java
      (sem cabecalho explicativo)
  - ContextoGundamF91.java
      (sem cabecalho explicativo)
  - ContextoGundamHathaway.java
      (sem cabecalho explicativo)
  - ContextoGundamNT.java
      (sem cabecalho explicativo)
  - ContextoGundamOrigin.java
      (sem cabecalho explicativo)
  - ContextoGundamSEED.java
      (sem cabecalho explicativo)
  - ContextoGundamSEEDAstray.java
      (sem cabecalho explicativo)
  - ContextoGundamSEEDDestiny.java
      (sem cabecalho explicativo)
  - ContextoGundamSEEDFreedom.java
      (sem cabecalho explicativo)
  - ContextoGundamSEEDStargazer.java
      (sem cabecalho explicativo)
  - ContextoGundamUnicorn.java
      (sem cabecalho explicativo)
  - ContextoGundamVictory.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/gundam/msteam/
  - ContextoGundam08thMSTeam.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/gundam/reconguista/
  - ContextoGundamReconguista.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/gundam/stardust/
  - ContextoGundam0083.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/gundam/warInpocket/
  - ContextoWarInPocket.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/gundam/zeta/
  - ContextoGundamZeta.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/gundam/zz/
  - ContextoGundamZZ.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/macross/
  - ContextoMacross2.java
      (sem cabecalho explicativo)
  - ContextoMacross7.java
      (sem cabecalho explicativo)
  - ContextoMacross7Encore.java
      (sem cabecalho explicativo)
  - ContextoMacross7Filme.java
      (sem cabecalho explicativo)
  - ContextoMacross7Filmes.java
      (sem cabecalho explicativo)
  - ContextoMacrossAnime.java
      (sem cabecalho explicativo)
  - ContextoMacrossDelta.java
      (sem cabecalho explicativo)
  - ContextoMacrossDeltaFilme1.java
      (sem cabecalho explicativo)
  - ContextoMacrossDeltaFilme2.java
      (sem cabecalho explicativo)
  - ContextoMacrossDeltaFilmes.java
      (sem cabecalho explicativo)
  - ContextoMacrossDynamite7.java
      (sem cabecalho explicativo)
  - ContextoMacrossDYRL.java
      (sem cabecalho explicativo)
  - ContextoMacrossFilme1.java
      (sem cabecalho explicativo)
  - ContextoMacrossFilme2.java
      (sem cabecalho explicativo)
  - ContextoMacrossFrontier.java
      (sem cabecalho explicativo)
  - ContextoMacrossFrontierFilme1.java
      (sem cabecalho explicativo)
  - ContextoMacrossFrontierFilme2.java
      (sem cabecalho explicativo)
  - ContextoMacrossFrontierFilmes.java
      (sem cabecalho explicativo)
  - ContextoMacrossPlus.java
      (sem cabecalho explicativo)
  - ContextoMacrossZero.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/sidonia/
  - ContextoKnightsOfSidonia.java
      (sem cabecalho explicativo)
  - ContextoSidoniaFilme.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/domain/exceptions/
  - AlucinacaoDetectadaException.java
      (sem cabecalho explicativo)
  - ArquivoLegendaException.java
      (sem cabecalho explicativo)
  - ContextoNaoEncontradoException.java
      (sem cabecalho explicativo)
  - DivergenciaLinhasException.java
      (sem cabecalho explicativo)
  - LlmFalhaComunicacaoException.java
      (sem cabecalho explicativo)
  - LmStudioOfflineException.java
      (sem cabecalho explicativo)
  - RespostaLlmVaziaException.java
      (sem cabecalho explicativo)
  - TraducaoParcialException.java
      Construtor usado pela camada do Episódio (nível de Lotes)
      Construtor usado pela camada de Arquivo (nível de Falas Mascaradas)
  - TradutorException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/domain/legenda/
  - DocumentoLegenda.java
      (sem cabecalho explicativo)
  - EventoLegenda.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/domain/
  - Lote.java
      (sem cabecalho explicativo)
  - StatusLlm.java
      Resultado da checagem de disponibilidade do servidor LLM local (ex: LM Studio)
      feita no início da execução, antes de começar a traduzir qualquer episódio.
  - TraducaoLote.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/domain/ports/
  - MistralPort.java
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
  - ProvedorContexto.java
      Retorna o ID único para seleção via UI.
      Retorna o nome amigável para exibição no combo box da UI.
      Retorna o prompt de sistema completo para o LLM, com regras gerais e lore especifico da midia.

[PASTA] src/main/java/org/traducao/projeto/traducao/infrastructure/adapters/
  - MistralClientAdapter.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/infrastructure/config/
  - LlmProperties.java
      (sem cabecalho explicativo)
  - RestClientConfig.java
      Beans de agregacao e suporte para injecao CDI/Quarkus.
  - TradutorProperties.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/infrastructure/contexto/
  - GerenciadorContexto.java
      Mutado pela thread única do executor de background (ApiController) e lido
      pela mesma thread ao montar o prompt do LLM (MistralClientAdapter). O
      volatile aqui é uma garantia defensiva de visibilidade, não uma alegação
      de que múltiplas threads concorrem por este campo.

[PASTA] src/main/java/org/traducao/projeto/traducao/infrastructure/dtos/
  - RecordsMistral.java
      Shape da API estendida da LM Studio ({@code /api/v0/models}, fora do
      prefixo {@code /v1}), que — diferente do endpoint OpenAI-compatible
      {@code /v1/models} — informa o campo {@code state} ("loaded" /
      "not-loaded"), permitindo saber com certeza qual modelo está de fato
      carregado em memória.

[PASTA] src/main/java/org/traducao/projeto/traducao/infrastructure/http/
  - JsonHttpClient.java
      Cliente HTTP JSON baseado em {@link HttpClient} do JDK (sem Spring RestClient).

[PASTA] src/main/java/org/traducao/projeto/traducao/infrastructure/legenda/
  - EscritorLegendaAss.java
      Reconstroi o arquivo .ass a partir do {@link DocumentoLegenda}, repetindo o
      cabecalho original e as linhas nao traduziveis byte a byte, e so trocando o
      campo Text dos eventos Dialogue pela versao traduzida.
  - LeitorLegendaAss.java
      Le arquivos .ass/.ssa preservando byte a byte tudo que nao for o campo Text
      dos eventos Dialogue (estilos, timestamps, secoes de metadados). So o campo
      Text e exposto para traducao; o resto e reconstruido identico pelo
      {@link EscritorLegendaAss}.
  - MascaradorTags.java
      Isola tags de formatação ASS/SSA (ex: {\i1}, {\pos(...)}) e códigos de quebra
      (\N, \n, \h) do texto antes de enviar ao LLM, trocando-os por marcadores
      [[TAGn]] que o modelo é instruído a preservar literalmente. Sem isso o LLM
      tende a "traduzir" ou descartar as tags, corrompendo a legenda renderizada.

[PASTA] src/main/java/org/traducao/projeto/traducao/presentation/
  - TradutorCLI.java
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

[PASTA] src/main/java/org/traducao/projeto/traducao/presentation/ui/
  - AnsiCores.java
      Cores ANSI compartilhadas entre o prompt interativo e o {@link ConsoleUILogger}.
      Usar apenas caracteres ASCII nos textos do prompt evita problemas de encoding
      no console do Windows (cp1252 vs UTF-8).
  - ConsoleEntrada.java
      Numeração segue a ordem natural do pipeline: primeiro auditar a
      mídia, depois extrair/traduzir/corrigir a legenda e por fim remuxar.
  - ConsoleUILogger.java
      Wrapper thread-safe em torno da barra de progresso (estilo tqdm). Todo
      acesso a {@code pb} e sincronizado porque mensagens podem chegar
      durante a tradução de um episódio.
      <p>
      O console e efêmero (a barra de progresso sobrescreve linhas antigas), por
      isso toda mensagem também é espelhada no logger SLF4J, que persiste em
      arquivo (ver {@code logging.file.name}) e sobrevive para análise posterior.
  - PastasExecucao.java
      Pastas efetivas da execução atual. Preenchidas pelo {@code TradutorCLI} a
      partir do diálogo Swing ou das propriedades/linha de comando.

[PASTA] src/main/java/org/traducao/projeto/traducao/presentation/web/
  - ApiController.java
      (sem cabecalho explicativo)
  - BrowserLauncher.java
      Abre o navegador apos a inicializacao do Quarkus quando {@code app.modo=WEB}.
  - ConsoleRedirector.java
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
  - DialogoArquivoController.java
      Usa OpenFileDialog (UI moderna do Explorer) em vez de FolderBrowserDialog/Shell.Application,
      cuja janela de seleção de pasta ainda usa a interface antiga (estilo Windows 95).
  - DocumentacaoController.java
      Serve o conteúdo bruto das páginas de documentação (pasta {@code docs/} na
      raiz do projeto) para o painel "Documentação" da SPA, que renderiza o
      markdown no navegador (ver static/documentacao/documentacao.js). O README
      raiz é o índice canônico no GitHub; este endpoint espelha a mesma pasta
      docs/ dentro do próprio app, sem precisar sair dele.
  - LogStreamResource.java
      Endpoint SSE nativo do Quarkus (substitui SseEmitter do Spring MVC).
  - LogStreamService.java
      Gerencia conexoes SSE (JAX-RS) e despacha logs em tempo real para clientes web.
  - TelemetriaStreamResource.java
      Endpoint Server-Sent Events (SSE) reativo para streaming da telemetria da KRONOS em tempo real.
      Rota mapeada especificamente para evitar colisões com o Controller Spring.

[PASTA] src/main/java/org/traducao/projeto/trocaTipoLegenda/application/
  - AuditoriaFontesService.java
      Mapeamento de fontes vietnamitas/ANSI problemáticas para Arial como padrão seguro.
  - TrocaTipoLegendaUseCase.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/trocaTipoLegenda/domain/
  - AuditoriaFonteInfo.java
      (sem cabecalho explicativo)
  - AuditoriaLegendaResultado.java
      (sem cabecalho explicativo)
  - EntradaAuditoriaTrocaFonte.java
      (sem cabecalho explicativo)
  - ResultadoGeralAuditoria.java
      (sem cabecalho explicativo)
  - ResultadoTrocaFonte.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/trocaTipoLegenda/domain/exceptions/
  - TrocaTipoLegendaException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/trocaTipoLegenda/infrastructure/
  - TrocaTipoLegendaAuditoriaCache.java
      Cache append-only para gravação de auditoria histórica e granular de cada alteração de fonte aplicada.

[PASTA] src/main/java/org/traducao/projeto/trocaTipoLegenda/presentation/
  - TrocaTipoLegendaController.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/analisadorMidia/application/
  - AnalisarMidiaClassificacaoTest.java
      Cobre o dado VITAL da análise: a classificação do tipo de legenda (codec →
      tipo) e o veredicto de traduzibilidade (texto = traduzível; bitmap = OCR;
      nenhuma = RAW/hardsub). Decide se um episódio segue no pipeline de tradução.

[PASTA] src/test/java/org/traducao/projeto/analisadorMidia/infrastructure/adapters/
  - FfprobeAdapterTest.java
      Cobre o parsing ffprobe-JSON → domínio sem executar ffprobe real: substitui o
      seam de processo externo ({@code executarFfprobeJson}) por JSON canônico e
      verifica container, faixas de vídeo/áudio/legenda e casos-limite.

[PASTA] src/test/java/org/traducao/projeto/
  - ApiControllerTest.java
      (sem cabecalho explicativo)
  - ApiEndpointsTest.java
      (sem cabecalho explicativo)
  - WebInterfaceTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/apiDadosAnime/application/
  - ObterMetadataAnimeUseCaseTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/auditorConteudoLegendas/application/
  - AuditorConteudoUseCaseTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/auditorConteudoLegendas/application/regras/
  - RegraAlucinacaoQuebraLinhaTest.java
      Simulando o erro do anime 86 Ep 2
  - RegraDanoKaraokeTest.java
      Caso real do 86 T1: romaji em estilo "Opening" com tags leves virou
      alucinação em PT — expansão de só 1.7x, que a checagem de tamanho
      não pegaria; a régua tem de ser a proteção de romaji do detector.
  - RegraEfeitoVazadoTest.java
      Simulando o problema de 86 Ep 2, onde um typesetting (como "{\\pos(100,100)}na")
      acaba sendo traduzido de forma louca pela IA.
  - RegraMetadadosAssTest.java
      (sem cabecalho explicativo)
  - RegraSincroniaEstilosTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/auditorConteudoLegendas/support/
  - AssAuditoriaFixtures.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/core/exception/
  - BasePipelineExceptionTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/core/execucao/
  - FilaExecucaoPipelineTest.java
      Cobre o contrato de execução da fila única do pipeline: submissão, execução
      síncrona com propagação de exceção, sinal de ocupação e cancelamento. É a
      invariante que garante que UI, MCP e CLI compartilhem a MESMA política de
      execução sequencial (um job pesado por vez).

[PASTA] src/test/java/org/traducao/projeto/correcaoLegendas/application/
  - CorrigirLegendasUseCaseTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/legendasExtracao/infrastructure/adapters/
  - FfmpegAdapterTest.java
      Cobre a identificação de faixas de legenda em contêineres não-MKV (mp4/mov/…)
      a partir do JSON do {@code ffprobe -show_streams}, sem ffprobe real: substitui
      o seam de processo externo ({@code executarIdentificacao}).
  - MkvToolNixAdapterTest.java
      Cobre a identificação de faixas de legenda a partir do JSON do
      {@code mkvmerge --identify}, sem MKVToolNix real: substitui o seam de processo
      externo ({@code executarIdentificacao}) por saída canônica.

[PASTA] src/test/java/org/traducao/projeto/mapaProjeto/application/
  - GeradorMapaProjetoUseCaseTest.java
      {@code Files.list} (usado por {@code executar}, diferente de
      {@code Files.walk} usado em outros use cases deste projeto) lança
      {@code NotDirectoryException} quando o caminho informado não é um
      diretório — e, ao contrário dos demais use cases, {@code executar} aqui
      não tem nenhuma checagem prévia que intercepte esse caso. Isso o torna o
      único, entre as lacunas de exceção corrigidas nesta auditoria, em que a
      falha real é reproduzível de forma determinística e portátil num teste.

[PASTA] src/test/java/org/traducao/projeto/mcp/
  - KronosMcpToolsTest.java
      Garante que a porta MCP siga a MESMA política de execução da UI: a análise
      roda pela {@link FilaExecucaoPipeline} (não direto), e uma solicitação com a
      fila ocupada é recusada de forma estruturada em vez de rodar em paralelo com
      outro job. Usa um fake do use case — sem ffprobe real.

[PASTA] src/test/java/org/traducao/projeto/novoKaraoke/application/
  - ConversorKaraokeUseCaseTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/raspagemCorrecao/infrastructure/
  - GoogleTranslateScraperTest.java
      Cobre o contrato tipado e o retry curado sem tocar na rede: substitui o
      transporte HTTP ({@code executarGet}) por respostas canônicas e anula a espera
      ({@code dormir}). Verifica o mapeamento de cada desfecho para
      {@link StatusRaspagem} e que só a falha transitória é retentada.

[PASTA] src/test/java/org/traducao/projeto/remuxer/application/
  - MapeadorMidiaServiceTest.java
      Criar arquivos de vídeo MKV com padrão "EpsXX" (como nos arquivos de 86 do usuário)
      Criar arquivos de legenda ASS com padrão "_-_XX" e colchetes

[PASTA] src/test/java/org/traducao/projeto/renomearArquivos/application/
  - RenomeadorUseCaseTest.java
      Ignora

[PASTA] src/test/java/org/traducao/projeto/revisaoLore/application/
  - DetectorTermosLoreServiceTest.java
      (sem cabecalho explicativo)
  - RevisarLoreUseCaseTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/revisaoLore/contexto/
  - ContextosRevisaoLoreCatalogoTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/revisaoLore/infrastructure/
  - RevisaoLoreAuditoriaCacheTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/telemetria/
  - TelemetriaDatasetPropertiesTest.java
      (sem cabecalho explicativo)
  - TelemetriaDatasetServiceTest.java
      O dataset público carrega SOMENTE métricas: sem textos de legenda (avisos
      viram contagem), sem caminhos de máquina (detalhe descartado, episódio
      reduzido ao nome do arquivo). Estes testes são o contrato de anonimização
      declarado no README do repositório do dataset.
  - TelemetriaServiceCompactacaoTest.java
      Teto de avisos por episódio no JSON canônico: sem ele, os textos de aviso
      dominavam o arquivo de telemetria (21,9 mil avisos ≈ 85% dos 3,5MB medidos
      em 2026-07-09) e eram regravados a cada registro.
  - TelemetriaServiceRevisaoLoreTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/traducaoKaraoke/application/
  - ClassificadorLetraKaraokeServiceTest.java
      O caso central do módulo: cantor japonês mistura inglês na letra.
      Estilo real do 86: "ED-ROM".
  - TraduzirKaraokeUseCaseTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/traducao/application/
  - DetectorEfeitoKaraokeServiceTest.java
      Linha real que escapou da revisão: letra "I" afogada em transformações.
      Linha com \pos e fscx/fscy onde o texto visível é curto em relação às tags.
  - ProcessarArquivoUseCaseGuardTest.java
      (sem cabecalho explicativo)
  - ValidadorTraducaoServiceTest.java
      Caso real (Gundam Narrative): LLM rotulou a resposta em vez de só traduzir.
      Caso real (G-Reconguista): marcador do pipeline Python antigo na legenda final.

[PASTA] src/test/java/org/traducao/projeto/traducao/infrastructure/legenda/
  - EscritorLegendaAssTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/traducao/presentation/web/
  - ConsoleRedirectorTest.java
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
  - LogStreamServiceTest.java
      Sem nenhum SSE client conectado, {@code publicarLog} ainda deve persistir a
      linha em {@code logs/console-web.log} (espelho em disco usado por
      {@link ConsoleRedirector} e pelos consoles web). Isso é o que prova que o
      pipeline de publicação/persistência em arquivo funciona independente de
      haver navegador conectado via SSE.

[PASTA] src/test/java/org/traducao/projeto/trocaTipoLegenda/application/
  - AuditoriaFontesServiceTest.java
      Default: Arial (Unicode Seguro)
  - TrocaTipoLegendaUseCaseTest.java
      (sem cabecalho explicativo)


================================================================================
 FIM DO MAPA
================================================================================
