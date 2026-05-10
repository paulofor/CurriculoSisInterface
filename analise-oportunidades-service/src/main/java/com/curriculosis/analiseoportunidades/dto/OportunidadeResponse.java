package com.curriculosis.analiseoportunidades.dto;

public record OportunidadeResponse(
        String titulo,
        String empresa,
        int scoreFinal,
        String recomendacao
) {
}
