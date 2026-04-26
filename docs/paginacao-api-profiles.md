# Paginação da API de perfis (`GET /profiles`)

Este documento descreve como percorrer páginas de resultados expostas pelo endpoint de listagem de perfis, incluindo filtros por nome, ordenação e cursores.

## Endpoint

| Método | Caminho     | Conteúdo |
|--------|-------------|----------|
| `GET`  | `/profiles` | JSON (`Pagination` com lista de `Profile` na visão protegida) |

Parâmetros são sempre *query parameters*.

## Parâmetros

| Parâmetro       | Obrigatório | Padrão  | Descrição |
|-----------------|-------------|---------|-----------|
| `name`          | Não         | —       | Se preenchido (texto não vazio), filtra perfis cujo **nome contém** o valor (comparação insensível a maiúsculas/minúsculas, conforme regras do domínio). Se omitido ou vazio, lista **todos** os perfis. |
| `limit`         | Não         | `100`   | Quantidade máxima de itens na página. Deve respeitar o limite máximo configurado na aplicação (`sajitar.domain.validation.limit.max`). |
| `reverse`       | Não         | `false` | `false`: ordenação **ascendente** por `(nome normalizado, id)`. `true`: ordenação **descendente** pelo mesmo critério. |
| `lastSeenName`  | Não*        | —       | Parte do **cursor** para a página seguinte (ou intermediária). |
| `lastSeenId`    | Não*        | —       | UUID associado ao cursor; deve corresponder ao perfil referenciado por `lastSeenName`. |

\* **Primeira página:** não envie `lastSeenName` nem `lastSeenId` (ou envie ambos de forma que a API trate como início de lista — o comportamento oficial é omitir o cursor).

**Páginas seguintes:** envie **sempre os dois** (`lastSeenName` e `lastSeenId`) juntos, obtidos do **último** elemento do array `content` da resposta anterior (campos `name` e `id` desse perfil). Enviar só um dos dois pode levar a resultados inconsistentes com a intenção de “primeira página” vs. continuação.

## Modelo de paginação: cursor (chave de ordenação)

A API não usa `page`/`offset`. Ela usa **paginação por cursor** sobre a ordenação estável `(nome purificado, id)`:

- **Ascendente** (`reverse=false`): próximos itens são os que vêm **depois** do par `(lastSeenName, lastSeenId)` na ordenação.
- **Descendente** (`reverse=true`): próximos itens são os que vêm **antes** desse par na ordenação ascendente, ou seja, a janela “anda” no sentido decrescente.

Na prática, para **avançar** na sequência que está a ver:

1. Faça o primeiro pedido **sem** cursor, com o `name` (se quiser filtrar), `limit` e `reverse` desejados.
2. Na resposta, pegue o **último** item de `content` (se a lista não estiver vazia).
3. O próximo pedido repete **os mesmos** `name`, `limit` e `reverse`, e acrescenta:
   - `lastSeenName` = `name` desse último item;
   - `lastSeenId` = `id` desse último item.

Repita o passo 2–3 enquanto `followingElements > 0` (ver abaixo).

## Por que paginação por cursor? (desempenho em grande volume)

Em bases com **muitos registos**, a abordagem clássica `OFFSET` / “página *n*” obriga o motor a **saltar e descartar** todas as linhas anteriores antes de devolver a janela pedida. Esse custo cresce com o número da página: a página 10 000 pode forçar o sistema a percorrer milhões de linhas só para as ignorar, com tempo de resposta e carga no disco/CPU difíceis de prever.

A paginação **por cursor** (como nesta API) fixa uma posição na ordenação estável `(nome, id)` e pede “os próximos *limit* registos **depois** deste par”. O índice ou o plano de execução pode **localizar o ponto de partida** e ler só o bloco necessário, com custo mais estável em relação ao “quão longe” o utilizador navegou na lista, em vez de proporcional ao offset.

