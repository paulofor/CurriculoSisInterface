package com.curriculosis.analiseoportunidades.service;

import com.curriculosis.analiseoportunidades.dto.OportunidadeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.curriculosis.analiseoportunidades.dto.OportunidadeResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnaliseOportunidadeServiceTest {

    private final AnaliseOportunidadeService service = new AnaliseOportunidadeService(
            new OpenAiAnaliseService("", "", "gpt-5.2"),
            new CurriculoPerfilService(new ObjectMapper(), "docs/curriculos/paulo_forestieri_curriculo_master_inicial.json")
    );

    @Test
    void deveDescartarOportunidadeComTextoTodoEmIngles() {
        OportunidadeRequest request = new OportunidadeRequest(
                "AI Engineer (Remote)",
                "Hire Feed",
                "senior",
                "Job Title: AI Engineer (Remote)\n" +
                        "Work Mode: Fully Remote\n" +
                        "Schedule: Flexible up to 40 hours per week based on project availability\n\n" +
                        "Role Overview:\n" +
                        "We are looking for software engineers across Latin America to join a paid remote AI training program. " +
                        "In this role you will design prompts, evaluate model responses, build software tasks and collaborate with a global team.",
                "Experiência com Java, Spring Boot, arquitetura, integração de APIs e IA generativa.",
                80,
                12000
        );

        OportunidadeResponse response = service.analisar(request);

        assertThat(response.scoreFinal()).isZero();
        assertThat(response.recomendacao()).contains("Ignorada");
        assertThat(response.analiseIa()).contains("vaga exige inglês ou está em inglês");
    }

    @Test
    void naoDeveDescartarOportunidadeEmPortuguesComTermosTecnicosEmIngles() {
        OportunidadeRequest request = new OportunidadeRequest(
                "Engenheiro de Software Java Remoto",
                "Empresa Brasil",
                "senior",
                "Buscamos pessoa desenvolvedora Java para atuar com Spring Boot, cloud, APIs, microservices e integração com modelos de IA. " +
                        "A rotina envolve arquitetura, revisão de código, automação e colaboração com times de produto no Brasil.",
                "Experiência com Java, Spring Boot, arquitetura, integração de APIs e IA generativa.",
                60,
                10000
        );

        OportunidadeResponse response = service.analisar(request);

        assertThat(response.scoreFinal()).isGreaterThan(0);
        assertThat(response.recomendacao()).doesNotContain("Ignorada");
    }
}
