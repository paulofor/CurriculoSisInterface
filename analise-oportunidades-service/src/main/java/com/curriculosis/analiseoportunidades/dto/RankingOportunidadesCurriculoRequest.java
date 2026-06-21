package com.curriculosis.analiseoportunidades.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RankingOportunidadesCurriculoRequest(
        @NotEmpty List<@Valid OportunidadeCurriculoRequest> oportunidades,
        List<RelatoExperienciaRequest> relatosExperiencia
) {
}
