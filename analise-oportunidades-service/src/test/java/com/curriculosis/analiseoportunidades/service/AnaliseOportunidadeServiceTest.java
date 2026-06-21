package com.curriculosis.analiseoportunidades.service;

import com.curriculosis.analiseoportunidades.dto.OportunidadeCurriculoRequest;
import com.curriculosis.analiseoportunidades.dto.OportunidadeRequest;
import com.curriculosis.analiseoportunidades.dto.RankingOportunidadesCurriculoRequest;
import com.curriculosis.analiseoportunidades.dto.RankingOportunidadesCurriculoResponse;
import com.curriculosis.analiseoportunidades.dto.RelatoExperienciaRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.curriculosis.analiseoportunidades.dto.OportunidadeResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

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

    @Test
    void deveUsarRelatosRecentesNoRankingPorCurriculo() {
        RankingOportunidadesCurriculoRequest request = new RankingOportunidadesCurriculoRequest(
                List.of(new OportunidadeCurriculoRequest(
                        "Engenheiro de Dados Spark",
                        "Banco Digital",
                        "senior",
                        "Vaga para criar pipelines com Spark, Databricks, Hadoop e Python para produtos financeiros.",
                        40,
                        0
                )),
                List.of(new RelatoExperienciaRequest(
                        "Atuação recente com machine learning, Spark, Databricks, Hadoop, Python e produtos financeiros no Banco do Brasil.",
                        LocalDate.now().minusMonths(12),
                        null,
                        "Banco do Brasil",
                        "Engenheiro de Dados"
                ))
        );

        RankingOportunidadesCurriculoResponse response = service.ranquearPorCurriculo(request);

        assertThat(response.curriculoReferencia()).contains("relato(s) recentes ponderados");
        assertThat(response.oportunidades()).hasSize(1);
        assertThat(response.oportunidades().get(0).termosAderentes()).contains("spark", "databricks", "hadoop", "python");
        assertThat(response.oportunidades().get(0).experienciasAderentes()).hasSize(1);
        assertThat(response.oportunidades().get(0).experienciasAderentes().get(0).resumo()).contains("Banco do Brasil");
    }

    @Test
    void devePriorizarTermosFortesEmRelatosRecentes() {
        RankingOportunidadesCurriculoRequest request = new RankingOportunidadesCurriculoRequest(
                List.of(
                        new OportunidadeCurriculoRequest(
                                "Pessoa Engenheira de Dados",
                                "Fintech A",
                                "senior",
                                "Vaga com Python, Spark e Databricks para dados financeiros.",
                                40,
                                0
                        ),
                        new OportunidadeCurriculoRequest(
                                "Pessoa Java",
                                "Empresa B",
                                "senior",
                                "Vaga com Java, Spring e APIs REST.",
                                40,
                                0
                        )
                ),
                List.of(new RelatoExperienciaRequest(
                        "Projeto recente com Python, Spark, Databricks e dados financeiros.",
                        LocalDate.now().minusMonths(6),
                        null,
                        "Banco",
                        "Engenheiro de Dados"
                ))
        );

        RankingOportunidadesCurriculoResponse response = service.ranquearPorCurriculo(request);

        assertThat(response.oportunidades().get(0).titulo()).isEqualTo("Pessoa Engenheira de Dados");
        assertThat(response.oportunidades().get(0).termosAderentes()).contains("python", "spark", "databricks");
        assertThat(response.oportunidades().get(0).experienciasAderentes().get(0).termosAderentes()).contains("python", "spark", "databricks");
    }

}
