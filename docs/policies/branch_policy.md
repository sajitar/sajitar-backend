# Política de branches e integração contínua

Este documento descreve o modelo de branches, o fluxo de merges, como o GitHub Actions valida as regras e o que você precisa configurar no GitHub para **impedir** que código fora do padrão chegue ao repositório.

---

## 1. Objetivo

- Garantir **nomenclatura consistente** (`feat/`, `fix/`, `hotfix/`, etc.).
- Garantir que **todo PR tenha assignee** (pelo menos um responsável).
- Garantir **fluxo de integração previsível**: trabalho diário integra em `develop`; cada versão publicada é um **GitHub Release** (tag) num SHA dessa branch.
- Falhar o pipeline quando algo estiver incorreto, de forma que — com **proteção de branch** — merges e pushes inválidos fiquem bloqueados.
- Rodar **em paralelo** (sem `needs` entre workflows) a política de branches, os **testes** com **JaCoCo** e a qualidade dos scripts de CI; falhar se os testes quebrarem ou se a **cobertura mínima** não for atingida.

Ver também a [**política de testes**](test_policy.md) (níveis de teste, rastreabilidade em PR e alinhamento à ISO/IEC 29119).

---

## 2. Modelo de branches

Uma única branch de longa duração. Versão publicada não é uma segunda branch: é uma tag + GitHub Release.

| O que | Função |
|--------|--------|
| `develop` | Única branch longa, protegida e default do repositório. Destino de todo merge do dia a dia. |
| `feat/*`, `fix/*`, … | Branches de trabalho partindo de `develop`. |
| `hotfix/*` | Correção de uma **tag já publicada** que não é o HEAD de `develop`. |
| GitHub Release (`vX.Y.Z`) | Snapshot imutável de um SHA de `develop` (notas + artefatos). |

Não existem `main`, `master`, `development` nem `release/*`. Produção não recebe PR: recebe uma Release criada no SHA desejado.

### 2.1 Nomenclatura permitida em **push**

Além de `develop`, são aceitas branches que sigam um destes padrões (com **pelo menos** um segmento após a barra):

| Prefixo | Uso típico |
|---------|------------|
| `feat/` ou `feature/` | Nova funcionalidade. |
| `fix/` ou `bugfix/` | Correção de defeito. |
| `docs/` | Apenas documentação. |
| `chore/` | Manutenção, tooling, tarefas sem impacto direto em funcionalidade. |
| `refactor/` | Refatoração sem mudança de comportamento pretendida. |
| `test/` | Testes. |
| `ci/` | Pipelines e automação. |
| `perf/` | Melhoria de desempenho. |
| `hotfix/` | Correção de uma tag já publicada (não o HEAD de `develop`). |

Também são aceitas branches `dependabot/…` (integrações automáticas de dependências, quando o Dependabot estiver configurado no repositório).

**Exemplos válidos:** `feat/login-oauth`, `fix/null-pointer-export`, `hotfix/session-leak`
**Exemplos inválidos:** `minha-branch`, `feature` (sem `/`), `FIX-bug` (prefixo fora da lista e maiúsculas não padronizadas), `main`, `master`, `development`, `release/2.4.0`, `v0.0.2` (tag de Release, não nome de branch).

Push de **tag** (GitHub Release) é outro caso: só `vX.Y.Z` com três números inteiros e **sem** prerelease (`v0.0.2` ok; `v0.0.2-rc.1`, `0.0.2` e `release/2.4.0` não). O workflow distingue tag de branch com `github.ref_type`.

Os padrões exatos estão em [`.github/scripts/validate-branch-policy.sh`](../../.github/scripts/validate-branch-policy.sh).

---

## 3. Fluxo de merges e “base” das branches

### 3.1 Pull requests para `develop`

