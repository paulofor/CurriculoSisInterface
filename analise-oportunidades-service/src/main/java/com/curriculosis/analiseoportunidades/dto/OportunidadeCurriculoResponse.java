package com.curriculosis.analiseoportunidades.dto;

import java.util.List;

public record OportunidadeCurriculoResponse(
        String titulo,
        String empresa,
        int scoreFinal,
        String recomendacao,
        List<String> termosAderentes,
        List<String> lacunas,
        String analiseIa
) {
}
