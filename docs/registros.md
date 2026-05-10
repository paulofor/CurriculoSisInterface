# Registros — CurriculoSisInterface

> Orientação: todos os registros deste documento devem sempre incluir **data e hora no fuso UTC-3**.
> Neste documento segue política de **append-only** (não pode ter nenhuma linha apagada; apenas inserções).

## 2026-05-04 14:05 (UTC-3)
- Ajuste aplicado no gateway Nginx para MCP: em `location /mcp/`, `proxy_pass` alterado de `http://mcp-server:8090/mcp/` para `http://mcp-server:8090/` para evitar encaminhamento indevido do path `/mcp/` ao upstream e eliminar resposta `404` do Spring.
- Atualizado `AGENTS.md` com nota operacional explicando a causa raiz e o mapeamento correto para futuras manutenções.

## 2026-05-10 14:45 (UTC-3)
- Criado `analise-oportunidades-service/AGENTS.md` com instrução de governança para o módulo: toda mudança realizada nesse módulo deve ser registrada em `/docs/registros.md`.

## 2026-05-10 17:41 (UTC-3)
- Evoluído o `analise-oportunidades-service` para refletir o objetivo principal de cruzar oportunidades recentes com a experiência do usuário: o request agora aceita `descricaoOportunidade` e `experienciaProfissional` e o cálculo de score passou a incluir aderência textual entre os dois campos.
- Refinadas mensagens de recomendação para explicitar o nível de aderência ao histórico profissional.

## 2026-05-10 17:46 (UTC-3)
- Implementada integração do `analise-oportunidades-service` com a API da OpenAI para enviar descrição da oportunidade e experiência profissional ao modelo de IA, retornando um parecer textual de aderência.
- Incluído novo campo `analiseIa` no response para expor o resultado textual da IA ao frontend/consumidores.
- Adicionadas propriedades de configuração `openai.api.key` e `openai.model`, com suporte a variáveis de ambiente `OPENAI_API_KEY` e `OPENAI_MODEL`.
