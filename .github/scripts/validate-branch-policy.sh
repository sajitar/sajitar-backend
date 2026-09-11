#!/usr/bin/env bash
# Valida nomenclatura de branches e fluxo base ↔ alvo em PRs.
# Falha com exit 1 quando a política é violada (o job do Actions fica vermelho).
# Evento desconhecido: exit 2.
#
# Uso:
#   bash validate-branch-policy.sh push <branch>
#   bash validate-branch-policy.sh workflow_dispatch <branch>
#   bash validate-branch-policy.sh pull_request <head> <base>
# Sem argumentos, usa EVENT_NAME, PUSH_REF_NAME, HEAD_REF e BASE_REF.

set -euo pipefail

# --- Ramificações de longa duração (nomes exatos permitidos em push) ---
PROTECTED_BRANCHES_REGEX='^(main|master|develop|development)$'

# --- Branches de trabalho: prefixo/descrição (mínimo um segmento após /) ---
WORK_BRANCH_REGEX='^(feat|feature|fix|bugfix|docs|chore|refactor|test|ci|perf)/.+'

# --- Integração com produção ---
RELEASE_BRANCH_REGEX='^release/.+'
HOTFIX_BRANCH_REGEX='^hotfix/.+'

# --- Automação ---
DEPENDABOT_REGEX='^dependabot/'

log_err() {
  if [[ "${GITHUB_ACTIONS:-}" == "true" ]]; then
    echo "::error::$*" >&2
  else
    echo "error: $*" >&2
  fi
}

print_usage() {
  echo "Uso: $0 push <branch>" >&2
  echo "     $0 workflow_dispatch <branch>" >&2
  echo "     $0 pull_request <head> <base>" >&2
  echo "Sem argumentos, usa EVENT_NAME, PUSH_REF_NAME, HEAD_REF e BASE_REF." >&2
}

is_protected_name() {
  [[ "$1" =~ $PROTECTED_BRANCHES_REGEX ]]
}

is_work_branch() {
  [[ "$1" =~ $WORK_BRANCH_REGEX ]]
}

is_release_or_hotfix() {
  [[ "$1" =~ $RELEASE_BRANCH_REGEX || "$1" =~ $HOTFIX_BRANCH_REGEX ]]
}

is_dependabot() {
  [[ "$1" =~ $DEPENDABOT_REGEX ]]
}

validate_push_branch() {
  local branch="$1"

  if [[ -z "$branch" ]]; then
    log_err "Nome da branch vazio no evento push."
    return 1
  fi

  if is_protected_name "$branch"; then
    return 0
  fi

  if is_dependabot "$branch"; then
    return 0
  fi

  if is_work_branch "$branch" || is_release_or_hotfix "$branch"; then
    return 0
  fi

  log_err "Push rejeitado pela política: a branch '$branch' não segue a nomenclatura (use feat/, fix/, release/, hotfix/, etc.)."
  return 1
}

validate_pull_request() {
  local head="$1"
  local base="$2"

  if [[ -z "$head" || -z "$base" ]]; then
    log_err "PR sem head_ref ou base_ref; não foi possível validar o fluxo."
    return 1
  fi

  # Nomenclatura da branch de origem (develop → main é o único caso em que "head" é branch de longa duração)
  if is_dependabot "$head"; then
    :
  elif [[ ( "$base" == "main" || "$base" == "master" ) && ( "$head" == "develop" || "$head" == "development" ) ]]; then
    :
  elif is_protected_name "$head"; then
    log_err "A branch de origem do PR não deve ser uma branch protegida ('$head'), exceto PR de develop/development → main/master."
    return 1
  elif ! { is_work_branch "$head" || is_release_or_hotfix "$head"; }; then
    log_err "Branch de origem '$head' com nomenclatura inválida. Use prefixos: feat/, fix/, docs/, chore/, release/, hotfix/, etc."
    return 1
  fi

  # Fluxo alvo (base) ↔ origem (head)
  if [[ "$base" == "develop" || "$base" == "development" ]]; then
    if is_dependabot "$head"; then
      return 0
    fi
    if is_work_branch "$head"; then
      return 0
    fi
    # Retorno de release/hotfix para alinhar develop após produção
    if is_release_or_hotfix "$head"; then
      return 0
    fi
    log_err "PR para '$base' deve vir de branch de trabalho (feat/, fix/, …), release/*, hotfix/* ou dependabot/. Origem atual: '$head'."
    return 1
  fi

  if [[ "$base" == "main" || "$base" == "master" ]]; then
    if is_dependabot "$head"; then
      return 0
    fi
    if [[ "$head" == "develop" || "$head" == "development" ]]; then
      return 0
    fi
    if is_release_or_hotfix "$head"; then
      return 0
    fi
    log_err "PR para '$base' só é permitido a partir de develop/development, release/*, hotfix/* ou dependabot/. Origem atual: '$head'."
    return 1
  fi

  # Outras branches como base: exige apenas nomenclatura válida na origem (já validada acima)
  return 0
}

if [[ $# -ge 1 ]]; then
  EVENT_NAME="$1"
  case "$EVENT_NAME" in
    push|workflow_dispatch)
      if [[ $# -ge 2 ]]; then
        PUSH_REF_NAME="$2"
      fi
      ;;
    pull_request)
      if [[ $# -ge 2 ]]; then
        HEAD_REF="$2"
      fi
      if [[ $# -ge 3 ]]; then
        BASE_REF="$3"
      fi
      ;;
  esac
fi

case "${EVENT_NAME:-}" in
  push|workflow_dispatch)
    validate_push_branch "${PUSH_REF_NAME:-}"
    ;;
  pull_request)
    validate_pull_request "${HEAD_REF:-}" "${BASE_REF:-}"
    ;;
  *)
    log_err "Evento '${EVENT_NAME:-}' não é suportado."
    print_usage
    exit 2
    ;;
esac
