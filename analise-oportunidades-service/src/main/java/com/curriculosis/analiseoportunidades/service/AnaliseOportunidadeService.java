package com.curriculosis.analiseoportunidades.service;

import com.curriculosis.analiseoportunidades.dto.OportunidadeCurriculoRequest;
import com.curriculosis.analiseoportunidades.dto.OportunidadeCurriculoResponse;
import com.curriculosis.analiseoportunidades.dto.OportunidadeRequest;
import com.curriculosis.analiseoportunidades.dto.OportunidadeResponse;
import com.curriculosis.analiseoportunidades.dto.RankingOportunidadesCurriculoRequest;
import com.curriculosis.analiseoportunidades.dto.RankingOportunidadesCurriculoResponse;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AnaliseOportunidadeService {

    private static final Set<String> STOP_WORDS = Set.of(
            "para", "com", "uma", "das", "dos", "por", "que", "the", "and", "em", "de", "do", "da", "no", "na",
            "ao", "as", "os", "ou", "se", "ser", "ter", "mais", "como", "seu", "sua", "vaga", "perfil"
    );

    private final OpenAiAnaliseService openAiAnaliseService;
    private final CurriculoPerfilService curriculoPerfilService;

    public AnaliseOportunidadeService(OpenAiAnaliseService openAiAnaliseService, CurriculoPerfilService curriculoPerfilService) {
        this.openAiAnaliseService = openAiAnaliseService;
        this.curriculoPerfilService = curriculoPerfilService;
    }

    public OportunidadeResponse analisar(OportunidadeRequest request) {
        if (deveIgnorarPorIngles(request.titulo(), request.descricaoOportunidade())) {
            return new OportunidadeResponse(
                    request.titulo(),
                    request.empresa(),
                    0,
                    "Ignorada: oportunidade em inglês ou com exigência de inglês fluente",
                    "Análise de IA não executada porque a vaga exige inglês ou está em inglês."
            );
        }

        int scoreFinal = calcularScoreFinal(
                request.nivel(),
                request.experienciaProfissional(),
                request.descricaoOportunidade(),
                request.compatibilidade(),
                request.salarioEstimado()
        );

        String analiseIa = openAiAnaliseService.analisarOportunidade(
                request.titulo(),
                request.empresa(),
                request.descricaoOportunidade(),
                request.experienciaProfissional()
        );

        return new OportunidadeResponse(request.titulo(), request.empresa(), scoreFinal, recomendar(scoreFinal), analiseIa);
    }

    public RankingOportunidadesCurriculoResponse ranquearPorCurriculo(RankingOportunidadesCurriculoRequest request) {
        String textoCurriculo = curriculoPerfilService.obterTextoCurriculo();
        List<OportunidadeCurriculoResponse> oportunidades = request.oportunidades().stream()
                .map(oportunidade -> analisarComCurriculo(oportunidade, textoCurriculo))
                .sorted(Comparator.comparingInt(OportunidadeCurriculoResponse::scoreFinal).reversed())
                .toList();

        return new RankingOportunidadesCurriculoResponse(
                curriculoPerfilService.obterReferencia(),
                oportunidades.size(),
                oportunidades
        );
    }

    private OportunidadeCurriculoResponse analisarComCurriculo(OportunidadeCurriculoRequest request, String textoCurriculo) {
        if (deveIgnorarPorIngles(request.titulo(), request.descricaoOportunidade())) {
            return new OportunidadeCurriculoResponse(
                    request.titulo(),
                    request.empresa(),
                    0,
                    "Ignorada: oportunidade em inglês ou com exigência de inglês fluente",
                    List.of(),
                    List.of("inglês"),
                    "Análise de IA não executada porque a vaga exige inglês ou está em inglês."
            );
        }

        int scoreFinal = calcularScoreFinal(
                request.nivel(),
                textoCurriculo,
                request.descricaoOportunidade(),
                request.compatibilidade(),
                request.salarioEstimado()
        );
        Set<String> termosCurriculo = tokenizar(textoCurriculo);
        Set<String> termosOportunidade = tokenizar(request.descricaoOportunidade());

        List<String> termosAderentes = termosOportunidade.stream()
                .filter(termosCurriculo::contains)
                .sorted()
                .limit(20)
                .toList();
        List<String> lacunas = termosOportunidade.stream()
                .filter(termo -> !termosCurriculo.contains(termo))
                .sorted()
                .limit(12)
                .toList();

        String analiseIa = openAiAnaliseService.analisarOportunidade(
                request.titulo(),
                request.empresa(),
                request.descricaoOportunidade(),
                textoCurriculo
        );

        return new OportunidadeCurriculoResponse(
                request.titulo(),
                request.empresa(),
                scoreFinal,
                recomendar(scoreFinal),
                termosAderentes,
                lacunas,
                analiseIa
        );
    }

    private int calcularScoreFinal(String nivel, String experienciaProfissional, String descricaoOportunidade, int compatibilidade, int salarioEstimado) {
        int pesoNivel = switch ((nivel == null ? "" : nivel).toLowerCase()) {
            case "junior" -> 10;
            case "pleno" -> 20;
            case "senior", "sênior" -> 30;
            default -> 15;
        };

        int scoreAderenciaExperiencia = calcularAderenciaPorExperiencia(experienciaProfissional, descricaoOportunidade);
        int scoreSalario = Math.min(salarioEstimado / 500, 25);
        return Math.min(compatibilidade + pesoNivel + scoreSalario + scoreAderenciaExperiencia, 100);
    }

    private String recomendar(int scoreFinal) {
        if (scoreFinal >= 80) {
            return "Alta prioridade: oportunidade muito aderente ao histórico profissional";
        } else if (scoreFinal >= 60) {
            return "Boa oportunidade: perfil parcialmente aderente";
        }
        return "Baixa aderência: pouca convergência com as experiências cadastradas";
    }

    private int calcularAderenciaPorExperiencia(String experienciaProfissional, String descricaoOportunidade) {
        Set<String> termosExperiencia = tokenizar(experienciaProfissional);
        Set<String> termosOportunidade = tokenizar(descricaoOportunidade);

        if (termosExperiencia.isEmpty() || termosOportunidade.isEmpty()) {
            return 0;
        }

        long intersecao = termosExperiencia.stream().filter(termosOportunidade::contains).count();
        double proporcao = (double) intersecao / termosOportunidade.size();

        return (int) Math.min(Math.round(proporcao * 25), 25);
    }

    private Set<String> tokenizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(texto.toLowerCase().split("[^a-z0-9áàâãéêíóôõúç]+"))
                .filter(token -> token.length() > 2)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(Collectors.toSet());
    }

    private boolean deveIgnorarPorIngles(String titulo, String descricaoOportunidade) {
        String textoOriginal = ((titulo == null ? "" : titulo) + " " + (descricaoOportunidade == null ? "" : descricaoOportunidade)).trim();
        if (textoOriginal.isBlank()) {
            return false;
        }

        String texto = textoOriginal.toLowerCase();
        if (texto.contains("inglês fluente")
                || texto.contains("ingles fluente")
                || texto.contains("inglês avançado")
                || texto.contains("ingles avançado")
                || texto.contains("advanced english")
                || texto.contains("fluent english")
                || texto.contains("english fluency")
                || texto.contains("professional english")
                || texto.contains("business english")
                || texto.contains("must speak english")
                || texto.contains("required english")
                || texto.contains("english required")) {
            return true;
        }

        return pareceTextoEmIngles(textoOriginal);
    }

    private boolean pareceTextoEmIngles(String textoOriginal) {
        Set<String> termos = tokenizar(textoOriginal);
        if (termos.size() < 8) {
            return false;
        }

        Set<String> marcadoresIngles = Set.of(
                "job", "remote", "responsibilities", "requirements", "required", "experience", "skills",
                "developer", "engineer", "software", "team", "work", "years", "strong", "knowledge",
                "ability", "build", "design", "develop", "systems", "applications", "cloud", "english"
        );

        long ocorrencias = termos.stream().filter(marcadoresIngles::contains).count();
        double proporcao = (double) ocorrencias / termos.size();
        return ocorrencias >= 5 && proporcao >= 0.12;
    }
}
