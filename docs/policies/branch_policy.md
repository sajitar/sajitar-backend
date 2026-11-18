# Política de branches e integração contínua

Este arquivo descreve o modelo de branches e o fluxo de merge.
Descreve também o que o GitHub Actions valida.
Descreve o que configurar no GitHub para bloquear código fora do padrão.

## Objetivo

Mantenha a nomenclatura `feat/`, `fix/`, `hotfix/` e os outros prefixos da tabela.
Exija ao menos um assignee em todo pull request.
Integre o trabalho diário em `develop`.
Publique cada versão como GitHub Release num SHA dessa branch.
Faça o pipeline falhar quando a regra quebrar.
Com proteção de branch, o merge e o push inválidos ficam bloqueados.
Rode em paralelo a política de branches, os testes com JaCoCo e a qualidade dos scripts.
Não há `needs` entre esses workflows.
Falha de teste ou de cobertura mínima falha o pipeline.

Veja também a [política de testes](test_policy.md).

## Modelo

Há uma branch longa.
Versão publicada não é uma segunda branch.
Versão publicada é uma tag e um GitHub Release.

| O que | Função |
| --- | --- |
| `develop` | Única branch longa. É protegida e é a default. Recebe o merge do dia a dia. |
| `feat/*`, `fix/*` e os outros prefixos | Branch de trabalho criada a partir de `develop`. |
| `hotfix/*` | Correção de uma tag já publicada que não é o HEAD de `develop`. |
| GitHub Release `vX.Y.Z` | Snapshot imutável de um SHA de `develop`, com notas e artefatos. |

Não existem `main`, `master`, `development` nem `release/*`.
Produção não recebe pull request.
Produção recebe uma Release no SHA escolhido.

### Nome aceito no push

Além de `develop`, o push aceita branch com ao menos um segmento depois da barra.

| Prefixo | Uso |
| --- | --- |
| `feat/` ou `feature/` | Funcionalidade nova |
| `fix/` ou `bugfix/` | Correção de defeito |
| `docs/` | Só documentação |
| `chore/` | Manutenção ou ferramenta, sem mudança direta de função |
| `refactor/` | Refatoração sem mudança de comportamento pretendida |
| `test/` | Teste |
| `ci/` | Pipeline e automação |
| `perf/` | Desempenho |
| `hotfix/` | Correção de tag já publicada |

O push também aceita `dependabot/…` quando o Dependabot estiver configurado.

Exemplos válidos: `feat/login-oauth`, `fix/null-pointer-export`, `hotfix/session-leak`.

Exemplos rejeitados: `minha-branch`, `feature` sem barra, `FIX-bug`, `main`, `master`, `development`, `release/2.4.0`.
`v0.0.2` é tag de Release, não nome de branch.

Tag de push aceita só `vX.Y.Z`, com três inteiros e sem prerelease.
`v0.0.2` passa.
`v0.0.2-rc.1`, `0.0.2` e `release/2.4.0` falham.
O workflow separa tag e branch com `github.ref_type`.

Os padrões estão em [`.github/scripts/validate-branch-policy.sh`](../../.github/scripts/validate-branch-policy.sh).

## Pull request

### Base `develop`

A origem esperada usa os prefixos de trabalho, `hotfix/*` ou `dependabot/*`.
Não use `develop` como head do PR.
O fluxo é branch nomeada, depois PR, depois `develop`.
O PR precisa de ao menos um assignee.
Sem assignee, o check `Validar nomenclatura e fluxo de branches` falha.
O mesmo vale para PR `dependabot/*`.
Atribuir ou remover assignee reexecuta o workflow.
Os eventos são `assigned` e `unassigned`.
O autor entra em geral com `--assignee @me`.

### Base `main`, `master` ou `development`

O script rejeita essas bases.
A única branch longa é `develop`.
Retargete o PR para `develop`.

### Outra base

