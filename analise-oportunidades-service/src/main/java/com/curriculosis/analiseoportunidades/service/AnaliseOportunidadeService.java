package com.curriculosis.analiseoportunidades.service;

import com.curriculosis.analiseoportunidades.dto.OportunidadeRequest;
import com.curriculosis.analiseoportunidades.dto.OportunidadeResponse;
import org.springframework.stereotype.Service;

@Service
public class AnaliseOportunidadeService {

    public OportunidadeResponse analisar(OportunidadeRequest request) {
        int pesoNivel = switch (request.nivel().toLowerCase()) {
            case "junior" -> 10;
            case "pleno" -> 20;
            case "senior" -> 30;
            default -> 15;
        };

        int scoreSalario = Math.min(request.salarioEstimado() / 500, 25);
        int scoreFinal = Math.min(request.compatibilidade() + pesoNivel + scoreSalario, 100);

        String recomendacao;
        if (scoreFinal >= 80) {
            recomendacao = "Alta prioridade para candidatura";
        } else if (scoreFinal >= 60) {
            recomendacao = "Boa oportunidade, vale acompanhar";
        } else {
            recomendacao = "Baixa aderência ao perfil atual";
        }

        return new OportunidadeResponse(request.titulo(), request.empresa(), scoreFinal, recomendacao);
    }
}
