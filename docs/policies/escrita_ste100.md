# Escrita técnica do Sajitar Backend

Este arquivo define a escrita dos documentos Markdown do repositório.
A referência é a ASD-STE100, Issue 9, de janeiro de 2025.
A Parte 1 dessa norma tem 53 regras, em nove seções.
A Parte 2 é um dicionário de inglês e não vale para esta prosa.
Este arquivo não copia o texto da norma.
A prosa usa português do Brasil.

## Nomes técnicos

Use um termo para cada conceito.
Mantenha o nome técnico na forma do código.
Rota, campo JSON, propriedade, classe, comando e script são nomes técnicos.
Não use um nome técnico como verbo.
Não troque o nome técnico por um sinônimo.

## Frase

Escreva em voz ativa.
Escreva o sujeito da frase.
Não use `-se` para apassivar o verbo.
Não feche a frase com gerúndio.
No procedimento, use o imperativo.
Na descrição, use o presente.
Uma frase de procedimento tem no máximo 20 palavras.
Uma frase de descrição tem no máximo 25 palavras.
Uma frase trata de um só tópico.
Não use ponto e vírgula.

## Parágrafo e lista

Um parágrafo trata de um só tópico.
Um parágrafo tem no máximo seis frases.
Use uma lista vertical para uma série de ações.
Use uma tabela para status, campos e propriedades.
Não use emoji no título.

## Contagem

Conte as palavras da frase corrida.
Um identificador conta como uma palavra.
Um número conta como uma palavra.
Um comando entre crases conta como uma palavra.
Um bloco de código fica fora da contagem.
Uma célula de tabela fica fora da contagem.
O texto entre parênteses conta como uma palavra.
Uma cadeia de `de` tem no máximo dois elos.

## Parênteses e aviso

Use parênteses para uma abreviação.
Use parênteses para uma alternativa curta.
Use parênteses para um esclarecimento curto.
Antes de um passo que apaga dado, escreva um aviso.

## Documentos de API

Cada documento de API segue a mesma ordem.
A ordem é função, `lang`, autenticação, operações e erros.
Cada operação fica em uma seção.
A seção informa caminho, pedido, resultado e erros.

## Skills

Mantenha as chaves `name`, `description` e `disable-model-invocation`.
A chave `description` conserva as frases que disparam a skill.
Mantenha caminhos, comandos e condições.
Não apague um ramo condicional para encurtar o texto.
Mantenha o diagrama Mermaid quando ele for o mapa do fluxo.

## Glossário

| Conceito | Termo na prosa | Nome técnico |
| --- | --- | --- |
| Pessoa cadastrada | perfil | `Profile`, `/profiles` |
| Texto do perfil | nota | `Note`, `/notes` |
| Texto legal do serviço | termo de uso | `Term`, `/terms` |
| Código temporário de conta | checker | `Checker` |
| Login ativo no Redis | sessão | `Session`, `sessionId` |
| Entrada | `signin` | `POST /tokens/signin` |
| Saída | `signout` | `POST /tokens/signout` |
| Renovação do par | `refresh` | `POST /tokens/refresh` |
| Campo de dupla etapa | segundo fator | `twoFactor` |
| Valor conferido do checker | código | `code` |
| Endereço do perfil | e-mail | `email` |
| Segredo do perfil | senha | `password` |

Não chame o perfil de usuário nem de conta.
Não chame a nota de recado.
Não chame o `signin` de login.

## Limite desta norma

Esta norma não altera código Java.
Esta norma não altera o YAML da aplicação.
Esta norma não altera `.cursor/rules/sajitar-backend.mdc`.
Documentação Markdown nova segue este arquivo.

## Conferência

Em divergência, vale o código.
O Markdown antigo não prevalece sobre o código.
Preserve status HTTP, corpos, campos e comandos.
Mude a estrutura e a frase.
Se o fato não cabe em 25 palavras, use tabela ou lista.

## Continuidade

Cada execução lê esta seção antes de editar um documento.
A execução pega o primeiro item da lista pendente.
A execução confere o código que sustenta esse arquivo.
A execução reescreve só esse arquivo.
A execução atualiza o estado, a lista concluída e a lista pendente.
A execução seguinte não repete um arquivo concluído.

Estado: concluído. Os arquivos da lista foram reestruturados.

Arquivo corrente: nenhum.

### Concluídos

1. [docs/development/schema_sql.md](../development/schema_sql.md)
2. [docs/api/notes.md](../api/notes.md)
3. [docs/api/tokens.md](../api/tokens.md)
4. [docs/api/profiles.md](../api/profiles.md)
5. [docs/development/commands.md](../development/commands.md)
6. [docs/policies/test_policy.md](test_policy.md)
7. [docs/policies/branch_policy.md](branch_policy.md)
8. [README.md](../../README.md)
9. [.github/PULL_REQUEST_TEMPLATE.md](../../.github/PULL_REQUEST_TEMPLATE.md)
10. [.cursor/skills/publish-github-release/SKILL.md](../../.cursor/skills/publish-github-release/SKILL.md)
11. [.cursor/skills/ship-implementation/SKILL.md](../../.cursor/skills/ship-implementation/SKILL.md)
12. [docs/api/terms.md](../api/terms.md)

### Pendentes

Nenhum.

Documentação Markdown nova segue este arquivo.
A regra do editor está em `.cursor/rules/documentacao-ste100.mdc`.
