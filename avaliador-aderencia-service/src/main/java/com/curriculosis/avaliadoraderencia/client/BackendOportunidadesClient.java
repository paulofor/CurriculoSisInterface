package com.curriculosis.avaliadoraderencia.client;

import com.curriculosis.avaliadoraderencia.dto.AvaliacaoAderenciaResultado;
import com.curriculosis.avaliadoraderencia.dto.OportunidadeBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class BackendOportunidadesClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackendOportunidadesClient.class);

    private final RestClient restClient;
    private final String backendBaseUrl;
    private final String resultadosPath;

    public BackendOportunidadesClient(
            @Value("${avaliador.backend.base-url}") String backendBaseUrl,
            @Value("${avaliador.backend.resultados-path}") String resultadosPath
    ) {
        this.restClient = RestClient.builder().baseUrl(backendBaseUrl).build();
        this.backendBaseUrl = backendBaseUrl;
        this.resultadosPath = resultadosPath;
    }

    public List<OportunidadeBackend> buscarOportunidadesPendentes(int limite) {
        String filter = "{\"where\":{\"and\":[{\"descricao\":{\"neq\":null}},{\"maisRecente\":1},{\"or\":[{\"statusAderencia\":{\"neq\":\"avaliada\"}},{\"statusAderencia\":null}]}]},\"order\":\"data DESC\",\"limit\":" + limite + "}";
        LOGGER.info("Buscando oportunidades pendentes no backend. baseUrl={}, limite={}, filter={}", backendBaseUrl, limite, filter);
        List<OportunidadeBackend> oportunidades = restClient.get()
                .uri(buildOportunidadesPendentesUri(filter))
                .retrieve()
                .body(new ParameterizedTypeReference<List<OportunidadeBackend>>() {});
        LOGGER.info("Backend retornou {} oportunidades pendentes para avaliação.", oportunidades == null ? 0 : oportunidades.size());
        return oportunidades;
    }

    static URI buildOportunidadesPendentesUri(String filter) {
        return URI.create("/api/OportunidadeLinkedins?filter=" + encodeQueryParam(filter));
    }

    static String encodeQueryParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public void enviarResultado(AvaliacaoAderenciaResultado resultado) {
        LOGGER.info(
                "Enviando resultado de aderência ao backend. oportunidadeId={}, status={}, nota={}, path={}",
                resultado.oportunidadeId(), resultado.status(), resultado.notaAderencia(), resultadosPath
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("oportunidadeId", resultado.oportunidadeId());
        payload.put("titulo", resultado.titulo());
        payload.put("empresa", resultado.empresa());
        payload.put("notaAderencia", resultado.notaAderencia());
        payload.put("analiseIa", resultado.analiseIa());
        payload.put("status", resultado.status());

        restClient.post()
                .uri(resultadosPath)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
        LOGGER.info("Resultado de aderência enviado ao backend com sucesso. oportunidadeId={}", resultado.oportunidadeId());
    }
}
