-- imports
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- functions

-- normalização textual
CREATE OR REPLACE FUNCTION purify(target text)
RETURNS text
LANGUAGE sql
IMMUTABLE
AS $$
  SELECT unaccent(lower(normalize(trim(target))))
$$;