- **Origem esperada:** branches de trabalho com prefixos `feat/`, `feature/`, `fix/`, `bugfix/`, `docs/`, `chore/`, `refactor/`, `test/`, `ci/`, `perf/`; `hotfix/*` ao devolver uma correção de tag para a linha viva; ou PRs `dependabot/*`.
- **Não use** `develop` como origem (“head”) de um PR. O fluxo é sempre trabalho (ou hotfix) em branch nomeada → PR → `develop`.
- **Assignee obrigatório:** o PR precisa de pelo menos um responsável (em geral o autor, `--assignee @me`). Sem assignee o check **“Validar nomenclatura e fluxo de branches”** falha — inclusive em PRs `dependabot/*`. Atribuir ou remover responsável reexecuta o workflow (`assigned` / `unassigned`).

### 3.2 Pull requests para `main`, `master` ou `development`

Não são permitidos. A única branch longa é `develop`; retargete o PR. O script falha com essa orientação para fechar o fluxo antigo, em vez de tratar esses nomes como “qualquer outra base”.

### 3.3 Outras branches como base de PR

Se alguém abrir PR para uma branch que **não** é `develop` nem os nomes legado da seção 3.2, o workflow exige que a **branch de origem** tenha nomenclatura válida (prefixos da tabela acima ou `dependabot/`) e que o PR tenha **assignee**. Ajuste esse comportamento no script se o time usar fluxos adicionais (por exemplo `staging` dedicada).

### 3.4 GitHub Release

O gate de código é o PR em `develop` (revisão + checks). O gate de “foi para produção” é **quem pode criar a Release** (permissão no repositório ou environment no GitHub).

- Snapshot do HEAD atual: `gh release create vX.Y.Z --target develop` (ou a UI **Releases → Draft a new release**, tag no commit de `develop`).
- Snapshot de um SHA específico: `gh release create vX.Y.Z --target <sha>`.
- Hotfix de uma versão já publicada: `git switch -c hotfix/slug vX.Y.Z`, PR para `develop`, e uma Release nova (`vX.Y.Z+1` ou patch) no SHA combinado.

Não há branch `release/*` nem PR `develop` → `main`.

---

## 4. O que o GitHub Actions faz

Três workflows **independentes** (sem `needs` entre si). Falha de nomenclatura **não** esconde falha de teste, e vice-versa.

| Workflow | Arquivo | Check na UI |
|----------|---------|-------------|
| **Política de branches** | [`.github/workflows/branch-policy.yml`](../../.github/workflows/branch-policy.yml) | **Validar nomenclatura e fluxo de branches** |
| **Testes e cobertura** | [`.github/workflows/verify.yml`](../../.github/workflows/verify.yml) | **Testes unitários e cobertura (JaCoCo)** |
| **Qualidade dos scripts** | [`.github/workflows/scripts.yml`](../../.github/workflows/scripts.yml) | **ShellCheck dos scripts** e **Testes do script de política de branches** |

### 4.1 Política de branches (`branch-policy.yml`)

Dispara em **todo push** (branches e tags), em **pull request** (`opened`, `synchronize`, `reopened`, `edited`, `assigned`, `unassigned`) e em `workflow_dispatch` (trata o dispatch como push da ref atual).

- **push / workflow_dispatch de branch:** valida o **nome** da branch (seção 2.1).
- **push / workflow_dispatch de tag:** aceita só `vX.Y.Z`; outras tags falham. `verify.yml` e `scripts.yml` **não** disparam em tag.
- **pull request:** valida o **nome** da branch de origem, o **par base ↔ origem** e a presença de **assignee** (seções 2 e 3). Sem responsável o check falha; `assigned` / `unassigned` reexecutam a validação.

Implementação: [`.github/scripts/validate-branch-policy.sh`](../../.github/scripts/validate-branch-policy.sh) (também via CLI local: `bash .github/scripts/validate-branch-policy.sh push feat/exemplo`, `bash .github/scripts/validate-branch-policy.sh push v0.0.2 tag` ou `bash .github/scripts/validate-branch-policy.sh pull_request feat/exemplo develop alice`). Testes em [`.github/scripts/validate-branch-policy.test.sh`](../../.github/scripts/validate-branch-policy.test.sh). Se falhar, o check **“Validar nomenclatura e fluxo de branches”** fica vermelho.

