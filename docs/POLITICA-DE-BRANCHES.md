# Política de branches e integração contínua

Este documento descreve o modelo de branches, o fluxo de merges, como o GitHub Actions valida as regras e o que você precisa configurar no GitHub para **impedir** que código fora do padrão chegue ao repositório.

---

## 1. Objetivo

- Garantir **nomenclatura consistente** (`feat/`, `fix/`, `release/`, etc.).
- Garantir **fluxo de integração previsível**: trabalho diário integra em `develop`; produção recebe alterações só por caminhos permitidos (`develop`, `release/*`, `hotfix/*`).
- Falhar o pipeline quando algo estiver incorreto, de forma que — com **proteção de branch** — merges e pushes inválidos fiquem bloqueados.
- Após a política de branches passar, rodar **testes unitários** com **JaCoCo** e falhar se os testes quebrarem ou se a **cobertura mínima** não for atingida.

---

## 2. Modelo de branches (Git Flow simplificado)

| Branch | Função |
|--------|--------|
| `main` ou `master` | Código em produção (ou refletindo o que foi liberado). |
| `develop` ou `development` | Integração contínua do time; destino padrão do dia a dia. |
| `feat/*`, `fix/*`, … | Branches de trabalho partindo tipicamente de `develop`. |
| `release/*` | Preparação de versão (congelamento, ajustes finais) antes de ir a produção. |
| `hotfix/*` | Correção urgente em produção, normalmente ramificada a partir de `main`. |

### 2.1 Nomenclatura permitida em **push**

Além de `main`, `master`, `develop` e `development`, são aceitas branches que sigam um destes padrões (com **pelo menos** um segmento após a barra):

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
| `release/` | Linha de release. |
| `hotfix/` | Hotfix de produção. |

Também são aceitas branches `dependabot/…` (integrações automáticas de dependências).

**Exemplos válidos:** `feat/login-oauth`, `fix/null-pointer-export`, `release/2.4.0`
**Exemplos inválidos:** `minha-branch`, `feature` (sem `/`), `FIX-bug` (prefixo fora da lista e maiúsculas não padronizadas).

Os padrões exatos estão em [`.github/scripts/validate-branch-policy.sh`](../.github/scripts/validate-branch-policy.sh).

---

## 3. Fluxo de merges e “base” das branches

### 3.1 Pull requests para `develop` (ou `development`)

- **Origem esperada:** branches de trabalho com prefixos `feat/`, `feature/`, `fix/`, `bugfix/`, `docs/`, `chore/`, `refactor/`, `test/`, `ci/`, `perf/`; branches `release/*` ou `hotfix/*` ao **sincronizar** `develop` após release ou hotfix em produção; ou PRs `dependabot/*`.
- **Não use** como origem diretamente `main`/`develop` como “head” de um PR (o fluxo é sempre trabalho em branch nomeada → PR → `develop`, exceto os casos de retorno de `release/*` / `hotfix/*` acima).

### 3.2 Pull requests para `main` (ou `master`)

- **Origens permitidas:**
  - `develop` ou `development` (release via integração da linha de desenvolvimento);
  - `release/*`;
  - `hotfix/*`;
  - `dependabot/*` (quando aplicável).

Assim o repositório evita que um `feat/minha-coisa` abra PR direto para produção sem passar pela política acordada.

### 3.3 Outras branches como base de PR

Se alguém abrir PR para uma branch que **não** é `main`/`master`/`develop`/`development`, o workflow exige apenas que a **branch de origem** tenha nomenclatura válida (prefixos da tabela acima ou `dependabot/`). Ajuste esse comportamento no script se o time usar fluxos adicionais (por exemplo `staging` dedicada).

---

## 4. O que o GitHub Actions faz

O workflow **“Política de branches e cobertura”** ([`.github/workflows/branch-policy.yml`](../.github/workflows/branch-policy.yml)) dispara em **push** e **pull request** (mesmos tipos de evento da política de branches).

### 4.1 Job `branch-policy` (sempre primeiro)

- **push:** valida o **nome** da branch.
- **pull request:** valida o **nome** da branch de origem e o **par base ↔ origem** (seções 2 e 3).

Implementação: [`.github/scripts/validate-branch-policy.sh`](../.github/scripts/validate-branch-policy.sh). Se falhar, o check **“Validar nomenclatura e fluxo de branches”** fica vermelho.

### 4.2 Job `unit-tests-jacoco` (só se o anterior passar)

