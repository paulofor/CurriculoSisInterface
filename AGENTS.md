# AGENTS Instructions

## MCP Server access
- O MCP server é publicado no mesmo host do frontend/backend, passando pelo gateway Nginx na porta `80`.
- Endpoint público para clientes MCP: `http://vps-40d69db1.vps.ovh.ca/mcp/`.
- Endpoint por IP (IPv4 resolvido em 04/05/2026): `http://51.79.51.172/mcp/`.
- Em ambiente local (docker compose), a rota equivalente é `http://localhost/mcp/`.

## Detalhe importante de roteamento
- O Spring MCP server roda no contexto raiz (`/`) na porta `8090`.
- Por isso, o gateway deve mapear `location /mcp/` para `proxy_pass http://mcp-server:8090/`.
- Se usar `proxy_pass http://mcp-server:8090/mcp/`, o upstream recebe `/mcp/` e tende a responder `404 Not Found`.

## Referências
- Host público usado pelo frontend: `loopback-angular6/src/app/constantes/base.url.ts`.
- Roteamento `/mcp/` no gateway: `deploy/nginx/default.conf`.