### 4.2 Testes e cobertura (`verify.yml`)

Dispara em **pull request** (qualquer base) e em **push** só para `develop` (evita Maven duplicado no mesmo commit de um PR). Também `workflow_dispatch`. Em `edited`, **não** reexecuta o Maven se só título ou corpo mudaram; **reexecuta** se a **base** do PR mudou.

- Sobe **PostgreSQL** (`postgres:18.6`) como *service container* (necessário porque a API usa SQL nativo com funções PostgreSQL; configuração complementar em [`src/test/resources/application.yml`](../../src/test/resources/application.yml)) e **Redis** (`redis:8.2.9-alpine`, sessões de `/tokens`) em um passo próprio, já que a ACL vem do repositório e só existe depois do checkout. Credenciais iguais a [`.github/scripts/ci.env`](../../.github/scripts/ci.env).
- Configura **JDK 26** (Eclipse Temurin) via `actions/setup-java` antes de `./mvnw verify`.
- Carrega [`.github/scripts/ci.env`](../../.github/scripts/ci.env) e executa `./mvnw verify` (dependências pelo **Maven Central** via wrapper, sem `settings.xml` corporativo no runner).
- O `verify` roda **Surefire** (testes com `@SpringBootTest` e recursos em `src/test/resources`) e o **JaCoCo** (`prepare-agent` → testes → `report` + `check` no `pom.xml`).
- Em qualquer resultado do Maven, anexa o relatório HTML em **Artifacts** (`jacoco-report`, retenção 7 dias), útil quando o `check` de cobertura falha.

**Limites de cobertura** (pacote agregado) estão nas propriedades `jacoco.coverage.minimum.*` do [`pom.xml`](../../pom.xml): `COVEREDRATIO` **1** (**100%**) em instrução, ramo (`BRANCH`), linha e método. Exclusões no plugin JaCoCo: `BackendApplication` e `ValidationErrorResponse`.

**Execução local de `./mvnw verify`:** exige **PostgreSQL** em `127.0.0.1:5432`, **Redis** em `127.0.0.1:6379` e as variáveis de [`.github/scripts/ci.env`](../../.github/scripts/ci.env) (Opção A) ou as derivadas do `local.env` (Opção B). Ver a seção **Testes e cobertura** em [Comandos](../development/commands.md).

### 4.3 Qualidade dos scripts (`scripts.yml`)

Mesmos gatilhos do verify. Jobs **ShellCheck dos scripts** (pacote `shellcheck` no Ubuntu) e **Testes do script de política de branches**.

**Importante:** o GitHub **só bloqueia merge** se os *status checks* obrigatórios passarem (próxima seção). Configure pelo menos os **dois** checks de produto: nomenclatura e JaCoCo. Os jobs de scripts são recomendados como checks adicionais.

---

## 5. Como impedir que código fora do padrão suba ou seja integrado

### 5.1 Proteger `develop` e alinhar o remoto

Depois de mesclar esta política, no GitHub (não versionado neste repositório):

1. **Settings → General → Default branch** = `develop`. Localmente: `git remote set-head origin develop`.
2. **Settings → Branches → Branch protection rules** (ou ruleset) só para `develop`.
3. Apague `main`, `master` e `development` no remoto se ainda existirem.
4. Quem publica versão: GitHub UI (**Releases**) ou `gh release create vX.Y.Z --target <sha>` — restrinja essa permissão a quem deve marcar produção.

Recomendações mínimas alinhadas a esta política:

- **Require a pull request before merging** (exige revisão via PR).
- **Require status checks to pass before merging** e marque pelo menos **ambos** os checks de produto: **“Validar nomenclatura e fluxo de branches”** e **“Testes unitários e cobertura (JaCoCo)”** (nomes exibidos na UI após a primeira execução; **não** mudam ao separar os workflows). Opcional: **“ShellCheck dos scripts”** e **“Testes do script de política de branches”**.
- **Require branches to be up to date before merging** (opcional, reduz surpresas no merge).
- **Do not allow bypassing the above settings** para quem não deve ignorar regras.
- Em `develop`: **Restrict who can push** ou desabilitar push direto, forçando tudo via PR.

