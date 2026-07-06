package org.traducao.projeto.trocaTipoLegenda.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.trocaTipoLegenda.domain.AuditoriaFonteInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AuditoriaFontesService {

    // Mapeamento de fontes vietnamitas/ANSI problemáticas para Arial como padrão seguro.
    private static final Map<String, String> FONTES_PROBLEMATICAS = Map.of(
        ".VnBook-Antiqua", "Arial",
        ".VnArial", "Arial",
        ".VnTimes", "Arial"
    );

    public List<AuditoriaFonteInfo> analisarCabecalho(String cabecalho) {
        if (cabecalho == null || cabecalho.isBlank()) {
            return Collections.emptyList();
        }

        String[] linhas = cabecalho.split("\r\n|\n", -1);
        int indiceStyles = -1;
        for (int i = 0; i < linhas.length; i++) {
            if (linhas[i].trim().equalsIgnoreCase("[V4+ Styles]")) {
                indiceStyles = i;
                break;
            }
        }

        // Se não houver a seção [V4+ Styles], não há estilos para analisar
        if (indiceStyles < 0) {
            return Collections.emptyList();
        }

        int indiceFormat = -1;
        for (int i = indiceStyles + 1; i < linhas.length; i++) {
            String linha = linhas[i].trim();
            // Se encontrar outra seção antes de achar o Format, encerra a busca
            if (linha.startsWith("[") && linha.endsWith("]")) {
                break;
            }
            if (linha.startsWith("Format:")) {
                indiceFormat = i;
                break;
            }
        }

        if (indiceFormat < 0) {
            return Collections.emptyList();
        }

        // Parseando colunas de formato
        String linhaFormat = linhas[indiceFormat];
        String dadosFormat = linhaFormat.substring(linhaFormat.indexOf(':') + 1).trim();
        List<String> colunas = Arrays.stream(dadosFormat.split(","))
            .map(String::trim)
            .toList();

        int indexName = colunas.indexOf("Name");
        int indexFontname = colunas.indexOf("Fontname");

        if (indexName < 0 || indexFontname < 0) {
            // Formato inválido ou inesperado, sem as colunas obrigatórias
            return Collections.emptyList();
        }

        List<AuditoriaFonteInfo> resultado = new ArrayList<>();
        int numColunas = colunas.size();

        for (int i = indiceFormat + 1; i < linhas.length; i++) {
            String linha = linhas[i].trim();
            // Se começar outra seção, a seção de estilos acabou
            if (linha.startsWith("[") && linha.endsWith("]")) {
                break;
            }
            if (linha.startsWith("Style:")) {
                String dadosEstilo = linha.substring(linha.indexOf(':') + 1);
                if (dadosEstilo.startsWith(" ")) {
                    dadosEstilo = dadosEstilo.substring(1);
                }
                
                // Dividir por vírgula limitando pelo número de colunas
                String[] partes = dadosEstilo.split(",", numColunas);
                if (partes.length >= numColunas) {
                    String nomeEstilo = partes[indexName].trim();
                    String nomeFonte = partes[indexFontname].trim();
                    
                    boolean problematica = FONTES_PROBLEMATICAS.containsKey(nomeFonte);
                    String fonteSugerida = FONTES_PROBLEMATICAS.getOrDefault(nomeFonte, nomeFonte);
                    
                    resultado.add(new AuditoriaFonteInfo(nomeEstilo, nomeFonte, fonteSugerida, problematica));
                }
            }
        }

        return resultado;
    }

    public Map<String, String> getFontesProblematicas() {
        return FONTES_PROBLEMATICAS;
    }
}
