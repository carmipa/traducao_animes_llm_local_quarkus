package org.traducao.projeto.renomearArquivos.presentation.web;

public record RenomearArquivosRequest(
    String caminhoOrigem,
    String nomePadrao
) {}
