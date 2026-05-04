package com.curriculosis.mcp;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMcpTools {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMcpTools(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Tool(name = "list_tables", description = "Lista as tabelas disponíveis no banco de dados atual.")
    public List<String> listTables() {
        return jdbcTemplate.queryForList("SHOW TABLES", String.class);
    }

    @Tool(name = "describe_table", description = "Descreve colunas, tipos e propriedades de uma tabela.")
    public List<Map<String, Object>> describeTable(
            @ToolParam(description = "Nome da tabela para inspecionar") @NotBlank String tableName) {
        return jdbcTemplate.queryForList("SHOW COLUMNS FROM " + sanitizeIdentifier(tableName));
    }

    @Tool(name = "query_table", description = "Consulta simples com limite de linhas para leitura de dados.")
    public List<Map<String, Object>> queryTable(
            @ToolParam(description = "Nome da tabela para consulta") @NotBlank String tableName,
            @ToolParam(description = "Limite de linhas retornadas (1-200)") @Min(1) @Max(200) int limit) {
        return jdbcTemplate.queryForList("SELECT * FROM " + sanitizeIdentifier(tableName) + " LIMIT ?", limit);
    }

    private String sanitizeIdentifier(String identifier) {
        if (!identifier.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Identificador inválido. Use apenas letras, números e underscore.");
        }
        return identifier;
    }
}
