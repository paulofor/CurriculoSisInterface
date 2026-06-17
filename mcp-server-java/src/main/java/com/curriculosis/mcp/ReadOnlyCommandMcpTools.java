package com.curriculosis.mcp;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class ReadOnlyCommandMcpTools {

    private static final int MAX_TIMEOUT_SECONDS = 30;
    private static final int MAX_OUTPUT_CHARS = 20_000;

    private static final Set<String> ALLOWED_COMMANDS = Set.of(
            "cat",
            "date",
            "df",
            "docker",
            "du",
            "find",
            "free",
            "head",
            "hostname",
            "id",
            "ls",
            "ps",
            "pwd",
            "ss",
            "stat",
            "tail",
            "uname",
            "uptime",
            "wc",
            "whoami");

    private static final Set<String> ALLOWED_DOCKER_SUBCOMMANDS = Set.of(
            "events",
            "info",
            "inspect",
            "logs",
            "ps",
            "stats",
            "version");

    private static final List<String> BLOCKED_PATH_FRAGMENTS = List.of(
            "/root/infra/openai-token",
            "/proc/kcore",
            "/proc/keys",
            "/proc/sysrq-trigger",
            "/etc/shadow",
            "/etc/gshadow",
            "/etc/sudoers",
            "/.ssh/",
            "/.docker/config.json");

    @Tool(name = "run_read_only_command", description = "Executa comandos Linux de leitura/diagnóstico com allowlist, timeout e limite de saída.")
    public Map<String, Object> runReadOnlyCommand(
            @ToolParam(description = "Comando Linux de leitura. Exemplos: 'pwd', 'ls -la /app', 'tail -n 100 /var/log/curriculosis/backend.log', 'docker ps'.")
            @NotBlank String command,
            @ToolParam(description = "Timeout em segundos (1-30).") @Min(1) @Max(MAX_TIMEOUT_SECONDS) int timeoutSeconds)
            throws IOException, InterruptedException {
        List<String> tokens = parseCommand(command);
        validateCommand(tokens);

        Process process = new ProcessBuilder(tokens).redirectErrorStream(false).start();
        CompletableFuture<String> stdoutFuture = readStream(process.getInputStream());
        CompletableFuture<String> stderrFuture = readStream(process.getErrorStream());
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            return Map.of(
                    "command", String.join(" ", tokens),
                    "timedOut", true,
                    "timeoutSeconds", timeoutSeconds,
                    "exitCode", -1,
                    "stdout", limitOutput(stdoutFuture.getNow("")),
                    "stderr", "Comando finalizado por timeout." + limitOutput(stderrFuture.getNow("")));
        }

        String stdout = limitOutput(stdoutFuture.join());
        String stderr = limitOutput(stderrFuture.join());

        return Map.of(
                "command", String.join(" ", tokens),
                "timedOut", false,
                "timeoutSeconds", timeoutSeconds,
                "exitCode", process.exitValue(),
                "stdout", sanitize(stdout),
                "stderr", sanitize(stderr));
    }

    static List<String> parseCommand(String command) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }

            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }

            if (Character.isWhitespace(c) && !inSingleQuote && !inDoubleQuote) {
                addToken(tokens, current);
                continue;
            }

            current.append(c);
        }

        if (inSingleQuote || inDoubleQuote) {
            throw new IllegalArgumentException("Comando contém aspas sem fechamento.");
        }

        addToken(tokens, current);
        return tokens;
    }

    static void validateCommand(List<String> tokens) {
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Comando vazio.");
        }

        String executable = tokens.getFirst();
        if (executable.contains("/") || !ALLOWED_COMMANDS.contains(executable)) {
            throw new IllegalArgumentException("Comando não permitido para execução MCP somente leitura: " + executable);
        }

        for (String token : tokens) {
            validateToken(token);
        }

        if ("docker".equals(executable)) {
            validateDockerCommand(tokens);
        }
    }

    private static void validateToken(String token) {
        if (token.indexOf('\0') >= 0 || token.indexOf('\n') >= 0 || token.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Comando contém caracteres de controle não permitidos.");
        }

        String normalized = token.toLowerCase(Locale.ROOT);
        for (String blockedPath : BLOCKED_PATH_FRAGMENTS) {
            if (normalized.contains(blockedPath)) {
                throw new IllegalArgumentException("Caminho sensível bloqueado para leitura via MCP: " + blockedPath);
            }
        }
    }

    private static void validateDockerCommand(List<String> tokens) {
        if (tokens.size() == 1) {
            return;
        }

        String subcommand = tokens.get(1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_DOCKER_SUBCOMMANDS.contains(subcommand)) {
            throw new IllegalArgumentException("Subcomando docker não permitido para leitura via MCP: " + subcommand);
        }
    }

    private static void addToken(List<String> tokens, StringBuilder current) {
        if (!current.isEmpty()) {
            tokens.add(current.toString());
            current.setLength(0);
        }
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
