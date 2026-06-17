package com.curriculosis.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReadOnlyCommandMcpToolsTest {

    @Test
    void parsesQuotedArguments() {
        assertThat(ReadOnlyCommandMcpTools.parseCommand("find /var/log -name 'backend log.txt'"))
                .containsExactly("find", "/var/log", "-name", "backend log.txt");
    }

    @Test
    void rejectsCommandsOutsideAllowlist() {
        assertThatThrownBy(() -> ReadOnlyCommandMcpTools.validateCommand(List.of("rm", "-rf", "/tmp/teste")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não permitido");
    }

    @Test
    void rejectsSensitivePaths() {
        assertThatThrownBy(() -> ReadOnlyCommandMcpTools.validateCommand(List.of("cat", "/etc/shadow")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Caminho sensível");
    }

    @Test
    void rejectsMutatingDockerSubcommands() {
        assertThatThrownBy(() -> ReadOnlyCommandMcpTools.validateCommand(List.of("docker", "exec", "app", "pwd")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Subcomando docker não permitido");
    }

    @Test
    void runsAllowedReadOnlyCommand() throws Exception {
        ReadOnlyCommandMcpTools tools = new ReadOnlyCommandMcpTools();

        Map<String, Object> result = tools.runReadOnlyCommand("pwd", 5);

        assertThat(result.get("timedOut")).isEqualTo(false);
        assertThat(result.get("exitCode")).isEqualTo(0);
        assertThat((String) result.get("stdout")).isNotBlank();
    }
}
