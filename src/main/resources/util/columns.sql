ALTER TABLE profile ADD COLUMN IF NOT EXISTS name_purified CHARACTER VARYING(30) GENERATED ALWAYS AS (purify(name)::text) STORED;
ALTER TABLE profile DROP CONSTRAINT IF EXISTS profile_email_format_check;
ALTER TABLE profile ADD CONSTRAINT profile_email_format_check CHECK (email::text ~ '^[a-z0-9._%+-]+@(?![.-])[a-z0-9.-]*[a-z0-9](?<!-)(?<!\.)[.][a-z]{2,}$'::text);
