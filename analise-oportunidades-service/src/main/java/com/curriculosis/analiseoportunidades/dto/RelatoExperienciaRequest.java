package com.curriculosis.analiseoportunidades.dto;

import java.time.LocalDate;

public record RelatoExperienciaRequest(
        String texto,
        LocalDate dataInicio,
        LocalDate dataTermino,
        String cliente,
        String tituloFuncao
) {
}
