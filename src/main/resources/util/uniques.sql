ALTER TABLE profile DROP CONSTRAINT IF EXISTS profile_email_unique;
ALTER TABLE profile ADD CONSTRAINT profile_email_unique UNIQUE (email);
ALTER TABLE checker DROP CONSTRAINT IF EXISTS checker_profile_id_type_unique;
ALTER TABLE checker ADD CONSTRAINT checker_profile_id_type_unique UNIQUE (profile_id, type);
ALTER TABLE authority DROP CONSTRAINT IF EXISTS authority_profile_id_type_unique;
ALTER TABLE authority ADD CONSTRAINT authority_profile_id_type_unique UNIQUE (profile_id, type);
