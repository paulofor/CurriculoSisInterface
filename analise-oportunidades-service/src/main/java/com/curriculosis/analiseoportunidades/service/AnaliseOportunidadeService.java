package com.curriculosis.analiseoportunidades.service;

import com.curriculosis.analiseoportunidades.dto.ExperienciaAderenteResponse;
import com.curriculosis.analiseoportunidades.dto.OportunidadeCurriculoRequest;
import com.curriculosis.analiseoportunidades.dto.OportunidadeCurriculoResponse;
import com.curriculosis.analiseoportunidades.dto.OportunidadeRequest;
import com.curriculosis.analiseoportunidades.dto.OportunidadeResponse;
import com.curriculosis.analiseoportunidades.dto.RankingOportunidadesCurriculoRequest;
import com.curriculosis.analiseoportunidades.dto.RankingOportunidadesCurriculoResponse;
import com.curriculosis.analiseoportunidades.dto.RelatoExperienciaRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AnaliseOportunidadeService {

    private static final Set<String> STOP_WORDS = Set.of(
            "para", "com", "uma", "das", "dos", "por", "que", "the", "and", "em", "de", "do", "da", "no", "na",
            "ao", "as", "os", "ou", "se", "ser", "ter", "mais", "como", "seu", "sua", "vaga", "perfil"
    );
    private static final Set<String> TERMOS_FORTES = Set.of(
            "java", "spring", "boot", "python", "spark", "hadoop", "databricks", "jupyter", "machine", "learning",
            "etl", "rest", "api", "apis", "microservices", "microsserviços", "sql", "aws", "cloud", "docker",
            "kubernetes", "jenkins", "git", "angular", "javascript", "typescript", "banco", "financeiros", "dados"
    );
    private static final Set<String> LACUNAS_FORTES = Set.of(
            "python", "spark", "databricks", "hadoop", "java", "spring", "aws", "cloud", "docker", "kubernetes", "sql"
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
        PerfilProfissional perfilProfissional = montarPerfilProfissional(request.relatosExperiencia());
        List<OportunidadeCurriculoResponse> oportunidades = request.oportunidades().stream()
                .map(oportunidade -> analisarComCurriculo(oportunidade, perfilProfissional))
                .sorted(Comparator.comparingInt(OportunidadeCurriculoResponse::scoreFinal).reversed())
                .toList();

        return new RankingOportunidadesCurriculoResponse(
                montarReferencia(request.relatosExperiencia()),
                oportunidades.size(),
                oportunidades
        );
    }

    private PerfilProfissional montarPerfilProfissional(List<RelatoExperienciaRequest> relatosExperiencia) {
        String textoCurriculo = curriculoPerfilService.obterTextoCurriculo();
        Map<String, Double> pesosTermos = new HashMap<>();
        adicionarTermosComPeso(pesosTermos, textoCurriculo, 1.0);

        if (relatosExperiencia == null || relatosExperiencia.isEmpty()) {
            return new PerfilProfissional(textoCurriculo, pesosTermos, List.of());
        }

        List<RelatoProfissional> relatosProfissionais = relatosExperiencia.stream()
                .filter(relato -> relato != null && relato.texto() != null && !relato.texto().isBlank())
                .map(relato -> {
                    double pesoRecencia = calcularPesoRecencia(relato);
                    adicionarTermosComPeso(pesosTermos, relato.texto(), pesoRecencia);
                    return new RelatoProfissional(
                            montarIdentificadorRelato(relato),
                            montarResumoOtimizado(relato),
                            tokenizar(relato.texto()),
                            pesoRecencia
                    );
                })
                .toList();
        String textoRelatos = relatosProfissionais.stream()
                .map(RelatoProfissional::resumo)
                .collect(Collectors.joining(" "));

        if (textoRelatos.isBlank()) {
            return new PerfilProfissional(textoCurriculo, pesosTermos, List.of());
        }

        return new PerfilProfissional(
                "Resumos otimizados dos relatos recentes da tela de experiência profissional: " + textoRelatos + " Currículo mestre: " + textoCurriculo,
                pesosTermos,
                relatosProfissionais
        );
    }

    private void adicionarTermosComPeso(Map<String, Double> pesosTermos, String texto, double pesoBase) {
        tokenizar(texto).forEach(termo -> {
            double multiplicadorTermoForte = TERMOS_FORTES.contains(termo) ? 1.6 : 1.0;
            pesosTermos.merge(termo, pesoBase * multiplicadorTermoForte, Double::sum);
        });
    }

    private double calcularPesoRecencia(RelatoExperienciaRequest relato) {
        LocalDate dataReferencia = relato.dataTermino() != null ? relato.dataTermino() : relato.dataInicio();
        if (dataReferencia == null) {
            return 1.5;
        }

        long meses = ChronoUnit.MONTHS.between(dataReferencia, LocalDate.now());
        if (meses <= 36) {
            return 3.0;
        }
        if (meses <= 72) {
            return 2.0;
        }
        return 1.25;
    }

    private String montarIdentificadorRelato(RelatoExperienciaRequest relato) {
        return Stream.of(relato.cliente(), relato.tituloFuncao())
                .filter(valor -> valor != null && !valor.isBlank())
                .collect(Collectors.joining(" - "));
    }

    private String montarResumoOtimizado(RelatoExperienciaRequest relato) {
        String identificador = montarIdentificadorRelato(relato);
        String periodo = Stream.of(relato.dataInicio(), relato.dataTermino())
                .filter(data -> data != null)
                .map(LocalDate::toString)
                .collect(Collectors.joining(" até "));
        String cabecalho = Stream.of(identificador, periodo)
                .filter(valor -> valor != null && !valor.isBlank())
                .collect(Collectors.joining(" | "));
        String texto = relato.texto().replaceAll("\\s+", " ").trim();
        if (texto.length() > 700) {
            texto = texto.substring(0, 700) + "...";
        }
        return cabecalho.isBlank() ? texto : cabecalho + ": " + texto;
    }

    private String montarReferencia(List<RelatoExperienciaRequest> relatosExperiencia) {
        String referencia = curriculoPerfilService.obterReferencia();
        long totalRelatos = relatosExperiencia == null ? 0 : relatosExperiencia.stream()
                .filter(relato -> relato != null && relato.texto() != null && !relato.texto().isBlank())
                .count();

        if (totalRelatos == 0) {
            return referencia;
        }

        return referencia + " + " + totalRelatos + " relato(s) recentes ponderados da tela Experiência Profissional";
    }

    private OportunidadeCurriculoResponse analisarComCurriculo(OportunidadeCurriculoRequest request, PerfilProfissional perfilProfissional) {
        if (deveIgnorarPorIngles(request.titulo(), request.descricaoOportunidade())) {
            return new OportunidadeCurriculoResponse(
                    request.titulo(),
                    request.empresa(),
                    0,
                    "Ignorada: oportunidade em inglês ou com exigência de inglês fluente",
                    List.of(),
                    List.of("inglês"),
                    List.of(),
                    "Análise de IA não executada porque a vaga exige inglês ou está em inglês."
            );
        }

        int scoreFinal = calcularScoreFinal(
                request.nivel(),
                perfilProfissional,
                request.descricaoOportunidade(),
                request.compatibilidade(),
                request.salarioEstimado()
        );
        Set<String> termosOportunidade = tokenizar(request.descricaoOportunidade());

        List<String> termosAderentes = termosOportunidade.stream()
                .filter(perfilProfissional.pesosTermos()::containsKey)
                .sorted()
                .limit(20)
                .toList();
        List<String> lacunas = termosOportunidade.stream()
                .filter(termo -> !perfilProfissional.pesosTermos().containsKey(termo))
                .sorted(Comparator.comparing((String termo) -> !LACUNAS_FORTES.contains(termo)).thenComparing(termo -> termo))
                .limit(12)
                .toList();

        List<ExperienciaAderenteResponse> experienciasAderentes = montarExperienciasAderentes(perfilProfissional, termosOportunidade);

        String analiseIa = openAiAnaliseService.analisarOportunidade(
                request.titulo(),
                request.empresa(),
                request.descricaoOportunidade(),
                perfilProfissional.texto()
        );

        return new OportunidadeCurriculoResponse(
                request.titulo(),
                request.empresa(),
                scoreFinal,
                recomendar(scoreFinal),
                termosAderentes,
                lacunas,
                experienciasAderentes,
                analiseIa
        );
    }

    private List<ExperienciaAderenteResponse> montarExperienciasAderentes(PerfilProfissional perfilProfissional, Set<String> termosOportunidade) {
        return perfilProfissional.relatos().stream()
                .map(relato -> {
                    List<String> termosAderentes = relato.termos().stream()
                            .filter(termosOportunidade::contains)
                            .sorted()
                            .limit(10)
                            .toList();
                    return new ExperienciaAderente(relato.identificador(), relato.resumo(), termosAderentes, termosAderentes.size() * relato.pesoRecencia());
                })
                .filter(experiencia -> !experiencia.termosAderentes().isEmpty())
                .sorted(Comparator.comparingDouble(ExperienciaAderente::peso).reversed())
                .limit(3)
                .map(experiencia -> new ExperienciaAderenteResponse(
                        experiencia.identificador().isBlank() ? "Relato profissional" : experiencia.identificador(),
                        experiencia.resumo(),
                        experiencia.termosAderentes()
                ))
                .toList();
    }

    private int calcularScoreFinal(String nivel, String experienciaProfissional, String descricaoOportunidade, int compatibilidade, int salarioEstimado) {
        return calcularScoreFinal(nivel, calcularAderenciaPorExperiencia(experienciaProfissional, descricaoOportunidade), compatibilidade, salarioEstimado);
    }

    private int calcularScoreFinal(String nivel, PerfilProfissional perfilProfissional, String descricaoOportunidade, int compatibilidade, int salarioEstimado) {
        return calcularScoreFinal(nivel, calcularAderenciaPonderada(perfilProfissional, descricaoOportunidade), compatibilidade, salarioEstimado);
    }

    private int calcularScoreFinal(String nivel, int scoreAderenciaExperiencia, int compatibilidade, int salarioEstimado) {
        int pesoNivel = switch ((nivel == null ? "" : nivel).toLowerCase()) {
            case "junior" -> 10;
            case "pleno" -> 20;
            case "senior", "sênior" -> 30;
            default -> 15;
        };

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


    private int calcularAderenciaPonderada(PerfilProfissional perfilProfissional, String descricaoOportunidade) {
        Set<String> termosOportunidade = tokenizar(descricaoOportunidade);
        if (perfilProfissional.pesosTermos().isEmpty() || termosOportunidade.isEmpty()) {
            return 0;
        }

        double pesoEncontrado = termosOportunidade.stream()
                .filter(perfilProfissional.pesosTermos()::containsKey)
                .mapToDouble(termo -> Math.min(perfilProfissional.pesosTermos().get(termo), TERMOS_FORTES.contains(termo) ? 4.0 : 2.5))
                .sum();
        double pesoEsperado = termosOportunidade.stream()
                .mapToDouble(termo -> TERMOS_FORTES.contains(termo) ? 2.0 : 1.0)
                .sum();

        return (int) Math.min(Math.round((pesoEncontrado / pesoEsperado) * 25), 25);
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

    private record PerfilProfissional(String texto, Map<String, Double> pesosTermos, List<RelatoProfissional> relatos) {
    }

    private record RelatoProfissional(String identificador, String resumo, Set<String> termos, double pesoRecencia) {
    }

    private record ExperienciaAderente(String identificador, String resumo, List<String> termosAderentes, double peso) {
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
                "ability", "across", "applications", "are", "availability", "based", "build", "cloud",
                "collaborate", "design", "develop", "developer", "engineer", "english", "evaluate",
                "experience", "flexible", "for", "fully", "global", "hours", "join", "job", "knowledge",
                "looking", "mode", "overview", "paid", "program", "project", "remote", "required",
                "requirements", "responsibilities", "role", "schedule", "skills", "software", "strong",
                "systems", "tasks", "team", "title", "training", "week", "work", "years"
        );
        Set<String> marcadoresPortugues = Set.of(
                "aos", "atuar", "buscamos", "candidato", "colaboração", "com", "conhecimento", "desenvolvedor",
                "desenvolvedora", "equipe", "experiência", "forma", "habilidades", "modelo", "oportunidade",
                "para", "perfil", "pessoa", "projeto", "remoto", "requisitos", "responsabilidades", "sobre",
                "time", "trabalho", "vaga"
        );

        long ocorrenciasIngles = termos.stream().filter(marcadoresIngles::contains).count();
        long ocorrenciasPortugues = termos.stream().filter(marcadoresPortugues::contains).count();
        double proporcaoIngles = (double) ocorrenciasIngles / termos.size();

        return ocorrenciasIngles >= 8
                && proporcaoIngles >= 0.18
                && ocorrenciasIngles >= (ocorrenciasPortugues * 2) + 4;
    }
}
