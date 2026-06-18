package com.curriculosis.avaliadoraderencia.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BackendOportunidadesClientTest {

    @Test
    void encodeQueryParamEscapaJsonFilterParaEvitarExpansaoDeTemplateUri() {
        String filter = "{\"where\":{\"descricao\":{\"neq\":null}},\"order\":\"data DESC\"}";

        String encoded = BackendOportunidadesClient.encodeQueryParam(filter);

        assertThat(encoded)
                .doesNotContain("{")
                .doesNotContain("}")
                .contains("%7B%22where%22%3A")
                .contains("data+DESC");
    }
}