### 5.2 Push direto em branches de trabalho

Para que um push com nome inválido seja **bloqueado antes** de atualizar o remoto, o GitHub Enterprise permite hooks; no GitHub.com a abordagem prática é:

- Exigir o mesmo workflow em **push** e tratar branches inválidas como **erro de processo** (reverter/delete branch + orientar o autor), **ou**
- Usar regras de proteção com padrões (`feat/*`, `fix/*`, …) quando disponíveis no seu plano, complementando o Actions.

O workflow de política já roda em **todo push** e falha o check na branch; combine isso com revisão e, se possível, [rulesets](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets) para restringir nomes.

### 5.3 Squash merge, merge commit ou rebase

O Actions **não** substitui a escolha do tipo de merge: isso se configura em **Settings → General → Pull Requests** (allow squash, merge, rebase). Defina o padrão do time e documente no guia interno.

---

## 6. Fluxo resumido para o desenvolvedor

1. Atualize `develop` localmente (`git fetch` / `git pull`).
2. Crie uma branch: `git checkout -b feat/descricao-curta`.
3. Faça commits e `git push -u origin feat/descricao-curta`.
4. Abra PR **para `develop` já com assignee** (`gh pr create --assignee @me …`). O workflow de política valida nome, destino e responsável; verify e scripts sobem em paralelo.
5. Após aprovação e CI verde (nomenclatura + JaCoCo, e scripts se exigidos), faça merge em `develop`.
6. Para marcar uma versão: crie um GitHub Release no SHA desejado (`gh release create vX.Y.Z --target develop`, ou a UI). Para corrigir uma tag antiga que não é o HEAD, use `hotfix/…` a partir da tag, PR para `develop`, e uma Release nova.

---

## 7. Personalizar regras

Altere apenas [`.github/scripts/validate-branch-policy.sh`](../../.github/scripts/validate-branch-policy.sh):

- Constantes `*_REGEX` no topo do arquivo.
- Lógica em `validate_pull_request` se quiser, por exemplo, permitir `staging` como base com regras específicas, ou isentar algum bot do assignee.

Depois de mudar, abra um PR e confira o job **Validar nomenclatura e fluxo de branches** na aba Actions (e os testes em `validate-branch-policy.test.sh`).

---

## 8. Referências rápidas

| Artefato | Caminho |
|----------|---------|
| Workflow da política de branches | `.github/workflows/branch-policy.yml` |
| Workflow de testes + JaCoCo | `.github/workflows/verify.yml` |
| Workflow de qualidade dos scripts | `.github/workflows/scripts.yml` |
| Script de validação de branches | `.github/scripts/validate-branch-policy.sh` |
| Testes do script de política | `.github/scripts/validate-branch-policy.test.sh` |
| Variáveis de ambiente do CI | `.github/scripts/ci.env` |
| Limites JaCoCo / Surefire | `pom.xml` |
| Esta documentação | `docs/policies/branch_policy.md` |

---

## 9. Glossário

- **Base (do PR):** branch **para onde** o merge será feito (`develop`, por exemplo).
- **Head (do PR):** branch **de onde** vêm os commits (sua `feat/…` ou `hotfix/…`).
- **Assignee:** usuário GitHub responsável pelo PR. A política exige pelo menos um; o GitHub não tem setting nativo equivalente — o check do Actions é o gate.
- **Release:** GitHub Release + tag (`vX.Y.Z`) num SHA de `develop`; não é uma branch.
- **Status check obrigatório:** configuração que impede merge até o job do Actions passar.

Com proteção de branch + estes workflows, o repositório passa a **enforçar** nomenclatura, assignee e fluxo de integração de forma visível e repetível.
