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

## 2026-06-14 19:56 (UTC-3)
- Criada rotina no `analise-oportunidades-service` para ranquear uma lista de oportunidades contra o currículo mestre em JSON de Paulo Forestieri, usando `docs/curriculos/paulo_forestieri_curriculo_master_inicial.json` como referência configurável.
- Incluído endpoint `POST /api/oportunidades/aderentes-curriculo`, com retorno ordenado por score, termos aderentes, lacunas e parecer de IA quando a chave OpenAI estiver configurada.

## 2026-06-15 11:27 (UTC-3)
- Alterado o modelo padrão do `analise-oportunidades-service` para `gpt-5.2`.
- Incluída regra para ignorar oportunidades em inglês ou com exigência de inglês fluente/avançado antes do cálculo de score e antes da chamada de IA.
- Criada tela no frontend para listar as melhores oportunidades recentes, consumindo o endpoint de ranking por currículo e destacando vagas ignoradas por inglês.

- Ajustado deploy Docker para servir o Angular por Nginx de produção (sem SockJS/live reload na porta 4200 pública), apontar o gateway para a porta interna 80 do frontend e disponibilizar a chave da OpenAI como Docker secret para o serviço de análise; o serviço também passou a tratar falhas da chamada à OpenAI sem derrubar o endpoint de ranking.

## 2026-06-16 15:55 (UTC-3)
- Corrigido o `analise-oportunidades-service` para empacotar o currículo mestre como recurso da aplicação e usá-lo como fallback quando o arquivo `docs/curriculos/paulo_forestieri_curriculo_master_inicial.json` não existir dentro da imagem Docker, evitando erro 500 no endpoint `/api/oportunidades/aderentes-curriculo` em produção.

## 2026-06-16 20:54 (UTC-3)
- Configurado o `analise-oportunidades-service` para gravar logs em arquivo via `LOGGING_FILE_NAME`, permitindo persistência em volume Docker compartilhado.
- Expandido o MCP Server com ferramentas para consultar status, tail e busca textual no log do serviço de análise de oportunidades, além do log do backend já existente.
- Ajustado o `docker-compose.yml` para montar o volume `/var/log/curriculosis` também no serviço de análise, viabilizando diagnóstico do erro 500 de `/api/oportunidades/aderentes-curriculo` via MCP.

## 2026-06-17
- Criado o módulo `avaliador-aderencia-service`, uma aplicação Spring Boot com scheduler para avaliar oportunidades em lotes usando OpenAI.
- Adicionados endpoints no backend LoopBack para consultar health/status, disparar execução manual e receber callbacks de resultados do avaliador.
- Atualizados `docker-compose.yml` e Nginx para publicar o novo container no mesmo host.

## 2026-06-17 18:41 (UTC-3)
- Otimizado o Dockerfile do `analise-oportunidades-service` para baixar dependências Maven em uma camada separada (`dependency:go-offline`) antes de copiar o código-fonte, reduzindo rebuilds longos no GitHub Actions quando apenas arquivos `src` mudam.

## 2026-06-18 23:35 (UTC-3)
- Corrigido o `avaliador-aderencia-service` para chamar o backend LoopBack com o filtro de oportunidades como `URI` já codificada, evitando dupla codificação do JSON do parâmetro `filter` e o erro 400 `Value is not an object` ao buscar oportunidades pendentes.
- Adicionado teste cobrindo que a URI de busca de oportunidades pendentes mantém o filtro codificado apenas uma vez.

## 2026-06-19 09:36 (UTC-3)
- Corrigido o `avaliador-aderencia-service` para solicitar resposta estruturada da OpenAI via JSON Schema na Responses API e interpretar o texto retornado tanto por `output_text` quanto pela lista `output[].content[].text`.
- Ajustado o envio de resultados do avaliador ao backend para postar um JSON plano com `oportunidadeId`, `notaAderencia`, `analiseIa` e `status`, evitando rejeição do callback com erro `Resultado de aderencia sem oportunidadeId`.

## 2026-06-19
- Reforçada a regra do `analise-oportunidades-service` para descartar oportunidades cujo texto esteja todo em inglês, mesmo quando não há exigência explícita de inglês fluente/avançado, preservando vagas em português com termos técnicos em inglês.

## 2026-06-21 11:55 (UTC)
- Corrigido o backend LoopBack para descartar automaticamente callbacks de aderência de oportunidades presenciais ou híbridas, zerando a nota e registrando o motivo na análise, mesmo quando a análise de IA retornaria score alto.
- A regra usa o campo estruturado `modelo`, a descrição coletada e a análise retornada para impedir que vagas presenciais/híbridas permaneçam entre as selecionadas.
- Incluída rotina de saneamento na inicialização do backend para retirar das selecionadas oportunidades já avaliadas com nota alta quando `modelo` ou `descricao` indiquem presencial/híbrido.

## 2026-06-21 15:11 (UTC-3)
- Implementada a Fase 1 de uso dos relatos da tela Experiência Profissional no ranking de oportunidades: o frontend agora envia os relatos recentes junto com as vagas e o serviço de análise consolida esse texto com o currículo mestre para melhorar o cálculo de score, termos aderentes, lacunas e parecer de IA.

## 2026-06-21 15:18 (UTC-3)
- Implementada a Fase 2 do ranking por relatos profissionais: os relatos agora são enviados com datas e metadados, o serviço pondera termos por recência, destaca termos técnicos fortes, prioriza lacunas fortes e usa a aderência ponderada no score das oportunidades.

## 2026-06-21 15:36 (UTC-3)
- Implementada a Fase 3 do uso dos relatos profissionais: o serviço agora gera resumos otimizados dos relatos, retorna as experiências mais aderentes para cada oportunidade com termos correspondentes e o frontend exibe essas experiências antes do parecer de IA.

## 2026-06-21 15:40 (UTC-3)
- Implementada a Fase 4 de interface dos relatos profissionais: a tela Experiência Profissional agora indica se cada relato está apto a entrar no score, exibe tags técnicas detectadas e permite gerar um resumo local otimizado para conferência antes de usar os relatos no ranking.

## 2026-06-30

- Ajustado o agendamento do `avaliador-aderencia-service` para executar a avaliação automática de oportunidades apenas uma vez por dia, às 06:00 UTC, tanto no padrão da aplicação quanto no `docker-compose.yml`.
