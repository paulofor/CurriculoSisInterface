package com.curriculosis.analiseoportunidades.dto;

import java.util.List;

public record RankingOportunidadesCurriculoResponse(
        String curriculoReferencia,
        int totalOportunidades,
        List<OportunidadeCurriculoResponse> oportunidades
) {
}
