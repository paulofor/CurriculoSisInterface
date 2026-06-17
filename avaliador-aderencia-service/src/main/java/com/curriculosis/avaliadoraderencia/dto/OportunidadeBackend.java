package com.curriculosis.avaliadoraderencia.dto;

public record OportunidadeBackend(
        Long id,
        String titulo,
        String empresa,
        String descricao,
        String url,
        String modelo,
        String data
) {
}
