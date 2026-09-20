#!/usr/bin/env bash
# Valida nomenclatura de branches e fluxo base ↔ alvo em PRs.
# Falha com exit 1 quando a política é violada (o job do Actions fica vermelho).
# Evento desconhecido: exit 2.
#
# Uso:
#   bash validate-branch-policy.sh push <branch>
#   bash validate-branch-policy.sh push <tag> tag
#   bash validate-branch-policy.sh workflow_dispatch <branch>
#   bash validate-branch-policy.sh pull_request <head> <base>
# Sem argumentos, usa EVENT_NAME, PUSH_REF_NAME, REF_TYPE, HEAD_REF e BASE_REF.

set -euo pipefail

# --- Única ramificação de longa duração (nome exato permitido em push) ---
PROTECTED_BRANCHES_REGEX='^develop$'

# --- Aliases e ponteiros do fluxo antigo: PR deve retargetar develop ---
LEGACY_LONG_LIVED_REGEX='^(main|master|development)$'

# --- Branches de trabalho: prefixo/descrição (mínimo um segmento após /) ---
WORK_BRANCH_REGEX='^(feat|feature|fix|bugfix|docs|chore|refactor|test|ci|perf)/.+'

# --- Correção de uma tag já publicada que não é o HEAD de develop ---
HOTFIX_BRANCH_REGEX='^hotfix/.+'

# --- Automação ---
DEPENDABOT_REGEX='^dependabot/'

# --- GitHub Release: tag imutável num SHA de develop (sem prerelease) ---
VERSION_TAG_REGEX='^v[0-9]+\.[0-9]+\.[0-9]+$'

log_err() {
  if [[ "${GITHUB_ACTIONS:-}" == "true" ]]; then
    echo "::error::$*" >&2
  else
    echo "error: $*" >&2
  fi
}

print_usage() {
  echo "Uso: $0 push <branch>" >&2
  echo "     $0 push <tag> tag" >&2
  echo "     $0 workflow_dispatch <branch>" >&2
  echo "     $0 pull_request <head> <base>" >&2
  echo "Sem argumentos, usa EVENT_NAME, PUSH_REF_NAME, REF_TYPE, HEAD_REF e BASE_REF." >&2
}

is_protected_name() {
  [[ "$1" =~ $PROTECTED_BRANCHES_REGEX ]]
}

is_legacy_long_lived() {
  [[ "$1" =~ $LEGACY_LONG_LIVED_REGEX ]]
}

is_work_branch() {
  [[ "$1" =~ $WORK_BRANCH_REGEX ]]
}

is_hotfix() {
  [[ "$1" =~ $HOTFIX_BRANCH_REGEX ]]
}

is_dependabot() {
  [[ "$1" =~ $DEPENDABOT_REGEX ]]
}

is_version_tag() {
  [[ "$1" =~ $VERSION_TAG_REGEX ]]
}

validate_push_tag() {
  local tag="$1"

  if [[ -z "$tag" ]]; then
    log_err "Nome da tag vazio no evento push."
    return 1
  fi

  if is_version_tag "$tag"; then
    return 0
  fi

  log_err "Push rejeitado pela política: a tag '$tag' não segue a nomenclatura de GitHub Release (use vX.Y.Z)."
  return 1
}

validate_push() {
  local name="$1"

  if [[ "${REF_TYPE:-branch}" == "tag" ]]; then
    validate_push_tag "$name"
    return
  fi

  validate_push_branch "$name"
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

  if is_work_branch "$branch" || is_hotfix "$branch"; then
    return 0
  fi

  log_err "Push rejeitado pela política: a branch '$branch' não segue a nomenclatura (use feat/, fix/, hotfix/, etc.)."
  return 1
}

validate_pull_request() {
  local head="$1"
  local base="$2"

  if [[ -z "$head" || -z "$base" ]]; then
    log_err "PR sem head_ref ou base_ref; não foi possível validar o fluxo."
    return 1
  fi

  if is_dependabot "$head"; then
    :
  elif is_protected_name "$head"; then
    log_err "A branch de origem do PR não deve ser a branch protegida ('$head'). Use uma branch de trabalho (feat/, fix/, …), hotfix/* ou dependabot/ → develop."
    return 1
  elif ! { is_work_branch "$head" || is_hotfix "$head"; }; then
    log_err "Branch de origem '$head' com nomenclatura inválida. Use prefixos: feat/, fix/, docs/, chore/, hotfix/, etc."
    return 1
  fi

  if is_legacy_long_lived "$base"; then
    log_err "PR para '$base' não é permitido. A única branch longa é develop; retargete o PR para develop."
    return 1
  fi

  # develop e demais bases: a origem já foi validada acima
  return 0
}

if [[ $# -ge 1 ]]; then
  EVENT_NAME="$1"
  case "$EVENT_NAME" in
    push|workflow_dispatch)
      if [[ $# -ge 2 ]]; then
        PUSH_REF_NAME="$2"
      fi
      if [[ $# -ge 3 ]]; then
        REF_TYPE="$3"
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
    validate_push "${PUSH_REF_NAME:-}"
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
