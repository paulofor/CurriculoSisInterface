package com.curriculosis.avaliadoraderencia.service;

import com.curriculosis.avaliadoraderencia.dto.AvaliacaoAderenciaResultado;
import com.curriculosis.avaliadoraderencia.dto.OportunidadeBackend;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OpenAiAderenciaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiAderenciaService.class);
    private static final Pattern NOTA_PATTERN = Pattern.compile("nota_aderencia\\s*[:=]\\s*(\\d{1,3})", Pattern.CASE_INSENSITIVE);

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final CurriculoReferenciaService curriculoReferenciaService;

    public OpenAiAderenciaService(
            @Value("${openai.api.key:}") String apiKey,
            @Value("${openai.api.key.file:}") String apiKeyFile,
            @Value("${openai.model:gpt-5.2}") String model,
            CurriculoReferenciaService curriculoReferenciaService
    ) {
        this.restClient = RestClient.builder().baseUrl("https://api.openai.com/v1").build();
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
                "Responda em português no formato: nota_aderencia: N; justificativa: texto curto; riscos: texto curto. " +
                "A nota deve ser um número inteiro de 0 a 100. Considere a exigência de inglês fluente como forte redutor.\n\n" +
                "Currículo de referência: " + curriculoReferenciaService.obterResumoCurriculo() + "\n\n" +
                "Oportunidade: " + texto(oportunidade.titulo()) + "\n" +
                "Empresa: " + texto(oportunidade.empresa()) + "\n" +
                "Descrição: " + texto(oportunidade.descricao());

        Map<String, Object> payload = Map.of(
                "model", model,
                "input", List.of(Map.of("role", "user", "content", prompt))
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

            String analise = resposta != null && resposta.get("output_text") != null
                    ? resposta.get("output_text").toString()
                    : "Sem retorno textual da avaliação de IA.";
            int nota = extrairNota(analise);
            LOGGER.info(
                    "Resposta OpenAI processada. oportunidadeId={}, nota={}, caracteresAnalise={}, retornoTextualPresente={}",
                    oportunidade.id(), nota, analise.length(), resposta != null && resposta.get("output_text") != null
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
}
