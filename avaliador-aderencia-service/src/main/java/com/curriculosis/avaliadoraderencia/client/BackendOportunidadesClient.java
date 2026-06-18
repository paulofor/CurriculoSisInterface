package com.curriculosis.avaliadoraderencia.client;

import com.curriculosis.avaliadoraderencia.dto.AvaliacaoAderenciaResultado;
import com.curriculosis.avaliadoraderencia.dto.OportunidadeBackend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class BackendOportunidadesClient {

    private final RestClient restClient;
    private final String resultadosPath;

    public BackendOportunidadesClient(
            @Value("${avaliador.backend.base-url}") String backendBaseUrl,
            @Value("${avaliador.backend.resultados-path}") String resultadosPath
    ) {
        this.restClient = RestClient.builder().baseUrl(backendBaseUrl).build();
        this.resultadosPath = resultadosPath;
    }

    public List<OportunidadeBackend> buscarOportunidadesPendentes(int limite) {
        String filter = "{\"where\":{\"and\":[{\"descricao\":{\"neq\":null}},{\"maisRecente\":1}]},\"order\":\"data DESC\",\"limit\":" + limite + "}";
        return restClient.get()
                .uri("/api/OportunidadeLinkedins?filter=" + encodeQueryParam(filter))
                .retrieve()
                .body(new ParameterizedTypeReference<List<OportunidadeBackend>>() {});
    }

    static String encodeQueryParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public void enviarResultado(AvaliacaoAderenciaResultado resultado) {
        restClient.post()
                .uri(resultadosPath)
                .body(Map.of("resultado", resultado))
                .retrieve()
                .toBodilessEntity();
    }
}
