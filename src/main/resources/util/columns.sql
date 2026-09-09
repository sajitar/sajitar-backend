ALTER TABLE profile ADD COLUMN IF NOT EXISTS name_purified CHARACTER VARYING(30) GENERATED ALWAYS AS (purify(name)::text) STORED;
ALTER TABLE profile DROP CONSTRAINT IF EXISTS profile_email_format_check;
ALTER TABLE profile ADD CONSTRAINT profile_email_format_check CHECK (email::text ~ '^[a-z0-9._%+-]+@(?![.-])[a-z0-9.-]*[a-z0-9](?<!-)(?<!\.)[.][a-z]{2,}$'::text);

ALTER TABLE checker DROP CONSTRAINT IF EXISTS checker_code_format_check;
ALTER TABLE checker ADD CONSTRAINT checker_code_format_check CHECK (code::text ~ '^[0-9]{6}$'::text);
ALTER TABLE checker DROP CONSTRAINT IF EXISTS checker_attempts_range_check;
ALTER TABLE checker ADD CONSTRAINT checker_attempts_range_check CHECK ((attempts >= 0) AND (attempts <= 10));
ALTER TABLE checker DROP CONSTRAINT IF EXISTS checker_replaces_range_check;
ALTER TABLE checker ADD CONSTRAINT checker_replaces_range_check CHECK ((replaces >= 0) AND (replaces <= 3));
ALTER TABLE checker DROP CONSTRAINT IF EXISTS checker_profile_id_fkey;
ALTER TABLE checker ADD CONSTRAINT checker_profile_id_fkey FOREIGN KEY (profile_id) REFERENCES profile (id) ON DELETE CASCADE;

ALTER TABLE authority DROP CONSTRAINT IF EXISTS authority_profile_id_fkey;
ALTER TABLE authority ADD CONSTRAINT authority_profile_id_fkey FOREIGN KEY (profile_id) REFERENCES profile (id) ON DELETE CASCADE;

ALTER TABLE note DROP CONSTRAINT IF EXISTS note_profile_id_fkey;
ALTER TABLE note ADD CONSTRAINT note_profile_id_fkey FOREIGN KEY (profile_id) REFERENCES profile (id) ON DELETE CASCADE;
ALTER TABLE note ALTER COLUMN content SET NOT NULL;
ALTER TABLE note DROP CONSTRAINT IF EXISTS note_content_length_check;
ALTER TABLE note ADD CONSTRAINT note_content_length_check CHECK (char_length(content) <= 1000);
