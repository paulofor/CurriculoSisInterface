# CurriculoSis MCP Server (Java + Maven + Spring)

Novo módulo para expor ferramentas MCP com acesso ao banco MySQL usando as mesmas credenciais do backend LoopBack.

## Requisitos

- Java 21+
- Maven 3.9+

## Executar

```bash
cd mcp-server-java
mvn spring-boot:run
```

Servidor sobe na porta `8090`.

## Ferramentas MCP expostas

- `list_tables`: lista tabelas
- `describe_table`: descreve colunas da tabela
- `query_table`: consulta registros com limite de 1 a 200
- `backend_log_status`, `tail_backend_log`, `search_backend_log`: consulta o log do backend LoopBack
- `analise_oportunidades_log_status`, `tail_analise_oportunidades_log`, `search_analise_oportunidades_log`: consulta o log do serviço de análise de oportunidades
- `run_read_only_command`: executa comandos Linux de leitura/diagnóstico com allowlist, timeout de 1 a 30 segundos e limite de saída
- `list_containers`, `tail_container_logs`, `tail_container_logs_since`: lista containers Docker do host e consulta seus logs via Docker socket montado no container MCP

## Configuração de banco

As credenciais e host foram espelhados do backend existente em `loopback-server/server/datasources.json`.
