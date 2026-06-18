# AGENTS Instructions

## MCP Server access
- Use sempre o MCP Server pelo endpoint SSE por IP: `http://191.252.92.222/mcp/sse`.
- O acesso MCP confirmado funciona via JSON-RPC sobre SSE:
  1. Abra uma conexão SSE em `http://191.252.92.222/mcp/sse` com header `Accept: text/event-stream`.
  2. Leia o evento `endpoint`; ele retorna um `sessionId` no formato `data:/mcp/message?sessionId=<SESSION_ID>`.
  3. Envie as mensagens JSON-RPC por `POST` para `http://191.252.92.222/mcp/mcp/message?sessionId=<SESSION_ID>` com headers `Content-Type: application/json` e `Accept: application/json, text/event-stream`.
  4. A primeira mensagem deve ser `initialize`; depois envie `notifications/initialized`; então use `tools/list`, `tools/call`, etc.
- Exemplo mínimo de payload `initialize`:

```json
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"codex-cli","version":"1.0.0"}}}
```

## Detalhe importante de roteamento
- O Spring MCP server roda no contexto raiz (`/`) na porta `8090`.
- O gateway mapeia `location /mcp/` para `proxy_pass http://mcp-server:8090/`.
- Na prática, o endpoint SSE retornou `data:/mcp/message?sessionId=...`; por isso, para o gateway encaminhar ao upstream `/mcp/message`, o POST externo confirmado deve usar `/mcp/mcp/message?sessionId=...`.

## OpenAI API key no host
- Existe uma chave para acesso aos modelos da OpenAI no arquivo físico do host: `/root/infra/openai-token/openai_api_key`.

## Referências
- Roteamento `/mcp/` no gateway: `deploy/nginx/default.conf`.
