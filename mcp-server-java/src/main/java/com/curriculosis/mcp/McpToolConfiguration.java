package com.curriculosis.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfiguration {

    @Bean
    ToolCallbackProvider curriculosisToolCallbackProvider(
            DatabaseMcpTools databaseMcpTools,
            LogMcpTools logMcpTools,
            ReadOnlyCommandMcpTools readOnlyCommandMcpTools,
            ContainerLogMcpTools containerLogMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(databaseMcpTools, logMcpTools, readOnlyCommandMcpTools, containerLogMcpTools)
                .build();
    }
}
