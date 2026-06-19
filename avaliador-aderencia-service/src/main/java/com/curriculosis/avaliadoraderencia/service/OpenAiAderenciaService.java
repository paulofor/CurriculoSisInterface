package com.curriculosis.avaliadoraderencia.service;

import com.curriculosis.avaliadoraderencia.dto.AvaliacaoAderenciaResultado;
import com.curriculosis.avaliadoraderencia.dto.OportunidadeBackend;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OpenAiAderenciaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiAderenciaService.class);
    private static final Pattern NOTA_PATTERN = Pattern.compile("nota_aderencia\\s*[:=]\\s*(\\d{1,3})", Pattern.CASE_INSENSITIVE);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final CurriculoReferenciaService curriculoReferenciaService;

    public OpenAiAderenciaService(
            @Value("${openai.api.key:}") String apiKey,
            @Value("${openai.api.key.file:}") String apiKeyFile,
            @Value("${openai.model:gpt-5.2}") String model,
            CurriculoReferenciaService curriculoReferenciaService,
            ObjectMapper objectMapper
    ) {
        this.restClient = RestClient.builder().baseUrl("https://api.openai.com/v1").build();
        this.objectMapper = objectMapper;
        this.apiKey = resolverApiKey(apiKey, apiKeyFile);
        this.model = model;
        this.curriculoReferenciaService = curriculoReferenciaService;
        LOGGER.info(
                "OpenAiAderenciaService inicializado. model={}, apiKeyConfigurada={}, apiKeyFileConfigurado={}",
                model, this.apiKey != null && !this.apiKey.isBlank(), apiKeyFile != null && !apiKeyFile.isBlank()
        );
    }

    public AvaliacaoAderenciaResultado avaliar(OportunidadeBackend oportunidade) {
        if (apiKey == null || apiKey.isBlank()) {
            LOGGER.warn("Avaliação não executada por ausência de chave OpenAI. oportunidadeId={}", oportunidade.id());
            return new AvaliacaoAderenciaResultado(
                    oportunidade.id(), oportunidade.titulo(), oportunidade.empresa(), 0,
                    "Avaliação não executada: configure OPENAI_API_KEY ou OPENAI_API_KEY_FILE.", "SEM_API_KEY"
            );
        }

        String prompt = "Avalie quanto a oportunidade abaixo é aderente ao currículo de referência. " +
                "Responda exclusivamente no JSON definido pelo schema. " +
                "A nota_aderencia deve ser um número inteiro de 0 a 100. " +
                "Considere a exigência de inglês fluente como forte redutor.\n\n" +
                "Currículo de referência: " + curriculoReferenciaService.obterResumoCurriculo() + "\n\n" +
                "Oportunidade: " + texto(oportunidade.titulo()) + "\n" +
                "Empresa: " + texto(oportunidade.empresa()) + "\n" +
                "Descrição: " + texto(oportunidade.descricao());

        Map<String, Object> payload = Map.of(
                "model", model,
                "input", List.of(Map.of("role", "user", "content", prompt)),
                "text", Map.of("format", schemaRespostaAderencia())
        );

        LOGGER.info(
                "Enviando oportunidade para avaliação OpenAI. oportunidadeId={}, model={}, caracteresPrompt={}",
                oportunidade.id(), model, prompt.length()
        );

        try {
            Map<?, ?> resposta = restClient.post()
                    .uri("/responses")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            String outputText = extrairOutputText(resposta).orElse("");
            ResultadoEstruturado resultadoEstruturado = interpretarResultadoEstruturado(outputText);
            int nota = resultadoEstruturado.notaAderencia();
            String analise = resultadoEstruturado.analise();
            LOGGER.info(
                    "Resposta OpenAI processada. oportunidadeId={}, nota={}, caracteresAnalise={}, retornoTextualPresente={}",
                    oportunidade.id(), nota, analise.length(), !outputText.isBlank()
            );
            return new AvaliacaoAderenciaResultado(
                    oportunidade.id(), oportunidade.titulo(), oportunidade.empresa(), nota, analise, "AVALIADA"
            );
        } catch (Exception e) {
            LOGGER.error("Falha na avaliação OpenAI. oportunidadeId={}, model={}, erro={}", oportunidade.id(), model, e.getMessage(), e);
            return new AvaliacaoAderenciaResultado(
                    oportunidade.id(), oportunidade.titulo(), oportunidade.empresa(), 0,
                    "Avaliação indisponível no momento: " + e.getMessage(), "ERRO"
            );
        }
    }

    private Map<String, Object> schemaRespostaAderencia() {
        return Map.of(
                "type", "json_schema",
                "name", "avaliacao_aderencia_oportunidade",
                "strict", true,
                "schema", Map.of(
                        "type", "object",
                        "additionalProperties", false,
                        "required", List.of("nota_aderencia", "justificativa", "riscos"),
                        "properties", Map.of(
                                "nota_aderencia", Map.of(
                                        "type", "integer",
                                        "description", "Nota de aderência da oportunidade ao currículo de referência, de 0 a 100."
                                ),
                                "justificativa", Map.of(
                                        "type", "string",
                                        "description", "Resumo curto em português explicando a aderência."
                                ),
                                "riscos", Map.of(
                                        "type", "string",
                                        "description", "Principais lacunas ou riscos da oportunidade para o currículo."
                                )
                        )
                )
        );
    }

    private Optional<String> extrairOutputText(Map<?, ?> resposta) {
        if (resposta == null) {
            return Optional.empty();
        }
        Object outputText = resposta.get("output_text");
        if (outputText != null && !outputText.toString().isBlank()) {
            return Optional.of(outputText.toString());
        }
        Object output = resposta.get("output");
        if (!(output instanceof List<?> outputItems)) {
            return Optional.empty();
        }
        for (Object outputItem : outputItems) {
            if (!(outputItem instanceof Map<?, ?> outputMap)) {
                continue;
            }
            Object content = outputMap.get("content");
            if (!(content instanceof List<?> contentItems)) {
                continue;
            }
            for (Object contentItem : contentItems) {
                if (!(contentItem instanceof Map<?, ?> contentMap)) {
                    continue;
                }
                Object text = contentMap.get("text");
                if (text != null && !text.toString().isBlank()) {
                    return Optional.of(text.toString());
                }
            }
        }
        return Optional.empty();
    }

    private ResultadoEstruturado interpretarResultadoEstruturado(String outputText) {
        if (outputText == null || outputText.isBlank()) {
            LOGGER.warn("Resposta OpenAI sem texto estruturado para avaliação de aderência.");
            return new ResultadoEstruturado(0, "Sem retorno textual da avaliação de IA.");
        }
        try {
            Map<String, Object> json = objectMapper.readValue(outputText, MAP_TYPE);
            int nota = normalizarNota(json.get("nota_aderencia"));
            String justificativa = texto(json.get("justificativa"));
            String riscos = texto(json.get("riscos"));
            return new ResultadoEstruturado(nota, "justificativa: " + justificativa + "; riscos: " + riscos);
        } catch (Exception e) {
            LOGGER.warn("Não foi possível interpretar JSON estruturado da OpenAI. erro={}", e.getMessage());
            return new ResultadoEstruturado(extrairNota(outputText), outputText);
        }
    }

    private int normalizarNota(Object valor) {
        if (valor instanceof Number number) {
            return Math.max(0, Math.min(100, number.intValue()));
        }
        if (valor != null) {
            try {
                return Math.max(0, Math.min(100, Integer.parseInt(valor.toString())));
            } catch (NumberFormatException e) {
                LOGGER.warn("nota_aderencia estruturada inválida: {}", valor);
            }
        }
        return 0;
    }

    private int extrairNota(String analise) {
        Matcher matcher = NOTA_PATTERN.matcher(analise == null ? "" : analise);
        if (!matcher.find()) {
            LOGGER.warn("Não foi possível extrair nota_aderencia da resposta da OpenAI. caracteresAnalise={}", analise == null ? 0 : analise.length());
            return 0;
        }
        int nota = Integer.parseInt(matcher.group(1));
        return Math.max(0, Math.min(100, nota));
    }

    private String resolverApiKey(String apiKey, String apiKeyFile) {
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }
        if (apiKeyFile == null || apiKeyFile.isBlank()) {
            return "";
        }
        try {
            return Files.readString(Path.of(apiKeyFile)).trim();
        } catch (Exception e) {
            LOGGER.warn("Não foi possível ler OPENAI_API_KEY_FILE em {}: {}", apiKeyFile, e.getMessage());
            return "";
        }
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? "Não informado" : valor;
    }

    private String texto(Object valor) {
        return valor == null || valor.toString().isBlank() ? "Não informado" : valor.toString();
    }

    private record ResultadoEstruturado(int notaAderencia, String analise) {
    }
}
