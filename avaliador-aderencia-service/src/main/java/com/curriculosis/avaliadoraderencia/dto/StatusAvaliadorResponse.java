package com.curriculosis.avaliadoraderencia.dto;

import java.time.Instant;

public record StatusAvaliadorResponse(
        boolean schedulerHabilitado,
        String cron,
        int tamanhoLotePadrao,
        Instant ultimaExecucaoInicio,
        Instant ultimaExecucaoFim,
        int ultimaQuantidadeAvaliada
) {
}
