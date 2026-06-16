package com.curriculosis.analiseoportunidades.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CurriculoPerfilServiceTest {

    @Test
    void deveLerCurriculoEmpacotadoQuandoArquivoNaoExisteNoSistema() {
        CurriculoPerfilService service = new CurriculoPerfilService(
                new ObjectMapper(),
                "diretorio-inexistente/paulo_forestieri_curriculo_master_inicial.json"
        );

        String textoCurriculo = service.obterTextoCurriculo();
        String referencia = service.obterReferencia();

        assertThat(textoCurriculo).containsIgnoringCase("Paulo");
        assertThat(textoCurriculo).containsIgnoringCase("Java");
        assertThat(referencia).contains("paulo_forestieri_curriculo_master_inicial.json");
    }
}
