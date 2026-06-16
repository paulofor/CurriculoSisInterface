package com.curriculosis.analiseoportunidades.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiAnaliseService {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public OpenAiAnaliseService(
            @Value("${openai.api.key:}") String apiKey,
            @Value("${openai.api.key.file:}") String apiKeyFile,
            @Value("${openai.model:gpt-4.1-mini}") String model
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .build();
        this.apiKey = resolverApiKey(apiKey, apiKeyFile);
        this.model = model;
    }

    public String analisarOportunidade(String titulo, String empresa, String descricaoOportunidade, String experienciaProfissional) {
        if (apiKey == null || apiKey.isBlank()) {
            return "Análise de IA não executada: configure openai.api.key para habilitar.";
        }

        String prompt = "Avalie a aderência entre a oportunidade e o perfil profissional em no máximo 4 linhas. " +
                "Responda em português com: nivel_aderencia (alta/media/baixa), pontos_fortes e lacunas. " +
                "Considere como restrição forte que o profissional não possui inglês fluente; se a vaga exigir inglês fluente, informe aderência baixa.\n\n" +
                "Oportunidade: " + titulo + " | Empresa: " + empresa + "\n" +
                "Descrição da vaga: " + descricaoOportunidade + "\n" +
                "Experiência do profissional: " + experienciaProfissional;

        Map<String, Object> payload = Map.of(
                "model", model,
                "input", List.of(Map.of("role", "user", "content", prompt))
        );

        try {
            Map<?, ?> resposta = restClient.post()
                    .uri("/responses")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            Object outputText = resposta != null ? resposta.get("output_text") : null;
            return outputText != null ? outputText.toString() : "Sem retorno textual da análise de IA.";
        } catch (Exception e) {
            return "Análise de IA indisponível no momento: " + e.getMessage();
        }
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
            return "";
        }
    }
}
