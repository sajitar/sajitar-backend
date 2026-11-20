# Política de testes

Este arquivo é a referência de teste do repositório `sajitar-backend`.
O time usa este arquivo para planejar, executar e mostrar evidência.
O alinhamento segue os objetivos da ISO/IEC 29119.
Este arquivo não equivale a certificação.
Este arquivo não equivale a auditoria externa.

## Objetivo

Defina os níveis de teste usados no repositório.
Defina o mínimo para a integração contínua.
Defina o que o pull request e a release precisam mostrar.
Ligue cada mudança de comportamento a um teste.
Complemente a [política de branches](branch_policy.md).
Aquela política diz quando o GitHub Actions roda os testes.

## Alinhamento com a ISO/IEC 29119

O alinhamento abaixo é informativo.

| Tema da norma | Aplicação neste repositório |
| --- | --- |
| Vocabulário | O time usa nível de teste, caso de teste, defeito e critério de aceite. |
| Processo | O PR traz o checklist. A proteção de `develop` exige o CI. A release documenta a versão no SHA publicado. |
| Documentação | Este arquivo, a descrição do PR e o relatório JaCoCo formam a evidência versionada. |

A conformidade plena com a norma depende do processo da organização.
Papéis e registros fora do Git ficam fora deste arquivo.
Este arquivo cobre o backend versionado.

## Níveis

| Nível | Finalidade | Onde aparece |
| --- | --- | --- |
| Componente | Valida domínio, validação, use case com porta simulada, configuration, handler e Jackson. A aplicação não sobe. | Classes `*Test` em `src/test/java`, no mesmo pacote da produção. |
| Integração HTTP | Valida status, JSON, validação e `lang` com Spring, PostgreSQL e Redis. | `TokenControllerIntegrationTest`, `ProfileControllerIntegrationTest`, `NoteControllerIntegrationTest`, `TermControllerIntegrationTest` e `CheckerRouteIntegrationTest`. |
| Contexto Spring | Confirma que a aplicação sobe com a configuração de teste. | `BackendApplicationTests`, método `contextLoads`. |

O teste de componente usa JUnit 5, AssertJ, Mockito e `@ParameterizedTest`.
As fixtures JSON ficam em `src/test/resources/fixtures/`.
O teste de anotação usa `*ConstraintFixture`.
Não use `@SpringBootTest` para regra pura.

O teste HTTP usa `@SpringBootTest` e MockMvc.
`CheckerRouteIntegrationTest` prova dois resultados de `/checkers`.
Sem Bearer, a rota responde 401 `{token}`.
Com access válido, a rota responde 404.
Endpoint novo cobre sucesso, 400, 404 e 409 quando o contrato os tiver.
O `signin` e o `refresh` também cobrem o 403 de `VERIFY_EMAIL` quando o fluxo o tiver.
O CI sobe `postgres:18.6` e `redis:8.2.9-alpine` antes de `./mvnw verify`.
O workflow é [`.github/workflows/verify.yml`](../../.github/workflows/verify.yml).

Se o time não usar um nível, registre o motivo no PR ou na issue.
Teste de carga e teste de ponta a ponta no browser são exemplos.
Registre também o plano para introduzir o nível, quando houver data ou condição.

## Ferramentas

Use JUnit 5, pelo `spring-boot-starter-test`.
Prefira AssertJ.
Use `@DisplayName` em português.
Organize o método em dado, ação e resultado.
Guarde cenário repetido em `src/test/resources/fixtures/` quando isso reduzir cópia.
A configuração de teste fica nos perfis Spring `LOCAL-TEST`, `CI` e `DOCKER-TEST`, em `src/test/resources`.

## Integração contínua e cobertura

O workflow [`.github/workflows/verify.yml`](../../.github/workflows/verify.yml) roda em paralelo à política de branches.
O check na UI é `Testes unitários e cobertura (JaCoCo)`.
O job usa o perfil Spring `CI`, de [`src/test/resources/application-CI.yml`](../../src/test/resources/application-CI.yml).
O runner usa JDK 25, Eclipse Temurin.
O PostgreSQL entra como serviço.
O Redis sobe no job, depois do checkout, porque a ACL está no repositório.
O comando é `SPRING_PROFILES_ACTIVE="CI" ./mvnw verify`.

O `pom.xml` exige `COVEREDRATIO` 1 no pacote agregado.
O mínimo vale para instrução, ramo, linha e método.
As propriedades são `jacoco.coverage.minimum.*`.
O plugin exclui `BackendApplication` e `ValidationErrorResponse`.
Não amplie essa lista.
O job anexa o artefato `jacoco-report`.
No host, o HTML fica em `target/site/jacoco/index.html`.

A execução local está em [Comandos](../development/commands.md#perfis-spring).
No host, use o perfil Spring `LOCAL-TEST`.
No container `sajitar-springboot`, use o perfil Spring `DOCKER-TEST`.

## Rastreabilidade no PR

Aplique esta seção quando o PR mudar comportamento observável.
API, validação, persistência, segurança e desempenho crítico entram nesse grupo.
Inclua no corpo do PR, ou na issue ligada, a tabela abaixo.

| Referência | Evidência de teste | Notas |
| --- | --- | --- |
| Issue, épico ou critério de aceite | Classe, método ou `@DisplayName` | |

Toda regra de negócio nova tem ao menos um teste automatizado.
O teste falha se alguém remover a regra.
Sem teste novo, explique o motivo no PR.
Aponte a regressão que os testes existentes já cobrem.
Refatoração sem mudança de comportamento é um motivo válido.

## Defeitos

Registre o defeito no GitHub Issues ou na ferramenta acordada pelo time.
Inclua os passos para reproduzir.
Inclua o comportamento esperado e o atual.
Inclua commit, branch e perfil.
No PR da correção, cite o defeito.
Depois do merge, confirme o reteste.
O CI verde é a confirmação mínima.

## Entrada e saída

| Momento | Entrada mínima | Evidência |
| --- | --- | --- |
| Abrir PR | Descrição da mudança. Mudança de comportamento inclui a tabela de rastreabilidade. | Checks obrigatórios, com testes e JaCoCo. |
| Merge em `develop` | Revisão e checks verdes. | Histórico do PR e commit em `develop`. |
| Release | Acordo do time. Exemplo: nenhuma issue bloqueante. | GitHub Release `vX.Y.Z` num SHA de `develop`, com notas. |

## Melhoria

Depois de um incidente grave que a suíte não pegou, registre a lição.
Abra uma issue para teste novo ou ajuste de cenário, quando couber.
Revise os níveis deste arquivo ao menos uma vez por ano.
Revise também a cada marco maior do produto.

## Glossário

| Termo | Sentido neste repositório |
| --- | --- |
| Caso de teste | Entrada, ação e resultado esperado. No código, é um método com nome ou `@DisplayName`. |
| Suíte de regressão | O conjunto que o CI executa com `./mvnw verify`. |
| Defeito | Comportamento diferente do requisito ou da especificação acordada. |

## Referências

- [ISO/IEC/IEEE 29119-1](https://www.iso.org/standard/45142.html), conceitos e vocabulário.
- [Política de branches](branch_policy.md).