Se a base não for `develop` nem um nome legado, o workflow ainda exige origem válida.
A origem usa um prefixo da tabela ou `dependabot/`.
O PR continua exigindo assignee.
Ajuste o script se o time criar outra base, por exemplo `staging`.

### GitHub Release

O gate de código é o PR em `develop`, com revisão e checks.
O gate de publicação é quem pode criar a Release.
A permissão fica no repositório ou num environment do GitHub.

- Snapshot do HEAD: `gh release create vX.Y.Z --target develop`.
- A UI equivalente é Releases, Draft a new release, com a tag no commit de `develop`.
- Snapshot de um SHA: `gh release create vX.Y.Z --target <sha>`.
- Hotfix: `git switch -c hotfix/slug vX.Y.Z`, PR para `develop`, e Release nova no SHA integrado.

Não há branch `release/*`.
Não há PR de `develop` para `main`.

## GitHub Actions

Três workflows rodam sem depender um do outro.
Falha de nome não esconde falha de teste.

| Workflow | Arquivo | Check na UI |
| --- | --- | --- |
| Política de branches | [`.github/workflows/branch-policy.yml`](../../.github/workflows/branch-policy.yml) | `Validar nomenclatura e fluxo de branches` |
| Testes e cobertura | [`.github/workflows/verify.yml`](../../.github/workflows/verify.yml) | `Testes unitários e cobertura (JaCoCo)` |
| Qualidade dos scripts | [`.github/workflows/scripts.yml`](../../.github/workflows/scripts.yml) | `ShellCheck dos scripts` e `Testes do script de política de branches` |

### Política de branches

O workflow dispara em todo push, de branch ou de tag.
Dispara em pull request `opened`, `synchronize`, `reopened`, `edited`, `assigned` e `unassigned`.
Dispara em `workflow_dispatch`, tratado como push da ref atual.

Push de branch valida o nome.
Push de tag aceita só `vX.Y.Z`.
`verify.yml` e `scripts.yml` não disparam em tag.
Pull request valida o nome da origem, o par base e origem, e o assignee.

O script local aceita estes exemplos.

```bash
bash .github/scripts/validate-branch-policy.sh push feat/exemplo
bash .github/scripts/validate-branch-policy.sh push v0.0.2 tag
bash .github/scripts/validate-branch-policy.sh pull_request feat/exemplo develop alice
```

Os testes estão em [`.github/scripts/validate-branch-policy.test.sh`](../../.github/scripts/validate-branch-policy.test.sh).

### Testes e cobertura

O workflow dispara em pull request de qualquer base.
Dispara em push só para `develop`.
Dispara em `workflow_dispatch`.
Em `edited`, o Maven não roda de novo se só título ou corpo mudaram.
O Maven roda de novo se a base do PR mudou.

O job sobe `postgres:18.6` como service.
O job sobe `redis:8.2.9-alpine` depois do checkout.
A ACL vem do repositório, então o Redis só nasce depois do checkout.
As credenciais são as do perfil Spring `CI`, em [`src/test/resources/application-CI.yml`](../../src/test/resources/application-CI.yml).
O job configura JDK 25 com `actions/setup-java`.
O job executa `SPRING_PROFILES_ACTIVE="CI" ./mvnw verify`.
As dependências vêm do Maven Central, pelo wrapper.
O runner não usa `settings.xml` corporativo.

O `verify` roda Surefire e o JaCoCo.
A ordem no `pom.xml` é `prepare-agent`, testes, `report` e `check`.
Qualquer resultado do Maven anexa o HTML no artefato `jacoco-report`.
A retenção é de 7 dias.

O mínimo é `COVEREDRATIO` 1 em instrução, ramo, linha e método.
O plugin exclui `BackendApplication` e `ValidationErrorResponse`.
A execução local está em [Comandos](../development/commands.md).

### Qualidade dos scripts

Os gatilhos são os mesmos do verify.
Um job instala `shellcheck` no Ubuntu e analisa os scripts.
O outro job testa o script da política de branches.

