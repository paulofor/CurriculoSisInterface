package com.curriculosis.analiseoportunidades.service;

import com.curriculosis.analiseoportunidades.dto.OportunidadeRequest;
import com.curriculosis.analiseoportunidades.dto.OportunidadeResponse;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AnaliseOportunidadeService {

    private final OpenAiAnaliseService openAiAnaliseService;

    public AnaliseOportunidadeService(OpenAiAnaliseService openAiAnaliseService) {
        this.openAiAnaliseService = openAiAnaliseService;
    }

    public OportunidadeResponse analisar(OportunidadeRequest request) {
        int pesoNivel = switch (request.nivel().toLowerCase()) {
            case "junior" -> 10;
            case "pleno" -> 20;
            case "senior" -> 30;
            default -> 15;
        };

        int scoreAderenciaExperiencia = calcularAderenciaPorExperiencia(
                request.experienciaProfissional(),
                request.descricaoOportunidade()
        );

        int scoreSalario = Math.min(request.salarioEstimado() / 500, 25);
        int scoreFinal = Math.min(request.compatibilidade() + pesoNivel + scoreSalario + scoreAderenciaExperiencia, 100);

        String recomendacao;
        if (scoreFinal >= 80) {
            recomendacao = "Alta prioridade: oportunidade muito aderente ao histórico profissional";
        } else if (scoreFinal >= 60) {
            recomendacao = "Boa oportunidade: perfil parcialmente aderente";
        } else {
            recomendacao = "Baixa aderência: pouca convergência com as experiências cadastradas";
        }

        String analiseIa = openAiAnaliseService.analisarOportunidade(
                request.titulo(),
                request.empresa(),
                request.descricaoOportunidade(),
                request.experienciaProfissional()
        );

        return new OportunidadeResponse(request.titulo(), request.empresa(), scoreFinal, recomendacao, analiseIa);
    }

    private int calcularAderenciaPorExperiencia(String experienciaProfissional, String descricaoOportunidade) {
        Set<String> termosExperiencia = tokenizar(experienciaProfissional);
        Set<String> termosOportunidade = tokenizar(descricaoOportunidade);

        if (termosExperiencia.isEmpty() || termosOportunidade.isEmpty()) {
            return 0;
        }

        long intersecao = termosExperiencia.stream().filter(termosOportunidade::contains).count();
        double proporcao = (double) intersecao / termosOportunidade.size();

        return (int) Math.min(Math.round(proporcao * 25), 25);
    }

    private Set<String> tokenizar(String texto) {
        return Arrays.stream(texto.toLowerCase().split("[^a-z0-9áàâãéêíóôõúç]+"))
                .filter(token -> token.length() > 2)
                .collect(Collectors.toSet());
    }
}
