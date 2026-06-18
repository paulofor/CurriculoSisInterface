package com.curriculosis.mcp;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class ContainerLogMcpTools {

    private static final int MAX_LINES = 1000;
    private static final int MAX_TIMEOUT_SECONDS = 30;
    private static final int MAX_OUTPUT_CHARS = 30_000;
    private static final Pattern SAFE_CONTAINER_NAME = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9_.-]{0,127}");

    @Tool(name = "list_containers", description = "Lista containers Docker visíveis ao MCP Server no host, incluindo nome, status e imagem.")
    public Map<String, Object> listContainers() throws IOException, InterruptedException {
        return runDockerCommand(List.of(
                "docker",
                "ps",
                "--format",
                "table {{.Names}}\\t{{.Status}}\\t{{.Image}}"),
                MAX_TIMEOUT_SECONDS);
    }

    @Tool(name = "tail_container_logs", description = "Retorna as últimas linhas dos logs de um container Docker do host, limitado a 1000 linhas.")
    public Map<String, Object> tailContainerLogs(
            @ToolParam(description = "Nome ou ID do container Docker. Use list_containers para descobrir os nomes.") @NotBlank String container,
            @ToolParam(description = "Quantidade de linhas a retornar (1-1000).") @Min(1) @Max(MAX_LINES) int lines)
            throws IOException, InterruptedException {
        validateContainer(container);
        return runDockerCommand(List.of("docker", "logs", "--tail", String.valueOf(lines), container), MAX_TIMEOUT_SECONDS);
    }

    @Tool(name = "tail_container_logs_since", description = "Retorna logs de um container Docker desde um intervalo aceito pelo docker logs --since, limitado a 1000 linhas.")
    public Map<String, Object> tailContainerLogsSince(
            @ToolParam(description = "Nome ou ID do container Docker. Use list_containers para descobrir os nomes.") @NotBlank String container,
            @ToolParam(description = "Intervalo do docker logs --since, por exemplo '10m', '1h' ou timestamp RFC3339.") @NotBlank String since,
            @ToolParam(description = "Quantidade de linhas a retornar (1-1000).") @Min(1) @Max(MAX_LINES) int lines)
            throws IOException, InterruptedException {
        validateContainer(container);
        validateSince(since);
        return runDockerCommand(List.of("docker", "logs", "--since", since, "--tail", String.valueOf(lines), container), MAX_TIMEOUT_SECONDS);
    }

    static void validateContainer(String container) {
        if (!SAFE_CONTAINER_NAME.matcher(container).matches()) {
            throw new IllegalArgumentException("Nome/ID de container inválido para consulta de logs: " + container);
        }
    }

    static void validateSince(String since) {
        if (since.indexOf('\0') >= 0 || since.indexOf('\n') >= 0 || since.indexOf('\r') >= 0 || since.length() > 64) {
            throw new IllegalArgumentException("Parâmetro since inválido para consulta de logs.");
        }
    }

    private Map<String, Object> runDockerCommand(List<String> command, int timeoutSeconds) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(false).start();
        CompletableFuture<String> stdoutFuture = readStream(process.getInputStream());
        CompletableFuture<String> stderrFuture = readStream(process.getErrorStream());
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            return Map.of(
                    "command", String.join(" ", command),
                    "timedOut", true,
                    "timeoutSeconds", timeoutSeconds,
                    "exitCode", -1,
                    "stdout", sanitize(limitOutput(stdoutFuture.getNow(""))),
                    "stderr", sanitize("Comando finalizado por timeout." + limitOutput(stderrFuture.getNow(""))));
        }

        return Map.of(
                "command", String.join(" ", command),
                "timedOut", false,
                "timeoutSeconds", timeoutSeconds,
                "exitCode", process.exitValue(),
                "stdout", sanitize(limitOutput(stdoutFuture.join())),
                "stderr", sanitize(limitOutput(stderrFuture.join())));
    }

    private static CompletableFuture<String> readStream(java.io.InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                return "Erro ao ler saída do processo: " + exception.getMessage();
            }
        });
    }

    private static String limitOutput(String output) {
        if (output.length() <= MAX_OUTPUT_CHARS) {
            return output;
        }
        return output.substring(0, MAX_OUTPUT_CHARS) + "\n[TRUNCATED]";
    }

    private static String sanitize(String value) {
        return value
                .replaceAll("(?i)(authorization:\\s*bearer\\s+)[^\\s,;]+", "$1[REDACTED]")
                .replaceAll("(?i)(password=)[^\\s&]+", "$1[REDACTED]")
                .replaceAll("(?i)(token=)[^\\s&]+", "$1[REDACTED]")
                .replaceAll("(?i)(api[_-]?key=)[^\\s&]+", "$1[REDACTED]");
    }
}
