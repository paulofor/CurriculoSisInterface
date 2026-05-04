# Registros — CurriculoSisInterface

> Orientação: todos os registros deste documento devem sempre incluir **data e hora no fuso UTC-3**.
> Neste documento segue política de **append-only** (não pode ter nenhuma linha apagada; apenas inserções).

## 2026-05-04 14:05 (UTC-3)
- Ajuste aplicado no gateway Nginx para MCP: em `location /mcp/`, `proxy_pass` alterado de `http://mcp-server:8090/mcp/` para `http://mcp-server:8090/` para evitar encaminhamento indevido do path `/mcp/` ao upstream e eliminar resposta `404` do Spring.
- Atualizado `AGENTS.md` com nota operacional explicando a causa raiz e o mapeamento correto para futuras manutenções.
