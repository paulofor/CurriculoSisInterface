package com.curriculosis.analiseoportunidades.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OportunidadeRequest(
        @NotBlank String titulo,
        @NotBlank String empresa,
        @NotBlank String nivel,
        @NotNull @Min(0) @Max(100) Integer compatibilidade,
        @NotNull @Min(0) Integer salarioEstimado
) {
}
