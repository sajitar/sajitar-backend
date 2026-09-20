#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'EOF'
Uso: show-local-vars.sh [host|compose|filter]

  host     (padrão) variáveis para rodar a API na máquina host (localhost)
  compose  variáveis como no container springboot do docker-compose (10.0.0.x)
  filter   lê KEY=VALUE da entrada padrão (ex.: printenv) e emite só as do projeto

Saída: linhas export VAR="valor", agrupadas e prontas para ~/.profile ou source.
EOF
}

repo_root() {
    local script_dir
    script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    cd "${script_dir}/../../../.." && pwd
}

declare -A VARS=()

load_local_env() {
    local env_file="$1"
    local line key value

    if [[ ! -f "$env_file" ]]; then
        echo "Arquivo não encontrado: $env_file" >&2
        exit 1
    fi

    while IFS= read -r line || [[ -n "$line" ]]; do
        line="${line%%#*}"
        line="${line#"${line%%[![:space:]]*}"}"
        line="${line%"${line##*[![:space:]]}"}"
        [[ -z "$line" ]] && continue
        if [[ "$line" =~ ^([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
            key="${BASH_REMATCH[1]}"
            value="${BASH_REMATCH[2]}"
            if [[ "$value" == \"*\" && "$value" == *\" ]]; then
                value="${value:1:${#value}-2}"
            fi
            VARS["$key"]="$value"
        fi
    done < "$env_file"
}

apply_target() {
    local target="$1"
    local postgres_user="${VARS[SPRING_DATASOURCE_USERNAME]:-${VARS[POSTGRES_USER]:-postgres}}"
    local postgres_password="${VARS[SPRING_DATASOURCE_PASSWORD]:-${VARS[POSTGRES_PASSWORD]:-}}"
    local postgres_db="${VARS[POSTGRES_DB]:-sajitar-db}"

    VARS[SPRING_DATASOURCE_USERNAME]="$postgres_user"
    VARS[SPRING_DATASOURCE_PASSWORD]="$postgres_password"

    case "$target" in
        host)
            VARS[SPRING_DATASOURCE_URL]="jdbc:postgresql://localhost:5432/${postgres_db}"
            VARS[SPRING_DATA_REDIS_HOST]="${VARS[SPRING_DATA_REDIS_HOST]:-127.0.0.1}"
            VARS[SPRING_DATA_REDIS_PORT]="${VARS[SPRING_DATA_REDIS_PORT]:-6379}"
            VARS[SAJITAR_MAIL_HOST]="${VARS[SAJITAR_MAIL_HOST]:-127.0.0.1}"
            ;;
        compose)
            VARS[SPRING_DATASOURCE_URL]="jdbc:postgresql://10.0.0.10:5432/${postgres_db}"
            VARS[SPRING_DATA_REDIS_HOST]="10.0.0.25"
            VARS[SPRING_DATA_REDIS_PORT]="6379"
            VARS[SAJITAR_MAIL_HOST]="10.0.0.35"
            VARS[SAJITAR_MAIL_PORT]="${VARS[SAJITAR_MAIL_PORT]:-1025}"
            VARS[SAJITAR_MAIL_FROM]="${VARS[SAJITAR_MAIL_FROM]:-noreply@localhost}"
            VARS[SPRING_PROFILES_ACTIVE]="${VARS[SPRING_PROFILES_ACTIVE]:-LOCAL}"
            ;;
        *)
            echo "Alvo inválido: $target" >&2
            usage >&2
            exit 1
            ;;
    esac
}

is_project_var() {
    local key="$1"
    case "$key" in
        SPRING_PROFILES_ACTIVE \
        | SPRING_DATASOURCE_* \
        | SPRING_JPA_* \
        | SPRING_SQL_* \
        | SPRING_DATA_REDIS_* \
        | SAJITAR_*)
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

load_from_stdin() {
    local line key value

    while IFS= read -r line || [[ -n "$line" ]]; do
        if [[ "$line" =~ ^([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
            key="${BASH_REMATCH[1]}"
            value="${BASH_REMATCH[2]}"
            if is_project_var "$key"; then
                VARS["$key"]="$value"
            fi
        fi
    done
}

print_exports() {
    local -a ordered=(
        SPRING_PROFILES_ACTIVE
        SPRING_DATASOURCE_USERNAME
        SPRING_DATASOURCE_PASSWORD
        SPRING_DATASOURCE_URL
        SPRING_JPA_HIBERNATE_DDL_AUTO
        SPRING_JPA_SHOW_SQL
        SPRING_SQL_INIT_MODE
        SPRING_SQL_BEFORE_FRAMEWORK
        SPRING_SQL_AFTER_FRAMEWORK
        SPRING_DATA_REDIS_HOST
        SPRING_DATA_REDIS_PORT
        SPRING_DATA_REDIS_USERNAME
        SPRING_DATA_REDIS_PASSWORD
        SPRING_DATA_REDIS_SSL_ENABLED
        SAJITAR_DOMAIN_VALIDATION_PROFILE_BIRTHDAY_MIN_AGE_YEARS
        SAJITAR_DOMAIN_VALIDATION_LIMIT_MAX
        SAJITAR_SECURITY_JWT_SECRET
        SAJITAR_SECURITY_JWT_EXPIRATION_SECONDS
        SAJITAR_SECURITY_JWT_REFRESH_EXPIRATION_SECONDS
        SAJITAR_SECURITY_JWT_SESSION_MAX_SECONDS
        SAJITAR_SECURITY_JWT_MAX_SESSIONS_PER_PROFILE
        SAJITAR_SECURITY_JWT_REFRESH_GRACE_SECONDS
        SAJITAR_SECURITY_JWT_ISSUER
        SAJITAR_SECURITY_JWT_AUDIENCE
        SAJITAR_SECURITY_ATTEMPT_CREDENTIALS_MAX
        SAJITAR_SECURITY_ATTEMPT_CREDENTIALS_WINDOW_SECONDS
        SAJITAR_SECURITY_ATTEMPT_REFRESH_MAX
        SAJITAR_SECURITY_ATTEMPT_REFRESH_WINDOW_SECONDS
        SAJITAR_SECURITY_ATTEMPT_TRUST_FORWARDED_FOR
        SAJITAR_MAIL_HOST
        SAJITAR_MAIL_PORT
        SAJITAR_MAIL_FROM
    )

    local -a groups=(
        "SPRING_PROFILES_ACTIVE"
        "SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD SPRING_DATASOURCE_URL"
        "SPRING_JPA_HIBERNATE_DDL_AUTO SPRING_JPA_SHOW_SQL SPRING_SQL_INIT_MODE SPRING_SQL_BEFORE_FRAMEWORK SPRING_SQL_AFTER_FRAMEWORK"
        "SPRING_DATA_REDIS_HOST SPRING_DATA_REDIS_PORT SPRING_DATA_REDIS_USERNAME SPRING_DATA_REDIS_PASSWORD SPRING_DATA_REDIS_SSL_ENABLED"
        "SAJITAR_DOMAIN_VALIDATION_PROFILE_BIRTHDAY_MIN_AGE_YEARS SAJITAR_DOMAIN_VALIDATION_LIMIT_MAX"
        "SAJITAR_SECURITY_JWT_SECRET SAJITAR_SECURITY_JWT_EXPIRATION_SECONDS SAJITAR_SECURITY_JWT_REFRESH_EXPIRATION_SECONDS SAJITAR_SECURITY_JWT_SESSION_MAX_SECONDS SAJITAR_SECURITY_JWT_MAX_SESSIONS_PER_PROFILE SAJITAR_SECURITY_JWT_REFRESH_GRACE_SECONDS SAJITAR_SECURITY_JWT_ISSUER SAJITAR_SECURITY_JWT_AUDIENCE"
        "SAJITAR_SECURITY_ATTEMPT_CREDENTIALS_MAX SAJITAR_SECURITY_ATTEMPT_CREDENTIALS_WINDOW_SECONDS SAJITAR_SECURITY_ATTEMPT_REFRESH_MAX SAJITAR_SECURITY_ATTEMPT_REFRESH_WINDOW_SECONDS SAJITAR_SECURITY_ATTEMPT_TRUST_FORWARDED_FOR"
        "SAJITAR_MAIL_HOST SAJITAR_MAIL_PORT SAJITAR_MAIL_FROM"
    )

    local group key has_any=false need_blank=false

    for group in "${groups[@]}"; do
        has_any=false
        for key in $group; do
            if [[ -n "${VARS[$key]+x}" ]]; then
                has_any=true
                break
            fi
        done
        [[ "$has_any" == false ]] && continue

        if [[ "$need_blank" == true ]]; then
            printf '\n'
        fi
        for key in $group; do
            if [[ -n "${VARS[$key]+x}" ]]; then
                printf 'export %s="%s"\n' "$key" "${VARS[$key]}"
            fi
        done
        need_blank=true
    done

    # Variáveis extras do projeto presentes na entrada, mas fora da ordem canônica.
    for key in "${!VARS[@]}"; do
        local known=false
        for ordered_key in "${ordered[@]}"; do
            if [[ "$key" == "$ordered_key" ]]; then
                known=true
                break
            fi
        done
        if [[ "$known" == false ]] && is_project_var "$key"; then
            printf 'export %s="%s"\n' "$key" "${VARS[$key]}"
        fi
    done
}

main() {
    local target="${1:-host}"

    case "$target" in
        -h|--help|help)
            usage
            exit 0
            ;;
        filter)
            load_from_stdin
            ;;
        host|compose)
            load_local_env "$(repo_root)/local.env"
            apply_target "$target"
            ;;
        *)
            echo "Alvo inválido: $target" >&2
            usage >&2
            exit 1
            ;;
    esac

    print_exports
}

main "$@"
