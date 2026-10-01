-- tipos explícitos (rótulos = name() do enum de domínio)
-- Sem DO $$: o ScriptUtils do Spring parte o script no ';' interno do bloco.
DROP TYPE IF EXISTS profile_type CASCADE;
CREATE TYPE profile_type AS ENUM ('MASTER', 'WRITER', 'READER');
DROP TYPE IF EXISTS checker_type CASCADE;
CREATE TYPE checker_type AS ENUM ('CHANGE_EMAIL', 'VERIFY_EMAIL', 'CHANGE_PASSWORD', 'DELETE_PROFILE', 'SIGN_IN');
DROP TYPE IF EXISTS note_type CASCADE;
CREATE TYPE note_type AS ENUM ('PUBLIC', 'PROTECTED', 'PRIVATE');
