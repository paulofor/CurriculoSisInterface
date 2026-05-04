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

## Configuração de banco

As credenciais e host foram espelhados do backend existente em `loopback-server/server/datasources.json`.
