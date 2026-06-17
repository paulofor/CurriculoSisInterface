package com.curriculosis.avaliadoraderencia.dto;

public record AvaliacaoAderenciaResultado(
        Long oportunidadeId,
        String titulo,
        String empresa,
        Integer notaAderencia,
        String analiseIa,
        String status
) {
}
