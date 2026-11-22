-- tipos explícitos (rótulos = name() do enum de domínio)
-- Separador de statement: ;; (schemaBefore). Um ; interno não parte o script.
-- O tipo existente permanece: outro contexto Spring no mesmo Postgres guarda o OID.
DO $$
BEGIN
    CREATE TYPE profile_type AS ENUM ('MASTER', 'WRITER', 'READER');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END
$$;;
DO $$
BEGIN
    CREATE TYPE checker_type AS ENUM ('CHANGE_EMAIL', 'VERIFY_EMAIL', 'CHANGE_PASSWORD', 'DELETE_PROFILE', 'SIGN_IN');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END
$$;;
DO $$
BEGIN
    CREATE TYPE note_type AS ENUM ('PUBLIC', 'PROTECTED', 'PRIVATE');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END
$$;;
DO $$
BEGIN
    CREATE TYPE term_type AS ENUM ('EN', 'PT', 'ES');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END
$$;;
