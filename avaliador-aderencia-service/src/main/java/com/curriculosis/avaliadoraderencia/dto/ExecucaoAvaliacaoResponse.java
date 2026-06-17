package com.curriculosis.avaliadoraderencia.dto;

import java.time.Instant;
import java.util.List;

public record ExecucaoAvaliacaoResponse(
        Instant inicio,
        Instant fim,
        int solicitadas,
        int avaliadas,
        List<AvaliacaoAderenciaResultado> resultados
) {
}