- Declaração `needs: branch-policy`: **não executa** testes nem JaCoCo se a política de branches falhar.
- Sobe **PostgreSQL 16** como *service containers* (necessários porque a API usa SQL nativo com funções PostgreSQL; o perfil `test` está em [`src/test/resources/application-test.yml`](../src/test/resources/application-test.yml)).
- Executa `mvn verify` com [`.github/maven-ci-settings.xml`](../.github/maven-ci-settings.xml) para resolver dependências pelo **Maven Central** (evita depender de `settings.xml` corporativo no runner).
- O `verify` roda **Surefire** (testes unitários / `@SpringBootTest` com `spring.profiles.active=test`) e o **JaCoCo** (`prepare-agent` → testes → `report` + `check` no `pom.xml`).
- Em qualquer resultado, anexa o relatório HTML em **Artifacts** (`jacoco-report`), útil quando o `check` de cobertura falha.

**Limites de cobertura** (pacote agregado, exceto `ApiApplication` excluída no plugin) estão em propriedades no [`pom.xml`](../pom.xml): instrução, ramo (`BRANCH`), linha e método — valores padrão exigentes (por exemplo 80% / 75% / 80% / 75%). Ajuste `jacoco.coverage.minimum.*` se o time ainda estiver elevando a cobertura.

**Execução local de `mvn verify`:** o perfil `test` espera **PostgreSQL** em `127.0.0.1:5432` (base `sajitar_ci`, usuário/senha `sajitar_ci`/`sajitar_ci`) em `127.0.0.1:6379`.

**Importante:** o GitHub **só bloqueia merge** se os *status checks* obrigatórios passarem (próxima seção). Configure **os dois** jobs como exigidos.

---

## 5. Como impedir que código fora do padrão suba ou seja integrado

### 5.1 Proteger `main` e `develop`

1. No GitHub: **Settings → Branches → Branch protection rules**.
2. Crie regras para `main` (e `master`, se existir) e para `develop` (e `development`, se existir).

Recomendações mínimas alinhadas a esta política:

- **Require a pull request before merging** (exige revisão via PR).
- **Require status checks to pass before merging** e marque **ambos** os checks: **“Validar nomenclatura e fluxo de branches”** e **“Testes unitários e cobertura (JaCoCo)”** (nomes exibidos na UI do repositório após a primeira execução do workflow).
- **Require branches to be up to date before merging** (opcional, reduz surpresas no merge).
- **Do not allow bypassing the above settings** para quem não deve ignorar regras.
- Em `main`: **Restrict who can push** ou desabilitar push direto, forçando tudo via PR.

### 5.2 Push direto em branches de trabalho

Para que um push com nome inválido seja **bloqueado antes** de atualizar o remoto, o GitHub Enterprise permite hooks; no GitHub.com a abordagem prática é:

- Exigir o mesmo workflow em **push** e tratar branches inválidas como **erro de processo** (reverter/delete branch + orientar o autor), **ou**
- Usar regras de proteção com padrões (`feat/*`, `fix/*`, …) quando disponíveis no seu plano, complementando o Actions.

O workflow atual já roda em **push** e falha o check na branch; combine isso com revisão e, se possível, [rulesets](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets) para restringir nomes.

### 5.3 Squash merge, merge commit ou rebase

O Actions **não** substitui a escolha do tipo de merge: isso se configura em **Settings → General → Pull Requests** (allow squash, merge, rebase). Defina o padrão do time e documente no guia interno.

---

## 6. Fluxo resumido para o desenvolvedor

1. Atualize `develop` localmente (`git fetch` / `git pull`).
2. Crie uma branch: `git checkout -b feat/descricao-curta`.
3. Faça commits e `git push -u origin feat/descricao-curta`.
4. Abra PR **para `develop`**. O workflow valida nome e destino.
5. Após aprovação e CI verde (incluindo **Política de branches**), faça merge em `develop`.
6. Para liberar produção: abra PR de `develop` → `main`, ou use `release/x.y.z` / `hotfix/…` conforme o processo de release do time.

---

## 7. Personalizar regras

Altere apenas [`.github/scripts/validate-branch-policy.sh`](../.github/scripts/validate-branch-policy.sh):

- Constantes `*_REGEX` no topo do arquivo.
- Lógica em `validate_pull_request` se quiser, por exemplo, permitir `staging` como base com regras específicas.

Depois de mudar, abra um PR e confira o job **Política de branches** na aba Actions.

---

## 8. Referências rápidas

| Artefato | Caminho |
|----------|---------|
| Workflow (política + testes + JaCoCo) | `.github/workflows/branch-policy.yml` |
| Script de validação de branches | `.github/scripts/validate-branch-policy.sh` |
| Limites JaCoCo / Surefire | `pom.xml` |
| Esta documentação | `docs/POLITICA-DE-BRANCHES.md` |

---

## 9. Glossário

- **Base (do PR):** branch **para onde** o merge será feito (`develop` ou `main`, por exemplo).
- **Head (do PR):** branch **de onde** vêm os commits (sua `feat/…` ou `hotfix/…`).
- **Status check obrigatório:** configuração que impede merge até o job do Actions passar.

Com proteção de branch + este workflow, o repositório passa a **enforçar** nomenclatura e fluxo de integração de forma visível e repetível.
