#!/usr/bin/env bash
# Testes do validate-branch-policy.sh (bash puro, sem Bats).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
POLICY="${SCRIPT_DIR}/validate-branch-policy.sh"

failures=0
passes=0

assert_exit() {
  local name="$1"
  local expected="$2"
  shift 2
  local actual=0
  local out
  out="$(mktemp)"
  if "$@" >"${out}" 2>&1; then
    actual=0
  else
    actual=$?
  fi
  if [[ "${actual}" -eq "${expected}" ]]; then
    echo "ok  - ${name}"
    passes=$((passes + 1))
  else
    echo "FAIL - ${name} (esperado exit ${expected}, obtido ${actual})"
    cat "${out}"
    failures=$((failures + 1))
  fi
  rm -f "${out}"
}

assert_stderr_contains() {
  local name="$1"
  local needle="$2"
  shift 2
  local out
  out="$(mktemp)"
  "$@" >"${out}" 2>&1 || true
  if grep -F -q -- "${needle}" "${out}"; then
    echo "ok  - ${name}"
    passes=$((passes + 1))
  else
    echo "FAIL - ${name} (não encontrou '${needle}')"
    cat "${out}"
    failures=$((failures + 1))
  fi
  rm -f "${out}"
}

# --- push / workflow_dispatch (CLI) ---
assert_exit "push main" 0 bash "${POLICY}" push main
assert_exit "push master" 0 bash "${POLICY}" push master
assert_exit "push develop" 0 bash "${POLICY}" push develop
assert_exit "push development" 0 bash "${POLICY}" push development
assert_exit "push feat/login" 0 bash "${POLICY}" push feat/login
assert_exit "push feature/oauth" 0 bash "${POLICY}" push feature/oauth
assert_exit "push fix/null-pointer" 0 bash "${POLICY}" push fix/null-pointer
assert_exit "push bugfix/export" 0 bash "${POLICY}" push bugfix/export
assert_exit "push docs/readme" 0 bash "${POLICY}" push docs/readme
assert_exit "push chore/tooling" 0 bash "${POLICY}" push chore/tooling
assert_exit "push refactor/hex" 0 bash "${POLICY}" push refactor/hex
assert_exit "push test/fixtures" 0 bash "${POLICY}" push test/fixtures
assert_exit "push ci/workflows" 0 bash "${POLICY}" push ci/workflows
assert_exit "push perf/query" 0 bash "${POLICY}" push perf/query
assert_exit "push release/2.4.0" 0 bash "${POLICY}" push release/2.4.0
assert_exit "push hotfix/prod" 0 bash "${POLICY}" push hotfix/prod
assert_exit "push dependabot/maven" 0 bash "${POLICY}" push dependabot/maven
assert_exit "workflow_dispatch feat/login" 0 bash "${POLICY}" workflow_dispatch feat/login
assert_exit "push minha-branch" 1 bash "${POLICY}" push minha-branch
assert_exit "push feature sem barra" 1 bash "${POLICY}" push feature
assert_exit "push FIX-bug" 1 bash "${POLICY}" push FIX-bug
assert_exit "push branch vazia" 1 bash "${POLICY}" push ""
assert_exit "push sem branch" 1 bash "${POLICY}" push

# --- pull_request ---
assert_exit "PR feat → develop" 0 bash "${POLICY}" pull_request feat/login develop
assert_exit "PR feat → development" 0 bash "${POLICY}" pull_request feat/login development
assert_exit "PR docs → develop" 0 bash "${POLICY}" pull_request docs/readme develop
assert_exit "PR release → develop" 0 bash "${POLICY}" pull_request release/2.4.0 develop
assert_exit "PR hotfix → develop" 0 bash "${POLICY}" pull_request hotfix/prod develop
assert_exit "PR dependabot → develop" 0 bash "${POLICY}" pull_request dependabot/maven develop
assert_exit "PR develop → main" 0 bash "${POLICY}" pull_request develop main
assert_exit "PR development → master" 0 bash "${POLICY}" pull_request development master
assert_exit "PR release → main" 0 bash "${POLICY}" pull_request release/2.4.0 main
assert_exit "PR hotfix → master" 0 bash "${POLICY}" pull_request hotfix/prod master
assert_exit "PR dependabot → main" 0 bash "${POLICY}" pull_request dependabot/npm main
assert_exit "PR feat → feat (base não protegida)" 0 bash "${POLICY}" pull_request feat/a feat/b
assert_exit "PR feat → main" 1 bash "${POLICY}" pull_request feat/login main
assert_exit "PR main → develop" 1 bash "${POLICY}" pull_request main develop
assert_exit "PR develop → development" 1 bash "${POLICY}" pull_request develop development
assert_exit "PR nomenclatura inválida → develop" 1 bash "${POLICY}" pull_request minha-branch develop
assert_exit "PR sem refs" 1 bash "${POLICY}" pull_request
assert_exit "PR head vazio" 1 bash "${POLICY}" pull_request "" develop

# --- env (como o Actions) ---
assert_exit "push via env" 0 env EVENT_NAME=push PUSH_REF_NAME=feat/login bash "${POLICY}"
assert_exit "PR via env" 0 env EVENT_NAME=pull_request HEAD_REF=feat/login BASE_REF=develop bash "${POLICY}"
assert_exit "workflow_dispatch via env" 0 env EVENT_NAME=workflow_dispatch PUSH_REF_NAME=develop bash "${POLICY}"

# --- evento desconhecido e anotações do Actions ---
assert_exit "evento desconhecido (CLI)" 2 bash "${POLICY}" something
assert_exit "evento desconhecido (env)" 2 env EVENT_NAME=schedule bash "${POLICY}"
assert_exit "sem evento" 2 bash "${POLICY}"

assert_stderr_contains "anotação ::error:: no Actions" "::error::" \
  env GITHUB_ACTIONS=true bash "${POLICY}" push minha-branch
assert_stderr_contains "error: fora do Actions" "error: " \
  bash "${POLICY}" push minha-branch

echo
echo "${passes} passou(aram), ${failures} falhou(aram)"
if [[ "${failures}" -ne 0 ]]; then
  exit 1
fi