Outros efeitos práticos:

- **Menos duplicados ou saltos** quando inserem ou removem linhas entre pedidos (com *offset*, a página “muda de composição” sob os pés do cliente).
- **Sem parâmetro de página** explícito: o contrato é “continuar a partir deste último elemento que já mostrei”, alinhado ao que o utilizador viu.

Em resumo: para **grandes volumes** e listas percorridas em profundidade, cursor tende a escalar melhor e de forma mais previsível do que `LIMIT/OFFSET` na listagem de perfis.

## Corpo da resposta (`Pagination`)

O JSON segue a estrutura abaixo (nomes de campos típicos do Jackson com getters Lombok):

| Campo                 | Tipo    | Significado |
|-----------------------|---------|-------------|
| `content`             | array   | Perfis desta “página” (até `limit` elementos). |
| `precedingElements`   | número  | Quantidade de elementos **antes** da janela atual na mesma ordenação e filtro. Na primeira página costuma ser `0`; em páginas seguintes reflete o que existe “atrás” do primeiro item retornado. |
| `followingElements`   | número  | Quantidade de elementos **depois** do **último** item desta página (ainda na mesma ordenação e filtro). Se for `0`, não há próxima página na direção “para a frente” dessa lista. |
| `reversed`            | boolean eco do pedido: `true` se a listagem está em ordem descendente. |

Cada elemento de `content` expõe os campos de `Profile` permitidos pela visão **protegida** (por exemplo `id`, `name`, `description`, `birthday`, `email`), **sem** dados privados como senha.

## Como saber se há próxima página

Use o campo **`followingElements`**:

- `followingElements > 0`: existe pelo menos um perfil após o último da página atual; pode montar o próximo `GET` com cursor a partir do **último** item de `content`.
- `followingElements == 0`: não há mais resultados nessa direção; não deve repetir o mesmo cursor esperando novos itens.

O campo **`precedingElements`** é útil para interfaces que mostram “quantos registos existem antes desta página” ou para implementar **retrocesso** na navegação (ver secção seguinte).

## Voltar à página anterior

O endpoint só expõe continuação “**para a frente**” usando o **último** elemento da página atual como cursor. **Não** existe um parâmetro `page` nem “cursor para trás”; o cliente tem de **reconstruir** o pedido da página anterior.

### Estratégia recomendada: pilha de cursores do pedido atual

A ideia é guardar, a cada **avanço**, o cursor que **identifica o pedido que produziu a página em que está** (não o cursor do *próximo* passo). Assim, ao recuar, volta a enviar o mesmo `GET` que já tinha montado para essa página.

Defina uma variável **`requestCursorForThisPage`**: o par `(lastSeenName, lastSeenId)` que foi enviado no **último** `GET` bem-sucedido e que gerou a página que o utilizador vê agora; na **primeira** página esse valor é “ausente” (equivalente a não enviar cursor).

Mantenha uma **pilha** `cursorStack` de entradas do tipo “cursor usado no pedido da página **antes** de avançarmos”, ou seja, o valor de `requestCursorForThisPage` **antes** de cada operação “próxima página”.

#### Avançar (“próxima página”)

1. Seja `edge` o **último** elemento de `content` da página atual (campos `name` e `id`).
2. **Empilhe** uma cópia do estado atual de `requestCursorForThisPage` (na primeira página, empilhe “vazio” / ausência de cursor).
3. Faça `GET /profiles?...&lastSeenName=<edge.name>&lastSeenId=<edge.id>` com o mesmo `name`, `limit` e `reverse` de sempre.
4. Após sucesso, atualize `requestCursorForThisPage` para `{ lastSeenName: edge.name, lastSeenId: edge.id }` — isto é o cursor **enviado** neste pedido, ou seja, o que define a página que acabou de ser carregada.

#### Recuar (“página anterior”)

