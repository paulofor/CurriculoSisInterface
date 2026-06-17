package com.curriculosis.avaliadoraderencia.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ExecutarAvaliacaoRequest(
        @Min(1) @Max(50) Integer limite
) {
    public int limiteOuPadrao(int padrao) {
        return limite == null ? padrao : limite;
    }
}
