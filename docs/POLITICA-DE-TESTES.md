# Política de testes (Sajitar Backend)

Este documento é o **artefato de referência** do time para planejamento, execução e evidência de testes no repositório **sajitar-backend**. Foi elaborado para **alinhar práticas** aos objetivos da série **ISO/IEC 29119** (conceitos, processos e documentação de teste), **sem** equivaler a certificação ou auditoria externa automática.

---

## 1. Objetivo

- Definir **níveis de teste** usados, **critérios mínimos** para integração contínua e **o que se espera em pull request / release**.
- Estabelecer **rastreabilidade** entre trabalho de produto (requisito ou critério de aceite) e evidência de verificação (testes automatizados e, quando existir, testes manuais).
- Complementar a [**política de branches e CI**](POLITICA-DE-BRANCHES.md), que descreve **quando** os testes rodam no GitHub Actions.

---

## 2. Alinhamento com ISO/IEC 29119 (informativo)

| Tema da norma (visão resumida) | Como o time aplica neste repositório |
|--------------------------------|--------------------------------------|
| Vocabulário e conceitos comuns | Uso consistente de “nível de teste”, “caso de teste”, “defeito” e “critério de aceite” (seção 3 e glossário abaixo). |
| Processo de teste | PR com checklist de testes; CI obrigatório conforme proteção de branch; decisões de release documentadas pelo fluxo acordado (Git Flow + revisão). |
| Documentação de teste | Este arquivo + descrição no PR + rastreio mínimo (seção 6); relatório JaCoCo como evidência de cobertura (seção 5). |

**Limitação explícita:** a conformidade plena com a ISO/IEC 29119 depende de **processo organizacional** (papéis, aprovações, registros fora do Git). Este documento cobre o **âmbito do backend** e o que fica **versionado** no repositório.

---

## 3. Níveis de teste no projeto

| Nível | Finalidade | Onde aparece hoje |
|-------|------------|-------------------|
| **Componente / unitário** | Validar regras de domínio, validações, use cases (portas mockadas), configuration, handler e Jackson, **sem** subir a aplicação. | Classes `*Test` em `src/test/java` espelhando o pacote de produção; **JUnit 5**, **AssertJ**, Mockito, `@ParameterizedTest`; fixtures em `src/test/resources/fixtures/*.json` e `*ConstraintFixture`. Não usar `@SpringBootTest` para regra pura. |
| **Integração (API HTTP)** | Validar contratos de endpoints (status, corpo JSON, validação, i18n `lang`) com a aplicação em contexto Spring e PostgreSQL. | Só `ProfileControllerIntegrationTest` (`@SpringBootTest` + MockMvc). CI com **PostgreSQL** (`postgres:latest` como serviço) antes de `./mvnw verify` — ver [`.github/workflows/branch-policy.yml`](../.github/workflows/branch-policy.yml). |
| **Contexto Spring** | Garantir que a aplicação sobe com a configuração de teste. | `BackendApplicationTests` (`@SpringBootTest`, `contextLoads`). |

**Decisões conscientes:** se um nível **não** for usado (por exemplo testes de contrato dedicados fora do Spring, testes de carga ou E2E com browser), registre no PR ou na issue do épico o **motivo** ou o **plano** (data ou condição) para introduzi-lo.

---

## 4. Ferramentas e padrões de implementação

- **Framework:** JUnit 5 (`spring-boot-starter-test`).
- **Asserções:** preferir AssertJ; mensagens claras em `as(...)` quando ajudar o diagnóstico.
- **Legibilidade:** `@DisplayName` e estrutura **given / when / then** nos comentários ou na organização do método, alinhados ao estilo já usado em testes de validação.
- **Dados:** cenários repetíveis em JSON em `src/test/resources/fixtures/` quando reduzir duplicação e facilitar revisão.
- **Configuração de teste:** `src/test/resources/application.yml` (e demais recursos em `src/test/resources`).

---

## 5. Integração contínua e cobertura