1. Se a pilha estiver vazia, não há página anterior na navegação atual.
2. **Desempilhe** um valor `popped` (o `requestCursorForThisPage` que tinha antes de ter ido para a página onde está agora).
3. Se `popped` for vazio (primeira página da sequência), faça `GET /profiles` **sem** `lastSeenName`/`lastSeenId` e defina `requestCursorForThisPage` de novo como vazio.
4. Caso contrário, faça `GET /profiles?...&lastSeenName=<popped.lastSeenName>&lastSeenId=<popped.lastSeenId>` com os mesmos `name`, `limit` e `reverse`.
5. Após sucesso, defina `requestCursorForThisPage = popped` (o cursor desse pedido é precisamente o que restaura a página anterior).

#### Exemplo de estados (ordem ascendente, `limit` fixo)

| Passo | Página mostrada | `requestCursorForThisPage` após o passo | `cursorStack` após o passo |
|--------|-----------------|------------------------------------------|----------------------------|
| Busca inicial (sem cursor) | 1 | vazio | `[]` |
| Próxima | 2 | último item da pág. 1 (enviado como cursor neste pedido) | `[ vazio ]` |
| Próxima | 3 | último item da pág. 2 | `[ vazio, último item da pág. 1 ]` |
| Anterior | 2 | último item da pág. 1 (valor desempilhado) | `[ vazio ]` |
| Anterior | 1 | vazio | `[]` |

Sempre que o utilizador fizer uma **nova busca** (mudou filtro, `limit` ou `reverse`), limpe a pilha e `requestCursorForThisPage` e volte ao fluxo da primeira página.

### Alternativa mais simples (e mais pesada)

Voltar sempre ao **primeiro** `GET` (sem cursor) e repetir **n** vezes o avanço até à página desejada. Funciona sem pilha, mas o custo em rede e no servidor cresce linearmente com o índice da página — aceitável só para depuração ou listas pequenas.

Em qualquer estratégia, mantenha **`name`**, **`limit`** e **`reverse`** iguais aos da navegação em que os cursores foram obtidos.

## Exemplos de sequência

### 1. Primeira página (todos os perfis, ascendente, 10 itens)

```http
GET /profiles?limit=10&reverse=false
```

### 2. Segunda página (cursor = último item da primeira resposta)

Supondo que o último item da primeira página tenha `name=Alice` e `id=019a0000-0000-7000-0000-000000000001`:

```http
GET /profiles?limit=10&reverse=false&lastSeenName=Alice&lastSeenId=019a0000-0000-7000-0000-000000000001
```

### 3. Busca por nome com paginação

```http
GET /profiles?name=silva&limit=5&reverse=false
```

Próxima página: acrescentar `lastSeenName` e `lastSeenId` do último elemento de `content`, **mantendo** `name=silva`, `limit=5` e `reverse=false`.

### 4. Ordem descendente

```http
GET /profiles?limit=20&reverse=true
```

O cursor continua a ser o **último** item de `content` em cada resposta; a ordenação subjacente é a mesma chave `(nome, id)`, apenas invertida.

## Boas práticas para clientes

1. **Consistência:** não misture cursores obtidos com um `name`/`reverse`/`limit` com pedidos que alterem esses valores sem voltar à primeira página (sem cursor).
2. **Tamanho da página:** escolha um `limit` estável entre pedidos da mesma navegação.
3. **Erros HTTP:** trate respostas de erro (4xx/5xx) e corpos vazios conforme a política da sua aplicação; valide também listas `content` vazias com `200 OK`.
4. **Codificação:** use *percent-encoding* em `name` e `lastSeenName` se contiverem caracteres especiais na URL.

## Referência no código

- Controlador: `ProfileController#getProfiles`
- Agregação da resposta: `Pagination`
- Regras de consulta e ordenação: `ProfileRepository` (queries nativas com `order by (name_purified, id)` asc/desc e condições `>` / `<` no cursor)
