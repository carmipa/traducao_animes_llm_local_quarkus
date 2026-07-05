# Prompt de Criação — KRONOS CORE (Pipeline Industrial de Tradução de Animes)

> Cole TODO o bloco no seu agente de IA. Anexe as imagens da pasta `imagens/` como referência visual.
> IMPORTANTE: o Painel Inicial usa uma **imagem de hero já criada** — NÃO a substitua; apenas reaplique o estilo ao redor dela.

---

Você é um engenheiro frontend sênior. Reconstrua o **frontend web completo** do **KRONOS CORE** — orquestrador industrial (backend Java Quarkus + LLM local) para análise, extração, tradução e remux de legendas de animes. São **13 telas** que compartilham a mesma casca (sidebar) e o mesmo sistema de design "command-center" dark. Siga TODAS as regras.

## 1. Stack
- HTML + CSS + JS puro (ou React se já usar). Sem framework CSS. **Chart.js v4** para a Telemetria. **Material Symbols Outlined** para ícones (nunca emojis). Fontes: `Space Grotesk` (títulos/números), `Inter Tight` (texto/UI), `JetBrains Mono` (caminhos, logs, timestamps).

## 2. Casca
- **Sidebar fixa (262px)**, fundo `linear-gradient(180deg,#0a0f1b,#070a12)`: emblema circular (ampulheta/`hourglass_top` teal) + "KRONOS CORE" / "PIPELINE INDUSTRIAL". Nav (item ativo = fundo teal translúcido + barra vertical teal 3px): Painel Inicial, 1. Análise de Mídia, 2. Extração, 3. Tradução Local, 4. Correção Cache, 6. Revisão de Legendas, 7. Correção de Karaoke, 8. Revisão de Lore, 9. Remuxer, 10. Mapa do Projeto, 11. Telemetria, Documentação, Sobre. Rodapé da sidebar: ponto verde pulsante "Backend Online".
- **Cabeçalho de cada tela:** título + subtítulo à esquerda; badge "CACHE LOCAL / 107 Arquivos" à direita.
- **Rodapé global:** "Desenvolvido por Paulo André Carminati · GitHub Quarkus Repo".

## 3. Design (dark, coeso — NENHUM card branco)
- Página `#05070e`. Cards `linear-gradient(180deg,#0e1420,#0a0e18)`, borda `1px solid rgba(255,255,255,0.07)`, radius 16px. Texto: `#eaf0f8` / `#8b97ad` / `#5b6679`. Accent teal `#2dd4bf`. Cores por operação: Análise teal, Extração rosa `#f472b6`, Tradução verde `#34d399`, Correção âmbar `#f59e0b`, Revisão/Lore roxo `#a78bfa`/`#818cf8`, Remuxer ciano `#38bdf8`. Inputs fundo `#080b13`, borda `rgba(255,255,255,0.10)`.

## 4. Telas
- **Painel Inicial:** 3 cards de status (Orquestrador Java Quarkus ONLINE; LLM Mistral Nemo Local LM STUDIO; Cache 107 Arquivos ATIVO); **hero com a imagem já criada** (overlay escuro à esquerda + "AUTOMATION SUITE / KRONOS CORE / Pipeline Industrial..." — NÃO trocar a imagem); grid "LÓGICA DO FLUXO (PIPELINE)" com 9 cards numerados (01–10) clicáveis que navegam para o módulo.
- **Telas de operação (1,2,3,4,6,7,8,9):** mesma estrutura — (opcional) banner informativo; (opcional) card de anime com capa+nota+ano+sinopse (Tradução/Revisão=DanMachi, Karaoke=86); card de configuração com campos (caminho + botão "Procurar" folder, selects de contexto/formato, checkbox) e botões de ação na cor do módulo; card de log (console dark mono, "Aguardando execução..." ou linhas de log).
- **10. Mapa do Projeto:** botão "Gerar Mapa do Projeto" (roxo) + área de saída markdown.
- **11. Telemetria:** hero "Eficiência do Cache 24.5%" com barra + card de provedor (LM Studio, Latência 141ms, Conectado); 6 KPIs (Erros 8.282, Falas 13.954, Tempo 340ms, Hits 3.416, Arquivos 107, Chamadas LLM 53); 3 painéis (funil de barras Brutas/Únicas/Cache/LLM, doughnut de modelos, barras JVM); e **Histórico Operacional** = tabela com busca, chips de filtro por tipo com contagem, cabeçalho fixo + scroll e paginação (~12/página) sobre 157 operações — NUNCA despejar tudo.
- **Documentação:** sumário lateral (Fundamentos/Pipeline/Módulos) + hero "Documentação Técnica" + grid de cards (Arquitetura, Instalação, Tradução Local, API REST, Solução de Problemas).
- **Sobre:** card de perfil (foto, nome, @carmipa, bio, stats 78/8/16); "Redes & Contato" (chips label|valor coloridos: GitHub, E-mail, LinkedIn, Instagram, YouTube, Discord, Blogger); "Formação" (timeline FIAP/ESAOAB/FMU); "Este Projeto" (descrição + tags JAVA/QUARKUS/LLM LOCAL/MISTRAL).

## 5. Chart.js (dark)
`Chart.defaults.color='#8b97ad'`; grid `rgba(255,255,255,0.05)`; doughnut `borderColor` = fundo do card; destrua/recrie ao entrar/sair da Telemetria.

## 6. Regras finais
- **Nenhum card branco.** A tabela de histórico sempre paginada + scroll + busca sobre o dataset completo. A imagem do hero do Painel Inicial é fixa (já criada) — só reestilizar o entorno. Responsivo; acessível. Código comentado por tela.

Construa as 13 telas seguindo exatamente este documento.
