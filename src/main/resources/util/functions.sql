-- imports
-- Separador de statement: ;; (schemaBefore). Um ; interno não parte o script.
CREATE EXTENSION IF NOT EXISTS pg_trgm;;
CREATE EXTENSION IF NOT EXISTS unaccent;;

-- functions

-- normalização textual
CREATE OR REPLACE FUNCTION purify(target text)
RETURNS text
LANGUAGE sql
IMMUTABLE
AS $$
  SELECT unaccent(lower(normalize(trim(target))))
$$;;

-- publicação de term_detail: uma linha com draft false exige EN, PT e ES no mesmo term_id
CREATE OR REPLACE FUNCTION term_draft_requires_all_types()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF NOT NEW.draft THEN
    IF (
      SELECT count(DISTINCT type)
      FROM (
        SELECT type FROM term_detail WHERE term_id = NEW.term_id
        UNION
        SELECT NEW.type
      ) AS term_types
    ) < 3 THEN
      RAISE EXCEPTION 'term % requires EN, PT and ES before publishing', NEW.term_id
        USING ERRCODE = 'check_violation';
    END IF;
  END IF;
  RETURN NEW;
END;
$$;;
