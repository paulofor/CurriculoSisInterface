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

    public LogMcpTools(@Value("${curriculosis.logs.backend-file:/var/log/curriculosis/backend.log}") String backendLogFile) {
        this.backendLogFile = Path.of(backendLogFile).normalize();
    }

    @Tool(name = "backend_log_status", description = "Mostra metadados do arquivo de log do backend configurado para consulta MCP.")
    public Map<String, Object> backendLogStatus() throws IOException {
        if (!Files.exists(backendLogFile)) {
            return Map.of(
                    "path", backendLogFile.toString(),
                    "exists", false);
        }

        return Map.of(
                "path", backendLogFile.toString(),
                "exists", true,
                "sizeBytes", Files.size(backendLogFile),
                "lastModified", Files.getLastModifiedTime(backendLogFile).toInstant().toString(),
                "checkedAt", Instant.now().toString());
    }

    @Tool(name = "tail_backend_log", description = "Retorna as últimas linhas do log do backend, limitado a 1000 linhas.")
    public String tailBackendLog(
            @ToolParam(description = "Quantidade de linhas a retornar (1-1000)") @Min(1) @Max(MAX_LINES) int lines)
            throws IOException {
        ensureReadableLogFile();
        return sanitize(String.join("\n", tailLines(lines)));
    }

    @Tool(name = "search_backend_log", description = "Busca texto no log do backend e retorna as últimas ocorrências, limitado a 1000 linhas.")
    public String searchBackendLog(
            @ToolParam(description = "Texto a buscar no log") @NotBlank String query,
            @ToolParam(description = "Quantidade máxima de linhas encontradas a retornar (1-1000)") @Min(1) @Max(MAX_LINES) int lines)
            throws IOException {
        ensureReadableLogFile();

        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        ArrayDeque<String> matches = new ArrayDeque<>();

        try (var stream = Files.lines(backendLogFile, StandardCharsets.UTF_8)) {
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

    private List<String> tailLines(int lines) throws IOException {
        ArrayDeque<String> tail = new ArrayDeque<>();

        try (var stream = Files.lines(backendLogFile, StandardCharsets.UTF_8)) {
            stream.forEach(line -> {
                if (tail.size() == lines) {
                    tail.removeFirst();
                }
                tail.addLast(line);
            });
        }

        return List.copyOf(tail);
    }

    private void ensureReadableLogFile() {
        if (!Files.isRegularFile(backendLogFile) || !Files.isReadable(backendLogFile)) {
            throw new IllegalStateException("Arquivo de log do backend não está disponível para leitura: " + backendLogFile);
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