- **Workflow:** [`.github/workflows/branch-policy.yml`](../.github/workflows/branch-policy.yml) — job **“Testes unitários e cobertura (JaCoCo)”** após a política de branches.
- **Ambiente no CI:** JDK 26 (Eclipse Temurin) no runner e PostgreSQL como serviço.
- **Comando:** `./mvnw verify` (Surefire + JaCoCo *report* e *check*).
- **Cobertura:** limiares agregados (**BUNDLE**) nas propriedades `jacoco.coverage.minimum.*` do [`pom.xml`](../pom.xml): `COVEREDRATIO` **1** (**100%**) em instrução, ramo, linha e método. Exclusões no plugin: `BackendApplication` e `ValidationErrorResponse` — não ampliar.
- **Evidência após falha:** artefato `jacoco-report` no job; localmente: `target/site/jacoco/index.html` após `./mvnw verify` (ver [README](../README.md)).

**Execução local:** PostgreSQL em `127.0.0.1:5432` e variáveis de ambiente exigidas por `src/main/resources/application.yml` (o CI usa credenciais `sajitar_ci`; no host também é possível alinhar ao `local.env` do Docker Compose). Detalhes na seção **Testes e cobertura** do README.

---

## 6. Rastreabilidade mínima (obrigatória em PR com mudança de comportamento)

Para cada PR que altere **comportamento observável** (API, validação, persistência, segurança, desempenho crítico), inclua no corpo do PR (ou link para a issue) uma das formas abaixo.

### 6.1 Tabela sugerida (copiar e preencher)

| Referência (issue / épico / critério de aceite) | Evidência de teste (classe#método ou `@DisplayName`) | Notas |
|-------------------------------------------------|------------------------------------------------------|-------|
| | | |

Regras:

- **Toda** nova regra de negócio ou validação deve ter **pelo menos** um teste automatizado que falhe se a regra for removida inadvertidamente.
- Se não houver teste novo, explique **por quê** (ex.: apenas refatoração mecânica sem mudança de comportamento) e aponte regressão coberta por testes existentes.

---

## 7. Defeitos (incidentes de teste / bugs)

- Registrar no rastreador do time (GitHub Issues ou ferramenta acordada) com: **passos para reproduzir**, **comportamento esperado vs. atual**, **ambiente** (commit, branch, perfil).
- Ao corrigir: referenciar o defeito no PR; após merge, **confirmar reteste** (CI verde e, se aplicável, teste manual).

---

## 8. Critérios de entrada e saída (resumo)

| Momento | Entrada mínima | Saída / evidência |
|---------|----------------|-------------------|
| **Abrir PR** | Descrição clara da mudança; para mudança de comportamento, tabela da seção 6. | Checks obrigatórios do repositório (incluindo testes + JaCoCo conforme configuração atual). |
| **Merge para branch protegida** | Revisão conforme política do time; checks verdes. | Histórico do PR e commit no branch de destino. |
| **Release / deploy** | Acordo do time com critérios extras (ex.: sem issues bloqueantes, changelog). | Registro na ferramenta de release ou notas de versão. |

---

## 9. Melhoria contínua

- Após incidente em produção ou falha grave escapada pela suíte atual: registrar **lição aprendida** e, se couber, issue para novo teste ou ajuste de limiar/cenário.
- Revisar **anualmente** (ou a cada marco maior) se os níveis da seção 3 ainda refletem o risco do produto.

---

## 10. Glossário rápido

- **Caso de teste:** condição de entrada, ação e resultado esperado — no código, tipicamente um método de teste com nome ou `@DisplayName` expressivo.
- **Suíte de regressão:** conjunto executado no CI a cada alteração relevante (`./mvnw verify` no workflow acordado).
- **Defeito:** comportamento em desacordo com requisito ou especificação acordada.

---

## 11. Referências externas

- [ISO/IEC/IEEE 29119-1](https://www.iso.org/standard/45142.html) — conceitos e vocabulário (editions may vary).
- Política de branches e CI: [POLITICA-DE-BRANCHES.md](POLITICA-DE-BRANCHES.md).