O GitHub só bloqueia o merge quando os status checks obrigatórios passam.
Configure ao menos os dois checks de produto: nomenclatura e JaCoCo.
Os jobs de script são checks adicionais recomendados.

## Proteção no GitHub

Esta configuração não está versionada no repositório.
Faça os passos no GitHub.

1. Em Settings, General, Default branch, escolha `develop`.
2. No clone local, rode `git remote set-head origin develop`.
3. Crie a regra de proteção, ou o ruleset, só para `develop`.
4. Apague `main`, `master` e `development` no remoto, se ainda existirem.
5. Restrinja quem cria Release pela UI ou por `gh release create vX.Y.Z --target <sha>`.

Marque estas exigências na proteção de `develop`.

- Exija pull request antes do merge.
- Exija os checks `Validar nomenclatura e fluxo de branches` e `Testes unitários e cobertura (JaCoCo)`.
- Os nomes aparecem na UI depois da primeira execução.
- Separar os workflows não muda esses nomes.
- Opcional: `ShellCheck dos scripts` e `Testes do script de política de branches`.
- Opcional: exija a branch atualizada antes do merge.
- Impeça bypass para quem não deve ignorar a regra.
- Restrinja o push direto em `develop`, ou desligue o push direto.

### Push direto em branch de trabalho

No GitHub.com, o bloqueio antes do update remoto depende do plano e de ruleset.
O workflow já roda em todo push e falha o check da branch inválida.
Trate a branch inválida como erro de processo: apague a branch e oriente o autor.
Quando o plano permitir, use [rulesets](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets) para restringir nomes.
No GitHub Enterprise, um hook também pode bloquear o push.

### Tipo de merge

O Actions não escolhe squash, merge commit ou rebase.
Configure isso em Settings, General, Pull Requests.
O time define o padrão e registra a escolha no guia interno.

## Fluxo do autor

1. Atualize `develop` com `git fetch` e `git pull`.
2. Crie a branch com `git checkout -b feat/descricao-curta`.
3. Faça os commits e rode `git push -u origin feat/descricao-curta`.
4. Abra o PR para `develop` já com assignee: `gh pr create --assignee @me`.
5. Espere revisão e CI verde.
6. Faça o merge em `develop`.
7. Para marcar versão, crie a Release no SHA: `gh release create vX.Y.Z --target develop`.
8. Para corrigir tag antiga que não é o HEAD, crie `hotfix/…` a partir da tag, abra PR para `develop` e publique outra Release.

## Como mudar a regra

Altere só [`.github/scripts/validate-branch-policy.sh`](../../.github/scripts/validate-branch-policy.sh).
Os padrões ficam nas constantes `*_REGEX`.
A lógica de PR fica em `validate_pull_request`.
Abra um PR depois da mudança.
Confira o check `Validar nomenclatura e fluxo de branches`.
Confira também `validate-branch-policy.test.sh`.

## Referências

| Artefato | Caminho |
| --- | --- |
| Workflow da política | `.github/workflows/branch-policy.yml` |
| Workflow de testes e JaCoCo | `.github/workflows/verify.yml` |
| Workflow dos scripts | `.github/workflows/scripts.yml` |
| Script de validação | `.github/scripts/validate-branch-policy.sh` |
| Testes do script | `.github/scripts/validate-branch-policy.test.sh` |
| Perfil Spring do CI | `src/test/resources/application-CI.yml` |
| Limites JaCoCo e Surefire | `pom.xml` |
| Este arquivo | `docs/policies/branch_policy.md` |

## Glossário

| Termo | Sentido |
| --- | --- |
| Base | Branch que recebe o merge. No fluxo diário, é `develop`. |
| Head | Branch que envia os commits. Exemplo: `feat/…` ou `hotfix/…`. |
| Assignee | Usuário GitHub responsável pelo PR. A política exige ao menos um. O check do Actions é o gate. |
| Release | GitHub Release mais a tag `vX.Y.Z` num SHA de `develop`. Não é uma branch. |
| Status check obrigatório | Configuração que impede o merge até o job passar. |
