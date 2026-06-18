package com.curriculosis.mcp;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ContainerLogMcpToolsTest {

    @Test
    void acceptsSafeContainerNamesAndIds() {
        assertThatCode(() -> ContainerLogMcpTools.validateContainer("curriculosis-backend"))
                .doesNotThrowAnyException();
        assertThatCode(() -> ContainerLogMcpTools.validateContainer("abc123DEF_01.2"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsUnsafeContainerNames() {
        assertThatThrownBy(() -> ContainerLogMcpTools.validateContainer("backend;cat /etc/shadow"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inválido");
    }

    @Test
    void rejectsControlCharactersInSinceParameter() {
        assertThatThrownBy(() -> ContainerLogMcpTools.validateSince("10m\n--follow"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("since inválido");
    }
}
