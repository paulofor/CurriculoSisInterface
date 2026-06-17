package com.curriculosis.mcp;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LogMcpTools {

    private static final int MAX_LINES = 1000;

    private final Path backendLogFile;
    private final Path analiseOportunidadesLogFile;

    public LogMcpTools(
            @Value("${curriculosis.logs.backend-file:/var/log/curriculosis/backend.log}") String backendLogFile,
            @Value("${curriculosis.logs.analise-oportunidades-file:/var/log/curriculosis/analise-oportunidades.log}") String analiseOportunidadesLogFile
    ) {
        this.backendLogFile = Path.of(backendLogFile).normalize();
        this.analiseOportunidadesLogFile = Path.of(analiseOportunidadesLogFile).normalize();
    }

    @Tool(name = "backend_log_status", description = "Mostra metadados do arquivo de log do backend configurado para consulta MCP.")
    public Map<String, Object> backendLogStatus() throws IOException {
        return logStatus(backendLogFile);
    }

    @Tool(name = "analise_oportunidades_log_status", description = "Mostra metadados do arquivo de log do serviço de análise de oportunidades configurado para consulta MCP.")
    public Map<String, Object> analiseOportunidadesLogStatus() throws IOException {
        return logStatus(analiseOportunidadesLogFile);
    }

    @Tool(name = "tail_backend_log", description = "Retorna as últimas linhas do log do backend, limitado a 1000 linhas.")
    public String tailBackendLog(
            @ToolParam(description = "Quantidade de linhas a retornar (1-1000)") @Min(1) @Max(MAX_LINES) int lines)
            throws IOException {
        ensureReadableLogFile(backendLogFile);
        return sanitize(String.join("\n", tailLines(backendLogFile, lines)));
    }

    @Tool(name = "tail_analise_oportunidades_log", description = "Retorna as últimas linhas do log do serviço de análise de oportunidades, limitado a 1000 linhas.")
    public String tailAnaliseOportunidadesLog(
            @ToolParam(description = "Quantidade de linhas a retornar (1-1000)") @Min(1) @Max(MAX_LINES) int lines)
            throws IOException {
        ensureReadableLogFile(analiseOportunidadesLogFile);
        return sanitize(String.join("\n", tailLines(analiseOportunidadesLogFile, lines)));
    }

    @Tool(name = "search_backend_log", description = "Busca texto no log do backend e retorna as últimas ocorrências, limitado a 1000 linhas.")
    public String searchBackendLog(
            @ToolParam(description = "Texto a buscar no log") @NotBlank String query,
            @ToolParam(description = "Quantidade máxima de linhas encontradas a retornar (1-1000)") @Min(1) @Max(MAX_LINES) int lines)
            throws IOException {
        ensureReadableLogFile(backendLogFile);
        return searchLog(backendLogFile, query, lines);
    }

    @Tool(name = "search_analise_oportunidades_log", description = "Busca texto no log do serviço de análise de oportunidades e retorna as últimas ocorrências, limitado a 1000 linhas.")
    public String searchAnaliseOportunidadesLog(
            @ToolParam(description = "Texto a buscar no log") @NotBlank String query,
            @ToolParam(description = "Quantidade máxima de linhas encontradas a retornar (1-1000)") @Min(1) @Max(MAX_LINES) int lines)
            throws IOException {
        ensureReadableLogFile(analiseOportunidadesLogFile);
        return searchLog(analiseOportunidadesLogFile, query, lines);
    }

    private String searchLog(Path logFile, String query, int lines) throws IOException {
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        ArrayDeque<String> matches = new ArrayDeque<>();

        try (var stream = Files.lines(logFile, StandardCharsets.UTF_8)) {
            stream.filter(line -> line.toLowerCase(Locale.ROOT).contains(normalizedQuery))
                    .forEach(line -> {
                        if (matches.size() == lines) {
                            matches.removeFirst();
                        }
                        matches.addLast(line);
                    });
        }

        return sanitize(String.join("\n", matches));
    }

    private List<String> tailLines(Path logFile, int lines) throws IOException {
        ArrayDeque<String> tail = new ArrayDeque<>();

        try (var stream = Files.lines(logFile, StandardCharsets.UTF_8)) {
            stream.forEach(line -> {
                if (tail.size() == lines) {
                    tail.removeFirst();
                }
                tail.addLast(line);
            });
        }

        return List.copyOf(tail);
    }

    private Map<String, Object> logStatus(Path logFile) throws IOException {
        if (!Files.exists(logFile)) {
            return Map.of(
                    "path", logFile.toString(),
                    "exists", false,
                    "checkedAt", Instant.now().toString());
        }

        return Map.of(
                "path", logFile.toString(),
                "exists", true,
                "sizeBytes", Files.size(logFile),
                "lastModified", Files.getLastModifiedTime(logFile).toInstant().toString(),
                "checkedAt", Instant.now().toString());
    }

    private void ensureReadableLogFile(Path logFile) {
        if (!Files.isRegularFile(logFile) || !Files.isReadable(logFile)) {
            throw new IllegalStateException("Arquivo de log não está disponível para leitura: " + logFile);
        }
    }

    private String sanitize(String value) {
        return value
                .replaceAll("(?i)(authorization:\\s*bearer\\s+)[^\\s,;]+", "$1[REDACTED]")
                .replaceAll("(?i)(password=)[^\\s&]+", "$1[REDACTED]")
                .replaceAll("(?i)(token=)[^\\s&]+", "$1[REDACTED]")
                .replaceAll("(?i)(api[_-]?key=)[^\\s&]+", "$1[REDACTED]");
    }
}
