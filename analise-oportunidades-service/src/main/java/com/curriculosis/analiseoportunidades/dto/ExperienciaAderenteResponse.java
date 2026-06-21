package com.curriculosis.analiseoportunidades.dto;

import java.util.List;

public record ExperienciaAderenteResponse(
        String identificador,
        String resumo,
        List<String> termosAderentes
) {
}
